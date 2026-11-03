package io.biotrace.app.domain.agent.tools

import io.biotrace.app.data.local.dao.LabBiomarkerDao
import io.biotrace.app.domain.agent.AgentTool
import io.biotrace.app.domain.agent.Citation
import io.biotrace.app.domain.agent.ToolResult
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

class GetActiveAnomaliesTool @Inject constructor(
    private val labBiomarkerDao: LabBiomarkerDao
) : AgentTool {

    override val name: String = "get_active_anomalies"
    override val description: String =
        "Retrieve currently active health anomalies and out-of-range lab markers. Takes no required parameters."

    override suspend fun execute(params: Map<String, Any>): ToolResult {
        return try {
            val anomalies = labBiomarkerDao.getAnomalies()
            if (anomalies.isEmpty()) {
                return ToolResult(
                    success = true,
                    data = "No active anomalies or out-of-range markers detected in the local database.",
                    citations = emptyList()
                )
            }

            val itemsJson = anomalies.joinToString(",", "[", "]") { a ->
                """{"id":"${a.id}","biomarker":"${a.biomarkerName}","value":${a.value},"unit":"${a.unit}","flag":"${a.flag}","referenceRange":"${a.referenceMin ?: "?"} - ${a.referenceMax ?: "?"}","date":"${a.recordedAt}"}"""
            }
            val citations = anomalies.map { a ->
                Citation(
                    id = a.id,
                    timestamp = a.recordedAt,
                    value = a.value,
                    metric = a.biomarkerName,
                    baseline = "Normal range: ${a.referenceMin}-${a.referenceMax}"
                )
            }

            ToolResult(
                success = true,
                data = itemsJson,
                citations = citations
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
