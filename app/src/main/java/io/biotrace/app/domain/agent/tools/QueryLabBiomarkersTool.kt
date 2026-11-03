package io.biotrace.app.domain.agent.tools

import io.biotrace.app.data.local.dao.LabBiomarkerDao
import io.biotrace.app.domain.agent.AgentTool
import io.biotrace.app.domain.agent.Citation
import io.biotrace.app.domain.agent.ToolResult
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

class QueryLabBiomarkersTool @Inject constructor(
    private val labBiomarkerDao: LabBiomarkerDao
) : AgentTool {

    override val name: String = "query_lab_biomarkers"
    override val description: String =
        "Query laboratory blood markers (e.g. HBA1C, CHOLESTEROL, FERRITIN, TSH, VITAMIN_D). Parameters: biomarkerName (String, optional)."

    override suspend fun execute(params: Map<String, Any>): ToolResult {
        return try {
            val biomarkerName = params["biomarkerName"] as? String

            val records = if (!biomarkerName.isNullOrBlank()) {
                val latest = labBiomarkerDao.getLatestByName(biomarkerName.uppercase())
                if (latest != null) listOf(latest) else emptyList()
            } else {
                labBiomarkerDao.getAnomalies()
            }

            if (records.isEmpty()) {
                return ToolResult(
                    success = true,
                    data = "No lab biomarker records found" + (if (biomarkerName != null) " for $biomarkerName" else ""),
                    citations = emptyList()
                )
            }

            val itemsJson = records.joinToString(",", "[", "]") { r ->
                """{"id":"${r.id}","biomarker":"${r.biomarkerName}","value":${r.value},"unit":"${r.unit}","referenceMin":${r.referenceMin},"referenceMax":${r.referenceMax},"flag":"${r.flag}","recordedAt":"${r.recordedAt}"}"""
            }
            val citations = records.map { r ->
                val ref = if (r.referenceMin != null && r.referenceMax != null) {
                    "${r.referenceMin}-${r.referenceMax} ${r.unit}"
                } else null

                Citation(
                    id = r.id,
                    timestamp = r.recordedAt,
                    value = r.value,
                    metric = r.biomarkerName,
                    baseline = ref
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
                error = "Failed to query biomarkers: ${e.message}"
            )
        }
    }
}
