package com.teamflux.fluxai.data.local.dao

import androidx.room.*
import com.teamflux.fluxai.data.local.entity.PerformanceEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface PerformanceDao {
    @Query("SELECT * FROM performance WHERE userId = :userId ORDER BY startDate DESC")
    fun getUserPerformance(userId: String): Flow<List<PerformanceEntity>>

    @Query("SELECT * FROM performance WHERE teamId = :teamId ORDER BY startDate DESC")
    fun getTeamPerformance(teamId: String): Flow<List<PerformanceEntity>>

    @Query("SELECT * FROM performance WHERE userId = :userId AND startDate BETWEEN :startDate AND :endDate")
    fun getUserPerformanceBetweenDates(userId: String, startDate: Date, endDate: Date): Flow<List<PerformanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerformance(performance: PerformanceEntity)

    @Update
    suspend fun updatePerformance(performance: PerformanceEntity)
}
