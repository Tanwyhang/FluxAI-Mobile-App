package com.teamflux.fluxai.network

/**
 * Aggregated employee performance metrics passed to WebhookService for AI evaluation.
 */
 data class EmployeeMetrics(
     val commits: Int,
     val commitsLastWeek: Int,
     val linesOfCode: Int,
     val employeeId: String,
     val githubUsername: String,
     val tasksCompleted: Int,
     val totalTasks: Int,
     val attendanceRate: Double,
     val codeQualityScore: Double,
     val collaborationScore: Double,
     val productivityScore: Double,
     val commitHistory: List<Int> = emptyList() // 30-day (or N-day) daily commit counts oldest -> newest
 )
