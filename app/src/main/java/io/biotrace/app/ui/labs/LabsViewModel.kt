package io.biotrace.app.ui.labs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.biotrace.app.data.local.dao.LabBiomarkerDao
import io.biotrace.app.data.local.entity.LabBiomarkerEntity
import io.biotrace.app.domain.llm.LlmClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

data class LabsUiState(
    val isLoading: Boolean = false,
    val isScanning: Boolean = false,
    val isManualEntryOpen: Boolean = false,
    val biomarkers: List<LabBiomarkerEntity> = emptyList(),
    val anomalies: List<LabBiomarkerEntity> = emptyList(),
    val scanMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class LabsViewModel @Inject constructor(
    private val labBiomarkerDao: LabBiomarkerDao,
    private val llmClient: LlmClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(LabsUiState())
    val uiState: StateFlow<LabsUiState> = _uiState.asStateFlow()

    init {
        loadBiomarkers()
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

    fun addManualBiomarker(
        name: String,
        value: Double,
        unit: String,
        min: Double?,
        max: Double?
    ) {
        viewModelScope.launch {
            val normalized = name.trim().uppercase().replace(" ", "_")
            val flag = when {
                max != null && value > max -> "HIGH"
                min != null && value < min -> "LOW"
                else -> "NORMAL"
            }

            val entity = LabBiomarkerEntity(
                id = "lab_${UUID.randomUUID()}",
                biomarkerName = normalized,
                rawName = name.trim(),
                value = value,
                unit = unit.trim(),
                referenceMin = min,
                referenceMax = max,
                flag = flag,
                recordedAt = Instant.now(),
                source = "MANUAL"
            )

            labBiomarkerDao.insert(entity)
            _uiState.update { it.copy(isManualEntryOpen = false) }
        }
    }

    fun scanBloodReport(base64Image: String, mimeType: String = "image/jpeg") {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanMessage = "Analyzing blood report with Vision LLM...", error = null) }
            try {
                val prompt = """
                    You are a clinical blood report parser. Analyze this lab test report image and extract all biomarker test results.
                    Extract each biomarker into a JSON object with:
                    - "biomarkerName": Normalized uppercase name (e.g., "HBA1C", "CHOLESTEROL_TOTAL", "HDL", "LDL", "TRIGLYCERIDES", "FASTING_GLUCOSE", "FERRITIN", "TSH", "VITAMIN_D", "HEMOGLOBIN", "PLATELETS", "WBC", "CREATININE")
                    - "rawName": Exact text string from the report
                    - "value": Number (Double)
                    - "unit": Unit string (e.g., "mg/dL", "ng/mL", "g/dL", "%")
                    - "referenceMin": Minimum standard range number (or null if not specified)
                    - "referenceMax": Maximum standard range number (or null if not specified)
                    - "flag": "HIGH", "LOW", or "NORMAL"

                    Return ONLY a valid JSON array of these objects. No markdown formatting, no other text.
                """.trimIndent()

                val response = llmClient.generateContentWithImage(prompt, base64Image, mimeType)
                val cleanedJson = response.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

                val jsonArray = JSONArray(cleanedJson)
                val extractedList = mutableListOf<LabBiomarkerEntity>()
                val now = Instant.now()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val rawName = obj.optString("rawName", "Biomarker")
                    val name = obj.optString("biomarkerName", rawName.uppercase().replace(" ", "_"))
                    val value = obj.optDouble("value", 0.0)
                    val unit = obj.optString("unit", "")
                    val refMin = if (obj.has("referenceMin") && !obj.isNull("referenceMin")) obj.getDouble("referenceMin") else null
                    val refMax = if (obj.has("referenceMax") && !obj.isNull("referenceMax")) obj.getDouble("referenceMax") else null
                    val flag = obj.optString("flag", "NORMAL")

                    extractedList.add(
                        LabBiomarkerEntity(
                            id = "scan_${UUID.randomUUID()}",
                            biomarkerName = name,
                            rawName = rawName,
                            value = value,
                            unit = unit,
                            referenceMin = refMin,
                            referenceMax = refMax,
                            flag = flag,
                            recordedAt = now,
                            source = "VISION_LLM"
                        )
                    )
                }

                if (extractedList.isNotEmpty()) {
                    labBiomarkerDao.insertAll(extractedList)
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            scanMessage = "Successfully parsed ${extractedList.size} biomarkers from report"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            error = "No blood markers could be recognized from the image"
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
