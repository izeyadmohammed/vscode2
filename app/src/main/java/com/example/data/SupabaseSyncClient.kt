package com.example.data

import com.example.BuildConfig
import com.example.model.EditorSettings
import com.example.model.FSNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class SupabaseSession(
    val userId: String,
    val email: String,
    val accessToken: String
)

data class CloudPushResult(
    val mode: String,
    val updatedAt: String
)

data class CloudPullResult(
    val mode: String,
    val files: List<FSNode>? = null,
    val settings: EditorSettings? = null,
    val name: String? = null,
    val updatedAt: String? = null
)

@Serializable
private data class SupabaseWorkspacePayload(
    val id: String,
    val user_id: String,
    val name: String,
    val files: List<FSNode>,
    val settings: EditorSettings,
    val updated_at: String
)

object SupabaseSyncClient {
    const val WORKSPACES_SQL = """-- Run this SQL in your Supabase SQL Editor to enable multi-project table storage:
create table if not exists public.vscode_workspaces (
  id text primary key,
  user_id uuid references auth.users(id) on delete cascade not null,
  name text not null,
  files jsonb not null,
  settings jsonb not null,
  updated_at timestamptz default now() not null
);

alter table public.vscode_workspaces enable row level security;

create policy "Users manage their own workspaces"
  on public.vscode_workspaces
  for all
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);"""

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val isConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    suspend fun signInWithEmail(email: String, password: String): Result<SupabaseSession> =
        withContext(Dispatchers.IO) {
            if (!isConfigured) return@withContext Result.failure(IllegalStateException("Supabase is not configured."))
            runCatching {
                val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
                val body = buildJsonObject {
                    put("email", email)
                    put("password", password)
                }.toString()
                val (code, resp) = request(
                    url = "$baseUrl/auth/v1/token?grant_type=password",
                    method = "POST",
                    body = body
                )
                val obj = json.parseToJsonElement(resp).jsonObject
                if (code !in 200..299) {
                    val msg = obj["error_description"]?.jsonPrimitive?.content
                        ?: obj["msg"]?.jsonPrimitive?.content
                        ?: "Sign in failed ($code)"
                    throw IllegalStateException(msg)
                }
                val token = obj["access_token"]?.jsonPrimitive?.content ?: throw IllegalStateException("Missing access token")
                val userObj = obj["user"]?.jsonObject ?: throw IllegalStateException("Missing user info")
                SupabaseSession(
                    userId = userObj["id"]?.jsonPrimitive?.content ?: "",
                    email = userObj["email"]?.jsonPrimitive?.content ?: email,
                    accessToken = token
                )
            }
        }

    suspend fun signUpWithEmail(email: String, password: String): Result<SupabaseSession?> =
        withContext(Dispatchers.IO) {
            if (!isConfigured) return@withContext Result.failure(IllegalStateException("Supabase is not configured."))
            runCatching {
                val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
                val body = buildJsonObject {
                    put("email", email)
                    put("password", password)
                }.toString()
                val (code, resp) = request(
                    url = "$baseUrl/auth/v1/signup",
                    method = "POST",
                    body = body
                )
                val obj = json.parseToJsonElement(resp).jsonObject
                if (code !in 200..299) {
                    val msg = obj["error_description"]?.jsonPrimitive?.content
                        ?: obj["msg"]?.jsonPrimitive?.content
                        ?: "Sign up failed ($code)"
                    throw IllegalStateException(msg)
                }
                val token = obj["access_token"]?.jsonPrimitive?.content
                val userObj = obj["user"]?.jsonObject
                if (token != null && userObj != null) {
                    SupabaseSession(
                        userId = userObj["id"]?.jsonPrimitive?.content ?: "",
                        email = userObj["email"]?.jsonPrimitive?.content ?: email,
                        accessToken = token
                    )
                } else {
                    null
                }
            }
        }

