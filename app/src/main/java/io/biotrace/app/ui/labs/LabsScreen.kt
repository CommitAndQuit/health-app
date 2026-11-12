package io.biotrace.app.ui.labs

import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import io.biotrace.app.data.local.entity.LabBiomarkerEntity
import io.biotrace.app.domain.clinical.ClinicalDateParser
import io.biotrace.app.ui.common.BioTraceTopBar
import io.biotrace.app.ui.theme.AlertRed
import io.biotrace.app.ui.theme.AlertRedLight
import io.biotrace.app.ui.theme.AlertRedText
import io.biotrace.app.ui.theme.BioOrange
import io.biotrace.app.ui.theme.BioOrangeLight
import io.biotrace.app.ui.theme.BioOrangePrimary
import io.biotrace.app.ui.theme.BioOrangeSoftTint
import io.biotrace.app.ui.theme.BioTraceTokens
import io.biotrace.app.ui.theme.CardBorder
import io.biotrace.app.ui.theme.CardSurface
import io.biotrace.app.ui.theme.ClinicalEmerald
import io.biotrace.app.ui.theme.ClinicalEmeraldDark
import io.biotrace.app.ui.theme.CoralAlertPrimary
import io.biotrace.app.ui.theme.CoralAlertSoftTint
import io.biotrace.app.ui.theme.EmeraldPrimary
import io.biotrace.app.ui.theme.EmeraldSoftTint
import io.biotrace.app.ui.theme.InnerWellSurface
import io.biotrace.app.ui.theme.LightBackground
import io.biotrace.app.ui.theme.MintBadgeBg
import io.biotrace.app.ui.theme.MintBadgeText
import io.biotrace.app.ui.theme.ProfileAvatarBg
import io.biotrace.app.ui.theme.ProfileAvatarIcon
import io.biotrace.app.ui.theme.PureWhite
import io.biotrace.app.ui.theme.TextDark
import io.biotrace.app.ui.theme.TextMuted
import io.biotrace.app.ui.theme.TextPrimary
import io.biotrace.app.ui.theme.TextSecondary
import io.biotrace.app.ui.theme.TextWhite
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun LabsScreen(
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToSettings: () -> Unit = {},
    viewModel: LabsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val testPdf = remember { java.io.File(context.filesDir, "report.pdf") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.processAndScanDocument(it)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let { viewModel.processAndScanBitmap(it) }
    }

    if (uiState.isManualEntryOpen) {
        ManualBiomarkerEntryDialog(
            onDismiss = { viewModel.openManualEntry(false) },
            onSave = { name, value, unit, min, max ->
                viewModel.addManualBiomarker(name, value, unit, min, max)
            }
        )
    }

    if (uiState.isReviewDialogOpen && uiState.pendingReviewBiomarkers.isNotEmpty()) {
        OcrPanelReviewDialog(
            uiState = uiState,
            onOperatorSelected = { op -> viewModel.updateReviewOperator(op) },
            onValueChanged = { v -> viewModel.updateReviewValue(v) },
            onDismiss = { viewModel.closeReviewDialog() },
            onConfirm = { reviewedList ->
                viewModel.confirmAndSaveReviewedMarkers(reviewedList)
            }
        )
    }

    Scaffold(
        containerColor = LightBackground,
        topBar = {
            BioTraceTopBar(
                onBackClick = onNavigateBack,
                onProfileClick = onNavigateToSettings
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Action Buttons: Snap Report (Camera), Upload Document/PDF, and Manual Add
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { cameraLauncher.launch(null) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BioOrange,
                                contentColor = PureWhite
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Snap Report (Camera)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { launcher.launch(arrayOf("application/pdf", "image/*")) },
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                tint = BioOrange,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Upload Document / PDF",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.openManualEntry(true) },
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = BioOrange,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add Test Manually",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Quick-scan card if clinical test PDF is provisioned in app storage
            if (testPdf.exists() && !uiState.isScanning) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFFF7F3),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD8CC)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.processAndScanFile(testPdf) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFFF1ED)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = BioOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ready to Scan: ${testPdf.name}",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = if (testPdf.exists()) String.format(Locale.US, "Clinical PDF detected (%.1f MB) • Tap to Scan with AI", testPdf.length() / (1024.0 * 1024.0)) else "Clinical PDF detected • Tap to Scan with AI",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BioOrange
                            ) {
                                Text(
                                    text = "Scan",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Scanning progress indicator
            if (uiState.isScanning) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BioOrange),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = BioOrange,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = uiState.scanningFileName ?: "Processing Report Document...",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = uiState.scanMessage ?: "Extracting biomarkers with Vision LLM...",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = TextSecondary
                                    )
                                }
                                if (uiState.scanningPageCount > 1) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = BioOrangeLight,
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        Text(
                                            text = "${uiState.scanningPageCount} Pages",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BioOrange,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            uiState.scanningPreviewBitmap?.let { bmp ->
                                Spacer(modifier = Modifier.height(12.dp))
                                androidx.compose.foundation.Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Report Document Preview",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }

            // Out-of-Range Anomalies Banner
            if (uiState.anomalies.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = AlertRedLight),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = AlertRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${uiState.anomalies.size} Markers Outside Reference Range",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AlertRedText
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Flags detected in your recent tests: " + uiState.anomalies.joinToString(", ") { "${it.rawName} (${it.value} ${it.unit})" },
                                fontSize = 12.5.sp,
                                color = Color(0xFF7F1D1D),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CLINICAL BIOMARKERS",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "${uiState.biomarkers.size} Tests Tracked",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MintBadgeBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Science,
                                    contentDescription = null,
                                    tint = ClinicalEmeraldDark,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "110+ Standardized",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MintBadgeText
                                )
                            }
                        }
                    }
                }
            }

            // Report Filter Selector (If multiple reports exist across the year)
            if (uiState.availableReports.size > 1) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "REPORT TIMELINE",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "${uiState.availableReports.size - 1} Scanned Reports",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uiState.availableReports) { report ->
                                val isSelected = report.id == uiState.selectedReportFilterId
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) BioOrange else PureWhite,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) BioOrange else CardBorder
                                    ),
                                    modifier = Modifier.clickable {
                                        viewModel.selectReportFilter(report.id)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = if (isSelected) PureWhite else TextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = report.displayLabel,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) PureWhite else TextDark
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // List of Longitudinal Biomarker Cards
            items(uiState.longitudinalBiomarkers) { biomarkerModel ->
                LongitudinalBiomarkerCard(model = biomarkerModel)
            }
        }
    }
}

