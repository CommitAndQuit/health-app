package io.biotrace.app.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.biotrace.app.ui.theme.BioOrangePrimary
import io.biotrace.app.ui.theme.BioOrangeSoftTint
import io.biotrace.app.ui.theme.CobaltBluePrimary
import io.biotrace.app.ui.theme.CobaltBlueSoftTint
import io.biotrace.app.ui.theme.EmeraldPrimary
import io.biotrace.app.ui.theme.EmeraldSoftTint
import io.biotrace.app.ui.theme.IndigoSleepPrimary
import io.biotrace.app.ui.theme.IndigoSleepSoftTint
import io.biotrace.app.ui.theme.SolarAmberPrimary
import io.biotrace.app.ui.theme.SolarAmberSoftTint
import java.time.Instant

enum class VitalMetricType(
    val title: String,
    val defaultUnit: String,
    val goalDescription: String,
    val defaultGoalValue: Double,
    val accentColor: Color,
    val softTintColor: Color
) {
    HEART_RATE("Heart Rate", "bpm", "60–100 bpm", 70.0, BioOrangePrimary, BioOrangeSoftTint),
    RESTING_HEART_RATE("Resting Heart Rate", "bpm", "< 65 bpm", 60.0, BioOrangePrimary, BioOrangeSoftTint),
    SPO2("Blood Oxygen", "SpO2", "≥ 95% Normal", 95.0, CobaltBluePrimary, CobaltBlueSoftTint),
    SLEEP("Sleep Recovery", "score", "7h 00m Goal", 420.0, IndigoSleepPrimary, IndigoSleepSoftTint),
    STEPS("Daily Steps", "steps", "8,000 Goal", 8000.0, SolarAmberPrimary, SolarAmberSoftTint),
    CALORIES_DISTANCE("Active Calories", "kcal", "500 kcal Goal", 500.0, BioOrangePrimary, BioOrangeSoftTint),
    BODY_METRICS("Body Mass & Height", "kg", "BMI 18.5–24.9", 22.0, CobaltBluePrimary, CobaltBlueSoftTint);

    val icon: ImageVector
        get() = when (this) {
            HEART_RATE -> Icons.Default.Favorite
            RESTING_HEART_RATE -> Icons.Default.MonitorHeart
            SPO2 -> Icons.Default.Air
            SLEEP -> Icons.Default.Nightlight
            STEPS -> Icons.AutoMirrored.Filled.DirectionsWalk
            CALORIES_DISTANCE -> Icons.Default.LocalFireDepartment
            BODY_METRICS -> Icons.Default.Scale
        }
}

enum class BentoCardTemplate {
    TEMPLATE_A, // Primary Bento: Top title/icon, Center big number, Bottom progress/pill
    TEMPLATE_B  // Integrated Biomarker: Top icon/pill, Center big value, Bottom recessed 2-col well
}

enum class MetricTimeRange(val label: String, val days: Int) {
    DAY("Day", 1),
    WEEK("7 Days", 7),
    MONTH("30 Days", 30),
    THREE_MONTHS("90 Days", 90)
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
    val deviceSource: String = "Health Connect"
)

data class ContinuousHeartRateHeroData(
    val latestBpm: Int? = null,
    val restingHrBpm: Int? = null,
    val dailyMinBpm: Int? = null,
    val dailyMaxBpm: Int? = null,
    val samples: List<ChartPoint> = emptyList(),
    val timeLabels: List<String> = listOf("06:00", "09:00", "12:00", "15:00", "18:00", "Now")
)

data class ProfileTriptychSummary(
    val userName: String = "BioTrace User",
    val deviceLabel: String = "Health Connect",
    val cardioStatus: String = "CARDIO • NO DATA",
    val restingHr: String = "-- bpm",
    val restingHrDelta: String = "No Data",
    val sleepScore: String = "--%",
    val sleepStatus: String = "No Data"
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
    val sleepStageProportions: List<Float>? = null,
    val isFallbackComputed: Boolean = false,
    val template: BentoCardTemplate = BentoCardTemplate.TEMPLATE_A,
    val deltaText: String? = null,
    val goalText: String? = null,
    val recessedWellLeftLabel: String? = null,
    val recessedWellLeftValue: String? = null,
    val recessedWellRightLabel: String? = null,
    val recessedWellRightValue: String? = null,
    val recessedWellRightColor: Color? = null
)

data class AlertBannerData(
    val headline: String,
    val subtitle: String,
    val actionText: String? = null
)

data class VitalDetailUiState(
    val metricType: VitalMetricType,
    val selectedRange: MetricTimeRange = MetricTimeRange.DAY,
    val headlineValue: String = "--",
    val headlineUnit: String = "",
    val headlineDateLabel: String = "",
    val subtitle: String = "",
    val statusBadge: StatusBadge? = null,
    val calloutInsight: String = "",
    val summaryStats: List<StatItem> = emptyList(),
    val points: List<ChartPoint> = emptyList(),
    val rangeBars: List<RangeBarItem> = emptyList(),
    val sleepBars: List<SleepStageBarItem> = emptyList(),
    val sleepBreakdown: SleepStagesBreakdown? = null,
    val historyRecords: List<VitalHistoryItem> = emptyList(),
    val alertBanner: AlertBannerData? = null,
    val referenceLineValue: Float? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isHistoricalAnchor: Boolean = false,
    val anchorNotice: String? = null,
    val dataSourceLabel: String = "Health Connect"
)
