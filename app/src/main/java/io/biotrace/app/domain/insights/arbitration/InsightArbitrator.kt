package io.biotrace.app.domain.insights.arbitration

import io.biotrace.app.domain.agent.Citation
import io.biotrace.app.domain.anomaly.AnomalyAlert
import io.biotrace.app.domain.anomaly.AnomalyDetectionEngine
import io.biotrace.app.domain.clinical.rules.ClinicalEvaluation
import io.biotrace.app.domain.clinical.rules.ClinicalSafetyRuleEngine
import io.biotrace.app.domain.clinical.rules.ClinicalStatus
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deterministic clinical safety arbitration engine.
 *
 * Ensures evidence-based medical safety rules take absolute precedence over
 * probabilistic generative LLM reports, strictly adhering to AGENTS.md guidelines.
 */
@Singleton
class InsightArbitrator @Inject constructor(
    val clinicalSafetyRuleEngine: ClinicalSafetyRuleEngine,
    val anomalyDetectionEngine: AnomalyDetectionEngine
) {

    /**
     * Arbitrates between deterministic clinical evaluations, anomaly alerts, and raw AI narrative.
     *
     * If any [ClinicalStatus.CRITICAL] or [ClinicalStatus.ELEVATED] evaluation exists, or high-severity
     * anomaly alert exists:
     * 1. Prepends a prominent Clinical Safety Notice banner.
     * 2. Guarantees medical priority over generative advice.
     * 3. Enforces standard citation grounding with source attribution.
     */
    fun arbitrate(
        evaluations: List<ClinicalEvaluation>,
        alerts: List<AnomalyAlert>,
        rawAiReport: String
    ): ArbitratedInsight {
        val criticalEvaluations = evaluations.filter { it.status == ClinicalStatus.CRITICAL }
        val elevatedEvaluations = evaluations.filter { it.status == ClinicalStatus.ELEVATED }
        val highSeverityAlerts = alerts.filter {
            it.severity.equals("ALERT", ignoreCase = true) ||
            it.severity.equals("CRITICAL", ignoreCase = true) ||
            it.severity.equals("HIGH", ignoreCase = true)
        }

        val highestStatus = when {
            criticalEvaluations.isNotEmpty() -> ClinicalStatus.CRITICAL
            elevatedEvaluations.isNotEmpty() -> ClinicalStatus.ELEVATED
            evaluations.any { it.status == ClinicalStatus.BORDERLINE } -> ClinicalStatus.BORDERLINE
            evaluations.any { it.status == ClinicalStatus.NORMAL } -> ClinicalStatus.NORMAL
            evaluations.any { it.status == ClinicalStatus.OPTIMAL } -> ClinicalStatus.OPTIMAL
            else -> null
        }

        val hasClinicalSafetyNotice = criticalEvaluations.isNotEmpty() ||
            elevatedEvaluations.isNotEmpty() ||
            highSeverityAlerts.isNotEmpty()

        if (!hasClinicalSafetyNotice) {
            return ArbitratedInsight(
                finalContent = rawAiReport,
                hasClinicalSafetyNotice = false,
                safetyNotice = null,
                highestClinicalStatus = highestStatus,
                criticalEvaluations = emptyList(),
                elevatedEvaluations = emptyList(),
                highSeverityAlerts = emptyList(),
                citations = emptyList()
            )
        }

        val citations = mutableListOf<Citation>()
        val banner = buildString {
            append("⚠️ CLINICAL SAFETY NOTICE\n")
            append("Evidence-based clinical guidelines take priority over AI-generated insights. The following safety flag(s) were identified in Health Connect records:\n\n")

            if (criticalEvaluations.isNotEmpty()) {
                append("CRITICAL CLINICAL FINDINGS:\n")
                criticalEvaluations.forEach { eval ->
                    append("• [CRITICAL] ${eval.metricName}: ${eval.value} ${eval.unit} (Normal: ${eval.referenceRange}, Guideline: ${eval.guidelineSource})\n")
                    append("  ${eval.narrative}\n")
                    citations.add(
                        Citation(
                            id = eval.ruleId,
                            timestamp = Instant.now(),
                            value = eval.value,
                            metric = eval.metricName,
                            baseline = eval.referenceRange
                        )
                    )
                }
                append("\n")
            }

            if (elevatedEvaluations.isNotEmpty()) {
                append("ELEVATED CLINICAL RISKS:\n")
                elevatedEvaluations.forEach { eval ->
                    append("• [ELEVATED] ${eval.metricName}: ${eval.value} ${eval.unit} (Normal: ${eval.referenceRange}, Guideline: ${eval.guidelineSource})\n")
                    append("  ${eval.narrative}\n")
                    citations.add(
                        Citation(
                            id = eval.ruleId,
                            timestamp = Instant.now(),
                            value = eval.value,
                            metric = eval.metricName,
                            baseline = eval.referenceRange
                        )
                    )
                }
                append("\n")
            }

            if (highSeverityAlerts.isNotEmpty()) {
                append("PHYSIOLOGICAL ANOMALIES:\n")
                highSeverityAlerts.forEach { alert ->
                    append("• [${alert.severity}] ${alert.metricName}: ${alert.narrative}\n")
                    citations.add(
                        Citation(
                            id = alert.id,
                            timestamp = alert.detectedAt,
                            value = alert.currentValue,
                            metric = alert.metricName,
                            baseline = alert.baselineValue?.let { "$it ${alert.unit}" }
                        )
                    )
                }
                append("\n")
            }

            append("Recommendation: Please consult a licensed healthcare professional for medical diagnosis and clinical treatment.")
        }.trim()

        val finalContent = if (rawAiReport.isBlank()) {
            banner
        } else {
            "$banner\n\n---\n\n$rawAiReport"
        }

        return ArbitratedInsight(
            finalContent = finalContent,
            hasClinicalSafetyNotice = true,
            safetyNotice = banner,
            highestClinicalStatus = highestStatus,
            criticalEvaluations = criticalEvaluations,
            elevatedEvaluations = elevatedEvaluations,
            highSeverityAlerts = highSeverityAlerts,
            citations = citations.distinctBy { it.id }
        )
    }
}
