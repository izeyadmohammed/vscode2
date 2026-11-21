package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.WorkbenchScreen
import com.example.ui.theme.VSCodeStudioTheme
import com.example.viewmodel.IdeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: IdeViewModel = viewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val theme = state.theme

            LaunchedEffect(theme.id) {
                val barColor = theme.colors.titleBarBg.toArgb()
                val navColor = theme.colors.statusBarBg.toArgb()
                if (theme.isDark) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.dark(barColor),
                        navigationBarStyle = SystemBarStyle.dark(navColor)
                    )
                } else {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.light(barColor, barColor),
                        navigationBarStyle = SystemBarStyle.light(navColor, navColor)
                    )
                }
            }

            VSCodeStudioTheme(themeConfig = theme) {
                WorkbenchScreen(viewModel = viewModel)
            }
        }
    }
}
