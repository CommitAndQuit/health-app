package io.biotrace.app.domain.clinical

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.floor

object VitalsCalculations {

    /**
     * Computes the 5th-percentile BPM from a collection of heart rate samples.
     * Used as the resting heart rate fallback when wearables (for example, a wearable that does not emit RestingHeartRateRecord)
     * do not write RestingHeartRateRecord directly into Health Connect.
     */
    fun calculateRestingHeartRate5thPercentile(bpmList: List<Int>): Int? {
        if (bpmList.isEmpty()) return null
        val sorted = bpmList.sorted()
        val index = floor(0.05 * (sorted.size - 1)).toInt().coerceIn(0, sorted.size - 1)
        return sorted[index]
    }

    /**
     * Calculates Body Mass Index (BMI): weight in kg / (height in meters)^2
     * Rounded to 1 decimal place.
     */
    fun calculateBmi(weightKg: Double, heightMeters: Double): Double {
        if (heightMeters <= 0.0 || weightKg <= 0.0) return 0.0
        val raw = weightKg / (heightMeters * heightMeters)
        return BigDecimal(raw).setScale(1, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Categorizes a BMI value according to standard WHO guidelines.
     */
    fun classifyBmi(bmi: Double): String {
        return when {
            bmi <= 0.0 -> "--"
            bmi < 18.5 -> "Underweight"
            bmi < 25.0 -> "Normal"
            bmi < 30.0 -> "Overweight"
            else -> "Obese"
        }
    }
}