@Composable
fun LongitudinalBiomarkerCard(model: LongitudinalBiomarkerCardModel) {
    val biomarker = model.currentReading
    val dateStr = ClinicalDateParser.formatDisplayDate(biomarker.recordedAt)

    val isOptimal = biomarker.flag == "NORMAL"
    val badgeBg = if (isOptimal) MintBadgeBg else AlertRedLight
    val badgeText = if (isOptimal) MintBadgeText else AlertRedText

    var isHistoryExpanded by remember { mutableStateOf(model.history.size > 1) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = biomarker.rawName,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = "$dateStr • ${biomarker.source}",
                        fontSize = 11.5.sp,
                        color = TextMuted
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = biomarker.flag ?: "NORMAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Value and standard range
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${biomarker.value}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isOptimal) TextDark else AlertRed
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = biomarker.unit,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }

                if (biomarker.referenceMin != null || biomarker.referenceMax != null) {
                    Text(
                        text = "Ref: ${biomarker.referenceMin ?: 0.0} – ${biomarker.referenceMax ?: "∞"} ${biomarker.unit}",
                        fontSize = 11.5.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }

            // Visual Range Corridor Bar
            if (biomarker.referenceMin != null && biomarker.referenceMax != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val min = biomarker.referenceMin
                val max = biomarker.referenceMax
                val span = (max - min).coerceAtLeast(1.0)
                val progress = ((biomarker.value - min) / span).coerceIn(0.0, 1.0).toFloat()

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isOptimal) ClinicalEmerald else AlertRed,
                    trackColor = Color(0xFFF1F5F9),
                )
            }

            // Trend Delta Badge (if previous test in the year exists)
            if (model.previousReading != null && model.deltaValue != null) {
                Spacer(modifier = Modifier.height(12.dp))
                val delta = model.deltaValue
                val prevDate = ClinicalDateParser.formatDisplayDate(model.previousReading.recordedAt)

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isHistoryExpanded = !isHistoryExpanded }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when {
                                    delta > 0 -> Icons.AutoMirrored.Filled.TrendingUp
                                    delta < 0 -> Icons.AutoMirrored.Filled.TrendingDown
                                    else -> Icons.AutoMirrored.Filled.TrendingFlat
                                },
                                contentDescription = null,
                                tint = when {
                                    delta > 0 -> BioOrange
                                    delta < 0 -> Color(0xFF2563EB)
                                    else -> TextMuted
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val sign = if (delta > 0) "+" else ""
                            Text(
                                text = "$sign$delta ${biomarker.unit} vs prior ($prevDate)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextDark
                            )
                        }

                        if (model.history.size > 1) {
                            Text(
                                text = if (isHistoryExpanded) "Hide History" else "${model.history.size} tests",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BioOrange,
                                modifier = Modifier.clickable { isHistoryExpanded = !isHistoryExpanded }
                            )
                        }
                    }
                }
            }

            // Biomarker Longitudinal Trend Chart
            if (model.history.size > 1) {
                Spacer(modifier = Modifier.height(12.dp))
                BiomarkerTrendChart(
                    history = model.history,
                    refMin = biomarker.referenceMin,
                    refMax = biomarker.referenceMax,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            }

            // Expandable Multi-Report History Panel
            if (isHistoryExpanded && model.history.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFAFAFA),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "LONGITUDINAL HISTORY",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        model.history.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${item.displayDate} • ${item.source}",
                                    fontSize = 11.5.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${item.value} ${item.unit} (${item.flag ?: "NORMAL"})",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.flag == "NORMAL") ClinicalEmeraldDark else AlertRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private val EmeraldSoftTint = Color(0x1810B981)

@Composable
fun BiomarkerTrendChart(
    history: List<BiomarkerReadingHistory>,
    refMin: Double?,
    refMax: Double?,
    modifier: Modifier = Modifier
) {
    if (history.size <= 1) return

    val chronological = remember(history) { history.sortedBy { it.date } }
    val density = LocalDensity.current

    val valueTextSize = with(density) { 10.5.sp.toPx() }
    val dateTextSize = with(density) { 9.sp.toPx() }

    val textPaintValue = remember(density) {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = valueTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.rgb(0x0F, 0x17, 0x2A)
        }
    }
    val textPaintDate = remember(density) {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = dateTextSize
            typeface = Typeface.DEFAULT
            color = android.graphics.Color.rgb(0x64, 0x74, 0x8B)
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFAFBFD),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E9F0)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartWidth = size.width
                val chartHeight = size.height
                if (chartWidth <= 0 || chartHeight <= 0) return@Canvas

                val paddingLeft = 32.dp.toPx()
                val paddingRight = 32.dp.toPx()
                val paddingTop = 22.dp.toPx()
                val paddingBottom = 20.dp.toPx()
                val plotWidth = chartWidth - paddingLeft - paddingRight
                val plotHeight = chartHeight - paddingTop - paddingBottom
                if (plotWidth <= 0 || plotHeight <= 0) return@Canvas

                val values = chronological.map { it.value }
                val allY = values + listOfNotNull(refMin, refMax)
                val minY = allY.minOrNull() ?: 0.0
                val maxY = allY.maxOrNull() ?: 100.0
                val rawSpread = maxY - minY
                val spread = if (rawSpread <= 0.001) 10.0 else rawSpread

                val yMin = minY - spread * 0.18
                val yMax = maxY + spread * 0.18
                val yRange = (yMax - yMin).coerceAtLeast(0.001)

                fun getY(v: Double): Float {
                    val normalized = ((yMax - v) / yRange).toFloat()
                    return (paddingTop + normalized * plotHeight).coerceIn(paddingTop, paddingTop + plotHeight)
                }

                val n = chronological.size
                val stepX = plotWidth / (n - 1).coerceAtLeast(1)
                fun getX(index: Int): Float = paddingLeft + index * stepX

                // 1. Shaded background band for healthy reference range
                if (refMin != null || refMax != null) {
                    val rMin = refMin ?: yMin
                    val rMax = refMax ?: yMax
                    val topY = getY(rMax)
                    val bottomY = getY(rMin)
                    val bandTop = minOf(topY, bottomY)
                    val bandBottom = maxOf(topY, bottomY)
                    val bandHeight = (bandBottom - bandTop).coerceAtLeast(0f)

                    drawRect(
                        color = EmeraldSoftTint,
                        topLeft = Offset(paddingLeft - 8.dp.toPx(), bandTop),
                        size = Size(plotWidth + 16.dp.toPx(), bandHeight)
                    )

                    // Reference bounds subtle dash line
                    if (refMax != null) {
                        drawLine(
                            color = ClinicalEmerald.copy(alpha = 0.35f),
                            start = Offset(paddingLeft - 8.dp.toPx(), bandTop),
                            end = Offset(paddingLeft + plotWidth + 8.dp.toPx(), bandTop),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                    }
                    if (refMin != null) {
                        drawLine(
                            color = ClinicalEmerald.copy(alpha = 0.35f),
                            start = Offset(paddingLeft - 8.dp.toPx(), bandBottom),
                            end = Offset(paddingLeft + plotWidth + 8.dp.toPx(), bandBottom),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                    }
                }

                // 2. Connected line curve connecting historical readings chronologically (left to right)
                val points = chronological.indices.map { i ->
                    Offset(getX(i), getY(chronological[i].value))
                }

                val linePath = Path()
                val fillPath = Path()

                linePath.moveTo(points.first().x, points.first().y)
                fillPath.moveTo(points.first().x, paddingTop + plotHeight)
                fillPath.lineTo(points.first().x, points.first().y)

                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val cx1 = (p0.x + p1.x) / 2f
                    val cy1 = p0.y
                    val cx2 = (p0.x + p1.x) / 2f
                    val cy2 = p1.y
                    linePath.cubicTo(cx1, cy1, cx2, cy2, p1.x, p1.y)
                    fillPath.cubicTo(cx1, cy1, cx2, cy2, p1.x, p1.y)
                }

                fillPath.lineTo(points.last().x, paddingTop + plotHeight)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(BioOrange.copy(alpha = 0.16f), BioOrange.copy(alpha = 0.01f)),
                        startY = paddingTop,
                        endY = paddingTop + plotHeight
                    )
                )

                drawPath(
                    path = linePath,
                    color = BioOrange,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // 3. Data point dots, value labels, and short date labels
                points.forEachIndexed { i, pt ->
                    val item = chronological[i]
                    val isNormal = item.flag == "NORMAL" || item.flag == null
                    val dotColor = if (isNormal) ClinicalEmerald else AlertRed

                    // Dot outer white ring
                    drawCircle(
                        color = PureWhite,
                        radius = 5.dp.toPx(),
                        center = pt
                    )
                    // Data point dot
                    drawCircle(
                        color = dotColor,
                        radius = 3.8.dp.toPx(),
                        center = pt
                    )

                    // Value label along the points
                    val valueStr = if (item.value % 1.0 == 0.0) item.value.toInt().toString() else item.value.toString()
                    val valueY = (pt.y - 7.dp.toPx()).coerceAtLeast(13.dp.toPx())
                    drawContext.canvas.nativeCanvas.drawText(
                        valueStr,
                        pt.x,
                        valueY,
                        textPaintValue
                    )

                    // Short date label along the points
                    val shortDate = try {
                        val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.US).withZone(ZoneId.systemDefault())
                        formatter.format(item.date)
                    } catch (e: Exception) {
                        item.displayDate.substringBefore(",")
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        shortDate,
                        pt.x,
                        chartHeight - 4.dp.toPx(),
                        textPaintDate
                    )
                }
            }
        }
    }
}

