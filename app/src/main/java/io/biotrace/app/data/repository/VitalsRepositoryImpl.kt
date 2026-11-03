package io.biotrace.app.data.repository

import io.biotrace.app.data.healthconnect.HealthConnectSyncService
import io.biotrace.app.data.healthconnect.SyncResult
import io.biotrace.app.data.local.dao.VitalsDao
import io.biotrace.app.data.local.entity.VitalsRecordEntity
import io.biotrace.app.domain.model.VitalPoint
import io.biotrace.app.domain.repository.VitalsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VitalsRepositoryImpl @Inject constructor(
    private val vitalsDao: VitalsDao,
    private val syncService: HealthConnectSyncService
) : VitalsRepository {

    private fun VitalsRecordEntity.toDomain(): VitalPoint {
        return VitalPoint(
            id = id,
            timestamp = timestamp,
            value = value,
            unit = unit,
            metricType = metricType
        )
    }

    override fun getVitalsFlow(
        metricType: String,
        startTime: Instant,
        endTime: Instant
    ): Flow<List<VitalPoint>> {
        return vitalsDao.getRecordsFlow(metricType, startTime, endTime).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getVitalsBetween(
        metricType: String,
        startTime: Instant,
        endTime: Instant
    ): List<VitalPoint> {
        return vitalsDao.getRecordsBetween(metricType, startTime, endTime).map { it.toDomain() }
    }

    override suspend fun getLatestValue(metricType: String): VitalPoint? {
        return vitalsDao.getLatestRecord(metricType)?.toDomain()
    }

    override fun getLatestValueFlow(metricType: String): Flow<VitalPoint?> {
        return vitalsDao.getLatestRecordFlow(metricType).map { it?.toDomain() }
    }

    override suspend fun syncFromHealthConnect(days: Long): SyncResult {
        return syncService.syncAll(days)
    }

    override suspend fun clearAll() {
        vitalsDao.deleteAll()
    }
}
