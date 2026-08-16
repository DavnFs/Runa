package id.rona.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.rona.app.data.repository.DataCategory
import id.rona.app.data.repository.DataManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeletionUiState(
    val isDeleting: Boolean = false,
    val confirmationText: String = "",
    val message: String? = null,
)

@HiltViewModel
class DeletionViewModel @Inject constructor(
    private val dataManager: DataManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeletionUiState())
    val uiState: StateFlow<DeletionUiState> = _uiState.asStateFlow()

    fun setConfirmationText(value: String) {
        _uiState.update { it.copy(confirmationText = value) }
    }

    fun deleteCategory(category: DataCategory) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            dataManager.deleteCategory(category)
            _uiState.update { it.copy(isDeleting = false, message = "Data kategori dihapus.") }
        }
    }

    fun deleteAll() {
        if (_uiState.value.confirmationText.trim() != CONFIRM_WORD) {
            _uiState.update { it.copy(message = "Ketik $CONFIRM_WORD untuk melanjutkan.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            dataManager.deleteAllData()
            _uiState.update { it.copy(isDeleting = false, message = "Semua data dihapus.") }
        }
    }

    companion object {
        const val CONFIRM_WORD = "HAPUS"
    }
}
