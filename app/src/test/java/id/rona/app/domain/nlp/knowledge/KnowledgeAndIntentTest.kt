package id.rona.app.domain.nlp.knowledge

import com.google.common.truth.Truth.assertThat
import id.rona.app.domain.engine.CyclePhase
import id.rona.app.domain.model.SymptomType
import id.rona.app.domain.nlp.HealthIntent
import id.rona.app.domain.nlp.KnowledgeCategory
import id.rona.app.domain.nlp.KnowledgeContext
import id.rona.app.domain.nlp.TriageLevel
import org.junit.Test

class KnowledgeCardRepositoryTest {

    private val repository = LocalKnowledgeCardRepository()

    @Test
    fun allCardsPresent() {
        val cards = repository.getAllCards()
        assertThat(cards).isNotEmpty()
        // Minimum required cards by id.
        val ids = cards.map { it.id }.toSet()
        assertThat(ids).containsAtLeast(
            "cycle_basics", "period_pain", "pms_and_mood",
            "discharge_normal", "discharge_consultation", "fertility_limitations",
            "pregnancy_general", "pregnancy_nausea", "food_and_hydration",
            "phase_menstruation", "phase_follicular", "phase_ovulation_est",
            "phase_luteal", "sleep_and_activity", "privacy_and_data",
        )
    }

    @Test
    fun phaseSpecificMatching() {
        val cards = repository.findRelevantCards(
            KnowledgeContext(phase = CyclePhase.EARLY)
        )
        assertThat(cards).isNotEmpty()
        // Cards applicable to EARLY get boosted; maximum 3 returned.
        assertThat(cards.size).isAtMost(3)
    }

    @Test
    fun symptomSpecificMatching() {
        val cards = repository.findRelevantCards(
            KnowledgeContext(symptoms = setOf(SymptomType.KRAM))
        )
        assertThat(cards.any { it.id == "period_pain" }).isTrue()
    }

    @Test
    fun pregnancyCardsGatedByDefault() {
        val cards = repository.findRelevantCards(
            KnowledgeContext(
                symptoms = setOf(SymptomType.NAUSEA),
                pregnancyModeEnabled = false,
                pregnancyContextDetected = false,
            )
        )
        assertThat(cards.map { it.id }).doesNotContain("pregnancy_nausea")
    }

    @Test
    fun pregnancyCardsUnlockedWhenModeEnabled() {
        val cards = repository.findRelevantCards(
            KnowledgeContext(
                symptoms = setOf(SymptomType.NAUSEA),
                pregnancyModeEnabled = true,
            )
        )
        assertThat(cards.map { it.id }).contains("pregnancy_nausea")
    }

    @Test
    fun maxCardsEnforced() {
        val cards = repository.findRelevantCards(
            KnowledgeContext(
                phase = CyclePhase.EARLY,
                symptoms = setOf(SymptomType.KRAM, SymptomType.NAUSEA, SymptomType.FATIGUE),
            )
        )
        assertThat(cards.size).isAtMost(LocalKnowledgeCardRepository.MAX_CARDS)
    }

    @Test
    fun urgentIntentBoostsConsultCards() {
        val cards = repository.findRelevantCards(
            KnowledgeContext(healthIntents = setOf(HealthIntent.ASK_URGENT_HELP))
        )
        // The highest triage cards should appear first.
        val levels = cards.map { it.triageLevel }
        val sorted = levels.sortedByDescending { it.ordinal }
        assertThat(levels).isEqualTo(sorted)
    }

    @Test
    fun noDiagnosisClaimsInSeedContent() {
        // Diagnosis names must not appear anywhere.
        val allText = repository.getAllCards().joinToString(" ") { card ->
            card.title + " " + card.shortSummary + " " +
                card.whatMayHelp.joinToString(" ") + " " +
                card.whatToAvoidClaiming.joinToString(" ") + " " +
                card.whenToSeekHelp.joinToString(" ")
        }.lowercase()

        assertThat(allText).doesNotContain("pcos")
        assertThat(allText).doesNotContain("endometriosis")
        assertThat(allText).doesNotContain("kandidiasis")
        assertThat(allText).doesNotContain("vaginosis")
        assertThat(allText).doesNotContain("anemia")
        assertThat(allText).doesNotContain("depresi")
        assertThat(allText).doesNotContain("hormonal imbalance")
    }

