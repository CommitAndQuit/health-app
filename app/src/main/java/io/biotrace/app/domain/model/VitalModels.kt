package io.biotrace.app.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.Instant

enum class VitalMetricType(
    val title: String,
    val defaultUnit: String,
    val goalDescription: String,
    val defaultGoalValue: Double,
    val accentColor: Color
) {
    HEART_RATE("Heart Rate", "bpm", "60–100 bpm", 70.0, Color(0xFFFF5722)),
    RESTING_HEART_RATE("Resting HR", "bpm", "< 65 bpm", 60.0, Color(0xFFEF4444)),
    STRESS("Stress Score", "", "< 60 Normal", 59.0, Color(0xFF3B82F6)),
    SPO2("Blood Oxygen", "%", "≥ 95% Normal", 95.0, Color(0xFF0EA5E9)),
    SLEEP("Sleep", "h m", "7h 00m Goal", 420.0, Color(0xFF8B5CF6)),
    STEPS("Daily Steps", "steps", "8,000 Goal", 8000.0, Color(0xFF10B981)),
    CALORIES_DISTANCE("Calories & Dist", "kcal", "500 kcal Goal", 500.0, Color(0xFFFF9800)),
    VO2_MAX_WORKOUTS("Cardio Fitness", "mL/kg/min", "VO2 Max & Workouts", 40.0, Color(0xFF6366F1)),
    BODY_METRICS("Body Metrics", "kg", "BMI 18.5–24.9", 22.0, Color(0xFF64748B));

    val icon: ImageVector
        get() = when (this) {
            HEART_RATE -> Icons.Default.Favorite
            RESTING_HEART_RATE -> Icons.Default.MonitorHeart
            STRESS -> Icons.Default.Psychology
            SPO2 -> Icons.Default.Air
            SLEEP -> Icons.Default.Nightlight
            STEPS -> Icons.AutoMirrored.Filled.DirectionsWalk
            CALORIES_DISTANCE -> Icons.Default.LocalFireDepartment
            VO2_MAX_WORKOUTS -> Icons.Default.FitnessCenter
            BODY_METRICS -> Icons.Default.Scale
        }
}

enum class MetricTimeRange(val label: String, val days: Int) {
    DAY("Day", 1),
    WEEK("7D", 7),
    MONTH("30D", 30),
    THREE_MONTHS("90D", 90)
}

data class StatusBadge(
    val text: String,
    val textColor: Color,
    val backgroundColor: Color
)

data class StatItem(
    val label: String,
    val value: String,
    val unit: String = "",
    val subtext: String? = null
)

data class ChartPoint(
    val timestamp: Instant,
    val xLabel: String,
    val value: Float,
    val secondaryValue: Float? = null,
    val color: Color? = null,
    val note: String? = null
)

data class RangeBarItem(
    val label: String,
    val date: Instant,
    val min: Float,
    val avg: Float,
    val max: Float,
    val color: Color? = null
)

data class SleepStageBarItem(
    val label: String,
    val date: Instant,
    val deepMinutes: Float,
    val lightMinutes: Float,
    val remMinutes: Float,
    val awakeMinutes: Float,
    val totalMinutes: Float
)

data class StressZoneDistribution(
    val relaxedCount: Int,
    val normalCount: Int,
    val mediumCount: Int,
    val highCount: Int,
    val totalCount: Int
) {
    val relaxedPct: Float get() = if (totalCount > 0) relaxedCount.toFloat() / totalCount else 0f
    val normalPct: Float get() = if (totalCount > 0) normalCount.toFloat() / totalCount else 0f
    val mediumPct: Float get() = if (totalCount > 0) mediumCount.toFloat() / totalCount else 0f
    val highPct: Float get() = if (totalCount > 0) highCount.toFloat() / totalCount else 0f
}

data class SleepStagesBreakdown(
    val deepMinutes: Int,
    val lightMinutes: Int,
    val remMinutes: Int,
    val awakeMinutes: Int,
    val totalMinutes: Int
) {
    val deepPct: Int get() = if (totalMinutes > 0) (deepMinutes * 100) / totalMinutes else 0
    val lightPct: Int get() = if (totalMinutes > 0) (lightMinutes * 100) / totalMinutes else 0
    val remPct: Int get() = if (totalMinutes > 0) (remMinutes * 100) / totalMinutes else 0
    val awakePct: Int get() = if (totalMinutes > 0) (awakeMinutes * 100) / totalMinutes else 0
}

data class VitalHistoryItem(
    val id: String,
    val timestamp: Instant,
    val formattedTime: String,
    val primaryValue: String,
    val primaryUnit: String,
    val secondaryDetail: String? = null,
    val badge: StatusBadge? = null,
    val deviceSource: String = "HONOR MagicWatch 2 • Gadgetbridge"
)

data class VitalSummaryCardModel(
    val metricType: VitalMetricType,
    val primaryValue: String,
    val primaryUnit: String,
    val secondaryText: String,
    val badgeText: String? = null,
    val badgeColor: Color? = null,
    val badgeBgColor: Color? = null,
    val progress: Float? = null,
    val sparklinePoints: List<Float>? = null,
    val actionText: String? = null,
    val stressLevel: StressLevel? = null,
    val sleepStageProportions: List<Float>? = null, // [deep, light, rem, awake]
    val isFallbackComputed: Boolean = false
)

data class VitalDetailUiState(
    val metricType: VitalMetricType,
    val selectedRange: MetricTimeRange = MetricTimeRange.DAY,
    val headlineValue: String = "--",
    val headlineUnit: String = "",
    val headlineDateLabel: String = "",
    val subtitle: String = "",
    val statusBadge: StatusBadge? = null,
    val summaryStats: List<StatItem> = emptyList(),
    val points: List<ChartPoint> = emptyList(),
    val rangeBars: List<RangeBarItem> = emptyList(),
    val sleepBars: List<SleepStageBarItem> = emptyList(),
    val stressDistribution: StressZoneDistribution? = null,
    val sleepBreakdown: SleepStagesBreakdown? = null,
    val historyRecords: List<VitalHistoryItem> = emptyList(),
    val referenceLineValue: Float? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isHistoricalAnchor: Boolean = false,
    val anchorNotice: String? = null
)
