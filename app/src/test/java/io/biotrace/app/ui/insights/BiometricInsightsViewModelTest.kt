package io.biotrace.app.ui.insights

import io.biotrace.app.data.local.dao.LabBiomarkerDao
import io.biotrace.app.data.local.store.UserProfileStore
import io.biotrace.app.domain.llm.LlmClient
import io.biotrace.app.domain.longevity.BiologicalAgeResult
import io.biotrace.app.domain.longevity.LongevityTier
import io.biotrace.app.domain.model.ProfileTriptychSummary
import io.biotrace.app.domain.repository.VitalsRepository
import io.biotrace.app.domain.usecase.GenerateHealthReportUseCase
import io.biotrace.app.domain.usecase.GetBiologicalAgeUseCase
import io.biotrace.app.domain.usecase.ReportChunk
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

    // -------------------------------------------------------------------------
    // generateReport tests
    // -------------------------------------------------------------------------

    @Test
    fun `generateReport transitions Loading Streaming Complete`() = runTest {
        stubDefaultDependencies()
        whenever(llmClient.isConfigured()).thenReturn(false)
        whenever(generateHealthReportUseCase.execute()).thenReturn(
            flowOf(
                ReportChunk.Started,
                ReportChunk.Text("Hello"),
                ReportChunk.Done
            )
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.generateReport()
        advanceUntilIdle()

        val reportState = viewModel.uiState.value.reportState
        assertTrue(reportState is AiReportState.Complete)
        assertEquals("Hello", (reportState as AiReportState.Complete).text)
    }

    @Test
    fun `generateReport sets Error state on LlmApiException`() = runTest {
        stubDefaultDependencies()
        whenever(llmClient.isConfigured()).thenReturn(false)
        whenever(generateHealthReportUseCase.execute()).thenReturn(
            flowOf(ReportChunk.Error("API error"))
        )

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
        // ensure execute() is never triggered (isConfigured=false skips auto-generation)
        whenever(generateHealthReportUseCase.execute()).thenReturn(flowOf())

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
}
