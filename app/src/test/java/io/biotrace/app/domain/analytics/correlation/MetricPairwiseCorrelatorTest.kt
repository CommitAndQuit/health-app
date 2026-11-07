package io.biotrace.app.domain.analytics.correlation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MetricPairwiseCorrelatorTest {

    private val delta = 0.001

    @Test
    fun `correlate pairs matching dates without lag`() {
        val baseDate = LocalDate.of(2026, 11, 1)

        val series1 = listOf(
            DailyMetricSample(baseDate, 10.0),
            DailyMetricSample(baseDate.plusDays(1), 20.0),
            DailyMetricSample(baseDate.plusDays(2), 30.0),
            DailyMetricSample(baseDate.plusDays(3), 40.0)
        )

        val series2 = listOf(
            DailyMetricSample(baseDate, 100.0),
            DailyMetricSample(baseDate.plusDays(1), 200.0),
            DailyMetricSample(baseDate.plusDays(2), 300.0),
            DailyMetricSample(baseDate.plusDays(3), 400.0)
        )

        val result = MetricPairwiseCorrelator.correlate(
            metric1Name = "Sleep Recovery",
            series1 = series1,
            metric2Name = "Resting Heart Rate",
            series2 = series2,
            lagDays = 0
        )

        assertEquals("Sleep Recovery", result.metric1)
        assertEquals("Resting Heart Rate", result.metric2)
        assertEquals(4, result.sampleCount)
        assertEquals(1.0, result.pearsonR, delta)
        assertEquals(1.0, result.spearmanRho, delta)
        assertEquals(CorrelationStrength.STRONG_POSITIVE, result.strength)
        assertTrue(result.isStatisticallySignificant)
        assertTrue(result.narrative.contains("Health Connect"))
        assertFalse(result.narrative.contains("Fitbit"))
        assertFalse(result.narrative.contains("Garmin"))
    }

    @Test
    fun `correlate aligns observations with 1-day lag correctly`() {
        val baseDate = LocalDate.of(2026, 11, 1)

        // Day T (Nov 1..4)
        val sleepSeries = listOf(
            DailyMetricSample(baseDate, 8.0),
            DailyMetricSample(baseDate.plusDays(1), 7.0),
            DailyMetricSample(baseDate.plusDays(2), 6.0),
            DailyMetricSample(baseDate.plusDays(3), 5.0)
        )

        // Day T + 1 (Nov 2..5)
        // With lagDays = 1, Nov 1 sleep aligns with Nov 2 steps (8000), Nov 2 sleep aligns with Nov 3 steps (7000), etc.
        val stepsSeries = listOf(
            DailyMetricSample(baseDate.plusDays(1), 8000.0),
            DailyMetricSample(baseDate.plusDays(2), 7000.0),
            DailyMetricSample(baseDate.plusDays(3), 6000.0),
            DailyMetricSample(baseDate.plusDays(4), 5000.0)
        )

        val result = MetricPairwiseCorrelator.correlate(
            metric1Name = "Sleep Recovery",
            series1 = sleepSeries,
            metric2Name = "Daily Steps",
            series2 = stepsSeries,
            lagDays = 1
        )

        assertEquals(4, result.sampleCount)
        assertEquals(1.0, result.pearsonR, delta)
        assertEquals(CorrelationStrength.STRONG_POSITIVE, result.strength)
        assertTrue(result.narrative.contains("1-day lag"))
    }

    @Test
    fun `correlate skips missing dates during alignment`() {
        val baseDate = LocalDate.of(2026, 11, 1)

        // Series 1 has Nov 1, 2, 4
        val series1 = listOf(
            DailyMetricSample(baseDate, 10.0),
            DailyMetricSample(baseDate.plusDays(1), 20.0),
            DailyMetricSample(baseDate.plusDays(3), 40.0)
        )

        // Series 2 has Nov 1, 3, 4
        val series2 = listOf(
            DailyMetricSample(baseDate, 100.0),
            DailyMetricSample(baseDate.plusDays(2), 300.0),
            DailyMetricSample(baseDate.plusDays(3), 400.0)
        )

        // Only Nov 1 and Nov 4 match -> 2 pairs (< 3 required)
        val result = MetricPairwiseCorrelator.correlate(
            metric1Name = "Heart Rate",
            series1 = series1,
            metric2Name = "Blood Oxygen",
            series2 = series2,
            lagDays = 0
        )

        assertEquals(2, result.sampleCount)
        assertEquals(0.0, result.pearsonR, delta)
        assertEquals(CorrelationStrength.NEUTRAL, result.strength)
        assertFalse(result.isStatisticallySignificant)
        assertTrue(result.narrative.contains("Insufficient aligned daily observations"))
    }

    @Test
    fun `computeCorrelationMatrix evaluates all unique metric pairs`() {
        val baseDate = LocalDate.of(2026, 11, 1)

        val metrics = mapOf(
            "Sleep" to listOf(
                DailyMetricSample(baseDate, 7.0),
                DailyMetricSample(baseDate.plusDays(1), 8.0),
                DailyMetricSample(baseDate.plusDays(2), 6.5)
            ),
            "RHR" to listOf(
                DailyMetricSample(baseDate, 62.0),
                DailyMetricSample(baseDate.plusDays(1), 58.0),
                DailyMetricSample(baseDate.plusDays(2), 65.0)
            ),
            "Steps" to listOf(
                DailyMetricSample(baseDate, 8000.0),
                DailyMetricSample(baseDate.plusDays(1), 10000.0),
                DailyMetricSample(baseDate.plusDays(2), 6000.0)
            )
        )

        // 3 metrics -> (3 * 2) / 2 = 3 unique pairwise combinations
        val matrix = MetricPairwiseCorrelator.computeCorrelationMatrix(metrics)
        assertEquals(3, matrix.size)

        val pairs = matrix.map { "${it.metric1} vs ${it.metric2}" }
        assertTrue(pairs.contains("Sleep vs RHR"))
        assertTrue(pairs.contains("Sleep vs Steps"))
        assertTrue(pairs.contains("RHR vs Steps"))
    }
}
