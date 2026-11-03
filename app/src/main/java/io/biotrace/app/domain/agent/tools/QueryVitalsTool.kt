package io.biotrace.app.domain.agent.tools

import io.biotrace.app.domain.agent.AgentTool
import io.biotrace.app.domain.agent.Citation
import io.biotrace.app.domain.agent.ToolResult
import io.biotrace.app.domain.repository.VitalsRepository
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

class QueryVitalsTool @Inject constructor(
    private val vitalsRepository: VitalsRepository
) : AgentTool {

    override val name: String = "query_vitals_summary"
    override val description: String =
        "Query user vitals such as HEART_RATE, HRV, STEPS, SLEEP, CALORIES, OXYGEN_SATURATION over a time window. Parameters: metric (String), startDate (ISO-8601 Instant String, optional), endDate (ISO-8601 Instant String, optional)."

    override suspend fun execute(params: Map<String, Any>): ToolResult {
        return try {
            val metric = (params["metric"] as? String)?.uppercase() ?: "HEART_RATE"
            val end = (params["endDate"] as? String)?.let { Instant.parse(it) } ?: Instant.now()
            val start = (params["startDate"] as? String)?.let { Instant.parse(it) } ?: end.minus(Duration.ofDays(7))

            val vitals = vitalsRepository.getVitalsBetween(metric, start, end)

            if (vitals.isEmpty()) {
                return ToolResult(
                    success = true,
                    data = "No records found for metric $metric between $start and $end",
                    citations = emptyList()
                )
            }

            val values = vitals.map { it.value }
            val avg = values.average()
            val min = values.minOrNull() ?: 0.0
            val max = values.maxOrNull() ?: 0.0

            val dataJson = """{"metric":"$metric","count":${vitals.size},"unit":"${vitals.first().unit}","average":"%.2f".format(avg),"min":$min,"max":$max,"latest":${vitals.last().value},"latestTimestamp":"${vitals.last().timestamp}"}"""

            val citations = vitals.takeLast(5).map {
                Citation(
                    id = it.id,
                    timestamp = it.timestamp,
                    value = it.value,
                    metric = it.metricType,
                    baseline = "Avg: %.1f %s".format(avg, it.unit)
                )
            }

            ToolResult(
                success = true,
                data = dataJson,
                citations = citations
            )
        } catch (e: Exception) {
            ToolResult(
                success = false,
                data = "",
                error = "Failed to query vitals: ${e.message}"
            )
        }
    }
}
