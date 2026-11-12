package io.biotrace.app.ui.labs

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.biotrace.app.data.local.dao.LabBiomarkerDao
import io.biotrace.app.data.local.entity.LabBiomarkerEntity
import io.biotrace.app.domain.clinical.ClinicalDateParser
import io.biotrace.app.domain.clinical.ClinicalSchemaNormalizer
import io.biotrace.app.domain.clinical.PdfReportProcessor
import io.biotrace.app.domain.llm.LlmClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import kotlin.math.roundToInt

data class BiomarkerReportFilter(
    val id: String, // "ALL" or ISO date like "2026-10-28"
    val displayLabel: String,
    val labName: String? = null,
    val testCount: Int = 0
)

data class BiomarkerReadingHistory(
    val id: String,
    val date: Instant,
    val displayDate: String,
    val value: Double,
    val unit: String,
    val flag: String?,
    val source: String
)

data class LongitudinalBiomarkerCardModel(
    val canonicalName: String,
    val displayName: String,
    val currentReading: LabBiomarkerEntity,
    val previousReading: LabBiomarkerEntity? = null,
    val deltaValue: Double? = null,
    val history: List<BiomarkerReadingHistory> = emptyList()
)

data class LabsUiState(
    val isLoading: Boolean = false,
    val isScanning: Boolean = false,
    val isManualEntryOpen: Boolean = false,
    val isReviewDialogOpen: Boolean = false,
    val biomarkers: List<LabBiomarkerEntity> = emptyList(),
    val longitudinalBiomarkers: List<LongitudinalBiomarkerCardModel> = emptyList(),
    val availableReports: List<BiomarkerReportFilter> = emptyList(),
    val selectedReportFilterId: String = "ALL",
    val anomalies: List<LabBiomarkerEntity> = emptyList(),
    val pendingReviewBiomarkers: List<LabBiomarkerEntity> = emptyList(),
    val scanningFileName: String? = null,
    val scanningPageCount: Int = 1,
    val scanningPreviewBitmap: Bitmap? = null,
    val scannedReportMetadata: String? = null,
    val scanMessage: String? = null,
    val error: String? = null,
    val reviewPanelName: String = "",
    val reviewOperator: String = "=",
    val reviewValue: String = "",
    val reviewUnit: String = "",
    val unitHarmonizationText: String? = null,
    val linkedPanelResults: List<LabBiomarkerEntity> = emptyList()
)

