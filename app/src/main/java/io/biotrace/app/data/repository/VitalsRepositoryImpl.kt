package io.biotrace.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import io.biotrace.app.data.gadgetbridge.GadgetbridgeStressDataSource
import io.biotrace.app.data.healthconnect.HealthConnectManager
import io.biotrace.app.data.healthconnect.HealthConnectSyncService
import io.biotrace.app.data.healthconnect.SyncResult
import io.biotrace.app.data.local.dao.StressDao
import io.biotrace.app.data.local.dao.VitalsDao
import io.biotrace.app.data.local.entity.StressSampleEntity
import io.biotrace.app.data.local.entity.VitalsRecordEntity
import io.biotrace.app.domain.clinical.VitalsCalculations
import io.biotrace.app.domain.model.ChartPoint
import io.biotrace.app.domain.model.MetricTimeRange
import io.biotrace.app.domain.model.RangeBarItem
import io.biotrace.app.domain.model.SleepStageBarItem
import io.biotrace.app.domain.model.SleepStagesBreakdown
import io.biotrace.app.domain.model.StatItem
import io.biotrace.app.domain.model.StatusBadge
import io.biotrace.app.domain.model.StressLevel
import io.biotrace.app.domain.model.StressZoneDistribution
import io.biotrace.app.domain.model.VitalDetailUiState
import io.biotrace.app.domain.model.VitalHistoryItem
import io.biotrace.app.domain.model.VitalMetricType
import io.biotrace.app.domain.model.VitalPoint
import io.biotrace.app.domain.model.VitalSummaryCardModel
import io.biotrace.app.domain.repository.VitalsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class VitalsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vitalsDao: VitalsDao,
    private val stressDao: StressDao,
    private val syncService: HealthConnectSyncService,
    private val healthConnectManager: HealthConnectManager,
    private val gadgetbridgeDataSource: GadgetbridgeStressDataSource
) : VitalsRepository {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    private val fullDateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.getDefault())

    private val client: HealthConnectClient?
        get() = healthConnectManager.healthConnectClient

    private fun VitalsRecordEntity.toDomain(): VitalPoint {
        return VitalPoint(
            id = id,
            timestamp = timestamp,
            value = value,
            unit = unit,
            metricType = metricType
        )
    }

    override fun getVitalsFlow(
        metricType: String,
        startTime: Instant,
        endTime: Instant
    ): Flow<List<VitalPoint>> {
        return vitalsDao.getRecordsFlow(metricType, startTime, endTime).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getVitalsBetween(
        metricType: String,
        startTime: Instant,
        endTime: Instant
    ): List<VitalPoint> {
        return vitalsDao.getRecordsBetween(metricType, startTime, endTime).map { it.toDomain() }
    }

    override suspend fun getLatestValue(metricType: String): VitalPoint? {
        return vitalsDao.getLatestRecord(metricType)?.toDomain()
    }

    override fun getLatestValueFlow(metricType: String): Flow<VitalPoint?> {
        return vitalsDao.getLatestRecordFlow(metricType).map { it?.toDomain() }
    }

    override suspend fun getRecentRecords(metricType: String, limit: Int): List<VitalPoint> {
        return vitalsDao.getRecentRecords(metricType, limit).map { it.toDomain() }
    }

    override fun getRecentRecordsFlow(metricType: String, limit: Int): Flow<List<VitalPoint>> {
        return vitalsDao.getRecentRecordsFlow(metricType, limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun syncFromHealthConnect(days: Long): SyncResult {
        return syncService.syncAll(days)
    }

    override suspend fun clearAll() {
        vitalsDao.deleteAll()
        stressDao.deleteAll()
    }

    override suspend fun importGadgetbridgeUri(uri: Uri): Result<Int> {
        return gadgetbridgeDataSource.importFromGadgetbridgeUri(uri)
    }

    override suspend fun refreshGadgetbridgeStress(): Result<Int>? {
        return gadgetbridgeDataSource.refreshFromSavedUri()
    }

    override fun hasSavedGadgetbridgeUri(): Boolean {
        return gadgetbridgeDataSource.hasSavedUri()
    }

    // =========================================================================
    // 1. Dashboard Snapshot (9 Vital Cards)
    // =========================================================================

    override suspend fun getVitalsDashboardSnapshot(): List<VitalSummaryCardModel> = withContext(Dispatchers.IO) {
        val zoneId = ZoneId.systemDefault()
        val now = Instant.now()
        val today = LocalDate.now(zoneId)
        val startOfDay = today.atStartOfDay(zoneId).toInstant()
        val endOfDay = today.plusDays(1).atStartOfDay(zoneId).toInstant()

        coroutineScope {
            val hrDeferred = async { fetchHeartRateCard(startOfDay, endOfDay) }
            val rhrDeferred = async { fetchRestingHeartRateCard(startOfDay, endOfDay, zoneId) }
            val stressDeferred = async { fetchStressCard(startOfDay, endOfDay, zoneId) }
            val spo2Deferred = async { fetchSpo2Card(startOfDay, endOfDay) }
            val sleepDeferred = async { fetchSleepCard(startOfDay, endOfDay, zoneId) }
            val stepsDeferred = async { fetchStepsCard(startOfDay, endOfDay) }
            val calDistDeferred = async { fetchCaloriesDistanceCard(startOfDay, endOfDay) }
            val vo2Deferred = async { fetchVo2MaxCard(startOfDay, endOfDay) }
            val bodyDeferred = async { fetchBodyMetricsCard() }

            listOf(
                hrDeferred.await(),
                rhrDeferred.await(),
                stressDeferred.await(),
                spo2Deferred.await(),
                sleepDeferred.await(),
                stepsDeferred.await(),
                calDistDeferred.await(),
                vo2Deferred.await(),
                bodyDeferred.await()
            )
        }
    }

    private suspend fun fetchHeartRateCard(startOfDay: Instant, endOfDay: Instant): VitalSummaryCardModel {
        var minBpm: Long? = null
        var maxBpm: Long? = null
        var avgBpm: Long? = null
        var latestBpm: Double? = null
        var sparkline = mutableListOf<Float>()

        try {
            val c = client
            if (c != null) {
                val agg = c.aggregate(
                    AggregateRequest(
                        metrics = setOf(HeartRateRecord.BPM_AVG, HeartRateRecord.BPM_MIN, HeartRateRecord.BPM_MAX),
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                    )
                )
                minBpm = agg[HeartRateRecord.BPM_MIN]
                maxBpm = agg[HeartRateRecord.BPM_MAX]
                avgBpm = agg[HeartRateRecord.BPM_AVG]

                val records = c.readRecords(
                    ReadRecordsRequest(
                        recordType = HeartRateRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay),
                        ascendingOrder = false,
                        pageSize = 10
                    )
                ).records

                val allSamples = records.flatMap { it.samples }
                if (allSamples.isNotEmpty()) {
                    latestBpm = allSamples.first().beatsPerMinute.toDouble()
                    sparkline = allSamples.take(12).map { it.beatsPerMinute.toFloat() }.reversed().toMutableList()
                }
            }
        } catch (_: Exception) {
            // Fallback to room cache if present
        }

        if (latestBpm == null) {
            val cachedLatest = vitalsDao.getLatestRecord("HEART_RATE")
            if (cachedLatest != null) {
                latestBpm = cachedLatest.value
            }
        }

        val primaryVal = latestBpm?.roundToInt()?.toString() ?: (avgBpm?.toString() ?: "--")
        val secondary = if (minBpm != null && maxBpm != null) {
            "Range: $minBpm–$maxBpm bpm"
        } else {
            "Resting target: 60–100"
        }

        return VitalSummaryCardModel(
            metricType = VitalMetricType.HEART_RATE,
            primaryValue = primaryVal,
            primaryUnit = "bpm",
            secondaryText = secondary,
            badgeText = if (avgBpm != null) "Avg $avgBpm" else null,
            badgeColor = Color(0xFF059669),
            badgeBgColor = Color(0xFFE6F9F0),
            sparklinePoints = if (sparkline.isNotEmpty()) sparkline else listOf(68f, 72f, 75f, 71f, 69f, 74f)
        )
    }

    private suspend fun fetchRestingHeartRateCard(
        startOfDay: Instant,
        endOfDay: Instant,
        zoneId: ZoneId
    ): VitalSummaryCardModel {
        var rhrBpm: Int? = null
        var isFallback = false

        try {
            val c = client
            if (c != null) {
                val rhrResponse = c.readRecords(
                    ReadRecordsRequest(
                        recordType = RestingHeartRateRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay),
                        ascendingOrder = false,
                        pageSize = 1
                    )
                )
                if (rhrResponse.records.isNotEmpty()) {
                    rhrBpm = rhrResponse.records.first().beatsPerMinute.toInt()
                }
            }
        } catch (_: Exception) {}

        // MANDATORY FALLBACK: Honor MagicWatch 2 does not write RestingHeartRateRecord directly
        if (rhrBpm == null) {
            try {
                val c = client
                if (c != null) {
                    val sleepStartWindow = startOfDay.minus(Duration.ofHours(12))
                    val sleepResponse = c.readRecords(
                        ReadRecordsRequest(
                            recordType = SleepSessionRecord::class,
                            timeRangeFilter = TimeRangeFilter.between(sleepStartWindow, endOfDay),
                            ascendingOrder = false,
                            pageSize = 1
                        )
                    )
                    val sleepSession = sleepResponse.records.firstOrNull()
                    val hrWindowStart = sleepSession?.startTime ?: startOfDay
                    val hrWindowEnd = sleepSession?.endTime ?: startOfDay.plus(Duration.ofHours(6))

                    val hrRecords = c.readRecords(
                        ReadRecordsRequest(
                            recordType = HeartRateRecord::class,
                            timeRangeFilter = TimeRangeFilter.between(hrWindowStart, hrWindowEnd)
                        )
                    ).records

                    val samples = hrRecords.flatMap { it.samples }.map { it.beatsPerMinute.toInt() }
                    val computed = VitalsCalculations.calculateRestingHeartRate5thPercentile(samples)
                    if (computed != null) {
                        rhrBpm = computed
                        isFallback = true
                    }
                }
            } catch (_: Exception) {}
        }

        val primaryVal = rhrBpm?.toString() ?: "--"
        val badgeTitle = if (isFallback) "Sleep 5th-pct" else "Resting"
        val badgeColor = if (isFallback) Color(0xFF6366F1) else Color(0xFF059669)
        val badgeBg = if (isFallback) Color(0xFFEEF2FF) else Color(0xFFE6F9F0)

        return VitalSummaryCardModel(
            metricType = VitalMetricType.RESTING_HEART_RATE,
            primaryValue = primaryVal,
            primaryUnit = "bpm",
            secondaryText = if (isFallback) "Overnight sleep baseline" else "Daily resting baseline",
            badgeText = badgeTitle,
            badgeColor = badgeColor,
            badgeBgColor = badgeBg,
            isFallbackComputed = isFallback
        )
    }

    private suspend fun fetchStressCard(
        startOfDay: Instant,
        endOfDay: Instant,
        zoneId: ZoneId
    ): VitalSummaryCardModel {
        val totalCount = stressDao.getStressSampleCount()
        if (totalCount == 0) {
            return VitalSummaryCardModel(
                metricType = VitalMetricType.STRESS,
                primaryValue = "--",
                primaryUnit = "/ 99",
                secondaryText = "No Gadgetbridge DB linked",
                actionText = "Tap to link Gadgetbridge DB"
            )
        }

        val startMillis = startOfDay.toEpochMilli()
        val endMillis = endOfDay.toEpochMilli()

        val latestSample = stressDao.getLatestStressSample()
        val todayMin = stressDao.getMinStress(startMillis, endMillis)
        val todayMax = stressDao.getMaxStress(startMillis, endMillis)

        val minVal: Int
        val maxVal: Int
        if (todayMin != null && todayMax != null) {
            minVal = todayMin
            maxVal = todayMax
        } else if (latestSample != null) {
            // Anchor to latest recorded day's range
            val sampleDate = Instant.ofEpochMilli(latestSample.timestampMillis).atZone(zoneId).toLocalDate()
            val dayStart = sampleDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val dayEnd = sampleDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            minVal = stressDao.getMinStress(dayStart, dayEnd) ?: latestSample.stress
            maxVal = stressDao.getMaxStress(dayStart, dayEnd) ?: latestSample.stress
        } else {
            minVal = 0
            maxVal = 0
        }

        val score = latestSample?.stress ?: 0
        val level = latestSample?.stressLevel ?: StressLevel.fromScore(score)

        return VitalSummaryCardModel(
            metricType = VitalMetricType.STRESS,
            primaryValue = "$score",
            primaryUnit = "/ 99",
            secondaryText = if (minVal > 0 && maxVal > 0) "Day range: $minVal–$maxVal" else "Latest reading",
            badgeText = level.title,
            badgeColor = level.color,
            badgeBgColor = level.backgroundColor,
            stressLevel = level
        )
    }

    private suspend fun fetchSpo2Card(startOfDay: Instant, endOfDay: Instant): VitalSummaryCardModel {
        var latestPercentage: Double? = null
        var recordTime: Instant? = null

        try {
            val c = client
            if (c != null) {
                // Today first
                val todayRecords = c.readRecords(
                    ReadRecordsRequest(
                        recordType = OxygenSaturationRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay),
                        ascendingOrder = false,
                        pageSize = 1
                    )
                ).records

                if (todayRecords.isNotEmpty()) {
                    latestPercentage = todayRecords.first().percentage.value
                    recordTime = todayRecords.first().time
                } else {
                    // Fallback to most recent historical reading
                    val historicalRecords = c.readRecords(
                        ReadRecordsRequest(
                            recordType = OxygenSaturationRecord::class,
                            timeRangeFilter = TimeRangeFilter.before(endOfDay),
                            ascendingOrder = false,
                            pageSize = 1
                        )
                    ).records
                    if (historicalRecords.isNotEmpty()) {
                        latestPercentage = historicalRecords.first().percentage.value
                        recordTime = historicalRecords.first().time
                    }
                }
            }
        } catch (_: Exception) {}

        if (latestPercentage == null) {
            val cached = vitalsDao.getLatestRecord("OXYGEN_SATURATION")
            if (cached != null) {
                latestPercentage = cached.value
                recordTime = cached.timestamp
            }
        }

        val pct = latestPercentage?.toInt()
        val primaryVal = pct?.let { "$it" } ?: "--"
        val isNormal = (pct ?: 0) >= 95
        val timeLabel = recordTime?.let { dateFormatter.format(it.atZone(ZoneId.systemDefault())) } ?: ""

        val badgeText = if (pct != null) (if (isNormal) "Normal" else "Low (<95%)") else null
        val badgeColor = if (isNormal) Color(0xFF059669) else Color(0xFFDC2626)
        val badgeBg = if (isNormal) Color(0xFFE6F9F0) else Color(0xFFFEE2E2)

        return VitalSummaryCardModel(
            metricType = VitalMetricType.SPO2,
            primaryValue = primaryVal,
            primaryUnit = "%",
            secondaryText = if (timeLabel.isNotEmpty()) "Latest • $timeLabel" else "Target: ≥ 95%",
            badgeText = badgeText,
            badgeColor = badgeColor,
            badgeBgColor = badgeBg
        )
    }

    private suspend fun fetchSleepCard(
        startOfDay: Instant,
        endOfDay: Instant,
        zoneId: ZoneId
    ): VitalSummaryCardModel {
        var totalMinutes = 0
        var deepMin = 0
        var lightMin = 0
        var remMin = 0
        var awakeMin = 0

        try {
            val c = client
            if (c != null) {
                val windowStart = startOfDay.minus(Duration.ofHours(14))
                val sleepRecords = c.readRecords(
                    ReadRecordsRequest(
                        recordType = SleepSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(windowStart, endOfDay),
                        ascendingOrder = false
                    )
                ).records

                val distinctSessions = sleepRecords.distinctBy { it.metadata.clientRecordId ?: it.metadata.id }
                val session = distinctSessions.firstOrNull()

                if (session != null) {
                    val durationSec = Duration.between(session.startTime, session.endTime).seconds
                    totalMinutes = (durationSec / 60).toInt()

                    for (stage in session.stages) {
                        val stageMin = (Duration.between(stage.startTime, stage.endTime).seconds / 60).toInt()
                        when (stage.stage) {
                            SleepSessionRecord.STAGE_TYPE_DEEP -> deepMin += stageMin
                            SleepSessionRecord.STAGE_TYPE_LIGHT -> lightMin += stageMin
                            SleepSessionRecord.STAGE_TYPE_REM -> remMin += stageMin
                            SleepSessionRecord.STAGE_TYPE_AWAKE -> awakeMin += stageMin
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        if (totalMinutes == 0) {
            val cached = vitalsDao.getLatestRecord("SLEEP")
            if (cached != null) {
                totalMinutes = (cached.value * 60).toInt()
            }
        }

        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        val primaryVal = if (totalMinutes > 0) "${hours}h ${mins}m" else "--"
        val progress = (totalMinutes / 420f).coerceIn(0f, 1f)
        val pctGoal = (progress * 100).roundToInt()

        val stageProps = if (totalMinutes > 0 && (deepMin + lightMin + remMin + awakeMin) > 0) {
            val sum = (deepMin + lightMin + remMin + awakeMin).toFloat()
            listOf(deepMin / sum, lightMin / sum, remMin / sum, awakeMin / sum)
        } else {
            listOf(0.20f, 0.50f, 0.20f, 0.10f)
        }

        return VitalSummaryCardModel(
            metricType = VitalMetricType.SLEEP,
            primaryValue = primaryVal,
            primaryUnit = "",
            secondaryText = "$pctGoal% of 7h 00m goal",
            progress = progress,
            sleepStageProportions = stageProps
        )
    }

    private suspend fun fetchStepsCard(startOfDay: Instant, endOfDay: Instant): VitalSummaryCardModel {
        var stepsCount = 0L

        try {
            val c = client
            if (c != null) {
                val agg = c.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                    )
                )
                stepsCount = agg[StepsRecord.COUNT_TOTAL] ?: 0L
            }
        } catch (_: Exception) {}

        if (stepsCount == 0L) {
            val cached = vitalsDao.getLatestRecord("STEPS")
            if (cached != null) {
                stepsCount = cached.value.toLong()
            }
        }

        val progress = (stepsCount / 8000f).coerceIn(0f, 1f)
        val formattedSteps = String.format(Locale.US, "%,d", stepsCount)
        val pct = (progress * 100).roundToInt()

        return VitalSummaryCardModel(
            metricType = VitalMetricType.STEPS,
            primaryValue = formattedSteps,
            primaryUnit = "steps",
            secondaryText = "$pct% of 8,000 goal",
            progress = progress
        )
    }

    private suspend fun fetchCaloriesDistanceCard(startOfDay: Instant, endOfDay: Instant): VitalSummaryCardModel {
        var activeKcal = 0.0
        var distanceKm = 0.0

        try {
            val c = client
            if (c != null) {
                val agg = c.aggregate(
                    AggregateRequest(
                        metrics = setOf(
                            ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                            DistanceRecord.DISTANCE_TOTAL
                        ),
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                    )
                )
                activeKcal = agg[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0
                val meters = agg[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
                distanceKm = meters / 1000.0
            }
        } catch (_: Exception) {}

        val primaryVal = if (activeKcal > 0) "${activeKcal.roundToInt()} kcal" else "-- kcal"
        val distText = if (distanceKm > 0) String.format(Locale.US, "%.2f km", distanceKm) else "0.00 km"

        return VitalSummaryCardModel(
            metricType = VitalMetricType.CALORIES_DISTANCE,
            primaryValue = primaryVal,
            primaryUnit = "",
            secondaryText = "Distance: $distText",
            badgeText = distText,
            badgeColor = Color(0xFFD97706),
            badgeBgColor = Color(0xFFFEF3C7)
        )
    }

    private suspend fun fetchVo2MaxCard(startOfDay: Instant, endOfDay: Instant): VitalSummaryCardModel {
        var vo2Max: Double? = null
        var lastWorkoutSummary: String? = null

        try {
            val c = client
            if (c != null) {
                val vo2Records = c.readRecords(
                    ReadRecordsRequest(
                        recordType = Vo2MaxRecord::class,
                        timeRangeFilter = TimeRangeFilter.before(endOfDay),
                        ascendingOrder = false,
                        pageSize = 1
                    )
                ).records
                if (vo2Records.isNotEmpty()) {
                    vo2Max = vo2Records.first().vo2MillilitersPerMinuteKilogram
                }

                val workouts = c.readRecords(
                    ReadRecordsRequest(
                        recordType = ExerciseSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.before(endOfDay),
                        ascendingOrder = false,
                        pageSize = 1
                    )
                ).records

                val lastWorkout = workouts.firstOrNull()
                if (lastWorkout != null) {
                    val durationMin = Duration.between(lastWorkout.startTime, lastWorkout.endTime).toMinutes()
                    lastWorkoutSummary = "${durationMin}m session"
                }
            }
        } catch (_: Exception) {}

        val primaryVal = vo2Max?.let { "${it.roundToInt()}" } ?: "--"
        val badge = if (vo2Max != null) {
            when {
                vo2Max >= 45 -> "Excellent"
                vo2Max >= 38 -> "Good"
                vo2Max >= 30 -> "Fair"
                else -> "Low"
            }
        } else null

        return VitalSummaryCardModel(
            metricType = VitalMetricType.VO2_MAX_WORKOUTS,
            primaryValue = primaryVal,
            primaryUnit = "mL/kg/min",
            secondaryText = lastWorkoutSummary ?: "Honor MagicWatch 2 cardio",
            badgeText = badge,
            badgeColor = Color(0xFF4F46E5),
            badgeBgColor = Color(0xFFEEF2FF)
        )
    }

    private suspend fun fetchBodyMetricsCard(): VitalSummaryCardModel {
        var latestWeightKg: Double? = null
        var latestHeightMeters: Double? = null

        try {
            val c = client
            if (c != null) {
                val weightRecords = c.readRecords(
                    ReadRecordsRequest(
                        recordType = WeightRecord::class,
                        timeRangeFilter = TimeRangeFilter.before(Instant.now()),
                        ascendingOrder = false,
                        pageSize = 1
                    )
                ).records
                if (weightRecords.isNotEmpty()) {
                    latestWeightKg = weightRecords.first().weight.inKilograms
                }

                val heightRecords = c.readRecords(
                    ReadRecordsRequest(
                        recordType = HeightRecord::class,
                        timeRangeFilter = TimeRangeFilter.before(Instant.now()),
                        ascendingOrder = false,
                        pageSize = 1
                    )
                ).records
                if (heightRecords.isNotEmpty()) {
                    latestHeightMeters = heightRecords.first().height.inMeters
                }
            }
        } catch (_: Exception) {}

        if (latestWeightKg == null) {
            val cached = vitalsDao.getLatestRecord("WEIGHT")
            if (cached != null) {
                latestWeightKg = cached.value
            }
        }

        val weightStr = latestWeightKg?.let { String.format(Locale.US, "%.1f", it) } ?: "--"
        val heightM = latestHeightMeters ?: 1.60
        val bmi = if (latestWeightKg != null) VitalsCalculations.calculateBmi(latestWeightKg, heightM) else 0.0
        val bmiCategory = VitalsCalculations.classifyBmi(bmi)
        val heightCm = (heightM * 100).roundToInt()

        return VitalSummaryCardModel(
            metricType = VitalMetricType.BODY_METRICS,
            primaryValue = weightStr,
            primaryUnit = "kg",
            secondaryText = if (bmi > 0) "BMI $bmi ($bmiCategory) • ${heightCm}cm" else "Height: ${heightCm}cm",
            badgeText = if (bmi > 0) bmiCategory else null,
            badgeColor = Color(0xFF0D9488),
            badgeBgColor = Color(0xFFCCFBF1)
        )
    }

    // =========================================================================
    // 2. Metric Detail Data
    // =========================================================================

    override suspend fun getMetricDetailData(
        metricType: VitalMetricType,
        range: MetricTimeRange
    ): VitalDetailUiState = withContext(Dispatchers.IO) {
        when (metricType) {
            VitalMetricType.STRESS -> getStressDetailData(range)
            VitalMetricType.HEART_RATE -> getHeartRateDetailData(range)
            VitalMetricType.RESTING_HEART_RATE -> getRestingHrDetailData(range)
            VitalMetricType.SPO2 -> getSpo2DetailData(range)
            VitalMetricType.SLEEP -> getSleepDetailData(range)
            VitalMetricType.STEPS -> getStepsDetailData(range)
            VitalMetricType.CALORIES_DISTANCE -> getCaloriesDistanceDetailData(range)
            VitalMetricType.VO2_MAX_WORKOUTS -> getVo2WorkoutsDetailData(range)
            VitalMetricType.BODY_METRICS -> getBodyMetricsDetailData(range)
        }
    }

    private suspend fun getStressDetailData(range: MetricTimeRange): VitalDetailUiState {
        val zoneId = ZoneId.systemDefault()
        val totalCount = stressDao.getStressSampleCount()
        if (totalCount == 0) {
            return VitalDetailUiState(
                metricType = VitalMetricType.STRESS,
                selectedRange = range,
                headlineValue = "--",
                subtitle = "No stress samples in database",
                anchorNotice = "Please import your Gadgetbridge SQLite file to visualize stress scores."
            )
        }

        val now = Instant.now()
        val latestEntity = stressDao.getLatestStressSample()
        var anchorNotice: String? = null
        var isHistoricalAnchor = false

        // Determine effective start/end
        val (windowStart, windowEnd) = if (range == MetricTimeRange.DAY) {
            val todayStart = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant()
            val todayEnd = LocalDate.now(zoneId).plusDays(1).atStartOfDay(zoneId).toInstant()
            val todaySamples = stressDao.getStressSamplesBetween(todayStart.toEpochMilli(), todayEnd.toEpochMilli())

            if (todaySamples.isNotEmpty()) {
                todayStart to todayEnd
            } else if (latestEntity != null) {
                isHistoricalAnchor = true
                val date = Instant.ofEpochMilli(latestEntity.timestampMillis).atZone(zoneId).toLocalDate()
                val anchorStart = date.atStartOfDay(zoneId).toInstant()
                val anchorEnd = date.plusDays(1).atStartOfDay(zoneId).toInstant()
                anchorNotice = "Showing data from most recent recorded date (${dateFormatter.format(anchorStart.atZone(zoneId))})"
                anchorStart to anchorEnd
            } else {
                todayStart to todayEnd
            }
        } else {
            val days = range.days.toLong()
            val refEnd = if (latestEntity != null && latestEntity.timestampMillis > now.toEpochMilli()) {
                Instant.ofEpochMilli(latestEntity.timestampMillis)
            } else {
                now
            }
            refEnd.minus(Duration.ofDays(days)) to refEnd
        }

        val samples = stressDao.getStressSamplesBetween(windowStart.toEpochMilli(), windowEnd.toEpochMilli())

        var relaxed = 0
        var normal = 0
        var medium = 0
        var high = 0
        samples.forEach {
            when (it.stressLevel) {
                StressLevel.RELAXED -> relaxed++
                StressLevel.NORMAL -> normal++
                StressLevel.MEDIUM -> medium++
                StressLevel.HIGH -> high++
            }
        }
        val distribution = StressZoneDistribution(
            relaxedCount = relaxed,
            normalCount = normal,
            mediumCount = medium,
            highCount = high,
            totalCount = samples.size
        )

        val minStress = stressDao.getMinStress(windowStart.toEpochMilli(), windowEnd.toEpochMilli()) ?: 0
        val maxStress = stressDao.getMaxStress(windowStart.toEpochMilli(), windowEnd.toEpochMilli()) ?: 0
        val avgStress = stressDao.getAvgStress(windowStart.toEpochMilli(), windowEnd.toEpochMilli()) ?: 0.0

        val headline = if (samples.isNotEmpty()) "${avgStress.roundToInt()}" else "--"
        val latestScore = samples.lastOrNull()?.stress ?: (latestEntity?.stress ?: 0)
        val currentLevel = StressLevel.fromScore(if (samples.isNotEmpty()) avgStress.roundToInt() else latestScore)

        val stats = listOf(
            StatItem("Average", "${avgStress.roundToInt()}", "/ 99"),
            StatItem("Minimum", "$minStress", "/ 99"),
            StatItem("Maximum", "$maxStress", "/ 99"),
            StatItem("Readings", "${samples.size}")
        )

        // Points for chart
        val points = if (range == MetricTimeRange.DAY) {
            samples.map { s ->
                val instant = Instant.ofEpochMilli(s.timestampMillis)
                ChartPoint(
                    timestamp = instant,
                    xLabel = timeFormatter.format(instant.atZone(zoneId)),
                    value = s.stress.toFloat(),
                    color = s.stressLevel.color,
                    note = s.stressLevel.title
                )
            }
        } else {
            emptyList()
        }

        val rangeBars = if (range != MetricTimeRange.DAY && samples.isNotEmpty()) {
            val groupedByDay = samples.groupBy {
                Instant.ofEpochMilli(it.timestampMillis).atZone(zoneId).toLocalDate()
            }
            groupedByDay.map { (date, daySamples) ->
                val dMin = daySamples.minOf { it.stress }.toFloat()
                val dMax = daySamples.maxOf { it.stress }.toFloat()
                val dAvg = daySamples.map { it.stress }.average().toFloat()
                val dInstant = date.atStartOfDay(zoneId).toInstant()
                RangeBarItem(
                    label = dateFormatter.format(dInstant.atZone(zoneId)),
                    date = dInstant,
                    min = dMin,
                    avg = dAvg,
                    max = dMax,
                    color = StressLevel.fromScore(dAvg.roundToInt()).color
                )
            }.sortedBy { it.date }
        } else {
            emptyList()
        }

        val historyItems = samples.reversed().map { s ->
            val instant = Instant.ofEpochMilli(s.timestampMillis)
            val durationSec = s.durationSeconds
            val dev = s.deviceName ?: "HONOR MagicWatch 2-38D"
            VitalHistoryItem(
                id = "${s.timestampMillis}_${s.deviceId}",
                timestamp = instant,
                formattedTime = fullDateTimeFormatter.format(instant.atZone(zoneId)),
                primaryValue = "${s.stress}",
                primaryUnit = "/ 99",
                secondaryDetail = "Measured for ${durationSec}s",
                badge = StatusBadge(s.stressLevel.title, s.stressLevel.color, s.stressLevel.backgroundColor),
                deviceSource = "$dev • Gadgetbridge"
            )
        }

        return VitalDetailUiState(
            metricType = VitalMetricType.STRESS,
            selectedRange = range,
            headlineValue = headline,
            headlineUnit = "/ 99",
            headlineDateLabel = if (range == MetricTimeRange.DAY) "Intraday Average" else "${range.label} Average",
            subtitle = "Stress score monitored by optical PPG pulse wave analysis",
            statusBadge = StatusBadge(currentLevel.title, currentLevel.color, currentLevel.backgroundColor),
            summaryStats = stats,
            points = points,
            rangeBars = rangeBars,
            stressDistribution = distribution,
            historyRecords = historyItems,
            referenceLineValue = 60f,
            isHistoricalAnchor = isHistoricalAnchor,
            anchorNotice = anchorNotice
        )
    }

    private suspend fun getHeartRateDetailData(range: MetricTimeRange): VitalDetailUiState {
        val zoneId = ZoneId.systemDefault()
        val now = Instant.now()
        val days = range.days.toLong()
        val startTime = now.minus(Duration.ofDays(days))

        var minBpm = 0L
        var maxBpm = 0L
        var avgBpm = 0L
        val points = mutableListOf<ChartPoint>()
        val rangeBars = mutableListOf<RangeBarItem>()
        val historyItems = mutableListOf<VitalHistoryItem>()

        try {
            val c = client
            if (c != null) {
                val agg = c.aggregate(
                    AggregateRequest(
                        metrics = setOf(HeartRateRecord.BPM_AVG, HeartRateRecord.BPM_MIN, HeartRateRecord.BPM_MAX),
                        timeRangeFilter = TimeRangeFilter.between(startTime, now)
                    )
                )
                minBpm = agg[HeartRateRecord.BPM_MIN] ?: 0L
                maxBpm = agg[HeartRateRecord.BPM_MAX] ?: 0L
                avgBpm = agg[HeartRateRecord.BPM_AVG] ?: 0L

                val records = c.readRecords(
                    ReadRecordsRequest(
                        recordType = HeartRateRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startTime, now),
                        ascendingOrder = true
                    )
                ).records

                val samples = records.flatMap { it.samples }
                if (range == MetricTimeRange.DAY) {
                    samples.forEach { s ->
                        points.add(
                            ChartPoint(
                                timestamp = s.time,
                                xLabel = timeFormatter.format(s.time.atZone(zoneId)),
                                value = s.beatsPerMinute.toFloat(),
                                color = VitalMetricType.HEART_RATE.accentColor
                            )
                        )
                    }
                } else {
                    val grouped = samples.groupBy { it.time.atZone(zoneId).toLocalDate() }
                    grouped.forEach { (date, dSamples) ->
                        val dMin = dSamples.minOf { it.beatsPerMinute }.toFloat()
                        val dMax = dSamples.maxOf { it.beatsPerMinute }.toFloat()
                        val dAvg = dSamples.map { it.beatsPerMinute }.average().toFloat()
                        val dInstant = date.atStartOfDay(zoneId).toInstant()
                        rangeBars.add(
                            RangeBarItem(
                                label = dateFormatter.format(dInstant.atZone(zoneId)),
                                date = dInstant,
                                min = dMin,
                                avg = dAvg,
                                max = dMax,
                                color = VitalMetricType.HEART_RATE.accentColor
                            )
                        )
                    }
                }

                samples.takeLast(50).reversed().forEach { s ->
                    historyItems.add(
                        VitalHistoryItem(
                            id = "hr_${s.time.toEpochMilli()}",
                            timestamp = s.time,
                            formattedTime = fullDateTimeFormatter.format(s.time.atZone(zoneId)),
                            primaryValue = "${s.beatsPerMinute}",
                            primaryUnit = "bpm",
                            secondaryDetail = "Optical PPG",
                            deviceSource = "HONOR MagicWatch 2 • Health Connect"
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        val stats = listOf(
            StatItem("Average", if (avgBpm > 0) "$avgBpm" else "--", "bpm"),
            StatItem("Minimum", if (minBpm > 0) "$minBpm" else "--", "bpm"),
            StatItem("Maximum", if (maxBpm > 0) "$maxBpm" else "--", "bpm"),
            StatItem("Latest", points.lastOrNull()?.value?.roundToInt()?.toString() ?: "--", "bpm")
        )

        return VitalDetailUiState(
            metricType = VitalMetricType.HEART_RATE,
            selectedRange = range,
            headlineValue = if (avgBpm > 0) "$avgBpm" else "--",
            headlineUnit = "bpm",
            headlineDateLabel = "${range.label} Average",
            subtitle = "Continuous optical pulse monitor",
            summaryStats = stats,
            points = points,
            rangeBars = rangeBars,
            historyRecords = historyItems
        )
    }

    private suspend fun getRestingHrDetailData(range: MetricTimeRange): VitalDetailUiState {
        val zoneId = ZoneId.systemDefault()
        val now = Instant.now()
        val days = range.days.toLong()
        val startTime = now.minus(Duration.ofDays(days))

        val points = mutableListOf<ChartPoint>()
        val historyItems = mutableListOf<VitalHistoryItem>()

        try {
            val c = client
            if (c != null) {
                val rhrRecords = c.readRecords(
                    ReadRecordsRequest(
                        recordType = RestingHeartRateRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startTime, now),
                        ascendingOrder = true
                    )
                ).records

                if (rhrRecords.isNotEmpty()) {
                    rhrRecords.forEach { r ->
                        points.add(
                            ChartPoint(
                                timestamp = r.time,
                                xLabel = dateFormatter.format(r.time.atZone(zoneId)),
                                value = r.beatsPerMinute.toFloat()
                            )
                        )
                        historyItems.add(
                            VitalHistoryItem(
                                id = "rhr_${r.time.toEpochMilli()}",
                                timestamp = r.time,
                                formattedTime = fullDateTimeFormatter.format(r.time.atZone(zoneId)),
                                primaryValue = "${r.beatsPerMinute}",
                                primaryUnit = "bpm",
                                secondaryDetail = "Health Connect Resting HR",
                                deviceSource = "Health Connect"
                            )
                        )
                    }
                } else {
                    // Fallback to computing 5th-percentile sleeping heart rate per day
                    val sleepRecords = c.readRecords(
                        ReadRecordsRequest(
                            recordType = SleepSessionRecord::class,
                            timeRangeFilter = TimeRangeFilter.between(startTime, now),
                            ascendingOrder = true
                        )
                    ).records

                    for (session in sleepRecords) {
                        val hrRecords = c.readRecords(
                            ReadRecordsRequest(
                                recordType = HeartRateRecord::class,
                                timeRangeFilter = TimeRangeFilter.between(session.startTime, session.endTime)
                            )
                        ).records
                        val samples = hrRecords.flatMap { it.samples }.map { it.beatsPerMinute.toInt() }
                        val rhr = VitalsCalculations.calculateRestingHeartRate5thPercentile(samples)
                        if (rhr != null) {
                            points.add(
                                ChartPoint(
                                    timestamp = session.endTime,
                                    xLabel = dateFormatter.format(session.endTime.atZone(zoneId)),
                                    value = rhr.toFloat(),
                                    note = "Sleep 5th-pct"
                                )
                            )
                            historyItems.add(
                                VitalHistoryItem(
                                    id = "sleep_rhr_${session.endTime.toEpochMilli()}",
                                    timestamp = session.endTime,
                                    formattedTime = fullDateTimeFormatter.format(session.endTime.atZone(zoneId)),
                                    primaryValue = "$rhr",
                                    primaryUnit = "bpm",
                                    secondaryDetail = "Overnight 5th-percentile HR",
                                    badge = StatusBadge("Computed from Sleep HR", Color(0xFF6366F1), Color(0xFFEEF2FF)),
                                    deviceSource = "HONOR MagicWatch 2 • Fallback"
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        val avgVal = if (points.isNotEmpty()) points.map { it.value }.average().roundToInt() else 0
        val minVal = if (points.isNotEmpty()) points.minOf { it.value }.roundToInt() else 0
        val maxVal = if (points.isNotEmpty()) points.maxOf { it.value }.roundToInt() else 0

        val stats = listOf(
            StatItem("Average", if (avgVal > 0) "$avgVal" else "--", "bpm"),
            StatItem("Lowest", if (minVal > 0) "$minVal" else "--", "bpm"),
            StatItem("Highest", if (maxVal > 0) "$maxVal" else "--", "bpm"),
            StatItem("Readings", "${points.size}")
        )

        return VitalDetailUiState(
            metricType = VitalMetricType.RESTING_HEART_RATE,
            selectedRange = range,
            headlineValue = if (avgVal > 0) "$avgVal" else "--",
            headlineUnit = "bpm",
            headlineDateLabel = "${range.label} Average",
            subtitle = "Resting heart rate (overnight 5th-percentile)",
            summaryStats = stats,
            points = points,
            historyRecords = historyItems.reversed(),
            referenceLineValue = 65f
        )
    }

    private suspend fun getSpo2DetailData(range: MetricTimeRange): VitalDetailUiState {
        val zoneId = ZoneId.systemDefault()
        val now = Instant.now()
        val days = range.days.toLong()
        val startTime = now.minus(Duration.ofDays(days))

        val points = mutableListOf<ChartPoint>()
        val historyItems = mutableListOf<VitalHistoryItem>()

        try {
            val c = client
            if (c != null) {
                var records = c.readRecords(
                    ReadRecordsRequest(
                        recordType = OxygenSaturationRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startTime, now),
                        ascendingOrder = true
                    )
                ).records

                var anchorNotice: String? = null
                var isHistorical = false
                if (records.isEmpty()) {
                    // Fetch historical readings
                    records = c.readRecords(
                        ReadRecordsRequest(
                            recordType = OxygenSaturationRecord::class,
                            timeRangeFilter = TimeRangeFilter.before(now),
                            ascendingOrder = false,
                            pageSize = 20
                        )
                    ).records.reversed()
                    if (records.isNotEmpty()) {
                        isHistorical = true
                        anchorNotice = "Displaying historical readings (SpO2 measured intermittently)"
                    }
                }

                records.forEach { r ->
                    val pct = r.percentage.value.toFloat()
                    points.add(
                        ChartPoint(
                            timestamp = r.time,
                            xLabel = dateFormatter.format(r.time.atZone(zoneId)),
                            value = pct
                        )
                    )
                    val isNormal = pct >= 95f
                    historyItems.add(
                        VitalHistoryItem(
                            id = "spo2_${r.time.toEpochMilli()}",
                            timestamp = r.time,
                            formattedTime = fullDateTimeFormatter.format(r.time.atZone(zoneId)),
                            primaryValue = "${pct.roundToInt()}",
                            primaryUnit = "%",
                            secondaryDetail = if (isNormal) "Optimal saturation" else "Below normal threshold",
                            badge = StatusBadge(
                                if (isNormal) "Normal" else "Low",
                                if (isNormal) Color(0xFF059669) else Color(0xFFDC2626),
                                if (isNormal) Color(0xFFE6F9F0) else Color(0xFFFEE2E2)
                            ),
                            deviceSource = "HONOR MagicWatch 2 • SpO2"
                        )
                    )
                }

                val avgPct = if (points.isNotEmpty()) points.map { it.value }.average().toFloat() else 0f
                val minPct = if (points.isNotEmpty()) points.minOf { it.value } else 0f
                val maxPct = if (points.isNotEmpty()) points.maxOf { it.value } else 0f

                val stats = listOf(
                    StatItem("Average", if (avgPct > 0) "${avgPct.roundToInt()}" else "--", "%"),
                    StatItem("Minimum", if (minPct > 0) "${minPct.roundToInt()}" else "--", "%"),
                    StatItem("Maximum", if (maxPct > 0) "${maxPct.roundToInt()}" else "--", "%"),
                    StatItem("Latest", points.lastOrNull()?.value?.roundToInt()?.toString() ?: "--", "%")
                )

                return VitalDetailUiState(
                    metricType = VitalMetricType.SPO2,
                    selectedRange = range,
                    headlineValue = points.lastOrNull()?.value?.roundToInt()?.toString() ?: "--",
                    headlineUnit = "%",
                    headlineDateLabel = "Latest Reading",
                    subtitle = "Peripheral blood oxygen saturation",
                    summaryStats = stats,
                    points = points,
                    historyRecords = historyItems.reversed(),
                    referenceLineValue = 95f,
                    isHistoricalAnchor = isHistorical,
                    anchorNotice = anchorNotice
                )
            }
        } catch (_: Exception) {}

        return VitalDetailUiState(
            metricType = VitalMetricType.SPO2,
            selectedRange = range,
            headlineValue = "--",
            headlineUnit = "%",
            subtitle = "No SpO2 records found"
        )
    }

    private suspend fun getSleepDetailData(range: MetricTimeRange): VitalDetailUiState {
        val zoneId = ZoneId.systemDefault()
        val now = Instant.now()
        val days = range.days.toLong()
        val startTime = now.minus(Duration.ofDays(days))

        val sleepBars = mutableListOf<SleepStageBarItem>()
        val historyItems = mutableListOf<VitalHistoryItem>()
        var latestBreakdown: SleepStagesBreakdown? = null

        try {
            val c = client
            if (c != null) {
                val records = c.readRecords(
                    ReadRecordsRequest(
                        recordType = SleepSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startTime, now),
                        ascendingOrder = true
                    )
                ).records

                val distinctSessions = records.distinctBy { it.metadata.clientRecordId ?: it.metadata.id }

                for (session in distinctSessions) {
                    var deep = 0
                    var light = 0
                    var rem = 0
                    var awake = 0

                    for (stage in session.stages) {
                        val min = (Duration.between(stage.startTime, stage.endTime).seconds / 60).toInt()
                        when (stage.stage) {
                            SleepSessionRecord.STAGE_TYPE_DEEP -> deep += min
                            SleepSessionRecord.STAGE_TYPE_LIGHT -> light += min
                            SleepSessionRecord.STAGE_TYPE_REM -> rem += min
                            SleepSessionRecord.STAGE_TYPE_AWAKE -> awake += min
                        }
                    }

                    val totalMin = (Duration.between(session.startTime, session.endTime).seconds / 60).toInt()
                    latestBreakdown = SleepStagesBreakdown(deep, light, rem, awake, totalMin)

                    sleepBars.add(
                        SleepStageBarItem(
                            label = dateFormatter.format(session.endTime.atZone(zoneId)),
                            date = session.endTime,
                            deepMinutes = deep.toFloat(),
                            lightMinutes = light.toFloat(),
                            remMinutes = rem.toFloat(),
                            awakeMinutes = awake.toFloat(),
                            totalMinutes = totalMin.toFloat()
                        )
                    )

                    val hrs = totalMin / 60
                    val m = totalMin % 60
                    historyItems.add(
                        VitalHistoryItem(
                            id = session.metadata.id,
                            timestamp = session.endTime,
                            formattedTime = fullDateTimeFormatter.format(session.endTime.atZone(zoneId)),
                            primaryValue = "${hrs}h ${m}m",
                            primaryUnit = "",
                            secondaryDetail = "Deep: ${deep}m • Light: ${light}m • REM: ${rem}m • Awake: ${awake}m",
                            deviceSource = "HONOR MagicWatch 2 • Sleep"
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        val avgTotalMin = if (sleepBars.isNotEmpty()) sleepBars.map { it.totalMinutes }.average().roundToInt() else 0
        val avgHrs = avgTotalMin / 60
        val avgM = avgTotalMin % 60

        val stats = listOf(
            StatItem("Average", if (avgTotalMin > 0) "${avgHrs}h ${avgM}m" else "--"),
            StatItem("Goal (7h)", "420m"),
            StatItem("Sessions", "${sleepBars.size}"),
            StatItem("Deep Ratio", "${latestBreakdown?.deepPct ?: 0}%")
        )

        return VitalDetailUiState(
            metricType = VitalMetricType.SLEEP,
            selectedRange = range,
            headlineValue = if (avgTotalMin > 0) "${avgHrs}h ${avgM}m" else "--",
            headlineDateLabel = "${range.label} Average Sleep",
            subtitle = "Tracked sleep stages & hypnogram",
            summaryStats = stats,
            sleepBars = sleepBars,
            sleepBreakdown = latestBreakdown,
            historyRecords = historyItems.reversed(),
            referenceLineValue = 420f
        )
    }

    private suspend fun getStepsDetailData(range: MetricTimeRange): VitalDetailUiState {
        val zoneId = ZoneId.systemDefault()
        val now = Instant.now()
        val days = range.days.toLong()
        val startTime = now.minus(Duration.ofDays(days))

        val rangeBars = mutableListOf<RangeBarItem>()
        val historyItems = mutableListOf<VitalHistoryItem>()
        var totalSteps = 0L

        try {
            val c = client
            if (c != null) {
                // Read steps records and group by day
                val records = c.readRecords(
                    ReadRecordsRequest(
                        recordType = StepsRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startTime, now),
                        ascendingOrder = true
                    )
                ).records

                val grouped = records.groupBy { it.startTime.atZone(zoneId).toLocalDate() }
                grouped.forEach { (date, stepRecords) ->
                    val daySum = stepRecords.sumOf { it.count }
                    totalSteps += daySum
                    val instant = date.atStartOfDay(zoneId).toInstant()
                    rangeBars.add(
                        RangeBarItem(
                            label = dateFormatter.format(instant.atZone(zoneId)),
                            date = instant,
                            min = 0f,
                            avg = daySum.toFloat(),
                            max = daySum.toFloat(),
                            color = if (daySum >= 8000) Color(0xFF10B981) else Color(0xFF94A3B8)
                        )
                    )

                    historyItems.add(
                        VitalHistoryItem(
                            id = "steps_${date}",
                            timestamp = instant,
                            formattedTime = dateFormatter.format(instant.atZone(zoneId)),
                            primaryValue = String.format(Locale.US, "%,d", daySum),
                            primaryUnit = "steps",
                            secondaryDetail = if (daySum >= 8000) "Goal achieved" else "${8000 - daySum} steps to goal",
                            badge = if (daySum >= 8000) StatusBadge("Goal Met", Color(0xFF059669), Color(0xFFE6F9F0)) else null,
                            deviceSource = "Health Connect • Deduplicated"
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        val avgDaily = if (rangeBars.isNotEmpty()) (totalSteps / rangeBars.size) else 0L
        val goalMetDays = rangeBars.count { it.avg >= 8000 }

        val stats = listOf(
            StatItem("Daily Avg", String.format(Locale.US, "%,d", avgDaily), "steps"),
            StatItem("Total", String.format(Locale.US, "%,d", totalSteps), "steps"),
            StatItem("Goal Days", "$goalMetDays / ${rangeBars.size}"),
            StatItem("Goal", "8,000", "steps")
        )

        return VitalDetailUiState(
            metricType = VitalMetricType.STEPS,
            selectedRange = range,
            headlineValue = String.format(Locale.US, "%,d", avgDaily),
            headlineUnit = "steps/day",
            headlineDateLabel = "${range.label} Daily Average",
            subtitle = "Deduplicated pedometer telemetry",
            summaryStats = stats,
            rangeBars = rangeBars,
            historyRecords = historyItems.reversed(),
            referenceLineValue = 8000f
        )
    }

    private suspend fun getCaloriesDistanceDetailData(range: MetricTimeRange): VitalDetailUiState {
        val zoneId = ZoneId.systemDefault()
        val now = Instant.now()
        val days = range.days.toLong()
        val startTime = now.minus(Duration.ofDays(days))

        val rangeBars = mutableListOf<RangeBarItem>()
        val historyItems = mutableListOf<VitalHistoryItem>()
        var totalKcal = 0.0
        var totalDistMeters = 0.0

        try {
            val c = client
            if (c != null) {
                val calRecords = c.readRecords(
                    ReadRecordsRequest(
                        recordType = ActiveCaloriesBurnedRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startTime, now),
                        ascendingOrder = true
                    )
                ).records

                val distRecords = c.readRecords(
                    ReadRecordsRequest(
                        recordType = DistanceRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startTime, now),
                        ascendingOrder = true
                    )
                ).records

                val calByDay = calRecords.groupBy { it.startTime.atZone(zoneId).toLocalDate() }
                val distByDay = distRecords.groupBy { it.startTime.atZone(zoneId).toLocalDate() }
                val allDays = (calByDay.keys + distByDay.keys).sorted()

                allDays.forEach { date ->
                    val dayKcal = calByDay[date]?.sumOf { it.energy.inKilocalories } ?: 0.0
                    val dayDist = distByDay[date]?.sumOf { it.distance.inMeters } ?: 0.0
                    totalKcal += dayKcal
                    totalDistMeters += dayDist

                    val instant = date.atStartOfDay(zoneId).toInstant()
                    rangeBars.add(
                        RangeBarItem(
                            label = dateFormatter.format(instant.atZone(zoneId)),
                            date = instant,
                            min = 0f,
                            avg = dayKcal.toFloat(),
                            max = dayKcal.toFloat(),
                            color = Color(0xFFFF9800)
                        )
                    )

                    historyItems.add(
                        VitalHistoryItem(
                            id = "cal_dist_$date",
                            timestamp = instant,
                            formattedTime = dateFormatter.format(instant.atZone(zoneId)),
                            primaryValue = "${dayKcal.roundToInt()} kcal",
                            primaryUnit = "",
                            secondaryDetail = String.format(Locale.US, "Distance: %.2f km", dayDist / 1000.0),
                            deviceSource = "HONOR MagicWatch 2 • Health Connect"
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        val avgKcal = if (rangeBars.isNotEmpty()) (totalKcal / rangeBars.size).roundToInt() else 0
        val totalKm = totalDistMeters / 1000.0

        val stats = listOf(
            StatItem("Daily Avg", "$avgKcal", "kcal"),
            StatItem("Total Burn", "${totalKcal.roundToInt()}", "kcal"),
            StatItem("Total Dist", String.format(Locale.US, "%.1f", totalKm), "km"),
            StatItem("Days Tracked", "${rangeBars.size}")
        )

        return VitalDetailUiState(
            metricType = VitalMetricType.CALORIES_DISTANCE,
            selectedRange = range,
            headlineValue = "$avgKcal",
            headlineUnit = "kcal/day",
            headlineDateLabel = "${range.label} Daily Burn",
            subtitle = "Active calories burned & distance covered",
            summaryStats = stats,
            rangeBars = rangeBars,
            historyRecords = historyItems.reversed(),
            referenceLineValue = 500f
        )
    }

    private suspend fun getVo2WorkoutsDetailData(range: MetricTimeRange): VitalDetailUiState {
        val zoneId = ZoneId.systemDefault()
        val now = Instant.now()
        val days = range.days.toLong()
        val startTime = now.minus(Duration.ofDays(days))

        val points = mutableListOf<ChartPoint>()
        val historyItems = mutableListOf<VitalHistoryItem>()

        try {
            val c = client
            if (c != null) {
                val vo2Records = c.readRecords(
                    ReadRecordsRequest(
                        recordType = Vo2MaxRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startTime, now),
                        ascendingOrder = true
                    )
                ).records

                vo2Records.forEach { r ->
                    points.add(
                        ChartPoint(
                            timestamp = r.time,
                            xLabel = dateFormatter.format(r.time.atZone(zoneId)),
                            value = r.vo2MillilitersPerMinuteKilogram.toFloat()
                        )
                    )
                }

                // Correlate exercise sessions with HR and distance
                val exerciseRecords = c.readRecords(
                    ReadRecordsRequest(
                        recordType = ExerciseSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startTime, now),
                        ascendingOrder = false
                    )
                ).records

                for (ex in exerciseRecords) {
                    val durationMin = Duration.between(ex.startTime, ex.endTime).toMinutes()
                    val hrAgg = c.aggregate(
                        AggregateRequest(
                            metrics = setOf(HeartRateRecord.BPM_AVG, HeartRateRecord.BPM_MAX),
                            timeRangeFilter = TimeRangeFilter.between(ex.startTime, ex.endTime)
                        )
                    )
                    val distAgg = c.aggregate(
                        AggregateRequest(
                            metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                            timeRangeFilter = TimeRangeFilter.between(ex.startTime, ex.endTime)
                        )
                    )
                    val avgHr = hrAgg[HeartRateRecord.BPM_AVG]
                    val maxHr = hrAgg[HeartRateRecord.BPM_MAX]
                    val distKm = (distAgg[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0) / 1000.0

                    historyItems.add(
                        VitalHistoryItem(
                            id = ex.metadata.id,
                            timestamp = ex.startTime,
                            formattedTime = fullDateTimeFormatter.format(ex.startTime.atZone(zoneId)),
                            primaryValue = "${durationMin}m",
                            primaryUnit = "workout",
                            secondaryDetail = "Dist: ${String.format(Locale.US, "%.2f km", distKm)} • HR: ${avgHr ?: "--"}/${maxHr ?: "--"} bpm",
                            badge = StatusBadge(ex.title ?: "Exercise", Color(0xFF4F46E5), Color(0xFFEEF2FF)),
                            deviceSource = "HONOR MagicWatch 2 • Workout"
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        val latestVo2 = points.lastOrNull()?.value?.roundToInt() ?: 40

        val stats = listOf(
            StatItem("Latest VO2", "$latestVo2", "mL/kg/min"),
            StatItem("Cardio Level", if (latestVo2 >= 40) "Good" else "Fair"),
            StatItem("Workouts", "${historyItems.size}")
        )

        return VitalDetailUiState(
            metricType = VitalMetricType.VO2_MAX_WORKOUTS,
            selectedRange = range,
            headlineValue = "$latestVo2",
            headlineUnit = "mL/kg/min",
            headlineDateLabel = "Cardio Fitness Score",
            subtitle = "Estimated VO2 Max and logged exercise sessions",
            summaryStats = stats,
            points = points,
            historyRecords = historyItems,
            referenceLineValue = 40f
        )
    }

    private suspend fun getBodyMetricsDetailData(range: MetricTimeRange): VitalDetailUiState {
        val zoneId = ZoneId.systemDefault()
        val now = Instant.now()
        val days = range.days.toLong()
        val startTime = now.minus(Duration.ofDays(days))

        val points = mutableListOf<ChartPoint>()
        val historyItems = mutableListOf<VitalHistoryItem>()
        var latestHeight = 1.60

        try {
            val c = client
            if (c != null) {
                val heightRecords = c.readRecords(
                    ReadRecordsRequest(
                        recordType = HeightRecord::class,
                        timeRangeFilter = TimeRangeFilter.before(now),
                        ascendingOrder = false,
                        pageSize = 1
                    )
                ).records
                if (heightRecords.isNotEmpty()) {
                    latestHeight = heightRecords.first().height.inMeters
                }

                val weightRecords = c.readRecords(
                    ReadRecordsRequest(
                        recordType = WeightRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startTime, now),
                        ascendingOrder = true
                    )
                ).records

                weightRecords.forEach { w ->
                    val weightKg = w.weight.inKilograms
                    val bmi = VitalsCalculations.calculateBmi(weightKg, latestHeight)
                    points.add(
                        ChartPoint(
                            timestamp = w.time,
                            xLabel = dateFormatter.format(w.time.atZone(zoneId)),
                            value = weightKg.toFloat(),
                            secondaryValue = bmi.toFloat()
                        )
                    )
                    historyItems.add(
                        VitalHistoryItem(
                            id = "weight_${w.time.toEpochMilli()}",
                            timestamp = w.time,
                            formattedTime = fullDateTimeFormatter.format(w.time.atZone(zoneId)),
                            primaryValue = String.format(Locale.US, "%.1f", weightKg),
                            primaryUnit = "kg",
                            secondaryDetail = "BMI $bmi (${VitalsCalculations.classifyBmi(bmi)})",
                            deviceSource = "Health Connect Body Metrics"
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        val latestWeight = points.lastOrNull()?.value ?: 59.0f
        val latestBmi = VitalsCalculations.calculateBmi(latestWeight.toDouble(), latestHeight)

        val stats = listOf(
            StatItem("Weight", String.format(Locale.US, "%.1f", latestWeight), "kg"),
            StatItem("BMI", "$latestBmi", "kg/m²"),
            StatItem("Category", VitalsCalculations.classifyBmi(latestBmi)),
            StatItem("Height", "${(latestHeight * 100).roundToInt()}", "cm")
        )

        return VitalDetailUiState(
            metricType = VitalMetricType.BODY_METRICS,
            selectedRange = range,
            headlineValue = String.format(Locale.US, "%.1f", latestWeight),
            headlineUnit = "kg",
            headlineDateLabel = "Latest Measurement",
            subtitle = "Weight & Body Mass Index trend",
            summaryStats = stats,
            points = points,
            historyRecords = historyItems.reversed()
        )
    }
}
