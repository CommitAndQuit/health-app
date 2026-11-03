package io.biotrace.app.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.biotrace.app.domain.model.VitalPoint

@Composable
fun SimpleLineChart(
    points: List<VitalPoint>,
    lineColor: Color,
    modifier: Modifier = Modifier.fillMaxWidth().height(120.dp)
) {
    if (points.size < 2) {
        Canvas(modifier = modifier) {
            // Draw empty guideline
            val y = size.height / 2
            drawLine(
                color = lineColor.copy(alpha = 0.2f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2f
            )
        }
        return
    }

    val values = points.map { it.value }
    val minVal = values.minOrNull() ?: 0.0
    val maxVal = values.maxOrNull() ?: 1.0
    val range = if (maxVal == minVal) 1.0 else maxVal - minVal

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = 16f
        val chartHeight = height - padding * 2
        val chartWidth = width - padding * 2

        val stepX = chartWidth / (points.size - 1)

        val strokePath = Path()
        val fillPath = Path()

        points.forEachIndexed { index, point ->
            val x = padding + index * stepX
            val normalizedY = ((point.value - minVal) / range).toFloat()
            val y = height - padding - (normalizedY * chartHeight)

            if (index == 0) {
                strokePath.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                strokePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            // Draw point dot
            drawCircle(
                color = lineColor,
                radius = 3f,
                center = Offset(x, y)
            )
        }

        fillPath.lineTo(padding + (points.size - 1) * stepX, height)
        fillPath.close()

        // Draw gradient area below curve
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.25f), Color.Transparent),
                startY = padding,
                endY = height
            )
        )

        // Draw line
        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
