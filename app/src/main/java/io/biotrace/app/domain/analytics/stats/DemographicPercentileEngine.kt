package io.biotrace.app.domain.analytics.stats

import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Result of demographic percentile normalization.
 */
data class DemographicPercentileResult(
    val metricName: String,
    val value: Double,
    val age: Int,
    val sex: String,
    val ageBracket: String,
    val percentile: Double,
    val ratingCategory: String,
    val narrative: String
)

/**
 * Deterministic engine providing age- and biological sex-stratified population percentile rankings.
 * Reference curves derived from:
 * - AHA / Cooper Institute Cardiorespiratory Fitness data (Resting Heart Rate)
 * - Published adult population normative HRV datasets (Nunan et al., Tegegne et al. for RMSSD)
 */
@Singleton
class DemographicPercentileEngine @Inject constructor() {

    /**
     * Calculates the cardiorespiratory fitness percentile for resting heart rate (bpm).
     * In resting heart rate, lower bpm reflects higher cardiovascular conditioning (higher percentile rank).
     */
    fun calculateRestingHrPercentile(
        bpm: Double,
        age: Int,
        sex: String?
    ): DemographicPercentileResult {
        val normalizedSex = normalizeSex(sex)
        val bracket = resolveAgeBracket(age)
        val anchors = if (normalizedSex == "female") {
            RHR_FEMALE_ANCHORS[bracket] ?: RHR_FEMALE_ANCHORS["30s"]!!
        } else {
            RHR_MALE_ANCHORS[bracket] ?: RHR_MALE_ANCHORS["30s"]!!
        }

        val rawPercentile = interpolateDecreasing(bpm, anchors)
        val percentile = (rawPercentile * 10.0).roundToInt() / 10.0
        val category = resolveCategory(percentile)
        val narrative = "Resting heart rate of ${bpm.roundToInt()} bpm ranks in the " +
                "${percentile.toInt()}th percentile ($category) for ${normalizedSex}s aged $bracket."

        return DemographicPercentileResult(
            metricName = "Resting Heart Rate",
            value = bpm,
            age = age,
            sex = normalizedSex,
            ageBracket = bracket,
            percentile = percentile,
            ratingCategory = category,
            narrative = narrative
        )
    }

    /**
     * Calculates the autonomic nervous system percentile for Heart Rate Variability (HRV) RMSSD (ms).
     * In HRV RMSSD, higher values reflect stronger vagal tone / parasympathetic recovery (higher percentile rank).
     */
    fun calculateHrvRmssdPercentile(
        rmssd: Double,
        age: Int,
        sex: String?
    ): DemographicPercentileResult {
        val normalizedSex = normalizeSex(sex)
        val bracket = resolveAgeBracket(age)
        val anchors = if (normalizedSex == "female") {
            HRV_FEMALE_ANCHORS[bracket] ?: HRV_FEMALE_ANCHORS["30s"]!!
        } else {
            HRV_MALE_ANCHORS[bracket] ?: HRV_MALE_ANCHORS["30s"]!!
        }

        val rawPercentile = interpolateIncreasing(rmssd, anchors)
        val percentile = (rawPercentile * 10.0).roundToInt() / 10.0
        val category = resolveCategory(percentile)
        val narrative = "HRV RMSSD of ${(rmssd * 10.0).roundToInt() / 10.0} ms ranks in the " +
                "${percentile.toInt()}th percentile ($category) for ${normalizedSex}s aged $bracket."

        return DemographicPercentileResult(
            metricName = "HRV RMSSD",
            value = rmssd,
            age = age,
            sex = normalizedSex,
            ageBracket = bracket,
            percentile = percentile,
            ratingCategory = category,
            narrative = narrative
        )
    }

    private fun resolveAgeBracket(age: Int): String = when {
        age < 30 -> "20s"
        age < 40 -> "30s"
        age < 50 -> "40s"
        age < 60 -> "50s"
        else -> "60s+"
    }

    private fun normalizeSex(sex: String?): String {
        val s = sex?.trim()?.lowercase(Locale.ROOT) ?: return "male"
        return if (s == "female" || s == "f" || s == "woman") "female" else "male"
    }

    private fun resolveCategory(percentile: Double): String = when {
        percentile >= 90.0 -> "Excellent"
        percentile >= 70.0 -> "Good"
        percentile >= 50.0 -> "Above Average"
        percentile >= 30.0 -> "Average"
        percentile >= 15.0 -> "Below Average"
        else -> "Poor"
    }

    private fun interpolateDecreasing(value: Double, anchors: List<Pair<Double, Double>>): Double {
        if (value <= anchors.first().first) return anchors.first().second
        if (value >= anchors.last().first) return anchors.last().second
        for (i in 0 until anchors.size - 1) {
            val (v1, p1) = anchors[i]
            val (v2, p2) = anchors[i + 1]
            if (value in v1..v2) {
                val fraction = (value - v1) / (v2 - v1)
                return p1 + fraction * (p2 - p1)
            }
        }
        return anchors.last().second
    }

    private fun interpolateIncreasing(value: Double, anchors: List<Pair<Double, Double>>): Double {
        if (value <= anchors.first().first) return anchors.first().second
        if (value >= anchors.last().first) return anchors.last().second
        for (i in 0 until anchors.size - 1) {
            val (v1, p1) = anchors[i]
            val (v2, p2) = anchors[i + 1]
            if (value in v1..v2) {
                val fraction = (value - v1) / (v2 - v1)
                return p1 + fraction * (p2 - p1)
            }
        }
        return anchors.last().second
    }

