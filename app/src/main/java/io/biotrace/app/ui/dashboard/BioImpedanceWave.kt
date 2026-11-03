package io.biotrace.app.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.biotrace.app.ui.theme.BioRed
import io.biotrace.app.ui.theme.DarkSurfaceBorder

@Composable
fun BioImpedanceWave(
    hasData: Boolean,
    modifier: Modifier = Modifier,
    color: Color = BioRed
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
    ) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        if (!hasData) {
            // Flat baseline indicating no telemetry received
            drawLine(
                color = DarkSurfaceBorder,
                start = Offset(0f, midY),
                end = Offset(w, midY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = DarkSurfaceBorder,
                radius = 3.dp.toPx(),
                center = Offset(w, midY)
            )
            return@Canvas
        }

        val path = Path().apply {
            moveTo(0f, midY)
            cubicTo(
                w * 0.25f, midY + 12.dp.toPx(),
                w * 0.45f, midY - 14.dp.toPx(),
                w * 0.70f, midY - 6.dp.toPx()
            )
            cubicTo(
                w * 0.85f, midY - 1.dp.toPx(),
                w * 0.95f, midY + 6.dp.toPx(),
                w, midY + 4.dp.toPx()
            )
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Ending point indicator
        val endOffset = Offset(w, midY + 4.dp.toPx())
        drawCircle(
            color = color,
            radius = 3.5.dp.toPx(),
            center = endOffset
        )
    }
}
