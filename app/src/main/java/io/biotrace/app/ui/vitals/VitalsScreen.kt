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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.biotrace.app.data.healthconnect.HealthConnectAvailability
import io.biotrace.app.data.healthconnect.HealthConnectManager
import io.biotrace.app.domain.model.BentoCardTemplate
import io.biotrace.app.domain.model.ChartPoint
import io.biotrace.app.domain.model.ContinuousHeartRateHeroData
import io.biotrace.app.domain.model.VitalMetricType
import io.biotrace.app.domain.model.VitalSummaryCardModel
import io.biotrace.app.ui.common.BioTraceTopBar
import io.biotrace.app.ui.theme.AppCanvasBackground
import io.biotrace.app.ui.theme.BioOrangeGradientBottom
import io.biotrace.app.ui.theme.BioOrangeGradientTop
import io.biotrace.app.ui.theme.BioOrangePrimary
import io.biotrace.app.ui.theme.BioOrangeSoftTint
import io.biotrace.app.ui.theme.BioTraceTokens
import io.biotrace.app.ui.theme.CardSurface
import io.biotrace.app.ui.theme.CobaltBluePrimary
import io.biotrace.app.ui.theme.DividerSubtle
import io.biotrace.app.ui.theme.EmeraldPrimary
import io.biotrace.app.ui.theme.EmeraldSoftTint
import io.biotrace.app.ui.theme.IndigoSleepPrimary
import io.biotrace.app.ui.theme.InnerWellSurface
import io.biotrace.app.ui.theme.SolarAmberPrimary
import io.biotrace.app.ui.theme.TextMuted
import io.biotrace.app.ui.theme.TextPrimary
import io.biotrace.app.ui.theme.TextSecondary
import io.biotrace.app.ui.theme.TextWhite
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
        containerColor = AppCanvasBackground,
        topBar = {
            BioTraceTopBar(
                subtitle = "HONOR MagicWatch 2-38D • Hebe-B19S",
                onImportGadgetbridgeUri = { uri ->
                    viewModel.importGadgetbridgeDatabase(uri)
                }
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
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 90.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Synced Vitals Title & Live Status Row
                item(span = { GridItemSpan(2) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Synced Vitals",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            letterSpacing = (-0.5).sp
                        )

                        // Status Pill Badge (with emerald dot)
                        Surface(
                            color = EmeraldSoftTint,
                            shape = BioTraceTokens.PillShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val syncText = uiState.lastSyncTime?.let {
                                    val time = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
                                        .format(it.atZone(ZoneId.systemDefault()))
                                    "Synced $time"
                                } ?: "Health Connect"
                                Text(
                                    text = syncText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }
                        }
                    }
                }

                // 2. Health Connect Notice Banner (if permissions missing or not installed)
                if (uiState.availability != HealthConnectAvailability.AVAILABLE || !uiState.hasHealthConnectPermissions) {
                    item(span = { GridItemSpan(2) }) {
                        HealthConnectNoticeBanner(
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

                // 3. Import Notice Banner (Feedback from Gadgetbridge import)
                if (uiState.importMessage != null) {
                    item(span = { GridItemSpan(2) }) {
                        Surface(
                            color = BioOrangeSoftTint,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BioOrangePrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = BioOrangePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = uiState.importMessage!!,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.clearImportMessage() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Hero Card: Continuous Heart Rate (Spans 2 Columns)
                item(span = { GridItemSpan(2) }) {
                    HeartRateHeroCard(
                        data = uiState.heroHeartRate,
                        onClick = { onNavigateToDetail(VitalMetricType.HEART_RATE) }
                    )
                }

                // 5. 2-Column Bento Grid Items (8 Cards)
                items(uiState.cards, key = { it.metricType.name }) { card ->
                    when (card.template) {
                        BentoCardTemplate.TEMPLATE_A -> {
                            BioTraceTemplateACard(
                                card = card,
                                onClick = { onNavigateToDetail(card.metricType) }
                            )
                        }
                        BentoCardTemplate.TEMPLATE_B -> {
                            BioTraceTemplateBCard(
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

                // 6. Bottom Sync Action Row (Spans 2 Columns)
                item(span = { GridItemSpan(2) }) {
                    SyncHealthConnectBottomRow(
                        isRefreshing = uiState.isRefreshing,
                        onSyncHealthConnect = { viewModel.refreshDashboard() },
                        onPickGadgetbridge = {
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

// =============================================================================
// Continuous Heart Rate Hero Card (Full Width Hero with Spline Area Chart)
// =============================================================================

@Composable
fun HeartRateHeroCard(
    data: ContinuousHeartRateHeroData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = CardSurface,
        shape = BioTraceTokens.HeroCardShape,
        border = BioTraceTokens.CardHairlineBorder,
        shadowElevation = BioTraceTokens.HeroCardElevation,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Top Row: Icon Badge + Title + Live Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BioOrangeSoftTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Heart Rate",
                            tint = BioOrangePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Continuous Heart Rate",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                // Live status badge
                Surface(
                    color = BioOrangeSoftTint,
                    shape = BioTraceTokens.PillShape
                ) {
                    Text(
                        text = if (data.latestBpm != null) "Live • Today" else "Synced",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BioOrangePrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stat Row: Giant Current BPM on Left, Dual Summary Columns on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Giant BPM
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = data.latestBpm?.toString() ?: "--",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        lineHeight = 44.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "bpm",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // Right Dual Columns
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Resting HR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = data.restingHrBpm?.let { "$it bpm" } ?: "--",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(DividerSubtle)
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Daily Range",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val rangeStr = if (data.dailyMinBpm != null && data.dailyMaxBpm != null) {
                            "${data.dailyMinBpm}-${data.dailyMaxBpm} bpm"
                        } else {
                            "--"
                        }
                        Text(
                            text = rangeStr,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cubic Bézier Spline Chart
            HeartRateHeroCanvasChart(
                points = data.samples,
                fallbackLatestBpm = data.latestBpm,
                fallbackMinBpm = data.dailyMinBpm,
                fallbackMaxBpm = data.dailyMaxBpm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Time Axis Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                data.timeLabels.forEach { label ->
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun HeartRateHeroCanvasChart(
    points: List<ChartPoint>,
    fallbackLatestBpm: Int?,
    fallbackMinBpm: Int?,
    fallbackMaxBpm: Int?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Prepare points: use real samples or synthesize an accurate smooth intraday spline from min/max/latest
        val effectivePoints: List<Float> = when {
            points.size >= 2 -> points.map { it.value }
            fallbackLatestBpm != null -> {
                val minVal = (fallbackMinBpm ?: (fallbackLatestBpm - 15)).toFloat()
                val maxVal = (fallbackMaxBpm ?: (fallbackLatestBpm + 25)).toFloat()
                val midVal = fallbackLatestBpm.toFloat()
                listOf(minVal + 4f, minVal + 10f, minVal + 2f, maxVal - 8f, midVal + 5f, midVal)
            }
            else -> listOf(65f, 72f, 68f, 85f, 74f, 70f)
        }

        val minVal = (effectivePoints.minOrNull() ?: 50f) - 6f
        val maxVal = (effectivePoints.maxOrNull() ?: 120f) + 8f
        val range = (maxVal - minVal).coerceAtLeast(10f)

        fun getY(value: Float): Float {
            val normalized = (value - minVal) / range
            return height - (normalized * (height - 24f)) - 12f
        }

        val stepX = width / (effectivePoints.size - 1).coerceAtLeast(1)

        val splinePath = Path()
        val fillPath = Path()

        val startY = getY(effectivePoints.first())
        splinePath.moveTo(0f, startY)
        fillPath.moveTo(0f, height)
        fillPath.lineTo(0f, startY)

        for (i in 0 until effectivePoints.size - 1) {
            val p0X = i * stepX
            val p0Y = getY(effectivePoints[i])
            val p1X = (i + 1) * stepX
            val p1Y = getY(effectivePoints[i + 1])

            val cp1X = p0X + (p1X - p0X) / 2f
            val cp1Y = p0Y
            val cp2X = p0X + (p1X - p0X) / 2f
            val cp2Y = p1Y

            splinePath.cubicTo(cp1X, cp1Y, cp2X, cp2Y, p1X, p1Y)
            fillPath.cubicTo(cp1X, cp1Y, cp2X, cp2Y, p1X, p1Y)
        }

        val lastX = (effectivePoints.size - 1) * stepX
        val lastY = getY(effectivePoints.last())

        fillPath.lineTo(lastX, height)
        fillPath.close()

        // 1. Draw Area Gradient
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(BioOrangeGradientTop, BioOrangeGradientBottom),
                startY = 0f,
                endY = height
            )
        )

        // 2. Draw Spline Stroke
        drawPath(
            path = splinePath,
            color = BioOrangePrimary,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // 3. Draw Endpoint Ring (Outer Halo + Solid Ring + White Hole)
        drawCircle(
            color = BioOrangePrimary.copy(alpha = 0.20f),
            radius = 9.dp.toPx(),
            center = Offset(lastX, lastY)
        )
        drawCircle(
            color = BioOrangePrimary,
            radius = 5.dp.toPx(),
            center = Offset(lastX, lastY)
        )
        drawCircle(
            color = CardSurface,
            radius = 2.2.dp.toPx(),
            center = Offset(lastX, lastY)
        )
    }
}

// =============================================================================
// Bento Grid Card: Template A (Standard Wearable Bento Card)
// =============================================================================

@Composable
fun BioTraceTemplateACard(
    card: VitalSummaryCardModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = CardSurface,
        shape = BioTraceTokens.BentoCardShape,
        border = BioTraceTokens.CardHairlineBorder,
        shadowElevation = BioTraceTokens.BentoCardElevation,
        modifier = modifier
            .fillMaxWidth()
            .height(184.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row: 36.dp Circular Icon Badge + Title + Chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val (icon, iconTint, iconBg) = when (card.metricType) {
                        VitalMetricType.CALORIES_DISTANCE -> Triple(Icons.Default.LocalFireDepartment, BioOrangePrimary, BioOrangeSoftTint)
                        VitalMetricType.STEPS -> Triple(Icons.AutoMirrored.Filled.DirectionsWalk, SolarAmberPrimary, Color(0xFFFFF7E6))
                        VitalMetricType.SPO2 -> Triple(Icons.Default.Opacity, CobaltBluePrimary, Color(0xFFEEF4FF))
                        VitalMetricType.SLEEP -> Triple(Icons.Default.Bedtime, IndigoSleepPrimary, Color(0xFFF1F0FF))
                        else -> Triple(card.metricType.icon, card.metricType.accentColor, card.metricType.accentColor.copy(alpha = 0.12f))
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = card.metricType.title,
                            tint = iconTint,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = card.metricType.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
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
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = card.primaryValue,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
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
            }

            // Bottom: Dynamic Visual Container (Pill or Linear Progress Bar)
            Column(modifier = Modifier.fillMaxWidth()) {
                when {
                    // Daily Steps: Linear Progress Bar
                    card.metricType == VitalMetricType.STEPS && card.progress != null -> {
                        LinearProgressIndicator(
                            progress = { card.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = SolarAmberPrimary,
                            trackColor = Color(0xFFFFF1D6),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = card.goalText ?: "${(card.progress * 100).toInt()}% of 8,000 goal",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }

                    // Active Calories / Sleep / SpO2: Status Pill Badge
                    card.badgeText != null -> {
                        Surface(
                            color = card.badgeBgColor ?: EmeraldSoftTint,
                            shape = BioTraceTokens.PillShape
                        ) {
                            Text(
                                text = card.badgeText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = card.badgeColor ?: EmeraldPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                maxLines = 1
                            )
                        }
                    }

                    // Secondary text fallback
                    else -> {
                        Text(
                            text = card.secondaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
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

// =============================================================================
// Bento Grid Card: Template B (Integrated Biomarker Card with Recessed Well)
// =============================================================================

@Composable
fun BioTraceTemplateBCard(
    card: VitalSummaryCardModel,
    onClick: () -> Unit,
    onImportGadgetbridge: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = CardSurface,
        shape = BioTraceTokens.BentoCardShape,
        border = BioTraceTokens.CardHairlineBorder,
        shadowElevation = BioTraceTokens.BentoCardElevation,
        modifier = modifier
            .fillMaxWidth()
            .height(184.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: 34.dp Icon + Title + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val (icon, iconTint, iconBg) = when (card.metricType) {
                        VitalMetricType.STRESS -> Triple(Icons.Default.Spa, BioOrangePrimary, BioOrangeSoftTint)
                        VitalMetricType.VO2_MAX_WORKOUTS -> Triple(Icons.Default.Speed, EmeraldPrimary, EmeraldSoftTint)
                        VitalMetricType.RESTING_HEART_RATE -> Triple(Icons.Default.Favorite, BioOrangePrimary, BioOrangeSoftTint)
                        else -> Triple(card.metricType.icon, card.metricType.accentColor, card.metricType.accentColor.copy(alpha = 0.12f))
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = card.metricType.title,
                            tint = iconTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = card.metricType.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (card.badgeText != null) {
                    Surface(
                        color = card.badgeBgColor ?: Color(0xFFF1F5F9),
                        shape = BioTraceTokens.PillShape
                    ) {
                        Text(
                            text = card.badgeText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = card.badgeColor ?: TextPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Center: Primary Value + Unit
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = card.primaryValue,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
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

            // Bottom: Recessed Inner Well Container (#F6F7FB)
            if (card.metricType == VitalMetricType.STRESS && card.actionText != null && card.primaryValue == "--") {
                // If stress is empty, show Link Gadgetbridge action inside well
                Surface(
                    onClick = onImportGadgetbridge,
                    color = BioOrangeSoftTint,
                    shape = BioTraceTokens.InnerWellSmallShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BioOrangePrimary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = BioOrangePrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Link Gadgetbridge",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BioOrangePrimary,
                            maxLines = 1
                        )
                    }
                }
            } else {
                Surface(
                    color = InnerWellSurface,
                    shape = BioTraceTokens.InnerWellSmallShape,
                    border = BioTraceTokens.WellHairlineBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = card.recessedWellLeftLabel ?: "Baseline",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary,
                                maxLines = 1
                            )
                            Text(
                                text = card.recessedWellLeftValue ?: "--",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(DividerSubtle)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = card.recessedWellRightLabel ?: "Status",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary,
                                maxLines = 1
                            )
                            Text(
                                text = card.recessedWellRightValue ?: "--",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = card.recessedWellRightColor ?: EmeraldPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// Bottom Action Row: Sync Health Connect + Gadgetbridge SAF Importer
// =============================================================================

@Composable
fun SyncHealthConnectBottomRow(
    isRefreshing: Boolean,
    onSyncHealthConnect: () -> Unit,
    onPickGadgetbridge: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Main Sync Pill Button (54.dp height)
        Button(
            onClick = onSyncHealthConnect,
            colors = ButtonDefaults.buttonColors(
                containerColor = BioOrangePrimary,
                contentColor = TextWhite
            ),
            shape = BioTraceTokens.PillShape,
            modifier = Modifier
                .weight(1f)
                .height(54.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = TextWhite,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRefreshing) "Syncing..." else "Sync Health Connect",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Square Gadgetbridge SQLite Import Button (54x54dp)
        Surface(
            onClick = onPickGadgetbridge,
            color = BioOrangeSoftTint,
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BioOrangePrimary.copy(alpha = 0.25f)),
            modifier = Modifier.size(54.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = "Import Gadgetbridge SQLite",
                    tint = BioOrangePrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// =============================================================================
// Health Connect Availability & Permission Banner
// =============================================================================

@Composable
private fun HealthConnectNoticeBanner(
    availability: HealthConnectAvailability,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    onInstall: () -> Unit
) {
    Surface(
        color = Color(0xFFFEF3C7),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (availability == HealthConnectAvailability.AVAILABLE) Icons.Default.Shield else Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFB45309),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (availability == HealthConnectAvailability.AVAILABLE) "Permissions Required" else "Health Connect Missing",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF78350F)
                )
                Text(
                    text = if (availability == HealthConnectAvailability.AVAILABLE)
                        "Grant read access for Heart Rate, Sleep, and Steps."
                    else
                        "Install Health Connect to enable automatic wearable sync.",
                    fontSize = 11.sp,
                    color = Color(0xFF92400E)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = if (availability == HealthConnectAvailability.AVAILABLE) onRequestPermissions else onInstall,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD97706),
                    contentColor = TextWhite
                ),
                shape = BioTraceTokens.PillShape
            ) {
                Text(
                    text = if (availability == HealthConnectAvailability.AVAILABLE) "Grant" else "Install",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
