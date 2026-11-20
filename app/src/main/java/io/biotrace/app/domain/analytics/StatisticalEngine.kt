package io.biotrace.app.domain.analytics

import kotlin.math.sqrt

/**
 * Deterministic mathematical and statistical calculations engine for biometric series.
 * Operates purely on on-device data without third-party dependencies.
 */
object StatisticalEngine {

    /**
     * Computes the Exponential Weighted Moving Average (EWMA) of a time series.
     * Uses recurrence: S_0 = x_0, S_t = alpha * x_t + (1 - alpha) * S_{t-1}.
     *
     * @param series Ordered chronological data points (oldest first, newest last).
     * @param alpha Smoothing factor (0.0 < alpha <= 1.0), defaults to 0.2.
     * @return EWMA value, or 0.0 if series is empty.
     */
    fun ewma(series: List<Double>, alpha: Double = 0.2): Double {
        if (series.isEmpty()) return 0.0
        val clampedAlpha = alpha.coerceIn(0.0001, 1.0)
        var current = series[0]
        for (i in 1 until series.size) {
            current = clampedAlpha * series[i] + (1.0 - clampedAlpha) * current
        }
        return current
    }

    /**
     * Computes the median value of a numerical series.
     *
     * @param series Collection of double values.
     * @return Median value, or 0.0 if series is empty.
     */
    fun median(series: List<Double>): Double {
        if (series.isEmpty()) return 0.0
        val sorted = series.sorted()
        val n = sorted.size
        return if (n % 2 == 1) {
            sorted[n / 2]
        } else {
            (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
        }
    }

    /**
     * Computes the arithmetic mean of a series.
     *
     * @param series Collection of double values.
     * @return Mean average, or 0.0 if series is empty.
     */
    fun mean(series: List<Double>): Double {
        if (series.isEmpty()) return 0.0
        return series.average()
    }

    /**
     * Computes the sample standard deviation using Bessel's correction (N - 1).
     *
     * @param series Collection of double values.
     * @return Sample standard deviation, or 0.0 if series has 1 or fewer elements.
     */
    fun stdDev(series: List<Double>): Double {
        if (series.size <= 1) return 0.0
        val avg = series.average()
        val sumSquaredDiffs = series.sumOf { (it - avg) * (it - avg) }
        return sqrt(sumSquaredDiffs / (series.size - 1))
    }

    /**
     * Computes the standard score (Z-Score) with a safety guard against division by zero.
     *
     * @param current The observed metric value.
     * @param mean Baseline mean of the metric distribution.
     * @param stdDev Baseline standard deviation of the metric distribution.
     * @return Z-Score if stdDev > 1e-4, else 0.0.
     */
    fun zScore(current: Double, mean: Double, stdDev: Double): Double {
        return if (!stdDev.isNaN() && stdDev > 1e-4) {
            (current - mean) / stdDev
        } else {
            0.0
        }
    }
}