    companion object {
        // Pairs of (bpm, percentile rank) - ordered by bpm ascending (percentile decreasing)
        private val RHR_MALE_ANCHORS = mapOf(
            "20s" to listOf(48.0 to 99.0, 55.0 to 90.0, 61.0 to 70.0, 65.0 to 50.0, 70.0 to 30.0, 75.0 to 15.0, 82.0 to 5.0, 95.0 to 1.0),
            "30s" to listOf(49.0 to 99.0, 56.0 to 90.0, 62.0 to 70.0, 66.0 to 50.0, 71.0 to 30.0, 76.0 to 15.0, 83.0 to 5.0, 96.0 to 1.0),
            "40s" to listOf(50.0 to 99.0, 57.0 to 90.0, 63.0 to 70.0, 67.0 to 50.0, 72.0 to 30.0, 77.0 to 15.0, 84.0 to 5.0, 97.0 to 1.0),
            "50s" to listOf(50.0 to 99.0, 58.0 to 90.0, 64.0 to 70.0, 68.0 to 50.0, 73.0 to 30.0, 78.0 to 15.0, 85.0 to 5.0, 98.0 to 1.0),
            "60s+" to listOf(50.0 to 99.0, 58.0 to 90.0, 64.0 to 70.0, 69.0 to 50.0, 74.0 to 30.0, 79.0 to 15.0, 86.0 to 5.0, 98.0 to 1.0)
        )

        private val RHR_FEMALE_ANCHORS = mapOf(
            "20s" to listOf(52.0 to 99.0, 60.0 to 90.0, 66.0 to 70.0, 70.0 to 50.0, 75.0 to 30.0, 80.0 to 15.0, 87.0 to 5.0, 98.0 to 1.0),
            "30s" to listOf(53.0 to 99.0, 61.0 to 90.0, 67.0 to 70.0, 71.0 to 50.0, 76.0 to 30.0, 81.0 to 15.0, 88.0 to 5.0, 99.0 to 1.0),
            "40s" to listOf(54.0 to 99.0, 62.0 to 90.0, 68.0 to 70.0, 72.0 to 50.0, 77.0 to 30.0, 82.0 to 15.0, 89.0 to 5.0, 100.0 to 1.0),
            "50s" to listOf(54.0 to 99.0, 63.0 to 90.0, 69.0 to 70.0, 73.0 to 50.0, 78.0 to 30.0, 83.0 to 15.0, 90.0 to 5.0, 101.0 to 1.0),
            "60s+" to listOf(54.0 to 99.0, 63.0 to 90.0, 69.0 to 70.0, 74.0 to 50.0, 79.0 to 30.0, 84.0 to 15.0, 91.0 to 5.0, 102.0 to 1.0)
        )

        // Pairs of (rmssd ms, percentile rank) - ordered by ms ascending (percentile increasing)
        private val HRV_MALE_ANCHORS = mapOf(
            "20s" to listOf(10.0 to 1.0, 24.0 to 10.0, 35.0 to 25.0, 50.0 to 50.0, 72.0 to 75.0, 95.0 to 90.0, 120.0 to 99.0),
            "30s" to listOf(8.0 to 1.0, 20.0 to 10.0, 28.0 to 25.0, 40.0 to 50.0, 58.0 to 75.0, 78.0 to 90.0, 105.0 to 99.0),
            "40s" to listOf(6.0 to 1.0, 16.0 to 10.0, 22.0 to 25.0, 32.0 to 50.0, 46.0 to 75.0, 62.0 to 90.0, 85.0 to 99.0),
            "50s" to listOf(5.0 to 1.0, 13.0 to 10.0, 18.0 to 25.0, 25.0 to 50.0, 36.0 to 75.0, 48.0 to 90.0, 70.0 to 99.0),
            "60s+" to listOf(4.0 to 1.0, 10.0 to 10.0, 14.0 to 25.0, 20.0 to 50.0, 28.0 to 75.0, 38.0 to 90.0, 55.0 to 99.0)
        )

        private val HRV_FEMALE_ANCHORS = mapOf(
            "20s" to listOf(9.0 to 1.0, 22.0 to 10.0, 33.0 to 25.0, 48.0 to 50.0, 70.0 to 75.0, 92.0 to 90.0, 115.0 to 99.0),
            "30s" to listOf(7.0 to 1.0, 19.0 to 10.0, 27.0 to 25.0, 38.0 to 50.0, 56.0 to 75.0, 75.0 to 90.0, 100.0 to 99.0),
            "40s" to listOf(6.0 to 1.0, 15.0 to 10.0, 21.0 to 25.0, 30.0 to 50.0, 44.0 to 75.0, 60.0 to 90.0, 82.0 to 99.0),
            "50s" to listOf(5.0 to 1.0, 12.0 to 10.0, 17.0 to 25.0, 24.0 to 50.0, 35.0 to 75.0, 46.0 to 90.0, 68.0 to 99.0),
            "60s+" to listOf(4.0 to 1.0, 10.0 to 10.0, 14.0 to 25.0, 19.0 to 50.0, 27.0 to 75.0, 37.0 to 90.0, 52.0 to 99.0)
        )
    }
}
