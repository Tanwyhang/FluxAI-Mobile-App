package com.teamflux.fluxai.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

@Serializable
private data class ChatRequest(
    val message: String,
    val username: String = "",
    val context: Map<String, String> = emptyMap()
)

@Serializable
private data class ChatEnvelope(
    val sessionId: String,
    val body: ChatRequest
)

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
private const val EMPLOYEE_CHAT_ENDPOINT = "http://10.0.2.2:5678/webhook/employee-chat"
private const val ADMIN_CHAT_ENDPOINT = "http://10.0.2.2:5678/webhook/admin-chat"
private const val NETWORK_TIMEOUT = 30000

suspend fun postEmployeeChat(
    sessionId: String,
    message: String,
    username: String = "",
    context: Map<String, String> = emptyMap()
): String {
    return postChat(
        url = EMPLOYEE_CHAT_ENDPOINT,
        sessionId = sessionId,
        message = message,
        username = username,
        context = context + mapOf("userType" to "Employee"),
        tag = "EmployeeChatClient"
    )
}

suspend fun postAdminChat(
    sessionId: String,
    message: String,
    username: String = "",
    context: Map<String, String> = emptyMap()
): String {
    return postChat(
        url = ADMIN_CHAT_ENDPOINT,
        sessionId = sessionId,
        message = message,
        username = username,
        context = context + mapOf("userType" to "Admin"),
        tag = "AdminChatClient"
    )
}

private suspend fun postChat(
    url: String,
    sessionId: String,
    message: String,
    username: String,
    context: Map<String, String>,
    tag: String
): String {
    return withContext(Dispatchers.IO) {
        try {
            val envelope = ChatEnvelope(
                sessionId = sessionId,
                body = ChatRequest(message = message, username = username, context = context)
            )
            val payload = json.encodeToString(envelope)
            Log.d(tag, "Posting to $url sessionId='$sessionId' message='${message.take(120)}'")

            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                // Broader accept to avoid HTML wrappers from some proxies
                setRequestProperty("Accept", "application/json, text/plain, */*")
                setRequestProperty("User-Agent", "FluxAI-Android/1.0")
                doOutput = true
                connectTimeout = NETWORK_TIMEOUT
                readTimeout = NETWORK_TIMEOUT
            }

            connection.outputStream.use { out ->
                out.write(payload.toByteArray())
                out.flush()
            }

            val code = connection.responseCode
            Log.d(tag, "Response code: $code")
            if (code == HttpURLConnection.HTTP_OK) {
                val raw = connection.inputStream.bufferedReader().use { it.readText() }
                val text = parseChatResponse(raw)
                Log.d(tag, "Response (preview): ${text.take(200)}")
                text
            } else {
                val err = connection.errorStream?.bufferedReader()?.readText() ?: "No error details"
                Log.e(tag, "HTTP $code: $err")
                throw IllegalStateException("Chat webhook HTTP $code: $err")
            }
        } catch (e: Exception) {
            Log.e(tag, "Network request failed", e)
            throw e
        }
    }
}

private fun parseChatResponse(raw: String): String {
    val trimmed = raw.trim()

    // If it's a bare JSON string (e.g. "hello\n") remove quotes
    if ((trimmed.startsWith('"') && trimmed.endsWith('"')) && trimmed.length >= 2) {
        return unescape(trimmed.substring(1, trimmed.length - 1))
    }

    // Try JSON parsing for { "output": ... } or similar
    val element = runCatching { json.parseToJsonElement(trimmed) }.getOrNull()
    if (element != null) {
        when (element) {
            is JsonObject -> {
                // Common keys to check
                val keys = listOf("output", "text", "message", "reply", "data")
                for (k in keys) {
                    val el = element[k]
                    when (el) {
                        null -> { /* key not present, continue */ }
                        is JsonPrimitive -> return unescape(el.content)
                        is JsonArray -> {
                            val joined = el.mapNotNull { (it as? JsonPrimitive)?.content ?: it.toString() }
                                .filter { it.isNotBlank() }
                                .joinToString("\n")
                            if (joined.isNotBlank()) return unescape(joined)
                        }
                        is JsonObject -> {
                            // Nested { text: "..." }
                            val inner = el["text"] ?: el["output"] ?: el["message"]
                            if (inner is JsonPrimitive) return unescape(inner.content)
                        }
                        else -> { /* ignore other element types */ }
                    }
                }
                // Fallback: first primitive value
                element.values.firstOrNull { it is JsonPrimitive }?.let { p ->
                    return unescape((p as JsonPrimitive).content)
                }
            }
            is JsonArray -> {
                // Join array items into paragraphs
                val joined = element.mapNotNull { (it as? JsonPrimitive)?.content ?: it.toString() }
                    .filter { it.isNotBlank() }
                    .joinToString("\n")
                if (joined.isNotBlank()) return unescape(joined)
            }
            is JsonPrimitive -> {
                return unescape(element.content)
            }
            else -> {
                // Unknown JsonElement subtype (e.g., JsonNull in older APIs)
                return unescape(trimmed)
            }
        }
    }

    // Plain text fallback, also unescape common sequences
    return unescape(trimmed)
}

private fun unescape(s: String): String {
    // Replace common escaped sequences; if input was JSON content, this results in readable text
    return s
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
        .trim()
}
