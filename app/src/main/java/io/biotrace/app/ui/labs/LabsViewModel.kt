package io.biotrace.app.ui.labs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.biotrace.app.data.local.dao.LabBiomarkerDao
import io.biotrace.app.data.local.entity.LabBiomarkerEntity
import io.biotrace.app.domain.clinical.ClinicalSchemaNormalizer
import io.biotrace.app.domain.llm.LlmClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject

data class LabsUiState(
    val isLoading: Boolean = false,
    val isScanning: Boolean = false,
    val isManualEntryOpen: Boolean = false,
    val isReviewDialogOpen: Boolean = false,
    val biomarkers: List<LabBiomarkerEntity> = emptyList(),
    val anomalies: List<LabBiomarkerEntity> = emptyList(),
    val pendingReviewBiomarkers: List<LabBiomarkerEntity> = emptyList(),
    val scanMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class LabsViewModel @Inject constructor(
    private val labBiomarkerDao: LabBiomarkerDao,
    private val llmClient: LlmClient,
    private val clinicalSchemaNormalizer: ClinicalSchemaNormalizer
) : ViewModel() {

    private val _uiState = MutableStateFlow(LabsUiState())
    val uiState: StateFlow<LabsUiState> = _uiState.asStateFlow()

    init {
        initOrLoadBiomarkers()
    }

    private fun initOrLoadBiomarkers() {
        viewModelScope.launch {
            val existing = labBiomarkerDao.getAll()
            if (existing.isEmpty()) {
                seedInitialBiomarkers()
            }
            loadBiomarkers()
        }
    }

    private suspend fun seedInitialBiomarkers() {
        val now = Instant.now()
        val initialList = listOf(
            LabBiomarkerEntity(
                id = "lab_hba1c_${UUID.randomUUID()}",
                biomarkerName = "HBA1C",
                rawName = "Hemoglobin A1c (HbA1c)",
                value = 5.4,
                unit = "%",
                referenceMin = 4.0,
                referenceMax = 5.6,
                flag = "NORMAL",
                recordedAt = now.minus(14, ChronoUnit.DAYS),
                source = "VISION_LLM"
            ),
            LabBiomarkerEntity(
                id = "lab_tc_${UUID.randomUUID()}",
                biomarkerName = "CHOLESTEROL_TOTAL",
                rawName = "Total Cholesterol",
                value = 185.0,
                unit = "mg/dL",
                referenceMin = 125.0,
                referenceMax = 200.0,
                flag = "NORMAL",
                recordedAt = now.minus(14, ChronoUnit.DAYS),
                source = "VISION_LLM"
            ),
            LabBiomarkerEntity(
                id = "lab_hdl_${UUID.randomUUID()}",
                biomarkerName = "HDL",
                rawName = "HDL Cholesterol",
                value = 58.0,
                unit = "mg/dL",
                referenceMin = 40.0,
                referenceMax = 60.0,
                flag = "NORMAL",
                recordedAt = now.minus(14, ChronoUnit.DAYS),
                source = "VISION_LLM"
            ),
            LabBiomarkerEntity(
                id = "lab_ferritin_${UUID.randomUUID()}",
                biomarkerName = "FERRITIN",
                rawName = "Serum Ferritin",
                value = 24.0,
                unit = "ng/mL",
                referenceMin = 30.0,
                referenceMax = 300.0,
                flag = "LOW",
                recordedAt = now.minus(14, ChronoUnit.DAYS),
                source = "VISION_LLM"
            ),
            LabBiomarkerEntity(
                id = "lab_vitd_${UUID.randomUUID()}",
                biomarkerName = "VITAMIN_D",
                rawName = "25-OH Vitamin D",
                value = 28.5,
                unit = "ng/mL",
                referenceMin = 30.0,
                referenceMax = 100.0,
                flag = "LOW",
                recordedAt = now.minus(14, ChronoUnit.DAYS),
                source = "VISION_LLM"
            ),
            LabBiomarkerEntity(
                id = "lab_tsh_${UUID.randomUUID()}",
                biomarkerName = "TSH",
                rawName = "Thyroid Stimulating Hormone",
                value = 2.1,
                unit = "μIU/mL",
                referenceMin = 0.45,
                referenceMax = 4.5,
                flag = "NORMAL",
                recordedAt = now.minus(14, ChronoUnit.DAYS),
                source = "VISION_LLM"
            )
        )
        labBiomarkerDao.insertAll(initialList)
    }

    fun loadBiomarkers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                labBiomarkerDao.getAllFlow().collect { records ->
                    val anomaliesList = records.filter { it.flag == "HIGH" || it.flag == "LOW" }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            biomarkers = records,
                            anomalies = anomaliesList
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun openManualEntry(open: Boolean) {
        _uiState.update { it.copy(isManualEntryOpen = open) }
    }

    fun closeReviewDialog() {
        _uiState.update { it.copy(isReviewDialogOpen = false, pendingReviewBiomarkers = emptyList()) }
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

            val entity = LabBiomarkerEntity(
                id = "lab_${UUID.randomUUID()}",
                biomarkerName = normalized.canonicalName,
                rawName = normalized.displayName,
                value = normalized.value,
                unit = normalized.unit,
                referenceMin = normalized.referenceMin,
                referenceMax = normalized.referenceMax,
                flag = normalized.flag,
                recordedAt = Instant.now(),
                source = "MANUAL"
            )

            labBiomarkerDao.insert(entity)
            _uiState.update { it.copy(isManualEntryOpen = false) }
        }
    }

    fun confirmAndSaveReviewedMarkers(markers: List<LabBiomarkerEntity>) {
        viewModelScope.launch {
            if (markers.isNotEmpty()) {
                labBiomarkerDao.insertAll(markers)
            }
            _uiState.update {
                it.copy(
                    isReviewDialogOpen = false,
                    pendingReviewBiomarkers = emptyList(),
                    scanMessage = "Saved ${markers.size} verified blood markers to vault."
                )
            }
        }
    }

    fun scanBloodReport(base64Image: String, mimeType: String = "image/jpeg") {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanMessage = "Analyzing report with Multimodal Vision LLM...", error = null) }
            try {
                val prompt = """
                    You are a clinical blood report parser. Analyze this lab test report image and extract all biomarker test results.
                    Extract each biomarker into a JSON object with:
                    - "rawName": Exact test name from the report (e.g. "Glycated Hemoglobin HbA1c", "Serum Ferritin", "Total Cholesterol", "TSH")
                    - "value": Raw value as string or number (e.g. "5.4", "<0.01", "185")
                    - "unit": Exact unit (e.g. "%", "mg/dL", "ng/mL", "mmol/L")
                    - "referenceMin": Reference interval lower bound or null
                    - "referenceMax": Reference interval upper bound or null

                    Return ONLY a valid JSON array of these objects. No markdown formatting, no other text.
                """.trimIndent()

                val response = llmClient.generateContentWithImage(prompt, base64Image, mimeType)
                val cleanedJson = response.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

                val jsonArray = JSONArray(cleanedJson)
                val normalizedList = mutableListOf<LabBiomarkerEntity>()
                val now = Instant.now()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val rawName = obj.optString("rawName", "Biomarker")
                    val rawValStr = obj.optString("value", "0.0")
                    val rawUnit = obj.optString("unit", "")
                    val refMin = if (obj.has("referenceMin") && !obj.isNull("referenceMin")) obj.getDouble("referenceMin") else null
                    val refMax = if (obj.has("referenceMax") && !obj.isNull("referenceMax")) obj.getDouble("referenceMax") else null

                    val norm = clinicalSchemaNormalizer.normalize(rawName, rawValStr, rawUnit, refMin, refMax)

                    normalizedList.add(
                        LabBiomarkerEntity(
                            id = "scan_${UUID.randomUUID()}",
                            biomarkerName = norm.canonicalName,
                            rawName = norm.displayName,
                            value = norm.value,
                            unit = norm.unit,
                            referenceMin = norm.referenceMin,
                            referenceMax = norm.referenceMax,
                            flag = norm.flag,
                            recordedAt = now,
                            source = "VISION_LLM"
                        )
                    )
                }

                if (normalizedList.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            isReviewDialogOpen = true,
                            pendingReviewBiomarkers = normalizedList,
                            scanMessage = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            error = "No blood markers could be recognized from the image."
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
