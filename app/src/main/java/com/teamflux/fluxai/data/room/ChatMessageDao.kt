package com.teamflux.fluxai.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE userId = :userId AND userRole = :userRole ORDER BY timestamp ASC")
    fun getChatHistory(userId: String, userRole: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE userId = :userId AND userRole = :userRole")
    suspend fun clearChatHistory(userId: String, userRole: String)
}
