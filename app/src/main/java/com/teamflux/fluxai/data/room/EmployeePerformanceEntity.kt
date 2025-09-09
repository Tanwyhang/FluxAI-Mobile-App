package com.teamflux.fluxai.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employee_performance")
data class EmployeePerformanceEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val role: String,
    val githubUsername: String,
    val commitsThisWeek: Int,
    val commitsLastWeek: Int,
    val linesOfCode: Int,
    val tasksCompleted: Int,
    val totalTasks: Int,
    val attendanceRate: Double,
    val codeQualityScore: Double,
    val collaborationScore: Double,
    val productivityScore: Double,
    val commitHistory: List<Int>,
    val lastUpdated: Long = System.currentTimeMillis()
)
