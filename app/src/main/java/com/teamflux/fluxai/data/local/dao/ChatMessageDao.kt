package com.teamflux.fluxai.data.local.dao

import androidx.room.*
import com.teamflux.fluxai.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE userId = :userId ORDER BY timestamp DESC")
    fun getChatHistoryForUser(userId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE userId = :userId")
    suspend fun clearChatHistoryForUser(userId: String)

    @Query("SELECT * FROM chat_messages WHERE userId = :userId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getChatHistoryBetweenDates(userId: String, startTime: Long, endTime: Long): Flow<List<ChatMessageEntity>>
}
