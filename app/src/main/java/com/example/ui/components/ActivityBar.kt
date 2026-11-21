package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActivityView
import com.example.model.ThemeConfig

private data class ActivityItemSpec(
    val view: ActivityView,
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun ActivityBar(
    theme: ThemeConfig,
    activeView: ActivityView,
    sidebarVisible: Boolean,
    gitChangesCount: Int,
    cloudUserEmail: String?,
    onSelectView: (ActivityView) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenCommandPalette: () -> Unit,
    onOpenCloudSync: () -> Unit
) {
    val c = theme.colors
    var accountMenuOpen by remember { mutableStateOf(false) }
    var manageMenuOpen by remember { mutableStateOf(false) }

    val items = listOf(
        ActivityItemSpec(ActivityView.EXPLORER, "Explorer", Icons.Filled.FileCopy),
        ActivityItemSpec(ActivityView.SEARCH, "Search", Icons.Filled.Search),
        ActivityItemSpec(ActivityView.GIT, "Source Control", Icons.Filled.CallSplit, gitChangesCount),
        ActivityItemSpec(ActivityView.DEBUG, "Run and Debug", Icons.Filled.PlayArrow),
        ActivityItemSpec(ActivityView.EXTENSIONS, "Extensions", Icons.Filled.Extension)
    )

    Column(
        modifier = Modifier
            .width(48.dp)
            .fillMaxHeight()
            .background(c.activityBarBg)
            .border(width = 0.5.dp, color = c.activityBarBorder),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Activity Icons
        Column(modifier = Modifier.fillMaxWidth()) {
            items.forEach { item ->
                val isActive = sidebarVisible && activeView == item.view
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { onSelectView(item.view) }
                        .testTag("activity_${item.view.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .width(2.5.dp)
                                .height(48.dp)
                                .background(c.activityBarIndicator)
                        )
                    }
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (isActive) c.activityBarFg else c.activityBarInactiveFg,
                        modifier = Modifier.size(22.dp)
                    )
                    if (item.badgeCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 5.dp, bottom = 6.dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(c.badgeBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.badgeCount.toString(),
                                color = c.badgeFg,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Bottom Icons: Accounts & Manage
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { accountMenuOpen = true }
                        .testTag("activity_account_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = "Accounts",
                        tint = if (cloudUserEmail != null) c.accent else c.activityBarInactiveFg,
                        modifier = Modifier.size(22.dp)
                    )
                }
                DropdownMenu(
                    expanded = accountMenuOpen,
                    onDismissRequest = { accountMenuOpen = false },
                    modifier = Modifier.background(c.dropdownBg)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(
                            text = cloudUserEmail ?: "Not Signed In",
                            color = c.sidebarFg,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (cloudUserEmail != null) "Supabase Cloud Sync Active" else "Sign in to sync across devices",
                            color = c.sidebarFg.copy(alpha = 0.65f),
                            fontSize = 11.sp
                        )
                    }
                    HorizontalDivider(color = c.dropdownBorder)
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (cloudUserEmail != null) "Cloud Sync & Account..." else "Sign in with Supabase...",
                                color = c.sidebarFg,
                                fontSize = 12.sp
                            )
                        },
                        onClick = {
                            accountMenuOpen = false
                            onOpenCloudSync()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Manage Workspace Settings", color = c.sidebarFg, fontSize = 12.sp) },
                        onClick = {
                            accountMenuOpen = false
                            onOpenSettings()
                        }
                    )
                }
            }

            Box {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { manageMenuOpen = true }
                        .testTag("activity_manage_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Manage",
                        tint = c.activityBarInactiveFg,
                        modifier = Modifier.size(22.dp)
                    )
                }
                DropdownMenu(
                    expanded = manageMenuOpen,
                    onDismissRequest = { manageMenuOpen = false },
                    modifier = Modifier.background(c.dropdownBg)
                ) {
                    DropdownMenuItem(
                        text = { Text("Command Palette...", color = c.sidebarFg, fontSize = 12.sp) },
                        onClick = {
                            manageMenuOpen = false
                            onOpenCommandPalette()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Settings", color = c.sidebarFg, fontSize = 12.sp) },
                        onClick = {
                            manageMenuOpen = false
                            onOpenSettings()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Themes: Color Theme", color = c.sidebarFg, fontSize = 12.sp) },
                        onClick = {
                            manageMenuOpen = false
                            onOpenThemes()
                        }
                    )
                }
            }
        }
    }
}
