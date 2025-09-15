package com.teamflux.fluxai.network

/**
 * Aggregated employee performance metrics passed to WebhookService for AI evaluation.
 */
data class EmployeeMetrics(
    val githubUsername: String,
    val productivityScore: Double,
    val collaborationScore: Double,
    val commits: Int,
    val attendanceRate: Double
)