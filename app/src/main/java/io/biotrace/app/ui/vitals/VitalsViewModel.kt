package io.biotrace.app.ui.vitals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.biotrace.app.data.healthconnect.HealthConnectAvailability
import io.biotrace.app.data.healthconnect.HealthConnectManager
import io.biotrace.app.data.local.dao.LabBiomarkerDao
import io.biotrace.app.data.local.dao.MealDao
import io.biotrace.app.data.local.entity.LabBiomarkerEntity
import io.biotrace.app.data.local.entity.MealEntity
import io.biotrace.app.domain.anomaly.AnomalyDetectionEngine
import io.biotrace.app.domain.model.ContinuousHeartRateHeroData
import io.biotrace.app.domain.model.MetricTimeRange
import io.biotrace.app.domain.model.ProfileTriptychSummary
import io.biotrace.app.domain.model.VitalDetailUiState
import io.biotrace.app.domain.model.VitalMetricType
import io.biotrace.app.domain.model.VitalPoint
import io.biotrace.app.domain.model.VitalSummaryCardModel
import io.biotrace.app.domain.repository.VitalsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class DashboardCopilotSummary(
    val summaryText: String = "",
    val hrvDeltaText: String? = null,
    val rhrDeltaText: String? = null,
    val sleepTargetText: String? = null,
    val labBiomarkerStatus: String? = null,
    val actionSuggestion: String? = null
)

data class DashboardAutonomicStrain(
    val isStrainDetected: Boolean = false,
    val severity: String = "NORMAL",
    val headline: String = "",
    val narrative: String = "",
    val rhrDeviation: Double = 0.0,
    val hrvDeviation: Double = 0.0,
    val sleepHours: Double = 0.0
)

data class DashboardHrvSparkline(
    val points: List<Float> = emptyList(),
    val latestHrvMs: Double? = null,
    val deltaPercent: Double? = null,
    val isUpTrend: Boolean = false,
    val isDownTrend: Boolean = false,
    val label: String = "HRV (RMSSD)"
)

data class DashboardRhrSplit(
    val currentBpm: Int? = null,
    val baselineMeanBpm: Double? = null,
    val zScore: Double? = null,
    val zScoreFormatted: String? = null,
    val status: String = "Normal"
)

data class DashboardSleepSplit(
    val durationFormatted: String = "--",
    val targetLabel: String = "8h Target",
    val deepPercentage: Int = 0,
    val remPercentage: Int = 0,
    val lightPercentage: Int = 0,
    val totalMinutes: Int = 0
)

data class DashboardLabItem(
    val name: String,
    val value: Double,
    val unit: String,
    val referenceMin: Double?,
    val referenceMax: Double?,
    val flag: String?,
    val rangePosition: Float
)

data class DashboardMealNutrition(
    val mealName: String = "",
    val calories: Int = 0,
    val proteinGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
    val fatsGrams: Double = 0.0,
    val fiberGrams: Double? = null,
    val consumedAt: Instant? = null,
    val consumedAtFormatted: String = "",
    val isLateMeal: Boolean = false,
    val postprandialHrImpactBpm: Double? = null,
    val narrative: String = ""
)

data class VitalsUiState(
    val cards: List<VitalSummaryCardModel> = emptyList(),
    val heroHeartRate: ContinuousHeartRateHeroData = ContinuousHeartRateHeroData(),
    val profileTriptych: ProfileTriptychSummary = ProfileTriptychSummary(),
    val isRefreshing: Boolean = false,
    val hasHealthConnectPermissions: Boolean = true,
    val availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
    val lastSyncTime: Instant? = null,
    val copilotSummary: DashboardCopilotSummary = DashboardCopilotSummary(),
    val autonomicStrain: DashboardAutonomicStrain = DashboardAutonomicStrain(),
    val hrvSparkline: DashboardHrvSparkline = DashboardHrvSparkline(),
    val rhrSplit: DashboardRhrSplit = DashboardRhrSplit(),
    val sleepSplit: DashboardSleepSplit = DashboardSleepSplit(),
    val labBiomarkers: List<DashboardLabItem> = emptyList(),
    val latestNutrition: DashboardMealNutrition? = null
)

