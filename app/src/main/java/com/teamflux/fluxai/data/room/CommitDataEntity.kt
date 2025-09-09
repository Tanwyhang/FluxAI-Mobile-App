package com.teamflux.fluxai.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "commit_data")
data class CommitDataEntity(
    @PrimaryKey
    val employeeId: String,
    val commitDates: List<String>,  // List of commit dates from the last 30 days
    val timestamp: Long  // When this data was cached
)
