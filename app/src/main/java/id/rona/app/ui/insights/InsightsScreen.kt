package id.rona.app.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.rona.app.domain.insights.CycleStabilityCard
import id.rona.app.domain.insights.EducationCard
import id.rona.app.domain.insights.InsightCard
import id.rona.app.domain.insights.InsightSourceLabel
import id.rona.app.domain.insights.LastObservedIntervalCard
import id.rona.app.domain.insights.PatternCard
import id.rona.app.domain.insights.PredictionRangeCard
import id.rona.app.domain.insights.RecordedCycleCard
import id.rona.app.domain.insights.TrendCard
import id.rona.app.ui.components.RonaLoadingSkeleton
import id.rona.app.ui.components.RonaTopBar
import id.rona.app.ui.theme.LocalRonaColors
import id.rona.app.ui.theme.RonaTheme
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun InsightsScreen(
    modifier: Modifier = Modifier,
    onOpenSettings: (() -> Unit)? = null,
    onLogToday: (() -> Unit)? = null,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalRonaColors.current

    if (uiState.isLoading) {
        RonaLoadingSkeleton(modifier = modifier.fillMaxSize(), message = "Menyiapkan pola…")
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        RonaTopBar(onOpenSettings = onOpenSettings)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Insight",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary,
                ),
            )
            Text(
                text = "Pola dari catatanmu, bukan diagnosis.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    color = colors.inkSecondary,
                ),
            )

            uiState.progressiveInsights.cards.forEach { card ->
                InsightCardView(card)
            }

            if (uiState.progressiveInsights.maturity == id.rona.app.domain.insights.InsightMaturityLevel.LEVEL_0_EMPTY && onLogToday != null) {
                id.rona.app.ui.components.RonaPrimaryButton(
                    text = "Catat periode terakhir",
                    onClick = onLogToday,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Edukasi umum tentang cara memahami siklus" },
                shape = MaterialTheme.shapes.medium,
                color = colors.surfaceSoft,
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Memahami siklusmu",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = colors.inkPrimary,
                        ),
                    )
                    Text(
                        text = "Pelajari cara membaca catatan siklus secara tenang dan non-diagnostik.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = colors.inkSecondary),
                    )
                    SourceLabel(InsightSourceLabel.GENERAL_EDUCATION)
                }
            }

            Text(
                text = "Semua angka dihitung dari catatanmu di perangkat ini — informasi umum, bukan nasihat medis.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = colors.inkTertiary,
                ),
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InsightCardView(card: InsightCard) {
    val colors = LocalRonaColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceSoft,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = sourceLabelText(card.sourceLabel),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        color = colors.cyclePrimary,
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = "Sumber insight: ${sourceLabelText(card.sourceLabel)}"
                    },
                )
            }
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = colors.inkPrimary,
                ),
            )
            Text(
                text = cardValue(card),
                style = MaterialTheme.typography.bodyLarge.copy(color = colors.inkPrimary),
            )
            card.explanation?.let { explanation ->
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyMedium.copy(color = colors.inkSecondary),
                )
            }
        }
    }
}

private fun cardValue(card: InsightCard): String = when (card) {
    is EducationCard -> card.explanation
    is RecordedCycleCard -> buildString {
        append("Mulai ${card.startDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("id", "ID")))}.")
        if (card.cycleDay != null) append(" Hari siklus saat ini: ${card.cycleDay}.")
        if (card.logCount > 0) append(" ${card.logCount} catatan harian terkonfirmasi.")
    }
    is LastObservedIntervalCard -> "${card.days} hari"
    is PredictionRangeCard -> {
        val formatter = DateTimeFormatter.ofPattern("d MMMM", Locale("id", "ID"))
        "Sekitar ${card.prediction.rangeLow.format(formatter)}–${card.prediction.rangeHigh.format(formatter)} " +
            "(${card.prediction.confidence.name.lowercase(Locale("id", "ID"))})."
    }
    is PatternCard -> "${card.observationCount} catatan memuat ${symptomLabel(card.symptomType)}."
    is CycleStabilityCard -> "Median ${card.medianDays} hari · MAD ${card.madDays} hari."
    is TrendCard -> card.cycleLengths.joinToString(" · ") { "$it hari" }
}

@Composable
private fun SourceLabel(source: InsightSourceLabel) {
    val colors = LocalRonaColors.current
    Text(
        text = sourceLabelText(source),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = colors.cyclePrimary,
        ),
    )
}

private fun sourceLabelText(source: InsightSourceLabel): String = when (source) {
    InsightSourceLabel.RECORDED -> "TERCATAT"
    InsightSourceLabel.ESTIMATED -> "PERKIRAAN"
    InsightSourceLabel.PERSONAL_PATTERN -> "POLA PRIBADI"
    InsightSourceLabel.GENERAL_EDUCATION -> "EDUKASI UMUM"
}

private fun symptomLabel(symptom: id.rona.app.domain.model.SymptomType): String = when (symptom) {
    id.rona.app.domain.model.SymptomType.KRAM -> "kram"
    id.rona.app.domain.model.SymptomType.HEADACHE -> "sakit kepala"
    id.rona.app.domain.model.SymptomType.NAUSEA -> "mual"
    id.rona.app.domain.model.SymptomType.BLOATING -> "kembung"
    id.rona.app.domain.model.SymptomType.FATIGUE -> "lelah"
    id.rona.app.domain.model.SymptomType.BREAST_TENDERNESS -> "nyeri payudara"
    id.rona.app.domain.model.SymptomType.ACNE -> "jerawat"
    id.rona.app.domain.model.SymptomType.BACKACHE -> "nyeri punggung"
    id.rona.app.domain.model.SymptomType.SLEEP_ISSUE -> "sulit tidur"
    id.rona.app.domain.model.SymptomType.APPETITE_CHANGE -> "perubahan nafsu makan"
}

@androidx.compose.ui.tooling.preview.Preview(name = "Insights Screen — Light", showBackground = true)
@Composable
private fun InsightsScreenLightPreview() {
    RonaTheme { InsightsScreen(onOpenSettings = {}) }
}