    suspend fun pushWorkspace(
        session: SupabaseSession,
        workspaceName: String,
        files: List<FSNode>,
        settings: EditorSettings
    ): Result<CloudPushResult> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(IllegalStateException("Supabase is not configured."))
        runCatching {
            val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
            val now = java.time.Instant.now().toString()
            val payload = SupabaseWorkspacePayload(
                id = "${session.userId}:$workspaceName",
                user_id = session.userId,
                name = workspaceName,
                files = files,
                settings = settings,
                updated_at = now
            )
            val (code, _) = request(
                url = "$baseUrl/rest/v1/vscode_workspaces?on_conflict=id",
                method = "POST",
                body = json.encodeToString(SupabaseWorkspacePayload.serializer(), payload),
                bearerToken = session.accessToken,
                extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates,return=minimal")
            )
            if (code in 200..299) {
                return@runCatching CloudPushResult(mode = "table", updatedAt = now)
            }

            val metaBody = buildJsonObject {
                put("data", buildJsonObject {
                    put("vscode_workspace_name", workspaceName)
                    put("vscode_files", json.encodeToJsonElement(
                        kotlinx.serialization.builtins.ListSerializer(FSNode.serializer()),
                        files
                    ))
                    put("vscode_settings", json.encodeToJsonElement(EditorSettings.serializer(), settings))
                    put("vscode_synced_at", now)
                })
            }.toString()
            val (metaCode, metaResp) = request(
                url = "$baseUrl/auth/v1/user",
                method = "PUT",
                body = metaBody,
                bearerToken = session.accessToken
            )
            if (metaCode !in 200..299) {
                val errObj = runCatching { json.parseToJsonElement(metaResp).jsonObject }.getOrNull()
                val msg = errObj?.get("msg")?.jsonPrimitive?.content ?: "Cloud sync failed ($metaCode)"
                throw IllegalStateException(msg)
            }
            CloudPushResult(mode = "metadata", updatedAt = now)
        }
    }

    suspend fun pullWorkspace(
        session: SupabaseSession,
        workspaceName: String
    ): Result<CloudPullResult> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(IllegalStateException("Supabase is not configured."))
        runCatching {
            val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
            val (code, resp) = request(
                url = "$baseUrl/rest/v1/vscode_workspaces?user_id=eq.${session.userId}&order=updated_at.desc&limit=1",
                method = "GET",
                bearerToken = session.accessToken
            )
            if (code in 200..299) {
                val arr = json.parseToJsonElement(resp).jsonArray
                if (arr.isNotEmpty()) {
                    val row = arr.first().jsonObject
                    val files = json.decodeFromJsonElement(
                        kotlinx.serialization.builtins.ListSerializer(FSNode.serializer()),
                        row["files"]!!
                    )
                    val settings = row["settings"]?.let {
                        json.decodeFromJsonElement(EditorSettings.serializer(), it)
                    }
                    return@runCatching CloudPullResult(
                        mode = "table",
                        files = files,
                        settings = settings,
                        name = row["name"]?.jsonPrimitive?.content ?: workspaceName,
                        updatedAt = row["updated_at"]?.jsonPrimitive?.content
                    )
                }
            }

            val (userCode, userResp) = request(
                url = "$baseUrl/auth/v1/user",
                method = "GET",
                bearerToken = session.accessToken
            )
            if (userCode in 200..299) {
                val userObj = json.parseToJsonElement(userResp).jsonObject
                val meta = userObj["user_metadata"] as? JsonObject
                val rawFiles = meta?.get("vscode_files")
                if (rawFiles != null) {
                    val files = json.decodeFromJsonElement(
                        kotlinx.serialization.builtins.ListSerializer(FSNode.serializer()),
                        rawFiles
                    )
                    val settings = meta["vscode_settings"]?.let {
                        json.decodeFromJsonElement(EditorSettings.serializer(), it)
                    }
                    return@runCatching CloudPullResult(
                        mode = "metadata",
                        files = files,
                        settings = settings,
                        name = meta["vscode_workspace_name"]?.jsonPrimitive?.content ?: workspaceName,
                        updatedAt = meta["vscode_synced_at"]?.jsonPrimitive?.content
                    )
                }
            }
            CloudPullResult(mode = "empty")
        }
    }

    private fun request(
        url: String,
        method: String,
        body: String? = null,
        bearerToken: String? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): Pair<Int, String> {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12_000
            readTimeout = 12_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Content-Type", "application/json")
            if (!bearerToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $bearerToken")
            }
            extraHeaders.forEach { (k, v) -> setRequestProperty(k, v) }
            if (body != null) {
                doOutput = true
            }
        }
        if (body != null) {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
        conn.disconnect()
        return code to text
    }
}
