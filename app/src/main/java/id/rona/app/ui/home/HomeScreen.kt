package id.rona.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.rona.app.R
import id.rona.app.domain.engine.CyclePrediction
import id.rona.app.domain.model.Confidence
import id.rona.app.ui.components.RonaEmptyState
import id.rona.app.ui.components.RonaErrorState
import id.rona.app.ui.components.RonaLoadingSkeleton
import id.rona.app.ui.theme.LocalRonaColors
import id.rona.app.ui.theme.RonaMotion
import id.rona.app.ui.theme.RonaTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HomeHeader(onOpenSettings = onOpenSettings)

        when (val state = uiState) {
            is HomeUiState.Loading -> RonaLoadingSkeleton(message = "Menyiapkan halamanmu…")
            is HomeUiState.Empty -> HomeEmptyContent(
                onStartPeriod = viewModel::startPeriod,
                onLogToday = onLogToday,
            )
            is HomeUiState.Error -> RonaErrorState(
                message = state.userMessage,
                onRetry = viewModel::retry,
            )
            is HomeUiState.Success -> HomeSuccessContent(
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
private fun HomeHeader(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_logo_symbol),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text("rona", style = MaterialTheme.typography.titleLarge)
            Text(
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("id", "ID"))),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = "Pengaturan & privasi")
        }
    }
}

@Composable
private fun HomeEmptyContent(
    onStartPeriod: () -> Unit,
    onLogToday: () -> Unit,
) {
    RonaEmptyState(
        icon = Icons.Rounded.Edit,
        title = "Mulai perjalananmu",
        message = "Catat periode pertamamu — rona akan belajar polamu dari sana.",
        actionLabel = "Catat periode terakhir",
        onAction = onStartPeriod,
    )
    OutlinedButton(onClick = onLogToday, modifier = Modifier.fillMaxWidth()) {
        Text("Log hari ini")
    }
}

@Composable
private fun HomeSuccessContent(
    homeData: HomeData,
    onStartPeriod: () -> Unit,
    onEndPeriod: () -> Unit,
    onLogToday: () -> Unit,
    onOpenCalendar: () -> Unit,
) {
    RonaCycleHero(homeData = homeData)

    Button(onClick = onLogToday, modifier = Modifier.fillMaxWidth().height(52.dp)) {
        Text("Catat keadaanmu hari ini")
    }

    val prediction = homeData.prediction
    if (prediction != null) {
        RonaInsightCard(
            title = "Perkiraan periode berikutnya",
            text = buildString {
                append("sekitar ")
                append(prediction.rangeLow.format(DateTimeFormatter.ofPattern("d MMM")))
                append(" – ")
                append(prediction.rangeHigh.format(DateTimeFormatter.ofPattern("d MMM")))
            },
            supporting = when (prediction.confidence) {
                Confidence.HIGH -> "Berdasarkan ${prediction.cycleCountUsed} siklus terakhir."
                Confidence.MEDIUM -> "Masih belajar dari ${prediction.cycleCountUsed} siklus tercatat."
                Confidence.LOW -> "Perkiraan awal — akurasinya bertambah seiring data."
            },
        )
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
        OutlinedButton(onClick = onOpenCalendar, modifier = Modifier.weight(1f)) {
            Text("Kalender")
        }
    }

    Text(
        "Data disimpan dan dienkripsi di perangkat ini.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    )
}

/** Calm cycle ring hero — not a generic progress bar. */
@Composable
fun RonaCycleHero(
    homeData: HomeData,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRonaColors.current
    val cycleDay = homeData.cycleDay
    val dayInCycle = (cycleDay ?: 1).coerceIn(1, 35)
    val progress = dayInCycle / 35f

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.cycleContainer,
    ) {
        Column(
            Modifier.padding(vertical = 32.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(160.dp)) {
                    val stroke = 10.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    // Track
                    drawArc(
                        color = colors.cyclePrimary.copy(alpha = 0.18f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    // Progress arc
                    drawArc(
                        color = colors.cyclePrimary,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (cycleDay != null) "Hari ke-$cycleDay" else "Belum ada data",
                        style = MaterialTheme.typography.displayMedium,
                        color = colors.onCycleContainer,
                    )
                    Text(
                        if (homeData.isPeriodActive) "Periode sedang berlangsung" else "Fase siklus",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onCycleContainer.copy(alpha = 0.8f),
                    )
                }
            }
            if (cycleDay != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Estimasi fase — bukan kepastian medis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onCycleContainer.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
fun RonaInsightCard(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
) {
    val colors = LocalRonaColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceSoft,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(text, style = MaterialTheme.typography.titleLarge)
            if (supporting != null) {
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

// ───────────────────────── Previews ─────────────────────────

@Preview(name = "Home Success — light", showBackground = true)
@Composable
private fun HomeSuccessLightPreview() {
    RonaTheme {
        HomeSuccessContent(
            homeData = HomeData(
                cycleDay = 12,
                isPeriodActive = false,
                prediction = CyclePrediction(
                    predictedStart = LocalDate.now().plusDays(16),
                    rangeLow = LocalDate.now().plusDays(14),
                    rangeHigh = LocalDate.now().plusDays(19),
                    medianCycleLengthDays = 28,
                    meanCycleLengthDays = 28.5,
                    madDays = 1.5,
                    cycleCountUsed = 4,
                    confidence = Confidence.MEDIUM,
                ),
                totalPeriods = 4,
                totalLogs = 30,
            ),
            onStartPeriod = {},
            onEndPeriod = {},
            onLogToday = {},
            onOpenCalendar = {},
        )
    }
}

@Preview(name = "Home Empty — light", showBackground = true)
@Composable
private fun HomeEmptyLightPreview() {
    RonaTheme {
        HomeEmptyContent(onStartPeriod = {}, onLogToday = {})
    }
}

@Preview(name = "Home Error — light", showBackground = true)
@Composable
private fun HomeErrorLightPreview() {
    RonaTheme {
        RonaErrorState(message = "Datamu tidak bisa dimuat sekarang.", onRetry = {})
    }
}

@Preview(name = "Home Success — dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeSuccessDarkPreview() {
    RonaTheme(themeMode = id.rona.app.domain.model.ThemeMode.DARK) {
        HomeSuccessContent(
            homeData = HomeData(
                cycleDay = 12,
                isPeriodActive = false,
                prediction = CyclePrediction(
                    predictedStart = LocalDate.now().plusDays(16),
                    rangeLow = LocalDate.now().plusDays(14),
                    rangeHigh = LocalDate.now().plusDays(19),
                    medianCycleLengthDays = 28,
                    meanCycleLengthDays = 28.5,
                    madDays = 1.5,
                    cycleCountUsed = 4,
                    confidence = Confidence.MEDIUM,
                ),
                totalPeriods = 4,
                totalLogs = 30,
            ),
            onStartPeriod = {},
            onEndPeriod = {},
            onLogToday = {},
            onOpenCalendar = {},
        )
    }
}
