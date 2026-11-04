package io.biotrace.app.ui.labs

import io.biotrace.app.data.local.dao.LabBiomarkerDao
import io.biotrace.app.data.local.entity.LabBiomarkerEntity
import io.biotrace.app.domain.clinical.ClinicalSchemaNormalizer
import io.biotrace.app.domain.clinical.PdfReportProcessor
import io.biotrace.app.domain.clinical.ProcessedDocument
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
    private val pdfReportProcessor: PdfReportProcessor = mock()

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

        val viewModel = LabsViewModel(labBiomarkerDao, llmClient, normalizer, pdfReportProcessor)
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

        val viewModel = LabsViewModel(labBiomarkerDao, llmClient, normalizer, pdfReportProcessor)
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
        whenever(llmClient.generateContentWithDocument(any(), any(), any())).thenReturn(jsonResponse)

        val viewModel = LabsViewModel(labBiomarkerDao, llmClient, normalizer, pdfReportProcessor)
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

    @Test
    fun `scanBloodReport handles JSON object envelope with diagnostic center metadata`() = runTest {
        whenever(labBiomarkerDao.getAll()).thenReturn(emptyList())
        whenever(labBiomarkerDao.getAllFlow()).thenReturn(flowOf(emptyList()))

        val jsonResponse = """
            {
              "labName": "Quest Diagnostics",
              "reportDate": "2026-10-21",
              "biomarkers": [
                {
                  "rawName": "Total Cholesterol",
                  "value": "210",
                  "unit": "mg/dL",
                  "referenceMin": 125,
                  "referenceMax": 200
                }
              ]
            }
        """.trimIndent()

        whenever(llmClient.generateContentWithDocument(any(), any(), any())).thenReturn(jsonResponse)

        val viewModel = LabsViewModel(labBiomarkerDao, llmClient, normalizer, pdfReportProcessor)
        advanceUntilIdle()

        viewModel.scanBloodReport("dummy_pdf_base64", "application/pdf", "quest_report.pdf")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isReviewDialogOpen)
        assertEquals(1, state.pendingReviewBiomarkers.size)
        assertEquals("CHOLESTEROL_TOTAL", state.pendingReviewBiomarkers[0].biomarkerName)
        assertEquals("HIGH", state.pendingReviewBiomarkers[0].flag)
        assertEquals("Quest Diagnostics • 2026-10-21", state.scannedReportMetadata)
        // Verify exact report date was parsed into recordedAt
        assertEquals("2026-10-21", io.biotrace.app.domain.clinical.ClinicalDateParser.toIsoDateString(state.pendingReviewBiomarkers[0].recordedAt))
    }

    @Test
    fun `longitudinal tracking groups reports across the year and calculates deltas`() = runTest {
        val date1 = java.time.LocalDate.of(2026, 6, 15).atTime(10, 0).toInstant(java.time.ZoneOffset.UTC)
        val date2 = java.time.LocalDate.of(2026, 10, 28).atTime(10, 0).toInstant(java.time.ZoneOffset.UTC)

        val vitdJune = LabBiomarkerEntity(
            id = "lab_VITAMIN_D_2026-06-15",
            biomarkerName = "VITAMIN_D",
            rawName = "25-OH Vitamin D",
            value = 28.5,
            unit = "ng/mL",
            referenceMin = 30.0,
            referenceMax = 100.0,
            flag = "LOW",
            recordedAt = date1,
            source = "Metropolis"
        )
        val vitdOct = LabBiomarkerEntity(
            id = "lab_VITAMIN_D_2026-10-28",
            biomarkerName = "VITAMIN_D",
            rawName = "Total 25-Hydroxy Vitamin D",
            value = 14.4,
            unit = "ng/mL",
            referenceMin = 30.0,
            referenceMax = 100.0,
            flag = "LOW",
            recordedAt = date2,
            source = "Healthi"
        )

        whenever(labBiomarkerDao.getAll()).thenReturn(listOf(vitdJune, vitdOct))
        whenever(labBiomarkerDao.getAllFlow()).thenReturn(flowOf(listOf(vitdOct, vitdJune)))

        val viewModel = LabsViewModel(labBiomarkerDao, llmClient, normalizer, pdfReportProcessor)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // Verify available report filters: "ALL" + 2 distinct report dates
        assertEquals(3, state.availableReports.size)
        assertEquals("ALL", state.availableReports[0].id)
        assertEquals("2026-10-28", state.availableReports[1].id)
        assertEquals("2026-06-15", state.availableReports[2].id)

        // Verify longitudinal cards
        assertEquals(1, state.longitudinalBiomarkers.size)
        val card = state.longitudinalBiomarkers[0]
        assertEquals("VITAMIN_D", card.canonicalName)
        assertEquals(14.4, card.currentReading.value, 0.01)
        assertEquals(28.5, card.previousReading?.value ?: 0.0, 0.01)
        // Delta: 14.4 - 28.5 = -14.1
        assertEquals(-14.1, card.deltaValue ?: 0.0, 0.01)
        assertEquals(2, card.history.size)
    }
}
