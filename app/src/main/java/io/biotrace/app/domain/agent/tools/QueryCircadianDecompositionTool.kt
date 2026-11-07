package io.biotrace.app.domain.agent.tools

import io.biotrace.app.domain.agent.AgentTool
import io.biotrace.app.domain.agent.Citation
import io.biotrace.app.domain.agent.ToolResult
import io.biotrace.app.domain.analytics.timeseries.CircadianDecompositionService
import io.biotrace.app.domain.analytics.timeseries.HeartRateObservation
import io.biotrace.app.domain.repository.VitalsRepository
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueryCircadianDecompositionTool @Inject constructor(
    private val vitalsRepository: VitalsRepository,
    private val circadianDecompositionService: CircadianDecompositionService = CircadianDecompositionService
) : AgentTool {

    override val name: String = "query_circadian_decomposition"
    override val description: String =
        "Decompose 24-hour diurnal heart rate trajectory from Health Connect to evaluate nocturnal autonomic dipping percentage, autonomic dipping pattern (EXTREME_DIPPER, NORMAL_DIPPER, NON_DIPPER, REVERSE_DIPPER), and circadian stability index. Parameters: none required."

    override suspend fun execute(params: Map<String, Any>): ToolResult {
        return try {
            val now = Instant.now()
            val threeDaysAgo = now.minus(Duration.ofDays(3))

            val hrPoints = vitalsRepository.getVitalsBetween("HEART_RATE", threeDaysAgo, now)
            if (hrPoints.isEmpty()) {
                return ToolResult(
                    success = true,
                    data = "No heart rate records found in Health Connect over the last 72 hours to perform diurnal decomposition.",
                    citations = emptyList()
                )
            }

            val observations = hrPoints.map {
                HeartRateObservation(timestamp = it.timestamp, bpm = it.value)
            }

            val result = circadianDecompositionService.decompose(observations)

            val json = JSONObject().apply {
                put("daytimeMeanHr", result.daytimeMeanHr)
                put("nocturnalMinHr", result.nocturnalMinHr)
                put("dipPercentage", result.dipPercentage)
                put("pattern", result.pattern.name)
                put("patternDisplayName", result.pattern.displayName)
                put("patternDescription", result.pattern.clinicalDescription)
                put("circadianStabilityIndex", result.circadianStabilityIndex)
                put("residualSpikeCount", result.residualSpikeCount)
                put("narrative", result.narrative)
                val profileArray = JSONArray()
                result.hourlyProfile.take(6).forEach { hp ->
                    profileArray.put(
                        JSONObject().apply {
                            put("hour", hp.hourOfDay)
                            put("meanBpm", hp.meanBpm)
                        }
                    )
                }
                put("sampleHourlyProfile", profileArray)
            }

            val citations = hrPoints.takeLast(5).map { pt ->
                Citation(
                    id = pt.id,
                    timestamp = pt.timestamp,
                    value = pt.value,
                    metric = pt.metricType,
                    baseline = "Day mean: %.1f bpm, Dip: %.1f%%".format(result.daytimeMeanHr, result.dipPercentage)
                )
            }

            ToolResult(
                success = true,
                data = json.toString(),
                citations = citations
            )
        } catch (e: Exception) {
            ToolResult(
                success = false,
                data = "",
                error = "Failed to decompose circadian rhythm: ${e.message}"
            )
        }
    }
}
