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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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

@Composable
fun InsightsScreen(
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Insight", style = MaterialTheme.typography.headlineSmall)

        if (uiState.totalPeriods < 2) {
            EmptyInsights()
            return@Column
        }

        StatCard(
            title = "Panjang siklus",
            value = uiState.medianCycleLength?.let { "$it hari" } ?: "—",
            supporting = buildString {
                uiState.avgCycleLength?.let { append("rata-rata ${it.toInt()} hari") }
                uiState.madCycleDays?.let {
                    if (isNotEmpty()) append(" · ")
                    append("variasi ±${it.toInt()} hari")
                }
            },
        )

        StatCard(
            title = "Durasi menstruasi",
            value = uiState.medianPeriodDuration?.let { "$it hari" } ?: "—",
            supporting = uiState.avgPeriodDuration?.let {
                "rata-rata ${String.format(java.util.Locale.US, "%.1f", it)} hari"
            } ?: "",
        )

        MostFrequentSymptoms(uiState.mostFrequentSymptoms)

        PhaseDistributionCard(uiState.phaseDistribution)

        Text(
            "Semua angka dihitung dari catatanmu di perangkat ini — bukan " +
                "informasi medis dan bukan alat kontrasepsi.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyInsights() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Insight muncul setelah beberapa siklus tercatat.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            "Lanjutkan mencatat — rona belajar dari polamu.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    supporting: String,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall)
            if (supporting.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MostFrequentSymptoms(items: List<SymptomFrequency>) {
    if (items.isEmpty()) return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Gejala paling sering tercatat", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            items.forEach { item ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(symptomLabel(item.symptomType), style = MaterialTheme.typography.bodyMedium)
                    Text("${item.frequency}×", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun PhaseDistributionCard(distribution: Map<CyclePhase, Int>) {
    if (distribution.isEmpty()) return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Catatan per fase siklus", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
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
