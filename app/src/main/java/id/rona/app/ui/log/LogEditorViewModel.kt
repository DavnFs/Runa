package id.rona.app.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.rona.app.data.db.dao.NlpSuggestionDao
import id.rona.app.data.db.dao.PeriodRecordDao
import id.rona.app.data.db.entity.NlpSuggestionEntity
import id.rona.app.data.repository.DailyLogRepository
import id.rona.app.data.repository.PeriodRecordRepository
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

/**
 * Immutable UI state for the log editor.
 *
 * `saveState` replaces the prior `isSaving`/`saved`/`error` triple so that a
 * successful save is a *transient* [SaveState.Saved] rather than a sticky
 * `true` flag that would auto-close a reopened session.
 *
 * There is never a fabricated value in here: mood/flow/energy/symptoms/note are
 * only non-null when the user explicitly selected them on a given step.
 */
data class LogEditorUiState(
    val date: LocalDate = LocalDate.now(),
    val isLoading: Boolean = false,
    val saveState: SaveState = SaveState.Idle,
    val flow: FlowLevel? = null,
    val selectedSymptoms: Map<SymptomType, Severity> = emptyMap(),
    val mood: Mood? = null,
    val energy: Energy? = null,
    val note: String = "",
    val isAnalyzing: Boolean = false,
    val analysisResult: NoteAnalysisResult? = null,
    val showAnalysisResult: Boolean = false,
    val safetyAlertsAcknowledged: Boolean = false,
    val currentStep: DailyCheckInStep = DailyCheckInStep.Initial,
    val stepHistory: List<DailyCheckInStep> = listOf(DailyCheckInStep.Initial),
    /** True when a period record covers [date] at session-load time (used for the period-context branch). */
    val isPeriodContextAvailable: Boolean = false,
    /** True while a new PeriodRecord is being created via confirmPeriodStart. */
    val isStartingPeriod: Boolean = false,
) {
    /** Convenience for views that predate the sealed SaveState. */
    val error: String? get() = (saveState as? SaveState.Error)?.message
    /** Convenience for views that predate the sealed SaveState. */
    val isSaving: Boolean get() = saveState is SaveState.Saving
    /** Convenience for views that predate the sealed SaveState. */
    val saved: Boolean get() = saveState == SaveState.Saved
}

