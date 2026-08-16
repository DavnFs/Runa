package id.rona.app.ui.home

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.rona.app.domain.model.Confidence
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onLogToday: () -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        Column(
            modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
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
        Text("rona", style = MaterialTheme.typography.titleLarge)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val cycleDay = uiState.cycleDay
                Text(
                    text = if (cycleDay != null) "Hari ke-$cycleDay" else "Belum ada data siklus",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(8.dp))
                if (uiState.isPeriodActive) {
                    Text(
                        "Periode sedang berlangsung",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    Text(
                        "Tidak sedang menstruasi",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        val prediction = uiState.prediction
        if (prediction != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Perkiraan periode berikutnya", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    val formatter = DateTimeFormatter.ofPattern("d MMM")
                    Text(
                        "sekitar ${prediction.rangeLow.format(formatter)} – ${prediction.rangeHigh.format(formatter)}",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when (prediction.confidence) {
                            Confidence.HIGH -> "Berdasarkan ${prediction.cycleCountUsed} siklus terakhir."
                            Confidence.MEDIUM -> "Masih belajar dari ${prediction.cycleCountUsed} siklus tercatat."
                            Confidence.LOW -> "Perkiraan awal — akurasinya bertambah seiring data."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (uiState.isPeriodActive) {
                Button(onClick = { viewModel.endPeriod() }, modifier = Modifier.weight(1f)) {
                    Text("Selesaikan periode")
                }
            } else {
                Button(onClick = { viewModel.startPeriod() }, modifier = Modifier.weight(1f)) {
                    Text("Mulai periode")
                }
            }
            OutlinedButton(onClick = onLogToday, modifier = Modifier.weight(1f)) {
                Text("Log hari ini")
            }
        }

        OutlinedButton(onClick = onOpenCalendar, modifier = Modifier.fillMaxWidth()) {
            Text("Lihat kalender")
        }

        if (uiState.totalPeriods == 0) {
            Text(
                "Catat periode pertamamu untuk mulai melihat pola.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
