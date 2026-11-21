package com.example.ui.components.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EditorTab
import com.example.model.FSNode
import com.example.model.FSNodeType
import com.example.model.GitCommit
import com.example.model.InlineCreateState
import com.example.model.ThemeConfig
import com.example.ui.components.FileIcon
import com.example.utils.FsHelpers

private data class FlattenedTreeNode(
    val node: FSNode,
    val depth: Int
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExplorerView(
    theme: ThemeConfig,
    workspaceName: String,
    nodes: List<FSNode>,
    activeFileId: String?,
    openTabs: List<EditorTab>,
    dirtyFileIds: Set<String>,
    modifiedFileIds: Set<String>,
    untrackedFileIds: Set<String>,
    commits: List<GitCommit>,
    inlineCreate: InlineCreateState?,
    onSetInlineCreate: (InlineCreateState?) -> Unit,
    onSelectFile: (String, Int?) -> Unit,
    onCloseTab: (String) -> Unit,
    onToggleFolder: (String) -> Unit,
    onCollapseAllFolders: () -> Unit,
    onCreateNode: (String?, FSNodeType, String) -> Unit,
    onRenameNode: (String, String) -> Unit,
    onDeleteNode: (String) -> Unit,
    onDuplicateFile: (String) -> Unit,
    onExportFile: (String) -> Unit,
    onOpenLocalFolder: () -> Unit
) {
    val c = theme.colors
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    var openEditorsExpanded by remember { mutableStateOf(true) }
    var filesExpanded by remember { mutableStateOf(true) }
    var outlineExpanded by remember { mutableStateOf(false) }
    var timelineExpanded by remember { mutableStateOf(false) }

    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var renamingId by remember { mutableStateOf<String?>(null) }
    var inlineInput by remember { mutableStateOf("") }
    var contextMenuNodeId by remember { mutableStateOf<String?>(null) }

    val activeFile = remember(nodes, activeFileId) {
        nodes.firstOrNull { it.id == activeFileId }
    }
    val outlineSymbols = remember(activeFile?.content, activeFile?.language) {
        activeFile?.content?.let { FsHelpers.extractOutlineSymbols(it, activeFile.language) } ?: emptyList()
    }

    val flattenedTree = remember(nodes) {
        val result = mutableListOf<FlattenedTreeNode>()
        fun traverse(parentId: String?, depth: Int) {
            val children = nodes
                .filter { it.parentId == parentId }
                .sortedWith(compareBy<FSNode> { it.type != FSNodeType.FOLDER }.thenBy { it.name.lowercase() })
            for (child in children) {
                result.add(FlattenedTreeNode(child, depth))
                if (child.type == FSNodeType.FOLDER && child.isOpen) {
                    traverse(child.id, depth + 1)
                }
            }
        }
        traverse(null, 0)
        result
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // OPEN EDITORS section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clickable { openEditorsExpanded = !openEditorsExpanded }
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (openEditorsExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = c.sidebarFg,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "OPEN EDITORS",
                color = c.sidebarFg,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = openTabs.size.toString(),
                color = c.sidebarFg.copy(alpha = 0.6f),
                fontSize = 11.sp,
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        if (openEditorsExpanded && openTabs.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 120.dp)
            ) {
                openTabs.forEach { tab ->
                    val isDirty = tab.fileId?.let { dirtyFileIds.contains(it) } == true
                    val isActive = tab.fileId != null && tab.fileId == activeFileId
                    val fileNode = tab.fileId?.let { fid -> nodes.firstOrNull { it.id == fid } }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .background(if (isActive) c.listActiveBg else Color.Transparent)
                            .clickable { tab.fileId?.let { onSelectFile(it, null) } }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close ${tab.title}",
                            tint = c.sidebarFg.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onCloseTab(tab.id) }
                        )
                        Spacer(Modifier.width(6.dp))
                        if (fileNode != null) {
                            FileIcon(name = fileNode.name, language = fileNode.language, size = 13.dp)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = tab.title,
                            color = if (isActive) c.listActiveFg else c.sidebarFg,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isDirty) {
                            Text("●", color = c.accent, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = c.sidebarBorder)

        // WORKSPACE FOLDER TREE HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { filesExpanded = !filesExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (filesExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = c.sidebarFg,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (nodes.isEmpty() && workspaceName == "workspace") {
                        "NO FOLDER OPENED"
                    } else {
                        workspaceName.uppercase()
                    },
                    color = c.sidebarFg,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = {
                    inlineInput = ""
                    filesExpanded = true
                    onSetInlineCreate(InlineCreateState(selectedFolderId, FSNodeType.FILE))
                },
                modifier = Modifier
                    .size(24.dp)
                    .testTag("explorer_new_file_button")
            ) {
                Icon(Icons.Filled.NoteAdd, contentDescription = "New File", tint = c.sidebarFg, modifier = Modifier.size(15.dp))
            }
            IconButton(
                onClick = {
                    inlineInput = ""
                    filesExpanded = true
                    onSetInlineCreate(InlineCreateState(selectedFolderId, FSNodeType.FOLDER))
                },
                modifier = Modifier
                    .size(24.dp)
                    .testTag("explorer_new_folder_button")
            ) {
                Icon(Icons.Filled.CreateNewFolder, contentDescription = "New Folder", tint = c.sidebarFg, modifier = Modifier.size(15.dp))
            }
            IconButton(
                onClick = onOpenLocalFolder,
                modifier = Modifier
                    .size(24.dp)
                    .testTag("explorer_open_folder_button")
            ) {
                Icon(Icons.Filled.FolderOpen, contentDescription = "Open Local Folder", tint = c.sidebarFg, modifier = Modifier.size(15.dp))
            }
            IconButton(
                onClick = onCollapseAllFolders,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Filled.UnfoldLess, contentDescription = "Collapse Folders", tint = c.sidebarFg, modifier = Modifier.size(15.dp))
            }
        }

        // TREE CONTENT
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (filesExpanded) {
                if (nodes.isEmpty() && inlineCreate == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "You have not yet opened a folder.",
                            color = c.sidebarFg.copy(alpha = 0.75f),
                            fontSize = 12.sp
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(c.buttonBg)
                                .clickable(onClick = onOpenLocalFolder)
                                .testTag("explorer_empty_open_folder"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = c.buttonFg, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Open Folder", color = c.buttonFg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .border(1.dp, c.inputBorder, RoundedCornerShape(3.dp))
                                .clickable {
                                    inlineInput = ""
                                    onSetInlineCreate(InlineCreateState(null, FSNodeType.FILE))
                                }
                                .testTag("explorer_empty_new_file"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.NoteAdd, contentDescription = null, tint = c.sidebarFg, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("New File", color = c.sidebarFg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (inlineCreate != null && inlineCreate.parentId == null) {
                            item(key = "inline-create-root") {
                                InlineNodeInputRow(
                                    theme = theme,
                                    depth = 0,
                                    value = inlineInput,
                                    isFolder = inlineCreate.type == FSNodeType.FOLDER,
                                    placeholder = if (inlineCreate.type == FSNodeType.FOLDER) "Folder name..." else "File name...",
                                    onValueChange = { inlineInput = it },
                                    onSubmit = {
                                        if (inlineInput.isNotBlank()) {
                                            onCreateNode(null, inlineCreate.type, inlineInput)
                                        } else {
                                            onSetInlineCreate(null)
                                        }
                                        inlineInput = ""
                                    },
                                    onCancel = {
                                        inlineInput = ""
                                        onSetInlineCreate(null)
                                    }
                                )
                            }
                        }

                        items(flattenedTree, key = { it.node.id }) { item ->
                            val node = item.node
                            val isFolder = node.type == FSNodeType.FOLDER
                            val isSelected = if (isFolder) selectedFolderId == node.id else activeFileId == node.id
                            val isDirty = dirtyFileIds.contains(node.id)
                            val isModified = modifiedFileIds.contains(node.id)
                            val isUntracked = untrackedFileIds.contains(node.id)

                            Column {
                                if (renamingId == node.id) {
                                    InlineNodeInputRow(
                                        theme = theme,
                                        depth = item.depth,
                                        value = inlineInput,
                                        isFolder = isFolder,
                                        placeholder = "New name...",
                                        onValueChange = { inlineInput = it },
                                        onSubmit = {
                                            if (inlineInput.isNotBlank()) {
                                                onRenameNode(node.id, inlineInput)
                                            }
                                            renamingId = null
                                            inlineInput = ""
                                        },
                                        onCancel = {
                                            renamingId = null
                                            inlineInput = ""
                                        }
                                    )
                                } else {
                                    Box {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(28.dp)
                                                .background(if (isSelected) c.listActiveBg else Color.Transparent)
                                                .combinedClickable(
                                                    onClick = {
                                                        if (isFolder) {
                                                            selectedFolderId = node.id
                                                            onToggleFolder(node.id)
                                                        } else {
                                                            selectedFolderId = node.parentId
                                                            onSelectFile(node.id, null)
                                                        }
                                                    },
                                                    onLongClick = { contextMenuNodeId = node.id }
                                                )
                                                .padding(start = (item.depth * 14 + 8).dp, end = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isFolder) {
                                                Icon(
                                                    imageVector = if (node.isOpen) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                                                    contentDescription = null,
                                                    tint = c.sidebarFg.copy(alpha = 0.75f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            } else {
                                                Spacer(Modifier.width(14.dp))
                                            }
                                            Spacer(Modifier.width(4.dp))
                                            FileIcon(
                                                name = node.name,
                                                language = node.language,
                                                isFolder = isFolder,
                                                isOpen = node.isOpen,
                                                size = 14.dp
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = node.name,
                                                color = when {
                                                    isSelected -> c.listActiveFg
                                                    isUntracked -> Color(0xFF73C991)
                                                    isModified -> Color(0xFFE2C08D)
                                                    else -> c.sidebarFg
                                                },
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isDirty) {
                                                Text("●", color = c.accent, fontSize = 10.sp)
                                                Spacer(Modifier.width(4.dp))
                                            }
                                            if (!isFolder && isModified) {
                                                Text("M", color = Color(0xFFE2C08D), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                            } else if (!isFolder && isUntracked) {
                                                Text("U", color = Color(0xFF73C991), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = contextMenuNodeId == node.id,
                                            onDismissRequest = { contextMenuNodeId = null },
                                            modifier = Modifier.background(c.dropdownBg)
                                        ) {
                                            if (isFolder) {
                                                DropdownMenuItem(
                                                    text = { Text("New File...", color = c.sidebarFg, fontSize = 12.sp) },
                                                    onClick = {
                                                        contextMenuNodeId = null
                                                        inlineInput = ""
                                                        onSetInlineCreate(InlineCreateState(node.id, FSNodeType.FILE))
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("New Folder...", color = c.sidebarFg, fontSize = 12.sp) },
                                                    onClick = {
                                                        contextMenuNodeId = null
                                                        inlineInput = ""
                                                        onSetInlineCreate(InlineCreateState(node.id, FSNodeType.FOLDER))
                                                    }
                                                )
                                            }
                                            DropdownMenuItem(
                                                text = { Text("Rename", color = c.sidebarFg, fontSize = 12.sp) },
                                                onClick = {
                                                    contextMenuNodeId = null
                                                    inlineInput = node.name
                                                    renamingId = node.id
                                                }
                                            )
                                            if (!isFolder) {
                                                DropdownMenuItem(
                                                    text = { Text("Duplicate File", color = c.sidebarFg, fontSize = 12.sp) },
                                                    onClick = {
                                                        contextMenuNodeId = null
                                                        onDuplicateFile(node.id)
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Export to Device", color = c.sidebarFg, fontSize = 12.sp) },
                                                    onClick = {
                                                        contextMenuNodeId = null
                                                        onExportFile(node.id)
                                                    }
                                                )
                                            }
                                            DropdownMenuItem(
                                                text = { Text("Copy Relative Path", color = c.sidebarFg, fontSize = 12.sp) },
                                                onClick = {
                                                    contextMenuNodeId = null
                                                    clipboard.setText(AnnotatedString(FsHelpers.getNodePath(nodes, node.id)))
                                                }
                                            )
                                            HorizontalDivider(color = c.dropdownBorder)
                                            DropdownMenuItem(
                                                text = { Text("Delete", color = Color(0xFFF87171), fontSize = 12.sp) },
                                                onClick = {
                                                    contextMenuNodeId = null
                                                    onDeleteNode(node.id)
                                                }
                                            )
                                        }
                                    }
                                }

                                if (isFolder && node.isOpen && inlineCreate != null && inlineCreate.parentId == node.id) {
                                    InlineNodeInputRow(
                                        theme = theme,
                                        depth = item.depth + 1,
                                        value = inlineInput,
                                        isFolder = inlineCreate.type == FSNodeType.FOLDER,
                                        placeholder = if (inlineCreate.type == FSNodeType.FOLDER) "Folder name..." else "File name...",
                                        onValueChange = { inlineInput = it },
                                        onSubmit = {
                                            if (inlineInput.isNotBlank()) {
                                                onCreateNode(node.id, inlineCreate.type, inlineInput)
                                            } else {
                                                onSetInlineCreate(null)
                                            }
                                            inlineInput = ""
                                        },
                                        onCancel = {
                                            inlineInput = ""
                                            onSetInlineCreate(null)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // OUTLINE SECTION
        HorizontalDivider(color = c.sidebarBorder)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clickable { outlineExpanded = !outlineExpanded }
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (outlineExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = c.sidebarFg,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("OUTLINE", color = c.sidebarFg, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(outlineSymbols.size.toString(), color = c.sidebarFg.copy(alpha = 0.6f), fontSize = 11.sp, modifier = Modifier.padding(end = 4.dp))
        }
        if (outlineExpanded) {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp)) {
                if (outlineSymbols.isEmpty()) {
                    Text(
                        text = "No symbols found in active editor",
                        color = c.sidebarFg.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                } else {
                    outlineSymbols.take(15).forEach { sym ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .clickable { activeFile?.let { onSelectFile(it.id, sym.line) } }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Code, contentDescription = null, tint = c.accent, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(sym.name, color = c.sidebarFg, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text(":${sym.line}", color = c.sidebarFg.copy(alpha = 0.55f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // TIMELINE SECTION
        HorizontalDivider(color = c.sidebarBorder)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clickable { timelineExpanded = !timelineExpanded }
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (timelineExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = c.sidebarFg,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("TIMELINE", color = c.sidebarFg, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(commits.size.toString(), color = c.sidebarFg.copy(alpha = 0.6f), fontSize = 11.sp, modifier = Modifier.padding(end = 4.dp))
        }
        if (timelineExpanded) {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp)) {
                commits.take(10).forEach { cm ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.History, contentDescription = null, tint = c.accent, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(cm.message, color = c.sidebarFg, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(6.dp))
                        Text(cm.hash, color = c.sidebarFg.copy(alpha = 0.55f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineNodeInputRow(
    theme: ThemeConfig,
    depth: Int,
    value: String,
    isFolder: Boolean,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit
) {
    val c = theme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 14 + 10).dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileIcon(
            name = value.ifEmpty { if (isFolder) "folder" else "file.ts" },
            isFolder = isFolder,
            size = 14.dp
        )
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(26.dp)
                .background(c.inputBg, RoundedCornerShape(2.dp))
                .border(1.dp, c.focusBorder, RoundedCornerShape(2.dp))
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(placeholder, color = c.inputFg.copy(alpha = 0.45f), fontSize = 12.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = c.inputFg, fontSize = 12.sp),
                cursorBrush = SolidColor(c.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("explorer_inline_input")
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = "OK",
            color = c.accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable(onClick = onSubmit)
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .testTag("explorer_inline_submit")
        )
        Text(
            text = "✕",
            color = c.sidebarFg.copy(alpha = 0.65f),
            fontSize = 11.sp,
            modifier = Modifier
                .clickable(onClick = onCancel)
                .padding(horizontal = 4.dp, vertical = 4.dp)
        )
    }
}