@Composable
fun BiomarkerCard(biomarker: LabBiomarkerEntity) {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US).withZone(ZoneId.systemDefault())
    val dateStr = formatter.format(biomarker.recordedAt)

    val isOptimal = biomarker.flag == "NORMAL"
    val badgeBg = if (isOptimal) MintBadgeBg else AlertRedLight
    val badgeText = if (isOptimal) MintBadgeText else AlertRedText

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = biomarker.rawName,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = "$dateStr • ${biomarker.source}",
                        fontSize = 11.5.sp,
                        color = TextMuted
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = biomarker.flag ?: "NORMAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Value and standard range
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${biomarker.value}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isOptimal) TextDark else AlertRed
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = biomarker.unit,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }

                if (biomarker.referenceMin != null || biomarker.referenceMax != null) {
                    Text(
                        text = "Ref: ${biomarker.referenceMin ?: 0.0} – ${biomarker.referenceMax ?: "∞"} ${biomarker.unit}",
                        fontSize = 11.5.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }

            // Visual Range Corridor Bar
            if (biomarker.referenceMin != null && biomarker.referenceMax != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val min = biomarker.referenceMin
                val max = biomarker.referenceMax
                val span = (max - min).coerceAtLeast(1.0)
                val progress = ((biomarker.value - min) / span).coerceIn(0.0, 1.0).toFloat()

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isOptimal) ClinicalEmerald else AlertRed,
                    trackColor = Color(0xFFF1F5F9),
                )
            }
        }
    }
}

