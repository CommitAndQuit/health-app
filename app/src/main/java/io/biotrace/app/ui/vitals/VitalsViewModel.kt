package io.biotrace.app.ui.vitals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.biotrace.app.data.healthconnect.HealthConnectAvailability
import io.biotrace.app.data.healthconnect.HealthConnectManager
import io.biotrace.app.domain.model.ContinuousHeartRateHeroData
import io.biotrace.app.domain.model.ProfileTriptychSummary
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
    val heroHeartRate: ContinuousHeartRateHeroData = ContinuousHeartRateHeroData(),
    val profileTriptych: ProfileTriptychSummary = ProfileTriptychSummary(),
    val isRefreshing: Boolean = false,
    val hasHealthConnectPermissions: Boolean = true,
    val availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
    val lastSyncTime: Instant? = null
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

            _uiState.update {
                it.copy(
                    availability = availability,
                    hasHealthConnectPermissions = hasPerms
                )
            }

            refreshDashboard()
        }
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                // Sync latest from Health Connect
                runCatching { vitalsRepository.syncFromHealthConnect(days = 7) }

                val cards = vitalsRepository.getVitalsDashboardSnapshot()
                val heroHr = vitalsRepository.getContinuousHeartRateHeroData()
                val profile = vitalsRepository.getProfileTriptychSummary()

                _uiState.update {
                    it.copy(
                        cards = cards,
                        heroHeartRate = heroHr,
                        profileTriptych = profile,
                        isRefreshing = false,
                        lastSyncTime = Instant.now()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }
}
