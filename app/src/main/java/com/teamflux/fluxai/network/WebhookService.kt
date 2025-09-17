package com.teamflux.fluxai.network

import android.content.Context
import android.os.Build
import android.util.Log
import com.teamflux.fluxai.model.AIEvaluation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

// Data structures for n8n webhook requests
@Serializable
data class N8nEmployeePerformanceRequest(
    val username: String
)

@Serializable
data class N8nChatRequest(
    val message: String,
    val username: String = "",
    val context: Map<String, String> = emptyMap()
)

// Response data structures
@Serializable
data class AIEvaluationResponse(
    val overallRating: String,
    val strengths: List<String>,
    val improvements: List<String>,
    val recommendation: String,
    val performanceTrend: String
)

@Serializable
data class CommitDataResponse(
    val commitDates: List<String>
)

@Serializable
data class CombinedEmployeePayload(
    val output: AIEvaluationResponse? = null,
    val date: List<String>? = null,
    val collaborationScore: Double? = null,
    val productivityScore: Double? = null
)

data class CombinedSummary(
    val evaluation: AIEvaluation?,
    val commitDates: List<String>,
    val collaborationScore: Double?,
    val productivityScore: Double?
)

class WebhookService(context: Context) {
    companion object {
        private const val EMPLOYEE_PERFORMANCE_ENDPOINT = "http://10.0.2.2:5678/webhook/commit-performance"
        private const val TEAM_INSIGHTS_ENDPOINT = "http://10.0.2.2:5678/webhook/team-insights"
        private const val ADMIN_CHAT_ENDPOINT = "http://10.0.2.2:5678/webhook/admin-chat"
        private const val EMPLOYEE_CHAT_ENDPOINT = "http://10.0.2.2:5678/webhook/employee-chat"

        private const val USE_WEBHOOKS = true
        const val DEBUG_WEBHOOKS = true
        private const val NETWORK_TIMEOUT = 30000

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

    // ===== Public API =====
    suspend fun evaluateEmployeePerformanceByUsername(githubUsername: String): AIEvaluationResponse {
        return withContext(Dispatchers.IO) {
            if (USE_WEBHOOKS) {
                try {
                    val request = N8nEmployeePerformanceRequest(username = githubUsername)
                    logDebug("=== N8N EMPLOYEE PERFORMANCE EVALUATION ===\nGitHub Username: $githubUsername\nEndpoint: $EMPLOYEE_PERFORMANCE_ENDPOINT")

                    val response = sendWebhookRequest(EMPLOYEE_PERFORMANCE_ENDPOINT, json.encodeToString(request))
                    if (response != null) {
                        logDebug("Raw evaluation response: ${response.take(300)}")
                        parseEvaluation(response)?.let { return@withContext it }
                    }
                } catch (e: Exception) {
                    logError("N8n employee evaluation failed", e)
                }
            }
            generateEmployeeFallbackEvaluation(githubUsername)
        }
    }

    suspend fun generateTeamInsights(teamName: String, memberCount: Int, teamRoles: List<String> = emptyList()): List<String> {
        return withContext(Dispatchers.IO) {
            if (USE_WEBHOOKS) {
                try {
                    val payload = JSONObject().apply {
                        put("teamName", teamName)
                        put("memberCount", memberCount)
                        put("roles", teamRoles.joinToString(","))
                    }.toString()

                    logDebug("=== N8N TEAM INSIGHTS GENERATION ===\nTeam: $teamName, Members: $memberCount\nRoles: ${teamRoles.joinToString(", ")}\nEndpoint: $TEAM_INSIGHTS_ENDPOINT")

                    val response = sendWebhookRequest(TEAM_INSIGHTS_ENDPOINT, payload)
                    if (!response.isNullOrBlank()) {
                        // 1) Direct decode to List<String>
                        runCatching { json.decodeFromString<List<String>>(response) }
                            .getOrNull()?.let { return@withContext it }

                        val cleaned = cleanTextResponse(response)
                        runCatching { json.decodeFromString<List<String>>(cleaned) }
                            .getOrNull()?.let { return@withContext it }

                        // 2) Parse JSON element to support multiple shapes
                        val root = runCatching { json.parseToJsonElement(cleaned) }.getOrNull()
                        when (root) {
                            is JsonArray -> {
                                // a) Array of strings
                                val strings = root.mapNotNull { (it as? JsonPrimitive)?.content }
                                if (strings.isNotEmpty()) return@withContext strings

                                // b) Array of objects (e.g., [{"result":[..]}, {"text":"[ ... ]"}])
                                val aggregated = mutableListOf<String>()
                                root.forEach { el ->
                                    val obj = el as? JsonObject ?: return@forEach
                                    // Prefer "result" array
                                    val resultArr = obj["result"] as? JsonArray
                                    if (resultArr != null) {
                                        aggregated += resultArr.mapNotNull { it.jsonPrimitive.content }
                                    } else {
                                        // "result" as JSON-array string
                                        val resultStr = obj["result"]?.jsonPrimitive?.content
                                        if (!resultStr.isNullOrBlank()) {
                                            runCatching { json.decodeFromString<List<String>>(cleanTextResponse(resultStr)) }
                                                .getOrNull()?.let { aggregated += it }
                                        }
                                    }
                                    // Legacy: "text" carries JSON-array string
                                    val textStr = obj["text"]?.jsonPrimitive?.content
                                    if (!textStr.isNullOrBlank()) {
                                        runCatching { json.decodeFromString<List<String>>(cleanTextResponse(textStr)) }
                                            .getOrNull()?.let { aggregated += it }
                                    }
                                }
                                if (aggregated.isNotEmpty()) return@withContext aggregated
                            }
                            is JsonObject -> {
                                // c) Object with one of keys -> array or JSON-array string
                                val keys = listOf("result", "insights", "data", "items", "text")
                                for (k in keys) {
                                    val arr = root[k] as? JsonArray
                                    if (arr != null) {
                                        val list = arr.mapNotNull { it.jsonPrimitive.content }
                                        if (list.isNotEmpty()) return@withContext list
                                    }
                                    val txt = root[k]?.jsonPrimitive?.content
                                    if (!txt.isNullOrBlank()) {
                                        runCatching { json.decodeFromString<List<String>>(cleanTextResponse(txt)) }
                                            .getOrNull()?.let { return@withContext it }
                                    }
                                }
                                // d) Last resort: take primitive values
                                val values = root.values.mapNotNull { (it as? JsonPrimitive)?.content }
                                if (values.isNotEmpty()) return@withContext values
                            }
                            else -> {}
                        }

                        // 3) Plain text fallback: split by lines
                        val lines = cleaned.lines().map { it.trim() }.filter { it.isNotBlank() }
                        if (lines.size > 1) return@withContext lines
                    }
                } catch (e: Exception) {
                    logError("N8n team insights failed", e)
                }
            }
            generateTeamFallbackInsights(teamName, memberCount, teamRoles)
        }
    }

    suspend fun getAdminChatResponse(message: String, context: Map<String, String> = emptyMap()): String {
        return withContext(Dispatchers.IO) {
            if (USE_WEBHOOKS) {
                try {
                    val request = N8nChatRequest(
                        message = message,
                        context = context + mapOf("userType" to "Admin")
                    )
                    logDebug("=== N8N ADMIN CHAT ===\nMessage: $message\nEndpoint: $ADMIN_CHAT_ENDPOINT")

                    val response = sendWebhookRequest(ADMIN_CHAT_ENDPOINT, json.encodeToString(request))
                    if (response != null) {
                        logDebug("N8n admin chat response received")
                        return@withContext cleanTextResponse(response)
                    }
                } catch (e: Exception) {
                    logError("N8n admin chat failed", e)
                }
            }
            generateAdminFallbackResponse(message)
        }
    }

    suspend fun getEmployeeChatResponse(
        message: String,
        username: String = "",
        context: Map<String, String> = emptyMap()
    ): String {
        return withContext(Dispatchers.IO) {
            val request = N8nChatRequest(
                message = message,
                username = username,
                context = context + mapOf("userType" to "Employee")
            )
            logDebug("=== N8N EMPLOYEE CHAT ===\nUsername: $username\nMessage: $message\nEndpoint: $EMPLOYEE_CHAT_ENDPOINT")

            val response = sendWebhookRequest(EMPLOYEE_CHAT_ENDPOINT, json.encodeToString(request))
            if (response != null) {
                logDebug("N8n employee chat response received")
                return@withContext cleanTextResponse(response)
            }
            throw IllegalStateException("Employee chat webhook did not return a response")
        }
    }

    // Fetch both evaluation (output) and commit dates in one call using only { username }
    suspend fun fetchCombinedEmployeeSummary(username: String): CombinedSummary {
        return withContext(Dispatchers.IO) {
            val payload = json.encodeToString(N8nEmployeePerformanceRequest(username = username))
            logDebug("=== N8N COMBINED EMPLOYEE SUMMARY ===\nGitHub Username: $username\nEndpoint: $EMPLOYEE_PERFORMANCE_ENDPOINT")
            val response = sendWebhookRequest(EMPLOYEE_PERFORMANCE_ENDPOINT, payload)
            if (response.isNullOrBlank()) return@withContext CombinedSummary(null, emptyList(), null, null)

            val root = runCatching { json.parseToJsonElement(response) }.getOrNull()
            when (root) {
                is JsonArray -> {
                    // Supports: [{"output":[{"output":{... with scores}}]}]
                    var eval: AIEvaluation? = null
                    var collab: Double? = null
                    var prod: Double? = null
                    var dates: List<String> = emptyList()
                    root.forEach { el ->
                        val obj = el as? JsonObject ?: return@forEach
                        val outEl = obj["output"]
                        if (outEl is JsonArray) {
                            val first = outEl.firstOrNull() as? JsonObject
                            val nestedOut = (first?.get("output") as? JsonObject) ?: first
                            if (nestedOut != null) {
                                val ar = runCatching { json.decodeFromJsonElement(AIEvaluationResponse.serializer(), nestedOut) }.getOrNull()
                                if (ar != null) {
                                    eval = AIEvaluation(ar.overallRating, ar.strengths, ar.improvements, ar.recommendation, ar.performanceTrend)
                                    collab = nestedOut["collaborationScore"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: collab
                                    prod = nestedOut["productivityScore"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: prod
                                }
                            }
                        } else if (outEl is JsonObject) {
                            val ar = runCatching { json.decodeFromJsonElement(AIEvaluationResponse.serializer(), outEl) }.getOrNull()
                            if (ar != null) {
                                eval = AIEvaluation(ar.overallRating, ar.strengths, ar.improvements, ar.recommendation, ar.performanceTrend)
                                collab = outEl["collaborationScore"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: collab
                                prod = outEl["productivityScore"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: prod
                            }
                        }
                        val d = (obj["date"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
                        if (d.isNotEmpty()) dates = d
                    }
                    CombinedSummary(eval, dates, collab, prod)
                }
                is JsonObject -> {
                    val outEl = root["output"]
                    var eval: AIEvaluation? = null
                    var collab: Double? = null
                    var prod: Double? = null
                    if (outEl is JsonArray) {
                        val first = outEl.firstOrNull() as? JsonObject
                        val nestedOut = (first?.get("output") as? JsonObject) ?: first
                        if (nestedOut != null) {
                            val ar = runCatching { json.decodeFromJsonElement(AIEvaluationResponse.serializer(), nestedOut) }.getOrNull()
                            if (ar != null) {
                                eval = AIEvaluation(ar.overallRating, ar.strengths, ar.improvements, ar.recommendation, ar.performanceTrend)
                                collab = nestedOut["collaborationScore"]?.jsonPrimitive?.content?.toDoubleOrNull()
                                prod = nestedOut["productivityScore"]?.jsonPrimitive?.content?.toDoubleOrNull()
                            }
                        }
                    } else if (outEl is JsonObject) {
                        val ar = runCatching { json.decodeFromJsonElement(AIEvaluationResponse.serializer(), outEl) }.getOrNull()
                        if (ar != null) {
                            eval = AIEvaluation(ar.overallRating, ar.strengths, ar.improvements, ar.recommendation, ar.performanceTrend)
                            collab = outEl["collaborationScore"]?.jsonPrimitive?.content?.toDoubleOrNull()
                            prod = outEl["productivityScore"]?.jsonPrimitive?.content?.toDoubleOrNull()
                        }
                    }
                    val dates = (root["date"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
                    CombinedSummary(eval, dates, collab, prod)
                }
                else -> {
                    val eval = parseEvaluation(response)?.let { o ->
                        AIEvaluation(
                            overallRating = o.overallRating,
                            strengths = o.strengths,
                            improvements = o.improvements,
                            recommendation = o.recommendation,
                            performanceTrend = o.performanceTrend
                        )
                    }
                    CombinedSummary(eval, extractCommitDates(response), null, null)
                }
            }
        }
    }

    suspend fun evaluateEmployeePerformance(metrics: EmployeeMetrics): AIEvaluation {
        return withContext(Dispatchers.IO) {
            try {
                val ai = evaluateEmployeePerformanceByUsername(metrics.githubUsername)
                AIEvaluation(
                    overallRating = ai.overallRating,
                    strengths = ai.strengths,
                    improvements = ai.improvements,
                    recommendation = ai.recommendation,
                    performanceTrend = ai.performanceTrend
                )
            } catch (e: Exception) {
                logError("Employee performance evaluation failed", e)
                generateFallbackEvaluationFromMetrics(metrics)
            }
        }
    }

    // ===== Networking =====
    private fun sendWebhookRequest(url: String, payload: String): String? {
        return try {
            logDebug("Sending request to: $url\nPayload size: ${payload.length} characters")
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "FluxAI-Android/1.0")
                doOutput = true
                connectTimeout = NETWORK_TIMEOUT
                readTimeout = NETWORK_TIMEOUT
            }

            connection.outputStream.use { os ->
                os.write(payload.toByteArray())
                os.flush()
            }

            val responseCode = connection.responseCode
            logDebug("Response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.use { it.bufferedReader().readText() }
                logDebug("Response received: ${responseText.take(200)}...")
                responseText
            } else {
                val errorText = connection.errorStream?.bufferedReader()?.readText() ?: "No error details"
                logError("HTTP error $responseCode: $errorText")
                null
            }
        } catch (e: Exception) {
            logError("Network request failed", e)
            null
        }
    }

    private fun cleanTextResponse(response: String): String {
        val trimmed = response.trim()
        return if (trimmed.startsWith('"') && trimmed.endsWith('"') && trimmed.length >= 2) {
            trimmed.substring(1, trimmed.length - 1)
        } else trimmed
    }

    // ===== Parsing helpers =====
    private fun parseEvaluation(response: String): AIEvaluationResponse? {
        // 1) Direct object
        runCatching { json.decodeFromString(AIEvaluationResponse.serializer(), response) }
            .getOrNull()?.let { if (it.overallRating.isNotBlank()) return it }

        // 2) Combined wrapper
        runCatching { json.decodeFromString(CombinedEmployeePayload.serializer(), response) }
            .getOrNull()?.output?.let { return it }

        // 3) Inspect JSON element (object/array)
        val root = runCatching { json.parseToJsonElement(response) }.getOrNull()
        when (root) {
            is JsonObject -> {
                val outElem = root["output"]
                if (outElem != null) {
                    return runCatching { json.decodeFromJsonElement(AIEvaluationResponse.serializer(), outElem) }.getOrNull()
                }
            }
            is JsonArray -> {
                // Look for first object with output
                root.firstOrNull { it is JsonObject && it["output"] != null }?.let { elem ->
                    val obj = elem as JsonObject
                    val outElem = obj["output"]
                    if (outElem != null) {
                        return runCatching { json.decodeFromJsonElement(AIEvaluationResponse.serializer(), outElem) }.getOrNull()
                    }
                }
                // Or maybe array of AIEvaluationResponse
                root.firstOrNull()?.let { first ->
                    return runCatching { json.decodeFromJsonElement(AIEvaluationResponse.serializer(), first) }.getOrNull()
                }
            }
            else -> {}
        }
        return null
    }

    private fun extractCommitDates(response: String): List<String> {
        // 1) Direct expected shape
        runCatching { json.decodeFromString(CommitDataResponse.serializer(), response) }
            .getOrNull()?.commitDates?.takeIf { it.isNotEmpty() }?.let { return it }

        // 2) Combined wrapper { date: [...] }
        runCatching { json.decodeFromString(CombinedEmployeePayload.serializer(), response) }
            .getOrNull()?.date?.takeIf { it.isNotEmpty() }?.let { return it }

        // 3) Inspect JSON element
        val root = runCatching { json.parseToJsonElement(response) }.getOrNull()
        when (root) {
            is JsonObject -> {
                val keys = listOf("date", "commitDates", "dates", "commits")
                for (k in keys) {
                    val arr = root[k] as? JsonArray
                    if (arr != null && arr.isNotEmpty()) {
                        return arr.mapNotNull { it.jsonPrimitive.content }
                    }
                }
            }
            is JsonArray -> {
                val first = root.firstOrNull()
                val obj = first as? JsonObject
                val dates = obj?.get("date") as? JsonArray
                if (dates != null && dates.isNotEmpty()) {
                    return dates.mapNotNull { it.jsonPrimitive.content }
                }
                // Or array of strings
                if (root.isNotEmpty() && root.all { it !is JsonObject }) {
                    return root.mapNotNull { it.jsonPrimitive.content }
                }
            }
            else -> {}
        }
        return emptyList()
    }

    // ===== Fallback Generators =====
    private fun generateEmployeeFallbackEvaluation(githubUsername: String): AIEvaluationResponse {
        logDebug("Generating fallback evaluation for: $githubUsername")
        val random = Random(githubUsername.hashCode())
        val performanceLevel = random.nextInt(4)
        val ratings = listOf("Needs Improvement", "Average", "Good", "Excellent")
        val rating = ratings[performanceLevel]

        val allStrengths = listOf(
            "Consistent commit frequency showing reliable development rhythm",
            "High code quality standards with clean, maintainable code",
            "Strong collaboration in code reviews and team discussions",
            "Reliable task completion with attention to requirements",
            "Good problem-solving approach and technical decision-making",
            "Active participation in team knowledge sharing",
            "Well-documented code practices enhancing team productivity"
        )

        val allImprovements = listOf(
            "Increase daily commit consistency for better project tracking",
            "Enhance code documentation for improved maintainability",
            "Improve test coverage to strengthen code reliability",
            "Participate more actively in code reviews and discussions",
            "Focus on performance optimization in development practices",
            "Strengthen error handling and edge case considerations"
        )

        val strengths = allStrengths.shuffled(random).take(2 + performanceLevel)
        val improvements = allImprovements.shuffled(random).take(maxOf(1, 3 - performanceLevel))

        return AIEvaluationResponse(
            overallRating = rating,
            strengths = strengths,
            improvements = improvements,
            recommendation = when (rating) {
                "Excellent" -> "Consider mentoring junior developers and leading technical initiatives"
                "Good" -> "Continue current practices while focusing on identified improvement areas"
                "Average" -> "Develop a structured improvement plan with regular check-ins"
                else -> "Requires focused development support and clear performance goals"
            },
            performanceTrend = listOf("Improving", "Stable", "Declining").random(random)
        )
    }

    private fun generateTeamFallbackInsights(teamName: String, memberCount: Int, teamRoles: List<String>): List<String> {
        logDebug("Generating fallback insights for team: $teamName")
        val insights = mutableListOf<String>()

        when {
            memberCount <= 3 -> {
                insights.add("Small team size enables rapid decision-making and close collaboration")
                insights.add("Consider cross-training to reduce single points of failure")
            }
            memberCount <= 8 -> {
                insights.add("Optimal team size for agile development and effective communication")
                insights.add("Good balance between collaboration and individual contribution")
            }
            else -> {
                insights.add("Large team size provides good redundancy and specialization opportunities")
                insights.add("Consider sub-team structures to maintain effective communication")
            }
        }

        if (teamRoles.isNotEmpty()) {
            val hasQA = teamRoles.any { it.lowercase().contains("qa") || it.lowercase().contains("test") }
            val hasDevOps = teamRoles.any { it.lowercase().contains("devops") || it.lowercase().contains("ops") }

            if (!hasQA && memberCount > 3) {
                insights.add("Consider adding dedicated QA resources to improve testing coverage")
            }
            if (!hasDevOps && memberCount > 5) {
                insights.add("DevOps expertise could streamline deployment and infrastructure management")
            }
        }

        val additionalInsights = listOf(
            "Regular retrospectives help maintain team velocity and satisfaction",
            "Code review practices directly impact both quality and knowledge sharing",
            "Clear communication channels are essential for distributed team success"
        )

        insights.addAll(additionalInsights.shuffled().take(2))
        return insights.take(5)
    }

    private fun generateAdminFallbackResponse(message: String): String {
        val lowercaseMessage = message.lowercase()
        return when {
            lowercaseMessage.contains("performance") || lowercaseMessage.contains("analytics") ->
                "Team performance metrics show positive trends. Current velocity and code quality indicators suggest good productivity levels across team members."
            lowercaseMessage.contains("team") || lowercaseMessage.contains("collaboration") ->
                "Team dynamics analysis indicates strong collaboration patterns. Consider implementing regular knowledge-sharing sessions to further enhance team cohesion."
            lowercaseMessage.contains("productivity") || lowercaseMessage.contains("efficiency") ->
                "Productivity metrics are within expected ranges. Focus areas include optimizing code review cycles and reducing deployment friction."
            lowercaseMessage.contains("quality") || lowercaseMessage.contains("review") ->
                "Code quality standards are being maintained well. Consider automated quality gates to further streamline the review process."
            else ->
                "I can help with team analytics, performance insights, resource planning, and management recommendations. What specific area interests you?"
        }
    }

    private fun generateEmployeeFallbackResponse(message: String, githubUsername: String, context: Map<String, String>): String {
        val lowercaseMessage = message.lowercase()
        return when {
            lowercaseMessage.contains("performance") || lowercaseMessage.contains("progress") ->
                "Your development activity shows consistent patterns. Focus on maintaining code quality while gradually increasing your contribution velocity."
            lowercaseMessage.contains("skill") || lowercaseMessage.contains("learn") ->
                "Based on current trends, consider exploring advanced testing frameworks and architectural patterns to enhance your technical toolkit."
            lowercaseMessage.contains("career") || lowercaseMessage.contains("growth") ->
                "Your consistent contributions demonstrate growth potential. Consider taking on code review responsibilities and mentoring opportunities."
            lowercaseMessage.contains("team") || lowercaseMessage.contains("collaboration") ->
                "Effective team collaboration includes active participation in code reviews and clear communication in pull requests."
            else ->
                "I'm here to help with your professional development, coding practices, and career growth. What would you like to focus on?"
        }
    }

    private fun generateCommitDataFallback(employeeId: String): CommitDataResponse {
        val random = Random(employeeId.hashCode())
        val today = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L
        val dates = (0 until 14).mapNotNull { daysAgo ->
            if (random.nextBoolean()) {
                val ts = today - daysAgo * oneDay
                if (Build.VERSION.SDK_INT >= 26) {
                    java.time.Instant.ofEpochMilli(ts).toString()
                } else {
                    val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                    fmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    fmt.format(java.util.Date(ts))
                }
            } else null
        }
        return CommitDataResponse(dates)
    }

    private fun generateFallbackEvaluationFromMetrics(metrics: EmployeeMetrics): AIEvaluation {
        val performanceLevel = when {
            metrics.productivityScore >= 8.0 -> 3
            metrics.productivityScore >= 6.0 -> 2
            metrics.productivityScore >= 4.0 -> 1
            else -> 0
        }

        val ratings = listOf("Needs Improvement", "Average", "Good", "Excellent")
        val rating = ratings[performanceLevel]

        val strengths = mutableListOf<String>()
        val improvements = mutableListOf<String>()

        if (metrics.collaborationScore >= 8.0) {
            strengths.add("Excellent collaboration and teamwork")
        } else {
            improvements.add("Enhance team collaboration and communication")
        }

        if (metrics.commits >= 15) {
            strengths.add("Consistent development activity with regular commits")
        } else {
            improvements.add("Increase commit frequency for better project tracking")
        }

        if (metrics.attendanceRate >= 0.95) {
            strengths.add("Excellent attendance and reliability")
        } else if (metrics.attendanceRate < 0.9) {
            improvements.add("Improve attendance consistency")
        }

        val recommendation = when (performanceLevel) {
            3 -> "Continue excellent work and consider mentoring opportunities"
            2 -> "Good performance with room for growth in identified areas"
            1 -> "Focus on improvement areas with regular check-ins"
            else -> "Requires immediate attention and support for improvement"
        }

        return AIEvaluation(
            overallRating = rating,
            strengths = strengths,
            improvements = improvements,
            recommendation = recommendation,
            performanceTrend = listOf("Improving", "Stable", "Declining").random()
        )
    }

    private fun logDebug(message: String) {
        if (DEBUG_WEBHOOKS) Log.d("WebhookService", message)
    }

    private fun logError(message: String, throwable: Throwable? = null) {
        Log.e("WebhookService", message, throwable)
    }
}
