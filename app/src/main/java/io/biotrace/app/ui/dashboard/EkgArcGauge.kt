package io.biotrace.app.ui.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.biotrace.app.ui.theme.BioRed
import io.biotrace.app.ui.theme.HeroCardBg
import io.biotrace.app.ui.theme.HeroCardTextPrimary
import io.biotrace.app.ui.theme.HeroCardTextSecondary
import io.biotrace.app.ui.theme.HeroCardTrack
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun EkgArcGauge(
    currentBpm: Int,
    rhythmStatus: String,
    modifier: Modifier = Modifier,
    minBpm: Int = 50,
    maxBpm: Int = 110
) {
    // Normal range fraction: 70 BPM on [50, 110] is (70 - 50)/(110 - 50) = 20/60 = 0.333
    val targetFraction = ((currentBpm - minBpm).toFloat() / (maxBpm - minBpm).toFloat())
        .coerceIn(0.05f, 0.95f)

    val animatedFraction = remember { Animatable(0f) }

    LaunchedEffect(currentBpm) {
        animatedFraction.animateTo(
            targetValue = targetFraction,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Range indicator labels above the arc
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "<60",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                color = HeroCardTextSecondary
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "100>",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                color = HeroCardTextSecondary
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(240.dp, 150.dp)
            ) {
                val strokeWidth = 12.dp.toPx()
                val padding = strokeWidth / 2f + 4.dp.toPx()
                val arcWidth = size.width - padding * 2
                val arcHeight = size.height * 1.8f - padding * 2

                val topLeft = Offset(padding, padding + 10.dp.toPx())
                val arcSize = Size(arcWidth, arcHeight)

                // Arc starts at 165° and sweeps 210°
                val startAngle = 165f
                val totalSweep = 210f

                // Background track
                drawArc(
                    color = HeroCardTrack,
                    startAngle = startAngle,
                    sweepAngle = totalSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Active progress gradient arc
                val activeSweep = totalSweep * animatedFraction.value
                val gradient = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFFF8577),
                        Color(0xFFFF5252),
                        Color(0xFFFF3B30)
                    ),
                    center = Offset(size.width / 2f, topLeft.y + arcSize.height / 2f)
                )

                if (activeSweep > 1f) {
                    drawArc(
                        brush = gradient,
                        startAngle = startAngle,
                        sweepAngle = activeSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Indicator knob at the tip of the active arc
                    val currentAngleRad = Math.toRadians((startAngle + activeSweep).toDouble())
                    val radiusX = arcSize.width / 2f
                    val radiusY = arcSize.height / 2f
                    val centerX = topLeft.x + radiusX
                    val centerY = topLeft.y + radiusY

                    val knobX = (centerX + radiusX * cos(currentAngleRad)).toFloat()
                    val knobY = (centerY + radiusY * sin(currentAngleRad)).toFloat()

                    // Outer white ring
                    drawCircle(
                        color = Color.White,
                        radius = 8.dp.toPx(),
                        center = Offset(knobX, knobY)
                    )
                    // Inner red dot
                    drawCircle(
                        color = Color(0xFFFF3B30),
                        radius = 4.5.dp.toPx(),
                        center = Offset(knobX, knobY)
                    )
                }
            }

            // BPM and Value in Center
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                Text(
                    text = "$currentBpm",
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Bold,
                    color = HeroCardTextPrimary,
                    lineHeight = 48.sp
                )
                Text(
                    text = "BPM",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = HeroCardTextSecondary
                )
            }
        }

        // Sinus rhythm status badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        ) {
            Surface(
                color = BioRed,
                shape = CircleShape,
                modifier = Modifier.size(7.dp)
            ) {}
            Text(
                text = " $rhythmStatus",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = HeroCardTextPrimary
            )
        }
    }
}
