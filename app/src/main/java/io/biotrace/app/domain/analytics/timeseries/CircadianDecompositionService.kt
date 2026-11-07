package io.biotrace.app.domain.analytics.timeseries

import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Service for 24-hour diurnal time-series decomposition, autonomic nocturnal dipping
 * classification, circadian stability index computation, and residual spike isolation.
 *
 * Fully deterministic and runs 100% on-device without network or vendor dependencies.
 */
object CircadianDecompositionService {

    /**
     * Decomposes timestamped heart rate observations into diurnal components.
     *
     * @param observations List of timestamped heart rate points
     * @param zoneId Timezone used to extract hour of the day (default: systemDefault)
     * @param daytimeStartHour Inclusive start of wakefulness window (default 7:00 AM)
     * @param daytimeEndHour Exclusive end of wakefulness window (default 22:00 / 10:00 PM)
     */
    fun decompose(
        observations: List<HeartRateObservation>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        daytimeStartHour: Int = 7,
        daytimeEndHour: Int = 22
    ): CircadianDecompositionResult {
        if (observations.isEmpty()) {
            return CircadianDecompositionResult(
                daytimeMeanHr = 0.0,
                nocturnalMinHr = 0.0,
                dipPercentage = 0.0,
                pattern = AutonomicDippingPattern.NON_DIPPER,
                circadianStabilityIndex = 0.0,
                residualSpikeCount = 0,
                narrative = "No heart rate records available in Health Connect to perform diurnal decomposition.",
                hourlyProfile = emptyList(),
                residualSpikes = emptyList()
            )
        }

        val overallMean = observations.map { it.bpm }.average()

        // Group observations by hour of day (0..23)
        val groupedByHour = observations.groupBy { it.timestamp.atZone(zoneId).hour }

        // Compute 24-hour profile
        val hourlyProfile = (0..23).map { hour ->
            val samples = groupedByHour[hour] ?: emptyList()
            if (samples.isNotEmpty()) {
                val mean = samples.map { it.bpm }.average()
                val min = samples.minOf { it.bpm }
                val max = samples.maxOf { it.bpm }
                HourlyDiurnalPoint(hour, mean, min, max, samples.size)
            } else {
                HourlyDiurnalPoint(hour, overallMean, overallMean, overallMean, 0)
            }
        }

        val hourlyMeanMap = hourlyProfile.associate { it.hourOfDay to it.meanBpm }

        // Partition observations into Daytime and Nocturnal
        val daytimeObservations = mutableListOf<HeartRateObservation>()
        val nocturnalObservations = mutableListOf<HeartRateObservation>()

        for (obs in observations) {
            val hour = obs.timestamp.atZone(zoneId).hour
            val isDaytime = if (daytimeStartHour <= daytimeEndHour) {
                hour in daytimeStartHour until daytimeEndHour
            } else {
                hour >= daytimeStartHour || hour < daytimeEndHour
            }

            if (isDaytime) {
                daytimeObservations.add(obs)
            } else {
                nocturnalObservations.add(obs)
            }
        }

        val daytimeMeanHr = if (daytimeObservations.isNotEmpty()) {
            daytimeObservations.map { it.bpm }.average()
        } else {
            overallMean
        }

        val nocturnalMinHr = if (nocturnalObservations.isNotEmpty()) {
            nocturnalObservations.minOf { it.bpm }
        } else {
            daytimeMeanHr
        }

        val dipPercentage = DiurnalDipCalculator.calculateDipPercentage(daytimeMeanHr, nocturnalMinHr)
        val pattern = DiurnalDipCalculator.classifyDippingPattern(dipPercentage)

        // Residual Analysis & Circadian Stability
        // Expected value for observation is the hourly mean of its corresponding hour
        var totalVarianceSum = 0.0
        var residualSumOfSquares = 0.0
        val residuals = mutableListOf<Double>()

        for (obs in observations) {
            val hour = obs.timestamp.atZone(zoneId).hour
            val expected = hourlyMeanMap[hour] ?: overallMean
            val residual = obs.bpm - expected
            residuals.add(residual)

            val totalDiff = obs.bpm - overallMean
            totalVarianceSum += totalDiff * totalDiff
            residualSumOfSquares += residual * residual
        }

        // Compute standard deviation of residuals
        val meanResidual = residuals.average()
        val residualVariance = if (residuals.size > 1) {
            residuals.sumOf { (it - meanResidual) * (it - meanResidual) } / (residuals.size - 1)
        } else {
            0.0
        }
        val residualStdDev = sqrt(residualVariance)

        // Identify residual spikes where |obs - expected| > 2 * stdDev
        val threshold = 2.0 * residualStdDev
        val residualSpikes = mutableListOf<ResidualSpike>()

        if (threshold > 1e-4) {
            for (obs in observations) {
                val hour = obs.timestamp.atZone(zoneId).hour
                val expected = hourlyMeanMap[hour] ?: overallMean
                val absDiff = abs(obs.bpm - expected)
                if (absDiff > threshold) {
                    val zScore = if (residualStdDev > 0) (obs.bpm - expected) / residualStdDev else 0.0
                    residualSpikes.add(ResidualSpike(obs.timestamp, obs.bpm, expected, zScore))
                }
            }
        }

        // Circadian Stability Index (fraction of total variance explained by the diurnal curve)
        val circadianStabilityIndex = if (totalVarianceSum > 1e-6) {
            (1.0 - (residualSumOfSquares / totalVarianceSum)).coerceIn(0.0, 1.0)
        } else {
            1.0
        }

        val narrative = buildNarrative(
            daytimeMean = daytimeMeanHr,
            nocturnalMin = nocturnalMinHr,
            dipPercentage = dipPercentage,
            pattern = pattern,
            stability = circadianStabilityIndex,
            spikesCount = residualSpikes.size
        )

        return CircadianDecompositionResult(
            daytimeMeanHr = daytimeMeanHr,
            nocturnalMinHr = nocturnalMinHr,
            dipPercentage = dipPercentage,
            pattern = pattern,
            circadianStabilityIndex = circadianStabilityIndex,
            residualSpikeCount = residualSpikes.size,
            narrative = narrative,
            hourlyProfile = hourlyProfile,
            residualSpikes = residualSpikes
        )
    }

