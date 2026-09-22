package id.rona.app.ui.insights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.rona.app.domain.insights.CycleEducationProvider
import id.rona.app.domain.insights.CycleEducationTopic
import id.rona.app.domain.insights.CycleStabilityCard
import id.rona.app.domain.insights.EducationCard
import id.rona.app.domain.insights.InsightCard
import id.rona.app.domain.insights.InsightMaturityLevel
import id.rona.app.domain.insights.InsightSourceLabel
import id.rona.app.domain.insights.LastObservedIntervalCard
import id.rona.app.domain.insights.PatternCard
import id.rona.app.domain.insights.PredictionRangeCard
import id.rona.app.domain.insights.RecordedCycleCard
import id.rona.app.domain.insights.TrendCard
import id.rona.app.ui.components.RunaLoadingSkeleton
import id.rona.app.ui.components.RunaTopBar
import id.rona.app.ui.theme.LocalRonaColors
import id.rona.app.ui.theme.RunaTheme
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
        RunaLoadingSkeleton(modifier = modifier.fillMaxSize(), message = "Menyiapkan pola…")
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        RunaTopBar(onOpenSettings = onOpenSettings)
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

            if (uiState.cycleLengths.size >= 3) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = colors.surfaceSoft,
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Tren siklusmu",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = colors.inkPrimary,
                            ),
                        )
                        Text(
                            text = "Panjang siklus terakhir, dalam hari.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = colors.inkSecondary),
                        )
                        id.rona.app.ui.components.RunaBarChart(
                            values = uiState.cycleLengths,
                            labels = uiState.cycleLengthLabels,
                            median = uiState.medianCycleLength,
                        )
                        uiState.medianCycleLength?.let { median ->
                            val lengths = uiState.cycleLengths
                            Text(
                                text = "Rata-rata: $median hari · Rentang: ${lengths.minOrNull() ?: median}–${lengths.maxOrNull() ?: median}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.inkTertiary,
                            )
                        }
                        SourceLabel(InsightSourceLabel.PERSONAL_PATTERN)
                    }
                }
            }

            if (uiState.progressiveInsights.maturity == id.rona.app.domain.insights.InsightMaturityLevel.LEVEL_0_EMPTY && onLogToday != null) {
                id.rona.app.ui.components.RunaPrimaryButton(
                    text = "Catat periode terakhir",
                    onClick = onLogToday,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            var showEducationSheet by remember { mutableStateOf(false) }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEducationSheet = true }
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
                        text = "Pelajari fase siklus dan cara kerja prediksi.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = colors.inkSecondary),
                    )
                    SourceLabel(InsightSourceLabel.GENERAL_EDUCATION)
                }
            }

            if (showEducationSheet) {
                CycleEducationSheet(
                    maturity = uiState.progressiveInsights.maturity,
                    cycleDay = uiState.cycleDay,
                    medianCycleLength = uiState.medianCycleLength,
                    onDismiss = { showEducationSheet = false },
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CycleEducationSheet(
    maturity: InsightMaturityLevel,
    cycleDay: Int?,
    medianCycleLength: Int?,
    onDismiss: () -> Unit,
) {
    val colors = LocalRonaColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val topics = remember(maturity) { CycleEducationProvider.topicsForMaturity(maturity) }
    val phaseTopic = remember(cycleDay, medianCycleLength, maturity) {
        CycleEducationProvider.phaseTopicForToday(cycleDay, medianCycleLength, maturity)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Memahami siklusmu",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary,
                ),
            )
            Text(
                text = "Pelajari fase siklus dan cara kerja prediksi.",
                style = MaterialTheme.typography.bodyMedium.copy(color = colors.inkSecondary),
            )

            Spacer(Modifier.height(8.dp))

            // Phase-specific topic at top if available
            if (phaseTopic != null) {
                EducationTopicCard(
                    topic = phaseTopic,
                    badge = "Fase saat ini",
                )
            }

            // All general topics
            topics.forEach { topic ->
                if (topic.id != phaseTopic?.id) {
                    EducationTopicCard(topic = topic)
                }
            }

            // Mandatory disclaimer
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = colors.surfaceSoft,
            ) {
                Text(
                    text = "Perkiraan kalender tidak dapat memastikan ovulasi dan tidak ditujukan sebagai metode kontrasepsi atau jaminan kehamilan.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = colors.inkTertiary,
                        fontWeight = FontWeight.Medium,
                    ),
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun EducationTopicCard(
    topic: CycleEducationTopic,
    badge: String? = null,
) {
    val colors = LocalRonaColors.current
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceSoft,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (badge != null) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.cyclePrimary,
                            ),
                        )
                        Spacer(Modifier.height(2.dp))
                    }
                    Text(
                        text = topic.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = colors.inkPrimary,
                        ),
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Tutup" else "Buka",
                    tint = colors.inkTertiary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = topic.summary,
                style = MaterialTheme.typography.bodyMedium.copy(color = colors.inkSecondary),
            )
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    topic.details.forEach { detail ->
                        Text(
                            text = "• $detail",
                            style = MaterialTheme.typography.bodySmall.copy(color = colors.inkSecondary),
                        )
                    }
                    topic.disclaimer?.let { disclaimer ->
                        Text(
                            text = disclaimer,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = colors.inkTertiary,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Insights Screen — Light", showBackground = true)
@Composable
private fun InsightsScreenLightPreview() {
    RunaTheme { InsightsScreen(onOpenSettings = {}) }
}
