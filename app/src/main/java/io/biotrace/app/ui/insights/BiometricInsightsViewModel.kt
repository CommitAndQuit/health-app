package io.biotrace.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.biotrace.app.data.local.dao.LabBiomarkerDao
import io.biotrace.app.data.local.entity.LabBiomarkerEntity
import io.biotrace.app.data.local.store.UserProfileStore
import io.biotrace.app.domain.analytics.clustering.DailyFeatureVector
import io.biotrace.app.domain.analytics.clustering.LifestyleArchetype
import io.biotrace.app.domain.analytics.clustering.LifestyleClusterResult
import io.biotrace.app.domain.analytics.clustering.LifestyleKMeansClusterer
import io.biotrace.app.domain.analytics.correlation.CorrelationResult
import io.biotrace.app.domain.analytics.correlation.DailyMetricSample
import io.biotrace.app.domain.analytics.correlation.MetricPairwiseCorrelator
import io.biotrace.app.domain.analytics.timeseries.CircadianDecompositionResult
import io.biotrace.app.domain.analytics.timeseries.CircadianDecompositionService
import io.biotrace.app.domain.analytics.timeseries.HeartRateObservation
import io.biotrace.app.domain.anomaly.AnomalyAlert
import io.biotrace.app.domain.anomaly.AnomalyDetectionEngine
import io.biotrace.app.domain.clinical.risk.AtherogenicRiskCalculator
import io.biotrace.app.domain.clinical.risk.AtherogenicRiskResult
import io.biotrace.app.domain.clinical.risk.HarmonizedMetabolicRiskCalculator
import io.biotrace.app.domain.clinical.risk.MetabolicRiskResult
import io.biotrace.app.domain.clinical.risk.SleepDebtCalculator
import io.biotrace.app.domain.clinical.risk.SleepDebtResult
import io.biotrace.app.domain.clinical.rules.ClinicalEvaluation
import io.biotrace.app.domain.clinical.rules.ClinicalSafetyRuleEngine
import io.biotrace.app.domain.insights.arbitration.ArbitratedInsight
import io.biotrace.app.domain.insights.arbitration.InsightArbitrator
import io.biotrace.app.domain.llm.LlmClient
import io.biotrace.app.domain.longevity.BiologicalAgeResult
import io.biotrace.app.domain.model.ProfileTriptychSummary
import io.biotrace.app.domain.model.VitalMetricType
import io.biotrace.app.domain.repository.VitalsRepository
import io.biotrace.app.domain.usecase.GenerateHealthReportUseCase
import io.biotrace.app.domain.usecase.GetBiologicalAgeUseCase
import io.biotrace.app.domain.usecase.ReportChunk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject

enum class InsightsTab {
    OVERVIEW,
    CORRELATIONS,
    CLINICAL_RISKS,
    AI_DEEP_DIVE
}

data class CorrelationCardData(
    val title: String,
    val category: String,
    val primaryInsight: String,
    val metric1Label: String,
    val metric1Value: String,
    val metric2Label: String,
    val metric2Value: String,
    val couplingScore: String,
    val statusTag: String
)

data class ScatterPlotData(
    val points: List<Pair<Double, Double>>,
    val xLabel: String,
    val yLabel: String,
    val pearsonR: Double,
    val spearmanRho: Double? = null
)

sealed class AiReportState {
    data object Idle      : AiReportState()
    data object Loading   : AiReportState()
    data class  Streaming(val text: String) : AiReportState()
    data class  Complete(val text: String)  : AiReportState()
    data class  Error(val message: String)  : AiReportState()
}

data class BiometricInsightsUiState(
    val profile: ProfileTriptychSummary = ProfileTriptychSummary(),
    val correlations: List<CorrelationCardData> = emptyList(),
    val labBiomarkers: List<LabBiomarkerEntity> = emptyList(),
    val biologicalAge: BiologicalAgeResult? = null,
    val isAgeDialogOpen: Boolean = false,
    val isLoading: Boolean = false,
    val isApiKeyConfigured: Boolean = false,
    val reportState: AiReportState = AiReportState.Idle,
    val selectedTab: InsightsTab = InsightsTab.OVERVIEW,
    val atherogenicRisk: AtherogenicRiskResult? = null,
    val metabolicRisk: MetabolicRiskResult? = null,
    val sleepDebt: SleepDebtResult? = null,
    val circadianDecomposition: CircadianDecompositionResult? = null,
    val lifestyleClusterResult: LifestyleClusterResult? = null,
    val activeAnomalies: List<AnomalyAlert> = emptyList(),
    val clinicalEvaluations: List<ClinicalEvaluation> = emptyList(),
    val arbitratedInsight: ArbitratedInsight? = null,
    val scatterData: ScatterPlotData? = null
)

