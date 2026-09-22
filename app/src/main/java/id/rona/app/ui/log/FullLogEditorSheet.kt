package id.rona.app.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.rona.app.domain.model.Energy
import id.rona.app.domain.model.FlowLevel
import id.rona.app.domain.model.Mood
import id.rona.app.domain.model.Severity
import id.rona.app.domain.model.SymptomType
import id.rona.app.ui.components.RunaJournalTextField
import id.rona.app.ui.components.RunaPrimaryButton
import id.rona.app.ui.components.RunaSecondaryButton
import id.rona.app.ui.components.RunaSection
import id.rona.app.ui.components.RunaSelectableChip
import id.rona.app.ui.nlp.NoteAnalysisResultSheet
import id.rona.app.ui.theme.LocalRonaColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Complete, single-view long-form Daily Log Editor Sheet.
 * Repurposed from original LogEditorSheet as the "Full Log" editor for comprehensive entry or "Tambah detail".
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FullLogEditorSheet(
    date: LocalDate? = null,
    sessionId: Long = 0L,
    onDismiss: () -> Unit,
    viewModel: LogEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Always (re)load on open so reopening after a save starts fresh.
    LaunchedEffect(sessionId, date) {
        viewModel.load(sessionId, date ?: java.time.LocalDate.now())
    }

    LaunchedEffect(uiState.saveState) {
        if (uiState.saveState == SaveState.Saved) {
            onDismiss()
            viewModel.acknowledgeSaved()
        }
    }

    val effectiveDate = date ?: uiState.date
    val dateFormatted = effectiveDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("id", "ID")))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Sheet Header: Date + Title + Close Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = dateFormatted,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
                Text(
                    text = "Editor Catatan Lengkap",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Tutup",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            return@Column
        }

        // ——— Perasaan tubuh (Flow) ———
        RunaSection(title = "Aliran Menstruasi") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FlowLevel.entries.forEach { flow ->
                    RunaSelectableChip(
                        label = when (flow) {
                            FlowLevel.SPOTTING -> "Spotting"
                            FlowLevel.LIGHT -> "Ringan"
                            FlowLevel.MEDIUM -> "Sedang"
                            FlowLevel.HEAVY -> "Banyak"
                        },
                        selected = uiState.flow == flow,
                        onClick = { viewModel.selectFlow(if (uiState.flow == flow) null else flow) },
                    )
                }
            }
        }

        // ——— Intensitas (gejala + tingkat) ———
        RunaSection(title = "Intensitas Gejala", supporting = "Pilih gejala yang kamu rasakan beserta tingkat keparahannya") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SymptomType.entries.forEach { symptom ->
                    val severity = uiState.selectedSymptoms[symptom]
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        RunaSelectableChip(
                            label = symptomLabel(symptom),
                            selected = severity != null,
                            onClick = { viewModel.toggleSymptom(symptom) },
                        )
                        if (severity != null) {
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                Severity.entries.forEachIndexed { index, sev ->
                                    SegmentedButton(
                                        selected = severity == sev,
                                        onClick = { viewModel.setSymptomSeverity(symptom, sev) },
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = Severity.entries.size,
                                        ),
                                    ) {
                                        Text(
                                            when (sev) {
                                                Severity.MILD -> "Ringan"
                                                Severity.MODERATE -> "Sedang"
                                                Severity.SEVERE -> "Berat"
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ——— Energi ———
        RunaSection(title = "Tingkat Energi", supporting = "Rendah — Sedang — Tinggi") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Energy.entries.forEach { energy ->
                    RunaSelectableChip(
                        label = energyLabel(energy),
                        selected = uiState.energy == energy,
                        onClick = { viewModel.selectEnergy(if (uiState.energy == energy) null else energy) },
                    )
                }
            }
        }

        // ——— Mood ———
        RunaSection(title = "Suasana Hati (Mood)") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Mood.entries.forEach { mood ->
                    RunaSelectableChip(
                        label = moodLabel(mood),
                        selected = uiState.mood == mood,
                        onClick = { viewModel.selectMood(if (uiState.mood == mood) null else mood) },
                    )
                }
            }
        }

        // ——— Catatan pribadi ———
        RunaSection(title = "Catatan pribadi") {
            RunaJournalTextField(
                value = uiState.note,
                onValueChange = viewModel::setNote,
                minLines = 4,
            )
        }

        // ——— NLP analysis ———
        OutlinedButton(
            onClick = viewModel::runAnalysis,
            enabled = uiState.note.isNotBlank() && !uiState.isAnalyzing,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (uiState.isAnalyzing) {
                CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Menganalisis…")
            } else {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.height(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Analisis catatan")
            }
        }

        val errorMessage = uiState.error
        if (errorMessage != null) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(4.dp))
    }

    // ————— Sticky bottom CTA (IME-aware) —————
    val colors = LocalRonaColors.current
    Surface(
        color = colors.pageCanvas,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(WindowInsets.ime.only(WindowInsetsSides.Bottom).asPaddingValues()),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RunaSecondaryButton(
                text = "Hapus",
                onClick = viewModel::delete,
                modifier = Modifier.weight(1f),
            )
            RunaPrimaryButton(
                text = if (uiState.isSaving) "Menyimpan…" else "Simpan catatan",
                onClick = viewModel::save,
                enabled = !uiState.isSaving,
                modifier = Modifier.weight(1f),
            )
        }
    }

    // ————— Analysis result bottom sheet —————
    if (uiState.showAnalysisResult && uiState.analysisResult != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissAnalysisResult,
            sheetState = sheetState,
        ) {
            NoteAnalysisResultSheet(
                result = uiState.analysisResult!!,
                onApply = viewModel::applySuggestions,
                onDismiss = viewModel::dismissAnalysisResult,
                onAcknowledgeSafety = viewModel::acknowledgeSafetyAlerts,
            )
        }
    }
}

internal fun symptomLabel(symptom: SymptomType): String = when (symptom) {
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

internal fun moodLabel(mood: Mood): String = when (mood) {
    Mood.VERY_LOW -> "Sangat rendah"
    Mood.LOW -> "Rendah"
    Mood.NEUTRAL -> "Netral"
    Mood.GOOD -> "Baik"
    Mood.GREAT -> "Sangat baik"
}

internal fun energyLabel(energy: Energy): String = when (energy) {
    Energy.VERY_LOW -> "Sangat lelah"
    Energy.LOW -> "Lelah"
    Energy.NEUTRAL -> "Sedang"
    Energy.HIGH -> "Berenergi"
    Energy.VERY_HIGH -> "Sangat berenergi"
}
