package io.biotrace.app.domain.clinical.risk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepDebtCalculatorTest {

    private val calculator = SleepDebtCalculator()

    @Test
    fun `calculates zero debt and optimal strain with target sleep and healthy HRV`() {
        val sleepRecords = List(14) { 8.0 }
        val result = calculator.calculate(
            sleepDurationsHours = sleepRecords,
            targetSleepHours = 8.0,
            currentHrvRmssd = 65.0,
            baselineHrvRmssd = 60.0 // HRV above baseline
        )

        assertEquals(0.0, result.cumulativeDebtHours, 0.01)
        assertEquals(0.0, result.allostaticAutonomicStrainScore, 0.01)
        assertEquals(SleepDebtTier.OPTIMAL, result.tier)
        assertTrue(result.recoveryRecommendation.contains("Optimal autonomic balance"))
    }

    @Test
    fun `calculates 14-day cumulative sleep deficit accurately`() {
        // 14 days of 7.0 hours against 8.0 target = 14.0 hours deficit
        val sleepRecords = List(14) { 7.0 }
        val result = calculator.calculate(
            sleepDurationsHours = sleepRecords,
            targetSleepHours = 8.0,
            currentHrvRmssd = 55.0,
            baselineHrvRmssd = 55.0 // HRV identical to baseline
        )

        assertEquals(14.0, result.cumulativeDebtHours, 0.01)
        // Sleep component: (14 / 20) * 60 = 42.0. HRV component: 0.0. Total = 42.0.
        assertEquals(42.0, result.allostaticAutonomicStrainScore, 0.1)
        assertEquals(SleepDebtTier.MILD_STRAIN, result.tier)
    }

    @Test
    fun `autonomic strain incorporates HRV RMSSD depression`() {
        val sleepRecords = List(14) { 7.0 } // 14.0h debt -> sleep score = 42.0
        // Baseline HRV = 50ms, Current HRV = 35ms (30% depression)
        // HRV score = (30% / 40%) * 40 = 30.0
        // Total strain = 42.0 + 30.0 = 72.0 -> MODERATE_STRAIN
        val result = calculator.calculate(
            sleepDurationsHours = sleepRecords,
            targetSleepHours = 8.0,
            currentHrvRmssd = 35.0,
            baselineHrvRmssd = 50.0
        )

        assertEquals(14.0, result.cumulativeDebtHours, 0.01)
        assertEquals(72.0, result.allostaticAutonomicStrainScore, 0.1)
        assertEquals(SleepDebtTier.MODERATE_STRAIN, result.tier)
        assertTrue(result.recoveryRecommendation.contains("Moderate autonomic recovery strain"))
    }

    @Test
    fun `severe sleep debt and severe HRV drop trigger SEVERE_STRAIN`() {
        // 14 days of 5.5 hours = 35.0h deficit (caps sleep component at 60.0)
        // Baseline HRV = 60ms, Current HRV = 30ms (50% depression, caps HRV component at 40.0)
        // Total strain = 60.0 + 40.0 = 100.0
        val sleepRecords = List(14) { 5.5 }
        val result = calculator.calculate(
            sleepDurationsHours = sleepRecords,
            targetSleepHours = 8.0,
            currentHrvRmssd = 30.0,
            baselineHrvRmssd = 60.0
        )

        assertTrue(result.cumulativeDebtHours >= 20.0)
        assertEquals(100.0, result.allostaticAutonomicStrainScore, 0.1)
        assertEquals(SleepDebtTier.SEVERE_STRAIN, result.tier)
        assertTrue(result.recoveryRecommendation.contains("Severe allostatic autonomic strain"))
    }

    @Test
    fun `window is capped at the rolling 14 days`() {
        // 20 days: first 6 days are 4.0h (ignored), last 14 days are 8.0h (evaluated)
        val sleepRecords = List(6) { 4.0 } + List(14) { 8.0 }
        val result = calculator.calculate(
            sleepDurationsHours = sleepRecords,
            targetSleepHours = 8.0
        )

        assertEquals(0.0, result.cumulativeDebtHours, 0.01)
        assertEquals(0.0, result.allostaticAutonomicStrainScore, 0.01)
        assertEquals(SleepDebtTier.OPTIMAL, result.tier)
    }

    @Test
    fun `compensatory sleep recovery pays down sleep debt`() {
        // Day 1: 6h (debt 2), Day 2: 10h (recovers 2 -> debt 0), Day 3: 8h (debt 0)
        val sleepRecords = listOf(6.0, 10.0, 8.0)
        val result = calculator.calculate(
            sleepDurationsHours = sleepRecords,
            targetSleepHours = 8.0
        )

        assertEquals(0.0, result.cumulativeDebtHours, 0.01)
    }
}
