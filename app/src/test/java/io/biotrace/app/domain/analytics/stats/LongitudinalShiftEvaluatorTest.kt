package io.biotrace.app.domain.analytics.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LongitudinalShiftEvaluatorTest {

    private val evaluator = LongitudinalShiftEvaluator()

    @Test
    fun `detects significant upward longitudinal shift when z-score exceeds 1 point 96`() {
        // Baseline window: 14 days, tight mean around 60 bpm with small variance
        val baseline = listOf(59.0, 60.0, 61.0, 60.0, 59.5, 60.5, 60.0, 59.0, 60.0, 61.0, 60.0, 60.5, 59.5, 60.0)
        // Current window: 7 days, mean ~ 68 bpm
        val current = listOf(67.0, 68.0, 69.0, 67.5, 68.5, 68.0, 67.0)

        val result = evaluator.evaluateShift(baseline, current)

        assertTrue(result.isSignificantShift)
        assertEquals(ShiftDirection.INCREASED, result.direction)
        assertTrue(result.zScore > 1.96)
        assertTrue(result.percentageShift > 10.0)
        assertTrue(result.narrative.contains("Significant upward shift", ignoreCase = true))
    }

    @Test
    fun `detects significant downward longitudinal shift`() {
        // Baseline: mean ~ 75 bpm
        val baseline = listOf(74.0, 75.0, 76.0, 75.0, 74.5, 75.5, 75.0)
        // Current: mean ~ 65 bpm
        val current = listOf(64.0, 65.0, 66.0, 65.0, 64.5, 65.5, 65.0)

        val result = evaluator.evaluateShift(baseline, current)

        assertTrue(result.isSignificantShift)
        assertEquals(ShiftDirection.DECREASED, result.direction)
        assertTrue(result.zScore < -1.96)
        assertTrue(result.narrative.contains("Significant downward shift", ignoreCase = true))
    }

    @Test
    fun `identifies stable window when mean remains within normal variance`() {
        val baseline = listOf(62.0, 63.0, 61.0, 62.5, 63.5, 61.5, 62.0)
        val current = listOf(62.2, 62.8, 61.9, 62.4, 62.1, 62.6, 62.3)

        val result = evaluator.evaluateShift(baseline, current)

        assertFalse(result.isSignificantShift)
        assertTrue(result.narrative.contains("remains stable", ignoreCase = true))
    }

    @Test
    fun `triggers significant shift via clinical delta threshold even when stdDev is broad`() {
        val baseline = listOf(50.0, 70.0, 55.0, 65.0, 60.0) // wide std dev
        val current = listOf(75.0, 80.0, 85.0) // current mean = 80.0, baseline mean = 60.0, diff = 20.0

        val result = evaluator.evaluateShift(baseline, current, clinicalDeltaThreshold = 10.0)

        assertTrue(result.isSignificantShift)
        assertEquals(ShiftDirection.INCREASED, result.direction)
    }

    @Test
    fun `handles empty series gracefully`() {
        val result = evaluator.evaluateShift(emptyList(), listOf(60.0))
        assertFalse(result.isSignificantShift)
        assertEquals(0.0, result.percentageShift, 0.001)
        assertEquals(ShiftDirection.STABLE, result.direction)
    }
}
