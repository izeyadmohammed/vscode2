package com.example.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class FileLanguage(val label: String) {
    @SerialName("typescript") TYPESCRIPT("TypeScript"),
    @SerialName("tsx") TSX("TypeScript React"),
    @SerialName("javascript") JAVASCRIPT("JavaScript"),
    @SerialName("jsx") JSX("JavaScript React"),
    @SerialName("html") HTML("HTML"),
    @SerialName("css") CSS("CSS"),
    @SerialName("json") JSON("JSON"),
    @SerialName("markdown") MARKDOWN("Markdown"),
    @SerialName("python") PYTHON("Python"),
    @SerialName("plaintext") PLAINTEXT("Plain Text");

    companion object {
        fun detect(filename: String): FileLanguage {
            val ext = filename.substringAfterLast('.', "").lowercase()
            return when (ext) {
                "ts" -> TYPESCRIPT
                "tsx" -> TSX
                "js", "mjs", "cjs" -> JAVASCRIPT
                "jsx" -> JSX
                "html", "htm", "svg" -> HTML
                "css", "scss" -> CSS
                "json" -> JSON
                "md", "markdown" -> MARKDOWN
                "py" -> PYTHON
                else -> PLAINTEXT
            }
        }
    }
}

@Serializable
enum class FSNodeType {
    @SerialName("file") FILE,
    @SerialName("folder") FOLDER
}

@Serializable
data class FSNode(
    val id: String,
    val name: String,
    val type: FSNodeType,
    val parentId: String? = null,
    val content: String? = null,
    val language: FileLanguage? = null,
    val isOpen: Boolean = false,
    val updatedAt: Long? = null
)

enum class ActivityView(val title: String) {
    EXPLORER("EXPLORER"),
    SEARCH("SEARCH"),
    GIT("SOURCE CONTROL"),
    DEBUG("RUN AND DEBUG"),
    EXTENSIONS("EXTENSIONS")
}

enum class BottomTab(val label: String) {
    PROBLEMS("PROBLEMS"),
    OUTPUT("OUTPUT"),
    DEBUG("DEBUG CONSOLE"),
    TERMINAL("TERMINAL")
}

@Serializable
enum class ThemeId(val id: String) {
    @SerialName("dark-modern") DARK_MODERN("dark-modern"),
    @SerialName("dark-plus") DARK_PLUS("dark-plus"),
    @SerialName("light-modern") LIGHT_MODERN("light-modern"),
    @SerialName("monokai") MONOKAI("monokai"),
    @SerialName("github-dark") GITHUB_DARK("github-dark"),
    @SerialName("dracula") DRACULA("dracula")
}

data class ThemeTokens(
    val keyword: Color,
    val control: Color,
    val string: Color,
    val number: Color,
    val comment: Color,
    val function: Color,
    val type: Color,
    val variable: Color,
    val operator: Color,
    val tag: Color,
    val attribute: Color,
    val property: Color,
    val punctuation: Color
)

data class ThemeColors(
    val titleBarBg: Color,
    val titleBarFg: Color,
    val titleBarBorder: Color,
    val activityBarBg: Color,
    val activityBarFg: Color,
    val activityBarInactiveFg: Color,
    val activityBarIndicator: Color,
    val activityBarBorder: Color,
    val sidebarBg: Color,
    val sidebarFg: Color,
    val sidebarBorder: Color,
    val sidebarSectionBg: Color,
    val editorBg: Color,
    val editorFg: Color,
    val editorGutterBg: Color,
    val editorLineNumberFg: Color,
    val editorActiveLineNumberFg: Color,
    val editorActiveLineBg: Color,
    val editorSelectionBg: Color,
    val tabBarBg: Color,
    val tabActiveBg: Color,
    val tabActiveFg: Color,
    val tabInactiveBg: Color,
    val tabInactiveFg: Color,
    val tabBorder: Color,
    val tabActiveBorderTop: Color,
    val panelBg: Color,
    val panelFg: Color,
    val panelBorder: Color,
    val statusBarBg: Color,
    val statusBarFg: Color,
    val statusBarBorder: Color,
    val inputBg: Color,
    val inputFg: Color,
    val inputBorder: Color,
    val focusBorder: Color,
    val listHoverBg: Color,
    val listActiveBg: Color,
    val listActiveFg: Color,
    val buttonBg: Color,
    val buttonFg: Color,
    val buttonHoverBg: Color,
    val dropdownBg: Color,
    val dropdownBorder: Color,
    val badgeBg: Color,
    val badgeFg: Color,
    val accent: Color
)

data class ThemeConfig(
    val id: ThemeId,
    val name: String,
    val isDark: Boolean,
    val colors: ThemeColors,
    val tokens: ThemeTokens
)

enum class TabKind {
    FILE, WELCOME, SETTINGS, DIFF, PREVIEW
}

data class EditorTab(
    val id: String,
    val kind: TabKind,
    val fileId: String? = null,
    val title: String
)

@Serializable
data class EditorSettings(
    val theme: ThemeId = ThemeId.DARK_MODERN,
    val fontSize: Int = 14,
    val tabSize: Int = 2,
    val wordWrap: Boolean = false,
    val minimap: Boolean = true,
    val lineNumbers: Boolean = true,
    val autoSave: Boolean = false,
    val formatOnSave: Boolean = false,
    val renderWhitespace: Boolean = false
)

@Serializable
data class GitCommit(
    val id: String,
    val hash: String,
    val message: String,
    val author: String,
    val timestamp: Long,
    val filesChanged: Int,
    val snapshot: Map<String, String>
)

enum class GitStatus {
    MODIFIED, UNTRACKED
}

data class GitChangeItem(
    val fileId: String,
    val path: String,
    val name: String,
    val status: GitStatus
)

@Serializable
data class ExtensionItem(
    val id: String,
    val name: String,
    val publisher: String,
    val description: String,
    val version: String,
    val downloads: String,
    val rating: Double,
    val category: String,
    val installed: Boolean,
    val enabled: Boolean,
    val iconColorHex: Long,
    val shortTag: String
)

enum class DiagnosticSeverity {
    ERROR, WARNING, INFO
}

data class DiagnosticItem(
    val id: String,
    val fileId: String,
    val filePath: String,
    val line: Int,
    val col: Int,
    val severity: DiagnosticSeverity,
    val message: String,
    val source: String
)

enum class NotificationType {
    INFO, WARNING, ERROR, SUCCESS
}

data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val message: String,
    val source: String,
    val timestamp: Long
)

enum class TerminalLineType {
    INPUT, OUTPUT, ERROR, INFO, SUCCESS
}

data class TerminalLine(
    val id: String,
    val type: TerminalLineType,
    val text: String
)

data class TerminalSession(
    val id: String,
    val name: String,
    val cwd: String = "/",
    val lines: List<TerminalLine> = emptyList(),
    val history: List<String> = emptyList()
)

enum class DebugLogType {
    LOG, ERROR, RESULT
}

data class DebugLogEntry(
    val id: String,
    val type: DebugLogType,
    val text: String
)

data class CommandItem(
    val id: String,
    val label: String,
    val category: String,
    val shortcut: String? = null,
    val action: () -> Unit
)

data class InlineCreateState(
    val parentId: String?,
    val type: FSNodeType
)
