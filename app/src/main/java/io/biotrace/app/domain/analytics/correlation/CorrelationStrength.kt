package io.biotrace.app.domain.analytics.correlation

/**
 * Categorical representation of statistical bivariate correlation strength.
 *
 * Threshold brackets:
 * - STRONG_POSITIVE: r >= 0.7
 * - MODERATE_POSITIVE: 0.4 <= r < 0.7
 * - WEAK_POSITIVE: 0.2 <= r < 0.4
 * - NEUTRAL: -0.2 < r < 0.2
 * - WEAK_NEGATIVE: -0.4 < r <= -0.2
 * - MODERATE_NEGATIVE: -0.7 < r <= -0.4
 * - STRONG_NEGATIVE: r <= -0.7
 */
data class CorrelationStrength(
    val label: String,
    val level: Int
) {
    companion object {
        val STRONG_POSITIVE = CorrelationStrength("Strong Positive", 3)
        val MODERATE_POSITIVE = CorrelationStrength("Moderate Positive", 2)
        val WEAK_POSITIVE = CorrelationStrength("Weak Positive", 1)
        val NEUTRAL = CorrelationStrength("Neutral / None", 0)
        val WEAK_NEGATIVE = CorrelationStrength("Weak Negative", -1)
        val MODERATE_NEGATIVE = CorrelationStrength("Moderate Negative", -2)
        val STRONG_NEGATIVE = CorrelationStrength("Strong Negative", -3)

        fun fromCoefficient(r: Double): CorrelationStrength = when {
            r.isNaN() -> NEUTRAL
            r >= 0.7 -> STRONG_POSITIVE
            r >= 0.4 -> MODERATE_POSITIVE
            r >= 0.2 -> WEAK_POSITIVE
            r > -0.2 -> NEUTRAL
            r > -0.4 -> WEAK_NEGATIVE
            r > -0.7 -> MODERATE_NEGATIVE
            else -> STRONG_NEGATIVE
        }
    }
}
