package com.teamflux.fluxai.network

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.teamflux.fluxai.data.DatabaseProvider
import com.teamflux.fluxai.data.room.AIInsightDao
import com.teamflux.fluxai.data.room.AIInsightEntity
import com.teamflux.fluxai.data.room.EmployeePerformanceDao
import com.teamflux.fluxai.data.room.CommitDataEntity
import com.teamflux.fluxai.model.AIEvaluation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

// Data structures for n8n webhook requests
@Serializable
data class N8nEmployeePerformanceRequest(
    val username: String
)

@Serializable
data class N8nTeamInsightsRequest(
    val teamName: String,
    val memberCount: Int,
    val recentPerformanceData: Map<String, String> = emptyMap()
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
    val commitDates: List<String>  // List of commit dates from the last 30 days
)

class WebhookService(context: Context) {
    private val database = DatabaseProvider.getDatabase(context)
    private val aiInsightDao: AIInsightDao = database.aiInsightDao()
    private val employeePerformanceDao: EmployeePerformanceDao = database.employeePerformanceDao()

    companion object {
        // N8n webhook endpoints
        private const val BASE_N8N_URL = "https://your-n8n-instance.com/webhook"
        private const val EMPLOYEE_PERFORMANCE_ENDPOINT = "$BASE_N8N_URL/commit-performance"
        private const val TEAM_INSIGHTS_ENDPOINT = "$BASE_N8N_URL/team-insights"
        private const val ADMIN_CHAT_ENDPOINT = "$BASE_N8N_URL/admin-chat"
        private const val EMPLOYEE_CHAT_ENDPOINT = "$BASE_N8N_URL/employee-chat"

        // Configuration
        private const val USE_WEBHOOKS = true
        const val DEBUG_WEBHOOKS = true
        private const val NETWORK_TIMEOUT = 30000

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

    suspend fun evaluateEmployeePerformanceByUsername(githubUsername: String): AIEvaluationResponse {
        return withContext(Dispatchers.IO) {
            if (USE_WEBHOOKS) {
                try {
                    val request = N8nEmployeePerformanceRequest(username = githubUsername)
                    logDebug("=== N8N EMPLOYEE PERFORMANCE EVALUATION ===\nGitHub Username: $githubUsername\nEndpoint: $EMPLOYEE_PERFORMANCE_ENDPOINT")

                    val response = sendWebhookRequest(EMPLOYEE_PERFORMANCE_ENDPOINT, json.encodeToString(request))
                    if (response != null) {
                        logDebug("N8n response received successfully")
                        return@withContext json.decodeFromString<AIEvaluationResponse>(response)
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
                    val request = N8nTeamInsightsRequest(
                        teamName = teamName,
                        memberCount = memberCount,
                        recentPerformanceData = mapOf(
                            "roles" to teamRoles.joinToString(","),
                            "team_size" to memberCount.toString(),
                            "team_name" to teamName
                        )
                    )
                    logDebug("=== N8N TEAM INSIGHTS GENERATION ===\nTeam: $teamName, Members: $memberCount\nRoles: ${teamRoles.joinToString(", ")}\nEndpoint: $TEAM_INSIGHTS_ENDPOINT")

                    val response = sendWebhookRequest(TEAM_INSIGHTS_ENDPOINT, json.encodeToString(request))
                    if (response != null) {
                        logDebug("N8n team insights received successfully")
                        return@withContext json.decodeFromString<List<String>>(response)
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

    suspend fun fetchCommitData(
        teamId: String = "",
        employeeId: String = "",
        timeframe: String = "7days",
        forceRefresh: Boolean = false
    ): CommitDataResponse {
        return withContext(Dispatchers.IO) {
            if (USE_WEBHOOKS || forceRefresh) {
                try {
                    val request = mapOf(
                        "employeeId" to employeeId,
                        "teamId" to teamId,
                        "timeframe" to timeframe,
                        "forceRefresh" to forceRefresh.toString()
                    )
                    logDebug("=== N8N COMMIT DATA FETCH ===\nEmployee: $employeeId\nTeam: $teamId\nTimeframe: $timeframe\nForce Refresh: $forceRefresh")

                    val response = sendWebhookRequest("$BASE_N8N_URL/commit-data", json.encodeToString(request))
                    if (response != null) {
                        val commitData = json.decodeFromString<CommitDataResponse>(response)
                        // Cache the commit data
                        employeePerformanceDao.insertCommitData(CommitDataEntity(
                            employeeId = employeeId,
                            commitDates = commitData.commitDates,
                            timestamp = System.currentTimeMillis()
                        ))
                        return@withContext commitData
                    }
                } catch (e: Exception) {
                    logError("N8n commit data fetch failed", e)
                }
            }

            // Try to get cached data if not forcing refresh or if webhook failed
            if (!forceRefresh) {
                try {
                    val cachedData = employeePerformanceDao.getCommitData(employeeId)
                    if (cachedData != null) {
                        return@withContext CommitDataResponse(
                            commitDates = cachedData.commitDates
                        )
                    }
                } catch (e: Exception) {
                    logError("Failed to retrieve cached commit data", e)
                }
            }

            // If force refresh failed or no cached data, return fallback
            generateCommitDataFallback(employeeId.ifEmpty { "default" })
        }
    }

    suspend fun evaluateEmployeePerformance(metrics: EmployeeMetrics): AIEvaluation {
        return withContext(Dispatchers.IO) {
            try {
                val aiResponse = evaluateEmployeePerformanceByUsername(metrics.githubUsername)
                val evaluation = AIEvaluation(
                    overallRating = aiResponse.overallRating,
                    strengths = aiResponse.strengths,
                    improvements = aiResponse.improvements,
                    recommendation = aiResponse.recommendation,
                    performanceTrend = aiResponse.performanceTrend
                )
                // Save to Room
                val entity = evaluation.toEntity(metrics.employeeId)
                aiInsightDao.insertAIInsight(entity)
                evaluation
            } catch (e: Exception) {
                logError("Employee performance evaluation failed", e)

                // Try cached evaluation
                val cachedEvaluation = try {
                    aiInsightDao.getAIInsight(metrics.employeeId)
                        .catch { emit(null) }
                        .firstOrNull()
                } catch (_: Exception) {
                    null
                }

                cachedEvaluation?.toAIEvaluation() ?: generateFallbackEvaluationFromMetrics(metrics)
            }
        }
    }

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

            connection.outputStream.use { outputStream ->
                outputStream.write(payload.toByteArray())
                outputStream.flush()
            }

            val responseCode = connection.responseCode
            logDebug("Response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.use { inputStream ->
                    inputStream.bufferedReader().readText()
                }
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
        return response.trim().let { trimmed ->
            if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                trimmed.substring(1, trimmed.length - 1)
            } else {
                trimmed
            }
        }
    }

    // === Fallback generators and helpers (unchanged from your version) ===
    // generateEmployeeFallbackEvaluation, generateTeamFallbackInsights,
    // generateAdminFallbackResponse, generateEmployeeFallbackResponse,
    // extensions toEntity(), toAIEvaluation(), logDebug(), logError()

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
        return CommitDataResponse(
            commitDates = (0..6).map { daysAgo ->
                if (random.nextBoolean()) {
                    val ts = today - (daysAgo * oneDay)
                    if (Build.VERSION.SDK_INT >= 26) {
                        java.time.Instant.ofEpochMilli(ts).toString()
                    } else {
                        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                            .format(java.util.Date(ts))
                    }
                } else ""
            }.filter { it.isNotEmpty() }
        )
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

        if (metrics.codeQualityScore >= 8.0) {
            strengths.add("High code quality standards maintained")
        } else {
            improvements.add("Focus on improving code quality and review practices")
        }

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

    private fun AIEvaluation.toEntity(employeeId: String) = AIInsightEntity(
        employeeId = employeeId,
        overallRating = overallRating,
        strengths = strengths,
        improvements = improvements,
        recommendation = recommendation,
        performanceTrend = performanceTrend
    )

    private fun AIInsightEntity.toAIEvaluation() = AIEvaluation(
        overallRating = overallRating,
        strengths = strengths,
        improvements = improvements,
        recommendation = recommendation,
        performanceTrend = performanceTrend
    )

    private fun logDebug(message: String) {
        if (DEBUG_WEBHOOKS) {
            android.util.Log.d("WebhookService", message)
        }
    }

    private fun logError(message: String, throwable: Throwable? = null) {
        android.util.Log.e("WebhookService", message, throwable)
    }
}