    @Test
    fun noDoseOrSupplementClaimsInAdviceFields() {
        // The app must never ADVISE doses/supplements; mentioning them inside
        // whatToAvoidClaiming (saying we won't) is required and allowed.
        val adviceText = repository.getAllCards().joinToString(" ") { card ->
            card.shortSummary + " " + card.whatMayHelp.joinToString(" ") + " " +
                card.whenToSeekHelp.joinToString(" ")
        }.lowercase()
        assertThat(adviceText).doesNotContain("dosis")
        assertThat(adviceText).doesNotContain("suplemen")
        assertThat(adviceText).doesNotContain("antibiotik")
    }

    @Test
    fun everyCardHasRequiredMetadata() {
        repository.getAllCards().forEach { card ->
            assertThat(card.sourceCategory).isNotNull()
            assertThat(card.reviewedAt).isNotNull()
            assertThat(card.contentVersion).isNotEmpty()
            assertThat(card.triageLevel).isNotNull()
        }
    }
}

class IntentRouterTest {

    @Test
    fun routesDischargeQuestion() {
        val intents = id.rona.app.domain.nlp.engine.IntentRouter.route(
            id.rona.app.domain.nlp.engine.TextNormalizer.normalize("normal tidak kalau keputihan bening licin?")
        )
        assertThat(intents.map { it.intent }).contains(HealthIntent.ASK_DISCHARGE)
    }

    @Test
    fun routesPusingSaatHaidAsNormality() {
        val intents = id.rona.app.domain.nlp.engine.IntentRouter.route(
            id.rona.app.domain.nlp.engine.TextNormalizer.normalize("kenapa aku pusing saat haid?")
        )
        assertThat(intents.map { it.intent }).contains(HealthIntent.ASK_NORMALITY)
    }

    @Test
    fun routesPeriodPrediction() {
        val intents = id.rona.app.domain.nlp.engine.IntentRouter.route(
            id.rona.app.domain.nlp.engine.TextNormalizer.normalize("kapan kira kira haid lagi?")
        )
        assertThat(intents.map { it.intent }).contains(HealthIntent.ASK_PERIOD_PREDICTION)
    }

    @Test
    fun routesCoffeeQuestionAsFoodOrDrink() {
        val intents = id.rona.app.domain.nlp.engine.IntentRouter.route(
            id.rona.app.domain.nlp.engine.TextNormalizer.normalize("boleh minum kopi tidak?")
        )
        assertThat(intents.map { it.intent }).contains(HealthIntent.ASK_FOOD_OR_DRINK)
    }

    @Test
    fun routesLatePeriodAsPregnancySymptom() {
        val intents = id.rona.app.domain.nlp.engine.IntentRouter.route(
            id.rona.app.domain.nlp.engine.TextNormalizer.normalize("telat 8 hari dan mual")
        )
        assertThat(intents.map { it.intent }).contains(HealthIntent.ASK_PREGNANCY_SYMPTOM)
    }

    @Test
    fun routesPregnancyBleedingAsUrgent() {
        val intents = id.rona.app.domain.nlp.engine.IntentRouter.route(
            id.rona.app.domain.nlp.engine.TextNormalizer.normalize("aku hamil dan keluar darah")
        )
        assertThat(intents.map { it.intent }).contains(HealthIntent.ASK_PREGNANCY_SYMPTOM)
        assertThat(intents.map { it.intent }).contains(HealthIntent.ASK_URGENT_HELP)
    }

    @Test
    fun routesPrivacyQuestion() {
        val intents = id.rona.app.domain.nlp.engine.IntentRouter.route(
            id.rona.app.domain.nlp.engine.TextNormalizer.normalize("apakah data aku tersimpan aman?")
        )
        assertThat(intents.map { it.intent }).contains(HealthIntent.ASK_PRIVACY)
    }

    @Test
    fun emptyTextNoIntents() {
        assertThat(id.rona.app.domain.nlp.engine.IntentRouter.route("")).isEmpty()
    }
}
