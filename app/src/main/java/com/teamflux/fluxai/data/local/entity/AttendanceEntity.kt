package com.teamflux.fluxai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "attendance")
data class AttendanceEntity(
    @PrimaryKey
    val attendanceId: String,
    val userId: String,
    val date: Date,
    val status: String
)
