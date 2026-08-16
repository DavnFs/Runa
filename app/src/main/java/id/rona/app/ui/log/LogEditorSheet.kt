package id.rona.app.ui.log

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.rona.app.domain.model.Energy
import id.rona.app.domain.model.FlowLevel
import id.rona.app.domain.model.Mood
import id.rona.app.domain.model.Severity
import id.rona.app.domain.model.SymptomType

@Composable
fun LogEditorSheet(
    date: java.time.LocalDate? = null,
    onDismiss: () -> Unit,
    viewModel: LogEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(date) {
        viewModel.load(date ?: java.time.LocalDate.now())
    }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onDismiss()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Catat hari ini", style = MaterialTheme.typography.headlineSmall)

        if (uiState.isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            return@Column
        }

        FlowSection(
            selected = uiState.flow,
            onSelect = viewModel::selectFlow,
        )

        SymptomSection(
            selectedSymptoms = uiState.selectedSymptoms,
            onToggle = viewModel::toggleSymptom,
            onSeverity = viewModel::setSymptomSeverity,
        )

        MoodSection(selected = uiState.mood, onSelect = viewModel::selectMood)
        EnergySection(selected = uiState.energy, onSelect = viewModel::selectEnergy)

        OutlinedTextField(
            value = uiState.note,
            onValueChange = viewModel::setNote,
            label = { Text("Catatan pribadi") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )

        val errorMessage = uiState.error
        if (errorMessage != null) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = viewModel::delete,
                modifier = Modifier.weight(1f),
            ) {
                Text("Hapus")
            }
            Button(
                onClick = viewModel::save,
                enabled = !uiState.isSaving,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (uiState.isSaving) "Menyimpan…" else "Simpan")
            }
        }
    }
}

@Composable
private fun FlowSection(
    selected: FlowLevel?,
    onSelect: (FlowLevel?) -> Unit,
) {
    Column {
        Text("Flow", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowLevel.entries.forEach { flow ->
                FilterChip(
                    selected = selected == flow,
                    onClick = { onSelect(if (selected == flow) null else flow) },
                    label = {
                        Text(
                            when (flow) {
                                FlowLevel.SPOTTING -> "Spotting"
                                FlowLevel.LIGHT -> "Ringan"
                                FlowLevel.MEDIUM -> "Sedang"
                                FlowLevel.HEAVY -> "Banyak"
                            }
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SymptomSection(
    selectedSymptoms: Map<SymptomType, Severity>,
    onToggle: (SymptomType) -> Unit,
    onSeverity: (SymptomType, Severity) -> Unit,
) {
    Column {
        Text("Gejala", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SymptomType.entries.forEach { symptom ->
                val severity = selectedSymptoms[symptom]
                Column {
                    FilterChip(
                        selected = severity != null,
                        onClick = { onToggle(symptom) },
                        label = { Text(symptomLabel(symptom)) },
                    )
                    if (severity != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Severity.entries.forEach { sev ->
                                FilterChip(
                                    selected = severity == sev,
                                    onClick = { onSeverity(symptom, sev) },
                                    label = {
                                        Text(
                                            when (sev) {
                                                Severity.MILD -> "Ringan"
                                                Severity.MODERATE -> "Sedang"
                                                Severity.SEVERE -> "Berat"
                                            }
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodSection(selected: Mood?, onSelect: (Mood?) -> Unit) {
    Column {
        Text("Mood", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            Mood.entries.forEachIndexed { index, mood ->
                SegmentedButton(
                    selected = selected == mood,
                    onClick = { onSelect(if (selected == mood) null else mood) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = Mood.entries.size),
                ) {
                    Text(moodLabel(mood))
                }
            }
        }
    }
}

@Composable
private fun EnergySection(selected: Energy?, onSelect: (Energy?) -> Unit) {
    Column {
        Text("Energi", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            Energy.entries.forEachIndexed { index, energy ->
                SegmentedButton(
                    selected = selected == energy,
                    onClick = { onSelect(if (selected == energy) null else energy) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = Energy.entries.size),
                ) {
                    Text(energyLabel(energy))
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

private fun moodLabel(mood: Mood): String = when (mood) {
    Mood.VERY_LOW -> "Sangat rendah"
    Mood.LOW -> "Rendah"
    Mood.NEUTRAL -> "Netral"
    Mood.GOOD -> "Baik"
    Mood.GREAT -> "Sangat baik"
}

private fun energyLabel(energy: Energy): String = when (energy) {
    Energy.VERY_LOW -> "Sangat lelah"
    Energy.LOW -> "Lelah"
    Energy.NEUTRAL -> "Sedang"
    Energy.HIGH -> "Berenergi"
    Energy.VERY_HIGH -> "Sangat berenergi"
}
