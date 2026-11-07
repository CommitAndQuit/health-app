package io.biotrace.app.domain.clinical.risk

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Risk tier for harmonized metabolic syndrome criteria (available-data model, zero blood pressure dependency).
 */
enum class MetabolicRiskLevel {
    LOW_RISK,
    MODERATE_METABOLIC_STRAIN,
    ELEVATED_METABOLIC_RISK,
    HIGH_METABOLIC_SYNDROME_RISK
}

/**
 * Detail for an individual harmonized metabolic criterion.
 */
data class MetabolicCriterionDetail(
    val criterionName: String,
    val isMet: Boolean,
    val observedValue: String,
    val thresholdDescription: String
)

/**
 * Result of harmonized metabolic risk calculation.
 */
data class MetabolicRiskResult(
    val criteriaMetCount: Int,
    val criteriaDetails: List<MetabolicCriterionDetail>,
    val riskLevel: MetabolicRiskLevel,
    val guidance: String
)

/**
 * Harmonized metabolic risk calculator utilizing available biomarker and anthropometric data.
 *
 * Adheres strictly to AGENTS.md: zero blood pressure dependency and zero vendor branding.
 *
 * Evaluates 4 available harmonized criteria:
 * 1. Glycemic: Fasting Glucose >= 100 mg/dL OR HbA1c >= 5.7%
 * 2. Dyslipidemia (Triglycerides): Fasting Triglycerides >= 150 mg/dL
 * 3. Dyslipidemia (HDL): Low HDL (< 40 mg/dL for males, < 50 mg/dL for females)
 * 4. Adiposity: BMI >= 25.0 kg/m²
 */
@Singleton
class HarmonizedMetabolicRiskCalculator @Inject constructor() {

    fun calculate(
        fastingGlucoseMgDl: Double? = null,
        hba1cPercent: Double? = null,
        triglyceridesMgDl: Double? = null,
        hdlMgDl: Double? = null,
        isMale: Boolean = true,
        bmi: Double? = null
    ): MetabolicRiskResult {
        val details = mutableListOf<MetabolicCriterionDetail>()

        // Criterion 1: Glycemic regulation (Fasting Glucose >= 100 mg/dL or HbA1c >= 5.7%)
        val glucoseMet = fastingGlucoseMgDl?.let { it >= 100.0 } ?: false
        val hba1cMet = hba1cPercent?.let { it >= 5.7 } ?: false
        val glycemicMet = glucoseMet || hba1cMet
        val glycemicObserved = when {
            fastingGlucoseMgDl != null && hba1cPercent != null ->
                "Glucose: ${fastingGlucoseMgDl} mg/dL, HbA1c: ${hba1cPercent}%"
            fastingGlucoseMgDl != null ->
                "Glucose: ${fastingGlucoseMgDl} mg/dL"
            hba1cPercent != null ->
                "HbA1c: ${hba1cPercent}%"
            else -> "Not available"
        }
        details.add(
            MetabolicCriterionDetail(
                criterionName = "Impaired Fasting Glucose / Pre-diabetes",
                isMet = glycemicMet,
                observedValue = glycemicObserved,
                thresholdDescription = "Fasting Glucose ≥ 100 mg/dL or HbA1c ≥ 5.7%"
            )
        )

        // Criterion 2: Hypertriglyceridemia (Triglycerides >= 150 mg/dL)
        val tgMet = triglyceridesMgDl?.let { it >= 150.0 } ?: false
        details.add(
            MetabolicCriterionDetail(
                criterionName = "Elevated Triglycerides",
                isMet = tgMet,
                observedValue = triglyceridesMgDl?.let { "$it mg/dL" } ?: "Not available",
                thresholdDescription = "Triglycerides ≥ 150 mg/dL"
            )
        )

        // Criterion 3: Low HDL (< 40 male, < 50 female)
        val hdlThreshold = if (isMale) 40.0 else 50.0
        val hdlMet = hdlMgDl?.let { it < hdlThreshold } ?: false
        val sexLabel = if (isMale) "male" else "female"
        details.add(
            MetabolicCriterionDetail(
                criterionName = "Low HDL Cholesterol",
                isMet = hdlMet,
                observedValue = hdlMgDl?.let { "$it mg/dL ($sexLabel)" } ?: "Not available ($sexLabel)",
                thresholdDescription = "HDL < $hdlThreshold mg/dL ($sexLabel)"
            )
        )

        // Criterion 4: Elevated Body Mass Index (BMI >= 25.0 kg/m²)
        val bmiMet = bmi?.let { it >= 25.0 } ?: false
        details.add(
            MetabolicCriterionDetail(
                criterionName = "Elevated Body Mass Index",
                isMet = bmiMet,
                observedValue = bmi?.let { "%.1f kg/m²".format(it) } ?: "Not available",
                thresholdDescription = "BMI ≥ 25.0 kg/m²"
            )
        )

        val criteriaMetCount = details.count { it.isMet }

        val riskLevel = when (criteriaMetCount) {
            0 -> MetabolicRiskLevel.LOW_RISK
            1 -> MetabolicRiskLevel.MODERATE_METABOLIC_STRAIN
            2 -> MetabolicRiskLevel.ELEVATED_METABOLIC_RISK
            else -> MetabolicRiskLevel.HIGH_METABOLIC_SYNDROME_RISK
        }

        val guidance = buildGuidance(riskLevel, criteriaMetCount)

        return MetabolicRiskResult(
            criteriaMetCount = criteriaMetCount,
            criteriaDetails = details,
            riskLevel = riskLevel,
            guidance = guidance
        )
    }

    private fun buildGuidance(riskLevel: MetabolicRiskLevel, count: Int): String {
        return when (riskLevel) {
            MetabolicRiskLevel.LOW_RISK ->
                "All evaluated metabolic parameters are within healthy clinical ranges (0/4 criteria met). Maintain balanced macronutrient intake and regular daily physical activity."
            MetabolicRiskLevel.MODERATE_METABOLIC_STRAIN ->
                "Early metabolic strain identified ($count/4 criteria met). Targeted lifestyle adjustments in dietary refined sugars or zone-2 aerobic conditioning can prevent progression."
            MetabolicRiskLevel.ELEVATED_METABOLIC_RISK ->
                "Elevated metabolic risk ($count/4 criteria met). Clustering of metabolic markers indicates insulin resistance. Prioritize resistance training, Mediterranean-style nutrition, and weight optimization."
            MetabolicRiskLevel.HIGH_METABOLIC_SYNDROME_RISK ->
                "High metabolic syndrome risk profile ($count/4 criteria met). Concurrent multi-system metabolic markers detected. Consult a healthcare professional for comprehensive metabolic and cardiovascular assessment."
        }
    }
}
