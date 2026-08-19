package id.rona.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.rona.app.ui.theme.LocalRonaColors
import id.rona.app.ui.theme.RonaTheme

/**
 * Soft journal-like note field — not a generic outlined TextField.
 * Uses Rona surfaceSoft with a subtle border and relaxed inner padding.
 */
@Composable
fun RonaJournalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Tuliskan apa yang kamu rasakan…",
    minLines: Int = 4,
) {
    val colors = LocalRonaColors.current
    val shape = RoundedCornerShape(18.dp)

    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .border(1.dp, colors.dividerSubtle, shape),
        placeholder = {
            Text(
                placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkTertiary,
            )
        },
        minLines = minLines,
        shape = shape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceSoft,
            unfocusedContainerColor = colors.surfaceSoft,
            disabledContainerColor = colors.surfaceSoft,
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            cursorColor = colors.cyclePrimary,
            focusedTextColor = colors.inkPrimary,
            unfocusedTextColor = colors.inkPrimary,
        ),
        textStyle = MaterialTheme.typography.bodyLarge,
    )
}

// ───────────────────────── Previews ─────────────────────────

@Preview(name = "RonaJournalTextField — Empty Light", showBackground = true)
@Composable
private fun RonaJournalTextFieldEmptyPreview() {
    RonaTheme {
        RonaJournalTextField(
            value = "",
            onValueChange = {},
            placeholder = "Tuliskan apapun yang kamu rasakan hari ini...",
        )
    }
}

@Preview(name = "RonaJournalTextField — Filled Light", showBackground = true)
@Composable
private fun RonaJournalTextFieldFilledPreview() {
    RonaTheme {
        RonaJournalTextField(
            value = "Merasa agak lelah di sore hari, perut sedikit kembung setelah makan siang.",
            onValueChange = {},
        )
    }
}
