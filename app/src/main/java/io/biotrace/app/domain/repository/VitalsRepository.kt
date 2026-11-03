package io.biotrace.app.domain.repository

import io.biotrace.app.data.healthconnect.SyncResult
import io.biotrace.app.domain.model.VitalPoint
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
}
