package id.rona.app.domain.nlp.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NegationDetectorTest {

    private fun norm(text: String) = TextNormalizer.normalize(text)

    @Test
    fun tidakPusingIsNegated() {
        val text = norm("tidak pusing")
        val range = SymptomExtractor.findWholeWordRange(text, "pusing")!!
        assertThat(NegationDetector.isNegated(text, range.first, "pusing")).isTrue()
    }

    @Test
    fun sudahTidakMualIsNegated() {
        val text = norm("sudah tidak mual")
        val range = SymptomExtractor.findWholeWordRange(text, "mual")!!
        assertThat(NegationDetector.isNegated(text, range.first, "mual")).isTrue()
    }

    @Test
    fun nggakAdaBauAmisIsNegatedByExistence() {
        val text = norm("nggak ada bau amis")
        assertThat(NegationDetector.isNegatedByExistence(text, "bau amis")).isTrue()
    }

    @Test
    fun bukanHamilIsNegated() {
        val text = norm("bukan hamil")
        val range = SymptomExtractor.findWholeWordRange(text, "hamil")!!
        assertThat(NegationDetector.isNegated(text, range.first, "hamil")).isTrue()
    }

    @Test
    fun kramnyaSudahHilangIsResolved() {
        val text = norm("kramnya sudah hilang")
        val range = SymptomExtractor.findWholeWordRange(text, "kram")!!
        assertThat(NegationDetector.isNegated(text, range.first, "kram")).isTrue()
    }

    @Test
    fun negationDoesNotAffectDifferentClause() {
        val text = norm("tidak pusing tapi mual")
        val pusingRange = SymptomExtractor.findWholeWordRange(text, "pusing")!!
        val mualRange = SymptomExtractor.findWholeWordRange(text, "mual")!!
        assertThat(NegationDetector.isNegated(text, pusingRange.first, "pusing")).isTrue()
        assertThat(NegationDetector.isNegated(text, mualRange.first, "mual")).isFalse()
    }

    @Test
    fun positiveStatementIsNotNegated() {
        val text = norm("pusing sejak pagi")
        val range = SymptomExtractor.findWholeWordRange(text, "pusing")!!
        assertThat(NegationDetector.isNegated(text, range.first, "pusing")).isFalse()
    }

    @Test
    fun distantNegationDoesNotApply() {
        // "tidak" appears 5+ words before the term — outside the window.
        val text = norm("tidak jadi ke kantor hari ini karena perut kram")
        val range = SymptomExtractor.findWholeWordRange(text, "kram")!!
        assertThat(NegationDetector.isNegated(text, range.first, "kram")).isFalse()
    }

    @Test
    fun phraseNegationTidakTerlalu() {
        val text = norm("tidak terlalu pusing")
        val range = SymptomExtractor.findWholeWordRange(text, "pusing")!!
        assertThat(NegationDetector.isNegated(text, range.first, "pusing")).isTrue()
    }

    @Test
    fun resolvedMarkerSudahReda() {
        val text = norm("mual sudah reda")
        val range = SymptomExtractor.findWholeWordRange(text, "mual")!!
        assertThat(NegationDetector.isNegated(text, range.first, "mual")).isTrue()
    }

    @Test
    fun noFalseNegationOnAffirmativeSudah() {
        // "sudah" alone is not a resolved marker; "sudah mulai mual" stays positive.
        val text = norm("sudah mulai mual")
        val range = SymptomExtractor.findWholeWordRange(text, "mual")!!
        assertThat(NegationDetector.isNegated(text, range.first, "mual")).isFalse()
    }
}
