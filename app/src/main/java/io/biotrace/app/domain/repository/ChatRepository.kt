package io.biotrace.app.domain.repository

import io.biotrace.app.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getChatHistory(): Flow<List<ChatMessageEntity>>
    suspend fun saveMessage(message: ChatMessageEntity)
    suspend fun clearHistory()
}