@HiltViewModel
class VitalsViewModel @Inject constructor(
    private val vitalsRepository: VitalsRepository,
    private val healthConnectManager: HealthConnectManager,
    private val labBiomarkerDao: LabBiomarkerDao,
    private val mealDao: MealDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(VitalsUiState())
    val uiState: StateFlow<VitalsUiState> = _uiState.asStateFlow()

    private val anomalyDetectionEngine = AnomalyDetectionEngine()

    init {
        checkStatusAndRefresh()
    }

    fun checkStatusAndRefresh() {
        viewModelScope.launch {
            val availability = healthConnectManager.checkAvailability()
            val hasPerms = healthConnectManager.hasAllPermissions()

            _uiState.update {
                it.copy(
                    availability = availability,
                    hasHealthConnectPermissions = hasPerms
                )
            }

            refreshDashboard()
        }
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                // Sync latest from Health Connect
                runCatching { vitalsRepository.syncFromHealthConnect(days = 7) }

                val cards = vitalsRepository.getVitalsDashboardSnapshot()
                val heroHr = vitalsRepository.getContinuousHeartRateHeroData()
                val profile = vitalsRepository.getProfileTriptychSummary()

                val now = Instant.now()
                val sevenDaysAgo = now.minus(7, ChronoUnit.DAYS)
                val thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS)

                // 1. HRV RMSSD sparkline & delta %
                val hrvPoints = vitalsRepository.getVitalsBetween("HRV_RMSSD", sevenDaysAgo, now).ifEmpty {
                    vitalsRepository.getVitalsBetween("HRV", sevenDaysAgo, now)
                }.sortedBy { it.timestamp }

                val hrvSparkline = computeHrvSparkline(hrvPoints)

                // 2. RHR Split (bpm, Z-score deviation guarded against sigma < 0.1 or < 2 history samples)
                val rhrHistory = vitalsRepository.getVitalsBetween("RESTING_HEART_RATE", thirtyDaysAgo, now)
                    .sortedBy { it.timestamp }
                val currentRhrBpm = heroHr.restingHrBpm ?: rhrHistory.lastOrNull()?.value?.roundToInt()
                val rhrSplit = computeRhrSplit(currentRhrBpm, rhrHistory)

                // 3. Sleep Split (duration string, target label, deep%, rem%, light% percentages, guarded against totalMinutes == 0)
                val sleepDetail = runCatching {
                    vitalsRepository.getMetricDetailData(VitalMetricType.SLEEP, MetricTimeRange.DAY)
                }.getOrNull()
                val sleepSplit = computeSleepSplit(sleepDetail)

                // 4. Autonomic Strain alert (evaluated from resting HR, HRV, and sleep anomalies)
                val autonomicStrain = computeAutonomicStrain(
                    currentRhrBpm = currentRhrBpm,
                    rhrHistory = rhrHistory,
                    hrvPoints = hrvPoints,
                    sleepMinutes = sleepSplit.totalMinutes
                )

                // 5. Labs summary (HbA1c, Ferritin, hs-CRP from LabBiomarkerDao with safe calculateRangePosition)
                val allBiomarkers = labBiomarkerDao.getAll()
                val labItems = computeLabSummary(allBiomarkers)

                // 6. Latest Nutrition (latest MealEntity from MealDao with postprandial HR impact calculation vs resting HR baseline)
                val latestMeal = mealDao.getLatestMeal()
                val latestNutrition = computeMealNutrition(latestMeal, currentRhrBpm)

                // 7. Copilot Summary (dynamic summary text, HRV & RHR delta, sleep stage target, lab biomarker status, action suggestion)
                val copilotSummary = computeCopilotSummary(
                    autonomicStrain = autonomicStrain,
                    hrvSparkline = hrvSparkline,
                    rhrSplit = rhrSplit,
                    sleepSplit = sleepSplit,
                    labItems = labItems,
                    latestNutrition = latestNutrition
                )

                _uiState.update {
                    it.copy(
                        cards = cards,
                        heroHeartRate = heroHr,
                        profileTriptych = profile,
                        copilotSummary = copilotSummary,
                        autonomicStrain = autonomicStrain,
                        hrvSparkline = hrvSparkline,
                        rhrSplit = rhrSplit,
                        sleepSplit = sleepSplit,
                        labBiomarkers = labItems,
                        latestNutrition = latestNutrition,
                        isRefreshing = false,
                        lastSyncTime = Instant.now()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private fun computeHrvSparkline(hrvPoints: List<VitalPoint>): DashboardHrvSparkline {
        if (hrvPoints.isEmpty()) {
            return DashboardHrvSparkline()
        }
        val points = hrvPoints.map { it.value.toFloat() }
        val latest = hrvPoints.last().value
        val latestHrvMs = (latest * 10.0).roundToInt() / 10.0

        // Guarded against < 2 points
        if (hrvPoints.size < 2) {
            return DashboardHrvSparkline(
                points = points,
                latestHrvMs = latestHrvMs,
                deltaPercent = null,
                isUpTrend = false,
                isDownTrend = false
            )
        }

        val prev = hrvPoints[hrvPoints.size - 2].value
        if (prev <= 0.0) {
            return DashboardHrvSparkline(
                points = points,
                latestHrvMs = latestHrvMs,
                deltaPercent = null,
                isUpTrend = false,
                isDownTrend = false
            )
        }

        val delta = ((latest - prev) / prev) * 100.0
        val deltaPercent = (delta * 10.0).roundToInt() / 10.0
        return DashboardHrvSparkline(
            points = points,
            latestHrvMs = latestHrvMs,
            deltaPercent = deltaPercent,
            isUpTrend = deltaPercent > 0.0,
            isDownTrend = deltaPercent < 0.0
        )
    }

    private fun computeRhrSplit(currentBpm: Int?, rhrHistory: List<VitalPoint>): DashboardRhrSplit {
        if (currentBpm == null && rhrHistory.isEmpty()) {
            return DashboardRhrSplit(status = "No Data")
        }

        val effectiveBpm = currentBpm ?: rhrHistory.lastOrNull()?.value?.roundToInt()

        // Guarded against < 2 history samples
        if (rhrHistory.size < 2) {
            return DashboardRhrSplit(
                currentBpm = effectiveBpm,
                baselineMeanBpm = rhrHistory.firstOrNull()?.value ?: effectiveBpm?.toDouble(),
                zScore = null,
                zScoreFormatted = null,
                status = "Baseline"
            )
        }

        val values = rhrHistory.map { it.value }
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        val sigma = sqrt(variance)
        val roundedMean = (mean * 10.0).roundToInt() / 10.0

        // Guarded against sigma < 0.1
        if (sigma < 0.1) {
            return DashboardRhrSplit(
                currentBpm = effectiveBpm,
                baselineMeanBpm = roundedMean,
                zScore = null,
                zScoreFormatted = null,
                status = "Stable"
            )
        }

        val currentVal = (effectiveBpm?.toDouble()) ?: values.last()
        val rawZ = (currentVal - mean) / sigma
        val zScore = (rawZ * 10.0).roundToInt() / 10.0
        val zFormatted = if (zScore >= 0.0) "+${zScore}σ" else "${zScore}σ"

        val status = when {
            zScore >= 1.5 -> "Elevated"
            zScore <= -1.5 -> "Optimal"
            else -> "Normal"
        }

        return DashboardRhrSplit(
            currentBpm = effectiveBpm,
            baselineMeanBpm = roundedMean,
            zScore = zScore,
            zScoreFormatted = zFormatted,
            status = status
        )
    }

    private fun computeSleepSplit(sleepDetail: VitalDetailUiState?): DashboardSleepSplit {
        val breakdown = sleepDetail?.sleepBreakdown
        if (breakdown == null || breakdown.totalMinutes == 0) {
            return DashboardSleepSplit(
                durationFormatted = "--",
                targetLabel = "8h Target",
                deepPercentage = 0,
                remPercentage = 0,
                lightPercentage = 0,
                totalMinutes = 0
            )
        }

        val totalMinutes = breakdown.totalMinutes
        val hrs = totalMinutes / 60
        val mins = totalMinutes % 60
        val durationStr = "${hrs}h ${mins}m"

        val deepPct = (breakdown.deepMinutes * 100) / totalMinutes
        val remPct = (breakdown.remMinutes * 100) / totalMinutes
        val lightPct = (breakdown.lightMinutes * 100) / totalMinutes

        return DashboardSleepSplit(
            durationFormatted = durationStr,
            targetLabel = "8h Target",
            deepPercentage = deepPct,
            remPercentage = remPct,
            lightPercentage = lightPct,
            totalMinutes = totalMinutes
        )
    }

    private fun computeAutonomicStrain(
        currentRhrBpm: Int?,
        rhrHistory: List<VitalPoint>,
        hrvPoints: List<VitalPoint>,
        sleepMinutes: Int
    ): DashboardAutonomicStrain {
        if (currentRhrBpm == null || rhrHistory.size < 2 || hrvPoints.size < 2) {
            return DashboardAutonomicStrain(
                isStrainDetected = false,
                severity = "NORMAL",
                headline = "Autonomic Tone Stable",
                narrative = "Biometric baselines are stable or awaiting additional sync data.",
                rhrDeviation = 0.0,
                hrvDeviation = 0.0,
                sleepHours = ((sleepMinutes.toDouble() / 60.0) * 10.0).roundToInt() / 10.0
            )
        }

        val rhrValues = rhrHistory.map { it.value }
        val rhrMean = rhrValues.average()
        val rhrVariance = rhrValues.map { (it - rhrMean) * (it - rhrMean) }.average()
        val rhrStdDev = sqrt(rhrVariance)

        val hrvValues = hrvPoints.map { it.value }
        val hrvMean = hrvValues.average()
        val hrvVariance = hrvValues.map { (it - hrvMean) * (it - hrvMean) }.average()
        val hrvStdDev = sqrt(hrvVariance)

        val currentHrv = hrvPoints.last().value
        val sleepHours = ((sleepMinutes.toDouble() / 60.0) * 10.0).roundToInt() / 10.0

        val alert = anomalyDetectionEngine.evaluateCompoundStrain(
            currentRhr = currentRhrBpm.toDouble(),
            baselineRhrMean = rhrMean,
            baselineRhrStdDev = rhrStdDev,
            currentHrv = currentHrv,
            baselineHrvMean = hrvMean,
            baselineHrvStdDev = hrvStdDev,
            recentSleepHours = sleepHours
        )

        if (alert != null) {
            val rhrZ = if (rhrStdDev > 0) (currentRhrBpm - rhrMean) / rhrStdDev else 0.0
            val hrvZ = if (hrvStdDev > 0) (hrvMean - currentHrv) / hrvStdDev else 0.0
            return DashboardAutonomicStrain(
                isStrainDetected = true,
                severity = alert.severity,
                headline = "Compound Autonomic Strain",
                narrative = alert.narrative,
                rhrDeviation = (rhrZ * 10.0).roundToInt() / 10.0,
                hrvDeviation = (hrvZ * 10.0).roundToInt() / 10.0,
                sleepHours = sleepHours
            )
        }

        // Check partial deviation
        val isRhrHigh = rhrStdDev >= 0.1 && (currentRhrBpm - rhrMean) > 1.5 * rhrStdDev
        val isHrvLow = hrvStdDev >= 0.1 && (hrvMean - currentHrv) > 1.8 * hrvStdDev
        if (isRhrHigh || isHrvLow) {
            val rhrZ = if (rhrStdDev > 0) (currentRhrBpm - rhrMean) / rhrStdDev else 0.0
            val hrvZ = if (hrvStdDev > 0) (hrvMean - currentHrv) / hrvStdDev else 0.0
            return DashboardAutonomicStrain(
                isStrainDetected = true,
                severity = "WARNING",
                headline = "Elevated Autonomic Load",
                narrative = if (isRhrHigh && isHrvLow) {
                    "Resting HR is elevated and HRV is suppressed relative to personal baseline."
                } else if (isRhrHigh) {
                    "Resting HR is elevated above normal baseline (+${((rhrZ * 10.0).roundToInt() / 10.0)}σ)."
                } else {
                    "HRV is suppressed relative to rolling 7-day baseline."
                },
                rhrDeviation = (rhrZ * 10.0).roundToInt() / 10.0,
                hrvDeviation = (hrvZ * 10.0).roundToInt() / 10.0,
                sleepHours = sleepHours
            )
        }

        return DashboardAutonomicStrain(
            isStrainDetected = false,
            severity = "NORMAL",
            headline = "Autonomic Tone Balanced",
            narrative = "Resting HR, HRV, and sleep markers within normal physiological limits.",
            rhrDeviation = 0.0,
            hrvDeviation = 0.0,
            sleepHours = sleepHours
        )
    }

    fun calculateRangePosition(value: Double, refMin: Double?, refMax: Double?): Float {
        if (refMin != null && refMax != null) {
            val span = refMax - refMin
            if (span <= 0.0) return 0.5f
            return ((value - refMin) / span).toFloat().coerceIn(0f, 1f)
        } else if (refMax != null && refMax > 0.0) {
            return (value / refMax).toFloat().coerceIn(0f, 1f)
        } else if (refMin != null && refMin > 0.0) {
            return (value / (refMin * 2.0)).toFloat().coerceIn(0f, 1f)
        }
        return 0.5f
    }

    private fun computeLabSummary(allBiomarkers: List<LabBiomarkerEntity>): List<DashboardLabItem> {
        if (allBiomarkers.isEmpty()) return emptyList()

        val hba1cEntity = allBiomarkers.firstOrNull { bm ->
            val name = bm.biomarkerName.uppercase(Locale.ROOT)
            name in setOf("HBA1C", "HEMOGLOBIN_A1C", "A1C") || bm.rawName.contains("a1c", ignoreCase = true)
        }
        val ferritinEntity = allBiomarkers.firstOrNull { bm ->
            val name = bm.biomarkerName.uppercase(Locale.ROOT)
            name == "FERRITIN" || bm.rawName.contains("ferritin", ignoreCase = true)
        }
        val crpEntity = allBiomarkers.firstOrNull { bm ->
            val name = bm.biomarkerName.uppercase(Locale.ROOT)
            name in setOf("HS_CRP", "HS-CRP", "CRP") || bm.rawName.contains("crp", ignoreCase = true)
        }

        val mappedTargetItems = listOfNotNull(
            hba1cEntity?.let { toDashboardLabItem(it, "HbA1c") },
            ferritinEntity?.let { toDashboardLabItem(it, "Ferritin") },
            crpEntity?.let { toDashboardLabItem(it, "hs-CRP") }
        )

        if (mappedTargetItems.isNotEmpty()) {
            return mappedTargetItems
        }

        // Fallback: take up to 3 distinct biomarkers from the store
        return allBiomarkers
            .distinctBy { it.biomarkerName }
            .take(3)
            .map { toDashboardLabItem(it, it.rawName.ifBlank { it.biomarkerName }) }
    }

    private fun toDashboardLabItem(entity: LabBiomarkerEntity, displayName: String): DashboardLabItem {
        val pos = calculateRangePosition(entity.value, entity.referenceMin, entity.referenceMax)
        val flag = entity.flag ?: when {
            entity.referenceMax != null && entity.value > entity.referenceMax -> "HIGH"
            entity.referenceMin != null && entity.value < entity.referenceMin -> "LOW"
            entity.referenceMin != null || entity.referenceMax != null -> "NORMAL"
            else -> null
        }
        return DashboardLabItem(
            name = displayName,
            value = entity.value,
            unit = entity.unit,
            referenceMin = entity.referenceMin,
            referenceMax = entity.referenceMax,
            flag = flag,
            rangePosition = pos
        )
    }

    private suspend fun computeMealNutrition(
        latestMeal: MealEntity?,
        restingHrBpm: Int?
    ): DashboardMealNutrition? {
        if (latestMeal == null) return null

        val zone = ZoneId.systemDefault()
        val localTime = latestMeal.consumedAt.atZone(zone).toLocalTime()
        val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.getDefault())
        val formattedTime = dateTimeFormatter.format(latestMeal.consumedAt.atZone(zone))
        val timeOnlyFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
        val formattedTimeOnly = timeOnlyFormatter.format(localTime)

        val isLateMeal = localTime.hour >= 20

        var postprandialImpact: Double? = null
        val narrative: String

        if (isLateMeal && restingHrBpm != null && restingHrBpm > 0) {
            val windowEnd = latestMeal.consumedAt.plus(Duration.ofHours(3))
            val postprandialHrRecords = vitalsRepository.getVitalsBetween(
                "HEART_RATE",
                latestMeal.consumedAt,
                windowEnd
            )

            if (postprandialHrRecords.isNotEmpty()) {
                val avgHr = postprandialHrRecords.map { it.value }.average()
                val rawImpact = avgHr - restingHrBpm
                val roundedImpact = (rawImpact * 10.0).roundToInt() / 10.0
                postprandialImpact = roundedImpact

                val impactStr = if (roundedImpact >= 0) "+$roundedImpact" else "$roundedImpact"
                narrative = "Late meal (${latestMeal.calories} kcal) at $formattedTimeOnly elevated postprandial HR by $impactStr bpm vs resting baseline."
            } else {
                narrative = "Late meal (${latestMeal.calories} kcal) logged at $formattedTimeOnly. Postprandial cardiac monitoring active."
            }
        } else if (isLateMeal) {
            narrative = "Late meal (${latestMeal.calories} kcal) logged at $formattedTimeOnly. Monitoring nocturnal recovery."
        } else {
            narrative = "${latestMeal.name} (${latestMeal.calories} kcal) logged at $formattedTimeOnly."
        }

        return DashboardMealNutrition(
            mealName = latestMeal.name,
            calories = latestMeal.calories,
            proteinGrams = latestMeal.proteinGrams,
            carbsGrams = latestMeal.carbsGrams,
            fatsGrams = latestMeal.fatsGrams,
            fiberGrams = latestMeal.fiberGrams,
            consumedAt = latestMeal.consumedAt,
            consumedAtFormatted = formattedTime,
            isLateMeal = isLateMeal,
            postprandialHrImpactBpm = postprandialImpact,
            narrative = narrative
        )
    }

    private fun computeCopilotSummary(
        autonomicStrain: DashboardAutonomicStrain,
        hrvSparkline: DashboardHrvSparkline,
        rhrSplit: DashboardRhrSplit,
        sleepSplit: DashboardSleepSplit,
        labItems: List<DashboardLabItem>,
        latestNutrition: DashboardMealNutrition?
    ): DashboardCopilotSummary {
        val hasData = hrvSparkline.latestHrvMs != null ||
                rhrSplit.currentBpm != null ||
                sleepSplit.totalMinutes > 0 ||
                labItems.isNotEmpty() ||
                latestNutrition != null

        if (!hasData) {
            return DashboardCopilotSummary(
                summaryText = "Awaiting health vitals sync from Health Connect. Sync records to see personalized biometric insights."
            )
        }

        val hrvDeltaText = hrvSparkline.deltaPercent?.let {
            if (it >= 0.0) "+$it%" else "$it%"
        }

        val rhrDeltaText = when {
            rhrSplit.zScoreFormatted != null -> "${rhrSplit.zScoreFormatted} (${rhrSplit.currentBpm} bpm)"
            rhrSplit.currentBpm != null -> "${rhrSplit.currentBpm} bpm"
            else -> null
        }

        val sleepTargetText = if (sleepSplit.totalMinutes > 0) {
            "${sleepSplit.durationFormatted} (${sleepSplit.targetLabel})"
        } else null

        val labBiomarkerStatus = if (labItems.isNotEmpty()) {
            val anomalies = labItems.filter { it.flag == "HIGH" || it.flag == "LOW" }
            if (anomalies.isNotEmpty()) {
                "${anomalies.size} marker${if (anomalies.size > 1) "s" else ""} out of range (${anomalies.joinToString { it.name }})"
            } else {
                "All ${labItems.size} lab markers optimal"
            }
        } else null

        val summaryText = when {
            autonomicStrain.isStrainDetected && autonomicStrain.severity == "ALERT" ->
                "Elevated compound autonomic strain detected across your cardiovascular and sleep streams. Prioritize active recovery and down-regulation today."
            autonomicStrain.isStrainDetected && autonomicStrain.severity == "WARNING" ->
                "Mild autonomic load observed with biometric shifts from baseline. Consider lighter physical exertion today."
            hrvSparkline.isUpTrend && rhrSplit.status == "Optimal" ->
                "Optimal autonomic resilience with rising HRV trend and lowered resting heart rate. Recovery capacity is strong."
            else ->
                "Your vital markers reflect stable cardiovascular and autonomic function."
        }

        val actionSuggestion = when {
            autonomicStrain.isStrainDetected ->
                "Schedule an early bedtime before 22:30 and engage in 10 minutes of restorative parasympathetic breathing."
            latestNutrition?.postprandialHrImpactBpm != null && latestNutrition.postprandialHrImpactBpm > 3.0 ->
                "Consider concluding your evening meal earlier to prevent elevated nocturnal heart rate."
            sleepSplit.totalMinutes in 1..389 ->
                "Target an extra 30–45 minutes of sleep tonight to offset accumulated sleep deficit."
            labItems.any { it.flag == "HIGH" || it.flag == "LOW" } ->
                "Review flagged laboratory biomarkers with your healthcare provider."
            else ->
                "Maintain your current balanced activity and sleep cadence."
        }

        return DashboardCopilotSummary(
            summaryText = summaryText,
            hrvDeltaText = hrvDeltaText,
            rhrDeltaText = rhrDeltaText,
            sleepTargetText = sleepTargetText,
            labBiomarkerStatus = labBiomarkerStatus,
            actionSuggestion = actionSuggestion
        )
    }
}
