package id.rona.app.domain.nlp.engine

import id.rona.app.domain.nlp.ConfidenceLevel
import id.rona.app.domain.nlp.HealthIntent
import id.rona.app.domain.nlp.IntentMatch
import id.rona.app.domain.nlp.dict.IntentDictionary

/**
 * Rule-based intent router for contextual help. Routes to structured
 * knowledge cards only — never generates free health answers.
 */
object IntentRouter {

    /**
     * Returns matched intents ordered by rule priority (dictionary order),
     * capped at [maxIntents]. Ambiguous phrasing keeps the rule's confidence.
     */
    fun route(normalizedText: String, maxIntents: Int = 3): List<IntentMatch> {
        if (normalizedText.isBlank()) return emptyList()

        val matches = mutableListOf<IntentMatch>()
        for (rule in IntentDictionary.rules) {
            val matchedTerms = rule.patterns.filter { pattern ->
                SymptomExtractor.findWholeWordRange(normalizedText, pattern) != null
            }
            if (matchedTerms.isNotEmpty()) {
                matches += IntentMatch(
                    intent = rule.intent,
                    confidence = rule.confidence,
                    matchedTerms = matchedTerms,
                )
            }
        }
        return matches.take(maxIntents)
    }
}
