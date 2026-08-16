package id.rona.app.domain.nlp.knowledge

import id.rona.app.domain.nlp.KnowledgeCard
import id.rona.app.domain.nlp.KnowledgeCardRepository
import id.rona.app.domain.nlp.KnowledgeContext
import id.rona.app.domain.nlp.TriageLevel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Structured local knowledge repository backed by typed seed data.
 * Fully offline; no file parsing, no network, no URL references.
 */
@Singleton
class LocalKnowledgeCardRepository @Inject constructor() : KnowledgeCardRepository {

    override fun getAllCards(): List<KnowledgeCard> = KnowledgeCardSeed.cards

    override fun findRelevantCards(context: KnowledgeContext): List<KnowledgeCard> {
        val scored = getAllCards()
            .filter { card -> !card.pregnancyModeOnly || context.pregnancyModeEnabled || context.pregnancyContextDetected }
            .mapNotNull { card ->
                val score = scoreCard(card, context)
                if (score <= 0) null else card to score
            }
            .sortedWith(
                compareByDescending<Pair<KnowledgeCard, Int>> {
                    // Triage priority: urgent cards first.
                    it.first.triageLevel.ordinal
                }.thenByDescending { it.second }
            )

        return scored.take(MAX_CARDS).map { it.first }
    }

    private fun scoreCard(card: KnowledgeCard, context: KnowledgeContext): Int {
        var score = 0

        // Phase match.
        if (context.phase != null && context.phase in card.applicablePhases) score += 2
        else if (context.phase != null && card.applicablePhases.isNotEmpty()) score -= 1

        // Symptom overlap.
        val overlap = card.applicableSymptoms.intersect(context.symptoms)
        score += overlap.size * 3

        // Intent boosts.
        if (id.rona.app.domain.nlp.HealthIntent.ASK_URGENT_HELP in context.healthIntents) {
            if (card.triageLevel >= TriageLevel.CONSULT_SOON) score += 4
        }
        if (id.rona.app.domain.nlp.HealthIntent.ASK_DISCHARGE in context.healthIntents &&
            card.category == id.rona.app.domain.nlp.KnowledgeCategory.DISCHARGE
        ) {
            score += 3
        }
        if (id.rona.app.domain.nlp.HealthIntent.ASK_PREGNANCY_SYMPTOM in context.healthIntents &&
            card.pregnancyModeOnly
        ) {
            score += 3
        }
        if (id.rona.app.domain.nlp.HealthIntent.ASK_PRIVACY in context.healthIntents &&
            card.category == id.rona.app.domain.nlp.KnowledgeCategory.PRIVACY_AND_DATA
        ) {
            score += 3
        }

        // Baseline so phase-generic cards can still surface.
        if (score <= 0 && card.applicablePhases.isEmpty() && card.applicableSymptoms.isEmpty()) {
            score = 1
        }
        return score
    }

    companion object {
        const val MAX_CARDS = 3
    }
}
