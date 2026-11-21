package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CloudPullResult
import com.example.data.InitialWorkspace
import com.example.data.PersistedWorkspace
import com.example.data.SupabaseSession
import com.example.data.SupabaseSyncClient
import com.example.data.Themes
import com.example.data.WorkspaceRepository
import com.example.model.ActivityView
import com.example.model.BottomTab
import com.example.model.CommandItem
import com.example.model.DebugLogEntry
import com.example.model.DebugLogType
import com.example.model.DiagnosticItem
import com.example.model.DiagnosticSeverity
import com.example.model.EditorSettings
import com.example.model.EditorTab
import com.example.model.ExtensionItem
import com.example.model.FSNode
import com.example.model.FSNodeType
import com.example.model.FileLanguage
import com.example.model.GitChangeItem
import com.example.model.GitCommit
import com.example.model.GitStatus
import com.example.model.InlineCreateState
import com.example.model.NotificationItem
import com.example.model.NotificationType
import com.example.model.TabKind
import com.example.model.TerminalLine
import com.example.model.TerminalLineType
import com.example.model.TerminalSession
import com.example.model.ThemeConfig
import com.example.model.ThemeId
import com.example.utils.FsHelpers
import com.example.utils.JsSandboxRunner
import com.example.utils.Linter
import com.example.utils.SafFolderLoader
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class CursorPosition(
    val line: Int = 1,
    val col: Int = 1
)

data class JumpToLineEvent(
    val fileId: String,
    val line: Int,
    val nonce: Long
)

data class IdeUiState(
    val workspaceName: String = "workspace",
    val nodes: List<FSNode> = emptyList(),
    val settings: EditorSettings = EditorSettings(),
    val sidebarVisible: Boolean = true,
    val activeView: ActivityView = ActivityView.EXPLORER,
    val panelVisible: Boolean = false,
    val panelMaximized: Boolean = false,
    val activeBottomTab: BottomTab = BottomTab.TERMINAL,
    val primaryTabs: List<EditorTab> = listOf(EditorTab(id = "welcome", kind = TabKind.WELCOME, title = "Welcome")),
    val activePrimaryTabId: String? = "welcome",
    val secondaryTabs: List<EditorTab> = emptyList(),
    val activeSecondaryTabId: String? = null,
    val splitVisible: Boolean = false,
    val focusedGroup: String = "primary",
    val savedContents: Map<String, String> = emptyMap(),
    val gitBranch: String = "main",
    val branches: List<String> = listOf("main"),
    val committedSnapshot: Map<String, String> = emptyMap(),
    val stagedIds: Set<String> = emptySet(),
    val commits: List<GitCommit> = emptyList(),
    val navHistory: List<String> = emptyList(),
    val navIdx: Int = -1,
    val cursorPos: CursorPosition = CursorPosition(),
    val jumpToLine: JumpToLineEvent? = null,
    val findTriggerNonce: Int = 0,
    val breakpoints: Map<String, List<Int>> = emptyMap(),
    val inlineCreate: InlineCreateState? = null,
    val paletteOpen: Boolean = false,
    val paletteInitial: String = "",
    val extensions: List<ExtensionItem> = InitialWorkspace.initialExtensions,
    val outputLogs: List<String> = emptyList(),
    val debugLogs: List<DebugLogEntry> = emptyList(),
    val notifications: List<NotificationItem> = emptyList(),
    val terminals: List<TerminalSession> = listOf(TerminalSession(id = "term-1", name = "bash", cwd = "/")),
    val activeTerminalId: String = "term-1",
    val cloudModalOpen: Boolean = false,
    val cloudSession: SupabaseSession? = null,
    val cloudSyncing: Boolean = false,
    val cloudAuthLoading: Boolean = false,
    val lastSyncedAt: String? = null,
    val syncMode: String? = null,
    val cloudError: String? = null,
    val cloudInfo: String? = null
) {
    val theme: ThemeConfig get() = Themes.get(settings.theme)

    val prettierEnabled: Boolean
        get() = extensions.firstOrNull { it.id == "esbenp.prettier-vscode" }?.let { it.installed && it.enabled } == true

    val eslintEnabled: Boolean
        get() = extensions.firstOrNull { it.id == "dbaeumer.vscode-eslint" }?.let { it.installed && it.enabled } == true

    val dirtyFileIds: Set<String>
        get() {
            if (settings.autoSave) return emptySet()
            return nodes.asSequence()
                .filter { it.type == FSNodeType.FILE }
                .filter { n ->
                    val saved = savedContents[n.id]
                    saved != null && saved != (n.content ?: "")
                }
                .map { it.id }
                .toSet()
        }

    val gitChanges: List<GitChangeItem>
        get() = nodes.asSequence()
            .filter { it.type == FSNodeType.FILE }
            .mapNotNull { n ->
                val base = committedSnapshot[n.id]
                when {
                    base == null -> GitChangeItem(
                        fileId = n.id,
                        path = FsHelpers.getNodePath(nodes, n.id),
                        name = n.name,
                        status = GitStatus.UNTRACKED
                    )
                    base != (n.content ?: "") -> GitChangeItem(
                        fileId = n.id,
                        path = FsHelpers.getNodePath(nodes, n.id),
                        name = n.name,
                        status = GitStatus.MODIFIED
                    )
                    else -> null
                }
            }
            .toList()

    val modifiedFileIds: Set<String>
        get() = gitChanges.filter { it.status == GitStatus.MODIFIED }.map { it.fileId }.toSet()

    val untrackedFileIds: Set<String>
        get() = gitChanges.filter { it.status == GitStatus.UNTRACKED }.map { it.fileId }.toSet()

    val diagnostics: List<DiagnosticItem>
        get() = Linter.runWorkspaceDiagnostics(nodes, eslintEnabled)

    val errorCount: Int get() = diagnostics.count { it.severity == DiagnosticSeverity.ERROR }
    val warningCount: Int get() = diagnostics.count { it.severity == DiagnosticSeverity.WARNING }

    val activeTabId: String?
        get() = if (focusedGroup == "secondary" && splitVisible) activeSecondaryTabId else activePrimaryTabId

    val activeTab: EditorTab?
        get() {
            val list = if (focusedGroup == "secondary" && splitVisible) secondaryTabs else primaryTabs
            return list.firstOrNull { it.id == activeTabId }
        }

    val activeFile: FSNode?
        get() = activeTab?.fileId?.let { fid -> nodes.firstOrNull { it.id == fid } }

    val canGoBack: Boolean get() = navIdx > 0
    val canGoForward: Boolean get() = navIdx < navHistory.lastIndex
}

class IdeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WorkspaceRepository(application)
    private val _uiState = MutableStateFlow(IdeUiState())
    val uiState: StateFlow<IdeUiState> = _uiState.asStateFlow()

    private var persistJob: Job? = null
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    init {
        viewModelScope.launch {
            val loaded = repository.load()
            val snap = createSnapshot(loaded.nodes)
            _uiState.update {
                it.copy(
                    workspaceName = loaded.workspaceName,
                    nodes = loaded.nodes,
                    settings = loaded.settings,
                    commits = loaded.commits,
                    committedSnapshot = if (loaded.committedSnapshot.isNotEmpty()) loaded.committedSnapshot else snap,
                    savedContents = snap,
                    extensions = loaded.extensions.ifEmpty { InitialWorkspace.initialExtensions }
                )
            }
        }
    }

    private fun createSnapshot(nodes: List<FSNode>): Map<String, String> =
        nodes.filter { it.type == FSNodeType.FILE }
            .associate { it.id to (it.content ?: "") }

    private fun schedulePersist() {
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            delay(250)
            val s = _uiState.value
            repository.save(
                PersistedWorkspace(
                    workspaceName = s.workspaceName,
                    nodes = s.nodes,
                    settings = s.settings,
                    commits = s.commits,
                    committedSnapshot = s.committedSnapshot,
                    extensions = s.extensions
                )
            )
        }
    }

    private fun pushNotification(
        message: String,
        source: String = "Workbench",
        type: NotificationType = NotificationType.INFO
    ) {
        val item = NotificationItem(
            id = "notif-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(4)}",
            type = type,
            message = message,
            source = source,
            timestamp = System.currentTimeMillis()
        )
        _uiState.update { it.copy(notifications = (listOf(item) + it.notifications).take(12)) }
    }

    private fun appendOutput(msg: String) {
        val ts = timeFormat.format(Date())
        _uiState.update { it.copy(outputLogs = it.outputLogs + "[$ts] $msg") }
    }

    fun clearNotifications() {
        _uiState.update { it.copy(notifications = emptyList()) }
    }

    fun toggleSidebar() {
        _uiState.update { it.copy(sidebarVisible = !it.sidebarVisible) }
    }

    fun setSidebarVisible(visible: Boolean) {
        _uiState.update { it.copy(sidebarVisible = visible) }
    }

    fun selectActivityView(view: ActivityView) {
        _uiState.update { state ->
            if (state.sidebarVisible && state.activeView == view) {
                state.copy(sidebarVisible = false)
            } else {
                state.copy(sidebarVisible = true, activeView = view)
            }
        }
    }

    fun showActivityView(view: ActivityView) {
        _uiState.update { it.copy(sidebarVisible = true, activeView = view) }
    }

    fun togglePanel() {
        _uiState.update { it.copy(panelVisible = !it.panelVisible) }
    }

    fun setPanelVisible(visible: Boolean) {
        _uiState.update { it.copy(panelVisible = visible) }
    }

    fun togglePanelMaximized() {
        _uiState.update { it.copy(panelMaximized = !it.panelMaximized) }
    }

    fun selectBottomTab(tab: BottomTab) {
        _uiState.update { it.copy(panelVisible = true, activeBottomTab = tab) }
    }

    fun focusGroup(group: String) {
        _uiState.update { it.copy(focusedGroup = group) }
    }

    fun selectTab(tabId: String, group: String) {
        _uiState.update { state ->
            if (group == "secondary") {
                state.copy(activeSecondaryTabId = tabId, focusedGroup = "secondary")
            } else {
                state.copy(activePrimaryTabId = tabId, focusedGroup = "primary")
            }
        }
    }

    fun openFile(fileId: String, line: Int? = null, targetGroup: String? = null) {
        _uiState.update { state ->
            val file = state.nodes.firstOrNull { it.id == fileId && it.type == FSNodeType.FILE }
                ?: return@update state
            val group = targetGroup ?: if (state.splitVisible) state.focusedGroup else "primary"
            val newTab = EditorTab(id = file.id, kind = TabKind.FILE, fileId = file.id, title = file.name)

            val sliced = state.navHistory.take(state.navIdx + 1)
            val nextHistory = if (sliced.lastOrNull() == file.id) sliced else sliced + file.id
            val nextIdx = nextHistory.lastIndex

            val jump = line?.let { JumpToLineEvent(file.id, it, System.currentTimeMillis()) }

            if (group == "secondary") {
                val tabs = if (state.secondaryTabs.any { it.id == file.id }) state.secondaryTabs else state.secondaryTabs + newTab
                state.copy(
                    secondaryTabs = tabs,
                    activeSecondaryTabId = file.id,
                    focusedGroup = "secondary",
                    navHistory = nextHistory,
                    navIdx = nextIdx,
                    jumpToLine = jump ?: state.jumpToLine
                )
            } else {
                val tabs = if (state.primaryTabs.any { it.id == file.id }) state.primaryTabs else state.primaryTabs + newTab
                state.copy(
                    primaryTabs = tabs,
                    activePrimaryTabId = file.id,
                    focusedGroup = "primary",
                    navHistory = nextHistory,
                    navIdx = nextIdx,
                    jumpToLine = jump ?: state.jumpToLine
                )
            }
        }
    }

    fun openSpecialTab(tab: EditorTab, group: String = "primary") {
        _uiState.update { state ->
            if (group == "secondary") {
                val tabs = if (state.secondaryTabs.any { it.id == tab.id }) state.secondaryTabs else state.secondaryTabs + tab
                state.copy(
                    splitVisible = true,
                    secondaryTabs = tabs,
                    activeSecondaryTabId = tab.id,
                    focusedGroup = "secondary"
                )
            } else {
                val tabs = if (state.primaryTabs.any { it.id == tab.id }) state.primaryTabs else state.primaryTabs + tab
                state.copy(
                    primaryTabs = tabs,
                    activePrimaryTabId = tab.id,
                    focusedGroup = "primary"
                )
            }
        }
    }

    fun closeTab(tabId: String, group: String) {
        _uiState.update { state ->
            if (group == "primary") {
                val next = state.primaryTabs.filterNot { it.id == tabId }
                val nextActive = if (state.activePrimaryTabId == tabId) next.lastOrNull()?.id else state.activePrimaryTabId
                state.copy(primaryTabs = next, activePrimaryTabId = nextActive)
            } else {
                val next = state.secondaryTabs.filterNot { it.id == tabId }
                val nextActive = if (state.activeSecondaryTabId == tabId) next.lastOrNull()?.id else state.activeSecondaryTabId
                if (next.isEmpty()) {
                    state.copy(
                        secondaryTabs = emptyList(),
                        activeSecondaryTabId = null,
                        splitVisible = false,
                        focusedGroup = "primary"
                    )
                } else {
                    state.copy(secondaryTabs = next, activeSecondaryTabId = nextActive)
                }
            }
        }
    }

    fun toggleSplit() {
        val s = _uiState.value
        if (s.splitVisible) {
            _uiState.update { it.copy(splitVisible = false, focusedGroup = "primary") }
        } else if (s.activeFile != null) {
            val af = s.activeFile!!
            openSpecialTab(EditorTab(id = af.id, kind = TabKind.FILE, fileId = af.id, title = af.name), "secondary")
        } else {
            openSpecialTab(EditorTab(id = "welcome", kind = TabKind.WELCOME, title = "Welcome"), "secondary")
        }
    }

    fun updateFileContent(fileId: String, content: String) {
        _uiState.update { state ->
            val updatedNodes = state.nodes.map {
                if (it.id == fileId) it.copy(content = content, updatedAt = System.currentTimeMillis()) else it
            }
            val updatedSaved = if (state.settings.autoSave) {
                state.savedContents + (fileId to content)
            } else {
                state.savedContents
            }
            state.copy(nodes = updatedNodes, savedContents = updatedSaved)
        }
        schedulePersist()
    }

    fun updateCursor(line: Int, col: Int) {
        _uiState.update { it.copy(cursorPos = CursorPosition(line, col)) }
    }

    fun triggerFindInEditor() {
        _uiState.update { it.copy(findTriggerNonce = it.findTriggerNonce + 1) }
    }

    fun saveFile(fileId: String? = null) {
        val state = _uiState.value
        val targetId = fileId ?: state.activeFile?.id ?: return
        val target = state.nodes.firstOrNull { it.id == targetId && it.type == FSNodeType.FILE } ?: return

        var contentToSave = target.content.orEmpty()
        if (state.settings.formatOnSave && state.prettierEnabled) {
            contentToSave = Linter.formatCode(contentToSave, target.language, state.settings.tabSize)
            _uiState.update { s ->
                s.copy(nodes = s.nodes.map { if (it.id == targetId) it.copy(content = contentToSave) else it })
            }
        }
        _uiState.update { s -> s.copy(savedContents = s.savedContents + (targetId to contentToSave)) }
        appendOutput("Saved ${FsHelpers.getNodePath(_uiState.value.nodes, targetId)}")
        pushNotification("Saved ${target.name}", "File", NotificationType.SUCCESS)
        schedulePersist()
    }

    fun saveAll() {
        val nextSaved = createSnapshot(_uiState.value.nodes)
        _uiState.update { it.copy(savedContents = nextSaved) }
        appendOutput("Saved all open workspace files.")
        pushNotification("Saved all workspace files.", "File", NotificationType.SUCCESS)
        schedulePersist()
    }

    fun formatDocument(fileId: String? = null) {
        val state = _uiState.value
        val targetId = fileId ?: state.activeFile?.id ?: return
        val target = state.nodes.firstOrNull { it.id == targetId && it.type == FSNodeType.FILE } ?: return
        val formatted = Linter.formatCode(target.content.orEmpty(), target.language, state.settings.tabSize)
        _uiState.update { s ->
            s.copy(nodes = s.nodes.map { if (it.id == targetId) it.copy(content = formatted) else it })
        }
        appendOutput("[Prettier] Formatted ${target.name}")
        schedulePersist()
    }

    fun setInlineCreate(valState: InlineCreateState?) {
        _uiState.update { it.copy(inlineCreate = valState) }
    }

    fun createNode(parentId: String?, type: FSNodeType, name: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        val id = "${if (type == FSNodeType.FILE) "file" else "folder"}-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(4)}"
        val language = if (type == FSNodeType.FILE) FileLanguage.detect(cleanName) else null
        val newNode = FSNode(
            id = id,
            name = cleanName,
            type = type,
            parentId = parentId,
            isOpen = type == FSNodeType.FOLDER,
            language = language,
            content = if (type == FSNodeType.FILE) "" else null
        )

        _uiState.update { state ->
            val updatedNodes = state.nodes.map {
                if (it.id == parentId) it.copy(isOpen = true) else it
            } + newNode
            if (type == FSNodeType.FILE) {
                val tab = EditorTab(id = id, kind = TabKind.FILE, fileId = id, title = cleanName)
                state.copy(
                    nodes = updatedNodes,
                    savedContents = state.savedContents + (id to ""),
                    primaryTabs = state.primaryTabs + tab,
                    activePrimaryTabId = id,
                    focusedGroup = "primary",
                    inlineCreate = null
                )
            } else {
                state.copy(nodes = updatedNodes, inlineCreate = null)
            }
        }
        appendOutput("Created ${if (type == FSNodeType.FILE) "file" else "folder"}: $cleanName")
        schedulePersist()
    }

    fun renameNode(nodeId: String, newName: String) {
        val cleanName = newName.trim()
        if (cleanName.isEmpty()) return
        _uiState.update { state ->
            state.copy(
                nodes = state.nodes.map { n ->
                    if (n.id == nodeId) {
                        n.copy(
                            name = cleanName,
                            language = if (n.type == FSNodeType.FILE) FileLanguage.detect(cleanName) else n.language
                        )
                    } else n
                },
                primaryTabs = state.primaryTabs.map { t -> if (t.fileId == nodeId) t.copy(title = cleanName) else t },
                secondaryTabs = state.secondaryTabs.map { t -> if (t.fileId == nodeId) t.copy(title = cleanName) else t }
            )
        }
        schedulePersist()
    }

    fun deleteNode(nodeId: String) {
        val target = _uiState.value.nodes.firstOrNull { it.id == nodeId } ?: return
        val toRemove = (listOf(nodeId) + FsHelpers.getDescendantIds(_uiState.value.nodes, nodeId)).toSet()
        _uiState.update { state ->
            val nextPrimary = state.primaryTabs.filter { it.fileId == null || !toRemove.contains(it.fileId) }
            val nextSecondary = state.secondaryTabs.filter { it.fileId == null || !toRemove.contains(it.fileId) }
            val nextActivePrimary = if (state.activePrimaryTabId != null && nextPrimary.none { it.id == state.activePrimaryTabId }) {
                nextPrimary.lastOrNull()?.id
            } else state.activePrimaryTabId
            val nextActiveSecondary = if (state.activeSecondaryTabId != null && nextSecondary.none { it.id == state.activeSecondaryTabId }) {
                nextSecondary.lastOrNull()?.id
            } else state.activeSecondaryTabId
            state.copy(
                nodes = state.nodes.filterNot { toRemove.contains(it.id) },
                primaryTabs = nextPrimary,
                activePrimaryTabId = nextActivePrimary,
                secondaryTabs = nextSecondary,
                activeSecondaryTabId = nextActiveSecondary,
                splitVisible = state.splitVisible && nextSecondary.isNotEmpty()
            )
        }
        appendOutput("Deleted ${target.name}")
        schedulePersist()
    }

    fun duplicateFile(fileId: String) {
        val orig = _uiState.value.nodes.firstOrNull { it.id == fileId && it.type == FSNodeType.FILE } ?: return
        val dotIdx = orig.name.lastIndexOf('.')
        val copyName = if (dotIdx > 0) {
            "${orig.name.substring(0, dotIdx)}.copy${orig.name.substring(dotIdx)}"
        } else {
            "${orig.name}.copy"
        }
        val id = "file-${System.currentTimeMillis()}"
        val newNode = orig.copy(id = id, name = copyName)
        val tab = EditorTab(id = id, kind = TabKind.FILE, fileId = id, title = copyName)
        _uiState.update { state ->
            state.copy(
                nodes = state.nodes + newNode,
                savedContents = state.savedContents + (id to (orig.content ?: "")),
                primaryTabs = state.primaryTabs + tab,
                activePrimaryTabId = id
            )
        }
        schedulePersist()
    }

    fun moveNode(nodeId: String, targetFolderId: String?) {
        if (nodeId == targetFolderId) return
        val descendants = FsHelpers.getDescendantIds(_uiState.value.nodes, nodeId).toSet()
        if (targetFolderId != null && descendants.contains(targetFolderId)) return
        _uiState.update { state ->
            state.copy(
                nodes = state.nodes.map { n ->
                    when {
                        n.id == nodeId -> n.copy(parentId = targetFolderId)
                        targetFolderId != null && n.id == targetFolderId -> n.copy(isOpen = true)
                        else -> n
                    }
                }
            )
        }
        schedulePersist()
    }

    fun toggleFolder(folderId: String) {
        _uiState.update { state ->
            state.copy(nodes = state.nodes.map { if (it.id == folderId) it.copy(isOpen = !it.isOpen) else it })
        }
        schedulePersist()
    }

    fun collapseAllFolders() {
        _uiState.update { state ->
            state.copy(nodes = state.nodes.map { if (it.type == FSNodeType.FOLDER) it.copy(isOpen = false) else it })
        }
    }

    fun replaceAllInWorkspace(query: String, replacement: String, matchCase: Boolean): Int {
        if (query.isEmpty()) return 0
        var total = 0
        val regex = Regex(Regex.escape(query), if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE))
        _uiState.update { state ->
            state.copy(
                nodes = state.nodes.map { n ->
                    if (n.type != FSNodeType.FILE || n.content == null) return@map n
                    val count = regex.findAll(n.content).count()
                    if (count == 0) return@map n
                    total += count
                    n.copy(content = n.content.replace(regex, replacement))
                }
            )
        }
        if (total > 0) schedulePersist()
        return total
    }

    fun toggleStage(fileId: String) {
        _uiState.update { state ->
            val next = state.stagedIds.toMutableSet()
            if (!next.add(fileId)) next.remove(fileId)
            state.copy(stagedIds = next)
        }
    }

    fun stageAll() {
        _uiState.update { state ->
            state.copy(stagedIds = state.gitChanges.map { it.fileId }.toSet())
        }
    }

    fun discardChange(fileId: String) {
        val orig = _uiState.value.committedSnapshot[fileId]
        if (orig == null) {
            deleteNode(fileId)
        } else {
            _uiState.update { state ->
                state.copy(
                    nodes = state.nodes.map { if (it.id == fileId) it.copy(content = orig) else it },
                    savedContents = state.savedContents + (fileId to orig)
                )
            }
            schedulePersist()
        }
    }

    fun commit(message: String) {
        val state = _uiState.value
        val changesCount = state.gitChanges.size
        if (message.isBlank() || changesCount == 0) return
        val snap = createSnapshot(state.nodes)
        val hash = UUID.randomUUID().toString().replace("-", "").take(7)
        val newCommit = GitCommit(
            id = "c-${System.currentTimeMillis()}",
            hash = hash,
            message = message.trim(),
            author = "Developer",
            timestamp = System.currentTimeMillis(),
            filesChanged = changesCount,
            snapshot = snap
        )
        _uiState.update {
            it.copy(
                committedSnapshot = snap,
                savedContents = snap,
                stagedIds = emptySet(),
                commits = listOf(newCommit) + it.commits
            )
        }
        pushNotification("Committed \"$message\" ($hash)", "Git", NotificationType.SUCCESS)
        appendOutput("[Git] Committed $hash: $message")
        schedulePersist()
    }

    fun openDiff(fileId: String) {
        val file = _uiState.value.nodes.firstOrNull { it.id == fileId } ?: return
        openSpecialTab(
            EditorTab(
                id = "diff:$fileId",
                kind = TabKind.DIFF,
                fileId = fileId,
                title = "${file.name} (Working Tree)"
            )
        )
    }

    fun openPreview(fileId: String? = null) {
        val target = fileId?.let { id -> _uiState.value.nodes.firstOrNull { it.id == id } }
            ?: _uiState.value.activeFile
            ?: return
        openSpecialTab(
            EditorTab(
                id = "preview:${target.id}",
                kind = TabKind.PREVIEW,
                fileId = target.id,
                title = "Preview ${target.name}"
            ),
            group = "secondary"
        )
    }

    fun runActiveFile(explicitFileId: String? = null) {
        val state = _uiState.value
        val target = if (explicitFileId != null) {
            state.nodes.firstOrNull { it.id == explicitFileId }
        } else {
            state.activeFile?.takeIf {
                it.language in setOf(
                    FileLanguage.JAVASCRIPT,
                    FileLanguage.TYPESCRIPT,
                    FileLanguage.HTML,
                    FileLanguage.MARKDOWN
                )
            }
        }
        if (target == null) {
            pushNotification("Open a JavaScript, TypeScript, HTML, or Markdown file to run it.", "Run", NotificationType.INFO)
            return
        }
        if (target.language == FileLanguage.HTML || target.language == FileLanguage.MARKDOWN) {
            openPreview(target.id)
            return
        }
        val path = FsHelpers.getNodePath(state.nodes, target.id)
        val header = DebugLogEntry(
            id = "run-${System.currentTimeMillis()}",
            type = DebugLogType.RESULT,
            text = "▶ Running $path..."
        )
        _uiState.update {
            it.copy(
                panelVisible = true,
                activeBottomTab = BottomTab.DEBUG,
                debugLogs = it.debugLogs + header
            )
        }
        viewModelScope.launch {
            val logs = JsSandboxRunner.executeScript(getApplication(), target.content.orEmpty())
            _uiState.update { it.copy(debugLogs = it.debugLogs + logs) }
        }
    }

    fun evalDebugExpr(expr: String) {
        val trimmed = expr.trim()
        if (trimmed.isEmpty()) return
        val inputEntry = DebugLogEntry(
            id = "d-in-${System.currentTimeMillis()}",
            type = DebugLogType.LOG,
            text = "> $trimmed"
        )
        _uiState.update { it.copy(debugLogs = it.debugLogs + inputEntry) }
        viewModelScope.launch {
            val outEntry = JsSandboxRunner.evaluateExpression(getApplication(), trimmed)
            _uiState.update { it.copy(debugLogs = it.debugLogs + outEntry) }
        }
    }

    fun toggleBreakpoint(fileId: String, line: Int) {
        _uiState.update { state ->
            val current = state.breakpoints[fileId] ?: emptyList()
            val next = if (current.contains(line)) current - line else (current + line).sorted()
            state.copy(breakpoints = state.breakpoints + (fileId to next))
        }
    }

    fun removeBreakpoint(fileId: String, line: Int) {
        _uiState.update { state ->
            val next = (state.breakpoints[fileId] ?: emptyList()).filterNot { it == line }
            state.copy(breakpoints = state.breakpoints + (fileId to next))
        }
    }

    fun addTerminal() {
        val count = _uiState.value.terminals.size + 1
        val id = "term-${System.currentTimeMillis()}"
        val session = TerminalSession(
            id = id,
            name = "bash ($count)",
            cwd = "/",
            lines = listOf(TerminalLine("i-$id", TerminalLineType.INFO, "New terminal session ready."))
        )
        _uiState.update {
            it.copy(
                terminals = it.terminals + session,
                activeTerminalId = id,
                panelVisible = true,
                activeBottomTab = BottomTab.TERMINAL
            )
        }
    }

    fun selectTerminal(id: String) {
        _uiState.update { it.copy(activeTerminalId = id) }
    }

    fun deleteTerminal(id: String) {
        _uiState.update { state ->
            val next = state.terminals.filterNot { it.id == id }
            if (next.isEmpty()) return@update state
            val nextActive = if (state.activeTerminalId == id) next.first().id else state.activeTerminalId
            state.copy(terminals = next, activeTerminalId = nextActive)
        }
    }

    fun runTerminalCommand(sessionId: String, rawCmd: String) {
        val cmdLine = rawCmd.trim()
        val state = _uiState.value
        val session = state.terminals.firstOrNull { it.id == sessionId } ?: return
        val promptPrefix = "dev@vscode:~${if (session.cwd == "/") "" else session.cwd}$ $rawCmd"
        val outLines = mutableListOf(
            TerminalLine("in-${System.nanoTime()}", TerminalLineType.OUTPUT, promptPrefix)
        )

        if (cmdLine.isEmpty()) {
            _uiState.update { s ->
                s.copy(terminals = s.terminals.map { if (it.id == sessionId) it.copy(lines = it.lines + outLines) else it })
            }
            return
        }

        if (cmdLine == "clear") {
            _uiState.update { s ->
                s.copy(terminals = s.terminals.map { if (it.id == sessionId) it.copy(lines = emptyList(), history = it.history + cmdLine) else it })
            }
            return
        }

        val argRegex = Regex("""(?:[^\s"']+|"[^"]*"|'[^']*')+""")
        val parts = argRegex.findAll(cmdLine).map { it.value }.toList()
        val cmd = parts.firstOrNull() ?: ""
        val args = parts.drop(1).map { it.trim('"', '\'') }
        var nextCwd = session.cwd

        fun pushOut(text: String, type: TerminalLineType = TerminalLineType.OUTPUT) {
            outLines.add(TerminalLine("o-${System.nanoTime()}", type, text))
        }

        var asyncNodeFileContent: String? = null

        when (cmd) {
            "help" -> pushOut(
                listOf(
                    "Available commands:",
                    "  ls [path]        List directory contents",
                    "  cd <dir>         Change directory",
                    "  pwd              Print working directory",
                    "  cat <file>       Display file contents",
                    "  touch <file>     Create a new empty file",
                    "  mkdir <dir>      Create a new folder",
                    "  rm <file>        Delete a file",
                    "  code <file>      Open file in VS Code editor",
                    "  node <file.js>   Execute JavaScript file",
                    "  git status|log   Inspect repository status or commits",
                    "  npm test|dev     Run workspace scripts",
                    "  clear            Clear terminal screen"
                ).joinToString("\n"),
                TerminalLineType.INFO
            )
            "pwd" -> pushOut(if (session.cwd == "/") "/${state.workspaceName}" else "/${state.workspaceName}${session.cwd}")
            "whoami" -> pushOut("developer")
            "date" -> pushOut(Date().toString())
            "echo" -> pushOut(args.joinToString(" "))
            "ls" -> {
                val targetPath = args.firstOrNull()?.let { FsHelpers.normalizePath(it, session.cwd) } ?: session.cwd
                val clean = targetPath.trimStart('/')
                var parentId: String? = null
                var ok = true
                if (clean.isNotEmpty()) {
                    val folder = state.nodes.firstOrNull {
                        it.type == FSNodeType.FOLDER && FsHelpers.getNodePath(state.nodes, it.id) == clean
                    }
                    if (folder == null) {
                        pushOut("ls: cannot access '${args.firstOrNull()}': No such directory", TerminalLineType.ERROR)
                        ok = false
                    } else {
                        parentId = folder.id
                    }
                }
                if (ok) {
                    val entries = state.nodes
                        .filter { it.parentId == parentId }
                        .sortedWith(compareBy<FSNode> { it.type != FSNodeType.FOLDER }.thenBy { it.name })
                        .map { if (it.type == FSNodeType.FOLDER) "${it.name}/" else it.name }
                    pushOut(if (entries.isEmpty()) "(empty)" else entries.joinToString("   "))
                }
            }
            "cd" -> {
                val targetArg = args.firstOrNull()
                if (targetArg.isNullOrEmpty() || targetArg == "~" || targetArg == "/") {
                    nextCwd = "/"
                } else {
                    val targetPath = FsHelpers.normalizePath(targetArg, session.cwd)
                    val clean = targetPath.trimStart('/')
                    if (clean.isEmpty()) {
                        nextCwd = "/"
                    } else {
                        val folder = state.nodes.firstOrNull {
                            it.type == FSNodeType.FOLDER && FsHelpers.getNodePath(state.nodes, it.id) == clean
                        }
                        if (folder == null) {
                            pushOut("cd: $targetArg: No such directory", TerminalLineType.ERROR)
                        } else {
                            nextCwd = targetPath
                        }
                    }
                }
            }
            "cat" -> {
                val arg = args.firstOrNull()
                if (arg == null) {
                    pushOut("usage: cat <file>", TerminalLineType.ERROR)
                } else {
                    val file = FsHelpers.findNodeByPath(state.nodes, arg, session.cwd)
                    if (file == null || file.type != FSNodeType.FILE) {
                        pushOut("cat: $arg: No such file", TerminalLineType.ERROR)
                    } else {
                        pushOut(file.content.orEmpty())
                    }
                }
            }
            "code" -> {
                val arg = args.firstOrNull()
                if (arg == null) {
                    pushOut("usage: code <file>", TerminalLineType.ERROR)
                } else {
                    val file = FsHelpers.findNodeByPath(state.nodes, arg, session.cwd)
                    if (file == null || file.type != FSNodeType.FILE) {
                        pushOut("code: $arg: File not found", TerminalLineType.ERROR)
                    } else {
                        openFile(file.id)
                        pushOut("Opened ${file.name} in editor.", TerminalLineType.SUCCESS)
                    }
                }
            }
            "touch" -> {
                val arg = args.firstOrNull()
                if (arg == null) {
                    pushOut("usage: touch <filename>", TerminalLineType.ERROR)
                } else {
                    val targetPath = FsHelpers.normalizePath(arg, session.cwd).trimStart('/')
                    val segments = targetPath.split("/").toMutableList()
                    val name = segments.removeAt(segments.lastIndex)
                    val parentPath = segments.joinToString("/")
                    val pFolder = if (parentPath.isNotEmpty()) {
                        state.nodes.firstOrNull { it.type == FSNodeType.FOLDER && FsHelpers.getNodePath(state.nodes, it.id) == parentPath }
                    } else null
                    if (parentPath.isNotEmpty() && pFolder == null) {
                        pushOut("touch: directory '$parentPath' does not exist", TerminalLineType.ERROR)
                    } else {
                        createNode(pFolder?.id, FSNodeType.FILE, name)
                        pushOut("Created file $targetPath", TerminalLineType.SUCCESS)
                    }
                }
            }
            "mkdir" -> {
                val arg = args.firstOrNull()
                if (arg == null) {
                    pushOut("usage: mkdir <folder>", TerminalLineType.ERROR)
                } else {
                    val targetPath = FsHelpers.normalizePath(arg, session.cwd).trimStart('/')
                    val segments = targetPath.split("/").toMutableList()
                    val name = segments.removeAt(segments.lastIndex)
                    val parentPath = segments.joinToString("/")
                    val pFolder = if (parentPath.isNotEmpty()) {
                        state.nodes.firstOrNull { it.type == FSNodeType.FOLDER && FsHelpers.getNodePath(state.nodes, it.id) == parentPath }
                    } else null
                    if (parentPath.isNotEmpty() && pFolder == null) {
                        pushOut("mkdir: directory '$parentPath' does not exist", TerminalLineType.ERROR)
                    } else {
                        createNode(pFolder?.id, FSNodeType.FOLDER, name)
                        pushOut("Created folder $targetPath", TerminalLineType.SUCCESS)
                    }
                }
            }
            "rm" -> {
                val arg = args.firstOrNull()
                if (arg == null) {
                    pushOut("usage: rm <file>", TerminalLineType.ERROR)
                } else {
                    val target = FsHelpers.findNodeByPath(state.nodes, arg, session.cwd)
                    if (target == null) {
                        pushOut("rm: cannot remove '$arg': No such file", TerminalLineType.ERROR)
                    } else {
                        deleteNode(target.id)
                        pushOut("Removed $arg", TerminalLineType.SUCCESS)
                    }
                }
            }
            "node" -> {
                val arg = args.firstOrNull()
                if (arg == null) {
                    pushOut("usage: node <script.js>", TerminalLineType.ERROR)
                } else {
                    val file = FsHelpers.findNodeByPath(state.nodes, arg, session.cwd)
                    if (file == null || file.type != FSNodeType.FILE) {
                        pushOut("Error: Cannot find module '$arg'", TerminalLineType.ERROR)
                    } else {
                        asyncNodeFileContent = file.content.orEmpty()
                    }
                }
            }
            "git" -> {
                when (args.firstOrNull()) {
                    "status" -> {
                        val changes = state.gitChanges
                        if (changes.isEmpty()) {
                            pushOut("On branch ${state.gitBranch}\nnothing to commit, working tree clean", TerminalLineType.SUCCESS)
                        } else {
                            val summary = changes.joinToString("\n") { ch ->
                                "  ${if (ch.status == GitStatus.MODIFIED) "modified:" else "untracked:"}  ${ch.path}"
                            }
                            pushOut("On branch ${state.gitBranch}\nChanges:\n$summary", TerminalLineType.INFO)
                        }
                    }
                    "log" -> {
                        if (state.commits.isEmpty()) {
                            pushOut("No commits yet.")
                        } else {
                            pushOut(
                                state.commits.joinToString("\n") { cm ->
                                    "commit ${cm.hash} (${state.gitBranch})\nAuthor: ${cm.author}\n\n    ${cm.message}\n"
                                }
                            )
                        }
                    }
                    else -> pushOut("git: supported subcommands are 'status' and 'log'", TerminalLineType.INFO)
                }
            }
            "npm" -> {
                when {
                    args.firstOrNull() == "test" -> {
                        val pkgNode = state.nodes.firstOrNull { it.name == "package.json" && it.parentId == null }
                        var pkgName = state.workspaceName
                        pkgNode?.content?.let { raw ->
                            runCatching {
                                pkgName = Json.parseToJsonElement(raw).jsonObject["name"]?.jsonPrimitive?.content ?: pkgName
                            }
                        }
                        pushOut(
                            "> $pkgName test\n\n ✓ Diagnostics scan (${state.errorCount} errors, ${state.warningCount} warnings)\n",
                            if (state.errorCount > 0) TerminalLineType.ERROR else TerminalLineType.SUCCESS
                        )
                    }
                    args.getOrNull(0) == "run" && args.getOrNull(1) == "dev" -> {
                        pushOut("VITE ready\n➜  Local:   http://localhost:3000/", TerminalLineType.SUCCESS)
                    }
                    else -> pushOut("npm scripts: \"npm run dev\", \"npm test\"", TerminalLineType.INFO)
                }
            }
            else -> pushOut("bash: $cmd: command not found. Type \"help\" for available commands.", TerminalLineType.ERROR)
        }

        _uiState.update { s ->
            s.copy(
                terminals = s.terminals.map { t ->
                    if (t.id == sessionId) {
                        t.copy(
                            cwd = nextCwd,
                            lines = t.lines + outLines,
                            history = t.history + cmdLine
                        )
                    } else t
                }
            )
        }

        if (asyncNodeFileContent != null) {
            viewModelScope.launch {
                val results = JsSandboxRunner.executeScript(getApplication(), asyncNodeFileContent)
                val nodeLines = if (results.isEmpty()) {
                    listOf(TerminalLine("n-${System.nanoTime()}", TerminalLineType.OUTPUT, "(process exited with code 0)"))
                } else {
                    results.map { r ->
                        TerminalLine(
                            id = "n-${System.nanoTime()}",
                            type = if (r.type == DebugLogType.ERROR) TerminalLineType.ERROR else TerminalLineType.OUTPUT,
                            text = r.text
                        )
                    }
                }
                _uiState.update { s ->
                    s.copy(terminals = s.terminals.map { if (it.id == sessionId) it.copy(lines = it.lines + nodeLines) else it })
                }
            }
        }
    }

    fun toggleInstallExtension(id: String) {
        _uiState.update { state ->
            state.copy(
                extensions = state.extensions.map { e ->
                    if (e.id == id) e.copy(installed = !e.installed, enabled = !e.installed) else e
                }
            )
        }
        schedulePersist()
    }

    fun toggleEnableExtension(id: String) {
        _uiState.update { state ->
            state.copy(extensions = state.extensions.map { e -> if (e.id == id) e.copy(enabled = !e.enabled) else e })
        }
        schedulePersist()
    }

    fun updateSettings(patch: (EditorSettings) -> EditorSettings) {
        _uiState.update { it.copy(settings = patch(it.settings)) }
        schedulePersist()
    }

    fun quickToggleTheme() {
        _uiState.update { state ->
            val nextTheme = if (state.theme.isDark) ThemeId.LIGHT_MODERN else ThemeId.DARK_MODERN
            state.copy(settings = state.settings.copy(theme = nextTheme))
        }
        schedulePersist()
    }

    fun selectTheme(themeId: ThemeId) {
        _uiState.update { it.copy(settings = it.settings.copy(theme = themeId)) }
        schedulePersist()
    }

    fun switchBranch(branch: String) {
        _uiState.update { it.copy(gitBranch = branch) }
        pushNotification("Switched to branch '$branch'", "Git", NotificationType.INFO)
    }

    fun changeActiveFileLanguage(lang: FileLanguage) {
        val activeId = _uiState.value.activeFile?.id ?: return
        _uiState.update { state ->
            state.copy(nodes = state.nodes.map { if (it.id == activeId) it.copy(language = lang) else it })
        }
        schedulePersist()
    }

    fun navigateHistory(direction: Int) {
        _uiState.update { state ->
            val nextIdx = state.navIdx + direction
            if (nextIdx !in state.navHistory.indices) return@update state
            val targetId = state.navHistory[nextIdx]
            val exists = state.nodes.firstOrNull { it.id == targetId } ?: return@update state
            val tab = EditorTab(id = exists.id, kind = TabKind.FILE, fileId = exists.id, title = exists.name)
            val tabs = if (state.primaryTabs.any { it.id == exists.id }) state.primaryTabs else state.primaryTabs + tab
            state.copy(
                navIdx = nextIdx,
                primaryTabs = tabs,
                activePrimaryTabId = exists.id,
                focusedGroup = "primary"
            )
        }
    }

    fun resetWorkspace() {
        _uiState.update {
            it.copy(
                workspaceName = "workspace",
                nodes = emptyList(),
                committedSnapshot = emptyMap(),
                savedContents = emptyMap(),
                commits = emptyList(),
                stagedIds = emptySet(),
                primaryTabs = listOf(EditorTab(id = "welcome", kind = TabKind.WELCOME, title = "Welcome")),
                activePrimaryTabId = "welcome",
                secondaryTabs = emptyList(),
                activeSecondaryTabId = null,
                splitVisible = false,
                outputLogs = emptyList(),
                debugLogs = emptyList(),
                settings = EditorSettings()
            )
        }
        viewModelScope.launch { repository.clear() }
        pushNotification("Workspace cleared and reset to clean state.", "Workbench", NotificationType.INFO)
    }

    fun loadFromSafTree(treeUri: Uri) {
        viewModelScope.launch {
            val loaded = SafFolderLoader.loadFromTreeUri(getApplication(), treeUri)
            if (loaded.nodes.isEmpty()) {
                pushNotification("No supported text/code files found in selected folder.", "Explorer", NotificationType.WARNING)
                return@launch
            }
            val snap = createSnapshot(loaded.nodes)
            val firstFile = loaded.nodes.firstOrNull { it.type == FSNodeType.FILE }
            val tabs = if (firstFile != null) {
                listOf(EditorTab(id = firstFile.id, kind = TabKind.FILE, fileId = firstFile.id, title = firstFile.name))
            } else {
                listOf(EditorTab(id = "welcome", kind = TabKind.WELCOME, title = "Welcome"))
            }
            _uiState.update {
                it.copy(
                    workspaceName = loaded.workspaceName,
                    nodes = loaded.nodes,
                    savedContents = snap,
                    committedSnapshot = snap,
                    primaryTabs = tabs,
                    activePrimaryTabId = tabs.first().id,
                    splitVisible = false,
                    secondaryTabs = emptyList(),
                    activeSecondaryTabId = null
                )
            }
            pushNotification("Opened local folder \"${loaded.workspaceName}\" (${loaded.nodes.size} items)", "Explorer", NotificationType.SUCCESS)
            appendOutput("[Explorer] Loaded local project \"${loaded.workspaceName}\"")
            schedulePersist()
        }
    }

    fun exportFileToUri(fileId: String, targetUri: Uri) {
        val file = _uiState.value.nodes.firstOrNull { it.id == fileId && it.type == FSNodeType.FILE } ?: return
        viewModelScope.launch {
            val res = SafFolderLoader.writeTextToUri(getApplication(), targetUri, file.content.orEmpty())
            if (res.isSuccess) {
                pushNotification("Exported \"${file.name}\" to device storage.", "File", NotificationType.SUCCESS)
            } else {
                pushNotification("Failed to export \"${file.name}\".", "File", NotificationType.ERROR)
            }
        }
    }

    fun openCommandPalette(initial: String = "") {
        _uiState.update { it.copy(paletteOpen = true, paletteInitial = initial) }
    }

    fun closeCommandPalette() {
        _uiState.update { it.copy(paletteOpen = false) }
    }

    fun openCloudModal() {
        _uiState.update { it.copy(cloudModalOpen = true, cloudError = null, cloudInfo = null) }
    }

    fun closeCloudModal() {
        _uiState.update { it.copy(cloudModalOpen = false) }
    }

    fun signInCloud(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(cloudAuthLoading = true, cloudError = null, cloudInfo = null) }
            val res = SupabaseSyncClient.signInWithEmail(email.trim(), password)
            res.fold(
                onSuccess = { session ->
                    _uiState.update {
                        it.copy(
                            cloudAuthLoading = false,
                            cloudSession = session,
                            cloudInfo = "Signed in successfully to Supabase."
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(cloudAuthLoading = false, cloudError = err.message ?: "Authentication failed")
                    }
                }
            )
        }
    }

    fun signUpCloud(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(cloudAuthLoading = true, cloudError = null, cloudInfo = null) }
            val res = SupabaseSyncClient.signUpWithEmail(email.trim(), password)
            res.fold(
                onSuccess = { session ->
                    _uiState.update {
                        it.copy(
                            cloudAuthLoading = false,
                            cloudSession = session,
                            cloudInfo = if (session == null) {
                                "Account created! Check your email to confirm, or sign in if confirmation is disabled."
                            } else {
                                "Account created and signed in!"
                            }
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(cloudAuthLoading = false, cloudError = err.message ?: "Sign up failed")
                    }
                }
            )
        }
    }

    fun signOutCloud() {
        _uiState.update { it.copy(cloudSession = null, cloudError = null, cloudInfo = null) }
    }

    fun pushToCloud() {
        val state = _uiState.value
        val session = state.cloudSession ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(cloudSyncing = true, cloudError = null) }
            val res = SupabaseSyncClient.pushWorkspace(session, state.workspaceName, state.nodes, state.settings)
            res.fold(
                onSuccess = { pushRes ->
                    _uiState.update {
                        it.copy(
                            cloudSyncing = false,
                            lastSyncedAt = pushRes.updatedAt,
                            syncMode = pushRes.mode
                        )
                    }
                    pushNotification("Synced workspace \"${state.workspaceName}\" to Supabase (${pushRes.mode}).", "Supabase", NotificationType.SUCCESS)
                    appendOutput("[Supabase] Saved workspace to cloud (${pushRes.mode})")
                },
                onFailure = { err ->
                    _uiState.update { it.copy(cloudSyncing = false, cloudError = err.message ?: "Cloud sync failed") }
                    pushNotification(err.message ?: "Cloud sync failed", "Supabase", NotificationType.ERROR)
                }
            )
        }
    }

    fun pullFromCloud() {
        val state = _uiState.value
        val session = state.cloudSession ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(cloudSyncing = true, cloudError = null) }
            val res = SupabaseSyncClient.pullWorkspace(session, state.workspaceName)
            res.fold(
                onSuccess = { pullRes: CloudPullResult ->
                    if (pullRes.mode == "empty" || pullRes.files == null) {
                        _uiState.update { it.copy(cloudSyncing = false) }
                        pushNotification("No cloud workspace saved yet. Tap \"Save to Cloud\" first.", "Supabase", NotificationType.INFO)
                        return@fold
                    }
                    val snap = createSnapshot(pullRes.files)
                    val firstFile = pullRes.files.firstOrNull { it.type == FSNodeType.FILE }
                    _uiState.update { s ->
                        s.copy(
                            cloudSyncing = false,
                            nodes = pullRes.files,
                            settings = pullRes.settings ?: s.settings,
                            workspaceName = pullRes.name ?: s.workspaceName,
                            savedContents = snap,
                            committedSnapshot = snap,
                            lastSyncedAt = pullRes.updatedAt,
                            syncMode = pullRes.mode,
                            primaryTabs = if (firstFile != null) {
                                listOf(EditorTab(firstFile.id, TabKind.FILE, firstFile.id, firstFile.name))
                            } else s.primaryTabs,
                            activePrimaryTabId = firstFile?.id ?: s.activePrimaryTabId
                        )
                    }
                    pushNotification("Loaded workspace \"${pullRes.name ?: state.workspaceName}\" from Supabase.", "Supabase", NotificationType.SUCCESS)
                    appendOutput("[Supabase] Loaded workspace from cloud (${pullRes.mode})")
                    schedulePersist()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(cloudSyncing = false, cloudError = err.message ?: "Cloud load failed") }
                    pushNotification(err.message ?: "Cloud load failed", "Supabase", NotificationType.ERROR)
                }
            )
        }
    }

    fun buildCommands(
        onTriggerOpenLocalFolder: () -> Unit,
        onTriggerExportActiveFile: () -> Unit
    ): List<CommandItem> = listOf(
        CommandItem("theme-picker", "Color Theme", "Preferences") {},
        CommandItem("open-settings", "Open Settings (UI)", "Preferences", "Ctrl+,") {
            openSpecialTab(EditorTab("settings", TabKind.SETTINGS, title = "Settings"))
        },
        CommandItem("new-file", "New File", "File", "Ctrl+N") {
            showActivityView(ActivityView.EXPLORER)
            setInlineCreate(InlineCreateState(null, FSNodeType.FILE))
        },
        CommandItem("new-folder", "New Folder", "File") {
            showActivityView(ActivityView.EXPLORER)
            setInlineCreate(InlineCreateState(null, FSNodeType.FOLDER))
        },
        CommandItem("open-folder", "Open Local Folder...", "File") {
            onTriggerOpenLocalFolder()
        },
        CommandItem("save-file", "Save Active File", "File", "Ctrl+S") { saveFile() },
        CommandItem("save-all", "Save All Files", "File") { saveAll() },
        CommandItem("download-file", "Export Active File to Device", "File") {
            onTriggerExportActiveFile()
        },
        CommandItem("format-doc", "Format Document", "Format", "Shift+Alt+F") { formatDocument() },
        CommandItem("toggle-sidebar", "Toggle Primary Side Bar", "View", "Ctrl+B") { toggleSidebar() },
        CommandItem("toggle-terminal", "Toggle Terminal", "View", "Ctrl+`") {
            if (_uiState.value.panelVisible && _uiState.value.activeBottomTab == BottomTab.TERMINAL) {
                setPanelVisible(false)
            } else {
                selectBottomTab(BottomTab.TERMINAL)
            }
        },
        CommandItem("show-explorer", "Show Explorer", "View", "Ctrl+Shift+E") {
            showActivityView(ActivityView.EXPLORER)
        },
        CommandItem("show-search", "Show Search", "View", "Ctrl+Shift+F") {
            showActivityView(ActivityView.SEARCH)
        },
        CommandItem("show-git", "Show Source Control", "View", "Ctrl+Shift+G") {
            showActivityView(ActivityView.GIT)
        },
        CommandItem("show-debug", "Show Run and Debug", "View", "Ctrl+Shift+D") {
            showActivityView(ActivityView.DEBUG)
        },
        CommandItem("show-extensions", "Show Extensions", "View", "Ctrl+Shift+X") {
            showActivityView(ActivityView.EXTENSIONS)
        },
        CommandItem("show-debug-console", "Show Debug Console", "View") {
            selectBottomTab(BottomTab.DEBUG)
        },
        CommandItem("split-editor", "Split Editor", "View", "Ctrl+\\") { toggleSplit() },
        CommandItem("run-file", "Run Active File", "Run", "F5") { runActiveFile() }
    )
}
