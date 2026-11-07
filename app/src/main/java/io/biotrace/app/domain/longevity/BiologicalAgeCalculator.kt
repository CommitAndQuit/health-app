package io.biotrace.app.domain.longevity

import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Pure domain calculation engine for biological age and phenotypic longevity index.
 * Derives phenotypic biological age adjustments using validated clinical biomarkers
 * and cardiovascular/autonomic vitals.
 */
@Singleton
class BiologicalAgeCalculator @Inject constructor() {

    fun calculate(
        chronologicalAge: Int,
        restingHrBpm: Int?,
        sleepDurationHours: Double?,
        sleepScore: Int?,
        labBiomarkers: Map<String, Double>
    ): BiologicalAgeResult {
        val safeChronologicalAge = chronologicalAge.coerceIn(MIN_CHRONO_AGE, MAX_CHRONO_AGE)
        var totalDelta = 0.0
        val drivers = mutableListOf<LongevityDriver>()

        // 1. Fasting Glucose adjustment (Normal: 70 - 99 mg/dL)
        labBiomarkers["FASTING_GLUCOSE"]?.let { glucose ->
            when {
                glucose > 100.0 -> {
                    val impact = min(3.5, 0.05 * (glucose - 100.0))
                    val rounded = (impact * 10.0).roundToInt() / 10.0
                    totalDelta += rounded
                    val formatted = if (glucose % 1.0 == 0.0) "${glucose.toInt()} mg/dL" else "%.1f mg/dL".format(Locale.ROOT, glucose)
                    drivers.add(
                        LongevityDriver(
                            title = "Elevated Fasting Glucose",
                            impactYears = rounded,
                            biomarkerValue = formatted,
                            insight = "Accelerates cellular glycation and vascular aging",
                            isBonus = false
                        )
                    )
                }
                glucose <= 90.0 -> {
                    val impact = -0.8
                    totalDelta += impact
                    val formatted = if (glucose % 1.0 == 0.0) "${glucose.toInt()} mg/dL" else "%.1f mg/dL".format(Locale.ROOT, glucose)
                    drivers.add(
                        LongevityDriver(
                            title = "Optimal Fasting Glucose",
                            impactYears = impact,
                            biomarkerValue = formatted,
                            insight = "Vascular protection",
                            isBonus = true
                        )
                    )
                }
            }
        }

        // 2. HbA1c adjustment (Normal: 4.0 - 5.6%)
        labBiomarkers["HBA1C"]?.let { hba1c ->
            when {
                hba1c > 5.7 -> {
                    val impact = min(4.0, 1.5 * (hba1c - 5.6))
                    val rounded = (impact * 10.0).roundToInt() / 10.0
                    totalDelta += rounded
                    drivers.add(
                        LongevityDriver(
                            title = "Elevated HbA1c",
                            impactYears = rounded,
                            biomarkerValue = "%.1f%%".format(Locale.ROOT, hba1c),
                            insight = "Prolonged glycation accelerates vascular aging",
                            isBonus = false
                        )
                    )
                }
                hba1c <= 5.2 -> {
                    val impact = -0.9
                    totalDelta += impact
                    drivers.add(
                        LongevityDriver(
                            title = "Optimal Glycemic Reserve",
                            impactYears = impact,
                            biomarkerValue = "%.1f%%".format(Locale.ROOT, hba1c),
                            insight = "Healthy glucose regulation confers longevity protection",
                            isBonus = true
                        )
                    )
                }
            }
        }

        // 3. hs-CRP systemic inflammation adjustment (Normal: < 1.0 mg/L)
        val hsCrp = labBiomarkers["HS_CRP"] ?: labBiomarkers["CRP"]
        if (hsCrp != null) {
            when {
                hsCrp > 2.0 -> {
                    val impact = min(3.0, 1.0 * ln(hsCrp))
                    val rounded = (impact * 10.0).roundToInt() / 10.0
                    totalDelta += rounded
                    drivers.add(
                        LongevityDriver(
                            title = "Systemic Inflammation (hs-CRP)",
                            impactYears = rounded,
                            biomarkerValue = "%.2f mg/L".format(Locale.ROOT, hsCrp),
                            insight = "Elevated inflammatory burden accelerates cellular senescence",
                            isBonus = false
                        )
                    )
                }
                hsCrp < 0.8 -> {
                    val impact = -1.2
                    totalDelta += impact
                    drivers.add(
                        LongevityDriver(
                            title = "Low Systemic Inflammation",
                            impactYears = impact,
                            biomarkerValue = "%.2f mg/L".format(Locale.ROOT, hsCrp),
                            insight = "Low systemic inflammation",
                            isBonus = true
                        )
                    )
                }
            }
        }

        // 4. Albumin adjustment (Normal: 3.5 - 5.0 g/dL)
        labBiomarkers["ALBUMIN"]?.let { albumin ->
            when {
                albumin >= 4.4 -> {
                    val impact = -0.7
                    totalDelta += impact
                    drivers.add(
                        LongevityDriver(
                            title = "High Serum Albumin",
                            impactYears = impact,
                            biomarkerValue = "%.1f g/dL".format(Locale.ROOT, albumin),
                            insight = "Liver & plasma reserve",
                            isBonus = true
                        )
                    )
                }
                albumin < 3.8 -> {
                    val impact = 1.0
                    totalDelta += impact
                    drivers.add(
                        LongevityDriver(
                            title = "Reduced Serum Albumin",
                            impactYears = impact,
                            biomarkerValue = "%.1f g/dL".format(Locale.ROOT, albumin),
                            insight = "Suboptimal liver synthesis capacity and protein reserve",
                            isBonus = false
                        )
                    )
                }
            }
        }

        // 5. Lipid Panel: TG/HDL ratio or HDL
        val tg = labBiomarkers["TRIGLYCERIDES"]
        val hdl = labBiomarkers["HDL"]
        val tgHdlRatio = if (tg != null && hdl != null && hdl > 0.0) tg / hdl else null
        val isFavorableLipid = (tgHdlRatio != null && tgHdlRatio < 2.0) || (hdl != null && hdl > 55.0)

        if (isFavorableLipid) {
            val impact = -0.6
            totalDelta += impact
            val detailStr = when {
                tgHdlRatio != null && hdl != null -> "TG/HDL ${"%.1f".format(Locale.ROOT, tgHdlRatio)}, HDL ${hdl.toInt()} mg/dL"
                hdl != null -> "HDL ${hdl.toInt()} mg/dL"
                else -> "Favorable lipids"
            }
            drivers.add(
                LongevityDriver(
                    title = "Cardioprotective Lipid Profile",
                    impactYears = impact,
                    biomarkerValue = detailStr,
                    insight = "Cardioprotective lipid metabolism and vascular resilience",
                    isBonus = true
                )
            )
        }

        // 6. Creatinine adjustment (Normal: 0.7 - 1.2 mg/dL)
        labBiomarkers["CREATININE"]?.let { creatinine ->
            if (creatinine > 1.3) {
                val impact = 1.2
                totalDelta += impact
                drivers.add(
                    LongevityDriver(
                        title = "Elevated Serum Creatinine",
                        impactYears = impact,
                        biomarkerValue = "%.2f mg/dL".format(Locale.ROOT, creatinine),
                        insight = "Elevated filtration marker suggests renal strain",
                        isBonus = false
                    )
                )
            }
        }

        // 7. Cardiovascular: Resting Heart Rate (bpm)
        if (restingHrBpm != null) {
            when {
                restingHrBpm < 60 -> {
                    val impact = -1.4
                    totalDelta += impact
                    drivers.add(
                        LongevityDriver(
                            title = "Resting Heart Rate",
                            impactYears = impact,
                            biomarkerValue = "$restingHrBpm bpm",
                            insight = "Superior cardiovascular stroke volume",
                            isBonus = true
                        )
                    )
                }
                restingHrBpm in 60..70 -> {
                    val impact = -0.5
                    totalDelta += impact
                    drivers.add(
                        LongevityDriver(
                            title = "Resting Heart Rate",
                            impactYears = impact,
                            biomarkerValue = "$restingHrBpm bpm",
                            insight = "Efficient cardiovascular recovery and autonomic balance",
                            isBonus = true
                        )
                    )
                }
                restingHrBpm > 75 -> {
                    val impact = min(2.5, 0.08 * (restingHrBpm - 75))
                    val rounded = (impact * 10.0).roundToInt() / 10.0
                    totalDelta += rounded
                    drivers.add(
                        LongevityDriver(
                            title = "Elevated Resting Heart Rate",
                            impactYears = rounded,
                            biomarkerValue = "$restingHrBpm bpm",
                            insight = "Elevated resting pulse indicates autonomic or cardiovascular strain",
                            isBonus = false
                        )
                    )
                }
            }
        }

        // 8. Sleep Duration (hours)
        if (sleepDurationHours != null) {
            when {
                sleepDurationHours in 7.0..8.5 -> {
                    val impact = -0.8
                    totalDelta += impact
                    drivers.add(
                        LongevityDriver(
                            title = "Optimal Sleep Duration",
                            impactYears = impact,
                            biomarkerValue = "%.1f hrs".format(Locale.ROOT, sleepDurationHours),
                            insight = "Optimal cellular repair window",
                            isBonus = true
                        )
                    )
                }
                sleepDurationHours < 6.0 -> {
                    val impact = 1.2
                    totalDelta += impact
                    drivers.add(
                        LongevityDriver(
                            title = "Insufficient Sleep Duration",
                            impactYears = impact,
                            biomarkerValue = "%.1f hrs".format(Locale.ROOT, sleepDurationHours),
                            insight = "Sleep debt strain",
                            isBonus = false
                        )
                    )
                }
            }
        }

        // Derived calculations
        val rawBiologicalAge = (safeChronologicalAge + totalDelta).coerceIn(MIN_BIO_AGE, MAX_BIO_AGE)
        val biologicalAge = (rawBiologicalAge * 10.0).roundToInt() / 10.0
        val ageDelta = ((biologicalAge - safeChronologicalAge) * 10.0).roundToInt() / 10.0
        val agingPace = if (safeChronologicalAge > 0) {
            ((biologicalAge / safeChronologicalAge.toDouble()) * 100.0).roundToInt() / 100.0
        } else {
            1.0
        }

        val longevityTier = when {
            ageDelta <= -2.5 -> LongevityTier.OPTIMAL
            ageDelta <= 0.5 -> LongevityTier.BALANCED
            ageDelta <= 3.0 -> LongevityTier.MILD_ACCELERATION
            else -> LongevityTier.ELEVATED_STRAIN
        }

        // Data completeness: base 50% for vitals, +10% per available lab biomarker, max 95%
        val vitalsPresent = restingHrBpm != null || sleepDurationHours != null || sleepScore != null
        val baseConfidence = if (vitalsPresent) 50 else 30
        val labCount = labBiomarkers.size
        val confidencePercentage = (baseConfidence + labCount * 10).coerceIn(20, 95)

        // Sort drivers by absolute magnitude of impact descending
        val sortedDrivers = drivers.sortedByDescending { kotlin.math.abs(it.impactYears) }

        // Primary recommendation
        val largestPenalty = drivers.filter { !it.isBonus }.maxByOrNull { it.impactYears }
        val primaryRecommendation = when {
            largestPenalty != null -> {
                when (largestPenalty.title) {
                    "Elevated Fasting Glucose" ->
                        "Prioritize post-meal glycemic management through 10-15 minute postprandial walks and a nutrient-dense, low-glycemic dietary approach to reduce tissue glycation."
                    "Elevated HbA1c" ->
                        "Target glycemic stability and insulin sensitivity through consistent aerobic exercise and minimizing refined carbohydrates."
                    "Systemic Inflammation (hs-CRP)" ->
                        "Target systemic inflammation reduction with anti-inflammatory nutrition, omega-3 fatty acids, and addressing recovery stressors."
                    "Elevated Resting Heart Rate" ->
                        "Enhance cardiovascular reserve with progressive Zone 2 aerobic conditioning and daily autonomic down-regulation routines."
                    "Insufficient Sleep Duration" ->
                        "Prioritize circadian alignment and target 7 to 8.5 hours of restorative sleep to restore autonomic balance and cellular repair."
                    "Elevated Serum Creatinine" ->
                        "Support renal hemodynamics with consistent daily hydration and review kidney filtration markers with your healthcare provider."
                    "Reduced Serum Albumin" ->
                        "Optimize dietary protein intake and liver metabolic support to sustain plasma albumin and cellular reserve."
                    else ->
                        "Address lifestyle strain factors by focusing on balanced aerobic activity, restorative sleep, and nutrient-dense nutrition."
                }
            }
            longevityTier == LongevityTier.OPTIMAL ->
                "Outstanding biological resilience. Your cardiovascular habits and metabolic biomarkers indicate an optimal longevity pace—continue your current lifestyle routine."
            else ->
                "Your biomarkers reflect a stable biological pace. Continue reinforcing consistent physical activity, sleep hygiene, and balanced nutrition."
        }

        return BiologicalAgeResult(
            chronologicalAge = safeChronologicalAge,
            biologicalAge = biologicalAge,
            ageDelta = ageDelta,
            agingPace = agingPace,
            longevityTier = longevityTier,
            confidencePercentage = confidencePercentage,
            drivers = sortedDrivers,
            primaryRecommendation = primaryRecommendation
        )
    }

    companion object {
        const val MIN_CHRONO_AGE = 18
        const val MAX_CHRONO_AGE = 120
        const val MIN_BIO_AGE = 18.0
        const val MAX_BIO_AGE = 110.0
    }
}
