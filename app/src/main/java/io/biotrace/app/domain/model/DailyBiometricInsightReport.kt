package io.biotrace.app.domain.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Domain categorization for biometric insight findings.
 */
enum class InsightDomainTag {
    WEARABLE,
    LAB_PANEL,
    NUTRITION,
    CIRCADIAN;

    companion object {
        fun fromString(value: String): InsightDomainTag? {
            val normalized = value.trim().uppercase().replace("-", "_")
            return entries.firstOrNull {
                it.name == normalized || it.name.replace("_", "") == normalized.replace("_", "")
            }
        }
    }
}

/**
 * Trajectory direction of a biometric metric or pattern.
 */
enum class TrendDirection {
    IMPROVING,
    STABLE,
    DRIFTING_OUT_OF_RANGE,
    ACUTE_STRAIN;

    companion object {
        fun fromString(value: String): TrendDirection? {
            val normalized = value.trim().uppercase().replace("-", "_")
            return entries.firstOrNull {
                it.name == normalized || it.name.replace("_", "") == normalized.replace("_", "")
            }
        }
    }
}

/**
 * A concrete trend observation synthesized across telemetry, lab, nutrition, or circadian data.
 */
data class TrendHighlight(
    val title: String,
    val domainTags: List<InsightDomainTag>,
    val direction: TrendDirection,
    val insightText: String,
    val supportingRecordIds: List<String>
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("title", title)
        val tagsArray = JSONArray()
        domainTags.forEach { tagsArray.put(it.name) }
        obj.put("domainTags", tagsArray)
        obj.put("direction", direction.name)
        obj.put("insightText", insightText)
        val recArray = JSONArray()
        supportingRecordIds.forEach { recArray.put(it) }
        obj.put("supportingRecordIds", recArray)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): TrendHighlight {
            val title = obj.optString("title", "")
            val tagsArray = obj.optJSONArray("domainTags") ?: obj.optJSONArray("domain_tags")
            val domainTags = mutableListOf<InsightDomainTag>()
            if (tagsArray != null) {
                for (i in 0 until tagsArray.length()) {
                    val tagStr = tagsArray.optString(i)
                    InsightDomainTag.fromString(tagStr)?.let { domainTags.add(it) }
                }
            }
            val dirStr = obj.optString("direction", "STABLE")
            val direction = TrendDirection.fromString(dirStr) ?: TrendDirection.STABLE
            val insightText = obj.optString("insightText", obj.optString("insight_text", ""))
            val recArray = obj.optJSONArray("supportingRecordIds") ?: obj.optJSONArray("supporting_record_ids")
            val supportingRecordIds = mutableListOf<String>()
            if (recArray != null) {
                for (i in 0 until recArray.length()) {
                    val rec = recArray.optString(i, "")
                    if (rec.isNotEmpty()) supportingRecordIds.add(rec)
                }
            }
            return TrendHighlight(
                title = title,
                domainTags = domainTags,
                direction = direction,
                insightText = insightText,
                supportingRecordIds = supportingRecordIds
            )
        }
    }
}

/**
 * High-leverage behavioral or clinical focus recommended for the day.
 */
data class ActionableFocus(
    val recommendation: String,
    val rationaleMetric: String
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("recommendation", recommendation)
        obj.put("rationaleMetric", rationaleMetric)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): ActionableFocus {
            val recommendation = obj.optString("recommendation", "")
            val rationaleMetric = obj.optString("rationaleMetric", obj.optString("rationale_metric", ""))
            return ActionableFocus(
                recommendation = recommendation,
                rationaleMetric = rationaleMetric
            )
        }
    }
}

/**
 * Strongly-typed synthesis report generated from multi-window biometric baselines,
 * nutrition records, lab biomarkers, circadian patterns, and clinical arbitration rules.
 */
