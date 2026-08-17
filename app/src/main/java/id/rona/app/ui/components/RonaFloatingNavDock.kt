package id.rona.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import id.rona.app.domain.model.ThemeMode
import id.rona.app.ui.theme.LocalRonaColors
import id.rona.app.ui.theme.RonaPillShape
import id.rona.app.ui.theme.RonaTheme

/**
 * Rona floating pill navigation dock — FINAL: icon-only, 3 destinations.
 *
 *   ┌────────────────────────────┐
 *   │       ◉      □      ◌       │
 *   └────────────────────────────┘
 *
 * - Beranda / Kalender / Insight: pure navigation. NO visible labels,
 *   NO Catat action (the primary CTA lives on Beranda), NO companion button.
 * - A single active capsule moves between icons (position + width),
 *   200ms FastOutSlowIn; icon color + subtle scale animate too.
 * - Labels are available via long-press tooltip, TalkBack contentDescription,
 *   and the selected semantic state.
 */
enum class RonaDockDestination(val label: String) {
    HOME("Beranda"),
    CALENDAR("Kalender"),
    INSIGHTS("Insight"),
}

/** Dock dimension tokens (semantic, single source of truth). */
object RonaDockTokens {
    val Height = 64.dp
    val CornerRadius = 32.dp
    val HorizontalMargin = 20.dp
    val BottomOffset = 12.dp
    val MinTouchTarget = 48.dp
    val ItemSpacing = 4.dp

    /** Clearance reserved below page content so the dock never covers it. */
    val ContentClearance: Dp = Height + BottomOffset + 8.dp
}

@Composable
fun RonaFloatingNavDock(
    selected: RonaDockDestination,
    onDestinationSelected: (RonaDockDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRonaColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                horizontal = RonaDockTokens.HorizontalMargin,
                vertical = RonaDockTokens.BottomOffset,
            ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 340.dp),
            shape = RonaPillShape,
            color = colors.dockSurface,
            contentColor = colors.dockContent,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, colors.dockBorder),
        ) {
            DockPillContent(
                selected = selected,
                onDestinationSelected = onDestinationSelected,
            )
        }
    }
}

/**
 * Inner pill: three equal icon targets + one moving active capsule.
 */
