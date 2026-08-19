package id.rona.app.ui.period

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.rona.app.domain.model.OverlapResolutionStrategy
import id.rona.app.domain.model.PeriodValidationResult
import id.rona.app.ui.components.RonaPrimaryButton
import id.rona.app.ui.components.RonaSecondaryButton
import id.rona.app.ui.theme.LocalRonaColors
import id.rona.app.ui.theme.RonaPillShape
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodEditBottomSheet(
    periodId: Long?,
    initialDate: LocalDate?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PeriodEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalRonaColors.current

    LaunchedEffect(periodId, initialDate) {
        viewModel.initialize(periodId, initialDate)
    }

    LaunchedEffect(uiState.isSavedSuccessfully) {
        if (uiState.isSavedSuccessfully) {
            onSaved()
        }
    }

    LaunchedEffect(uiState.isDeletedSuccessfully) {
        if (uiState.isDeletedSuccessfully) {
            onDeleted()
        }
    }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val isEditMode = uiState.initialPeriodId != 0L
    val titleText = if (isEditMode) "Edit Catatan Periode" else "Catat Periode"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary,
                ),
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Tutup",
                    tint = colors.inkSecondary,
                )
            }
        }

        // Error Banner if date order is invalid
        if (uiState.errorMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = uiState.errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                        ),
                    )
                }
            }
        }

        // Field 1: Tanggal Mulai
        DateFieldSelector(
            label = "Tanggal Mulai",
            date = uiState.startDate,
            onClick = { showStartDatePicker = true },
        )

        // Field 2: Tanggal Selesai (atau toggle masih berlangsung)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!uiState.isOngoing) {
                DateFieldSelector(
                    label = "Tanggal Selesai",
                    date = uiState.endDate ?: uiState.startDate,
                    onClick = { showEndDatePicker = true },
                )
            }

            // Checkbox "Periode masih berlangsung"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setOngoing(!uiState.isOngoing) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = uiState.isOngoing,
                    onCheckedChange = { viewModel.setOngoing(it) },
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Periode ini masih berlangsung",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = colors.inkPrimary,
                        fontSize = 15.sp,
                    ),
                )
            }
        }

        // Action Buttons
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RonaPrimaryButton(
                text = if (uiState.isLoading) "Menyimpan…" else "Simpan perubahan",
                onClick = viewModel::requestSave,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (isEditMode) {
                    OutlinedButton(
                        onClick = viewModel::showDeleteDialog,
                        shape = RonaPillShape,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Hapus", style = MaterialTheme.typography.labelLarge)
                    }
                }

                RonaSecondaryButton(
                    text = "Batal",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }

    // ───────────────────────── Date Pickers ─────────────────────────
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.startDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        viewModel.setStartDate(picked)
                    }
                    showStartDatePicker = false
                }) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val currentEnd = uiState.endDate ?: uiState.startDate
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentEnd.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        viewModel.setEndDate(picked)
                    }
                    showEndDatePicker = false
                }) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ───────────────────────── Dialogs ─────────────────────────

    // 1. Overlap Conflict Dialog
    if (uiState.showOverlapDialog) {
        val conflict = uiState.validationResult as? PeriodValidationResult.OverlapConflict
        AlertDialog(
            onDismissRequest = viewModel::dismissOverlapDialog,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = colors.plumAccent,
                )
            },
            title = {
                Text("Catatan Beririsan", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            },
            text = {
                Text(
                    text = conflict?.message
                        ?: "Rentang tanggal ini beririsan dengan catatan periode lain. Pilih tindakan:",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.resolveOverlapAndSave(OverlapResolutionStrategy.MERGE) },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.cyclePrimary),
                ) {
                    Text("Gabungkan rentang")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { viewModel.resolveOverlapAndSave(OverlapResolutionStrategy.REPLACE) }) {
                        Text("Ganti lama")
                    }
                    TextButton(onClick = viewModel::dismissOverlapDialog) {
                        Text("Kembali")
                    }
                }
            }
        )
    }

    // 2. Unusual Duration Warning Dialog (>15 days)
    if (uiState.showWarningConfirmation) {
        val warning = uiState.validationResult as? PeriodValidationResult.UnusualDurationWarning
        AlertDialog(
            onDismissRequest = viewModel::dismissWarningDialog,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = colors.amberAccent,
                )
            },
            title = {
                Text("Konfirmasi Durasi", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            },
            text = {
                Text(
                    text = warning?.message
                        ?: "Periode ini tercatat lebih dari 15 hari. Pastikan tanggalnya sudah benar.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmSaveDespiteWarning,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.cyclePrimary),
                ) {
                    Text("Simpan tetap")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissWarningDialog) {
                    Text("Periksa tanggal")
                }
            }
        )
    }

    // 3. Delete Confirmation Dialog
    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = {
                Text("Hapus Catatan Periode?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            },
            text = {
                Text(
                    "Catatan periode ini akan dihapus permanen. Perkiraan siklus dan insight akan disesuaikan otomatis.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDialog) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun DateFieldSelector(
    label: String,
    date: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRonaColors.current
    val formatted = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id", "ID")))

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp,
                color = colors.inkTertiary,
            ),
        )

        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            color = colors.surfaceSoft,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatted,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.inkPrimary,
                    ),
                )
                Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = null,
                    tint = colors.inkSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
