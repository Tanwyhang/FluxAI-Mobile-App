package com.teamflux.fluxai.repository

import com.teamflux.fluxai.data.room.EmployeePerformanceDao
import com.teamflux.fluxai.data.room.EmployeePerformanceEntity
import com.teamflux.fluxai.model.AIEvaluation
import com.teamflux.fluxai.model.EmployeePerformance
import com.teamflux.fluxai.network.WebhookService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerformanceDataRepository @Inject constructor(
    private val performanceDao: EmployeePerformanceDao,
    private val webhookService: WebhookService
) {
    // Refresh a single employee performance by forcing commit data refresh
    suspend fun refreshEmployeePerformance(employeeId: String): Flow<EmployeePerformance> {
        val commitData = webhookService.fetchCommitData(employeeId = employeeId, forceRefresh = true)
        val now = System.currentTimeMillis()
        val oneDay = 86_400_000L
        // Parse commit dates (ISO) to epoch
        val epochs = commitData.commitDates.mapNotNull { iso ->
            try { if (android.os.Build.VERSION.SDK_INT >= 26) java.time.Instant.parse(iso).toEpochMilli() else null } catch (_: Exception) { null }
        }
        val last7Start = now - 6 * oneDay
        val prev7Start = now - 13 * oneDay
        val commitsThisWeek = epochs.count { it >= last7Start }
        val commitsLastWeek = epochs.count { it in prev7Start until last7Start }
        val dailyCounts = (6 downTo 0).map { offset ->
            val dayStart = now - offset * oneDay
            val prevStart = dayStart - oneDay
            epochs.count { it in (prevStart + 1)..dayStart }
        }
        return performanceDao.getEmployeePerformance(employeeId)
            .map { entity ->
                (entity ?: placeholderEntity(employeeId)).toEmployeePerformance().copy(
                    commitsThisWeek = commitsThisWeek,
                    commitsLastWeek = commitsLastWeek,
                    commitHistory = dailyCounts
                )
            }
    }

    // Refresh all employees
    suspend fun refreshAllEmployeePerformances(): Flow<List<EmployeePerformance>> {
        val ids = performanceDao.getAllEmployeeIds()
        ids.forEach { id ->
            webhookService.fetchCommitData(employeeId = id, forceRefresh = true)
        }
        return performanceDao.getAllEmployeePerformances()
            .map { list -> list.map { it.toEmployeePerformance() } }
    }

    fun getEmployeePerformance(employeeId: String): Flow<EmployeePerformance> {
        return performanceDao.getEmployeePerformance(employeeId)
            .map { entity -> (entity ?: placeholderEntity(employeeId)).toEmployeePerformance() }
    }

    fun getAllEmployeePerformances(): Flow<List<EmployeePerformance>> {
        return performanceDao.getAllEmployeePerformances()
            .map { list -> list.map { it.toEmployeePerformance() } }
    }

    private fun placeholderEntity(id: String) = EmployeePerformanceEntity(
        id = id,
        name = "Unknown",
        role = "Unknown",
        githubUsername = "",
        commitsThisWeek = 0,
        commitsLastWeek = 0,
        linesOfCode = 0,
        tasksCompleted = 0,
        totalTasks = 0,
        attendanceRate = 0.0,
        codeQualityScore = 0.0,
        collaborationScore = 0.0,
        productivityScore = 0.0,
        commitHistory = List(7) { 0 }
    )

    private fun EmployeePerformanceEntity.toEmployeePerformance(eval: AIEvaluation? = null) = EmployeePerformance(
        id = id,
        name = name,
        role = role,
        githubUsername = githubUsername,
        commitsThisWeek = commitsThisWeek,
        commitsLastWeek = commitsLastWeek,
        linesOfCode = linesOfCode,
        tasksCompleted = tasksCompleted,
        totalTasks = totalTasks,
        attendanceRate = attendanceRate,
        codeQualityScore = codeQualityScore,
        collaborationScore = collaborationScore,
        productivityScore = productivityScore,
        commitHistory = commitHistory,
        aiEvaluation = eval ?: AIEvaluation(
            overallRating = "Pending",
            strengths = emptyList(),
            improvements = emptyList(),
            recommendation = "No evaluation yet",
            performanceTrend = "Stable"
        )
    )
}
