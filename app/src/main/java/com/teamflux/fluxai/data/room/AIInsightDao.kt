package com.teamflux.fluxai.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AIInsightDao {
    @Query("SELECT * FROM ai_insights WHERE employeeId = :employeeId")
    fun getAIInsight(employeeId: String): Flow<AIInsightEntity?>

    @Query("SELECT * FROM ai_insights")
    fun getAllAIInsights(): Flow<List<AIInsightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAIInsight(insight: AIInsightEntity)

    @Query("DELETE FROM ai_insights WHERE employeeId = :employeeId")
    suspend fun deleteAIInsight(employeeId: String)
}
