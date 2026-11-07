package io.biotrace.app.domain.clinical.rules

import java.util.Locale

/**
 * Validated medical clinical guideline rules catalog.
 * Encapsulates deterministic evaluation thresholds from AHA/ACC, WHO, ADA, and NCEP ATP III.
 */
object MedicalGuidelinesCatalog {

    const val SOURCE_AHA_ACC = "AHA/ACC Clinical Practice Guidelines"
    const val SOURCE_WHO_OXIMETRY = "WHO Pulse Oximetry Clinical Guidelines"
    const val SOURCE_ADA_2024 = "ADA Standards of Care in Diabetes 2024"
    const val SOURCE_NCEP_ATP_III = "NCEP ATP III / AHA Cholesterol Guidelines"
    const val SOURCE_WHO_ANEMIA = "WHO Haemoglobin Concentrations for Diagnosis of Anaemia"

    private fun isFemale(sex: String?): Boolean {
        val s = sex?.trim()?.lowercase(Locale.ROOT) ?: return false
        return s == "f" || s == "female" || s == "woman"
    }

    /**
     * AHA/ACC Resting Heart Rate Rule:
     * - Bradycardia (< 50 bpm waking)
     * - Normal (50 - 85 bpm)
     * - Elevated (86 - 100 bpm)
     * - Tachycardia (> 100 bpm resting)
     */
    val restingHeartRateRule: ClinicalRule = object : ClinicalRule {
        override val id: String = "AHA_ACC_RESTING_HEART_RATE"
        override val name: String = "Resting Heart Rate"
        override val guidelineSource: String = SOURCE_AHA_ACC

        override fun evaluate(value: Double): ClinicalEvaluation {
            val rounded = (value * 10.0).let { Math.round(it) / 10.0 }
            val (status, narrative) = when {
                value < 50.0 -> ClinicalStatus.BORDERLINE to
                        "Resting heart rate of $rounded bpm is below normal waking range, indicating bradycardia (<50 bpm)."
                value <= 85.0 -> ClinicalStatus.NORMAL to
                        "Resting heart rate of $rounded bpm is within the normal resting range (50-85 bpm)."
                value <= 100.0 -> ClinicalStatus.ELEVATED to
                        "Resting heart rate of $rounded bpm is elevated above normal resting baseline (86-100 bpm)."
                else -> ClinicalStatus.CRITICAL to
                        "Resting heart rate of $rounded bpm indicates resting tachycardia (>100 bpm)."
            }
            return ClinicalEvaluation(
                ruleId = id,
                metricName = name,
                status = status,
                value = rounded,
                unit = "bpm",
                referenceRange = "50-85 bpm",
                narrative = narrative,
                guidelineSource = guidelineSource
            )
        }
    }

    /**
     * WHO Pulse Oximetry Oxygen Saturation (SpO2) Rule:
     * - Normal (>= 95%)
     * - Mild Hypoxemia (90 - 94%)
     * - Critical Alert (< 90%)
     */
    val oxygenSaturationRule: ClinicalRule = object : ClinicalRule {
        override val id: String = "WHO_OXYGEN_SATURATION"
        override val name: String = "Oxygen Saturation"
        override val guidelineSource: String = SOURCE_WHO_OXIMETRY

        override fun evaluate(value: Double): ClinicalEvaluation {
            val rounded = (value * 10.0).let { Math.round(it) / 10.0 }
            val (status, narrative) = when {
                value >= 95.0 -> ClinicalStatus.NORMAL to
                        "Oxygen saturation of $rounded% is within normal reference range (≥95%)."
                value >= 90.0 -> ClinicalStatus.BORDERLINE to
                        "Oxygen saturation of $rounded% indicates mild hypoxemia (90-94%)."
                else -> ClinicalStatus.CRITICAL to
                        "Critical alert: Oxygen saturation of $rounded% indicates critical hypoxemia (<90%)."
            }
            return ClinicalEvaluation(
                ruleId = id,
                metricName = name,
                status = status,
                value = rounded,
                unit = "%",
                referenceRange = "≥95%",
                narrative = narrative,
                guidelineSource = guidelineSource
            )
        }
    }

