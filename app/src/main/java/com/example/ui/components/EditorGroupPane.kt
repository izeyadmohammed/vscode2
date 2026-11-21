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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.EditorSettings
import com.example.model.EditorTab
import com.example.model.FSNode
import com.example.model.FSNodeType
import com.example.model.FileLanguage
import com.example.model.TabKind
import com.example.model.ThemeConfig
import com.example.ui.components.editor.CodeEditor
import com.example.ui.components.editor.DiffViewer
import com.example.ui.components.editor.LivePreviewPane
import com.example.ui.components.editor.SettingsTab
import com.example.ui.components.editor.WelcomeTab
import com.example.utils.FsHelpers
import com.example.viewmodel.JumpToLineEvent

@Composable
fun EditorGroupPane(
    groupId: String,
    theme: ThemeConfig,
    settings: EditorSettings,
    tabs: List<EditorTab>,
    activeTabId: String?,
    nodes: List<FSNode>,
    dirtyFileIds: Set<String>,
    committedSnapshot: Map<String, String>,
    breakpoints: Map<String, List<Int>>,
    jumpToLine: JumpToLineEvent?,
    findTriggerNonce: Int,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
    onFocusGroup: () -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onChangeContent: (String, String) -> Unit,
    onSaveFile: (String) -> Unit,
    onCursorChange: (Int, Int) -> Unit,
    onToggleBreakpoint: (String, Int) -> Unit,
    onUpdateSettings: ((EditorSettings) -> EditorSettings) -> Unit,
    onResetWorkspace: () -> Unit,
    onOpenFile: (String, Int?) -> Unit,
    onNewFile: () -> Unit,
    onOpenLocalFolder: () -> Unit,
    onOpenQuickOpen: () -> Unit,
    onOpenTerminal: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenCloudSync: () -> Unit,
    onRunActiveFile: (String?) -> Unit,
    onOpenPreview: (String) -> Unit,
    onSplitEditor: () -> Unit,
    onFormatDocument: (String) -> Unit,
    onExportFile: (String) -> Unit
) {
    val c = theme.colors
    val activeTab = tabs.firstOrNull { it.id == activeTabId } ?: tabs.firstOrNull()
    val activeFile = activeTab?.fileId?.let { fid -> nodes.firstOrNull { it.id == fid } }
    val recentFiles = nodes.filter { it.type == FSNodeType.FILE }
    val breadcrumbs = activeFile?.let { FsHelpers.getNodeBreadcrumbParts(nodes, it.id) } ?: emptyList()

    val canPreview = activeTab?.kind == TabKind.FILE && activeFile != null &&
        (activeFile.language == FileLanguage.HTML || activeFile.language == FileLanguage.MARKDOWN)
    val canRun = activeTab?.kind == TabKind.FILE && activeFile != null &&
        (activeFile.language == FileLanguage.JAVASCRIPT || activeFile.language == FileLanguage.TYPESCRIPT)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.editorBg)
            .clickable(enabled = !isFocused, onClick = onFocusGroup)
    ) {
        // Tab bar + actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(c.tabBarBg)
                .border(0.5.dp, c.tabBorder),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val isActive = activeTab?.id == tab.id
                    val fileNode = tab.fileId?.let { fid -> nodes.firstOrNull { it.id == fid } }
                    val isDirty = tab.kind == TabKind.FILE && tab.fileId != null && dirtyFileIds.contains(tab.fileId)

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(if (isActive) c.tabActiveBg else c.tabInactiveBg)
                            .border(0.5.dp, c.tabBorder)
                            .clickable { onSelectTab(tab.id) }
                    ) {
                        if (isActive && isFocused) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(c.tabActiveBorderTop)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(start = 10.dp, end = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (fileNode != null) {
                                FileIcon(name = fileNode.name, language = fileNode.language, size = 14.dp)
                            } else {
                                Icon(Icons.Filled.Code, contentDescription = null, tint = c.accent, modifier = Modifier.size(14.dp))
                            }
                            Text(
                                text = tab.title,
                                color = if (isActive) c.tabActiveFg else c.tabInactiveFg,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                            if (isDirty) {
                                Text("●", color = c.tabActiveFg, fontSize = 10.sp)
                            }
                            IconButton(
                                onClick = { onCloseTab(tab.id) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.cd_close_tab),
                                    tint = if (isActive) c.tabActiveFg.copy(alpha = 0.85f) else c.tabInactiveFg.copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Right Editor Actions
            Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canRun && activeFile != null) {
                    IconButton(
                        onClick = { onRunActiveFile(activeFile.id) },
                        modifier = Modifier.size(30.dp).testTag("editor_run_button")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Run Active File", tint = c.tabActiveFg, modifier = Modifier.size(16.dp))
                    }
                }
                if (canPreview && activeFile != null) {
                    IconButton(
                        onClick = { onOpenPreview(activeFile.id) },
                        modifier = Modifier.size(30.dp).testTag("editor_preview_button")
                    ) {
                        Icon(Icons.Filled.Visibility, contentDescription = "Open Live Preview", tint = c.tabActiveFg, modifier = Modifier.size(15.dp))
                    }
                }
                if (activeTab?.kind == TabKind.FILE && activeFile != null) {
                    IconButton(
                        onClick = { onSaveFile(activeFile.id) },
                        modifier = Modifier.size(30.dp).testTag("editor_save_button")
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = "Save File", tint = c.tabActiveFg, modifier = Modifier.size(15.dp))
                    }
                    IconButton(
                        onClick = { onExportFile(activeFile.id) },
                        modifier = Modifier.size(30.dp).testTag("editor_export_button")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = "Export File", tint = c.tabActiveFg, modifier = Modifier.size(15.dp))
                    }
                    IconButton(
                        onClick = { onFormatDocument(activeFile.id) },
                        modifier = Modifier.size(30.dp).testTag("editor_format_button")
                    ) {
                        Icon(Icons.Filled.AutoFixHigh, contentDescription = "Format Document", tint = c.tabActiveFg, modifier = Modifier.size(15.dp))
                    }
                }
                if (groupId == "primary") {
                    IconButton(
                        onClick = onSplitEditor,
                        modifier = Modifier.size(30.dp).testTag("editor_split_button")
                    ) {
                        Icon(Icons.Filled.VerticalSplit, contentDescription = "Split Editor", tint = c.tabActiveFg, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }

        // Breadcrumbs Bar
        if (activeFile != null && activeTab?.kind == TabKind.FILE && breadcrumbs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .background(c.editorBg)
                    .padding(horizontal = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                breadcrumbs.forEachIndexed { idx, part ->
                    if (idx > 0) {
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = c.tabInactiveFg.copy(alpha = 0.6f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    if (part.type == FSNodeType.FILE) {
                        FileIcon(name = part.name, language = activeFile.language, size = 12.dp)
                    }
                    Text(
                        text = part.name,
                        color = if (idx == breadcrumbs.lastIndex) c.editorFg else c.tabInactiveFg,
                        fontSize = 11.sp,
                        fontWeight = if (idx == breadcrumbs.lastIndex) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Active Tab Body
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                activeTab == null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Code,
                            contentDescription = null,
                            tint = c.editorFg.copy(alpha = 0.16f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Show All Commands · Ctrl+Shift+P", color = c.editorFg.copy(alpha = 0.45f), fontSize = 12.sp)
                        Text("Go to File · Ctrl+P", color = c.editorFg.copy(alpha = 0.45f), fontSize = 12.sp)
                        Text("Toggle Terminal · Ctrl+`", color = c.editorFg.copy(alpha = 0.45f), fontSize = 12.sp)
                    }
                }
                activeTab.kind == TabKind.WELCOME -> WelcomeTab(
                    theme = theme,
                    recentFiles = recentFiles,
                    allNodes = nodes,
                    onNewFile = onNewFile,
                    onOpenLocalFolder = onOpenLocalFolder,
                    onOpenQuickOpen = onOpenQuickOpen,
                    onOpenTerminal = onOpenTerminal,
                    onOpenThemes = onOpenThemes,
                    onOpenCloudSync = onOpenCloudSync,
                    onOpenFile = { fid -> onOpenFile(fid, null) }
                )
                activeTab.kind == TabKind.SETTINGS -> SettingsTab(
                    theme = theme,
                    settings = settings,
                    onUpdateSettings = onUpdateSettings,
                    onResetWorkspace = onResetWorkspace
                )
                activeTab.kind == TabKind.DIFF && activeFile != null -> DiffViewer(
                    theme = theme,
                    fileName = activeFile.name,
                    language = activeFile.language ?: FileLanguage.PLAINTEXT,
                    originalContent = committedSnapshot[activeFile.id] ?: "",
                    modifiedContent = activeFile.content ?: ""
                )
                activeTab.kind == TabKind.PREVIEW && activeFile != null -> LivePreviewPane(
                    theme = theme,
                    file = activeFile
                )
                activeFile != null -> CodeEditor(
                    fileId = activeFile.id,
                    content = activeFile.content ?: "",
                    language = activeFile.language ?: FileLanguage.PLAINTEXT,
                    theme = theme,
                    settings = settings,
                    breakpoints = breakpoints[activeFile.id] ?: emptyList(),
                    jumpToLine = if (jumpToLine?.fileId == activeFile.id) jumpToLine else null,
                    findTriggerNonce = if (isFocused) findTriggerNonce else 0,
                    onChange = { valStr -> onChangeContent(activeFile.id, valStr) },
                    onCursorChange = onCursorChange,
                    onToggleBreakpoint = { ln -> onToggleBreakpoint(activeFile.id, ln) }
                )
            }
        }
    }
}
