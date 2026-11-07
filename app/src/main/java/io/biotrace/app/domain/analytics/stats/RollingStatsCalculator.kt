package io.biotrace.app.domain.analytics.stats

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Deterministic statistical computations engine for vital metrics time-series analysis.
 * Operates purely on on-device data streams without cloud dependencies.
 */
object RollingStatsCalculator {

    /**
     * Computes the Simple Moving Average (SMA) / arithmetic mean of the series.
     * Returns 0.0 for empty collections.
     */
    fun sma(series: List<Double>): Double {
        if (series.isEmpty()) return 0.0
        return series.average()
    }

    /**
     * Computes the Exponential Weighted Moving Average (EWMA) of the series.
     * Uses recurrence: S_0 = x_0, S_t = alpha * x_t + (1 - alpha) * S_{t-1}.
     *
     * @param series Ordered chronological data points (oldest first, newest last)
     * @param alpha Smoothing factor (0.0 < alpha <= 1.0), defaults to 0.2
     */
    fun ewma(series: List<Double>, alpha: Double = 0.2): Double {
        if (series.isEmpty()) return 0.0
        val clampedAlpha = alpha.coerceIn(0.0001, 1.0)
        var currentEwma = series[0]
        for (i in 1 until series.size) {
            currentEwma = clampedAlpha * series[i] + (1.0 - clampedAlpha) * currentEwma
        }
        return currentEwma
    }

    /**
     * Computes the sample standard deviation (Bessel's correction N - 1).
     * Returns 0.0 if series has 1 or 0 elements.
     */
    fun stdDev(series: List<Double>): Double {
        if (series.size <= 1) return 0.0
        val mean = series.average()
        val sumOfSquares = series.sumOf { (it - mean) * (it - mean) }
        return sqrt(sumOfSquares / (series.size - 1))
    }

    /**
     * Computes the median value of the series.
     * Returns 0.0 for empty collections.
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
     * Computes the Median Absolute Deviation (MAD): median(|x_i - median(X)|).
     * Provides an outlier-resilient measure of statistical dispersion.
     */
    fun mad(series: List<Double>): Double {
        if (series.isEmpty()) return 0.0
        val med = median(series)
        val absoluteDeviations = series.map { abs(it - med) }
        return median(absoluteDeviations)
    }
}
