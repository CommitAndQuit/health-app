package io.biotrace.app.ui.insights

import io.biotrace.app.data.local.dao.LabBiomarkerDao
import io.biotrace.app.data.local.store.UserProfileStore
import io.biotrace.app.domain.llm.LlmClient
import io.biotrace.app.domain.longevity.BiologicalAgeResult
import io.biotrace.app.domain.longevity.LongevityTier
import io.biotrace.app.domain.model.ActionableFocus
import io.biotrace.app.domain.model.DailyBiometricInsightReport
import io.biotrace.app.domain.model.InsightDomainTag
import io.biotrace.app.domain.model.ProfileTriptychSummary
import io.biotrace.app.domain.model.TrendDirection
import io.biotrace.app.domain.model.TrendHighlight
import io.biotrace.app.domain.repository.VitalsRepository
import io.biotrace.app.domain.usecase.GenerateHealthReportUseCase
import io.biotrace.app.domain.usecase.GetBiologicalAgeUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class BiometricInsightsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val vitalsRepository: VitalsRepository = mock()
    private val labBiomarkerDao: LabBiomarkerDao = mock()
    private val generateHealthReportUseCase: GenerateHealthReportUseCase = mock()
    private val llmClient: LlmClient = mock()
    private val getBiologicalAgeUseCase: GetBiologicalAgeUseCase = mock()
    private val userProfileStore: UserProfileStore = mock()

    private val defaultBiologicalAgeResult = BiologicalAgeResult(
        chronologicalAge = 35,
        biologicalAge = 33.0,
        ageDelta = -2.0,
        agingPace = 0.94,
        longevityTier = LongevityTier.OPTIMAL,
        confidencePercentage = 80,
        drivers = emptyList(),
        primaryRecommendation = "Keep it up"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun stubDefaultDependencies() {
        whenever(vitalsRepository.getProfileTriptychSummary()).thenReturn(ProfileTriptychSummary())
        whenever(vitalsRepository.getVitalsDashboardSnapshot()).thenReturn(emptyList())
        whenever(labBiomarkerDao.getAll()).thenReturn(emptyList())
        whenever(userProfileStore.getChronologicalAge()).thenReturn(35)
        whenever(getBiologicalAgeUseCase.invoke()).thenReturn(defaultBiologicalAgeResult)
    }

    private fun createViewModel(): BiometricInsightsViewModel =
        BiometricInsightsViewModel(
            vitalsRepository,
            labBiomarkerDao,
            generateHealthReportUseCase,
            llmClient,
            getBiologicalAgeUseCase,
            userProfileStore
        )

    private val dummyReport = DailyBiometricInsightReport(
        headlineSummary = "Biometric summary normal.",
        clinicalArbitrationAlert = null,
        trendHighlights = listOf(
            TrendHighlight(
                title = "Resting Heart Rate",
                domainTags = listOf(InsightDomainTag.WEARABLE),
                direction = TrendDirection.STABLE,
                insightText = "RHR is steady.",
                supportingRecordIds = listOf("hr_1")
            )
        ),
        actionableFocus = ActionableFocus(
            recommendation = "Maintain regular sleep schedule.",
            rationaleMetric = "Circadian alignment."
        )
    )

    // -------------------------------------------------------------------------
    // generateReport tests
    // -------------------------------------------------------------------------

    @Test
    fun `generateReport transitions through Loading and Complete checking structuredReport`() = runTest {
        stubDefaultDependencies()
        whenever(llmClient.isConfigured()).thenReturn(false)
        var stateDuringExecute: AiReportState? = null
        val viewModel = createViewModel()
        advanceUntilIdle()

        whenever(generateHealthReportUseCase.execute()).thenAnswer {
            stateDuringExecute = viewModel.uiState.value.reportState
            dummyReport
        }

        viewModel.generateReport()
        advanceUntilIdle()

        assertEquals(AiReportState.Loading, stateDuringExecute)
        val reportState = viewModel.uiState.value.reportState
        assertTrue(reportState is AiReportState.Complete)
        assertEquals(dummyReport.toFormattedMarkdown(), (reportState as AiReportState.Complete).text)
        assertEquals(dummyReport, viewModel.uiState.value.structuredReport)
    }

    @Test
    fun `generateReport sets Error state on exception`() = runTest {
        stubDefaultDependencies()
        whenever(llmClient.isConfigured()).thenReturn(false)
        whenever(generateHealthReportUseCase.execute()).thenAnswer {
            throw RuntimeException("API error")
        }

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.generateReport()
        advanceUntilIdle()

        val reportState = viewModel.uiState.value.reportState
        assertTrue(reportState is AiReportState.Error)
        assertEquals("API error", (reportState as AiReportState.Error).message)
    }

    @Test
    fun `checkApiKeyAndMaybeGenerate sets isApiKeyConfigured false when no key`() = runTest {
        stubDefaultDependencies()
        whenever(llmClient.isConfigured()).thenReturn(false)
        whenever(generateHealthReportUseCase.execute()).thenReturn(dummyReport)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isApiKeyConfigured)
        assertTrue(viewModel.uiState.value.reportState is AiReportState.Idle)
    }

    @Test
    fun `toggleAgeDialog updates isAgeDialogOpen state`() = runTest {
        stubDefaultDependencies()
        whenever(llmClient.isConfigured()).thenReturn(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAgeDialogOpen)

        viewModel.toggleAgeDialog(true)
        assertTrue(viewModel.uiState.value.isAgeDialogOpen)

        viewModel.toggleAgeDialog(false)
        assertFalse(viewModel.uiState.value.isAgeDialogOpen)
    }

    @Test
    fun `updateChronologicalAge updates store and recomputes biological age`() = runTest {
        stubDefaultDependencies()
        whenever(llmClient.isConfigured()).thenReturn(false)

        val updatedResult = defaultBiologicalAgeResult.copy(chronologicalAge = 40)
        whenever(getBiologicalAgeUseCase.invoke()).thenReturn(defaultBiologicalAgeResult, updatedResult)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(35, viewModel.uiState.value.biologicalAge?.chronologicalAge)

        viewModel.updateChronologicalAge(40)
        advanceUntilIdle()

        verify(userProfileStore).setChronologicalAge(40)
        assertEquals(40, viewModel.uiState.value.biologicalAge?.chronologicalAge)
    }

    @Test
    fun `selectTab updates selectedTab state`() = runTest {
        stubDefaultDependencies()
        whenever(llmClient.isConfigured()).thenReturn(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(InsightsTab.OVERVIEW, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(InsightsTab.CLINICAL_RISKS)
        assertEquals(InsightsTab.CLINICAL_RISKS, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(InsightsTab.CORRELATIONS)
        assertEquals(InsightsTab.CORRELATIONS, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(InsightsTab.AI_DEEP_DIVE)
        assertEquals(InsightsTab.AI_DEEP_DIVE, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `loadInsights evaluates atherogenic risk and metabolic risk from lab biomarkers`() = runTest {
        stubDefaultDependencies()
        whenever(llmClient.isConfigured()).thenReturn(false)

        val now = java.time.Instant.now()
        val labs = listOf(
            io.biotrace.app.data.local.entity.LabBiomarkerEntity(
                id = "chol_1",
                rawName = "Total Cholesterol",
                biomarkerName = "CHOLESTEROL",
                value = 220.0,
                unit = "mg/dL",
                referenceMin = 120.0,
                referenceMax = 200.0,
                flag = "HIGH",
                recordedAt = now,
                source = "MANUAL"
            ),
            io.biotrace.app.data.local.entity.LabBiomarkerEntity(
                id = "hdl_1",
                rawName = "HDL",
                biomarkerName = "HDL",
                value = 40.0,
                unit = "mg/dL",
                referenceMin = 40.0,
                referenceMax = 60.0,
                flag = "NORMAL",
                recordedAt = now,
                source = "MANUAL"
            ),
            io.biotrace.app.data.local.entity.LabBiomarkerEntity(
                id = "tg_1",
                rawName = "Triglycerides",
                biomarkerName = "TRIGLYCERIDES",
                value = 160.0,
                unit = "mg/dL",
                referenceMin = 0.0,
                referenceMax = 150.0,
                flag = "HIGH",
                recordedAt = now,
                source = "MANUAL"
            ),
            io.biotrace.app.data.local.entity.LabBiomarkerEntity(
                id = "glu_1",
                rawName = "Fasting Glucose",
                biomarkerName = "FASTING_GLUCOSE",
                value = 110.0,
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

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        org.junit.Assert.assertNotNull(state.atherogenicRisk)
        assertEquals(4.0, state.atherogenicRisk?.tgHdlRatio ?: 0.0, 0.01)
        org.junit.Assert.assertNotNull(state.metabolicRisk)
        assertTrue(state.metabolicRisk?.criteriaMetCount ?: 0 >= 1)
        assertTrue(state.clinicalEvaluations.isNotEmpty())
    }

    @Test
    fun `generateReport arbitrates with clinical safety notice when elevated biomarker exists`() = runTest {
        stubDefaultDependencies()
        whenever(llmClient.isConfigured()).thenReturn(false)

        val now = java.time.Instant.now()
        val criticalLab = listOf(
            io.biotrace.app.data.local.entity.LabBiomarkerEntity(
                id = "glu_crit",
                rawName = "Fasting Glucose",
                biomarkerName = "FASTING_GLUCOSE",
                value = 140.0,
                unit = "mg/dL",
                referenceMin = 70.0,
                referenceMax = 99.0,
                flag = "HIGH",
                recordedAt = now,
                source = "MANUAL"
            )
        )
        whenever(labBiomarkerDao.getAll()).thenReturn(criticalLab)
        val reportWithAlert = dummyReport.copy(
            clinicalArbitrationAlert = "Blood glucose elevated",
            headlineSummary = "General wellness plan: stay active."
        )
        whenever(generateHealthReportUseCase.execute()).thenReturn(reportWithAlert)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.generateReport()
        advanceUntilIdle()

        val arbitrated = viewModel.uiState.value.arbitratedInsight
        org.junit.Assert.assertNotNull(arbitrated)
        assertTrue(arbitrated?.hasClinicalSafetyNotice == true)
        assertTrue(arbitrated?.finalContent?.contains("CLINICAL SAFETY NOTICE") == true)
        assertTrue(arbitrated?.finalContent?.contains("General wellness plan") == true)
    }
}
