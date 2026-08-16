package id.rona.app.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.rona.app.data.db.dao.NlpSuggestionDao
import id.rona.app.data.db.dao.PeriodRecordDao
import id.rona.app.data.db.entity.NlpSuggestionEntity
import id.rona.app.data.repository.DailyLogRepository
import id.rona.app.domain.engine.CycleEngine
import id.rona.app.domain.engine.CyclePhase
import id.rona.app.domain.engine.StatisticsEngine
import id.rona.app.domain.model.Energy
import id.rona.app.domain.model.FlowLevel
import id.rona.app.domain.model.Mood
import id.rona.app.domain.model.NlpStatus
import id.rona.app.domain.model.Severity
import id.rona.app.domain.model.SymptomType
import id.rona.app.domain.nlp.CurrentLogValues
import id.rona.app.domain.nlp.LocalNoteAnalyzer
import id.rona.app.domain.nlp.NoteAnalysisInput
import id.rona.app.domain.nlp.NoteAnalysisResult
import id.rona.app.util.PrivacyLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
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
    val isAnalyzing: Boolean = false,
    val analysisResult: NoteAnalysisResult? = null,
    val showAnalysisResult: Boolean = false,
    val safetyAlertsAcknowledged: Boolean = false,
)

@HiltViewModel
class LogEditorViewModel @Inject constructor(
    private val repository: DailyLogRepository,
    private val localNoteAnalyzer: LocalNoteAnalyzer,
    private val periodRecordDao: PeriodRecordDao,
    private val nlpSuggestionDao: NlpSuggestionDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogEditorUiState())
    val uiState: StateFlow<LogEditorUiState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

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

    /**
     * Runs local analysis on the private note. Triggered ONLY by the explicit
     * "Analisis catatan" button — never automatically, never in background.
     * The raw note never leaves this coroutine and is never logged.
     */
    fun runAnalysis() {
        val state = _uiState.value
        if (state.note.isBlank()) return
        if (state.isAnalyzing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, showAnalysisResult = false) }
            val analysis = localNoteAnalyzer.analyze(
                NoteAnalysisInput(
                    noteText = state.note,
                    cycleDay = currentCycleDay(),
                    currentPhase = currentPhase(),
                    isPeriodActive = isPeriodActive(),
                    isPregnancyModeEnabled = false,
                    currentLogValues = CurrentLogValues(
                        selectedSymptomTypes = state.selectedSymptoms.keys,
                        flow = state.flow,
                        mood = state.mood,
                        energy = state.energy,
                    ),
                    analysisTriggeredByUser = true,
                )
            )
            PrivacyLogger.d("LogEditor") { "local analysis done: ${analysis.symptomSuggestions.size} symptoms" }
            _uiState.update {
                it.copy(
                    isAnalyzing = false,
                    analysisResult = analysis,
                    showAnalysisResult = true,
                )
            }
        }
    }

    fun dismissAnalysisResult() {
        _uiState.update { it.copy(showAnalysisResult = false, analysisResult = null) }
    }

    fun acknowledgeSafetyAlerts() {
        _uiState.update { it.copy(safetyAlertsAcknowledged = true) }
    }

    /**
     * Applies user-confirmed suggestions into editor state. Nothing is saved
     * to the database here — the user still presses "Simpan" like always.
     */
    fun applySuggestions() {
        val result = _uiState.value.analysisResult ?: return
        _uiState.update { state ->
            var symptoms = state.selectedSymptoms
            result.symptomSuggestions.forEach { suggestion ->
                if (!symptoms.containsKey(suggestion.symptomType)) {
                    symptoms = symptoms + (suggestion.symptomType to (suggestion.severity ?: Severity.MILD))
                }
            }
            state.copy(
                selectedSymptoms = symptoms,
                flow = state.flow ?: result.flowSuggestion?.flow,
                mood = state.mood ?: result.moodSuggestions.firstOrNull()?.mood,
                energy = state.energy ?: result.energySuggestion?.energy,
                showAnalysisResult = false,
                analysisResult = result,
            )
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                val logId = repository.saveWithId(
                    date = state.date,
                    flow = state.flow,
                    mood = state.mood,
                    energy = state.energy,
                    note = state.note,
                    symptoms = state.selectedSymptoms.toList(),
                )
                auditAcceptedSuggestions(state, logId)
                _uiState.update { it.copy(isSaving = false, saved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Gagal menyimpan. Coba lagi.") }
            }
        }
    }

    /**
     * Minimal audit metadata when the user actually saved the log. No raw
     * note text, no matched text, no health narrative — only structured
     * accepted types + rule version + timestamp.
     */
    private suspend fun auditAcceptedSuggestions(state: LogEditorUiState, logId: Long) {
        val result = state.analysisResult ?: return
        val acceptedTypes = result.symptomSuggestions
            .filter { it.symptomType in state.selectedSymptoms }
            .map { it.symptomType.name }
        if (acceptedTypes.isEmpty()) return
        nlpSuggestionDao.upsert(
            NlpSuggestionEntity(
                dailyLogId = logId,
                source = "RULES",
                modelVersion = result.ruleVersion,
                payloadJson = json.encodeToString(
                    serializer<List<String>>(),
                    acceptedTypes,
                ),
                status = NlpStatus.CONFIRMED,
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    fun delete() {
        viewModelScope.launch {
            repository.delete(_uiState.value.date)
            _uiState.update { it.copy(saved = true) }
        }
    }

    private suspend fun currentCycleDay(): Int? {
        val starts = periodRecordDao.getAll().map { LocalDate.ofEpochDay(it.startEpochDay) }
        return CycleEngine.cycleDayFor(LocalDate.now(), starts)
    }

    private suspend fun currentPhase(): CyclePhase? {
        val starts = periodRecordDao.getAll().map { LocalDate.ofEpochDay(it.startEpochDay) }
        val cycleDay = CycleEngine.cycleDayFor(LocalDate.now(), starts) ?: return null
        val median = CycleEngine.recentValidCycleLengths(starts)
            .takeIf { it.isNotEmpty() }
            ?.let { CycleEngine.median(it) } ?: return null
        return StatisticsEngine.phaseForCycleDay(cycleDay, median)
    }

    private suspend fun isPeriodActive(): Boolean =
        periodRecordDao.getOngoing() != null
}