@HiltViewModel
class LogEditorViewModel @Inject constructor(
    private val repository: DailyLogRepository,
    private val localNoteAnalyzer: LocalNoteAnalyzer,
    private val periodRecordDao: PeriodRecordDao,
    private val periodRecordRepository: PeriodRecordRepository,
    private val nlpSuggestionDao: NlpSuggestionDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogEditorUiState())
    val uiState: StateFlow<LogEditorUiState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }
    private var loadedSessionIdentity: Pair<Long, LocalDate>? = null

    /**
     * Start (or restart) a session for [date].
     *
     * Fully resets every editorial field and the step history so that reopening
     * after a prior save (or switching days) never resurrects stale answers or
     * skips back to a terminal state. If an existing log exists for [date], its
     * persisted values are loaded back in.
     */
    fun load(sessionId: Long, date: LocalDate = LocalDate.now()) {
        if (loadedSessionIdentity == sessionId to date) return
        loadedSessionIdentity = sessionId to date
        viewModelScope.launch {
            // Fresh session: wipe everything, then hydrate from persistence if present.
            _uiState.value = LogEditorUiState(
                date = date,
                isLoading = true,
                isPeriodContextAvailable = periodRecordDao.getActiveOn(date.toEpochDay()) != null,
            )

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

    // ───────────────────────── State Machine Navigation ─────────────────────────

    fun selectInitialChoice(option: InitialChoiceOption) {
        when (option) {
            InitialChoiceOption.FEELING_GOOD -> selectFeelingOk()
            InitialChoiceOption.PERIOD_FLOW -> {
                // Branch on period context: if a period already covers this day,
                // record the flow directly; otherwise confirm a new period start.
                if (_uiState.value.isPeriodContextAvailable) {
                    navigateToStep(DailyCheckInStep.Flow)
                } else {
                    navigateToStep(DailyCheckInStep.PeriodStartConfirm)
                }
            }
            InitialChoiceOption.SYMPTOMS -> navigateToStep(DailyCheckInStep.Symptoms)
            InitialChoiceOption.ENERGY_MOOD -> navigateToStep(DailyCheckInStep.Energy)
        }
    }

    /**
     * "Baik-baik saja" fast path. Records NO fabricated wellness value: just
     * optionally asks the user to capture energy and then lands on Summary.
     */
    fun selectFeelingOk() {
        navigateToStep(DailyCheckInStep.QuickEnergy)
    }

    /** Confirm creating a new PeriodRecord starting on the active day, then continue to Flow. */
    fun confirmPeriodStart() {
        val targetDate = _uiState.value.date
        if (_uiState.value.isPeriodContextAvailable) {
            // An ongoing period materialized since session load (race/external); proceed to Flow.
            _uiState.update { it.copy(isStartingPeriod = false) }
            navigateToStep(DailyCheckInStep.Flow)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isStartingPeriod = true) }
            try {
                periodRecordRepository.savePeriod(start = targetDate, end = null)
                _uiState.update {
                    it.copy(
                        isStartingPeriod = false,
                        isPeriodContextAvailable = true,
                        saveState = SaveState.Idle,
                    )
                }
                PrivacyLogger.d("LogEditor") { "started new period for $targetDate" }
                navigateToStep(DailyCheckInStep.Flow)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isStartingPeriod = false,
                        saveState = SaveState.Error("Gagal memulai periode. Coba lagi."),
                    )
                }
            }
        }
    }

    /** Back out of period-start confirmation: return to Initial, create nothing. */
    fun cancelPeriodStart() {
        previousStep()
    }

    fun nextStep() {
        val state = _uiState.value
        val next = when (state.currentStep) {
            DailyCheckInStep.Initial -> DailyCheckInStep.Flow
            // The quick path optionally skips energy capture.
            DailyCheckInStep.QuickEnergy -> DailyCheckInStep.Summary
            DailyCheckInStep.PeriodStartConfirm -> DailyCheckInStep.PeriodStartConfirm
            DailyCheckInStep.Flow -> DailyCheckInStep.Symptoms
            DailyCheckInStep.Symptoms -> {
                if (state.selectedSymptoms.isNotEmpty()) {
                    DailyCheckInStep.Severity
                } else {
                    DailyCheckInStep.Energy
                }
            }
            DailyCheckInStep.Severity -> DailyCheckInStep.Energy
            // Energy reached from the quick path continues straight to Summary.
            DailyCheckInStep.Energy -> {
                if (state.stepHistory.lastIndex >= 0 &&
                    state.stepHistory.contains(DailyCheckInStep.QuickEnergy)
                ) {
                    DailyCheckInStep.Summary
                } else {
                    DailyCheckInStep.Note
                }
            }
            DailyCheckInStep.Note -> DailyCheckInStep.Summary
            DailyCheckInStep.Summary -> DailyCheckInStep.Summary
        }
        navigateToStep(next)
    }

    fun skipStep() {
        val state = _uiState.value
        when (state.currentStep) {
            DailyCheckInStep.Initial -> navigateToStep(DailyCheckInStep.Flow)
            DailyCheckInStep.QuickEnergy -> {
                // Skip energy capture: saves no fabricated value, go straight to Summary.
                navigateToStep(DailyCheckInStep.Summary)
            }
            DailyCheckInStep.PeriodStartConfirm -> {
                // Decline creating a period: back to Initial, persist nothing.
                previousStep()
            }
            DailyCheckInStep.Flow -> {
                selectFlow(null)
                navigateToStep(DailyCheckInStep.Symptoms)
            }
            DailyCheckInStep.Symptoms -> {
                _uiState.update { it.copy(selectedSymptoms = emptyMap()) }
                navigateToStep(DailyCheckInStep.Energy)
            }
            DailyCheckInStep.Severity -> {
                navigateToStep(DailyCheckInStep.Energy)
            }
            DailyCheckInStep.Energy -> {
                navigateToStep(DailyCheckInStep.Note)
            }
            DailyCheckInStep.Note -> {
                navigateToStep(DailyCheckInStep.Summary)
            }
            DailyCheckInStep.Summary -> {
                // already at summary
            }
        }
    }

    fun previousStep() {
        _uiState.update { state ->
            if (state.stepHistory.size > 1) {
                val updatedHistory = state.stepHistory.dropLast(1)
                val prevStep = updatedHistory.last()
                state.copy(
                    currentStep = prevStep,
                    stepHistory = updatedHistory,
                )
            } else {
                state.copy(
                    currentStep = DailyCheckInStep.Initial,
                    stepHistory = listOf(DailyCheckInStep.Initial),
                )
            }
        }
    }

    fun navigateToStep(step: DailyCheckInStep) {
        _uiState.update { state ->
            if (state.currentStep == step) return@update state
            val updatedHistory = state.stepHistory + step
            state.copy(
                currentStep = step,
                stepHistory = updatedHistory,
            )
        }
    }

    fun resetStep() {
        _uiState.update {
            it.copy(
                currentStep = DailyCheckInStep.Initial,
                stepHistory = listOf(DailyCheckInStep.Initial),
            )
        }
    }

    // ───────────────────────── Field Mutators ─────────────────────────

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

    // ───────────────────────── NLP Analysis ─────────────────────────

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

    // ───────────────────────── Database Operations ─────────────────────────

    fun save() {
        val state = _uiState.value
        if (state.saveState is SaveState.Saving) return
        viewModelScope.launch {
            _uiState.update { it.copy(saveState = SaveState.Saving) }
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
                _uiState.update { it.copy(saveState = SaveState.Saved) }
            } catch (e: Exception) {
                _uiState.update { it.copy(saveState = SaveState.Error("Gagal menyimpan. Coba lagi.")) }
            }
        }
    }

    /**
     * Acknowledges a transient [SaveState.Saved] and resets the session to a
     * clean idle state. After this returns, reopening the sheet starts fresh
     * (Initial card, cleared fields) — no stale saved/auto-close behaviour.
     */
    fun acknowledgeSaved() {
        _uiState.update {
            it.copy(
                saveState = SaveState.Idle,
                currentStep = DailyCheckInStep.Initial,
                stepHistory = listOf(DailyCheckInStep.Initial),
                flow = null,
                selectedSymptoms = emptyMap(),
                mood = null,
                energy = null,
                note = "",
                isPeriodContextAvailable = false,
            )
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
            _uiState.update { it.copy(saveState = SaveState.Saved) }
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
