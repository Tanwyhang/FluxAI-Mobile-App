package com.teamflux.fluxai.repository

import com.teamflux.fluxai.model.AIEvaluation
import com.teamflux.fluxai.model.EmployeePerformance
import com.teamflux.fluxai.network.EmployeeMetrics
import com.teamflux.fluxai.network.WebhookService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class PerformanceDataRepository @Inject constructor(
    private val webhookService: WebhookService
) {
    // In-memory cache (not persisted)
    private val employeesState = MutableStateFlow<List<EmployeePerformance>>(emptyList())

    // Selected team members (set from ViewModel/UI)
    private val selectedTeamMembers = MutableStateFlow<List<BaseEmployee>>(emptyList())

    data class BaseEmployee(
        val id: String,
        val name: String,
        val role: String,
        val githubUsername: String,
        val teamId: String
    )

    fun setSelectedTeamMembers(members: List<BaseEmployee>) {
        selectedTeamMembers.value = members
    }

    fun setSelectedTeamFromUsernames(usernames: List<Pair<String, String>>, teamId: String = "") {
        // usernames: List of Pair<id, githubUsername>
        selectedTeamMembers.value = usernames.map { (id, username) ->
            BaseEmployee(id, name = "", role = "", githubUsername = username, teamId = teamId)
        }
    }

    fun getAllEmployeePerformances(): Flow<List<EmployeePerformance>> = flow {
        val members = selectedTeamMembers.first()
        if (members.isEmpty()) {
            emit(emptyList())
            return@flow
        }
        val performances = members.map { buildFromNetworkOrFallback(it) }
        employeesState.value = performances
        emit(performances)
    }

    fun getEmployeePerformance(employeeId: String): Flow<EmployeePerformance> = flow {
        val member = selectedTeamMembers.first().firstOrNull { it.id == employeeId }
        if (member == null) {
            emit(generateFallback(employeeId))
            return@flow
        }
        emit(buildFromNetworkOrFallback(member))
    }

    fun refreshAllEmployeePerformances(): Flow<List<EmployeePerformance>> = flow {
        val members = selectedTeamMembers.first()
        if (members.isEmpty()) {
            emit(emptyList())
            return@flow
        }
        val refreshed = members.map { buildFromNetworkOrFallback(it) }
        employeesState.value = refreshed
        emit(refreshed)
    }

    fun refreshEmployeePerformance(employeeId: String): Flow<EmployeePerformance> = flow {
        val member = selectedTeamMembers.first().firstOrNull { it.id == employeeId }
            ?: BaseEmployee(employeeId, "Unknown", "Engineer", "", "")
        val updated = buildFromNetworkOrFallback(member)
        val list = employeesState.value.toMutableList().apply {
            val idx = indexOfFirst { it.id == employeeId }
            if (idx >= 0) set(idx, updated) else add(updated)
        }
        employeesState.value = list
        emit(updated)
    }

    // Build performance object from webhook (commit data + evaluation) or fallback
    private suspend fun buildFromNetworkOrFallback(base: BaseEmployee): EmployeePerformance {
        return try {
            val commitData = webhookService.fetchCommitData(
                githubUsername = base.githubUsername,
                forceRefresh = true
            )
            val epochs = commitData.commitDates.mapNotNull { it.toLongOrNull() }
            val attendanceRate = 0.9 + (0..9).random() / 100.0
            val collaboration = 6 + (0..40).random() / 10.0
            val productivity = 6 + (0..40).random() / 10.0
            val metrics = EmployeeMetrics(
                commits = epochs.size,
                githubUsername = base.githubUsername,
                attendanceRate = attendanceRate,
                collaborationScore = collaboration,
                productivityScore = productivity,
            )
            val aiEval = webhookService.evaluateEmployeePerformance(metrics)
            EmployeePerformance(
                id = base.id,
                githubUsername = base.githubUsername,
                attendanceRate = attendanceRate,
                collaborationScore = collaboration,
                productivityScore = productivity,
                aiEvaluation = aiEval,
                commits = epochs.size,
            )
        } catch (_: Exception) {
            generateFallback(base.id)
        }
    }

    // Fallback single
    private fun generateFallback(id: String): EmployeePerformance {
        val base = selectedTeamMembers.value.firstOrNull { it.id == id }
        val rnd = Random(id.hashCode())
        val commits = 5 + rnd.nextInt(20)
        return EmployeePerformance(
            id = id,
            githubUsername = base?.githubUsername ?: "",
            attendanceRate = 0.85 + rnd.nextDouble() * 0.15,
            collaborationScore = 6 + rnd.nextDouble() * 4,
            productivityScore = 6 + rnd.nextDouble() * 4,
            commits = commits,
            aiEvaluation = AIEvaluation(
                overallRating = listOf("Average", "Good", "Excellent").random(rnd),
                strengths = listOf("Reliable", "Team Player", "Learns quickly").shuffled(rnd).take(2),
                improvements = listOf("Add tests", "Refactor modules", "Improve docs").shuffled(rnd).take(1),
                recommendation = "Maintain momentum and focus on quality",
                performanceTrend = listOf("Improving", "Stable", "Declining").random(rnd)
            )
        )
    }

    private fun generateFallbackList(): List<EmployeePerformance> = selectedTeamMembers.value.map { generateFallback(it.id) }
}