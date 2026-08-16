package id.rona.app.domain.nlp.engine

import java.text.Normalizer
import java.util.Locale

/**
 * Deterministic, offline Indonesian text normalization.
 *
 * Produces a normalized copy for ephemeral analysis only. The user's raw note
 * is never modified (the function has no side effects).
 *
 * Steps (order matters):
 * 1. Unicode NFKD + strip diacritics (keeps emoji intact).
 * 2. Lowercase locale-safe.
 * 3. Phrase slang normalization (longest phrase first).
 * 4. Word-boundary single-word slang normalization.
 * 5. Conservative possessive-suffix stripping ("-nya").
 * 6. Repeated-character collapsing (3+ identical letters -> 1).
 * 7. Punctuation/emoji folding and whitespace normalization.
 */
object TextNormalizer {

    /** Multi-word slang replacements (longest first). */
    private val phraseMap: Map<String, String> = mapOf(
        // negations (multi-word first)
        "nggak ada" to "tidak ada",
        "gak ada" to "tidak ada",
        "ga ada" to "tidak ada",
        "gk ada" to "tidak ada",
        "tak ada" to "tidak ada",
        "nggak ada" to "tidak ada",
        // pregnancy
        "positif testpack" to "testpack positif",
        "tespek positif" to "testpack positif",
        "test pack positif" to "testpack positif",
        "terlambat haid" to "telat haid",
        "terlambat menstruasi" to "telat haid",
        "telat menstruasi" to "telat haid",
        "keluar darah saat hamil" to "keluar darah saat hamil",
        "flek saat hamil" to "flek saat hamil",
        "darah saat hamil" to "darah saat hamil",
        // discharge phrases
        "cairan vagina" to "keputihan",
        "cairan kewanitaan" to "keputihan",
        "bau ikan" to "bau amis",
        "berbau amis" to "bau amis",
        "berbau" to "berbau",
        // intensity
        "sangat sakit" to "sangat sakit",
        "sampai tidak bisa" to "tidak bisa",
        "tidak bisa kerja" to "tidak bisa aktivitas",
        "tidak bisa sekolah" to "tidak bisa aktivitas",
        "tidak bisa aktivitas" to "tidak bisa aktivitas",
        "susah kerja" to "tidak bisa aktivitas",
        "harus tiduran" to "tidak bisa aktivitas",
        // pain phrases
        "nyeri perut bawah" to "perut bawah sakit",
        "sakit perut bawah" to "perut bawah sakit",
        "sakit di bawah pusar" to "perut bawah sakit",
        "sakit punggung" to "sakit punggung",
        "nyeri punggung" to "sakit punggung",
        "sakit pinggang" to "sakit pinggang",
        "nyeri pinggang" to "sakit pinggang",
        "nyeri payudara" to "nyeri payudara",
        "payudara sakit" to "nyeri payudara",
        "payudara nyeri" to "nyeri payudara",
        // fatigue / sleep
        "tidak bertenaga" to "energi habis",
        "ngantuk terus" to "ngantuk terus",
        "sulit tidur" to "susah tidur",
        "tidur tidak nyenyak" to "tidur tidak nyenyak",
        "sering terbangun" to "kebangun terus",
        "susah tidur nyenyak" to "susah tidur",
        "makan terus" to "makan terus",
        "nafsu makan berubah" to "nafsu makan berubah",
        "nafsu makan naik" to "nafsu makan naik",
        "nafsu makan turun" to "nafsu makan turun",
        "tidak nafsu makan" to "tidak nafsu makan",
        "selera makan hilang" to "tidak nafsu makan",
        // mood
        "bad mood" to "mood jelek",
        "mood buruk" to "mood jelek",
        "mood bagus" to "mood baik",
        "mudah marah" to "mudah marah",
    )

    /** Single-word slang replacements applied with strict word boundaries. */
    private val wordMap: Map<String, String> = mapOf(
        "nggak" to "tidak",
        "gak" to "tidak",
        "gk" to "tidak",
        "ga" to "tidak",
        "enggak" to "tidak",
        "tak" to "tidak",
        "bgt" to "banget",
        "puyeng" to "pusing",
        "mules" to "mulas",
        "lemes" to "lemas",
        "cape" to "lelah",
        "capek" to "lelah",
        "ngantuk" to "mengantuk",
        "emosian" to "emosian",
        "pengen" to "ingin",
        "kudu" to "harus",
    )

    private val letterOrDigit = Regex("[\\p{L}\\p{N}]")

    fun normalize(input: String): String {
        if (input.isBlank()) return ""

        var text = input

        // 1. Unicode NFKD, strip combining marks (emoji survive).
        text = Normalizer.normalize(text, Normalizer.Form.NFKD)
            .replace(Regex("\\p{Mn}+"), "")

        // 2. Lowercase locale-safe.
        text = text.lowercase(Locale.ROOT)

        // 3. Phrase slang map — longest key first so multi-word phrases win.
        val phraseKeys = phraseMap.keys.sortedByDescending { it.length }
        for (from in phraseKeys) {
            text = text.replace(from, phraseMap.getValue(from))
        }

        // 4. Single-word slang with strict word boundaries ("ga" must not
        //    eat "agak" or "gatal").
        for ((from, to) in wordMap) {
            text = text.replace(Regex("(?<![\\p{L}\\p{N}])${Regex.escape(from)}(?![\\p{L}\\p{N}])"), to)
        }
        text = text.split(Regex("\\s+"))
            .joinToString(" ") { word -> stripPossessiveSuffix(word) }

        // 6. Repeated-character collapsing: 3+ identical letters -> 1.
        text = text.split(Regex("\\s+"))
            .joinToString(" ") { word -> collapseRepeats(word) }

        // 7. Punctuation & emoji folding: keep word boundaries, drop symbols.
        text = text.replace(Regex("[\\p{Punct}—–…]+"), " ")
        text = text.replace(Regex("[\\p{So}\\p{Sk}]+"), " ")

        // 8. Whitespace normalization.
        return text.replace(Regex("\\s+"), " ").trim()
    }

    private fun stripPossessiveSuffix(word: String): String {
        if (word.length > 4 && word.endsWith("nya") && word.dropLast(3).all { it.isLetter() }) {
            return word.dropLast(3)
        }
        return word
    }

    private fun collapseRepeats(word: String): String {
        if (word.length < 3) return word
        // Conservative elongation stripping: only words that contain a run of
        // 3+ identical letters are "elongated". For those, collapse ALL runs
        // of 2+ down to 1. Valid words like "menggumpal" or "mood" (double
        // letters only) stay untouched.
        val hasLongRun = (1 until word.length - 1).any { i ->
            word[i] == word[i - 1] && word[i] == word[i + 1] && word[i].isLetter()
        }
        if (!hasLongRun) return word

        val sb = StringBuilder()
        for (i in word.indices) {
            if (i > 0 && word[i] == word[i - 1] && word[i].isLetter()) {
                // skip repeated letter
            } else {
                sb.append(word[i])
            }
        }
        return sb.toString()
    }
}