@Composable
private fun DockPillContent(
    selected: RonaDockDestination,
    onDestinationSelected: (RonaDockDestination) -> Unit,
) {
    val colors = LocalRonaColors.current
    val density = LocalDensity.current
    val destinations = RonaDockDestination.entries

    // Measured geometry (px) of each item, relative to the pill's Row.
    val itemX = remember { mutableStateMapOf<RonaDockDestination, Int>() }
    val itemWidth = remember { mutableStateMapOf<RonaDockDestination, Int>() }

    val defaultWidthPx = with(density) { 56.dp.roundToPx() }
    val spacingPx = with(density) { RonaDockTokens.ItemSpacing.roundToPx() }

    fun xOf(d: RonaDockDestination): Int =
        itemX[d] ?: (destinations.indexOf(d) * (defaultWidthPx + spacingPx))
    fun widthOf(d: RonaDockDestination): Int = itemWidth[d] ?: defaultWidthPx

    val indicatorOffset by animateDpAsState(
        targetValue = with(density) { xOf(selected).toDp() },
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "dockIndicatorOffset",
    )
    val indicatorWidth by animateDpAsState(
        targetValue = with(density) { widthOf(selected).toDp() },
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "dockIndicatorWidth",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(RonaDockTokens.Height),
        contentAlignment = Alignment.Center,
    ) {
        // ————— Single moving active capsule —————
        if (indicatorWidth > 0.dp) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = indicatorOffset)
                    .width(indicatorWidth)
                    .height(RonaDockTokens.MinTouchTarget),
                shape = RonaPillShape,
                color = colors.dockSelectedContainer,
            ) {}
        }

        // ————— Icon targets —————
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(RonaDockTokens.ItemSpacing),
        ) {
            destinations.forEach { destination ->
                DockIconItem(
                    destination = destination,
                    selected = destination == selected,
                    onClick = { onDestinationSelected(destination) },
                    onMeasured = { x, w ->
                        itemX[destination] = x
                        itemWidth[destination] = w
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DockIconItem(
    destination: RonaDockDestination,
    selected: Boolean,
    onClick: () -> Unit,
    onMeasured: (x: Int, width: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRonaColors.current
    val tooltipState = rememberTooltipState()

    val iconColor by animateColorAsState(
        targetValue = if (selected) colors.dockContent else colors.dockInactiveContent,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "dockIconColor",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.94f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "dockIconScale",
    )

    // Outer Surface carries the click + semantics; TooltipBox wraps only the
    // visual icon so semantics stay merged and TalkBack/long-press both work.
    Surface(
        onClick = onClick,
        shape = RonaPillShape,
        color = Color.Transparent,
        contentColor = iconColor,
        modifier = modifier
            .height(RonaDockTokens.MinTouchTarget)
            .onGloballyPositioned { coords ->
                onMeasured(coords.positionInWindow().x.roundToInt(), coords.size.width)
            }
            .semantics {
                this.selected = selected
                this.role = Role.Tab
                contentDescription = if (selected) {
                    "${destination.label}, tab dipilih"
                } else {
                    destination.label
                }
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = {
                    Surface(
                        shape = RonaPillShape,
                        color = colors.dockSurface,
                        tonalElevation = 4.dp,
                        shadowElevation = 8.dp,
                    ) {
                        Text(
                            text = destination.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.dockContent,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                },
                state = rememberTooltipState(),
            ) {
                Icon(
                    imageVector = destination.icon(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .scale(iconScale),
                )
            }
        }
    }
}

private fun RonaDockDestination.icon(): ImageVector = when (this) {
    RonaDockDestination.HOME -> Icons.Rounded.Home
    RonaDockDestination.CALENDAR -> Icons.Rounded.CalendarMonth
    RonaDockDestination.INSIGHTS -> Icons.Rounded.AutoGraph
}

// ───────────────────────── Previews ─────────────────────────

@Preview(name = "Dock — Beranda selected, light", showBackground = true, widthDp = 390)
@Composable
private fun DockBerandaLightPreview() {
    RonaTheme {
        RonaFloatingNavDock(
            selected = RonaDockDestination.HOME,
            onDestinationSelected = {},
        )
    }
}

@Preview(name = "Dock — Kalender selected, light", showBackground = true, widthDp = 390)
@Composable
private fun DockKalenderLightPreview() {
    RonaTheme {
        RonaFloatingNavDock(
            selected = RonaDockDestination.CALENDAR,
            onDestinationSelected = {},
        )
    }
}

@Preview(name = "Dock — Insight selected, light", showBackground = true, widthDp = 390)
@Composable
private fun DockInsightLightPreview() {
    RonaTheme {
        RonaFloatingNavDock(
            selected = RonaDockDestination.INSIGHTS,
            onDestinationSelected = {},
        )
    }
}

@Preview(
    name = "Dock — Beranda selected, dark",
    showBackground = true,
    widthDp = 390,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun DockBerandaDarkPreview() {
    RonaTheme(themeMode = ThemeMode.DARK) {
        RonaFloatingNavDock(
            selected = RonaDockDestination.HOME,
            onDestinationSelected = {},
        )
    }
}

@Preview(name = "Dock — narrow 320dp", showBackground = true, widthDp = 320)
@Composable
private fun DockNarrowPreview() {
    RonaTheme {
        RonaFloatingNavDock(
            selected = RonaDockDestination.HOME,
            onDestinationSelected = {},
        )
    }
}

@Preview(
    name = "Dock — large font scale",
    showBackground = true,
    widthDp = 390,
    fontScale = 1.5f,
)
@Composable
private fun DockLargeFontPreview() {
    RonaTheme {
        RonaFloatingNavDock(
            selected = RonaDockDestination.HOME,
            onDestinationSelected = {},
        )
    }
}
