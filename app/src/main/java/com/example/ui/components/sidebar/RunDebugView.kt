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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FSNode
import com.example.model.FileLanguage
import com.example.model.ThemeConfig
import com.example.utils.FsHelpers

private data class BreakpointEntry(
    val fileId: String,
    val path: String,
    val name: String,
    val line: Int
)

@Composable
fun RunDebugView(
    theme: ThemeConfig,
    activeFile: FSNode?,
    nodes: List<FSNode>,
    breakpoints: Map<String, List<Int>>,
    onRunActiveFile: () -> Unit,
    onOpenPreview: () -> Unit,
    onRemoveBreakpoint: (String, Int) -> Unit,
    onJumpToLine: (String, Int) -> Unit
) {
    val c = theme.colors
    val bpEntries = breakpoints.entries.flatMap { (fileId, lines) ->
        val file = nodes.firstOrNull { it.id == fileId } ?: return@flatMap emptyList()
        val path = FsHelpers.getNodePath(nodes, fileId)
        lines.map { ln -> BreakpointEntry(fileId, path, file.name, ln) }
    }

    val canRunJs = activeFile?.language == FileLanguage.JAVASCRIPT || activeFile?.language == FileLanguage.TYPESCRIPT
    val canPreview = activeFile?.language == FileLanguage.HTML || activeFile?.language == FileLanguage.MARKDOWN

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (canRunJs) c.buttonBg else c.buttonBg.copy(alpha = 0.45f))
                    .clickable(enabled = canRunJs, onClick = onRunActiveFile)
                    .testTag("run_active_file_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = c.buttonFg, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (canRunJs) "Run ${activeFile?.name}" else "Run Active JS/TS File",
                        color = c.buttonFg,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (canPreview) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .border(1.dp, c.inputBorder, RoundedCornerShape(3.dp))
                        .clickable(onClick = onOpenPreview)
                        .testTag("run_open_preview_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Visibility, contentDescription = null, tint = c.sidebarFg, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Open Live Preview", color = c.sidebarFg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Text(
                text = "Tap any line number in the editor gutter to toggle breakpoints. Output prints directly to the Debug Console.",
                color = c.sidebarFg.copy(alpha = 0.65f),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }

        HorizontalDivider(color = c.sidebarBorder)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("BREAKPOINTS", color = c.sidebarFg.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(bpEntries.size.toString(), color = c.sidebarFg.copy(alpha = 0.7f), fontSize = 11.sp)
        }
        HorizontalDivider(color = c.sidebarBorder)

        if (bpEntries.isEmpty()) {
            Text(
                text = "No breakpoints set.",
                color = c.sidebarFg.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(14.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(bpEntries, key = { "${it.fileId}-${it.line}" }) { bp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clickable { onJumpToLine(bp.fileId, bp.line) }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.FiberManualRecord, contentDescription = null, tint = Color(0xFFE51400), modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(bp.name, color = c.sidebarFg, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.width(6.dp))
                        Text(bp.path, color = c.sidebarFg.copy(alpha = 0.5f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text("Ln ${bp.line}", color = c.sidebarFg.copy(alpha = 0.65f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Remove breakpoint",
                            tint = c.sidebarFg.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onRemoveBreakpoint(bp.fileId, bp.line) }
                        )
                    }
                }
            }
        }
    }
}
