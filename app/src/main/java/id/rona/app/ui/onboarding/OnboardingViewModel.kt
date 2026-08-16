package id.rona.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.rona.app.data.repository.OnboardingRepository
import id.rona.app.data.repository.AppLockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class OnboardingStep {
    WELCOME,
    LOCK_SETUP,
    LAST_PERIOD,
    CYCLE_LENGTH,
    NOTIFICATIONS,
    DONE,
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val isLoading: Boolean = true,
    val isOnboardingCompleted: Boolean = false,
    val lastPeriodStart: LocalDate? = null,
    val lastPeriodEnd: LocalDate? = null,
    val defaultCycleLengthDays: Int? = null,
    val cycleLengthKnown: Boolean = false,
    val pin: String = "",
    val pinConfirmed: Boolean = false,
    val pinError: String? = null,
    val notificationsEnabled: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
    private val appLockRepository: AppLockRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val completed = onboardingRepository.isOnboardingCompleted()
            _uiState.update { it.copy(isLoading = false, isOnboardingCompleted = completed) }
        }
    }

    fun nextStep() {
        _uiState.update {
            it.copy(step = when (it.step) {
                OnboardingStep.WELCOME -> OnboardingStep.LOCK_SETUP
                OnboardingStep.LOCK_SETUP -> OnboardingStep.LAST_PERIOD
                OnboardingStep.LAST_PERIOD -> OnboardingStep.CYCLE_LENGTH
                OnboardingStep.CYCLE_LENGTH -> OnboardingStep.NOTIFICATIONS
                OnboardingStep.NOTIFICATIONS -> OnboardingStep.DONE
                OnboardingStep.DONE -> OnboardingStep.DONE
            })
        }
    }

    fun setLastPeriodStart(date: LocalDate) {
        _uiState.update { it.copy(lastPeriodStart = date, error = null) }
    }

    fun setLastPeriodEnd(date: LocalDate?) {
        _uiState.update { it.copy(lastPeriodEnd = date) }
    }

    fun setCycleLength(days: Int?) {
        _uiState.update { it.copy(defaultCycleLengthDays = days, cycleLengthKnown = days != null) }
    }

    fun setPin(pin: String) {
        _uiState.update {
            it.copy(
                pin = pin,
                pinConfirmed = false,
                pinError = when {
                    pin.isBlank() -> null
                    pin.length !in 4..6 -> "PIN harus 4–6 digit"
                    pin.any { !it.isDigit() } -> "PIN hanya boleh angka"
                    else -> null
                },
            )
        }
    }

    fun confirmPin() {
        val state = _uiState.value
        if (state.pinError != null || state.pin.length !in 4..6) return
        _uiState.update { it.copy(pinConfirmed = true) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    fun completeOnboarding() {
        val state = _uiState.value
        val start = state.lastPeriodStart
        if (start == null) {
            _uiState.update { it.copy(error = "Tanggal hari pertama menstruasi wajib diisi.") }
            return
        }
        if (state.pin.isNotBlank() && !state.pinConfirmed) {
            _uiState.update { it.copy(error = "Konfirmasi PIN terlebih dahulu.") }
            return
        }
        viewModelScope.launch {
            if (state.pin.isNotBlank()) {
                appLockRepository.setPin(state.pin)
            }
            onboardingRepository.completeOnboarding(
                lastPeriodStart = start,
                lastPeriodEnd = state.lastPeriodEnd,
                defaultCycleLengthDays = state.defaultCycleLengthDays,
            )
            _uiState.update { it.copy(isOnboardingCompleted = true) }
        }
    }
}
