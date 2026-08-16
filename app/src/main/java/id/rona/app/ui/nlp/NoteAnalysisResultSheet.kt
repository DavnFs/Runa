package id.rona.app.ui.nlp

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.rona.app.domain.model.SymptomType
import id.rona.app.domain.nlp.ConfidenceLevel
import id.rona.app.domain.nlp.NoteAnalysisResult
import id.rona.app.domain.nlp.SafetyAlert
import id.rona.app.domain.nlp.TriageLevel

/**
 * Result sheet for "Analisis catatan". Shows safety alerts FIRST, then
 * editable suggestions, then at most 3 knowledge cards. Nothing is saved
 * until the user applies choices AND presses "Simpan" in the editor.
 */
@Composable
fun NoteAnalysisResultSheet(
    result: NoteAnalysisResult,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    onAcknowledgeSafety: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Analisis catatan", style = MaterialTheme.typography.headlineSmall)

        // ————— Safety alerts first (never hidden behind suggestions) —————
        if (result.safetyAlerts.isNotEmpty()) {
            SafetySection(alerts = result.safetyAlerts, onAcknowledge = onAcknowledgeSafety)
        }

        // ————— Suggestions —————
        if (result.hasAnySuggestions) {
            Text("Kami menemukan beberapa kemungkinan dari catatanmu.", style = MaterialTheme.typography.bodyLarge)
            SuggestionsSection(result = result)
        } else if (result.safetyAlerts.isEmpty()) {
            Text(
                "Tidak ada saran otomatis untuk catatan ini. Semua tetap tersimpan sebagai catatan pribadimu.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        // ————— Knowledge cards (max 3) —————
        if (result.relevantKnowledgeCards.isNotEmpty()) {
            Text("Informasi terkait", style = MaterialTheme.typography.titleMedium)
            result.relevantKnowledgeCards.forEach { card ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(card.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(card.shortSummary, style = MaterialTheme.typography.bodySmall)
                        if (!card.whatToAvoidClaiming.isNullOrEmpty() && card.whatToAvoidClaiming.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                card.whatToAvoidClaiming.joinToString(" "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        card.medicalDisclaimer?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text("Tutup tanpa menerapkan")
            }
            Button(onClick = onApply, modifier = Modifier.weight(1f)) {
                Text("Terapkan pilihan")
            }
        }
    }
}

@Composable
private fun SafetySection(
    alerts: List<SafetyAlert>,
    onAcknowledge: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Perlu perhatian",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        alerts.forEach { alert ->
            val isEmergency = alert.triageLevel == TriageLevel.EMERGENCY
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Peringatan: ${alert.title}. ${alert.message}" },
                colors = CardDefaults.cardColors(
                    containerColor = when (alert.triageLevel) {
                        TriageLevel.EMERGENCY -> MaterialTheme.colorScheme.errorContainer
                        TriageLevel.URGENT -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    },
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = null,
                            tint = if (isEmergency) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Spacer(Modifier.height(0.dp))
                        Text(
                            alert.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(alert.message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        OutlinedButton(onClick = onAcknowledge, modifier = Modifier.fillMaxWidth()) {
            Text("Saya sudah membacanya")
        }
    }
}

@Composable
private fun SuggestionsSection(result: NoteAnalysisResult) {
    val selectedSymptoms = remember { mutableStateMapOf<SymptomType, Boolean>() }
    var allChecked by remember { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Saran untuk ditambahkan", style = MaterialTheme.typography.titleMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Pilih semua", style = MaterialTheme.typography.bodyMedium)
            Checkbox(checked = allChecked, onCheckedChange = { checked ->
                allChecked = checked
                result.symptomSuggestions.forEach { selectedSymptoms[it.symptomType] = checked }
            })
        }

        result.symptomSuggestions.forEach { suggestion ->
            val checked = selectedSymptoms[suggestion.symptomType] ?: true
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = checked, onCheckedChange = { selectedSymptoms[suggestion.symptomType] = it })
                Column(Modifier.padding(start = 4.dp)) {
                    Text(symptomLabel(suggestion.symptomType), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        buildString {
                            append(confidenceLabel(suggestion.confidence))
                            suggestion.severity?.let { append(" · ${severityLabel(it)}") }
                            if (suggestion.matchedTerms.isNotEmpty()) {
                                append(" · dari kata: ${suggestion.matchedTerms.joinToString(", ")}")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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

private fun severityLabel(severity: id.rona.app.domain.model.Severity): String = when (severity) {
    id.rona.app.domain.model.Severity.MILD -> "ringan"
    id.rona.app.domain.model.Severity.MODERATE -> "sedang"
    id.rona.app.domain.model.Severity.SEVERE -> "berat"
}

private fun confidenceLabel(confidence: ConfidenceLevel): String = when (confidence) {
    ConfidenceLevel.HIGH -> "Sangat mungkin"
    ConfidenceLevel.MEDIUM -> "Mungkin"
    ConfidenceLevel.LOW -> "Kemungkinan kecil"
}
