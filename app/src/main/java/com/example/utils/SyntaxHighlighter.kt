package com.example.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.model.FileLanguage
import com.example.model.ThemeTokens

data class TokenSegment(
    val text: String,
    val color: Color? = null,
    val italic: Boolean = false,
    val bold: Boolean = false
)

object SyntaxHighlighter {
    private val jsKeywords = setOf(
        "const", "let", "var", "function", "class", "extends", "implements",
        "interface", "type", "enum", "async", "await", "new", "this", "super",
        "true", "false", "null", "undefined", "void", "typeof", "instanceof",
        "in", "of", "private", "public", "protected", "readonly", "static", "declare"
    )

    private val jsControl = setOf(
        "import", "from", "export", "default", "return", "if", "else", "for",
        "while", "do", "switch", "case", "break", "continue", "try", "catch",
        "finally", "throw", "as"
    )

    private val pyKeywords = setOf(
        "def", "class", "True", "False", "None", "and", "or", "not", "is", "in",
        "lambda", "global", "nonlocal", "pass", "self"
    )

    private val pyControl = setOf(
        "import", "from", "as", "return", "if", "elif", "else", "for", "while",
        "break", "continue", "try", "except", "finally", "raise", "with", "yield"
    )

    private val mdHeadingRegex = Regex("""^#{1,6}\s""")
    private val mdBulletRegex = Regex("""^(\s*[-*+]\s)(.*)$""")
    private val mdInlineRegex = Regex("""(`[^`]+`|\*\*[^*]+\*\*)""")
    private val jsonRegex = Regex("""("(?:\\.|[^"])*")(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d+)?""")
    private val cssRegex = Regex("""(/\*.*?\*/|@[a-z-]+|--[\w-]+|#[0-9a-fA-F]{3,8}\b|'[^']*'|"[^"]*"|\b\d+(?:\.\d+)?(?:px|rem|em|vh|vw|%|s|ms)?\b|[a-z-]+(?=\s*:)|[.#][\w-]+)""")
    private val cssHexRegex = Regex("""^#[0-9a-fA-F]{3,8}$""")
    private val htmlRegex = Regex("""(<!--[\s\S]*?-->|</?[a-zA-Z0-9-]+|\s+[a-zA-Z-:]+(?==)|"[^"]*"|'[^']*'|/?>)""")
    private val pyRegex = Regex("""(#.*$|f?"(?:\\.|[^"])*"|f?'(?:\\.|[^'])*'|\b\d+(?:\.\d+)?\b|@[a-zA-Z_]\w*|\b[A-Za-z_]\w*(?=\s*\()|\b[A-Z][A-Za-z0-9_]*\b|\b[a-zA-Z_]\w*\b)""")
    private val jsRegex = Regex("""(//.*$|/\*.*?\*/|"(?:\\.|[^"])*"|'(?:\\.|[^'])*'|`(?:\\.|[^`])*`|\b\d+(?:\.\d+)?\b|</?[A-Za-z][A-Za-z0-9.]*|\b[A-Za-z_${'$'}][\w${'$'}]*(?=\s*\()|\b[A-Z][A-Za-z0-9_${'$'}]*\b|\b[a-zA-Z_${'$'}][\w${'$'}]*\b)""")

    fun highlightDocument(
        text: String,
        language: FileLanguage,
        tokens: ThemeTokens,
        searchQuery: String = ""
    ): AnnotatedString {
        val builder = AnnotatedString.Builder()
        val lines = text.split("\n")
        val lowerQ = searchQuery.lowercase()

        lines.forEachIndexed { lineIdx, line ->
            val segments = tokenizeLine(line, language, tokens)
            for (seg in segments) {
                val baseStyle = SpanStyle(
                    color = seg.color ?: Color.Unspecified,
                    fontStyle = if (seg.italic) FontStyle.Italic else null,
                    fontWeight = if (seg.bold) FontWeight.SemiBold else null
                )
                if (lowerQ.isEmpty()) {
                    builder.pushStyle(baseStyle)
                    builder.append(seg.text)
                    builder.pop()
                } else {
                    val lowerText = seg.text.lowercase()
                    var cursor = 0
                    var matchIdx = lowerText.indexOf(lowerQ, cursor)
                    if (matchIdx == -1) {
                        builder.pushStyle(baseStyle)
                        builder.append(seg.text)
                        builder.pop()
                    } else {
                        while (matchIdx != -1) {
                            if (matchIdx > cursor) {
                                builder.pushStyle(baseStyle)
                                builder.append(seg.text.substring(cursor, matchIdx))
                                builder.pop()
                            }
                            builder.pushStyle(
                                baseStyle.copy(background = Color(0x59EAB308))
                            )
                            builder.append(seg.text.substring(matchIdx, matchIdx + searchQuery.length))
                            builder.pop()
                            cursor = matchIdx + searchQuery.length
                            matchIdx = lowerText.indexOf(lowerQ, cursor)
                        }
                        if (cursor < seg.text.length) {
                            builder.pushStyle(baseStyle)
                            builder.append(seg.text.substring(cursor))
                            builder.pop()
                        }
                    }
                }
            }
            if (lineIdx < lines.lastIndex) {
                builder.append("\n")
            }
        }
        return builder.toAnnotatedString()
    }

