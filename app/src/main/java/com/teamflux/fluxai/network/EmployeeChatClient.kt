package com.teamflux.fluxai.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

@Serializable
private data class EmployeeChatRequest(
    val message: String,
    val username: String = "",
    val context: Map<String, String> = emptyMap()
)

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
private const val EMPLOYEE_CHAT_ENDPOINT = "http://10.0.2.2:5678/webhook/employee-chat"
private const val NETWORK_TIMEOUT = 30000

suspend fun postEmployeeChat(message: String, username: String = "", context: Map<String, String> = emptyMap()): String {
    return withContext(Dispatchers.IO) {
        try {
            val requestBody = EmployeeChatRequest(message = message, username = username, context = context + mapOf("userType" to "Employee"))
            Log.d("EmployeeChatClient", "Posting to $EMPLOYEE_CHAT_ENDPOINT message='${message.take(80)}'")

            val connection = (URL(EMPLOYEE_CHAT_ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "FluxAI-Android/1.0")
                doOutput = true
                connectTimeout = NETWORK_TIMEOUT
                readTimeout = NETWORK_TIMEOUT
            }

            connection.outputStream.use { out ->
                out.write(json.encodeToString(requestBody).toByteArray())
                out.flush()
            }

            val code = connection.responseCode
            Log.d("EmployeeChatClient", "Response code: $code")
            if (code == HttpURLConnection.HTTP_OK) {
                val resp = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("EmployeeChatClient", "Response (preview): ${resp.take(200)}")
                return@withContext resp.trim().let { t -> if (t.startsWith("\"") && t.endsWith("\"")) t.substring(1, t.length - 1) else t }
            } else {
                val err = connection.errorStream?.bufferedReader()?.readText() ?: "No error details"
                Log.e("EmployeeChatClient", "HTTP $code: $err")
                throw IllegalStateException("Employee chat webhook HTTP $code: $err")
            }
        } catch (e: Exception) {
            Log.e("EmployeeChatClient", "Network request failed", e)
            throw e
        }
    }
}

