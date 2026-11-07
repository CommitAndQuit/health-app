package io.biotrace.app.domain.clinical.benchmarks

import io.biotrace.app.domain.clinical.rules.ClinicalStatus
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Stage-level evaluation against National Sleep Foundation normative distributions.
 */
data class SleepStageEvaluation(
    val stageName: String,
    val actualPercentage: Double,
    val targetRange: ClosedFloatingPointRange<Double>,
    val status: ClinicalStatus,
    val isWithinNormativeRange: Boolean,
    val commentary: String
)

/**
 * Comprehensive sleep architecture distribution evaluation.
 */
data class SleepArchitectureEvaluation(
    val totalSleepMinutes: Double,
    val deepSleep: SleepStageEvaluation,
    val remSleep: SleepStageEvaluation,
    val lightSleep: SleepStageEvaluation,
    val isArchitectureBalanced: Boolean,
    val overallQualityRating: String,
    val narrative: String
)

/**
 * Deterministic sleep architecture evaluation engine benchmarking against National Sleep Foundation norms:
 * - Deep Sleep (N3 slow wave): 15% - 20% of Total Sleep Time (TST)
 * - REM Sleep: 20% - 25% of TST
 * - Light Sleep (N1 + N2): 50% - 60% of TST
 */
@Singleton
class SleepArchitectureBenchmark @Inject constructor() {

    /**
     * Evaluates sleep architecture given stage percentages.
     *
     * @param deepPercent Percentage of total sleep spent in deep / slow-wave sleep (0-100)
     * @param remPercent Percentage of total sleep spent in REM sleep (0-100)
     * @param lightPercent Percentage of total sleep spent in light sleep (0-100)
     * @param totalSleepMinutes Optional total sleep duration in minutes
     */
    fun evaluatePercentages(
        deepPercent: Double,
        remPercent: Double,
        lightPercent: Double,
        totalSleepMinutes: Double = 0.0
    ): SleepArchitectureEvaluation {
        val deepEval = evaluateDeep(deepPercent)
        val remEval = evaluateRem(remPercent)
        val lightEval = evaluateLight(lightPercent)

        val isBalanced = deepEval.isWithinNormativeRange &&
                remEval.isWithinNormativeRange &&
                lightEval.isWithinNormativeRange

        val overallRating = when {
            isBalanced -> "Optimal"
            deepPercent >= DEEP_MIN && remPercent >= REM_MIN && lightPercent <= 65.0 -> "Balanced"
            else -> "Imbalanced"
        }

        val narrative = buildNarrative(overallRating, deepEval, remEval, lightEval)

        return SleepArchitectureEvaluation(
            totalSleepMinutes = totalSleepMinutes,
            deepSleep = deepEval,
            remSleep = remEval,
            lightSleep = lightEval,
            isArchitectureBalanced = isBalanced,
            overallQualityRating = overallRating,
            narrative = narrative
        )
    }

    /**
     * Evaluates sleep architecture from raw stage durations in minutes.
     */
    fun evaluateFromMinutes(
        deepMinutes: Double,
        remMinutes: Double,
        lightMinutes: Double
    ): SleepArchitectureEvaluation {
        val totalMinutes = deepMinutes + remMinutes + lightMinutes
        if (totalMinutes <= 0.0) {
            return evaluatePercentages(0.0, 0.0, 0.0, 0.0)
        }

        val deepPct = ((deepMinutes / totalMinutes) * 1000.0).roundToInt() / 10.0
        val remPct = ((remMinutes / totalMinutes) * 1000.0).roundToInt() / 10.0
        val lightPct = ((lightMinutes / totalMinutes) * 1000.0).roundToInt() / 10.0

        return evaluatePercentages(deepPct, remPct, lightPct, totalMinutes)
    }

