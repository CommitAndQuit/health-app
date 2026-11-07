package io.biotrace.app.domain.clinical.rules

import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deterministic clinical evaluation engine for vital metrics and laboratory biomarkers.
 * Evaluates inputs against the evidence-based [MedicalGuidelinesCatalog] with zero external/cloud dependencies.
 */
@Singleton
class ClinicalSafetyRuleEngine @Inject constructor() {

    /**
     * Evaluates a vital sign metric against standardized clinical safety rules.
     *
     * @param metricType Identifier for the vital metric (e.g. "RESTING_HEART_RATE", "SPO2")
     * @param value Numerical measurement
     * @param biologicalSex Optional biological sex ("male", "female") for sex-stratified rules
     * @return List of matching [ClinicalEvaluation] results, or empty list if no rule applies
     */
    fun evaluateVitals(
        metricType: String,
        value: Double,
        biologicalSex: String? = null
    ): List<ClinicalEvaluation> {
        val normalizedType = metricType.trim().uppercase(Locale.ROOT)
            .replace('-', '_')
            .replace(' ', '_')

        val evaluation = when {
            normalizedType in RHR_KEYS -> {
                MedicalGuidelinesCatalog.restingHeartRateRule.evaluate(value, biologicalSex)
            }
            normalizedType in SPO2_KEYS -> {
                MedicalGuidelinesCatalog.oxygenSaturationRule.evaluate(value, biologicalSex)
            }
            else -> null
        }

        return listOfNotNull(evaluation)
    }

    /**
     * Evaluates a normalized laboratory biomarker against clinical safety thresholds.
     *
     * @param normalizedName Canonical or normalized biomarker name (e.g. "FASTING_GLUCOSE", "HBA1C", "LDL")
     * @param value Biomarker numeric value
     * @param unit Unit of measurement (e.g. "mg/dL", "g/dL", "%", "mmol/L")
     * @param biologicalSex Optional biological sex for sex-stratified thresholds
     * @return List of matching [ClinicalEvaluation] results
     */
    fun evaluateBiomarker(
        normalizedName: String,
        value: Double,
        unit: String,
        biologicalSex: String? = null
    ): List<ClinicalEvaluation> {
        val key = normalizedName.trim().uppercase(Locale.ROOT)
            .replace('-', '_')
            .replace(' ', '_')
        val unitNormalized = unit.trim().lowercase(Locale.ROOT)

        val evaluation = when {
            key in GLUCOSE_KEYS -> {
                val standardizedValue = if (unitNormalized.contains("mmol")) {
                    value * MMOL_TO_MGDL_GLUCOSE
                } else {
                    value
                }
                MedicalGuidelinesCatalog.fastingGlucoseRule.evaluate(standardizedValue, biologicalSex)
            }
            key in HBA1C_KEYS -> {
                val standardizedValue = if (unitNormalized.contains("mmol/mol")) {
                    (value * 0.09148) + 2.152 // IFCC to DCCT conversion
                } else {
                    value
                }
                MedicalGuidelinesCatalog.hbA1cRule.evaluate(standardizedValue, biologicalSex)
            }
            key in TRIGLYCERIDES_KEYS -> {
                val standardizedValue = if (unitNormalized.contains("mmol")) {
                    value * MMOL_TO_MGDL_TRIGLYCERIDES
                } else {
                    value
                }
                MedicalGuidelinesCatalog.triglyceridesRule.evaluate(standardizedValue, biologicalSex)
            }
            key in LDL_KEYS -> {
                val standardizedValue = if (unitNormalized.contains("mmol")) {
                    value * MMOL_TO_MGDL_CHOLESTEROL
                } else {
                    value
                }
                MedicalGuidelinesCatalog.ldlCholesterolRule.evaluate(standardizedValue, biologicalSex)
            }
            key in HDL_KEYS -> {
                val standardizedValue = if (unitNormalized.contains("mmol")) {
                    value * MMOL_TO_MGDL_CHOLESTEROL
                } else {
                    value
                }
                MedicalGuidelinesCatalog.hdlCholesterolRule.evaluate(standardizedValue, biologicalSex)
            }
            key in HEMOGLOBIN_KEYS -> {
                val standardizedValue = if (unitNormalized == "g/l") {
                    value / 10.0
                } else {
                    value
                }
                MedicalGuidelinesCatalog.hemoglobinRule.evaluate(standardizedValue, biologicalSex)
            }
            else -> null
        }

        return listOfNotNull(evaluation)
    }

    companion object {
        private const val MMOL_TO_MGDL_GLUCOSE = 18.0182
        private const val MMOL_TO_MGDL_CHOLESTEROL = 38.67
        private const val MMOL_TO_MGDL_TRIGLYCERIDES = 88.57

        private val RHR_KEYS = setOf(
            "RESTING_HEART_RATE",
            "HEART_RATE",
            "RHR",
            "RESTING_HR",
            "PULSE"
        )

        private val SPO2_KEYS = setOf(
            "SPO2",
            "OXYGEN_SATURATION",
            "BLOOD_OXYGEN",
            "OXYGEN"
        )

        private val GLUCOSE_KEYS = setOf(
            "FASTING_GLUCOSE",
            "GLUCOSE",
            "FBS",
            "BLOOD_GLUCOSE",
            "SERUM_GLUCOSE",
            "FASTING_BLOOD_SUGAR"
        )

        private val HBA1C_KEYS = setOf(
            "HBA1C",
            "HEMOGLOBIN_A1C",
            "GLYCATED_HEMOGLOBIN",
            "A1C",
            "GLYCOHEMOGLOBIN"
        )

        private val TRIGLYCERIDES_KEYS = setOf(
            "TRIGLYCERIDES",
            "TG",
            "TRIGS",
            "FASTING_TRIGLYCERIDES"
        )

        private val LDL_KEYS = setOf(
            "LDL",
            "LDL_C",
            "LDL_CHOLESTEROL",
            "DIRECT_LDL",
            "LOW_DENSITY_LIPOPROTEIN"
        )

        private val HDL_KEYS = setOf(
            "HDL",
            "HDL_C",
            "HDL_CHOLESTEROL",
            "HIGH_DENSITY_LIPOPROTEIN"
        )

        private val HEMOGLOBIN_KEYS = setOf(
            "HEMOGLOBIN",
            "HB",
            "HGB"
        )
    }
}
