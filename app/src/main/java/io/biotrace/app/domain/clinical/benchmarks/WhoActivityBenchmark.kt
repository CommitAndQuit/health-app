package io.biotrace.app.domain.clinical.benchmarks

import io.biotrace.app.domain.clinical.rules.ClinicalStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Compliance tiers for physical activity based on WHO 2020 Guidelines on Physical Activity.
 */
enum class ActivityComplianceCategory {
    INSUFFICIENT,
    PARTIALLY_COMPLIANT,
    COMPLIANT,
    OPTIMAL
}

/**
 * Daily ambulatory steps evaluation against standard epidemiological step categories.
 */
data class DailyStepsEvaluation(
    val steps: Int,
    val category: ActivityComplianceCategory,
    val status: ClinicalStatus,
    val targetRange: String,
    val narrative: String
)

/**
 * Weekly aerobic physical activity and active calorie expenditure evaluation against WHO guidelines.
 */
data class WeeklyActivityEvaluation(
    val equivalentModerateMinutes: Double,
    val weeklyActiveCalories: Double?,
    val isGuidelineMet: Boolean,
    val category: ActivityComplianceCategory,
    val status: ClinicalStatus,
    val targetDescription: String,
    val narrative: String
)

/**
 * Combined WHO physical activity compliance summary.
 */
data class WhoActivityEvaluation(
    val dailyStepsEvaluation: DailyStepsEvaluation,
    val weeklyActivityEvaluation: WeeklyActivityEvaluation,
    val isOverallCompliant: Boolean,
    val narrative: String
)

/**
 * Evaluates patient ambulatory steps and aerobic activity against WHO guidelines.
 * All logic runs purely on-device without external telemetry.
 */
@Singleton
class WhoActivityBenchmark @Inject constructor() {

    /**
     * Evaluates daily step counts against established clinical activity tiers:
     * - < 5,000 steps: Sedentary / Insufficient
     * - 5,000 - 7,499 steps: Low Active / Partially Compliant
     * - 7,500 - 9,999 steps: Active / WHO Baseline Met
     * - >= 10,000 steps: Highly Active / Optimal
     */
    fun evaluateDailySteps(steps: Int): DailyStepsEvaluation {
        val targetRange = "7,500 - 10,000 steps/day"
        val (category, status, narrative) = when {
            steps < 5000 -> Triple(
                ActivityComplianceCategory.INSUFFICIENT,
                ClinicalStatus.BORDERLINE,
                "Daily step volume of $steps is sedentary (<5,000 steps/day). Aim for at least 7,500 steps/day."
            )
            steps < 7500 -> Triple(
                ActivityComplianceCategory.PARTIALLY_COMPLIANT,
                ClinicalStatus.BORDERLINE,
                "Daily step volume of $steps is low active (5,000-7,499 steps/day). Reaching 7,500+ steps/day achieves WHO cardioprotective baseline."
            )
            steps < 10000 -> Triple(
                ActivityComplianceCategory.COMPLIANT,
                ClinicalStatus.NORMAL,
                "Daily step volume of $steps meets WHO guideline target (7,500-10,000 steps/day)."
            )
            else -> Triple(
                ActivityComplianceCategory.OPTIMAL,
                ClinicalStatus.OPTIMAL,
                "Daily step volume of $steps achieves optimal physical activity recommendation (≥10,000 steps/day)."
            )
        }

        return DailyStepsEvaluation(
            steps = steps,
            category = category,
            status = status,
            targetRange = targetRange,
            narrative = narrative
        )
    }

