package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SupabaseSession
import com.example.data.SupabaseSyncClient
import com.example.model.ThemeConfig

@Composable
fun CloudAccountModal(
    theme: ThemeConfig,
    isOpen: Boolean,
    session: SupabaseSession?,
    syncing: Boolean,
    authLoading: Boolean,
    lastSyncedAt: String?,
    syncMode: String?,
    errorMsg: String?,
    infoMsg: String?,
    onClose: () -> Unit,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onSignOut: () -> Unit,
    onPushWorkspace: () -> Unit,
    onPullWorkspace: () -> Unit
) {
    if (!isOpen) return
    val c = theme.colors
    val clipboard = LocalClipboardManager.current

    var mode by remember { mutableStateOf("signin") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var copiedSql by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onClose)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(c.dropdownBg)
                .border(1.dp, c.dropdownBorder, RoundedCornerShape(8.dp))
                .clickable(enabled = false) {}
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Storage, contentDescription = null, tint = c.accent, modifier = Modifier.size(18.dp))
                    Text("Supabase Cloud Sync & Accounts", color = c.sidebarFg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = c.sidebarFg, modifier = Modifier.size(16.dp))
                }
            }
            HorizontalDivider(color = c.dropdownBorder)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    !SupabaseSyncClient.isConfigured -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x1FF59E0B))
                                .border(1.dp, Color(0x55F59E0B), RoundedCornerShape(4.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Supabase environment variables (SUPABASE_URL / SUPABASE_ANON_KEY) are not configured.",
                                color = c.sidebarFg,
                                fontSize = 12.sp
                            )
                        }
                    }
                    session == null -> {
                        // Sign In / Create Account tabs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("signin" to "Sign In", "signup" to "Create Account").forEach { (id, label) ->
                                val selected = mode == id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (selected) c.listActiveBg else Color.Transparent)
                                        .clickable { mode = id }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (selected) c.listActiveFg else c.sidebarFg,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Email", color = c.sidebarFg.copy(alpha = 0.8f), fontSize = 12.sp)
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
                                if (email.isEmpty()) {
                                    Text("developer@example.com", color = c.inputFg.copy(alpha = 0.45f), fontSize = 13.sp)
                                }
                                BasicTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    singleLine = true,
                                    textStyle = TextStyle(color = c.inputFg, fontSize = 13.sp),
                                    cursorBrush = SolidColor(c.accent),
                                    modifier = Modifier.fillMaxWidth().testTag("cloud_email_input")
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Password", color = c.sidebarFg.copy(alpha = 0.8f), fontSize = 12.sp)
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
                                if (password.isEmpty()) {
                                    Text("Minimum 6 characters", color = c.inputFg.copy(alpha = 0.45f), fontSize = 13.sp)
                                }
                                BasicTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    textStyle = TextStyle(color = c.inputFg, fontSize = 13.sp),
                                    cursorBrush = SolidColor(c.accent),
                                    modifier = Modifier.fillMaxWidth().testTag("cloud_password_input")
                                )
                            }
                        }

                        if (!errorMsg.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x26EF4444))
                                    .padding(10.dp)
                            ) {
                                Text(errorMsg, color = Color(0xFFF87171), fontSize = 12.sp)
                            }
                        }

                        if (!infoMsg.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x2610B981))
                                    .padding(10.dp)
                            ) {
                                Text(infoMsg, color = Color(0xFF4ADE80), fontSize = 12.sp)
                            }
                        }

                        val canSubmit = !authLoading && email.isNotBlank() && password.length >= 6
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (canSubmit) c.buttonBg else c.buttonBg.copy(alpha = 0.45f))
                                .clickable(enabled = canSubmit) {
                                    if (mode == "signin") onSignIn(email, password) else onSignUp(email, password)
                                }
                                .testTag("cloud_auth_submit_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when {
                                    authLoading -> "Connecting..."
                                    mode == "signin" -> "Sign In to Sync Workspace"
                                    else -> "Create Supabase Account"
                                },
                                color = c.buttonFg,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    else -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(c.inputBg)
                                .border(1.dp, c.inputBorder, RoundedCornerShape(4.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(18.dp))
                                Column {
                                    Text(session.email, color = c.sidebarFg, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        text = "Connected to Supabase" + (lastSyncedAt?.let { " · Synced" } ?: ""),
                                        color = c.sidebarFg.copy(alpha = 0.65f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(1.dp, c.inputBorder, RoundedCornerShape(4.dp))
                                    .clickable(onClick = onSignOut)
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = c.sidebarFg, modifier = Modifier.size(13.dp))
                                Text("Sign Out", color = c.sidebarFg, fontSize = 11.sp)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (syncing) c.buttonBg.copy(alpha = 0.5f) else c.buttonBg)
                                    .clickable(enabled = !syncing, onClick = onPushWorkspace),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = c.buttonFg, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (syncing) "Syncing..." else "Save to Cloud", color = c.buttonFg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(1.dp, c.inputBorder, RoundedCornerShape(4.dp))
                                    .clickable(enabled = !syncing, onClick = onPullWorkspace),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.CloudDownload, contentDescription = null, tint = c.sidebarFg, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Load from Cloud", color = c.sidebarFg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(c.editorBg)
                                .border(1.dp, c.dropdownBorder, RoundedCornerShape(4.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (syncMode == "table") "Postgres Table Active (public.vscode_workspaces)" else "Optional: Create Postgres Table",
                                    color = c.sidebarFg.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .clickable {
                                            clipboard.setText(AnnotatedString(SupabaseSyncClient.WORKSPACES_SQL))
                                            copiedSql = true
                                        }
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(if (copiedSql) Icons.Filled.Check else Icons.Filled.ContentCopy, contentDescription = "Copy SQL", tint = c.accent, modifier = Modifier.size(12.dp))
                                    Text(if (copiedSql) "Copied" else "Copy SQL", color = c.accent, fontSize = 11.sp)
                                }
                            }
                            Text(
                                text = SupabaseSyncClient.WORKSPACES_SQL,
                                color = c.sidebarFg.copy(alpha = 0.72f),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            )
                        }
                    }
                }
            }
        }
    }
}
