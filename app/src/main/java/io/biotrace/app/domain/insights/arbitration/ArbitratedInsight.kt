package io.biotrace.app.domain.insights.arbitration

import io.biotrace.app.domain.agent.Citation
import io.biotrace.app.domain.anomaly.AnomalyAlert
import io.biotrace.app.domain.clinical.rules.ClinicalEvaluation
import io.biotrace.app.domain.clinical.rules.ClinicalStatus

/**
 * Encapsulates the arbitrated AI health insight guaranteeing clinical safety priority,
 * deterministic guideline enforcement, and citation grounding.
 */
data class ArbitratedInsight(
    val finalContent: String,
    val hasClinicalSafetyNotice: Boolean,
    val safetyNotice: String?,
    val highestClinicalStatus: ClinicalStatus?,
    val criticalEvaluations: List<ClinicalEvaluation> = emptyList(),
    val elevatedEvaluations: List<ClinicalEvaluation> = emptyList(),
    val highSeverityAlerts: List<AnomalyAlert> = emptyList(),
    val citations: List<Citation> = emptyList()
)
