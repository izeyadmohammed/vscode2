package com.example.ui.components.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FileLanguage
import com.example.model.ThemeConfig
import com.example.utils.DiffEngine
import com.example.utils.DiffLineType
import com.example.utils.SyntaxHighlighter

@Composable
fun DiffViewer(
    theme: ThemeConfig,
    fileName: String,
    language: FileLanguage,
    originalContent: String,
    modifiedContent: String
) {
    val c = theme.colors
    val diffLines = remember(originalContent, modifiedContent) {
        DiffEngine.computeDiff(originalContent, modifiedContent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.editorBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(c.tabBarBg)
                .border(0.5.dp, c.tabBorder)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "HEAD: $fileName (Original)",
                color = c.editorFg.copy(alpha = 0.75f),
                fontSize = 11.sp
            )
            Text(
                text = "Working Tree: $fileName (Modified)",
                color = c.editorFg.copy(alpha = 0.75f),
                fontSize = 11.sp
            )
        }

        val hScroll = rememberScrollState()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(hScroll)
                .padding(vertical = 4.dp)
        ) {
            items(diffLines) { line ->
                val bgColor = when (line.type) {
                    DiffLineType.ADDED -> Color(0x284ADE80)
                    DiffLineType.REMOVED -> Color(0x28F87171)
                    DiffLineType.UNCHANGED -> Color.Transparent
                }
                val marker = when (line.type) {
                    DiffLineType.ADDED -> "+"
                    DiffLineType.REMOVED -> "-"
                    DiffLineType.UNCHANGED -> " "
                }
                val markerColor = when (line.type) {
                    DiffLineType.ADDED -> Color(0xFF4ADE80)
                    DiffLineType.REMOVED -> Color(0xFFF87171)
                    DiffLineType.UNCHANGED -> c.editorLineNumberFg
                }

                val highlighted = remember(line.text, language, theme.id) {
                    SyntaxHighlighter.highlightDocument(line.text, language, theme.tokens)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor)
                        .padding(horizontal = 8.dp, vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = line.originalLineNumber?.toString() ?: "",
                        color = c.editorLineNumberFg,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(34.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = line.modifiedLineNumber?.toString() ?: "",
                        color = c.editorLineNumberFg,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(34.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = marker,
                        color = markerColor,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(14.dp)
                    )
                    Text(
                        text = highlighted,
                        color = c.editorFg,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
