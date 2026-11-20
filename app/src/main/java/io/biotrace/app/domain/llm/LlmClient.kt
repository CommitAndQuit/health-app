package io.biotrace.app.domain.llm

import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

class LlmNotConfiguredException(
    message: String = "Gemini API key is not configured. Please set it in Settings."
) : Exception(message)

class LlmApiException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

interface LlmClient {
    fun generateContentStream(prompt: String): Flow<String>

    suspend fun generateContent(
        prompt: String,
        systemInstructionOverride: String? = null,
        responseSchema: JSONObject? = null,
        responseMimeType: String? = null,
        temperature: Double? = null
    ): String

    suspend fun generateContent(prompt: String): String =
        generateContent(prompt, null, null, null, null)

    suspend fun generateContentWithImage(prompt: String, base64Image: String, mimeType: String = "image/jpeg"): String
    suspend fun generateContentWithDocument(prompt: String, base64Data: String, mimeType: String = "application/pdf"): String =
        generateContentWithImage(prompt, base64Data, mimeType)
    suspend fun isConfigured(): Boolean
}
