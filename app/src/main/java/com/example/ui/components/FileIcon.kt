package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FileLanguage

@Composable
fun FileIcon(
    name: String,
    language: FileLanguage? = null,
    isFolder: Boolean = false,
    isOpen: Boolean = false,
    size: Dp = 15.dp,
    modifier: Modifier = Modifier
) {
    if (isFolder) {
        Icon(
            imageVector = if (isOpen) Icons.Filled.FolderOpen else Icons.Filled.Folder,
            contentDescription = null,
            tint = Color(0xFFDCAD5A),
            modifier = modifier.size(size)
        )
        return
    }

    val resolvedLang = language ?: FileLanguage.detect(name)
    val (badge, color) = when (resolvedLang) {
        FileLanguage.TYPESCRIPT -> "TS" to Color(0xFF3178C6)
        FileLanguage.TSX -> "TX" to Color(0xFF38BDF8)
        FileLanguage.JAVASCRIPT -> "JS" to Color(0xFFF7DF1E)
        FileLanguage.JSX -> "JX" to Color(0xFF61DAFB)
        FileLanguage.HTML -> "<>" to Color(0xFFE34F26)
        FileLanguage.CSS -> "#" to Color(0xFF42A5F5)
        FileLanguage.JSON -> "{}" to Color(0xFFCBB040)
        FileLanguage.MARKDOWN -> "M" to Color(0xFF60A5FA)
        FileLanguage.PYTHON -> "Py" to Color(0xFF3776AB)
        FileLanguage.PLAINTEXT -> "≡" to Color(0xFF9CA3AF)
    }

    val textColor = if (resolvedLang == FileLanguage.JAVASCRIPT) Color(0xFF181818) else Color.White

    Box(
        modifier = modifier
            .size(size)
            .background(color.copy(alpha = 0.9f), RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = badge,
            color = textColor,
            fontSize = (size.value * 0.52f).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            lineHeight = (size.value * 0.6f).sp
        )
    }
}
