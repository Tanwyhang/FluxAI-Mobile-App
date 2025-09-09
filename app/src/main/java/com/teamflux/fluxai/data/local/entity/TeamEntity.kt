package com.teamflux.fluxai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey
    val teamId: String,
    val teamName: String,
    val createdBy: String,
    val createdAt: Date
)
