package io.biotrace.app.ui.insights.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.biotrace.app.domain.analytics.timeseries.AutonomicDippingPattern
import io.biotrace.app.domain.analytics.timeseries.CircadianDecompositionResult
import io.biotrace.app.domain.analytics.timeseries.HourlyDiurnalPoint
import io.biotrace.app.ui.theme.BioOrangePrimary
import io.biotrace.app.ui.theme.BioOrangeSoftTint
import io.biotrace.app.ui.theme.BioTraceTokens
import io.biotrace.app.ui.theme.CardSurface
import io.biotrace.app.ui.theme.CobaltBluePrimary
import io.biotrace.app.ui.theme.CoralAlertPrimary
import io.biotrace.app.ui.theme.CoralAlertSoftTint
import io.biotrace.app.ui.theme.DividerSubtle
import io.biotrace.app.ui.theme.EmeraldPrimary
import io.biotrace.app.ui.theme.EmeraldSoftTint
import io.biotrace.app.ui.theme.IndigoSleepPrimary
import io.biotrace.app.ui.theme.IndigoSleepSoftTint
import io.biotrace.app.ui.theme.InnerWellSurface
import io.biotrace.app.ui.theme.SolarAmberPrimary
import io.biotrace.app.ui.theme.SolarAmberSoftTint
import io.biotrace.app.ui.theme.TextMuted
import io.biotrace.app.ui.theme.TextPrimary
import io.biotrace.app.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Material 3 Card displaying 24-hour diurnal heart rate trajectory,
 * daytime vs nocturnal shaded chronobiology zones, nocturnal dipping badge,
 * and clinical autonomic interpretation chips.
 */
