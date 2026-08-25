package id.rona.app.ui.log

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDate

/**
 * Backward-compatible delegating composable for [FullLogEditorSheet].
 */
@Composable
fun LogEditorSheet(
    date: LocalDate? = null,
    onDismiss: () -> Unit,
    viewModel: LogEditorViewModel = hiltViewModel(),
) {
    FullLogEditorSheet(
        date = date,
        onDismiss = onDismiss,
        viewModel = viewModel,
    )
}
