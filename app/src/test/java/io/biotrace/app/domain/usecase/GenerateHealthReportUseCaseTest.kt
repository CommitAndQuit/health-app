package io.biotrace.app.domain.usecase

import app.cash.turbine.test
import io.biotrace.app.data.local.dao.LabBiomarkerDao
import io.biotrace.app.domain.llm.LlmApiException
import io.biotrace.app.domain.llm.LlmClient
import io.biotrace.app.domain.model.ProfileTriptychSummary
import io.biotrace.app.domain.repository.VitalsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GenerateHealthReportUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()

    private val vitalsRepository: VitalsRepository = mock()
    private val labBiomarkerDao: LabBiomarkerDao = mock()
    private val llmClient: LlmClient = mock()

    private lateinit var useCase: GenerateHealthReportUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        useCase = GenerateHealthReportUseCase(vitalsRepository, labBiomarkerDao, llmClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `emits Error when LLM not configured`() = runTest {
        whenever(llmClient.isConfigured()).thenReturn(false)

        useCase.execute().test {
            val item = awaitItem()
            assertTrue(item is ReportChunk.Error)
            assertEquals("API key not configured", (item as ReportChunk.Error).message)
            awaitComplete()
        }
    }

    @Test
    fun `emits Started then Text chunks then Done on success`() = runTest {
        whenever(llmClient.isConfigured()).thenReturn(true)
        whenever(vitalsRepository.getProfileTriptychSummary()).thenReturn(ProfileTriptychSummary())
        whenever(vitalsRepository.getVitalsDashboardSnapshot()).thenReturn(emptyList())
        whenever(labBiomarkerDao.getAll()).thenReturn(emptyList())
        whenever(llmClient.generateContentStream(any())).thenReturn(flowOf("Hello ", "world"))

        useCase.execute().test {
            assertEquals(ReportChunk.Started, awaitItem())
            assertEquals(ReportChunk.Text("Hello "), awaitItem())
            assertEquals(ReportChunk.Text("world"), awaitItem())
            assertEquals(ReportChunk.Done, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `emits Error on LlmApiException from stream`() = runTest {
        whenever(llmClient.isConfigured()).thenReturn(true)
        whenever(vitalsRepository.getProfileTriptychSummary()).thenReturn(ProfileTriptychSummary())
        whenever(vitalsRepository.getVitalsDashboardSnapshot()).thenReturn(emptyList())
        whenever(labBiomarkerDao.getAll()).thenReturn(emptyList())
        whenever(llmClient.generateContentStream(any())).thenReturn(
            flow { throw LlmApiException("API error") }
        )

        useCase.execute().test {
            assertEquals(ReportChunk.Started, awaitItem())
            val errorItem = awaitItem()
            assertTrue(errorItem is ReportChunk.Error)
            assertEquals("API error", (errorItem as ReportChunk.Error).message)
            awaitComplete()
        }
    }
}