    /**
     * ADA Fasting Blood Glucose Rule:
     * - Hypoglycemia (< 70 mg/dL)
     * - Normal (70 - 99 mg/dL)
     * - Pre-diabetes (100 - 125 mg/dL)
     * - Diabetes (>= 126 mg/dL)
     */
    val fastingGlucoseRule: ClinicalRule = object : ClinicalRule {
        override val id: String = "ADA_FASTING_GLUCOSE"
        override val name: String = "Fasting Blood Glucose"
        override val guidelineSource: String = SOURCE_ADA_2024

        override fun evaluate(value: Double): ClinicalEvaluation {
            val rounded = (value * 10.0).let { Math.round(it) / 10.0 }
            val (status, narrative) = when {
                value < 70.0 -> ClinicalStatus.CRITICAL to
                        "Fasting glucose of $rounded mg/dL indicates hypoglycemia (<70 mg/dL)."
                value <= 99.0 -> ClinicalStatus.NORMAL to
                        "Fasting glucose of $rounded mg/dL is within normal glycemic range (70-99 mg/dL)."
                value <= 125.0 -> ClinicalStatus.BORDERLINE to
                        "Fasting glucose of $rounded mg/dL indicates impaired fasting glucose / pre-diabetes (100-125 mg/dL)."
                else -> ClinicalStatus.CRITICAL to
                        "Fasting glucose of $rounded mg/dL meets or exceeds diagnostic threshold for diabetes (≥126 mg/dL)."
            }
            return ClinicalEvaluation(
                ruleId = id,
                metricName = name,
                status = status,
                value = rounded,
                unit = "mg/dL",
                referenceRange = "70-99 mg/dL",
                narrative = narrative,
                guidelineSource = guidelineSource
            )
        }
    }

    /**
     * ADA Glycated Hemoglobin (HbA1c) Rule:
     * - Normal (< 5.7%)
     * - Pre-diabetes (5.7 - 6.4%)
     * - Diabetes (>= 6.5%)
     */
    val hbA1cRule: ClinicalRule = object : ClinicalRule {
        override val id: String = "ADA_HBA1C"
        override val name: String = "Hemoglobin A1c"
        override val guidelineSource: String = SOURCE_ADA_2024

        override fun evaluate(value: Double): ClinicalEvaluation {
            val rounded = (value * 10.0).let { Math.round(it) / 10.0 }
            val (status, narrative) = when {
                value < 5.7 -> ClinicalStatus.NORMAL to
                        "HbA1c of $rounded% is within normal reference range (<5.7%)."
                value <= 6.4 -> ClinicalStatus.BORDERLINE to
                        "HbA1c of $rounded% indicates pre-diabetes (5.7-6.4%)."
                else -> ClinicalStatus.CRITICAL to
                        "HbA1c of $rounded% meets diagnostic criteria for diabetes (≥6.5%)."
            }
            return ClinicalEvaluation(
                ruleId = id,
                metricName = name,
                status = status,
                value = rounded,
                unit = "%",
                referenceRange = "<5.7%",
                narrative = narrative,
                guidelineSource = guidelineSource
            )
        }
    }

    /**
     * NCEP ATP III / AHA Fasting Triglycerides Rule:
     * - Normal (< 150 mg/dL)
     * - Borderline (150 - 199 mg/dL)
     * - High (200 - 499 mg/dL)
     * - Severe (>= 500 mg/dL)
     */
    val triglyceridesRule: ClinicalRule = object : ClinicalRule {
        override val id: String = "NCEP_TRIGLYCERIDES"
        override val name: String = "Fasting Triglycerides"
        override val guidelineSource: String = SOURCE_NCEP_ATP_III

        override fun evaluate(value: Double): ClinicalEvaluation {
            val rounded = (value * 10.0).let { Math.round(it) / 10.0 }
            val (status, narrative) = when {
                value < 150.0 -> ClinicalStatus.NORMAL to
                        "Fasting triglycerides of $rounded mg/dL is normal (<150 mg/dL)."
                value <= 199.0 -> ClinicalStatus.BORDERLINE to
                        "Fasting triglycerides of $rounded mg/dL is borderline high (150-199 mg/dL)."
                value <= 499.0 -> ClinicalStatus.ELEVATED to
                        "Fasting triglycerides of $rounded mg/dL is high (200-499 mg/dL)."
                else -> ClinicalStatus.CRITICAL to
                        "Fasting triglycerides of $rounded mg/dL is very high / severe (≥500 mg/dL), presenting risk for pancreatitis."
            }
            return ClinicalEvaluation(
                ruleId = id,
                metricName = name,
                status = status,
                value = rounded,
                unit = "mg/dL",
                referenceRange = "<150 mg/dL",
                narrative = narrative,
                guidelineSource = guidelineSource
            )
        }
    }

