package id.rona.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun BackupExportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var fileName by remember { mutableStateOf("rona-backup-${System.currentTimeMillis()}.rona") }

    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            viewModel.exportTo(uri, fileName, context.contentResolver)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Export backup terenkripsi", style = MaterialTheme.typography.headlineSmall)

        Text(
            "File .rona dienkripsi dengan passphrase-mu. Tanpa passphrase, " +
                "file tidak bisa dibaca — termasuk oleh kami. Jika passphrase " +
                "lupa, backup tidak dapat dipulihkan.",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = uiState.passphrase,
            onValueChange = viewModel::setPassphrase,
            label = { Text("Passphrase (min. 8 karakter)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.passphraseConfirm,
            onValueChange = viewModel::setPassphraseConfirm,
            label = { Text("Ulangi passphrase") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (uiState.message != null) {
            Text(uiState.message!!, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                if (viewModel.validatePassphrase() == null) {
                    createDocument.launch(fileName)
                } else {
                    viewModel.setValidationError()
                }
            },
            enabled = !uiState.isWorking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.isWorking) "Mengekspor…" else "Pilih lokasi & export")
        }
    }
}

@Composable
fun BackupRestoreScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.restoreFrom(uri, context.contentResolver)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Restore dari backup", style = MaterialTheme.typography.headlineSmall)

        Text(
            "Restore akan menggantikan seluruh data saat ini dengan isi backup.",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = uiState.passphrase,
            onValueChange = viewModel::setPassphrase,
            label = { Text("Passphrase backup") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (uiState.message != null) {
            Text(uiState.message!!, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = { openDocument.launch(arrayOf("application/octet-stream", "*/*")) },
            enabled = !uiState.isWorking && uiState.passphrase.length >= 8,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.isWorking) "Memulihkan…" else "Pilih file .rona")
        }
    }
}
