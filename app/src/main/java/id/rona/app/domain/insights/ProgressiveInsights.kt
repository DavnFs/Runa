package id.rona.app.domain.insights

import id.rona.app.domain.engine.CycleEngine
import id.rona.app.domain.engine.CyclePrediction
import id.rona.app.domain.engine.CyclePhase
import id.rona.app.domain.model.Confidence
import id.rona.app.domain.model.SymptomType
import java.time.LocalDate

/** Derived maturity; never persisted. */
enum class InsightMaturityLevel {
    LEVEL_0_EMPTY,
    LEVEL_1_SINGLE_START,
    LEVEL_2_INITIAL_HISTORY,
    LEVEL_3_ESTABLISHING,
    LEVEL_4_MATURE,
}

enum class InsightCategory {
    RECORDED,
    ESTIMATED,
    PERSONAL_PATTERN,
    GENERAL_EDUCATION,
}

enum class InsightSourceLabel {
    RECORDED,
    ESTIMATED,
    PERSONAL_PATTERN,
    GENERAL_EDUCATION,
}

sealed interface InsightCard {
    val id: String
    val category: InsightCategory
    val sourceLabel: InsightSourceLabel
    val title: String
    val explanation: String?
}

data class EducationCard(
    override val id: String,
    override val title: String,
    override val explanation: String,
) : InsightCard {
    override val category = InsightCategory.GENERAL_EDUCATION
    override val sourceLabel = InsightSourceLabel.GENERAL_EDUCATION
}

data class RecordedCycleCard(
    override val id: String,
    val startDate: LocalDate,
    val cycleDay: Int?,
    val logCount: Int,
    override val title: String = "Catatan yang sudah ada",
    override val explanation: String? = null,
) : InsightCard {
    override val category = InsightCategory.RECORDED
    override val sourceLabel = InsightSourceLabel.RECORDED
}

data class LastObservedIntervalCard(
    override val id: String,
    val days: Int,
    override val title: String = "Jarak dua periode terakhir",
    override val explanation: String = "Ini adalah catatan awal. Perkiraan akan menjadi lebih personal saat histori bertambah.",
) : InsightCard {
    override val category = InsightCategory.RECORDED
    override val sourceLabel = InsightSourceLabel.RECORDED
}

data class CurrentCycleDayCard(
    override val id: String,
    val cycleDay: Int,
    override val title: String = "Hari siklus saat ini",
    override val explanation: String? = null,
) : InsightCard {
    override val category = InsightCategory.RECORDED
    override val sourceLabel = InsightSourceLabel.RECORDED
}

data class PredictionRangeCard(
    override val id: String,
    val prediction: CyclePrediction,
    override val title: String = "Perkiraan periode berikutnya",
    override val explanation: String,
) : InsightCard {
    override val category = InsightCategory.ESTIMATED
    override val sourceLabel = InsightSourceLabel.ESTIMATED
}

data class PatternCard(
    override val id: String,
    val symptomType: SymptomType,
    val phase: CyclePhase?,
    val observationCount: Int,
    override val title: String,
    override val explanation: String = "Pola pada catatanmu, bukan diagnosis.",
) : InsightCard {
    override val category = InsightCategory.PERSONAL_PATTERN
    override val sourceLabel = InsightSourceLabel.PERSONAL_PATTERN
}

data class CycleStabilityCard(
    override val id: String,
    val medianDays: Int,
    val madDays: Double,
    val confidence: Confidence,
    override val title: String = "Variasi panjang siklus",
    override val explanation: String = "Pola pada catatanmu, bukan diagnosis.",
) : InsightCard {
    override val category = InsightCategory.PERSONAL_PATTERN
    override val sourceLabel = InsightSourceLabel.PERSONAL_PATTERN
}

data class TrendCard(
    override val id: String,
    val cycleLengths: List<Int>,
    override val title: String = "Panjang siklus",
    override val explanation: String = "Pola pada catatanmu, bukan diagnosis.",
) : InsightCard {
    override val category = InsightCategory.PERSONAL_PATTERN
    override val sourceLabel = InsightSourceLabel.PERSONAL_PATTERN
}

data class ProgressiveInsights(
    val maturity: InsightMaturityLevel,
    val cards: List<InsightCard>,
) {
    val primaryCard: InsightCard? = cards.firstOrNull()
}

object ProgressiveInsightsGenerator {
    const val MIN_PATTERN_OBSERVATIONS = 3
    const val MIN_TREND_INTERVALS = 5

    fun maturity(
        periodStartCount: Int,
        validIntervalCount: Int,
        dailyLogCount: Int,
        symptomCount: Int,
    ): InsightMaturityLevel = when {
        periodStartCount <= 0 -> InsightMaturityLevel.LEVEL_0_EMPTY
        periodStartCount == 1 -> InsightMaturityLevel.LEVEL_1_SINGLE_START
        periodStartCount == 2 && validIntervalCount <= 1 -> InsightMaturityLevel.LEVEL_2_INITIAL_HISTORY
        periodStartCount >= 6 && validIntervalCount >= 5 -> InsightMaturityLevel.LEVEL_4_MATURE
        else -> InsightMaturityLevel.LEVEL_3_ESTABLISHING
    }

