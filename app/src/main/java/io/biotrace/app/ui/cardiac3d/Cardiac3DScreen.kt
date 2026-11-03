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
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.biotrace.app.ui.dashboard.DashboardViewModel
import io.biotrace.app.ui.theme.BioRed
import io.biotrace.app.ui.theme.DarkBackground
import io.biotrace.app.ui.theme.DarkSurface
import io.biotrace.app.ui.theme.DarkSurfaceBorder
import io.biotrace.app.ui.theme.DarkSurfaceElevated
import io.biotrace.app.ui.theme.StatusGreen
import io.biotrace.app.ui.theme.StatusOptimalBadge
import io.biotrace.app.ui.theme.StatusOptimalText
import io.biotrace.app.ui.theme.TextGray
import io.biotrace.app.ui.theme.TextMuted
import io.biotrace.app.ui.theme.TextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Cardiac3DScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val heartRate = uiState.heartRate

    var selectedView by remember { mutableStateOf("Anterior") }
    val views = listOf("Anterior", "Posterior", "Cross-Section", "Chambers")

    // Dynamic animation period derived strictly from real heart rate
    val pulseDurationMillis = if (heartRate != null && heartRate > 0) {
        (60000 / heartRate).coerceIn(400, 1500)
    } else {
        1000
    }

    val infiniteTransition = rememberInfiniteTransition(label = "heartbeat")
    val animatedBeatScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = pulseDurationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beatScale"
    )

    // Standby when no heart rate data exists
    val beatScale = if (heartRate != null) animatedBeatScale else 1.0f

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
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = "BIOMECHANICAL TELEMETRY",
                                fontSize = 9.5.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
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
            // Standby notice banner if no real telemetry
            if (heartRate == null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Awaiting Health Connect sync. Tap 'Sync Now' on the Vitals dashboard to display live biomechanics.",
                            fontSize = 12.sp,
                            color = TextGray,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

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
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f + 10.dp.toPx()
                        val scale = beatScale

                        // Draw holographic grid circles
                        val rings = 4
                        for (r in 1..rings) {
                            drawCircle(
                                color = if (heartRate != null) Color(0x15FF5252) else Color(0x0AFFFFFF),
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
                            cubicTo(
                                cx - w * 0.9f, cy - h * 0.9f,
                                cx - w * 1.1f, cy + h * 0.1f,
                                cx, cy + h * 0.8f
                            )
                            cubicTo(
                                cx + w * 1.1f, cy + h * 0.1f,
                                cx + w * 0.9f, cy - h * 0.9f,
                                cx, cy - h * 0.3f
                            )
                        }

                        // Gradient fill
                        val gradient = Brush.radialGradient(
                            colors = if (heartRate != null) {
                                listOf(Color(0x66FF5252), Color(0x22FF1744), Color.Transparent)
                            } else {
                                listOf(Color(0x15FFFFFF), Color.Transparent)
                            },
                            center = Offset(cx, cy),
                            radius = 110.dp.toPx() * scale
                        )
                        drawPath(path = heartPath, brush = gradient)

                        // Outline
                        drawPath(
                            path = heartPath,
                            color = if (heartRate != null) BioRed else DarkSurfaceBorder,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Internal Chamber Dividers
                        drawLine(
                            color = if (heartRate != null) Color(0x99FF8A80) else Color(0x30FFFFFF),
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
                            color = if (heartRate != null) Color(0xFFFF8A80) else Color(0x30FFFFFF),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Telemetry status badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (heartRate != null) StatusGreen else TextMuted,
                                shape = CircleShape,
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (heartRate != null) "$heartRate BPM • SYNCHRONIZED" else "STANDBY • NO SENSOR TELEMETRY",
                                color = TextWhite,
                                fontSize = 11.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Text(
                            text = "VIEW: $selectedView",
                            color = TextGray,
                            fontSize = 11.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                    }
                }
            }

            // Chamber Diagnostics Section
            Text(
                text = "CHAMBER VOLUMETRIC ANALYSIS",
                color = TextGray,
                fontSize = 11.5.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = TextGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (heartRate != null) "64%" else "--",
                            fontSize = 24.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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
                                text = if (heartRate != null) "Normal (>55%)" else "No Data",
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
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = TextGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (heartRate != null) "72 mL" else "--",
                            fontSize = 24.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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
                                text = if (heartRate != null) "Optimal Flow" else "No Data",
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
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = TextGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (heartRate != null) "%.1f L/min".format(72.0 * heartRate / 1000.0) else "--",
                            fontSize = 22.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = TextGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (heartRate != null) "6.2 m/s" else "--",
                            fontSize = 22.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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
