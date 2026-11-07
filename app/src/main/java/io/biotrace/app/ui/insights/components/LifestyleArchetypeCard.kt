package io.biotrace.app.ui.insights.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.biotrace.app.domain.analytics.clustering.LifestyleArchetype
import io.biotrace.app.domain.analytics.clustering.LifestyleClusterResult
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
fun LifestyleArchetypeCard(
    result: LifestyleClusterResult,
    modifier: Modifier = Modifier
) {
    val (accentColor, accentTint, title, description) = when (result.archetype) {
        LifestyleArchetype.PEAK_PERFORMANCE -> Quad(
            CobaltBluePrimary,
            CobaltBlueSoftTint,
            "Peak Performance",
            "High daily physical exertion matched with resilient autonomic recovery and optimal energy burn."
        )
        LifestyleArchetype.DEEP_RECOVERY -> Quad(
            IndigoSleepPrimary,
            IndigoSleepSoftTint,
            "Deep Recovery",
            "Restorative sleep duration and elevated parasympathetic vagal tone supporting cellular repair."
        )
        LifestyleArchetype.STRAIN_DEPLETED -> Quad(
            CoralAlertPrimary,
            CoralAlertSoftTint,
            "Strain Depleted",
            "High allostatic stress marked by curtailed sleep, depressed HRV, or late evening metabolic intake."
        )
        LifestyleArchetype.SEDENTARY_MAINTENANCE -> Quad(
            SolarAmberPrimary,
            SolarAmberSoftTint,
            "Sedentary Maintenance",
            "Low physical exertion with baseline physiological stability and maintenance energy expenditure."
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = BioTraceTokens.BentoCardShape,
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BioTraceTokens.CardHairlineBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Category & Tag Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIFESTYLE ARCHETYPE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.1.sp
                )

                Text(
                    text = "${result.dailyAssignments.size} Days Clustered",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dominant Archetype Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )

                Box(
                    modifier = Modifier
                        .background(accentTint, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "%.1f%% Dominant".format(Locale.US, result.dominantPercentage),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            if (result.clusterInsights.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(InnerWellSurface, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    result.clusterInsights.forEach { insight ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 5.dp, end = 8.dp)
                                    .size(6.dp)
                                    .background(accentColor, CircleShape)
                            )
                            Text(
                                text = insight,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
