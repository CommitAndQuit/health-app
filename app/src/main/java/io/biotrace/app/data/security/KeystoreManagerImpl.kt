package io.biotrace.app.data.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import io.biotrace.app.domain.security.KeystoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeystoreManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : KeystoreManager {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val PREFS_NAME = "biotrace_secure_prefs"
        private const val KEY_ALIAS_PREFIX = "biotrace_apikey_"
        private const val GCM_TAG_LENGTH = 128
        private const val DELIMITER = ":"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
    }

    private fun getAlias(provider: String): String = "$KEY_ALIAS_PREFIX$provider"

    private fun getOrCreateSecretKey(alias: String): SecretKey {
        if (keyStore.containsAlias(alias)) {
            val entry = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    override suspend fun storeApiKey(provider: String, apiKey: String): Unit = withContext(Dispatchers.IO) {
        val alias = getAlias(provider)
        val secretKey = getOrCreateSecretKey(alias)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))

        val encodedIv = Base64.encodeToString(iv, Base64.NO_WRAP)
        val encodedCiphertext = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        val combined = "$encodedIv$DELIMITER$encodedCiphertext"

        prefs.edit().putString(alias, combined).apply()
    }

    override suspend fun getApiKey(provider: String): String? = withContext(Dispatchers.IO) {
        val alias = getAlias(provider)
        val combined = prefs.getString(alias, null)

        if (combined == null) {
            // Check if key was provisioned for test / local setup via filesDir
            val keyFile = java.io.File(context.filesDir, "gemini_api_key.txt")
            if (keyFile.exists()) {
                val key = keyFile.readText().trim()
                if (key.isNotBlank()) {
                    storeApiKey(provider, key)
                    return@withContext key
                }
            }
            return@withContext null
        }

        val parts = combined.split(DELIMITER)
        if (parts.size != 2) return@withContext null

        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)

        return@withContext try {
            val secretKey = (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.secretKey
                ?: return@withContext null

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(ciphertext)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            val keyFile = java.io.File(context.filesDir, "gemini_api_key.txt")
            if (keyFile.exists()) {
                val key = keyFile.readText().trim()
                if (key.isNotBlank()) {
                    storeApiKey(provider, key)
                    return@withContext key
                }
            }
            null
        }
    }

    override suspend fun hasApiKey(provider: String): Boolean = withContext(Dispatchers.IO) {
        val alias = getAlias(provider)
        prefs.contains(alias) || java.io.File(context.filesDir, "gemini_api_key.txt").exists()
    }

    override suspend fun deleteApiKey(provider: String): Unit = withContext(Dispatchers.IO) {
        val alias = getAlias(provider)
        prefs.edit().remove(alias).apply()
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }
    }
}
