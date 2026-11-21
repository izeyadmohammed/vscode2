package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material.icons.filled.WebAsset
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ThemeConfig

private data class WorkbenchMenuEntry(
    val label: String = "",
    val shortcut: String? = null,
    val isSeparator: Boolean = false,
    val enabled: Boolean = true,
    val action: () -> Unit = {}
)

@Composable
fun TitleBar(
    theme: ThemeConfig,
    workspaceName: String,
    sidebarVisible: Boolean,
    panelVisible: Boolean,
    splitActive: Boolean,
    cloudUserEmail: String?,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isCompact: Boolean,
    onToggleSidebar: () -> Unit,
    onTogglePanel: () -> Unit,
    onToggleSplit: () -> Unit,
    onOpenCommandPalette: (String) -> Unit,
    onToggleThemeQuick: () -> Unit,
    onNewFile: () -> Unit,
    onNewFolder: () -> Unit,
    onOpenLocalFolder: () -> Unit,
    onOpenCloudSync: () -> Unit,
    onSaveFile: () -> Unit,
    onSaveAll: () -> Unit,
    onExportFile: () -> Unit,
    onCloseTab: () -> Unit,
    onFormatDocument: () -> Unit,
    onOpenFind: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWelcome: () -> Unit,
    onRunActiveFile: () -> Unit,
    onNewTerminal: () -> Unit,
    onNavigateHistory: (Int) -> Unit
) {
    val c = theme.colors
    var expandedMenu by remember { mutableStateOf<String?>(null) }

    val menus: Map<String, List<WorkbenchMenuEntry>> = remember(
        canGoBack, canGoForward
    ) {
        linkedMapOf(
            "File" to listOf(
                WorkbenchMenuEntry("New Text File", "Ctrl+N", action = onNewFile),
                WorkbenchMenuEntry("New Folder...", action = onNewFolder),
                WorkbenchMenuEntry("Open Local Folder...", action = onOpenLocalFolder),
                WorkbenchMenuEntry(isSeparator = true),
                WorkbenchMenuEntry("Save", "Ctrl+S", action = onSaveFile),
                WorkbenchMenuEntry("Save All", "Ctrl+K S", action = onSaveAll),
                WorkbenchMenuEntry("Export File...", action = onExportFile),
                WorkbenchMenuEntry("Supabase Cloud Sync...", action = onOpenCloudSync),
                WorkbenchMenuEntry(isSeparator = true),
                WorkbenchMenuEntry("Preferences: Settings", "Ctrl+,", action = onOpenSettings),
                WorkbenchMenuEntry("Color Theme...") { onOpenCommandPalette(">Preferences: Color Theme") },
                WorkbenchMenuEntry(isSeparator = true),
                WorkbenchMenuEntry("Close Editor", "Ctrl+W", action = onCloseTab)
            ),
            "Edit" to listOf(
                WorkbenchMenuEntry("Find", "Ctrl+F", action = onOpenFind),
                WorkbenchMenuEntry("Replace in Files", "Ctrl+Shift+H") { onOpenCommandPalette(">View: Show Search") },
                WorkbenchMenuEntry(isSeparator = true),
                WorkbenchMenuEntry("Format Document", "Shift+Alt+F", action = onFormatDocument)
            ),
            "View" to listOf(
                WorkbenchMenuEntry("Command Palette...", "Ctrl+Shift+P") { onOpenCommandPalette(">") },
                WorkbenchMenuEntry(isSeparator = true),
                WorkbenchMenuEntry("Primary Side Bar", "Ctrl+B", action = onToggleSidebar),
                WorkbenchMenuEntry("Bottom Panel", "Ctrl+J", action = onTogglePanel),
                WorkbenchMenuEntry("Split Editor", "Ctrl+\\", action = onToggleSplit),
                WorkbenchMenuEntry(isSeparator = true),
                WorkbenchMenuEntry("Explorer") { onOpenCommandPalette(">View: Show Explorer") },
                WorkbenchMenuEntry("Search") { onOpenCommandPalette(">View: Show Search") },
                WorkbenchMenuEntry("Source Control") { onOpenCommandPalette(">View: Show Source Control") },
                WorkbenchMenuEntry("Extensions") { onOpenCommandPalette(">View: Show Extensions") }
            ),
            "Go" to listOf(
                WorkbenchMenuEntry("Back", "Alt+Left", enabled = canGoBack) { onNavigateHistory(-1) },
                WorkbenchMenuEntry("Forward", "Alt+Right", enabled = canGoForward) { onNavigateHistory(1) },
                WorkbenchMenuEntry(isSeparator = true),
                WorkbenchMenuEntry("Go to File...", "Ctrl+P") { onOpenCommandPalette("") }
            ),
            "Run" to listOf(
                WorkbenchMenuEntry("Run Active File", "F5", action = onRunActiveFile),
                WorkbenchMenuEntry("Show Debug Console") { onOpenCommandPalette(">View: Show Debug Console") }
            ),
            "Terminal" to listOf(
                WorkbenchMenuEntry("New Terminal", action = onNewTerminal),
                WorkbenchMenuEntry("Toggle Terminal", "Ctrl+`", action = onTogglePanel)
            ),
            "Help" to listOf(
                WorkbenchMenuEntry("Welcome", action = onOpenWelcome),
                WorkbenchMenuEntry("Show All Commands", "F1") { onOpenCommandPalette(">") },
                WorkbenchMenuEntry("Settings", action = onOpenSettings)
            )
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(c.titleBarBg)
            .border(width = 0.5.dp, color = c.titleBarBorder)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: App logo + Menu
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onOpenWelcome,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("title_welcome_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Code,
                    contentDescription = stringResource(R.string.cd_app_home),
                    tint = Color(0xFF007ACC),
                    modifier = Modifier.size(20.dp)
                )
            }

            if (isCompact) {
                Box {
                    IconButton(
                        onClick = { expandedMenu = if (expandedMenu == "__root") null else "__root" },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("title_compact_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Workbench Menu",
                            tint = c.titleBarFg,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = expandedMenu == "__root",
                        onDismissRequest = { expandedMenu = null },
                        modifier = Modifier.background(c.dropdownBg)
                    ) {
                        menus.keys.forEach { menuName ->
                            DropdownMenuItem(
                                text = { Text(menuName, color = c.sidebarFg, fontSize = 13.sp) },
                                onClick = { expandedMenu = menuName }
                            )
                        }
                    }
                    menus.forEach { (menuName, entries) ->
                        DropdownMenu(
                            expanded = expandedMenu == menuName,
                            onDismissRequest = { expandedMenu = null },
                            modifier = Modifier.background(c.dropdownBg)
                        ) {
                            entries.forEach { item ->
                                if (item.isSeparator) {
                                    HorizontalDivider(color = c.dropdownBorder)
                                } else {
                                    DropdownMenuItem(
                                        enabled = item.enabled,
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(item.label, color = c.sidebarFg, fontSize = 12.sp)
                                                if (item.shortcut != null) {
                                                    Spacer(Modifier.width(16.dp))
                                                    Text(
                                                        item.shortcut,
                                                        color = c.sidebarFg.copy(alpha = 0.6f),
                                                        fontSize = 11.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            expandedMenu = null
                                            item.action()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                menus.forEach { (menuName, entries) ->
                    Box {
                        Text(
                            text = menuName,
                            color = c.titleBarFg,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { expandedMenu = if (expandedMenu == menuName) null else menuName }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                        DropdownMenu(
                            expanded = expandedMenu == menuName,
                            onDismissRequest = { expandedMenu = null },
                            modifier = Modifier.background(c.dropdownBg)
                        ) {
                            entries.forEach { item ->
                                if (item.isSeparator) {
                                    HorizontalDivider(color = c.dropdownBorder)
                                } else {
                                    DropdownMenuItem(
                                        enabled = item.enabled,
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(item.label, color = c.sidebarFg, fontSize = 12.sp)
                                                if (item.shortcut != null) {
                                                    Spacer(Modifier.width(16.dp))
                                                    Text(
                                                        item.shortcut,
                                                        color = c.sidebarFg.copy(alpha = 0.6f),
                                                        fontSize = 11.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            expandedMenu = null
                                            item.action()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Center: History arrows + Command Center pill
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (!isCompact) {
                IconButton(
                    onClick = { onNavigateHistory(-1) },
                    enabled = canGoBack,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = c.titleBarFg.copy(alpha = if (canGoBack) 0.9f else 0.3f),
                        modifier = Modifier.size(15.dp)
                    )
                }
                IconButton(
                    onClick = { onNavigateHistory(1) },
                    enabled = canGoForward,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.cd_forward),
                        tint = c.titleBarFg.copy(alpha = if (canGoForward) 0.9f else 0.3f),
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
            }

            Row(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(c.inputBg)
                    .border(1.dp, c.inputBorder, RoundedCornerShape(6.dp))
                    .clickable { onOpenCommandPalette("") }
                    .padding(horizontal = 10.dp)
                    .testTag("command_center_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.cd_command_palette),
                    tint = c.titleBarFg.copy(alpha = 0.7f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = workspaceName,
                    color = c.titleBarFg.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Right: Cloud Sync, Layout Toggles & Theme Switch
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onOpenCloudSync,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("title_cloud_sync_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Cloud,
                    contentDescription = stringResource(R.string.cd_cloud_sync),
                    tint = if (cloudUserEmail != null) Color(0xFF4ADE80) else c.titleBarFg,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = onToggleSidebar,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("title_toggle_sidebar_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.ViewSidebar,
                    contentDescription = stringResource(R.string.cd_toggle_sidebar),
                    tint = if (sidebarVisible) c.titleBarFg else c.titleBarFg.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = onTogglePanel,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("title_toggle_panel_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.WebAsset,
                    contentDescription = stringResource(R.string.cd_toggle_panel),
                    tint = if (panelVisible) c.titleBarFg else c.titleBarFg.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
            if (!isCompact) {
                IconButton(
                    onClick = onToggleSplit,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("title_toggle_split_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerticalSplit,
                        contentDescription = stringResource(R.string.cd_split_editor),
                        tint = if (splitActive) c.accent else c.titleBarFg,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            IconButton(
                onClick = onToggleThemeQuick,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("title_toggle_theme_button")
            ) {
                Icon(
                    imageVector = if (theme.isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = stringResource(R.string.cd_toggle_theme),
                    tint = c.titleBarFg,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
