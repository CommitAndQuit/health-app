package io.biotrace.app.domain.llm

import kotlinx.coroutines.flow.Flow

class LlmNotConfiguredException(
    message: String = "Gemini API key is not configured. Please set it in Settings."
) : Exception(message)

class LlmApiException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

interface LlmClient {
    fun generateContentStream(prompt: String): Flow<String>
    suspend fun generateContent(prompt: String): String
    suspend fun isConfigured(): Boolean
}
