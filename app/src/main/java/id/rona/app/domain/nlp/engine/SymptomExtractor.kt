package id.rona.app.domain.nlp.engine

import id.rona.app.domain.model.Severity
import id.rona.app.domain.model.SymptomType
import id.rona.app.domain.nlp.ConfidenceLevel
import id.rona.app.domain.nlp.SymptomSuggestion
import id.rona.app.domain.nlp.dict.SymptomDictionary

/**
 * Deterministic symptom extraction. Precision over recall: matches only
 * whole dictionary phrases (longest first), applies negation, and maps
 * severity from nearby contextual modifiers.
 */
object SymptomExtractor {

    fun extract(
        normalizedText: String,
        alreadySelected: Set<SymptomType> = emptySet(),
    ): List<SymptomSuggestion> {
        if (normalizedText.isBlank()) return emptyList()

        val suggestions = mutableListOf<SymptomSuggestion>()
        val claimedRanges = mutableSetOf<IntRange>()
        val claimedTypes = mutableSetOf<SymptomType>()

        // Longest phrase first so "kram perut" wins over "kram".
        val sortedRules = SymptomDictionary.rules
            .flatMap { rule -> rule.terms.map { rule to it } }
            .sortedByDescending { it.second.length }

        for ((rule, term) in sortedRules) {
            if (rule.symptomType in claimedTypes) continue
            val range = findWholeWordRange(normalizedText, term) ?: continue
            if (claimedRanges.any { it.overlaps(range) }) continue
            if (NegationDetector.isNegated(normalizedText, range.first, term)) continue
            if (NegationDetector.isNegatedByExistence(normalizedText, term)) continue

            // Skip if user already selected this symptom manually.
            if (rule.symptomType in alreadySelected) continue

            val severityMatch = SeverityExtractor.extractForTerm(normalizedText, term)
            val severity = severityMatch?.severity ?: rule.defaultSeverity
            val confidence = when {
                severityMatch?.confidence == ConfidenceLevel.HIGH -> ConfidenceLevel.HIGH
                rule.isFuzzy -> ConfidenceLevel.MEDIUM
                severityMatch != null -> ConfidenceLevel.MEDIUM
                else -> ConfidenceLevel.HIGH
            }

            claimedRanges += range
            claimedTypes += rule.symptomType
            suggestions += SymptomSuggestion(
                symptomType = rule.symptomType,
                severity = severity,
                confidence = confidence,
                matchedTerms = listOf(term),
                explanation = "",
            )
        }
        return suggestions
    }

    /**
     * Whole-word phrase matching: "pusing" must not match inside "pusingan".
     * Returns the char range of the first match, or null.
     */
    fun findWholeWordRange(text: String, phrase: String): IntRange? {
        var searchFrom = 0
        while (true) {
            val index = text.indexOf(phrase, searchFrom)
            if (index < 0) return null

            val before = if (index == 0) ' ' else text[index - 1]
            val afterIndex = index + phrase.length
            val after = if (afterIndex >= text.length) ' ' else text[afterIndex]
            val boundaryBefore = before == ' ' || before == '\n' || before == '\t'
            val boundaryAfter = after == ' ' || after == '\n' || after == '\t'

            if (boundaryBefore && boundaryAfter) {
                return index until afterIndex
            }
            searchFrom = index + 1
        }
    }

    private fun IntRange.overlaps(other: IntRange): Boolean =
        first <= other.last && other.first <= last
}
