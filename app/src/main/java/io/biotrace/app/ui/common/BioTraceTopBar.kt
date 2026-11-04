package io.biotrace.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.biotrace.app.ui.theme.CardBorder
import io.biotrace.app.ui.theme.LightBackground
import io.biotrace.app.ui.theme.PureWhite
import io.biotrace.app.ui.theme.SoftInputBg
import io.biotrace.app.ui.theme.TextDark
import io.biotrace.app.ui.theme.TextMuted
import io.biotrace.app.ui.theme.TextSecondary

@Composable
fun BioTraceTopBar(
    subtitle: String = "Nutrition",
    onAvatarClick: () -> Unit = {},
    onSettingsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(LightBackground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "BioTrace",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextDark,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onSettingsClick != null) {
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // User Profile Avatar
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(PureWhite)
                    .border(1.5.dp, CardBorder, CircleShape)
                    .clickable { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
