package com.teamflux.fluxai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.teamflux.fluxai.data.local.dao.*
import com.teamflux.fluxai.data.local.entity.*

@Database(
    entities = [
        AdminEntity::class,
        TeamEntity::class,
        TeamMemberEntity::class,
        AttendanceEntity::class,
        PerformanceEntity::class,
        ChatMessageEntity::class,
        UserProfileEntity::class,
        CommitDataEntity::class // Added CommitDataEntity
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FluxDatabase : RoomDatabase() {
    abstract fun adminDao(): AdminDao
    abstract fun teamDao(): TeamDao
    abstract fun teamMemberDao(): TeamMemberDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun performanceDao(): PerformanceDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun employeePerformanceDao(): EmployeePerformanceDao // Added DAO for commit data
}
