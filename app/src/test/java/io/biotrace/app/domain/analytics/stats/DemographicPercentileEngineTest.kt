package io.biotrace.app.domain.analytics.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemographicPercentileEngineTest {

    private val engine = DemographicPercentileEngine()

    @Test
    fun `resting heart rate ranks highly for athletic low bpm and poorly for elevated bpm`() {
        // Male 25 years old (20s bracket):
        val athletic = engine.calculateRestingHrPercentile(bpm = 50.0, age = 25, sex = "male")
        assertEquals("20s", athletic.ageBracket)
        assertEquals("Excellent", athletic.ratingCategory)
        assertTrue(athletic.percentile >= 90.0)

        val average = engine.calculateRestingHrPercentile(bpm = 65.0, age = 25, sex = "male")
        assertEquals(50.0, average.percentile, 1.0)
        assertEquals("Above Average", average.ratingCategory)

        val elevated = engine.calculateRestingHrPercentile(bpm = 85.0, age = 25, sex = "male")
        assertEquals("Poor", elevated.ratingCategory)
        assertTrue(elevated.percentile <= 10.0)
    }

    @Test
    fun `resting heart rate percentile is monotonically decreasing with increasing bpm`() {
        var previousPercentile = 100.0
        val bpmSamples = listOf(48.0, 55.0, 60.0, 65.0, 70.0, 75.0, 80.0, 90.0)

        for (bpm in bpmSamples) {
            val result = engine.calculateRestingHrPercentile(bpm = bpm, age = 35, sex = "female")
            assertTrue("Expected monotonic decrease: $bpm gave ${result.percentile} vs prev $previousPercentile",
                result.percentile <= previousPercentile)
            previousPercentile = result.percentile
        }
    }

    @Test
    fun `hrv rmssd ranks highly for elevated ms and poorly for depleted ms`() {
        // Male 25 years old:
        val highVagal = engine.calculateHrvRmssdPercentile(rmssd = 95.0, age = 25, sex = "male")
        assertEquals("20s", highVagal.ageBracket)
        assertEquals("Excellent", highVagal.ratingCategory)
        assertEquals(90.0, highVagal.percentile, 1.0)

        val medianVagal = engine.calculateHrvRmssdPercentile(rmssd = 50.0, age = 25, sex = "male")
        assertEquals(50.0, medianVagal.percentile, 1.0)

        val lowVagal = engine.calculateHrvRmssdPercentile(rmssd = 15.0, age = 25, sex = "male")
        assertEquals("Poor", lowVagal.ratingCategory)
        assertTrue(lowVagal.percentile < 10.0)
    }

    @Test
    fun `hrv rmssd percentile is monotonically increasing with increasing rmssd`() {
        var previousPercentile = 0.0
        val rmssdSamples = listOf(10.0, 20.0, 30.0, 45.0, 60.0, 75.0, 95.0, 110.0)

        for (rmssd in rmssdSamples) {
            val result = engine.calculateHrvRmssdPercentile(rmssd = rmssd, age = 45, sex = "male")
            assertTrue("Expected monotonic increase: $rmssd gave ${result.percentile} vs prev $previousPercentile",
                result.percentile >= previousPercentile)
            previousPercentile = result.percentile
        }
    }

    @Test
    fun `resolves age brackets correctly across lifetime milestones`() {
        assertEquals("20s", engine.calculateRestingHrPercentile(65.0, 18, "male").ageBracket)
        assertEquals("20s", engine.calculateRestingHrPercentile(65.0, 29, "male").ageBracket)
        assertEquals("30s", engine.calculateRestingHrPercentile(65.0, 30, "male").ageBracket)
        assertEquals("40s", engine.calculateRestingHrPercentile(65.0, 45, "male").ageBracket)
        assertEquals("50s", engine.calculateRestingHrPercentile(65.0, 55, "male").ageBracket)
        assertEquals("60s+", engine.calculateRestingHrPercentile(65.0, 68, "male").ageBracket)
        assertEquals("60s+", engine.calculateRestingHrPercentile(65.0, 85, "male").ageBracket)
    }

    @Test
    fun `handles female sex normative calibrations`() {
        val femaleResult = engine.calculateRestingHrPercentile(bpm = 60.0, age = 28, sex = "female")
        assertEquals("female", femaleResult.sex)
        assertTrue(femaleResult.percentile >= 90.0)
        assertEquals("Excellent", femaleResult.ratingCategory)
    }
}
