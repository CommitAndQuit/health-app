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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.biotrace.app.data.healthconnect.LastSyncStore
import io.biotrace.app.ui.theme.AppCanvasBackground
import io.biotrace.app.ui.theme.BioOrangePrimary
import io.biotrace.app.ui.theme.CardSurface
import io.biotrace.app.ui.theme.ClinicalEmerald
import io.biotrace.app.ui.theme.InnerWellSurface
import io.biotrace.app.ui.theme.TextPrimary
import io.biotrace.app.ui.theme.TextSecondary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun BioTraceTopBar(
    subtitle: String? = null,
    onAvatarClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showDataSourceDialog by remember { mutableStateOf(false) }

    if (showDataSourceDialog) {
        DataSourceDialog(
            onDismiss = { showDataSourceDialog = false }
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppCanvasBackground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: "BioTrace" wordmark + 6.dp BioOrangePrimary circle dot
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BioTrace",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(BioOrangePrimary)
            )
        }

        // Right: 38.dp circular avatar button with white 2.dp border and subtle elevation
        Box(
            modifier = Modifier
                .size(38.dp)
                .shadow(elevation = 3.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(CardSurface)
                .border(2.dp, CardSurface, CircleShape)
                .clickable {
                    if (onAvatarClick != null) {
                        onAvatarClick()
                    } else {
                        showDataSourceDialog = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Data source",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun DataSourceDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lastSync = remember { LastSyncStore.lastSyncMillis(context) }
    val lastSyncLabel = remember(lastSync) {
        if (lastSync != null) {
            val formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.getDefault())
            val formatted = formatter.format(Instant.ofEpochMilli(lastSync).atZone(ZoneId.systemDefault()))
            "Last sync: $formatted"
        } else {
            "Last sync: Not synced yet"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Data Source",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = InnerWellSurface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ClinicalEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Health Connect",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "BioTrace reads health data directly from Health Connect. Data appears here from any app you allow to write to Health Connect.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = lastSyncLabel,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Close",
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}
