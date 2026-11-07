package io.biotrace.app.domain.anomaly

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MultivariateAnomalyTest {

    private val engine = AnomalyDetectionEngine()

    // Test baseline parameters:
    // RHR: mean = 60.0 bpm, stdDev = 4.0 bpm (elevation threshold: > 60 + 1.5 * 4 = 66.0 bpm)
    // HRV: mean = 65.0 ms, stdDev = 10.0 ms (depression threshold: < 65 - 1.8 * 10 = 47.0 ms)
    // Sleep: short sleep threshold: < 6.0 hours

    @Test
    fun `flags compound autonomic strain when all three conditions are simultaneously met`() {
        val alert = engine.evaluateCompoundStrain(
            currentRhr = 68.0, // +2.0 stdDev (> 1.5)
            baselineRhrMean = 60.0,
            baselineRhrStdDev = 4.0,
            currentHrv = 42.0, // -2.3 stdDev (> 1.8)
            baselineHrvMean = 65.0,
            baselineHrvStdDev = 10.0,
            recentSleepHours = 5.2 // < 6.0 hours
        )

        assertNotNull(alert)
        assertEquals(AnomalyRuleType.MULTIVARIATE_COMPOUND_STRAIN, alert?.ruleType)
        assertEquals("COMPOUND_AUTONOMIC_STRAIN", alert?.metricName)
        assertEquals("ALERT", alert?.severity)
        assertTrue(alert?.narrative?.contains("Multivariate autonomic strain detected") == true)
        assertTrue(alert?.narrative?.contains("5.2h < 6.0h") == true)
    }

    @Test
    fun `single metric HRV depression alone does not trigger compound alert`() {
        val alert = engine.evaluateCompoundStrain(
            currentRhr = 62.0, // Normal RHR (+0.5 stdDev)
            baselineRhrMean = 60.0,
            baselineRhrStdDev = 4.0,
            currentHrv = 40.0, // Depressed HRV (-2.5 stdDev)
            baselineHrvMean = 65.0,
            baselineHrvStdDev = 10.0,
            recentSleepHours = 7.5 // Normal sleep
        )

        assertNull(alert)
    }

    @Test
    fun `single metric RHR elevation alone does not trigger compound alert`() {
        val alert = engine.evaluateCompoundStrain(
            currentRhr = 70.0, // Elevated RHR (+2.5 stdDev)
            baselineRhrMean = 60.0,
            baselineRhrStdDev = 4.0,
            currentHrv = 64.0, // Normal HRV
            baselineHrvMean = 65.0,
            baselineHrvStdDev = 10.0,
            recentSleepHours = 7.8 // Normal sleep
        )

        assertNull(alert)
    }

    @Test
    fun `single metric short sleep alone does not trigger compound alert`() {
        val alert = engine.evaluateCompoundStrain(
            currentRhr = 61.0, // Normal RHR
            baselineRhrMean = 60.0,
            baselineRhrStdDev = 4.0,
            currentHrv = 63.0, // Normal HRV
            baselineHrvMean = 65.0,
            baselineHrvStdDev = 10.0,
            recentSleepHours = 5.0 // Curtailed sleep (< 6.0h)
        )

        assertNull(alert)
    }

    @Test
    fun `two metric deviation with normal sleep does not trigger compound alert`() {
        val alert = engine.evaluateCompoundStrain(
            currentRhr = 68.0, // Elevated (> 1.5 stdDev)
            baselineRhrMean = 60.0,
            baselineRhrStdDev = 4.0,
            currentHrv = 42.0, // Depressed (> 1.8 stdDev)
            baselineHrvMean = 65.0,
            baselineHrvStdDev = 10.0,
            recentSleepHours = 7.0 // Healthy sleep (>= 6.0h)
        )

        assertNull(alert)
    }

    @Test
    fun `two metric deviation with normal HRV does not trigger compound alert`() {
        val alert = engine.evaluateCompoundStrain(
            currentRhr = 68.0, // Elevated (> 1.5 stdDev)
            baselineRhrMean = 60.0,
            baselineRhrStdDev = 4.0,
            currentHrv = 62.0, // Normal HRV
            baselineHrvMean = 65.0,
            baselineHrvStdDev = 10.0,
            recentSleepHours = 5.0 // Short sleep (< 6.0h)
        )

        assertNull(alert)
    }

    @Test
    fun `two metric deviation with normal RHR does not trigger compound alert`() {
        val alert = engine.evaluateCompoundStrain(
            currentRhr = 61.0, // Normal RHR
            baselineRhrMean = 60.0,
            baselineRhrStdDev = 4.0,
            currentHrv = 42.0, // Depressed (> 1.8 stdDev)
            baselineHrvMean = 65.0,
            baselineHrvStdDev = 10.0,
            recentSleepHours = 5.0 // Short sleep (< 6.0h)
        )

        assertNull(alert)
    }

    @Test
    fun `handles zero or negative standard deviation gracefully`() {
        val alert = engine.evaluateCompoundStrain(
            currentRhr = 75.0,
            baselineRhrMean = 60.0,
            baselineRhrStdDev = 0.0, // Zero stdDev
            currentHrv = 30.0,
            baselineHrvMean = 65.0,
            baselineHrvStdDev = 10.0,
            recentSleepHours = 5.0
        )

        assertNull(alert)
    }
}
