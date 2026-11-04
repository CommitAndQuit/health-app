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
    suspend fun generateContentWithImage(prompt: String, base64Image: String, mimeType: String = "image/jpeg"): String
    suspend fun generateContentWithDocument(prompt: String, base64Data: String, mimeType: String = "application/pdf"): String =
        generateContentWithImage(prompt, base64Data, mimeType)
    suspend fun isConfigured(): Boolean
}