@HiltViewModel
class LabsViewModel @Inject constructor(
    private val labBiomarkerDao: LabBiomarkerDao,
    private val llmClient: LlmClient,
    private val clinicalSchemaNormalizer: ClinicalSchemaNormalizer,
    private val pdfReportProcessor: PdfReportProcessor
) : ViewModel() {

    private val _uiState = MutableStateFlow(LabsUiState())
    val uiState: StateFlow<LabsUiState> = _uiState.asStateFlow()

    private var allRawBiomarkers: List<LabBiomarkerEntity> = emptyList()

    init {
        initOrLoadBiomarkers()
    }

    private fun initOrLoadBiomarkers() {
        viewModelScope.launch {
            // Clean up any legacy un-dated scan_* entries created before deterministic date extraction
            labBiomarkerDao.deleteLegacyScanEntries()
            loadBiomarkers()
        }
    }

    fun loadBiomarkers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                labBiomarkerDao.getAllFlow().collect { records ->
                    allRawBiomarkers = records
                    recomputeUiState()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun selectReportFilter(filterId: String) {
        _uiState.update { it.copy(selectedReportFilterId = filterId) }
        recomputeUiState()
    }

    private fun recomputeUiState() {
        val records = allRawBiomarkers
        if (records.isEmpty()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    biomarkers = emptyList(),
                    longitudinalBiomarkers = emptyList(),
                    availableReports = listOf(BiomarkerReportFilter("ALL", "All Reports", null, 0)),
                    anomalies = emptyList()
                )
            }
            return
        }

        // 1. Group records by ISO report date to discover distinct reports across the year
        val dateGroups = records.groupBy { ClinicalDateParser.toIsoDateString(it.recordedAt) }
        val sortedDates = dateGroups.keys.sortedDescending()

        val reportFilters = mutableListOf<BiomarkerReportFilter>()
        reportFilters.add(BiomarkerReportFilter("ALL", "All Reports", null, records.size))

        for (dateKey in sortedDates) {
            val listForDate = dateGroups[dateKey] ?: emptyList()
            val sample = listForDate.first()
            val displayDate = ClinicalDateParser.formatDisplayDate(sample.recordedAt)
            val labName = listForDate.map { it.source }
                .firstOrNull { it != "MANUAL" && it != "VISION_LLM" }
                ?: sample.source
            val label = if (labName != "MANUAL" && labName != "VISION_LLM") "$displayDate • $labName" else displayDate
            reportFilters.add(BiomarkerReportFilter(dateKey, label, labName, listForDate.size))
        }

        val currentFilter = _uiState.value.selectedReportFilterId
        val effectiveFilter = if (currentFilter == "ALL" || dateGroups.containsKey(currentFilter)) currentFilter else "ALL"

        // 2. Build longitudinal cards by grouping on canonical biomarkerName
        val biomarkerGroups = records.groupBy { it.biomarkerName }
        val cardModels = mutableListOf<LongitudinalBiomarkerCardModel>()

        for ((canonical, groupRecords) in biomarkerGroups) {
            val sortedChronological = groupRecords.sortedByDescending { it.recordedAt }
            val currentReading = if (effectiveFilter == "ALL") {
                sortedChronological.first()
            } else {
                sortedChronological.firstOrNull { ClinicalDateParser.toIsoDateString(it.recordedAt) == effectiveFilter }
            }

            if (currentReading != null) {
                val previousReading = sortedChronological.firstOrNull { it.recordedAt.isBefore(currentReading.recordedAt) }
                val delta = if (previousReading != null) {
                    ((currentReading.value - previousReading.value) * 100.0).roundToInt() / 100.0
                } else null

                val history = sortedChronological.map {
                    BiomarkerReadingHistory(
                        id = it.id,
                        date = it.recordedAt,
                        displayDate = ClinicalDateParser.formatDisplayDate(it.recordedAt),
                        value = it.value,
                        unit = it.unit,
                        flag = it.flag,
                        source = it.source
                    )
                }

                cardModels.add(
                    LongitudinalBiomarkerCardModel(
                        canonicalName = canonical,
                        displayName = currentReading.rawName,
                        currentReading = currentReading,
                        previousReading = previousReading,
                        deltaValue = delta,
                        history = history
                    )
                )
            }
        }

        // Sort cards: anomalies (HIGH/LOW) at top, then alphabetical by displayName
        val sortedCards = cardModels.sortedWith(
            compareByDescending<LongitudinalBiomarkerCardModel> { it.currentReading.flag == "HIGH" || it.currentReading.flag == "LOW" }
                .thenBy { it.displayName }
        )

        val displayedEntities = if (effectiveFilter == "ALL") {
            sortedCards.map { it.currentReading }
        } else {
            records.filter { ClinicalDateParser.toIsoDateString(it.recordedAt) == effectiveFilter }
        }

        val anomaliesList = displayedEntities.filter { it.flag == "HIGH" || it.flag == "LOW" }

        _uiState.update {
            it.copy(
                isLoading = false,
                biomarkers = displayedEntities,
                longitudinalBiomarkers = sortedCards,
                availableReports = reportFilters,
                selectedReportFilterId = effectiveFilter,
                anomalies = anomaliesList
            )
        }
    }

    fun openManualEntry(open: Boolean) {
        _uiState.update { it.copy(isManualEntryOpen = open) }
    }

    fun addManualBiomarker(
        name: String,
        value: Double,
        unit: String,
        min: Double?,
        max: Double?
    ) {
        viewModelScope.launch {
            val normalized = clinicalSchemaNormalizer.normalize(name, value.toString(), unit, min, max)
            val now = Instant.now()
            val dateIso = ClinicalDateParser.toIsoDateString(now)

            val entity = LabBiomarkerEntity(
                id = "lab_${normalized.canonicalName}_${dateIso}",
                biomarkerName = normalized.canonicalName,
                rawName = normalized.displayName,
                value = normalized.value,
                unit = normalized.unit,
                referenceMin = normalized.referenceMin,
                referenceMax = normalized.referenceMax,
                flag = normalized.flag,
                recordedAt = now,
                source = "MANUAL"
            )

            labBiomarkerDao.insert(entity)
            _uiState.update { it.copy(isManualEntryOpen = false) }
        }
    }

    fun updateReviewOperator(op: String) {
        _uiState.update { it.copy(reviewOperator = op) }
    }

    fun updateReviewValue(valueStr: String) {
        _uiState.update { current ->
            val biomarker = current.pendingReviewBiomarkers.firstOrNull { it.biomarkerName.equals("HBA1C", ignoreCase = true) }
                ?: current.pendingReviewBiomarkers.firstOrNull()
            val bName = biomarker?.biomarkerName ?: current.reviewPanelName
            val harmonization = computeUnitHarmonization(
                biomarkerName = bName,
                valueStr = valueStr,
                unit = current.reviewUnit
            )
            val numVal = valueStr.trim().toDoubleOrNull()
            val updatedPending = if (numVal != null && biomarker != null) {
                current.pendingReviewBiomarkers.map {
                    if (it.id == biomarker.id) it.copy(value = numVal) else it
                }
            } else {
                current.pendingReviewBiomarkers
            }
            current.copy(
                reviewValue = valueStr,
                unitHarmonizationText = harmonization,
                pendingReviewBiomarkers = updatedPending
            )
        }
    }

    fun computeUnitHarmonization(biomarkerName: String, valueStr: String, unit: String): String? {
        if (!biomarkerName.trim().equals("HBA1C", ignoreCase = true)) {
            return null
        }
        val numericVal = valueStr.trim().toDoubleOrNull() ?: return null
        val cleanVal = valueStr.trim()
        return when {
            unit.contains("%") -> {
                val ifcc = ((numericVal - 2.15) * 10.929).roundToInt()
                "$cleanVal % ↔ $ifcc mmol/mol"
            }
            unit.replace(" ", "").contains("mmol/mol", ignoreCase = true) -> {
                val dcct = ((numericVal * 0.09148 + 2.152) * 10.0).roundToInt() / 10.0
                "$cleanVal mmol/mol ↔ $dcct %"
            }
            else -> null
        }
    }

    fun closeReviewDialog() {
        _uiState.update {
            it.copy(
                isReviewDialogOpen = false,
                pendingReviewBiomarkers = emptyList(),
                reviewPanelName = "",
                reviewOperator = "=",
                reviewValue = "",
                reviewUnit = "",
                unitHarmonizationText = null,
                linkedPanelResults = emptyList()
            )
        }
    }

    fun confirmAndSaveReviewedMarkers(reviewedList: List<LabBiomarkerEntity> = _uiState.value.pendingReviewBiomarkers) {
        viewModelScope.launch {
            val reviewOperator = _uiState.value.reviewOperator
            val reviewValueNum = _uiState.value.reviewValue.toDoubleOrNull()
            val targetList = reviewedList.ifEmpty { _uiState.value.pendingReviewBiomarkers }
            val primary = targetList.firstOrNull { it.biomarkerName.equals("HBA1C", ignoreCase = true) }
                ?: targetList.firstOrNull()

            val finalMarkers = targetList.map { marker ->
                val finalRawName = if (!reviewOperator.isNullOrBlank() && !marker.rawName.startsWith(reviewOperator)) {
                    "$reviewOperator ${marker.rawName}"
                } else {
                    marker.rawName
                }
                val finalVal = if (marker.id == primary?.id && reviewValueNum != null) reviewValueNum else marker.value
                marker.copy(rawName = finalRawName, value = finalVal)
            }

            if (finalMarkers.isNotEmpty()) {
                labBiomarkerDao.insertAll(finalMarkers)
            }
            _uiState.update {
                it.copy(
                    isReviewDialogOpen = false,
                    pendingReviewBiomarkers = emptyList(),
                    reviewPanelName = "",
                    reviewOperator = "=",
                    reviewValue = "",
                    reviewUnit = "",
                    unitHarmonizationText = null,
                    linkedPanelResults = emptyList(),
                    scanMessage = "Saved ${finalMarkers.size} verified blood markers to vault."
                )
            }
        }
    }

    fun processAndScanBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isScanning = true,
                    scanningPreviewBitmap = bitmap,
                    scanningFileName = "Paper Lab Report (Camera)",
                    scanMessage = "Optimizing photo and analyzing with Multimodal Vision LLM...",
                    error = null
                )
            }
            try {
                // Downsample bitmap so max edge is 1600px (OOM-safe)
                val maxEdge = 1600
                val width = bitmap.width
                val height = bitmap.height
                val scaledBitmap = if (width > maxEdge || height > maxEdge) {
                    val ratio = if (width > height) maxEdge.toFloat() / width else maxEdge.toFloat() / height
                    val targetW = (width * ratio).roundToInt().coerceAtLeast(1)
                    val targetH = (height * ratio).roundToInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
                } else {
                    bitmap
                }

                // Compress to JPEG (85% quality) and encode to Base64 (NO_WRAP)
                val outputStream = java.io.ByteArrayOutputStream()
                val format: Bitmap.CompressFormat? = Bitmap.CompressFormat.JPEG
                if (format != null) {
                    scaledBitmap.compress(format, 85, outputStream)
                }
                val bytes = outputStream.toByteArray()
                val base64Data = (try {
                    android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                } catch (t: Throwable) {
                    null
                }) ?: java.util.Base64.getEncoder().encodeToString(bytes)

                scanBloodReport(base64Data, "image/jpeg", "paper_report.jpg")
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        error = e.message ?: "Failed to process photo"
                    )
                }
            }
        }
    }

    fun processAndScanDocument(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isScanning = true,
                    scanMessage = "Inspecting lab report...",
                    error = null
                )
            }
            try {
                val doc = pdfReportProcessor.processUri(uri)
                val isPdf = doc.mimeType == "application/pdf"
                _uiState.update {
                    it.copy(
                        scanningFileName = doc.fileName,
                        scanningPageCount = doc.pageCount,
                        scanningPreviewBitmap = doc.previewBitmap,
                        scanMessage = if (isPdf) {
                            "Analyzing ${doc.pageCount}-page PDF with Multimodal Vision LLM..."
                        } else {
                            "Analyzing report scan with Multimodal Vision LLM..."
                        }
                    )
                }

                scanBloodReport(doc.base64Data, doc.mimeType, doc.fileName)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        error = e.message ?: "Failed to process selected report"
                    )
                }
            }
        }
    }

    fun processAndScanFile(file: java.io.File) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isScanning = true,
                    scanMessage = "Inspecting lab report ${file.name}...",
                    error = null
                )
            }
            try {
                val doc = pdfReportProcessor.processFile(file)
                val isPdf = doc.mimeType == "application/pdf"
                _uiState.update {
                    it.copy(
                        scanningFileName = doc.fileName,
                        scanningPageCount = doc.pageCount,
                        scanningPreviewBitmap = doc.previewBitmap,
                        scanMessage = if (isPdf) {
                            "Analyzing ${doc.pageCount}-page PDF with Multimodal Vision LLM..."
                        } else {
                            "Analyzing report scan with Multimodal Vision LLM..."
                        }
                    )
                }

                scanBloodReport(doc.base64Data, doc.mimeType, doc.fileName)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        error = e.message ?: "Failed to process selected report"
                    )
                }
            }
        }
    }

    fun scanBloodReport(base64Data: String, mimeType: String = "image/jpeg", fileName: String? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isScanning = true,
                    scanMessage = it.scanMessage ?: "Analyzing report with Multimodal Vision LLM...",
                    error = null
                )
            }
            try {
                val prompt = """
                    You are an expert clinical laboratory report parser. Analyze this diagnostic blood report document and extract all biomarker test results.
                    For each test result found in the document, extract:
                    - "rawName": Exact test name from report (e.g. "Glycated Hemoglobin HbA1c", "Serum Ferritin", "Total Cholesterol", "TSH", "Fasting Blood Sugar", "Vitamin D, 25-Hydroxy", "Estimated Average Glucose")
                    - "value": Test result value as string (e.g. "5.4", "185", "<0.01", "24.0")
                    - "unit": Exact unit of measurement (e.g. "%", "mg/dL", "ng/mL", "mmol/L", "μIU/mL", "g/dL")
                    - "referenceMin": Reference interval lower bound number or null if not provided
                    - "referenceMax": Reference interval upper bound number or null if not provided
                    - "flag": One of "NORMAL", "HIGH", "LOW" based on the reference range

                    Also extract:
                    - "labName": Diagnostic center or laboratory name if visible (e.g. "Quest Diagnostics", "Healthi", "Metropolis", "Lal PathLabs") or null
                    - "reportDate": Date of collection or report (YYYY-MM-DD or string) or null

                    Return ONLY a valid JSON object with the format:
                    {
                      "labName": "...",
                      "reportDate": "...",
                      "biomarkers": [ ... ]
                    }
                    Or directly a JSON array of biomarker objects. No markdown formatting, no explanatory text.
                """.trimIndent()

                val response = llmClient.generateContentWithDocument(prompt, base64Data, mimeType)
                val cleanedJson = response.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

                var extractedLabName = ""
                var extractedReportDate = ""

                val jsonArray = if (cleanedJson.startsWith("{")) {
                    val root = JSONObject(cleanedJson)
                    extractedLabName = root.optString("labName", "")
                    extractedReportDate = root.optString("reportDate", "")
                    if (extractedLabName.isNotBlank() || extractedReportDate.isNotBlank()) {
                        val metaStr = listOf(extractedLabName, extractedReportDate).filter { it.isNotBlank() }.joinToString(" • ")
                        _uiState.update { it.copy(scannedReportMetadata = metaStr) }
                    }
                    root.optJSONArray("biomarkers")
                        ?: root.optJSONArray("results")
                        ?: root.optJSONArray("tests")
                        ?: JSONArray()
                } else {
                    JSONArray(cleanedJson)
                }

                val reportInstant = ClinicalDateParser.parseToInstant(extractedReportDate)
                val dateIso = ClinicalDateParser.toIsoDateString(reportInstant)
                val normalizedList = mutableListOf<LabBiomarkerEntity>()
                val operatorMap = mutableMapOf<String, String>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val rawName = obj.optString("rawName", obj.optString("testName", "Biomarker"))
                    val rawValStr = obj.optString("value", obj.optString("result", "0.0"))
                    val rawUnit = obj.optString("unit", obj.optString("units", ""))
                    val refMin = if (obj.has("referenceMin") && !obj.isNull("referenceMin")) obj.getDouble("referenceMin") else null
                    val refMax = if (obj.has("referenceMax") && !obj.isNull("referenceMax")) obj.getDouble("referenceMax") else null

                    val norm = clinicalSchemaNormalizer.normalize(rawName, rawValStr, rawUnit, refMin, refMax)
                    val entityId = "lab_${norm.canonicalName}_${dateIso}"

                    if (norm.operator != null) {
                        val opStr = when (norm.operator) {
                            "<=" -> "≤"
                            ">=" -> "≥"
                            else -> norm.operator
                        }
                        operatorMap[entityId] = opStr
                    }

                    normalizedList.add(
                        LabBiomarkerEntity(
                            id = entityId,
                            biomarkerName = norm.canonicalName,
                            rawName = norm.displayName,
                            value = norm.value,
                            unit = norm.unit,
                            referenceMin = norm.referenceMin,
                            referenceMax = norm.referenceMax,
                            flag = norm.flag,
                            recordedAt = reportInstant,
                            source = extractedLabName.ifBlank { "VISION_LLM" }
                        )
                    )
                }

                if (normalizedList.isNotEmpty()) {
                    val primaryMarker = normalizedList.firstOrNull { it.biomarkerName.equals("HBA1C", ignoreCase = true) }
                        ?: normalizedList.first()
                    val linkedResults = normalizedList.filter { it.id != primaryMarker.id }
                    val panelName = if (primaryMarker.biomarkerName.equals("HBA1C", ignoreCase = true)) {
                        "HbA1c Panel"
                    } else if (primaryMarker.rawName.endsWith("Panel", ignoreCase = true)) {
                        primaryMarker.rawName
                    } else {
                        "${primaryMarker.rawName} Panel"
                    }
                    val detectedOp = operatorMap[primaryMarker.id] ?: "="
                    val formattedValue = if (primaryMarker.value % 1.0 == 0.0) {
                        primaryMarker.value.toInt().toString()
                    } else {
                        primaryMarker.value.toString()
                    }
                    val harmonization = computeUnitHarmonization(
                        primaryMarker.biomarkerName,
                        formattedValue,
                        primaryMarker.unit
                    )

                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            isReviewDialogOpen = true,
                            pendingReviewBiomarkers = normalizedList,
                            scanMessage = null,
                            reviewPanelName = panelName,
                            reviewOperator = detectedOp,
                            reviewValue = formattedValue,
                            reviewUnit = primaryMarker.unit,
                            unitHarmonizationText = harmonization,
                            linkedPanelResults = linkedResults
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            error = "No blood markers could be recognized from the document."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        error = e.message ?: "Failed to scan report. Check Gemini API key in Settings."
                    )
                }
            }
        }
    }
}
