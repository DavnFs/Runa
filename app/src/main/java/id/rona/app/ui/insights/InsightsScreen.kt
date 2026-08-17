package id.rona.app.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.rona.app.domain.engine.CyclePhase
import id.rona.app.domain.engine.SymptomFrequency
import id.rona.app.domain.model.SymptomType
import id.rona.app.ui.components.RonaEmptyState
import id.rona.app.ui.components.RonaLoadingSkeleton
import id.rona.app.ui.home.RonaInsightCard
import id.rona.app.ui.theme.LocalRonaColors

@Composable
fun InsightsScreen(
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        RonaLoadingSkeleton(modifier = modifier.fillMaxSize(), message = "Menyiapkan pola…")
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Insight", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Pola pada catatanmu — bukan diagnosis.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (uiState.totalPeriods < 2) {
            RonaEmptyState(
                icon = Icons.Rounded.QueryStats,
                title = "Pola akan muncul perlahan",
                message = "Semakin banyak catatan yang kamu simpan, semakin mudah melihat pola personalmu.",
            )
            return@Column
        }

        // ——— Max 3 meaningful summary blocks ———
        RonaInsightCard(
            title = "Panjang siklus",
            text = uiState.medianCycleLength?.let { "$it hari" } ?: "—",
            supporting = buildString {
                uiState.avgCycleLength?.let { append("rata-rata ${it.toInt()} hari") }
                uiState.madCycleDays?.let {
                    if (isNotEmpty()) append(" · ")
                    append("variasi ±${it.toInt()} hari")
                }
            },
        )

        RonaInsightCard(
            title = "Durasi menstruasi",
            text = uiState.medianPeriodDuration?.let { "$it hari" } ?: "—",
            supporting = uiState.avgPeriodDuration?.let {
                "rata-rata ${String.format(java.util.Locale.US, "%.1f", it)} hari"
            } ?: "",
        )

        val symptoms = uiState.mostFrequentSymptoms
        if (symptoms.isNotEmpty()) {
            MostFrequentSymptoms(symptoms)
        }

        // ——— Phase distribution: subtle tonal block ———
        val distribution = uiState.phaseDistribution
        if (distribution.isNotEmpty()) {
            PhaseDistributionBlock(distribution)
        }

        Text(
            "Semua angka dihitung dari catatanmu di perangkat ini — " +
                "informasi umum, bukan nasihat medis.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MostFrequentSymptoms(items: List<SymptomFrequency>) {
    val colors = LocalRonaColors.current
    Column(Modifier.fillMaxWidth()) {
        Text("Gejala paling sering", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        items.take(3).forEach { item ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(symptomLabel(item.symptomType), style = MaterialTheme.typography.bodyMedium)
                Text("${item.frequency}×", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(horizontal = 0.dp)
        )
    }
}

@Composable
private fun PhaseDistributionBlock(distribution: Map<CyclePhase, Int>) {
    Column(Modifier.fillMaxWidth()) {
        Text("Catatan per fase siklus", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        CyclePhase.entries.forEach { phase ->
            val count = distribution[phase] ?: 0
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    when (phase) {
                        CyclePhase.EARLY -> "Awal siklus"
                        CyclePhase.MIDDLE -> "Tengah siklus"
                        CyclePhase.LATE -> "Akhir siklus"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text("$count hari", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun symptomLabel(symptom: SymptomType): String = when (symptom) {
    SymptomType.KRAM -> "Kram perut"
    SymptomType.HEADACHE -> "Sakit kepala"
    SymptomType.NAUSEA -> "Mual"
    SymptomType.BLOATING -> "Kembung"
    SymptomType.FATIGUE -> "Lelah"
    SymptomType.BREAST_TENDERNESS -> "Nyeri payudara"
    SymptomType.ACNE -> "Jerawat"
    SymptomType.BACKACHE -> "Nyeri punggung"
    SymptomType.SLEEP_ISSUE -> "Sulit tidur"
    SymptomType.APPETITE_CHANGE -> "Perubahan nafsu makan"
}
