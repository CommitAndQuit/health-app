package io.biotrace.app.ui.nutrition

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import io.biotrace.app.R
import io.biotrace.app.data.local.entity.MealEntity
import io.biotrace.app.ui.common.BioTraceTopBar
import io.biotrace.app.ui.theme.BioOrange
import io.biotrace.app.ui.theme.CardBorder
import io.biotrace.app.ui.theme.ClinicalEmerald
import io.biotrace.app.ui.theme.ClinicalEmeraldDark
import io.biotrace.app.ui.theme.CorrelationPeachBg
import io.biotrace.app.ui.theme.CorrelationPeachBorder
import io.biotrace.app.ui.theme.LightBackground
import io.biotrace.app.ui.theme.MacroCarbsColor
import io.biotrace.app.ui.theme.MacroFatsColor
import io.biotrace.app.ui.theme.MacroProteinColor
import io.biotrace.app.ui.theme.MintBadgeBg
import io.biotrace.app.ui.theme.MintBadgeText
import io.biotrace.app.ui.theme.PureWhite
import io.biotrace.app.ui.theme.TextDark
import io.biotrace.app.ui.theme.TextMuted
import io.biotrace.app.ui.theme.TextSecondary
import java.io.ByteArrayOutputStream
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun FoodScanScreen(
    onNavigateToSettings: () -> Unit = {},
    viewModel: FoodScanViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            viewModel.scanFoodPhoto(base64)
        }
    }

    if (uiState.isManualEntryOpen) {
        ManualMealEntryDialog(
            onDismiss = { viewModel.openManualEntry(false) },
            onSave = { name, cals, p, c, f, fiber ->
                viewModel.addManualMeal(name, cals, p, c, f, fiber)
            }
        )
    }

    Scaffold(
        containerColor = LightBackground,
        topBar = {
            BioTraceTopBar(
                onProfileClick = onNavigateToSettings
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Scanning progress indicator
            if (uiState.isScanning) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BioOrange),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = BioOrange,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = uiState.scanMessage ?: "Analyzing meal photo with Vision AI...",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextDark
                            )
                        }
                    }
                }
            }

            // Error banner if any
            if (uiState.error != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.error ?: "",
                            color = Color(0xFFB91C1C),
                            fontSize = 12.5.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            // CARD 1: METABOLIC TELEMETRY - "Today's Nutrition"
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Card Header & Date Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "METABOLIC TELEMETRY",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = TextMuted
                                )
                                Text(
                                    text = "Today's Nutrition",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }

                            // Date Badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFF1ED),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = BioOrange,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Today, Oct 24",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextDark
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Ring Gauge & Calorie Target Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Circular Burnt Progress Ring
                            Box(
                                modifier = Modifier.size(105.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val percentage = uiState.burntPercentage
                                Canvas(modifier = Modifier.size(95.dp)) {
                                    // Background track
                                    drawArc(
                                        color = Color(0xFFF1F5F9),
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                    // Active burnt arc
                                    drawArc(
                                        color = BioOrange,
                                        startAngle = -90f,
                                        sweepAngle = (percentage / 100f) * 360f,
                                        useCenter = false,
                                        style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "BURNT",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "$percentage%",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextDark
                                    )
                                    Text(
                                        text = "ON TRACK",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BioOrange
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            // Energy Target Readout
                            Column {
                                Text(
                                    text = "Daily Energy Target",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Text(
                                        text = "%,d".format(Locale.US, uiState.totalCalories),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextDark
                                    )
                                    Text(
                                        text = " / %,d".format(Locale.US, uiState.dailyTargetCalories),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextMuted,
                                        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                                    )
                                    Text(
                                        text = " kcal",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BioOrange,
                                        modifier = Modifier.padding(bottom = 5.dp, start = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                val remaining = (uiState.dailyTargetCalories - uiState.totalCalories).coerceAtLeast(0)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MintBadgeBg
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(MintBadgeText)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "$remaining KCAL REMAINING",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MintBadgeText,
                                            letterSpacing = 0.4.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 3 Macro Progress Cards (Protein, Carbs, Fats)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MacroStatCard(
                                title = "PROTEIN",
                                icon = { ProteinIcon(color = MacroProteinColor) },
                                amountGrams = uiState.totalProtein.toInt(),
                                goalGrams = uiState.proteinGoal.toInt(),
                                barColor = MacroProteinColor,
                                modifier = Modifier.weight(1f)
                            )
                            MacroStatCard(
                                title = "CARBS",
                                icon = { CarbsIcon(color = MacroCarbsColor) },
                                amountGrams = uiState.totalCarbs.toInt(),
                                goalGrams = uiState.carbsGoal.toInt(),
                                barColor = MacroCarbsColor,
                                modifier = Modifier.weight(1f)
                            )
                            MacroStatCard(
                                title = "FATS",
                                icon = { FatsIcon(color = MacroFatsColor) },
                                amountGrams = uiState.totalFats.toInt(),
                                goalGrams = uiState.fatsGoal.toInt(),
                                barColor = MacroFatsColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // CARD 2: "Smart Macro Scan" (Vision LLM & Meal Capture)
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Section Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFFFF1ED))
                                        .border(1.dp, Color(0xFFFFD8CC), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = BioOrange,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Smart Macro Scan",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                    Text(
                                        text = "ON-DEVICE VISION LLM v2.4",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        color = ClinicalEmeraldDark
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MintBadgeBg
                            ) {
                                Text(
                                    text = "CONFIDENCE ${uiState.scanConfidence}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MintBadgeText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Viewfinder Food Image Preview Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF2C2F36),
                                            Color(0xFF1E2128)
                                        )
                                    )
                                )
                        ) {
                            // High fidelity photorealistic food presentation
                            Image(
                                painter = painterResource(id = R.drawable.plate_salmon_scan),
                                contentDescription = "Smart Macro Scan Meal Plate",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Orange Viewfinder Corner Crop Marks
                            CornerBracketMarks(
                                color = BioOrange,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp)
                            )

                            // Floating Semantic Tags directly overlaid on plate
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PureWhite
                                    ) {
                                        Text(
                                            text = "Est: 640 kcal",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PureWhite
                                    ) {
                                        Text(
                                            text = "Protein: 42g",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PureWhite
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(ClinicalEmerald)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "GLYCEMIC: LOW",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ClinicalEmeraldDark
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PureWhite
                                    ) {
                                        Text(
                                            text = "PLATE SEGMENTED",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Full-width Orange Action Button
                        Button(
                            onClick = { launcher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BioOrange,
                                contentColor = PureWhite
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Log Another Meal (Snap Photo)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp
                            )
                        }
                    }
                }
            }

            // CARD 3: "Wearable Correlation Insight" (BIO-CORRELATION PILLAR 5)
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CorrelationPeachBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CorrelationPeachBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "BIO-CORRELATION PILLAR 5",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = BioOrange,
                            letterSpacing = 0.8.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(BioOrange),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = null,
                                        tint = PureWhite,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Wearable Correlation\nInsight",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    lineHeight = 21.sp
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = BioOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Narrative Text with bold highlighting
                        val annotatedText = buildAnnotatedString {
                            append("Dinner logged ")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = BioOrange)) {
                                append("before 7:30 PM")
                            }
                            append(" correlates with ")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = ClinicalEmeraldDark)) {
                                append("+14% higher")
                            }
                            append(" overnight HRV (")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("76 ms")
                            }
                            append(" vs 62 ms avg) and ")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = TextDark)) {
                                append("28 min additional Deep Sleep")
                            }
                            append(".")
                        }

                        Text(
                            text = annotatedText,
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp,
                            color = TextDark
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Inner Comparison Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Overnight HRV Recovery by Meal Timing",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextDark
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MintBadgeBg
                                    ) {
                                        Text(
                                            text = "+14 ms Delta",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MintBadgeText,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Row 1: Before 7:30 PM
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Nightlight,
                                        contentDescription = null,
                                        tint = ClinicalEmerald,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Before 7:30 PM",
                                        fontSize = 11.sp,
                                        color = TextDark,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "76 ms (Restorative)",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ClinicalEmeraldDark
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { 0.76f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = ClinicalEmerald,
                                    trackColor = Color(0xFFF1F5F9),
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Row 2: After 8:30 PM
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Nightlight,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "After 8:30 PM",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "62 ms (Sub-baseline)",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { 0.62f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Color(0xFFCBD5E1),
                                    trackColor = Color(0xFFF1F5F9),
                                )
                            }
                        }
                    }
                }
            }

            // CARD 4: "Logged Intake"
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
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
                                    text = "Logged Intake",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = "${uiState.todayMeals.size} meals calculated today",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { viewModel.openManualEntry(true) }
                            ) {
                                Text(
                                    text = "View History >",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BioOrange
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Meal Items List
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            uiState.todayMeals.forEach { meal ->
                                MealIntakeRowItem(meal = meal)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProteinIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(13.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.6.dp.toPx()

        // Arrow 1: center to top-right
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w * 0.42f, h * 0.58f),
            end = androidx.compose.ui.geometry.Offset(w * 0.95f, h * 0.05f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // Arrowhead 1
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w * 0.6f, h * 0.05f),
            end = androidx.compose.ui.geometry.Offset(w * 0.95f, h * 0.05f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w * 0.95f, h * 0.4f),
            end = androidx.compose.ui.geometry.Offset(w * 0.95f, h * 0.05f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Arrow 2: center to bottom-left
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w * 0.58f, h * 0.42f),
            end = androidx.compose.ui.geometry.Offset(w * 0.05f, h * 0.95f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // Arrowhead 2
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w * 0.4f, h * 0.95f),
            end = androidx.compose.ui.geometry.Offset(w * 0.05f, h * 0.95f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w * 0.05f, h * 0.6f),
            end = androidx.compose.ui.geometry.Offset(w * 0.05f, h * 0.95f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun CarbsIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(13.dp)) {
        val radius = 1.25.dp.toPx()
        val spacingX = size.width * 0.38f
        val spacingY = size.height * 0.32f
        val startX = size.width * 0.31f
        val startY = size.height * 0.2f

        for (row in 0..2) {
            for (col in 0..1) {
                drawCircle(
                    color = color,
                    radius = radius,
                    center = androidx.compose.ui.geometry.Offset(
                        startX + col * spacingX,
                        startY + row * spacingY
                    )
                )
            }
        }
    }
}

@Composable
fun FatsIcon(color: Color, modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Default.WaterDrop,
        contentDescription = null,
        tint = color,
        modifier = modifier.size(13.dp)
    )
}

@Composable
fun MacroStatCard(
    title: String,
    icon: @Composable () -> Unit,
    amountGrams: Int,
    goalGrams: Int,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = TextSecondary
                )
                icon()
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${amountGrams}g",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(6.dp))

            val progress = (amountGrams.toFloat() / goalGrams.toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = barColor,
                trackColor = Color(0xFFE2E8F0),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Goal ${goalGrams}g",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted
            )
        }
    }
}

