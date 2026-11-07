package io.biotrace.app.ui.labs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import io.biotrace.app.data.local.entity.LabBiomarkerEntity
import io.biotrace.app.domain.clinical.ClinicalDateParser
import io.biotrace.app.ui.common.BioTraceTopBar
import io.biotrace.app.ui.theme.AlertRed
import io.biotrace.app.ui.theme.AlertRedLight
import io.biotrace.app.ui.theme.AlertRedText
import io.biotrace.app.ui.theme.BioOrange
import io.biotrace.app.ui.theme.BioOrangeLight
import io.biotrace.app.ui.theme.CardBorder
import io.biotrace.app.ui.theme.ClinicalEmerald
import io.biotrace.app.ui.theme.ClinicalEmeraldDark
import io.biotrace.app.ui.theme.LightBackground
import io.biotrace.app.ui.theme.MintBadgeBg
import io.biotrace.app.ui.theme.MintBadgeText
import io.biotrace.app.ui.theme.PureWhite
import io.biotrace.app.ui.theme.TextDark
import io.biotrace.app.ui.theme.TextMuted
import io.biotrace.app.ui.theme.TextSecondary
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun LabsScreen(
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

    if (uiState.isManualEntryOpen) {
        ManualBiomarkerEntryDialog(
            onDismiss = { viewModel.openManualEntry(false) },
            onSave = { name, value, unit, min, max ->
                viewModel.addManualBiomarker(name, value, unit, min, max)
            }
        )
    }

    if (uiState.isReviewDialogOpen && uiState.pendingReviewBiomarkers.isNotEmpty()) {
        BiomarkerReviewDialog(
            biomarkers = uiState.pendingReviewBiomarkers,
            reportMetadata = uiState.scannedReportMetadata,
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
            // Action Buttons: Scan Blood Report & Manual Add
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { launcher.launch(arrayOf("application/pdf", "image/jpeg", "image/png", "image/*")) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BioOrange,
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scan Lab Report",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.openManualEntry(true) },
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = BioOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add Test",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
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
fun BiomarkerReviewDialog(
    biomarkers: List<LabBiomarkerEntity>,
    reportMetadata: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (List<LabBiomarkerEntity>) -> Unit
) {
    val editableList = remember { mutableStateListOf<LabBiomarkerEntity>().apply { addAll(biomarkers) } }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = PureWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
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
                            text = "Verify Parsed Report",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        if (!reportMetadata.isNullOrBlank()) {
                            Text(
                                text = reportMetadata,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BioOrange
                            )
                        }
                        Text(
                            text = "${editableList.size} tests extracted • Confirm to save",
                            fontSize = 11.5.sp,
                            color = TextSecondary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(editableList) { item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEF2F6)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.rawName,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextDark
                                    )
                                    Text(
                                        text = "Ref: ${item.referenceMin ?: 0.0} – ${item.referenceMax ?: "∞"} ${item.unit}",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${item.value} ${item.unit}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.flag == "NORMAL") TextDark else AlertRed
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (item.flag == "NORMAL") MintBadgeBg else AlertRedLight
                                    ) {
                                        Text(
                                            text = item.flag ?: "NORMAL",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.flag == "NORMAL") MintBadgeText else AlertRedText,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { editableList.remove(item) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = TextMuted,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onConfirm(editableList.toList()) },
                    colors = ButtonDefaults.buttonColors(containerColor = BioOrange),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save to Health Vault", fontWeight = FontWeight.Bold, color = PureWhite)
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
                        text = "Add Lab Biomarker",
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
                    label = { Text("Biomarker Name (e.g. HbA1c, Ferritin)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BioOrange,
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
                        label = { Text("Value") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BioOrange,
                            unfocusedBorderColor = CardBorder
                        ),
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BioOrange,
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
                        label = { Text("Ref Min") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BioOrange,
                            unfocusedBorderColor = CardBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = max,
                        onValueChange = { max = it },
                        label = { Text("Ref Max") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BioOrange,
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
                    colors = ButtonDefaults.buttonColors(containerColor = BioOrange),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Save Biomarker", fontWeight = FontWeight.Bold, color = PureWhite)
                }
            }
        }
    }
}
