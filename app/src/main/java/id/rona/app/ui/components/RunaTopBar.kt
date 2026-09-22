package id.rona.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.rona.app.domain.model.ThemeMode
import id.rona.app.ui.theme.LocalRonaColors
import id.rona.app.ui.theme.RonaTheme
import id.rona.app.ui.theme.RonaWordmarkStyle

/**
 * Standard Runa TopBar across main destinations (Beranda, Kalender, Insight).
 *
 * Visual layout:
 * - Center: Elegant RUNA wordmark with optional date/context subtitle.
 * - Trailing: Settings action icon (⚙).
 * - Leading: Optional leading action or balanced spacer (No hamburger menu).
 */
@Composable
fun RonaTopBar(
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onOpenSettings: (() -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    val colors = LocalRonaColors.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Leading slot
            if (leadingContent != null) {
                Box(
                    modifier = Modifier.align(Alignment.CenterStart),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    leadingContent()
                }
            }

            // Center: Wordmark + optional subtitle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 48.dp),
            ) {
                Text(
                    text = "RUNA",
                    style = RonaWordmarkStyle,
                    color = colors.cyclePrimary,
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Trailing: Settings icon
            if (onOpenSettings != null) {
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Pengaturan",
                        tint = colors.inkSecondary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

// ───────────────────────── Previews ─────────────────────────

@Preview(name = "RonaTopBar — Default Light", showBackground = true)
@Composable
private fun RonaTopBarLightPreview() {
    RonaTheme {
        RonaTopBar(
            subtitle = "Senin, 17 Agustus",
            onOpenSettings = {},
        )
    }
}

@Preview(name = "RonaTopBar — Wordmark Only Light", showBackground = true)
@Composable
private fun RonaTopBarWordmarkOnlyLightPreview() {
    RonaTheme {
        RonaTopBar(
            onOpenSettings = {},
        )
    }
}

@Preview(
    name = "RonaTopBar — Dark",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun RonaTopBarDarkPreview() {
    RonaTheme(themeMode = ThemeMode.DARK) {
        RonaTopBar(
            subtitle = "Senin, 17 Agustus",
            onOpenSettings = {},
        )
    }
}