@Composable
fun MealIntakeRowItem(meal: MealEntity) {
    val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US).withZone(ZoneId.systemDefault())
    val timeFormatted = formatter.format(meal.consumedAt)

    val category = when {
        meal.name.contains("Oatmeal", ignoreCase = true) || meal.name.contains("Breakfast", ignoreCase = true) -> "BREAKFAST"
        meal.name.contains("Bowl", ignoreCase = true) || meal.name.contains("Salmon", ignoreCase = true) || meal.name.contains("Lunch", ignoreCase = true) -> "LUNCH"
        meal.name.contains("Yogurt", ignoreCase = true) || meal.name.contains("Snack", ignoreCase = true) -> "SNACK"
        else -> "MEAL"
    }

    val thumbnailRes = when {
        meal.name.contains("Oatmeal", ignoreCase = true) -> R.drawable.thumb_oatmeal
        meal.name.contains("Salmon", ignoreCase = true) || meal.name.contains("Bowl", ignoreCase = true) -> R.drawable.thumb_salmon
        meal.name.contains("Yogurt", ignoreCase = true) -> R.drawable.thumb_yogurt
        else -> null
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PureWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Food Thumbnail Box
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFF1ED)),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailRes != null) {
                    Image(
                        painter = painterResource(id = thumbnailRes),
                        contentDescription = meal.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = BioOrange,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$timeFormatted • $category",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClinicalEmeraldDark,
                    letterSpacing = 0.4.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = meal.name,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${meal.calories} kcal • ${meal.proteinGrams.toInt()}g Protein",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            // Green Verified Badge
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE6F8F0))
                    .border(1.dp, Color(0xFFA7F3D0), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Verified",
                    tint = ClinicalEmeraldDark,
                    modifier = Modifier.size(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun PlateGraphicBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width * 0.5f
        val centerY = size.height * 0.48f
        val radius = size.height * 0.42f

        // Outer plate rim
        drawCircle(
            color = Color(0xFF3B4048),
            radius = radius,
            center = androidx.compose.ui.geometry.Offset(centerX, centerY)
        )
        // Inner plate dish
        drawCircle(
            color = Color(0xFFE2D9CC),
            radius = radius * 0.88f,
            center = androidx.compose.ui.geometry.Offset(centerX, centerY)
        )
        // Salmon fillet shape
        drawRoundRect(
            color = Color(0xFFE76F51),
            topLeft = androidx.compose.ui.geometry.Offset(centerX - radius * 0.55f, centerY - radius * 0.2f),
            size = androidx.compose.ui.geometry.Size(radius * 0.55f, radius * 0.45f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f, 14f)
        )
        // Asparagus & Veggies green tint
        drawCircle(
            color = Color(0xFF2A9D8F),
            radius = radius * 0.35f,
            center = androidx.compose.ui.geometry.Offset(centerX + radius * 0.25f, centerY - radius * 0.1f)
        )
        // Quinoa / Grains warm base
        drawCircle(
            color = Color(0xFFE9C46A),
            radius = radius * 0.3f,
            center = androidx.compose.ui.geometry.Offset(centerX, centerY + radius * 0.35f)
        )
    }
}

