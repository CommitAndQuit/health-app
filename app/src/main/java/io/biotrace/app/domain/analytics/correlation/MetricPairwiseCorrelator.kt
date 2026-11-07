package io.biotrace.app.domain.analytics.correlation

import java.time.LocalDate
import java.util.Locale

/**
 * Service for aligning longitudinal biometric time-series with configurable day lag
 * and computing bivariate Pearson and Spearman correlation statistics.
 */
object MetricPairwiseCorrelator {

    /**
     * Correlates two daily metric time-series.
     *
     * @param metric1Name Display name of the predictor / baseline metric (e.g. "Sleep Duration")
     * @param series1 Daily samples for metric 1
     * @param metric2Name Display name of the outcome metric (e.g. "Resting Heart Rate")
     * @param series2 Daily samples for metric 2
     * @param lagDays Positive lag pairs metric 1 on day T with metric 2 on day T + lagDays.
     *                For example, lagDays = 1 evaluates sleep on night T vs activity on day T + 1.
     * @param alpha Significance threshold (default 0.05)
     */
    fun correlate(
        metric1Name: String,
        series1: List<DailyMetricSample>,
        metric2Name: String,
        series2: List<DailyMetricSample>,
        lagDays: Int = 0,
        alpha: Double = 0.05
    ): CorrelationResult {
        val map2 = series2.associate { it.date to it.value }

        // Align observations based on lag
        val pairedX = mutableListOf<Double>()
        val pairedY = mutableListOf<Double>()

        // Sort series1 chronologically
        val sortedSeries1 = series1.sortedBy { it.date }

        for (sample1 in sortedSeries1) {
            val targetDate = if (lagDays != 0) {
                sample1.date.plusDays(lagDays.toLong())
            } else {
                sample1.date
            }

            val value2 = map2[targetDate]
            if (value2 != null) {
                pairedX.add(sample1.value)
                pairedY.add(value2)
            }
        }

        val sampleCount = pairedX.size

        if (sampleCount < 3) {
            return CorrelationResult(
                metric1 = metric1Name,
                metric2 = metric2Name,
                pearsonR = 0.0,
                spearmanRho = 0.0,
                sampleCount = sampleCount,
                strength = CorrelationStrength.NEUTRAL,
                isStatisticallySignificant = false,
                narrative = "Insufficient aligned daily observations ($sampleCount day${if (sampleCount == 1) "" else "s"}). " +
                    "A minimum of 3 aligned observations from Health Connect is required to compute correlation.",
                tStatistic = 0.0,
                pValue = 1.0,
                lagDays = lagDays
            )
        }

        val r = CorrelationMatrixCalculator.calculatePearsonR(pairedX, pairedY)
        val rho = CorrelationMatrixCalculator.calculateSpearmanRho(pairedX, pairedY)
        val t = CorrelationMatrixCalculator.calculateTStatistic(r, sampleCount)
        val p = CorrelationMatrixCalculator.calculatePValue(t, sampleCount - 2)
        val strength = CorrelationStrength.fromCoefficient(r)
        val isSignificant = p < alpha

        val narrative = buildNarrative(
            metric1 = metric1Name,
            metric2 = metric2Name,
            r = r,
            rho = rho,
            p = p,
            n = sampleCount,
            strength = strength,
            isSignificant = isSignificant,
            lagDays = lagDays
        )

        return CorrelationResult(
            metric1 = metric1Name,
            metric2 = metric2Name,
            pearsonR = r,
            spearmanRho = rho,
            sampleCount = sampleCount,
            strength = strength,
            isStatisticallySignificant = isSignificant,
            narrative = narrative,
            tStatistic = t,
            pValue = p,
            lagDays = lagDays
        )
    }

    /**
     * Map-based convenience overload for correlation analysis.
     */
    fun correlate(
        metric1Name: String,
        series1: Map<LocalDate, Double>,
        metric2Name: String,
        series2: Map<LocalDate, Double>,
        lagDays: Int = 0,
        alpha: Double = 0.05
    ): CorrelationResult {
        val list1 = series1.map { DailyMetricSample(it.key, it.value) }
        val list2 = series2.map { DailyMetricSample(it.key, it.value) }
        return correlate(metric1Name, list1, metric2Name, list2, lagDays, alpha)
    }

    /**
     * Computes pairwise correlation across multiple metrics.
     */
    fun computeCorrelationMatrix(
        metrics: Map<String, List<DailyMetricSample>>,
        lagDays: Int = 0,
        alpha: Double = 0.05
    ): List<CorrelationResult> {
        val metricKeys = metrics.keys.toList()
        val results = mutableListOf<CorrelationResult>()

        for (i in metricKeys.indices) {
            for (j in (i + 1) until metricKeys.size) {
                val key1 = metricKeys[i]
                val key2 = metricKeys[j]
                val series1 = metrics[key1] ?: emptyList()
                val series2 = metrics[key2] ?: emptyList()

                results.add(correlate(key1, series1, key2, series2, lagDays, alpha))
            }
        }

        return results
    }

    private fun buildNarrative(
        metric1: String,
        metric2: String,
        r: Double,
        rho: Double,
        p: Double,
        n: Int,
        strength: CorrelationStrength,
        isSignificant: Boolean,
        lagDays: Int
    ): String {
        val lagDesc = when {
            lagDays > 0 -> " with a $lagDays-day lag ($metric1 leading $metric2)"
            lagDays < 0 -> " with a ${-lagDays}-day inverse lag ($metric2 leading $metric1)"
            else -> " measured concurrently"
        }

        val relationshipDesc = if (r > 0) {
            "higher values of $metric1 were associated with higher $metric2"
        } else {
            "higher values of $metric1 were associated with lower $metric2"
        }

        return if (isSignificant) {
            val formattedR = String.format(Locale.US, "%.2f", r)
            val formattedRho = String.format(Locale.US, "%.2f", rho)
            val formattedP = if (p < 0.001) "< 0.001" else String.format(Locale.US, "%.3f", p)

            "A statistically significant ${strength.label.lowercase(Locale.US)} correlation " +
                "(Pearson r = $formattedR, Spearman ρ = $formattedRho, p = $formattedP, n = $n days) " +
                "was observed between $metric1 and $metric2$lagDesc. In Health Connect records, $relationshipDesc."
        } else {
            val formattedR = String.format(Locale.US, "%.2f", r)
            val formattedRho = String.format(Locale.US, "%.2f", rho)
            val formattedP = String.format(Locale.US, "%.3f", p)

            "No statistically significant correlation " +
                "(Pearson r = $formattedR, Spearman ρ = $formattedRho, p = $formattedP, n = $n days) " +
                "was established between $metric1 and $metric2$lagDesc. The observed biometric fluctuations did not show a consistent linear association."
        }
    }
}
