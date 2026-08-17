package id.rona.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.rona.app.ui.components.RonaLoadingSkeleton
import id.rona.app.ui.theme.LocalRonaColors
import id.rona.app.ui.theme.RonaBottomSheetShape
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val weekdayLabels = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        RonaLoadingSkeleton(modifier = modifier.fillMaxSize(), message = "Memuat kalender…")
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        MonthHeader(
            yearMonth = uiState.yearMonth,
            onPrevious = viewModel::previousMonth,
            onNext = viewModel::nextMonth,
        )
        Spacer(Modifier.height(8.dp))
        WeekdayHeader()
        Spacer(Modifier.height(4.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(uiState.days, key = { it.date.toEpochDay() }) { day ->
                DayCell(
                    day = day,
                    isSelected = day.date == uiState.selectedDay,
                    onClick = { viewModel.selectDay(day.date) },
                )
            }
        }
        Legend()
    }

    val selectedDay = uiState.selectedDay
    if (selectedDay != null) {
        val selectedCell = uiState.days.firstOrNull { it.date == selectedDay }
        if (selectedCell != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = viewModel::dismissDayDetail,
                sheetState = sheetState,
                shape = RonaBottomSheetShape,
            ) {
                DayDetailSheet(
                    day = selectedCell,
                    onDismiss = viewModel::dismissDayDetail,
                )
            }
        }
    }
}

@Composable
private fun MonthHeader(
    yearMonth: java.time.YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Rounded.ChevronLeft, contentDescription = "Bulan sebelumnya")
        }
        Text(
            text = yearMonth.month.getDisplayName(java.time.format.TextStyle.FULL, Locale("id", "ID"))
                .replaceFirstChar { it.uppercase() } + " " + yearMonth.year,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = "Bulan berikutnya")
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(Modifier.fillMaxWidth()) {
        weekdayLabels.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DayCell(
    day: CalendarDay,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalRonaColors.current
    val isToday = day.date == LocalDate.now()

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> colors.cycleContainer
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = buildString {
                    append(day.date.format(DateTimeFormatter.ofPattern("d MMMM")))
                    if (day.isPeriodActual) append(", menstruasi tercatat")
                    if (day.isPredicted) append(", perkiraan periode")
                    if (day.hasLog) append(", ada catatan")
                    if (isToday) append(", hari ini")
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Actual period: solid filled rose circle.
        if (day.isPeriodActual) {
            Box(
                Modifier
                    .fillMaxSize(0.72f)
                    .clip(CircleShape)
                    .background(colors.cyclePrimary)
            )
        } else if (day.isPredicted) {
            // Prediction: outline only (not color-dependent).
            Box(
                Modifier
                    .fillMaxSize(0.72f)
                    .clip(CircleShape)
                    .border(1.5.dp, colors.plumAccent, CircleShape)
            )
        }

        // Today: restrained ring.
        if (isToday) {
            Box(
                Modifier
                    .fillMaxSize(0.9f)
                    .border(1.5.dp, colors.cyclePrimary, CircleShape)
            )
        }

        Text(
            text = day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                day.isPeriodActual -> colors.onCyclePrimary
                !day.inCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.onSurface
            },
        )

        // Daily log dot.
        if (day.hasLog) {
            Box(
                Modifier
                    .size(6.dp)
                    .align(Alignment.BottomCenter)
                    .clip(CircleShape)
                    .background(colors.inkTertiary)
            )
        }
    }
}

@Composable
private fun Legend() {
    val colors = LocalRonaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(color = colors.cyclePrimary, label = "Menstruasi tercatat")
        LegendItem(color = colors.plumAccent, label = "Perkiraan", outlined = true)
    }
}

@Composable
private fun LegendItem(color: Color, label: String, outlined: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (outlined) {
            Box(
                Modifier
                    .size(12.dp)
                    .border(1.5.dp, color, CircleShape)
            )
        } else {
            Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        }
        Spacer(Modifier.size(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DayDetailSheet(
    day: CalendarDay,
    onDismiss: () -> Unit,
) {
    val colors = LocalRonaColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            day.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id", "ID"))),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(4.dp))
        if (day.isPeriodActual) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(colors.cyclePrimary))
                Spacer(Modifier.size(8.dp))
                Text("Menstruasi tercatat", style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (day.isPredicted) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .border(1.5.dp, colors.plumAccent, CircleShape)
                )
                Spacer(Modifier.size(8.dp))
                Text("Perkiraan periode", style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (day.hasLog) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(colors.inkTertiary))
                Spacer(Modifier.size(8.dp))
                Text("Ada catatan harian", style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (!day.isPeriodActual && !day.isPredicted && !day.hasLog) {
            Text(
                "Tidak ada catatan untuk tanggal ini.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
            Text("Tutup")
        }
    }
}
