package id.rona.app.domain.nlp

import id.rona.app.domain.engine.CyclePhase
import id.rona.app.domain.model.SymptomType
import java.time.LocalDate

enum class TriageLevel {
    INFO,
    SELF_CARE,
    CONSULT_SOON,
    URGENT,
    EMERGENCY,
}

enum class ConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW,
}

enum class SuggestionSource {
    RULE_BASED,
}

enum class SourceCategory {
    WHO_GUIDANCE,
    ACOG_GUIDANCE,
    CDC_GUIDANCE,
    NHS_PATIENT_GUIDANCE,
    SYSTEMATIC_REVIEW,
    PRODUCT_SAFETY_POLICY,
}

enum class KnowledgeCategory {
    CYCLE_BASICS,
    MENSTRUATION,
    PERIOD_PAIN,
    PMS_AND_MOOD,
    DISCHARGE,
    FERTILITY_LIMITATIONS,
    PREGNANCY_GENERAL,
    PREGNANCY_NAUSEA,
    PREGNANCY_WARNING_SIGNS,
    FOOD_AND_HYDRATION,
    SLEEP_AND_ACTIVITY,
    WHEN_TO_SEEK_HELP,
    PRIVACY_AND_DATA,
}

enum class HealthIntent {
    LOG_SYMPTOM,
    ASK_NORMALITY,
    ASK_CYCLE_STATUS,
    ASK_PERIOD_PREDICTION,
    ASK_DISCHARGE,
    ASK_FERTILITY,
    ASK_PREGNANCY_SYMPTOM,
    ASK_FOOD_OR_DRINK,
    ASK_SELF_CARE,
    ASK_URGENT_HELP,
    ASK_PRIVACY,
}

/**
 * Structured local knowledge. Every response in the app comes from these
 * cards — never from generative text or scattered UI strings.
 */
data class KnowledgeCard(
    val id: String,
    val category: KnowledgeCategory,
    val title: String,
    val shortSummary: String,
    val whatMayHelp: List<String>,
    val whatToAvoidClaiming: List<String>,
    val whenToSeekHelp: List<String>,
    val triageLevel: TriageLevel,
    val applicablePhases: Set<CyclePhase>,
    val applicableSymptoms: Set<SymptomType>,
    val pregnancyModeOnly: Boolean,
    val medicalDisclaimer: String?,
    val sourceCategory: SourceCategory,
    val reviewedAt: LocalDate,
    val contentVersion: String,
)

/** Context used to pick relevant knowledge cards. */
data class KnowledgeContext(
    val phase: CyclePhase? = null,
    val symptoms: Set<SymptomType> = emptySet(),
    val pregnancyModeEnabled: Boolean = false,
    val pregnancyContextDetected: Boolean = false,
    val healthIntents: Set<HealthIntent> = emptySet(),
)

/** Structured, editable symptom suggestion. */
data class SymptomSuggestion(
    val symptomType: SymptomType,
    val severity: id.rona.app.domain.model.Severity?,
    val confidence: ConfidenceLevel,
    val matchedTerms: List<String>,
    val source: SuggestionSource = SuggestionSource.RULE_BASED,
    val explanation: String = "",
)

data class MoodSuggestion(
    val mood: id.rona.app.domain.model.Mood,
    val confidence: ConfidenceLevel,
    val matchedTerms: List<String>,
    val source: SuggestionSource = SuggestionSource.RULE_BASED,
    val explanation: String = "",
)

data class EnergySuggestion(
    val energy: id.rona.app.domain.model.Energy,
    val confidence: ConfidenceLevel,
    val matchedTerms: List<String>,
    val source: SuggestionSource = SuggestionSource.RULE_BASED,
    val explanation: String = "",
)

data class FlowSuggestion(
    val flow: id.rona.app.domain.model.FlowLevel,
    val confidence: ConfidenceLevel,
    val matchedTerms: List<String>,
    val source: SuggestionSource = SuggestionSource.RULE_BASED,
    val explanation: String = "",
)

data class DischargeSuggestion(
    val descriptors: Set<P1DischargeDescriptor>,
    val itchingOrBurning: Boolean = false,
    val odorConcern: Boolean = false,
    val confidence: ConfidenceLevel,
    val matchedTerms: List<String>,
    val source: SuggestionSource = SuggestionSource.RULE_BASED,
    val explanation: String = "",
)

data class DurationSuggestion(
    val label: String,
    val matchedTerms: List<String>,
    val confidence: ConfidenceLevel,
    val source: SuggestionSource = SuggestionSource.RULE_BASED,
)

data class PregnancyContext(
    val kind: PregnancyContextKind,
    val confidence: ConfidenceLevel,
    val matchedTerms: List<String>,
)

enum class PregnancyContextKind {
    TEST_POSITIVE,
    SUSPECTED,
    BLEEDING_IN_PREGNANCY,
}

data class IntentMatch(
    val intent: HealthIntent,
    val confidence: ConfidenceLevel,
    val matchedTerms: List<String>,
)

/**
 * A structured safety alert. [triageLevel] determines presentation order;
 * copy stays calm, non-judgmental, and free of diagnosis wording.
 */
data class SafetyAlert(
    val id: String,
    val title: String,
    val message: String,
    val triageLevel: TriageLevel,
    val matchedTerms: List<String>,
)

/**
 * Controlled discharge descriptors for P1. Deliberately descriptive —
 * no diagnosis labels (no BV, no candidiasis, no STI).
 */
enum class P1DischargeDescriptor {
    CLEAR,
    WHITE,
    YELLOW,
    GREEN,
    GREY,
    BROWN,
    WATERY,
    STRETCHY,
    THICK,
    CLUMPY,
    ODOR_NONE,
    ODOR_FISHY,
    ITCHING,
    BURNING,
}
