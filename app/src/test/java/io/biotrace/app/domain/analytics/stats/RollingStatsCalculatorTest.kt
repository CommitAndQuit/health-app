package io.biotrace.app.domain.analytics.stats

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sqrt

class RollingStatsCalculatorTest {

    private val delta = 0.0001

    @Test
    fun `sma calculates arithmetic mean accurately and handles edge cases`() {
        assertEquals(0.0, RollingStatsCalculator.sma(emptyList()), delta)
        assertEquals(42.0, RollingStatsCalculator.sma(listOf(42.0)), delta)
        assertEquals(20.0, RollingStatsCalculator.sma(listOf(10.0, 20.0, 30.0)), delta)
        assertEquals(3.0, RollingStatsCalculator.sma(listOf(1.5, 2.5, 5.0)), delta)
    }

    @Test
    fun `ewma computes recurrence accurately and clamps smoothing alpha`() {
        assertEquals(0.0, RollingStatsCalculator.ewma(emptyList()), delta)
        assertEquals(50.0, RollingStatsCalculator.ewma(listOf(50.0), alpha = 0.2), delta)

        // S_0 = 10.0; S_1 = 0.2 * 20 + 0.8 * 10 = 12.0; S_2 = 0.2 * 30 + 0.8 * 12 = 15.6
        val series = listOf(10.0, 20.0, 30.0)
        assertEquals(15.6, RollingStatsCalculator.ewma(series, alpha = 0.2), delta)

        // Alpha = 1.0 implies zero memory: should match latest element
        assertEquals(30.0, RollingStatsCalculator.ewma(series, alpha = 1.0), delta)
    }

    @Test
    fun `stdDev computes sample standard deviation with Bessel correction`() {
        assertEquals(0.0, RollingStatsCalculator.stdDev(emptyList()), delta)
        assertEquals(0.0, RollingStatsCalculator.stdDev(listOf(99.0)), delta)
        assertEquals(0.0, RollingStatsCalculator.stdDev(listOf(10.0, 10.0, 10.0)), delta)

        // Series with known mean = 5.0, sum of squared diffs = 32.0, N = 8 -> variance = 32 / 7
        val series = listOf(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0)
        val expected = sqrt(32.0 / 7.0)
        assertEquals(expected, RollingStatsCalculator.stdDev(series), delta)
    }

    @Test
    fun `median computes exact center value for odd and even size collections`() {
        assertEquals(0.0, RollingStatsCalculator.median(emptyList()), delta)
        assertEquals(7.0, RollingStatsCalculator.median(listOf(7.0)), delta)

        // Odd collection: sorted [1, 5, 9] -> 5.0
        assertEquals(5.0, RollingStatsCalculator.median(listOf(5.0, 1.0, 9.0)), delta)

        // Even collection: sorted [1, 2, 3, 4] -> (2 + 3) / 2 = 2.5
        assertEquals(2.5, RollingStatsCalculator.median(listOf(1.0, 3.0, 2.0, 4.0)), delta)
    }

    @Test
    fun `mad computes median absolute deviation correctly`() {
        assertEquals(0.0, RollingStatsCalculator.mad(emptyList()), delta)
        assertEquals(0.0, RollingStatsCalculator.mad(listOf(10.0, 10.0, 10.0)), delta)

        // Series: [1, 2, 3, 4, 5, 6, 7, 8, 9] -> median is 5.0
        // Absolute deviations: [4, 3, 2, 1, 0, 1, 2, 3, 4] -> sorted: [0, 1, 1, 2, 2, 3, 3, 4, 4] -> median is 2.0
        val series = listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0)
        assertEquals(2.0, RollingStatsCalculator.mad(series), delta)
    }
}
