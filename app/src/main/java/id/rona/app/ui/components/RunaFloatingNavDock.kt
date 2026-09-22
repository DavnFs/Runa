package id.rona.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
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
import id.rona.app.ui.theme.RunaPillShape
import id.rona.app.ui.theme.RunaTheme

/**
 * Runa floating pill navigation dock — Glassmorphic iOS-style, icon-only, 3 destinations.
 *
 * Visual layout:
 * - Translucent glass surface (bright & luminous in light mode, deep & frosted in dark mode).
 * - Specular border highlight simulating iOS glass reflection.
 * - Active pill indicator moving between icons (position + width), 200ms FastOutSlowIn.
 */
enum class RunaDockDestination(val label: String) {
    HOME("Beranda"),
    CALENDAR("Kalender"),
    INSIGHTS("Insight"),
}

/** Dock dimension tokens (semantic, single source of truth). */
object RunaDockTokens {
    val Height = 60.dp
    val CornerRadius = 30.dp
    val HorizontalMargin = 20.dp
    val BottomOffset = 12.dp
    val MinTouchTarget = 46.dp
    val ItemSpacing = 4.dp

    /** Clearance reserved below page content so the dock never covers it. */
    val ContentClearance: Dp = Height + BottomOffset + 8.dp
}

@Composable
fun RunaFloatingNavDock(
    selected: RunaDockDestination,
    onDestinationSelected: (RunaDockDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRonaColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                horizontal = RunaDockTokens.HorizontalMargin,
                vertical = RunaDockTokens.BottomOffset,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Outer glassmorphic pill container
        Surface(
            modifier = Modifier
                .width(236.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RunaPillShape,
                    ambientColor = colors.inkPrimary.copy(alpha = 0.12f),
                    spotColor = colors.inkPrimary.copy(alpha = 0.18f),
                ),
            shape = RunaPillShape,
            color = Color.Transparent,
            contentColor = colors.dockContent,
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0x99FFFFFF),
                        colors.dockBorder,
                        Color(0x22000000),
                    )
                ),
            ),
        ) {
            Box {
                // Liquid glass: real backdrop blur through Haze where available.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .runaGlass(
                            shape = RunaPillShape,
                            backgroundColor = colors.dockSurface,
                            blurRadius = 24.dp,
                        ),
                )
                DockPillContent(
                    selected = selected,
                    onDestinationSelected = onDestinationSelected,
                )
            }
        }
    }
}

/**
 * Inner pill: three equal icon targets + one moving active capsule.
 */
@Composable
private fun DockPillContent(
    selected: RunaDockDestination,
    onDestinationSelected: (RunaDockDestination) -> Unit,
) {
    val colors = LocalRonaColors.current
    val density = LocalDensity.current
    val destinations = RunaDockDestination.entries

    // Measured geometry (px) of each item, relative to the pill's Row.
    val itemX = remember { mutableStateMapOf<RunaDockDestination, Int>() }
    val itemWidth = remember { mutableStateMapOf<RunaDockDestination, Int>() }

    val defaultWidthPx = with(density) { 56.dp.roundToPx() }
    val spacingPx = with(density) { RunaDockTokens.ItemSpacing.roundToPx() }

    fun xOf(d: RunaDockDestination): Int =
        itemX[d] ?: (destinations.indexOf(d) * (defaultWidthPx + spacingPx))
    fun widthOf(d: RunaDockDestination): Int = itemWidth[d] ?: defaultWidthPx

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
            .height(RunaDockTokens.Height)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        // ————— Single moving active capsule —————
        if (indicatorWidth > 0.dp) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = indicatorOffset)
                    .width(indicatorWidth)
                    .height(RunaDockTokens.MinTouchTarget)
                    .shadow(
                        elevation = 4.dp,
                        shape = RunaPillShape,
                        spotColor = colors.dockSelectedContainer.copy(alpha = 0.4f),
                    ),
                shape = RunaPillShape,
                color = colors.dockSelectedContainer,
            ) {}
        }

        // ————— Icon targets —————
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(RunaDockTokens.ItemSpacing),
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
    destination: RunaDockDestination,
    selected: Boolean,
    onClick: () -> Unit,
    onMeasured: (x: Int, width: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRonaColors.current

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

    Surface(
        onClick = onClick,
        shape = RunaPillShape,
        color = Color.Transparent,
        contentColor = iconColor,
        modifier = modifier
            .height(RunaDockTokens.MinTouchTarget)
            .onGloballyPositioned { coords ->
                onMeasured(coords.positionInParent().x.roundToInt(), coords.size.width)
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
                        shape = RunaPillShape,
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

private fun RunaDockDestination.icon(): ImageVector = when (this) {
    RunaDockDestination.HOME -> Icons.Rounded.Home
    RunaDockDestination.CALENDAR -> Icons.Rounded.CalendarMonth
    RunaDockDestination.INSIGHTS -> Icons.Rounded.BarChart
}

// ───────────────────────── Previews ─────────────────────────

@Preview(name = "Dock — Beranda selected, light", showBackground = true, widthDp = 390)
@Composable
private fun DockBerandaLightPreview() {
    RunaTheme {
        RunaFloatingNavDock(
            selected = RunaDockDestination.HOME,
            onDestinationSelected = {},
        )
    }
}

@Preview(name = "Dock — Kalender selected, light", showBackground = true, widthDp = 390)
@Composable
private fun DockKalenderLightPreview() {
    RunaTheme {
        RunaFloatingNavDock(
            selected = RunaDockDestination.CALENDAR,
            onDestinationSelected = {},
        )
    }
}

@Preview(name = "Dock — Insight selected, light", showBackground = true, widthDp = 390)
@Composable
private fun DockInsightLightPreview() {
    RunaTheme {
        RunaFloatingNavDock(
            selected = RunaDockDestination.INSIGHTS,
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
    RunaTheme(themeMode = ThemeMode.DARK) {
        RunaFloatingNavDock(
            selected = RunaDockDestination.HOME,
            onDestinationSelected = {},
        )
    }
}

@Preview(
    name = "Dock — Kalender selected, dark",
    showBackground = true,
    widthDp = 390,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun DockKalenderDarkPreview() {
    RunaTheme(themeMode = ThemeMode.DARK) {
        RunaFloatingNavDock(
            selected = RunaDockDestination.CALENDAR,
            onDestinationSelected = {},
        )
    }
}

@Preview(
    name = "Dock — Insight selected, dark",
    showBackground = true,
    widthDp = 390,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun DockInsightDarkPreview() {
    RunaTheme(themeMode = ThemeMode.DARK) {
        RunaFloatingNavDock(
            selected = RunaDockDestination.INSIGHTS,
            onDestinationSelected = {},
        )
    }
}

@Preview(name = "Dock — narrow 320dp", showBackground = true, widthDp = 320)
@Composable
private fun DockNarrowPreview() {
    RunaTheme {
        RunaFloatingNavDock(
            selected = RunaDockDestination.HOME,
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
    RunaTheme {
        RunaFloatingNavDock(
            selected = RunaDockDestination.HOME,
            onDestinationSelected = {},
        )
    }
}
