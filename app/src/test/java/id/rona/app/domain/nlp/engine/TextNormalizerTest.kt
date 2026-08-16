package id.rona.app.domain.nlp.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TextNormalizerTest {

    @Test
    fun emptyInputReturnsEmpty() {
        assertThat(TextNormalizer.normalize("")).isEmpty()
    }

    @Test
    fun blankInputReturnsEmpty() {
        assertThat(TextNormalizer.normalize("   \n\t  ")).isEmpty()
    }

    @Test
    fun lowercasesLocaleSafe() {
        assertThat(TextNormalizer.normalize("SAKIT Kepala")).isEqualTo("sakit kepala")
    }

    @Test
    fun trimsAndNormalizesWhitespace() {
        assertThat(TextNormalizer.normalize("  kram   perut \n  hari ini ")).isEqualTo("kram perut hari ini")
    }

    @Test
    fun normalizesSlangGakVariants() {
        assertThat(TextNormalizer.normalize("gak pusing")).isEqualTo("tidak pusing")
        assertThat(TextNormalizer.normalize("ga pusing")).isEqualTo("tidak pusing")
        assertThat(TextNormalizer.normalize("nggak pusing")).isEqualTo("tidak pusing")
        assertThat(TextNormalizer.normalize("gk pusing")).isEqualTo("tidak pusing")
    }

    @Test
    fun normalizesBgtToBanget() {
        assertThat(TextNormalizer.normalize("sakit bgt")).isEqualTo("sakit banget")
    }

    @Test
    fun normalizesPuyengMulesCape() {
        assertThat(TextNormalizer.normalize("puyeng")).isEqualTo("pusing")
        assertThat(TextNormalizer.normalize("mules")).isEqualTo("mulas")
        assertThat(TextNormalizer.normalize("capek")).isEqualTo("lelah")
        assertThat(TextNormalizer.normalize("cape")).isEqualTo("lelah")
        assertThat(TextNormalizer.normalize("lemes")).isEqualTo("lemas")
        assertThat(TextNormalizer.normalize("ngantuk")).isEqualTo("mengantuk")
    }

    @Test
    fun collapsesRepeatedCharactersConservatively() {
        assertThat(TextNormalizer.normalize("sakiittt")).isEqualTo("sakit")
        assertThat(TextNormalizer.normalize("bangettt")).isEqualTo("banget")
        assertThat(TextNormalizer.normalize("sakit kepalaaa")).isEqualTo("sakit kepala")
    }

    @Test
    fun doesNotCollapseDoubleLettersInValidWords() {
        // "makan" has no double; "banyak" stays; only 3+ runs collapse.
        assertThat(TextNormalizer.normalize("banyak")).isEqualTo("banyak")
        assertThat(TextNormalizer.normalize("telat")).isEqualTo("telat")
    }

    @Test
    fun stripsPunctuation() {
        assertThat(TextNormalizer.normalize("kram, perut! sakit?"))
            .isEqualTo("kram perut sakit")
        assertThat(TextNormalizer.normalize("pusing... banget...")).isEqualTo("pusing banget")
    }

    @Test
    fun stripsEmoji() {
        assertThat(TextNormalizer.normalize("kram perut 😭 hari ini"))
            .isEqualTo("kram perut hari ini")
    }

    @Test
    fun handlesMixedIndonesianEnglish() {
        assertThat(TextNormalizer.normalize("bad mood terus, gak strong")).isEqualTo("mood jelek terus tidak strong")
    }

    @Test
    fun rawInputIsNeverMutated() {
        val raw = "Sakiittt BGT 😭"
        TextNormalizer.normalize(raw)
        assertThat(raw).isEqualTo("Sakiittt BGT 😭")
    }

    @Test
    fun normalizesPerutBawahPhrase() {
        // "sakit perut bawah" folds into the canonical "perut bawah sakit" context phrase.
        assertThat(TextNormalizer.normalize("nyeri perut bawah")).isEqualTo("perut bawah sakit")
    }

    @Test
    fun keepsWordBoundariesIntact() {
        // "ga" inside "gado" must not be replaced.
        assertThat(TextNormalizer.normalize("makan gado-gado")).isEqualTo("makan gado gado")
    }

    @Test
    fun normalizesSusahKerjaToActivityImpact() {
        assertThat(TextNormalizer.normalize("susah kerja karena kram"))
            .isEqualTo("tidak bisa aktivitas karena kram")
    }

    @Test
    fun normalizesTestpackVariants() {
        assertThat(TextNormalizer.normalize("testpack positif")).isEqualTo("testpack positif")
        assertThat(TextNormalizer.normalize("positif testpack")).isEqualTo("testpack positif")
    }

    @Test
    fun normalizesExistenceNegation() {
        assertThat(TextNormalizer.normalize("nggak ada bau amis")).isEqualTo("tidak ada bau amis")
    }

    @Test
    fun normalizesTelatHaidVariants() {
        assertThat(TextNormalizer.normalize("terlambat haid")).isEqualTo("telat haid")
    }
}
