package io.biotrace.app.domain.repository

import android.net.Uri
import io.biotrace.app.data.healthconnect.SyncResult
import io.biotrace.app.domain.model.ContinuousHeartRateHeroData
import io.biotrace.app.domain.model.MetricTimeRange
import io.biotrace.app.domain.model.ProfileTriptychSummary
import io.biotrace.app.domain.model.VitalDetailUiState
import io.biotrace.app.domain.model.VitalMetricType
import io.biotrace.app.domain.model.VitalPoint
import io.biotrace.app.domain.model.VitalSummaryCardModel
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface VitalsRepository {
    fun getVitalsFlow(metricType: String, startTime: Instant, endTime: Instant): Flow<List<VitalPoint>>
    suspend fun getVitalsBetween(metricType: String, startTime: Instant, endTime: Instant): List<VitalPoint>
    suspend fun getLatestValue(metricType: String): VitalPoint?
    fun getLatestValueFlow(metricType: String): Flow<VitalPoint?>
    suspend fun getRecentRecords(metricType: String, limit: Int = 20): List<VitalPoint>
    fun getRecentRecordsFlow(metricType: String, limit: Int = 20): Flow<List<VitalPoint>>
    suspend fun syncFromHealthConnect(days: Long = 7): SyncResult
    suspend fun clearAll()

    // Health Connect + Gadgetbridge Stress feature extensions
    suspend fun getVitalsDashboardSnapshot(): List<VitalSummaryCardModel>
    suspend fun getContinuousHeartRateHeroData(): ContinuousHeartRateHeroData
    suspend fun getProfileTriptychSummary(): ProfileTriptychSummary
    suspend fun getMetricDetailData(metricType: VitalMetricType, range: MetricTimeRange): VitalDetailUiState
    suspend fun importGadgetbridgeUri(uri: Uri): Result<Int>
    suspend fun refreshGadgetbridgeStress(): Result<Int>?
    fun hasSavedGadgetbridgeUri(): Boolean
}
