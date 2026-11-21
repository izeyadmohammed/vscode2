package com.example.ui.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BottomTab
import com.example.model.DebugLogEntry
import com.example.model.DebugLogType
import com.example.model.DiagnosticItem
import com.example.model.DiagnosticSeverity
import com.example.model.TerminalLineType
import com.example.model.TerminalSession
import com.example.model.ThemeConfig

@Composable
fun BottomPanel(
    theme: ThemeConfig,
    maximized: Boolean,
    activeTab: BottomTab,
    problems: List<DiagnosticItem>,
    outputLogs: List<String>,
    debugLogs: List<DebugLogEntry>,
    terminals: List<TerminalSession>,
    activeTerminalId: String,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
    onSelectTab: (BottomTab) -> Unit,
    onToggleMaximize: () -> Unit,
    onClose: () -> Unit,
    onJumpToProblem: (String, Int) -> Unit,
    onRunTerminalCommand: (String, String) -> Unit,
    onAddTerminal: () -> Unit,
    onSelectTerminal: (String) -> Unit,
    onDeleteTerminal: (String) -> Unit,
    onEvalDebugExpr: (String) -> Unit
) {
    val c = theme.colors
    var termInput by remember { mutableStateOf("") }
    var historyIdx by remember { mutableStateOf(-1) }
    var debugInput by remember { mutableStateOf("") }

    val activeTerminal = terminals.firstOrNull { it.id == activeTerminalId } ?: terminals.firstOrNull()
    val termListState = rememberLazyListState()
    val debugListState = rememberLazyListState()

    LaunchedEffect(activeTab, activeTerminal?.lines?.size) {
        if (activeTab == BottomTab.TERMINAL && activeTerminal != null && activeTerminal.lines.isNotEmpty()) {
            termListState.animateScrollToItem(activeTerminal.lines.lastIndex)
        }
    }
    LaunchedEffect(activeTab, debugLogs.size) {
        if (activeTab == BottomTab.DEBUG && debugLogs.isNotEmpty()) {
            debugListState.animateScrollToItem(debugLogs.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(c.panelBg)
            .border(0.5.dp, c.panelBorder)
    ) {
        // Header Tabs & Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BottomTab.entries.forEach { tab ->
                    val isActive = activeTab == tab
                    val badge = if (tab == BottomTab.PROBLEMS && problems.isNotEmpty()) problems.size else 0
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clickable { onSelectTab(tab) }
                            .testTag("bottom_tab_${tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = tab.label,
                                color = if (isActive) c.tabActiveFg else c.tabInactiveFg,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp
                            )
                            if (badge > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(c.badgeBg)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(badge.toString(), color = c.badgeFg, fontSize = 9.sp)
                                }
                            }
                        }
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(c.accent)
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (activeTab == BottomTab.TERMINAL) {
                    IconButton(onClick = onAddTerminal, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = "New Terminal", tint = c.panelFg, modifier = Modifier.size(16.dp))
                    }
                }
                IconButton(onClick = onToggleMaximize, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = if (maximized) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                        contentDescription = if (maximized) "Restore Panel" else "Maximize Panel",
                        tint = c.panelFg,
                        modifier = Modifier.size(17.dp)
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(30.dp).testTag("bottom_panel_close")) {
                    Icon(Icons.Filled.Close, contentDescription = "Close Panel", tint = c.panelFg, modifier = Modifier.size(15.dp))
                }
            }
        }

        // Body
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (activeTab) {
                BottomTab.PROBLEMS -> {
                    if (problems.isEmpty()) {
                        Text(
                            text = "No problems have been detected in the workspace.",
                            color = c.panelFg.copy(alpha = 0.65f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
                            items(problems, key = { it.id }) { p ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { onJumpToProblem(p.fileId, p.line) }
                                        .padding(horizontal = 8.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val icon = when (p.severity) {
                                        DiagnosticSeverity.ERROR -> Icons.Filled.Cancel
                                        DiagnosticSeverity.WARNING -> Icons.Filled.Warning
                                        DiagnosticSeverity.INFO -> Icons.Filled.Info
                                    }
                                    val tint = when (p.severity) {
                                        DiagnosticSeverity.ERROR -> Color(0xFFF87171)
                                        DiagnosticSeverity.WARNING -> Color(0xFFFBBF24)
                                        DiagnosticSeverity.INFO -> Color(0xFF38BDF8)
                                    }
                                    Icon(icon, contentDescription = p.severity.name, tint = tint, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = p.message,
                                        color = c.panelFg,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${p.filePath} [${p.line}, ${p.col}]",
                                        color = c.panelFg.copy(alpha = 0.6f),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
                BottomTab.OUTPUT -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(outputLogs) { log ->
                            Text(
                                text = log,
                                color = c.panelFg.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                BottomTab.DEBUG -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = debugListState,
                            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (debugLogs.isEmpty()) {
                                item {
                                    Text(
                                        text = "Debug Console ready. Run a JS file or evaluate a JavaScript expression below.",
                                        color = c.panelFg.copy(alpha = 0.55f),
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            } else {
                                items(debugLogs, key = { it.id }) { entry ->
                                    val col = when (entry.type) {
                                        DebugLogType.ERROR -> Color(0xFFF87171)
                                        DebugLogType.RESULT -> Color(0xFF60A5FA)
                                        DebugLogType.LOG -> c.panelFg
                                    }
                                    Text(
                                        text = entry.text,
                                        color = col,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .border(0.5.dp, c.panelBorder)
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(">", color = c.accent, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                            BasicTextField(
                                value = debugInput,
                                onValueChange = { debugInput = it },
                                singleLine = true,
                                textStyle = TextStyle(color = c.panelFg, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                                cursorBrush = SolidColor(c.accent),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (debugInput.isNotBlank()) {
                                            onEvalDebugExpr(debugInput.trim())
                                            debugInput = ""
                                        }
                                    }
                                ),
                                decorationBox = { inner ->
                                    if (debugInput.isEmpty()) {
                                        Text("Evaluate expression...", color = c.panelFg.copy(alpha = 0.45f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    inner()
                                },
                                modifier = Modifier.weight(1f).testTag("debug_console_input")
                            )
                        }
                    }
                }
                BottomTab.TERMINAL -> if (activeTerminal != null) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            // Compact terminal switcher chip bar on narrow phones
                            if (isCompact && terminals.size > 1) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    terminals.forEach { term ->
                                        val sel = term.id == activeTerminal.id
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (sel) c.listActiveBg else c.inputBg)
                                                .clickable { onSelectTerminal(term.id) }
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(term.name, color = if (sel) c.listActiveFg else c.panelFg, fontSize = 11.sp)
                                            Icon(
                                                Icons.Filled.Close,
                                                contentDescription = "Kill Terminal",
                                                tint = c.panelFg.copy(alpha = 0.7f),
                                                modifier = Modifier.size(12.dp).clickable { onDeleteTerminal(term.id) }
                                            )
                                        }
                                    }
                                }
                            }

                            LazyColumn(
                                state = termListState,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                items(activeTerminal.lines, key = { it.id }) { line ->
                                    val col = when (line.type) {
                                        TerminalLineType.ERROR -> Color(0xFFF87171)
                                        TerminalLineType.SUCCESS -> Color(0xFF4ADE80)
                                        TerminalLineType.INFO -> Color(0xFF60A5FA)
                                        else -> c.panelFg
                                    }
                                    Text(
                                        text = line.text,
                                        color = col,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Terminal Prompt Input + History Nav
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(0.5.dp, c.panelBorder)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text("dev@vscode", color = Color(0xFF4ADE80), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                                Text(":", color = c.panelFg.copy(alpha = 0.7f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = "~${if (activeTerminal.cwd == "/") "" else activeTerminal.cwd}",
                                    color = Color(0xFF60A5FA),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text("$", color = c.panelFg, fontSize = 12.sp, fontFamily = FontFamily.Monospace)

                                BasicTextField(
                                    value = termInput,
                                    onValueChange = { termInput = it },
                                    singleLine = true,
                                    textStyle = TextStyle(color = c.panelFg, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                                    cursorBrush = SolidColor(c.accent),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(
                                        onSend = {
                                            onRunTerminalCommand(activeTerminal.id, termInput)
                                            termInput = ""
                                            historyIdx = -1
                                        }
                                    ),
                                    decorationBox = { inner ->
                                        if (termInput.isEmpty()) {
                                            Text("type 'help'...", color = c.panelFg.copy(alpha = 0.4f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                        }
                                        inner()
                                    },
                                    modifier = Modifier.weight(1f).testTag("terminal_command_input")
                                )

                                if (activeTerminal.history.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            val hist = activeTerminal.history
                                            val next = if (historyIdx == -1) hist.lastIndex else (historyIdx - 1).coerceAtLeast(0)
                                            historyIdx = next
                                            termInput = hist[next]
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Previous Command", tint = c.panelFg.copy(alpha = 0.75f), modifier = Modifier.size(13.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            val hist = activeTerminal.history
                                            if (historyIdx != -1) {
                                                if (historyIdx + 1 >= hist.size) {
                                                    historyIdx = -1
                                                    termInput = ""
                                                } else {
                                                    historyIdx += 1
                                                    termInput = hist[historyIdx]
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Filled.ArrowDownward, contentDescription = "Next Command", tint = c.panelFg.copy(alpha = 0.75f), modifier = Modifier.size(13.dp))
                                    }
                                }
                            }
                        }

                        // Right Terminal Instances Switcher on wide screens
                        if (!isCompact) {
                            Column(
                                modifier = Modifier
                                    .width(140.dp)
                                    .fillMaxHeight()
                                    .border(0.5.dp, c.panelBorder)
                                    .padding(vertical = 4.dp)
                            ) {
                                terminals.forEach { term ->
                                    val isSel = term.id == activeTerminal.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(26.dp)
                                            .background(if (isSel) c.listActiveBg else Color.Transparent)
                                            .clickable { onSelectTerminal(term.id) }
                                            .padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Filled.Terminal, contentDescription = null, tint = if (isSel) c.listActiveFg else c.panelFg, modifier = Modifier.size(12.dp))
                                            Text(
                                                text = term.name,
                                                color = if (isSel) c.listActiveFg else c.panelFg,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (terminals.size > 1) {
                                            IconButton(
                                                onClick = { onDeleteTerminal(term.id) },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Kill Terminal", tint = c.panelFg.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
