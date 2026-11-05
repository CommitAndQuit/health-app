package io.biotrace.app.ui.vitals

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.biotrace.app.data.healthconnect.HealthConnectAvailability
import io.biotrace.app.data.healthconnect.HealthConnectManager
import io.biotrace.app.domain.model.StressLevel
import io.biotrace.app.domain.model.VitalMetricType
import io.biotrace.app.domain.model.VitalSummaryCardModel
import io.biotrace.app.ui.theme.BioOrange
import io.biotrace.app.ui.theme.CardBorder
import io.biotrace.app.ui.theme.ClinicalEmerald
import io.biotrace.app.ui.theme.LightBackground
import io.biotrace.app.ui.theme.PureWhite
import io.biotrace.app.ui.theme.TextDark
import io.biotrace.app.ui.theme.TextMuted
import io.biotrace.app.ui.theme.TextSecondary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalsScreen(
    onNavigateToDetail: (VitalMetricType) -> Unit,
    onNavigateToPermissions: () -> Unit = {},
    viewModel: VitalsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Observe lifecycle for automatic data refresh on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkStatusAndRefresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Health Connect Permission Launcher
    val healthConnectPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) {
        viewModel.checkStatusAndRefresh()
    }

    // Gadgetbridge SQLite file picker launcher (SAF)
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importGadgetbridgeDatabase(uri)
        }
    }

    Scaffold(
        containerColor = LightBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Vitals",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        val lastSyncStr = uiState.lastSyncTime?.let {
                            val time = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
                                .format(it.atZone(ZoneId.systemDefault()))
                            "Synced today at $time"
                        } ?: "Honor MagicWatch 2 • Active"
                        Text(
                            text = lastSyncStr,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    // Import Gadgetbridge DB Action Button
                    IconButton(
                        onClick = {
                            openDocumentLauncher.launch(
                                arrayOf(
                                    "application/octet-stream",
                                    "application/x-sqlite3",
                                    "application/vnd.sqlite3",
                                    "*/*"
                                )
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Import Gadgetbridge DB",
                            tint = if (uiState.hasSavedGadgetbridgeDb) ClinicalEmerald else BioOrange
                        )
                    }

                    // Refresh Button
                    IconButton(onClick = { viewModel.refreshDashboard() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = TextDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBackground)
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refreshDashboard() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Notice banner if permissions missing or Health Connect not available
                if (uiState.availability != HealthConnectAvailability.AVAILABLE || !uiState.hasHealthConnectPermissions) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                        HealthConnectBanner(
                            availability = uiState.availability,
                            hasPermissions = uiState.hasHealthConnectPermissions,
                            onRequestPermissions = {
                                val c = HealthConnectManager(context)
                                healthConnectPermissionLauncher.launch(c.requiredPermissions)
                            },
                            onInstall = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("market://details?id=com.google.android.apps.healthdata")
                                )
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                // Import feedback snackbar / banner
                if (uiState.importMessage != null) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                        ImportNoticeBanner(
                            message = uiState.importMessage!!,
                            onDismiss = { viewModel.clearImportMessage() }
                        )
                    }
                }

                // Render 9 Vital Cards in 2-per-row grid
                items(uiState.cards, key = { it.metricType.name }) { card ->
                    VitalSummaryCard(
                        card = card,
                        onClick = { onNavigateToDetail(card.metricType) },
                        onImportGadgetbridge = {
                            openDocumentLauncher.launch(
                                arrayOf(
                                    "application/octet-stream",
                                    "application/x-sqlite3",
                                    "application/vnd.sqlite3",
                                    "*/*"
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun VitalSummaryCard(
    card: VitalSummaryCardModel,
    onClick: () -> Unit,
    onImportGadgetbridge: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = PureWhite,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(168.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Icon + Title + Chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(card.metricType.accentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = card.metricType.icon,
                            contentDescription = card.metricType.title,
                            tint = card.metricType.accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = card.metricType.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Center: Primary Value + Unit
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = card.primaryValue,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDark,
                        lineHeight = 28.sp
                    )
                    if (card.primaryUnit.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = card.primaryUnit,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }

                // If badge is present, show badge row
                if (card.badgeText != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        color = card.badgeBgColor ?: Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = card.badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = card.badgeColor ?: TextDark,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Bottom: Contextual secondary indicator
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                when {
                    // Empty Stress state with Import Chip
                    card.metricType == VitalMetricType.STRESS && card.actionText != null -> {
                        Surface(
                            onClick = onImportGadgetbridge,
                            color = BioOrange.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = BioOrange,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Link Gadgetbridge",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BioOrange,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Stress Score: Mini 4-Zone Color Bar + range
                    card.metricType == VitalMetricType.STRESS && card.stressLevel != null -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            MiniStressZoneBar(currentLevel = card.stressLevel)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = card.secondaryText,
                                fontSize = 10.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    // Sleep: Mini 4-color horizontal stacked bar + % goal
                    card.metricType == VitalMetricType.SLEEP && card.sleepStageProportions != null -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            MiniSleepStageBar(proportions = card.sleepStageProportions)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = card.secondaryText,
                                fontSize = 10.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    // Steps: Linear progress bar toward 8,000 steps
                    card.metricType == VitalMetricType.STEPS && card.progress != null -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            LinearProgressIndicator(
                                progress = { card.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = ClinicalEmerald,
                                trackColor = Color(0xFFE2E8F0)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = card.secondaryText,
                                fontSize = 10.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    // Heart Rate: Mini Sparkline
                    card.metricType == VitalMetricType.HEART_RATE && card.sparklinePoints != null -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = card.secondaryText,
                                fontSize = 10.sp,
                                color = TextSecondary,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            MiniSparkline(
                                points = card.sparklinePoints,
                                color = BioOrange,
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(18.dp)
                            )
                        }
                    }

                    // Default text
                    else -> {
                        Text(
                            text = card.secondaryText,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MiniStressZoneBar(currentLevel: StressLevel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp)),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val zones = listOf(StressLevel.RELAXED, StressLevel.NORMAL, StressLevel.MEDIUM, StressLevel.HIGH)
        zones.forEach { zone ->
            val isSelected = zone == currentLevel
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(if (isSelected) zone.color else zone.color.copy(alpha = 0.25f))
            )
        }
    }
}

@Composable
fun MiniSleepStageBar(proportions: List<Float>) {
    // [deep, light, rem, awake]
    val colors = listOf(
        Color(0xFF3B82F6), // Deep
        Color(0xFF60A5FA), // Light
        Color(0xFF818CF8), // REM
        Color(0xFFF59E0B)  // Awake
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp)),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        proportions.forEachIndexed { index, prop ->
            val weight = prop.coerceAtLeast(0.05f)
            Box(
                modifier = Modifier
                    .weight(weight)
                    .fillMaxSize()
                    .background(colors.getOrElse(index) { Color.LightGray })
            )
        }
    }
}

@Composable
fun MiniSparkline(
    points: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val min = points.minOrNull() ?: 0f
        val max = points.maxOrNull() ?: 1f
        val range = if (max - min > 0) max - min else 1f

        val stepX = size.width / (points.size - 1)
        val path = Path()

        points.forEachIndexed { i, p ->
            val x = i * stepX
            val y = size.height - ((p - min) / range) * (size.height - 4f) - 2f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun HealthConnectBanner(
    availability: HealthConnectAvailability,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    onInstall: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = Color(0xFFD97706),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                val title = if (availability != HealthConnectAvailability.AVAILABLE) {
                    "Health Connect Required"
                } else {
                    "Permissions Needed"
                }
                val desc = if (availability != HealthConnectAvailability.AVAILABLE) {
                    "Install or update Health Connect to read wearable telemetry."
                } else {
                    "Allow BioTrace to access wearable heart rate, sleep & steps."
                }
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text(text = desc, fontSize = 11.sp, color = TextSecondary)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = if (availability != HealthConnectAvailability.AVAILABLE) onInstall else onRequestPermissions,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (availability != HealthConnectAvailability.AVAILABLE) "Install" else "Grant",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )
            }
        }
    }
}

@Composable
fun ImportNoticeBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        color = Color(0xFFECFDF5),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message,
                fontSize = 11.sp,
                color = Color(0xFF065F46),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color(0xFF065F46),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VitalsScreenGridPreview() {
    val sampleCards = listOf(
        VitalSummaryCardModel(
            metricType = VitalMetricType.HEART_RATE,
            primaryValue = "72",
            primaryUnit = "bpm",
            secondaryText = "Range: 46–127 bpm",
            badgeText = "Avg 68",
            sparklinePoints = listOf(65f, 72f, 85f, 68f, 74f, 72f)
        ),
        VitalSummaryCardModel(
            metricType = VitalMetricType.RESTING_HEART_RATE,
            primaryValue = "58",
            primaryUnit = "bpm",
            secondaryText = "Overnight sleep baseline",
            badgeText = "Sleep 5th-pct",
            badgeColor = Color(0xFF6366F1),
            badgeBgColor = Color(0xFFEEF2FF),
            isFallbackComputed = true
        ),
        VitalSummaryCardModel(
            metricType = VitalMetricType.STRESS,
            primaryValue = "54",
            primaryUnit = "/ 99",
            secondaryText = "Day range: 15–73",
            badgeText = "Normal",
            badgeColor = Color(0xFF2563EB),
            badgeBgColor = Color(0xFFDBEAFE),
            stressLevel = StressLevel.NORMAL
        ),
        VitalSummaryCardModel(
            metricType = VitalMetricType.SPO2,
            primaryValue = "98",
            primaryUnit = "%",
            secondaryText = "Latest • Oct 28",
            badgeText = "Normal",
            badgeColor = Color(0xFF059669),
            badgeBgColor = Color(0xFFE6F9F0)
        ),
        VitalSummaryCardModel(
            metricType = VitalMetricType.SLEEP,
            primaryValue = "6h 30m",
            primaryUnit = "",
            secondaryText = "93% of 7h 00m goal",
            progress = 0.93f,
            sleepStageProportions = listOf(0.25f, 0.45f, 0.20f, 0.10f)
        ),
        VitalSummaryCardModel(
            metricType = VitalMetricType.STEPS,
            primaryValue = "5,420",
            primaryUnit = "steps",
            secondaryText = "68% of 8,000 goal",
            progress = 0.68f
        ),
        VitalSummaryCardModel(
            metricType = VitalMetricType.CALORIES_DISTANCE,
            primaryValue = "420 kcal",
            primaryUnit = "",
            secondaryText = "Distance: 3.12 km",
            badgeText = "3.12 km"
        ),
        VitalSummaryCardModel(
            metricType = VitalMetricType.VO2_MAX_WORKOUTS,
            primaryValue = "40",
            primaryUnit = "mL/kg/min",
            secondaryText = "Last workout: 37m • 3.01 km",
            badgeText = "Good"
        ),
        VitalSummaryCardModel(
            metricType = VitalMetricType.BODY_METRICS,
            primaryValue = "59.0",
            primaryUnit = "kg",
            secondaryText = "BMI 23.0 (Normal) • 160cm",
            badgeText = "Normal"
        )
    )

    MaterialTheme {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sampleCards) { card ->
                VitalSummaryCard(
                    card = card,
                    onClick = {},
                    onImportGadgetbridge = {}
                )
            }
        }
    }
}
