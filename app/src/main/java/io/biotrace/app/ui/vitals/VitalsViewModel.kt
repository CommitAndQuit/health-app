package io.biotrace.app.ui.vitals

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.biotrace.app.data.healthconnect.HealthConnectAvailability
import io.biotrace.app.data.healthconnect.HealthConnectManager
import io.biotrace.app.domain.model.VitalSummaryCardModel
import io.biotrace.app.domain.repository.VitalsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class VitalsUiState(
    val cards: List<VitalSummaryCardModel> = emptyList(),
    val isRefreshing: Boolean = false,
    val hasHealthConnectPermissions: Boolean = true,
    val availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
    val lastSyncTime: Instant? = null,
    val importMessage: String? = null,
    val hasSavedGadgetbridgeDb: Boolean = false
)

@HiltViewModel
class VitalsViewModel @Inject constructor(
    private val vitalsRepository: VitalsRepository,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VitalsUiState())
    val uiState: StateFlow<VitalsUiState> = _uiState.asStateFlow()

    init {
        checkStatusAndRefresh()
    }

    fun checkStatusAndRefresh() {
        viewModelScope.launch {
            val availability = healthConnectManager.checkAvailability()
            val hasPerms = healthConnectManager.hasAllPermissions()
            val hasDb = vitalsRepository.hasSavedGadgetbridgeUri()

            _uiState.update {
                it.copy(
                    availability = availability,
                    hasHealthConnectPermissions = hasPerms,
                    hasSavedGadgetbridgeDb = hasDb
                )
            }

            refreshDashboard()
        }
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, importMessage = null) }
            try {
                // Auto refresh Gadgetbridge stress if saved URI is present
                if (vitalsRepository.hasSavedGadgetbridgeUri()) {
                    runCatching { vitalsRepository.refreshGadgetbridgeStress() }
                }

                // Sync latest from Health Connect
                runCatching { vitalsRepository.syncFromHealthConnect(days = 7) }

                val cards = vitalsRepository.getVitalsDashboardSnapshot()
                _uiState.update {
                    it.copy(
                        cards = cards,
                        isRefreshing = false,
                        lastSyncTime = Instant.now(),
                        hasSavedGadgetbridgeDb = vitalsRepository.hasSavedGadgetbridgeUri()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun importGadgetbridgeDatabase(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val result = vitalsRepository.importGadgetbridgeUri(uri)
            result.fold(
                onSuccess = { count ->
                    val cards = vitalsRepository.getVitalsDashboardSnapshot()
                    _uiState.update {
                        it.copy(
                            cards = cards,
                            isRefreshing = false,
                            importMessage = "Successfully imported $count stress records from Gadgetbridge",
                            hasSavedGadgetbridgeDb = true,
                            lastSyncTime = Instant.now()
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            importMessage = "Import failed: ${error.localizedMessage ?: "Unknown error"}"
                        )
                    }
                }
            )
        }
    }

    fun clearImportMessage() {
        _uiState.update { it.copy(importMessage = null) }
    }
}
