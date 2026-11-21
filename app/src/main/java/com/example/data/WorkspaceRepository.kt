package com.example.data

import android.content.Context
import com.example.model.EditorSettings
import com.example.model.ExtensionItem
import com.example.model.FSNode
import com.example.model.GitCommit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class PersistedWorkspace(
    val workspaceName: String = "workspace",
    val nodes: List<FSNode> = emptyList(),
    val settings: EditorSettings = EditorSettings(),
    val commits: List<GitCommit> = emptyList(),
    val committedSnapshot: Map<String, String> = emptyMap(),
    val extensions: List<ExtensionItem> = InitialWorkspace.initialExtensions
)

class WorkspaceRepository(context: Context) {
    private val storageFile = File(context.filesDir, "vscode_workspace_v2.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    suspend fun load(): PersistedWorkspace = withContext(Dispatchers.IO) {
        if (!storageFile.exists()) {
            return@withContext PersistedWorkspace()
        }
        return@withContext try {
            val raw = storageFile.readText()
            json.decodeFromString<PersistedWorkspace>(raw)
        } catch (_: Exception) {
            PersistedWorkspace()
        }
    }

    suspend fun save(workspace: PersistedWorkspace) = withContext(Dispatchers.IO) {
        try {
            val tempFile = File(storageFile.parentFile, "vscode_workspace_v2.tmp")
            tempFile.writeText(json.encodeToString(PersistedWorkspace.serializer(), workspace))
            if (!tempFile.renameTo(storageFile)) {
                storageFile.writeText(tempFile.readText())
                tempFile.delete()
            }
        } catch (_: Exception) {
            // ignore IO failure silently
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        if (storageFile.exists()) {
            storageFile.delete()
        }
    }
}
