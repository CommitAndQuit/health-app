package io.biotrace.app.data.llm

import io.biotrace.app.domain.llm.LlmApiException
import io.biotrace.app.domain.llm.LlmClient
import io.biotrace.app.domain.llm.LlmNotConfiguredException
import io.biotrace.app.domain.security.KeystoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiClient @Inject constructor(
    private val keystoreManager: KeystoreManager
) : LlmClient {

    companion object {
        const val PROVIDER_NAME = "gemini"
        private const val MODEL_NAME = "gemini-2.0-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val SYSTEM_INSTRUCTION =
            "You are BioTrace, an intelligent, private, local-first health & biomarker intelligence assistant. " +
            "You help users interpret health vitals, lab reports, and nutrition data. " +
            "Always cite the exact data records used in your recommendations using [Record #ID: Date, Value, Baseline]. " +
            "Be conservative, evidence-grounded, and clearly state you are an AI assistant and not a medical doctor."
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun isConfigured(): Boolean {
        val key = keystoreManager.getApiKey(PROVIDER_NAME)
        return !key.isNullOrBlank()
    }

    private suspend fun getValidApiKey(): String {
        val apiKey = keystoreManager.getApiKey(PROVIDER_NAME)
        if (apiKey.isNullOrBlank()) {
            throw LlmNotConfiguredException()
        }
        return apiKey
    }

    private fun buildRequestBody(prompt: String): String {
        val systemInstructionObj = JSONObject().apply {
            put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_INSTRUCTION)))
        }

        val userContentObj = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", prompt)))
        }

        val root = JSONObject().apply {
            put("system_instruction", systemInstructionObj)
            put("contents", JSONArray().put(userContentObj))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.2)
            })
        }

        return root.toString()
    }

    override suspend fun generateContent(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = getValidApiKey()
        val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"
        val jsonBody = buildRequestBody(prompt)

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                when (response.code) {
                    400 -> throw LlmApiException("Invalid request format: $responseBody")
                    401, 403 -> throw LlmApiException("Invalid Gemini API key: $responseBody")
                    429 -> throw LlmApiException("Rate limit exceeded. Please wait a moment.")
                    else -> throw LlmApiException("Gemini API error (${response.code}): $responseBody")
                }
            }

            extractTextFromResponse(responseBody)
        }
    }

    override fun generateContentStream(prompt: String): Flow<String> = flow {
        val apiKey = getValidApiKey()
        val url = "$BASE_URL/$MODEL_NAME:streamGenerateContent?alt=sse&key=$apiKey"
        val jsonBody = buildRequestBody(prompt)

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        val call = okHttpClient.newCall(request)
        val response = call.execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string().orEmpty()
            response.close()
            when (response.code) {
                401, 403 -> throw LlmApiException("Invalid Gemini API key.")
                429 -> throw LlmApiException("Rate limit exceeded.")
                else -> throw LlmApiException("Stream error (${response.code}): $errorBody")
            }
        }

        val responseBody = response.body ?: throw LlmApiException("Empty response stream")
        val reader = BufferedReader(InputStreamReader(responseBody.byteStream()))

        response.use {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line?.trim().orEmpty()
                if (currentLine.startsWith("data:")) {
                    val dataJson = currentLine.removePrefix("data:").trim()
                    if (dataJson.isNotEmpty() && dataJson != "[DONE]") {
                        val textChunk = extractTextFromResponse(dataJson)
                        if (textChunk.isNotEmpty()) {
                            emit(textChunk)
                        }
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun extractTextFromResponse(jsonStr: String): String {
        return try {
            val root = JSONObject(jsonStr)
            val candidates = root.optJSONArray("candidates") ?: return ""
            if (candidates.length() == 0) return ""

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return ""
            val parts = content.optJSONArray("parts") ?: return ""

            val sb = java.lang.StringBuilder()
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                val text = part.optString("text", "")
                sb.append(text)
            }
            sb.toString()
        } catch (e: Exception) {
            ""
        }
    }
}
