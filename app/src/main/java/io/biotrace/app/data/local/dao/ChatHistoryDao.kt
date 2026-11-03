package io.biotrace.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.biotrace.app.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity)

    @Query("SELECT * FROM chat_history ORDER BY timestamp ASC LIMIT :limit")
    fun getRecentMessagesFlow(limit: Int = 100): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_history ORDER BY timestamp ASC")
    suspend fun getAllMessages(): List<ChatMessageEntity>

    @Query("DELETE FROM chat_history")
    suspend fun clearHistory()
}
