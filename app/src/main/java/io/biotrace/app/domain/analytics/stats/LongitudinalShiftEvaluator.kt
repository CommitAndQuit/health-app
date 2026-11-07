package io.biotrace.app.domain.analytics.stats

import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Direction of biometric shift across longitudinal comparison windows.
 */
enum class ShiftDirection {
    INCREASED,
    DECREASED,
    STABLE
}

/**
 * Result of longitudinal time-series shift analysis comparing a baseline window to a current observation window.
 */
data class LongitudinalShiftResult(
    val baselineMean: Double,
    val baselineStdDev: Double,
    val currentMean: Double,
    val percentageShift: Double,
    val zScore: Double,
    val isSignificantShift: Boolean,
    val direction: ShiftDirection,
    val narrative: String
)

/**
 * Evaluates physiological baseline drifts and acute deviations between a historical baseline window
 * (e.g., 14-30 days) and a recent observation window (e.g., 7 days).
 *
 * Uses standard normal distribution critical boundaries (|z| >= 1.96, p < 0.05) and clinical delta thresholds.
 */
@Singleton
class LongitudinalShiftEvaluator @Inject constructor() {

    /**
     * Evaluates longitudinal biometric shift between baseline window and current window.
     *
     * @param baselineSeries Historical baseline values (e.g. past 14-30 days)
     * @param currentSeries Recent observation values (e.g. past 7 days)
     * @param clinicalDeltaThreshold Optional domain-specific clinical delta (e.g. 5.0 bpm for RHR)
     * @param zScoreThreshold Critical standard score threshold, defaults to 1.96 (95% two-tailed confidence)
     */
    fun evaluateShift(
        baselineSeries: List<Double>,
        currentSeries: List<Double>,
        clinicalDeltaThreshold: Double? = null,
        zScoreThreshold: Double = CRITICAL_Z_SCORE
    ): LongitudinalShiftResult {
        if (baselineSeries.isEmpty() || currentSeries.isEmpty()) {
            return LongitudinalShiftResult(
                baselineMean = 0.0,
                baselineStdDev = 0.0,
                currentMean = 0.0,
                percentageShift = 0.0,
                zScore = 0.0,
                isSignificantShift = false,
                direction = ShiftDirection.STABLE,
                narrative = "Insufficient observation data to compute longitudinal shift."
            )
        }

        val baseMean = RollingStatsCalculator.sma(baselineSeries)
        val baseStd = RollingStatsCalculator.stdDev(baselineSeries)
        val curMean = RollingStatsCalculator.sma(currentSeries)

        val percentageShift = if (baseMean != 0.0) {
            ((curMean - baseMean) / baseMean) * 100.0
        } else {
            0.0
        }

        val zScore = when {
            baseStd > 0.0 -> (curMean - baseMean) / baseStd
            curMean > baseMean -> Double.POSITIVE_INFINITY
            curMean < baseMean -> Double.NEGATIVE_INFINITY
            else -> 0.0
        }

        val diff = abs(curMean - baseMean)
        val exceedsZ = !zScore.isNaN() && abs(zScore) >= zScoreThreshold
        val exceedsDelta = clinicalDeltaThreshold != null && diff >= clinicalDeltaThreshold

        val isSignificant = exceedsZ || exceedsDelta

        val direction = when {
            curMean > baseMean + EPSILON -> ShiftDirection.INCREASED
            curMean < baseMean - EPSILON -> ShiftDirection.DECREASED
            else -> ShiftDirection.STABLE
        }

        val narrative = buildNarrative(
            curMean = curMean,
            baseMean = baseMean,
            percentageShift = percentageShift,
            zScore = zScore,
            isSignificant = isSignificant,
            direction = direction
        )

        return LongitudinalShiftResult(
            baselineMean = (baseMean * 100.0).roundToInt() / 100.0,
            baselineStdDev = (baseStd * 100.0).roundToInt() / 100.0,
            currentMean = (curMean * 100.0).roundToInt() / 100.0,
            percentageShift = (percentageShift * 10.0).roundToInt() / 10.0,
            zScore = if (zScore.isInfinite()) zScore else (zScore * 100.0).roundToInt() / 100.0,
            isSignificantShift = isSignificant,
            direction = direction,
            narrative = narrative
        )
    }

    private fun buildNarrative(
        curMean: Double,
        baseMean: Double,
        percentageShift: Double,
        zScore: Double,
        isSignificant: Boolean,
        direction: ShiftDirection
    ): String {
        val sign = if (percentageShift >= 0.0) "+" else ""
        val formattedCur = String.format(Locale.US, "%.1f", curMean)
        val formattedBase = String.format(Locale.US, "%.1f", baseMean)
        val formattedPct = String.format(Locale.US, "%s%.1f%%", sign, percentageShift)
        val formattedZ = if (zScore.isInfinite()) "infinite" else String.format(Locale.US, "%.2f", zScore)

        return if (isSignificant) {
            when (direction) {
                ShiftDirection.INCREASED ->
                    "Significant upward shift detected: current mean $formattedCur vs baseline $formattedBase ($formattedPct, z = $formattedZ)."
                ShiftDirection.DECREASED ->
                    "Significant downward shift detected: current mean $formattedCur vs baseline $formattedBase ($formattedPct, z = $formattedZ)."
                ShiftDirection.STABLE ->
                    "Statistically stable across windows (current $formattedCur vs baseline $formattedBase)."
            }
        } else {
            "Biometric metric remains stable within normal variance: current $formattedCur vs baseline $formattedBase ($formattedPct, z = $formattedZ)."
        }
    }

    companion object {
        const val CRITICAL_Z_SCORE = 1.96 // Standard 95% two-tailed confidence boundary
        private const val EPSILON = 0.001
    }
}
