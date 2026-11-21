package com.example.ui.components.sidebar

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExtensionItem
import com.example.model.ThemeConfig

@Composable
fun ExtensionsView(
    theme: ThemeConfig,
    extensions: List<ExtensionItem>,
    onToggleInstall: (String) -> Unit,
    onToggleEnable: (String) -> Unit
) {
    val c = theme.colors
    var query by remember { mutableStateOf("") }

    val filtered = remember(extensions, query) {
        if (query.isBlank()) extensions
        else extensions.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.publisher.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(c.inputBg, RoundedCornerShape(3.dp))
                    .border(1.dp, c.inputBorder, RoundedCornerShape(3.dp))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text("Search Extensions in Marketplace", color = c.inputFg.copy(alpha = 0.45f), fontSize = 12.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = c.inputFg, fontSize = 12.sp),
                    cursorBrush = SolidColor(c.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("extensions_search_input")
                )
            }
        }
        HorizontalDivider(color = c.sidebarBorder)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filtered, key = { it.id }) { ext ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(ext.iconColorHex)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ext.shortTag,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ext.name,
                                    color = c.sidebarFg,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "v${ext.version}",
                                    color = c.sidebarFg.copy(alpha = 0.55f),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = ext.description,
                                color = c.sidebarFg.copy(alpha = 0.75f),
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(ext.publisher, color = c.sidebarFg.copy(alpha = 0.6f), fontSize = 10.sp)
                                    Icon(Icons.Filled.Download, contentDescription = null, tint = c.sidebarFg.copy(alpha = 0.6f), modifier = Modifier.size(10.dp))
                                    Text(ext.downloads, color = c.sidebarFg.copy(alpha = 0.6f), fontSize = 10.sp)
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(10.dp))
                                    Text(ext.rating.toString(), color = c.sidebarFg.copy(alpha = 0.6f), fontSize = 10.sp)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (ext.installed) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(if (ext.enabled) c.listActiveBg else c.inputBg)
                                                .border(1.dp, c.inputBorder, RoundedCornerShape(3.dp))
                                                .clickable { onToggleEnable(ext.id) }
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                                .testTag("ext_enable_${ext.id}")
                                        ) {
                                            Text(
                                                text = if (ext.enabled) "Disable" else "Enable",
                                                color = if (ext.enabled) c.listActiveFg else c.sidebarFg,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(3.dp))
                                                .border(1.dp, c.inputBorder, RoundedCornerShape(3.dp))
                                                .clickable { onToggleInstall(ext.id) }
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text("Uninstall", color = c.sidebarFg.copy(alpha = 0.8f), fontSize = 10.sp)
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(c.buttonBg)
                                                .clickable { onToggleInstall(ext.id) }
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                                .testTag("ext_install_${ext.id}")
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.Check, contentDescription = null, tint = c.buttonFg, modifier = Modifier.size(10.dp))
                                                Spacer(Modifier.width(3.dp))
                                                Text("Install", color = c.buttonFg, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = c.sidebarBorder)
                }
            }
        }
    }
}