@Composable
fun CircadianRhythmCard(
    result: CircadianDecompositionResult,
    modifier: Modifier = Modifier
) {
    val patternColors = getPatternColors(result.pattern)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = BioTraceTokens.BentoCardShape,
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BioTraceTokens.CardHairlineBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row: Title & Nocturnal Dip Percentage Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Circadian Autonomic Dipping",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "24-Hour Diurnal Heart Rate Profile",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                // Dipping percentage badge
                Box(
                    modifier = Modifier
                        .background(patternColors.second, shape = BioTraceTokens.PillShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    val dipText = String.format(Locale.US, "%.1f%% Dip", result.dipPercentage)
                    Text(
                        text = dipText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = patternColors.first
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Clinical Pattern Chip & Stability Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Autonomic dipping pattern chip
                Box(
                    modifier = Modifier
                        .background(patternColors.second, shape = BioTraceTokens.PillShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = result.pattern.displayName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = patternColors.first
                    )
                }

                // Circadian Stability Chip
                Box(
                    modifier = Modifier
                        .background(InnerWellSurface, shape = BioTraceTokens.InnerWellSmallShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    val stabilityFormatted = String.format(Locale.US, "Stability: %.2f", result.circadianStabilityIndex)
                    Text(
                        text = stabilityFormatted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }

                // Spikes Chip
                if (result.residualSpikeCount > 0) {
                    Box(
                        modifier = Modifier
                            .background(SolarAmberSoftTint, shape = BioTraceTokens.InnerWellSmallShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${result.residualSpikeCount} Spikes",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = SolarAmberPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 24-Hour Diurnal Canvas Chart
            DiurnalCurveCanvas(
                hourlyProfile = result.hourlyProfile,
                daytimeMean = result.daytimeMeanHr,
                nocturnalMin = result.nocturnalMinHr,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )

            // Hour Axis Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "00:00 (Night)", fontSize = 10.sp, color = IndigoSleepPrimary)
                Text(text = "06:00", fontSize = 10.sp, color = TextMuted)
                Text(text = "12:00 (Day)", fontSize = 10.sp, color = BioOrangePrimary)
                Text(text = "18:00", fontSize = 10.sp, color = TextMuted)
                Text(text = "24:00 (Night)", fontSize = 10.sp, color = IndigoSleepPrimary)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Recessed Metrics Well
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(InnerWellSurface, shape = BioTraceTokens.InnerWellShape)
                    .border(BioTraceTokens.WellHairlineBorder, shape = BioTraceTokens.InnerWellShape)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    DiurnalMetricItem(
                        label = "Day Mean",
                        value = String.format(Locale.US, "%.0f", result.daytimeMeanHr),
                        unit = "bpm"
                    )
                    DiurnalMetricItem(
                        label = "Night Min",
                        value = String.format(Locale.US, "%.0f", result.nocturnalMinHr),
                        unit = "bpm"
                    )
                    DiurnalMetricItem(
                        label = "Nocturnal Dip",
                        value = String.format(Locale.US, "%.1f", result.dipPercentage),
                        unit = "%"
                    )
                    DiurnalMetricItem(
                        label = "Stability",
                        value = String.format(Locale.US, "%.2f", result.circadianStabilityIndex),
                        unit = "IS"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Clinical Narrative
            Text(
                text = result.narrative,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun DiurnalMetricItem(
    label: String,
    value: String,
    unit: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = unit,
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 1.dp)
            )
        }
    }
}

@Composable
private fun DiurnalCurveCanvas(
    hourlyProfile: List<HourlyDiurnalPoint>,
    daytimeMean: Double,
    nocturnalMin: Double,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val paddingTop = 12f
        val paddingBottom = 16f
        val paddingHorizontal = 12f

        val chartWidth = width - paddingHorizontal * 2
        val chartHeight = height - paddingTop - paddingBottom

        // Chronobiology Background Shading:
        // Nocturnal Zone 1: 00:00 - 07:00 (hours 0..7)
        val night1Width = chartWidth * (7f / 24f)
        drawRect(
            color = IndigoSleepSoftTint.copy(alpha = 0.55f),
            topLeft = Offset(paddingHorizontal, paddingTop),
            size = androidx.compose.ui.geometry.Size(night1Width, chartHeight)
        )

        // Daytime Zone: 07:00 - 22:00 (hours 7..22)
        val dayWidth = chartWidth * (15f / 24f)
        drawRect(
            color = BioOrangeSoftTint.copy(alpha = 0.20f),
            topLeft = Offset(paddingHorizontal + night1Width, paddingTop),
            size = androidx.compose.ui.geometry.Size(dayWidth, chartHeight)
        )

        // Nocturnal Zone 2: 22:00 - 24:00 (hours 22..24)
        val night2Width = chartWidth * (2f / 24f)
        drawRect(
            color = IndigoSleepSoftTint.copy(alpha = 0.55f),
            topLeft = Offset(paddingHorizontal + night1Width + dayWidth, paddingTop),
            size = androidx.compose.ui.geometry.Size(night2Width, chartHeight)
        )

        if (hourlyProfile.isEmpty()) {
            return@Canvas
        }

        val allBpm = hourlyProfile.map { it.meanBpm }
        val rawMinBpm = min(allBpm.minOrNull() ?: 50.0, nocturnalMin)
        val rawMaxBpm = max(allBpm.maxOrNull() ?: 100.0, daytimeMean)
        val bpmSpan = if (rawMaxBpm > rawMinBpm) rawMaxBpm - rawMinBpm else 20.0

        val minBpm = rawMinBpm - bpmSpan * 0.12
        val maxBpm = rawMaxBpm + bpmSpan * 0.12
        val bpmRange = max(maxBpm - minBpm, 1.0)

        fun mapPoint(hour: Int, bpm: Double): Offset {
            val fracX = (hour.coerceIn(0, 23)) / 23f
            val x = paddingHorizontal + fracX * chartWidth
            val fracY = ((bpm - minBpm) / bpmRange).toFloat().coerceIn(0f, 1f)
            val y = paddingTop + (1f - fracY) * chartHeight
            return Offset(x, y)
        }

        // Draw Daytime Mean reference dashed line
        if (daytimeMean > 0) {
            val meanFracY = ((daytimeMean - minBpm) / bpmRange).toFloat().coerceIn(0f, 1f)
            val meanY = paddingTop + (1f - meanFracY) * chartHeight
            drawLine(
                color = BioOrangePrimary.copy(alpha = 0.45f),
                start = Offset(paddingHorizontal, meanY),
                end = Offset(width - paddingHorizontal, meanY),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )
        }

        // Construct smooth curve path
        val strokePath = Path()
        val fillPath = Path()

        val points = hourlyProfile.sortedBy { it.hourOfDay }.map { mapPoint(it.hourOfDay, it.meanBpm) }

        if (points.isNotEmpty()) {
            strokePath.moveTo(points[0].x, points[0].y)
            fillPath.moveTo(points[0].x, height - paddingBottom)
            fillPath.lineTo(points[0].x, points[0].y)

            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val midX = (p0.x + p1.x) / 2f
                strokePath.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                fillPath.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
            }

            fillPath.lineTo(points.last().x, height - paddingBottom)
            fillPath.close()

            // Fill gradient below curve
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CobaltBluePrimary.copy(alpha = 0.28f),
                        CobaltBluePrimary.copy(alpha = 0.02f)
                    ),
                    startY = paddingTop,
                    endY = height - paddingBottom
                )
            )

            // Draw line
            drawPath(
                path = strokePath,
                color = CobaltBluePrimary,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw hourly point dots
            for (p in points) {
                drawCircle(
                    color = CobaltBluePrimary,
                    radius = 2.dp.toPx(),
                    center = p
                )
            }

            // Find and highlight Nocturnal Minimum point
            val nocturnalPoint = hourlyProfile.filter { it.hourOfDay in 0..6 || it.hourOfDay in 22..23 }
                .minByOrNull { it.meanBpm }
            if (nocturnalPoint != null) {
                val minCenter = mapPoint(nocturnalPoint.hourOfDay, nocturnalPoint.meanBpm)
                drawCircle(
                    color = IndigoSleepPrimary.copy(alpha = 0.3f),
                    radius = 8.dp.toPx(),
                    center = minCenter
                )
                drawCircle(
                    color = IndigoSleepPrimary,
                    radius = 4.dp.toPx(),
                    center = minCenter
                )
                drawCircle(
                    color = Color.White,
                    radius = 1.8.dp.toPx(),
                    center = minCenter
                )
            }
        }
    }
}

private fun getPatternColors(pattern: AutonomicDippingPattern): Pair<Color, Color> {
    return when (pattern) {
        AutonomicDippingPattern.NORMAL_DIPPER -> Pair(EmeraldPrimary, EmeraldSoftTint)
        AutonomicDippingPattern.EXTREME_DIPPER -> Pair(SolarAmberPrimary, SolarAmberSoftTint)
        AutonomicDippingPattern.NON_DIPPER -> Pair(BioOrangePrimary, BioOrangeSoftTint)
        AutonomicDippingPattern.REVERSE_DIPPER -> Pair(CoralAlertPrimary, CoralAlertSoftTint)
    }
}
