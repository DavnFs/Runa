package id.rona.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import id.rona.app.ui.theme.LocalRonaColors
import id.rona.app.ui.theme.RunaShapes

/**
 * Tonal, calm surface for meaningful content groups — not for every UI block.
 * Uses Runa semantic colors (surfaceSoft / elevatedSurface) with a soft border.
 */
@Composable
fun RunaTonalSurface(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    borderColor: Color = LocalRonaColors.current.dividerSubtle,
    shape: CornerBasedShape = RunaShapes.medium,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, shape),
        shape = shape,
        color = containerColor,
        tonalElevation = 1.dp,
    ) {
        Column(
            Modifier.padding(20.dp),
            content = content,
        )
    }
}

/** Elevated surface for floating/docked elements (nav dock, FAB-like actions). */
@Composable
fun RunaElevatedSurface(
    modifier: Modifier = Modifier,
    containerColor: Color = LocalRonaColors.current.elevatedSurface,
    shape: CornerBasedShape = RunaShapes.large,
    borderColor: Color = LocalRonaColors.current.dividerSubtle,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, shape),
        shape = shape,
        color = containerColor,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
    ) {
        Column(
            Modifier.padding(20.dp),
            content = content,
        )
    }
}
