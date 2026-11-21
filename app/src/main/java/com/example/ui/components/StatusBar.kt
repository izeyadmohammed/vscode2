package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.FileLanguage
import com.example.model.NotificationItem
import com.example.model.ThemeConfig

@Composable
fun StatusBar(
    theme: ThemeConfig,
    branch: String,
    branches: List<String>,
    errorCount: Int,
    warningCount: Int,
    cursorLine: Int,
    cursorCol: Int,
    tabSize: Int,
    language: FileLanguage?,
    prettierEnabled: Boolean,
    notifications: List<NotificationItem>,
    isCompact: Boolean,
    onSwitchBranch: (String) -> Unit,
    onOpenProblems: () -> Unit,
    onFormatDocument: () -> Unit,
    onChangeLanguage: (FileLanguage) -> Unit,
    onOpenSettings: () -> Unit,
    onClearNotifications: () -> Unit
) {
    val c = theme.colors
    var branchMenuOpen by remember { mutableStateOf(false) }
    var langMenuOpen by remember { mutableStateOf(false) }
    var notifOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .background(c.statusBarBg)
            .border(width = 0.5.dp, color = c.statusBarBorder),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left status items
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(c.accent)
                    .clickable(onClick = onOpenSettings)
                    .padding(horizontal = 10.dp)
                    .testTag("status_remote_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Code,
                    contentDescription = "Open Workbench Settings",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            Box(modifier = Modifier.fillMaxHeight()) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clickable { branchMenuOpen = true }
                        .padding(horizontal = 8.dp)
                        .testTag("status_branch_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CallSplit,
                        contentDescription = "Git Branch",
                        tint = c.statusBarFg,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(branch, color = c.statusBarFg, fontSize = 11.sp)
                }
                DropdownMenu(
                    expanded = branchMenuOpen,
                    onDismissRequest = { branchMenuOpen = false },
                    modifier = Modifier.background(c.dropdownBg)
                ) {
                    Text(
                        text = "Select Git Branch",
                        color = c.sidebarFg.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    branches.forEach { b ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(b, color = c.sidebarFg, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                    if (b == branch) {
                                        Icon(Icons.Filled.Check, contentDescription = null, tint = c.accent, modifier = Modifier.size(14.dp))
                                    }
                                }
                            },
                            onClick = {
                                onSwitchBranch(b)
                                branchMenuOpen = false
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable(onClick = onOpenProblems)
                    .padding(horizontal = 8.dp)
                    .testTag("status_problems_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Cancel,
                    contentDescription = "Errors",
                    tint = c.statusBarFg,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(errorCount.toString(), color = c.statusBarFg, fontSize = 11.sp)
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Warnings",
                    tint = c.statusBarFg,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(warningCount.toString(), color = c.statusBarFg, fontSize = 11.sp)
            }
        }

        // Right status items
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (language != null) {
                Text(
                    text = "Ln $cursorLine, Col $cursorCol",
                    color = c.statusBarFg,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clickable(onClick = onOpenSettings)
                        .padding(horizontal = 6.dp)
                )
                if (!isCompact) {
                    Text(
                        text = "Spaces: $tabSize",
                        color = c.statusBarFg,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clickable(onClick = onOpenSettings)
                            .padding(horizontal = 6.dp)
                    )
                    Text(
                        text = "UTF-8",
                        color = c.statusBarFg.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }

                Box(modifier = Modifier.fillMaxHeight()) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clickable { langMenuOpen = true }
                            .padding(horizontal = 6.dp)
                            .testTag("status_language_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(language.label, color = c.statusBarFg, fontSize = 11.sp)
                    }
                    DropdownMenu(
                        expanded = langMenuOpen,
                        onDismissRequest = { langMenuOpen = false },
                        modifier = Modifier.background(c.dropdownBg)
                    ) {
                        FileLanguage.entries.forEach { lang ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(lang.label, color = c.sidebarFg, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                        if (lang == language) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = c.accent, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                },
                                onClick = {
                                    onChangeLanguage(lang)
                                    langMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }

            if (prettierEnabled && language != null && !isCompact) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clickable(onClick = onFormatDocument)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = c.statusBarFg, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("Prettier", color = c.statusBarFg, fontSize = 11.sp)
                }
            }

            Box(modifier = Modifier.fillMaxHeight()) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clickable { notifOpen = true }
                        .padding(horizontal = 8.dp)
                        .testTag("status_notifications_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = stringResource(R.string.cd_notifications),
                        tint = c.statusBarFg,
                        modifier = Modifier.size(13.dp)
                    )
                    if (notifications.isNotEmpty()) {
                        Spacer(Modifier.width(3.dp))
                        Text(notifications.size.toString(), color = c.statusBarFg, fontSize = 10.sp)
                    }
                }
                DropdownMenu(
                    expanded = notifOpen,
                    onDismissRequest = { notifOpen = false },
                    modifier = Modifier
                        .background(c.dropdownBg)
                        .widthIn(min = 240.dp, max = 320.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Notifications", color = c.sidebarFg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        if (notifications.isNotEmpty()) {
                            Text(
                                text = "Clear All",
                                color = c.accent,
                                fontSize = 11.sp,
                                modifier = Modifier.clickable {
                                    onClearNotifications()
                                }
                            )
                        }
                    }
                    HorizontalDivider(color = c.dropdownBorder)
                    if (notifications.isEmpty()) {
                        Text(
                            text = "No new notifications",
                            color = c.sidebarFg.copy(alpha = 0.65f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 220.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            notifications.forEach { n ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(n.source, color = c.sidebarFg.copy(alpha = 0.65f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    Text(n.message, color = c.sidebarFg, fontSize = 12.sp)
                                }
                                HorizontalDivider(color = c.dropdownBorder.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}
