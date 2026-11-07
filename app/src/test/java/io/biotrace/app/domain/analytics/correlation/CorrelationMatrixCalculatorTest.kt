package io.biotrace.app.domain.analytics.correlation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class CorrelationMatrixCalculatorTest {

    private val delta = 0.0001

    @Test
    fun `calculatePearsonR returns 1 for perfect positive linear correlation`() {
        val x = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
        val y = listOf(10.0, 20.0, 30.0, 40.0, 50.0)

        val r = CorrelationMatrixCalculator.calculatePearsonR(x, y)
        assertEquals(1.0, r, delta)
    }

    @Test
    fun `calculatePearsonR returns -1 for perfect negative linear correlation`() {
        val x = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
        val y = listOf(50.0, 40.0, 30.0, 20.0, 10.0)

        val r = CorrelationMatrixCalculator.calculatePearsonR(x, y)
        assertEquals(-1.0, r, delta)
    }

    @Test
    fun `calculatePearsonR returns 0 for orthogonal or uncorrelated series`() {
        // Orthogonal vectors symmetric around 0
        val x = listOf(-1.0, 0.0, 1.0, 0.0)
        val y = listOf(0.0, 1.0, 0.0, -1.0)

        val r = CorrelationMatrixCalculator.calculatePearsonR(x, y)
        assertEquals(0.0, r, delta)
    }

    @Test
    fun `calculatePearsonR handles edge cases gracefully without crashing`() {
        // Empty lists
        assertEquals(0.0, CorrelationMatrixCalculator.calculatePearsonR(emptyList(), emptyList()), delta)

        // Single element
        assertEquals(0.0, CorrelationMatrixCalculator.calculatePearsonR(listOf(5.0), listOf(10.0)), delta)

        // Mismatched sizes
        assertEquals(0.0, CorrelationMatrixCalculator.calculatePearsonR(listOf(1.0, 2.0), listOf(1.0)), delta)

        // Zero variance in x (all elements identical)
        val constantX = listOf(5.0, 5.0, 5.0, 5.0)
        val varyingY = listOf(1.0, 2.0, 3.0, 4.0)
        assertEquals(0.0, CorrelationMatrixCalculator.calculatePearsonR(constantX, varyingY), delta)

        // Zero variance in y
        assertEquals(0.0, CorrelationMatrixCalculator.calculatePearsonR(varyingY, constantX), delta)
    }

    @Test
    fun `computeFractionalRanks assigns average ranks to tied values`() {
        // [10, 20, 20, 40]
        // Sorted: 10 (rank 1), 20 (ranks 2,3 -> avg 2.5), 40 (rank 4)
        val values = listOf(10.0, 20.0, 20.0, 40.0)
        val ranks = CorrelationMatrixCalculator.computeFractionalRanks(values)

        assertEquals(listOf(1.0, 2.5, 2.5, 4.0), ranks)

        // Unordered with three-way tie: [50, 20, 20, 20, 10]
        // Sorted: 10 (rank 1), 20 (ranks 2,3,4 -> avg 3.0), 50 (rank 5)
        val tiedValues = listOf(50.0, 20.0, 20.0, 20.0, 10.0)
        val tiedRanks = CorrelationMatrixCalculator.computeFractionalRanks(tiedValues)

        assertEquals(5.0, tiedRanks[0], delta)
        assertEquals(3.0, tiedRanks[1], delta)
        assertEquals(3.0, tiedRanks[2], delta)
        assertEquals(3.0, tiedRanks[3], delta)
        assertEquals(1.0, tiedRanks[4], delta)
    }

    @Test
    fun `calculateSpearmanRho captures nonlinear monotonic relationships`() {
        // y = e^x or x^3 is non-linear but strictly monotonic
        val x = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
        val y = listOf(1.0, 8.0, 27.0, 64.0, 125.0)

        val pearson = CorrelationMatrixCalculator.calculatePearsonR(x, y)
        val spearman = CorrelationMatrixCalculator.calculateSpearmanRho(x, y)

        // Pearson is high but < 1 due to non-linearity
        assertTrue(pearson < 0.99)
        // Spearman must be exactly 1.0 because rank ordering is preserved
        assertEquals(1.0, spearman, delta)
    }

    @Test
    fun `calculateSpearmanRho handles ties accurately`() {
        val x = listOf(1.0, 2.0, 2.0, 4.0)
        val y = listOf(10.0, 20.0, 20.0, 40.0)

        val rho = CorrelationMatrixCalculator.calculateSpearmanRho(x, y)
        assertEquals(1.0, rho, delta)
    }

    @Test
    fun `calculateTStatistic computes student t with df = n - 2`() {
        // n = 10, r = 0.8
        // df = 8
        // t = 0.8 * sqrt(8 / (1 - 0.64)) = 0.8 * sqrt(8 / 0.36) = 0.8 * sqrt(22.2222) = 3.771236
        val n = 10
        val r = 0.8
        val expectedT = 0.8 * sqrt(8.0 / 0.36)
        val calculatedT = CorrelationMatrixCalculator.calculateTStatistic(r, n)

        assertEquals(expectedT, calculatedT, delta)

        // Edge cases
        assertEquals(0.0, CorrelationMatrixCalculator.calculateTStatistic(r, 2), delta)
        assertEquals(0.0, CorrelationMatrixCalculator.calculateTStatistic(0.0, 10), delta)
        assertTrue(CorrelationMatrixCalculator.calculateTStatistic(1.0, 10).isInfinite())
        assertTrue(CorrelationMatrixCalculator.calculateTStatistic(-1.0, 10).isInfinite())
    }

    @Test
    fun `calculatePValue produces accurate two-tailed significance`() {
        // For t = 0, p-value is 1.0
        assertEquals(1.0, CorrelationMatrixCalculator.calculatePValue(0.0, 8), delta)

        // For n = 10 (df = 8), r = 0.8 (t = 3.7712):
        // Analytical p-value for Student t with df=8, t=3.7712 is ~0.00545
        val t = CorrelationMatrixCalculator.calculateTStatistic(0.8, 10)
        val p = CorrelationMatrixCalculator.calculatePValue(t, 8)
        assertTrue("p-value should be ~0.0054", p in 0.004..0.007)

        // Null correlation with small t: p-value should be large
        val tSmall = CorrelationMatrixCalculator.calculateTStatistic(0.1, 10)
        val pLarge = CorrelationMatrixCalculator.calculatePValue(tSmall, 8)
        assertTrue("p-value should be > 0.5", pLarge > 0.5)
    }

    @Test
    fun `isStatisticallySignificant correctly flags alpha threshold 0_05`() {
        // Strong correlation with n = 20 is significant
        assertTrue(CorrelationMatrixCalculator.isStatisticallySignificant(0.7, 20, alpha = 0.05))

        // Weak correlation with n = 5 is not significant
        assertFalse(CorrelationMatrixCalculator.isStatisticallySignificant(0.2, 5, alpha = 0.05))

        // Insufficient sample size (< 3) must always return false
        assertFalse(CorrelationMatrixCalculator.isStatisticallySignificant(1.0, 2, alpha = 0.05))
    }

    @Test
    fun `classifyStrength categorizes all defined brackets correctly`() {
        // Strong positive (>= 0.7)
        assertEquals(CorrelationStrength.STRONG_POSITIVE, CorrelationMatrixCalculator.classifyStrength(0.85))
        assertEquals(CorrelationStrength.STRONG_POSITIVE, CorrelationMatrixCalculator.classifyStrength(0.70))

        // Moderate positive (0.4 <= r < 0.7)
        assertEquals(CorrelationStrength.MODERATE_POSITIVE, CorrelationMatrixCalculator.classifyStrength(0.69))
        assertEquals(CorrelationStrength.MODERATE_POSITIVE, CorrelationMatrixCalculator.classifyStrength(0.40))

        // Weak positive (0.2 <= r < 0.4)
        assertEquals(CorrelationStrength.WEAK_POSITIVE, CorrelationMatrixCalculator.classifyStrength(0.39))
        assertEquals(CorrelationStrength.WEAK_POSITIVE, CorrelationMatrixCalculator.classifyStrength(0.20))

        // Neutral (-0.2 < r < 0.2)
        assertEquals(CorrelationStrength.NEUTRAL, CorrelationMatrixCalculator.classifyStrength(0.19))
        assertEquals(CorrelationStrength.NEUTRAL, CorrelationMatrixCalculator.classifyStrength(0.0))
        assertEquals(CorrelationStrength.NEUTRAL, CorrelationMatrixCalculator.classifyStrength(-0.19))

        // Weak negative (-0.4 < r <= -0.2)
        assertEquals(CorrelationStrength.WEAK_NEGATIVE, CorrelationMatrixCalculator.classifyStrength(-0.20))
        assertEquals(CorrelationStrength.WEAK_NEGATIVE, CorrelationMatrixCalculator.classifyStrength(-0.39))

        // Moderate negative (-0.7 < r <= -0.4)
        assertEquals(CorrelationStrength.MODERATE_NEGATIVE, CorrelationMatrixCalculator.classifyStrength(-0.40))
        assertEquals(CorrelationStrength.MODERATE_NEGATIVE, CorrelationMatrixCalculator.classifyStrength(-0.69))

        // Strong negative (r <= -0.7)
        assertEquals(CorrelationStrength.STRONG_NEGATIVE, CorrelationMatrixCalculator.classifyStrength(-0.70))
        assertEquals(CorrelationStrength.STRONG_NEGATIVE, CorrelationMatrixCalculator.classifyStrength(-0.95))
    }
}
