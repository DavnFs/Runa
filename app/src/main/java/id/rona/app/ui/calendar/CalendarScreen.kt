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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.format.DateTimeFormatter
import java.util.Locale

private val weekdayLabels = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")

@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
            DayDetailDialog(
                day = selectedCell,
                onDismiss = viewModel::dismissDayDetail,
            )
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
    val actualColor = MaterialTheme.colorScheme.primary
    val predictedColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.secondaryContainer
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
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (day.isPeriodActual) {
            Box(
                Modifier
                    .fillMaxSize(0.72f)
                    .clip(CircleShape)
                    .background(actualColor)
            )
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else if (day.isPredicted) {
            Box(
                Modifier
                    .fillMaxSize(0.72f)
                    .clip(CircleShape)
                    .background(predictedColor.copy(alpha = 0.3f))
                    .border(
                        width = 1.dp,
                        color = predictedColor,
                        shape = CircleShape,
                    )
            )
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (day.inCurrentMonth) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
            )
        }

        if (day.hasLog) {
            Box(
                Modifier
                    .size(6.dp)
                    .align(Alignment.BottomCenter)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

@Composable
private fun Legend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(color = MaterialTheme.colorScheme.primary, label = "Menstruasi tercatat")
        LegendItem(color = MaterialTheme.colorScheme.tertiary, label = "Perkiraan")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.size(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DayDetailDialog(
    day: CalendarDay,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(day.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id", "ID"))))
        },
        text = {
            Column {
                if (day.isPeriodActual) {
                    Text("• Menstruasi tercatat")
                }
                if (day.isPredicted) {
                    Text("• Perkiraan periode")
                }
                if (day.hasLog) {
                    Text("• Ada catatan harian")
                }
                if (!day.isPeriodActual && !day.isPredicted && !day.hasLog) {
                    Text("Tidak ada catatan untuk tanggal ini.")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        },
    )
}
