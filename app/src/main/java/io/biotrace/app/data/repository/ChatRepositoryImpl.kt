package io.biotrace.app.data.repository

import io.biotrace.app.data.local.dao.ChatHistoryDao
import io.biotrace.app.data.local.entity.ChatMessageEntity
import io.biotrace.app.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatHistoryDao: ChatHistoryDao
) : ChatRepository {

    override fun getChatHistory(): Flow<List<ChatMessageEntity>> {
        return chatHistoryDao.getRecentMessagesFlow(100)
    }

    override suspend fun saveMessage(message: ChatMessageEntity) {
        chatHistoryDao.insert(message)
    }

    override suspend fun clearHistory() {
        chatHistoryDao.clearHistory()
    }
}
