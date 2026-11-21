package com.example.ui.components.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Themes
import com.example.model.EditorSettings
import com.example.model.ThemeConfig

@Composable
fun SettingsTab(
    theme: ThemeConfig,
    settings: EditorSettings,
    onUpdateSettings: ((EditorSettings) -> EditorSettings) -> Unit,
    onResetWorkspace: () -> Unit
) {
    val c = theme.colors
    var search by remember { mutableStateOf("") }
    var themeDropdownOpen by remember { mutableStateOf(false) }
    var tabDropdownOpen by remember { mutableStateOf(false) }

    fun matches(text: String): Boolean =
        search.isBlank() || text.contains(search.trim(), ignoreCase = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.editorBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
            Text(
                text = "Settings",
                color = c.editorFg,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(c.inputBg)
                    .border(1.dp, c.inputBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (search.isEmpty()) {
                    Text(
                        text = "Search settings (e.g., theme, font, minimap, wrap)...",
                        color = c.inputFg.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                }
                BasicTextField(
                    value = search,
                    onValueChange = { search = it },
                    singleLine = true,
                    textStyle = TextStyle(color = c.inputFg, fontSize = 13.sp),
                    cursorBrush = SolidColor(c.accent),
                    modifier = Modifier.fillMaxWidth().testTag("settings_search_input")
                )
            }

            Spacer(Modifier.height(16.dp))

            if (matches("Color Theme workbench appearance")) {
                SettingBlock(
                    theme = theme,
                    title = "Workbench: Color Theme",
                    description = "Specifies the color theme used in the workbench and syntax highlighter."
                ) {
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(c.inputBg)
                                .border(1.dp, c.inputBorder, RoundedCornerShape(4.dp))
                                .clickable { themeDropdownOpen = true }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                .testTag("settings_theme_dropdown"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(Themes.get(settings.theme).name, color = c.inputFg, fontSize = 13.sp)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = c.inputFg)
                        }
                        DropdownMenu(
                            expanded = themeDropdownOpen,
                            onDismissRequest = { themeDropdownOpen = false },
                            modifier = Modifier.background(c.dropdownBg)
                        ) {
                            Themes.all.values.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t.name, color = c.sidebarFg, fontSize = 13.sp) },
                                    onClick = {
                                        onUpdateSettings { it.copy(theme = t.id) }
                                        themeDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (matches("Editor: Font Size")) {
                SettingBlock(
                    theme = theme,
                    title = "Editor: Font Size",
                    description = "Controls the font size in scaled pixels (${settings.fontSize}sp)."
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = settings.fontSize.toFloat(),
                            onValueChange = { v -> onUpdateSettings { it.copy(fontSize = v.toInt()) } },
                            valueRange = 11f..24f,
                            steps = 12,
                            colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent),
                            modifier = Modifier.width(200.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "${settings.fontSize}px",
                            color = c.editorFg,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            if (matches("Editor: Tab Size")) {
                SettingBlock(
                    theme = theme,
                    title = "Editor: Tab Size",
                    description = "The number of spaces a tab is equal to."
                ) {
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(c.inputBg)
                                .border(1.dp, c.inputBorder, RoundedCornerShape(4.dp))
                                .clickable { tabDropdownOpen = true }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(settings.tabSize.toString(), color = c.inputFg, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                            Spacer(Modifier.width(10.dp))
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = c.inputFg)
                        }
                        DropdownMenu(
                            expanded = tabDropdownOpen,
                            onDismissRequest = { tabDropdownOpen = false },
                            modifier = Modifier.background(c.dropdownBg)
                        ) {
                            listOf(2, 4).forEach { sz ->
                                DropdownMenuItem(
                                    text = { Text("$sz spaces", color = c.sidebarFg, fontSize = 13.sp) },
                                    onClick = {
                                        onUpdateSettings { it.copy(tabSize = sz) }
                                        tabDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (matches("Editor: Word Wrap")) {
                SettingCheckboxRow(
                    theme = theme,
                    title = "Editor: Word Wrap",
                    description = "Controls how lines should wrap inside the editor.",
                    checked = settings.wordWrap,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(wordWrap = v) } }
                )
            }

            if (matches("Editor: Minimap")) {
                SettingCheckboxRow(
                    theme = theme,
                    title = "Editor: Minimap Enabled",
                    description = "Controls whether the minimap is shown on the right.",
                    checked = settings.minimap,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(minimap = v) } }
                )
            }

            if (matches("Editor: Line Numbers")) {
                SettingCheckboxRow(
                    theme = theme,
                    title = "Editor: Line Numbers",
                    description = "Controls the display of line numbers in the gutter.",
                    checked = settings.lineNumbers,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(lineNumbers = v) } }
                )
            }

            if (matches("Files: Auto Save")) {
                SettingCheckboxRow(
                    theme = theme,
                    title = "Files: Auto Save",
                    description = "Automatically saves dirty editors after a brief edit.",
                    checked = settings.autoSave,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(autoSave = v) } }
                )
            }

            if (matches("Editor: Format On Save")) {
                SettingCheckboxRow(
                    theme = theme,
                    title = "Editor: Format On Save",
                    description = "Format a file automatically when saving.",
                    checked = settings.formatOnSave,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(formatOnSave = v) } }
                )
            }

            if (matches("Editor: Render Whitespace")) {
                SettingCheckboxRow(
                    theme = theme,
                    title = "Editor: Render Whitespace",
                    description = "Controls how the editor renders whitespace characters.",
                    checked = settings.renderWhitespace,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(renderWhitespace = v) } }
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("Workspace Storage", color = c.editorFg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Clear all workspace files, commits, and preferences back to a clean empty workbench.",
                color = c.editorFg.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(c.inputBg)
                    .border(1.dp, c.inputBorder, RoundedCornerShape(4.dp))
                    .clickable(onClick = onResetWorkspace)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("settings_reset_workspace_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.RestartAlt, contentDescription = null, tint = c.editorFg, modifier = Modifier.size(15.dp))
                Text("Reset Workspace", color = c.editorFg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SettingBlock(
    theme: ThemeConfig,
    title: String,
    description: String,
    control: @Composable () -> Unit
) {
    val c = theme.colors
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(title, color = c.editorFg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(3.dp))
        Text(description, color = c.editorFg.copy(alpha = 0.7f), fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        control()
    }
    HorizontalDivider(color = c.tabBorder)
}

@Composable
private fun SettingCheckboxRow(
    theme: ThemeConfig,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val c = theme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = c.accent, uncheckedColor = c.inputBorder)
        )
        Column(modifier = Modifier.padding(top = 8.dp)) {
            Text(title, color = c.editorFg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(description, color = c.editorFg.copy(alpha = 0.7f), fontSize = 12.sp)
        }
    }
    HorizontalDivider(color = c.tabBorder)
}
