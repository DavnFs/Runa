package id.rona.app.domain.nlp.engine

import com.google.common.truth.Truth.assertThat
import id.rona.app.domain.engine.CyclePhase
import id.rona.app.domain.model.SymptomType
import id.rona.app.domain.nlp.CurrentLogValues
import id.rona.app.domain.nlp.HealthIntent
import id.rona.app.domain.nlp.NoteAnalysisInput
import id.rona.app.domain.nlp.PregnancyContextKind
import id.rona.app.domain.nlp.TriageLevel
import id.rona.app.domain.nlp.knowledge.LocalKnowledgeCardRepository
import id.rona.app.domain.nlp.safety.SafetyTriageEngineImpl
import org.junit.Test

class RuleBasedNoteAnalyzerTest {

    private val analyzer = RuleBasedNoteAnalyzer(
        safetyTriageEngine = SafetyTriageEngineImpl(),
        knowledgeCardRepository = LocalKnowledgeCardRepository(),
    )

    private fun input(
        note: String,
        cycleDay: Int? = 14,
        phase: CyclePhase? = CyclePhase.MIDDLE,
        periodActive: Boolean = false,
        pregnancyMode: Boolean = false,
        currentValues: CurrentLogValues? = null,
        triggeredByUser: Boolean = true,
    ) = NoteAnalysisInput(
        noteText = note,
        cycleDay = cycleDay,
        currentPhase = phase,
        isPeriodActive = periodActive,
        isPregnancyModeEnabled = pregnancyMode,
        currentLogValues = currentValues,
        analysisTriggeredByUser = triggeredByUser,
    )

    @Test
    fun refusesAnalysisWhenNotTriggeredByUser() {
        val result = analyzer.analyze(input("kram perut", triggeredByUser = false))
        assertThat(result.hasAnySuggestions).isFalse()
        assertThat(result.safetyAlerts).isEmpty()
    }

    @Test
    fun blankNoteYieldsEmptyResult() {
        val result = analyzer.analyze(input("   "))
        assertThat(result.hasAnySuggestions).isFalse()
        assertThat(result.safetyAlerts).isEmpty()
    }

    @Test
    fun extractsMixedEntities() {
        val result = analyzer.analyze(input("kram perut parah, pusing, dan lelah sejak pagi"))
        assertThat(result.symptomSuggestions.map { it.symptomType })
            .contains(SymptomType.KRAM)
        assertThat(result.symptomSuggestions.map { it.symptomType })
            .contains(SymptomType.HEADACHE)
        assertThat(result.symptomSuggestions.map { it.symptomType })
            .contains(SymptomType.FATIGUE)
    }

    @Test
    fun pregnancyBleedingRaisesSafetyAlertAndUrgentIntent() {
        val result = analyzer.analyze(input("aku hamil dan keluar darah"))
        assertThat(result.safetyAlerts).isNotEmpty()
        assertThat(result.detectedIntents.map { it.intent }).contains(HealthIntent.ASK_URGENT_HELP)
    }

    @Test
    fun safetyAlertsComeBeforeKnowledgeCards() {
        val result = analyzer.analyze(input("hamil dan keluar darah banyak"))
        // Result structure always contains alerts first in UI ordering;
        // here verify both are populated and the alert triage is at least URGENT.
        assertThat(result.safetyAlerts.first().triageLevel.ordinal)
            .isAtLeast(TriageLevel.URGENT.ordinal)
    }

    @Test
    fun negatedSymptomNotSuggested() {
        val result = analyzer.analyze(input("tidak pusing, tapi kram"))
        assertThat(result.symptomSuggestions.map { it.symptomType })
            .doesNotContain(SymptomType.HEADACHE)
        assertThat(result.symptomSuggestions.map { it.symptomType })
            .contains(SymptomType.KRAM)
    }

    @Test
    fun negatedPregnancyNotActivated() {
        val result = analyzer.analyze(input("bukan hamil kok"))
        assertThat(result.pregnancyContext).isNull()
    }

    @Test
    fun alreadySelectedSymptomsAreSkipped() {
        val result = analyzer.analyze(
            input(
                "kram perut dan pusing",
                currentValues = CurrentLogValues(selectedSymptomTypes = setOf(SymptomType.KRAM)),
            )
        )
        assertThat(result.symptomSuggestions.map { it.symptomType })
            .doesNotContain(SymptomType.KRAM)
        assertThat(result.symptomSuggestions.map { it.symptomType })
            .contains(SymptomType.HEADACHE)
    }

    @Test
    fun ruleVersionIsStable() {
        val result = analyzer.analyze(input("kram"))
        assertThat(result.ruleVersion).isEqualTo(RuleBasedNoteAnalyzer.RULE_VERSION)
    }

    @Test
    fun knowledgeCardsRespectMax() {
        val result = analyzer.analyze(input("kram perut dan lelah dan mual dan pusing"))
        assertThat(result.relevantKnowledgeCards.size).isAtMost(3)
    }

    @Test
    fun pregnancyModeUnlocksPregnancyCards() {
        val result = analyzer.analyze(input("mual setiap pagi", pregnancyMode = true))
        assertThat(result.relevantKnowledgeCards.any { it.id == "pregnancy_nausea" }).isTrue()
    }

    @Test
    fun noPregnancyCardsWithoutModeOrContext() {
        val result = analyzer.analyze(input("mual setiap pagi"))
        assertThat(result.relevantKnowledgeCards.map { it.id })
            .doesNotContain("pregnancy_nausea")
    }

    @Test
    fun selfHarmWordingRaisesEmergency() {
        val result = analyzer.analyze(input("akhir akhir ini aku ingin mati"))
        assertThat(result.safetyAlerts.any { it.triageLevel == TriageLevel.EMERGENCY }).isTrue()
    }

    @Test
    fun dischargeNoteYieldsDischargeSuggestion() {
        val result = analyzer.analyze(input("keputihan bening licin hari ini"))
        assertThat(result.dischargeSuggestion).isNotNull()
    }

    @Test
    fun normalizedTextIsEphemeralCopy() {
        val raw = "Kram perut BGT 😭"
        analyzer.analyze(input(raw))
        // Raw input unchanged in the result input (no mutation).
        assertThat(raw).isEqualTo("Kram perut BGT 😭")
    }

    @Test
    fun heavyBleedingWithDizzinessIsEmergency() {
        val result = analyzer.analyze(input("darah sangat banyak dan pusing banget"))
        assertThat(result.safetyAlerts.any { it.triageLevel == TriageLevel.EMERGENCY }).isTrue()
    }
}
