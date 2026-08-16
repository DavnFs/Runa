package id.rona.app.ui.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.rona.app.data.repository.BackupRepository
import id.rona.app.util.PrivacyLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val isWorking: Boolean = false,
    val passphrase: String = "",
    val passphraseConfirm: String = "",
    val message: String? = null,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun setPassphrase(value: String) {
        _uiState.update { it.copy(passphrase = value) }
    }

    fun setPassphraseConfirm(value: String) {
        _uiState.update { it.copy(passphraseConfirm = value) }
    }

    fun validatePassphrase(): String? {
        val state = _uiState.value
        if (state.passphrase.length < 8) return "Passphrase minimal 8 karakter."
        if (state.passphrase != state.passphraseConfirm) return "Passphrase tidak cocok."
        return null
    }

    fun setValidationError() {
        _uiState.update { it.copy(message = validatePassphrase()) }
    }

    fun exportTo(uri: Uri, fileName: String?, contentResolver: ContentResolver) {
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, message = null) }
            try {
                val passphrase = _uiState.value.passphrase
                contentResolver.openOutputStream(uri)?.use { out ->
                    backupRepository.export(passphrase, out, fileName)
                } ?: throw IllegalStateException("Cannot open output stream")
                _uiState.update { it.copy(isWorking = false, message = "Backup berhasil dibuat.") }
            } catch (e: Exception) {
                PrivacyLogger.e("BackupVM") { "export failed: ${e.javaClass.simpleName}" }
                _uiState.update {
                    it.copy(isWorking = false, message = "Export gagal. Coba lagi.")
                }
            }
        }
    }

    fun restoreFrom(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, message = null) }
            try {
                val passphrase = _uiState.value.passphrase
                contentResolver.openInputStream(uri)?.use { input ->
                    backupRepository.restore(input, passphrase)
                } ?: throw IllegalStateException("Cannot open input stream")
                _uiState.update { it.copy(isWorking = false, message = "Backup dipulihkan.") }
            } catch (e: Exception) {
                PrivacyLogger.e("BackupVM") { "restore failed: ${e.javaClass.simpleName}" }
                _uiState.update {
                    it.copy(isWorking = false, message = "Restore gagal. Periksa passphrase & file.")
                }
            }
        }
    }
}
