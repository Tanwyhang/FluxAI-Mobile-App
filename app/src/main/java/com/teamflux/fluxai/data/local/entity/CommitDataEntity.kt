package com.teamflux.fluxai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "commit_data")
data class CommitDataEntity(
    @PrimaryKey
    val employeeId: String,
    val commitDates: List<String>,  // List of commit dates from GitHub
    val timestamp: Long  // When this data was cached
)
