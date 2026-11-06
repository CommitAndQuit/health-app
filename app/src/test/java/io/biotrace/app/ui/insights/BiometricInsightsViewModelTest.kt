package io.biotrace.app.ui.insights

import io.biotrace.app.data.local.dao.LabBiomarkerDao
import io.biotrace.app.domain.llm.LlmClient
import io.biotrace.app.domain.model.ProfileTriptychSummary
import io.biotrace.app.domain.repository.VitalsRepository
import io.biotrace.app.domain.usecase.GenerateHealthReportUseCase
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
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class BiometricInsightsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val vitalsRepository: VitalsRepository = mock()
    private val labBiomarkerDao: LabBiomarkerDao = mock()
    private val generateHealthReportUseCase: GenerateHealthReportUseCase = mock()
    private val llmClient: LlmClient = mock()

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
    }

    private fun createViewModel(): BiometricInsightsViewModel =
        BiometricInsightsViewModel(
            vitalsRepository,
            labBiomarkerDao,
            generateHealthReportUseCase,
            llmClient
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
}
