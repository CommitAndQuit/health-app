package io.biotrace.app.data.repository

import android.content.Context
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
import io.biotrace.app.data.healthconnect.HealthConnectManager
import io.biotrace.app.data.healthconnect.HealthConnectSyncService
import io.biotrace.app.data.healthconnect.SyncResult
import io.biotrace.app.data.local.dao.VitalsDao
import io.biotrace.app.data.local.entity.VitalsRecordEntity
import io.biotrace.app.domain.clinical.VitalsCalculations
import io.biotrace.app.domain.model.AlertBannerData
import io.biotrace.app.domain.model.BentoCardTemplate
import io.biotrace.app.domain.model.ChartPoint
import io.biotrace.app.domain.model.ContinuousHeartRateHeroData
import io.biotrace.app.domain.model.MetricTimeRange
import io.biotrace.app.domain.model.ProfileTriptychSummary
import io.biotrace.app.domain.model.RangeBarItem
import io.biotrace.app.domain.model.SleepStageBarItem
import io.biotrace.app.domain.model.SleepStagesBreakdown
import io.biotrace.app.domain.model.StatItem
import io.biotrace.app.domain.model.StatusBadge
import io.biotrace.app.domain.model.UserProfile
import io.biotrace.app.domain.model.VitalDetailUiState
import io.biotrace.app.domain.model.VitalHistoryItem
import io.biotrace.app.domain.model.VitalMetricType
import io.biotrace.app.domain.model.VitalPoint
import io.biotrace.app.domain.model.VitalSummaryCardModel
import io.biotrace.app.domain.repository.VitalsRepository
import io.biotrace.app.ui.theme.BioOrangePrimary
import io.biotrace.app.ui.theme.BioOrangeSoftTint
import io.biotrace.app.ui.theme.CobaltBluePrimary
import io.biotrace.app.ui.theme.CobaltBlueSoftTint
import io.biotrace.app.ui.theme.EmeraldPrimary
import io.biotrace.app.ui.theme.EmeraldSoftTint
import io.biotrace.app.ui.theme.IndigoSleepPrimary
import io.biotrace.app.ui.theme.IndigoSleepSoftTint
import io.biotrace.app.ui.theme.InnerWellSurface
import io.biotrace.app.ui.theme.SolarAmberPrimary
import io.biotrace.app.ui.theme.SolarAmberSoftTint
import io.biotrace.app.ui.theme.TextSecondary
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
    private val syncService: HealthConnectSyncService,
    private val healthConnectManager: HealthConnectManager
) : VitalsRepository {

    private val userProfile = UserProfile()

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
    }

    // =========================================================================
    // 1. Continuous Heart Rate Hero Card Data
    // =========================================================================

    override suspend fun getContinuousHeartRateHeroData(): ContinuousHeartRateHeroData = withContext(Dispatchers.IO) {
        val zoneId = ZoneId.systemDefault()
        val now = Instant.now()
        val today = LocalDate.now(zoneId)
        val startOfDay = today.atStartOfDay(zoneId).toInstant()
        val endOfDay = today.plusDays(1).atStartOfDay(zoneId).toInstant()

        var latestBpm: Int? = null
        var minBpm: Int? = null
        var maxBpm: Int? = null
        var restingHr: Int? = null
        val samplesList = mutableListOf<ChartPoint>()

        try {
            val c = client
            if (c != null) {
                // Read aggregates
                val agg = c.aggregate(
                    AggregateRequest(
                        metrics = setOf(HeartRateRecord.BPM_AVG, HeartRateRecord.BPM_MIN, HeartRateRecord.BPM_MAX),
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                    )
                )
                minBpm = agg[HeartRateRecord.BPM_MIN]?.toInt()
                maxBpm = agg[HeartRateRecord.BPM_MAX]?.toInt()

                // Read continuous samples for today
                var records = c.readRecords(
                    ReadRecordsRequest(
                        recordType = HeartRateRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay),
                        ascendingOrder = true
                    )
                ).records

                // If today has no records yet, anchor to latest recorded day
                if (records.isEmpty()) {
                    val latestRecord = c.readRecords(
                        ReadRecordsRequest(
                            recordType = HeartRateRecord::class,
                            timeRangeFilter = TimeRangeFilter.before(endOfDay),
                            ascendingOrder = false,
                            pageSize = 1
                        )
                    ).records.firstOrNull()

                    if (latestRecord != null) {
                        val anchorDate = latestRecord.startTime.atZone(zoneId).toLocalDate()
                        val aStart = anchorDate.atStartOfDay(zoneId).toInstant()
                        val aEnd = anchorDate.plusDays(1).atStartOfDay(zoneId).toInstant()
                        records = c.readRecords(
                            ReadRecordsRequest(
                                recordType = HeartRateRecord::class,
                                timeRangeFilter = TimeRangeFilter.between(aStart, aEnd),
                                ascendingOrder = true
                            )
                        ).records

                        val anchorAgg = c.aggregate(
                            AggregateRequest(
                                metrics = setOf(HeartRateRecord.BPM_MIN, HeartRateRecord.BPM_MAX),
                                timeRangeFilter = TimeRangeFilter.between(aStart, aEnd)
                            )
                        )
                        minBpm = anchorAgg[HeartRateRecord.BPM_MIN]?.toInt()
                        maxBpm = anchorAgg[HeartRateRecord.BPM_MAX]?.toInt()
                    }
                }

                val allSamples = records.flatMap { it.samples }
                if (allSamples.isNotEmpty()) {
                    latestBpm = allSamples.last().beatsPerMinute.toInt()
                    // Subsample to around 20-30 points for smooth canvas rendering
                    val step = (allSamples.size / 24).coerceAtLeast(1)
                    allSamples.filterIndexed { index, _ -> index % step == 0 }.forEach { s ->
                        samplesList.add(
                            ChartPoint(
                                timestamp = s.time,
                                xLabel = timeFormatter.format(s.time.atZone(zoneId)),
                                value = s.beatsPerMinute.toFloat()
                            )
                        )
                    }
                }

                // Compute resting HR (RestingHeartRateRecord or 5th-percentile sleeping fallback)
                val rhrResponse = c.readRecords(
                    ReadRecordsRequest(
                        recordType = RestingHeartRateRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay.minus(Duration.ofDays(1)), endOfDay),
                        ascendingOrder = false,
                        pageSize = 1
                    )
                ).records
                if (rhrResponse.isNotEmpty()) {
                    restingHr = rhrResponse.first().beatsPerMinute.toInt()
                } else {
                    val sleepSession = c.readRecords(
                        ReadRecordsRequest(
                            recordType = SleepSessionRecord::class,
                            timeRangeFilter = TimeRangeFilter.between(startOfDay.minus(Duration.ofHours(14)), endOfDay),
                            ascendingOrder = false,
                            pageSize = 1
                        )
                    ).records.firstOrNull()

                    val sleepStart = sleepSession?.startTime ?: startOfDay
                    val sleepEnd = sleepSession?.endTime ?: startOfDay.plus(Duration.ofHours(6))
                    val sleepHrSamples = c.readRecords(
                        ReadRecordsRequest(
                            recordType = HeartRateRecord::class,
                            timeRangeFilter = TimeRangeFilter.between(sleepStart, sleepEnd)
                        )
                    ).records.flatMap { it.samples }.map { it.beatsPerMinute.toInt() }

                    restingHr = VitalsCalculations.calculateRestingHeartRate5thPercentile(sleepHrSamples)
                }
            }
        } catch (_: Exception) {}

        if (latestBpm == null) {
            val cached = vitalsDao.getLatestRecord("HEART_RATE")
            if (cached != null) {
                latestBpm = cached.value.toInt()
            }
        }

        ContinuousHeartRateHeroData(
            latestBpm = latestBpm,
            restingHrBpm = restingHr,
            dailyMinBpm = minBpm,
            dailyMaxBpm = maxBpm,
            samples = samplesList
        )
    }

    // =========================================================================
    // 2. Profile & 3-Metric Triptych Summary (Insights Tab Header)
    // =========================================================================

    override suspend fun getProfileTriptychSummary(): ProfileTriptychSummary = withContext(Dispatchers.IO) {
        val heroData = getContinuousHeartRateHeroData()

        var vo2Max = "--"
        var vo2Tier = "No Data"
        var sleepScore = "--%"
        var sleepStatus = "No Data"

        try {
            val c = client
            if (c != null) {
                val vo2 = c.readRecords(
                    ReadRecordsRequest(
                        recordType = Vo2MaxRecord::class,
                        timeRangeFilter = TimeRangeFilter.before(Instant.now()),
                        ascendingOrder = false,
                        pageSize = 1
                    )
                ).records.firstOrNull()

                if (vo2 != null) {
                    val v = vo2.vo2MillilitersPerMinuteKilogram
                    vo2Max = String.format(Locale.US, "%.1f", v)
                    vo2Tier = if (v >= 40.0) "Optimal" else "Fair"
                }

                val sleep = c.readRecords(
                    ReadRecordsRequest(
                        recordType = SleepSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.before(Instant.now()),
                        ascendingOrder = false,
                        pageSize = 1
                    )
                ).records.firstOrNull()

                if (sleep != null) {
                    val durationMin = Duration.between(sleep.startTime, sleep.endTime).toMinutes().toInt()
                    val score = (durationMin * 100 / userProfile.sleepGoalMinutes).coerceIn(0, 100)
                    sleepScore = "$score%"
                    val deepMin = sleep.stages.filter { it.stage == SleepSessionRecord.STAGE_TYPE_DEEP }
                        .sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
                    sleepStatus = if (deepMin >= 60) "Deep / REM OK" else "Light Dominant"
                }
            }
        } catch (_: Exception) {}

        val rhrBpm = heroData.restingHrBpm ?: vitalsDao.getLatestRecord("RESTING_HEART_RATE")?.value?.roundToInt()
        val rhrStr = rhrBpm?.let { "$it bpm" } ?: "-- bpm"
        val rhrDelta = if (rhrBpm != null) "Resting baseline" else "No Data"

        ProfileTriptychSummary(
            userName = userProfile.userName,
            deviceLabel = "Health Connect",
            cardioStatus = if (rhrBpm != null || vo2Max != "--") "CARDIO STATUS • OPTIMAL" else "CARDIO • NO DATA",
            restingHr = rhrStr,
            restingHrDelta = rhrDelta,
            sleepScore = sleepScore,
            sleepStatus = sleepStatus,
            vo2Max = vo2Max,
            vo2MaxTier = vo2Tier
        )
    }

    // =========================================================================
    // 3. Dashboard Snapshot (Bento Grid Cards matching Template A & B)
    // =========================================================================

    override suspend fun getVitalsDashboardSnapshot(): List<VitalSummaryCardModel> = withContext(Dispatchers.IO) {
        val zoneId = ZoneId.systemDefault()
        val now = Instant.now()
        val today = LocalDate.now(zoneId)
        val startOfDay = today.atStartOfDay(zoneId).toInstant()
        val endOfDay = today.plusDays(1).atStartOfDay(zoneId).toInstant()

        coroutineScope {
            // Template A Cards
            val calDeferred = async { fetchCaloriesDistanceCard(startOfDay, endOfDay) }
            val stepsDeferred = async { fetchStepsCard(startOfDay, endOfDay) }
            val spo2Deferred = async { fetchSpo2Card(startOfDay, endOfDay) }
            val sleepDeferred = async { fetchSleepCard(startOfDay, endOfDay, zoneId) }

            // Template B Cards
            val vo2Deferred = async { fetchVo2MaxCard(startOfDay, endOfDay) }
            val rhrDeferred = async { fetchRestingHeartRateCard(startOfDay, endOfDay, zoneId) }
            val bodyDeferred = async { fetchBodyMetricsCard() }

            listOf(
                calDeferred.await(),
                stepsDeferred.await(),
                spo2Deferred.await(),
                sleepDeferred.await(),
                vo2Deferred.await(),
                rhrDeferred.await(),
                bodyDeferred.await()
            )
        }
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

        val primaryVal = if (activeKcal > 0) "${activeKcal.roundToInt()}" else "0"
        val distText = if (distanceKm > 0) String.format(Locale.US, "%.2f km", distanceKm) else "0.00 km"
        val progress = if (activeKcal > 0) (activeKcal / 500.0).coerceIn(0.0, 1.0).toFloat() else 0f

        return VitalSummaryCardModel(
            metricType = VitalMetricType.CALORIES_DISTANCE,
            primaryValue = primaryVal,
            primaryUnit = "kcal",
            secondaryText = distText,
            template = BentoCardTemplate.TEMPLATE_A,
            deltaText = if (activeKcal > 0) "↗ Active" else null,
            progress = progress
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

        val goal = userProfile.stepsGoal.toFloat()
        val progress = (stepsCount / goal).coerceIn(0f, 1f)
        val formattedSteps = String.format(Locale.US, "%,d", stepsCount)
        val pct = (progress * 100).roundToInt()

        return VitalSummaryCardModel(
            metricType = VitalMetricType.STEPS,
            primaryValue = formattedSteps,
            primaryUnit = "steps",
            secondaryText = "Goal: ${String.format(Locale.US, "%,d", userProfile.stepsGoal)}",
            template = BentoCardTemplate.TEMPLATE_A,
            goalText = "Goal: ${String.format(Locale.US, "%,d", userProfile.stepsGoal)}",
            deltaText = "$pct%",
            progress = progress
        )
    }

    private suspend fun fetchSpo2Card(startOfDay: Instant, endOfDay: Instant): VitalSummaryCardModel {
        var latestPercentage: Double? = null
        var recordTime: Instant? = null

        try {
            val c = client
            if (c != null) {
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

        if (latestPercentage == null) {
            return VitalSummaryCardModel(
                metricType = VitalMetricType.SPO2,
                primaryValue = "--",
                primaryUnit = "SpO2",
                secondaryText = "No readings logged",
                template = BentoCardTemplate.TEMPLATE_A,
                badgeText = "No Data",
                badgeColor = TextSecondary,
                badgeBgColor = InnerWellSurface
            )
        }

        val pct = latestPercentage.toInt()
        val isOptimal = pct >= 95
        val timeLabel = recordTime?.let { dateFormatter.format(it.atZone(ZoneId.systemDefault())) } ?: "Today"

        return VitalSummaryCardModel(
            metricType = VitalMetricType.SPO2,
            primaryValue = "$pct%",
            primaryUnit = "SpO2",
            secondaryText = timeLabel,
            template = BentoCardTemplate.TEMPLATE_A,
            badgeText = if (isOptimal) "• Optimal" else "• Below 95%",
            badgeColor = if (isOptimal) CobaltBluePrimary else SolarAmberPrimary,
            badgeBgColor = if (isOptimal) CobaltBlueSoftTint else SolarAmberSoftTint
        )
    }

    private suspend fun fetchSleepCard(
        startOfDay: Instant,
        endOfDay: Instant,
        zoneId: ZoneId
    ): VitalSummaryCardModel {
        var totalMinutes = 0
        var deepMin = 0

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

                val session = sleepRecords.distinctBy { it.metadata.clientRecordId ?: it.metadata.id }.firstOrNull()
                if (session != null) {
                    val durationSec = Duration.between(session.startTime, session.endTime).seconds
                    totalMinutes = (durationSec / 60).toInt()
                    deepMin = session.stages.filter { it.stage == SleepSessionRecord.STAGE_TYPE_DEEP }
                        .sumOf { (Duration.between(it.startTime, it.endTime).seconds / 60).toInt() }
                }
            }
        } catch (_: Exception) {}

        if (totalMinutes == 0) {
            val cached = vitalsDao.getLatestRecord("SLEEP")
            if (cached != null) {
                totalMinutes = (cached.value * 60).toInt()
            }
        }

        if (totalMinutes == 0) {
            return VitalSummaryCardModel(
                metricType = VitalMetricType.SLEEP,
                primaryValue = "--",
                primaryUnit = "",
                secondaryText = "No sleep logged",
                template = BentoCardTemplate.TEMPLATE_A,
                badgeText = "No Session",
                badgeColor = TextSecondary,
                badgeBgColor = InnerWellSurface
            )
        }

        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        val sleepScore = ((totalMinutes * 100) / userProfile.sleepGoalMinutes).coerceIn(0, 100)
        val deepRatio = if (totalMinutes > 0) (deepMin * 100 / totalMinutes) else 0

        return VitalSummaryCardModel(
            metricType = VitalMetricType.SLEEP,
            primaryValue = "$sleepScore",
            primaryUnit = "score",
            secondaryText = if (deepRatio >= 20) "Deep / REM OK" else "Light Dominant",
            template = BentoCardTemplate.TEMPLATE_A,
            badgeText = "${hours}h ${mins}m",
            badgeColor = IndigoSleepPrimary,
            badgeBgColor = IndigoSleepSoftTint
        )
    }

    private suspend fun fetchVo2MaxCard(startOfDay: Instant, endOfDay: Instant): VitalSummaryCardModel {
        var vo2Max: Double? = null
        var lastDurationMin: Long? = null
        var peakHr: Long? = null

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
                    lastDurationMin = Duration.between(lastWorkout.startTime, lastWorkout.endTime).toMinutes()
                    val hrAgg = c.aggregate(
                        AggregateRequest(
                            metrics = setOf(HeartRateRecord.BPM_MAX),
                            timeRangeFilter = TimeRangeFilter.between(lastWorkout.startTime, lastWorkout.endTime)
                        )
                    )
                    hrAgg[HeartRateRecord.BPM_MAX]?.let { peakHr = it }
                }
            }
        } catch (_: Exception) {}

        if (vo2Max == null) {
            val cached = vitalsDao.getLatestRecord("VO2_MAX")
            if (cached != null) {
                vo2Max = cached.value
            }
        }

        if (vo2Max == null) {
            return VitalSummaryCardModel(
                metricType = VitalMetricType.VO2_MAX_WORKOUTS,
                primaryValue = "--",
                primaryUnit = "mL/kg",
                secondaryText = "Cardio VO2 Max",
                badgeText = "NO DATA",
                badgeColor = TextSecondary,
                badgeBgColor = InnerWellSurface,
                template = BentoCardTemplate.TEMPLATE_B,
                recessedWellLeftLabel = "Last Workout",
                recessedWellLeftValue = lastDurationMin?.let { "${it}m" } ?: "--",
                recessedWellRightLabel = "Peak HR",
                recessedWellRightValue = peakHr?.let { "$it bpm" } ?: "--",
                recessedWellRightColor = TextSecondary
            )
        }

        val tier = if (vo2Max >= 40.0) "OPTIMAL" else "GOOD"

        return VitalSummaryCardModel(
            metricType = VitalMetricType.VO2_MAX_WORKOUTS,
            primaryValue = String.format(Locale.US, "%.1f", vo2Max),
            primaryUnit = "mL/kg",
            secondaryText = "Cardio VO2 Max",
            badgeText = tier,
            badgeColor = BioOrangePrimary,
            badgeBgColor = BioOrangeSoftTint,
            template = BentoCardTemplate.TEMPLATE_B,
            recessedWellLeftLabel = "Last Workout",
            recessedWellLeftValue = lastDurationMin?.let { "${it}m" } ?: "--",
            recessedWellRightLabel = "Peak HR",
            recessedWellRightValue = peakHr?.let { "$it bpm" } ?: "--",
            recessedWellRightColor = EmeraldPrimary
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

        if (rhrBpm == null) {
            try {
                val c = client
                if (c != null) {
                    val sleepStartWindow = startOfDay.minus(Duration.ofHours(12))
                    val sleepSession = c.readRecords(
                        ReadRecordsRequest(
                            recordType = SleepSessionRecord::class,
                            timeRangeFilter = TimeRangeFilter.between(sleepStartWindow, endOfDay),
                            ascendingOrder = false,
                            pageSize = 1
                        )
                    ).records.firstOrNull()

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

        if (rhrBpm == null) {
            return VitalSummaryCardModel(
                metricType = VitalMetricType.RESTING_HEART_RATE,
                primaryValue = "--",
                primaryUnit = "bpm",
                secondaryText = "Resting Heart Rate",
                badgeText = "NO DATA",
                badgeColor = TextSecondary,
                badgeBgColor = InnerWellSurface,
                isFallbackComputed = false,
                template = BentoCardTemplate.TEMPLATE_B,
                recessedWellLeftLabel = "7D Baseline",
                recessedWellLeftValue = "--",
                recessedWellRightLabel = "Delta",
                recessedWellRightValue = "--",
                recessedWellRightColor = TextSecondary
            )
        }

        val badge = if (isFallback) "SLEEP 5TH%" else "RESTING"

        return VitalSummaryCardModel(
            metricType = VitalMetricType.RESTING_HEART_RATE,
            primaryValue = "$rhrBpm",
            primaryUnit = "bpm",
            secondaryText = "Resting Heart Rate",
            badgeText = badge,
            badgeColor = BioOrangePrimary,
            badgeBgColor = BioOrangeSoftTint,
            isFallbackComputed = isFallback,
            template = BentoCardTemplate.TEMPLATE_B,
            recessedWellLeftLabel = "7D Baseline",
            recessedWellLeftValue = "$rhrBpm bpm",
            recessedWellRightLabel = "Delta",
            recessedWellRightValue = "Stable",
            recessedWellRightColor = EmeraldPrimary
        )
    }

    private suspend fun fetchBodyMetricsCard(): VitalSummaryCardModel {
        var weightKg: Double? = null
        var heightMeters: Double? = null

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
                    weightKg = weightRecords.first().weight.inKilograms
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
                    heightMeters = heightRecords.first().height.inMeters
                }
            }
        } catch (_: Exception) {}

        val w = weightKg
        val h = heightMeters

        if (w == null) {
            return VitalSummaryCardModel(
                metricType = VitalMetricType.BODY_METRICS,
                primaryValue = "--",
                primaryUnit = "kg",
                secondaryText = "Body Mass & Height",
                badgeText = "NO DATA",
                badgeColor = TextSecondary,
                badgeBgColor = InnerWellSurface,
                template = BentoCardTemplate.TEMPLATE_B,
                recessedWellLeftLabel = "Height",
                recessedWellLeftValue = if (h != null) "${(h * 100).roundToInt()} cm" else "--",
                recessedWellRightLabel = "Status",
                recessedWellRightValue = "--",
                recessedWellRightColor = TextSecondary
            )
        }

        val heightUsed = h ?: 1.70
        val bmi = VitalsCalculations.calculateBmi(w, heightUsed)
        val bmiCategory = VitalsCalculations.classifyBmi(bmi)
        val heightCmStr = if (h != null) "${(h * 100).roundToInt()} cm" else "--"

        return VitalSummaryCardModel(
            metricType = VitalMetricType.BODY_METRICS,
            primaryValue = String.format(Locale.US, "%.1f", w),
            primaryUnit = "kg",
            secondaryText = "Body Mass & Height",
            badgeText = "BMI $bmi",
            badgeColor = CobaltBluePrimary,
            badgeBgColor = CobaltBlueSoftTint,
            template = BentoCardTemplate.TEMPLATE_B,
            recessedWellLeftLabel = "Height",
            recessedWellLeftValue = heightCmStr,
            recessedWellRightLabel = "Status",
            recessedWellRightValue = bmiCategory,
            recessedWellRightColor = EmeraldPrimary
        )
    }

    // =========================================================================
    // 4. Metric Detail Drill-Down Data (VitalDetailScreen)
    // =========================================================================

    override suspend fun getMetricDetailData(
        metricType: VitalMetricType,
        range: MetricTimeRange
    ): VitalDetailUiState = withContext(Dispatchers.IO) {
        when (metricType) {
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
                                color = BioOrangePrimary
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
                                color = BioOrangePrimary
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
                            secondaryDetail = "Optical PPG • Sinus Rhythm",
                            deviceSource = "Health Connect"
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        val stats = if (minBpm > 0 || avgBpm > 0 || maxBpm > 0) {
            listOf(
                StatItem("Minimum", if (minBpm > 0) "$minBpm" else "--", "bpm"),
                StatItem("Average", if (avgBpm > 0) "$avgBpm" else "--", "bpm"),
                StatItem("Maximum", if (maxBpm > 0) "$maxBpm" else "--", "bpm")
            )
        } else {
            emptyList()
        }

        val insight = if (avgBpm > 0 || points.isNotEmpty() || rangeBars.isNotEmpty()) {
            "Daily range spanned $minBpm–$maxBpm bpm with an average cardiovascular pace of $avgBpm bpm across active intervals."
        } else {
            "No heart rate readings recorded for this range."
        }

        return VitalDetailUiState(
            metricType = VitalMetricType.HEART_RATE,
            selectedRange = range,
            headlineValue = if (avgBpm > 0) "$avgBpm" else "--",
            headlineUnit = "bpm",
            headlineDateLabel = "${range.label} Average",
            subtitle = "Continuous optical pulse monitor",
            calloutInsight = insight,
            summaryStats = stats,
            points = points,
            rangeBars = rangeBars,
            historyRecords = historyItems,
            dataSourceLabel = "Health Connect"
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
                                secondaryDetail = "Overnight 5th-percentile sleeping HR",
                                badge = StatusBadge("SLEEP 5TH%", BioOrangePrimary, BioOrangeSoftTint),
                                deviceSource = "Health Connect • Derived"
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        if (points.isEmpty()) {
            return VitalDetailUiState(
                metricType = VitalMetricType.RESTING_HEART_RATE,
                selectedRange = range,
                headlineValue = "--",
                headlineUnit = "bpm",
                headlineDateLabel = "${range.label} Baseline",
                subtitle = "Cardiovascular recovery baseline",
                calloutInsight = "No resting heart rate data recorded for this time range. Ensure Health Connect permissions are granted.",
                summaryStats = emptyList(),
                points = emptyList(),
                historyRecords = emptyList(),
                dataSourceLabel = "Health Connect • Derived"
            )
        }

        val avgVal = points.map { it.value }.average().roundToInt()
        val minVal = points.minOf { it.value }.roundToInt()
        val maxVal = points.maxOf { it.value }.roundToInt()

        val stats = listOf(
            StatItem("Lowest", "$minVal", "bpm"),
            StatItem("Average", "$avgVal", "bpm"),
            StatItem("Highest", "$maxVal", "bpm")
        )

        val insight = "Overnight resting heart rate baseline averaged $avgVal bpm, computed dynamically from sleep window 5th-percentile samples."

        return VitalDetailUiState(
            metricType = VitalMetricType.RESTING_HEART_RATE,
            selectedRange = range,
            headlineValue = "$avgVal",
            headlineUnit = "bpm",
            headlineDateLabel = "${range.label} Baseline",
            subtitle = "Cardiovascular recovery baseline",
            calloutInsight = insight,
            summaryStats = stats,
            points = points,
            historyRecords = historyItems.reversed(),
            referenceLineValue = 65f,
            dataSourceLabel = "Health Connect • Derived"
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

                var isHistorical = false
                if (records.isEmpty()) {
                    records = c.readRecords(
                        ReadRecordsRequest(
                            recordType = OxygenSaturationRecord::class,
                            timeRangeFilter = TimeRangeFilter.before(now),
                            ascendingOrder = false,
                            pageSize = 20
                        )
                    ).records.reversed()
                    isHistorical = true
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
                    val isOptimal = pct >= 95f
                    historyItems.add(
                        VitalHistoryItem(
                            id = "spo2_${r.time.toEpochMilli()}",
                            timestamp = r.time,
                            formattedTime = fullDateTimeFormatter.format(r.time.atZone(zoneId)),
                            primaryValue = "${pct.roundToInt()}%",
                            primaryUnit = "SpO2",
                            secondaryDetail = if (isOptimal) "Optimal saturation" else "Below normal threshold",
                            badge = StatusBadge(
                                if (isOptimal) "• Optimal" else "• Below 95%",
                                if (isOptimal) CobaltBluePrimary else SolarAmberPrimary,
                                if (isOptimal) CobaltBlueSoftTint else SolarAmberSoftTint
                            ),
                            deviceSource = "Health Connect"
                        )
                    )
                }

                if (points.isEmpty()) {
                    return VitalDetailUiState(
                        metricType = VitalMetricType.SPO2,
                        selectedRange = range,
                        headlineValue = "--",
                        headlineUnit = "SpO2",
                        headlineDateLabel = "${range.label} Readings",
                        subtitle = "Peripheral blood oxygen saturation",
                        calloutInsight = "No blood oxygen readings recorded for this range. Ensure Health Connect permissions are granted.",
                        summaryStats = emptyList(),
                        points = emptyList(),
                        historyRecords = emptyList(),
                        dataSourceLabel = "Health Connect"
                    )
                }

                val avgPct = points.map { it.value }.average().toFloat().roundToInt()
                val minPct = points.minOf { it.value }.roundToInt()
                val maxPct = points.maxOf { it.value }.roundToInt()

                val stats = listOf(
                    StatItem("Minimum", "$minPct%", ""),
                    StatItem("Average", "$avgPct%", ""),
                    StatItem("Maximum", "$maxPct%", "")
                )

                val insight = "Blood oxygen saturation averaged $avgPct% with all readings maintaining optimal cellular oxygenation above clinical thresholds."

                return VitalDetailUiState(
                    metricType = VitalMetricType.SPO2,
                    selectedRange = range,
                    headlineValue = "${points.last().value.roundToInt()}%",
                    headlineUnit = "SpO2",
                    headlineDateLabel = "Latest Reading",
                    subtitle = "Peripheral blood oxygen saturation",
                    calloutInsight = insight,
                    summaryStats = stats,
                    points = points,
                    historyRecords = historyItems.reversed(),
                    referenceLineValue = 95f,
                    isHistoricalAnchor = isHistorical,
                    dataSourceLabel = "Health Connect"
                )
            }
        } catch (_: Exception) {}

        return VitalDetailUiState(
            metricType = VitalMetricType.SPO2,
            selectedRange = range,
            headlineValue = "--",
            headlineUnit = "SpO2",
            subtitle = "Peripheral blood oxygen saturation",
            calloutInsight = "No blood oxygen readings recorded for this range.",
            summaryStats = emptyList(),
            points = emptyList(),
            historyRecords = emptyList(),
            dataSourceLabel = "Health Connect"
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
                            secondaryDetail = "Deep: ${deep}m • REM: ${rem}m • Light: ${light}m",
                            badge = StatusBadge("SLEEP STAGES", IndigoSleepPrimary, IndigoSleepSoftTint),
                            deviceSource = "Health Connect"
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        if (sleepBars.isEmpty()) {
            return VitalDetailUiState(
                metricType = VitalMetricType.SLEEP,
                selectedRange = range,
                headlineValue = "--",
                headlineDateLabel = "${range.label} Average Sleep",
                subtitle = "Sleep stages recorded in Health Connect",
                calloutInsight = "No sleep sessions recorded for this range. Wear your watch to bed to track sleep architecture.",
                summaryStats = emptyList(),
                sleepBars = emptyList(),
                historyRecords = emptyList(),
                dataSourceLabel = "Health Connect"
            )
        }

        val avgTotalMin = sleepBars.map { it.totalMinutes }.average().roundToInt()
        val avgHrs = avgTotalMin / 60
        val avgM = avgTotalMin % 60
        val deepRatio = latestBreakdown?.deepPct ?: 0

        val stats = listOf(
            StatItem("Deep Sleep", "${latestBreakdown?.deepMinutes ?: 0}m", ""),
            StatItem("Average", "${avgHrs}h ${avgM}m", ""),
            StatItem("REM Sleep", "${latestBreakdown?.remMinutes ?: 0}m", "")
        )

        val insight = "Average nightly duration is ${avgHrs}h ${avgM}m with $deepRatio% deep restorative sleep, supporting autonomic nervous system repair."

        return VitalDetailUiState(
            metricType = VitalMetricType.SLEEP,
            selectedRange = range,
            headlineValue = "${avgHrs}h ${avgM}m",
            headlineDateLabel = "${range.label} Average Sleep",
            subtitle = "Sleep stages recorded in Health Connect",
            calloutInsight = insight,
            summaryStats = stats,
            sleepBars = sleepBars,
            sleepBreakdown = latestBreakdown,
            historyRecords = historyItems.reversed(),
            referenceLineValue = 420f,
            dataSourceLabel = "Health Connect"
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
                            color = if (daySum >= 8000) EmeraldPrimary else SolarAmberPrimary
                        )
                    )

                    historyItems.add(
                        VitalHistoryItem(
                            id = "steps_${date}",
                            timestamp = instant,
                            formattedTime = dateFormatter.format(instant.atZone(zoneId)),
                            primaryValue = String.format(Locale.US, "%,d", daySum),
                            primaryUnit = "steps",
                            secondaryDetail = if (daySum >= 8000) "Goal achieved" else "${8000 - daySum} steps remaining",
                            badge = if (daySum >= 8000) StatusBadge("GOAL MET", EmeraldPrimary, EmeraldSoftTint) else null,
                            deviceSource = "Health Connect • Deduplicated"
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        if (rangeBars.isEmpty() && totalSteps == 0L) {
            return VitalDetailUiState(
                metricType = VitalMetricType.STEPS,
                selectedRange = range,
                headlineValue = "0",
                headlineUnit = "steps",
                headlineDateLabel = "${range.label} Pedometer",
                subtitle = "Deduplicated pedometer telemetry",
                calloutInsight = "No steps recorded for this range. Ensure Health Connect permissions are granted and sync is active.",
                summaryStats = emptyList(),
                rangeBars = emptyList(),
                historyRecords = emptyList(),
                referenceLineValue = 8000f,
                dataSourceLabel = "Health Connect"
            )
        }

        val avgDaily = totalSteps / rangeBars.size.coerceAtLeast(1)
        val goalMetDays = rangeBars.count { it.avg >= 8000 }

        val stats = listOf(
            StatItem("Daily Avg", String.format(Locale.US, "%,d", avgDaily), "steps"),
            StatItem("Goal Days", "$goalMetDays / ${rangeBars.size.coerceAtLeast(1)}", ""),
            StatItem("Total", String.format(Locale.US, "%,d", totalSteps), "steps")
        )

        val insight = "Total steps logged: ${String.format(Locale.US, "%,d", totalSteps)}. Daily goal was achieved on $goalMetDays of ${rangeBars.size.coerceAtLeast(1)} tracked days."

        return VitalDetailUiState(
            metricType = VitalMetricType.STEPS,
            selectedRange = range,
            headlineValue = String.format(Locale.US, "%,d", avgDaily),
            headlineUnit = "steps/day",
            headlineDateLabel = "${range.label} Daily Average",
            subtitle = "Deduplicated pedometer telemetry",
            calloutInsight = insight,
            summaryStats = stats,
            rangeBars = rangeBars,
            historyRecords = historyItems.reversed(),
            referenceLineValue = 8000f,
            dataSourceLabel = "Health Connect"
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
                            color = BioOrangePrimary
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
                            deviceSource = "Health Connect"
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        if (rangeBars.isEmpty() && totalKcal == 0.0) {
            return VitalDetailUiState(
                metricType = VitalMetricType.CALORIES_DISTANCE,
                selectedRange = range,
                headlineValue = "0",
                headlineUnit = "kcal/day",
                headlineDateLabel = "${range.label} Daily Burn",
                subtitle = "Active calories burned & distance covered",
                calloutInsight = "No active energy expenditure or distance recorded for this range.",
                summaryStats = emptyList(),
                rangeBars = emptyList(),
                historyRecords = emptyList(),
                referenceLineValue = 500f,
                dataSourceLabel = "Health Connect"
            )
        }

        val avgKcal = (totalKcal / rangeBars.size).roundToInt()
        val totalKm = totalDistMeters / 1000.0

        val stats = listOf(
            StatItem("Daily Avg", "$avgKcal", "kcal"),
            StatItem("Total Burn", "${totalKcal.roundToInt()}", "kcal"),
            StatItem("Total Dist", String.format(Locale.US, "%.1f", totalKm), "km")
        )

        val insight = "Active energy expenditure reached ${totalKcal.roundToInt()} kcal over ${String.format(Locale.US, "%.1f", totalKm)} km of physical distance."

        return VitalDetailUiState(
            metricType = VitalMetricType.CALORIES_DISTANCE,
            selectedRange = range,
            headlineValue = "$avgKcal",
            headlineUnit = "kcal/day",
            headlineDateLabel = "${range.label} Daily Burn",
            subtitle = "Active calories burned & distance covered",
            calloutInsight = insight,
            summaryStats = stats,
            rangeBars = rangeBars,
            historyRecords = historyItems.reversed(),
            referenceLineValue = 500f,
            dataSourceLabel = "Health Connect"
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
                            badge = StatusBadge(ex.title ?: "Workout", BioOrangePrimary, BioOrangeSoftTint),
                            deviceSource = "Health Connect"
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        if (points.isEmpty() && historyItems.isEmpty()) {
            return VitalDetailUiState(
                metricType = VitalMetricType.VO2_MAX_WORKOUTS,
                selectedRange = range,
                headlineValue = "--",
                headlineUnit = "mL/kg",
                headlineDateLabel = "Cardio Fitness Score",
                subtitle = "Estimated VO2 Max and logged exercise sessions",
                calloutInsight = "No VO2 Max or workout sessions recorded for this range.",
                summaryStats = emptyList(),
                points = emptyList(),
                historyRecords = emptyList(),
                referenceLineValue = 40f,
                dataSourceLabel = "Health Connect"
            )
        }

        val latestVo2 = points.lastOrNull()?.value?.roundToInt()
        val tier = if (latestVo2 != null) (if (latestVo2 >= 40) "Optimal" else "Good") else "Unrated"

        val stats = listOfNotNull(
            latestVo2?.let { StatItem("Score", "$it", "mL/kg") },
            StatItem("Tier", tier, ""),
            StatItem("Workouts", "${historyItems.size}", "")
        )

        val insight = if (latestVo2 != null) {
            "Cardiorespiratory fitness score of $latestVo2 mL/kg/min indicates $tier aerobic capacity relative to age and sex baseline."
        } else {
            "${historyItems.size} exercise sessions recorded in this time range."
        }

        return VitalDetailUiState(
            metricType = VitalMetricType.VO2_MAX_WORKOUTS,
            selectedRange = range,
            headlineValue = latestVo2?.toString() ?: "--",
            headlineUnit = "mL/kg",
            headlineDateLabel = "Cardio Fitness Score",
            subtitle = "Estimated VO2 Max and logged exercise sessions",
            calloutInsight = insight,
            summaryStats = stats,
            points = points,
            historyRecords = historyItems,
            referenceLineValue = 40f,
            dataSourceLabel = "Health Connect"
        )
    }

    private suspend fun getBodyMetricsDetailData(range: MetricTimeRange): VitalDetailUiState {
        val zoneId = ZoneId.systemDefault()
        val now = Instant.now()
        val days = range.days.toLong()
        val startTime = now.minus(Duration.ofDays(days))

        val points = mutableListOf<ChartPoint>()
        val historyItems = mutableListOf<VitalHistoryItem>()
        var latestHeightMeters: Double? = null

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
                    latestHeightMeters = heightRecords.first().height.inMeters
                }
            }
        } catch (_: Exception) {}

        try {
            val c = client
            if (c != null) {
                val weightRecords = c.readRecords(
                    ReadRecordsRequest(
                        recordType = WeightRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startTime, now),
                        ascendingOrder = true
                    )
                ).records

                weightRecords.forEach { w ->
                    val weightKg = w.weight.inKilograms
                    val bmi = latestHeightMeters?.let { VitalsCalculations.calculateBmi(weightKg, it) }
                    points.add(
                        ChartPoint(
                            timestamp = w.time,
                            xLabel = dateFormatter.format(w.time.atZone(zoneId)),
                            value = weightKg.toFloat(),
                            secondaryValue = bmi?.toFloat()
                        )
                    )
                    val bmiDetail = if (bmi != null) "BMI $bmi (${VitalsCalculations.classifyBmi(bmi)})" else "Weight logged"
                    historyItems.add(
                        VitalHistoryItem(
                            id = "weight_${w.time.toEpochMilli()}",
                            timestamp = w.time,
                            formattedTime = fullDateTimeFormatter.format(w.time.atZone(zoneId)),
                            primaryValue = String.format(Locale.US, "%.1f", weightKg),
                            primaryUnit = "kg",
                            secondaryDetail = bmiDetail,
                            deviceSource = "Health Connect Body Metrics"
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        val latestWeight = points.lastOrNull()?.value

        if (latestWeight == null) {
            return VitalDetailUiState(
                metricType = VitalMetricType.BODY_METRICS,
                selectedRange = range,
                headlineValue = "--",
                headlineUnit = "kg",
                headlineDateLabel = "Latest Measurement",
                subtitle = "Weight & Body Mass Index trend",
                calloutInsight = "No weight or body metrics recorded. Record body metrics in Health Connect.",
                summaryStats = emptyList(),
                points = emptyList(),
                historyRecords = emptyList(),
                dataSourceLabel = "Health Connect"
            )
        }

        val latestBmi = latestHeightMeters?.let { VitalsCalculations.calculateBmi(latestWeight.toDouble(), it) }
        val category = latestBmi?.let { VitalsCalculations.classifyBmi(it) } ?: "Unrated"

        val stats = listOfNotNull(
            StatItem("Weight", String.format(Locale.US, "%.1f", latestWeight), "kg"),
            latestBmi?.let { StatItem("BMI", "$it", "kg/m²") },
            latestHeightMeters?.let { StatItem("Height", "${(it * 100).roundToInt()}", "cm") }
        )

        val insight = if (latestBmi != null) {
            "Current BMI of $latestBmi kg/m² falls in the $category category for your profile height of ${((latestHeightMeters ?: 1.70) * 100).roundToInt()} cm."
        } else {
            "Weight recorded at ${String.format(Locale.US, "%.1f", latestWeight)} kg. Add height in profile to calculate Body Mass Index."
        }

        return VitalDetailUiState(
            metricType = VitalMetricType.BODY_METRICS,
            selectedRange = range,
            headlineValue = String.format(Locale.US, "%.1f", latestWeight),
            headlineUnit = "kg",
            headlineDateLabel = "Latest Measurement",
            subtitle = "Weight & Body Mass Index trend",
            calloutInsight = insight,
            summaryStats = stats,
            points = points,
            historyRecords = historyItems.reversed(),
            dataSourceLabel = "Health Connect"
        )
    }
}
