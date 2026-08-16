package id.rona.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.rona.app.data.repository.DataCategory

@Composable
fun DataDeletionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeletionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Data & privasi", style = MaterialTheme.typography.headlineSmall)

        Text(
            "Data hanya tersimpan di perangkat ini. Menghapus data tidak bisa dibatalkan.",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedButton(
            onClick = { viewModel.deleteCategory(DataCategory.DAILY_LOGS) },
            enabled = !uiState.isDeleting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Hapus catatan harian")
        }
        OutlinedButton(
            onClick = { viewModel.deleteCategory(DataCategory.PERIOD_HISTORY) },
            enabled = !uiState.isDeleting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Hapus riwayat periode")
        }
        OutlinedButton(
            onClick = { viewModel.deleteCategory(DataCategory.SETTINGS) },
            enabled = !uiState.isDeleting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Reset pengaturan")
        }

        Spacer(Modifier.height(16.dp))

        Text("Hapus semua data", style = MaterialTheme.typography.titleMedium)
        Text(
            "Menghapus seluruh catatan, riwayat, pengaturan, dan PIN. " +
                "Export backup dulu bila ragu.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = uiState.confirmationText,
            onValueChange = viewModel::setConfirmationText,
            label = { Text("Ketik HAPUS untuk mengonfirmasi") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = viewModel::deleteAll,
            enabled = !uiState.isDeleting,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.isDeleting) "Menghapus…" else "Hapus semua data")
        }

        if (uiState.message != null) {
            Text(uiState.message!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
