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

import id.rona.app.ui.theme.runaDynamicDockClearance

/**
 * Standard Runa page shell: warm canvas background, consistent horizontal
 * padding, and correct insets.
 *
 * - Top: safeDrawing (status bar).
 * - Bottom: [dockClearance] dynamically reserves space for the floating dock,
 *   including bottom margin, navigation bars inset, and safety scroll padding.
 * - IME: when [imeAware] is true, imePadding() is applied so editable
 *   forms stay reachable above the keyboard.
 */
@Composable
fun RunaPageScaffold(
    modifier: Modifier = Modifier,
    dockClearance: Dp = runaDynamicDockClearance(),
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
                .runaPageContainer()
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
