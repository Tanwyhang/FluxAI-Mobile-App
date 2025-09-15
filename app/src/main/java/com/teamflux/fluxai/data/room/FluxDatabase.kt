package com.teamflux.fluxai.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.teamflux.fluxai.data.local.entity.TeamMemberEntity
import com.teamflux.fluxai.data.local.entity.TeamEntity

@Database(
    entities = [
        AIInsightEntity::class,
        EmployeePerformanceEntity::class,
        ChatMessageEntity::class,
        CommitDataEntity::class,
        TeamMemberEntity::class,
        TeamEntity::class
    ],
    version = 5, // bumped from 4 -> 5 to force clean forward migration path
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

        // NOTE: TeamMemberEntity has a FK to TeamEntity, so teams MUST exist before team_members.
        // If a v3 database was ever shipped, we ensure both tables exist (idempotent via IF NOT EXISTS).
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create teams first (added early to satisfy FK on team_members)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `teams` (
                        `teamId` TEXT NOT NULL,
                        `teamName` TEXT NOT NULL,
                        `createdBy` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`teamId`)
                    )
                    """.trimIndent()
                )
                // Then team_members
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `team_members` (
                        `userId` TEXT NOT NULL,
                        `teamId` TEXT NOT NULL,
                        `username` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        PRIMARY KEY(`userId`, `teamId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_team_members_teamId` ON `team_members` (`teamId`)")
            }
        }

        // Kept for completeness if any device ever reached version 3 without teams table (defensive)
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `teams` (
                        `teamId` TEXT NOT NULL,
                        `teamName` TEXT NOT NULL,
                        `createdBy` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`teamId`)
                    )
                    """.trimIndent()
                )
            }
        }

        // Direct jump 2 -> 4 (ensure order: teams then team_members)
        val MIGRATION_2_4 = object : Migration(2, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `teams` (
                        `teamId` TEXT NOT NULL,
                        `teamName` TEXT NOT NULL,
                        `createdBy` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`teamId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `team_members` (
                        `userId` TEXT NOT NULL,
                        `teamId` TEXT NOT NULL,
                        `username` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        PRIMARY KEY(`userId`, `teamId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_team_members_teamId` ON `team_members` (`teamId`)")
            }
        }

        // Defensive migration for 4 -> 5 (idempotent safety)
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ensure teams table exists (was added earlier but recreate if missing)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `teams` (
                        `teamId` TEXT NOT NULL,
                        `teamName` TEXT NOT NULL,
                        `createdBy` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`teamId`)
                    )
                    """.trimIndent()
                )
                // Ensure team_members table exists (prior migration created it)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `team_members` (
                        `userId` TEXT NOT NULL,
                        `teamId` TEXT NOT NULL,
                        `username` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        PRIMARY KEY(`userId`, `teamId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_team_members_teamId` ON `team_members` (`teamId`)")
            }
        }
    }
}
