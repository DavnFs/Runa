package id.rona.app.domain.nlp

import id.rona.app.domain.engine.CyclePhase

/**
 * The single input contract for P1 note analysis. Pure Kotlin, no Android imports.
 *
 * All fields are already available in the existing LogEditorViewModel; nothing
 * is ever read from network. [analysisTriggeredByUser] is always true in P1 —
 * the engine refuses to run otherwise.
 */
data class NoteAnalysisInput(
    val noteText: String,
    val cycleDay: Int?,
    val currentPhase: CyclePhase?,
    val isPeriodActive: Boolean,
    val isPregnancyModeEnabled: Boolean,
    val currentLogValues: CurrentLogValues? = null,
    val analysisTriggeredByUser: Boolean,
)

/**
 * Values already selected manually in the log editor, so the analyzer can
 * avoid re-suggesting what the user already chose.
 */
data class CurrentLogValues(
    val selectedSymptomTypes: Set<id.rona.app.domain.model.SymptomType> = emptySet(),
    val flow: id.rona.app.domain.model.FlowLevel? = null,
    val mood: id.rona.app.domain.model.Mood? = null,
    val energy: id.rona.app.domain.model.Energy? = null,
)

/**
 * The single output contract for P1 note analysis. Typed, explainable, editable.
 * Kept stable so a future P2 ONNX implementation can return the same shape.
 */
data class NoteAnalysisResult(
    val normalizedText: String,
    val symptomSuggestions: List<SymptomSuggestion>,
    val moodSuggestions: List<MoodSuggestion>,
    val energySuggestion: EnergySuggestion?,
    val flowSuggestion: FlowSuggestion?,
    val dischargeSuggestion: DischargeSuggestion?,
    val durationSuggestion: DurationSuggestion?,
    val pregnancyContext: PregnancyContext?,
    val detectedIntents: List<IntentMatch>,
    val safetyAlerts: List<SafetyAlert>,
    val relevantKnowledgeCards: List<KnowledgeCard>,
    val ruleVersion: String,
) {
    val hasAnySuggestions: Boolean
        get() = symptomSuggestions.isNotEmpty() ||
            moodSuggestions.isNotEmpty() ||
            energySuggestion != null ||
            flowSuggestion != null ||
            dischargeSuggestion != null ||
            durationSuggestion != null
}
