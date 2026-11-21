package com.example.ui.components

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Themes
import com.example.model.CommandItem
import com.example.model.FSNode
import com.example.model.FSNodeType
import com.example.model.ThemeConfig
import com.example.model.ThemeId
import com.example.utils.FsHelpers

private data class PaletteEntry(
    val key: String,
    val title: String,
    val subtitle: String? = null,
    val shortcut: String? = null,
    val icon: @Composable () -> Unit,
    val run: () -> Unit
)

@Composable
fun CommandPaletteModal(
    theme: ThemeConfig,
    isOpen: Boolean,
    initialInput: String,
    nodes: List<FSNode>,
    commands: List<CommandItem>,
    onClose: () -> Unit,
    onSelectFile: (String) -> Unit,
    onSelectTheme: (ThemeId) -> Unit
) {
    if (!isOpen) return
    val c = theme.colors

    var input by remember(isOpen, initialInput) { mutableStateOf(initialInput) }
    var activeIndex by remember(isOpen, initialInput) { mutableStateOf(0) }

    LaunchedEffect(isOpen, initialInput) {
        input = initialInput
        activeIndex = 0
    }

    val isCommandMode = input.startsWith(">")
    val isThemeSubmode = input.lowercase().startsWith(">preferences: color theme")
    val query = if (isCommandMode) input.drop(1).trim().lowercase() else input.trim().lowercase()

    val entries: List<PaletteEntry> = when {
        isThemeSubmode -> Themes.all.values.map { t ->
            PaletteEntry(
                key = t.id.id,
                title = t.name,
                subtitle = if (t.isDark) "Dark Theme" else "Light Theme",
                icon = { Icon(Icons.Filled.Palette, contentDescription = null, tint = c.accent, modifier = Modifier.size(15.dp)) },
                run = {
                    onSelectTheme(t.id)
                    onClose()
                }
            )
        }
        isCommandMode -> commands
            .filter { cmd -> "${cmd.category}: ${cmd.label}".lowercase().contains(query) }
            .map { cmd ->
                PaletteEntry(
                    key = cmd.id,
                    title = "${cmd.category}: ${cmd.label}",
                    shortcut = cmd.shortcut,
                    icon = { Icon(Icons.Filled.Terminal, contentDescription = null, tint = c.sidebarFg.copy(alpha = 0.7f), modifier = Modifier.size(15.dp)) },
                    run = {
                        if (cmd.id == "theme-picker") {
                            input = ">Preferences: Color Theme"
                            activeIndex = 0
                        } else {
                            onClose()
                            cmd.action()
                        }
                    }
                )
            }
        else -> nodes
            .filter { it.type == FSNodeType.FILE }
            .filter { f ->
                val path = FsHelpers.getNodePath(nodes, f.id).lowercase()
                query.isEmpty() || path.contains(query)
            }
            .map { f ->
                PaletteEntry(
                    key = f.id,
                    title = f.name,
                    subtitle = FsHelpers.getNodePath(nodes, f.id),
                    icon = { FileIcon(name = f.name, language = f.language, size = 15.dp) },
                    run = {
                        onSelectFile(f.id)
                        onClose()
                    }
                )
            }
    }

    val clampedIdx = activeIndex.coerceIn(0, (entries.size - 1).coerceAtLeast(0))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onClose)
            .padding(top = 48.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(c.dropdownBg)
                .border(1.dp, c.dropdownBorder, RoundedCornerShape(8.dp))
                .clickable(enabled = false) {}
        ) {
            Box(modifier = Modifier.padding(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(c.inputBg)
                        .border(1.dp, c.focusBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (input.isEmpty()) {
                        Text(
                            text = "Search files by name (type > for commands)",
                            color = c.inputFg.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    }
                    BasicTextField(
                        value = input,
                        onValueChange = {
                            input = it
                            activeIndex = 0
                        },
                        singleLine = true,
                        textStyle = TextStyle(color = c.inputFg, fontSize = 13.sp),
                        cursorBrush = SolidColor(c.accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = { entries.getOrNull(clampedIdx)?.run?.invoke() }
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("command_palette_input")
                    )
                }
            }
            HorizontalDivider(color = c.dropdownBorder)

            if (entries.isEmpty()) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Code, contentDescription = null, tint = c.sidebarFg.copy(alpha = 0.6f), modifier = Modifier.size(15.dp))
                    Text("No matching results", color = c.sidebarFg.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 340.dp).padding(vertical = 4.dp)) {
                    itemsIndexed(entries, key = { _, e -> e.key }) { idx, item ->
                        val isSelected = idx == clampedIdx
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) c.listActiveBg else Color.Transparent)
                                .clickable { item.run() }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item.icon()
                                Text(
                                    text = item.title,
                                    color = if (isSelected) c.listActiveFg else c.sidebarFg,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (item.subtitle != null) {
                                    Text(
                                        text = item.subtitle,
                                        color = (if (isSelected) c.listActiveFg else c.sidebarFg).copy(alpha = 0.6f),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (item.shortcut != null) {
                                Text(
                                    text = item.shortcut,
                                    color = (if (isSelected) c.listActiveFg else c.sidebarFg).copy(alpha = 0.65f),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
