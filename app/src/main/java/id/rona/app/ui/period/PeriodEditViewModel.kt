package id.rona.app.ui.period

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.rona.app.data.repository.PeriodRecordRepository
import id.rona.app.domain.model.OverlapResolutionStrategy
import id.rona.app.domain.model.PeriodRecord
import id.rona.app.domain.model.PeriodValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class PeriodEditUiState(
    val initialPeriodId: Long = 0L,
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val isOngoing: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val validationResult: PeriodValidationResult = PeriodValidationResult.Valid,
    val isSavedSuccessfully: Boolean = false,
    val isDeletedSuccessfully: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val showWarningConfirmation: Boolean = false,
    val showOverlapDialog: Boolean = false,
)

@HiltViewModel
class PeriodEditViewModel @Inject constructor(
    private val periodRecordRepository: PeriodRecordRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PeriodEditUiState())
    val uiState: StateFlow<PeriodEditUiState> = _uiState.asStateFlow()

    fun initialize(periodId: Long?, initialDate: LocalDate?) {
        viewModelScope.launch {
            if (periodId != null && periodId != 0L) {
                _uiState.update { it.copy(isLoading = true) }
                val record = periodRecordRepository.getPeriodById(periodId)
                if (record != null) {
                    _uiState.update {
                        it.copy(
                            initialPeriodId = record.id,
                            startDate = record.startDate,
                            endDate = record.endDate,
                            isOngoing = record.endDate == null,
                            isLoading = false,
                        )
                    }
                    return@launch
                }
            }

            // New record initialized with a given date or today
            val defaultStart = initialDate ?: LocalDate.now()
            _uiState.update {
                it.copy(
                    initialPeriodId = 0L,
                    startDate = defaultStart,
                    endDate = null,
                    isOngoing = true,
                    isLoading = false,
                )
            }
        }
    }

    fun setStartDate(date: LocalDate) {
        _uiState.update { it.copy(startDate = date, errorMessage = null) }
    }

    fun setEndDate(date: LocalDate?) {
        _uiState.update { it.copy(endDate = date, isOngoing = date == null, errorMessage = null) }
    }

    fun setOngoing(ongoing: Boolean) {
        _uiState.update {
            it.copy(
                isOngoing = ongoing,
                endDate = if (ongoing) null else it.startDate.plusDays(4),
                errorMessage = null,
            )
        }
    }

    fun requestSave() {
        val state = _uiState.value
        val effectiveEnd = if (state.isOngoing) null else state.endDate

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val validation = periodRecordRepository.validatePeriod(
                start = state.startDate,
                end = effectiveEnd,
                currentRecordId = state.initialPeriodId,
            )

            when (validation) {
                is PeriodValidationResult.InvalidDateOrder -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = validation.message,
                            validationResult = validation,
                        )
                    }
                }
                is PeriodValidationResult.OverlapConflict -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            validationResult = validation,
                            showOverlapDialog = true,
                        )
                    }
                }
                is PeriodValidationResult.UnusualDurationWarning -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            validationResult = validation,
                            showWarningConfirmation = true,
                        )
                    }
                }
                PeriodValidationResult.Valid -> {
                    executeSave(effectiveEnd = effectiveEnd, strategy = null)
                }
            }
        }
    }

    fun confirmSaveDespiteWarning() {
        _uiState.update { it.copy(showWarningConfirmation = false) }
        val state = _uiState.value
        val effectiveEnd = if (state.isOngoing) null else state.endDate
        executeSave(effectiveEnd = effectiveEnd, strategy = null)
    }

    fun resolveOverlapAndSave(strategy: OverlapResolutionStrategy) {
        _uiState.update { it.copy(showOverlapDialog = false) }
        val state = _uiState.value
        val effectiveEnd = if (state.isOngoing) null else state.endDate
        executeSave(effectiveEnd = effectiveEnd, strategy = strategy)
    }

    private fun executeSave(effectiveEnd: LocalDate?, strategy: OverlapResolutionStrategy?) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = periodRecordRepository.savePeriod(
                start = state.startDate,
                end = effectiveEnd,
                currentRecordId = state.initialPeriodId,
                resolutionStrategy = strategy,
            )

            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, isSavedSuccessfully = true) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Gagal menyimpan catatan.",
                    )
                }
            }
        }
    }

    fun showDeleteDialog() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun dismissWarningDialog() {
        _uiState.update { it.copy(showWarningConfirmation = false) }
    }

    fun dismissOverlapDialog() {
        _uiState.update { it.copy(showOverlapDialog = false) }
    }

    fun confirmDelete() {
        val id = _uiState.value.initialPeriodId
        if (id == 0L) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showDeleteConfirmation = false) }
            val result = periodRecordRepository.deletePeriod(id)
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, isDeletedSuccessfully = true) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Gagal menghapus catatan.",
                    )
                }
            }
        }
    }
}
