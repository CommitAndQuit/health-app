package io.biotrace.app.ui.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.biotrace.app.data.healthconnect.HealthConnectAvailability
import io.biotrace.app.data.healthconnect.HealthConnectManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PermissionsUiState(
    val availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
    val hasAllPermissions: Boolean = false,
    val grantedPermissions: Set<String> = emptySet(),
    val requiredPermissions: Set<String> = emptySet()
)

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionsUiState())
    val uiState: StateFlow<PermissionsUiState> = _uiState.asStateFlow()

    init {
        checkPermissions()
    }

    fun checkPermissions() {
        viewModelScope.launch {
            val availability = healthConnectManager.checkAvailability()
            val hasAll = healthConnectManager.hasAllPermissions()
            val granted = healthConnectManager.getGrantedPermissions()
            val required = healthConnectManager.requiredPermissions

            _uiState.update {
                it.copy(
                    availability = availability,
                    hasAllPermissions = hasAll,
                    grantedPermissions = granted,
                    requiredPermissions = required
                )
            }
        }
    }

    fun onPermissionsResult(granted: Set<String>) {
        checkPermissions()
    }
}
