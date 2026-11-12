package io.biotrace.app.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.biotrace.app.ui.navigation.Screen
import io.biotrace.app.ui.theme.BioOrangePrimary
import io.biotrace.app.ui.theme.BioTraceTokens
import io.biotrace.app.ui.theme.CardBorder
import io.biotrace.app.ui.theme.CardSurface
import io.biotrace.app.ui.theme.TextSecondary
import io.biotrace.app.ui.theme.TextWhite

data class BioTraceBottomBarItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

val bioTraceBottomTabs = listOf(
    BioTraceBottomBarItem("Vitals", Icons.Default.Favorite, Screen.Vitals.route),
    BioTraceBottomBarItem("Lab Hub", Icons.Default.Science, Screen.Labs.route),
    BioTraceBottomBarItem("Macro Cam", Icons.Default.CameraAlt, Screen.Nutrition.route),
    BioTraceBottomBarItem("Insights", Icons.Default.AutoAwesome, Screen.BiometricInsights.route)
)

@Composable
fun BioTraceFloatingBottomBar(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onCopilotClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = CardSurface,
            shape = BioTraceTokens.FloatingBarShape,
            border = BioTraceTokens.CardHairlineBorder,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Voice / Copilot trigger (Microphone button on left)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onCopilotClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Copilot",
                        tint = TextWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Hairline vertical divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(CardBorder)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Navigation Tabs
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bioTraceBottomTabs.forEachIndexed { index, tab ->
                        val isSelected = index == selectedIndex

                        val activeBgColor by animateColorAsState(
                            targetValue = if (isSelected) BioOrangePrimary else Color.Transparent,
                            animationSpec = tween(durationMillis = 200),
                            label = "tab_bg"
                        )
                        val iconTint by animateColorAsState(
                            targetValue = if (isSelected) TextWhite else TextSecondary,
                            animationSpec = tween(durationMillis = 200),
                            label = "tab_icon_tint"
                        )

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(activeBgColor)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (!isSelected) {
                                        onSelect(index)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = iconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
