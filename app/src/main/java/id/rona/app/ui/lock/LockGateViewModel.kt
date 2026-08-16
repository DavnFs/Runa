package id.rona.app.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.rona.app.data.AppLockManager
import id.rona.app.data.repository.AppLockRepository
import id.rona.app.domain.model.LockOutcome
import id.rona.app.util.PrivacyLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LockGateUiState(
    val isLoading: Boolean = true,
    val biometricAvailable: Boolean = false,
    val isLocked: Boolean = true,
    val pinLength: Int = 4,
    val attemptsLeft: Int = LockGateViewModel.MAX_ATTEMPTS,
    val lockoutRemainingSeconds: Int = 0,
    val showForgotPinDialog: Boolean = false,
)

@HiltViewModel
class LockGateViewModel @Inject constructor(
    private val appLockManager: AppLockManager,
    private val lockRepository: AppLockRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LockGateUiState())
    val uiState: StateFlow<LockGateUiState> = _uiState.asStateFlow()

    private val _outcome = MutableStateFlow<LockOutcome?>(null)
    val outcome: StateFlow<LockOutcome?> = _outcome.asStateFlow()

    private var failures = 0

    init {
        viewModelScope.launch {
            val settings = lockRepository.getLockSettings()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    biometricAvailable = settings?.biometricEnabled == true,
                    isLocked = settings?.lockEnabled == true,
                    pinLength = settings?.pinLength ?: 4,
                )
            }
        }
    }

    fun onUnlockSuccess() {
        appLockManager.onUnlocked()
        _outcome.value = LockOutcome.UNLOCKED
    }

    fun onBiometricFailure() {
        _outcome.value = LockOutcome.AUTHENTICATION_FAILED
    }

    fun submitPin(pin: String) {
        viewModelScope.launch {
            val ok = lockRepository.verifyPin(pin)
            if (ok) {
                failures = 0
                onUnlockSuccess()
            } else {
                failures++
                if (failures >= MAX_ATTEMPTS) {
                    _outcome.value = LockOutcome.LOCKOUT
                } else {
                    _uiState.update { it.copy(attemptsLeft = MAX_ATTEMPTS - failures) }
                    _outcome.value = LockOutcome.PIN_INCORRECT
                }
            }
        }
    }

    fun onForgotPinRequested() {
        _uiState.update { it.copy(showForgotPinDialog = true) }
    }

    fun dismissForgotPinDialog() {
        _uiState.update { it.copy(showForgotPinDialog = false) }
    }

    fun onOutcomeHandled() {
        _outcome.value = null
    }

    companion object {
        const val MAX_ATTEMPTS = 5
    }
}
