package io.biotrace.app.domain.analytics.timeseries

/**
 * Evaluates nocturnal autonomic dipping parameters and classifies chronobiological diurnal profiles.
 */
object DiurnalDipCalculator {

    /**
     * Evaluates nocturnal dip percentage:
     * ((dayMean - nightMin) / dayMean) * 100%
     *
     * @param daytimeMeanHr Average heart rate during wakeful daytime hours (e.g. 07:00 - 22:00)
     * @param nocturnalMinHr Minimum heart rate reached during nocturnal sleep hours (e.g. 22:00 - 07:00)
     * @return Dip percentage, or 0.0 if daytimeMeanHr is non-positive.
     */
    fun calculateDipPercentage(daytimeMeanHr: Double, nocturnalMinHr: Double): Double {
        if (daytimeMeanHr <= 1e-6 || daytimeMeanHr.isNaN() || nocturnalMinHr.isNaN()) {
            return 0.0
        }
        return ((daytimeMeanHr - nocturnalMinHr) / daytimeMeanHr) * 100.0
    }

    /**
     * Categorizes autonomic dipping pattern based on dipping percentage:
     * - EXTREME_DIPPER: dip > 20%
     * - NORMAL_DIPPER: 10% <= dip <= 20%
     * - NON_DIPPER: 0% <= dip < 10%
     * - REVERSE_DIPPER: dip < 0%
     */
    fun classifyDippingPattern(dipPercentage: Double): AutonomicDippingPattern {
        return when {
            dipPercentage.isNaN() -> AutonomicDippingPattern.NON_DIPPER
            dipPercentage > 20.0 -> AutonomicDippingPattern.EXTREME_DIPPER
            dipPercentage >= 10.0 -> AutonomicDippingPattern.NORMAL_DIPPER
            dipPercentage >= 0.0 -> AutonomicDippingPattern.NON_DIPPER
            else -> AutonomicDippingPattern.REVERSE_DIPPER
        }
    }
}