@HiltViewModel
class BiometricInsightsViewModel @Inject constructor(
    private val vitalsRepository: VitalsRepository,
    private val labBiomarkerDao: LabBiomarkerDao,
    private val generateHealthReportUseCase: GenerateHealthReportUseCase,
    private val llmClient: LlmClient,
    private val getBiologicalAgeUseCase: GetBiologicalAgeUseCase,
    private val userProfileStore: UserProfileStore,
    clinicalSafetyRuleEngine: ClinicalSafetyRuleEngine? = null,
    atherogenicRiskCalculator: AtherogenicRiskCalculator? = null,
    harmonizedMetabolicRiskCalculator: HarmonizedMetabolicRiskCalculator? = null,
    sleepDebtCalculator: SleepDebtCalculator? = null,
    metricPairwiseCorrelator: MetricPairwiseCorrelator? = null,
    circadianDecompositionService: CircadianDecompositionService? = null,
    lifestyleKMeansClusterer: LifestyleKMeansClusterer? = null,
    anomalyDetectionEngine: AnomalyDetectionEngine? = null,
    insightArbitrator: InsightArbitrator? = null
) : ViewModel() {

    private val safeClinicalSafetyRuleEngine: ClinicalSafetyRuleEngine =
        clinicalSafetyRuleEngine ?: ClinicalSafetyRuleEngine()
    private val safeAtherogenicRiskCalculator: AtherogenicRiskCalculator =
        atherogenicRiskCalculator ?: AtherogenicRiskCalculator()
    private val safeHarmonizedMetabolicRiskCalculator: HarmonizedMetabolicRiskCalculator =
        harmonizedMetabolicRiskCalculator ?: HarmonizedMetabolicRiskCalculator()
    private val safeSleepDebtCalculator: SleepDebtCalculator =
        sleepDebtCalculator ?: SleepDebtCalculator()
    private val safeMetricPairwiseCorrelator: MetricPairwiseCorrelator =
        metricPairwiseCorrelator ?: MetricPairwiseCorrelator
    private val safeCircadianDecompositionService: CircadianDecompositionService =
        circadianDecompositionService ?: CircadianDecompositionService
    private val safeLifestyleKMeansClusterer: LifestyleKMeansClusterer =
        lifestyleKMeansClusterer ?: LifestyleKMeansClusterer()
    private val safeAnomalyDetectionEngine: AnomalyDetectionEngine =
        anomalyDetectionEngine ?: AnomalyDetectionEngine()
    private val safeInsightArbitrator: InsightArbitrator =
        insightArbitrator ?: InsightArbitrator(safeClinicalSafetyRuleEngine, safeAnomalyDetectionEngine)

    private val _uiState = MutableStateFlow(BiometricInsightsUiState())
    val uiState: StateFlow<BiometricInsightsUiState> = _uiState.asStateFlow()

    private var lastReportDate: java.time.LocalDate? = null

    init {
        loadInsights()
        checkApiKeyAndMaybeGenerate()
    }

    private fun checkApiKeyAndMaybeGenerate() {
        viewModelScope.launch {
            val configured = llmClient.isConfigured()
            _uiState.update { it.copy(isApiKeyConfigured = configured) }
            val today = java.time.LocalDate.now()
            if (configured && lastReportDate != today) {
                generateReport()
            }
        }
    }

    fun selectTab(tab: InsightsTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun generateReport() {
        viewModelScope.launch {
            var buffer = ""
            generateHealthReportUseCase.execute().collect { chunk ->
                when (chunk) {
                    is ReportChunk.Started -> _uiState.update { it.copy(reportState = AiReportState.Loading) }
                    is ReportChunk.Text    -> {
                        buffer += chunk.text
                        _uiState.update { it.copy(reportState = AiReportState.Streaming(buffer)) }
                    }
                    is ReportChunk.Done    -> {
                        lastReportDate = java.time.LocalDate.now()
                        val arbitrated = safeInsightArbitrator.arbitrate(
                            evaluations = _uiState.value.clinicalEvaluations,
                            alerts = _uiState.value.activeAnomalies,
                            rawAiReport = buffer
                        )
                        _uiState.update {
                            it.copy(
                                reportState = AiReportState.Complete(buffer),
                                arbitratedInsight = arbitrated
                            )
                        }
                    }
                    is ReportChunk.Error   -> _uiState.update { it.copy(reportState = AiReportState.Error(chunk.message)) }
                }
            }
        }
    }

    fun loadInsights() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val bioAge = try {
                getBiologicalAgeUseCase()
            } catch (e: Exception) {
                null
            }

            val profile = (try {
                vitalsRepository.getProfileTriptychSummary()
            } catch (e: Exception) {
                ProfileTriptychSummary()
            }) ?: ProfileTriptychSummary()

            val cards = (try {
                vitalsRepository.getVitalsDashboardSnapshot()
            } catch (e: Exception) {
                emptyList()
            }) ?: emptyList()

            val labs = (try {
                labBiomarkerDao.getAll()
            } catch (e: Exception) {
                emptyList()
            }) ?: emptyList()

            val now = Instant.now()
            val thirtyDaysAgo = now.minus(Duration.ofDays(30))
            val zone = ZoneId.systemDefault()

            val hrPoints = (try {
                vitalsRepository.getVitalsBetween("HEART_RATE", thirtyDaysAgo, now)
            } catch (e: Exception) {
                emptyList()
            }) ?: emptyList()

            val hrvPoints = (try {
                vitalsRepository.getVitalsBetween("HRV_RMSSD", thirtyDaysAgo, now)
            } catch (e: Exception) {
                emptyList()
            } ?: emptyList()).ifEmpty {
                try {
                    vitalsRepository.getVitalsBetween("HRV", thirtyDaysAgo, now)
                } catch (e: Exception) {
                    emptyList()
                } ?: emptyList()
            }

            val sleepPoints = (try {
                vitalsRepository.getVitalsBetween("SLEEP", thirtyDaysAgo, now)
            } catch (e: Exception) {
                emptyList()
            }) ?: emptyList()

            val stepsPoints = (try {
                vitalsRepository.getVitalsBetween("STEPS", thirtyDaysAgo, now)
            } catch (e: Exception) {
                emptyList()
            }) ?: emptyList()

            val calPoints = (try {
                vitalsRepository.getVitalsBetween("CALORIES", thirtyDaysAgo, now)
            } catch (e: Exception) {
                emptyList()
            }) ?: emptyList()

            // 1. Clinical Safety Rule Evaluations & Anomaly Detection
            val clinicalEvaluations = mutableListOf<ClinicalEvaluation>()
            val activeAnomalies = mutableListOf<AnomalyAlert>()

            val sex = (try {
                userProfileStore.getBiologicalSex()
            } catch (e: Exception) {
                "UNSPECIFIED"
            }) ?: "UNSPECIFIED"
            val isMale = sex.equals("male", ignoreCase = true)

            for (bm in labs) {
                val evals = safeClinicalSafetyRuleEngine.evaluateBiomarker(
                    normalizedName = bm.biomarkerName,
                    value = bm.value,
                    unit = bm.unit,
                    biologicalSex = sex
                )
                clinicalEvaluations.addAll(evals)

                val alerts = safeAnomalyDetectionEngine.evaluateBiomarker(bm, labs)
                activeAnomalies.addAll(alerts)
            }

            hrPoints.lastOrNull()?.let { latestHr ->
                val rhrEvals = safeClinicalSafetyRuleEngine.evaluateVitals(
                    metricType = "RESTING_HEART_RATE",
                    value = latestHr.value,
                    biologicalSex = sex
                )
                clinicalEvaluations.addAll(rhrEvals)
            }

            // 2. Atherogenic Lipid Risk Calculation
            val cholEntity = labs.firstOrNull {
                val n = it.biomarkerName.uppercase(Locale.ROOT)
                n == "CHOLESTEROL" || n == "TOTAL_CHOLESTEROL" || n == "TOTAL CHOLESTEROL"
            }
            val hdlEntity = labs.firstOrNull {
                val n = it.biomarkerName.uppercase(Locale.ROOT)
                n == "HDL" || n == "HDL_CHOLESTEROL" || n == "HDL CHOLESTEROL"
            }
            val tgEntity = labs.firstOrNull {
                val n = it.biomarkerName.uppercase(Locale.ROOT)
                n == "TRIGLYCERIDES" || n == "TG"
            }
            val glucoseEntity = labs.firstOrNull {
                val n = it.biomarkerName.uppercase(Locale.ROOT)
                n == "FASTING_GLUCOSE" || n == "GLUCOSE" || n == "BLOOD_GLUCOSE"
            }
            val hba1cEntity = labs.firstOrNull {
                val n = it.biomarkerName.uppercase(Locale.ROOT)
                n == "HBA1C" || n == "HEMOGLOBIN_A1C"
            }

            val atherogenicResult = if (cholEntity != null && hdlEntity != null && tgEntity != null && hdlEntity.value > 0.0) {
                try {
                    safeAtherogenicRiskCalculator.calculate(
                        totalCholesterolMgDl = cholEntity.value,
                        hdlMgDl = hdlEntity.value,
                        triglyceridesMgDl = tgEntity.value
                    )
                } catch (e: Exception) {
                    null
                }
            } else null

            // 3. Harmonized Metabolic Risk Calculation
            val metabolicResult = try {
                safeHarmonizedMetabolicRiskCalculator.calculate(
                    fastingGlucoseMgDl = glucoseEntity?.value,
                    hba1cPercent = hba1cEntity?.value,
                    triglyceridesMgDl = tgEntity?.value,
                    hdlMgDl = hdlEntity?.value,
                    isMale = isMale,
                    bmi = null
                )
            } catch (e: Exception) {
                null
            }

            // 4. Cumulative Sleep Debt & Allostatic Strain
            val dailySleepMap = sleepPoints.groupBy { it.timestamp.atZone(zone).toLocalDate() }
            val dailySleepHours = dailySleepMap.values.map { it.sumOf { p -> p.value } }
            val currentHrv = hrvPoints.lastOrNull()?.value
            val baselineHrv = if (hrvPoints.isNotEmpty()) hrvPoints.map { it.value }.average() else null

            val sleepDebtResult = try {
                safeSleepDebtCalculator.calculate(
                    sleepDurationsHours = dailySleepHours,
                    targetSleepHours = 8.0,
                    currentHrvRmssd = currentHrv,
                    baselineHrvRmssd = baselineHrv
                )
            } catch (e: Exception) {
                null
            }

            // 5. Circadian 24-Hour Diurnal Decomposition
            val circadianResult = try {
                val observations = hrPoints.map { HeartRateObservation(it.timestamp, it.value) }
                safeCircadianDecompositionService.decompose(observations)
            } catch (e: Exception) {
                null
            }

            // 6. Longitudinal Lifestyle K-Means Clustering
            val allDates = (dailySleepMap.keys + stepsPoints.map { it.timestamp.atZone(zone).toLocalDate() }).distinct()
            val lifestyleClusterResult = if (allDates.size >= 3) {
                try {
                    val stepsByDate = stepsPoints.groupBy { it.timestamp.atZone(zone).toLocalDate() }
                    val hrvByDate = hrvPoints.groupBy { it.timestamp.atZone(zone).toLocalDate() }
                    val calByDate = calPoints.groupBy { it.timestamp.atZone(zone).toLocalDate() }

                    val vectors = allDates.sorted().map { date ->
                        DailyFeatureVector(
                            date = date,
                            steps = stepsByDate[date]?.sumOf { it.value } ?: 0.0,
                            sleepHours = dailySleepMap[date]?.sumOf { it.value } ?: 0.0,
                            hrvRmssd = hrvByDate[date]?.map { it.value }?.average() ?: 0.0,
                            activeCalories = calByDate[date]?.sumOf { it.value } ?: 0.0,
                            lateDinnerFlag = false
                        )
                    }
                    safeLifestyleKMeansClusterer.cluster(vectors, k = 3)
                } catch (e: Exception) {
                    null
                }
            } else null

            // 7. Dynamic Cross-Correlations derived strictly from Health Connect records
            val correlationsList = mutableListOf<CorrelationCardData>()
            var scatterDataResult: ScatterPlotData? = null

            val sleepCard = cards.find { it.metricType == VitalMetricType.SLEEP }
            val calCard = cards.find { it.metricType == VitalMetricType.CALORIES_DISTANCE }
            val sleepDuration = sleepCard?.primaryValue ?: profile.sleepScore
            val calVal = calCard?.primaryValue ?: "--"

            if (sleepDuration != "--" && sleepDuration != "--%" && calVal != "--" && calVal != "0" && calVal != "0 kcal") {
                correlationsList.add(
                    CorrelationCardData(
                        title = "Sleep & Glycemic Energy Expenditure",
                        category = "CIRCADIAN METABOLISM",
                        primaryInsight = "Overnight cellular repair window supports insulin sensitivity and glycogen restoration during active daytime exertion.",
                        metric1Label = "Sleep Window",
                        metric1Value = "$sleepDuration",
                        metric2Label = "Active Burn",
                        metric2Value = "$calVal kcal",
                        couplingScore = "Strong Sync",
                        statusTag = "METABOLICALLY STABLE"
                    )
                )
            }

            // Pairwise correlation between Sleep and Steps
            val sleepSeries = sleepPoints.groupBy { it.timestamp.atZone(zone).toLocalDate() }
                .map { (date, pts) -> DailyMetricSample(date, pts.sumOf { it.value }) }
            val stepsSeries = stepsPoints.groupBy { it.timestamp.atZone(zone).toLocalDate() }
                .map { (date, pts) -> DailyMetricSample(date, pts.sumOf { it.value }) }

            if (sleepSeries.size >= 3 && stepsSeries.size >= 3) {
                val corr = safeMetricPairwiseCorrelator.correlate(
                    metric1Name = "Sleep Duration",
                    series1 = sleepSeries,
                    metric2Name = "Daily Steps",
                    series2 = stepsSeries
                )
                val pairedMap = stepsSeries.associate { it.date to it.value }
                val pairedPoints = sleepSeries.mapNotNull { s ->
                    pairedMap[s.date]?.let { stepVal -> Pair(s.value, stepVal) }
                }

                if (pairedPoints.size >= 3) {
                    scatterDataResult = ScatterPlotData(
                        points = pairedPoints,
                        xLabel = "Sleep Duration (hrs)",
                        yLabel = "Daily Steps",
                        pearsonR = corr.pearsonR,
                        spearmanRho = corr.spearmanRho
                    )
                }

                correlationsList.add(
                    CorrelationCardData(
                        title = "Sleep Duration vs Daily Steps",
                        category = "ACTIVITY & RESTORATION",
                        primaryInsight = corr.narrative,
                        metric1Label = "Sleep (hrs)",
                        metric1Value = "%.1f".format(Locale.US, sleepSeries.map { it.value }.average()),
                        metric2Label = "Steps",
                        metric2Value = "%.0f".format(Locale.US, stepsSeries.map { it.value }.average()),
                        couplingScore = corr.strength.label,
                        statusTag = if (corr.isStatisticallySignificant) "SIGNIFICANT" else "OBSERVED"
                    )
                )
            }

            // Initial arbitration if report completed
            val existingReport = when (val s = _uiState.value.reportState) {
                is AiReportState.Complete -> s.text
                is AiReportState.Streaming -> s.text
                else -> ""
            }

            val arbitrated = if (existingReport.isNotBlank()) {
                safeInsightArbitrator.arbitrate(clinicalEvaluations, activeAnomalies, existingReport)
            } else if (clinicalEvaluations.any { it.status == io.biotrace.app.domain.clinical.rules.ClinicalStatus.CRITICAL || it.status == io.biotrace.app.domain.clinical.rules.ClinicalStatus.ELEVATED }) {
                safeInsightArbitrator.arbitrate(clinicalEvaluations, activeAnomalies, "")
            } else null

            _uiState.update {
                it.copy(
                    profile = profile,
                    correlations = correlationsList,
                    labBiomarkers = labs,
                    biologicalAge = bioAge,
                    atherogenicRisk = atherogenicResult,
                    metabolicRisk = metabolicResult,
                    sleepDebt = sleepDebtResult,
                    circadianDecomposition = circadianResult,
                    lifestyleClusterResult = lifestyleClusterResult,
                    activeAnomalies = activeAnomalies,
                    clinicalEvaluations = clinicalEvaluations,
                    arbitratedInsight = arbitrated,
                    scatterData = scatterDataResult,
                    isLoading = false
                )
            }
        }
    }

    fun toggleAgeDialog(open: Boolean) {
        _uiState.update { it.copy(isAgeDialogOpen = open) }
    }

    fun updateChronologicalAge(age: Int) {
        viewModelScope.launch {
            userProfileStore.setChronologicalAge(age)
            val bioAge = try {
                getBiologicalAgeUseCase()
            } catch (e: Exception) {
                null
            }
            _uiState.update {
                it.copy(
                    biologicalAge = bioAge,
                    isAgeDialogOpen = false
                )
            }
        }
    }
}
