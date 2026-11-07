package io.biotrace.app.domain.usecase

import io.biotrace.app.data.healthconnect.SyncResult
import io.biotrace.app.domain.model.ContinuousHeartRateHeroData
import io.biotrace.app.domain.model.MetricTimeRange
import io.biotrace.app.domain.model.ProfileTriptychSummary
import io.biotrace.app.domain.model.VitalDetailUiState
import io.biotrace.app.domain.model.VitalMetricType
import io.biotrace.app.domain.model.VitalPoint
import io.biotrace.app.domain.model.VitalSummaryCardModel
import io.biotrace.app.domain.repository.VitalsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class GetVitalsUseCaseTest {

    private val fakeRepo = object : VitalsRepository {
        private val samplePoint = VitalPoint(
            id = "1",
            timestamp = Instant.parse("2026-11-03T10:00:00Z"),
            value = 75.0,
            unit = "bpm",
            metricType = "HEART_RATE"
        )

        override fun getVitalsFlow(metricType: String, startTime: Instant, endTime: Instant): Flow<List<VitalPoint>> {
            return flowOf(listOf(samplePoint))
        }

        override suspend fun getVitalsBetween(metricType: String, startTime: Instant, endTime: Instant): List<VitalPoint> {
            return listOf(samplePoint)
        }

        override suspend fun getLatestValue(metricType: String): VitalPoint? {
            return samplePoint
        }

        override fun getLatestValueFlow(metricType: String): Flow<VitalPoint?> {
            return flowOf(samplePoint)
        }

        override suspend fun getRecentRecords(metricType: String, limit: Int): List<VitalPoint> {
            return listOf(samplePoint)
        }

        override fun getRecentRecordsFlow(metricType: String, limit: Int): Flow<List<VitalPoint>> {
            return flowOf(listOf(samplePoint))
        }

        override suspend fun syncFromHealthConnect(days: Long): SyncResult {
            return SyncResult(recordsSynced = 1)
        }

        override suspend fun clearAll() {}

        override suspend fun getVitalsDashboardSnapshot(): List<VitalSummaryCardModel> {
            TODO("Not required for GetVitalsUseCaseTest")
        }

        override suspend fun getContinuousHeartRateHeroData(): ContinuousHeartRateHeroData {
            TODO("Not required for GetVitalsUseCaseTest")
        }

        override suspend fun getProfileTriptychSummary(): ProfileTriptychSummary {
            TODO("Not required for GetVitalsUseCaseTest")
        }

        override suspend fun getMetricDetailData(
            metricType: VitalMetricType,
            range: MetricTimeRange
        ): VitalDetailUiState {
            TODO("Not required for GetVitalsUseCaseTest")
        }
    }

    private val useCase = GetVitalsUseCase(fakeRepo)

    @Test
    fun testGetVitalsFlow() = runBlocking {
        val now = Instant.now()
        val result = useCase("HEART_RATE", now.minusSeconds(3600), now).first()
        assertEquals(1, result.size)
        assertEquals(75.0, result[0].value, 0.001)
        assertEquals("bpm", result[0].unit)
    }

    @Test
    fun testGetLatest() = runBlocking {
        val latest = useCase.getLatest("HEART_RATE")
        assertEquals(75.0, latest?.value ?: 0.0, 0.001)
    }
}
