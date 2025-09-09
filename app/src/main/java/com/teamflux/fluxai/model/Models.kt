package com.teamflux.fluxai.model

import java.util.Date

// --- USER AUTH ---
data class Admin(
    val adminId: String,
    val username: String,
    val email: String,
    val phone: String,
)

// --- TEAM ---
data class Team(
    val teamId: String = "",
    val teamName: String = "",
    val createdBy: String = "", // adminId
    val createdAt: Date = Date()
)

data class TeamMember(
    val userId: String = "",
    val teamId: String = "", // belongs to what team
    val username: String = "",
    val role: String = "", // frontend | backend etc..
    val email: String = "",
    val phone: String = "",
)

// --- ATTENDANCE ---
data class Attendance(
    val attendanceId: String,
    val userId: String,
    val date: Date,
    val status: String // "present", "absent", "late"
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
    val overallRating: String,
    val strengths: List<String>,
    val improvements: List<String>,
    val recommendation: String,
    val performanceTrend: String
)

// --- CHAT MODELS ---
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

// --- USER PROFILE (for Settings) ---
data class UserProfile(
    val displayName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val githubUsername: String? = null,
    val githubProfileUrl: String? = null,
    val avatarUrl: String? = null
)
