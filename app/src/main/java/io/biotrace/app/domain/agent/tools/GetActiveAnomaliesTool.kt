package io.biotrace.app.domain.agent.tools

import io.biotrace.app.data.local.dao.LabBiomarkerDao
import io.biotrace.app.domain.agent.AgentTool
import io.biotrace.app.domain.agent.Citation
import io.biotrace.app.domain.agent.ToolResult
import io.biotrace.app.domain.anomaly.AnomalyDetectionEngine
import java.time.Instant
import javax.inject.Inject

class GetActiveAnomaliesTool @Inject constructor(
    private val labBiomarkerDao: LabBiomarkerDao,
    private val anomalyDetectionEngine: AnomalyDetectionEngine
) : AgentTool {

    override val name: String = "get_active_anomalies"
    override val description: String =
        "Retrieve currently active health anomalies, out-of-range lab markers, and longitudinal trend shifts. Takes no required parameters."

    override suspend fun execute(params: Map<String, Any>): ToolResult {
        return try {
            val biomarkers = labBiomarkerDao.getAll()
            if (biomarkers.isEmpty()) {
                return ToolResult(
                    success = true,
                    data = "No active anomalies or lab markers detected in the local database.",
                    citations = emptyList()
                )
            }

            val alerts = mutableListOf<String>()
            val citations = mutableListOf<Citation>()

            for (bm in biomarkers) {
                val evaluated = anomalyDetectionEngine.evaluateBiomarker(bm, biomarkers)
                for (alert in evaluated) {
                    alerts.add("""{"id":"${alert.id}","rule":"${alert.ruleType}","metric":"${alert.metricName}","value":${alert.currentValue},"unit":"${alert.unit}","severity":"${alert.severity}","narrative":"${alert.narrative}"}""")
                    citations.add(
                        Citation(
                            id = bm.id,
                            timestamp = bm.recordedAt,
                            value = bm.value,
                            metric = bm.biomarkerName,
                            baseline = "Normal: ${bm.referenceMin ?: "?"}-${bm.referenceMax ?: "?"} ${bm.unit}"
                        )
                    )
                }
            }

            if (alerts.isEmpty()) {
                // Check if any direct flag exists
                val directAnomalies = labBiomarkerDao.getAnomalies()
                if (directAnomalies.isEmpty()) {
                    return ToolResult(
                        success = true,
                        data = "All biomarkers are within normal healthy reference ranges.",
                        citations = emptyList()
                    )
                }
            }

            ToolResult(
                success = true,
                data = alerts.joinToString(",", "[", "]"),
                citations = citations.distinctBy { it.id }
            )
        } catch (e: Exception) {
            ToolResult(
                success = false,
                data = "",
                error = "Failed to get active anomalies: ${e.message}"
            )
        }
    }
}
