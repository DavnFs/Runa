package id.rona.app.domain.nlp.engine

import id.rona.app.domain.nlp.dict.NegationDictionary

/**
 * Negation detection over normalized text.
 *
 * Strategy (precision-first, deterministic):
 * - A match is NEGATED when a negation marker appears in the 3 words
 *   immediately before the matched span, within the same clause.
 * - Clause boundaries ("tapi", "tetapi", "namun", ...) reset the window so a
 *   negation in another clause does not leak ("tidak pusing tapi mual").
 * - A match is RESOLVED when a resolved marker appears in the 4 words before
 *   OR after the span ("kramnya sudah hilang").
 * - Existence negations ("tidak ada X") cancel descriptor matches up to 4
 *   words after the marker; detected as word bigrams ("tidak ada").
 */
object NegationDetector {

    private const val NEGATION_WINDOW_BEFORE = 3
    private const val EXISTENCE_WINDOW_AFTER = 4
    private const val RESOLVED_WINDOW = 4

    private val clauseBoundaries = setOf(
        "tapi", "tetapi", "namun", "cuma", "sedangkan", "meski", "meskipun",
        "walaupun", "walau", "lalu", "kemudian", "dan", "atau", "tapi juga",
        ".", ",", ";",
    )

    fun isNegated(normalizedText: String, termStartIndex: Int, term: String): Boolean {
        val words = normalizedText.split(" ")
        val termWordCount = term.split(" ").size
        val firstTermWordIndex = wordIndexAtOffset(words, termStartIndex) ?: return false

        // Words before the term within the negation window, but only those
        // after the last clause boundary (negation does not cross clauses).
        val windowStart = (firstTermWordIndex - NEGATION_WINDOW_BEFORE).coerceAtLeast(0)
        val beforeWords = words.subList(windowStart, firstTermWordIndex)
        val afterClauseBoundary = beforeWords.indexOfLast { it in clauseBoundaries }
        val scopedBefore = if (afterClauseBoundary >= 0) {
            beforeWords.subList(afterClauseBoundary + 1, beforeWords.size)
        } else {
            beforeWords
        }

        if (scopedBefore.any { it in NegationDictionary.simpleNegations }) return true
        val scopedJoined = scopedBefore.joinToString(" ")
        if (NegationDictionary.phraseNegations.any { scopedJoined.endsWith(it) }) return true

        // Resolved markers before the term.
        val resolvedStart = (firstTermWordIndex - RESOLVED_WINDOW).coerceAtLeast(0)
        val resolvedBefore = words.subList(resolvedStart, firstTermWordIndex)
        if (resolvedBefore.any { it in NegationDictionary.resolvedMarkers }) return true

        // Resolved markers after the term ("kram sudah hilang").
        val resolvedAfterStart = firstTermWordIndex + termWordCount
        val resolvedAfterEnd = (resolvedAfterStart + RESOLVED_WINDOW).coerceAtMost(words.size)
        if (resolvedAfterStart < words.size) {
            val resolvedAfter = words.subList(resolvedAfterStart, resolvedAfterEnd)
            if (resolvedAfter.any { it in NegationDictionary.resolvedMarkers }) return true
        }

        return false
    }

    /**
     * True when an existence negation ("tidak ada X") covers the term.
     * Detects markers as bigrams ("tidak ada") and single tokens ("tanpa").
     */
    fun isNegatedByExistence(normalizedText: String, term: String): Boolean {
        val words = normalizedText.split(" ")
        if (words.size < 2) return false
        val termWords = term.split(" ")

        words.forEachIndexed { index, word ->
            val markerAt = when {
                // Bigram marker: "tidak ada", "nggak ada" (normalized), etc.
                index + 1 < words.size &&
                    "${words[index]} ${words[index + 1]}" in NegationDictionary.existenceNegations -> index
                word in NegationDictionary.existenceNegations -> index
                else -> -1
            }
            if (markerAt >= 0) {
                val windowEnd = (markerAt + 1 + EXISTENCE_WINDOW_AFTER).coerceAtMost(words.size)
                val afterWords = words.subList(markerAt + 1, windowEnd)
                for (start in 0..(afterWords.size - termWords.size).coerceAtLeast(0)) {
                    if (afterWords.subList(start, start + termWords.size) == termWords) return true
                }
            }
        }
        return false
    }

    /**
     * Maps a character offset to the index of the word containing it.
     */
    private fun wordIndexAtOffset(words: List<String>, charIndex: Int): Int? {
        var offset = 0
        words.forEachIndexed { index, word ->
            val wordStart = offset
            val wordEnd = offset + word.length
            if (charIndex in wordStart..wordEnd) return index
            offset = wordEnd + 1
        }
        return null
    }
}
