package io.biotrace.app.data.gadgetbridge

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.net.Uri
import io.biotrace.app.data.local.dao.StressDao
import io.biotrace.app.data.local.entity.StressSampleEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GadgetbridgeStressDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stressDao: StressDao
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "gadgetbridge_prefs"
        private const val KEY_SAVED_URI = "saved_db_uri"
        private const val TEMP_DB_NAME = "gadgetbridge_temp.db"
    }

    fun getSavedUri(): Uri? {
        val uriStr = prefs.getString(KEY_SAVED_URI, null) ?: return null
        return runCatching { Uri.parse(uriStr) }.getOrNull()
    }

    fun hasSavedUri(): Boolean {
        return getSavedUri() != null
    }

    fun clearSavedUri() {
        prefs.edit().remove(KEY_SAVED_URI).apply()
    }

    private fun persistUri(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Some file providers do not support persistable permissions
        } catch (_: Exception) {
            // Safe fallback
        }
        prefs.edit().putString(KEY_SAVED_URI, uri.toString()).apply()
    }

    suspend fun refreshFromSavedUri(): Result<Int>? {
        val uri = getSavedUri() ?: return null
        return importFromGadgetbridgeUri(uri)
    }

    suspend fun importFromGadgetbridgeUri(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        persistUri(uri)
        val tempFile = File(context.cacheDir, TEMP_DB_NAME)

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("Could not open input stream from URI: $uri"))

            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            importFromDbFile(tempFile)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    suspend fun importFromDbFile(dbFile: File): Result<Int> = withContext(Dispatchers.IO) {
        var database: SQLiteDatabase? = null
        try {
            database = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )

            val queryWithDevice = """
                SELECT s.TIMESTAMP, s.START_TIME, s.STRESS, s.LEVEL, s.DEVICE_ID, d.NAME, d.MODEL
                FROM HUAWEI_STRESS_SAMPLE s
                LEFT JOIN DEVICE d ON s.DEVICE_ID = d._id
                WHERE s.STRESS > 0
                ORDER BY s.TIMESTAMP ASC
            """.trimIndent()

            val queryFallback = """
                SELECT TIMESTAMP, START_TIME, STRESS, LEVEL, DEVICE_ID, NULL AS NAME, NULL AS MODEL
                FROM HUAWEI_STRESS_SAMPLE
                WHERE STRESS > 0
                ORDER BY TIMESTAMP ASC
            """.trimIndent()

            val cursor = try {
                database.rawQuery(queryWithDevice, null)
            } catch (e: SQLiteException) {
                // If DEVICE table or JOIN fails, fallback to direct table query
                database.rawQuery(queryFallback, null)
            }

            val samples = mutableListOf<StressSampleEntity>()
            cursor.use { c ->
                val colTimestamp = c.getColumnIndexOrThrow("TIMESTAMP")
                val colStartTime = c.getColumnIndexOrThrow("START_TIME")
                val colStress = c.getColumnIndexOrThrow("STRESS")
                val colLevel = c.getColumnIndexOrThrow("LEVEL")
                val colDeviceId = c.getColumnIndexOrThrow("DEVICE_ID")
                val colName = c.getColumnIndex("NAME")
                val colModel = c.getColumnIndex("MODEL")

                while (c.moveToNext()) {
                    val timestamp = c.getLong(colTimestamp)
                    val startTime = c.getLong(colStartTime)
                    val stress = c.getInt(colStress)
                    val level = c.getInt(colLevel)
                    val deviceId = c.getInt(colDeviceId)
                    val deviceName = if (colName >= 0 && !c.isNull(colName)) c.getString(colName) else null
                    val deviceModel = if (colModel >= 0 && !c.isNull(colModel)) c.getString(colModel) else null

                    val rawRow = RawGadgetbridgeStressRow(
                        timestamp = timestamp,
                        startTime = startTime,
                        stress = stress,
                        level = level,
                        deviceId = deviceId,
                        deviceName = deviceName,
                        deviceModel = deviceModel
                    )

                    GadgetbridgeStressParser.parseRow(rawRow)?.let {
                        samples.add(it)
                    }
                }
            }

            if (samples.isNotEmpty()) {
                stressDao.insertAll(samples)
            }

            Result.success(samples.size)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            database?.close()
        }
    }
}
