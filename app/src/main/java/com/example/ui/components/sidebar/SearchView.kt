package com.example.ui.components.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FSNode
import com.example.model.FSNodeType
import com.example.model.ThemeConfig
import com.example.ui.components.FileIcon
import com.example.utils.FsHelpers

private data class SearchMatch(val line: Int, val text: String)
private data class FileSearchResult(val file: FSNode, val path: String, val matches: List<SearchMatch>)

@Composable
fun SearchView(
    theme: ThemeConfig,
    nodes: List<FSNode>,
    onSelectMatch: (String, Int) -> Unit,
    onReplaceAllInWorkspace: (String, String, Boolean) -> Int
) {
    val c = theme.colors
    var query by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var showReplace by remember { mutableStateOf(false) }
    var matchCase by remember { mutableStateOf(false) }
    var wholeWord by remember { mutableStateOf(false) }
    var useRegex by remember { mutableStateOf(false) }
    var collapsedFiles by remember { mutableStateOf<Set<String>>(emptySet()) }
    var replaceStatus by remember { mutableStateOf<String?>(null) }

    val results = remember(nodes, query, matchCase, wholeWord, useRegex) {
        if (query.isEmpty()) return@remember emptyList()
        val pattern = runCatching {
            val source = if (useRegex) query else Regex.escape(query)
            val wrapped = if (wholeWord) "\\b$source\\b" else source
            val opts = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
            Regex(wrapped, opts)
        }.getOrNull() ?: return@remember emptyList()

        nodes.filter { it.type == FSNodeType.FILE }.mapNotNull { file ->
            val lines = file.content.orEmpty().split("\n")
            val matches = lines.mapIndexedNotNull { idx, line ->
                if (pattern.containsMatchIn(line)) SearchMatch(idx + 1, line.trim()) else null
            }
            if (matches.isNotEmpty()) {
                FileSearchResult(file, FsHelpers.getNodePath(nodes, file.id), matches)
            } else null
        }
    }

    val totalMatches = results.sumOf { it.matches.size }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = if (showReplace) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                    contentDescription = "Toggle Replace",
                    tint = c.sidebarFg,
                    modifier = Modifier
                        .padding(top = 5.dp, end = 4.dp)
                        .size(16.dp)
                        .clickable { showReplace = !showReplace }
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .background(c.inputBg, RoundedCornerShape(3.dp))
                            .border(1.dp, c.inputBorder, RoundedCornerShape(3.dp))
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text("Search across files...", color = c.inputFg.copy(alpha = 0.45f), fontSize = 12.sp)
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = {
                                    query = it
                                    replaceStatus = null
                                },
                                singleLine = true,
                                textStyle = TextStyle(color = c.inputFg, fontSize = 12.sp),
                                cursorBrush = SolidColor(c.accent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_query_input")
                            )
                        }

                        SearchFlagChip("Aa", matchCase) { matchCase = !matchCase }
                        SearchFlagChip("ab", wholeWord) { wholeWord = !wholeWord }
                        SearchFlagChip(".*", useRegex) { useRegex = !useRegex }
                    }

                    if (showReplace) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                                    .background(c.inputBg, RoundedCornerShape(3.dp))
                                    .border(1.dp, c.inputBorder, RoundedCornerShape(3.dp))
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (replaceText.isEmpty()) {
                                    Text("Replace...", color = c.inputFg.copy(alpha = 0.45f), fontSize = 12.sp)
                                }
                                BasicTextField(
                                    value = replaceText,
                                    onValueChange = { replaceText = it },
                                    singleLine = true,
                                    textStyle = TextStyle(color = c.inputFg, fontSize = 12.sp),
                                    cursorBrush = SolidColor(c.accent),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .border(1.dp, c.inputBorder, RoundedCornerShape(3.dp))
                                    .clickable(enabled = query.isNotEmpty() && totalMatches > 0) {
                                        val count = onReplaceAllInWorkspace(query, replaceText, matchCase)
                                        replaceStatus = "Replaced $count occurrence(s)"
                                    }
                                    .testTag("search_replace_all_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FindReplace,
                                    contentDescription = "Replace All",
                                    tint = c.sidebarFg.copy(alpha = if (query.isNotEmpty() && totalMatches > 0) 0.95f else 0.35f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (query.isNotEmpty()) {
                val suffix = replaceStatus?.let { " · $it" } ?: ""
                Text(
                    text = "$totalMatches result${if (totalMatches == 1) "" else "s"} in ${results.size} file${if (results.size == 1) "" else "s"}$suffix",
                    color = c.sidebarFg.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 20.dp)
                )
            }
        }

        HorizontalDivider(color = c.sidebarBorder)

        if (query.isEmpty()) {
            Text(
                text = "Type to search across all workspace files. Tap any match to jump directly to that line.",
                color = c.sidebarFg.copy(alpha = 0.65f),
                fontSize = 12.sp,
                modifier = Modifier.padding(14.dp)
            )
        } else if (results.isEmpty()) {
            Text(
                text = "No results found for \"$query\".",
                color = c.sidebarFg.copy(alpha = 0.65f),
                fontSize = 12.sp,
                modifier = Modifier.padding(14.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                results.forEach { res ->
                    val isCollapsed = collapsedFiles.contains(res.file.id)
                    item(key = "hdr-${res.file.id}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .clickable {
                                    collapsedFiles = if (isCollapsed) {
                                        collapsedFiles - res.file.id
                                    } else {
                                        collapsedFiles + res.file.id
                                    }
                                }
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isCollapsed) Icons.Filled.KeyboardArrowRight else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = c.sidebarFg,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            FileIcon(name = res.file.name, language = res.file.language, size = 13.dp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = res.file.name,
                                color = c.sidebarFg,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = res.matches.size.toString(),
                                color = c.sidebarFg.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (!isCollapsed) {
                        items(res.matches, key = { "${res.file.id}-${it.line}" }) { match ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .clickable { onSelectMatch(res.file.id, match.line) }
                                    .padding(start = 28.dp, end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${match.line}:",
                                    color = c.sidebarFg.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = match.text,
                                    color = c.sidebarFg.copy(alpha = 0.85f),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchFlagChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val c = com.example.ui.theme.IdeTheme.current.colors
    Box(
        modifier = Modifier
            .padding(start = 3.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (active) c.listActiveBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (active) c.accent else c.sidebarFg.copy(alpha = 0.75f),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}
