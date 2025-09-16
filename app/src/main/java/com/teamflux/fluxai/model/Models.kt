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

// Refined team member model used across repository and UI
// memberId is the stable identifier for a membership row (distinct from userId)
data class TeamMember(
    val memberId: String,            // unique membership id
    val teamId: String,              // owning team id
    val userId: String? = null,      // optional app user id
    val githubUsername: String,      // required for n8n/GitHub flows
    val displayName: String? = null, // shown name; fallback to githubUsername in UI
    val role: String = "",          // e.g., frontend, backend, PM
    val email: String? = null,
    val phone: String? = null,
    val avatarUrl: String? = null,
    val joinedAt: Date = Date(),
    val isActive: Boolean = true
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
    val commitDates: List<String> = emptyList(), // Commit dates for chart visualization
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
