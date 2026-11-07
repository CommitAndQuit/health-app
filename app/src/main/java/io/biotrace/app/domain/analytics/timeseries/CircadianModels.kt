package io.biotrace.app.domain.analytics.timeseries

import java.time.Instant

/**
 * Autonomic nocturnal dipping classifications according to ambulatory chronobiology standards.
 */
enum class AutonomicDippingPattern(val displayName: String, val clinicalDescription: String) {
    EXTREME_DIPPER(
        displayName = "Extreme Dipper",
        clinicalDescription = "Marked nocturnal heart rate reduction exceeding 20% of daytime mean. Reflects intense parasympathetic vagal dominance during sleep."
    ),
    NORMAL_DIPPER(
        displayName = "Normal Dipper",
        clinicalDescription = "Healthy physiological diurnal dipping between 10% and 20%. Indicates balanced autonomic circadian tone."
    ),
    NON_DIPPER(
        displayName = "Non-Dipper",
        clinicalDescription = "Blunted nocturnal dipping (0% to 9.9%). Associated with nocturnal sympathetic tone persistence, fragmented sleep, or autonomic strain."
    ),
    REVERSE_DIPPER(
        displayName = "Reverse Dipper",
        clinicalDescription = "Paradoxical nocturnal elevation (<0% dip; night rate exceeds day mean). Indicates autonomic stress or inverted circadian chronobiology."
    )
}

/**
 * Hourly aggregation along the 24-hour diurnal cycle (0..23).
 */
data class HourlyDiurnalPoint(
    val hourOfDay: Int,
    val meanBpm: Double,
    val minBpm: Double,
    val maxBpm: Double,
    val sampleCount: Int
)

/**
 * Represents a timestamped heart rate observation.
 */
data class HeartRateObservation(
    val timestamp: Instant,
    val bpm: Double
)

/**
 * Represents a single residual spike where observed heart rate deviated significantly from circadian expectation.
 */
data class ResidualSpike(
    val timestamp: Instant,
    val actualBpm: Double,
    val expectedBpm: Double,
    val deviationZScore: Double
)

/**
 * Comprehensive diurnal time-series decomposition result.
 */
data class CircadianDecompositionResult(
    val daytimeMeanHr: Double,
    val nocturnalMinHr: Double,
    val dipPercentage: Double,
    val pattern: AutonomicDippingPattern,
    val circadianStabilityIndex: Double,
    val residualSpikeCount: Int,
    val narrative: String,
    val hourlyProfile: List<HourlyDiurnalPoint> = emptyList(),
    val residualSpikes: List<ResidualSpike> = emptyList()
)