    fun tokenizeLine(
        line: String,
        language: FileLanguage,
        tokens: ThemeTokens
    ): List<TokenSegment> {
        if (line.isEmpty()) return listOf(TokenSegment(""))
        if (language == FileLanguage.PLAINTEXT) return listOf(TokenSegment(line))

        return when (language) {
            FileLanguage.MARKDOWN -> {
                if (mdHeadingRegex.containsMatchIn(line)) {
                    listOf(TokenSegment(line, color = tokens.keyword, bold = true))
                } else if (line.trimStart().startsWith("```")) {
                    listOf(TokenSegment(line, color = tokens.comment))
                } else {
                    val bulletMatch = mdBulletRegex.find(line)
                    if (bulletMatch != null) {
                        listOf(TokenSegment(bulletMatch.groupValues[1], color = tokens.control)) +
                            tokenizeInlineMarkdown(bulletMatch.groupValues[2], tokens)
                    } else {
                        tokenizeInlineMarkdown(line, tokens)
                    }
                }
            }
            FileLanguage.JSON -> tokenizeJsonLine(line, tokens)
            FileLanguage.CSS -> tokenizeCssLine(line, tokens)
            FileLanguage.HTML -> tokenizeHtmlLine(line, tokens)
            FileLanguage.PYTHON -> tokenizeCodeLine(line, isPython = true, tokens = tokens)
            else -> tokenizeCodeLine(line, isPython = false, tokens = tokens)
        }
    }

    private fun tokenizeInlineMarkdown(line: String, tokens: ThemeTokens): List<TokenSegment> {
        val segments = mutableListOf<TokenSegment>()
        var lastIndex = 0
        for (match in mdInlineRegex.findAll(line)) {
            if (match.range.first > lastIndex) {
                segments.add(TokenSegment(line.substring(lastIndex, match.range.first)))
            }
            val token = match.value
            if (token.startsWith("`")) {
                segments.add(TokenSegment(token, color = tokens.string))
            } else {
                segments.add(TokenSegment(token, color = tokens.function, bold = true))
            }
            lastIndex = match.range.last + 1
        }
        if (lastIndex < line.length) {
            segments.add(TokenSegment(line.substring(lastIndex)))
        }
        return segments
    }

    private fun tokenizeJsonLine(line: String, tokens: ThemeTokens): List<TokenSegment> {
        val segments = mutableListOf<TokenSegment>()
        var lastIndex = 0
        for (match in jsonRegex.findAll(line)) {
            if (match.range.first > lastIndex) {
                segments.add(TokenSegment(line.substring(lastIndex, match.range.first), color = tokens.punctuation))
            }
            val g1 = match.groupValues[1]
            val g2 = match.groupValues[2]
            val g3 = match.groupValues[3]
            if (g1.isNotEmpty()) {
                if (g2.isNotEmpty()) {
                    segments.add(TokenSegment(g1, color = tokens.property))
                    segments.add(TokenSegment(g2, color = tokens.punctuation))
                } else {
                    segments.add(TokenSegment(g1, color = tokens.string))
                }
            } else if (g3.isNotEmpty()) {
                segments.add(TokenSegment(g3, color = tokens.keyword))
            } else {
                segments.add(TokenSegment(match.value, color = tokens.number))
            }
            lastIndex = match.range.last + 1
        }
        if (lastIndex < line.length) {
            segments.add(TokenSegment(line.substring(lastIndex), color = tokens.punctuation))
        }
        return segments
    }

