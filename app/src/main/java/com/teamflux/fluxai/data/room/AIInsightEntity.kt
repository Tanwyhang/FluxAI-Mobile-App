package com.teamflux.fluxai.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_insights")
data class AIInsightEntity(
    @PrimaryKey
    val employeeId: String,
    val overallRating: String,
    val strengths: List<String>,
    val improvements: List<String>,
    val recommendation: String,
    val performanceTrend: String,
    val lastUpdated: Long = System.currentTimeMillis()
)