@Composable
fun OcrPanelReviewDialog(
    uiState: LabsUiState,
    onOperatorSelected: (String) -> Unit = {},
    onValueChanged: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onConfirm: (List<LabBiomarkerEntity>) -> Unit
) {
    var editingItem by remember { mutableStateOf<LabBiomarkerEntity?>(null) }
    val editableList = remember(uiState.pendingReviewBiomarkers) {
        mutableStateListOf<LabBiomarkerEntity>().apply { addAll(uiState.pendingReviewBiomarkers) }
    }

    editingItem?.let { target ->
        EditBiomarkerDialog(
            biomarker = target,
            onDismiss = { editingItem = null },
            onSave = { updated ->
                val idx = editableList.indexOfFirst { it.id == target.id }
                if (idx != -1) {
                    editableList[idx] = updated
                }
                editingItem = null
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardSurface,
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header: Document/Scan icon, Panel name, formatted value with operator, "OCR Confirmed" green pill badge, user avatar
                val panelName = uiState.reviewPanelName.ifBlank {
                    val first = uiState.pendingReviewBiomarkers.firstOrNull()
                    if (first?.biomarkerName.equals("HBA1C", ignoreCase = true)) "HbA1c Panel"
                    else if (first != null) "${first.rawName} Panel"
                    else "HbA1c Panel"
                }

                val op = uiState.reviewOperator.trim()
                val valStr = uiState.reviewValue.trim().ifBlank {
                    uiState.pendingReviewBiomarkers.firstOrNull()?.value?.let {
                        if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
                    } ?: "5.7"
                }
                val unitStr = uiState.reviewUnit.trim().ifBlank {
                    uiState.pendingReviewBiomarkers.firstOrNull()?.unit ?: "%"
                }
                val formattedHeaderValue = if (op.isNotBlank() && op != "=") {
                    "$op $valStr$unitStr"
                } else {
                    "$valStr $unitStr".trim()
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BioOrangeSoftTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "Scan Document",
                                tint = BioOrangePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = panelName,
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier
                                        .clip(BioTraceTokens.PillShape)
                                        .background(EmeraldSoftTint)
                                        .border(1.dp, EmeraldPrimary.copy(alpha = 0.35f), BioTraceTokens.PillShape)
                                        .padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "OCR Confirmed",
                                        color = EmeraldPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = formattedHeaderValue,
                                color = TextSecondary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(ProfileAvatarBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "User Avatar",
                                tint = ProfileAvatarIcon,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // "OPERATOR & VALUE" Card
                Surface(
                    shape = BioTraceTokens.BentoCardShape,
                    color = InnerWellSurface,
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "OPERATOR & VALUE",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var operatorMenuExpanded by remember { mutableStateOf(false) }
                            val operators = listOf("<", ">", "=", "≤", "≥")
                            val currentOp = if (uiState.reviewOperator.isNotBlank()) uiState.reviewOperator else "="

                            Box {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = CardSurface,
                                    border = BorderStroke(1.dp, CardBorder),
                                    modifier = Modifier
                                        .height(52.dp)
                                        .clickable { operatorMenuExpanded = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = currentOp,
                                            color = BioOrangePrimary,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Select Operator",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = operatorMenuExpanded,
                                    onDismissRequest = { operatorMenuExpanded = false },
                                    modifier = Modifier.background(CardSurface)
                                ) {
                                    operators.forEach { opItem ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = opItem,
                                                    color = if (opItem == currentOp) BioOrangePrimary else TextPrimary,
                                                    fontSize = 16.sp,
                                                    fontWeight = if (opItem == currentOp) FontWeight.Bold else FontWeight.Medium
                                                )
                                            },
                                            onClick = {
                                                onOperatorSelected(opItem)
                                                operatorMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = uiState.reviewValue,
                                onValueChange = { onValueChanged(it) },
                                singleLine = true,
                                placeholder = { Text("5.7", color = TextMuted) },
                                trailingIcon = {
                                    Text(
                                        text = unitStr,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = CardSurface,
                                    unfocusedContainerColor = CardSurface,
                                    cursorColor = BioOrangePrimary,
                                    focusedBorderColor = BioOrangePrimary,
                                    unfocusedBorderColor = CardBorder
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            )
                        }

                        // Operator Quick Selector Row
                        val operators = listOf("<", ">", "=", "≤", "≥")
                        val currentOp = if (uiState.reviewOperator.isNotBlank()) uiState.reviewOperator else "="
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            operators.forEach { opItem ->
                                val isSelected = opItem == currentOp
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) BioOrangePrimary else CardSurface)
                                        .border(1.dp, if (isSelected) BioOrangePrimary else CardBorder, RoundedCornerShape(10.dp))
                                        .clickable { onOperatorSelected(opItem) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = opItem,
                                        color = if (isSelected) TextWhite else TextSecondary,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Dynamic Unit Harmonization Card
                        Surface(
                            shape = BioTraceTokens.InnerWellSmallShape,
                            color = CardSurface,
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(BioOrangeSoftTint),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "Unit Harmonization",
                                        tint = BioOrangePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "Unit Harmonization",
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = uiState.unitHarmonizationText ?: "5.7 % ↔ 39 mmol/mol",
                                        color = TextPrimary,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // "LINKED PANEL RESULTS" Card
                val linkedList = if (uiState.linkedPanelResults.isNotEmpty()) {
                    uiState.linkedPanelResults
                } else if (editableList.isNotEmpty()) {
                    editableList
                } else {
                    uiState.pendingReviewBiomarkers
                }

                Surface(
                    shape = BioTraceTokens.BentoCardShape,
                    color = InnerWellSurface,
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LINKED PANEL RESULTS",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${linkedList.size} Tests Checked",
                                color = EmeraldPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            linkedList.forEach { item ->
                                val isNormal = item.flag == "NORMAL"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CardSurface)
                                        .border(1.dp, CardBorder.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                        .clickable { editingItem = item }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (isNormal) EmeraldSoftTint else CoralAlertSoftTint),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isNormal) Icons.Default.Check else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (isNormal) EmeraldPrimary else CoralAlertPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = item.rawName,
                                                color = TextPrimary,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isNormal) EmeraldSoftTint else CoralAlertSoftTint
                                            ) {
                                                Text(
                                                    text = item.flag ?: (if (isNormal) "NORMAL" else "LOW"),
                                                    color = if (isNormal) EmeraldPrimary else CoralAlertPrimary,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        val refMinStr = item.referenceMin?.let {
                                            if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
                                        } ?: "0"
                                        val refMaxStr = item.referenceMax?.let {
                                            if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
                                        } ?: "∞"

                                        Text(
                                            text = "Ref: $refMinStr – $refMaxStr ${item.unit}".trim(),
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    val itemValStr = if (item.value % 1.0 == 0.0) item.value.toInt().toString() else item.value.toString()
                                    Text(
                                        text = "$itemValStr ${item.unit}",
                                        color = if (isNormal) TextPrimary else CoralAlertPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldSoftTint)
                                            .border(1.dp, EmeraldPrimary.copy(alpha = 0.5f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Verified",
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Save button: "Save to Health Vault"
                Button(
                    onClick = {
                        val toSave = editableList.toList().ifEmpty { uiState.pendingReviewBiomarkers }
                        onConfirm(toSave)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BioOrangePrimary,
                        contentColor = TextWhite
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = TextWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save to Health Vault",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextWhite
                    )
                }
            }
        }
    }
}

@Composable
fun BiomarkerReviewDialog(
    uiState: LabsUiState,
    onOperatorSelected: (String) -> Unit = {},
    onValueChanged: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onConfirm: (List<LabBiomarkerEntity>) -> Unit = {}
) {
    OcrPanelReviewDialog(
        uiState = uiState,
        onOperatorSelected = onOperatorSelected,
        onValueChanged = onValueChanged,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Composable
fun BiomarkerReviewDialog(
    biomarkers: List<LabBiomarkerEntity>,
    reportMetadata: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (List<LabBiomarkerEntity>) -> Unit
) {
    val primary = biomarkers.firstOrNull()
    val rawVal = primary?.let { if (it.value % 1.0 == 0.0) it.value.toInt().toString() else it.value.toString() } ?: "5.7"
    val unit = primary?.unit ?: "%"
    val panel = if (primary?.biomarkerName.equals("HBA1C", ignoreCase = true)) "HbA1c Panel"
    else if (primary != null) "${primary.rawName} Panel"
    else "HbA1c Panel"

    var currentOp by remember { mutableStateOf("<") }
    var currentVal by remember { mutableStateOf(rawVal) }

    val syntheticUiState = LabsUiState(
        isReviewDialogOpen = true,
        pendingReviewBiomarkers = biomarkers,
        scannedReportMetadata = reportMetadata,
        reviewPanelName = panel,
        reviewOperator = currentOp,
        reviewValue = currentVal,
        reviewUnit = unit,
        unitHarmonizationText = "5.7 % ↔ 39 mmol/mol",
        linkedPanelResults = if (biomarkers.size > 1) biomarkers.drop(1) else biomarkers
    )

    OcrPanelReviewDialog(
        uiState = syntheticUiState,
        onOperatorSelected = { currentOp = it },
        onValueChanged = { currentVal = it },
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Composable
fun EditBiomarkerDialog(
    biomarker: LabBiomarkerEntity,
    onDismiss: () -> Unit,
    onSave: (LabBiomarkerEntity) -> Unit
) {
    var name by remember { mutableStateOf(biomarker.rawName) }
    var valueText by remember {
        mutableStateOf(if (biomarker.value % 1.0 == 0.0) biomarker.value.toInt().toString() else biomarker.value.toString())
    }
    var unitText by remember { mutableStateOf(biomarker.unit) }
    var minText by remember { mutableStateOf(biomarker.referenceMin?.toString() ?: "") }
    var maxText by remember { mutableStateOf(biomarker.referenceMax?.toString() ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardSurface,
            border = BorderStroke(1.dp, CardBorder),
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
                        text = "Edit Extracted Biomarker",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Biomarker Name", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = InnerWellSurface,
                        unfocusedContainerColor = InnerWellSurface,
                        cursorColor = BioOrangePrimary,
                        focusedBorderColor = BioOrangePrimary,
                        unfocusedBorderColor = CardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = valueText,
                        onValueChange = { valueText = it },
                        label = { Text("Value", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = InnerWellSurface,
                            unfocusedContainerColor = InnerWellSurface,
                            cursorColor = BioOrangePrimary,
                            focusedBorderColor = BioOrangePrimary,
                            unfocusedBorderColor = CardBorder
                        ),
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = unitText,
                        onValueChange = { unitText = it },
                        label = { Text("Unit", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = InnerWellSurface,
                            unfocusedContainerColor = InnerWellSurface,
                            cursorColor = BioOrangePrimary,
                            focusedBorderColor = BioOrangePrimary,
                            unfocusedBorderColor = CardBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = minText,
                        onValueChange = { minText = it },
                        label = { Text("Ref Min", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = InnerWellSurface,
                            unfocusedContainerColor = InnerWellSurface,
                            cursorColor = BioOrangePrimary,
                            focusedBorderColor = BioOrangePrimary,
                            unfocusedBorderColor = CardBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxText,
                        onValueChange = { maxText = it },
                        label = { Text("Ref Max", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = InnerWellSurface,
                            unfocusedContainerColor = InnerWellSurface,
                            cursorColor = BioOrangePrimary,
                            focusedBorderColor = BioOrangePrimary,
                            unfocusedBorderColor = CardBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        val parsedVal = valueText.toDoubleOrNull() ?: biomarker.value
                        val parsedMin = minText.toDoubleOrNull()
                        val parsedMax = maxText.toDoubleOrNull()

                        val flag = when {
                            parsedMin != null && parsedVal < parsedMin -> "LOW"
                            parsedMax != null && parsedVal > parsedMax -> "HIGH"
                            else -> "NORMAL"
                        }

                        val updated = biomarker.copy(
                            rawName = name.trim().ifBlank { biomarker.rawName },
                            value = parsedVal,
                            unit = unitText.trim(),
                            referenceMin = parsedMin,
                            referenceMax = parsedMax,
                            flag = flag
                        )
                        onSave(updated)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BioOrangePrimary,
                        contentColor = TextWhite
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = TextWhite, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply Changes", fontWeight = FontWeight.Bold, color = TextWhite)
                }
            }
        }
    }
}

@Composable
fun ManualBiomarkerEntryDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, value: Double, unit: String, min: Double?, max: Double?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var min by remember { mutableStateOf("") }
    var max by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardSurface,
            border = BorderStroke(1.dp, CardBorder),
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
                        text = "Add Lab Biomarker",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Biomarker Name (e.g. HbA1c, Ferritin)", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = InnerWellSurface,
                        unfocusedContainerColor = InnerWellSurface,
                        cursorColor = BioOrangePrimary,
                        focusedBorderColor = BioOrangePrimary,
                        unfocusedBorderColor = CardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text("Value", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = InnerWellSurface,
                            unfocusedContainerColor = InnerWellSurface,
                            cursorColor = BioOrangePrimary,
                            focusedBorderColor = BioOrangePrimary,
                            unfocusedBorderColor = CardBorder
                        ),
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = InnerWellSurface,
                            unfocusedContainerColor = InnerWellSurface,
                            cursorColor = BioOrangePrimary,
                            focusedBorderColor = BioOrangePrimary,
                            unfocusedBorderColor = CardBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = min,
                        onValueChange = { min = it },
                        label = { Text("Ref Min", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = InnerWellSurface,
                            unfocusedContainerColor = InnerWellSurface,
                            cursorColor = BioOrangePrimary,
                            focusedBorderColor = BioOrangePrimary,
                            unfocusedBorderColor = CardBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = max,
                        onValueChange = { max = it },
                        label = { Text("Ref Max", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = InnerWellSurface,
                            unfocusedContainerColor = InnerWellSurface,
                            cursorColor = BioOrangePrimary,
                            focusedBorderColor = BioOrangePrimary,
                            unfocusedBorderColor = CardBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        val v = value.toDoubleOrNull() ?: 0.0
                        val refMin = min.toDoubleOrNull()
                        val refMax = max.toDoubleOrNull()
                        if (name.isNotBlank() && value.isNotBlank()) {
                            onSave(name, v, unit, refMin, refMax)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BioOrangePrimary,
                        contentColor = TextWhite
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Save Biomarker", fontWeight = FontWeight.Bold, color = TextWhite)
                }
            }
        }
    }
}
