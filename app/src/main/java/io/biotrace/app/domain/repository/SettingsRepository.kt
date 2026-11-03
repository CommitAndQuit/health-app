package io.biotrace.app.domain.repository

interface SettingsRepository {
    suspend fun setApiKey(provider: String, apiKey: String)
    suspend fun getApiKey(provider: String): String?
    suspend fun hasApiKey(provider: String): Boolean
    suspend fun deleteApiKey(provider: String)
    suspend fun testApiKey(apiKey: String): Result<String>
    suspend fun clearAllData()
}
