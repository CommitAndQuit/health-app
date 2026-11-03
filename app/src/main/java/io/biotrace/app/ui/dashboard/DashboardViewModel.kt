package io.biotrace.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.biotrace.app.data.healthconnect.HealthConnectManager
import io.biotrace.app.domain.model.MetricOverview
import io.biotrace.app.domain.model.TimeRangeSelection
import io.biotrace.app.domain.model.VitalPoint
import io.biotrace.app.domain.repository.VitalsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

enum class MoodType(val label: String) {
    NORMAL("Normal"),
    CALM("Calm"),
    SAD("Sad"),
    TIRED("Tired"),
    HAPPY("Happy"),
    STRESSED("Stressed")
}

data class EkgRecord(
    val id: String,
    val bpm: Int,
    val rhythm: String,
    val timestamp: String,
    val durationSeconds: Int = 30
)

data class DashboardUiState(
    val userName: String = "Alexandra",
    val selectedMood: MoodType = MoodType.CALM,
    val clinicalNotes: List<String> = emptyList(),
    val heartRate: Int = 70,
    val rhythmStatus: String = "REGULAR SINUS RHYTHM",
    val ekgTimestamp: String = "Oct 24, 11:00 pm",
    val bmi: Double = 20.0,
    val bmiStatus: String = "Optimal",
    val bodyFatPercentage: Double = 14.8,
    val bioImpedanceTimestamp: String = "Oct 24, 10:00 pm",
    val rrIntervalMs: Int = 872,
    val signalQuality: Int = 89,
    val isRecordingEkg: Boolean = false,
    val recordingProgress: Float = 0f,
    val recordingBpm: Int = 70,
    val isHistoryOpen: Boolean = false,
    val isAddNotesOpen: Boolean = false,
    val ekgHistory: List<EkgRecord> = listOf(
        EkgRecord("1", 70, "REGULAR SINUS RHYTHM", "Oct 24, 11:00 pm"),
        EkgRecord("2", 68, "NORMAL SINUS RHYTHM", "Oct 24, 08:30 am"),
        EkgRecord("3", 72, "REGULAR SINUS RHYTHM", "Oct 23, 10:15 pm"),
        EkgRecord("4", 65, "RESTING SINUS RHYTHM", "Oct 22, 11:45 pm")
    ),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val selectedRange: TimeRangeSelection = TimeRangeSelection.WEEK,
    val metrics: List<MetricOverview> = emptyList(),
    val hasPermissions: Boolean = false,
    val lastSyncMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val vitalsRepository: VitalsRepository,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var recordingJob: Job? = null

    init {
        checkPermissionsAndLoad()
    }

    fun checkPermissionsAndLoad() {
        viewModelScope.launch {
            val hasPerms = healthConnectManager.hasAllPermissions()
            _uiState.update { it.copy(hasPermissions = hasPerms) }
            loadData(_uiState.value.selectedRange)
        }
    }

    fun selectRange(range: TimeRangeSelection) {
        _uiState.update { it.copy(selectedRange = range) }
        loadData(range)
    }

    fun selectMood(mood: MoodType) {
        _uiState.update { it.copy(selectedMood = mood) }
    }

    fun openAddNotesDialog(open: Boolean) {
        _uiState.update { it.copy(isAddNotesOpen = open) }
    }

    fun addClinicalNote(note: String) {
        if (note.isNotBlank()) {
            _uiState.update {
                it.copy(
                    clinicalNotes = listOf(note.trim()) + it.clinicalNotes,
                    isAddNotesOpen = false
                )
            }
        }
    }

    fun openHistory(open: Boolean) {
        _uiState.update { it.copy(isHistoryOpen = open) }
    }

    fun startEkgRecording() {
        recordingJob?.cancel()
        _uiState.update {
            it.copy(
                isRecordingEkg = true,
                recordingProgress = 0f,
                recordingBpm = 70
            )
        }

        recordingJob = viewModelScope.launch {
            val totalSteps = 60
            for (i in 1..totalSteps) {
                delay(500)
                val progress = i / totalSteps.toFloat()
                val liveBpm = 68 + (i % 5)
                _uiState.update {
                    it.copy(
                        recordingProgress = progress,
                        recordingBpm = liveBpm
                    )
                }
            }

            // Finish recording
            val formatter = DateTimeFormatter.ofPattern("MMM dd, hh:mm a")
                .withZone(ZoneId.systemDefault())
            val nowStr = formatter.format(Instant.now()).lowercase()
            val newRecord = EkgRecord(
                id = UUID.randomUUID().toString(),
                bpm = 70,
                rhythm = "REGULAR SINUS RHYTHM",
                timestamp = nowStr
            )

            _uiState.update {
                it.copy(
                    isRecordingEkg = false,
                    recordingProgress = 1f,
                    heartRate = 70,
                    ekgTimestamp = nowStr,
                    ekgHistory = listOf(newRecord) + it.ekgHistory
                )
            }
        }
    }

    fun cancelEkgRecording() {
        recordingJob?.cancel()
        _uiState.update { it.copy(isRecordingEkg = false, recordingProgress = 0f) }
    }

    fun loadData(range: TimeRangeSelection) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val end = Instant.now()
                val start = end.minus(Duration.ofDays(range.days))

                val metricTypes = listOf(
                    Triple("HEART_RATE", "Heart Rate", "bpm"),
                    Triple("HRV", "HRV (RMSSD)", "ms"),
                    Triple("STEPS", "Steps", "steps"),
                    Triple("SLEEP", "Sleep Duration", "hrs"),
                    Triple("CALORIES", "Active Calories", "kcal"),
                    Triple("OXYGEN_SATURATION", "SpO2", "%")
                )

                val overviews = metricTypes.map { (type, title, unit) ->
                    val points = vitalsRepository.getVitalsBetween(type, start, end)
                    val values = points.map { it.value }
                    val latest = points.lastOrNull()?.value
                    val avg = if (values.isNotEmpty()) values.average() else null

                    MetricOverview(
                        metricType = type,
                        title = title,
                        unit = unit,
                        latestValue = latest,
                        averageValue = avg,
                        trend = null,
                        history = points
                    )
                }

                // If real heart rate exists in DB, update latest heart rate
                val hrOverview = overviews.find { it.metricType == "HEART_RATE" }
                val hrVal = hrOverview?.latestValue?.toInt()
                val updatedHr = if (hrVal != null && hrVal > 0) hrVal else _uiState.value.heartRate
                val updatedRr = if (updatedHr > 0) (60000 / updatedHr) else _uiState.value.rrIntervalMs

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        metrics = overviews,
                        heartRate = updatedHr,
                        rrIntervalMs = updatedRr
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load vitals"
                    )
                }
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, error = null) }
            try {
                val result = vitalsRepository.syncFromHealthConnect(_uiState.value.selectedRange.days)
                val msg = if (result.errors.isEmpty()) {
                    "Synced ${result.recordsSynced} records"
                } else {
                    "Sync finished with issues: ${result.errors.firstOrNull()}"
                }
                _uiState.update { it.copy(isSyncing = false, lastSyncMessage = msg) }
                loadData(_uiState.value.selectedRange)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        error = "Sync failed: ${e.message}"
                    )
                }
            }
        }
    }
}
