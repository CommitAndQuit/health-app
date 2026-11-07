package io.biotrace.app.domain.clinical.risk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AtherogenicRiskCalculatorTest {

    private val calculator = AtherogenicRiskCalculator()

    @Test
    fun `evaluates optimal lipid profile across all metrics`() {
        // TG=90, TC=170, HDL=60
        // TG/HDL = 1.5 (< 2.0) -> OPTIMAL
        // Non-HDL = 110 (< 130) -> OPTIMAL
        // TC/HDL = 2.83 (< 3.5) -> OPTIMAL
        val result = calculator.calculate(
            totalCholesterolMgDl = 170.0,
            hdlMgDl = 60.0,
            triglyceridesMgDl = 90.0
        )

        assertEquals(AtherogenicRiskTier.OPTIMAL, result.overallRiskTier)
        assertEquals(AtherogenicRiskTier.OPTIMAL, result.tgHdlTier)
        assertEquals(AtherogenicRiskTier.OPTIMAL, result.nonHdlTier)
        assertEquals(AtherogenicRiskTier.OPTIMAL, result.tcHdlTier)
        assertEquals(1.5, result.tgHdlRatio, 0.01)
        assertEquals(110.0, result.nonHdlCholesterol, 0.01)
        assertEquals(2.83, result.tcHdlRatio, 0.01)
        assertTrue(result.narrative.contains("optimal"))
    }

    @Test
    fun `evaluates boundary cutoffs for TG to HDL ratio`() {
        // Optimal (< 2.0)
        val optimal = calculator.calculate(totalCholesterolMgDl = 150.0, hdlMgDl = 50.0, triglyceridesMgDl = 95.0)
        assertEquals(1.9, optimal.tgHdlRatio, 0.01)
        assertEquals(AtherogenicRiskTier.OPTIMAL, optimal.tgHdlTier)

        // Moderate lower bound (2.0)
        val moderateLower = calculator.calculate(totalCholesterolMgDl = 150.0, hdlMgDl = 50.0, triglyceridesMgDl = 100.0)
        assertEquals(2.0, moderateLower.tgHdlRatio, 0.01)
        assertEquals(AtherogenicRiskTier.MODERATE_RISK, moderateLower.tgHdlTier)

        // Moderate upper bound (3.0)
        val moderateUpper = calculator.calculate(totalCholesterolMgDl = 150.0, hdlMgDl = 50.0, triglyceridesMgDl = 150.0)
        assertEquals(3.0, moderateUpper.tgHdlRatio, 0.01)
        assertEquals(AtherogenicRiskTier.MODERATE_RISK, moderateUpper.tgHdlTier)

        // High (> 3.0)
        val elevated = calculator.calculate(totalCholesterolMgDl = 150.0, hdlMgDl = 50.0, triglyceridesMgDl = 160.0)
        assertEquals(3.2, elevated.tgHdlRatio, 0.01)
        assertEquals(AtherogenicRiskTier.HIGH_RISK, elevated.tgHdlTier)
    }

    @Test
    fun `evaluates boundary cutoffs for Non-HDL cholesterol`() {
        // Optimal (< 130 mg/dL)
        val optimal = calculator.calculate(totalCholesterolMgDl = 175.0, hdlMgDl = 50.0, triglyceridesMgDl = 80.0)
        assertEquals(125.0, optimal.nonHdlCholesterol, 0.01)
        assertEquals(AtherogenicRiskTier.OPTIMAL, optimal.nonHdlTier)

        // Moderate lower bound (130 mg/dL)
        val moderateLower = calculator.calculate(totalCholesterolMgDl = 180.0, hdlMgDl = 50.0, triglyceridesMgDl = 80.0)
        assertEquals(130.0, moderateLower.nonHdlCholesterol, 0.01)
        assertEquals(AtherogenicRiskTier.MODERATE_RISK, moderateLower.nonHdlTier)

        // Moderate upper bound (< 160 mg/dL)
        val moderateUpper = calculator.calculate(totalCholesterolMgDl = 205.0, hdlMgDl = 50.0, triglyceridesMgDl = 80.0)
        assertEquals(155.0, moderateUpper.nonHdlCholesterol, 0.01)
        assertEquals(AtherogenicRiskTier.MODERATE_RISK, moderateUpper.nonHdlTier)

        // Elevated (>= 160 mg/dL)
        val elevatedBoundary = calculator.calculate(totalCholesterolMgDl = 210.0, hdlMgDl = 50.0, triglyceridesMgDl = 80.0)
        assertEquals(160.0, elevatedBoundary.nonHdlCholesterol, 0.01)
        assertEquals(AtherogenicRiskTier.HIGH_RISK, elevatedBoundary.nonHdlTier)
    }

    @Test
    fun `evaluates boundary cutoffs for TC to HDL ratio`() {
        // Optimal (< 3.5)
        val optimal = calculator.calculate(totalCholesterolMgDl = 160.0, hdlMgDl = 50.0, triglyceridesMgDl = 80.0)
        assertEquals(3.2, optimal.tcHdlRatio, 0.01)
        assertEquals(AtherogenicRiskTier.OPTIMAL, optimal.tcHdlTier)

        // Moderate lower bound (3.5)
        val moderateLower = calculator.calculate(totalCholesterolMgDl = 175.0, hdlMgDl = 50.0, triglyceridesMgDl = 80.0)
        assertEquals(3.5, moderateLower.tcHdlRatio, 0.01)
        assertEquals(AtherogenicRiskTier.MODERATE_RISK, moderateLower.tcHdlTier)

        // Moderate upper bound (5.0)
        val moderateUpper = calculator.calculate(totalCholesterolMgDl = 250.0, hdlMgDl = 50.0, triglyceridesMgDl = 80.0)
        assertEquals(5.0, moderateUpper.tcHdlRatio, 0.01)
        assertEquals(AtherogenicRiskTier.MODERATE_RISK, moderateUpper.tcHdlTier)

        // Elevated (> 5.0)
        val elevated = calculator.calculate(totalCholesterolMgDl = 260.0, hdlMgDl = 50.0, triglyceridesMgDl = 80.0)
        assertEquals(5.2, elevated.tcHdlRatio, 0.01)
        assertEquals(AtherogenicRiskTier.HIGH_RISK, elevated.tcHdlTier)
    }

    @Test
    fun `high risk tier overrides moderate when one ratio is severely elevated`() {
        // TG=220, HDL=50, TC=170
        // TG/HDL = 4.4 (> 3.0) -> HIGH_RISK
        // Non-HDL = 120 (< 130) -> OPTIMAL
        // TC/HDL = 3.4 (< 3.5) -> OPTIMAL
        val result = calculator.calculate(
            totalCholesterolMgDl = 170.0,
            hdlMgDl = 50.0,
            triglyceridesMgDl = 220.0
        )
        assertEquals(AtherogenicRiskTier.HIGH_RISK, result.overallRiskTier)
        assertEquals(AtherogenicRiskTier.HIGH_RISK, result.tgHdlTier)
        assertEquals(AtherogenicRiskTier.OPTIMAL, result.nonHdlTier)
        assertEquals(AtherogenicRiskTier.OPTIMAL, result.tcHdlTier)
        assertTrue(result.narrative.contains("High atherogenic cardiovascular risk indicated"))
    }

    @Test
    fun `invalid HDL throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            calculator.calculate(totalCholesterolMgDl = 180.0, hdlMgDl = 0.0, triglyceridesMgDl = 120.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            calculator.calculate(totalCholesterolMgDl = 180.0, hdlMgDl = -10.0, triglyceridesMgDl = 120.0)
        }
    }
}
