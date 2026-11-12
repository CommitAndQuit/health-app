package io.biotrace.app.ui.vitals

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import io.biotrace.app.domain.model.VitalMetricType
import io.biotrace.app.ui.theme.AppCanvasBackground
import io.biotrace.app.ui.theme.BioOrangePrimary
import io.biotrace.app.ui.theme.BioOrangeSoftTint
import io.biotrace.app.ui.theme.BioTraceTokens
import io.biotrace.app.ui.theme.CardSurface
import io.biotrace.app.ui.theme.CobaltBluePrimary
import io.biotrace.app.ui.theme.CobaltBlueSoftTint
import io.biotrace.app.ui.theme.CopilotBadgeBg
import io.biotrace.app.ui.theme.CopilotBadgeText
import io.biotrace.app.ui.theme.CopilotGlowEnd
import io.biotrace.app.ui.theme.CopilotGlowStart
import io.biotrace.app.ui.theme.CoralAlertPrimary
import io.biotrace.app.ui.theme.CoralAlertSoftTint
import io.biotrace.app.ui.theme.CorrelationPeachBg
import io.biotrace.app.ui.theme.CorrelationPeachBorder
import io.biotrace.app.ui.theme.DividerSubtle
import io.biotrace.app.ui.theme.EmeraldPrimary
import io.biotrace.app.ui.theme.EmeraldSoftTint
import io.biotrace.app.ui.theme.IndigoSleepPrimary
import io.biotrace.app.ui.theme.IndigoSleepSoftTint
import io.biotrace.app.ui.theme.InnerWellSurface
import io.biotrace.app.ui.theme.MacroCarbsColor
import io.biotrace.app.ui.theme.MacroFatsColor
import io.biotrace.app.ui.theme.MacroProteinColor
import io.biotrace.app.ui.theme.ProfileAvatarBg
import io.biotrace.app.ui.theme.ProfileAvatarIcon
import io.biotrace.app.ui.theme.SleepDeepColor
import io.biotrace.app.ui.theme.SleepLightColor
import io.biotrace.app.ui.theme.SleepRemColor
import io.biotrace.app.ui.theme.SolarAmberPrimary
import io.biotrace.app.ui.theme.SolarAmberSoftTint
import io.biotrace.app.ui.theme.StrainAlertBg
import io.biotrace.app.ui.theme.StrainAlertBorder
import io.biotrace.app.ui.theme.TextMuted
import io.biotrace.app.ui.theme.TextPrimary
import io.biotrace.app.ui.theme.TextSecondary
import io.biotrace.app.ui.theme.TextWhite
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalsScreen(
    onNavigateToDetail: (VitalMetricType) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {},
    onNavigateToCopilot: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNavigateToLabs: () -> Unit = {},
    onNavigateToNutrition: () -> Unit = {},
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

    Scaffold(
        containerColor = AppCanvasBackground
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refreshDashboard() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Synced Vitals Title & Live Status Row with Profile Avatar
                item {
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

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Status Pill Badge (with emerald dot)
                            Surface(
                                color = EmeraldSoftTint,
                                shape = BioTraceTokens.PillShape,
                                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.2f))
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

                            // Profile Avatar
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ProfileAvatarBg)
                                    .clickable { onNavigateToSettings() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile and settings",
                                    tint = ProfileAvatarIcon,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Health Connect Notice Banner (if permissions missing or not installed)
                if (uiState.availability != HealthConnectAvailability.AVAILABLE || !uiState.hasHealthConnectPermissions) {
                    item {
                        HealthConnectNoticeBanner(
                            availability = uiState.availability,
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

                // 3. Copilot Summary Card (Cyan-to-Purple gradient border, brain icon, TODAY badge, bullets, Ask Copilot)
                item {
                    CopilotSummaryCard(
                        copilotSummary = uiState.copilotSummary,
                        onNavigateToCopilot = onNavigateToCopilot
                    )
                }

                // 4. Autonomic Strain Alert Banner (Coral warning icon, dynamic strain metrics)
                if (uiState.autonomicStrain.isStrainDetected) {
                    item {
                        AutonomicStrainAlertBanner(
                            strain = uiState.autonomicStrain,
                            onClick = onNavigateToInsights
                        )
                    }
                }

                // 5. Telemetry Section: HRV RMSSD Card (large value, delta %, Bezier sparkline canvas)
                item {
                    HrvRmssdCard(
                        hrvData = uiState.hrvSparkline,
                        onClick = { onNavigateToDetail(VitalMetricType.HEART_RATE) }
                    )
                }

                // 6. Split 2-Column Row (RHR Card + Sleep Card)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        RhrSplitCard(
                            rhrData = uiState.rhrSplit,
                            onClick = { onNavigateToDetail(VitalMetricType.RESTING_HEART_RATE) },
                            modifier = Modifier.weight(1f)
                        )

                        SleepSplitCard(
                            sleepData = uiState.sleepSplit,
                            onClick = { onNavigateToDetail(VitalMetricType.SLEEP) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 7. Labs Section (Biomarker telemetry, normal/low/high badges, normalized range bars)
                item {
                    LabsTelemetryCard(
                        labItems = uiState.labBiomarkers,
                        onNavigateToLabs = onNavigateToLabs
                    )
                }

                // 8. Nutrition Section (latest meal, calories, macros well, postprandial HR impact)
                item {
                    NutritionSectionCard(
                        nutrition = uiState.latestNutrition,
                        onNavigateToNutrition = onNavigateToNutrition
                    )
                }
            }
        }
    }
}

// =============================================================================
// Copilot Summary Card
// =============================================================================

@Composable
fun CopilotSummaryCard(
    copilotSummary: DashboardCopilotSummary,
    onNavigateToCopilot: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = CardSurface,
        shape = BioTraceTokens.HeroCardShape,
        border = BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(listOf(CopilotGlowStart, CopilotGlowEnd))
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onNavigateToCopilot() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row: Brain Icon + Title + TODAY badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(CopilotGlowStart.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "BioTrace Copilot",
                            tint = CopilotGlowStart,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "BioTrace Copilot",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Surface(
                    color = CopilotBadgeBg,
                    shape = BioTraceTokens.PillShape
                ) {
                    Text(
                        text = "TODAY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CopilotBadgeText,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dynamic Summary Narrative
            val narrative = copilotSummary.summaryText.ifBlank {
                "Awaiting health vitals sync from Health Connect. Sync records to see personalized biometric insights."
            }
            Text(
                text = narrative,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Normal,
                color = TextPrimary,
                lineHeight = 20.sp
            )

            // Dynamic Bullet Items (HRV/RHR, Sleep, Labs, Action suggestion)
            val hasBullets = copilotSummary.hrvDeltaText != null ||
                    copilotSummary.rhrDeltaText != null ||
                    copilotSummary.sleepTargetText != null ||
                    copilotSummary.labBiomarkerStatus != null ||
                    copilotSummary.actionSuggestion != null

            if (hasBullets) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = InnerWellSurface,
                    shape = BioTraceTokens.InnerWellShape,
                    border = BioTraceTokens.WellHairlineBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // HRV / RHR bullet
                        if (copilotSummary.hrvDeltaText != null || copilotSummary.rhrDeltaText != null) {
                            val text = buildString {
                                copilotSummary.hrvDeltaText?.let { append("HRV $it") }
                                if (copilotSummary.hrvDeltaText != null && copilotSummary.rhrDeltaText != null) {
                                    append(" • ")
                                }
                                copilotSummary.rhrDeltaText?.let { append("RHR $it") }
                            }
                            CopilotBulletRow(
                                bulletColor = CopilotGlowStart,
                                label = "Cardiovascular Tone",
                                value = text
                            )
                        }

                        // Sleep stage target bullet
                        if (copilotSummary.sleepTargetText != null) {
                            CopilotBulletRow(
                                bulletColor = SleepDeepColor,
                                label = "Sleep Cadence",
                                value = copilotSummary.sleepTargetText
                            )
                        }

                        // Lab biomarker status bullet
                        if (copilotSummary.labBiomarkerStatus != null) {
                            CopilotBulletRow(
                                bulletColor = EmeraldPrimary,
                                label = "Biomarker Labs",
                                value = copilotSummary.labBiomarkerStatus
                            )
                        }

                        // Action recommendation bullet
                        if (copilotSummary.actionSuggestion != null) {
                            CopilotBulletRow(
                                bulletColor = BioOrangePrimary,
                                label = "Directive",
                                value = copilotSummary.actionSuggestion
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ask Copilot Button
            Button(
                onClick = onNavigateToCopilot,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(),
                shape = BioTraceTokens.PillShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(CopilotGlowStart, CopilotGlowEnd)
                        ),
                        shape = BioTraceTokens.PillShape
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = TextWhite,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ask Copilot",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }
            }
        }
    }
}

@Composable
private fun CopilotBulletRow(
    bulletColor: Color,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(bulletColor)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = buildString {
                append("$label: ")
                append(value)
            },
            fontSize = 12.sp,
            color = TextPrimary,
            lineHeight = 17.sp
        )
    }
}

// =============================================================================
// Autonomic Strain Alert Banner
// =============================================================================

@Composable
fun AutonomicStrainAlertBanner(
    strain: io.biotrace.app.ui.vitals.DashboardAutonomicStrain,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = StrainAlertBg,
        shape = BioTraceTokens.RecordCardShape,
        border = BorderStroke(1.dp, StrainAlertBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
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
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CoralAlertSoftTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Autonomic Strain Warning",
                            tint = CoralAlertPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = strain.headline.ifBlank { "Compound Autonomic Strain" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoralAlertPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = CoralAlertSoftTint,
                    shape = BioTraceTokens.PillShape,
                    border = BorderStroke(1.dp, CoralAlertPrimary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = strain.severity.ifBlank { "ALERT" },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoralAlertPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (strain.narrative.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = strain.narrative,
                    fontSize = 12.sp,
                    color = TextPrimary,
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic Strain Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StrainMetricPill(
                    label = "RHR",
                    value = "${if (strain.rhrDeviation >= 0) "+" else ""}${strain.rhrDeviation}σ",
                    isWarning = strain.rhrDeviation > 1.0,
                    modifier = Modifier.weight(1f)
                )
                StrainMetricPill(
                    label = "HRV",
                    value = "${if (strain.hrvDeviation >= 0) "+" else ""}${strain.hrvDeviation}σ",
                    isWarning = strain.hrvDeviation > 1.0,
                    modifier = Modifier.weight(1f)
                )
                StrainMetricPill(
                    label = "Sleep",
                    value = "${strain.sleepHours}h",
                    isWarning = strain.sleepHours in 0.1..6.0,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StrainMetricPill(
    label: String,
    value: String,
    isWarning: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = InnerWellSurface,
        shape = BioTraceTokens.InnerWellSmallShape,
        border = BorderStroke(1.dp, if (isWarning) CoralAlertPrimary.copy(alpha = 0.3f) else DividerSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isWarning) CoralAlertPrimary else TextPrimary
            )
        }
    }
}

// =============================================================================
// Telemetry Section: HRV RMSSD Card (Spline Canvas + Dash Baseline)
// =============================================================================

@Composable
fun HrvRmssdCard(
    hrvData: io.biotrace.app.ui.vitals.DashboardHrvSparkline,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = CardSurface,
        shape = BioTraceTokens.HeroCardShape,
        border = BioTraceTokens.CardHairlineBorder,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row: Circular Icon + Title + Delta % Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(IndigoSleepSoftTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = hrvData.label,
                            tint = IndigoSleepPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = hrvData.label,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                // Delta % pill badge
                if (hrvData.deltaPercent != null) {
                    val isPositive = hrvData.deltaPercent >= 0.0
                    val pillBg = if (isPositive) EmeraldSoftTint else CoralAlertSoftTint
                    val pillColor = if (isPositive) EmeraldPrimary else CoralAlertPrimary
                    val sign = if (isPositive) "+" else ""
                    Surface(
                        color = pillBg,
                        shape = BioTraceTokens.PillShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = pillColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "$sign${hrvData.deltaPercent}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = pillColor
                            )
                        }
                    }
                } else {
                    Surface(
                        color = InnerWellSurface,
                        shape = BioTraceTokens.PillShape
                    ) {
                        Text(
                            text = "7D Baseline",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stat Row: Large RMSSD Value + Trend Subtitle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    val displayVal = hrvData.latestHrvMs?.let {
                        if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
                    } ?: (hrvData.points.lastOrNull()?.let {
                        if (it % 1f == 0f) it.toInt().toString() else "%.1f".format(Locale.US, it)
                    } ?: "--")

                    Text(
                        text = displayVal,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        lineHeight = 40.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ms",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }

                Text(
                    text = if (hrvData.isUpTrend) "Rising resilience" else if (hrvData.isDownTrend) "Parasympathetic drop" else "Rolling 7-day trend",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cubic Bezier Sparkline Canvas
            HrvSparklineCanvas(
                points = hrvData.points,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            )
        }
    }
}

@Composable
fun HrvSparklineCanvas(
    points: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Safe math guard: check points.size >= 2; if < 2, render clean dashed baseline
        if (points.size < 2) {
            val midY = height / 2f
            drawLine(
                color = DividerSubtle,
                start = Offset(0f, midY),
                end = Offset(width, midY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
            return@Canvas
        }

        val minVal = (points.minOrNull() ?: 20f) - 4f
        val maxVal = (points.maxOrNull() ?: 100f) + 4f
        val range = (maxVal - minVal).coerceAtLeast(10f)

        fun getY(v: Float): Float {
            val normalized = (v - minVal) / range
            return height - (normalized * (height - 24f)) - 12f
        }

        val stepX = width / (points.size - 1)

        // 1. Draw dashed baseline at average
        val avgVal = points.average().toFloat()
        val avgY = getY(avgVal)
        drawLine(
            color = DividerSubtle.copy(alpha = 0.6f),
            start = Offset(0f, avgY),
            end = Offset(width, avgY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )

        // 2. Build Cubic Bezier Path and Fill Path
        val splinePath = Path()
        val fillPath = Path()

        val startY = getY(points.first())
        splinePath.moveTo(0f, startY)
        fillPath.moveTo(0f, height)
        fillPath.lineTo(0f, startY)

        for (i in 0 until points.size - 1) {
            val p0X = i * stepX
            val p0Y = getY(points[i])
            val p1X = (i + 1) * stepX
            val p1Y = getY(points[i + 1])

            val cp1X = p0X + (p1X - p0X) / 2f
            val cp1Y = p0Y
            val cp2X = p0X + (p1X - p0X) / 2f
            val cp2Y = p1Y

            splinePath.cubicTo(cp1X, cp1Y, cp2X, cp2Y, p1X, p1Y)
            fillPath.cubicTo(cp1X, cp1Y, cp2X, cp2Y, p1X, p1Y)
        }

        val lastX = (points.size - 1) * stepX
        val lastY = getY(points.last())

        fillPath.lineTo(lastX, height)
        fillPath.close()

        // Draw area gradient under curve
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    IndigoSleepPrimary.copy(alpha = 0.35f),
                    IndigoSleepPrimary.copy(alpha = 0.0f)
                ),
                startY = 0f,
                endY = height
            )
        )

        // Draw spline stroke
        drawPath(
            path = splinePath,
            color = IndigoSleepPrimary,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // 3. Draw glowing marker dot at last point
        drawCircle(
            color = IndigoSleepPrimary.copy(alpha = 0.25f),
            radius = 8.dp.toPx(),
            center = Offset(lastX, lastY)
        )
        drawCircle(
            color = IndigoSleepPrimary,
            radius = 4.5.dp.toPx(),
            center = Offset(lastX, lastY)
        )
        drawCircle(
            color = TextWhite,
            radius = 2.dp.toPx(),
            center = Offset(lastX, lastY)
        )
    }
}

// =============================================================================
// Split 2-Column Row: RHR Card (Z-Score & Dual Track)
// =============================================================================

@Composable
fun RhrSplitCard(
    rhrData: io.biotrace.app.ui.vitals.DashboardRhrSplit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = CardSurface,
        shape = BioTraceTokens.BentoCardShape,
        border = BioTraceTokens.CardHairlineBorder,
        modifier = modifier.height(188.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Icon + Title + Z-score pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(BioOrangeSoftTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "RHR",
                            tint = BioOrangePrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Resting HR",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                // Guarded z-score display: zScoreFormatted ?: "Baseline"
                val zScoreDisplay = rhrData.zScoreFormatted ?: "Baseline"
                val (badgeBg, badgeColor) = when (rhrData.status) {
                    "Elevated" -> Pair(CoralAlertSoftTint, CoralAlertPrimary)
                    "Optimal" -> Pair(EmeraldSoftTint, EmeraldPrimary)
                    else -> Pair(InnerWellSurface, TextSecondary)
                }
                Surface(
                    color = badgeBg,
                    shape = BioTraceTokens.PillShape
                ) {
                    Text(
                        text = zScoreDisplay,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Center: Current BPM Value
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = rhrData.currentBpm?.toString() ?: "--",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    lineHeight = 30.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "bpm",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }

            // Bottom: Dual Track Well (Current vs 30D baseline)
            Surface(
                color = InnerWellSurface,
                shape = BioTraceTokens.InnerWellSmallShape,
                border = BioTraceTokens.WellHairlineBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "30D Avg",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Text(
                            text = rhrData.baselineMeanBpm?.let { "${it.roundToInt()} bpm" } ?: "--",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(BioTraceTokens.PillShape)
                            .background(DividerSubtle)
                    ) {
                        val current = rhrData.currentBpm?.toFloat() ?: 60f
                        val progress = ((current - 40f) / 60f).coerceIn(0.05f, 0.95f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .background(BioOrangePrimary)
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// Split 2-Column Row: Sleep Card (Segmented 3-Color Bar)
// =============================================================================

@Composable
fun SleepSplitCard(
    sleepData: io.biotrace.app.ui.vitals.DashboardSleepSplit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = CardSurface,
        shape = BioTraceTokens.BentoCardShape,
        border = BioTraceTokens.CardHairlineBorder,
        modifier = modifier.height(188.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Icon + Title + Target Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(IndigoSleepSoftTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "Sleep",
                            tint = IndigoSleepPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sleep",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                Surface(
                    color = InnerWellSurface,
                    shape = BioTraceTokens.PillShape
                ) {
                    Text(
                        text = sleepData.targetLabel,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = IndigoSleepPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Center: Duration Value
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = sleepData.durationFormatted,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    lineHeight = 28.sp
                )
            }

            // Bottom: 3-Color Segmented Sleep Stage Bar (Deep, REM, Light)
            Column(modifier = Modifier.fillMaxWidth()) {
                // Safe math guard: check totalMinutes > 0; apply Modifier.weight(maxOf(0.01f, pct.toFloat())); if 0, show neutral track
                if (sleepData.totalMinutes > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(BioTraceTokens.PillShape)
                            .background(InnerWellSurface),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val deepWeight = maxOf(0.01f, sleepData.deepPercentage.toFloat())
                        val remWeight = maxOf(0.01f, sleepData.remPercentage.toFloat())
                        val lightWeight = maxOf(0.01f, sleepData.lightPercentage.toFloat())

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(deepWeight)
                                .background(SleepDeepColor)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(remWeight)
                                .background(SleepRemColor)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(lightWeight)
                                .background(SleepLightColor)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SleepStageLegendItem(color = SleepDeepColor, label = "D ${sleepData.deepPercentage}%")
                        SleepStageLegendItem(color = SleepRemColor, label = "R ${sleepData.remPercentage}%")
                        SleepStageLegendItem(color = SleepLightColor, label = "L ${sleepData.lightPercentage}%")
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(BioTraceTokens.PillShape)
                            .background(DividerSubtle)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "No sleep stages recorded",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun SleepStageLegendItem(
    color: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
    }
}

// =============================================================================
// Labs Section: Biomarker Telemetry & Normalized Range Bars
// =============================================================================

@Composable
fun LabsTelemetryCard(
    labItems: List<io.biotrace.app.ui.vitals.DashboardLabItem>,
    onNavigateToLabs: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onNavigateToLabs,
        color = CardSurface,
        shape = BioTraceTokens.BentoCardShape,
        border = BioTraceTokens.CardHairlineBorder,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Icon + Title + Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(CobaltBlueSoftTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = "Labs",
                            tint = CobaltBluePrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Biomarker Telemetry",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                Surface(
                    onClick = onNavigateToLabs,
                    color = InnerWellSurface,
                    shape = BioTraceTokens.PillShape,
                    border = BorderStroke(1.dp, DividerSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (labItems.isNotEmpty()) "Scan Report" else "Add Labs",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CobaltBluePrimary
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = CobaltBluePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (labItems.isEmpty()) {
                Surface(
                    color = InnerWellSurface,
                    shape = BioTraceTokens.InnerWellShape,
                    border = BioTraceTokens.WellHairlineBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Lab Panels Synced",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Upload bloodwork or scan lab PDF to track validated biomarker ranges.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    labItems.forEach { item ->
                        BiomarkerRangeItem(item = item)
                    }
                }
            }
        }
    }
}

@Composable
fun BiomarkerRangeItem(
    item: io.biotrace.app.ui.vitals.DashboardLabItem,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                val (badgeBg, badgeColor, badgeLabel) = when (item.flag) {
                    "HIGH" -> Triple(CoralAlertSoftTint, CoralAlertPrimary, "HIGH")
                    "LOW" -> Triple(SolarAmberSoftTint, SolarAmberPrimary, "LOW")
                    "NORMAL" -> Triple(EmeraldSoftTint, EmeraldPrimary, "NORMAL")
                    else -> Triple(InnerWellSurface, TextSecondary, item.flag ?: "SYNCED")
                }
                Surface(
                    color = badgeBg,
                    shape = BioTraceTokens.PillShape
                ) {
                    Text(
                        text = badgeLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "${item.value} ${item.unit}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Normalized horizontal range bar: use item.rangePosition clamped between 0.05f and 0.95f
        val clampedPos = item.rangePosition.coerceIn(0.05f, 0.95f)
        val markerColor = when (item.flag) {
            "HIGH" -> CoralAlertPrimary
            "LOW" -> SolarAmberPrimary
            else -> EmeraldPrimary
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
        ) {
            val width = size.width
            val height = size.height
            val barHeight = 5.dp.toPx()
            val midY = height / 2f

            // Full track background
            drawRoundRect(
                color = InnerWellSurface,
                size = androidx.compose.ui.geometry.Size(width, barHeight),
                topLeft = Offset(0f, midY - barHeight / 2f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barHeight / 2f)
            )

            // Optimal normal target zone (25% to 75%)
            drawRoundRect(
                color = EmeraldPrimary.copy(alpha = 0.15f),
                size = androidx.compose.ui.geometry.Size(width * 0.5f, barHeight),
                topLeft = Offset(width * 0.25f, midY - barHeight / 2f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barHeight / 2f)
            )

            // Marker dot at clamped position
            val markerX = width * clampedPos
            drawCircle(
                color = markerColor.copy(alpha = 0.25f),
                radius = 6.dp.toPx(),
                center = Offset(markerX, midY)
            )
            drawCircle(
                color = markerColor,
                radius = 3.5.dp.toPx(),
                center = Offset(markerX, midY)
            )
        }

        // Reference range captions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val minText = item.referenceMin?.let { "$it ${item.unit}" } ?: "Ref Min"
            val maxText = item.referenceMax?.let { "$it ${item.unit}" } ?: "Ref Max"
            Text(
                text = minText,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted
            )
            Text(
                text = maxText,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted
            )
        }
    }
}

// =============================================================================
// Nutrition Section: Meal Nutrition & Postprandial Impact
// =============================================================================

@Composable
fun NutritionSectionCard(
    nutrition: io.biotrace.app.ui.vitals.DashboardMealNutrition?,
    onNavigateToNutrition: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onNavigateToNutrition,
        color = CardSurface,
        shape = BioTraceTokens.BentoCardShape,
        border = BioTraceTokens.CardHairlineBorder,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Icon + Title + Action Pill Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(SolarAmberSoftTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = "Nutrition",
                            tint = SolarAmberPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Metabolic Nutrition",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                Surface(
                    onClick = onNavigateToNutrition,
                    color = InnerWellSurface,
                    shape = BioTraceTokens.PillShape,
                    border = BorderStroke(1.dp, DividerSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (nutrition != null) "Log Meal" else "Snap Meal",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SolarAmberPrimary
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = SolarAmberPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Safe guard: if latestNutrition == null, render graceful empty card with camera button
            if (nutrition == null) {
                Surface(
                    color = InnerWellSurface,
                    shape = BioTraceTokens.InnerWellShape,
                    border = BioTraceTokens.WellHairlineBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SolarAmberSoftTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = SolarAmberPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Meals Logged Today",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Photograph a meal to compute macros and track postprandial cardiac tone.",
                            fontSize = 11.5.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onNavigateToNutrition,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SolarAmberPrimary,
                                contentColor = TextWhite
                            ),
                            shape = BioTraceTokens.PillShape,
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = TextWhite,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Camera Meal Scan",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Top Meal Row: Thumbnail + Meal Name + Time + Calories
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SolarAmberSoftTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = nutrition.mealName,
                                tint = SolarAmberPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = nutrition.mealName.ifBlank { "Logged Meal" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = nutrition.consumedAtFormatted.ifBlank { "Recent" },
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${nutrition.calories}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(
                            text = "kcal",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Macros Well: Protein, Carbs, Fats
                Surface(
                    color = InnerWellSurface,
                    shape = BioTraceTokens.InnerWellSmallShape,
                    border = BioTraceTokens.WellHairlineBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MacroColumn(
                            label = "Protein",
                            value = "${nutrition.proteinGrams.roundToInt()}g",
                            color = MacroProteinColor
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(22.dp)
                                .background(DividerSubtle)
                        )
                        MacroColumn(
                            label = "Carbs",
                            value = "${nutrition.carbsGrams.roundToInt()}g",
                            color = MacroCarbsColor
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(22.dp)
                                .background(DividerSubtle)
                        )
                        MacroColumn(
                            label = "Fats",
                            value = "${nutrition.fatsGrams.roundToInt()}g",
                            color = MacroFatsColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Postprandial HR Impact Pill
                if (nutrition.postprandialHrImpactBpm != null) {
                    val impact = nutrition.postprandialHrImpactBpm
                    val isHigh = impact > 3.0
                    val (pillBg, pillBorder, pillColor) = if (isHigh) {
                        Triple(StrainAlertBg, StrainAlertBorder, CoralAlertPrimary)
                    } else {
                        Triple(EmeraldSoftTint, EmeraldPrimary.copy(alpha = 0.2f), EmeraldPrimary)
                    }
                    val sign = if (impact >= 0) "+" else ""

                    Surface(
                        color = pillBg,
                        shape = BioTraceTokens.PillShape,
                        border = BorderStroke(1.dp, pillBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = pillColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Postprandial HR: ${sign}${impact} bpm vs baseline",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = pillColor
                            )
                        }
                    }
                } else if (nutrition.isLateMeal) {
                    Surface(
                        color = CorrelationPeachBg,
                        shape = BioTraceTokens.PillShape,
                        border = BorderStroke(1.dp, CorrelationPeachBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                tint = SolarAmberPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Late meal • Monitoring nocturnal recovery",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SolarAmberPrimary
                            )
                        }
                    }
                }

                if (nutrition.narrative.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = nutrition.narrative,
                        fontSize = 11.5.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroColumn(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}


// =============================================================================
// Health Connect Availability & Permission Banner
// =============================================================================

@Composable
private fun HealthConnectNoticeBanner(
    availability: HealthConnectAvailability,
    onRequestPermissions: () -> Unit,
    onInstall: () -> Unit
) {
    Surface(
        color = Color(0xFFFEF3C7),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
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
