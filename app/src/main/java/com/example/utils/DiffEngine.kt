package com.example.utils

enum class DiffLineType {
    UNCHANGED, ADDED, REMOVED
}

data class DiffLine(
    val type: DiffLineType,
    val originalLineNumber: Int?,
    val modifiedLineNumber: Int?,
    val text: String
)

object DiffEngine {
    fun computeDiff(original: String, modified: String): List<DiffLine> {
        val origLines = if (original.isEmpty()) emptyList() else original.split("\n")
        val modLines = if (modified.isEmpty()) emptyList() else modified.split("\n")

        val m = origLines.size
        val n = modLines.size

        // Guard against huge files by falling back to simple comparison
        if (m * n > 250_000) {
            val out = mutableListOf<DiffLine>()
            origLines.forEachIndexed { idx, line ->
                out.add(DiffLine(DiffLineType.REMOVED, idx + 1, null, line))
            }
            modLines.forEachIndexed { idx, line ->
                out.add(DiffLine(DiffLineType.ADDED, null, idx + 1, line))
            }
            return out
        }

        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in m - 1 downTo 0) {
            for (j in n - 1 downTo 0) {
                dp[i][j] = if (origLines[i] == modLines[j]) {
                    dp[i + 1][j + 1] + 1
                } else {
                    maxOf(dp[i + 1][j], dp[i][j + 1])
                }
            }
        }

        val result = mutableListOf<DiffLine>()
        var i = 0
        var j = 0
        while (i < m && j < n) {
            when {
                origLines[i] == modLines[j] -> {
                    result.add(DiffLine(DiffLineType.UNCHANGED, i + 1, j + 1, origLines[i]))
                    i++
                    j++
                }
                dp[i + 1][j] >= dp[i][j + 1] -> {
                    result.add(DiffLine(DiffLineType.REMOVED, i + 1, null, origLines[i]))
                    i++
                }
                else -> {
                    result.add(DiffLine(DiffLineType.ADDED, null, j + 1, modLines[j]))
                    j++
                }
            }
        }
        while (i < m) {
            result.add(DiffLine(DiffLineType.REMOVED, i + 1, null, origLines[i]))
            i++
        }
        while (j < n) {
            result.add(DiffLine(DiffLineType.ADDED, null, j + 1, modLines[j]))
            j++
        }
        return result
    }
}
