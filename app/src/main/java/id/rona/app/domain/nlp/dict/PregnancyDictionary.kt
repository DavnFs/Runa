package id.rona.app.domain.nlp.dict

import id.rona.app.domain.nlp.PregnancyContextKind

/** Typed pregnancy-context rules. All confirmatory wording is avoided. */
data class PregnancyRule(
    val kind: PregnancyContextKind,
    val terms: List<String>,
)

object PregnancyDictionary {

    val rules: List<PregnancyRule> = listOf(
        PregnancyRule(
            PregnancyContextKind.TEST_POSITIVE,
            listOf("positif testpack", "testpack positif", "tespek positif", "test pack positif", "dua garis"),
        ),
        PregnancyRule(
            PregnancyContextKind.SUSPECTED,
            listOf("telat haid", "terlambat haid", "terlambat menstruasi", "telat menstruasi", "kemungkinan hamil", "mungkin hamil", "hamil"),
        ),
        PregnancyRule(
            PregnancyContextKind.BLEEDING_IN_PREGNANCY,
            listOf("keluar darah saat hamil", "flek saat hamil", "darah saat hamil", "perdarahan saat hamil", "bercak saat hamil"),
        ),
    )

    val redFlagTerms: Map<String, PregnancyRedFlag> = mapOf(
        "keluar darah saat hamil" to PregnancyRedFlag.BLEEDING,
        "flek saat hamil" to PregnancyRedFlag.BLEEDING,
        "darah saat hamil" to PregnancyRedFlag.BLEEDING,
        "perdarahan saat hamil" to PregnancyRedFlag.BLEEDING,
        "bercak saat hamil" to PregnancyRedFlag.BLEEDING,
        "nyeri sebelah" to PregnancyRedFlag.ONE_SIDED_PAIN,
        "sakit perut satu sisi" to PregnancyRedFlag.ONE_SIDED_PAIN,
        "nyeri perut satu sisi" to PregnancyRedFlag.ONE_SIDED_PAIN,
        "pingsan" to PregnancyRedFlag.FAINTING,
        "pandangan kabur" to PregnancyRedFlag.VISUAL_CHANGE,
        "melihat bintik" to PregnancyRedFlag.VISUAL_CHANGE,
        "penglihatan kabur" to PregnancyRedFlag.VISUAL_CHANGE,
        "demam" to PregnancyRedFlag.FEVER,
        "cairan keluar banyak" to PregnancyRedFlag.FLUID_LEAK,
        "sakit kepala hebat" to PregnancyRedFlag.SEVERE_HEADACHE,
        "sakit kepala parah" to PregnancyRedFlag.SEVERE_HEADACHE,
        "sakit kepala berat" to PregnancyRedFlag.SEVERE_HEADACHE,
    )

    enum class PregnancyRedFlag {
        BLEEDING,
        SEVERE_HEADACHE,
        VISUAL_CHANGE,
        ONE_SIDED_PAIN,
        FAINTING,
        FEVER,
        FLUID_LEAK,
    }
}
