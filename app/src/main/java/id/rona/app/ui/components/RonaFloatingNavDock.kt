package id.rona.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.rona.app.ui.theme.LocalRonaColors
import id.rona.app.ui.theme.RonaPillShape
import id.rona.app.ui.theme.RonaTheme

/**
 * Rona floating pill navigation dock — Google Photos-inspired structure with
 * Rona's own ink-plum + dusty-rose identity.
 *
 *   ┌─────────────────────────────┐   ┌──────┐
 *   │  Beranda   Kalender  ✎ Catat│   │  ◌   │
 *   └─────────────────────────────┘   └──────┘
 *                                            Insight
 *
 * - Beranda / Kalender: TEXT-FIRST destinations. A single animated capsule
 *   moves between them (position + width), labels never disappear.
 * - Catat: integrated primary rose action (pencil + label), opens the log
 *   editor — not a navigation destination.
 * - Insight: detached circular companion with a persistent small label.
 *
 * Motion: 200ms FastOutSlowIn, no bounce, no layout jump, navigation never
 * waits for the animation.
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
    val CompanionSize = 60.dp
    val CompanionGap = 12.dp
    val HorizontalMargin = 16.dp
    val BottomOffset = 12.dp
    val MinTouchTarget = 48.dp

    /** Clearance reserved below page content so the dock never covers it. */
    val ContentClearance: Dp = Height + BottomOffset + 8.dp
}

@Composable
fun RonaFloatingNavDock(
    selected: RonaDockDestination,
    onDestinationSelected: (RonaDockDestination) -> Unit,
    onLogAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRonaColors.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = RonaDockTokens.HorizontalMargin, vertical = RonaDockTokens.BottomOffset),
    ) {
        val compact = maxWidth < 360.dp

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RonaDockTokens.CompanionGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ————— Main pill —————
            Surface(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .widthIn(max = 340.dp),
                shape = RonaPillShape,
                color = colors.dockSurface,
                contentColor = colors.dockContent,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, colors.dockBorder),
            ) {
                MainPillContent(
                    selected = selected,
                    onDestinationSelected = onDestinationSelected,
                    onLogAction = onLogAction,
                    compact = compact,
                )
            }

            // ————— Companion Insight —————
            CompanionInsightButton(
                icon = Icons.Rounded.QueryStats,
                label = RonaDockDestination.INSIGHTS.label,
                selected = selected == RonaDockDestination.INSIGHTS,
                onClick = { onDestinationSelected(RonaDockDestination.INSIGHTS) },
            )
        }
    }
}

/**
 * Inner pill content: two text-first destinations + primary Catat action.
 * A single animated capsule tracks the selected destination.
 */
@Composable
private fun MainPillContent(
    selected: RonaDockDestination,
    onDestinationSelected: (RonaDockDestination) -> Unit,
    onLogAction: () -> Unit,
    compact: Boolean,
) {
    val colors = LocalRonaColors.current
    val density = LocalDensity.current

    val destinations = listOf(
        RonaDockDestination.HOME,
        RonaDockDestination.CALENDAR,
    )

    // Measured widths (pixels) of each destination item.
    var homeWidthPx by remember { mutableStateOf(0) }
    var calendarWidthPx by remember { mutableStateOf(0) }

    val homeWidth = with(density) { homeWidthPx.toDp() }
    val calendarWidth = with(density) { calendarWidthPx.toDp() }

    val selectedItem = if (selected == RonaDockDestination.INSIGHTS) {
        RonaDockDestination.HOME
    } else {
        selected
    }

    // Indicator offset = width of items before the selected one.
    val indicatorOffset by animateDpAsState(
        targetValue = if (selectedItem == RonaDockDestination.CALENDAR) homeWidth else 0.dp,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "indicatorOffset",
    )
    val indicatorWidth by animateDpAsState(
        targetValue = if (selectedItem == RonaDockDestination.CALENDAR) calendarWidth else homeWidth,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "indicatorWidth",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(RonaDockTokens.Height)
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        // ————— Animated active capsule —————
        if (indicatorWidth > 0.dp) {
            Surface(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(indicatorWidth)
                    .height(RonaDockTokens.Height - 16.dp),
                shape = RonaPillShape,
                color = colors.dockSelectedContainer,
            ) {}
        }

        // ————— Items on top —————
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DockTextItem(
                label = RonaDockDestination.HOME.label,
                selected = selectedItem == RonaDockDestination.HOME,
                compact = compact,
                onClick = { onDestinationSelected(RonaDockDestination.HOME) },
                modifier = Modifier
                    .weight(1f)
                    .onSizeChanged { homeWidthPx = it.width },
            )
            DockTextItem(
                label = RonaDockDestination.CALENDAR.label,
                selected = selectedItem == RonaDockDestination.CALENDAR,
                compact = compact,
                onClick = { onDestinationSelected(RonaDockDestination.CALENDAR) },
                modifier = Modifier
                    .weight(1f)
                    .onSizeChanged { calendarWidthPx = it.width },
            )
            DockPrimaryAction(
                icon = Icons.Rounded.Edit,
                label = "Catat",
                onClick = onLogAction,
                modifier = Modifier.weight(1.4f),
            )
        }
    }
}

