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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.rona.app.ui.components.RonaLoadingSkeleton
import id.rona.app.ui.theme.LocalRonaColors
import id.rona.app.ui.theme.RonaBottomSheetShape
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

import id.rona.app.ui.components.RonaTopBar
import id.rona.app.ui.theme.RonaTheme

private val weekdayLabels = listOf("MIN", "SEN", "SEL", "RAB", "KAM", "JUM", "SAB")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    onOpenSettings: (() -> Unit)? = null,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalRonaColors.current

    if (uiState.isLoading) {
        RonaLoadingSkeleton(modifier = modifier.fillMaxSize(), message = "Memuat kalender…")
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize(),
    ) {
        RonaTopBar(
            onOpenSettings = onOpenSettings,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MonthHeader(
                yearMonth = uiState.yearMonth,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
            )

            // Calendar Card Container
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = colors.surfaceSoft,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    WeekdayHeader()

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(uiState.days, key = { it.date.toEpochDay() }) { day ->
                            DayCell(
                                day = day,
                                isSelected = day.date == uiState.selectedDay,
                                onClick = { viewModel.selectDay(day.date) },
                            )
                        }
                    }
                }
            }

            Legend()
        }
    }

    var showPeriodEditSheet by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var editingPeriodId by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<Long?>(null) }
    var editingInitialDate by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<LocalDate?>(null) }

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
                    onOpenPeriodEdit = { periodId, date ->
                        viewModel.dismissDayDetail()
                        editingPeriodId = periodId
                        editingInitialDate = date
                        showPeriodEditSheet = true
                    },
                    onDismiss = viewModel::dismissDayDetail,
                )
            }
        }
    }

    if (showPeriodEditSheet) {
        val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPeriodEditSheet = false },
            sheetState = editSheetState,
            shape = RonaBottomSheetShape,
        ) {
            id.rona.app.ui.period.PeriodEditBottomSheet(
                periodId = editingPeriodId,
                initialDate = editingInitialDate,
                onDismiss = { showPeriodEditSheet = false },
                onSaved = { showPeriodEditSheet = false },
                onDeleted = { showPeriodEditSheet = false },
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
    val colors = LocalRonaColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Surface(
            onClick = onPrevious,
            shape = CircleShape,
            color = colors.surfaceSoft,
            modifier = Modifier.size(44.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.ChevronLeft,
                    contentDescription = "Bulan sebelumnya",
                    tint = colors.inkSecondary,
                )
            }
        }

        Text(
            text = yearMonth.month.getDisplayName(java.time.format.TextStyle.FULL, Locale("id", "ID"))
                .replaceFirstChar { it.uppercase() } + " " + yearMonth.year,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = colors.inkPrimary,
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        Surface(
            onClick = onNext,
            shape = CircleShape,
            color = colors.surfaceSoft,
            modifier = Modifier.size(44.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = "Bulan berikutnya",
                    tint = colors.inkSecondary,
                )
            }
        }
    }
}

@Composable
private fun WeekdayHeader() {
    val colors = LocalRonaColors.current
    Row(Modifier.fillMaxWidth()) {
        weekdayLabels.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp,
                    color = colors.inkTertiary,
                ),
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
        // Actual period: solid filled rose circle (#8C4558).
        if (day.isPeriodActual) {
            Box(
                Modifier
                    .fillMaxSize(0.85f)
                    .clip(CircleShape)
                    .background(colors.cyclePrimary)
            )
        } else if (day.isPredicted) {
            // Prediction: outline circle (#8C4558 border).
            Box(
                Modifier
                    .fillMaxSize(0.85f)
                    .clip(CircleShape)
                    .border(1.5.dp, colors.cyclePrimary, CircleShape)
            )
        }

        // Today: solid dark ring if not in period.
        if (isToday && !day.isPeriodActual) {
            Box(
                Modifier
                    .fillMaxSize(0.88f)
                    .border(1.5.dp, colors.inkPrimary, CircleShape)
            )
        }

        Text(
            text = day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            ),
            color = when {
                day.isPeriodActual -> colors.onCyclePrimary
                !day.inCurrentMonth -> colors.inkTertiary.copy(alpha = 0.45f)
                else -> colors.inkPrimary
            },
        )

        // Daily log dot indicator badge.
        if (day.hasLog) {
            Box(
                Modifier
                    .size(6.dp)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 1.dp)
                    .clip(CircleShape)
                    .background(if (day.isPeriodActual) colors.surfaceSoft else colors.cyclePrimary)
            )
        }
    }
}

@Composable
private fun Legend() {
    val colors = LocalRonaColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LegendItem(color = colors.cyclePrimary, label = "Tercatat", solid = true)
        LegendItem(color = colors.inkSecondary, label = "Perkiraan", outlined = true)
        LegendItem(color = colors.cyclePrimary, label = "Catatan harian", hasDot = true)
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    solid: Boolean = false,
    outlined: Boolean = false,
    hasDot: Boolean = false,
) {
    val colors = LocalRonaColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .then(
                    when {
                        solid -> Modifier.background(color)
                        outlined -> Modifier.border(1.5.dp, color, CircleShape)
                        hasDot -> Modifier
                            .border(1.5.dp, colors.inkSecondary, CircleShape)
                            .background(Color.Transparent)
                        else -> Modifier.background(color)
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (hasDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
        Spacer(Modifier.size(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                color = colors.inkSecondary,
            ),
        )
    }
}

@Composable
private fun DayDetailSheet(
    day: CalendarDay,
    onOpenPeriodEdit: (Long?, LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalRonaColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            day.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id", "ID"))),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
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

        // Action Buttons
        if (day.isPeriodActual) {
            id.rona.app.ui.components.RonaPrimaryButton(
                text = "Edit catatan periode",
                onClick = { onOpenPeriodEdit(day.periodId, day.date) },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            id.rona.app.ui.components.RonaSecondaryButton(
                text = "+ Catat periode di tanggal ini",
                onClick = { onOpenPeriodEdit(null, day.date) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Tutup")
        }
    }
}
