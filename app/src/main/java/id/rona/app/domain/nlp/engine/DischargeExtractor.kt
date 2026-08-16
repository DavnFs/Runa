package id.rona.app.domain.nlp.engine

import id.rona.app.domain.nlp.ConfidenceLevel
import id.rona.app.domain.nlp.DischargeSuggestion
import id.rona.app.domain.nlp.P1DischargeDescriptor
import id.rona.app.domain.nlp.dict.DischargeDictionary

/**
 * Discharge extraction: descriptive descriptors only, no diagnosis labels.
 * Anchors ("keputihan", "lendir", ...) gate extraction to keep precision high;
 * descriptor matches without an anchor are ignored.
 */
object DischargeExtractor {

    fun extract(normalizedText: String): DischargeSuggestion? {
        if (normalizedText.isBlank()) return emptyResultIfAnchored(normalizedText)

        val hasAnchor = DischargeDictionary.anchors.any { anchor ->
            SymptomExtractor.findWholeWordRange(normalizedText, anchor) != null
        }
        if (!hasAnchor) return null

        val descriptors = mutableSetOf<P1DischargeDescriptor>()
        val matchedTerms = mutableListOf<String>()

        val sortedRules = DischargeDictionary.rules
            .flatMap { rule -> rule.terms.map { rule to it } }
            .sortedByDescending { it.second.length }

        for ((rule, term) in sortedRules) {
            if (rule.descriptor in descriptors) continue
            val range = SymptomExtractor.findWholeWordRange(normalizedText, term) ?: continue
            if (NegationDetector.isNegated(normalizedText, range.first, term)) continue
            if (NegationDetector.isNegatedByExistence(normalizedText, term)) continue

            descriptors += rule.descriptor
            matchedTerms += term
        }

        if (descriptors.isEmpty()) return emptyResultIfAnchored(normalizedText)

        val itchingOrBurning = descriptors.any {
            it == P1DischargeDescriptor.ITCHING || it == P1DischargeDescriptor.BURNING
        }
        val odorConcern = descriptors.any {
            it == P1DischargeDescriptor.ODOR_FISHY
        }

        // Color descriptors beyond normal (clear/white) lower overall confidence.
        val confidence = when {
            descriptors.any {
                it in setOf(
                    P1DischargeDescriptor.YELLOW, P1DischargeDescriptor.GREEN,
                    P1DischargeDescriptor.GREY, P1DischargeDescriptor.BROWN,
                )
            } -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.HIGH
        }

        return DischargeSuggestion(
            descriptors = descriptors,
            itchingOrBurning = itchingOrBurning,
            odorConcern = odorConcern,
            confidence = confidence,
            matchedTerms = matchedTerms,
        )
    }

    private fun emptyResultIfAnchored(normalizedText: String): DischargeSuggestion? {
        val hasAnchor = DischargeDictionary.anchors.any { anchor ->
            SymptomExtractor.findWholeWordRange(normalizedText, anchor) != null
        }
        return if (hasAnchor) {
            DischargeSuggestion(
                descriptors = emptySet(),
                confidence = ConfidenceLevel.LOW,
                matchedTerms = emptyList(),
            )
        } else {
            null
        }
    }
}
