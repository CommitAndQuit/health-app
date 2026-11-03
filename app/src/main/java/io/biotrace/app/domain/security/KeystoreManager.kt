package io.biotrace.app.domain.security

/**
 * Interface for securely storing and retrieving sensitive secrets (like API keys)
 * backed by the Android Keystore.
 */
interface KeystoreManager {
    suspend fun storeApiKey(provider: String, apiKey: String)
    suspend fun getApiKey(provider: String): String?
    suspend fun hasApiKey(provider: String): Boolean
    suspend fun deleteApiKey(provider: String)
}
