package io.biotrace.app.domain.analytics.timeseries

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class CircadianDecompositionServiceTest {

    private val delta = 0.05
    private val zoneId = ZoneOffset.UTC

    @Test
    fun `decompose computes normal dipping pattern with accurate nocturnal min extraction`() {
        val baseInstant = Instant.parse("2026-11-01T00:00:00Z")
        val observations = mutableListOf<HeartRateObservation>()

        // Generate synthetic diurnal curve:
        // Daytime (07:00 to 22:00): HR ~ 75 bpm
        // Nocturnal (22:00 to 07:00): HR ~ 63 bpm, reaching minimum 60 bpm at 03:00
        for (hour in 0..23) {
            val timestamp = baseInstant.plusSeconds(hour * 3600L)
            val bpm = when (hour) {
                in 7..21 -> 75.0
                3 -> 60.0 // Nocturnal minimum
                else -> 64.0
            }
            observations.add(HeartRateObservation(timestamp, bpm))
        }

        val result = CircadianDecompositionService.decompose(
            observations = observations,
            zoneId = zoneId,
            daytimeStartHour = 7,
            daytimeEndHour = 22
        )

        // Daytime mean = 75.0 bpm
        assertEquals(75.0, result.daytimeMeanHr, delta)
        // Nocturnal min = 60.0 bpm
        assertEquals(60.0, result.nocturnalMinHr, delta)

        // Dip = (75 - 60) / 75 * 100% = 20.0%
        assertEquals(20.0, result.dipPercentage, delta)
        assertEquals(AutonomicDippingPattern.NORMAL_DIPPER, result.pattern)
        assertTrue(result.circadianStabilityIndex > 0.8)
        assertTrue(result.narrative.contains("Normal nocturnal autonomic dipping"))
        assertTrue(result.narrative.contains("Health Connect"))
        assertFalse(result.narrative.contains("Huawei"))
        assertFalse(result.narrative.contains("Gadgetbridge"))
    }

    @Test
    fun `decompose identifies non-dipper pattern when nocturnal drop is blunted`() {
        val baseInstant = Instant.parse("2026-11-01T00:00:00Z")
        val observations = mutableListOf<HeartRateObservation>()

        // Daytime: 78.0 bpm, Nocturnal: 74.0 bpm (dip = (78 - 74) / 78 = 5.1%)
        for (hour in 0..23) {
            val timestamp = baseInstant.plusSeconds(hour * 3600L)
            val bpm = if (hour in 7..21) 78.0 else 74.0
            observations.add(HeartRateObservation(timestamp, bpm))
        }

        val result = CircadianDecompositionService.decompose(
            observations = observations,
            zoneId = zoneId,
            daytimeStartHour = 7,
            daytimeEndHour = 22
        )

        assertEquals(AutonomicDippingPattern.NON_DIPPER, result.pattern)
        assertTrue(result.dipPercentage in 5.0..6.0)
        assertTrue(result.narrative.contains("Blunted nocturnal dipping"))
    }

    @Test
    fun `decompose identifies reverse-dipper pattern when nocturnal heart rate is elevated`() {
        val baseInstant = Instant.parse("2026-11-01T00:00:00Z")
        val observations = mutableListOf<HeartRateObservation>()

        // Daytime: 70.0 bpm, Nocturnal: 78.0 bpm (paradoxical nocturnal elevation)
        for (hour in 0..23) {
            val timestamp = baseInstant.plusSeconds(hour * 3600L)
            val bpm = if (hour in 7..21) 70.0 else 78.0
            observations.add(HeartRateObservation(timestamp, bpm))
        }

        val result = CircadianDecompositionService.decompose(
            observations = observations,
            zoneId = zoneId,
            daytimeStartHour = 7,
            daytimeEndHour = 22
        )

        assertEquals(AutonomicDippingPattern.REVERSE_DIPPER, result.pattern)
        assertTrue(result.dipPercentage < 0.0)
        assertTrue(result.narrative.contains("Reverse diurnal dipping"))
    }

    @Test
    fun `decompose isolates residual spikes exceeding 2 standard deviations`() {
        val baseInstant = Instant.parse("2026-11-01T00:00:00Z")
        val observations = mutableListOf<HeartRateObservation>()

        // Hourly baseline with 4 samples per hour (96 total samples)
        // Add random-like baseline fluctuations of +- 1 bpm
        for (hour in 0..23) {
            val hourMean = if (hour in 7..21) 75.0 else 62.0
            for (quarter in 0..3) {
                val timestamp = baseInstant.plusSeconds((hour * 3600L) + (quarter * 900L))
                // Normal variance +- 0.8 bpm
                val jitter = if (quarter % 2 == 0) 0.8 else -0.8
                observations.add(HeartRateObservation(timestamp, hourMean + jitter))
            }
        }

        // Inject 2 extreme outlier residual spikes at 14:15 and 03:30 (e.g. +28 bpm)
        val spikeTime1 = baseInstant.plusSeconds((14 * 3600L) + 900L)
        val spikeTime2 = baseInstant.plusSeconds((3 * 3600L) + 1800L)

        // Replace the existing samples with spiked ones
        val modifiedObservations = observations.map { obs ->
            when (obs.timestamp) {
                spikeTime1 -> HeartRateObservation(obs.timestamp, obs.bpm + 28.0)
                spikeTime2 -> HeartRateObservation(obs.timestamp, obs.bpm + 25.0)
                else -> obs
            }
        }

        val result = CircadianDecompositionService.decompose(
            observations = modifiedObservations,
            zoneId = zoneId,
            daytimeStartHour = 7,
            daytimeEndHour = 22
        )

        // Both injected outliers should be detected as residual spikes (> 2 * stdDev)
        assertTrue("Expected at least 2 residual spikes, found ${result.residualSpikeCount}", result.residualSpikeCount >= 2)
        assertEquals(24, result.hourlyProfile.size)
    }

    @Test
    fun `decompose handles empty observations safely`() {
        val result = CircadianDecompositionService.decompose(emptyList())

        assertEquals(0.0, result.daytimeMeanHr, delta)
        assertEquals(0.0, result.nocturnalMinHr, delta)
        assertEquals(0.0, result.dipPercentage, delta)
        assertEquals(AutonomicDippingPattern.NON_DIPPER, result.pattern)
        assertEquals(0.0, result.circadianStabilityIndex, delta)
        assertEquals(0, result.residualSpikeCount)
        assertTrue(result.narrative.contains("No heart rate records available in Health Connect"))
    }
}
