package id.rona.app.domain.nlp.dict

import id.rona.app.domain.model.Severity
import id.rona.app.domain.model.SymptomType

/**
 * Typed symptom dictionary. Each [SymptomRule] maps controlled terms to the
 * existing MVP [SymptomType] vocabulary — P1 never invents DB schema.
 *
 * Convention:
 * - `terms` are normalized (already passed through TextNormalizer) phrases.
 * - Match prefers the longest phrase (multi-word before single word).
 * - Adding a rule requires: a dictionary test + a safety review (see docs/NLP_RULE_ENGINE.md).
 */
data class SymptomRule(
    val symptomType: SymptomType,
    val terms: List<String>,
    val defaultSeverity: Severity = Severity.MODERATE,
    val isFuzzy: Boolean = false,
)

object SymptomDictionary {

    val rules: List<SymptomRule> = listOf(
        // ————— Lower abdominal cramps —————
        SymptomRule(
            SymptomType.KRAM,
            listOf(
                "kram perut", "perut kram", "perut sakit", "perut bawah sakit",
                "nyeri perut bawah", "sakit di bawah pusar", "perut ketarik",
                "perut seperti diremas", "perut mulas", "mulas", "mules", "kram",
            ),
            defaultSeverity = Severity.MODERATE,
        ),
        // ————— Headache —————
        SymptomRule(
            SymptomType.HEADACHE,
            listOf(
                "sakit kepala", "kepala berat", "kepala cenut cenut", "kepala cenut",
                "migrain", "pusing kepala", "pusing", "puyeng",
            ),
            defaultSeverity = Severity.MODERATE,
        ),
        // ————— Nausea / vomiting —————
        SymptomRule(
            SymptomType.NAUSEA,
            listOf("mual", "eneg", "mau muntah", "ingin muntah"),
            defaultSeverity = Severity.MODERATE,
        ),
        SymptomRule(
            SymptomType.NAUSEA,
            listOf("muntah", "tidak bisa menahan muntah", "muntah terus"),
            defaultSeverity = Severity.SEVERE,
        ),
        // ————— Bloating —————
        SymptomRule(
            SymptomType.BLOATING,
            listOf("kembung", "begah", "perut penuh", "perut terasa penuh", "perut kembung"),
            defaultSeverity = Severity.MILD,
        ),
        // ————— Fatigue —————
        SymptomRule(
            SymptomType.FATIGUE,
            listOf(
                "capek", "cape", "lelah", "lemas", "lesu", "lemes",
                "energi habis", "tidak bertenaga", "ngantuk terus", "mengantuk terus",
                "ngantukan", "mengantuk",
            ),
            defaultSeverity = Severity.MODERATE,
        ),
        // ————— Breast tenderness —————
        SymptomRule(
            SymptomType.BREAST_TENDERNESS,
            listOf("nyeri payudara", "payudara sakit", "payudara nyeri", "payudara nyut nyutan"),
            defaultSeverity = Severity.MODERATE,
        ),
        // ————— Acne —————
        SymptomRule(
            SymptomType.ACNE,
            listOf("jerawat", "jerawatan", "jerawat muncul", "beruntusan"),
            defaultSeverity = Severity.MILD,
        ),
        // ————— Back pain —————
        SymptomRule(
            SymptomType.BACKACHE,
            listOf("sakit punggung", "nyeri punggung", "punggung sakit", "pegal pinggang", "sakit pinggang", "nyeri pinggang"),
            defaultSeverity = Severity.MODERATE,
        ),
        // ————— Sleep difficulty —————
        SymptomRule(
            SymptomType.SLEEP_ISSUE,
            listOf(
                "susah tidur", "sulit tidur", "insomnia", "tidur tidak nyenyak",
                "kebangun terus", "sering terbangun", "susah tidur nyenyak",
            ),
            defaultSeverity = Severity.MODERATE,
        ),
        // ————— Appetite change —————
        SymptomRule(
            SymptomType.APPETITE_CHANGE,
            listOf(
                "nafsu makan berubah", "nafsu makan naik", "nafsu makan turun",
                "tidak nafsu makan", "makan terus", "ngidam", "selera makan hilang",
            ),
            defaultSeverity = Severity.MILD,
            isFuzzy = true,
        ),
    )
}
