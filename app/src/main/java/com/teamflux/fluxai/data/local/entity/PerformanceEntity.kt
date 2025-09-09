package com.teamflux.fluxai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "performance")
data class PerformanceEntity(
    @PrimaryKey
    val performanceId: String,
    val userId: String,
    val teamId: String,
    val commits: Int,
    val performanceScore: Float,
    val startDate: Date,
    val endDate: Date
)
