package id.rona.app.domain.nlp.engine

import id.rona.app.domain.nlp.KnowledgeCardRepository
import id.rona.app.domain.nlp.KnowledgeContext
import id.rona.app.domain.nlp.LocalNoteAnalyzer
import id.rona.app.domain.nlp.NoteAnalysisInput
import id.rona.app.domain.nlp.NoteAnalysisResult
import id.rona.app.domain.nlp.P1DischargeDescriptor
import id.rona.app.domain.nlp.PregnancyContext
import id.rona.app.domain.nlp.SafetyContext
import id.rona.app.domain.nlp.SafetyTriageEngine
import id.rona.app.domain.nlp.HealthIntent
import id.rona.app.domain.nlp.PregnancyContextKind
import id.rona.app.domain.nlp.dict.PregnancyDictionary
import javax.inject.Inject
import javax.inject.Singleton

/**
 * P1 analyzer: deterministic rule-based pipeline, fully offline.
 *
 * Pipeline order (safety first, by design):
 * 1. Guard: refuse analysis unless explicitly triggered by the user.
 * 2. Normalize text (ephemeral copy only; raw note is never mutated/logged).
 * 3. Extract entities (symptoms, mood, energy, flow, discharge, duration, pregnancy).
 * 4. Build SafetyContext and run safety triage BEFORE any output.
 * 5. Route intents.
 * 6. Match knowledge cards from the local repository.
 *
 * P2 (ONNX) can implement [LocalNoteAnalyzer] with the same output contract.
 */
@Singleton
class RuleBasedNoteAnalyzer @Inject constructor(
    private val safetyTriageEngine: SafetyTriageEngine,
    private val knowledgeCardRepository: KnowledgeCardRepository,
) : LocalNoteAnalyzer {

    override fun analyze(input: NoteAnalysisInput): NoteAnalysisResult {
        // Guard: never analyze without explicit user action (privacy rule #5).
        if (!input.analysisTriggeredByUser) {
            return emptyResult()
        }
        if (input.noteText.isBlank()) {
            return emptyResult()
        }

        val normalized = TextNormalizer.normalize(input.noteText)

        // ————— Extraction (pure, deterministic) —————
        val symptoms = SymptomExtractor.extract(
            normalized,
            alreadySelected = input.currentLogValues?.selectedSymptomTypes.orEmpty(),
        )
        val moods = MoodEnergyFlowExtractor.extractMoods(
            normalized,
            alreadySelected = input.currentLogValues?.mood,
        )
        val energy = MoodEnergyFlowExtractor.extractEnergy(
            normalized,
            alreadySelected = input.currentLogValues?.energy,
        )
        val flow = MoodEnergyFlowExtractor.extractFlow(
            normalized,
            alreadySelected = input.currentLogValues?.flow,
        )
        val discharge = DischargeExtractor.extract(normalized)
        val duration = DurationExtractor.extract(normalized)
        var pregnancyContext = PregnancyContextExtractor.extract(normalized)
        val redFlags = PregnancyContextExtractor.extractRedFlags(normalized)

        // Generic bleeding phrases co-occurring with a pregnancy context are
        // treated as bleeding-in-pregnancy for safety purposes.
        val genericBleeding = normalized.contains("keluar darah") ||
            normalized.contains("perdarahan") ||
            normalized.contains("flek")
        if (pregnancyContext != null && genericBleeding) {
            pregnancyContext = pregnancyContext.copy(
                kind = PregnancyContextKind.BLEEDING_IN_PREGNANCY,
                matchedTerms = pregnancyContext.matchedTerms + "keluar darah",
            )
        }

        // ————— Safety context (evaluated before any suggestion output) —————
        val heavyBleedingMentioned = normalized.contains("tembus tiap jam") ||
            normalized.contains("ganti pembalut tiap jam") ||
            normalized.contains("darah tidak berhenti") ||
            normalized.contains("darah sangat banyak") ||
            normalized.contains("darah banyak") ||
            normalized.contains("darah deras")
        val severePainMentioned = normalized.contains("sangat sakit") ||
            normalized.contains("tidak tahan") ||
            normalized.contains("nyeri sekali") ||
            normalized.contains("sakit sekali") ||
            normalized.contains("parah")
        val faintingMentioned = normalized.contains("pingsan") ||
            normalized.contains("hampir pingsan") ||
            normalized.contains("kunang kunang")
        val visualDisturbance = redFlags.containsKey(PregnancyDictionary.PregnancyRedFlag.VISUAL_CHANGE)
        val feverMentioned = redFlags.containsKey(PregnancyDictionary.PregnancyRedFlag.FEVER) ||
            normalized.contains("demam")
        val selfHarmMentioned = containsSelfHarmPhrases(normalized)

        val safetyAlerts = safetyTriageEngine.evaluate(
            SafetyContext(
                noteText = input.noteText,
                normalizedText = normalized,
                detectedSymptoms = symptoms.map { it.symptomType }.toSet(),
                dischargeDescriptors = discharge?.descriptors.orEmpty(),
                dischargeItchingOrBurning = discharge?.itchingOrBurning == true,
                pregnancyContext = pregnancyContext,
                heavyBleedingMentioned = heavyBleedingMentioned,
                severePainMentioned = severePainMentioned,
                faintingMentioned = faintingMentioned,
                visualDisturbanceMentioned = visualDisturbance,
                feverMentioned = feverMentioned,
                selfHarmMentioned = selfHarmMentioned,
                periodActive = input.isPeriodActive,
            )
        )

        // ————— Intents —————
        val intents = IntentRouter.route(normalized)

        // ————— Knowledge cards —————
        val cards = knowledgeCardRepository.findRelevantCards(
            KnowledgeContext(
                phase = input.currentPhase,
                symptoms = symptoms.map { it.symptomType }.toSet(),
                pregnancyModeEnabled = input.isPregnancyModeEnabled,
                pregnancyContextDetected = pregnancyContext != null,
                healthIntents = intents.map { it.intent }.toSet(),
            )
        )

        return NoteAnalysisResult(
            normalizedText = normalized,
            symptomSuggestions = symptoms,
            moodSuggestions = moods,
            energySuggestion = energy,
            flowSuggestion = flow,
            dischargeSuggestion = discharge,
            durationSuggestion = duration,
            pregnancyContext = pregnancyContext,
            detectedIntents = intents,
            safetyAlerts = safetyAlerts,
            relevantKnowledgeCards = cards,
            ruleVersion = RULE_VERSION,
        )
    }

    private fun emptyResult() = NoteAnalysisResult(
        normalizedText = "",
        symptomSuggestions = emptyList(),
        moodSuggestions = emptyList(),
        energySuggestion = null,
        flowSuggestion = null,
        dischargeSuggestion = null,
        durationSuggestion = null,
        pregnancyContext = null,
        detectedIntents = emptyList(),
        safetyAlerts = emptyList(),
        relevantKnowledgeCards = emptyList(),
        ruleVersion = RULE_VERSION,
    )

    private fun containsSelfHarmPhrases(normalized: String): Boolean =
        SELF_HARM_PHRASES.any { normalized.contains(it) }

    companion object {
        const val RULE_VERSION = "rules-id-v1"
        private val SELF_HARM_PHRASES = listOf(
            "ingin mati", "mau mati", "ingin melukai diri", "melukai diri sendiri",
            "menyakiti diri sendiri", "ingin mengakhiri", "tidak ingin hidup",
        )
    }
}
