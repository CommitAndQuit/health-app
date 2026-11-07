package io.biotrace.app.domain.analytics.correlation

import java.time.LocalDate

/**
 * Daily aggregated biometric observation for cross-metric time-series alignment.
 */
data class DailyMetricSample(
    val date: LocalDate,
    val value: Double
)
