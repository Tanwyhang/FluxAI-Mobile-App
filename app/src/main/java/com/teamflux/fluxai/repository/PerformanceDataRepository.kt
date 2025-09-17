package com.teamflux.fluxai.repository

import com.teamflux.fluxai.model.AIEvaluation
import com.teamflux.fluxai.model.EmployeePerformance
import com.teamflux.fluxai.network.WebhookService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

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
        val accumulator = mutableListOf<EmployeePerformance>()
        employeesState.value = emptyList()
        // One webhook call per teammate; emit as each completes
        for (member in members) {
            val perf = buildFromNetwork(member)
            accumulator.add(perf)
            employeesState.value = accumulator.toList()
            emit(accumulator.toList())
        }
    }

    fun getEmployeePerformance(employeeId: String): Flow<EmployeePerformance> = flow {
        val member = selectedTeamMembers.first().firstOrNull { it.id == employeeId }
        if (member == null) {
            emit(emptyPerformance(employeeId))
            return@flow
        }
        emit(buildFromNetwork(member))
    }

    fun refreshAllEmployeePerformances(): Flow<List<EmployeePerformance>> = flow {
        val members = selectedTeamMembers.first()
        if (members.isEmpty()) {
            emit(emptyList())
            return@flow
        }
        val accumulator = mutableListOf<EmployeePerformance>()
        employeesState.value = emptyList()
        for (member in members) {
            val perf = buildFromNetwork(member)
            val idx = accumulator.indexOfFirst { it.id == perf.id }
            if (idx >= 0) accumulator[idx] = perf else accumulator.add(perf)
            employeesState.value = accumulator.toList()
            emit(accumulator.toList())
        }
    }

    fun refreshEmployeePerformance(employeeId: String): Flow<EmployeePerformance> = flow {
        val member = selectedTeamMembers.first().firstOrNull { it.id == employeeId }
            ?: BaseEmployee(employeeId, "Unknown", "Engineer", "", "")
        val updated = buildFromNetwork(member)
        val list = employeesState.value.toMutableList().apply {
            val idx = indexOfFirst { it.id == employeeId }
            if (idx >= 0) set(idx, updated) else add(updated)
        }
        employeesState.value = list
        emit(updated)
    }

    // Build performance object using a single webhook call: only username required
    private suspend fun buildFromNetwork(base: BaseEmployee): EmployeePerformance {
        return try {
            val combined = webhookService.fetchCombinedEmployeeSummary(base.githubUsername)
            val ai: AIEvaluation? = combined.evaluation
            val dates: List<String> = combined.commitDates
            val collab = combined.collaborationScore ?: 0.0
            val prod = combined.productivityScore ?: 0.0
            EmployeePerformance(
                id = base.id,
                githubUsername = base.githubUsername,
                commits = dates.size,
                commitDates = dates,
                attendanceRate = 0.0,
                collaborationScore = collab.coerceIn(0.0, 10.0),
                productivityScore = prod.coerceIn(0.0, 10.0),
                aiEvaluation = ai
            )
        } catch (_: Exception) {
            emptyPerformance(base.id, base.githubUsername)
        }
    }

    private fun emptyPerformance(id: String, username: String = ""): EmployeePerformance = EmployeePerformance(

        id = id,
        githubUsername = username,
        commits = 0,
        commitDates = emptyList(),
        attendanceRate = 0.0,
        collaborationScore = 0.0,
        productivityScore = 0.0,
        aiEvaluation = null
    )
}