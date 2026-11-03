package io.biotrace.app.data.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import io.biotrace.app.data.local.dao.VitalsDao
import io.biotrace.app.data.local.entity.VitalsRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectSyncService @Inject constructor(
    private val healthConnectManager: HealthConnectManager,
    private val vitalsDao: VitalsDao
) {

    suspend fun syncAll(days: Long = 7): SyncResult = withContext(Dispatchers.IO) {
        val endTime = Instant.now()
        val startTime = endTime.minus(Duration.ofDays(days))

        val hrResult = syncHeartRate(startTime, endTime)
        val hrvResult = syncHrv(startTime, endTime)
        val stepsResult = syncSteps(startTime, endTime)
        val sleepResult = syncSleep(startTime, endTime)
        val caloriesResult = syncCalories(startTime, endTime)
        val oxygenResult = syncOxygen(startTime, endTime)
        val weightResult = syncWeight(startTime, endTime)
        val bodyFatResult = syncBodyFat(startTime, endTime)

        hrResult + hrvResult + stepsResult + sleepResult + caloriesResult + oxygenResult + weightResult + bodyFatResult
    }

    suspend fun syncHeartRate(startTime: Instant, endTime: Instant): SyncResult = withContext(Dispatchers.IO) {
        val client = healthConnectManager.healthConnectClient
            ?: return@withContext SyncResult(0, listOf("Health Connect client unavailable"))

        try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            val now = Instant.now()
            val entities = mutableListOf<VitalsRecordEntity>()

            for (record in response.records) {
                for (sample in record.samples) {
                    entities.add(
                        VitalsRecordEntity(
                            id = "hr_${record.metadata.id}_${sample.time.toEpochMilli()}",
                            metricType = "HEART_RATE",
                            value = sample.beatsPerMinute.toDouble(),
                            unit = "bpm",
                            timestamp = sample.time,
                            source = "HEALTH_CONNECT",
                            createdAt = now
                        )
                    )
                }
            }

            if (entities.isNotEmpty()) {
                vitalsDao.insertAll(entities)
            }
            SyncResult(entities.size)
        } catch (e: Exception) {
            SyncResult(0, listOf("Heart Rate sync error: ${e.message}"))
        }
    }

    suspend fun syncHrv(startTime: Instant, endTime: Instant): SyncResult = withContext(Dispatchers.IO) {
        val client = healthConnectManager.healthConnectClient
            ?: return@withContext SyncResult(0, listOf("Health Connect client unavailable"))

        try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateVariabilityRmssdRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            val now = Instant.now()
            val entities = response.records.map { record ->
                VitalsRecordEntity(
                    id = "hrv_${record.metadata.id}",
                    metricType = "HRV",
                    value = record.heartRateVariabilityMillis,
                    unit = "ms",
                    timestamp = record.time,
                    source = "HEALTH_CONNECT",
                    createdAt = now
                )
            }

            if (entities.isNotEmpty()) {
                vitalsDao.insertAll(entities)
            }
            SyncResult(entities.size)
        } catch (e: Exception) {
            SyncResult(0, listOf("HRV sync error: ${e.message}"))
        }
    }

    suspend fun syncSteps(startTime: Instant, endTime: Instant): SyncResult = withContext(Dispatchers.IO) {
        val client = healthConnectManager.healthConnectClient
            ?: return@withContext SyncResult(0, listOf("Health Connect client unavailable"))

        try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            val now = Instant.now()
            val entities = response.records.map { record ->
                VitalsRecordEntity(
                    id = "steps_${record.metadata.id}",
                    metricType = "STEPS",
                    value = record.count.toDouble(),
                    unit = "steps",
                    timestamp = record.startTime,
                    source = "HEALTH_CONNECT",
                    createdAt = now
                )
            }

            if (entities.isNotEmpty()) {
                vitalsDao.insertAll(entities)
            }
            SyncResult(entities.size)
        } catch (e: Exception) {
            SyncResult(0, listOf("Steps sync error: ${e.message}"))
        }
    }

    suspend fun syncSleep(startTime: Instant, endTime: Instant): SyncResult = withContext(Dispatchers.IO) {
        val client = healthConnectManager.healthConnectClient
            ?: return@withContext SyncResult(0, listOf("Health Connect client unavailable"))

        try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            val now = Instant.now()
            val entities = response.records.map { record ->
                val durationHours = Duration.between(record.startTime, record.endTime).toMinutes() / 60.0
                VitalsRecordEntity(
                    id = "sleep_${record.metadata.id}",
                    metricType = "SLEEP",
                    value = durationHours,
                    unit = "hours",
                    timestamp = record.startTime,
                    source = "HEALTH_CONNECT",
                    createdAt = now
                )
            }

            if (entities.isNotEmpty()) {
                vitalsDao.insertAll(entities)
            }
            SyncResult(entities.size)
        } catch (e: Exception) {
            SyncResult(0, listOf("Sleep sync error: ${e.message}"))
        }
    }

    suspend fun syncCalories(startTime: Instant, endTime: Instant): SyncResult = withContext(Dispatchers.IO) {
        val client = healthConnectManager.healthConnectClient
            ?: return@withContext SyncResult(0, listOf("Health Connect client unavailable"))

        try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = ActiveCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            val now = Instant.now()
            val entities = response.records.map { record ->
                VitalsRecordEntity(
                    id = "cal_${record.metadata.id}",
                    metricType = "CALORIES",
                    value = record.energy.inKilocalories,
                    unit = "kcal",
                    timestamp = record.startTime,
                    source = "HEALTH_CONNECT",
                    createdAt = now
                )
            }

            if (entities.isNotEmpty()) {
                vitalsDao.insertAll(entities)
            }
            SyncResult(entities.size)
        } catch (e: Exception) {
            SyncResult(0, listOf("Calories sync error: ${e.message}"))
        }
    }

    suspend fun syncOxygen(startTime: Instant, endTime: Instant): SyncResult = withContext(Dispatchers.IO) {
        val client = healthConnectManager.healthConnectClient
            ?: return@withContext SyncResult(0, listOf("Health Connect client unavailable"))

        try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = OxygenSaturationRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            val now = Instant.now()
            val entities = response.records.map { record ->
                VitalsRecordEntity(
                    id = "spo2_${record.metadata.id}",
                    metricType = "OXYGEN_SATURATION",
                    value = record.percentage.value,
                    unit = "%",
                    timestamp = record.time,
                    source = "HEALTH_CONNECT",
                    createdAt = now
                )
            }

            if (entities.isNotEmpty()) {
                vitalsDao.insertAll(entities)
            }
            SyncResult(entities.size)
        } catch (e: Exception) {
            SyncResult(0, listOf("SpO2 sync error: ${e.message}"))
        }
    }

    suspend fun syncWeight(startTime: Instant, endTime: Instant): SyncResult = withContext(Dispatchers.IO) {
        val client = healthConnectManager.healthConnectClient
            ?: return@withContext SyncResult(0, listOf("Health Connect client unavailable"))

        try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            val now = Instant.now()
            val entities = response.records.map { record ->
                VitalsRecordEntity(
                    id = "weight_${record.metadata.id}",
                    metricType = "WEIGHT",
                    value = record.weight.inKilograms,
                    unit = "kg",
                    timestamp = record.time,
                    source = "HEALTH_CONNECT",
                    createdAt = now
                )
            }

            if (entities.isNotEmpty()) {
                vitalsDao.insertAll(entities)
            }
            SyncResult(entities.size)
        } catch (e: Exception) {
            // Optional telemetry error shouldn't fail the whole sync
            SyncResult(0, listOf("Weight sync: ${e.message}"))
        }
    }

    suspend fun syncBodyFat(startTime: Instant, endTime: Instant): SyncResult = withContext(Dispatchers.IO) {
        val client = healthConnectManager.healthConnectClient
            ?: return@withContext SyncResult(0, listOf("Health Connect client unavailable"))

        try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = BodyFatRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            val now = Instant.now()
            val entities = response.records.map { record ->
                VitalsRecordEntity(
                    id = "bodyfat_${record.metadata.id}",
                    metricType = "BODY_FAT",
                    value = record.percentage.value,
                    unit = "%",
                    timestamp = record.time,
                    source = "HEALTH_CONNECT",
                    createdAt = now
                )
            }

            if (entities.isNotEmpty()) {
                vitalsDao.insertAll(entities)
            }
            SyncResult(entities.size)
        } catch (e: Exception) {
            // Optional telemetry error shouldn't fail the whole sync
            SyncResult(0, listOf("Body fat sync: ${e.message}"))
        }
    }
}
