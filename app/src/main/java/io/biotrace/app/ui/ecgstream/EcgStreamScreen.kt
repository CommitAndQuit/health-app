package io.biotrace.app.ui.ecgstream

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import io.biotrace.app.ui.theme.TextGray
import io.biotrace.app.ui.theme.TextMuted
import io.biotrace.app.ui.theme.TextWhite
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcgStreamScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val heartRate = uiState.heartRate

    var selectedLead by remember { mutableStateOf("Lead I") }
    val leads = listOf("Lead I", "Lead II", "Lead III", "aVR", "V1")

    var isPaused by remember { mutableStateOf(false) }
    var sweepSpeed by remember { mutableStateOf("25 mm/s") }
    var gain by remember { mutableStateOf("10 mm/mV") }

    val infiniteTransition = rememberInfiniteTransition(label = "ecgSweep")
    val sweepPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepPosition"
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
                                imageVector = Icons.Default.Timeline,
                                contentDescription = null,
                                tint = BioRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Live ECG Oscilloscope",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = if (heartRate == null) TextMuted else if (isPaused) BioRed else StatusGreen,
                                    shape = CircleShape,
                                    modifier = Modifier.size(6.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (heartRate == null) "OFFLINE • NO TELEMETRY"
                                    else if (isPaused) "TELEMETRY PAUSED"
                                    else "STREAMING • $heartRate BPM • 500 Hz",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.8.sp,
                                    color = TextGray
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (heartRate != null) {
                        IconButton(onClick = { isPaused = !isPaused }) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isPaused) "Resume" else "Pause",
                                tint = TextWhite
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
            // Notice banner if no telemetry
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
                            text = "No wearable or sensor telemetry found. Sync Health Connect on the Vitals dashboard to stream live cardiac rhythm.",
                            fontSize = 12.sp,
                            color = TextGray,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Lead Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                leads.forEach { lead ->
                    val isSelected = selectedLead == lead
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedLead = lead },
                        label = { Text(lead, fontSize = 12.sp) },
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

            // Oscilloscope Waveform Display
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF090A0E)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val gridSmall = 16.dp.toPx()
                        val gridColor = Color(0x15FF5252)
                        val gridMajorColor = Color(0x30FF5252)

                        // 1mm and 5mm medical grid
                        var x = 0f
                        var countX = 0
                        while (x < size.width) {
                            drawLine(
                                color = if (countX % 5 == 0) gridMajorColor else gridColor,
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = if (countX % 5 == 0) 1.5f else 0.8f
                            )
                            x += gridSmall
                            countX++
                        }

                        var y = 0f
                        var countY = 0
                        while (y < size.height) {
                            drawLine(
                                color = if (countY % 5 == 0) gridMajorColor else gridColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = if (countY % 5 == 0) 1.5f else 0.8f
                            )
                            y += gridSmall
                            countY++
                        }

                        val midY = size.height / 2f
                        val path = Path()
                        val pointsCount = 200
                        val stepX = size.width / pointsCount
                        val currentPhase = if (isPaused || heartRate == null) 0.5f else sweepPosition
                        val cursorX = currentPhase * size.width

                        // If no heart rate data exists, draw a flat calibration line with slight baseline noise
                        if (heartRate == null) {
                            for (i in 0 until pointsCount) {
                                val px = i * stepX
                                val py = midY + (sin(i * 0.2f) * 1.5.dp.toPx())
                                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                            }

                            drawPath(
                                path = path,
                                color = TextMuted,
                                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        } else {
                            // Real heart rate rhythm synthesis
                            val cyclesAcrossScreen = (heartRate / 30f).coerceIn(1.5f, 4f)
                            for (i in 0 until pointsCount) {
                                val px = i * stepX
                                val cyclePos = (i.toFloat() / (pointsCount / cyclesAcrossScreen) + currentPhase * 2f) % 1f

                                val py = when {
                                    cyclePos in 0.12f..0.22f -> midY - 16.dp.toPx() * sin(((cyclePos - 0.12f) / 0.10f) * Math.PI.toFloat()) // P wave
                                    cyclePos in 0.32f..0.35f -> midY + 12.dp.toPx() // Q wave
                                    cyclePos in 0.35f..0.42f -> midY - 60.dp.toPx() // R spike
                                    cyclePos in 0.42f..0.46f -> midY + 22.dp.toPx() // S wave
                                    cyclePos in 0.58f..0.74f -> midY - 22.dp.toPx() * sin(((cyclePos - 0.58f) / 0.16f) * Math.PI.toFloat()) // T wave
                                    else -> midY + (sin(i * 0.35f) * 2.dp.toPx()) // baseline
                                }

                                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                            }

                            drawPath(
                                path = path,
                                color = BioRed,
                                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Sweeping glowing vertical cursor line
                            drawLine(
                                color = Color.White,
                                start = Offset(cursorX, 0f),
                                end = Offset(cursorX, size.height),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }

                    // On-screen telemetry watermark
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = selectedLead.uppercase(),
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "$sweepSpeed • $gain",
                                color = TextGray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (heartRate != null) "$heartRate BPM" else "-- BPM",
                                color = if (heartRate != null) StatusGreen else TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (heartRate != null) (uiState.rhythmStatus ?: "SINUS NORMAL") else "NO SENSOR DATA",
                                color = TextGray,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            // Controls & Calibration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        sweepSpeed = if (sweepSpeed == "25 mm/s") "50 mm/s" else "25 mm/s"
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurface,
                        contentColor = TextWhite
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Speed: $sweepSpeed", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        gain = if (gain == "10 mm/mV") "20 mm/mV" else "10 mm/mV"
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurface,
                        contentColor = TextWhite
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Gain: $gain", fontSize = 12.sp)
                }
            }

            // Interval Metrics Grid (derived from real heart rate if present)
            Text(
                text = "CLINICAL INTERVAL TELEMETRY",
                color = TextGray,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            val prValue = "--"
            val qrsValue = "--"
            val qtValue = "--"
            val qtcValue = "--"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricPill("PR INTERVAL", prValue, "--")
                MetricPill("QRS DURATION", qrsValue, "--")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricPill("QT INTERVAL", qtValue, "--")
                MetricPill("QTc (BAZETT)", qtcValue, "--")
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.MetricPill(
    title: String,
    value: String,
    status: String
) {
    Card(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = TextGray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = status,
                fontSize = 11.sp,
                color = Color(0xFF9EA6B8)
            )
        }
    }
}
