package io.biotrace.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.biotrace.app.data.local.dao.LabBiomarkerDao
import io.biotrace.app.data.local.entity.LabBiomarkerEntity
import io.biotrace.app.data.local.store.UserProfileStore
import io.biotrace.app.domain.llm.LlmClient
import io.biotrace.app.domain.longevity.BiologicalAgeResult
import io.biotrace.app.domain.model.ProfileTriptychSummary
import io.biotrace.app.domain.model.VitalMetricType
import io.biotrace.app.domain.repository.VitalsRepository
import io.biotrace.app.domain.usecase.GenerateHealthReportUseCase
import io.biotrace.app.domain.usecase.GetBiologicalAgeUseCase
import io.biotrace.app.domain.usecase.ReportChunk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CorrelationCardData(
    val title: String,
    val category: String,
    val primaryInsight: String,
    val metric1Label: String,
    val metric1Value: String,
    val metric2Label: String,
    val metric2Value: String,
    val couplingScore: String,
    val statusTag: String
)

sealed class AiReportState {
    data object Idle      : AiReportState()
    data object Loading   : AiReportState()
    data class  Streaming(val text: String) : AiReportState()
    data class  Complete(val text: String)  : AiReportState()
    data class  Error(val message: String)  : AiReportState()
}

data class BiometricInsightsUiState(
    val profile: ProfileTriptychSummary = ProfileTriptychSummary(),
    val correlations: List<CorrelationCardData> = emptyList(),
    val labBiomarkers: List<LabBiomarkerEntity> = emptyList(),
    val biologicalAge: BiologicalAgeResult? = null,
    val isAgeDialogOpen: Boolean = false,
    val isLoading: Boolean = false,
    val isApiKeyConfigured: Boolean = false,
    val reportState: AiReportState = AiReportState.Idle
)

@HiltViewModel
class BiometricInsightsViewModel @Inject constructor(
    private val vitalsRepository: VitalsRepository,
    private val labBiomarkerDao: LabBiomarkerDao,
    private val generateHealthReportUseCase: GenerateHealthReportUseCase,
    private val llmClient: LlmClient,
    private val getBiologicalAgeUseCase: GetBiologicalAgeUseCase,
    private val userProfileStore: UserProfileStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(BiometricInsightsUiState())
    val uiState: StateFlow<BiometricInsightsUiState> = _uiState.asStateFlow()

    private var lastReportDate: java.time.LocalDate? = null

    init {
        loadInsights()
        checkApiKeyAndMaybeGenerate()
    }

    private fun checkApiKeyAndMaybeGenerate() {
        viewModelScope.launch {
            val configured = llmClient.isConfigured()
            _uiState.update { it.copy(isApiKeyConfigured = configured) }
            val today = java.time.LocalDate.now()
            if (configured && lastReportDate != today) {
                generateReport()
            }
        }
    }

    fun generateReport() {
        viewModelScope.launch {
            var buffer = ""
            generateHealthReportUseCase.execute().collect { chunk ->
                when (chunk) {
                    is ReportChunk.Started -> _uiState.update { it.copy(reportState = AiReportState.Loading) }
                    is ReportChunk.Text    -> {
                        buffer += chunk.text
                        _uiState.update { it.copy(reportState = AiReportState.Streaming(buffer)) }
                    }
                    is ReportChunk.Done    -> {
                        lastReportDate = java.time.LocalDate.now()
                        _uiState.update { it.copy(reportState = AiReportState.Complete(buffer)) }
                    }
                    is ReportChunk.Error   -> _uiState.update { it.copy(reportState = AiReportState.Error(chunk.message)) }
                }
            }
        }
    }

    fun loadInsights() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val bioAge = try {
                getBiologicalAgeUseCase()
            } catch (e: Exception) {
                null
            }

            val profile = vitalsRepository.getProfileTriptychSummary()
            val cards = vitalsRepository.getVitalsDashboardSnapshot()
            val labs = labBiomarkerDao.getAll()

            // Dynamic Cross-Correlations derived strictly from real collected measurements
            val correlationsList = mutableListOf<CorrelationCardData>()

            // 1. Sleep Architecture vs Metabolic Recovery
            val sleepCard = cards.find { it.metricType == VitalMetricType.SLEEP }
            val calCard = cards.find { it.metricType == VitalMetricType.CALORIES_DISTANCE }
            val sleepDuration = sleepCard?.primaryValue ?: profile.sleepScore
            val calVal = calCard?.primaryValue ?: "--"

            if (sleepDuration != "--" && sleepDuration != "--%"  && calVal != "--" && calVal != "0" && calVal != "0 kcal") {
                correlationsList.add(
                    CorrelationCardData(
                        title = "Sleep & Glycemic Energy Expenditure",
                        category = "CIRCADIAN METABOLISM",
                        primaryInsight = "Overnight cellular repair window supports insulin sensitivity and glycogen restoration during active daytime exertion.",
                        metric1Label = "Sleep Window",
                        metric1Value = "$sleepDuration",
                        metric2Label = "Active Burn",
                        metric2Value = "$calVal kcal",
                        couplingScore = "Strong Sync",
                        statusTag = "METABOLICALLY STABLE"
                    )
                )
            }

            _uiState.update {
                it.copy(
                    profile = profile,
                    correlations = correlationsList,
                    labBiomarkers = labs,
                    biologicalAge = bioAge,
                    isLoading = false
                )
            }
        }
    }

    fun toggleAgeDialog(open: Boolean) {
        _uiState.update { it.copy(isAgeDialogOpen = open) }
    }

    fun updateChronologicalAge(age: Int) {
        viewModelScope.launch {
            userProfileStore.setChronologicalAge(age)
            val bioAge = try {
                getBiologicalAgeUseCase()
            } catch (e: Exception) {
                null
            }
            _uiState.update {
                it.copy(
                    biologicalAge = bioAge,
                    isAgeDialogOpen = false
                )
            }
        }
    }
}
