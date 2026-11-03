package io.biotrace.app.ui.cardiac3d

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.biotrace.app.ui.theme.BioRed
import io.biotrace.app.ui.theme.DarkBackground
import io.biotrace.app.ui.theme.DarkSurface
import io.biotrace.app.ui.theme.DarkSurfaceBorder
import io.biotrace.app.ui.theme.DarkSurfaceElevated
import io.biotrace.app.ui.theme.StatusGreen
import io.biotrace.app.ui.theme.StatusOptimalBadge
import io.biotrace.app.ui.theme.StatusOptimalText
import io.biotrace.app.ui.theme.TextGray
import io.biotrace.app.ui.theme.TextWhite
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Cardiac3DScreen(
    modifier: Modifier = Modifier
) {
    var selectedView by remember { mutableStateOf("Anterior") }
    val views = listOf("Anterior", "Posterior", "Cross-Section", "Chambers")

    val infiniteTransition = rememberInfiniteTransition(label = "heartbeat")
    val beatScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 857, easing = FastOutSlowInEasing), // ~70 BPM
            repeatMode = RepeatMode.Reverse
        ),
        label = "beatScale"
    )

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurfaceElevated)
                                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = null,
                                tint = BioRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Cardiac 3D Diagnostics",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = "BIOMECHANICAL TELEMETRY",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp,
                                color = TextGray
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // View Angle Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                views.forEach { viewName ->
                    val isSelected = selectedView == viewName
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedView = viewName },
                        label = { Text(viewName, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BioRed,
                            selectedLabelColor = TextWhite,
                            containerColor = DarkSurface,
                            labelColor = TextGray
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) BioRed else DarkSurfaceBorder
                        )
                    )
                }
            }

            // 3D Visualizer Canvas Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // 3D Cardiac Mesh Vector Canvas
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f + 10.dp.toPx()
                        val scale = beatScale

                        // Draw holographic grid circles
                        val rings = 4
                        for (r in 1..rings) {
                            drawCircle(
                                color = Color(0x15FF5252),
                                radius = r * 30.dp.toPx(),
                                center = Offset(cx, cy),
                                style = Stroke(width = 1f)
                            )
                        }

                        // Draw heart anatomy vector lines
                        val heartPath = Path().apply {
                            val w = 90.dp.toPx() * scale
                            val h = 105.dp.toPx() * scale

                            moveTo(cx, cy - h * 0.3f)
                            // Left atrium & ventricle curve
                            cubicTo(
                                cx - w * 0.9f, cy - h * 0.9f,
                                cx - w * 1.1f, cy + h * 0.1f,
                                cx, cy + h * 0.8f
                            )
                            // Right ventricle & atrium curve
                            cubicTo(
                                cx + w * 1.1f, cy + h * 0.1f,
                                cx + w * 0.9f, cy - h * 0.9f,
                                cx, cy - h * 0.3f
                            )
                        }

                        // Gradient fill
                        val gradient = Brush.radialGradient(
                            colors = listOf(
                                Color(0x66FF5252),
                                Color(0x22FF1744),
                                Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = 110.dp.toPx() * scale
                        )
                        drawPath(path = heartPath, brush = gradient)

                        // Outline
                        drawPath(
                            path = heartPath,
                            color = BioRed,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Internal Chamber Dividers (Septum)
                        drawLine(
                            color = Color(0x99FF8A80),
                            start = Offset(cx, cy - 25.dp.toPx() * scale),
                            end = Offset(cx - 8.dp.toPx(), cy + 70.dp.toPx() * scale),
                            strokeWidth = 2.dp.toPx()
                        )

                        // Aorta arch
                        val aortaPath = Path().apply {
                            moveTo(cx - 15.dp.toPx(), cy - 30.dp.toPx() * scale)
                            cubicTo(
                                cx - 25.dp.toPx(), cy - 75.dp.toPx() * scale,
                                cx + 30.dp.toPx(), cy - 75.dp.toPx() * scale,
                                cx + 25.dp.toPx(), cy - 20.dp.toPx() * scale
                            )
                        }
                        drawPath(
                            path = aortaPath,
                            color = Color(0xFFFF8A80),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Watermarks & Live status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = StatusGreen,
                                shape = CircleShape,
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "70 BPM • SYNCHRONIZED",
                                color = TextWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Text(
                            text = "ORIENTATION: $selectedView",
                            color = TextGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Chamber Diagnostics Section
            Text(
                text = "CHAMBER VOLUMETRIC ANALYSIS",
                color = TextGray,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Ventricle Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "LEFT VENTRICLE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "64%",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Ejection Fraction",
                            fontSize = 11.sp,
                            color = TextGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(StatusOptimalBadge)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Normal (>55%)",
                                fontSize = 10.5.sp,
                                color = StatusOptimalText
                            )
                        }
                    }
                }

                // Stroke Volume Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "STROKE VOLUME",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "72 mL",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Per Cardiac Beat",
                            fontSize = 11.sp,
                            color = TextGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(StatusOptimalBadge)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Optimal Flow",
                                fontSize = 10.5.sp,
                                color = StatusOptimalText
                            )
                        }
                    }
                }
            }

            // Hemodynamic Telemetry Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cardiac Output
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "CARDIAC OUTPUT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "5.0 L/min",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Systemic Perfusion",
                            fontSize = 11.sp,
                            color = TextGray
                        )
                    }
                }

                // Arterial Pulse Wave Velocity
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "PULSE VELOCITY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "6.2 m/s",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Aortic Elasticity",
                            fontSize = 11.sp,
                            color = TextGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
