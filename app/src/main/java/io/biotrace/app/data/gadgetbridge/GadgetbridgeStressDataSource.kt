package io.biotrace.app.data.gadgetbridge

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.biotrace.app.data.local.dao.StressDao
import io.biotrace.app.data.local.entity.StressSampleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class GadgetbridgeMetadata(
    val deviceName: String = "No Wearable Connected",
    val deviceModel: String = "Link Gadgetbridge",
    val userName: String = "User Profile",
    val gender: String = "User",
    val stepsGoal: Int = 8000,
    val sleepGoalMinutes: Int = 420,
    val heightCm: Int? = null,
    val weightKg: Double? = null
)

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

        private const val KEY_DEVICE_NAME = "meta_device_name"
        private const val KEY_DEVICE_MODEL = "meta_device_model"
        private const val KEY_USER_NAME = "meta_user_name"
        private const val KEY_USER_GENDER = "meta_user_gender"
        private const val KEY_STEPS_GOAL = "meta_steps_goal"
        private const val KEY_SLEEP_GOAL = "meta_sleep_goal"
        private const val KEY_HEIGHT_CM = "meta_height_cm"
        private const val KEY_WEIGHT_KG = "meta_weight_kg"
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

    fun getMetadata(): GadgetbridgeMetadata {
        return GadgetbridgeMetadata(
            deviceName = prefs.getString(KEY_DEVICE_NAME, null) ?: "No Wearable Connected",
            deviceModel = prefs.getString(KEY_DEVICE_MODEL, null) ?: "Link Gadgetbridge",
            userName = prefs.getString(KEY_USER_NAME, null) ?: "User Profile",
            gender = prefs.getString(KEY_USER_GENDER, null) ?: "User",
            stepsGoal = prefs.getInt(KEY_STEPS_GOAL, 8000),
            sleepGoalMinutes = prefs.getInt(KEY_SLEEP_GOAL, 420),
            heightCm = if (prefs.contains(KEY_HEIGHT_CM)) prefs.getInt(KEY_HEIGHT_CM, 0).takeIf { it > 0 } else null,
            weightKg = if (prefs.contains(KEY_WEIGHT_KG)) prefs.getFloat(KEY_WEIGHT_KG, 0f).toDouble().takeIf { it > 0.0 } else null
        )
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

            // 1. Extract Device & User Metadata if available
            extractMetadata(database)

            // 2. Query HUAWEI_STRESS_SAMPLE
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

    private fun extractMetadata(database: SQLiteDatabase) {
        val editor = prefs.edit()

        // Device
        try {
            database.rawQuery("SELECT NAME, MODEL FROM DEVICE LIMIT 1", null).use { c ->
                if (c.moveToNext()) {
                    val name = c.getString(0)
                    val model = c.getString(1)
                    if (!name.isNullOrBlank()) editor.putString(KEY_DEVICE_NAME, name)
                    if (!model.isNullOrBlank()) editor.putString(KEY_DEVICE_MODEL, model)
                }
            }
        } catch (_: Exception) {}

        // User
        try {
            database.rawQuery("SELECT NAME, GENDER, HEIGHT_CM, WEIGHT_KG FROM USER LIMIT 1", null).use { c ->
                if (c.moveToNext()) {
                    val name = c.getString(0)
                    val genderInt = if (!c.isNull(1)) c.getInt(1) else -1
                    val height = if (!c.isNull(2)) c.getInt(2) else -1
                    val weight = if (!c.isNull(3)) c.getFloat(3) else -1f

                    if (!name.isNullOrBlank()) editor.putString(KEY_USER_NAME, name)
                    val genderStr = when (genderInt) {
                        0 -> "Male"
                        1 -> "Female"
                        else -> "User"
                    }
                    editor.putString(KEY_USER_GENDER, genderStr)
                    if (height > 0) editor.putInt(KEY_HEIGHT_CM, height)
                    if (weight > 0) editor.putFloat(KEY_WEIGHT_KG, weight)
                }
            }
        } catch (_: Exception) {}

        // User Attributes (Goals)
        try {
            database.rawQuery("SELECT STEPS_GOAL_SPD, SLEEP_GOAL_MPD FROM USER_ATTRIBUTES LIMIT 1", null).use { c ->
                if (c.moveToNext()) {
                    val steps = c.getInt(0)
                    val sleep = c.getInt(1)
                    if (steps > 0) editor.putInt(KEY_STEPS_GOAL, steps)
                    if (sleep > 0) editor.putInt(KEY_SLEEP_GOAL, sleep)
                }
            }
        } catch (_: Exception) {}

        editor.apply()
    }
}
