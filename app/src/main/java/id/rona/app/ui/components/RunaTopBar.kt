package id.rona.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.rona.app.domain.model.ThemeMode
import id.rona.app.ui.theme.LocalRonaColors
import id.rona.app.ui.theme.RunaTheme

/**
 * Standard Runa TopBar across main destinations (Beranda, Kalender, Insight).
 *
 * Frosted in the BitChord manner: translucent glass over the scrolling page
 * content (real blur when a haze source is present), rounded bottom corners,
 * and a hairline bottom border.
 */
@Composable
fun RunaTopBar(
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onOpenSettings: (() -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    val colors = LocalRonaColors.current
    val glassShape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(glassShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Hairline bottom border
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.dividerSubtle),
            )

            // Leading slot
            if (leadingContent != null) {
                Box(
                    modifier = Modifier.align(Alignment.CenterStart),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    leadingContent()
                }
            }

            // Center: brand mark + optional subtitle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 48.dp),
            ) {
                RunaLogoMark(size = 30.dp)
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

@Preview(name = "RunaTopBar — Default Light", showBackground = true)
@Composable
private fun RunaTopBarLightPreview() {
    RunaTheme {
        RunaTopBar(
            subtitle = "Senin, 17 Agustus",
            onOpenSettings = {},
        )
    }
}

@Preview(name = "RunaTopBar — Wordmark Only Light", showBackground = true)
@Composable
private fun RunaTopBarWordmarkOnlyLightPreview() {
    RunaTheme {
        RunaTopBar(
            onOpenSettings = {},
        )
    }
}

@Preview(
    name = "RunaTopBar — Dark",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun RunaTopBarDarkPreview() {
    RunaTheme(themeMode = ThemeMode.DARK) {
        RunaTopBar(
            subtitle = "Senin, 17 Agustus",
            onOpenSettings = {},
        )
    }
}
