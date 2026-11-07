package io.biotrace.app.domain.analytics.timeseries

import org.junit.Assert.assertEquals
import org.junit.Test

class DiurnalDipCalculatorTest {

    private val delta = 0.001

    @Test
    fun `calculateDipPercentage evaluates diurnal dip formula correctly`() {
        // Day mean = 80 bpm, night min = 68 bpm -> (80 - 68) / 80 * 100% = 15.0%
        val dip = DiurnalDipCalculator.calculateDipPercentage(80.0, 68.0)
        assertEquals(15.0, dip, delta)

        // Reverse dip: Day mean = 70 bpm, night min = 77 bpm -> (70 - 77) / 70 * 100% = -10.0%
        val reverseDip = DiurnalDipCalculator.calculateDipPercentage(70.0, 77.0)
        assertEquals(-10.0, reverseDip, delta)

        // Zero daytime mean edge case
        assertEquals(0.0, DiurnalDipCalculator.calculateDipPercentage(0.0, 50.0), delta)
        assertEquals(0.0, DiurnalDipCalculator.calculateDipPercentage(-10.0, 50.0), delta)
    }

    @Test
    fun `classifyDippingPattern categorizes EXTREME_DIPPER above 20 percent`() {
        // 25% dip (e.g. 80 -> 60 bpm)
        val pattern = DiurnalDipCalculator.classifyDippingPattern(25.0)
        assertEquals(AutonomicDippingPattern.EXTREME_DIPPER, pattern)

        // Exact boundary 20.001%
        assertEquals(AutonomicDippingPattern.EXTREME_DIPPER, DiurnalDipCalculator.classifyDippingPattern(20.001))
    }

    @Test
    fun `classifyDippingPattern categorizes NORMAL_DIPPER between 10 and 20 percent inclusive`() {
        // 15% dip
        assertEquals(AutonomicDippingPattern.NORMAL_DIPPER, DiurnalDipCalculator.classifyDippingPattern(15.0))

        // Upper boundary 20.0%
        assertEquals(AutonomicDippingPattern.NORMAL_DIPPER, DiurnalDipCalculator.classifyDippingPattern(20.0))

        // Lower boundary 10.0%
        assertEquals(AutonomicDippingPattern.NORMAL_DIPPER, DiurnalDipCalculator.classifyDippingPattern(10.0))
    }

    @Test
    fun `classifyDippingPattern categorizes NON_DIPPER between 0 and 9_99 percent`() {
        // 6.5% dip
        assertEquals(AutonomicDippingPattern.NON_DIPPER, DiurnalDipCalculator.classifyDippingPattern(6.5))

        // 0.0% dip
        assertEquals(AutonomicDippingPattern.NON_DIPPER, DiurnalDipCalculator.classifyDippingPattern(0.0))

        // 9.99% dip
        assertEquals(AutonomicDippingPattern.NON_DIPPER, DiurnalDipCalculator.classifyDippingPattern(9.99))
    }

    @Test
    fun `classifyDippingPattern categorizes REVERSE_DIPPER below 0 percent`() {
        // -5.0% dip
        assertEquals(AutonomicDippingPattern.REVERSE_DIPPER, DiurnalDipCalculator.classifyDippingPattern(-5.0))

        // -0.01% dip
        assertEquals(AutonomicDippingPattern.REVERSE_DIPPER, DiurnalDipCalculator.classifyDippingPattern(-0.01))
    }
}
