package com.teamflux.fluxai.di

import android.content.Context
import androidx.room.Room
import com.teamflux.fluxai.data.room.AIInsightDao
import com.teamflux.fluxai.data.room.EmployeePerformanceDao
import com.teamflux.fluxai.data.room.FluxDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): FluxDatabase =
        Room.databaseBuilder(
            context,
            FluxDatabase::class.java,
            FluxDatabase.DATABASE_NAME
        ).addMigrations(FluxDatabase.MIGRATION_1_2).build()

    @Provides
    fun provideEmployeePerformanceDao(db: FluxDatabase): EmployeePerformanceDao = db.employeePerformanceDao()

    @Provides
    fun provideAIInsightDao(db: FluxDatabase): AIInsightDao = db.aiInsightDao()
}

