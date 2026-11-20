package io.biotrace.app.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.biotrace.app.domain.agent.Citation
import io.biotrace.app.domain.anomaly.AnomalyAlert
import io.biotrace.app.domain.clinical.rules.ClinicalStatus
import io.biotrace.app.domain.longevity.BiologicalAgeResult
import io.biotrace.app.domain.longevity.LongevityDriver
import io.biotrace.app.domain.model.ActionableFocus
import io.biotrace.app.domain.model.DailyBiometricInsightReport
import io.biotrace.app.domain.model.InsightDomainTag
import io.biotrace.app.domain.model.ProfileTriptychSummary
import io.biotrace.app.domain.model.TrendDirection
import io.biotrace.app.domain.model.TrendHighlight
import io.biotrace.app.ui.common.BioTraceTopBar
import io.biotrace.app.ui.insights.AiReportState
import io.biotrace.app.ui.insights.components.CircadianRhythmCard
import io.biotrace.app.ui.insights.components.CorrelationScatterPlot
import io.biotrace.app.ui.insights.components.LifestyleArchetypeCard
import io.biotrace.app.ui.insights.components.RiskScoreGaugeCard
import io.biotrace.app.ui.insights.components.RiskSubMetric
import io.biotrace.app.ui.theme.AppCanvasBackground
import io.biotrace.app.ui.theme.BioOrangePrimary
import io.biotrace.app.ui.theme.BioOrangeSoftTint
import io.biotrace.app.ui.theme.BioTraceTokens
import io.biotrace.app.ui.theme.CardSurface
import io.biotrace.app.ui.theme.CobaltBluePrimary
import io.biotrace.app.ui.theme.CobaltBlueSoftTint
import io.biotrace.app.ui.theme.CoralAlertPrimary
import io.biotrace.app.ui.theme.CoralAlertSoftTint
import io.biotrace.app.ui.theme.DividerSubtle
import io.biotrace.app.ui.theme.EmeraldPrimary
import io.biotrace.app.ui.theme.EmeraldSoftTint
import io.biotrace.app.ui.theme.IndigoSleepPrimary
import io.biotrace.app.ui.theme.IndigoSleepSoftTint
import io.biotrace.app.ui.theme.InnerWellSurface
import io.biotrace.app.ui.theme.SolarAmberPrimary
import io.biotrace.app.ui.theme.SolarAmberSoftTint
import io.biotrace.app.ui.theme.TextMuted
import io.biotrace.app.ui.theme.TextPrimary
import io.biotrace.app.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun BiometricInsightsScreen(
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToLabs: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: BiometricInsightsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isAgeDialogOpen) {
        EditChronologicalAgeDialog(
            currentAge = uiState.biologicalAge?.chronologicalAge ?: 35,
            onDismiss = { viewModel.toggleAgeDialog(false) },
            onSave = { newAge -> viewModel.updateChronologicalAge(newAge) }
        )
    }

    Scaffold(
        containerColor = AppCanvasBackground,
        topBar = {
            BioTraceTopBar(
                onBackClick = onNavigateBack,
                onProfileClick = onNavigateToSettings
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading && uiState.correlations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BioOrangePrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = 8.dp,
                    bottom = 90.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Header Title & Subtitle Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Biometric Insights",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "Triangulated Wearable & Metabolic Correlates",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                        }

                        Surface(
                            color = BioOrangeSoftTint,
                            shape = BioTraceTokens.PillShape
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = BioOrangePrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "BioTrace AI",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BioOrangePrimary
                                )
                            }
                        }
                    }
                }

                // 2. Multi-Tab Navigation / Filter Bar (4 Tabs)
                item {
                    InsightsTabBar(
                        selectedTab = uiState.selectedTab,
                        onTabSelected = { viewModel.selectTab(it) }
                    )
                }

                // 3. Tab Contents
                when (uiState.selectedTab) {
                    InsightsTab.OVERVIEW -> {
                        item {
                            ProfileTriptychHeroCard(profile = uiState.profile)
                        }

                        uiState.biologicalAge?.let { bioAge ->
                            item {
                                BiologicalAgeHeroCard(
                                    result = bioAge,
                                    onEditAgeClick = { viewModel.toggleAgeDialog(true) }
                                )
                            }
                        }

                        item {
                            OverviewReadinessCard(profile = uiState.profile)
                        }
                    }

                    InsightsTab.CORRELATIONS -> {
                        uiState.scatterData?.let { scatter ->
                            item {
                                CorrelationScatterPlot(
                                    points = scatter.points,
                                    xLabel = scatter.xLabel,
                                    yLabel = scatter.yLabel,
                                    pearsonR = scatter.pearsonR,
                                    spearmanRho = scatter.spearmanRho
                                )
                            }
                        }

                        uiState.circadianDecomposition?.let { circadian ->
                            item {
                                CircadianRhythmCard(result = circadian)
                            }
                        }

                        uiState.lifestyleClusterResult?.let { lifestyle ->
                            item {
                                LifestyleArchetypeCard(result = lifestyle)
                            }
                        }

                        item {
                            Text(
                                text = "Discovered Bivariate Correlations",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        if (uiState.correlations.isEmpty()) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    color = CardSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Science,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "No Correlations Analyzed Yet",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Sync Health Connect to calculate multi-metric physiological couplings.",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = TextSecondary,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(uiState.correlations) { correlation ->
                                CorrelationCard(item = correlation)
                            }
                        }
                    }

                    InsightsTab.CLINICAL_RISKS -> {
                        uiState.atherogenicRisk?.let { athero ->
                            item {
                                val scoreFrac = when (athero.overallRiskTier) {
                                    io.biotrace.app.domain.clinical.risk.AtherogenicRiskTier.OPTIMAL -> 0.25f
                                    io.biotrace.app.domain.clinical.risk.AtherogenicRiskTier.MODERATE_RISK -> 0.65f
                                    io.biotrace.app.domain.clinical.risk.AtherogenicRiskTier.HIGH_RISK -> 1.0f
                                }
                                RiskScoreGaugeCard(
                                    title = "Atherogenic Lipid Triad",
                                    category = "Cardiovascular Lipidomics",
                                    riskTier = athero.overallRiskTier.name,
                                    primaryValue = "%.2f".format(Locale.US, athero.tgHdlRatio),
                                    primaryUnit = "TG/HDL",
                                    scoreFraction = scoreFrac,
                                    statusDescription = athero.narrative,
                                    subMetrics = listOf(
                                        RiskSubMetric("TG / HDL Ratio", "%.2f".format(Locale.US, athero.tgHdlRatio), isWarning = athero.tgHdlRatio >= 2.0, threshold = "Optimal < 2.0"),
                                        RiskSubMetric("Non-HDL Cholesterol", "%.1f mg/dL".format(Locale.US, athero.nonHdlCholesterol), isWarning = athero.nonHdlCholesterol >= 130.0, threshold = "Optimal < 130 mg/dL"),
                                        RiskSubMetric("Total Chol / HDL", "%.2f".format(Locale.US, athero.tcHdlRatio), isWarning = athero.tcHdlRatio >= 3.5, threshold = "Optimal < 3.5")
                                    ),
                                    guidelineSource = "NCEP ATP III Guidelines"
                                )
                            }
                        }

                        uiState.metabolicRisk?.let { metabolic ->
                            item {
                                RiskScoreGaugeCard(
                                    title = "Harmonized Metabolic Syndrome",
                                    category = "Metabolic Homeostasis",
                                    riskTier = metabolic.riskLevel.name,
                                    primaryValue = "${metabolic.criteriaMetCount}/4",
                                    primaryUnit = "Criteria Met",
                                    scoreFraction = (metabolic.criteriaMetCount / 4.0f).coerceIn(0.0f, 1.0f),
                                    statusDescription = metabolic.guidance,
                                    subMetrics = metabolic.criteriaDetails.map { cd ->
                                        RiskSubMetric(
                                            label = cd.criterionName,
                                            value = cd.observedValue,
                                            isWarning = cd.isMet,
                                            threshold = cd.thresholdDescription
                                        )
                                    },
                                    guidelineSource = "JIS 2009 (Zero BP Dependency)"
                                )
                            }
                        }

                        uiState.sleepDebt?.let { sleepDebt ->
                            item {
                                RiskScoreGaugeCard(
                                    title = "Sleep Deficit & Autonomic Strain",
                                    category = "Circadian Neuro-Recovery",
                                    riskTier = sleepDebt.tier.name,
                                    primaryValue = "%.1f".format(Locale.US, sleepDebt.cumulativeDebtHours),
                                    primaryUnit = "Hours Debt",
                                    scoreFraction = (sleepDebt.allostaticAutonomicStrainScore / 100.0).toFloat().coerceIn(0.0f, 1.0f),
                                    statusDescription = sleepDebt.recoveryRecommendation,
                                    subMetrics = listOf(
                                        RiskSubMetric("Allostatic Autonomic Strain", "%.1f / 100".format(Locale.US, sleepDebt.allostaticAutonomicStrainScore), isWarning = sleepDebt.allostaticAutonomicStrainScore >= 50.0),
                                        RiskSubMetric("14-Day Cumulative Deficit", "%.1f hrs".format(Locale.US, sleepDebt.cumulativeDebtHours), isWarning = sleepDebt.cumulativeDebtHours > 5.0)
                                    ),
                                    guidelineSource = "AASM Clinical Chronobiology"
                                )
                            }
                        }

                        item {
                            Text(
                                text = "In-App Anomaly Alerts",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        if (uiState.activeAnomalies.isNotEmpty()) {
                            items(uiState.activeAnomalies) { alert ->
                                InAppAnomalyAlertCard(alert = alert)
                            }
                        } else {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = BioTraceTokens.BentoCardShape,
                                    color = CardSurface,
                                    border = BioTraceTokens.CardHairlineBorder
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "No Active In-App Anomalies",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "All analyzed biometric streams and lab biomarkers are within normal reference ranges.",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            if (uiState.labBiomarkers.isNotEmpty()) {
                                LabBiomarkersSummaryCard(
                                    count = uiState.labBiomarkers.size,
                                    onNavigateToLabs = onNavigateToLabs
                                )
                            } else {
                                LabTriangulationInviteCard(onNavigateToLabs = onNavigateToLabs)
                            }
                        }
                    }

                    InsightsTab.AI_DEEP_DIVE -> {
                        if (uiState.arbitratedInsight?.hasClinicalSafetyNotice == true) {
                            item {
                                ClinicalSafetyNoticeBannerCard(
                                    notice = uiState.arbitratedInsight?.safetyNotice ?: "",
                                    highestStatus = uiState.arbitratedInsight?.highestClinicalStatus,
                                    citations = uiState.arbitratedInsight?.citations ?: emptyList()
                                )
                            }
                        }

                        item {
                            AiReportCard(
                                reportState = uiState.reportState,
                                isApiKeyConfigured = uiState.isApiKeyConfigured,
                                onGenerateClick = { viewModel.generateReport() },
                                onNavigateToSettings = onNavigateToSettings,
                                structuredReport = uiState.structuredReport,
                            )
                        }

                        if (uiState.arbitratedInsight != null && uiState.arbitratedInsight!!.citations.isNotEmpty()) {
                            item {
                                GroundedCitationsCard(citations = uiState.arbitratedInsight!!.citations)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// Profile & 3-Metric Triptych Hero Card
// =============================================================================

@Composable
fun ProfileTriptychHeroCard(
    profile: ProfileTriptychSummary,
    modifier: Modifier = Modifier
) {
    Surface(
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
            // Profile Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(BioOrangeSoftTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User Avatar",
                            tint = BioOrangePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.userName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = profile.deviceLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                val isOptimal = !profile.cardioStatus.contains("NO DATA")
                Surface(
                    color = if (isOptimal) EmeraldSoftTint else InnerWellSurface,
                    shape = BioTraceTokens.PillShape
                ) {
                    Text(
                        text = profile.cardioStatus,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOptimal) EmeraldPrimary else TextMuted,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3-Metric Triptych Capsules Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Triptych Capsule 1: Resting HR
                TriptychCapsule(
                    icon = Icons.Default.Favorite,
                    iconTint = BioOrangePrimary,
                    iconBg = BioOrangeSoftTint,
                    title = "Resting HR",
                    value = profile.restingHr,
                    subtext = profile.restingHrDelta,
                    subtextColor = EmeraldPrimary,
                    modifier = Modifier.weight(1f)
                )

                // Triptych Capsule 2: Sleep Recovery
                TriptychCapsule(
                    icon = Icons.Default.Bedtime,
                    iconTint = IndigoSleepPrimary,
                    iconBg = IndigoSleepSoftTint,
                    title = "Sleep Score",
                    value = profile.sleepScore,
                    subtext = profile.sleepStatus,
                    subtextColor = IndigoSleepPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun TriptychCapsule(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    value: String,
    subtext: String,
    subtextColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = InnerWellSurface,
        shape = BioTraceTokens.InnerWellShape,
        border = BioTraceTokens.WellHairlineBorder,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(15.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                maxLines = 1
            )

            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtext,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = subtextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// =============================================================================
// Correlation Card Composable
// =============================================================================

@Composable
fun CorrelationCard(
    item: CorrelationCardData,
    modifier: Modifier = Modifier
) {
    Surface(
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
            // Header Row: Category Pill + Status Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.category,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )

                Surface(
                    color = EmeraldSoftTint,
                    shape = BioTraceTokens.PillShape
                ) {
                    Text(
                        text = item.couplingScore,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Clinical Insight Box
            Surface(
                color = InnerWellSurface,
                shape = BioTraceTokens.InnerWellSmallShape,
                border = BioTraceTokens.WellHairlineBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.primaryInsight,
                    fontSize = 12.sp,
                    color = TextPrimary,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Recessed Metric Coupling Wells
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.metric1Label,
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = item.metric1Value,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(DividerSubtle)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = item.metric2Label,
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = item.metric2Value,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "• ${item.statusTag}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
            }
        }
    }
}

// =============================================================================
// Lab Hub Integration & Invite Cards
// =============================================================================

@Composable
fun LabBiomarkersSummaryCard(
    count: Int,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(EmeraldSoftTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Blood Lab Correlation Active",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "$count clinical biomarkers loaded",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View Labs",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun LabTriangulationInviteCard(
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BioOrangeSoftTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = BioOrangePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Triangulate with Blood Labs",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Import PDF lab report in Lab Hub to correlate lipids & glucose with wearable vitals.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open Labs",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// =============================================================================
// AI Health Report Card
// =============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiReportCard(
    reportState: AiReportState,
    isApiKeyConfigured: Boolean,
    onGenerateClick: () -> Unit,
    onNavigateToSettings: () -> Unit,
    structuredReport: DailyBiometricInsightReport? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = CardSurface,
        shape = BioTraceTokens.BentoCardShape,
        border = BioTraceTokens.CardHairlineBorder,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(BioOrangeSoftTint),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = BioOrangePrimary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AI Health Report",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                        Text(
                            text = "Powered by Gemini · ${io.biotrace.app.data.llm.GeminiClient.MODEL_NAME}",
                            fontSize = 10.sp,
                            color = TextMuted,
                        )
                    }
                }
                if (reportState is AiReportState.Loading || reportState is AiReportState.Streaming) {
                    CircularProgressIndicator(
                        color = BioOrangePrimary,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Report content area
            if (reportState is AiReportState.Complete && structuredReport != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Headline Summary
                    Surface(
                        color = InnerWellSurface,
                        shape = BioTraceTokens.InnerWellSmallShape,
                        border = BioTraceTokens.WellHairlineBorder,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = BioOrangePrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "HEADLINE SUMMARY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = structuredReport.headlineSummary,
                                fontSize = 13.sp,
                                color = TextPrimary,
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Trend Highlights
                    if (structuredReport.trendHighlights.isNotEmpty()) {
                        Text(
                            text = "TREND HIGHLIGHTS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(top = 2.dp, start = 2.dp)
                        )
                        structuredReport.trendHighlights.forEach { highlight ->
                            Surface(
                                color = InnerWellSurface,
                                shape = BioTraceTokens.InnerWellSmallShape,
                                border = BioTraceTokens.WellHairlineBorder,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    // Top Row: Domain tags and Direction pill
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Domain tag pills
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            highlight.domainTags.forEach { tag ->
                                                val (bgColor, textColor, label) = when (tag) {
                                                    InsightDomainTag.WEARABLE -> Triple(CobaltBlueSoftTint, CobaltBluePrimary, "WEARABLE")
                                                    InsightDomainTag.LAB_PANEL -> Triple(EmeraldSoftTint, EmeraldPrimary, "LAB PANEL")
                                                    InsightDomainTag.NUTRITION -> Triple(BioOrangeSoftTint, BioOrangePrimary, "NUTRITION")
                                                    InsightDomainTag.CIRCADIAN -> Triple(IndigoSleepSoftTint, IndigoSleepPrimary, "CIRCADIAN")
                                                }
                                                Surface(
                                                    color = bgColor,
                                                    shape = BioTraceTokens.PillShape
                                                ) {
                                                    Text(
                                                        text = label,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = textColor,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        // Direction pill
                                        val (dirBg, dirColor, dirLabel) = when (highlight.direction) {
                                            TrendDirection.IMPROVING -> Triple(EmeraldSoftTint, EmeraldPrimary, "IMPROVING")
                                            TrendDirection.STABLE -> Triple(CardSurface, TextSecondary, "STABLE")
                                            TrendDirection.DRIFTING_OUT_OF_RANGE -> Triple(SolarAmberSoftTint, SolarAmberPrimary, "DRIFTING")
                                            TrendDirection.ACUTE_STRAIN -> Triple(CoralAlertSoftTint, CoralAlertPrimary, "ACUTE STRAIN")
                                        }
                                        Surface(
                                            color = dirBg,
                                            shape = BioTraceTokens.PillShape
                                        ) {
                                            Text(
                                                text = dirLabel,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = dirColor,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Title
                                    Text(
                                        text = highlight.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Insight text
                                    Text(
                                        text = highlight.insightText,
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        lineHeight = 17.sp
                                    )

                                    // Clickable AssistChips for supportingRecordIds
                                    if (highlight.supportingRecordIds.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            highlight.supportingRecordIds.forEach { recordId ->
                                                AssistChip(
                                                    onClick = { },
                                                    label = {
                                                        Text(
                                                            text = recordId,
                                                            fontSize = 10.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    },
                                                    colors = AssistChipDefaults.assistChipColors(
                                                        containerColor = CardSurface,
                                                        labelColor = TextSecondary
                                                    ),
                                                    border = AssistChipDefaults.assistChipBorder(
                                                        enabled = true,
                                                        borderColor = DividerSubtle
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Actionable Focus Card
                    Surface(
                        color = InnerWellSurface,
                        shape = BioTraceTokens.InnerWellSmallShape,
                        border = BioTraceTokens.WellHairlineBorder,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Surface(
                                color = BioOrangeSoftTint,
                                shape = BioTraceTokens.PillShape
                            ) {
                                Text(
                                    text = "ACTIONABLE FOCUS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BioOrangePrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = structuredReport.actionableFocus.recommendation,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                lineHeight = 18.sp
                            )

                            if (structuredReport.actionableFocus.rationaleMetric.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = CardSurface,
                                    shape = BioTraceTokens.InnerWellSmallShape,
                                    border = BioTraceTokens.WellHairlineBorder,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "RATIONALE METRIC",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextMuted,
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = structuredReport.actionableFocus.rationaleMetric,
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Surface(
                    color = InnerWellSurface,
                    shape = BioTraceTokens.InnerWellSmallShape,
                    border = BioTraceTokens.WellHairlineBorder,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    when (reportState) {
                        is AiReportState.Idle -> {
                            Text(
                                text = "Tap \u201cGenerate Report\u201d to get a personalised AI health summary based on your Health Connect data.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(14.dp),
                            )
                        }
                        is AiReportState.Loading -> {
                            Text(
                                text = "Analysing your health data…",
                                fontSize = 12.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(14.dp),
                            )
                        }
                        is AiReportState.Streaming -> {
                            Text(
                                text = reportState.text,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(14.dp),
                            )
                        }
                        is AiReportState.Complete -> {
                            Text(
                                text = reportState.text,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(14.dp),
                            )
                        }
                        is AiReportState.Error -> {
                            Text(
                                text = "⚠ ${reportState.message}",
                                fontSize = 12.sp,
                                color = androidx.compose.ui.graphics.Color(0xFFF59E0B),
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(14.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // CTA row
            if (!isApiKeyConfigured) {
                Surface(
                    onClick = onNavigateToSettings,
                    color = BioOrangeSoftTint,
                    shape = BioTraceTokens.PillShape,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = BioOrangePrimary,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Set up Gemini API key in Settings →",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BioOrangePrimary,
                        )
                    }
                }
            } else {
                val isGenerating = reportState is AiReportState.Loading || reportState is AiReportState.Streaming
                androidx.compose.material3.Button(
                    onClick = onGenerateClick,
                    enabled = !isGenerating,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = BioOrangePrimary,
                        disabledContainerColor = BioOrangeSoftTint,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (isGenerating) "Generating…" else "Generate Report",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

// =============================================================================
// Biological Age & Longevity Hero Card
// =============================================================================

@Composable
fun BiologicalAgeHeroCard(
    result: BiologicalAgeResult,
    onEditAgeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = CardSurface,
        shape = BioTraceTokens.BentoCardShape,
        border = BioTraceTokens.CardHairlineBorder,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Title, Subtitle, and Confidence Badge Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Biological Age & Longevity Index",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "PhenoAge & Autonomic Biomarker Pace",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }

                val confidence = result.confidencePercentage
                val badgeBg = if (confidence >= 60) EmeraldSoftTint else CobaltBlueSoftTint
                val badgeColor = if (confidence >= 60) EmeraldPrimary else CobaltBluePrimary

                Surface(
                    color = badgeBg,
                    shape = BioTraceTokens.PillShape
                ) {
                    Text(
                        text = "$confidence% Confidence",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Metric Row: Left (Biological Age + Delta Pill) & Right (Chronological Age + Pace)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Large Biological Age + Delta pill
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format(Locale.ROOT, "%.1f", result.biologicalAge),
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            letterSpacing = (-1).sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "yrs",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val isYounger = result.ageDelta < 0.0
                    val isOlder = result.ageDelta > 0.0
                    val deltaText = when {
                        isYounger -> "-${String.format(Locale.ROOT, "%.1f", kotlin.math.abs(result.ageDelta))} yrs younger"
                        isOlder -> "+${String.format(Locale.ROOT, "%.1f", result.ageDelta)} yrs accelerated"
                        else -> "Baseline pace"
                    }
                    val deltaBg = if (!isOlder) EmeraldSoftTint else SolarAmberSoftTint
                    val deltaColor = if (!isOlder) EmeraldPrimary else SolarAmberPrimary

                    Surface(
                        color = deltaBg,
                        shape = BioTraceTokens.PillShape
                    ) {
                        Text(
                            text = deltaText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = deltaColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Right Column: Chronological Age with Edit button & Aging Pace
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(BioTraceTokens.PillShape)
                            .clickable { onEditAgeClick() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Chronological: ${result.chronologicalAge} yrs",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Chronological Age",
                            tint = BioOrangePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Surface(
                        color = InnerWellSurface,
                        shape = BioTraceTokens.PillShape,
                        border = BioTraceTokens.WellHairlineBorder
                    ) {
                        Text(
                            text = "Pace: ${String.format(Locale.ROOT, "%.2f", result.agingPace)}x",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status Tier Banner
            val tierColor = Color(result.longevityTier.colorHex)
            Surface(
                color = tierColor.copy(alpha = 0.1f),
                shape = BioTraceTokens.InnerWellSmallShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, tierColor.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(tierColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = result.longevityTier.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = tierColor,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = result.longevityTier.subtitle,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Top Longevity Drivers
            if (result.drivers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Key Longevity Drivers",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = InnerWellSurface,
                    shape = BioTraceTokens.InnerWellSmallShape,
                    border = BioTraceTokens.WellHairlineBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        result.drivers.take(3).forEach { driver ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isBonus = driver.isBonus
                                val icon = if (isBonus) Icons.Default.Check else Icons.Default.Warning
                                val tint = if (isBonus) EmeraldPrimary else SolarAmberPrimary
                                val bg = if (isBonus) EmeraldSoftTint else SolarAmberSoftTint

                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(bg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = tint,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    val sign = if (driver.impactYears > 0) "+" else ""
                                    val impactFormatted = "$sign${String.format(Locale.ROOT, "%.1f", driver.impactYears)} yrs"
                                    Text(
                                        text = "${driver.title} ($impactFormatted)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = driver.insight,
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (driver.biomarkerValue.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = driver.biomarkerValue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Primary Actionable Recommendation text
            if (result.primaryRecommendation.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    color = InnerWellSurface,
                    shape = BioTraceTokens.InnerWellSmallShape,
                    border = BioTraceTokens.WellHairlineBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = BioOrangePrimary,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Longevity Protocol Recommendation",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BioOrangePrimary,
                                letterSpacing = 0.3.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = result.primaryRecommendation,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// Edit Chronological Age Dialog
// =============================================================================

@Composable
fun EditChronologicalAgeDialog(
    currentAge: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var ageState by remember(currentAge) { mutableIntStateOf(currentAge.coerceIn(18, 100)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Chronological Age",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Adjust your chronological age baseline (18–100) to calibrate phenotypic pace.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { if (ageState > 18) ageState-- },
                        enabled = ageState > 18
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = InnerWellSurface,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "−",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ageState > 18) TextPrimary else TextMuted
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Text(
                        text = "$ageState yrs",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.width(20.dp))

                    IconButton(
                        onClick = { if (ageState < 100) ageState++ },
                        enabled = ageState < 100
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = InnerWellSurface,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "+",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ageState < 100) TextPrimary else TextMuted
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(ageState) },
                colors = ButtonDefaults.textButtonColors(contentColor = BioOrangePrimary)
            ) {
                Text(text = "Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) {
                Text(text = "Cancel")
            }
        },
        containerColor = CardSurface,
        shape = BioTraceTokens.BentoCardShape
    )
}

// =============================================================================
// Phase 8: Overhauled Hub Components (Tab Bar, Clinical Safety & Citations)
// =============================================================================

@Composable
fun InsightsTabBar(
    selectedTab: InsightsTab,
    onTabSelected: (InsightsTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(InnerWellSurface, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        InsightsTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val label = when (tab) {
                InsightsTab.OVERVIEW -> "Overview"
                InsightsTab.CORRELATIONS -> "Correlations"
                InsightsTab.CLINICAL_RISKS -> "Clinical Risks"
                InsightsTab.AI_DEEP_DIVE -> "AI Deep Dive"
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onTabSelected(tab) },
                color = if (isSelected) CardSurface else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
                border = if (isSelected) BioTraceTokens.CardHairlineBorder else null
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) BioOrangePrimary else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun OverviewReadinessCard(
    profile: ProfileTriptychSummary,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = BioTraceTokens.BentoCardShape,
        color = CardSurface,
        border = BioTraceTokens.CardHairlineBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Readiness & Longevity",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Unified autonomic and metabolic capacity",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .background(EmeraldSoftTint, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = profile.cardioStatus.ifBlank { "ACTIVE" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(InnerWellSurface, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Resting Tone", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "${profile.restingHr} bpm", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Column {
                    Text(text = "Sleep Quality", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = profile.sleepScore, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Column {
                    Text(text = "Recovery Tone", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = profile.sleepStatus, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
fun InAppAnomalyAlertCard(
    alert: AnomalyAlert,
    modifier: Modifier = Modifier
) {
    val isAlert = alert.severity.equals("ALERT", ignoreCase = true) || alert.severity.equals("CRITICAL", ignoreCase = true)
    val accentColor = if (isAlert) CoralAlertPrimary else SolarAmberPrimary
    val accentTint = if (isAlert) CoralAlertSoftTint else SolarAmberSoftTint

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = BioTraceTokens.BentoCardShape,
        color = CardSurface,
        border = BioTraceTokens.CardHairlineBorder
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = alert.metricName.replace('_', ' '),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .background(accentTint, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = alert.severity.uppercase(Locale.ROOT),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = alert.narrative,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun ClinicalSafetyNoticeBannerCard(
    notice: String,
    highestStatus: ClinicalStatus?,
    citations: List<Citation>,
    modifier: Modifier = Modifier
) {
    val isCritical = highestStatus == ClinicalStatus.CRITICAL
    val accentColor = if (isCritical) CoralAlertPrimary else SolarAmberPrimary
    val accentTint = if (isCritical) CoralAlertSoftTint else SolarAmberSoftTint

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = BioTraceTokens.BentoCardShape,
        color = accentTint,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CLINICAL SAFETY NOTICE",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = accentColor,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = notice,
                fontSize = 12.sp,
                color = TextPrimary,
                lineHeight = 17.sp
            )

            if (citations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Evidence Grounding (${citations.size} Health Connect Citations):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                citations.take(3).forEach { citation ->
                    Text(
                        text = "• ${citation.format()}",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun GroundedCitationsCard(
    citations: List<Citation>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = BioTraceTokens.BentoCardShape,
        color = CardSurface,
        border = BioTraceTokens.CardHairlineBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = CobaltBluePrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Grounded Medical Citations",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            citations.forEach { citation ->
                Text(
                    text = citation.format(),
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
