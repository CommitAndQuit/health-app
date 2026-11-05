package io.biotrace.app.domain.clinical

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VitalsCalculationsTest {

    @Test
    fun `calculateRestingHeartRate5thPercentile returns null for empty list`() {
        val result = VitalsCalculations.calculateRestingHeartRate5thPercentile(emptyList())
        assertNull(result)
    }

    @Test
    fun `calculateRestingHeartRate5thPercentile with single sample returns that sample`() {
        val result = VitalsCalculations.calculateRestingHeartRate5thPercentile(listOf(62))
        assertEquals(62, result)
    }

    @Test
    fun `calculateRestingHeartRate5thPercentile computes correct 5th percentile on overnight HR samples`() {
        // Construct 100 samples from 50 bpm to 149 bpm
        val samples = (50..149).toList().shuffled()
        // Index for N=100: floor(0.05 * 99) = floor(4.95) = 4
        // Sorted: index 0=50, 1=51, 2=52, 3=53, 4=54
        val result = VitalsCalculations.calculateRestingHeartRate5thPercentile(samples)
        assertEquals(54, result)
    }

    @Test
    fun `calculateRestingHeartRate5thPercentile ignores outliers and returns true baseline`() {
        // 20 samples with a couple low dips
        val samples = listOf(45, 48, 52, 53, 54, 55, 56, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 68, 70, 75)
        // N=20: floor(0.05 * 19) = floor(0.95) = 0 -> 45
        val result = VitalsCalculations.calculateRestingHeartRate5thPercentile(samples)
        assertEquals(45, result)
    }

    @Test
    fun `calculateBmi calculates standard BMI accurately to 1 decimal place`() {
        // 59.0 kg, 1.60 m -> 59 / 2.56 = 23.046875 -> 23.0
        val bmi = VitalsCalculations.calculateBmi(59.0, 1.60)
        assertEquals(23.0, bmi, 0.01)

        // 70.0 kg, 1.75 m -> 70 / 3.0625 = 22.8571 -> 22.9
        val bmi2 = VitalsCalculations.calculateBmi(70.0, 1.75)
        assertEquals(22.9, bmi2, 0.01)
    }

    @Test
    fun `calculateBmi handles zero and negative dimensions gracefully`() {
        assertEquals(0.0, VitalsCalculations.calculateBmi(0.0, 1.60), 0.001)
        assertEquals(0.0, VitalsCalculations.calculateBmi(59.0, 0.0), 0.001)
        assertEquals(0.0, VitalsCalculations.calculateBmi(-59.0, 1.60), 0.001)
    }

    @Test
    fun `classifyBmi categorizes WHO weight classes accurately`() {
        assertEquals("Underweight", VitalsCalculations.classifyBmi(17.5))
        assertEquals("Normal", VitalsCalculations.classifyBmi(18.5))
        assertEquals("Normal", VitalsCalculations.classifyBmi(23.0))
        assertEquals("Normal", VitalsCalculations.classifyBmi(24.9))
        assertEquals("Overweight", VitalsCalculations.classifyBmi(25.0))
        assertEquals("Overweight", VitalsCalculations.classifyBmi(28.4))
        assertEquals("Obese", VitalsCalculations.classifyBmi(30.0))
        assertEquals("Obese", VitalsCalculations.classifyBmi(34.2))
        assertEquals("--", VitalsCalculations.classifyBmi(0.0))
    }
}
