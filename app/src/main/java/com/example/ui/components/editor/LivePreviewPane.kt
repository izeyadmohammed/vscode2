package com.example.ui.components.editor

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.FSNode
import com.example.model.FileLanguage
import com.example.model.ThemeConfig

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LivePreviewPane(
    theme: ThemeConfig,
    file: FSNode
) {
    val c = theme.colors
    val content = file.content.orEmpty()

    if (file.language == FileLanguage.MARKDOWN) {
        MarkdownPreviewBody(theme = theme, markdown = content)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(c.tabBarBg)
                .border(0.5.dp, c.tabBorder)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Live Preview — ${file.name}", color = c.tabActiveFg, fontSize = 11.sp)
            Text(
                text = "localhost:3000/${file.name}",
                color = c.tabActiveFg.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = WebViewClient()
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL("http://localhost:3000/", content, "text/html", "UTF-8", null)
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun MarkdownPreviewBody(
    theme: ThemeConfig,
    markdown: String
) {
    val c = theme.colors
    val blocks = markdown.split(Regex("```[\\w]*\\n([\\s\\S]*?)```"))
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.editorBg)
            .verticalScroll(rememberScrollState())
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEachIndexed { idx, part ->
            if (idx % 2 == 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(c.inputBg)
                        .padding(12.dp)
                ) {
                    Text(
                        text = part.trimEnd(),
                        color = c.editorFg,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                part.split("\n").forEach { line ->
                    when {
                        line.startsWith("### ") -> Text(
                            text = line.drop(4),
                            color = c.editorFg,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        line.startsWith("## ") -> {
                            Text(
                                text = line.drop(3),
                                color = c.editorFg,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                            HorizontalDivider(color = c.tabBorder)
                        }
                        line.startsWith("# ") -> Text(
                            text = line.drop(2),
                            color = c.editorFg,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        line.startsWith("- ") -> Text(
                            text = formatInlineMarkdown("• " + line.drop(2), c.inputBg),
                            color = c.editorFg,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                        line.isBlank() -> Spacer(Modifier.height(4.dp))
                        else -> Text(
                            text = formatInlineMarkdown(line, c.inputBg),
                            color = c.editorFg,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatInlineMarkdown(line: String, codeBg: Color) = buildAnnotatedString {
    val inlineRegex = Regex("(\\*\\*[^*]+\\*\\*|`[^`]+`)")
    var last = 0
    for (m in inlineRegex.findAll(line)) {
        if (m.range.first > last) {
            append(line.substring(last, m.range.first))
        }
        val tok = m.value
        if (tok.startsWith("**")) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(tok.substring(2, tok.length - 2))
            }
        } else {
            withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg)) {
                append(tok.substring(1, tok.length - 1))
            }
        }
        last = m.range.last + 1
    }
    if (last < line.length) {
        append(line.substring(last))
    }
}
