package io.biotrace.app.domain.usecase

import app.cash.turbine.test
import io.biotrace.app.data.local.dao.LabBiomarkerDao
import io.biotrace.app.data.local.dao.MealDao
import io.biotrace.app.data.local.entity.LabBiomarkerEntity
import io.biotrace.app.data.local.entity.MealEntity
import io.biotrace.app.domain.analytics.clustering.LifestyleKMeansClusterer
import io.biotrace.app.domain.anomaly.AnomalyDetectionEngine
import io.biotrace.app.domain.clinical.ClinicalSchemaNormalizer
import io.biotrace.app.domain.clinical.risk.SleepDebtCalculator
import io.biotrace.app.domain.clinical.rules.ClinicalSafetyRuleEngine
import io.biotrace.app.domain.insights.arbitration.InsightArbitrator
import io.biotrace.app.domain.llm.LlmApiException
import io.biotrace.app.domain.llm.LlmClient
import io.biotrace.app.domain.model.ActionableFocus
import io.biotrace.app.domain.model.DailyBiometricInsightReport
import io.biotrace.app.domain.model.InsightDomainTag
import io.biotrace.app.domain.model.TrendDirection
import io.biotrace.app.domain.model.TrendHighlight
import io.biotrace.app.domain.model.VitalPoint
import io.biotrace.app.domain.repository.VitalsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class GenerateHealthReportUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()

    private val vitalsRepository: VitalsRepository = mock()
    private val labBiomarkerDao: LabBiomarkerDao = mock()
    private val mealDao: MealDao = mock()
    private val llmClient: LlmClient = mock()

    private lateinit var useCase: GenerateHealthReportUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val safetyEngine = ClinicalSafetyRuleEngine()
        val anomalyEngine = AnomalyDetectionEngine()
        val normalizer = ClinicalSchemaNormalizer()
        val arbitrator = InsightArbitrator(safetyEngine, anomalyEngine)
        val sleepDebt = SleepDebtCalculator()
        val clusterer = LifestyleKMeansClusterer()

        useCase = GenerateHealthReportUseCase(
            vitalsRepository = vitalsRepository,
            labBiomarkerDao = labBiomarkerDao,
            mealDao = mealDao,
            llmClient = llmClient,
            clinicalSchemaNormalizer = normalizer,
            clinicalSafetyRuleEngine = safetyEngine,
            anomalyDetectionEngine = anomalyEngine,
            insightArbitrator = arbitrator,
            sleepDebtCalculator = sleepDebt,
            lifestyleKMeansClusterer = clusterer
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun stubDefaultData() {
        whenever(vitalsRepository.getVitalsBetween(any(), any(), any())).thenReturn(emptyList())
        whenever(mealDao.getMealsBetween(any(), any())).thenReturn(emptyList())
        whenever(labBiomarkerDao.getAll()).thenReturn(emptyList())
    }

    @Test
    fun `execute constructs payload with all 3 data sources and correlations`() = runTest {
        whenever(llmClient.isConfigured()).thenReturn(true)

        val now = Instant.now()

        // 1. Telemetry vitals across 5 days (RHR, HRV, SLEEP, STEPS, CALORIES)
        val rhrPoints = (0..4).map { i ->
            VitalPoint(
                id = "hr_$i",
                metricType = "HEART_RATE",
                value = 65.0 + i,
                unit = "bpm",
                timestamp = now.minus(Duration.ofDays(i.toLong()))
            )
        }
        val hrvPoints = (0..4).map { i ->
            VitalPoint(
                id = "hrv_$i",
                metricType = "HRV_RMSSD",
                value = 60.0 - i,
                unit = "ms",
                timestamp = now.minus(Duration.ofDays(i.toLong()))
            )
        }
        val sleepPoints = (0..4).map { i ->
            VitalPoint(
                id = "sleep_$i",
                metricType = "SLEEP",
                value = 7.5 + (i * 0.1),
                unit = "hours",
                timestamp = now.minus(Duration.ofDays(i.toLong()))
            )
        }
        val stepPoints = (0..4).map { i ->
            VitalPoint(
                id = "step_$i",
                metricType = "STEPS",
                value = 8000.0 + (i * 500),
                unit = "count",
                timestamp = now.minus(Duration.ofDays(i.toLong()))
            )
        }
        val calPoints = (0..4).map { i ->
            VitalPoint(
                id = "cal_$i",
                metricType = "CALORIES",
                value = 2200.0 + (i * 50),
                unit = "kcal",
                timestamp = now.minus(Duration.ofDays(i.toLong()))
            )
        }

        whenever(vitalsRepository.getVitalsBetween(eq("HEART_RATE"), any(), any())).thenReturn(rhrPoints)
        whenever(vitalsRepository.getVitalsBetween(eq("HRV_RMSSD"), any(), any())).thenReturn(hrvPoints)
        whenever(vitalsRepository.getVitalsBetween(eq("SLEEP"), any(), any())).thenReturn(sleepPoints)
        whenever(vitalsRepository.getVitalsBetween(eq("STEPS"), any(), any())).thenReturn(stepPoints)
        whenever(vitalsRepository.getVitalsBetween(eq("CALORIES"), any(), any())).thenReturn(calPoints)
        whenever(vitalsRepository.getVitalsBetween(eq("OXYGEN_SATURATION"), any(), any())).thenReturn(emptyList())
        whenever(vitalsRepository.getVitalsBetween(eq("WEIGHT"), any(), any())).thenReturn(emptyList())

        // 2. Nutrition meals across 7 days
        val meals = (0..4).map { i ->
            MealEntity(
                id = "meal_$i",
                name = "Macro Plate $i",
                calories = 650,
                proteinGrams = 42.0,
                carbsGrams = 55.0,
                fatsGrams = 22.0,
                fiberGrams = 9.0,
                consumedAt = now.minus(Duration.ofDays(i.toLong())),
                photoPath = null
            )
        }
        whenever(mealDao.getMealsBetween(any(), any())).thenReturn(meals)

        // 3. Normalized Lab Biomarkers
        val labs = listOf(
            LabBiomarkerEntity(
                id = "lab_ferritin",
                rawName = "Serum Ferritin",
                biomarkerName = "FERRITIN",
                value = 95.0,
                unit = "ng/mL",
                referenceMin = 30.0,
                referenceMax = 400.0,
                flag = "NORMAL",
                recordedAt = now.minus(Duration.ofDays(10)),
                source = "LAB"
            )
        )
        whenever(labBiomarkerDao.getAll()).thenReturn(labs)

        val jsonResponse = """
            {
              "headlineSummary": "All biometric streams aligned.",
              "clinicalArbitrationAlert": null,
              "trendHighlights": [],
              "actionableFocus": {
                "recommendation": "Keep up the routine.",
                "rationaleMetric": "Stable RHR."
              }
            }
        """.trimIndent()
        whenever(llmClient.generateContent(any(), any(), any(), any(), any())).thenReturn(jsonResponse)

        val report = useCase.execute()
        assertNotNull(report)

        val promptCaptor = argumentCaptor<String>()
        verify(llmClient).generateContent(
            prompt = promptCaptor.capture(),
            systemInstructionOverride = any(),
            responseSchema = any(),
            responseMimeType = eq("application/json"),
            temperature = any()
        )

        val payload = promptCaptor.firstValue
        // Verify Vitals telemetry with EWMA and Z-score
        assertTrue(payload.contains("7D_EWMA"))
        assertTrue(payload.contains("Z-Score"))
        assertTrue(payload.contains("Resting Heart Rate"))
        // Verify Nutrition meals with 7D macro averages
        assertTrue(payload.contains("7D Rolling Daily Averages"))
        assertTrue(payload.contains("Protein:"))
        assertTrue(payload.contains("Carbs:"))
        assertTrue(payload.contains("Fat:"))
        // Verify Normalized Labs
        assertTrue(payload.contains("FERRITIN") || payload.contains("Ferritin"))
        // Verify Correlations
        assertTrue(payload.contains("PRE-COMPUTED MULTI-METRIC CORRELATIONS"))
    }

    @Test
    fun `execute deserializes JSON into DailyBiometricInsightReport`() = runTest {
        whenever(llmClient.isConfigured()).thenReturn(true)
        stubDefaultData()

        val jsonResponse = """
            {
              "headlineSummary": "Cardiovascular stability maintained with positive recovery trends.",
              "clinicalArbitrationAlert": null,
              "trendHighlights": [
                {
                  "title": "Resting Heart Rate Recovery",
                  "domainTags": ["WEARABLE", "CIRCADIAN"],
                  "direction": "IMPROVING",
                  "insightText": "Resting heart rate decreased by 3 bpm relative to 7D EWMA.",
                  "supportingRecordIds": ["HeartRateRecord:hr_1"]
                }
              ],
              "actionableFocus": {
                "recommendation": "Maintain morning zone 2 cardio.",
                "rationaleMetric": "RHR Z-score at -1.2 indicates positive adaptation."
              }
            }
        """.trimIndent()
        whenever(llmClient.generateContent(any(), any(), any(), any(), any())).thenReturn(jsonResponse)

        val report = useCase.execute()

        assertEquals("Cardiovascular stability maintained with positive recovery trends.", report.headlineSummary)
        assertEquals(1, report.trendHighlights.size)
        val highlight = report.trendHighlights[0]
        assertEquals("Resting Heart Rate Recovery", highlight.title)
        assertEquals(listOf(InsightDomainTag.WEARABLE, InsightDomainTag.CIRCADIAN), highlight.domainTags)
        assertEquals(TrendDirection.IMPROVING, highlight.direction)
        assertEquals("Resting heart rate decreased by 3 bpm relative to 7D EWMA.", highlight.insightText)
        assertEquals(listOf("HeartRateRecord:hr_1"), highlight.supportingRecordIds)
        assertEquals("Maintain morning zone 2 cardio.", report.actionableFocus.recommendation)
        assertEquals("RHR Z-score at -1.2 indicates positive adaptation.", report.actionableFocus.rationaleMetric)
    }

    @Test
    fun `execute injects deterministic clinicalArbitrationAlert when LLM omits it`() = runTest {
        whenever(llmClient.isConfigured()).thenReturn(true)
        stubDefaultData()

        val now = Instant.now()
        // Provide an extreme resting heart rate point (135 bpm triggers Tachycardia CRITICAL)
        val criticalHr = listOf(
            VitalPoint(
                id = "hr_crit",
                metricType = "HEART_RATE",
                value = 135.0,
                unit = "bpm",
                timestamp = now
            )
        )
        whenever(vitalsRepository.getVitalsBetween(eq("HEART_RATE"), any(), any())).thenReturn(criticalHr)

        val jsonResponse = """
            {
              "headlineSummary": "Elevated heart rate noticed during active day.",
              "clinicalArbitrationAlert": null,
              "trendHighlights": [],
              "actionableFocus": {
                "recommendation": "Rest and hydrate.",
                "rationaleMetric": "Heart rate observation."
              }
            }
        """.trimIndent()
        whenever(llmClient.generateContent(any(), any(), any(), any(), any())).thenReturn(jsonResponse)

        val report = useCase.execute()

        assertNotNull(report.clinicalArbitrationAlert)
        assertTrue(
            report.clinicalArbitrationAlert!!.contains("TACHYCARDIA") ||
            report.clinicalArbitrationAlert!!.contains("CLINICAL SAFETY NOTICE") ||
            report.clinicalArbitrationAlert!!.contains("HEART_RATE")
        )
    }

    @Test
    fun `fallback behavior when llmClient is not configured`() = runTest {
        whenever(llmClient.isConfigured()).thenReturn(false)
        stubDefaultData()

        val report = useCase.execute()

        assertNotNull(report)
        assertTrue(report.headlineSummary.contains("API key not configured"))
    }

    @Test
    fun `fallback behavior when llmClient throws LlmApiException`() = runTest {
        whenever(llmClient.isConfigured()).thenReturn(true)
        stubDefaultData()
        whenever(llmClient.generateContent(any(), any(), any(), any(), any())).thenAnswer {
            throw LlmApiException("Gemini rate limit exceeded")
        }

        val report = useCase.execute()

        assertNotNull(report)
        assertTrue(report.headlineSummary.contains("Gemini rate limit exceeded"))
    }

    @Test
    fun `executeStream emits Started then Text then Done`() = runTest {
        whenever(llmClient.isConfigured()).thenReturn(true)
        stubDefaultData()

        val jsonResponse = """
            {
              "headlineSummary": "Cardiovascular stability maintained.",
              "clinicalArbitrationAlert": null,
              "trendHighlights": [],
              "actionableFocus": {
                "recommendation": "Rest well.",
                "rationaleMetric": "Stable vitals."
              }
            }
        """.trimIndent()
        whenever(llmClient.generateContent(any(), any(), any(), any(), any())).thenReturn(jsonResponse)

        useCase.executeStream().test {
            assertEquals(ReportChunk.Started, awaitItem())
            val textChunk = awaitItem()
            assertTrue(textChunk is ReportChunk.Text)
            assertTrue((textChunk as ReportChunk.Text).text.contains("Cardiovascular stability maintained."))
            assertEquals(ReportChunk.Done, awaitItem())
            awaitComplete()
        }
    }
}
