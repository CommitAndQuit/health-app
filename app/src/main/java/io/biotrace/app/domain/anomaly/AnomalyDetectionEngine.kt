package io.biotrace.app.domain.anomaly

import io.biotrace.app.data.local.entity.LabBiomarkerEntity
import io.biotrace.app.data.local.entity.VitalsRecordEntity
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

enum class AnomalyRuleType {
    OUT_OF_RANGE_CLINICAL,
    LONGITUDINAL_TREND_SHIFT,
    PERSONAL_BASELINE_DEVIATION,
    MULTIVARIATE_COMPOUND_STRAIN
}

data class AnomalyAlert(
    val id: String,
    val ruleType: AnomalyRuleType,
    val metricName: String,
    val currentValue: Double,
    val baselineValue: Double?,
    val unit: String,
    val severity: String, // "INFO", "WARNING", "ALERT"
    val narrative: String,
    val detectedAt: Instant = Instant.now()
)

@Singleton
class AnomalyDetectionEngine @Inject constructor() {

    fun evaluateBiomarker(
        current: LabBiomarkerEntity,
        historical: List<LabBiomarkerEntity> = emptyList()
    ): List<AnomalyAlert> {
        val alerts = mutableListOf<AnomalyAlert>()

        // Rule 1: Out-of-Range Clinical Reference
        val min = current.referenceMin
        val max = current.referenceMax
        if (max != null && current.value > max) {
            val delta = ((current.value - max) / max) * 100
            alerts.add(
                AnomalyAlert(
                    id = "rule1_${current.id}",
                    ruleType = AnomalyRuleType.OUT_OF_RANGE_CLINICAL,
                    metricName = current.rawName,
                    currentValue = current.value,
                    baselineValue = max,
                    unit = current.unit,
                    severity = if (delta > 25.0) "ALERT" else "WARNING",
                    narrative = "${current.rawName} (${current.value} ${current.unit}) exceeds clinical upper limit ($max ${current.unit}) by ${delta.toInt()}%."
                )
            )
        } else if (min != null && current.value < min) {
            val delta = ((min - current.value) / min) * 100
            alerts.add(
                AnomalyAlert(
                    id = "rule1_${current.id}",
                    ruleType = AnomalyRuleType.OUT_OF_RANGE_CLINICAL,
                    metricName = current.rawName,
                    currentValue = current.value,
                    baselineValue = min,
                    unit = current.unit,
                    severity = if (delta > 25.0) "ALERT" else "WARNING",
                    narrative = "${current.rawName} (${current.value} ${current.unit}) sits below clinical lower limit ($min ${current.unit}) by ${delta.toInt()}%."
                )
            )
        }

        // Rule 2: 180-Day Longitudinal Shift
        val sameBiomarkerHistory = historical
            .filter { it.biomarkerName == current.biomarkerName && it.id != current.id }
            .sortedByDescending { it.recordedAt }

        if (sameBiomarkerHistory.isNotEmpty()) {
            val prior = sameBiomarkerHistory.first()
            if (prior.value > 0) {
                val shiftPct = ((current.value - prior.value) / prior.value) * 100
                if (abs(shiftPct) >= 20.0) {
                    alerts.add(
                        AnomalyAlert(
                            id = "rule2_${current.id}",
                            ruleType = AnomalyRuleType.LONGITUDINAL_TREND_SHIFT,
                            metricName = current.rawName,
                            currentValue = current.value,
                            baselineValue = prior.value,
                            unit = current.unit,
                            severity = "WARNING",
                            narrative = "${current.rawName} shifted by ${if (shiftPct > 0) "+" else ""}${shiftPct.toInt()}% compared to prior reading (${prior.value} ${prior.unit})."
                        )
                    )
                }
            }
        }

        return alerts
    }

    fun evaluateVitals(
        metricType: String,
        currentValue: Double,
        unit: String,
        recent30DaysRecords: List<VitalsRecordEntity>
    ): List<AnomalyAlert> {
        val alerts = mutableListOf<AnomalyAlert>()
        if (recent30DaysRecords.isEmpty()) return alerts

        val values = recent30DaysRecords.map { it.value }.sorted()
        val median = values[values.size / 2]

        // Rule 3: Personal Baseline Median Deviation (>15% shift over personal 30-day baseline)
        if (median > 0) {
            val deviationPct = ((currentValue - median) / median) * 100
            if (abs(deviationPct) >= 15.0) {
                val dir = if (deviationPct > 0) "elevation" else "reduction"
                alerts.add(
                    AnomalyAlert(
                        id = "rule3_${metricType}_${System.currentTimeMillis()}",
                        ruleType = AnomalyRuleType.PERSONAL_BASELINE_DEVIATION,
                        metricName = metricType,
                        currentValue = currentValue,
                        baselineValue = median,
                        unit = unit,
                        severity = "INFO",
                        narrative = "Observed 15%+ $dir in $metricType (${currentValue.toInt()} vs personal 30-day median of ${median.toInt()} $unit)."
                    )
                )
            }
        }

        return alerts
    }

    /**
     * Rule 4: Multivariate Compound Autonomic Strain.
     *
     * Evaluates compound strain across resting heart rate, heart rate variability, and sleep duration.
     * Flags when:
     * - HRV is depressed by > 1.8 * stdDev below baseline mean
     * - RHR is elevated by > 1.5 * stdDev above baseline mean
     * - Sleep is curtailed (< 6.0 hours)
     *
     * Anomaly alerts are strictly in-app only, with no external push or system tray dependencies.
     */
    fun evaluateCompoundStrain(
        currentRhr: Double,
        baselineRhrMean: Double,
        baselineRhrStdDev: Double,
        currentHrv: Double,
        baselineHrvMean: Double,
        baselineHrvStdDev: Double,
        recentSleepHours: Double
    ): AnomalyAlert? {
        if (baselineRhrStdDev <= 0.0 || baselineHrvStdDev <= 0.0) {
            return null
        }

        val hrvDepressed = (baselineHrvMean - currentHrv) > (1.8 * baselineHrvStdDev)
        val rhrElevated = (currentRhr - baselineRhrMean) > (1.5 * baselineRhrStdDev)
        val sleepShort = recentSleepHours < 6.0

        if (hrvDepressed && rhrElevated && sleepShort) {
            val rhrZScore = (currentRhr - baselineRhrMean) / baselineRhrStdDev
            val hrvZScore = (baselineHrvMean - currentHrv) / baselineHrvStdDev

            return AnomalyAlert(
                id = "rule4_compound_${System.currentTimeMillis()}",
                ruleType = AnomalyRuleType.MULTIVARIATE_COMPOUND_STRAIN,
                metricName = "COMPOUND_AUTONOMIC_STRAIN",
                currentValue = currentRhr,
                baselineValue = baselineRhrMean,
                unit = "bpm",
                severity = "ALERT",
                narrative = "Multivariate autonomic strain detected: Resting HR is elevated (+${"%.1f".format(rhrZScore)}σ), HRV RMSSD is depressed (-${"%.1f".format(hrvZScore)}σ), and recent sleep was insufficient (${recentSleepHours}h < 6.0h). In-app recovery protocol advised."
            )
        }

        return null
    }
}
