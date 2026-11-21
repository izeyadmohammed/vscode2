package com.example.utils

import com.example.model.DiagnosticItem
import com.example.model.DiagnosticSeverity
import com.example.model.FSNode
import com.example.model.FSNodeType
import com.example.model.FileLanguage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

object Linter {
    private val jsonParser = Json { isLenient = false }
    private val varRegex = Regex("""\bvar\s+""")
    private val todoRegex = Regex("""TODO|FIXME""")
    private val openBracketsRegex = Regex("""[{(\[]""")
    private val closeBracketsRegex = Regex("""[})\]]""")
    private val leadingCloseRegex = Regex("""^[}\])]""")

    fun runWorkspaceDiagnostics(
        nodes: List<FSNode>,
        eslintEnabled: Boolean
    ): List<DiagnosticItem> {
        val diagnostics = mutableListOf<DiagnosticItem>()

        for (node in nodes) {
            if (node.type != FSNodeType.FILE) continue
            val content = node.content.orEmpty()
            val filePath = FsHelpers.getNodePath(nodes, node.id)

            if (node.language == FileLanguage.JSON && content.isNotBlank()) {
                try {
                    jsonParser.decodeFromString(JsonElement.serializer(), content)
                } catch (err: Exception) {
                    diagnostics.add(
                        DiagnosticItem(
                            id = "${node.id}-json-err",
                            fileId = node.id,
                            filePath = filePath,
                            line = 1,
                            col = 1,
                            severity = DiagnosticSeverity.ERROR,
                            message = err.message ?: "Invalid JSON syntax",
                            source = "json"
                        )
                    )
                }
            }

            if (node.language in setOf(
                    FileLanguage.TYPESCRIPT,
                    FileLanguage.TSX,
                    FileLanguage.JAVASCRIPT,
                    FileLanguage.JSX
                )
            ) {
                var braceBalance = 0
                var parenBalance = 0
                val lines = content.split("\n")
                lines.forEachIndexed { idx, line ->
                    for (ch in line) {
                        when (ch) {
                            '{' -> braceBalance++
                            '}' -> braceBalance--
                            '(' -> parenBalance++
                            ')' -> parenBalance--
                        }
                    }

                    if (eslintEnabled) {
                        if (varRegex.containsMatchIn(line) && !line.trimStart().startsWith("//")) {
                            diagnostics.add(
                                DiagnosticItem(
                                    id = "${node.id}-var-$idx",
                                    fileId = node.id,
                                    filePath = filePath,
                                    line = idx + 1,
                                    col = line.indexOf("var") + 1,
                                    severity = DiagnosticSeverity.WARNING,
                                    message = "Unexpected 'var', use 'let' or 'const' instead. (no-var)",
                                    source = "eslint"
                                )
                            )
                        }
                        if (todoRegex.containsMatchIn(line)) {
                            diagnostics.add(
                                DiagnosticItem(
                                    id = "${node.id}-todo-$idx",
                                    fileId = node.id,
                                    filePath = filePath,
                                    line = idx + 1,
                                    col = 1,
                                    severity = DiagnosticSeverity.INFO,
                                    message = "Task marker detected in comment.",
                                    source = "workspace"
                                )
                            )
                        }
                    }
                }

                if (braceBalance != 0) {
                    diagnostics.add(
                        DiagnosticItem(
                            id = "${node.id}-brace",
                            fileId = node.id,
                            filePath = filePath,
                            line = lines.size,
                            col = 1,
                            severity = DiagnosticSeverity.ERROR,
                            message = if (braceBalance > 0) {
                                "'}' expected: unclosed block brace."
                            } else {
                                "Unexpected '}': extra closing brace."
                            },
                            source = "ts"
                        )
                    )
                }
                if (parenBalance != 0) {
                    diagnostics.add(
                        DiagnosticItem(
                            id = "${node.id}-paren",
                            fileId = node.id,
                            filePath = filePath,
                            line = lines.size,
                            col = 1,
                            severity = DiagnosticSeverity.ERROR,
                            message = if (parenBalance > 0) {
                                "')' expected: unclosed parenthesis."
                            } else {
                                "Unexpected ')': extra closing parenthesis."
                            },
                            source = "ts"
                        )
                    )
                }
            }
        }

        return diagnostics
    }

    fun formatCode(content: String, language: FileLanguage?, tabSize: Int = 2): String {
        if (language == FileLanguage.JSON) {
            return try {
                val prettyJson = Json {
                    prettyPrint = true
                    prettyPrintIndent = " ".repeat(tabSize)
                }
                val elem = Json.parseToJsonElement(content)
                prettyJson.encodeToString(JsonElement.serializer(), elem) + "\n"
            } catch (_: Exception) {
                content
            }
        }

        val indentUnit = " ".repeat(tabSize)
        val lines = content.split("\n")
        var level = 0
        val formatted = mutableListOf<String>()

        for (rawLine in lines) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) {
                formatted.add("")
                continue
            }

            val startsWithClose = leadingCloseRegex.containsMatchIn(trimmed)
            if (startsWithClose) {
                level = maxOf(0, level - 1)
            }

            formatted.add(indentUnit.repeat(level) + trimmed)

            val opens = openBracketsRegex.findAll(trimmed).count()
            val closes = closeBracketsRegex.findAll(trimmed).count()
            val net = opens - closes
            level = if (startsWithClose) {
                maxOf(0, level + net + 1)
            } else {
                maxOf(0, level + net)
            }
        }

        return formatted.joinToString("\n").trimEnd('\n') + "\n"
    }
}
