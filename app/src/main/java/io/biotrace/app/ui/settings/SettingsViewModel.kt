package io.biotrace.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.biotrace.app.data.llm.GeminiClient
import io.biotrace.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val hasApiKey: Boolean = false,
    val isTesting: Boolean = false,
    val isSaving: Boolean = false,
    val testMessage: String? = null,
    val isTestSuccess: Boolean? = null,
    val statusMessage: String? = null,
    val showClearConfirmDialog: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            val hasKey = settingsRepository.hasApiKey(GeminiClient.PROVIDER_NAME)
            _uiState.update { it.copy(hasApiKey = hasKey) }
        }
    }

    fun saveApiKey(key: String) {
        if (key.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, statusMessage = null) }
            try {
                settingsRepository.setApiKey(GeminiClient.PROVIDER_NAME, key.trim())
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        hasApiKey = true,
                        statusMessage = "API key saved securely in Android Keystore"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        statusMessage = "Failed to save key: ${e.message}"
                    )
                }
            }
        }
    }

    fun testConnection(key: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, testMessage = null, isTestSuccess = null) }
            val keyToTest = if (key.isNotBlank()) key.trim() else settingsRepository.getApiKey(GeminiClient.PROVIDER_NAME)
            if (keyToTest.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        isTesting = false,
                        testMessage = "Please enter an API key first",
                        isTestSuccess = false
                    )
                }
                return@launch
            }

            val result = settingsRepository.testApiKey(keyToTest)
            result.fold(
                onSuccess = { msg ->
                    _uiState.update {
                        it.copy(
                            isTesting = false,
                            testMessage = "✓ $msg",
                            isTestSuccess = true
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isTesting = false,
                            testMessage = "✗ ${err.message}",
                            isTestSuccess = false
                        )
                    }
                }
            )
        }
    }

    fun deleteApiKey() {
        viewModelScope.launch {
            settingsRepository.deleteApiKey(GeminiClient.PROVIDER_NAME)
            _uiState.update {
                it.copy(
                    hasApiKey = false,
                    statusMessage = "API key deleted",
                    testMessage = null,
                    isTestSuccess = null
                )
            }
        }
    }

    fun showClearConfirm(show: Boolean) {
        _uiState.update { it.copy(showClearConfirmDialog = show) }
    }

    fun clearAllData() {
        viewModelScope.launch {
            settingsRepository.clearAllData()
            _uiState.update {
                it.copy(
                    showClearConfirmDialog = false,
                    statusMessage = "All local data has been cleared"
                )
            }
        }
    }
}
