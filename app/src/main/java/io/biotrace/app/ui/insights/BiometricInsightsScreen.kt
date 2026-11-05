package io.biotrace.app.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.biotrace.app.domain.model.ProfileTriptychSummary
import io.biotrace.app.ui.common.BioTraceTopBar
import io.biotrace.app.ui.theme.AppCanvasBackground
import io.biotrace.app.ui.theme.BioOrangePrimary
import io.biotrace.app.ui.theme.BioOrangeSoftTint
import io.biotrace.app.ui.theme.BioTraceTokens
import io.biotrace.app.ui.theme.CardSurface
import io.biotrace.app.ui.theme.CobaltBluePrimary
import io.biotrace.app.ui.theme.DividerSubtle
import io.biotrace.app.ui.theme.EmeraldPrimary
import io.biotrace.app.ui.theme.EmeraldSoftTint
import io.biotrace.app.ui.theme.IndigoSleepPrimary
import io.biotrace.app.ui.theme.IndigoSleepSoftTint
import io.biotrace.app.ui.theme.InnerWellSurface
import io.biotrace.app.ui.theme.SolarAmberPrimary
import io.biotrace.app.ui.theme.TextMuted
import io.biotrace.app.ui.theme.TextPrimary
import io.biotrace.app.ui.theme.TextSecondary
import io.biotrace.app.ui.theme.TextWhite

@Composable
fun BiometricInsightsScreen(
    onNavigateToLabs: () -> Unit = {},
    viewModel: BiometricInsightsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = AppCanvasBackground,
        topBar = {
            BioTraceTopBar(subtitle = "Cross-Correlation & Physiological Models")
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

                // 2. Profile & 3-Metric Triptych Hero Card
                item {
                    ProfileTriptychHeroCard(profile = uiState.profile)
                }

                // 3. Cross-Correlation Section Header
                item {
                    Text(
                        text = "Physiological Cross-Correlations",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                // 4. Cross-Correlation Cards
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

                // 6. Blood Biomarker Triangulation Banner
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
        shadowElevation = BioTraceTokens.HeroCardElevation,
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

                // Triptych Capsule 3: VO2 Max
                TriptychCapsule(
                    icon = Icons.Default.Speed,
                    iconTint = EmeraldPrimary,
                    iconBg = EmeraldSoftTint,
                    title = "Cardio Fitness",
                    value = profile.vo2Max,
                    subtext = profile.vo2MaxTier,
                    subtextColor = TextSecondary,
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
        shadowElevation = BioTraceTokens.BentoCardElevation,
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
        shadowElevation = BioTraceTokens.BentoCardElevation,
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
        shadowElevation = BioTraceTokens.BentoCardElevation,
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