    /**
     * Evaluates weekly aerobic exercise and active caloric expenditure:
     * - WHO minimum recommendation: >= 150 minutes of moderate-intensity activity (or 75 mins vigorous) per week.
     * - Moderate-equivalent minutes = moderateMinutes + (2.0 * vigorousMinutes).
     * - Caloric equivalent: ~1,250 active kcal/week correlates with 150 moderate minutes for average body mass.
     */
    fun evaluateWeeklyActivity(
        moderateMinutes: Double = 0.0,
        vigorousMinutes: Double = 0.0,
        weeklyActiveCalories: Double? = null
    ): WeeklyActivityEvaluation {
        val targetDesc = "≥ 150 mins moderate physical activity/week (WHO 2020)"
        val equivMinutes = moderateMinutes + (2.0 * vigorousMinutes)

        // Calorie proxy when minute tracking is incomplete: 150 mins ~= 1,250 kcal active burn
        val calorieCompliant = weeklyActiveCalories != null && weeklyActiveCalories >= WEEKLY_CALORIE_TARGET_KCAL
        val minuteCompliant = equivMinutes >= WEEKLY_MODERATE_MINUTES_TARGET
        val isCompliant = minuteCompliant || calorieCompliant

        val (category, status, narrative) = when {
            equivMinutes >= 300.0 || (weeklyActiveCalories != null && weeklyActiveCalories >= 2500.0) -> Triple(
                ActivityComplianceCategory.OPTIMAL,
                ClinicalStatus.OPTIMAL,
                "Weekly physical activity (${equivMinutes.roundToInt()} equiv mins${calText(weeklyActiveCalories)}) meets WHO high/optimal volume recommendation (≥300 mins/week)."
            )
            isCompliant -> Triple(
                ActivityComplianceCategory.COMPLIANT,
                ClinicalStatus.NORMAL,
                "Weekly physical activity (${equivMinutes.roundToInt()} equiv mins${calText(weeklyActiveCalories)}) satisfies WHO baseline recommendations (≥150 mins/week)."
            )
            equivMinutes >= 75.0 || (weeklyActiveCalories != null && weeklyActiveCalories >= 600.0) -> Triple(
                ActivityComplianceCategory.PARTIALLY_COMPLIANT,
                ClinicalStatus.BORDERLINE,
                "Weekly physical activity (${equivMinutes.roundToInt()} equiv mins${calText(weeklyActiveCalories)}) is partially compliant. Target 150+ minutes for standard cardiovascular benefit."
            )
            else -> Triple(
                ActivityComplianceCategory.INSUFFICIENT,
                ClinicalStatus.BORDERLINE,
                "Weekly physical activity (${equivMinutes.roundToInt()} equiv mins${calText(weeklyActiveCalories)}) is insufficient. WHO recommends at least 150 mins of moderate activity per week."
            )
        }

        return WeeklyActivityEvaluation(
            equivalentModerateMinutes = equivMinutes,
            weeklyActiveCalories = weeklyActiveCalories,
            isGuidelineMet = isCompliant,
            category = category,
            status = status,
            targetDescription = targetDesc,
            narrative = narrative
        )
    }

    /**
     * Evaluates combined physical activity profile across daily ambulatory volume and weekly aerobic exercise.
     */
    fun evaluateOverallActivity(
        dailySteps: Int,
        moderateMinutes: Double = 0.0,
        vigorousMinutes: Double = 0.0,
        weeklyActiveCalories: Double? = null
    ): WhoActivityEvaluation {
        val stepsEval = evaluateDailySteps(dailySteps)
        val weeklyEval = evaluateWeeklyActivity(moderateMinutes, vigorousMinutes, weeklyActiveCalories)

        val isOverallCompliant = (stepsEval.category == ActivityComplianceCategory.COMPLIANT ||
                stepsEval.category == ActivityComplianceCategory.OPTIMAL) && weeklyEval.isGuidelineMet

        val narrative = if (isOverallCompliant) {
            "Physical activity profile meets WHO health and cardiorespiratory fitness guidelines."
        } else {
            "Physical activity profile is below WHO recommended targets. Prioritize daily steps and moderate aerobic sessions."
        }

        return WhoActivityEvaluation(
            dailyStepsEvaluation = stepsEval,
            weeklyActivityEvaluation = weeklyEval,
            isOverallCompliant = isOverallCompliant,
            narrative = narrative
        )
    }

    private fun calText(calories: Double?): String =
        if (calories != null) ", ${calories.roundToInt()} kcal" else ""

    companion object {
        const val WEEKLY_MODERATE_MINUTES_TARGET = 150.0
        const val WEEKLY_CALORIE_TARGET_KCAL = 1250.0
    }
}
