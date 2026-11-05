package io.biotrace.app.ui.vitals

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.biotrace.app.domain.model.ChartPoint
import io.biotrace.app.domain.model.MetricTimeRange
import io.biotrace.app.domain.model.RangeBarItem
import io.biotrace.app.domain.model.SleepStageBarItem
import io.biotrace.app.domain.model.SleepStagesBreakdown
import io.biotrace.app.domain.model.StatItem
import io.biotrace.app.domain.model.StatusBadge
import io.biotrace.app.domain.model.VitalDetailUiState
import io.biotrace.app.domain.model.VitalHistoryItem
import io.biotrace.app.domain.model.VitalMetricType
import io.biotrace.app.ui.theme.BioOrange
import io.biotrace.app.ui.theme.BioOrangePrimary
import io.biotrace.app.ui.theme.BioTraceTokens
import io.biotrace.app.ui.theme.CardBorder
import io.biotrace.app.ui.theme.ClinicalEmerald
import io.biotrace.app.ui.theme.CoralAlertPrimary
import io.biotrace.app.ui.theme.InnerWellSurface
import io.biotrace.app.ui.theme.LightBackground
import io.biotrace.app.ui.theme.PureWhite
import io.biotrace.app.ui.theme.SoftInputBg
import io.biotrace.app.ui.theme.SolarAmberPrimary
import io.biotrace.app.ui.theme.TextDark
import io.biotrace.app.ui.theme.TextMuted
import io.biotrace.app.ui.theme.TextSecondary
import java.time.Instant
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: VitalDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        MetricInfoDialog(
            metricType = uiState.metricType,
            onDismiss = { showInfoDialog = false }
        )
    }

    Scaffold(
        containerColor = LightBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.metricType.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDark
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Metric Info",
                            tint = TextSecondary
                        )
                    }

                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = TextDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBackground)
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading && uiState.historyRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BioOrange)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Time Range Selector
                item {
                    TimeRangeSegmentedControl(
                        selectedRange = uiState.selectedRange,
                        onRangeSelected = { viewModel.selectRange(it) }
                    )
                }

                // 2. Historical Anchor Notice
                if (uiState.anchorNotice != null) {
                    item {
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = uiState.anchorNotice!!,
                                fontSize = 12.sp,
                                color = Color(0xFF1E40AF),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                // High Contrast Alert Banner (if anomaly detected)
                if (uiState.alertBanner != null) {
                    item {
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = BioTraceTokens.BentoCardShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = CoralAlertPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = uiState.alertBanner!!.headline,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF991B1B)
                                    )
                                    Text(
                                        text = uiState.alertBanner!!.subtitle,
                                        fontSize = 11.sp,
                                        color = Color(0xFFB91C1C)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Hero Summary Card
                item {
                    HeroSummaryCard(uiState = uiState)
                }

                // 4. Interactive Compose Canvas Chart
                item {
                    InteractiveChartCard(uiState = uiState)
                }

                // 5. Sleep Stages Summary (Specialized for SLEEP)
                if (uiState.metricType == VitalMetricType.SLEEP && uiState.sleepBreakdown != null) {
                    item {
                        SleepBreakdownCard(breakdown = uiState.sleepBreakdown!!)
                    }
                }

                // 7. Summary Statistics Row
                if (uiState.summaryStats.isNotEmpty()) {
                    item {
                        SummaryStatsStrip(stats = uiState.summaryStats)
                    }
                }

                // 8. History Records Section Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Historical Records",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "${uiState.historyRecords.size} entries",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                // 9. Detailed Records List
                items(uiState.historyRecords, key = { it.id }) { item ->
                    VitalHistoryRecordRow(item = item)
                }

                if (uiState.historyRecords.isEmpty()) {
                    item {
                        Surface(
                            color = PureWhite,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No records logged for this time range.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(24.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimeRangeSegmentedControl(
    selectedRange: MetricTimeRange,
    onRangeSelected: (MetricTimeRange) -> Unit
) {
    val ranges = MetricTimeRange.entries

    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        ranges.forEachIndexed { index, range ->
            SegmentedButton(
                selected = range == selectedRange,
                onClick = { onRangeSelected(range) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = ranges.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = BioOrange,
                    activeContentColor = PureWhite,
                    inactiveContainerColor = SoftInputBg,
                    inactiveContentColor = TextSecondary
                )
            ) {
                Text(
                    text = range.label,
                    fontSize = 12.sp,
                    fontWeight = if (range == selectedRange) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun HeroSummaryCard(uiState: VitalDetailUiState) {
    Surface(
        color = PureWhite,
        shape = BioTraceTokens.HeroCardShape,
        border = BioTraceTokens.CardHairlineBorder,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 4.dp gradient accent bar at the top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(BioOrangePrimary, SolarAmberPrimary)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = uiState.headlineDateLabel.ifEmpty { "Metric Summary" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )

                    if (uiState.statusBadge != null) {
                        Surface(
                            color = uiState.statusBadge.backgroundColor,
                            shape = BioTraceTokens.PillShape
                        ) {
                            Text(
                                text = uiState.statusBadge.text,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = uiState.statusBadge.textColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = uiState.headlineValue,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark,
                        lineHeight = 40.sp
                    )
                    if (uiState.headlineUnit.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = uiState.headlineUnit,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = uiState.subtitle,
                    fontSize = 12.sp,
                    color = TextMuted
                )

                // Clinical Callout Insight Well
                if (uiState.calloutInsight.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = InnerWellSurface,
                        shape = BioTraceTokens.InnerWellSmallShape,
                        border = BioTraceTokens.WellHairlineBorder,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = BioOrangePrimary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 1.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.calloutInsight,
                                fontSize = 12.sp,
                                color = TextDark,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveChartCard(uiState: VitalDetailUiState) {
    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "${uiState.metricType.title} Trend (${uiState.selectedRange.label})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                text = "Touch and drag to inspect readings",
                fontSize = 11.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                when {
                    uiState.metricType == VitalMetricType.SLEEP && uiState.sleepBars.isNotEmpty() -> {
                        SleepStackedBarCanvas(bars = uiState.sleepBars)
                    }
                    uiState.rangeBars.isNotEmpty() -> {
                        RangeBarsCanvas(
                            bars = uiState.rangeBars,
                            referenceLine = uiState.referenceLineValue,
                            accentColor = uiState.metricType.accentColor
                        )
                    }
                    uiState.points.isNotEmpty() -> {
                        InteractiveTimeSeriesCanvas(
                            points = uiState.points,
                            referenceLine = uiState.referenceLineValue,
                            accentColor = uiState.metricType.accentColor
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No time-series data available for this range.",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveTimeSeriesCanvas(
    points: List<ChartPoint>,
    referenceLine: Float?,
    accentColor: Color
) {
    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }

    val values = points.map { it.value }
    val minVal = (values.minOrNull() ?: 0f).coerceAtLeast(0f)
    val maxVal = (values.maxOrNull() ?: 100f).coerceAtLeast(minVal + 10f)

    Column(modifier = Modifier.fillMaxSize()) {
        // Scrubbing tooltip readout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            selectedIndex?.let { idx ->
                val p = points.getOrNull(idx)
                if (p != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = p.color ?: accentColor,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${p.value.roundToInt()} ${p.note ?: ""}",
                                color = PureWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = p.xLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextDark
                        )
                    }
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(points) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            val step = size.width / points.size.coerceAtLeast(1)
                            selectedIndex = (offset.x / step).toInt().coerceIn(0, points.size - 1)
                        },
                        onHorizontalDrag = { change, _ ->
                            val step = size.width / points.size.coerceAtLeast(1)
                            selectedIndex = (change.position.x / step).toInt().coerceIn(0, points.size - 1)
                        },
                        onDragEnd = { selectedIndex = null },
                        onDragCancel = { selectedIndex = null }
                    )
                }
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val step = size.width / points.size.coerceAtLeast(1)
                        selectedIndex = (offset.x / step).toInt().coerceIn(0, points.size - 1)
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val paddingBottom = 16f
            val chartH = h - paddingBottom
            val range = (maxVal - minVal).coerceAtLeast(1f)

            // Draw horizontal reference grid lines
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = chartH - (i.toFloat() / gridLines) * chartH
                drawLine(
                    color = Color(0xFFF1F5F9),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
            }

            // Draw optional reference line
            referenceLine?.let { ref ->
                if (ref in minVal..maxVal) {
                    val refY = chartH - ((ref - minVal) / range) * chartH
                    drawLine(
                        color = Color(0xFF94A3B8),
                        start = Offset(0f, refY),
                        end = Offset(w, refY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                }
            }

            if (points.size < 2) {
                // Single point
                if (points.isNotEmpty()) {
                    val p = points.first()
                    val y = chartH - ((p.value - minVal) / range) * chartH
                    drawCircle(color = p.color ?: accentColor, radius = 6.dp.toPx(), center = Offset(w / 2f, y))
                }
                return@Canvas
            }

            val stepX = w / (points.size - 1)
            val linePath = Path()
            val fillPath = Path()

            fillPath.moveTo(0f, chartH)

            points.forEachIndexed { i, p ->
                val x = i * stepX
                val y = chartH - ((p.value - minVal) / range) * chartH

                if (i == 0) {
                    linePath.moveTo(x, y)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }

            fillPath.lineTo(w, chartH)
            fillPath.close()

            // Draw shaded gradient underneath
            drawPath(
                path = fillPath,
                color = accentColor.copy(alpha = 0.08f)
            )

            // Draw stroke line
            drawPath(
                path = linePath,
                color = accentColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw node points
            points.forEachIndexed { i, p ->
                val x = i * stepX
                val y = chartH - ((p.value - minVal) / range) * chartH
                val ptColor = p.color ?: accentColor

                drawCircle(
                    color = PureWhite,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = ptColor,
                    radius = 3.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            // Draw scrubber cursor
            selectedIndex?.let { idx ->
                val x = idx * stepX
                val y = chartH - ((points[idx].value - minVal) / range) * chartH
                drawLine(
                    color = TextDark,
                    start = Offset(x, 0f),
                    end = Offset(x, chartH),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )
                drawCircle(
                    color = PureWhite,
                    radius = 7.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = points[idx].color ?: accentColor,
                    radius = 5.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
    }
}

@Composable
fun RangeBarsCanvas(
    bars: List<RangeBarItem>,
    referenceLine: Float?,
    accentColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val maxVal = (bars.maxOfOrNull { it.max } ?: 100f).coerceAtLeast(10f)
        val minVal = 0f
        val range = (maxVal - minVal).coerceAtLeast(1f)

        // Draw horizontal reference lines
        referenceLine?.let { ref ->
            val refY = h - ((ref - minVal) / range) * h
            drawLine(
                color = Color(0xFFCBD5E1),
                start = Offset(0f, refY),
                end = Offset(w, refY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )
        }

        val barCount = bars.size
        val barWidth = (w / barCount * 0.5f).coerceIn(4.dp.toPx(), 24.dp.toPx())
        val slotWidth = w / barCount

        bars.forEachIndexed { i, bar ->
            val centerX = i * slotWidth + slotWidth / 2f
            val topY = h - ((bar.max - minVal) / range) * h
            val bottomY = h - ((bar.min - minVal) / range) * h
            val avgY = h - ((bar.avg - minVal) / range) * h
            val barColor = bar.color ?: accentColor

            // Background bar (min to max)
            drawRoundRect(
                color = barColor.copy(alpha = 0.35f),
                topLeft = Offset(centerX - barWidth / 2f, topY),
                size = Size(barWidth, (bottomY - topY).coerceAtLeast(4f)),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // Average dot
            drawCircle(
                color = barColor,
                radius = (barWidth * 0.45f).coerceAtLeast(3.dp.toPx()),
                center = Offset(centerX, avgY)
            )
        }
    }
}

@Composable
fun SleepStackedBarCanvas(bars: List<SleepStageBarItem>) {
    val colors = listOf(
        Color(0xFF3B82F6), // Deep
        Color(0xFF60A5FA), // Light
        Color(0xFF818CF8), // REM
        Color(0xFFF59E0B)  // Awake
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val goalLineMinutes = 420f // 7 hours
        val maxMinutes = (bars.maxOfOrNull { it.totalMinutes } ?: 480f).coerceAtLeast(goalLineMinutes + 60f)

        // Draw 7h goal dashed line
        val goalY = h - (goalLineMinutes / maxMinutes) * h
        drawLine(
            color = Color(0xFF8B5CF6),
            start = Offset(0f, goalY),
            end = Offset(w, goalY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
        )

        val slotWidth = w / bars.size
        val barWidth = (slotWidth * 0.55f).coerceIn(6.dp.toPx(), 28.dp.toPx())

        bars.forEachIndexed { i, bar ->
            val centerX = i * slotWidth + slotWidth / 2f
            var currentY = h

            val stages = listOf(
                bar.deepMinutes,
                bar.lightMinutes,
                bar.remMinutes,
                bar.awakeMinutes
            )

            stages.forEachIndexed { sIdx, stageMin ->
                val segHeight = (stageMin / maxMinutes) * h
                val segTop = currentY - segHeight

                drawRoundRect(
                    color = colors[sIdx],
                    topLeft = Offset(centerX - barWidth / 2f, segTop),
                    size = Size(barWidth, segHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
                currentY = segTop
            }
        }
    }
}

@Composable
fun SleepBreakdownCard(breakdown: SleepStagesBreakdown) {
    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Sleep Stage Architecture",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val stages = listOf(
                    Triple("Deep", "${breakdown.deepMinutes}m", Color(0xFF3B82F6)),
                    Triple("Light", "${breakdown.lightMinutes}m", Color(0xFF60A5FA)),
                    Triple("REM", "${breakdown.remMinutes}m", Color(0xFF818CF8)),
                    Triple("Awake", "${breakdown.awakeMinutes}m", Color(0xFFF59E0B))
                )
                stages.forEach { (name, duration, color) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = name, fontSize = 11.sp, color = TextSecondary)
                        Text(text = duration, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryStatsStrip(stats: List<StatItem>) {
    Surface(
        color = PureWhite,
        shape = BioTraceTokens.BentoCardShape,
        border = BioTraceTokens.CardHairlineBorder,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            stats.forEach { stat ->
                Surface(
                    color = InnerWellSurface,
                    shape = BioTraceTokens.InnerWellSmallShape,
                    border = BioTraceTokens.WellHairlineBorder,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = stat.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = stat.value,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = TextDark
                            )
                            if (stat.unit.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = stat.unit,
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 1.dp)
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
fun VitalHistoryRecordRow(item: VitalHistoryItem) {
    Surface(
        color = PureWhite,
        shape = BioTraceTokens.RecordCardShape,
        border = BioTraceTokens.CardHairlineBorder,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.formattedTime,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                if (item.secondaryDetail != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.secondaryDetail,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.deviceSource,
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = item.primaryValue,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDark
                    )
                    if (item.primaryUnit.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = item.primaryUnit,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 1.dp)
                        )
                    }
                }

                if (item.badge != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = item.badge.backgroundColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.badge.text,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = item.badge.textColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricInfoDialog(
    metricType: VitalMetricType,
    onDismiss: () -> Unit
) {
    val description = when (metricType) {
        VitalMetricType.HEART_RATE -> "Heart Rate is measured continuously via the watch's optical photoplethysmography (PPG) sensor. Health Connect aggregates Min, Average, and Max BPM across active time windows."
        VitalMetricType.RESTING_HEART_RATE -> "Resting Heart Rate represents baseline cardiovascular effort during minimum physical exertion. When RestingHeartRateRecord is omitted, it is computed as the 5th-percentile BPM recorded during sleep."
        VitalMetricType.SPO2 -> "Peripheral Blood Oxygen Saturation reflects the percentage of oxygenated hemoglobin in the blood. Measured via red and infrared light absorption. Normal values typically range from 95% to 100%."
        VitalMetricType.SLEEP -> "Health Connect tracks sleep sessions across 4 distinct stages: Deep, Light, REM, and Awake. The clinical target is set to 7 hours (420 minutes) per night."
        VitalMetricType.STEPS -> "Daily steps are tracked via the onboard 3-axis accelerometer and deduplicated automatically through Health Connect's aggregation pipelines against an 8,000 steps daily goal."
        VitalMetricType.CALORIES_DISTANCE -> "Active calories burned and distance covered during movement and workouts. Distance is calculated from step cadence and GPS tracking when available."
        VitalMetricType.VO2_MAX_WORKOUTS -> "Cardio Fitness (VO2 Max in mL/kg/min) measures maximum rate of oxygen consumption during strenuous exercise, correlated with heart rate dynamics and workout duration."
        VitalMetricType.BODY_METRICS -> "Body metrics track weight and calculated Body Mass Index (BMI = kg/m²). Standard WHO healthy range is 18.5 to 24.9 kg/m²."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = metricType.title, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(text = description, fontSize = 13.sp, lineHeight = 18.sp, color = TextDark)
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Got it", color = BioOrange, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun VitalDetailScreenPreview() {
    val sampleStats = listOf(
        StatItem("Average", "72", "bpm"),
        StatItem("Minimum", "58", "bpm"),
        StatItem("Maximum", "112", "bpm"),
        StatItem("Readings", "252")
    )

    val sampleHistory = listOf(
        VitalHistoryItem(
            id = "1",
            timestamp = Instant.now(),
            formattedTime = "Today, 14:32",
            primaryValue = "72",
            primaryUnit = "bpm",
            secondaryDetail = "Measured for 58s",
            badge = StatusBadge("Normal", ClinicalEmerald, ClinicalEmerald.copy(alpha = 0.12f)),
            deviceSource = "Health Connect"
        ),
        VitalHistoryItem(
            id = "2",
            timestamp = Instant.now().minusSeconds(1800),
            formattedTime = "Today, 14:02",
            primaryValue = "78",
            primaryUnit = "bpm",
            secondaryDetail = "Measured for 60s",
            badge = StatusBadge("Normal", ClinicalEmerald, ClinicalEmerald.copy(alpha = 0.12f)),
            deviceSource = "Health Connect"
        ),
        VitalHistoryItem(
            id = "3",
            timestamp = Instant.now().minusSeconds(3600),
            formattedTime = "Today, 13:30",
            primaryValue = "65",
            primaryUnit = "bpm",
            secondaryDetail = "Measured for 57s",
            badge = StatusBadge("Optimal", ClinicalEmerald, ClinicalEmerald.copy(alpha = 0.12f)),
            deviceSource = "Health Connect"
        )
    )

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LightBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TimeRangeSegmentedControl(
                selectedRange = MetricTimeRange.DAY,
                onRangeSelected = {}
            )

            HeroSummaryCard(
                uiState = VitalDetailUiState(
                    metricType = VitalMetricType.HEART_RATE,
                    headlineValue = "72",
                    headlineUnit = "bpm",
                    headlineDateLabel = "Today's Average",
                    subtitle = "Heart rate monitored by optical PPG sensor",
                    statusBadge = StatusBadge("Normal", ClinicalEmerald, ClinicalEmerald.copy(alpha = 0.12f))
                )
            )

            SummaryStatsStrip(stats = sampleStats)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sampleHistory.forEach { item ->
                    VitalHistoryRecordRow(item = item)
                }
            }
        }
    }
}
