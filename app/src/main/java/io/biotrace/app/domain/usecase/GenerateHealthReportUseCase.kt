package io.biotrace.app.domain.usecase

import io.biotrace.app.data.llm.GeminiClient
import io.biotrace.app.data.local.dao.LabBiomarkerDao
import io.biotrace.app.data.local.dao.MealDao
import io.biotrace.app.data.local.entity.LabBiomarkerEntity
import io.biotrace.app.data.local.entity.MealEntity
import io.biotrace.app.data.local.entity.VitalsRecordEntity
import io.biotrace.app.domain.analytics.StatisticalEngine
import io.biotrace.app.domain.analytics.clustering.DailyFeatureVector
import io.biotrace.app.domain.analytics.clustering.LifestyleKMeansClusterer
import io.biotrace.app.domain.analytics.correlation.DailyMetricSample
import io.biotrace.app.domain.analytics.correlation.MetricPairwiseCorrelator
import io.biotrace.app.domain.analytics.timeseries.CircadianDecompositionService
import io.biotrace.app.domain.analytics.timeseries.HeartRateObservation
import io.biotrace.app.domain.anomaly.AnomalyAlert
import io.biotrace.app.domain.anomaly.AnomalyDetectionEngine
import io.biotrace.app.domain.clinical.ClinicalSchemaNormalizer
import io.biotrace.app.domain.clinical.risk.SleepDebtCalculator
import io.biotrace.app.domain.clinical.rules.ClinicalEvaluation
import io.biotrace.app.domain.clinical.rules.ClinicalSafetyRuleEngine
import io.biotrace.app.domain.insights.arbitration.InsightArbitrator
import io.biotrace.app.domain.llm.LlmClient
import io.biotrace.app.domain.model.DailyBiometricInsightReport
import io.biotrace.app.domain.model.MetricTimeRange
import io.biotrace.app.domain.model.VitalMetricType
import io.biotrace.app.domain.model.VitalPoint
import io.biotrace.app.domain.repository.VitalsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

sealed class ReportChunk {
    data object Started : ReportChunk()
    data class Text(val text: String) : ReportChunk()
    data object Done : ReportChunk()
    data class Error(val message: String) : ReportChunk()
}