@Composable
fun CornerBracketMarks(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = 3.dp.toPx()
        val len = 20.dp.toPx()

        // Top-left corner
        drawLine(color, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(len, 0f), strokeWidth = stroke)
        drawLine(color, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(0f, len), strokeWidth = stroke)

        // Top-right corner
        drawLine(color, androidx.compose.ui.geometry.Offset(size.width, 0f), androidx.compose.ui.geometry.Offset(size.width - len, 0f), strokeWidth = stroke)
        drawLine(color, androidx.compose.ui.geometry.Offset(size.width, 0f), androidx.compose.ui.geometry.Offset(size.width, len), strokeWidth = stroke)

        // Bottom-left corner
        drawLine(color, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(len, size.height), strokeWidth = stroke)
        drawLine(color, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(0f, size.height - len), strokeWidth = stroke)

        // Bottom-right corner
        drawLine(color, androidx.compose.ui.geometry.Offset(size.width, size.height), androidx.compose.ui.geometry.Offset(size.width - len, size.height), strokeWidth = stroke)
        drawLine(color, androidx.compose.ui.geometry.Offset(size.width, size.height), androidx.compose.ui.geometry.Offset(size.width, size.height - len), strokeWidth = stroke)
    }
}

@Composable
fun ManualMealEntryDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, calories: Int, protein: Double, carbs: Double, fats: Double, fiber: Double?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fats by remember { mutableStateOf("") }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = PureWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Log Meal Manually",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Meal Name") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BioOrange,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextDark,
                        unfocusedTextColor = TextDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Calories (kcal)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BioOrange,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextDark,
                        unfocusedTextColor = TextDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Protein (g)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Next
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Right) }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MacroProteinColor,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Carbs (g)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Next
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Right) }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MacroCarbsColor,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fats,
                        onValueChange = { fats = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Fats (g)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MacroFatsColor,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        val cals = calories.toIntOrNull() ?: 0
                        val p = protein.toDoubleOrNull() ?: 0.0
                        val c = carbs.toDoubleOrNull() ?: 0.0
                        val f = fats.toDoubleOrNull() ?: 0.0
                        if (name.isNotBlank() && cals > 0) {
                            onSave(name, cals, p, c, f, null)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BioOrange),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Save Meal", fontWeight = FontWeight.Bold, color = PureWhite)
                }
            }
        }
    }
}
