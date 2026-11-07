package io.biotrace.app.domain.clinical.risk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HarmonizedMetabolicRiskCalculatorTest {

    private val calculator = HarmonizedMetabolicRiskCalculator()

    @Test
    fun `evaluates 0 criteria met as LOW_RISK`() {
        val result = calculator.calculate(
            fastingGlucoseMgDl = 88.0,
            hba1cPercent = 5.2,
            triglyceridesMgDl = 110.0,
            hdlMgDl = 55.0,
            isMale = true,
            bmi = 22.4
        )

        assertEquals(0, result.criteriaMetCount)
        assertEquals(MetabolicRiskLevel.LOW_RISK, result.riskLevel)
        assertTrue(result.criteriaDetails.none { it.isMet })
        assertTrue(result.guidance.contains("0/4 criteria met"))
    }

    @Test
    fun `evaluates 1 criterion met as MODERATE_METABOLIC_STRAIN`() {
        // Only BMI is elevated (26.5 >= 25.0)
        val result = calculator.calculate(
            fastingGlucoseMgDl = 92.0,
            hba1cPercent = 5.3,
            triglyceridesMgDl = 120.0,
            hdlMgDl = 52.0,
            isMale = true,
            bmi = 26.5
        )

        assertEquals(1, result.criteriaMetCount)
        assertEquals(MetabolicRiskLevel.MODERATE_METABOLIC_STRAIN, result.riskLevel)
        assertTrue(result.criteriaDetails.first { it.criterionName == "Elevated Body Mass Index" }.isMet)
        assertTrue(result.guidance.contains("1/4 criteria met"))
    }

    @Test
    fun `evaluates 2 criteria met as ELEVATED_METABOLIC_RISK`() {
        // Triglycerides (160 >= 150) and Glucose (105 >= 100)
        val result = calculator.calculate(
            fastingGlucoseMgDl = 105.0,
            hba1cPercent = 5.4,
            triglyceridesMgDl = 160.0,
            hdlMgDl = 48.0,
            isMale = true,
            bmi = 24.1
        )

        assertEquals(2, result.criteriaMetCount)
        assertEquals(MetabolicRiskLevel.ELEVATED_METABOLIC_RISK, result.riskLevel)
        assertTrue(result.guidance.contains("2/4 criteria met"))
    }

    @Test
    fun `evaluates 3 criteria met as HIGH_METABOLIC_SYNDROME_RISK`() {
        // Glucose (102 >= 100), Triglycerides (175 >= 150), and Low HDL in male (36 < 40)
        val result = calculator.calculate(
            fastingGlucoseMgDl = 102.0,
            hba1cPercent = 5.5,
            triglyceridesMgDl = 175.0,
            hdlMgDl = 36.0,
            isMale = true,
            bmi = 24.5
        )

        assertEquals(3, result.criteriaMetCount)
        assertEquals(MetabolicRiskLevel.HIGH_METABOLIC_SYNDROME_RISK, result.riskLevel)
        assertTrue(result.guidance.contains("3/4 criteria met"))
    }

    @Test
    fun `evaluates all 4 criteria met as HIGH_METABOLIC_SYNDROME_RISK`() {
        val result = calculator.calculate(
            fastingGlucoseMgDl = 115.0,
            hba1cPercent = 6.1,
            triglyceridesMgDl = 210.0,
            hdlMgDl = 34.0,
            isMale = true,
            bmi = 29.2
        )

        assertEquals(4, result.criteriaMetCount)
        assertEquals(MetabolicRiskLevel.HIGH_METABOLIC_SYNDROME_RISK, result.riskLevel)
        assertTrue(result.criteriaDetails.all { it.isMet })
        assertTrue(result.guidance.contains("4/4 criteria met"))
    }

    @Test
    fun `evaluates sex-specific HDL thresholds correctly`() {
        // For male: HDL 45 is normal (>= 40)
        val maleResult = calculator.calculate(hdlMgDl = 45.0, isMale = true)
        val maleHdl = maleResult.criteriaDetails.first { it.criterionName == "Low HDL Cholesterol" }
        assertFalse(maleHdl.isMet)

        // For female: HDL 45 is low (< 50)
        val femaleResult = calculator.calculate(hdlMgDl = 45.0, isMale = false)
        val femaleHdl = femaleResult.criteriaDetails.first { it.criterionName == "Low HDL Cholesterol" }
        assertTrue(femaleHdl.isMet)
    }

    @Test
    fun `hba1c triggers glycemic criterion even if fasting glucose is absent or normal`() {
        // Fasting glucose normal (92), but HbA1c elevated (5.8 >= 5.7%)
        val result = calculator.calculate(
            fastingGlucoseMgDl = 92.0,
            hba1cPercent = 5.8
        )
        val glycemic = result.criteriaDetails.first { it.criterionName.contains("Glucose") }
        assertTrue(glycemic.isMet)
    }
}
