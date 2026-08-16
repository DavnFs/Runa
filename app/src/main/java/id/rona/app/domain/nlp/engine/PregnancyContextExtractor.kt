package id.rona.app.domain.nlp.engine

import id.rona.app.domain.nlp.ConfidenceLevel
import id.rona.app.domain.nlp.PregnancyContext
import id.rona.app.domain.nlp.PregnancyContextKind
import id.rona.app.domain.nlp.dict.PregnancyDictionary

/**
 * Pregnancy-context extraction. Conservative:
 * - "bukan hamil" / "tidak hamil" never activates pregnancy context.
 * - Confirmed test wording -> HIGH; suspected wording -> MEDIUM.
 */
object PregnancyContextExtractor {

    fun extract(normalizedText: String): PregnancyContext? {
        if (normalizedText.isBlank()) return null

        val sortedRules = PregnancyDictionary.rules
            .flatMap { rule -> rule.terms.map { rule to it } }
            .sortedByDescending { it.second.length }

        for ((rule, term) in sortedRules) {
            val range = SymptomExtractor.findWholeWordRange(normalizedText, term) ?: continue
            if (NegationDetector.isNegated(normalizedText, range.first, term)) continue

            // Specific guard: "bukan hamil" style phrases.
            if (term == "hamil" && NegationDetector.isNegated(normalizedText, range.first, term)) {
                continue
            }

            val confidence = when (rule.kind) {
                PregnancyContextKind.TEST_POSITIVE -> ConfidenceLevel.HIGH
                PregnancyContextKind.BLEEDING_IN_PREGNANCY -> ConfidenceLevel.HIGH
                PregnancyContextKind.SUSPECTED -> ConfidenceLevel.MEDIUM
            }
            return PregnancyContext(
                kind = rule.kind,
                confidence = confidence,
                matchedTerms = listOf(term),
            )
        }
        return null
    }

    fun extractRedFlags(normalizedText: String): Map<PregnancyDictionary.PregnancyRedFlag, List<String>> {
        val result = mutableMapOf<PregnancyDictionary.PregnancyRedFlag, MutableList<String>>()
        PregnancyDictionary.redFlagTerms.forEach { (term, flag) ->
            val range = SymptomExtractor.findWholeWordRange(normalizedText, term) ?: return@forEach
            if (NegationDetector.isNegated(normalizedText, range.first, term)) return@forEach
            result.getOrPut(flag) { mutableListOf() }.add(term)
        }
        return result
    }
}