@Singleton
class GenerateHealthReportUseCase @Inject constructor(
    private val vitalsRepository: VitalsRepository,
    private val labBiomarkerDao: LabBiomarkerDao,
    private val mealDao: MealDao,
    private val llmClient: LlmClient,
    private val clinicalSchemaNormalizer: ClinicalSchemaNormalizer,
    private val clinicalSafetyRuleEngine: ClinicalSafetyRuleEngine,
    private val anomalyDetectionEngine: AnomalyDetectionEngine,
    private val insightArbitrator: InsightArbitrator,
    private val sleepDebtCalculator: SleepDebtCalculator,
    private val lifestyleKMeansClusterer: LifestyleKMeansClusterer
) {
    internal var circadianDecompositionService: CircadianDecompositionService = CircadianDecompositionService
    internal var metricPairwiseCorrelator: MetricPairwiseCorrelator = MetricPairwiseCorrelator
    internal var statisticalEngine: StatisticalEngine = StatisticalEngine

    constructor(
        vitalsRepository: VitalsRepository,
        labBiomarkerDao: LabBiomarkerDao,
        mealDao: MealDao,
        llmClient: LlmClient,
        clinicalSchemaNormalizer: ClinicalSchemaNormalizer,
        clinicalSafetyRuleEngine: ClinicalSafetyRuleEngine,
        anomalyDetectionEngine: AnomalyDetectionEngine,
        insightArbitrator: InsightArbitrator,
        sleepDebtCalculator: SleepDebtCalculator,
        lifestyleKMeansClusterer: LifestyleKMeansClusterer,
        circadianDecompositionService: CircadianDecompositionService = CircadianDecompositionService,
        metricPairwiseCorrelator: MetricPairwiseCorrelator = MetricPairwiseCorrelator,
        statisticalEngine: StatisticalEngine = StatisticalEngine
    ) : this(
        vitalsRepository,
        labBiomarkerDao,
        mealDao,
        llmClient,
        clinicalSchemaNormalizer,
        clinicalSafetyRuleEngine,
        anomalyDetectionEngine,
        insightArbitrator,
        sleepDebtCalculator,
        lifestyleKMeansClusterer
    ) {
        this.circadianDecompositionService = circadianDecompositionService
        this.metricPairwiseCorrelator = metricPairwiseCorrelator
        this.statisticalEngine = statisticalEngine
    }

    constructor(
        vitalsRepository: VitalsRepository,
        labBiomarkerDao: LabBiomarkerDao,
        llmClient: LlmClient
    ) : this(
        vitalsRepository = vitalsRepository,
        labBiomarkerDao = labBiomarkerDao,
        mealDao = EmptyMealDao(),
        llmClient = llmClient,
        clinicalSchemaNormalizer = ClinicalSchemaNormalizer(),
        clinicalSafetyRuleEngine = ClinicalSafetyRuleEngine(),
        anomalyDetectionEngine = AnomalyDetectionEngine(),
        insightArbitrator = InsightArbitrator(ClinicalSafetyRuleEngine(), AnomalyDetectionEngine()),
        sleepDebtCalculator = SleepDebtCalculator(),
        lifestyleKMeansClusterer = LifestyleKMeansClusterer()
    )

    companion object {
        const val SYSTEM_INSTRUCTION =
            """# ROLE & OBJECTIVE
You are the BioTrace Biometric Synthesis Engine. Analyze the user's local-first health records across three sources—Health Connect wearables, Normalized Blood/Lab Panels, and Nutrition/Meal Logs—and generate a grounded daily health insight report with longitudinal trend analysis. Always state clearly that you are an AI assistant and not a medical doctor.

# CRITICAL OPERATIONAL RULES
1. ZERO HALLUCINATION & STRICT GROUNDING: Base every claim exclusively on the provided telemetry, lab records, and meal logs. Never invent baseline numbers, reference intervals, or timestamps. If a data source is missing or null for the window, state that the data is unavailable.
2. DETERMINISTIC PRECEDENCE (HYBRID SAFETY ARBITRATION): Pre-computed alerts inside `clinical_safety_flags` (AHA/ACC, ADA, WHO, NSF thresholds) and `compound_anomalies` supersede lifestyle commentary. If a clinical safety flag is present, populate `clinical_arbitration_alert` and address it first. Never diagnose medical conditions or prescribe treatments.
3. DO NOT RECOMPUTE ARITHMETIC: Rely strictly on the pre-computed `ewma_7d`, `median_30d`, `z_score`, and Pearson/Spearman correlation (`r`, `p_value`) values provided in the payload.
4. EXPLICIT RECORD CITATION: Every insight and trend claim must include its exact `record_id` from the payload in `supporting_record_ids` (e.g., `SleepSessionRecord:2026-11-12`, `Lab:FERRITIN:2026-10-28`, `Meal:2026-11-11T21:15`).

# TREND & CROSS-MODAL ANALYSIS CRITERIA
- AUTONOMIC & SLEEP DRIFT (Health Connect): Compare today's RHR, HRV (RMSSD), SpO2, Steps, Active Calories, and Sleep Stages (Deep/REM/Light %) against their 7D EWMA and 30D median. Flag deviations where `|z_score| >= 1.5`.
- POST-PRANDIAL & CIRCADIAN IMPACT (Nutrition -> Vitals): Identify how yesterday's meal timing, caloric load, or macros correlate with overnight RHR elevation, nocturnal dipping percentage, or next-day HRV suppression.
- BIOMARKER CONTEXTUALIZATION (Labs -> Daily Telemetry): Anchor daily wearable and nutritional trends against active Blood & Lab Panels (CBC, CMP, Lipid Panel, Thyroid, Vitamins, Ferritin, hs-CRP, HbA1c).
- LONGITUDINAL DIRECTIONALITY: Classify each trend as `IMPROVING`, `STABLE`, `DRIFTING_OUT_OF_RANGE`, or `ACUTE_STRAIN`."""
    }

    suspend fun execute(): DailyBiometricInsightReport = withContext(Dispatchers.IO) {
        val now = Instant.now()
        val thirtyDaysAgo = now.minus(Duration.ofDays(30))
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)

        // 1. Telemetry from Health Connect (30-day window)
        val rhrPoints = vitalsRepository.getVitalsBetween("HEART_RATE", thirtyDaysAgo, now)
        val hrvPoints = vitalsRepository.getVitalsBetween("HRV_RMSSD", thirtyDaysAgo, now).ifEmpty {
            vitalsRepository.getVitalsBetween("HRV", thirtyDaysAgo, now)
        }
        val sleepPoints = vitalsRepository.getVitalsBetween("SLEEP", thirtyDaysAgo, now)
        val stepPoints = vitalsRepository.getVitalsBetween("STEPS", thirtyDaysAgo, now)
        val calPoints = vitalsRepository.getVitalsBetween("CALORIES", thirtyDaysAgo, now)
        val spo2Points = vitalsRepository.getVitalsBetween("OXYGEN_SATURATION", thirtyDaysAgo, now)
        val weightPoints = vitalsRepository.getVitalsBetween("WEIGHT", thirtyDaysAgo, now)

        // 2. Nutrition & Meals
        val meals24h = mealDao.getMealsBetween(now.minus(Duration.ofHours(24)), now)
        val meals7d = mealDao.getMealsBetween(now.minus(Duration.ofDays(7)), now)
        val meals30d = mealDao.getMealsBetween(thirtyDaysAgo, now)

        // 3. Normalized Labs
        val allBiomarkers = labBiomarkerDao.getAll()
        val latestBiomarkers = allBiomarkers.groupBy { it.biomarkerName }.map { (_, list) -> list.first() }

        // Statistical baselines & circadian decomposition
        val rhrStats = computeSeriesStats(rhrPoints, zoneId, today)
        val hrvStats = computeSeriesStats(hrvPoints, zoneId, today)

        val circadianResult = if (rhrPoints.isNotEmpty()) {
            val observations = rhrPoints.map { HeartRateObservation(it.timestamp, it.value) }
            circadianDecompositionService.decompose(observations, zoneId)
        } else null

        // 4. Deterministic Clinical Safety & Anomalies
        val evaluations = mutableListOf<ClinicalEvaluation>()
        if (rhrStats != null) {
            evaluations.addAll(clinicalSafetyRuleEngine.evaluateVitals("RESTING_HEART_RATE", rhrStats.todayValue))
        }
        if (spo2Points.isNotEmpty()) {
            evaluations.addAll(clinicalSafetyRuleEngine.evaluateVitals("SPO2", spo2Points.last().value))
        }
        for (bm in latestBiomarkers) {
            val norm = clinicalSchemaNormalizer.normalize(bm.rawName, bm.value.toString(), bm.unit, bm.referenceMin, bm.referenceMax)
            evaluations.addAll(clinicalSafetyRuleEngine.evaluateBiomarker(norm.canonicalName, norm.value, norm.unit))
        }

        val alerts = mutableListOf<AnomalyAlert>()
        for (bm in allBiomarkers) {
            alerts.addAll(anomalyDetectionEngine.evaluateBiomarker(bm, allBiomarkers))
        }
        if (rhrStats != null) {
            val rhrEntities = rhrPoints.map {
                VitalsRecordEntity(
                    id = it.id,
                    metricType = it.metricType,
                    value = it.value,
                    unit = it.unit,
                    timestamp = it.timestamp,
                    source = "HEALTH_CONNECT",
                    createdAt = now
                )
            }
            alerts.addAll(anomalyDetectionEngine.evaluateVitals("HEART_RATE", rhrStats.todayValue, "bpm", rhrEntities))
        }

        if (rhrStats != null && hrvStats != null && sleepPoints.isNotEmpty()) {
            val dailySleep = sleepPoints
                .groupBy { it.timestamp.atZone(zoneId).toLocalDate() }
                .mapValues { (_, pts) -> pts.sumOf { it.value } }
            val todaySleepHours = dailySleep[today] ?: dailySleep.values.last()

            val rhrSeries = rhrPoints.groupBy { it.timestamp.atZone(zoneId).toLocalDate() }.mapValues { (_, pts) -> pts.map { it.value }.average() }.values.toList()
            val hrvSeries = hrvPoints.groupBy { it.timestamp.atZone(zoneId).toLocalDate() }.mapValues { (_, pts) -> pts.map { it.value }.average() }.values.toList()

            val rhrMean = statisticalEngine.mean(rhrSeries)
            val rhrStdDev = statisticalEngine.stdDev(rhrSeries)
            val hrvMean = statisticalEngine.mean(hrvSeries)
            val hrvStdDev = statisticalEngine.stdDev(hrvSeries)

            val compoundStrainAlert = anomalyDetectionEngine.evaluateCompoundStrain(
                currentRhr = rhrStats.todayValue,
                baselineRhrMean = rhrMean,
                baselineRhrStdDev = rhrStdDev,
                currentHrv = hrvStats.todayValue,
                baselineHrvMean = hrvMean,
                baselineHrvStdDev = hrvStdDev,
                recentSleepHours = todaySleepHours
            )
            if (compoundStrainAlert != null) {
                alerts.add(compoundStrainAlert)
            }
        }

        val arbitrated = insightArbitrator.arbitrate(
            evaluations = evaluations,
            alerts = alerts,
            rawAiReport = ""
        )

        // 5. Lifestyle Clustering & Correlations
        val allDates = mutableSetOf<LocalDate>()
        val stepsByDate = stepPoints.groupBy { it.timestamp.atZone(zoneId).toLocalDate() }
        val sleepByDate = sleepPoints.groupBy { it.timestamp.atZone(zoneId).toLocalDate() }
        val hrvByDate = hrvPoints.groupBy { it.timestamp.atZone(zoneId).toLocalDate() }
        val calByDate = calPoints.groupBy { it.timestamp.atZone(zoneId).toLocalDate() }
        val meals30dByDate = meals30d.groupBy { it.consumedAt.atZone(zoneId).toLocalDate() }

        allDates.addAll(stepsByDate.keys)
        allDates.addAll(sleepByDate.keys)
        allDates.addAll(hrvByDate.keys)
        allDates.addAll(calByDate.keys)

        val lifestyleArchetypeStr = if (allDates.size >= 3) {
            val featureVectors = allDates.sorted().map { date ->
                val daySteps = stepsByDate[date]?.sumOf { it.value } ?: 0.0
                val daySleep = sleepByDate[date]?.sumOf { it.value } ?: 0.0
                val dayHrv = hrvByDate[date]?.map { it.value }?.average() ?: 0.0
                val dayCal = calByDate[date]?.sumOf { it.value } ?: 0.0
                val dayMeals = meals30dByDate[date] ?: emptyList()
                val lateDinner = dayMeals.any { m ->
                    m.consumedAt.atZone(zoneId).toLocalTime().isAfter(LocalTime.of(20, 30))
                }

                DailyFeatureVector(
                    date = date,
                    steps = daySteps,
                    sleepHours = daySleep,
                    hrvRmssd = dayHrv,
                    activeCalories = dayCal,
                    lateDinnerFlag = lateDinner
                )
            }
            val clusterResult = lifestyleKMeansClusterer.cluster(featureVectors, k = 4)
            clusterResult.archetype.name
        } else {
            "UNDETERMINED (Insufficient data)"
        }

        // Build Payload
        val prompt = buildPayload(
            today = today,
            lifestyleArchetypeStr = lifestyleArchetypeStr,
            evaluations = evaluations,
            alerts = alerts,
            rhrPoints = rhrPoints,
            rhrStats = rhrStats,
            circadianResult = circadianResult,
            hrvPoints = hrvPoints,
            hrvStats = hrvStats,
            sleepPoints = sleepPoints,
            stepPoints = stepPoints,
            calPoints = calPoints,
            spo2Points = spo2Points,
            weightPoints = weightPoints,
            meals24h = meals24h,
            meals7d = meals7d,
            allBiomarkers = allBiomarkers,
            latestBiomarkers = latestBiomarkers,
            zoneId = zoneId
        )

        // Fail-safe arbitration & LLM invocation
        if (!llmClient.isConfigured()) {
            return@withContext DailyBiometricInsightReport.createDeterministicFallback(
                alert = arbitrated.safetyNotice,
                reason = "API key not configured"
            )
        }

        try {
            val response = llmClient.generateContent(
                prompt = prompt,
                systemInstructionOverride = SYSTEM_INSTRUCTION,
                responseSchema = GeminiClient.createDailyReportSchema(),
                responseMimeType = "application/json",
                temperature = 0.2
            )

            val parsedReport = DailyBiometricInsightReport.fromJson(response)

            // Deterministic Safety Precedence:
            // If local arbitration flagged clinical alerts but LLM left alert null, force injection
            if (arbitrated.hasClinicalSafetyNotice && parsedReport.clinicalArbitrationAlert == null) {
                parsedReport.copy(clinicalArbitrationAlert = arbitrated.safetyNotice)
            } else {
                parsedReport
            }
        } catch (e: Exception) {
            DailyBiometricInsightReport.createDeterministicFallback(
                alert = arbitrated.safetyNotice,
                reason = e.message ?: "Failed to generate report"
            )
        }
    }

    fun executeStream(): Flow<ReportChunk> = flow {
        if (!llmClient.isConfigured()) {
            emit(ReportChunk.Error("API key not configured"))
            return@flow
        }
        emit(ReportChunk.Started)
        val report = execute()
        emit(ReportChunk.Text(report.toFormattedMarkdown()))
        emit(ReportChunk.Done)
    }
        .catch { e -> emit(ReportChunk.Error(e.message ?: "Unknown error")) }
        .flowOn(Dispatchers.IO)

    internal suspend fun buildPayload(
        today: LocalDate,
        lifestyleArchetypeStr: String,
        evaluations: List<ClinicalEvaluation>,
        alerts: List<AnomalyAlert>,
        rhrPoints: List<VitalPoint>,
        rhrStats: MetricStats?,
        circadianResult: io.biotrace.app.domain.analytics.timeseries.CircadianDecompositionResult?,
        hrvPoints: List<VitalPoint>,
        hrvStats: MetricStats?,
        sleepPoints: List<VitalPoint>,
        stepPoints: List<VitalPoint>,
        calPoints: List<VitalPoint>,
        spo2Points: List<VitalPoint>,
        weightPoints: List<VitalPoint>,
        meals24h: List<MealEntity>,
        meals7d: List<MealEntity>,
        allBiomarkers: List<LabBiomarkerEntity>,
        latestBiomarkers: List<LabBiomarkerEntity>,
        zoneId: ZoneId
    ): String = buildString {
        appendLine("# USER BIOMETRIC PAYLOAD (LOCAL ROOM DB SNAPSHOT)")
        appendLine("Analysis Date: $today")
        appendLine("Detected Lifestyle Archetype: $lifestyleArchetypeStr")
        appendLine()

        // 1. Deterministic Clinical Safety & Anomaly Flags
        appendLine("## 1. DETERMINISTIC CLINICAL SAFETY & ANOMALY FLAGS")
        if (evaluations.isEmpty() && alerts.isEmpty()) {
            appendLine("No active clinical safety alerts or anomalies detected.")
        } else {
            val flagsArray = JSONArray()
            for (e in evaluations) {
                flagsArray.put(
                    JSONObject().apply {
                        put("id", e.ruleId)
                        put("type", "CLINICAL_SAFETY_RULE")
                        put("metric", e.metricName)
                        put("status", e.status.name)
                        put("value", e.value)
                        put("unit", e.unit)
                        put("guideline", e.guidelineSource)
                        put("narrative", e.narrative)
                    }
                )
            }
            for (a in alerts) {
                flagsArray.put(
                    JSONObject().apply {
                        put("id", a.id)
                        put("type", "ANOMALY")
                        put("metric", a.metricName)
                        put("severity", a.severity)
                        put("current_value", a.currentValue)
                        if (a.baselineValue != null) put("baseline_value", a.baselineValue) else put("baseline_value", JSONObject.NULL)
                        put("unit", a.unit)
                        put("narrative", a.narrative)
                    }
                )
            }
            appendLine(flagsArray.toString(2))
        }
        appendLine()

        // 2. Health Connect Telemetry
        appendLine("## 2. HEALTH CONNECT TELEMETRY (Today vs. 7D EWMA vs. 30D Median)")

        // Resting Heart Rate
        if (rhrPoints.isNotEmpty() && rhrStats != null) {
            val dipPctStr = if (circadianResult != null) String.format(Locale.US, "%.1f", circadianResult.dipPercentage) else "0.0"
            val patternStr = circadianResult?.pattern?.name ?: "NON_DIPPER"
            appendLine(
                "- Resting Heart Rate (`HeartRateRecord` [ID: ${rhrStats.latestId}]): Today=${rhrStats.todayValue.roundToInt()} bpm | " +
                    "7D_EWMA=${String.format(Locale.US, "%.1f", rhrStats.ewma7d)} bpm | " +
                    "30D_Median=${String.format(Locale.US, "%.1f", rhrStats.median30d)} bpm | " +
                    "Z-Score=${String.format(Locale.US, "%.2f", rhrStats.zScore)} | " +
                    "Nocturnal Dip=$dipPctStr% ($patternStr)"
            )
        } else {
            appendLine("- Resting Heart Rate (`HeartRateRecord`): No records logged in window")
        }

        // HRV
        if (hrvPoints.isNotEmpty() && hrvStats != null) {
            appendLine(
                "- HRV (`HeartRateVariabilityRmssdRecord` [ID: ${hrvStats.latestId}]): Today=${hrvStats.todayValue.roundToInt()} ms | " +
                    "7D_EWMA=${String.format(Locale.US, "%.1f", hrvStats.ewma7d)} ms | " +
                    "30D_Median=${String.format(Locale.US, "%.1f", hrvStats.median30d)} ms | " +
                    "Z-Score=${String.format(Locale.US, "%.2f", hrvStats.zScore)}"
            )
        } else {
            appendLine("- HRV (`HeartRateVariabilityRmssdRecord`): No records logged in window")
        }

        // Sleep
        if (sleepPoints.isNotEmpty()) {
            val latestId = sleepPoints.last().id
            val dailySleep = sleepPoints
                .groupBy { it.timestamp.atZone(zoneId).toLocalDate() }
                .mapValues { (_, pts) -> pts.sumOf { it.value } }
                .toSortedMap()
            val todaySleepHours = dailySleep[today] ?: dailySleep.values.last()
            val sleepDurations = dailySleep.values.toList()

            val currentHrv = hrvStats?.todayValue
            val baselineHrv = hrvStats?.median30d
            val sleepDebtRes = sleepDebtCalculator.calculate(
                sleepDurationsHours = sleepDurations,
                targetSleepHours = 8.0,
                currentHrvRmssd = currentHrv,
                baselineHrvRmssd = baselineHrv
            )

            var eff = "--"
            var deep = "--"
            var rem = "--"
            var light = "--"
            try {
                val detail = vitalsRepository.getMetricDetailData(VitalMetricType.SLEEP, MetricTimeRange.DAY)
                val bar = detail.sleepBars.lastOrNull()
                if (bar != null && bar.totalMinutes > 0f) {
                    val total = bar.totalMinutes
                    eff = ((total - bar.awakeMinutes) / total * 100).roundToInt().toString()
                    deep = (bar.deepMinutes / total * 100).roundToInt().toString()
                    rem = (bar.remMinutes / total * 100).roundToInt().toString()
                    light = (bar.lightMinutes / total * 100).roundToInt().toString()
                }
            } catch (_: Exception) {}

            val totalH = todaySleepHours.toInt()
            val totalM = ((todaySleepHours - totalH) * 60).roundToInt()
            val totalStr = "${totalH}h ${totalM}m"
            val debtStr = String.format(Locale.US, "%.1f", sleepDebtRes.cumulativeDebtHours)

            appendLine("- Sleep (`SleepSessionRecord` [ID: $latestId]): Total=$totalStr | Efficiency=$eff% | Deep=$deep% | REM=$rem% | Light=$light% | 14D Sleep Debt=${debtStr}h")
        } else {
            appendLine("- Sleep (`SleepSessionRecord`): No records logged in window")
        }

        // Activity
        if (stepPoints.isNotEmpty() || calPoints.isNotEmpty()) {
            val latestId = stepPoints.lastOrNull()?.id ?: calPoints.lastOrNull()?.id ?: "N/A"

            val dailySteps = stepPoints
                .groupBy { it.timestamp.atZone(zoneId).toLocalDate() }
                .mapValues { (_, pts) -> pts.sumOf { it.value } }
            val todaySteps = (dailySteps[today] ?: dailySteps.values.lastOrNull() ?: 0.0).roundToInt()
            val steps7d = dailySteps.values.toList().takeLast(7).let { if (it.isNotEmpty()) it.average().roundToInt() else 0 }

            val dailyCal = calPoints
                .groupBy { it.timestamp.atZone(zoneId).toLocalDate() }
                .mapValues { (_, pts) -> pts.sumOf { it.value } }
            val todayCal = (dailyCal[today] ?: dailyCal.values.lastOrNull() ?: 0.0).roundToInt()
            val cal7d = dailyCal.values.toList().takeLast(7).let { if (it.isNotEmpty()) it.average().roundToInt() else 0 }

            appendLine("- Activity (`StepsRecord` / `ActiveCaloriesBurnedRecord` [ID: $latestId]): Steps=$todaySteps (7D Avg: $steps7d) | Active Burn=$todayCal kcal (7D Avg: $cal7d kcal)")
        } else {
            appendLine("- Activity (`StepsRecord` / `ActiveCaloriesBurnedRecord`): No records logged in window")
        }

        // SpO2
        if (spo2Points.isNotEmpty()) {
            val latestId = spo2Points.last().id
            val dailySpo2 = spo2Points
                .groupBy { it.timestamp.atZone(zoneId).toLocalDate() }
                .mapValues { (_, pts) -> pts.map { it.value }.average() }
                .toSortedMap()
            val series = dailySpo2.values.toList()
            val todaySpo2 = dailySpo2[today] ?: series.last()
            val median30d = statisticalEngine.median(series)
            val todayStr = String.format(Locale.US, "%.1f", todaySpo2)
            val medianStr = String.format(Locale.US, "%.1f", median30d)
            appendLine("- SpO2 (`OxygenSaturationRecord` [ID: $latestId]): Night Avg=$todayStr% (30D Median: $medianStr%)")
        } else {
            appendLine("- SpO2 (`OxygenSaturationRecord`): No records logged in window")
        }

        // Weight
        if (weightPoints.isNotEmpty()) {
            val latestId = weightPoints.last().id
            val latestWeight = weightPoints.last().value
            val firstWeight = weightPoints.first().value
            val delta = latestWeight - firstWeight
            val wtStr = String.format(Locale.US, "%.1f", latestWeight)
            val deltaStr = String.format(Locale.US, "%+.1f", delta)
            appendLine("- Weight (`WeightRecord` [ID: $latestId]): Latest=$wtStr kg | 30D Delta=$deltaStr kg")
        } else {
            appendLine("- Weight (`WeightRecord`): No records logged in window")
        }
        appendLine()

        // 3. Nutrition & Meals Log
        appendLine("## 3. NUTRITION & MEALS LOG (Last 24 Hours & 7D Macro Averages)")
        val meals7dByDate = meals7d.groupBy { it.consumedAt.atZone(zoneId).toLocalDate() }
        if (meals7dByDate.isNotEmpty()) {
            val dailyCals = meals7dByDate.values.map { dayMeals -> dayMeals.sumOf { it.calories } }
            val dailyProtein = meals7dByDate.values.map { dayMeals -> dayMeals.sumOf { it.proteinGrams } }
            val dailyCarbs = meals7dByDate.values.map { dayMeals -> dayMeals.sumOf { it.carbsGrams } }
            val dailyFat = meals7dByDate.values.map { dayMeals -> dayMeals.sumOf { it.fatsGrams } }
            val dailyFiber = meals7dByDate.values.map { dayMeals -> dayMeals.sumOf { it.fiberGrams ?: 0.0 } }

            val avgKcal = dailyCals.average().roundToInt()
            val avgP = String.format(Locale.US, "%.1f", dailyProtein.average())
            val avgC = String.format(Locale.US, "%.1f", dailyCarbs.average())
            val avgF = String.format(Locale.US, "%.1f", dailyFat.average())
            val avgFib = String.format(Locale.US, "%.1f", dailyFiber.average())
            appendLine("- 7D Rolling Daily Averages: $avgKcal kcal | Protein: ${avgP}g | Carbs: ${avgC}g | Fat: ${avgF}g | Fiber: ${avgFib}g")
        } else {
            appendLine("- 7D Rolling Daily Averages: No records logged in window")
        }

        appendLine("- Last 24h Logged Meals:")
        if (meals24h.isNotEmpty()) {
            val array = JSONArray()
            for (m in meals24h) {
                array.put(
                    JSONObject().apply {
                        put("meal_id", m.id)
                        put("timestamp", m.consumedAt.toString())
                        put("items", m.name)
                        put("kcal", m.calories)
                        put("protein_g", m.proteinGrams)
                        put("carbs_g", m.carbsGrams)
                        put("fat_g", m.fatsGrams)
                        put("fiber_g", m.fiberGrams ?: 0.0)
                    }
                )
            }
            appendLine(array.toString(2))
        } else {
            appendLine("No records logged in window")
        }
        appendLine()

        // 4. Most Recent Normalized Blood & Lab Panels
        appendLine("## 4. MOST RECENT NORMALIZED BLOOD & LAB PANELS (`ClinicalSchemaNormalizer`)")
        if (latestBiomarkers.isNotEmpty()) {
            val array = JSONArray()
            for (bm in latestBiomarkers) {
                val norm = clinicalSchemaNormalizer.normalize(
                    bm.rawName,
                    bm.value.toString(),
                    bm.unit,
                    bm.referenceMin,
                    bm.referenceMax
                )
                val prevValue = allBiomarkers
                    .filter { it.biomarkerName == bm.biomarkerName && it.id != bm.id }
                    .firstOrNull()?.value
                val panel = determineLabPanel(norm.canonicalName)
                val refRange = if (norm.referenceMin != null && norm.referenceMax != null) {
                    "${norm.referenceMin}-${norm.referenceMax} ${norm.unit}"
                } else "N/A"

                array.put(
                    JSONObject().apply {
                        put("record_id", bm.id)
                        put("biomarker", norm.displayName)
                        put("lab_panel", panel)
                        put("value", norm.value)
                        put("unit", norm.unit)
                        put("ref_range", refRange)
                        put("flag", norm.flag)
                        if (prevValue != null) put("prev_value", prevValue) else put("prev_value", JSONObject.NULL)
                    }
                )
            }
            appendLine(array.toString(2))
        } else {
            appendLine("No records logged in window")
        }
        appendLine()

        // 5. Pre-computed Multi-Metric Correlations
        appendLine("## 5. PRE-COMPUTED MULTI-METRIC CORRELATIONS (30-Day Window)")
        val seriesMap = mutableMapOf<String, List<DailyMetricSample>>()
        if (rhrPoints.isNotEmpty()) {
            seriesMap["HEART_RATE"] = rhrPoints
                .groupBy { it.timestamp.atZone(zoneId).toLocalDate() }
                .map { (d, pts) -> DailyMetricSample(d, pts.map { it.value }.average()) }
                .sortedBy { it.date }
        }
        if (hrvPoints.isNotEmpty()) {
            seriesMap["HRV_RMSSD"] = hrvPoints
                .groupBy { it.timestamp.atZone(zoneId).toLocalDate() }
                .map { (d, pts) -> DailyMetricSample(d, pts.map { it.value }.average()) }
                .sortedBy { it.date }
        }
        if (sleepPoints.isNotEmpty()) {
            seriesMap["SLEEP"] = sleepPoints
                .groupBy { it.timestamp.atZone(zoneId).toLocalDate() }
                .map { (d, pts) -> DailyMetricSample(d, pts.sumOf { it.value }) }
                .sortedBy { it.date }
        }
        if (stepPoints.isNotEmpty()) {
            seriesMap["STEPS"] = stepPoints
                .groupBy { it.timestamp.atZone(zoneId).toLocalDate() }
                .map { (d, pts) -> DailyMetricSample(d, pts.sumOf { it.value }) }
                .sortedBy { it.date }
        }
        if (calPoints.isNotEmpty()) {
            seriesMap["CALORIES"] = calPoints
                .groupBy { it.timestamp.atZone(zoneId).toLocalDate() }
                .map { (d, pts) -> DailyMetricSample(d, pts.sumOf { it.value }) }
                .sortedBy { it.date }
        }

        if (seriesMap.size >= 2) {
            val matrix = metricPairwiseCorrelator.computeCorrelationMatrix(seriesMap, lagDays = 0, alpha = 0.05)
            val significant = matrix.filter { it.isStatisticallySignificant || it.pValue < 0.05 }
            if (significant.isNotEmpty()) {
                val array = JSONArray()
                for (res in significant) {
                    array.put(
                        JSONObject().apply {
                            put("pair", "${res.metric1} vs ${res.metric2}")
                            put("pearson_r", (res.pearsonR * 100).roundToInt() / 100.0)
                            put("spearman_rho", (res.spearmanRho * 100).roundToInt() / 100.0)
                            put("p_value", (res.pValue * 1000).roundToInt() / 1000.0)
                            put("lag_days", res.lagDays)
                        }
                    )
                }
                appendLine(array.toString(2))
            } else {
                appendLine("No records logged in window")
            }
        } else {
            appendLine("No records logged in window")
        }
        appendLine()

        appendLine("---")
        appendLine("Based on the biometric payload above, generate today's daily insights and cross-modal trend analysis. Prioritize any active deterministic safety flags, connect overnight wearable recovery to yesterday's meal timing/macros, and contextualize ongoing trends with the user's latest blood biomarker panel.")
    }

    private fun computeSeriesStats(
        points: List<VitalPoint>,
        zoneId: ZoneId,
        today: LocalDate
    ): MetricStats? {
        if (points.isEmpty()) return null
        val daily = points
            .groupBy { it.timestamp.atZone(zoneId).toLocalDate() }
            .mapValues { (_, pts) -> pts.map { it.value }.average() }
            .toSortedMap()

        val series = daily.values.toList()
        if (series.isEmpty()) return null

        val latestId = points.last().id
        val todayVal = daily[today] ?: series.last()
        val sevenDaySeries = series.takeLast(7)
        val ewma7d = statisticalEngine.ewma(sevenDaySeries, 0.2)
        val median30d = statisticalEngine.median(series)
        val mean30d = statisticalEngine.mean(series)
        val stdDev30d = statisticalEngine.stdDev(series)
        val zScore = statisticalEngine.zScore(todayVal, mean30d, stdDev30d)

        return MetricStats(
            latestId = latestId,
            todayValue = todayVal,
            ewma7d = ewma7d,
            median30d = median30d,
            zScore = zScore
        )
    }

    private fun determineLabPanel(canonicalName: String): String {
        val key = canonicalName.uppercase(Locale.ROOT)
        return when {
            key in listOf("HBA1C", "FASTING_GLUCOSE", "PP_GLUCOSE", "FASTING_INSULIN", "C_PEPTIDE") -> "Glycemic Panel"
            key in listOf("CHOLESTEROL_TOTAL", "HDL", "LDL", "TRIGLYCERIDES", "VLDL", "NON_HDL_CHOLESTEROL", "APOB", "APOA1", "LIPOPROTEIN_A") -> "Lipid Panel"
            key in listOf("FERRITIN", "SERUM_IRON", "TIBC", "TRANSFERRIN", "TRANSFERRIN_SATURATION", "HEMOGLOBIN", "HEMATOCRIT", "RBC", "MCV", "MCH", "MCHC", "RDW", "WBC", "PLATELETS", "NEUTROPHILS", "LYMPHOCYTES", "MONOCYTES", "EOSINOPHILS", "BASOPHILS") -> "Complete Blood Count & Iron"
            key in listOf("CREATININE", "BUN", "UREA", "EGFR", "URIC_ACID", "SODIUM", "POTASSIUM", "CHLORIDE", "CALCIUM", "MAGNESIUM", "PHOSPHORUS") -> "Renal & Metabolic Panel"
            key in listOf("ALT", "AST", "ALP", "BILIRUBIN_TOTAL", "BILIRUBIN_DIRECT", "ALBUMIN", "TOTAL_PROTEIN", "GGT") -> "Liver Function Panel"
            key in listOf("TSH", "FREE_T3", "FREE_T4", "TOTAL_T3", "TOTAL_T4", "TESTOSTERONE_TOTAL", "TESTOSTERONE_FREE", "ESTRADIOL", "PROGESTERONE") -> "Thyroid & Endocrine Panel"
            key in listOf("VITAMIN_D", "VITAMIN_B12", "FOLATE") -> "Vitamin Panel"
            key in listOf("HS_CRP", "CRP", "ESR") -> "Inflammatory Markers"
            else -> "Comprehensive Metabolic Panel"
        }
    }
}

internal data class MetricStats(
    val latestId: String,
    val todayValue: Double,
    val ewma7d: Double,
    val median30d: Double,
    val zScore: Double
)

private class EmptyMealDao : MealDao {
    override suspend fun insert(meal: MealEntity) {}
    override suspend fun insertAll(meals: List<MealEntity>) {}
    override fun getMealsBetweenFlow(startTime: Instant, endTime: Instant): Flow<List<MealEntity>> = flowOf(emptyList())
    override suspend fun getMealsBetween(startTime: Instant, endTime: Instant): List<MealEntity> = emptyList()
    override suspend fun getLatestMeal(): MealEntity? = null
    override fun getLatestMealFlow(): Flow<MealEntity?> = flowOf(null)
    override suspend fun deleteAll() {}
}
