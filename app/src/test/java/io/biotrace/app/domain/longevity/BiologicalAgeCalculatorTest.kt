package io.biotrace.app.domain.longevity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BiologicalAgeCalculatorTest {

    private lateinit var calculator: BiologicalAgeCalculator

    @Before
    fun setUp() {
        calculator = BiologicalAgeCalculator()
    }

    @Test
    fun `healthy optimal scenario calculates negative delta, OPTIMAL tier, and pace below 1`() {
        // Chronological age: 40
        // Optimal vitals: RHR 56 bpm (<60 -> -1.4), Sleep 7.8 hrs (7.0..8.5 -> -0.8)
        // Optimal labs: Glucose 85 mg/dL (<=90 -> -0.8), hs-CRP 0.5 mg/L (<0.8 -> -1.2)
        // Total expected delta = -1.4 + -0.8 + -0.8 + -1.2 = -4.2
        val result = calculator.calculate(
            chronologicalAge = 40,
            restingHrBpm = 56,
            sleepDurationHours = 7.8,
            sleepScore = 88,
            labBiomarkers = mapOf(
                "FASTING_GLUCOSE" to 85.0,
                "HS_CRP" to 0.5
            )
        )

        assertEquals(40, result.chronologicalAge)
        assertTrue("Biological age should be younger than chronological age", result.biologicalAge < 40.0)
        assertTrue("Delta should be negative for optimal health", result.ageDelta < 0.0)
        assertEquals(-4.2, result.ageDelta, 0.01)
        assertEquals(35.8, result.biologicalAge, 0.01)
        assertTrue("Aging pace should be less than 1.0", result.agingPace < 1.0)
        assertEquals(LongevityTier.OPTIMAL, result.longevityTier)
        assertEquals(70, result.confidencePercentage) // 50 base vitals + 2 * 10 labs

        // Verify drivers
        assertEquals(4, result.drivers.size)
        assertTrue("All drivers should be longevity bonuses", result.drivers.all { it.isBonus })
        assertTrue(result.drivers.any { it.title == "Low Systemic Inflammation" && it.impactYears < 0 })
        assertTrue(result.drivers.any { it.title == "Resting Heart Rate" && it.impactYears < 0 })
        assertTrue(result.drivers.any { it.title == "Optimal Fasting Glucose" && it.impactYears < 0 })
        assertTrue(result.drivers.any { it.title == "Optimal Sleep Duration" && it.impactYears < 0 })
        assertNotNull(result.primaryRecommendation)
    }

    @Test
    fun `elevated risk scenario calculates positive delta, ELEVATED_STRAIN tier, and pace above 1`() {
        // Chronological age: 40
        // Strained vitals: RHR 82 bpm (>75 -> min(2.5, 0.08 * 7) = 0.56 -> 0.6), Sleep 5.2 hrs (<6.0 -> 1.2)
        // Strained labs: Glucose 120 mg/dL (>100 -> min(3.5, 0.05 * 20) = 1.0), hs-CRP 3.5 mg/L (>2.0 -> min(3.0, ln(3.5)) = 1.25 -> 1.3)
        // Total expected delta = 0.6 + 1.2 + 1.0 + 1.3 = +4.1
        val result = calculator.calculate(
            chronologicalAge = 40,
            restingHrBpm = 82,
            sleepDurationHours = 5.2,
            sleepScore = 55,
            labBiomarkers = mapOf(
                "FASTING_GLUCOSE" to 120.0,
                "HS_CRP" to 3.5
            )
        )

        assertEquals(40, result.chronologicalAge)
        assertTrue("Biological age should be older than chronological age", result.biologicalAge > 40.0)
        assertTrue("Delta should be positive for elevated risk", result.ageDelta > 0.0)
        assertEquals(4.1, result.ageDelta, 0.01)
        assertEquals(44.1, result.biologicalAge, 0.01)
        assertTrue("Aging pace should be greater than 1.0", result.agingPace > 1.0)
        assertEquals(LongevityTier.ELEVATED_STRAIN, result.longevityTier)
        assertEquals(70, result.confidencePercentage)

        // Verify drivers are penalty flags
        assertEquals(4, result.drivers.size)
        assertTrue("All drivers should be penalties", result.drivers.none { it.isBonus })
        assertTrue(result.drivers.any { it.title == "Systemic Inflammation (hs-CRP)" })
        assertTrue(result.drivers.any { it.title == "Elevated Resting Heart Rate" })
        assertTrue(result.drivers.any { it.title == "Insufficient Sleep Duration" })
        assertTrue(result.drivers.any { it.title == "Elevated Fasting Glucose" })

        // Primary recommendation targets largest penalty
        assertTrue(
            "Primary recommendation should address the top penalty driver",
            result.primaryRecommendation.contains("inflammation", ignoreCase = true) ||
                result.primaryRecommendation.contains("sleep", ignoreCase = true)
        )
    }

    @Test
    fun `partial data scenario with only vitals returns baseline with 50 percent confidence`() {
        // Only vitals present, no lab biomarkers
        val result = calculator.calculate(
            chronologicalAge = 35,
            restingHrBpm = 65, // in 60..70 -> -0.5
            sleepDurationHours = 7.5, // in 7.0..8.5 -> -0.8
            sleepScore = 80,
            labBiomarkers = emptyMap()
        )

        assertEquals(35, result.chronologicalAge)
        assertEquals(50, result.confidencePercentage) // 50 base for vitals, 0 labs
        assertEquals(2, result.drivers.size)
        assertEquals(-1.3, result.ageDelta, 0.01)
        assertEquals(33.7, result.biologicalAge, 0.01)
        assertEquals(LongevityTier.BALANCED, result.longevityTier) // -1.3 is between -2.5 and 0.5
    }

    @Test
    fun `baseline scenario with no vitals and no labs returns default 30 percent confidence`() {
        val result = calculator.calculate(
            chronologicalAge = 35,
            restingHrBpm = null,
            sleepDurationHours = null,
            sleepScore = null,
            labBiomarkers = emptyMap()
        )

        assertEquals(35, result.chronologicalAge)
        assertEquals(35.0, result.biologicalAge, 0.01)
        assertEquals(0.0, result.ageDelta, 0.01)
        assertEquals(1.0, result.agingPace, 0.01)
        assertEquals(LongevityTier.BALANCED, result.longevityTier)
        assertEquals(30, result.confidencePercentage)
        assertTrue(result.drivers.isEmpty())
    }

    @Test
    fun `boundary case - chronological age clamped to minimum 18 prevents division by zero`() {
        val resultZero = calculator.calculate(
            chronologicalAge = 0,
            restingHrBpm = null,
            sleepDurationHours = null,
            sleepScore = null,
            labBiomarkers = emptyMap()
        )

        assertEquals(BiologicalAgeCalculator.MIN_CHRONO_AGE, resultZero.chronologicalAge)
        assertFalse("Aging pace must not be NaN", resultZero.agingPace.isNaN())
        assertFalse("Aging pace must not be Infinite", resultZero.agingPace.isInfinite())
        assertEquals(1.0, resultZero.agingPace, 0.01)

        val resultNegative = calculator.calculate(
            chronologicalAge = -10,
            restingHrBpm = null,
            sleepDurationHours = null,
            sleepScore = null,
            labBiomarkers = emptyMap()
        )
        assertEquals(BiologicalAgeCalculator.MIN_CHRONO_AGE, resultNegative.chronologicalAge)
    }

    @Test
    fun `boundary case - chronological age clamped to maximum 120`() {
        val result = calculator.calculate(
            chronologicalAge = 150,
            restingHrBpm = null,
            sleepDurationHours = null,
            sleepScore = null,
            labBiomarkers = emptyMap()
        )

        assertEquals(BiologicalAgeCalculator.MAX_CHRONO_AGE, result.chronologicalAge)
    }

    @Test
    fun `boundary case - biological age clamped to bounds`() {
        // Chronological age 19 with heavy positive penalties: biological age coerced to MAX_BIO_AGE (110.0)
        val highRisk = calculator.calculate(
            chronologicalAge = 115,
            restingHrBpm = 110,
            sleepDurationHours = 3.0,
            sleepScore = 20,
            labBiomarkers = mapOf(
                "FASTING_GLUCOSE" to 250.0,
                "HBA1C" to 12.0,
                "HS_CRP" to 20.0,
                "CREATININE" to 3.5
            )
        )
        assertTrue(
            "Biological age should not exceed MAX_BIO_AGE",
            highRisk.biologicalAge <= BiologicalAgeCalculator.MAX_BIO_AGE
        )

        // Chronological age 20 with heavy negative bonuses: biological age coerced to MIN_BIO_AGE (18.0)
        val lowRisk = calculator.calculate(
            chronologicalAge = 18,
            restingHrBpm = 45,
            sleepDurationHours = 8.0,
            sleepScore = 95,
            labBiomarkers = mapOf(
                "FASTING_GLUCOSE" to 75.0,
                "HBA1C" to 4.5,
                "HS_CRP" to 0.2,
                "ALBUMIN" to 4.8,
                "HDL" to 70.0
            )
        )
        assertTrue(
            "Biological age should not drop below MIN_BIO_AGE",
            lowRisk.biologicalAge >= BiologicalAgeCalculator.MIN_BIO_AGE
        )
    }

    @Test
    fun `boundary case - lipid panel avoids division by zero when HDL is zero or negative`() {
        val result = calculator.calculate(
            chronologicalAge = 40,
            restingHrBpm = null,
            sleepDurationHours = null,
            sleepScore = null,
            labBiomarkers = mapOf(
                "TRIGLYCERIDES" to 150.0,
                "HDL" to 0.0
            )
        )

        assertFalse("Biological age must not be NaN", result.biologicalAge.isNaN())
        assertFalse("Age delta must not be NaN", result.ageDelta.isNaN())
        // When HDL is 0, isFavorableLipid is false because tgHdlRatio is null and HDL <= 55
        assertFalse(result.drivers.any { it.title == "Cardioprotective Lipid Profile" })
    }

    @Test
    fun `mild acceleration tier classification when delta between 0 point 5 and 3 point 0`() {
        // Chronological age: 40
        // RHR 78 bpm (>75 -> min(2.5, 0.08 * 3) = 0.24 -> 0.2)
        // Sleep 5.5 hrs (<6.0 -> 1.2)
        // Total delta = 1.4 -> between 0.5 and 3.0 -> MILD_ACCELERATION
        val result = calculator.calculate(
            chronologicalAge = 40,
            restingHrBpm = 78,
            sleepDurationHours = 5.5,
            sleepScore = 65,
            labBiomarkers = emptyMap()
        )

        assertEquals(1.4, result.ageDelta, 0.01)
        assertEquals(LongevityTier.MILD_ACCELERATION, result.longevityTier)
    }

    @Test
    fun `comprehensive lab biomarkers trigger appropriate drivers and confidence cap`() {
        val labs = mapOf(
            "FASTING_GLUCOSE" to 85.0, // -0.8
            "HBA1C" to 5.0, // -0.9
            "HS_CRP" to 0.5, // -1.2
            "ALBUMIN" to 4.6, // -0.7
            "TRIGLYCERIDES" to 80.0,
            "HDL" to 65.0, // TG/HDL = 1.22 (<2.0) -> -0.6
            "CREATININE" to 0.9 // normal -> no penalty
        )

        val result = calculator.calculate(
            chronologicalAge = 45,
            restingHrBpm = 58, // -1.4
            sleepDurationHours = 8.0, // -0.8
            sleepScore = 92,
            labBiomarkers = labs
        )

        // baseConfidence 50 + 6 labs * 10 = 110 -> coerced to 95% max
        assertEquals(95, result.confidencePercentage)
        assertEquals(LongevityTier.OPTIMAL, result.longevityTier)
        assertTrue(result.drivers.size >= 6)
    }

    @Test
    fun `hs-CRP fallback to CRP key when HS_CRP is absent`() {
        val result = calculator.calculate(
            chronologicalAge = 40,
            restingHrBpm = null,
            sleepDurationHours = null,
            sleepScore = null,
            labBiomarkers = mapOf("CRP" to 0.4) // < 0.8 -> -1.2
        )

        assertTrue(result.drivers.any { it.title == "Low Systemic Inflammation" && it.impactYears == -1.2 })
    }
}
