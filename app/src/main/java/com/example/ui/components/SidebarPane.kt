package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActivityView
import com.example.model.EditorTab
import com.example.model.ExtensionItem
import com.example.model.FSNode
import com.example.model.FSNodeType
import com.example.model.GitChangeItem
import com.example.model.GitCommit
import com.example.model.InlineCreateState
import com.example.model.ThemeConfig
import com.example.ui.components.sidebar.ExplorerView
import com.example.ui.components.sidebar.ExtensionsView
import com.example.ui.components.sidebar.RunDebugView
import com.example.ui.components.sidebar.SearchView
import com.example.ui.components.sidebar.SourceControlView

@Composable
fun SidebarPane(
    theme: ThemeConfig,
    width: Dp,
    activeView: ActivityView,
    workspaceName: String,
    nodes: List<FSNode>,
    activeFileId: String?,
    openTabs: List<EditorTab>,
    dirtyFileIds: Set<String>,
    modifiedFileIds: Set<String>,
    untrackedFileIds: Set<String>,
    gitBranch: String,
    gitChanges: List<GitChangeItem>,
    stagedIds: Set<String>,
    commits: List<GitCommit>,
    breakpoints: Map<String, List<Int>>,
    extensions: List<ExtensionItem>,
    inlineCreate: InlineCreateState?,
    onSetInlineCreate: (InlineCreateState?) -> Unit,
    onSelectFile: (String, Int?) -> Unit,
    onCloseTab: (String) -> Unit,
    onToggleFolder: (String) -> Unit,
    onCollapseAllFolders: () -> Unit,
    onCreateNode: (String?, FSNodeType, String) -> Unit,
    onRenameNode: (String, String) -> Unit,
    onDeleteNode: (String) -> Unit,
    onDuplicateFile: (String) -> Unit,
    onExportFile: (String) -> Unit,
    onOpenLocalFolder: () -> Unit,
    onReplaceAllInWorkspace: (String, String, Boolean) -> Int,
    onToggleStage: (String) -> Unit,
    onStageAll: () -> Unit,
    onDiscardChange: (String) -> Unit,
    onCommit: (String) -> Unit,
    onOpenDiff: (String) -> Unit,
    onRunActiveFile: () -> Unit,
    onOpenPreview: () -> Unit,
    onRemoveBreakpoint: (String, Int) -> Unit,
    onToggleInstallExtension: (String) -> Unit,
    onToggleEnableExtension: (String) -> Unit,
    onOpenCommandPalette: () -> Unit
) {
    val c = theme.colors
    val activeFile = nodes.firstOrNull { it.id == activeFileId }

    Column(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(c.sidebarBg)
            .border(width = 0.5.dp, color = c.sidebarBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = activeView.title,
                color = c.sidebarFg,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.8.sp
            )
            IconButton(
                onClick = onOpenCommandPalette,
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreHoriz,
                    contentDescription = "Views and More Actions",
                    tint = c.sidebarFg.copy(alpha = 0.75f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (activeView) {
                ActivityView.EXPLORER -> ExplorerView(
                    theme = theme,
                    workspaceName = workspaceName,
                    nodes = nodes,
                    activeFileId = activeFileId,
                    openTabs = openTabs,
                    dirtyFileIds = dirtyFileIds,
                    modifiedFileIds = modifiedFileIds,
                    untrackedFileIds = untrackedFileIds,
                    commits = commits,
                    inlineCreate = inlineCreate,
                    onSetInlineCreate = onSetInlineCreate,
                    onSelectFile = onSelectFile,
                    onCloseTab = onCloseTab,
                    onToggleFolder = onToggleFolder,
                    onCollapseAllFolders = onCollapseAllFolders,
                    onCreateNode = onCreateNode,
                    onRenameNode = onRenameNode,
                    onDeleteNode = onDeleteNode,
                    onDuplicateFile = onDuplicateFile,
                    onExportFile = onExportFile,
                    onOpenLocalFolder = onOpenLocalFolder
                )
                ActivityView.SEARCH -> SearchView(
                    theme = theme,
                    nodes = nodes,
                    onSelectMatch = { fid, ln -> onSelectFile(fid, ln) },
                    onReplaceAllInWorkspace = onReplaceAllInWorkspace
                )
                ActivityView.GIT -> SourceControlView(
                    theme = theme,
                    branch = gitBranch,
                    changes = gitChanges,
                    stagedIds = stagedIds,
                    commits = commits,
                    nodes = nodes,
                    onToggleStage = onToggleStage,
                    onStageAll = onStageAll,
                    onDiscardChange = onDiscardChange,
                    onCommit = onCommit,
                    onOpenDiff = onOpenDiff
                )
                ActivityView.DEBUG -> RunDebugView(
                    theme = theme,
                    activeFile = activeFile,
                    nodes = nodes,
                    breakpoints = breakpoints,
                    onRunActiveFile = onRunActiveFile,
                    onOpenPreview = onOpenPreview,
                    onRemoveBreakpoint = onRemoveBreakpoint,
                    onJumpToLine = { fid, ln -> onSelectFile(fid, ln) }
                )
                ActivityView.EXTENSIONS -> ExtensionsView(
                    theme = theme,
                    extensions = extensions,
                    onToggleInstall = onToggleInstallExtension,
                    onToggleEnable = onToggleEnableExtension
                )
            }
        }
    }
}
