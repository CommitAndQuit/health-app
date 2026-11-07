package io.biotrace.app.domain.analytics.clustering

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LifestyleKMeansClustererTest {

    private val clusterer = LifestyleKMeansClusterer()

    @Test
    fun `deterministic clustering identifies 4 distinct lifestyle archetypes on 30-day synthetic dataset`() {
        val startDate = LocalDate.of(2026, 10, 1)
        val vectors = mutableListOf<DailyFeatureVector>()

        // 1. Peak Performance Days (10 days: Oct 1 - Oct 10)
        // High steps (14k-16k), high active calories (800-950), good sleep (7.2h), good HRV (55ms)
        for (i in 0 until 10) {
            vectors.add(
                DailyFeatureVector(
                    date = startDate.plusDays(i.toLong()),
                    steps = 14500.0 + (i * 100),
                    sleepHours = 7.2,
                    hrvRmssd = 56.0,
                    activeCalories = 820.0 + (i * 10),
                    lateDinnerFlag = false
                )
            )
        }

        // 2. Deep Recovery Days (8 days: Oct 11 - Oct 18)
        // High sleep (8.8-9.0h), high HRV (70-75ms), low steps (4k), low calories (250)
        for (i in 10 until 18) {
            vectors.add(
                DailyFeatureVector(
                    date = startDate.plusDays(i.toLong()),
                    steps = 4200.0 + (i * 30),
                    sleepHours = 8.8,
                    hrvRmssd = 74.0,
                    activeCalories = 240.0,
                    lateDinnerFlag = false
                )
            )
        }

        // 3. Strain Depleted Days (7 days: Oct 19 - Oct 25)
        // Low sleep (4.8-5.2h), low HRV (28-32ms), late dinner = true
        for (i in 18 until 25) {
            vectors.add(
                DailyFeatureVector(
                    date = startDate.plusDays(i.toLong()),
                    steps = 6500.0,
                    sleepHours = 5.0,
                    hrvRmssd = 29.0,
                    activeCalories = 380.0,
                    lateDinnerFlag = true
                )
            )
        }

        // 4. Sedentary Maintenance Days (5 days: Oct 26 - Oct 30)
        // Low steps (3k), low calories (180), baseline sleep (7.0h), moderate HRV (46ms)
        for (i in 25 until 30) {
            vectors.add(
                DailyFeatureVector(
                    date = startDate.plusDays(i.toLong()),
                    steps = 3200.0,
                    sleepHours = 7.0,
                    hrvRmssd = 46.0,
                    activeCalories = 190.0,
                    lateDinnerFlag = false
                )
            )
        }

        assertEquals(30, vectors.size)

        val result = clusterer.cluster(vectors = vectors, k = 4, randomSeed = 42L)

        // Peak Performance has 10 days out of 30 (33.3%), making it the dominant archetype
        assertEquals(LifestyleArchetype.PEAK_PERFORMANCE, result.archetype)
        assertEquals(33.3, result.dominantPercentage, 0.5)

        // Check daily assignments contains all 30 days
        assertEquals(30, result.dailyAssignments.size)

        // Verify that representatives of each class received the correct archetype
        val peakDayAssignment = result.dailyAssignments[startDate.plusDays(2)]
        assertEquals(LifestyleArchetype.PEAK_PERFORMANCE, peakDayAssignment)

        val recoveryDayAssignment = result.dailyAssignments[startDate.plusDays(13)]
        assertEquals(LifestyleArchetype.DEEP_RECOVERY, recoveryDayAssignment)

        val strainDayAssignment = result.dailyAssignments[startDate.plusDays(21)]
        assertEquals(LifestyleArchetype.STRAIN_DEPLETED, strainDayAssignment)

        val sedentaryDayAssignment = result.dailyAssignments[startDate.plusDays(28)]
        assertEquals(LifestyleArchetype.SEDENTARY_MAINTENANCE, sedentaryDayAssignment)

        // Verify insights narrative
        assertTrue(result.clusterInsights.isNotEmpty())
        assertTrue(result.clusterInsights.first().contains("PEAK_PERFORMANCE"))
    }

    @Test
    fun `clustering handles empty vectors safely`() {
        val result = clusterer.cluster(vectors = emptyList(), k = 4)
        assertEquals(0.0, result.dominantPercentage, 0.01)
        assertTrue(result.dailyAssignments.isEmpty())
        assertNotNull(result.archetype)
    }

    @Test
    fun `clustering handles single vector gracefully`() {
        val single = listOf(
            DailyFeatureVector(
                date = LocalDate.now(),
                steps = 8000.0,
                sleepHours = 7.5,
                hrvRmssd = 50.0,
                activeCalories = 400.0,
                lateDinnerFlag = false
            )
        )
        val result = clusterer.cluster(vectors = single, k = 4)
        assertEquals(100.0, result.dominantPercentage, 0.01)
        assertEquals(1, result.dailyAssignments.size)
    }

    @Test
    fun `clustering supports k equals 3`() {
        val startDate = LocalDate.of(2026, 11, 1)
        val vectors = (0 until 15).map { i ->
            DailyFeatureVector(
                date = startDate.plusDays(i.toLong()),
                steps = if (i < 5) 15000.0 else if (i < 10) 4000.0 else 6000.0,
                sleepHours = if (i < 5) 7.0 else if (i < 10) 8.5 else 5.0,
                hrvRmssd = if (i < 5) 55.0 else if (i < 10) 70.0 else 30.0,
                activeCalories = if (i < 5) 800.0 else if (i < 10) 250.0 else 350.0,
                lateDinnerFlag = i >= 10
            )
        }

        val result = clusterer.cluster(vectors = vectors, k = 3, randomSeed = 42L)
        assertEquals(15, result.dailyAssignments.size)
        assertTrue(result.dailyAssignments.values.contains(LifestyleArchetype.PEAK_PERFORMANCE))
        assertTrue(result.dailyAssignments.values.contains(LifestyleArchetype.DEEP_RECOVERY))
    }
}
