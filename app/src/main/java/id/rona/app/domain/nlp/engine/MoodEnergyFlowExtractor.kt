package id.rona.app.domain.nlp.engine

import id.rona.app.domain.model.Energy
import id.rona.app.domain.model.FlowLevel
import id.rona.app.domain.model.Mood
import id.rona.app.domain.nlp.ConfidenceLevel
import id.rona.app.domain.nlp.EnergySuggestion
import id.rona.app.domain.nlp.FlowSuggestion
import id.rona.app.domain.nlp.MoodSuggestion
import id.rona.app.domain.nlp.dict.EnergyDictionary
import id.rona.app.domain.nlp.dict.FlowDictionary
import id.rona.app.domain.nlp.dict.MoodDictionary

/**
 * Mood + energy + flow extraction from normalized text.
 * Mood and energy are independent axes in the MVP model.
 */
object MoodEnergyFlowExtractor {

    fun extractMoods(
        normalizedText: String,
        alreadySelected: Mood? = null,
    ): List<MoodSuggestion> {
        if (normalizedText.isBlank()) return emptyList()

        val results = mutableListOf<MoodSuggestion>()
        val matched = mutableSetOf<Mood>()

        // Exact dictionary rules first (longest term first).
        val sortedRules = MoodDictionary.rules
            .flatMap { rule -> rule.terms.map { rule to it } }
            .sortedByDescending { it.second.length }

        for ((rule, term) in sortedRules) {
            if (rule.mood in matched) continue
            if (rule.mood == alreadySelected) continue
            val range = SymptomExtractor.findWholeWordRange(normalizedText, term) ?: continue
            if (NegationDetector.isNegated(normalizedText, range.first, term)) continue

            matched += rule.mood
            results += MoodSuggestion(
                mood = rule.mood,
                confidence = ConfidenceLevel.HIGH,
                matchedTerms = listOf(term),
            )
        }

        // Emotional descriptors (sensitif, cemas, ...) — MEDIUM confidence,
        // mapped to the closest MVP mood axis.
        for ((term, mood) in MoodDictionary.emotionalDescriptors) {
            if (mood in matched) continue
            if (mood == alreadySelected) continue
            val range = SymptomExtractor.findWholeWordRange(normalizedText, term) ?: continue
            if (NegationDetector.isNegated(normalizedText, range.first, term)) continue

            matched += mood
            results += MoodSuggestion(
                mood = mood,
                confidence = ConfidenceLevel.MEDIUM,
                matchedTerms = listOf(term),
            )
        }
        return results
    }

    fun extractEnergy(
        normalizedText: String,
        alreadySelected: Energy? = null,
    ): EnergySuggestion? {
        if (normalizedText.isBlank()) return null

        val sortedRules = EnergyDictionary.rules
            .flatMap { rule -> rule.terms.map { rule to it } }
            .sortedByDescending { it.second.length }

        for ((rule, term) in sortedRules) {
            if (rule.energy == alreadySelected) continue
            val range = SymptomExtractor.findWholeWordRange(normalizedText, term) ?: continue
            if (NegationDetector.isNegated(normalizedText, range.first, term)) continue
            return EnergySuggestion(
                energy = rule.energy,
                confidence = ConfidenceLevel.HIGH,
                matchedTerms = listOf(term),
            )
        }
        return null
    }

    fun extractFlow(
        normalizedText: String,
        alreadySelected: FlowLevel? = null,
    ): FlowSuggestion? {
        if (normalizedText.isBlank()) return null

        val sortedRules = FlowDictionary.rules
            .flatMap { rule -> rule.terms.map { rule to it } }
            .sortedByDescending { it.second.length }

        for ((rule, term) in sortedRules) {
            if (rule.flow == alreadySelected) continue
            val range = SymptomExtractor.findWholeWordRange(normalizedText, term) ?: continue
            if (NegationDetector.isNegated(normalizedText, range.first, term)) continue
            return FlowSuggestion(
                flow = rule.flow,
                confidence = ConfidenceLevel.HIGH,
                matchedTerms = listOf(term),
            )
        }
        return null
    }
}
