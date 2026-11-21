package com.example.ui.components.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EditorSettings
import com.example.model.FileLanguage
import com.example.model.ThemeConfig
import com.example.utils.SyntaxHighlighter
import com.example.viewmodel.JumpToLineEvent
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun CodeEditor(
    fileId: String,
    content: String,
    language: FileLanguage,
    theme: ThemeConfig,
    settings: EditorSettings,
    breakpoints: List<Int>,
    jumpToLine: JumpToLineEvent?,
    findTriggerNonce: Int,
    onChange: (String) -> Unit,
    onCursorChange: (Int, Int) -> Unit,
    onToggleBreakpoint: (Int) -> Unit
) {
    val c = theme.colors
    var textFieldValue by remember(fileId) { mutableStateOf(TextFieldValue(content)) }
    var findVisible by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var currentMatchIdx by remember { mutableStateOf(0) }

    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()

    // Keep external updates (format document, discard, replace all) in sync
    LaunchedEffect(fileId, content) {
        if (textFieldValue.text != content) {
            val clamped = textFieldValue.selection.start.coerceIn(0, content.length)
            textFieldValue = TextFieldValue(text = content, selection = TextRange(clamped))
        }
    }

    LaunchedEffect(findTriggerNonce) {
        if (findTriggerNonce > 0) {
            findVisible = true
        }
    }

    val lines = remember(textFieldValue.text) {
        textFieldValue.text.split("\n")
    }
    val lineHeightSp = (settings.fontSize * 1.48f).sp
    val lineHeightDp = (settings.fontSize * 1.48f).dp

    val cursorLineAndCol = remember(textFieldValue.text, textFieldValue.selection) {
        val pos = textFieldValue.selection.start.coerceIn(0, textFieldValue.text.length)
        val before = textFieldValue.text.substring(0, pos)
        val line = before.count { it == '\n' } + 1
        val lastNl = before.lastIndexOf('\n')
        val col = if (lastNl == -1) pos + 1 else pos - lastNl
        line to col
    }

    LaunchedEffect(cursorLineAndCol) {
        onCursorChange(cursorLineAndCol.first, cursorLineAndCol.second)
    }

    // Jump to line
    LaunchedEffect(jumpToLine?.nonce, fileId) {
        if (jumpToLine != null && jumpToLine.fileId == fileId) {
            val targetLine = jumpToLine.line.coerceIn(1, max(1, lines.size))
            var charOffset = 0
            for (i in 0 until targetLine - 1) {
                charOffset += (lines.getOrNull(i)?.length ?: 0) + 1
            }
            textFieldValue = textFieldValue.copy(
                selection = TextRange(charOffset.coerceIn(0, textFieldValue.text.length))
            )
            val approxPx = ((targetLine - 1) * lineHeightDp.value * 2.2f).roundToInt()
            vScroll.animateScrollTo(max(0, approxPx))
        }
    }

    // Find match offsets
    val matchOffsets = remember(textFieldValue.text, findQuery) {
        if (findQuery.isEmpty()) emptyList()
        else {
            val out = mutableListOf<Int>()
            val lowerText = textFieldValue.text.lowercase()
            val lowerQ = findQuery.lowercase()
            var idx = lowerText.indexOf(lowerQ)
            while (idx >= 0) {
                out.add(idx)
                idx = lowerText.indexOf(lowerQ, idx + lowerQ.length)
            }
            out
        }
    }

    fun jumpToMatch(index: Int) {
        if (matchOffsets.isEmpty()) return
        val wrapped = ((index % matchOffsets.size) + matchOffsets.size) % matchOffsets.size
        currentMatchIdx = wrapped
        val offset = matchOffsets[wrapped]
        textFieldValue = textFieldValue.copy(
            selection = TextRange(offset, (offset + findQuery.length).coerceAtMost(textFieldValue.text.length))
        )
    }

    val visualTransformation = remember(language, theme.id, findQuery, settings.renderWhitespace) {
        VisualTransformation { annotated ->
            val raw = annotated.text
            val displayRaw = if (settings.renderWhitespace) {
                raw.replace(' ', '·')
            } else {
                raw
            }
            val highlighted: AnnotatedString = SyntaxHighlighter.highlightDocument(
                text = displayRaw,
                language = language,
                tokens = theme.tokens,
                searchQuery = findQuery
            )
            TransformedText(highlighted, OffsetMapping.Identity)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.editorBg)
    ) {
        // Find-in-File bar
        if (findVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.sidebarBg)
                    .border(0.5.dp, c.tabBorder)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = c.sidebarFg.copy(alpha = 0.7f), modifier = Modifier.size(15.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(c.inputBg)
                        .border(1.dp, c.focusBorder, RoundedCornerShape(3.dp))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (findQuery.isEmpty()) {
                        Text("Find in file...", color = c.inputFg.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                    BasicTextField(
                        value = findQuery,
                        onValueChange = {
                            findQuery = it
                            currentMatchIdx = 0
                        },
                        singleLine = true,
                        textStyle = TextStyle(color = c.inputFg, fontSize = 12.sp),
                        cursorBrush = SolidColor(c.accent),
                        modifier = Modifier.fillMaxWidth().testTag("editor_find_input")
                    )
                }
                Text(
                    text = if (findQuery.isEmpty()) "" else "${if (matchOffsets.isEmpty()) 0 else currentMatchIdx + 1}/${matchOffsets.size}",
                    color = c.sidebarFg.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                IconButton(
                    onClick = { jumpToMatch(currentMatchIdx - 1) },
                    enabled = matchOffsets.isNotEmpty(),
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Previous Match", tint = c.sidebarFg, modifier = Modifier.size(14.dp))
                }
                IconButton(
                    onClick = { jumpToMatch(currentMatchIdx + 1) },
                    enabled = matchOffsets.isNotEmpty(),
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(Icons.Filled.ArrowDownward, contentDescription = "Next Match", tint = c.sidebarFg, modifier = Modifier.size(14.dp))
                }
                IconButton(
                    onClick = {
                        findVisible = false
                        findQuery = ""
                    },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close Find", tint = c.sidebarFg, modifier = Modifier.size(14.dp))
                }
            }
        }

        // Main Editor Body + Gutter + Optional Minimap
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(vScroll)
                    .padding(vertical = 8.dp)
            ) {
                // Gutter (breakpoints + line numbers)
                if (settings.lineNumbers) {
                    Column(
                        modifier = Modifier
                            .background(c.editorGutterBg)
                            .padding(start = 6.dp, end = 10.dp)
                    ) {
                        lines.forEachIndexed { idx, _ ->
                            val lineNum = idx + 1
                            val hasBp = breakpoints.contains(lineNum)
                            val isActiveLine = lineNum == cursorLineAndCol.first
                            Row(
                                modifier = Modifier
                                    .height(lineHeightDp)
                                    .clickable { onToggleBreakpoint(lineNum) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.width(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (hasBp) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFEF4444))
                                        )
                                    }
                                }
                                Text(
                                    text = lineNum.toString(),
                                    color = if (isActiveLine) c.editorActiveLineNumberFg else c.editorLineNumberFg,
                                    fontSize = (settings.fontSize - 1).coerceAtLeast(10).sp,
                                    lineHeight = lineHeightSp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(30.dp)
                                )
                            }
                        }
                    }
                }

                // Editable Syntax-Highlighted Text Field
                val editorModifier = if (settings.wordWrap) {
                    Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                } else {
                    Modifier
                        .weight(1f)
                        .horizontalScroll(hScroll)
                        .padding(end = 24.dp)
                }

                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { incoming ->
                        val nextVal = applySmartEditing(textFieldValue, incoming, settings.tabSize)
                        textFieldValue = nextVal
                        if (nextVal.text != content) {
                            onChange(nextVal.text)
                        }
                    },
                    textStyle = TextStyle(
                        color = c.editorFg,
                        fontSize = settings.fontSize.sp,
                        lineHeight = lineHeightSp,
                        fontFamily = FontFamily.Monospace
                    ),
                    cursorBrush = SolidColor(c.accent),
                    visualTransformation = visualTransformation,
                    modifier = editorModifier.testTag("code_editor_textfield")
                )
            }

            // Minimap strip when enabled
            if (settings.minimap && lines.size > 1) {
                Column(
                    modifier = Modifier
                        .width(34.dp)
                        .fillMaxHeight()
                        .background(c.editorBg)
                        .border(0.5.dp, c.tabBorder.copy(alpha = 0.5f))
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    lines.take(80).forEachIndexed { idx, line ->
                        val lenFrac = (line.trim().length / 64f).coerceIn(0.08f, 1f)
                        val isCursor = idx + 1 == cursorLineAndCol.first
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(lenFrac)
                                .height(2.dp)
                                .background(
                                    if (isCursor) c.accent else c.editorFg.copy(alpha = if (line.isBlank()) 0.04f else 0.25f)
                                )
                        )
                    }
                }
            }
        }
    }
}

