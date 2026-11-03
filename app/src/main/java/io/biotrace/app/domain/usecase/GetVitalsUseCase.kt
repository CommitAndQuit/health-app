package io.biotrace.app.domain.usecase

import io.biotrace.app.domain.model.VitalPoint
import io.biotrace.app.domain.repository.VitalsRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject

class GetVitalsUseCase @Inject constructor(
    private val vitalsRepository: VitalsRepository
) {
    operator fun invoke(
        metricType: String,
        startTime: Instant,
        endTime: Instant
    ): Flow<List<VitalPoint>> {
        return vitalsRepository.getVitalsFlow(metricType, startTime, endTime)
    }

    suspend fun getLatest(metricType: String): VitalPoint? {
        return vitalsRepository.getLatestValue(metricType)
    }

    fun getLatestFlow(metricType: String): Flow<VitalPoint?> {
        return vitalsRepository.getLatestValueFlow(metricType)
    }
}