    private fun tokenizeCssLine(line: String, tokens: ThemeTokens): List<TokenSegment> {
        val trimmed = line.trim()
        if (trimmed.startsWith("/*") || trimmed.startsWith("*") || trimmed.startsWith("*/")) {
            return listOf(TokenSegment(line, color = tokens.comment, italic = true))
        }
        val segments = mutableListOf<TokenSegment>()
        var lastIndex = 0
        for (match in cssRegex.findAll(line)) {
            if (match.range.first > lastIndex) {
                segments.add(TokenSegment(line.substring(lastIndex, match.range.first)))
            }
            val tok = match.value
            val color = when {
                tok.startsWith("/*") -> tokens.comment
                tok.startsWith("@") -> tokens.control
                tok.startsWith("--") -> tokens.variable
                tok.startsWith("#") && cssHexRegex.matches(tok) -> tokens.string
                tok.startsWith("'") || tok.startsWith("\"") -> tokens.string
                tok[0].isDigit() -> tokens.number
                tok.startsWith(".") || tok.startsWith("#") -> tokens.function
                else -> tokens.property
            }
            segments.add(TokenSegment(tok, color = color, italic = tok.startsWith("/*")))
            lastIndex = match.range.last + 1
        }
        if (lastIndex < line.length) {
            segments.add(TokenSegment(line.substring(lastIndex)))
        }
        return segments
    }

    private fun tokenizeHtmlLine(line: String, tokens: ThemeTokens): List<TokenSegment> {
        val segments = mutableListOf<TokenSegment>()
        var lastIndex = 0
        for (match in htmlRegex.findAll(line)) {
            if (match.range.first > lastIndex) {
                segments.add(TokenSegment(line.substring(lastIndex, match.range.first)))
            }
            val tok = match.value
            val color = when {
                tok.startsWith("<!--") -> tokens.comment
                tok.startsWith("<") || tok == ">" || tok == "/>" -> tokens.tag
                tok.startsWith("\"") || tok.startsWith("'") -> tokens.string
                else -> tokens.attribute
            }
            segments.add(TokenSegment(tok, color = color, italic = tok.startsWith("<!--")))
            lastIndex = match.range.last + 1
        }
        if (lastIndex < line.length) {
            segments.add(TokenSegment(line.substring(lastIndex)))
        }
        return segments
    }

    private fun tokenizeCodeLine(
        line: String,
        isPython: Boolean,
        tokens: ThemeTokens
    ): List<TokenSegment> {
        val trimmed = line.trim()
        if ((!isPython && (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*"))) ||
            (isPython && trimmed.startsWith("#"))
        ) {
            return listOf(TokenSegment(line, color = tokens.comment, italic = true))
        }

        val kwSet = if (isPython) pyKeywords else jsKeywords
        val ctrlSet = if (isPython) pyControl else jsControl
        val regex = if (isPython) pyRegex else jsRegex

        val segments = mutableListOf<TokenSegment>()
        var lastIndex = 0
        for (match in regex.findAll(line)) {
            if (match.range.first > lastIndex) {
                segments.add(TokenSegment(line.substring(lastIndex, match.range.first)))
            }
            val tok = match.value
            val nextIdx = match.range.last + 1
            val isComment = tok.startsWith("//") || tok.startsWith("/*") || (isPython && tok.startsWith("#"))
            val color = when {
                isComment -> tokens.comment
                tok.startsWith("\"") || tok.startsWith("'") || tok.startsWith("`") ||
                    tok.startsWith("f\"") || tok.startsWith("f'") -> tokens.string
                tok[0].isDigit() -> tokens.number
                tok.startsWith("<") -> tokens.type
                tok.startsWith("@") -> tokens.control
                ctrlSet.contains(tok) -> tokens.control
                kwSet.contains(tok) -> tokens.keyword
                tok[0].isUpperCase() -> tokens.type
                nextIdx < line.length && line[nextIdx] == '(' -> tokens.function
                else -> tokens.variable
            }
            segments.add(TokenSegment(tok, color = color, italic = isComment))
            lastIndex = nextIdx
        }
        if (lastIndex < line.length) {
            segments.add(TokenSegment(line.substring(lastIndex)))
        }
        return segments
    }
}
