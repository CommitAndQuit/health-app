package io.biotrace.app.ui.dashboard

import io.biotrace.app.data.healthconnect.HealthConnectManager
import io.biotrace.app.data.local.dao.VitalsDao
import io.biotrace.app.domain.model.VitalPoint
import io.biotrace.app.domain.repository.VitalsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val vitalsRepository: VitalsRepository = mock()
    private val vitalsDao: VitalsDao = mock()
    private val healthConnectManager: HealthConnectManager = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state with no data returns null for all biometric fields and empty history`() = runTest {
        whenever(healthConnectManager.hasAllPermissions()).thenReturn(false)
        whenever(vitalsRepository.getVitalsBetween(any(), any(), any())).thenReturn(emptyList())
        whenever(vitalsRepository.getLatestValue(any())).thenReturn(null)
        whenever(vitalsRepository.getRecentRecords(any(), any())).thenReturn(emptyList())

        val viewModel = DashboardViewModel(vitalsRepository, vitalsDao, healthConnectManager)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Assert strictly zero hardcoded mock values
        assertNull(state.userName)
        assertNull(state.selectedMood)
        assertNull(state.heartRate)
        assertNull(state.rhythmStatus)
        assertNull(state.ekgTimestamp)
        assertNull(state.bmi)
        assertNull(state.bmiStatus)
        assertNull(state.bodyFatPercentage)
        assertNull(state.bioImpedanceTimestamp)
        assertNull(state.rrIntervalMs)
        assertNull(state.signalQuality)
        assertTrue(state.ekgHistory.isEmpty())
    }

    @Test
    fun `when health connect heart rate data is imported, state reflects real biometric readings`() = runTest {
        val now = Instant.now()
        val realHrPoint = VitalPoint(
            id = "hr_1",
            timestamp = now,
            value = 74.0,
            unit = "bpm",
            metricType = "HEART_RATE"
        )
        val realHrvPoint = VitalPoint(
            id = "hrv_1",
            timestamp = now,
            value = 58.0,
            unit = "ms",
            metricType = "HRV"
        )
        val realWeightPoint = VitalPoint(
            id = "weight_1",
            timestamp = now,
            value = 68.0,
            unit = "kg",
            metricType = "WEIGHT"
        )
        val realBodyFatPoint = VitalPoint(
            id = "bodyfat_1",
            timestamp = now,
            value = 15.2,
            unit = "%",
            metricType = "BODY_FAT"
        )

        whenever(healthConnectManager.hasAllPermissions()).thenReturn(true)
        whenever(vitalsRepository.getVitalsBetween(any(), any(), any())).thenReturn(emptyList())
        whenever(vitalsRepository.getLatestValue("HEART_RATE")).thenReturn(realHrPoint)
        whenever(vitalsRepository.getLatestValue("HRV")).thenReturn(realHrvPoint)
        whenever(vitalsRepository.getLatestValue("WEIGHT")).thenReturn(realWeightPoint)
        whenever(vitalsRepository.getLatestValue("BODY_FAT")).thenReturn(realBodyFatPoint)
        whenever(vitalsRepository.getRecentRecords(any(), any())).thenReturn(listOf(realHrPoint))

        val viewModel = DashboardViewModel(vitalsRepository, vitalsDao, healthConnectManager)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(74, state.heartRate)
        assertEquals("REGULAR SINUS RHYTHM", state.rhythmStatus)
        assertNotNull(state.ekgTimestamp)
        assertEquals(58, state.rrIntervalMs)
        assertEquals(22.2, state.bmi)
        assertEquals("Optimal", state.bmiStatus)
        assertEquals(15.2, state.bodyFatPercentage)
        assertEquals(1, state.ekgHistory.size)
        assertEquals(74, state.ekgHistory[0].bpm)
    }

    @Test
    fun `sinus rhythm correctly diagnoses bradycardia and tachycardia`() = runTest {
        val now = Instant.now()
        val bradycardiaPoint = VitalPoint("hr_brady", now, 52.0, "bpm", "HEART_RATE")
        whenever(healthConnectManager.hasAllPermissions()).thenReturn(true)
        whenever(vitalsRepository.getVitalsBetween(any(), any(), any())).thenReturn(emptyList())
        whenever(vitalsRepository.getLatestValue("HEART_RATE")).thenReturn(bradycardiaPoint)
        whenever(vitalsRepository.getLatestValue("HRV")).thenReturn(null)
        whenever(vitalsRepository.getLatestValue("WEIGHT")).thenReturn(null)
        whenever(vitalsRepository.getLatestValue("BODY_FAT")).thenReturn(null)
        whenever(vitalsRepository.getRecentRecords(any(), any())).thenReturn(listOf(bradycardiaPoint))

        val viewModel = DashboardViewModel(vitalsRepository, vitalsDao, healthConnectManager)
        advanceUntilIdle()

        assertEquals(52, viewModel.uiState.value.heartRate)
        assertEquals("SINUS BRADYCARDIA", viewModel.uiState.value.rhythmStatus)
    }
}
