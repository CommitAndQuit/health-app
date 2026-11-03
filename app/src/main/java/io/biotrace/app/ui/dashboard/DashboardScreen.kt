package io.biotrace.app.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.biotrace.app.R
import io.biotrace.app.domain.model.MetricOverview
import io.biotrace.app.domain.model.TimeRangeSelection
import io.biotrace.app.ui.theme.BioRed
import io.biotrace.app.ui.theme.BioRedBright
import io.biotrace.app.ui.theme.DarkBackground
import io.biotrace.app.ui.theme.DarkInputBg
import io.biotrace.app.ui.theme.DarkSurface
import io.biotrace.app.ui.theme.DarkSurfaceBorder
import io.biotrace.app.ui.theme.DarkSurfaceElevated
import io.biotrace.app.ui.theme.HeroCardBg
import io.biotrace.app.ui.theme.HeroCardButtonSecondary
import io.biotrace.app.ui.theme.HeroCardTextPrimary
import io.biotrace.app.ui.theme.HeroCardTextSecondary
import io.biotrace.app.ui.theme.MetricCalories
import io.biotrace.app.ui.theme.MetricHeartRate
import io.biotrace.app.ui.theme.MetricHrv
import io.biotrace.app.ui.theme.MetricOxygen
import io.biotrace.app.ui.theme.MetricSleep
import io.biotrace.app.ui.theme.MetricSteps
import io.biotrace.app.ui.theme.StatusGreen
import io.biotrace.app.ui.theme.StatusOptimalBadge
import io.biotrace.app.ui.theme.StatusOptimalText
import io.biotrace.app.ui.theme.TextGray
import io.biotrace.app.ui.theme.TextMuted
import io.biotrace.app.ui.theme.TextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToPermissions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCopilot: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showOtherVitals by remember { mutableStateOf(false) }

    // Dialogs
    if (uiState.isRecordingEkg) {
        EkgRecordDialog(
            progress = uiState.recordingProgress,
            bpm = uiState.recordingBpm,
            onCancel = { viewModel.cancelEkgRecording() }
        )
    }

    if (uiState.isHistoryOpen) {
        HistoryBottomSheet(
            records = uiState.ekgHistory,
            onDismiss = { viewModel.openHistory(false) }
        )
    }

    if (uiState.isAddNotesOpen) {
        ClinicalNotesDialog(
            onDismiss = { viewModel.openAddNotesDialog(false) },
            onSave = { note -> viewModel.addClinicalNote(note) }
        )
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Shield icon squircle container
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurfaceElevated)
                                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = TextWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "BioTrace",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = StatusGreen,
                                    shape = CircleShape,
                                    modifier = Modifier.size(6.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "ON-DEVICE • AES-256",
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
                    // Sync icon / spinner
                    if (uiState.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = BioRed
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        IconButton(onClick = { viewModel.syncNow() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync",
                                tint = TextGray,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Notification bell
                    IconButton(onClick = {}) {
                        Box {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = TextGray,
                                modifier = Modifier.size(22.dp)
                            )
                            // Red unread badge dot
                            Surface(
                                color = BioRed,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(6.dp)
                                    .align(Alignment.TopEnd)
                            ) {}
                        }
                    }

                    // Profile Avatar button
                    IconButton(onClick = onNavigateToSettings) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFB4A2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = Color(0xFF3E2723),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Greeting row with AI Copilot avatar thumbnail
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hello, ${uiState.userName}!",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextGray
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "How do you\nfeel today?",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 32.sp,
                            color = TextWhite
                        )
                    }

                    // Copilot circular thumbnail with active live dot badge
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .clickable { onNavigateToCopilot() }
                    ) {
                        // Background gradient simulating professional clinician/copilot photo
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF3949AB),
                                            Color(0xFF1E2333)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Copilot",
                                tint = Color(0xFFFFD180),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Live red pulsing indicator badge on bottom right
                        Surface(
                            color = BioRed,
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(2.dp, DarkBackground),
                            modifier = Modifier
                                .size(14.dp)
                                .align(Alignment.BottomEnd)
                        ) {}
                    }
                }
            }

            // 2. Mood Selector horizontal row
            item {
                MoodSelectorRow(
                    selectedMood = uiState.selectedMood,
                    onMoodSelected = { viewModel.selectMood(it) }
                )
            }

            // 3. Clinical notes / symptoms input bar
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkInputBg),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openAddNotesDialog(true) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditCalendar,
                            contentDescription = null,
                            tint = BioRed,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = if (uiState.clinicalNotes.isNotEmpty()) {
                                uiState.clinicalNotes.first()
                            } else {
                                "Add clinical notes or symptoms..."
                            },
                            color = if (uiState.clinicalNotes.isNotEmpty()) TextWhite else TextGray,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF232733)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = TextWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Permission banner (if permissions not granted)
            if (!uiState.hasPermissions) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2C1618)
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF5A2024))
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = BioRed,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Health Connect sync is not active",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextWhite,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = onNavigateToPermissions,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BioRed,
                                    contentColor = TextWhite
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Connect", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 4. Hero Card: Electrocardiogram (White contrast card)
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = HeroCardBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Header row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MonitorHeart,
                                    contentDescription = null,
                                    tint = BioRed,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Electrocardiogram",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HeroCardTextPrimary
                                )
                            }

                            // Timestamp
                            Text(
                                text = uiState.ekgTimestamp.replace(" ", "\n"),
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                color = HeroCardTextSecondary,
                                lineHeight = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Semi-circular Arc Gauge
                        EkgArcGauge(
                            currentBpm = uiState.heartRate,
                            rhythmStatus = uiState.rhythmStatus
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Bottom Actions: Record EKG & History
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Primary Record EKG button
                            Button(
                                onClick = { viewModel.startEkgRecording() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BioRed,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp)
                                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp), ambientColor = BioRed, spotColor = BioRed)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FiberManualRecord,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Record EKG",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            // Secondary History button
                            Button(
                                onClick = { viewModel.openHistory(true) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HeroCardButtonSecondary,
                                    contentColor = HeroCardTextPrimary
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = HeroCardTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "History",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = HeroCardTextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // 5. Bio Impedance Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        // Header row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x22FF5252)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessibilityNew,
                                        contentDescription = null,
                                        tint = BioRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Bio Impedance",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            }

                            Text(
                                text = uiState.bioImpedanceTimestamp,
                                fontSize = 12.sp,
                                color = TextGray
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Metrics row: BMI & Wave
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${uiState.bmi.toInt()}",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite,
                                    lineHeight = 34.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "bmi",
                                    fontSize = 13.sp,
                                    color = TextGray,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .padding(bottom = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(StatusOptimalBadge)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = uiState.bmiStatus,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = StatusOptimalText
                                    )
                                }
                            }

                            // Sine Waveform
                            Box(modifier = Modifier.weight(1f)) {
                                BioImpedanceWave()
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Bottom row: Encrypted Telemetry & Body Fat
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ENCRYPTED TELEMETRY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.8.sp,
                                    color = TextMuted
                                )
                            }

                            Text(
                                text = "Body Fat: ${uiState.bodyFatPercentage}%",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFC0C7D5)
                            )
                        }
                    }
                }
            }

            // 6. Twin Metric Cards (R-R Interval & Signal Quality)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left: R-R INTERVAL
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "R-R INTERVAL",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.8.sp,
                                    color = TextGray
                                )
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = BioRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${uiState.rrIntervalMs}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite,
                                    lineHeight = 30.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ms",
                                    fontSize = 13.sp,
                                    color = TextGray,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }
                    }

                    // Right: SIGNAL QUALITY
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SIGNAL QUALITY",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.8.sp,
                                    color = TextGray
                                )
                                Icon(
                                    imageVector = Icons.Default.Sensors,
                                    contentDescription = null,
                                    tint = BioRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${uiState.signalQuality}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite,
                                    lineHeight = 30.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "/100",
                                    fontSize = 13.sp,
                                    color = TextGray,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 7. Toggle to view additional continuous synced vitals (Sleep, HRV, Steps, SpO2)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showOtherVitals = !showOtherVitals }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Connected Health Telemetry",
                            color = TextWhite,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = if (showOtherVitals) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = TextGray
                        )
                    }
                }
            }

            // Additional vitals stream content
            if (showOtherVitals) {
                // Time range chips
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimeRangeSelection.entries.forEach { range ->
                            val isSelected = uiState.selectedRange == range
                            val label = when (range) {
                                TimeRangeSelection.DAY -> "Day"
                                TimeRangeSelection.WEEK -> "Week"
                                TimeRangeSelection.MONTH -> "Month"
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectRange(range) },
                                label = { Text(label, fontSize = 12.sp) },
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
                }

                items(uiState.metrics, key = { it.metricType }) { metric ->
                    MetricCardDark(metric = metric)
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun MoodSelectorRow(
    selectedMood: MoodType,
    onMoodSelected: (MoodType) -> Unit,
    modifier: Modifier = Modifier
) {
    val moods = listOf(
        Pair(MoodType.NORMAL, Icons.Default.SentimentNeutral),
        Pair(MoodType.CALM, Icons.Default.SentimentSatisfied),
        Pair(MoodType.SAD, Icons.Default.SentimentDissatisfied),
        Pair(MoodType.TIRED, Icons.Default.Nightlight),
        Pair(MoodType.HAPPY, Icons.Default.SentimentVerySatisfied),
        Pair(MoodType.STRESSED, Icons.Default.PowerSettingsNew)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        moods.forEach { (mood, icon) ->
            val isSelected = selectedMood == mood
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onMoodSelected(mood) }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else DarkSurface)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color.White else DarkSurfaceBorder,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = mood.label,
                        tint = if (isSelected) Color(0xFF161820) else TextGray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = mood.label,
                    fontSize = 11.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else TextGray
                )
            }
        }
    }
}

