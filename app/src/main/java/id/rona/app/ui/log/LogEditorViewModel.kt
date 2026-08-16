package id.rona.app.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.rona.app.data.repository.DailyLogRepository
import id.rona.app.domain.model.Energy
import id.rona.app.domain.model.FlowLevel
import id.rona.app.domain.model.Mood
import id.rona.app.domain.model.Severity
import id.rona.app.domain.model.SymptomType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class LogEditorUiState(
    val date: LocalDate = LocalDate.now(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val flow: FlowLevel? = null,
    val selectedSymptoms: Map<SymptomType, Severity> = emptyMap(),
    val mood: Mood? = null,
    val energy: Energy? = null,
    val note: String = "",
    val error: String? = null,
)

@HiltViewModel
class LogEditorViewModel @Inject constructor(
    private val repository: DailyLogRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogEditorUiState())
    val uiState: StateFlow<LogEditorUiState> = _uiState.asStateFlow()

    fun load(date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, date = date) }
            val log = repository.getForDate(date)
            if (log != null) {
                val symptoms = repository.getSymptomsForLog(log.id)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        flow = log.flow,
                        mood = log.mood,
                        energy = log.energy,
                        note = log.note.orEmpty(),
                        selectedSymptoms = symptoms.associate { s -> s.symptomType to s.severity },
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun selectFlow(flow: FlowLevel?) {
        _uiState.update { it.copy(flow = flow) }
    }

    fun toggleSymptom(symptom: SymptomType, severity: Severity = Severity.MILD) {
        _uiState.update { state ->
            val updated = if (state.selectedSymptoms.containsKey(symptom)) {
                state.selectedSymptoms - symptom
            } else {
                state.selectedSymptoms + (symptom to severity)
            }
            state.copy(selectedSymptoms = updated)
        }
    }

    fun setSymptomSeverity(symptom: SymptomType, severity: Severity) {
        _uiState.update { state ->
            if (state.selectedSymptoms.containsKey(symptom)) {
                state.copy(selectedSymptoms = state.selectedSymptoms + (symptom to severity))
            } else {
                state
            }
        }
    }

    fun selectMood(mood: Mood?) {
        _uiState.update { it.copy(mood = mood) }
    }

    fun selectEnergy(energy: Energy?) {
        _uiState.update { it.copy(energy = energy) }
    }

    fun setNote(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                repository.save(
                    date = state.date,
                    flow = state.flow,
                    mood = state.mood,
                    energy = state.energy,
                    note = state.note,
                    symptoms = state.selectedSymptoms.toList(),
                )
                _uiState.update { it.copy(isSaving = false, saved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Gagal menyimpan. Coba lagi.") }
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            repository.delete(_uiState.value.date)
            _uiState.update { it.copy(saved = true) }
        }
    }
}
