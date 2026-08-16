package id.rona.app.domain.nlp.engine

import id.rona.app.domain.nlp.ConfidenceLevel
import id.rona.app.domain.nlp.DurationSuggestion

/**
 * Deterministic duration extraction ("dari pagi", "seharian", "2 jam", "3 hari").
 */
object DurationExtractor {

    private val labelByPattern: List<Pair<Regex, String>> = listOf(
        Regex("\\b(\\d+)\\s*(jam|hari|minggu|menit)\\b") to "durasi_numeric",
        Regex("seharian") to "seharian",
        Regex("sejak tadi malam") to "sejak tadi malam",
        Regex("dari pagi") to "dari pagi",
        Regex("dari tadi") to "dari tadi",
        Regex("sepanjang hari") to "sepanjang hari",
        Regex("sejak kemarin") to "sejak kemarin",
        Regex("berhari hari") to "berhari hari",
    )

    fun extract(normalizedText: String): DurationSuggestion? {
        for ((regex, label) in labelByPattern) {
            val match = regex.find(normalizedText) ?: continue
            val rawTerm = normalizedText.substring(
                normalizedText.indexOf(match.value).coerceAtLeast(0),
                (normalizedText.indexOf(match.value) + match.value.length).coerceAtMost(normalizedText.length),
            )
            return DurationSuggestion(
                label = label,
                matchedTerms = listOf(rawTerm.trim()),
                confidence = ConfidenceLevel.HIGH,
            )
        }
        return null
    }
}
