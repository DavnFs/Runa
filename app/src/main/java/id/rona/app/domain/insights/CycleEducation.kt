package id.rona.app.domain.insights

import id.rona.app.domain.engine.CycleEngine
import id.rona.app.domain.engine.CyclePhase
import id.rona.app.domain.engine.StatisticsEngine

/**
 * A single education topic shown in the "Memahami siklusmu" sheet.
 * Content is sourced from [id.rona.app.domain.nlp.knowledge.KnowledgeCardSeed]
 * and mapped here for the Insights UI. No medical claims, no fertility guarantees.
 */
data class CycleEducationTopic(
    val id: String,
    val title: String,
    val summary: String,
    val details: List<String>,
    val disclaimer: String?,
    val sourceLabel: InsightSourceLabel = InsightSourceLabel.GENERAL_EDUCATION,
    val isPhaseSpecific: Boolean = false,
    val requiresMaturity: InsightMaturityLevel? = null,
)

/**
 * Pure domain provider. Maps existing knowledge card content to education topics,
 * gated by maturity level. No Android imports, no network, no schema dependency.
 */
object CycleEducationProvider {

    private const val MANDATORY_DISCLAIMER =
        "Perkiraan kalender tidak dapat memastikan ovulasi dan tidak ditujukan sebagai metode kontrasepsi atau jaminan kehamilan."

    private val generalTopics = listOf(
        CycleEducationTopic(
            id = "edu_menstruation",
            title = "Fase menstruasi",
            summary = "Ini fase awal siklus. Energi dan mood setiap orang bisa berbeda — tidak ada 'seharusnya' tertentu.",
            details = listOf(
                "Istirahat dan hidrasi sesuai kebutuhan.",
                "Kram ringan hingga sedang dapat terjadi saat menstruasi.",
                "Nyeri berat atau perdarahan jauh lebih banyak dari biasanya — pertimbangkan bicara dengan tenaga kesehatan.",
            ),
            disclaimer = "Ini informasi umum, bukan saran medis.",
            isPhaseSpecific = true,
        ),
        CycleEducationTopic(
            id = "edu_follicular",
            title = "Fase folikular",
            summary = "Setelah menstruasi, sebagian orang merasa energi meningkat. Ini gambaran umum, bukan aturan.",
            details = listOf(
                "Gunakan momen berenergi untuk aktivitas yang kamu sukai.",
                "Tidur teratur dan gerak ringan sesuai kenyamanan bisa membantu.",
            ),
            disclaimer = "Ini informasi umum, bukan saran medis.",
            isPhaseSpecific = true,
        ),
        CycleEducationTopic(
            id = "edu_ovulation_est",
            title = "Estimasi jendela ovulasi",
            summary = "Aplikasi hanya bisa memperkirakan rentang, bukan hari pasti. Ini bukan alat kontrasepsi.",
            details = listOf(
                "Gunakan sebagai gambaran pola, bukan kepastian.",
                "Ovulasi tidak dapat dipastikan hanya dari prediksi kalender.",
                "Untuk keputusan kontrasepsi atau merencanakan kehamilan, bicarakan dengan tenaga kesehatan.",
            ),
            disclaimer = MANDATORY_DISCLAIMER,
            isPhaseSpecific = true,
        ),
        CycleEducationTopic(
            id = "edu_luteal",
            title = "Fase luteal",
            summary = "Menjelang periode, sebagian orang mencatat perubahan mood, tidur, atau nafsu makan. Pola pribadi lebih berarti dari aturan umum.",
            details = listOf(
                "Catat pola pribadimu dan bersikap lembut pada diri sendiri.",
                "Bila mood sangat mengganggu keseharian dalam waktu lama — pertimbangkan bicara dengan tenaga kesehatan.",
            ),
            disclaimer = "Ini informasi umum, bukan saran medis.",
            isPhaseSpecific = true,
        ),
        CycleEducationTopic(
            id = "edu_cycle_basics",
            title = "Mengapa siklus bisa berbeda",
            summary = "Panjang siklus yang umum berkisar luas, dan milikmu bisa berbeda dari 28 hari. Prediksi rona hanyalah estimasi dari riwayatmu.",
            details = listOf(
                "Mencatat tanggal mulai secara konsisten membantu estimasi menjadi lebih personal.",
                "Perhatikan pola tubuhmu tanpa memaksakan angka tertentu.",
            ),
            disclaimer = "Ini informasi umum, bukan saran medis.",
        ),
        CycleEducationTopic(
            id = "edu_fertility_limits",
            title = "Batas prediksi kalender",
            summary = "Ovulasi tidak dapat dipastikan hanya dari prediksi kalender. Catatan siklus membantu memahami pola, bukan jaminan.",
            details = listOf(
                "Gunakan rona untuk memahami pola pribadimu.",
                "Tidak ada 'hari aman' yang pasti dari kalender.",
                "Prediksi kalender bisa mencegah atau menjamin kehamilan.",
            ),
            disclaimer = MANDATORY_DISCLAIMER,
        ),
    )

    private val predictionChangeTopic = CycleEducationTopic(
        id = "edu_prediction_change",
        title = "Mengapa prediksi Runa berubah",
        summary = "Prediksi didasarkan pada jarak antar periode terakhirmu. Setiap catatan baru bisa menggeser estimasi.",
        details = listOf(
            "Runa menggunakan median dari ${CycleEngine.MAX_CYCLES_USED} siklus terakhir yang valid.",
            "Siklus di luar ${CycleEngine.MIN_CYCLE_DAYS}–${CycleEngine.MAX_CYCLE_DAYS} hari tidak digunakan untuk prediksi.",
            "Semakin banyak catatan yang kamu simpan, semakin stabil estimasinya.",
            "Prediksi selalu berupa rentang tanggal, bukan satu hari pasti.",
        ),
        disclaimer = MANDATORY_DISCLAIMER,
        requiresMaturity = InsightMaturityLevel.LEVEL_2_INITIAL_HISTORY,
    )

    /**
     * All topics available at the given maturity level.
     * General topics are always available. Prediction-change topic requires Level 2+.
     */
    fun topicsForMaturity(level: InsightMaturityLevel): List<CycleEducationTopic> =
        generalTopics + if (level >= InsightMaturityLevel.LEVEL_2_INITIAL_HISTORY) {
            listOf(predictionChangeTopic)
        } else {
            emptyList()
        }

    /**
     * Phase-specific topic for today, if the user has enough data to determine their cycle day.
     * Returns null if cycle day cannot be determined or maturity is too low.
     */
    fun phaseTopicForToday(
        cycleDay: Int?,
        cycleLengthDays: Int?,
        maturity: InsightMaturityLevel,
    ): CycleEducationTopic? {
        if (cycleDay == null || cycleLengthDays == null) return null
        if (maturity < InsightMaturityLevel.LEVEL_1_SINGLE_START) return null
        val phase = StatisticsEngine.phaseForCycleDay(cycleDay, cycleLengthDays)
        return generalTopics.firstOrNull { topic ->
            topic.isPhaseSpecific && topic.id == phaseTopicId(phase)
        }
    }

    private fun phaseTopicId(phase: CyclePhase): String = when (phase) {
        CyclePhase.EARLY -> "edu_menstruation"
        CyclePhase.MIDDLE -> "edu_ovulation_est"
        CyclePhase.LATE -> "edu_luteal"
    }
}
