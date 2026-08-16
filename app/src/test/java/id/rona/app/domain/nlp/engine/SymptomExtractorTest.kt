package id.rona.app.domain.nlp.engine

import com.google.common.truth.Truth.assertThat
import id.rona.app.domain.model.Severity
import id.rona.app.domain.model.SymptomType
import org.junit.Test

class SymptomExtractorTest {

    private fun analyze(note: String) = SymptomExtractor.extract(TextNormalizer.normalize(note))

    @Test
    fun extractsKram() {
        val result = analyze("perut kram sejak pagi")
        assertThat(result.map { it.symptomType }).contains(SymptomType.KRAM)
    }

    @Test
    fun extractsLongestPhraseNotSubstring() {
        val result = analyze("kram perut")
        val kram = result.single { it.symptomType == SymptomType.KRAM }
        assertThat(kram.matchedTerms).contains("kram perut")
    }

    @Test
    fun extractsPusingAsHeadache() {
        val result = analyze("pusing sekali hari ini")
        assertThat(result.map { it.symptomType }).contains(SymptomType.HEADACHE)
    }

    @Test
    fun extractsMual() {
        val result = analyze("mual sejak bangun tidur")
        assertThat(result.map { it.symptomType }).contains(SymptomType.NAUSEA)
    }

    @Test
    fun extractsMuntahAsSevereNausea() {
        val result = analyze("muntah terus pagi ini")
        val nausea = result.single { it.symptomType == SymptomType.NAUSEA }
        assertThat(nausea.severity).isEqualTo(Severity.SEVERE)
    }

    @Test
    fun extractsLelah() {
        val result = analyze("lelah banget hari ini")
        assertThat(result.map { it.symptomType }).contains(SymptomType.FATIGUE)
    }

    @Test
    fun extractsKembung() {
        val result = analyze("perut kembung setelah makan")
        assertThat(result.map { it.symptomType }).contains(SymptomType.BLOATING)
    }

    @Test
    fun extractsSleepDifficulty() {
        val result = analyze("susah tidur semalam")
        assertThat(result.map { it.symptomType }).contains(SymptomType.SLEEP_ISSUE)
    }

    @Test
    fun extractsBackPain() {
        val result = analyze("sakit punggung dari pagi")
        assertThat(result.map { it.symptomType }).contains(SymptomType.BACKACHE)
    }

    @Test
    fun extractsBreastTenderness() {
        val result = analyze("payudara nyeri kalau disentuh")
        assertThat(result.map { it.symptomType }).contains(SymptomType.BREAST_TENDERNESS)
    }

    @Test
    fun extractsAcne() {
        val result = analyze("jerawatan muncul di dagu")
        assertThat(result.map { it.symptomType }).contains(SymptomType.ACNE)
    }

    @Test
    fun extractsAppetiteChange() {
        val result = analyze("nafsu makan naik drastis")
        assertThat(result.map { it.symptomType }).contains(SymptomType.APPETITE_CHANGE)
    }

    @Test
    fun mixedSymptomsAllExtracted() {
        val result = analyze("kram perut, pusing, mual, lelah")
        val types = result.map { it.symptomType }
        assertThat(types).contains(SymptomType.KRAM)
        assertThat(types).contains(SymptomType.HEADACHE)
        assertThat(types).contains(SymptomType.NAUSEA)
        assertThat(types).contains(SymptomType.FATIGUE)
    }

    @Test
    fun negatedSymptomNotExtracted() {
        val result = analyze("tidak pusing tapi mual")
        assertThat(result.map { it.symptomType }).doesNotContain(SymptomType.HEADACHE)
        assertThat(result.map { it.symptomType }).contains(SymptomType.NAUSEA)
    }

    @Test
    fun resolvedSymptomNotExtracted() {
        val result = analyze("kramnya sudah hilang")
        assertThat(result.map { it.symptomType }).doesNotContain(SymptomType.KRAM)
    }

    @Test
    fun severityFromContextualModifier() {
        val result = analyze("kram parah banget")
        val kram = result.single { it.symptomType == SymptomType.KRAM }
        assertThat(kram.severity).isEqualTo(Severity.SEVERE)
    }

    @Test
    fun mildSeverityFromContextualModifier() {
        val result = analyze("sedikit mual")
        val nausea = result.single { it.symptomType == SymptomType.NAUSEA }
        assertThat(nausea.severity).isEqualTo(Severity.MILD)
    }

    @Test
    fun ambiguousStatementYieldsMediumOrLowConfidence() {
        val result = analyze("mungkin agak lelah")
        val fatigue = result.single { it.symptomType == SymptomType.FATIGUE }
        assertThat(fatigue.confidence).isNotEqualTo(id.rona.app.domain.nlp.ConfidenceLevel.HIGH)
    }

    @Test
    fun alreadySelectedSymptomIsSkipped() {
        val result = SymptomExtractor.extract(
            TextNormalizer.normalize("kram perut dan pusing"),
            alreadySelected = setOf(SymptomType.KRAM),
        )
        assertThat(result.map { it.symptomType }).doesNotContain(SymptomType.KRAM)
        assertThat(result.map { it.symptomType }).contains(SymptomType.HEADACHE)
    }

    @Test
    fun substringMatchIsAvoided() {
        // "pusing" inside "pusingan" style word should not match; here the
        // whole-word boundary prevents matching inside longer words.
        val result = analyze("kepusingan cari kunci")
        assertThat(result.map { it.symptomType }).doesNotContain(SymptomType.HEADACHE)
    }

    @Test
    fun emptyNoteYieldsNothing() {
        assertThat(analyze("")).isEmpty()
    }

    @Test
    fun slangTermIsRecognizedAfterNormalization() {
        val result = analyze("puyeng dan mules")
        assertThat(result.map { it.symptomType }).contains(SymptomType.HEADACHE)
        assertThat(result.map { it.symptomType }).contains(SymptomType.KRAM)
    }

    @Test
    fun noOverlappingDuplicates() {
        val result = analyze("kram perut sakit")
        val kramCount = result.count { it.symptomType == SymptomType.KRAM }
        assertThat(kramCount).isEqualTo(1)
    }
}