data class DailyBiometricInsightReport(
    val headlineSummary: String,
    val clinicalArbitrationAlert: String?,
    val trendHighlights: List<TrendHighlight>,
    val actionableFocus: ActionableFocus
) {

    /**
     * Serializes this report to an [org.json.JSONObject].
     */
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("headlineSummary", headlineSummary)
        if (clinicalArbitrationAlert != null) {
            json.put("clinicalArbitrationAlert", clinicalArbitrationAlert)
        } else {
            json.put("clinicalArbitrationAlert", JSONObject.NULL)
        }
        val highlightsArray = JSONArray()
        trendHighlights.forEach { highlightsArray.put(it.toJson()) }
        json.put("trendHighlights", highlightsArray)
        json.put("actionableFocus", actionableFocus.toJson())
        return json
    }

    /**
     * Formats the structured report into clean Markdown suitable for display in UI cards or text stream.
     */
    fun toFormattedMarkdown(): String {
        val sb = StringBuilder()
        if (!clinicalArbitrationAlert.isNullOrBlank()) {
            sb.append("> ⚠️ **Clinical Safety Alert**: ").append(clinicalArbitrationAlert).append("\n\n")
        }
        sb.append("### Summary\n").append(headlineSummary).append("\n\n")
        if (trendHighlights.isNotEmpty()) {
            sb.append("### Trend Highlights\n")
            for (th in trendHighlights) {
                val tags = th.domainTags.joinToString(", ") { it.name }
                val dir = when (th.direction) {
                    TrendDirection.IMPROVING -> "🟢 Improving"
                    TrendDirection.STABLE -> "⚪ Stable"
                    TrendDirection.DRIFTING_OUT_OF_RANGE -> "🟡 Drifting"
                    TrendDirection.ACUTE_STRAIN -> "🔴 Acute Strain"
                }
                sb.append("- **").append(th.title).append("**")
                if (tags.isNotEmpty()) {
                    sb.append(" [").append(tags).append("]")
                }
                sb.append(" (").append(dir).append("): ").append(th.insightText)
                if (th.supportingRecordIds.isNotEmpty()) {
                    sb.append(" *(Records: ").append(th.supportingRecordIds.joinToString(", ")).append(")*")
                }
                sb.append("\n")
            }
            sb.append("\n")
        }
        sb.append("### Actionable Focus\n")
        sb.append("- **Recommendation**: ").append(actionableFocus.recommendation).append("\n")
        sb.append("- **Rationale**: ").append(actionableFocus.rationaleMetric).append("\n")
        return sb.toString().trimEnd()
    }

    companion object {

        /**
         * Parses a [JSONObject] into a [DailyBiometricInsightReport], supporting both camelCase and snake_case keys.
         */
        fun fromJson(json: JSONObject): DailyBiometricInsightReport {
            val headlineSummary = json.optString("headlineSummary", json.optString("headline_summary", ""))

            val rawAlert = when {
                json.has("clinicalArbitrationAlert") && !json.isNull("clinicalArbitrationAlert") ->
                    json.optString("clinicalArbitrationAlert")
                json.has("clinical_arbitration_alert") && !json.isNull("clinical_arbitration_alert") ->
                    json.optString("clinical_arbitration_alert")
                else -> null
            }
            val clinicalArbitrationAlert = rawAlert?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }

            val highlightsArray = json.optJSONArray("trendHighlights") ?: json.optJSONArray("trend_highlights")
            val trendHighlights = mutableListOf<TrendHighlight>()
            if (highlightsArray != null) {
                for (i in 0 until highlightsArray.length()) {
                    val item = highlightsArray.optJSONObject(i)
                    if (item != null) {
                        trendHighlights.add(TrendHighlight.fromJson(item))
                    }
                }
            }

            val focusObj = json.optJSONObject("actionableFocus")
                ?: json.optJSONObject("actionable_focus")
                ?: JSONObject()
            val actionableFocus = ActionableFocus.fromJson(focusObj)

            return DailyBiometricInsightReport(
                headlineSummary = headlineSummary,
                clinicalArbitrationAlert = clinicalArbitrationAlert,
                trendHighlights = trendHighlights,
                actionableFocus = actionableFocus
            )
        }

        /**
         * Strips optional markdown fencing (e.g. ```json ... ```) and parses the inner JSON string.
         */
        fun fromJson(jsonStr: String): DailyBiometricInsightReport {
            val trimmed = jsonStr.trim()
            val stripped = if (trimmed.startsWith("```")) {
                trimmed.lines()
                    .filterNot { it.trim().startsWith("```") }
                    .joinToString("\n")
                    .trim()
            } else {
                trimmed
            }

            val start = stripped.indexOf('{')
            val end = stripped.lastIndexOf('}')
            val cleanJson = if (start != -1 && end != -1 && start < end) {
                stripped.substring(start, end + 1)
            } else {
                stripped
            }

            return fromJson(JSONObject(cleanJson))
        }

        /**
         * Creates a deterministic grounded fallback report if the LLM invocation fails or returns unparseable content.
         * Ensures clinical arbitration safety alerts are never swallowed.
         */
        fun createDeterministicFallback(alert: String?, reason: String): DailyBiometricInsightReport {
            val highlights = if (!alert.isNullOrBlank()) {
                listOf(
                    TrendHighlight(
                        title = "Deterministic Safety Arbitration",
                        domainTags = listOf(InsightDomainTag.WEARABLE),
                        direction = TrendDirection.ACUTE_STRAIN,
                        insightText = alert,
                        supportingRecordIds = emptyList()
                    )
                )
            } else {
                emptyList()
            }

            val focus = if (!alert.isNullOrBlank()) {
                ActionableFocus(
                    recommendation = "Review clinical safety warnings with a qualified healthcare professional.",
                    rationaleMetric = "Deterministic Clinical Rule Threshold Exceeded"
                )
            } else {
                ActionableFocus(
                    recommendation = "Continue baseline biometric monitoring and maintain steady circadian routines.",
                    rationaleMetric = "Baseline Telemetry Stability"
                )
            }

            val headline = if (!alert.isNullOrBlank()) {
                "Automated biometric synthesis operating in deterministic fallback mode: $reason. Clinical alerts detected and preserved."
            } else {
                "Automated biometric synthesis operating in deterministic fallback mode: $reason."
            }

            return DailyBiometricInsightReport(
                headlineSummary = headline,
                clinicalArbitrationAlert = alert?.takeIf { it.isNotBlank() },
                trendHighlights = highlights,
                actionableFocus = focus
            )
        }
    }
}
