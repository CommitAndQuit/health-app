package io.biotrace.app.domain.clinical.rules

/**
 * Clinical severity classification status according to evidence-based medical guidelines.
 */
enum class ClinicalStatus {
    OPTIMAL,
    NORMAL,
    BORDERLINE,
    ELEVATED,
    CRITICAL
}

/**
 * Structured clinical evaluation result for a vital sign or laboratory biomarker.
 */
data class ClinicalEvaluation(
    val ruleId: String,
    val metricName: String,
    val status: ClinicalStatus,
    val value: Double,
    val unit: String,
    val referenceRange: String,
    val narrative: String,
    val guidelineSource: String
)

/**
 * Contract for deterministic clinical safety rules evaluating biometric inputs against standard guidelines.
 */
interface ClinicalRule {
    val id: String
    val name: String
    val guidelineSource: String

    fun evaluate(value: Double): ClinicalEvaluation?

    fun evaluate(value: Double, biologicalSex: String?): ClinicalEvaluation? = evaluate(value)
}
