package io.biotrace.app.data.healthconnect

import android.content.Context

/**
 * Records when BioTrace last completed a Health Connect sync.
 * Deliberately source-agnostic: only a timestamp is stored.
 */
object LastSyncStore {
    private const val PREFS = "sync_status"
    private const val KEY_LAST_SYNC = "last_sync_epoch_ms"

    fun record(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
            .apply()
    }

    fun lastSyncMillis(context: Context): Long? {
        val value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_SYNC, 0L)
        return value.takeIf { it > 0L }
    }
}