@Composable
private fun DockTextItem(
    label: String,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRonaColors.current
    val textColor by animateColorAsState(
        targetValue = if (selected) colors.onDockSelectedContainer else colors.dockInactiveContent,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "dockTextColor",
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.72f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "dockLabelAlpha",
    )

    Surface(
        onClick = onClick,
        shape = RonaPillShape,
        color = Color.Transparent,
        contentColor = textColor,
        modifier = modifier
            .height(RonaDockTokens.Height - 16.dp)
            .semantics {
                this.selected = selected
                this.role = Role.Tab
                contentDescription = if (selected) "$label, tab dipilih" else label
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            // Compact mode: icon + label; normal mode: text-first (no icon).
            if (compact) {
                Icon(
                    imageVector = if (label == "Beranda") Icons.Rounded.Home else Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                modifier = Modifier.alpha(labelAlpha),
            )
        }
    }
}

@Composable
private fun DockPrimaryAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRonaColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Gentle press feedback ~120ms, no bounce.
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(120),
        label = "catatPress",
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RonaPillShape,
        color = colors.cyclePrimary,
        contentColor = colors.onCyclePrimary,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        modifier = modifier
            .height(RonaDockTokens.Height - 16.dp)
            .scale(pressScale)
            .semantics {
                role = Role.Button
                contentDescription = "Catat keadaanmu hari ini"
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CompanionInsightButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalRonaColors.current
    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.dockSelectedContainer else colors.dockSurface,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "insightContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.onDockSelectedContainer else colors.dockContent,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "insightContent",
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, colors.dockBorder),
        modifier = Modifier
            .size(RonaDockTokens.CompanionSize)
            .semantics {
                this.selected = selected
                this.role = Role.Tab
                contentDescription = if (selected) "$label, tab dipilih" else label
            },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                maxLines = 1,
            )
        }
    }
}

// ───────────────────────── Previews ─────────────────────────

@Preview(name = "Dock — Beranda selected", showBackground = true, widthDp = 390)
@Composable
private fun DockBerandaPreview() {
    RonaTheme {
        RonaFloatingNavDock(
            selected = RonaDockDestination.HOME,
            onDestinationSelected = {},
            onLogAction = {},
        )
    }
}

@Preview(name = "Dock — Kalender selected", showBackground = true, widthDp = 390)
@Composable
private fun DockKalenderPreview() {
    RonaTheme {
        RonaFloatingNavDock(
            selected = RonaDockDestination.CALENDAR,
            onDestinationSelected = {},
            onLogAction = {},
        )
    }
}

@Preview(name = "Dock — Insight selected", showBackground = true, widthDp = 390)
@Composable
private fun DockInsightPreview() {
    RonaTheme {
        RonaFloatingNavDock(
            selected = RonaDockDestination.INSIGHTS,
            onDestinationSelected = {},
            onLogAction = {},
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
            onLogAction = {},
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
            onLogAction = {},
        )
    }
}

@Preview(
    name = "Dock — narrow 320dp dark",
    showBackground = true,
    widthDp = 320,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun DockNarrowDarkPreview() {
    RonaTheme(themeMode = id.rona.app.domain.model.ThemeMode.DARK) {
        RonaFloatingNavDock(
            selected = RonaDockDestination.CALENDAR,
            onDestinationSelected = {},
            onLogAction = {},
        )
    }
}
