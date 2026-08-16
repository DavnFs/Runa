package id.rona.app.domain.nlp.engine

import id.rona.app.domain.model.Severity
import id.rona.app.domain.nlp.ConfidenceLevel

data class SeverityMatch(
    val severity: Severity,
    val matchedTerms: List<String>,
    val confidence: ConfidenceLevel,
)

/**
 * Contextual severity extraction (deterministic, ordered: strong > moderate > mild).
 * Multi-word modifiers ("tidak tahan", "sangat sakit") are matched as phrases.
 */
object SeverityExtractor {

    private val severeTerms = listOf(
        "tidak tahan", "sangat sakit", "nyeri sekali", "sakit sekali",
        "tidak bisa", "banget", "parah", "hebat", "sangat", "berat",
    )

    private val moderateTerms = listOf("lumayan parah", "lumayan", "cukup", "sedang")

    private val mildTerms = listOf("sedikit", "ringan", "dikit", "agak", "tipis", "kecil")

    private val impactTerms = listOf(
        "tidak bisa aktivitas", "sampai nangis", "harus tiduran",
    )

    private val clauseBoundaries = setOf("tapi", "tetapi", "namun", "dan", "atau", "lalu", "cuma")

    fun extract(normalizedText: String): SeverityMatch? {
        if (normalizedText.isBlank()) return null

        val severeFound = severeTerms.filter { containsWord(normalizedText, it) }
        val impactFound = impactTerms.filter { normalizedText.contains(it) }
        if (severeFound.isNotEmpty() || impactFound.isNotEmpty()) {
            return SeverityMatch(
                severity = Severity.SEVERE,
                matchedTerms = severeFound + impactFound,
                confidence = ConfidenceLevel.HIGH,
            )
        }

        val moderateFound = moderateTerms.filter { containsWord(normalizedText, it) }
        if (moderateFound.isNotEmpty()) {
            return SeverityMatch(
                severity = Severity.MODERATE,
                matchedTerms = moderateFound,
                confidence = ConfidenceLevel.MEDIUM,
            )
        }

        val mildFound = mildTerms.filter { containsWord(normalizedText, it) }
        if (mildFound.isNotEmpty()) {
            return SeverityMatch(
                severity = Severity.MILD,
                matchedTerms = mildFound,
                confidence = ConfidenceLevel.MEDIUM,
            )
        }

        return null
    }

    private fun containsWord(text: String, term: String): Boolean {
        val words = text.split(" ")
        val termWords = term.split(" ")
        if (words.size < termWords.size) return false
        for (i in 0..(words.size - termWords.size)) {
            if (words.subList(i, i + termWords.size) == termWords) return true
        }
        return false
    }

    /**
     * Severity for a specific matched term: scans words around the term's
     * first occurrence, respecting clause boundaries. Falls back to global [extract].
     */
    fun extractForTerm(normalizedText: String, term: String): SeverityMatch? {
        val words = normalizedText.split(" ")
        val termWords = term.split(" ")
        val termIndex = indexOfTerm(words, termWords) ?: return extract(normalizedText)

        val beforeRaw = words.subList((termIndex - 3).coerceAtLeast(0), termIndex)
        val afterRaw = words.subList(
            termIndex + termWords.size,
            (termIndex + termWords.size + 3).coerceAtMost(words.size),
        )
        // Respect clause boundaries: modifiers beyond "tapi/dan/..." belong
        // to a different clause and must not leak.
        val before = beforeRaw.drop(beforeRaw.indexOfLast { it in clauseBoundaries } + 1)
        val after = afterRaw.take(afterRaw.indexOfFirst { it in clauseBoundaries }
            .let { if (it == -1) afterRaw.size else it })
        val nearby = (before + after).joinToString(" ")

        severeTerms.firstOrNull { containsWord(nearby, it) }?.let {
            return SeverityMatch(Severity.SEVERE, listOf(it), ConfidenceLevel.HIGH)
        }
        moderateTerms.firstOrNull { containsWord(nearby, it) }?.let {
            return SeverityMatch(Severity.MODERATE, listOf(it), ConfidenceLevel.MEDIUM)
        }
        mildTerms.firstOrNull { containsWord(nearby, it) }?.let {
            return SeverityMatch(Severity.MILD, listOf(it), ConfidenceLevel.MEDIUM)
        }
        return extract(normalizedText)
    }

    private fun indexOfTerm(words: List<String>, term: List<String>): Int? {
        for (i in 0..(words.size - term.size).coerceAtLeast(0)) {
            if (words.subList(i, i + term.size) == term) return i
        }
        return null
    }
}
