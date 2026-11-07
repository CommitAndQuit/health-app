package io.biotrace.app.domain.clinical.benchmarks

import io.biotrace.app.domain.clinical.rules.ClinicalStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BenchmarksTest {

    private val activityBenchmark = WhoActivityBenchmark()
    private val sleepBenchmark = SleepArchitectureBenchmark()

    // --- WHO Activity Benchmark Tests ---

    @Test
    fun `classifies daily ambulatory steps according to WHO clinical tiers`() {
        // Sedentary (< 5,000 steps)
        val sedentary = activityBenchmark.evaluateDailySteps(3500)
        assertEquals(ActivityComplianceCategory.INSUFFICIENT, sedentary.category)
        assertEquals(ClinicalStatus.BORDERLINE, sedentary.status)
        assertTrue(sedentary.narrative.contains("sedentary", ignoreCase = true))

        // Low Active (5,000 - 7,499 steps)
        val lowActive = activityBenchmark.evaluateDailySteps(6200)
        assertEquals(ActivityComplianceCategory.PARTIALLY_COMPLIANT, lowActive.category)
        assertEquals(ClinicalStatus.BORDERLINE, lowActive.status)

        // Active / WHO Baseline Met (7,500 - 9,999 steps)
        val compliant = activityBenchmark.evaluateDailySteps(8500)
        assertEquals(ActivityComplianceCategory.COMPLIANT, compliant.category)
        assertEquals(ClinicalStatus.NORMAL, compliant.status)

        // Optimal / Highly Active (>= 10,000 steps)
        val optimal = activityBenchmark.evaluateDailySteps(12000)
        assertEquals(ActivityComplianceCategory.OPTIMAL, optimal.category)
        assertEquals(ClinicalStatus.OPTIMAL, optimal.status)
    }

    @Test
    fun `evaluates weekly aerobic exercise against 150-minute WHO guidelines`() {
        // 160 minutes moderate: Meets guideline
        val moderateMet = activityBenchmark.evaluateWeeklyActivity(moderateMinutes = 160.0)
        assertTrue(moderateMet.isGuidelineMet)
        assertEquals(ActivityComplianceCategory.COMPLIANT, moderateMet.category)
        assertEquals(ClinicalStatus.NORMAL, moderateMet.status)

        // 80 minutes vigorous = 160 equivalent moderate minutes: Meets guideline
        val vigorousMet = activityBenchmark.evaluateWeeklyActivity(moderateMinutes = 0.0, vigorousMinutes = 80.0)
        assertTrue(vigorousMet.isGuidelineMet)
        assertEquals(160.0, vigorousMet.equivalentModerateMinutes, 0.1)

        // Calorie expenditure proxy (e.g. 1,400 active kcal)
        val calorieMet = activityBenchmark.evaluateWeeklyActivity(
            moderateMinutes = 60.0,
            weeklyActiveCalories = 1400.0
        )
        assertTrue(calorieMet.isGuidelineMet)

        // Insufficient activity (< 75 equiv minutes)
        val insufficient = activityBenchmark.evaluateWeeklyActivity(moderateMinutes = 40.0)
        assertFalse(insufficient.isGuidelineMet)
        assertEquals(ActivityComplianceCategory.INSUFFICIENT, insufficient.category)
    }

    @Test
    fun `evaluates combined overall activity profile compliance`() {
        val fullyCompliant = activityBenchmark.evaluateOverallActivity(
            dailySteps = 8200,
            moderateMinutes = 180.0
        )
        assertTrue(fullyCompliant.isOverallCompliant)

        val nonCompliant = activityBenchmark.evaluateOverallActivity(
            dailySteps = 4000,
            moderateMinutes = 60.0
        )
        assertFalse(nonCompliant.isOverallCompliant)
    }

    // --- Sleep Architecture Benchmark Tests ---

    @Test
    fun `identifies optimal balanced sleep architecture meeting National Sleep Foundation norms`() {
        // Deep 18% (15-20%), REM 22% (20-25%), Light 60% (50-60%)
        val result = sleepBenchmark.evaluatePercentages(
            deepPercent = 18.0,
            remPercent = 22.0,
            lightPercent = 60.0,
            totalSleepMinutes = 450.0
        )

        assertTrue(result.isArchitectureBalanced)
        assertEquals("Optimal", result.overallQualityRating)
        assertEquals(ClinicalStatus.OPTIMAL, result.deepSleep.status)
        assertEquals(ClinicalStatus.OPTIMAL, result.remSleep.status)
        assertEquals(ClinicalStatus.OPTIMAL, result.lightSleep.status)
        assertTrue(result.narrative.contains("optimal", ignoreCase = true))
    }

    @Test
    fun `flags imbalanced sleep architecture with depleted deep sleep`() {
        // Deep 8% (suboptimal), REM 22%, Light 70%
        val result = sleepBenchmark.evaluatePercentages(
            deepPercent = 8.0,
            remPercent = 22.0,
            lightPercent = 70.0
        )

        assertFalse(result.isArchitectureBalanced)
        assertEquals("Imbalanced", result.overallQualityRating)
        assertEquals(ClinicalStatus.BORDERLINE, result.deepSleep.status)
        assertFalse(result.deepSleep.isWithinNormativeRange)
        assertTrue(result.deepSleep.commentary.contains("low", ignoreCase = true))
    }

    @Test
    fun `flags imbalanced sleep architecture with depleted REM sleep`() {
        // Deep 18%, REM 12% (suboptimal), Light 70%
        val result = sleepBenchmark.evaluatePercentages(
            deepPercent = 18.0,
            remPercent = 12.0,
            lightPercent = 70.0
        )

        assertFalse(result.isArchitectureBalanced)
        assertEquals(ClinicalStatus.BORDERLINE, result.remSleep.status)
        assertFalse(result.remSleep.isWithinNormativeRange)
        assertTrue(result.remSleep.commentary.contains("low", ignoreCase = true))
    }

    @Test
    fun `computes stage percentages accurately from raw minute durations`() {
        // Total = 90 + 100 + 260 = 450 minutes
        // Deep = 90 / 450 = 20.0%
        // REM = 100 / 450 = 22.2%
        // Light = 260 / 450 = 57.8%
        val result = sleepBenchmark.evaluateFromMinutes(
            deepMinutes = 90.0,
            remMinutes = 100.0,
            lightMinutes = 260.0
        )

        assertEquals(450.0, result.totalSleepMinutes, 0.1)
        assertEquals(20.0, result.deepSleep.actualPercentage, 0.2)
        assertEquals(22.2, result.remSleep.actualPercentage, 0.2)
        assertEquals(57.8, result.lightSleep.actualPercentage, 0.2)
        assertTrue(result.isArchitectureBalanced)
        assertEquals("Optimal", result.overallQualityRating)
    }

    @Test
    fun `handles zero duration sleep safely`() {
        val result = sleepBenchmark.evaluateFromMinutes(0.0, 0.0, 0.0)
        assertEquals(0.0, result.totalSleepMinutes, 0.001)
        assertFalse(result.isArchitectureBalanced)
    }
}