    private fun buildNarrative(
        daytimeMean: Double,
        nocturnalMin: Double,
        dipPercentage: Double,
        pattern: AutonomicDippingPattern,
        stability: Double,
        spikesCount: Int
    ): String {
        val dipFormatted = String.format(Locale.US, "%.1f%%", dipPercentage)
        val dayFormatted = String.format(Locale.US, "%.1f", daytimeMean)
        val nightFormatted = String.format(Locale.US, "%.1f", nocturnalMin)
        val stabilityFormatted = String.format(Locale.US, "%.2f", stability)

        val clinicalInterpretation = when (pattern) {
            AutonomicDippingPattern.NORMAL_DIPPER ->
                "Normal nocturnal autonomic dipping ($dipFormatted dip from daytime mean $dayFormatted bpm to nocturnal minimum $nightFormatted bpm). " +
                    "Demonstrates healthy autonomic vagal modulation and restorative sleep physiology."

            AutonomicDippingPattern.EXTREME_DIPPER ->
                "Extreme nocturnal dipping ($dipFormatted dip from daytime mean $dayFormatted bpm to nocturnal minimum $nightFormatted bpm). " +
                    "Reflects marked parasympathetic dominance during nocturnal periods."

            AutonomicDippingPattern.NON_DIPPER ->
                "Blunted nocturnal dipping ($dipFormatted dip; daytime mean $dayFormatted bpm vs nocturnal minimum $nightFormatted bpm). " +
                    "A non-dipping diurnal profile suggests persistent nocturnal sympathetic activity or fragmented sleep architecture."

            AutonomicDippingPattern.REVERSE_DIPPER ->
                "Reverse diurnal dipping ($dipFormatted dip; nocturnal minimum $nightFormatted bpm exceeded daytime mean $dayFormatted bpm). " +
                    "A paradoxical nocturnal elevation is associated with circadian rhythm misalignment or elevated nocturnal autonomic stress."
        }

        val stabilityText = "Circadian stability index is $stabilityFormatted with $spikesCount transient residual spike${if (spikesCount == 1) "" else "s"} isolated from Health Connect biometric trends."

        return "$clinicalInterpretation $stabilityText"
    }
}
