package io.biotrace.app.domain.clinical.risk

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Risk tier categorization for atherogenic dyslipidemia ratios.
 */
enum class AtherogenicRiskTier {
    OPTIMAL,
    MODERATE_RISK,
    HIGH_RISK
}

/**
 * Output of atherogenic lipid risk calculation.
 */
data class AtherogenicRiskResult(
    val overallRiskTier: AtherogenicRiskTier,
    val tgHdlRatio: Double,
    val nonHdlCholesterol: Double,
    val tcHdlRatio: Double,
    val tgHdlTier: AtherogenicRiskTier,
    val nonHdlTier: AtherogenicRiskTier,
    val tcHdlTier: AtherogenicRiskTier,
    val narrative: String
)

/**
 * Available-data clinical calculator for atherogenic dyslipidemia.
 *
 * Evaluates:
 * - Triglyceride / HDL ratio (TG/HDL): Optimal (< 2.0), Moderate (2.0 - 3.0), Elevated (> 3.0).
 * - Non-HDL Cholesterol (Total Cholesterol - HDL): Optimal (< 130 mg/dL), Moderate (130 - 159 mg/dL), Elevated (>= 160 mg/dL).
 * - Total Cholesterol / HDL ratio (TC/HDL): Optimal (< 3.5), Moderate (3.5 - 5.0), Elevated (> 5.0).
 */
@Singleton
class AtherogenicRiskCalculator @Inject constructor() {

    fun calculate(
        totalCholesterolMgDl: Double,
        hdlMgDl: Double,
        triglyceridesMgDl: Double
    ): AtherogenicRiskResult {
        require(hdlMgDl > 0.0) { "HDL cholesterol must be greater than zero." }
        require(totalCholesterolMgDl >= 0.0) { "Total cholesterol cannot be negative." }
        require(triglyceridesMgDl >= 0.0) { "Triglycerides cannot be negative." }

        val rawTgHdl = triglyceridesMgDl / hdlMgDl
        val rawNonHdl = (totalCholesterolMgDl - hdlMgDl).coerceAtLeast(0.0)
        val rawTcHdl = totalCholesterolMgDl / hdlMgDl

        val tgHdlRatio = (rawTgHdl * 100.0).roundToInt() / 100.0
        val nonHdlCholesterol = (rawNonHdl * 10.0).roundToInt() / 10.0
        val tcHdlRatio = (rawTcHdl * 100.0).roundToInt() / 100.0

        val tgHdlTier = when {
            tgHdlRatio < 2.0 -> AtherogenicRiskTier.OPTIMAL
            tgHdlRatio <= 3.0 -> AtherogenicRiskTier.MODERATE_RISK
            else -> AtherogenicRiskTier.HIGH_RISK
        }

        val nonHdlTier = when {
            nonHdlCholesterol < 130.0 -> AtherogenicRiskTier.OPTIMAL
            nonHdlCholesterol < 160.0 -> AtherogenicRiskTier.MODERATE_RISK
            else -> AtherogenicRiskTier.HIGH_RISK
        }

        val tcHdlTier = when {
            tcHdlRatio < 3.5 -> AtherogenicRiskTier.OPTIMAL
            tcHdlRatio <= 5.0 -> AtherogenicRiskTier.MODERATE_RISK
            else -> AtherogenicRiskTier.HIGH_RISK
        }

        val overallTier = when {
            tgHdlTier == AtherogenicRiskTier.HIGH_RISK ||
                nonHdlTier == AtherogenicRiskTier.HIGH_RISK ||
                tcHdlTier == AtherogenicRiskTier.HIGH_RISK -> AtherogenicRiskTier.HIGH_RISK

            tgHdlTier == AtherogenicRiskTier.MODERATE_RISK ||
                nonHdlTier == AtherogenicRiskTier.MODERATE_RISK ||
                tcHdlTier == AtherogenicRiskTier.MODERATE_RISK -> AtherogenicRiskTier.MODERATE_RISK

            else -> AtherogenicRiskTier.OPTIMAL
        }

        val narrative = buildNarrative(
            overallTier = overallTier,
            tgHdlRatio = tgHdlRatio,
            tgHdlTier = tgHdlTier,
            nonHdl = nonHdlCholesterol,
            nonHdlTier = nonHdlTier,
            tcHdlRatio = tcHdlRatio,
            tcHdlTier = tcHdlTier
        )

        return AtherogenicRiskResult(
            overallRiskTier = overallTier,
            tgHdlRatio = tgHdlRatio,
            nonHdlCholesterol = nonHdlCholesterol,
            tcHdlRatio = tcHdlRatio,
            tgHdlTier = tgHdlTier,
            nonHdlTier = nonHdlTier,
            tcHdlTier = tcHdlTier,
            narrative = narrative
        )
    }

    private fun buildNarrative(
        overallTier: AtherogenicRiskTier,
        tgHdlRatio: Double,
        tgHdlTier: AtherogenicRiskTier,
        nonHdl: Double,
        nonHdlTier: AtherogenicRiskTier,
        tcHdlRatio: Double,
        tcHdlTier: AtherogenicRiskTier
    ): String {
        val summary = when (overallTier) {
            AtherogenicRiskTier.OPTIMAL ->
                "Atherogenic lipid profile is optimal. Circulating particle density and insulin resistance markers are within healthy physiological ranges."
            AtherogenicRiskTier.MODERATE_RISK ->
                "Moderate atherogenic dyslipidemia detected. Ratios indicate mild elevation in atherogenic remnant particles or sub-optimal clearance."
            AtherogenicRiskTier.HIGH_RISK ->
                "High atherogenic cardiovascular risk indicated. Pattern suggests elevated small dense LDL particles and impaired reverse cholesterol transport."
        }

        val details = "TG/HDL: $tgHdlRatio ($tgHdlTier), Non-HDL: ${nonHdl.toInt()} mg/dL ($nonHdlTier), TC/HDL: $tcHdlRatio ($tcHdlTier)."
        return "$summary $details"
    }
}
