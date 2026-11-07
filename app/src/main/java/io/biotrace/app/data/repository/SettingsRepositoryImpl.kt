package io.biotrace.app.data.repository

import io.biotrace.app.data.local.database.BioTraceDatabase
import io.biotrace.app.domain.repository.SettingsRepository
import io.biotrace.app.domain.security.KeystoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val keystoreManager: KeystoreManager,
    private val database: BioTraceDatabase
) : SettingsRepository {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override suspend fun setApiKey(provider: String, apiKey: String) {
        keystoreManager.storeApiKey(provider, apiKey)
    }

    override suspend fun getApiKey(provider: String): String? {
        return keystoreManager.getApiKey(provider)
    }

    override suspend fun hasApiKey(provider: String): Boolean {
        return keystoreManager.hasApiKey(provider)
    }

    override suspend fun deleteApiKey(provider: String) {
        keystoreManager.deleteApiKey(provider)
    }

    override suspend fun testApiKey(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val payload = JSONObject().apply {
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().put(JSONObject().put("text", "ping")))
                        }
                    )
                )
            }

            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Connected successfully")
                } else {
                    val code = response.code
                    val errorBody = response.body?.string().orEmpty()
                    if (code == 400 && errorBody.contains("API_KEY_INVALID", ignoreCase = true) || code == 403 || code == 401) {
                        Result.failure(Exception("Invalid API key"))
                    } else {
                        Result.failure(Exception("API returned HTTP $code: $errorBody"))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearAllData() = withContext(Dispatchers.IO) {
        database.vitalsDao().deleteAll()
        database.labBiomarkerDao().deleteAll()
        database.mealDao().deleteAll()
        database.chatHistoryDao().clearHistory()
    }
}
