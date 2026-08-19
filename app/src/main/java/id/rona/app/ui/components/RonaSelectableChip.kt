package id.rona.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.rona.app.ui.theme.LocalRonaColors
import id.rona.app.ui.theme.RonaMotion
import id.rona.app.ui.theme.RonaPillShape
import id.rona.app.ui.theme.RonaTheme

/**
 * Tactile, fluid selectable chip for symptoms / quick options with Apple-grade micro-interactions.
 * Selected state: animated rose container + animated border + fluid spring scale.
 * Minimum height 44dp; wraps naturally inside FlowRow.
 */
@Composable
fun RonaSelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRonaColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.cycleContainer else MaterialTheme.colorScheme.surface,
        animationSpec = RonaMotion.ColorSpec,
        label = "chipContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.onCycleContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = RonaMotion.ColorSpec,
        label = "chipContent",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.cyclePrimary else colors.dividerSubtle,
        animationSpec = RonaMotion.ColorSpec,
        label = "chipBorder",
    )
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = RonaMotion.appleBouncySpring(),
        label = "chipPressScale",
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RonaPillShape,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .scale(pressScale)
            .semantics {
                this.selected = selected
                this.role = Role.Checkbox
            },
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 44.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

// ───────────────────────── Previews ─────────────────────────

@Preview(name = "RonaSelectableChip — Unselected Light", showBackground = true)
@Composable
private fun RonaSelectableChipUnselectedPreview() {
    RonaTheme {
        RonaSelectableChip(
            label = "Kram perut",
            selected = false,
            onClick = {},
        )
    }
}

@Preview(name = "RonaSelectableChip — Selected Light", showBackground = true)
@Composable
private fun RonaSelectableChipSelectedPreview() {
    RonaTheme {
        RonaSelectableChip(
            label = "Kram perut",
            selected = true,
            onClick = {},
        )
    }
}
