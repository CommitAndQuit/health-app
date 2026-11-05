package io.biotrace.app.ui.vitals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.biotrace.app.domain.model.MetricTimeRange
import io.biotrace.app.domain.model.VitalDetailUiState
import io.biotrace.app.domain.model.VitalMetricType
import io.biotrace.app.domain.repository.VitalsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VitalDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vitalsRepository: VitalsRepository
) : ViewModel() {

    private val metricTypeArg: String = checkNotNull(savedStateHandle["metricType"])
    val metricType: VitalMetricType = runCatching {
        VitalMetricType.valueOf(metricTypeArg)
    }.getOrDefault(VitalMetricType.HEART_RATE)

    private val _uiState = MutableStateFlow(
        VitalDetailUiState(
            metricType = metricType,
            isLoading = true
        )
    )
    val uiState: StateFlow<VitalDetailUiState> = _uiState.asStateFlow()

    init {
        loadData(MetricTimeRange.DAY)
    }

    fun selectRange(range: MetricTimeRange) {
        if (_uiState.value.selectedRange == range && !_uiState.value.isLoading) return
        loadData(range)
    }

    fun refresh() {
        loadData(_uiState.value.selectedRange)
    }

    private fun loadData(range: MetricTimeRange) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, selectedRange = range) }
            try {
                val data = vitalsRepository.getMetricDetailData(metricType, range)
                _uiState.value = data.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to load metric details"
                    )
                }
            }
        }
    }
}
