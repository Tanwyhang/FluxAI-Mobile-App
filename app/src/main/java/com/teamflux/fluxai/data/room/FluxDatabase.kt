package com.teamflux.fluxai.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AIInsightEntity::class,
        EmployeePerformanceEntity::class,
        ChatMessageEntity::class,
        CommitDataEntity::class // Added commit data entity
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class FluxDatabase : RoomDatabase() {
    abstract fun aiInsightDao(): AIInsightDao
    abstract fun employeePerformanceDao(): EmployeePerformanceDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        const val DATABASE_NAME = "flux_database"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `commit_data` (`employeeId` TEXT NOT NULL, `commitDates` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`employeeId`))"
                )
            }
        }
    }
}
