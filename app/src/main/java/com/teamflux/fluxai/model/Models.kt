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
    val status: String // "present", "absent"
)

// --- UI MODELS FOR DASHBOARD ---
data class EmployeePerformance(
    val id: String, // Unique employee ID
    val githubUsername: String, // GitHub username
    val commits: Int, // Total commits in the timeframe (e.g., 30 days)
    val attendanceRate: Double, // Attendance rate (0.0 to 1.0)
    val collaborationScore: Double, // Collaboration score (0.0 to 10.0)
    val productivityScore: Double, // Productivity score (0.0 to 10.0)
    val aiEvaluation: AIEvaluation? = null // AI-generated evaluation (nullable
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