    private fun evaluateDeep(pct: Double): SleepStageEvaluation {
        val rounded = (pct * 10.0).roundToInt() / 10.0
        val withinRange = pct in DEEP_MIN..DEEP_MAX
        val (status, commentary) = when {
            pct < DEEP_MIN -> ClinicalStatus.BORDERLINE to
                    "Deep sleep is low ($rounded% vs norm 15-20%). Physical restoration and muscle recovery may be impaired."
            withinRange -> ClinicalStatus.OPTIMAL to
                    "Deep sleep is optimal ($rounded% vs norm 15-20%), supporting cellular repair and somatic recovery."
            else -> ClinicalStatus.NORMAL to
                    "Deep sleep is high ($rounded% vs norm 15-20%), typical following intense physical exertion."
        }

        return SleepStageEvaluation(
            stageName = "Deep Sleep",
            actualPercentage = rounded,
            targetRange = DEEP_MIN..DEEP_MAX,
            status = status,
            isWithinNormativeRange = withinRange,
            commentary = commentary
        )
    }

    private fun evaluateRem(pct: Double): SleepStageEvaluation {
        val rounded = (pct * 10.0).roundToInt() / 10.0
        val withinRange = pct in REM_MIN..REM_MAX
        val (status, commentary) = when {
            pct < REM_MIN -> ClinicalStatus.BORDERLINE to
                    "REM sleep is low ($rounded% vs norm 20-25%). Memory consolidation and emotional regulation may be impacted."
            withinRange -> ClinicalStatus.OPTIMAL to
                    "REM sleep is optimal ($rounded% vs norm 20-25%), promoting neural plasticity and memory consolidation."
            else -> ClinicalStatus.NORMAL to
                    "REM sleep is elevated ($rounded% vs norm 20-25%), often seen during REM rebound or recovery."
        }

        return SleepStageEvaluation(
            stageName = "REM Sleep",
            actualPercentage = rounded,
            targetRange = REM_MIN..REM_MAX,
            status = status,
            isWithinNormativeRange = withinRange,
            commentary = commentary
        )
    }

    private fun evaluateLight(pct: Double): SleepStageEvaluation {
        val rounded = (pct * 10.0).roundToInt() / 10.0
        val withinRange = pct in LIGHT_MIN..LIGHT_MAX
        val (status, commentary) = when {
            pct < LIGHT_MIN -> ClinicalStatus.BORDERLINE to
                    "Light sleep is below reference distribution ($rounded% vs norm 50-60%)."
            withinRange -> ClinicalStatus.OPTIMAL to
                    "Light sleep is within normative physiological distribution ($rounded% vs norm 50-60%)."
            else -> ClinicalStatus.BORDERLINE to
                    "Light sleep is high ($rounded% vs norm 50-60%), suggesting potentially fragmented or restless sleep."
        }

        return SleepStageEvaluation(
            stageName = "Light Sleep",
            actualPercentage = rounded,
            targetRange = LIGHT_MIN..LIGHT_MAX,
            status = status,
            isWithinNormativeRange = withinRange,
            commentary = commentary
        )
    }

    private fun buildNarrative(
        rating: String,
        deep: SleepStageEvaluation,
        rem: SleepStageEvaluation,
        light: SleepStageEvaluation
    ): String {
        return when (rating) {
            "Optimal" ->
                "Sleep architecture is optimal across all stages: Deep (${deep.actualPercentage}%), REM (${rem.actualPercentage}%), Light (${light.actualPercentage}%)."
            "Balanced" ->
                "Sleep architecture is balanced: Deep (${deep.actualPercentage}%) and REM (${rem.actualPercentage}%) satisfy minimum restorative thresholds."
            else -> {
                val issues = mutableListOf<String>()
                if (!deep.isWithinNormativeRange) issues.add("Deep ${deep.actualPercentage}%")
                if (!rem.isWithinNormativeRange) issues.add("REM ${rem.actualPercentage}%")
                if (!light.isWithinNormativeRange) issues.add("Light ${light.actualPercentage}%")
                "Sleep architecture is imbalanced: ${issues.joinToString(", ")} deviate from National Sleep Foundation normative ranges."
            }
        }
    }

    companion object {
        const val DEEP_MIN = 15.0
        const val DEEP_MAX = 20.0
        const val REM_MIN = 20.0
        const val REM_MAX = 25.0
        const val LIGHT_MIN = 50.0
        const val LIGHT_MAX = 60.0
    }
}
