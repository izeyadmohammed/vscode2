package com.example.ui.components.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FSNode
import com.example.model.ThemeConfig
import com.example.ui.components.FileIcon
import com.example.utils.FsHelpers

@Composable
fun WelcomeTab(
    theme: ThemeConfig,
    recentFiles: List<FSNode>,
    allNodes: List<FSNode>,
    onNewFile: () -> Unit,
    onOpenLocalFolder: () -> Unit,
    onOpenQuickOpen: () -> Unit,
    onOpenTerminal: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenCloudSync: () -> Unit,
    onOpenFile: (String) -> Unit
) {
    val c = theme.colors

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(c.editorBg)
    ) {
        val isWide = maxWidth >= 640.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isWide) 36.dp else 20.dp, vertical = 28.dp)
        ) {
            Text(
                text = "Visual Studio Code",
                color = c.editorFg,
                fontSize = if (isWide) 30.sp else 24.sp,
                fontWeight = FontWeight.Light
            )
            Text(
                text = "Editing evolved",
                color = c.editorFg.copy(alpha = 0.65f),
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        StartAndRecentColumn(
                            theme = theme,
                            recentFiles = recentFiles,
                            allNodes = allNodes,
                            onNewFile = onNewFile,
                            onOpenLocalFolder = onOpenLocalFolder,
                            onOpenQuickOpen = onOpenQuickOpen,
                            onOpenCloudSync = onOpenCloudSync,
                            onOpenTerminal = onOpenTerminal,
                            onOpenFile = onOpenFile
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        WalkthroughsAndShortcutsColumn(
                            theme = theme,
                            onOpenThemes = onOpenThemes,
                            onOpenQuickOpen = onOpenQuickOpen
                        )
                    }
                }
            } else {
                StartAndRecentColumn(
                    theme = theme,
                    recentFiles = recentFiles,
                    allNodes = allNodes,
                    onNewFile = onNewFile,
                    onOpenLocalFolder = onOpenLocalFolder,
                    onOpenQuickOpen = onOpenQuickOpen,
                    onOpenCloudSync = onOpenCloudSync,
                    onOpenTerminal = onOpenTerminal,
                    onOpenFile = onOpenFile
                )
                Spacer(Modifier.height(24.dp))
                WalkthroughsAndShortcutsColumn(
                    theme = theme,
                    onOpenThemes = onOpenThemes,
                    onOpenQuickOpen = onOpenQuickOpen
                )
            }
        }
    }
}

@Composable
private fun StartAndRecentColumn(
    theme: ThemeConfig,
    recentFiles: List<FSNode>,
    allNodes: List<FSNode>,
    onNewFile: () -> Unit,
    onOpenLocalFolder: () -> Unit,
    onOpenQuickOpen: () -> Unit,
    onOpenCloudSync: () -> Unit,
    onOpenTerminal: () -> Unit,
    onOpenFile: (String) -> Unit
) {
    val c = theme.colors

    Text("Start", color = c.editorFg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(10.dp))

    WelcomeActionLink(Icons.Filled.NoteAdd, "New File...", c.accent, "welcome_new_file", onNewFile)
    WelcomeActionLink(Icons.Filled.CreateNewFolder, "Open Local Folder from Device...", c.accent, "welcome_open_folder", onOpenLocalFolder)
    WelcomeActionLink(Icons.Filled.FolderOpen, "Open File (Ctrl+P)...", c.accent, "welcome_open_file", onOpenQuickOpen)
    WelcomeActionLink(Icons.Filled.Cloud, "Supabase Cloud Sync & Accounts...", c.accent, "welcome_cloud_sync", onOpenCloudSync)
    WelcomeActionLink(Icons.Filled.Terminal, "Open Integrated Terminal...", c.accent, "welcome_terminal", onOpenTerminal)

    Spacer(Modifier.height(24.dp))
    Text("Recent Files", color = c.editorFg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(10.dp))

    if (recentFiles.isEmpty()) {
        Text(
            text = "No recent files yet. Create a file or open a folder to begin.",
            color = c.editorFg.copy(alpha = 0.55f),
            fontSize = 12.sp
        )
    } else {
        recentFiles.take(6).forEach { file ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onOpenFile(file.id) }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FileIcon(name = file.name, language = file.language, size = 14.dp)
                Text(
                    text = file.name,
                    color = c.accent,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Text(
                    text = FsHelpers.getNodePath(allNodes, file.id),
                    color = c.editorFg.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun WalkthroughsAndShortcutsColumn(
    theme: ThemeConfig,
    onOpenThemes: () -> Unit,
    onOpenQuickOpen: () -> Unit
) {
    val c = theme.colors

    Text("Walkthroughs", color = c.editorFg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(10.dp))

    WalkthroughCard(
        theme = theme,
        icon = Icons.Filled.Palette,
        title = "Choose your Color Theme",
        description = "Switch between Dark Modern, Dark+, Light Modern, Monokai, GitHub Dark, and Dracula.",
        onClick = onOpenThemes
    )
    Spacer(Modifier.height(10.dp))
    WalkthroughCard(
        theme = theme,
        icon = Icons.Filled.Code,
        title = "Master the Command Palette",
        description = "Open the Command Center to run any workbench command or jump directly to workspace files.",
        onClick = onOpenQuickOpen
    )

    Spacer(Modifier.height(22.dp))
    Text("Keyboard Shortcuts", color = c.editorFg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(10.dp))

    val shortcuts = listOf(
        "Show All Commands" to "Ctrl+Shift+P",
        "Quick Open File" to "Ctrl+P",
        "Toggle Side Bar" to "Ctrl+B",
        "Toggle Terminal / Panel" to "Ctrl+J",
        "Split Editor Right" to "Ctrl+\\",
        "Find in File" to "Ctrl+F"
    )
    shortcuts.forEach { (label, keys) ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = c.editorFg.copy(alpha = 0.75f), fontSize = 12.sp)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(c.inputBg)
                    .border(0.5.dp, c.inputBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = keys,
                    color = c.editorFg,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun WelcomeActionLink(
    icon: ImageVector,
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
        Text(label, color = accent, fontSize = 13.sp)
    }
}

@Composable
private fun WalkthroughCard(
    theme: ThemeConfig,
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    val c = theme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(c.tabInactiveBg)
            .border(1.dp, c.tabBorder, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
        Column {
            Text(title, color = c.editorFg, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(description, color = c.editorFg.copy(alpha = 0.68f), fontSize = 12.sp)
        }
    }
}
