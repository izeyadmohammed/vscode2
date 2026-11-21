package com.example.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.example.model.DebugLogEntry
import com.example.model.DebugLogType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

object JsSandboxRunner {
    private val importStripRegex = Regex("""^\s*import\s+.*?;?\s*$""", RegexOption.MULTILINE)
    private val exportStripRegex = Regex("""^\s*export\s+(default\s+)?""", RegexOption.MULTILINE)

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun executeScript(context: Context, rawSource: String): List<DebugLogEntry> =
        withContext(Dispatchers.Main) {
            val runnable = rawSource
                .replace(importStripRegex, "")
                .replace(exportStripRegex, "")
            val deferred = CompletableDeferred<List<DebugLogEntry>>()
            val webView = WebView(context.applicationContext)
            webView.settings.javaScriptEnabled = true

            val bridge = object {
                @JavascriptInterface
                fun report(payloadJson: String) {
                    val entries = parseEntries(payloadJson)
                    Handler(Looper.getMainLooper()).post {
                        if (!deferred.isCompleted) deferred.complete(entries)
                    }
                }
            }
            webView.addJavascriptInterface(bridge, "StudioBridge")

            val escapedCode = JSONObject.quote(runnable)
            val script = """
                (function() {
                    var __logs = [];
                    var __fmt = function(a) {
                        try { return typeof a === 'object' ? JSON.stringify(a) : String(a); }
                        catch (e) { return String(a); }
                    };
                    var __console = {
                        log: function() {
                            var parts = [];
                            for (var i = 0; i < arguments.length; i++) parts.push(__fmt(arguments[i]));
                            __logs.push({ type: 'log', text: parts.join(' ') });
                        },
                        warn: function() {
                            var parts = [];
                            for (var i = 0; i < arguments.length; i++) parts.push(__fmt(arguments[i]));
                            __logs.push({ type: 'log', text: '⚠ ' + parts.join(' ') });
                        },
                        error: function() {
                            var parts = [];
                            for (var i = 0; i < arguments.length; i++) parts.push(__fmt(arguments[i]));
                            __logs.push({ type: 'error', text: '✖ ' + parts.join(' ') });
                        }
                    };
                    try {
                        var __fn = new Function('console', $escapedCode);
                        __fn(__console);
                    } catch (err) {
                        __logs.push({ type: 'error', text: (err && err.name ? err.name + ': ' : '') + (err && err.message ? err.message : String(err)) });
                    }
                    StudioBridge.report(JSON.stringify(__logs));
                })();
            """.trimIndent()

            webView.loadDataWithBaseURL("about:blank", "<html><body></body></html>", "text/html", "UTF-8", null)
            webView.evaluateJavascript(script, null)

            val result = withTimeoutOrNull(4000L) { deferred.await() }
                ?: listOf(
                    DebugLogEntry(
                        id = "timeout-${System.currentTimeMillis()}",
                        type = DebugLogType.ERROR,
                        text = "Script execution timed out."
                    )
                )
            webView.destroy()
            result
        }

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun evaluateExpression(context: Context, expr: String): DebugLogEntry =
        withContext(Dispatchers.Main) {
            val deferred = CompletableDeferred<DebugLogEntry>()
            val webView = WebView(context.applicationContext)
            webView.settings.javaScriptEnabled = true

            val bridge = object {
                @JavascriptInterface
                fun report(isError: Boolean, text: String) {
                    Handler(Looper.getMainLooper()).post {
                        if (!deferred.isCompleted) {
                            deferred.complete(
                                DebugLogEntry(
                                    id = "d-out-${System.currentTimeMillis()}",
                                    type = if (isError) DebugLogType.ERROR else DebugLogType.RESULT,
                                    text = text
                                )
                            )
                        }
                    }
                }
            }
            webView.addJavascriptInterface(bridge, "StudioBridge")

            val escapedExpr = JSONObject.quote(expr)
            val script = """
                (function() {
                    try {
                        var __res = (0, eval)($escapedExpr);
                        var __txt = typeof __res === 'object' ? JSON.stringify(__res) : String(__res);
                        StudioBridge.report(false, __txt);
                    } catch (err) {
                        StudioBridge.report(true, err && err.message ? err.message : String(err));
                    }
                })();
            """.trimIndent()
            webView.loadDataWithBaseURL("about:blank", "<html><body></body></html>", "text/html", "UTF-8", null)
            webView.evaluateJavascript(script, null)

            val result = withTimeoutOrNull(3000L) { deferred.await() }
                ?: DebugLogEntry(
                    id = "d-err-${System.currentTimeMillis()}",
                    type = DebugLogType.ERROR,
                    text = "Evaluation timed out."
                )
            webView.destroy()
            result
        }

    private fun parseEntries(payloadJson: String): List<DebugLogEntry> {
        val out = mutableListOf<DebugLogEntry>()
        val arr = runCatching { JSONArray(payloadJson) }.getOrNull() ?: return out
        val now = System.currentTimeMillis()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val typeStr = obj.optString("type", "log")
            val text = obj.optString("text", "")
            out.add(
                DebugLogEntry(
                    id = "run-$now-$i",
                    type = when (typeStr) {
                        "error" -> DebugLogType.ERROR
                        "result" -> DebugLogType.RESULT
                        else -> DebugLogType.LOG
                    },
                    text = text
                )
            )
        }
        return out
    }
}
