package com.teamflux.fluxai.di

import android.content.Context
import androidx.room.Room
import com.teamflux.fluxai.data.local.FluxDatabase
import com.teamflux.fluxai.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FluxDatabase {
        return Room.databaseBuilder(
            context,
            FluxDatabase::class.java,
            "flux_database"
        ).build()
    }

    @Provides
    fun provideAdminDao(database: FluxDatabase): AdminDao = database.adminDao()

    @Provides
    fun provideTeamDao(database: FluxDatabase): TeamDao = database.teamDao()

    @Provides
    fun provideTeamMemberDao(database: FluxDatabase): TeamMemberDao = database.teamMemberDao()

    @Provides
    fun provideAttendanceDao(database: FluxDatabase): AttendanceDao = database.attendanceDao()

    @Provides
    fun providePerformanceDao(database: FluxDatabase): PerformanceDao = database.performanceDao()

    @Provides
    fun provideChatMessageDao(database: FluxDatabase): ChatMessageDao = database.chatMessageDao()

    @Provides
    fun provideUserProfileDao(database: FluxDatabase): UserProfileDao = database.userProfileDao()
}
