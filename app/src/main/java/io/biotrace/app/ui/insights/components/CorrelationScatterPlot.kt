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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.biotrace.app.domain.analytics.correlation.CorrelationStrength
import io.biotrace.app.ui.theme.BioOrangePrimary
import io.biotrace.app.ui.theme.BioOrangeSoftTint
import io.biotrace.app.ui.theme.BioTraceTokens
import io.biotrace.app.ui.theme.CardSurface
import io.biotrace.app.ui.theme.CobaltBluePrimary
import io.biotrace.app.ui.theme.CobaltBlueSoftTint
import io.biotrace.app.ui.theme.CoralAlertPrimary
import io.biotrace.app.ui.theme.CoralAlertSoftTint
import io.biotrace.app.ui.theme.DividerSubtle
import io.biotrace.app.ui.theme.EmeraldPrimary
import io.biotrace.app.ui.theme.EmeraldSoftTint
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
 * Visual scatter plot canvas rendering paired biometric observations,
 * best-fit linear regression trendline (y = mx + b), and correlation badges.
 */
@Composable
fun CorrelationScatterPlot(
    points: List<Pair<Double, Double>>,
    xLabel: String,
    yLabel: String,
    pearsonR: Double,
    spearmanRho: Double? = null,
    modifier: Modifier = Modifier,
    pointColor: Color = CobaltBluePrimary,
    trendlineColor: Color = BioOrangePrimary
) {
    val strength = CorrelationStrength.fromCoefficient(pearsonR)
    val badgeColors = getStrengthColors(strength)

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
            // Header Row: Metric pair & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$xLabel vs $yLabel",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Cross-metric correlation scatter",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                // Correlation Coefficient Badge
                Box(
                    modifier = Modifier
                        .background(badgeColors.second, shape = BioTraceTokens.PillShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    val sign = if (pearsonR > 0) "+" else ""
                    val rFormatted = String.format(Locale.US, "%.2f", pearsonR)
                    Text(
                        text = "r = $sign$rFormatted",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeColors.first
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-metrics chip row: Strength description & Spearman rho
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(InnerWellSurface, shape = BioTraceTokens.InnerWellSmallShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = strength.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = badgeColors.first
                    )
                }

                spearmanRho?.let { rho ->
                    Box(
                        modifier = Modifier
                            .background(InnerWellSurface, shape = BioTraceTokens.InnerWellSmallShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        val rhoFormatted = String.format(Locale.US, "%.2f", rho)
                        Text(
                            text = "Spearman ρ = $rhoFormatted",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(InnerWellSurface, shape = BioTraceTokens.InnerWellSmallShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "n = ${points.size} pts",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scatter Plot Canvas
            if (points.size < 2) {
                EmptyScatterPlotCanvas(
                    xLabel = xLabel,
                    yLabel = yLabel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            } else {
                ActiveScatterPlotCanvas(
                    points = points,
                    xLabel = xLabel,
                    yLabel = yLabel,
                    pointColor = pointColor,
                    trendlineColor = trendlineColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }
    }
}

@Composable
private fun ActiveScatterPlotCanvas(
    points: List<Pair<Double, Double>>,
    xLabel: String,
    yLabel: String,
    pointColor: Color,
    trendlineColor: Color,
    modifier: Modifier = Modifier
) {
    val xValues = points.map { it.first }
    val yValues = points.map { it.second }

    val rawMinX = xValues.minOrNull() ?: 0.0
    val rawMaxX = xValues.maxOrNull() ?: 1.0
    val rawMinY = yValues.minOrNull() ?: 0.0
    val rawMaxY = yValues.maxOrNull() ?: 1.0

    val spanX = if (rawMaxX > rawMinX) rawMaxX - rawMinX else 1.0
    val spanY = if (rawMaxY > rawMinY) rawMaxY - rawMinY else 1.0

    // Add 8% margin around bounds
    val minX = rawMinX - spanX * 0.08
    val maxX = rawMaxX + spanX * 0.08
    val minY = rawMinY - spanY * 0.08
    val maxY = rawMaxY + spanY * 0.08
    val rangeX = max(maxX - minX, 0.001)
    val rangeY = max(maxY - minY, 0.001)

    // Linear Regression calculation: y = m*x + b
    val n = points.size.toDouble()
    val sumX = xValues.sum()
    val sumY = yValues.sum()
    val sumXY = points.sumOf { it.first * it.second }
    val sumXSquare = xValues.sumOf { it * it }

    val denom = n * sumXSquare - sumX * sumX
    val hasValidRegression = denom != 0.0 && !denom.isNaN()
    val slope = if (hasValidRegression) (n * sumXY - sumX * sumY) / denom else 0.0
    val intercept = if (hasValidRegression) (sumY - slope * sumX) / n else 0.0

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val paddingLeft = 36f
        val paddingRight = 16f
        val paddingTop = 16f
        val paddingBottom = 32f

        val plotWidth = width - paddingLeft - paddingRight
        val plotHeight = height - paddingTop - paddingBottom

        fun mapToCanvas(x: Double, y: Double): Offset {
            val cx = paddingLeft + ((x - minX) / rangeX).toFloat() * plotWidth
            val cy = paddingTop + (1f - ((y - minY) / rangeY).toFloat()) * plotHeight
            return Offset(cx, cy)
        }

        // Draw grid lines
        val gridLines = 4
        for (i in 0..gridLines) {
            val frac = i / gridLines.toFloat()
            val gy = paddingTop + frac * plotHeight
            drawLine(
                color = DividerSubtle,
                start = Offset(paddingLeft, gy),
                end = Offset(width - paddingRight, gy),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )

            val gx = paddingLeft + frac * plotWidth
            drawLine(
                color = DividerSubtle,
                start = Offset(gx, paddingTop),
                end = Offset(gx, height - paddingBottom),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
        }

        // Draw best-fit regression trendline
        if (hasValidRegression) {
            val x1 = minX
            val y1 = slope * x1 + intercept
            val x2 = maxX
            val y2 = slope * x2 + intercept

            val startOffset = mapToCanvas(x1, y1)
            val endOffset = mapToCanvas(x2, y2)

            drawLine(
                color = trendlineColor,
                start = startOffset,
                end = endOffset,
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
        }

        // Draw scatter data points
        for (point in points) {
            val center = mapToCanvas(point.first, point.second)

            // Outer soft glow halo
            drawCircle(
                color = pointColor.copy(alpha = 0.22f),
                radius = 7.dp.toPx(),
                center = center
            )
            // Core point circle
            drawCircle(
                color = pointColor,
                radius = 3.5.dp.toPx(),
                center = center
            )
            // White highlight center
            drawCircle(
                color = Color.White,
                radius = 1.2.dp.toPx(),
                center = center
            )
        }

        // Axis base lines
        drawLine(
            color = DividerSubtle,
            start = Offset(paddingLeft, height - paddingBottom),
            end = Offset(width - paddingRight, height - paddingBottom),
            strokeWidth = 1.5f
        )
        drawLine(
            color = DividerSubtle,
            start = Offset(paddingLeft, paddingTop),
            end = Offset(paddingLeft, height - paddingBottom),
            strokeWidth = 1.5f
        )
    }

    // Bottom Axis Labels
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 36.dp, end = 16.dp, top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val minXFormatted = String.format(Locale.US, "%.1f", rawMinX)
        val maxXFormatted = String.format(Locale.US, "%.1f", rawMaxX)
        Text(text = minXFormatted, fontSize = 10.sp, color = TextMuted)
        Text(text = xLabel, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
        Text(text = maxXFormatted, fontSize = 10.sp, color = TextMuted)
    }
}

@Composable
private fun EmptyScatterPlotCanvas(
    xLabel: String,
    yLabel: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(InnerWellSurface, shape = BioTraceTokens.InnerWellShape)
            .border(BioTraceTokens.WellHairlineBorder, shape = BioTraceTokens.InnerWellShape),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Awaiting Aligned Biometric Data",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Health Connect requires at least 2 paired observations to plot $xLabel vs $yLabel.",
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}

private fun getStrengthColors(strength: CorrelationStrength): Pair<Color, Color> {
    return when (strength) {
        CorrelationStrength.STRONG_POSITIVE -> Pair(EmeraldPrimary, EmeraldSoftTint)
        CorrelationStrength.MODERATE_POSITIVE -> Pair(CobaltBluePrimary, CobaltBlueSoftTint)
        CorrelationStrength.WEAK_POSITIVE -> Pair(SolarAmberPrimary, SolarAmberSoftTint)
        CorrelationStrength.NEUTRAL -> Pair(TextSecondary, InnerWellSurface)
        CorrelationStrength.WEAK_NEGATIVE -> Pair(SolarAmberPrimary, SolarAmberSoftTint)
        CorrelationStrength.MODERATE_NEGATIVE -> Pair(BioOrangePrimary, BioOrangeSoftTint)
        CorrelationStrength.STRONG_NEGATIVE -> Pair(CoralAlertPrimary, CoralAlertSoftTint)
        else -> Pair(TextSecondary, InnerWellSurface)
    }
}
