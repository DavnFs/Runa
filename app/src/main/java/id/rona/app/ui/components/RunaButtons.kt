package id.rona.app.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.rona.app.ui.theme.LocalRonaColors
import id.rona.app.ui.theme.RonaPillShape
import id.rona.app.ui.theme.RonaTheme

import androidx.compose.animation.core.animateFloatAsState
import id.rona.app.ui.theme.RonaMotion

/**
 * Rona primary action button — dusty-rose fill, full pill shape,
 * gentle press scale, min height 52dp for touch comfort.
 */
@Composable
fun RonaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = RonaMotion.appleBouncySpring(),
        label = "btnPressScale",
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RonaPillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = modifier
            .heightIn(min = 52.dp)
            .scale(pressScale),
    ) {
        ButtonContent(text = text, icon = icon)
    }
}

/** Rona secondary action — tonal plum/surface outline, full pill shape. */
@Composable
fun RonaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = RonaMotion.appleBouncySpring(),
        label = "secondaryBtnPressScale",
    )

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RonaPillShape,
        modifier = modifier
            .heightIn(min = 48.dp)
            .scale(pressScale),
    ) {
        ButtonContent(text = text, icon = icon)
    }
}

/** Low-emphasis inline action — no heavy container. */
@Composable
fun RonaTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ButtonContent(text: String, icon: ImageVector?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.width(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

// ───────────────────────── Previews ─────────────────────────

@Preview(name = "RonaPrimaryButton — Default Light", showBackground = true)
@Composable
private fun RonaPrimaryButtonLightPreview() {
    RonaTheme {
        RonaPrimaryButton(
            text = "Catat keadaanmu hari ini",
            icon = Icons.Rounded.Edit,
            onClick = {},
        )
    }
}

@Preview(name = "RonaSecondaryButton — Default Light", showBackground = true)
@Composable
private fun RonaSecondaryButtonLightPreview() {
    RonaTheme {
        RonaSecondaryButton(
            text = "Batal",
            onClick = {},
        )
    }
}
