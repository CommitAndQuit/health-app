package io.biotrace.app.domain.model

import java.time.Instant

data class VitalPoint(
    val id: String,
    val timestamp: Instant,
    val value: Double,
    val unit: String,
    val metricType: String
)

enum class TimeRangeSelection(val days: Long) {
    DAY(1),
    WEEK(7),
    MONTH(30)
}

data class AggregatedStats(
    val min: Double,
    val max: Double,
    val avg: Double,
    val count: Int
)

data class MetricOverview(
    val metricType: String,
    val title: String,
    val unit: String,
    val latestValue: Double?,
    val averageValue: Double?,
    val trend: Double?, // change compared to previous period
    val history: List<VitalPoint> = emptyList()
)
