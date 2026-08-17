package id.rona.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import id.rona.app.ui.theme.LocalRonaColors

/**
 * Tactile, readable selectable chip for symptoms / quick options.
 * Selected state: rose container + check-free clear fill (not color-only).
 */
@Composable
fun RonaSelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRonaColors.current
    val container = if (selected) colors.cycleContainer else MaterialTheme.colorScheme.surface
    val content = if (selected) colors.onCycleContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (selected) colors.cyclePrimary else colors.dividerSubtle

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = container,
        contentColor = content,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = modifier.semantics {
            this.selected = selected
            this.role = Role.Checkbox
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}
