package id.rona.app.domain.nlp.dict

import id.rona.app.domain.nlp.ConfidenceLevel
import id.rona.app.domain.nlp.HealthIntent

/**
 * Rule-based intent patterns for foundational contextual help.
 * Matched against the normalized text; each pattern has a confidence so
 * ambiguous phrasing stays MEDIUM/LOW instead of claiming certainty.
 */
data class IntentRule(
    val intent: HealthIntent,
    val patterns: List<String>,
    val confidence: ConfidenceLevel = ConfidenceLevel.HIGH,
)

object IntentDictionary {

    val rules: List<IntentRule> = listOf(
        IntentRule(
            HealthIntent.ASK_URGENT_HELP,
            listOf(
                "darah banyak", "darah tidak berhenti", "tembus tiap jam",
                "ganti pembalut tiap jam", "pingsan", "ingin melukai",
                "ingin mati", "tidak aman", "keluar darah", "perdarahan",
            ),
        ),
        IntentRule(
            HealthIntent.ASK_PREGNANCY_SYMPTOM,
            listOf(
                "hamil", "testpack positif", "positif testpack", "telat haid",
                "terlambat haid", "dua garis", "kemungkinan hamil",
                "telat", "terlambat",
            ),
            confidence = ConfidenceLevel.MEDIUM,
        ),
        IntentRule(
            HealthIntent.ASK_DISCHARGE,
            listOf(
                "keputihan", "lendir", "cairan vagina", "keputihan keluar",
                "lendir serviks",
            ),
        ),
        IntentRule(
            HealthIntent.ASK_PERIOD_PREDICTION,
            listOf(
                "kapan haid", "kapan menstruasi", "haid lagi", "menstruasi lagi",
                "kapan datang bulan", "prediksi haid", "kapan periode",
            ),
        ),
        IntentRule(
            HealthIntent.ASK_FERTILITY,
            listOf(
                "masa subur", "jendela subur", "ovulasi", "kesuburan",
                "kapan subur", "bisa hamil",
            ),
        ),
        IntentRule(
            HealthIntent.ASK_FOOD_OR_DRINK,
            listOf(
                "boleh makan", "boleh minum", "makanan", "minuman", "kopi",
                "teh", "cokelat", "pedas", "es",
            ),
        ),
        IntentRule(
            HealthIntent.ASK_CYCLE_STATUS,
            listOf(
                "hari ke", "fase apa", "sedang fase", "siklus ke", "hari siklus",
            ),
        ),
        IntentRule(
            HealthIntent.ASK_NORMALITY,
            listOf(
                "normal tidak", "apakah normal", "wajar tidak", "apakah wajar",
                "kenapa", "kok", "normal kah",
            ),
        ),
        IntentRule(
            HealthIntent.ASK_SELF_CARE,
            listOf(
                "mengatasi", "meredakan", "cara mengurangi", "tips",
                "apa yang bisa dilakukan", "gimana cara",
            ),
        ),
        IntentRule(
            HealthIntent.ASK_PRIVACY,
            listOf(
                "data", "privasi", "apakah tersimpan", "siapa yang bisa melihat",
                "aman tidak", "tersimpan di",
            ),
        ),
        IntentRule(
            HealthIntent.LOG_SYMPTOM,
            listOf("catat", "mencatat", "gejala", "hari ini terasa"),
            confidence = ConfidenceLevel.MEDIUM,
        ),
    )
}
