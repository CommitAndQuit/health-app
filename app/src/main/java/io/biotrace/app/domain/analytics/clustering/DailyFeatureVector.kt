package io.biotrace.app.domain.analytics.clustering

import java.time.LocalDate

/**
 * 5-dimensional feature vector representing daily activity, sleep, and autonomic recovery.
 *
 * @param date Calendar date of the observation.
 * @param steps Daily step count.
 * @param sleepHours Total sleep duration in hours.
 * @param hrvRmssd Nocturnal or resting HRV RMSSD in milliseconds.
 * @param activeCalories Active energy expenditure in kilocalories.
 * @param lateDinnerFlag Whether caloric intake occurred within 2-3 hours before sleep onset.
 */
data class DailyFeatureVector(
    val date: LocalDate,
    val steps: Double,
    val sleepHours: Double,
    val hrvRmssd: Double,
    val activeCalories: Double,
    val lateDinnerFlag: Boolean
) {
    /**
     * Converts properties into a numeric double array for vector math.
     * Dimensions: [steps, sleepHours, hrvRmssd, activeCalories, lateDinner (1.0 or 0.0)]
     */
    fun toFeatureArray(): DoubleArray = doubleArrayOf(
        steps,
        sleepHours,
        hrvRmssd,
        activeCalories,
        if (lateDinnerFlag) 1.0 else 0.0
    )
}
