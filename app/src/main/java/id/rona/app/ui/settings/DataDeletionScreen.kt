package id.rona.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.rona.app.data.repository.DataCategory
import id.rona.app.ui.components.RonaJournalTextField
import id.rona.app.ui.components.RonaSecondaryButton
import id.rona.app.ui.theme.LocalRonaColors

@Composable
fun DataDeletionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeletionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalRonaColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Data & privasi", style = MaterialTheme.typography.headlineSmall)

        Text(
            "Data hanya tersimpan di perangkat ini. Menghapus data tidak bisa dibatalkan.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))

        RonaSecondaryButton(
            text = "Hapus catatan harian",
            onClick = { viewModel.deleteCategory(DataCategory.DAILY_LOGS) },
            enabled = !uiState.isDeleting,
            modifier = Modifier.fillMaxWidth(),
        )
        RonaSecondaryButton(
            text = "Hapus riwayat periode",
            onClick = { viewModel.deleteCategory(DataCategory.PERIOD_HISTORY) },
            enabled = !uiState.isDeleting,
            modifier = Modifier.fillMaxWidth(),
        )
        RonaSecondaryButton(
            text = "Reset pengaturan",
            onClick = { viewModel.deleteCategory(DataCategory.SETTINGS) },
            enabled = !uiState.isDeleting,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        // ————— Destructive action, visually separated & calm —————
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Hapus semua data", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Menghapus seluruh catatan, riwayat, pengaturan, dan PIN. " +
                        "Export backup dulu bila ragu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RonaJournalTextField(
                    value = uiState.confirmationText,
                    onValueChange = viewModel::setConfirmationText,
                    placeholder = "Ketik HAPUS untuk mengonfirmasi",
                    minLines = 1,
                )
                Button(
                    onClick = viewModel::deleteAll,
                    enabled = !uiState.isDeleting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (uiState.isDeleting) "Menghapus…" else "Hapus semua data",
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        if (uiState.message != null) {
            Text(
                uiState.message!!,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
