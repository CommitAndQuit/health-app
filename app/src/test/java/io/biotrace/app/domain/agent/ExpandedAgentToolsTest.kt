package io.biotrace.app.domain.agent

import io.biotrace.app.data.local.dao.LabBiomarkerDao
import io.biotrace.app.data.local.dao.MealDao
import io.biotrace.app.data.local.entity.LabBiomarkerEntity
import io.biotrace.app.data.local.entity.MealEntity
import io.biotrace.app.data.local.store.UserProfileStore
import io.biotrace.app.domain.agent.tools.GetActiveAnomaliesTool
import io.biotrace.app.domain.agent.tools.GetNutritionIntakeTool
import io.biotrace.app.domain.agent.tools.QueryCircadianDecompositionTool
import io.biotrace.app.domain.agent.tools.QueryClinicalRiskTool
import io.biotrace.app.domain.agent.tools.QueryCorrelationMatrixTool
import io.biotrace.app.domain.agent.tools.QueryLabBiomarkersTool
import io.biotrace.app.domain.agent.tools.QueryLifestyleClustersTool
import io.biotrace.app.domain.agent.tools.QueryVitalsTool
import io.biotrace.app.domain.analytics.clustering.LifestyleKMeansClusterer
import io.biotrace.app.domain.analytics.correlation.MetricPairwiseCorrelator
import io.biotrace.app.domain.analytics.timeseries.CircadianDecompositionService
import io.biotrace.app.domain.anomaly.AnomalyDetectionEngine
import io.biotrace.app.domain.clinical.risk.AtherogenicRiskCalculator
import io.biotrace.app.domain.clinical.risk.HarmonizedMetabolicRiskCalculator
import io.biotrace.app.domain.clinical.risk.SleepDebtCalculator
import io.biotrace.app.domain.llm.LlmClient
import io.biotrace.app.domain.model.VitalPoint
import io.biotrace.app.domain.repository.VitalsRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.temporal.ChronoUnit

class ExpandedAgentToolsTest {

    private val vitalsRepository: VitalsRepository = mock()
    private val labBiomarkerDao: LabBiomarkerDao = mock()
    private val mealDao: MealDao = mock()
    private val userProfileStore: UserProfileStore = mock()
    private val llmClient: LlmClient = mock()

    private val atherogenicRiskCalculator = AtherogenicRiskCalculator()
    private val harmonizedMetabolicRiskCalculator = HarmonizedMetabolicRiskCalculator()
    private val sleepDebtCalculator = SleepDebtCalculator()
    private val lifestyleKMeansClusterer = LifestyleKMeansClusterer()
    private val anomalyDetectionEngine = AnomalyDetectionEngine()

    private lateinit var correlationTool: QueryCorrelationMatrixTool
    private lateinit var clinicalRiskTool: QueryClinicalRiskTool
    private lateinit var circadianTool: QueryCircadianDecompositionTool
    private lateinit var lifestyleTool: QueryLifestyleClustersTool

    @Before
    fun setUp() {
        correlationTool = QueryCorrelationMatrixTool(vitalsRepository, MetricPairwiseCorrelator)
        clinicalRiskTool = QueryClinicalRiskTool(
            atherogenicRiskCalculator = atherogenicRiskCalculator,
            harmonizedMetabolicRiskCalculator = harmonizedMetabolicRiskCalculator,
            sleepDebtCalculator = sleepDebtCalculator,
            labBiomarkerDao = labBiomarkerDao,
            vitalsRepository = vitalsRepository,
            userProfileStore = userProfileStore
        )
        circadianTool = QueryCircadianDecompositionTool(vitalsRepository, CircadianDecompositionService)
        lifestyleTool = QueryLifestyleClustersTool(vitalsRepository, mealDao, lifestyleKMeansClusterer)
    }