@Composable
fun MetricCardDark(
    metric: MetricOverview,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val accentColor = when (metric.metricType) {
        "HEART_RATE" -> MetricHeartRate
        "HRV" -> MetricHrv
        "STEPS" -> MetricSteps
        "SLEEP" -> MetricSleep
        "CALORIES" -> MetricCalories
        "OXYGEN_SATURATION" -> MetricOxygen
        else -> BioRed
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(18.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = metric.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite
                )
                Text(
                    text = metric.unit,
                    fontSize = 12.sp,
                    color = TextGray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = if (metric.latestValue != null) {
                            if (metric.metricType == "STEPS") "%,d".format(metric.latestValue.toLong())
                            else "%.1f".format(metric.latestValue)
                        } else "--",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    if (metric.averageValue != null) {
                        Text(
                            text = "Avg: %.1f %s".format(metric.averageValue, metric.unit),
                            fontSize = 11.5.sp,
                            color = TextGray
                        )
                    }
                }

                if (metric.history.isEmpty()) {
                    Text(
                        text = "No sync data",
                        fontSize = 11.5.sp,
                        color = TextMuted
                    )
                }
            }

            if (metric.history.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                SimpleLineChart(
                    points = metric.history,
                    lineColor = accentColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (expanded) 140.dp else 60.dp)
                )
            }
        }
    }
}
