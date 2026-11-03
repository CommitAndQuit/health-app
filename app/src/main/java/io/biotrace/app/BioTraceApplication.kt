package io.biotrace.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Main application class for BioTrace.
 * 
 * A privacy-first health tracking application that:
 * - Syncs health data from Health Connect
 * - Stores all data locally in encrypted Room database
 * - Provides AI-powered health insights using user's own API key
 * - Never sends data to any backend server except LLM provider
 * 
 * @property TAG Logging tag
 */
@HiltAndroidApp
class BioTraceApplication : Application() {

    companion object {
        private const val TAG = "BioTrace"
    }

    override fun onCreate() {
        super.onCreate()
        // Hilt automatically handles dependency injection initialization
        // via the @HiltAndroidApp annotation
    }
}
