package io.biotrace.app.domain.analytics.correlation

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Deterministic mathematical calculator for parametric (Pearson r) and non-parametric
 * (Spearman rho) cross-metric correlation analysis and Student's t statistical significance.
 *
 * All calculations run on-device with zero external dependencies.
 */
object CorrelationMatrixCalculator {

    /**
     * Calculates the Pearson product-moment correlation coefficient (r).
     *
     * Formula:
     * r = sum((x_i - mean_x) * (y_i - mean_y)) / sqrt(sum((x_i - mean_x)^2) * sum((y_i - mean_y)^2))
     *
     * Returns 0.0 if samples have length < 2 or if variance of x or y is zero.
     */
    fun calculatePearsonR(x: List<Double>, y: List<Double>): Double {
        if (x.size != y.size || x.size < 2) return 0.0

        val n = x.size
        val meanX = x.average()
        val meanY = y.average()

        var sumCovariance = 0.0
        var sumSquaresX = 0.0
        var sumSquaresY = 0.0

        for (i in 0 until n) {
            val diffX = x[i] - meanX
            val diffY = y[i] - meanY
            sumCovariance += diffX * diffY
            sumSquaresX += diffX * diffX
            sumSquaresY += diffY * diffY
        }

        if (sumSquaresX <= 1e-12 || sumSquaresY <= 1e-12) {
            return 0.0
        }

        val denominator = sqrt(sumSquaresX * sumSquaresY)
        if (denominator <= 1e-12) return 0.0

        return (sumCovariance / denominator).coerceIn(-1.0, 1.0)
    }

    /**
     * Calculates the Spearman rank-order correlation coefficient (rho) using fractional
     * ranking for ties.
     */
    fun calculateSpearmanRho(x: List<Double>, y: List<Double>): Double {
        if (x.size != y.size || x.size < 2) return 0.0

        val ranksX = computeFractionalRanks(x)
        val ranksY = computeFractionalRanks(y)

        return calculatePearsonR(ranksX, ranksY)
    }

    /**
     * Computes fractional ranks for a numeric series.
     * Tied values receive the arithmetic mean of the rank positions they span.
     */
    fun computeFractionalRanks(values: List<Double>): List<Double> {
        val n = values.size
        if (n == 0) return emptyList()
        if (n == 1) return listOf(1.0)

        data class IndexedValue(val originalIndex: Int, val value: Double)

        val indexed = values.mapIndexed { idx, v -> IndexedValue(idx, v) }
            .sortedBy { it.value }

        val ranks = DoubleArray(n)
        var i = 0
        while (i < n) {
            var j = i
            while (j < n - 1 && indexed[j + 1].value == indexed[i].value) {
                j++
            }
            // 1-based ranks for positions i..j
            val rankSum = (i + 1..j + 1).sum().toDouble()
            val avgRank = rankSum / (j - i + 1)
            for (k in i..j) {
                ranks[indexed[k].originalIndex] = avgRank
            }
            i = j + 1
        }

        return ranks.toList()
    }

    /**
     * Computes the two-tailed Student's t-statistic for a correlation coefficient r with sample size n:
     * t = r * sqrt((n - 2) / (1 - r^2))
     *
     * Degrees of freedom: df = n - 2
     */
    fun calculateTStatistic(r: Double, n: Int): Double {
        if (n <= 2 || r.isNaN()) return 0.0
        val clampedR = r.coerceIn(-1.0, 1.0)
        val denom = 1.0 - clampedR * clampedR
        if (denom <= 1e-12) {
            return if (clampedR >= 0.0) Double.POSITIVE_INFINITY else Double.NEGATIVE_INFINITY
        }
        val df = (n - 2).toDouble()
        return clampedR * sqrt(df / denom)
    }

    /**
     * Calculates the two-tailed p-value for Student's t-statistic with given degrees of freedom (df = n - 2)
     * using the regularized incomplete beta function I_x(df/2, 1/2).
     */
    fun calculatePValue(t: Double, df: Int): Double {
        if (df <= 0) return 1.0
        if (t.isInfinite()) return 0.0
        if (t == 0.0) return 1.0

        val x = df.toDouble() / (df.toDouble() + t * t)
        val a = df.toDouble() / 2.0
        val b = 0.5
        val p = incompleteBeta(a, b, x)
        return p.coerceIn(0.0, 1.0)
    }

    /**
     * Determines whether the correlation is statistically significant at significance level alpha (default 0.05).
     */
    fun isStatisticallySignificant(r: Double, n: Int, alpha: Double = 0.05): Boolean {
        if (n < 3) return false
        val t = calculateTStatistic(r, n)
        val p = calculatePValue(t, n - 2)
        return p < alpha
    }

    /**
     * Classifies correlation strength from the coefficient r.
     */
    fun classifyStrength(r: Double): CorrelationStrength =
        CorrelationStrength.fromCoefficient(r)

    // ==========================================
    // Numerical Mathematics: Regularized Incomplete Beta
    // ==========================================

    private fun logGamma(z: Double): Double {
        val p = doubleArrayOf(
            676.5203681218851,
            -1259.1392167224028,
            771.32342877765313,
            -176.61502916214059,
            12.507343278686905,
            -0.13857109526572012,
            9.9843695780195716e-6,
            1.5056327351493116e-7
        )
        var x = z
        if (x < 0.5) {
            return ln(Math.PI / sin(Math.PI * x)) - logGamma(1.0 - x)
        }
        x -= 1.0
        var a = 0.99999999999980993
        for (i in p.indices) {
            a += p[i] / (x + i + 1)
        }
        val t = x + 7.5
        return 0.5 * ln(2.0 * Math.PI) + (x + 0.5) * ln(t) - t + ln(a)
    }

    private fun betaContinuedFraction(a: Double, b: Double, x: Double): Double {
        val maxIterations = 200
        val epsilon = 3.0e-12
        val fpMin = 1.0e-30

        val qab = a + b
        val qap = a + 1.0
        val qam = a - 1.0
        var c = 1.0
        var d = 1.0 - qab * x / qap
        if (abs(d) < fpMin) d = fpMin
        d = 1.0 / d
        var h = d

        for (m in 1..maxIterations) {
            val m2 = 2 * m
            // Even step
            var aa = m * (b - m) * x / ((qam + m2) * (a + m2))
            d = 1.0 + aa * d
            if (abs(d) < fpMin) d = fpMin
            c = 1.0 + aa / c
            if (abs(c) < fpMin) c = fpMin
            d = 1.0 / d
            h *= d * c

            // Odd step
            aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2))
            d = 1.0 + aa * d
            if (abs(d) < fpMin) d = fpMin
            c = 1.0 + aa / c
            if (abs(c) < fpMin) c = fpMin
            d = 1.0 / d
            val del = d * c
            h *= del
            if (abs(del - 1.0) < epsilon) break
        }
        return h
    }

    /**
     * Regularized incomplete beta function I_x(a, b).
     */
    fun incompleteBeta(a: Double, b: Double, x: Double): Double {
        if (x <= 0.0) return 0.0
        if (x >= 1.0) return 1.0
        val bt = exp(logGamma(a + b) - logGamma(a) - logGamma(b) + a * ln(x) + b * ln(1.0 - x))
        return if (x < (a + 1.0) / (a + b + 2.0)) {
            bt * betaContinuedFraction(a, b, x) / a
        } else {
            1.0 - bt * betaContinuedFraction(b, a, 1.0 - x) / b
        }
    }
}
