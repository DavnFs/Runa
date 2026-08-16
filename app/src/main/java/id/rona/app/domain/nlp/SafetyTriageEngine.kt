package id.rona.app.domain.nlp

import id.rona.app.domain.engine.CyclePhase
import id.rona.app.domain.model.SymptomType

/**
 * Structured knowledge repository. Cards live in typed Kotlin seed data
 * (KnowledgeCardSeed) — no runtime file parsing, fully offline.
 */
interface KnowledgeCardRepository {
    fun getAllCards(): List<KnowledgeCard>
    fun findRelevantCards(context: KnowledgeContext): List<KnowledgeCard>
}

/**
 * Safety context: what the rule engine has extracted, before any UI output.
 */
data class SafetyContext(
    val noteText: String = "",
    val normalizedText: String = "",
    val detectedSymptoms: Set<SymptomType> = emptySet(),
    val dischargeDescriptors: Set<P1DischargeDescriptor> = emptySet(),
    val dischargeItchingOrBurning: Boolean = false,
    val pregnancyContext: PregnancyContext? = null,
    val heavyBleedingMentioned: Boolean = false,
    val severePainMentioned: Boolean = false,
    val faintingMentioned: Boolean = false,
    val visualDisturbanceMentioned: Boolean = false,
    val feverMentioned: Boolean = false,
    val selfHarmMentioned: Boolean = false,
    val periodActive: Boolean = false,
)

interface SafetyTriageEngine {
    fun evaluate(context: SafetyContext): List<SafetyAlert>
}
