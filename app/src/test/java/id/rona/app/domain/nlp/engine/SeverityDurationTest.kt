package id.rona.app.domain.nlp.engine

import com.google.common.truth.Truth.assertThat
import id.rona.app.domain.model.Severity
import id.rona.app.domain.nlp.ConfidenceLevel
import org.junit.Test

class SeverityDurationTest {

    @Test
    fun severeFromBanget() {
        val match = SeverityExtractor.extract(TextNormalizer.normalize("kram banget"))
        assertThat(match!!.severity).isEqualTo(Severity.SEVERE)
    }

    @Test
    fun severeFromParah() {
        val match = SeverityExtractor.extract(TextNormalizer.normalize("nyeri parah"))
        assertThat(match!!.severity).isEqualTo(Severity.SEVERE)
    }

    @Test
    fun severeFromTidakTahan() {
        val match = SeverityExtractor.extract(TextNormalizer.normalize("sakit tidak tahan"))
        assertThat(match!!.severity).isEqualTo(Severity.SEVERE)
    }

    @Test
    fun severeFromImpact() {
        val match = SeverityExtractor.extract(TextNormalizer.normalize("sampai nangis"))
        assertThat(match!!.severity).isEqualTo(Severity.SEVERE)
    }

    @Test
    fun moderateFromLumayan() {
        val match = SeverityExtractor.extract(TextNormalizer.normalize("lumayan mual"))
        assertThat(match!!.severity).isEqualTo(Severity.MODERATE)
    }

    @Test
    fun mildFromSedikit() {
        val match = SeverityExtractor.extract(TextNormalizer.normalize("sedikit lelah"))
        assertThat(match!!.severity).isEqualTo(Severity.MILD)
    }

    @Test
    fun mildFromAgak() {
        val match = SeverityExtractor.extract(TextNormalizer.normalize("agak pusing"))
        assertThat(match!!.severity).isEqualTo(Severity.MILD)
    }

    @Test
    fun nullWhenNoModifier() {
        val match = SeverityExtractor.extract(TextNormalizer.normalize("hari biasa saja"))
        assertThat(match).isNull()
    }

    @Test
    fun termProximityOverridesGlobal() {
        // "parah" is far from "mual"; "sedikit" is adjacent -> MILD.
        val match = SeverityExtractor.extractForTerm(
            TextNormalizer.normalize("sedikit mual tapi kramnya parah"),
            "mual",
        )
        assertThat(match!!.severity).isEqualTo(Severity.MILD)
    }

    @Test
    fun durationNumericHours() {
        val d = DurationExtractor.extract(TextNormalizer.normalize("kram sudah 2 jam"))
        assertThat(d).isNotNull()
        assertThat(d!!.matchedTerms).isNotEmpty()
    }

    @Test
    fun durationSeharian() {
        val d = DurationExtractor.extract(TextNormalizer.normalize("lelah seharian"))
        assertThat(d!!.label).isEqualTo("seharian")
    }

    @Test
    fun durationDariPagi() {
        val d = DurationExtractor.extract(TextNormalizer.normalize("pusing dari pagi"))
        assertThat(d!!.label).isEqualTo("dari pagi")
    }

    @Test
    fun durationSejakTadiMalam() {
        val d = DurationExtractor.extract(TextNormalizer.normalize("tidur tidak nyenyak sejak tadi malam"))
        assertThat(d!!.label).isEqualTo("sejak tadi malam")
    }

    @Test
    fun durationThreeDays() {
        val d = DurationExtractor.extract(TextNormalizer.normalize("kram 3 hari"))
        assertThat(d).isNotNull()
    }

    @Test
    fun noDurationReturnsNull() {
        val d = DurationExtractor.extract(TextNormalizer.normalize("hari ini biasa saja"))
        assertThat(d).isNull()
    }

    @Test
    fun durationConfidenceIsHigh() {
        val d = DurationExtractor.extract(TextNormalizer.normalize("mual 2 jam"))
        assertThat(d!!.confidence).isEqualTo(ConfidenceLevel.HIGH)
    }
}
