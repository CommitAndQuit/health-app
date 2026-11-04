package io.biotrace.app.ui.labs

import io.biotrace.app.data.local.dao.LabBiomarkerDao
import io.biotrace.app.data.local.entity.LabBiomarkerEntity
import io.biotrace.app.domain.clinical.ClinicalSchemaNormalizer
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
    private val normalizer = ClinicalSchemaNormalizer()

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

        whenever(labBiomarkerDao.getAll()).thenReturn(listOf(normalBiomarker, highBiomarker))
        whenever(labBiomarkerDao.getAllFlow()).thenReturn(flowOf(listOf(normalBiomarker, highBiomarker)))

        val viewModel = LabsViewModel(labBiomarkerDao, llmClient, normalizer)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.biomarkers.size)
        assertEquals(1, state.anomalies.size)
        assertEquals("HBA1C", state.anomalies[0].biomarkerName)
    }

    @Test
    fun `addManualBiomarker inserts into dao with calculated flag`() = runTest {
        whenever(labBiomarkerDao.getAll()).thenReturn(emptyList())
        whenever(labBiomarkerDao.getAllFlow()).thenReturn(flowOf(emptyList()))

        val viewModel = LabsViewModel(labBiomarkerDao, llmClient, normalizer)
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

    @Test
    fun `scanBloodReport parses and opens human-in-the-loop review dialog`() = runTest {
        whenever(labBiomarkerDao.getAll()).thenReturn(emptyList())
        whenever(labBiomarkerDao.getAllFlow()).thenReturn(flowOf(emptyList()))

        val jsonResponse = """
            [
              {
                "rawName": "Glycated Hemoglobin HbA1c",
                "value": "5.5",
                "unit": "%",
                "referenceMin": 4.0,
                "referenceMax": 5.6
              },
              {
                "rawName": "Fast Glucose",
                "value": "5.5",
                "unit": "mmol/L",
                "referenceMin": 3.9,
                "referenceMax": 5.5
              }
            ]
        """.trimIndent()

        whenever(llmClient.generateContentWithImage(any(), any(), any())).thenReturn(jsonResponse)

        val viewModel = LabsViewModel(labBiomarkerDao, llmClient, normalizer)
        advanceUntilIdle()

        viewModel.scanBloodReport("dummy_base64")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isReviewDialogOpen)
        assertEquals(2, state.pendingReviewBiomarkers.size)
        // Verify canonical name mapping
        assertEquals("HBA1C", state.pendingReviewBiomarkers[0].biomarkerName)
        assertEquals("FASTING_GLUCOSE", state.pendingReviewBiomarkers[1].biomarkerName)
        // Verify unit conversion from mmol/L to mg/dL (5.5 * 18.0182 ~ 99.1)
        assertEquals("mg/dL", state.pendingReviewBiomarkers[1].unit)
        assertTrue(state.pendingReviewBiomarkers[1].value > 90.0)
    }
}
