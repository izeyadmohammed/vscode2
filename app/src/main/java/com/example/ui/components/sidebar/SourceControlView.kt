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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Undo
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
import com.example.model.GitChangeItem
import com.example.model.GitCommit
import com.example.model.GitStatus
import com.example.model.ThemeConfig
import com.example.ui.components.FileIcon

@Composable
fun SourceControlView(
    theme: ThemeConfig,
    branch: String,
    changes: List<GitChangeItem>,
    stagedIds: Set<String>,
    commits: List<GitCommit>,
    nodes: List<FSNode>,
    onToggleStage: (String) -> Unit,
    onStageAll: () -> Unit,
    onDiscardChange: (String) -> Unit,
    onCommit: (String) -> Unit,
    onOpenDiff: (String) -> Unit
) {
    val c = theme.colors
    var commitMsg by remember { mutableStateOf("") }

    val stagedChanges = changes.filter { stagedIds.contains(it.fileId) }
    val unstagedChanges = changes.filter { !stagedIds.contains(it.fileId) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Commit box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(c.inputBg, RoundedCornerShape(3.dp))
                    .border(1.dp, c.inputBorder, RoundedCornerShape(3.dp))
                    .padding(8.dp)
            ) {
                if (commitMsg.isEmpty()) {
                    Text(
                        text = "Message (commit on '$branch')",
                        color = c.inputFg.copy(alpha = 0.45f),
                        fontSize = 12.sp
                    )
                }
                BasicTextField(
                    value = commitMsg,
                    onValueChange = { commitMsg = it },
                    textStyle = TextStyle(color = c.inputFg, fontSize = 12.sp),
                    cursorBrush = SolidColor(c.accent),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("git_commit_input")
                )
            }

            val canCommit = commitMsg.isNotBlank() && changes.isNotEmpty()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (canCommit) c.buttonBg else c.buttonBg.copy(alpha = 0.45f))
                    .clickable(enabled = canCommit) {
                        onCommit(commitMsg.trim())
                        commitMsg = ""
                    }
                    .testTag("git_commit_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = c.buttonFg, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Commit", color = c.buttonFg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        HorizontalDivider(color = c.sidebarBorder)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (changes.isEmpty()) {
                item {
                    Text(
                        text = "Working tree clean. Edit any file to track changes and open diffs.",
                        color = c.sidebarFg.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                if (stagedChanges.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("STAGED CHANGES", color = c.sidebarFg.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(stagedChanges.size.toString(), color = c.sidebarFg.copy(alpha = 0.7f), fontSize = 11.sp)
                        }
                    }
                    items(stagedChanges, key = { "staged-${it.fileId}" }) { ch ->
                        GitChangeRow(
                            theme = theme,
                            item = ch,
                            fileNode = nodes.firstOrNull { it.id == ch.fileId },
                            isStaged = true,
                            onOpenDiff = { onOpenDiff(ch.fileId) },
                            onDiscard = { onDiscardChange(ch.fileId) },
                            onToggleStage = { onToggleStage(ch.fileId) }
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("CHANGES", color = c.sidebarFg.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Stage All Changes",
                                tint = c.sidebarFg.copy(alpha = 0.75f),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable(onClick = onStageAll)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(unstagedChanges.size.toString(), color = c.sidebarFg.copy(alpha = 0.7f), fontSize = 11.sp)
                        }
                    }
                }
                items(unstagedChanges, key = { "unstaged-${it.fileId}" }) { ch ->
                    GitChangeRow(
                        theme = theme,
                        item = ch,
                        fileNode = nodes.firstOrNull { it.id == ch.fileId },
                        isStaged = false,
                        onOpenDiff = { onOpenDiff(ch.fileId) },
                        onDiscard = { onDiscardChange(ch.fileId) },
                        onToggleStage = { onToggleStage(ch.fileId) }
                    )
                }
            }

            item {
                HorizontalDivider(color = c.sidebarBorder, modifier = Modifier.padding(top = 8.dp))
                Text(
                    text = "COMMITS",
                    color = c.sidebarFg.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            items(commits, key = { it.id }) { cm ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Filled.Commit, contentDescription = null, tint = c.accent, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(cm.message, color = c.sidebarFg, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${cm.hash} · ${cm.author}", color = c.sidebarFg.copy(alpha = 0.55f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun GitChangeRow(
    theme: ThemeConfig,
    item: GitChangeItem,
    fileNode: FSNode?,
    isStaged: Boolean,
    onOpenDiff: () -> Unit,
    onDiscard: () -> Unit,
    onToggleStage: () -> Unit
) {
    val c = theme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clickable(onClick = onOpenDiff)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileIcon(name = item.name, language = fileNode?.language, size = 13.dp)
        Spacer(Modifier.width(6.dp))
        Text(
            text = item.name,
            color = c.sidebarFg,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = item.path,
            color = c.sidebarFg.copy(alpha = 0.5f),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (!isStaged) {
            Icon(
                imageVector = Icons.Filled.Undo,
                contentDescription = "Discard Changes",
                tint = c.sidebarFg.copy(alpha = 0.75f),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(14.dp)
                    .clickable(onClick = onDiscard)
            )
        }
        Icon(
            imageVector = if (isStaged) Icons.Filled.Remove else Icons.Filled.Add,
            contentDescription = if (isStaged) "Unstage" else "Stage",
            tint = c.sidebarFg.copy(alpha = 0.75f),
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .size(14.dp)
                .clickable(onClick = onToggleStage)
        )
        Text(
            text = if (item.status == GitStatus.MODIFIED) "M" else "U",
            color = if (item.status == GitStatus.MODIFIED) Color(0xFFE2C08D) else Color(0xFF73C991),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
