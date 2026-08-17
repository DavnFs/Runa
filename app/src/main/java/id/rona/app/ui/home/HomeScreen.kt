package id.rona.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.rona.app.R
import id.rona.app.domain.model.Confidence
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onLogToday: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logo_symbol),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("rona", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Rounded.Settings, contentDescription = "Pengaturan")
            }
        }

        when (val state = uiState) {
            is HomeUiState.Loading -> HomeLoading()
            is HomeUiState.Empty -> HomeEmpty(
                onStartPeriod = viewModel::startPeriod,
                onLogToday = onLogToday,
            )
            is HomeUiState.Error -> HomeError(
                message = state.userMessage,
                onRetry = viewModel::retry,
            )
            is HomeUiState.Success -> HomeContent(
                homeData = state.homeData,
                onStartPeriod = viewModel::startPeriod,
                onEndPeriod = viewModel::endPeriod,
                onLogToday = onLogToday,
                onOpenCalendar = onOpenCalendar,
            )
        }
    }
}

@Composable
private fun HomeLoading() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
        Spacer(Modifier.height(12.dp))
        Text(
            "Memuat…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HomeEmpty(
    onStartPeriod: () -> Unit,
    onLogToday: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Mulai perjalananmu",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Catat periode pertamamu — rona akan belajar polamu dari sana.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onStartPeriod) {
                Text("Catat periode terakhir")
            }
        }
    }
    OutlinedButton(onClick = onLogToday, modifier = Modifier.fillMaxWidth()) {
        Text("Log hari ini")
    }
}

@Composable
private fun HomeError(
    message: String,
    onRetry: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Coba lagi")
            }
        }
    }
}

@Composable
private fun HomeContent(
    homeData: HomeData,
    onStartPeriod: () -> Unit,
    onEndPeriod: () -> Unit,
    onLogToday: () -> Unit,
    onOpenCalendar: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val cycleDay = homeData.cycleDay
            Text(
                text = if (cycleDay != null) "Hari ke-$cycleDay" else "Hari siklus belum diketahui",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (homeData.isPeriodActive) "Periode sedang berlangsung" else "Tidak sedang menstruasi",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }

    val prediction = homeData.prediction
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
        if (homeData.isPeriodActive) {
            Button(onClick = onEndPeriod, modifier = Modifier.weight(1f)) {
                Text("Selesaikan periode")
            }
        } else {
            Button(onClick = onStartPeriod, modifier = Modifier.weight(1f)) {
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
}