    fun generate(
        periodStarts: List<LocalDate>,
        cycleDay: Int?,
        dailyLogCount: Int,
        symptomCounts: Map<SymptomType, Int> = emptyMap(),
        prediction: CyclePrediction? = CycleEngine.predict(periodStarts),
        cycleLengths: List<Int> = CycleEngine.recentValidCycleLengths(periodStarts),
        latestStart: LocalDate? = periodStarts.maxOrNull(),
    ): ProgressiveInsights {
        val starts = periodStarts.distinct().sorted()
        val validIntervals = CycleEngine.recentValidCycleLengths(starts)
        val level = maturity(starts.size, validIntervals.size, dailyLogCount, symptomCounts.values.sum())
        val cards: List<InsightCard> = when (level) {
            InsightMaturityLevel.LEVEL_0_EMPTY -> listOf(
                EducationCard(
                    id = "education-start",
                    title = "Mulai mengenali pola tubuhmu",
                    explanation = "Tambahkan periode terakhir untuk mulai membangun perkiraan yang lebih personal.",
                ),
                EducationCard(
                    id = "education-cycle",
                    title = "Cara Runa membangun perkiraan",
                    explanation = "Runa membandingkan catatan periode yang kamu konfirmasi di perangkat ini.",
                ),
            )
            InsightMaturityLevel.LEVEL_1_SINGLE_START -> buildList {
                latestStart?.let { start ->
                    add(RecordedCycleCard("recorded-start", start, cycleDay, dailyLogCount))
                }
                add(EducationCard(
                    id = "education-next-period",
                    title = "Tambahkan periode berikutnya",
                    explanation = "Runa membutuhkan setidaknya satu periode berikutnya untuk membandingkan jarak antar siklus.",
                ))
            }
            InsightMaturityLevel.LEVEL_2_INITIAL_HISTORY -> buildList {
                validIntervals.lastOrNull()?.let { add(LastObservedIntervalCard("recorded-interval", it)) }
                add(EducationCard(
                    id = "education-history",
                    title = "Histori awal",
                    explanation = "Catatan ini belum cukup untuk menyebut siklus stabil atau pola pribadi.",
                ))
            }
            InsightMaturityLevel.LEVEL_3_ESTABLISHING,
            InsightMaturityLevel.LEVEL_4_MATURE,
            -> buildList {
                prediction?.let {
                    add(PredictionRangeCard(
                        id = "estimated-next-period",
                        prediction = it,
                        explanation = "Berdasarkan ${it.cycleCountUsed} jarak siklus yang tercatat.",
                    ))
                    val median = validIntervals.takeIf { values -> values.isNotEmpty() }?.let(CycleEngine::median)
                    val mad = median?.let { value -> CycleEngine.medianAbsoluteDeviation(validIntervals, value) }
                    if (median != null && mad != null && level == InsightMaturityLevel.LEVEL_4_MATURE) {
                        add(CycleStabilityCard("personal-variation", median, mad, it.confidence))
                    }
                }
                symptomCounts.entries
                    .filter { it.value >= MIN_PATTERN_OBSERVATIONS }
                    .maxByOrNull { it.value }
                    ?.let { (symptom, count) ->
                        add(PatternCard(
                            id = "pattern-${symptom.name.lowercase()}",
                            symptomType = symptom,
                            phase = null,
                            observationCount = count,
                            title = "Pola awal pada catatanmu",
                            explanation = "${count} catatan memuat ${symptom.displayName()}. Pola pada catatanmu, bukan diagnosis.",
                        ))
                    }
                if (level == InsightMaturityLevel.LEVEL_4_MATURE && validIntervals.size >= MIN_TREND_INTERVALS) {
                    add(TrendCard("cycle-length-trend", validIntervals))
                }
            }
        }
        return ProgressiveInsights(level, cards)
    }
}

private fun SymptomType.displayName(): String = when (this) {
    SymptomType.KRAM -> "kram"
    SymptomType.HEADACHE -> "sakit kepala"
    SymptomType.NAUSEA -> "mual"
    SymptomType.BLOATING -> "kembung"
    SymptomType.FATIGUE -> "lelah"
    SymptomType.BREAST_TENDERNESS -> "nyeri payudara"
    SymptomType.ACNE -> "jerawat"
    SymptomType.BACKACHE -> "nyeri punggung"
    SymptomType.SLEEP_ISSUE -> "sulit tidur"
    SymptomType.APPETITE_CHANGE -> "perubahan nafsu makan"
}
