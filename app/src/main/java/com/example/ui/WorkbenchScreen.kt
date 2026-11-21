package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ActivityView
import com.example.model.BottomTab
import com.example.model.EditorTab
import com.example.model.FSNodeType
import com.example.model.InlineCreateState
import com.example.model.TabKind
import com.example.ui.components.ActivityBar
import com.example.ui.components.BottomPanel
import com.example.ui.components.CloudAccountModal
import com.example.ui.components.CommandPaletteModal
import com.example.ui.components.EditorGroupPane
import com.example.ui.components.SidebarPane
import com.example.ui.components.StatusBar
import com.example.ui.components.TitleBar
import com.example.viewmodel.IdeViewModel

@Composable
fun WorkbenchScreen(viewModel: IdeViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val theme = state.theme
    val c = theme.colors

    var pendingExportFileId by remember { mutableStateOf<String?>(null) }

    val openFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            viewModel.loadFromSafTree(treeUri)
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { targetUri: Uri? ->
        val fid = pendingExportFileId
        pendingExportFileId = null
        if (targetUri != null && fid != null) {
            viewModel.exportFileToUri(fid, targetUri)
        }
    }

    val triggerExportFile: (String?) -> Unit = { explicitId ->
        val fid = explicitId ?: state.activeFile?.id
        val file = fid?.let { id -> state.nodes.firstOrNull { it.id == id && it.type == FSNodeType.FILE } }
        if (file != null) {
            pendingExportFileId = file.id
            createDocumentLauncher.launch(file.name)
        }
    }

    val commands = remember(state.activeFile?.id) {
        viewModel.buildCommands(
            onTriggerOpenLocalFolder = { openFolderLauncher.launch(null) },
            onTriggerExportActiveFile = { triggerExportFile(null) }
        )
    }

    // Handle Android system Back navigation smoothly
    BackHandler(enabled = state.paletteOpen || state.cloudModalOpen || state.panelVisible) {
        when {
            state.paletteOpen -> viewModel.closeCommandPalette()
            state.cloudModalOpen -> viewModel.closeCloudModal()
            state.panelVisible -> viewModel.setPanelVisible(false)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(c.titleBarBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
    ) {
        val isCompact = maxWidth < 680.dp

        Column(modifier = Modifier.fillMaxSize()) {
            TitleBar(
                theme = theme,
                workspaceName = state.workspaceName,
                sidebarVisible = state.sidebarVisible,
                panelVisible = state.panelVisible,
                splitActive = state.splitVisible,
                cloudUserEmail = state.cloudSession?.email,
                canGoBack = state.canGoBack,
                canGoForward = state.canGoForward,
                isCompact = isCompact,
                onToggleSidebar = viewModel::toggleSidebar,
                onTogglePanel = viewModel::togglePanel,
                onToggleSplit = viewModel::toggleSplit,
                onOpenCommandPalette = viewModel::openCommandPalette,
                onToggleThemeQuick = viewModel::quickToggleTheme,
                onNewFile = {
                    viewModel.showActivityView(ActivityView.EXPLORER)
                    viewModel.setInlineCreate(InlineCreateState(null, FSNodeType.FILE))
                },
                onNewFolder = {
                    viewModel.showActivityView(ActivityView.EXPLORER)
                    viewModel.setInlineCreate(InlineCreateState(null, FSNodeType.FOLDER))
                },
                onOpenLocalFolder = { openFolderLauncher.launch(null) },
                onOpenCloudSync = viewModel::openCloudModal,
                onSaveFile = { viewModel.saveFile() },
                onSaveAll = viewModel::saveAll,
                onExportFile = { triggerExportFile(null) },
                onCloseTab = {
                    val activeId = state.activeTabId
                    if (activeId != null) viewModel.closeTab(activeId, state.focusedGroup)
                },
                onFormatDocument = { viewModel.formatDocument() },
                onOpenFind = viewModel::triggerFindInEditor,
                onOpenSettings = { viewModel.openSpecialTab(EditorTab("settings", TabKind.SETTINGS, title = "Settings")) },
                onOpenWelcome = { viewModel.openSpecialTab(EditorTab("welcome", TabKind.WELCOME, title = "Welcome")) },
                onRunActiveFile = { viewModel.runActiveFile() },
                onNewTerminal = viewModel::addTerminal,
                onNavigateHistory = viewModel::navigateHistory
            )

            // Main Workbench Area (Activity Bar + Sidebar + Editor Groups + Bottom Panel)
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                ActivityBar(
                    theme = theme,
                    activeView = state.activeView,
                    sidebarVisible = state.sidebarVisible,
                    gitChangesCount = state.gitChanges.size,
                    cloudUserEmail = state.cloudSession?.email,
                    onSelectView = viewModel::selectActivityView,
                    onOpenSettings = { viewModel.openSpecialTab(EditorTab("settings", TabKind.SETTINGS, title = "Settings")) },
                    onOpenThemes = { viewModel.openCommandPalette(">Preferences: Color Theme") },
                    onOpenCommandPalette = { viewModel.openCommandPalette(">") },
                    onOpenCloudSync = viewModel::openCloudModal
                )

                // On wide/tablet screens, Sidebar renders inline; on compact phones it overlays editor smoothly
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        if (state.sidebarVisible && !isCompact) {
                            SidebarPane(
                                theme = theme,
                                width = 260.dp,
                                activeView = state.activeView,
                                workspaceName = state.workspaceName,
                                nodes = state.nodes,
                                activeFileId = state.activeFile?.id,
                                openTabs = state.primaryTabs,
                                dirtyFileIds = state.dirtyFileIds,
                                modifiedFileIds = state.modifiedFileIds,
                                untrackedFileIds = state.untrackedFileIds,
                                gitBranch = state.gitBranch,
                                gitChanges = state.gitChanges,
                                stagedIds = state.stagedIds,
                                commits = state.commits,
                                breakpoints = state.breakpoints,
                                extensions = state.extensions,
                                inlineCreate = state.inlineCreate,
                                onSetInlineCreate = viewModel::setInlineCreate,
                                onSelectFile = { fid, ln -> viewModel.openFile(fid, ln) },
                                onCloseTab = { tid -> viewModel.closeTab(tid, "primary") },
                                onToggleFolder = viewModel::toggleFolder,
                                onCollapseAllFolders = viewModel::collapseAllFolders,
                                onCreateNode = viewModel::createNode,
                                onRenameNode = viewModel::renameNode,
                                onDeleteNode = viewModel::deleteNode,
                                onDuplicateFile = viewModel::duplicateFile,
                                onExportFile = { fid -> triggerExportFile(fid) },
                                onOpenLocalFolder = { openFolderLauncher.launch(null) },
                                onReplaceAllInWorkspace = viewModel::replaceAllInWorkspace,
                                onToggleStage = viewModel::toggleStage,
                                onStageAll = viewModel::stageAll,
                                onDiscardChange = viewModel::discardChange,
                                onCommit = viewModel::commit,
                                onOpenDiff = viewModel::openDiff,
                                onRunActiveFile = { viewModel.runActiveFile() },
                                onOpenPreview = { viewModel.openPreview() },
                                onRemoveBreakpoint = viewModel::removeBreakpoint,
                                onToggleInstallExtension = viewModel::toggleInstallExtension,
                                onToggleEnableExtension = viewModel::toggleEnableExtension,
                                onOpenCommandPalette = { viewModel.openCommandPalette(">") }
                            )
                        }

                        // Editor + Bottom Panel Stack
                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            if (!(state.panelVisible && state.panelMaximized)) {
                                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    EditorGroupPane(
                                        groupId = "primary",
                                        theme = theme,
                                        settings = state.settings,
                                        tabs = state.primaryTabs,
                                        activeTabId = state.activePrimaryTabId,
                                        nodes = state.nodes,
                                        dirtyFileIds = state.dirtyFileIds,
                                        committedSnapshot = state.committedSnapshot,
                                        breakpoints = state.breakpoints,
                                        jumpToLine = state.jumpToLine,
                                        findTriggerNonce = state.findTriggerNonce,
                                        isFocused = state.focusedGroup == "primary",
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        onFocusGroup = { viewModel.focusGroup("primary") },
                                        onSelectTab = { tid -> viewModel.selectTab(tid, "primary") },
                                        onCloseTab = { tid -> viewModel.closeTab(tid, "primary") },
                                        onChangeContent = viewModel::updateFileContent,
                                        onSaveFile = { fid -> viewModel.saveFile(fid) },
                                        onCursorChange = viewModel::updateCursor,
                                        onToggleBreakpoint = viewModel::toggleBreakpoint,
                                        onUpdateSettings = viewModel::updateSettings,
                                        onResetWorkspace = viewModel::resetWorkspace,
                                        onOpenFile = { fid, ln -> viewModel.openFile(fid, ln, "primary") },
                                        onNewFile = {
                                            viewModel.showActivityView(ActivityView.EXPLORER)
                                            viewModel.setInlineCreate(InlineCreateState(null, FSNodeType.FILE))
                                        },
                                        onOpenLocalFolder = { openFolderLauncher.launch(null) },
                                        onOpenQuickOpen = { viewModel.openCommandPalette("") },
                                        onOpenTerminal = { viewModel.selectBottomTab(BottomTab.TERMINAL) },
                                        onOpenThemes = { viewModel.openCommandPalette(">Preferences: Color Theme") },
                                        onOpenCloudSync = viewModel::openCloudModal,
                                        onRunActiveFile = { fid -> viewModel.runActiveFile(fid) },
                                        onOpenPreview = { fid -> viewModel.openPreview(fid) },
                                        onSplitEditor = viewModel::toggleSplit,
                                        onFormatDocument = { fid -> viewModel.formatDocument(fid) },
                                        onExportFile = { fid -> triggerExportFile(fid) }
                                    )

                                    if (state.splitVisible) {
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .fillMaxHeight()
                                                .background(c.tabBorder)
                                        )
                                        EditorGroupPane(
                                            groupId = "secondary",
                                            theme = theme,
                                            settings = state.settings,
                                            tabs = state.secondaryTabs,
                                            activeTabId = state.activeSecondaryTabId,
                                            nodes = state.nodes,
                                            dirtyFileIds = state.dirtyFileIds,
                                            committedSnapshot = state.committedSnapshot,
                                            breakpoints = state.breakpoints,
                                            jumpToLine = state.jumpToLine,
                                            findTriggerNonce = state.findTriggerNonce,
                                            isFocused = state.focusedGroup == "secondary",
                                            modifier = Modifier.weight(1f).fillMaxHeight(),
                                            onFocusGroup = { viewModel.focusGroup("secondary") },
                                            onSelectTab = { tid -> viewModel.selectTab(tid, "secondary") },
                                            onCloseTab = { tid -> viewModel.closeTab(tid, "secondary") },
                                            onChangeContent = viewModel::updateFileContent,
                                            onSaveFile = { fid -> viewModel.saveFile(fid) },
                                            onCursorChange = viewModel::updateCursor,
                                            onToggleBreakpoint = viewModel::toggleBreakpoint,
                                            onUpdateSettings = viewModel::updateSettings,
                                            onResetWorkspace = viewModel::resetWorkspace,
                                            onOpenFile = { fid, ln -> viewModel.openFile(fid, ln, "secondary") },
                                            onNewFile = {
                                                viewModel.showActivityView(ActivityView.EXPLORER)
                                                viewModel.setInlineCreate(InlineCreateState(null, FSNodeType.FILE))
                                            },
                                            onOpenLocalFolder = { openFolderLauncher.launch(null) },
                                            onOpenQuickOpen = { viewModel.openCommandPalette("") },
                                            onOpenTerminal = { viewModel.selectBottomTab(BottomTab.TERMINAL) },
                                            onOpenThemes = { viewModel.openCommandPalette(">Preferences: Color Theme") },
                                            onOpenCloudSync = viewModel::openCloudModal,
                                            onRunActiveFile = { fid -> viewModel.runActiveFile(fid) },
                                            onOpenPreview = { fid -> viewModel.openPreview(fid) },
                                            onSplitEditor = viewModel::toggleSplit,
                                            onFormatDocument = { fid -> viewModel.formatDocument(fid) },
                                            onExportFile = { fid -> triggerExportFile(fid) }
                                        )
                                    }
                                }
                            }

                            if (state.panelVisible) {
                                BottomPanel(
                                    theme = theme,
                                    maximized = state.panelMaximized,
                                    activeTab = state.activeBottomTab,
                                    problems = state.diagnostics,
                                    outputLogs = state.outputLogs,
                                    debugLogs = state.debugLogs,
                                    terminals = state.terminals,
                                    activeTerminalId = state.activeTerminalId,
                                    isCompact = isCompact,
                                    modifier = if (state.panelMaximized) {
                                        Modifier.weight(1f).fillMaxWidth()
                                    } else {
                                        Modifier.height(if (isCompact) 210.dp else 230.dp).fillMaxWidth()
                                    },
                                    onSelectTab = viewModel::selectBottomTab,
                                    onToggleMaximize = viewModel::togglePanelMaximized,
                                    onClose = { viewModel.setPanelVisible(false) },
                                    onJumpToProblem = { fid, ln -> viewModel.openFile(fid, ln) },
                                    onRunTerminalCommand = viewModel::runTerminalCommand,
                                    onAddTerminal = viewModel::addTerminal,
                                    onSelectTerminal = viewModel::selectTerminal,
                                    onDeleteTerminal = viewModel::deleteTerminal,
                                    onEvalDebugExpr = viewModel::evalDebugExpr
                                )
                            }
                        }
                    }

                    // Compact phone overlay drawer for Sidebar
                    if (state.sidebarVisible && isCompact) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            SidebarPane(
                                theme = theme,
                                width = 264.dp,
                                activeView = state.activeView,
                                workspaceName = state.workspaceName,
                                nodes = state.nodes,
                                activeFileId = state.activeFile?.id,
                                openTabs = state.primaryTabs,
                                dirtyFileIds = state.dirtyFileIds,
                                modifiedFileIds = state.modifiedFileIds,
                                untrackedFileIds = state.untrackedFileIds,
                                gitBranch = state.gitBranch,
                                gitChanges = state.gitChanges,
                                stagedIds = state.stagedIds,
                                commits = state.commits,
                                breakpoints = state.breakpoints,
                                extensions = state.extensions,
                                inlineCreate = state.inlineCreate,
                                onSetInlineCreate = viewModel::setInlineCreate,
                                onSelectFile = { fid, ln ->
                                    viewModel.openFile(fid, ln)
                                    viewModel.setSidebarVisible(false)
                                },
                                onCloseTab = { tid -> viewModel.closeTab(tid, "primary") },
                                onToggleFolder = viewModel::toggleFolder,
                                onCollapseAllFolders = viewModel::collapseAllFolders,
                                onCreateNode = viewModel::createNode,
                                onRenameNode = viewModel::renameNode,
                                onDeleteNode = viewModel::deleteNode,
                                onDuplicateFile = viewModel::duplicateFile,
                                onExportFile = { fid -> triggerExportFile(fid) },
                                onOpenLocalFolder = { openFolderLauncher.launch(null) },
                                onReplaceAllInWorkspace = viewModel::replaceAllInWorkspace,
                                onToggleStage = viewModel::toggleStage,
                                onStageAll = viewModel::stageAll,
                                onDiscardChange = viewModel::discardChange,
                                onCommit = viewModel::commit,
                                onOpenDiff = { fid ->
                                    viewModel.openDiff(fid)
                                    viewModel.setSidebarVisible(false)
                                },
                                onRunActiveFile = { viewModel.runActiveFile() },
                                onOpenPreview = {
                                    viewModel.openPreview()
                                    viewModel.setSidebarVisible(false)
                                },
                                onRemoveBreakpoint = viewModel::removeBreakpoint,
                                onToggleInstallExtension = viewModel::toggleInstallExtension,
                                onToggleEnableExtension = viewModel::toggleEnableExtension,
                                onOpenCommandPalette = { viewModel.openCommandPalette(">") }
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(Color.Black.copy(alpha = 0.35f))
                                    .clickable { viewModel.setSidebarVisible(false) }
                            )
                        }
                    }
                }
            }

            StatusBar(
                theme = theme,
                branch = state.gitBranch,
                branches = state.branches,
                errorCount = state.errorCount,
                warningCount = state.warningCount,
                cursorLine = state.cursorPos.line,
                cursorCol = state.cursorPos.col,
                tabSize = state.settings.tabSize,
                language = state.activeFile?.language,
                prettierEnabled = state.prettierEnabled,
                notifications = state.notifications,
                isCompact = isCompact,
                onSwitchBranch = viewModel::switchBranch,
                onOpenProblems = { viewModel.selectBottomTab(BottomTab.PROBLEMS) },
                onFormatDocument = { viewModel.formatDocument() },
                onChangeLanguage = viewModel::changeActiveFileLanguage,
                onOpenSettings = { viewModel.openSpecialTab(EditorTab("settings", TabKind.SETTINGS, title = "Settings")) },
                onClearNotifications = viewModel::clearNotifications
            )
        }

        CommandPaletteModal(
            theme = theme,
            isOpen = state.paletteOpen,
            initialInput = state.paletteInitial,
            nodes = state.nodes,
            commands = commands,
            onClose = viewModel::closeCommandPalette,
            onSelectFile = { fid -> viewModel.openFile(fid) },
            onSelectTheme = viewModel::selectTheme
        )

        CloudAccountModal(
            theme = theme,
            isOpen = state.cloudModalOpen,
            session = state.cloudSession,
            syncing = state.cloudSyncing,
            authLoading = state.cloudAuthLoading,
            lastSyncedAt = state.lastSyncedAt,
            syncMode = state.syncMode,
            errorMsg = state.cloudError,
            infoMsg = state.cloudInfo,
            onClose = viewModel::closeCloudModal,
            onSignIn = viewModel::signInCloud,
            onSignUp = viewModel::signUpCloud,
            onSignOut = viewModel::signOutCloud,
            onPushWorkspace = viewModel::pushToCloud,
            onPullWorkspace = viewModel::pullFromCloud
        )
    }
}
