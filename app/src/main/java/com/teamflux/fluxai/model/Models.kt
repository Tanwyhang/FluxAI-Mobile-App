package com.teamflux.fluxai.model

import java.util.Date

// --- USER AUTH ---
data class Admin(
    val adminId: String,
    val username: String,
    val email: String,
    val phone: String,
    val passwordHash: String
)

// --- TEAM ---
data class Team(
    val teamId: String,
    val teamName: String,
    val passwordHash: String,
    val createdBy: String, // adminId
    val createdAt: Date
)

data class TeamMember(
    val userId: String,
    val teamId: String, // belongs to what team
    val role: String, // frontend | backend etc..
    val githubProfileLink: String,
    val email: String,
    val phone: String,
    val passwordHash: String
)

// --- ATTENDANCE ---
data class Attendance(
    val attendanceId: String,
    val userId: String,
    val date: Date,
    val status: Boolean // Present = true | Absent = false
)

// --- PERFORMANCE ---
data class Performance(
    val performanceId: String,
    val userId: String,
    val teamId: String, // for team performance
    val commits: Int,
    val performanceScore: Float,
    val startDate: Date,
    val endDate: Date
)

// --- UI MODELS FOR DASHBOARD ---
data class EmployeePerformance(
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
    val commitHistory: List<Int>, // Last 7 days commit count
    val aiEvaluation: AIEvaluation
)

data class AIEvaluation(
    val overallRating: String, // "Excellent", "Good", "Average", "Needs Improvement"
    val strengths: List<String>,
    val improvements: List<String>,
    val recommendation: String,
    val performanceTrend: String // "Improving", "Stable", "Declining"
)

// --- CHAT MODELS ---
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