    /**
     * NCEP ATP III / AHA LDL Cholesterol Rule:
     * - Optimal (< 100 mg/dL)
     * - Near optimal (100 - 129 mg/dL)
     * - Borderline (130 - 159 mg/dL)
     * - High (160 - 189 mg/dL)
     * - Very High (>= 190 mg/dL)
     */
    val ldlCholesterolRule: ClinicalRule = object : ClinicalRule {
        override val id: String = "NCEP_LDL"
        override val name: String = "LDL Cholesterol"
        override val guidelineSource: String = SOURCE_NCEP_ATP_III

        override fun evaluate(value: Double): ClinicalEvaluation {
            val rounded = (value * 10.0).let { Math.round(it) / 10.0 }
            val (status, narrative) = when {
                value < 100.0 -> ClinicalStatus.OPTIMAL to
                        "LDL-C of $rounded mg/dL is optimal (<100 mg/dL)."
                value <= 129.0 -> ClinicalStatus.NORMAL to
                        "LDL-C of $rounded mg/dL is near optimal (100-129 mg/dL)."
                value <= 159.0 -> ClinicalStatus.BORDERLINE to
                        "LDL-C of $rounded mg/dL is borderline high (130-159 mg/dL)."
                value <= 189.0 -> ClinicalStatus.ELEVATED to
                        "LDL-C of $rounded mg/dL is high (160-189 mg/dL)."
                else -> ClinicalStatus.CRITICAL to
                        "LDL-C of $rounded mg/dL is very high (≥190 mg/dL), indicating substantial cardiovascular risk."
            }
            return ClinicalEvaluation(
                ruleId = id,
                metricName = name,
                status = status,
                value = rounded,
                unit = "mg/dL",
                referenceRange = "<100 mg/dL",
                narrative = narrative,
                guidelineSource = guidelineSource
            )
        }
    }

    /**
     * NCEP ATP III / AHA HDL Cholesterol Rule:
     * - Low / Risk (< 40 mg/dL male, < 50 mg/dL female)
     * - Acceptable (40-59 mg/dL male, 50-59 mg/dL female)
     * - Protective (>= 60 mg/dL)
     */
    val hdlCholesterolRule: ClinicalRule = object : ClinicalRule {
        override val id: String = "NCEP_HDL"
        override val name: String = "HDL Cholesterol"
        override val guidelineSource: String = SOURCE_NCEP_ATP_III

        override fun evaluate(value: Double): ClinicalEvaluation = evaluate(value, null)

        override fun evaluate(value: Double, biologicalSex: String?): ClinicalEvaluation {
            val rounded = (value * 10.0).let { Math.round(it) / 10.0 }
            val female = isFemale(biologicalSex)
            val thresholdLow = if (female) 50.0 else 40.0
            val refRange = if (female) "≥50 mg/dL (female)" else "≥40 mg/dL (male)"

            val (status, narrative) = when {
                value >= 60.0 -> ClinicalStatus.OPTIMAL to
                        "HDL-C of $rounded mg/dL is optimal and cardioprotective (≥60 mg/dL)."
                value < thresholdLow -> ClinicalStatus.BORDERLINE to
                        "HDL-C of $rounded mg/dL is low (<$thresholdLow mg/dL for ${if (female) "females" else "males"}), presenting cardiovascular risk."
                else -> ClinicalStatus.NORMAL to
                        "HDL-C of $rounded mg/dL is within acceptable clinical range."
            }
            return ClinicalEvaluation(
                ruleId = id,
                metricName = name,
                status = status,
                value = rounded,
                unit = "mg/dL",
                referenceRange = refRange,
                narrative = narrative,
                guidelineSource = guidelineSource
            )
        }
    }

    /**
     * WHO Hemoglobin Anemia Rule:
     * - Anemia (< 13.5 g/dL male, < 12.0 g/dL female)
     * - Severe anemia (< 8.0 g/dL)
     * - Normal (13.5-17.5 g/dL male, 12.0-15.5 g/dL female)
     */
    val hemoglobinRule: ClinicalRule = object : ClinicalRule {
        override val id: String = "WHO_HEMOGLOBIN"
        override val name: String = "Hemoglobin"
        override val guidelineSource: String = SOURCE_WHO_ANEMIA

        override fun evaluate(value: Double): ClinicalEvaluation = evaluate(value, null)

        override fun evaluate(value: Double, biologicalSex: String?): ClinicalEvaluation {
            val rounded = (value * 10.0).let { Math.round(it) / 10.0 }
            val female = isFemale(biologicalSex)
            val lowerThreshold = if (female) 12.0 else 13.5
            val upperThreshold = if (female) 15.5 else 17.5
            val sexLabel = if (female) "female" else "male"
            val refRange = "$lowerThreshold-$upperThreshold g/dL ($sexLabel)"

            val (status, narrative) = when {
                value < 8.0 -> ClinicalStatus.CRITICAL to
                        "Critical alert: Hemoglobin of $rounded g/dL indicates severe anemia (<8.0 g/dL)."
                value < lowerThreshold -> ClinicalStatus.BORDERLINE to
                        "Hemoglobin of $rounded g/dL indicates anemia (<$lowerThreshold g/dL for ${sexLabel}s)."
                value <= upperThreshold -> ClinicalStatus.NORMAL to
                        "Hemoglobin of $rounded g/dL is within normal range ($refRange)."
                else -> ClinicalStatus.ELEVATED to
                        "Hemoglobin of $rounded g/dL is elevated above normal upper limit ($upperThreshold g/dL)."
            }

            return ClinicalEvaluation(
                ruleId = id,
                metricName = name,
                status = status,
                value = rounded,
                unit = "g/dL",
                referenceRange = refRange,
                narrative = narrative,
                guidelineSource = guidelineSource
            )
        }
    }
}
