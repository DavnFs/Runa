package id.rona.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.rona.app.ui.theme.LocalRonaColors

/**
 * Standard Rona page shell: warm canvas background, edge-to-edge content,
 * consistent horizontal padding, and bottom content inset so the floating
 * nav dock never covers primary content.
 *
 * @param bottomInset extra bottom space reserved for the nav dock + spacing.
 */
@Composable
fun RonaPageScaffold(
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets = WindowInsets.safeDrawing,
    bottomInset: androidx.compose.ui.unit.Dp = 96.dp,
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
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp)
                .padding(bottom = bottomInset)
                .padding(WindowInsets.navigationBars.asPaddingValues()),
            content = content,
        )
    }
}
