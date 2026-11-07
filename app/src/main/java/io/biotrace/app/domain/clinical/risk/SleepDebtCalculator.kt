package io.biotrace.app.domain.clinical.risk

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Autonomic strain severity tier associated with cumulative sleep deficit.
 */
enum class SleepDebtTier {
    OPTIMAL,
    MILD_STRAIN,
    MODERATE_STRAIN,
    SEVERE_STRAIN
}

/**
 * Output of rolling 14-day sleep deficit and autonomic recovery calculation.
 */
data class SleepDebtResult(
    val cumulativeDebtHours: Double,
    val allostaticAutonomicStrainScore: Double, // 0.0 to 100.0
    val recoveryRecommendation: String,
    val tier: SleepDebtTier
)

/**
 * Available-data clinical calculator for cumulative sleep deficit and autonomic recovery strain.
 *
 * Evaluates:
 * - Rolling 14-day cumulative sleep deficit against target (default 8.0h/day).
 * - Autonomic recovery strain factoring in HRV RMSSD depression relative to personal 30-day baseline.
 */
@Singleton
class SleepDebtCalculator @Inject constructor() {

    fun calculate(
        sleepDurationsHours: List<Double>,
        targetSleepHours: Double = 8.0,
        currentHrvRmssd: Double? = null,
        baselineHrvRmssd: Double? = null
    ): SleepDebtResult {
        require(targetSleepHours > 0.0) { "Target sleep hours must be positive." }

        // Rolling 14-day window
        val window = sleepDurationsHours.takeLast(14)

        // Compute cumulative sleep deficit with non-negative compensatory recovery
        val rawDebt = if (window.isEmpty()) {
            0.0
        } else {
            window.fold(0.0) { acc, sleep ->
                (acc + (targetSleepHours - sleep)).coerceAtLeast(0.0)
            }
        }
        val cumulativeDebtHours = (rawDebt * 10.0).roundToInt() / 10.0

        // Compute allostatic autonomic strain score (0 to 100)
        val hasHrv = baselineHrvRmssd != null && baselineHrvRmssd > 0.0 && currentHrvRmssd != null
        val rawStrain = if (hasHrv) {
            val sleepComponent = (cumulativeDebtHours / 20.0).coerceIn(0.0, 1.0) * 60.0
            val hrvDepression = ((baselineHrvRmssd!! - currentHrvRmssd!!).coerceAtLeast(0.0)) / baselineHrvRmssd
            val hrvComponent = (hrvDepression / 0.40).coerceIn(0.0, 1.0) * 40.0
            (sleepComponent + hrvComponent).coerceIn(0.0, 100.0)
        } else {
            // Scale sleep debt directly to 100 if autonomic vitals are absent
            (cumulativeDebtHours / 20.0).coerceIn(0.0, 1.0) * 100.0
        }

        val allostaticStrainScore = (rawStrain * 10.0).roundToInt() / 10.0

        val tier = when {
            allostaticStrainScore < 25.0 -> SleepDebtTier.OPTIMAL
            allostaticStrainScore < 50.0 -> SleepDebtTier.MILD_STRAIN
            allostaticStrainScore < 75.0 -> SleepDebtTier.MODERATE_STRAIN
            else -> SleepDebtTier.SEVERE_STRAIN
        }

        val recommendation = buildRecommendation(tier, cumulativeDebtHours, hasHrv)

        return SleepDebtResult(
            cumulativeDebtHours = cumulativeDebtHours,
            allostaticAutonomicStrainScore = allostaticStrainScore,
            recoveryRecommendation = recommendation,
            tier = tier
        )
    }

    private fun buildRecommendation(tier: SleepDebtTier, debtHours: Double, hasHrv: Boolean): String {
        return when (tier) {
            SleepDebtTier.OPTIMAL ->
                "Optimal autonomic balance and minimal sleep debt (${debtHours}h deficit over 14 days). Maintain consistent sleep-wake timing and morning natural light exposure."
            SleepDebtTier.MILD_STRAIN ->
                "Mild sleep debt detected (${debtHours}h deficit). Prioritize 30-45 minutes earlier sleep onset and limit evening blue spectrum lighting to preserve parasympathetic recovery."
            SleepDebtTier.MODERATE_STRAIN ->
                "Moderate autonomic recovery strain (${debtHours}h deficit). Extend nightly sleep opportunity by 60 minutes, avoid late caffeine or strenuous evening workouts, and favor active recovery."
            SleepDebtTier.SEVERE_STRAIN ->
                "Severe allostatic autonomic strain (${debtHours}h deficit${if (hasHrv) " with marked HRV depression" else ""}). Acute recovery required: target 9+ hours of sleep opportunity, minimize high-stress activities, and focus on parasympathetic rest."
        }
    }
}
