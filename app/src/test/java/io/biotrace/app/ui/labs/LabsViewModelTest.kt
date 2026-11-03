package io.biotrace.app.ui.labs

import io.biotrace.app.data.local.dao.LabBiomarkerDao
import io.biotrace.app.data.local.entity.LabBiomarkerEntity
import io.biotrace.app.domain.llm.LlmClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class LabsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val labBiomarkerDao: LabBiomarkerDao = mock()
    private val llmClient: LlmClient = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadBiomarkers populates biomarkers and detects anomalies`() = runTest {
        val normalBiomarker = LabBiomarkerEntity(
            id = "1",
            biomarkerName = "FERRITIN",
            rawName = "Ferritin",
            value = 65.0,
            unit = "ng/mL",
            referenceMin = 30.0,
            referenceMax = 300.0,
            flag = "NORMAL",
            recordedAt = Instant.now(),
            source = "MANUAL"
        )
        val highBiomarker = LabBiomarkerEntity(
            id = "2",
            biomarkerName = "HBA1C",
            rawName = "Hemoglobin A1c",
            value = 6.8,
            unit = "%",
            referenceMin = 4.0,
            referenceMax = 5.6,
            flag = "HIGH",
            recordedAt = Instant.now(),
            source = "VISION_LLM"
        )

        whenever(labBiomarkerDao.getAllFlow()).thenReturn(flowOf(listOf(normalBiomarker, highBiomarker)))

        val viewModel = LabsViewModel(labBiomarkerDao, llmClient)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.biomarkers.size)
        assertEquals(1, state.anomalies.size)
        assertEquals("HBA1C", state.anomalies[0].biomarkerName)
    }

    @Test
    fun `addManualBiomarker inserts into dao with calculated flag`() = runTest {
        whenever(labBiomarkerDao.getAllFlow()).thenReturn(flowOf(emptyList()))

        val viewModel = LabsViewModel(labBiomarkerDao, llmClient)
        advanceUntilIdle()

        viewModel.addManualBiomarker(
            name = "Vitamin D",
            value = 18.0,
            unit = "ng/mL",
            min = 30.0,
            max = 100.0
        )
        advanceUntilIdle()

        verify(labBiomarkerDao).insert(any())
    }
}
