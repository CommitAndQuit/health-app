package io.biotrace.app.domain.usecase

import io.biotrace.app.data.local.dao.MealDao
import io.biotrace.app.domain.repository.VitalsRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

data class NutritionWearableCorrelationResult(
    val earlyDinnerTiming: String = "Before 7:30 PM",
    val earlyDinnerHrv: Double = 0.0,
    val lateDinnerTiming: String = "After 8:30 PM",
    val lateDinnerHrv: Double = 0.0,
    val hrvDeltaPercent: Int = 0,
    val hrvDeltaMs: Int = 0,
    val deepSleepBonusMinutes: Int = 0,
    val summaryNarrative: String = ""
) {
    companion object {
        val EMPTY = NutritionWearableCorrelationResult(
            earlyDinnerTiming = "Before 7:30 PM",
            earlyDinnerHrv = 0.0,
            lateDinnerTiming = "After 8:30 PM",
            lateDinnerHrv = 0.0,
            hrvDeltaPercent = 0,
            hrvDeltaMs = 0,
            deepSleepBonusMinutes = 0,
            summaryNarrative = "Insufficient meal or HRV data to calculate correlation."
        )
    }
}

@Singleton
class CorrelateNutritionWithVitalsUseCase @Inject constructor(
    private val mealDao: MealDao,
    private val vitalsRepository: VitalsRepository
) {

    suspend fun execute(): NutritionWearableCorrelationResult {
        val now = Instant.now()
        val thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS)

        // Retrieve HRV vitals and meals
        val hrvPoints = vitalsRepository.getVitalsBetween("HRV_RMSSD", thirtyDaysAgo, now)
        val meals = mealDao.getMealsBetween(thirtyDaysAgo, now)

        if (hrvPoints.isEmpty() || meals.isEmpty()) {
            return NutritionWearableCorrelationResult.EMPTY
        }

        val zone = ZoneId.systemDefault()
        val earlyHrvValues = mutableListOf<Double>()
        val lateHrvValues = mutableListOf<Double>()

        for (meal in meals) {
            val localTime = meal.consumedAt.atZone(zone).toLocalTime()
            val mealDate = meal.consumedAt.atZone(zone).toLocalDate()
            val nightStart = mealDate.atTime(20, 0).atZone(zone).toInstant()
            val nightEnd = mealDate.plusDays(1).atTime(10, 0).atZone(zone).toInstant()

            val overnightHrv = hrvPoints.filter { it.timestamp in nightStart..nightEnd }
            if (overnightHrv.isNotEmpty()) {
                val avgNightHrv = overnightHrv.map { it.value }.average()
                if (localTime.isBefore(LocalTime.of(19, 30))) {
                    earlyHrvValues.add(avgNightHrv)
                } else if (localTime.isAfter(LocalTime.of(20, 30))) {
                    lateHrvValues.add(avgNightHrv)
                }
            }
        }

        val overallAvg = hrvPoints.map { it.value }.average()
        val earlyHrv = if (earlyHrvValues.isNotEmpty()) earlyHrvValues.average() else overallAvg
        val lateHrv = if (lateHrvValues.isNotEmpty()) lateHrvValues.average() else overallAvg
        val deltaMs = (earlyHrv - lateHrv).toInt()
        val deltaPct = if (lateHrv > 0.0) (((earlyHrv - lateHrv) / lateHrv) * 100).toInt() else 0
        val deepSleepBonus = 0

        val roundedEarly = (earlyHrv * 10).roundToInt() / 10.0
        val roundedLate = (lateHrv * 10).roundToInt() / 10.0

        val narrative = if (earlyHrvValues.isNotEmpty() && lateHrvValues.isNotEmpty()) {
            val sign = if (deltaPct >= 0) "+$deltaPct%" else "$deltaPct%"
            "Dinner logged before 7:30 PM correlates with $sign overnight HRV (${roundedEarly.toInt()} ms vs ${roundedLate.toInt()} ms avg)."
        } else {
            "Average overnight HRV is ${roundedEarly.toInt()} ms across ${hrvPoints.size} measurements."
        }

        return NutritionWearableCorrelationResult(
            earlyDinnerTiming = "Before 7:30 PM",
            earlyDinnerHrv = roundedEarly,
            lateDinnerTiming = "After 8:30 PM",
            lateDinnerHrv = roundedLate,
            hrvDeltaPercent = deltaPct,
            hrvDeltaMs = deltaMs,
            deepSleepBonusMinutes = deepSleepBonus,
            summaryNarrative = narrative
        )
    }
}
