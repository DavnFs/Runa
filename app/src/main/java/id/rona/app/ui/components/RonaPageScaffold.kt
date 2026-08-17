package id.rona.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.rona.app.ui.theme.LocalRonaColors

/**
 * Standard Rona page shell: warm canvas background, consistent horizontal
 * padding, and correct insets.
 *
 * - Top: safeDrawing (status bar).
 * - Bottom: [dockClearance] reserves space for the floating dock, then
 *   navigation bars inset is applied ONCE (dock itself also applies
 *   navigationBarsPadding, so content must NOT add it again).
 * - IME: when [imeAware] is true, imePadding() is applied so editable
 *   forms stay reachable above the keyboard.
 *
 * [dockClearance] defaults to [RonaDockTokens.ContentClearance] so the dock
 * never covers primary content.
 */
@Composable
fun RonaPageScaffold(
    modifier: Modifier = Modifier,
    dockClearance: Dp = RonaDockTokens.ContentClearance,
    imeAware: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val canvas = LocalRonaColors.current.pageCanvas

    Scaffold(
        modifier = modifier,
        containerColor = canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(contentPadding)
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp)
                .padding(bottom = dockClearance)
                .then(
                    if (imeAware) {
                        Modifier.padding(WindowInsets.ime.only(WindowInsetsSides.Bottom).asPaddingValues())
                    } else {
                        Modifier
                    }
                ),
            content = content,
        )
    }
}
