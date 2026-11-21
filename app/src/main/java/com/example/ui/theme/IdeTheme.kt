package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.data.Themes
import com.example.model.ThemeConfig
import com.example.model.ThemeId

val LocalIdeTheme = staticCompositionLocalOf { Themes.get(ThemeId.DARK_MODERN) }

object IdeTheme {
    val current: ThemeConfig
        @Composable
        get() = LocalIdeTheme.current
}

val WorkbenchTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

@Composable
fun VSCodeStudioTheme(
    themeConfig: ThemeConfig,
    content: @Composable () -> Unit
) {
    val c = themeConfig.colors
    val colorScheme = if (themeConfig.isDark) {
        darkColorScheme(
            primary = c.accent,
            onPrimary = c.buttonFg,
            background = c.editorBg,
            onBackground = c.editorFg,
            surface = c.sidebarBg,
            onSurface = c.sidebarFg,
            surfaceVariant = c.panelBg,
            onSurfaceVariant = c.panelFg,
            outline = c.sidebarBorder
        )
    } else {
        lightColorScheme(
            primary = c.accent,
            onPrimary = c.buttonFg,
            background = c.editorBg,
            onBackground = c.editorFg,
            surface = c.sidebarBg,
            onSurface = c.sidebarFg,
            surfaceVariant = c.panelBg,
            onSurfaceVariant = c.panelFg,
            outline = c.sidebarBorder
        )
    }

    CompositionLocalProvider(LocalIdeTheme provides themeConfig) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WorkbenchTypography,
            content = content
        )
    }
}