    @Test
    fun `QueryCorrelationMatrixTool executes bivariate correlation with citations`() = runTest {
        val now = Instant.now()
        val s1 = listOf(
            VitalPoint("hr_1", now.minus(3, ChronoUnit.DAYS), 65.0, "bpm", "HEART_RATE"),
            VitalPoint("hr_2", now.minus(2, ChronoUnit.DAYS), 70.0, "bpm", "HEART_RATE"),
            VitalPoint("hr_3", now.minus(1, ChronoUnit.DAYS), 75.0, "bpm", "HEART_RATE")
        )
        val s2 = listOf(
            VitalPoint("hrv_1", now.minus(3, ChronoUnit.DAYS), 60.0, "ms", "HRV_RMSSD"),
            VitalPoint("hrv_2", now.minus(2, ChronoUnit.DAYS), 50.0, "ms", "HRV_RMSSD"),
            VitalPoint("hrv_3", now.minus(1, ChronoUnit.DAYS), 40.0, "ms", "HRV_RMSSD")
        )

        whenever(vitalsRepository.getVitalsBetween(eq("HEART_RATE"), any(), any())).thenReturn(s1)
        whenever(vitalsRepository.getVitalsBetween(eq("HRV_RMSSD"), any(), any())).thenReturn(s2)

        val result = correlationTool.execute(mapOf("metric1" to "HEART_RATE", "metric2" to "HRV_RMSSD"))

        assertTrue(result.success)
        assertTrue(result.data.contains("HEART_RATE"))
        assertTrue(result.data.contains("HRV_RMSSD"))
        assertTrue(result.citations.isNotEmpty())
    }

    @Test
    fun `QueryClinicalRiskTool evaluates atherogenic metabolic and sleep risk`() = runTest {
        val now = Instant.now()
        val labs = listOf(
            LabBiomarkerEntity(
                id = "lab_1",
                rawName = "Total Cholesterol",
                biomarkerName = "CHOLESTEROL",
                value = 210.0,
                unit = "mg/dL",
                referenceMin = 125.0,
                referenceMax = 200.0,
                flag = "HIGH",
                recordedAt = now,
                source = "MANUAL"
            ),
            LabBiomarkerEntity(
                id = "lab_2",
                rawName = "HDL",
                biomarkerName = "HDL",
                value = 45.0,
                unit = "mg/dL",
                referenceMin = 40.0,
                referenceMax = 60.0,
                flag = "NORMAL",
                recordedAt = now,
                source = "MANUAL"
            ),
            LabBiomarkerEntity(
                id = "lab_3",
                rawName = "Triglycerides",
                biomarkerName = "TRIGLYCERIDES",
                value = 180.0,
                unit = "mg/dL",
                referenceMin = 0.0,
                referenceMax = 150.0,
                flag = "HIGH",
                recordedAt = now,
                source = "MANUAL"
            ),
            LabBiomarkerEntity(
                id = "lab_4",
                rawName = "Fasting Glucose",
                biomarkerName = "FASTING_GLUCOSE",
                value = 105.0,
                unit = "mg/dL",
                referenceMin = 70.0,
                referenceMax = 99.0,
                flag = "HIGH",
                recordedAt = now,
                source = "MANUAL"
            )
        )

        whenever(labBiomarkerDao.getAll()).thenReturn(labs)
        whenever(userProfileStore.getBiologicalSex()).thenReturn("male")
        whenever(vitalsRepository.getVitalsBetween(any(), any(), any())).thenReturn(emptyList())

        val result = clinicalRiskTool.execute(emptyMap())

        assertTrue(result.success)
        assertTrue(result.data.contains("atherogenicRisk"))
        assertTrue(result.data.contains("tgHdlRatio"))
        assertTrue(result.data.contains("metabolicRisk"))
        assertTrue(result.data.contains("sleepDebt"))
        assertEquals(4, result.citations.size)
    }

    @Test
    fun `QueryCircadianDecompositionTool evaluates diurnal dipping pattern`() = runTest {
        val now = Instant.now()
        // Day time points (mean ~ 75)
        val dayPoints = (8..18).map { hour ->
            VitalPoint("hr_day_$hour", now.minus(hour.toLong(), ChronoUnit.HOURS), 75.0, "bpm", "HEART_RATE")
        }
        // Night time points (mean ~ 60, dipping ~ 20%)
        val nightPoints = (1..6).map { hour ->
            VitalPoint("hr_night_$hour", now.minus(hour.toLong(), ChronoUnit.HOURS), 60.0, "bpm", "HEART_RATE")
        }

        whenever(vitalsRepository.getVitalsBetween(eq("HEART_RATE"), any(), any())).thenReturn(dayPoints + nightPoints)

        val result = circadianTool.execute(emptyMap())

        assertTrue(result.success)
        assertTrue(result.data.contains("dipPercentage"))
        assertTrue(result.data.contains("pattern"))
        assertTrue(result.citations.isNotEmpty())
    }