private fun applySmartEditing(
    previous: TextFieldValue,
    incoming: TextFieldValue,
    tabSize: Int
): TextFieldValue {
    val oldText = previous.text
    val newText = incoming.text
    if (newText.length != oldText.length + 1) return incoming
    val cursor = incoming.selection.start
    if (cursor <= 0 || cursor > newText.length) return incoming
    val inserted = newText[cursor - 1]

    // Auto-close brackets and quotes
    val pairClose = when (inserted) {
        '(' -> ')'
        '[' -> ']'
        '{' -> '}'
        '"' -> '"'
        '\'' -> '\''
        '`' -> '`'
        else -> null
    }
    if (pairClose != null) {
        val nextChar = newText.getOrNull(cursor)
        if (nextChar == null || nextChar.isWhitespace() || nextChar in listOf(')', ']', '}', ',', ';')) {
            val patched = newText.substring(0, cursor) + pairClose + newText.substring(cursor)
            return incoming.copy(text = patched, selection = TextRange(cursor))
        }
        return incoming
    }

    // Smart auto-indent on newline
    if (inserted == '\n') {
        val beforeNewline = newText.substring(0, cursor - 1)
        val lastLineStart = beforeNewline.lastIndexOf('\n') + 1
        val prevLine = beforeNewline.substring(lastLineStart)
        val baseIndent = prevLine.takeWhile { it == ' ' || it == '\t' }
        val trimmedPrev = prevLine.trimEnd()
        val extraIndent = if (trimmedPrev.endsWith("{") || trimmedPrev.endsWith(":") || trimmedPrev.endsWith("(")) {
            " ".repeat(tabSize)
        } else {
            ""
        }
        val indent = baseIndent + extraIndent
        if (indent.isNotEmpty()) {
            val patched = newText.substring(0, cursor) + indent + newText.substring(cursor)
            return incoming.copy(text = patched, selection = TextRange(cursor + indent.length))
        }
    }

    return incoming
}
