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
        const val MODEL_NAME = "gemini-2.5-flash"
        const val HARDCODED_API_KEY = "AQ.Ab8RN6IzIaWFeKq3qz79GgNyollccit3s7CpYOpUS1_AlrkNKA"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val SYSTEM_INSTRUCTION =
            "You are BioTrace, an intelligent, private, local-first health & biomarker intelligence assistant. " +
            "You help users interpret health vitals, lab reports, and nutrition data. " +
            "Always cite the exact data records used in your recommendations using [Record #ID: Date, Value, Baseline]. " +
            "Be conservative, evidence-grounded, and clearly state you are an AI assistant and not a medical doctor."

        fun createDailyReportSchema(): JSONObject {
            val headlineSummaryProp = JSONObject().apply {
                put("type", "STRING")
            }

            val clinicalAlertProp = JSONObject().apply {
                put("type", "STRING")
                put("nullable", true)
            }

            val trendHighlightItem = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("title", JSONObject().apply { put("type", "STRING") })
                    put("domain_tags", JSONObject().apply {
                        put("type", "ARRAY")
                        put("items", JSONObject().apply { put("type", "STRING") })
                    })
                    put("direction", JSONObject().apply { put("type", "STRING") })
                    put("insight_text", JSONObject().apply { put("type", "STRING") })
                    put("supporting_record_ids", JSONObject().apply {
                        put("type", "ARRAY")
                        put("items", JSONObject().apply { put("type", "STRING") })
                    })
                })
                put("required", JSONArray().apply {
                    put("title")
                    put("domain_tags")
                    put("direction")
                    put("insight_text")
                    put("supporting_record_ids")
                })
            }

            val trendHighlightsProp = JSONObject().apply {
                put("type", "ARRAY")
                put("items", trendHighlightItem)
            }

            val actionableFocusProp = JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("recommendation", JSONObject().apply { put("type", "STRING") })
                    put("rationale_metric", JSONObject().apply { put("type", "STRING") })
                })
                put("required", JSONArray().apply {
                    put("recommendation")
                    put("rationale_metric")
                })
            }

            return JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("headline_summary", headlineSummaryProp)
                    put("clinical_arbitration_alert", clinicalAlertProp)
                    put("trend_highlights", trendHighlightsProp)
                    put("actionable_focus", actionableFocusProp)
                })
                put("required", JSONArray().apply {
                    put("headline_summary")
                    put("trend_highlights")
                    put("actionable_focus")
                })
            }
        }
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun isConfigured(): Boolean = true

    private fun getValidApiKey(): String {
        val buildKey = io.biotrace.app.BuildConfig.GEMINI_API_KEY.trim()
        val key = if (buildKey.isNotBlank()) buildKey else HARDCODED_API_KEY
        if (key.isBlank()) {
            throw LlmNotConfiguredException()
        }
        return key
    }

    private fun buildRequestBody(
        prompt: String,
        systemInstructionOverride: String? = null,
        responseSchema: JSONObject? = null,
        responseMimeType: String? = null,
        temperature: Double? = null
    ): String {
        val instruction = systemInstructionOverride ?: SYSTEM_INSTRUCTION
        val systemInstructionObj = JSONObject().apply {
            put("parts", JSONArray().put(JSONObject().put("text", instruction)))
        }

        val userContentObj = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", prompt)))
        }

        val generationConfig = JSONObject().apply {
            put("temperature", temperature ?: 0.2)
            if (responseMimeType != null) {
                put("responseMimeType", responseMimeType)
                put("response_mime_type", responseMimeType)
            }
            if (responseSchema != null) {
                put("responseSchema", responseSchema)
                put("response_schema", responseSchema)
            }
        }

        val root = JSONObject().apply {
            put("system_instruction", systemInstructionObj)
            put("contents", JSONArray().put(userContentObj))
            put("generationConfig", generationConfig)
        }

        return root.toString()
    }

    private fun buildRequestBodyWithImage(prompt: String, base64Image: String, mimeType: String): String {
        val systemInstructionObj = JSONObject().apply {
            put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_INSTRUCTION)))
        }

        val partsArray = JSONArray().apply {
            put(JSONObject().put("text", prompt))
            put(JSONObject().apply {
                put("inline_data", JSONObject().apply {
                    put("mime_type", mimeType)
                    put("data", base64Image)
                })
            })
        }

        val userContentObj = JSONObject().apply {
            put("role", "user")
            put("parts", partsArray)
        }

        val root = JSONObject().apply {
            put("system_instruction", systemInstructionObj)
            put("contents", JSONArray().put(userContentObj))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1)
            })
        }

        return root.toString()
    }

    override suspend fun generateContent(
        prompt: String,
        systemInstructionOverride: String?,
        responseSchema: JSONObject?,
        responseMimeType: String?,
        temperature: Double?
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getValidApiKey()
        val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"
        val jsonBody = buildRequestBody(
            prompt = prompt,
            systemInstructionOverride = systemInstructionOverride,
            responseSchema = responseSchema,
            responseMimeType = responseMimeType,
            temperature = temperature
        )

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

    override suspend fun generateContent(prompt: String): String =
        generateContent(prompt, null, null, null, null)

    override suspend fun generateContentWithImage(
        prompt: String,
        base64Image: String,
        mimeType: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getValidApiKey()
        val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"
        val jsonBody = buildRequestBodyWithImage(prompt, base64Image, mimeType)

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        var lastException: Exception? = null
        val maxRetries = 3

        for (attempt in 1..maxRetries) {
            try {
                val callResult = okHttpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        if ((response.code == 503 || response.code == 429) && attempt < maxRetries) {
                            return@use Pair(false, responseBody)
                        }
                        when (response.code) {
                            400 -> throw LlmApiException("Invalid request format: $responseBody")
                            401, 403 -> throw LlmApiException("Invalid Gemini API key: $responseBody")
                            429 -> throw LlmApiException("Rate limit exceeded. Please wait a moment.")
                            else -> throw LlmApiException("Gemini API error (${response.code}): $responseBody")
                        }
                    }

                    Pair(true, extractTextFromResponse(responseBody))
                }

                if (callResult.first) {
                    return@withContext callResult.second
                } else {
                    kotlinx.coroutines.delay(2500L * attempt)
                }
            } catch (e: LlmApiException) {
                throw e
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) {
                    kotlinx.coroutines.delay(2500L * attempt)
                }
            }
        }

        throw lastException ?: LlmApiException("Gemini API service unavailable after $maxRetries attempts. Please try again.")
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
