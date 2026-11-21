package com.example.utils

import com.example.model.FSNode
import com.example.model.FSNodeType
import com.example.model.FileLanguage

data class BreadcrumbPart(
    val id: String,
    val name: String,
    val type: FSNodeType
)

enum class OutlineSymbolKind {
    FUNCTION, INTERFACE, CONST, CLASS, HEADING, SELECTOR
}

data class OutlineSymbol(
    val name: String,
    val kind: OutlineSymbolKind,
    val line: Int
)

object FsHelpers {
    fun getNodePath(nodes: List<FSNode>, nodeId: String): String {
        val nodeMap = nodes.associateBy { it.id }
        val parts = mutableListOf<String>()
        var current = nodeMap[nodeId]
        val visited = mutableSetOf<String>()
        while (current != null && visited.add(current.id)) {
            parts.add(0, current.name)
            current = current.parentId?.let { nodeMap[it] }
        }
        return parts.joinToString("/")
    }

    fun getNodeBreadcrumbParts(nodes: List<FSNode>, nodeId: String): List<BreadcrumbPart> {
        val nodeMap = nodes.associateBy { it.id }
        val parts = mutableListOf<BreadcrumbPart>()
        var current = nodeMap[nodeId]
        val visited = mutableSetOf<String>()
        while (current != null && visited.add(current.id)) {
            parts.add(0, BreadcrumbPart(current.id, current.name, current.type))
            current = current.parentId?.let { nodeMap[it] }
        }
        return parts
    }

    fun normalizePath(target: String, cwd: String = "/"): String {
        val isAbs = target.startsWith("/")
        val baseParts = if (isAbs) emptyList() else cwd.split("/").filter { it.isNotEmpty() }
        val targetParts = target.split("/").filter { it.isNotEmpty() }
        val stack = baseParts.toMutableList()
        for (part in targetParts) {
            when (part) {
                "." -> continue
                ".." -> if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
                else -> stack.add(part)
            }
        }
        return "/" + stack.joinToString("/")
    }

    fun findNodeByPath(nodes: List<FSNode>, rawPath: String, cwd: String = "/"): FSNode? {
        val clean = normalizePath(rawPath, cwd).trimStart('/')
        if (clean.isEmpty()) return null
        return nodes.firstOrNull { getNodePath(nodes, it.id) == clean }
    }

    fun getDescendantIds(nodes: List<FSNode>, folderId: String): List<String> {
        val ids = mutableListOf<String>()
        val children = nodes.filter { it.parentId == folderId }
        for (child in children) {
            ids.add(child.id)
            if (child.type == FSNodeType.FOLDER) {
                ids.addAll(getDescendantIds(nodes, child.id))
            }
        }
        return ids
    }

    private val headingRegex = Regex("""^(#{1,4})\s+(.+)""")
    private val cssSelectorRegex = Regex("""^([.#a-zA-Z:][^{]+)\s*\{""")
    private val fnRegex = Regex("""^(?:export\s+(?:default\s+)?)?(?:async\s+)?(?:function|def)\s+([a-zA-Z_${'$'}][\w${'$'}]*)""")
    private val ifaceRegex = Regex("""^(?:export\s+)?(?:interface|type)\s+([a-zA-Z_${'$'}][\w${'$'}]*)""")
    private val classRegex = Regex("""^(?:export\s+)?class\s+([a-zA-Z_${'$'}][\w${'$'}]*)""")
    private val constRegex = Regex("""^(?:export\s+)?const\s+([a-zA-Z_${'$'}][\w${'$'}]*)\s*=""")

    fun extractOutlineSymbols(content: String, language: FileLanguage?): List<OutlineSymbol> {
        val symbols = mutableListOf<OutlineSymbol>()
        val lines = content.split("\n")
        lines.forEachIndexed { idx, line ->
            val trimmed = line.trim()
            if (language == FileLanguage.MARKDOWN) {
                headingRegex.find(trimmed)?.let { m ->
                    symbols.add(OutlineSymbol(m.groupValues[2], OutlineSymbolKind.HEADING, idx + 1))
                }
                return@forEachIndexed
            }
            if (language == FileLanguage.CSS) {
                cssSelectorRegex.find(trimmed)?.let { m ->
                    symbols.add(OutlineSymbol(m.groupValues[1].trim(), OutlineSymbolKind.SELECTOR, idx + 1))
                }
                return@forEachIndexed
            }
            fnRegex.find(trimmed)?.let { m ->
                symbols.add(OutlineSymbol(m.groupValues[1], OutlineSymbolKind.FUNCTION, idx + 1))
                return@forEachIndexed
            }
            ifaceRegex.find(trimmed)?.let { m ->
                symbols.add(OutlineSymbol(m.groupValues[1], OutlineSymbolKind.INTERFACE, idx + 1))
                return@forEachIndexed
            }
            classRegex.find(trimmed)?.let { m ->
                symbols.add(OutlineSymbol(m.groupValues[1], OutlineSymbolKind.CLASS, idx + 1))
                return@forEachIndexed
            }
            constRegex.find(trimmed)?.let { m ->
                symbols.add(OutlineSymbol(m.groupValues[1], OutlineSymbolKind.CONST, idx + 1))
            }
        }
        return symbols
    }
}