    @Test
    fun `QueryLifestyleClustersTool clusters longitudinal vitals into archetypes`() = runTest {
        val now = Instant.now()
        val steps = (1..10).map { day ->
            VitalPoint("step_$day", now.minus(day.toLong(), ChronoUnit.DAYS), 9000.0, "steps", "STEPS")
        }
        val sleep = (1..10).map { day ->
            VitalPoint("sleep_$day", now.minus(day.toLong(), ChronoUnit.DAYS), 7.5, "hours", "SLEEP")
        }
        val hrv = (1..10).map { day ->
            VitalPoint("hrv_$day", now.minus(day.toLong(), ChronoUnit.DAYS), 55.0, "ms", "HRV_RMSSD")
        }
        val meals = (1..10).map { day ->
            MealEntity(
                id = "meal_$day",
                name = "Dinner",
                calories = 600,
                proteinGrams = 30.0,
                carbsGrams = 60.0,
                fatsGrams = 20.0,
                fiberGrams = 5.0,
                consumedAt = now.minus(day.toLong(), ChronoUnit.DAYS),
                photoPath = null
            )
        }

        whenever(vitalsRepository.getVitalsBetween(eq("STEPS"), any(), any())).thenReturn(steps)
        whenever(vitalsRepository.getVitalsBetween(eq("SLEEP"), any(), any())).thenReturn(sleep)
        whenever(vitalsRepository.getVitalsBetween(eq("HRV_RMSSD"), any(), any())).thenReturn(hrv)
        whenever(vitalsRepository.getVitalsBetween(eq("CALORIES"), any(), any())).thenReturn(emptyList())
        whenever(mealDao.getMealsBetween(any(), any())).thenReturn(meals)

        val result = lifestyleTool.execute(mapOf("k" to 2))

        assertTrue(result.success)
        assertTrue(result.data.contains("dominantArchetype"))
        assertTrue(result.data.contains("dominantDisplayName"))
        assertTrue(result.citations.isNotEmpty())
    }

    @Test
    fun `HealthAgent end-to-end handles new tools properly`() = runTest {
        val vitalsTool = QueryVitalsTool(vitalsRepository)
        val labTool = QueryLabBiomarkersTool(labBiomarkerDao)
        val anomaliesTool = GetActiveAnomaliesTool(labBiomarkerDao, anomalyDetectionEngine)
        val nutritionTool = GetNutritionIntakeTool(mealDao)

        val agent = HealthAgent(
            llmClient = llmClient,
            queryVitalsTool = vitalsTool,
            queryLabBiomarkersTool = labTool,
            getActiveAnomaliesTool = anomaliesTool,
            getNutritionIntakeTool = nutritionTool,
            queryCorrelationMatrixTool = correlationTool,
            queryClinicalRiskTool = clinicalRiskTool,
            queryCircadianDecompositionTool = circadianTool,
            queryLifestyleClustersTool = lifestyleTool
        )

        var step = 0
        whenever(llmClient.generateContent(any())).thenAnswer {
            step++
            if (step == 1) {
                "Action: query_circadian_decomposition\nAction Input: {}"
            } else {
                "Final Answer: Your circadian diurnal dipping pattern is Normal Dipper (14.2% dip)."
            }
        }

        val hrPoints = listOf(
            VitalPoint("hr_1", Instant.now(), 72.0, "bpm", "HEART_RATE")
        )
        whenever(vitalsRepository.getVitalsBetween(eq("HEART_RATE"), any(), any())).thenReturn(hrPoints)

        val responses = agent.processQuery("How is my circadian dip?", emptyList()).toList()

        assertEquals(1, responses.size)
        val resp = responses.first()
        assertTrue(resp.isComplete)
        assertEquals("query_circadian_decomposition", resp.toolCallsMade.first())
        assertTrue(resp.text.contains("Normal Dipper"))
    }
}
