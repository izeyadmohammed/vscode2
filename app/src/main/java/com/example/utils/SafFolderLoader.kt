package com.example.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.model.FSNode
import com.example.model.FSNodeType
import com.example.model.FileLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

data class LoadedWorkspace(
    val workspaceName: String,
    val nodes: List<FSNode>
)

object SafFolderLoader {
    private val ignoredDirs = setOf("node_modules", ".git", "dist", "build", ".next", ".turbo", ".gradle")
    private val textExts = setOf(
        "ts", "tsx", "js", "jsx", "mjs", "cjs", "json", "html", "htm", "css", "scss",
        "md", "markdown", "py", "txt", "yml", "yaml", "env", "svg", "xml", "sql", "sh", "kt", "kts"
    )
    private const val MAX_FILE_BYTES = 300_000L

    private fun isLikelyTextFile(name: String): Boolean {
        if (name.startsWith(".env") || name == ".gitignore" || name == "Dockerfile") return true
        val ext = name.substringAfterLast('.', "").lowercase()
        return textExts.contains(ext)
    }

    suspend fun loadFromTreeUri(context: Context, treeUri: Uri): LoadedWorkspace =
        withContext(Dispatchers.IO) {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                ?: return@withContext LoadedWorkspace("local-project", emptyList())
            val rootName = rootDoc.name ?: "local-project"
            val nodes = mutableListOf<FSNode>()

            fun traverse(dir: DocumentFile, parentId: String?, depth: Int) {
                for (child in dir.listFiles()) {
                    val name = child.name ?: continue
                    if (child.isDirectory) {
                        if (ignoredDirs.contains(name)) continue
                        val folderId = "folder-${UUID.randomUUID().toString().take(8)}"
                        nodes.add(
                            FSNode(
                                id = folderId,
                                name = name,
                                type = FSNodeType.FOLDER,
                                parentId = parentId,
                                isOpen = depth < 2
                            )
                        )
                        traverse(child, folderId, depth + 1)
                    } else if (child.isFile) {
                        if (!isLikelyTextFile(name) || child.length() > MAX_FILE_BYTES) continue
                        val content = runCatching {
                            context.contentResolver.openInputStream(child.uri)?.bufferedReader()?.use {
                                it.readText()
                            }
                        }.getOrNull() ?: continue
                        nodes.add(
                            FSNode(
                                id = "file-${UUID.randomUUID().toString().take(8)}",
                                name = name,
                                type = FSNodeType.FILE,
                                parentId = parentId,
                                language = FileLanguage.detect(name),
                                content = content
                            )
                        )
                    }
                }
            }

            traverse(rootDoc, null, 0)
            LoadedWorkspace(rootName, nodes)
        }

    suspend fun writeTextToUri(context: Context, uri: Uri, content: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                    it.write(content)
                } ?: throw IllegalStateException("Unable to open output stream")
            }
        }
}
