package id.rona.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.rona.app.ui.theme.LocalRonaColors
import id.rona.app.ui.theme.RonaMotion
import id.rona.app.ui.theme.RonaPillShape
import id.rona.app.ui.theme.RonaTheme

/**
 * Rona floating pill navigation dock.
 *
 * Layout:
 *   ┌──────────────────────────┐   ┌──────┐
 *   │ Beranda  Kalender  Catat │   │  ◌   │
 *   └──────────────────────────┘   └──────┘
 *                                       Insight
 *
 * - Beranda & Kalender: destinations.
 * - Catat: primary quick action (opens the daily log editor — NOT a nav
 *   destination).
 * - Insight: detached circular companion destination.
 *
 * Dock surface is a dark ink/plum capsule in BOTH themes (strong identity,
 * close to the reference); in dark theme it lifts slightly above the canvas.
 */
enum class RonaDockDestination(val label: String) {
    HOME("Beranda"),
    CALENDAR("Kalender"),
    INSIGHTS("Insight"),
}

@Composable
fun RonaFloatingNavDock(
    selected: RonaDockDestination,
    onDestinationSelected: (RonaDockDestination) -> Unit,
    onLogAction: () -> Unit,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true,
) {
    val colors = LocalRonaColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ————— Main pill —————
        Surface(
            modifier = Modifier
                .widthIn(min = 0.dp, max = 340.dp)
                .weight(1f, fill = true),
            shape = RonaPillShape,
            color = colors.dockSurface,
            contentColor = colors.dockContent,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, colors.dockBorder),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DockDestinationItem(
                    icon = Icons.Rounded.Home,
                    label = RonaDockDestination.HOME.label,
                    selected = selected == RonaDockDestination.HOME,
                    showLabel = showLabels,
                    onClick = { onDestinationSelected(RonaDockDestination.HOME) },
                )
                DockDestinationItem(
                    icon = Icons.Rounded.CalendarMonth,
                    label = RonaDockDestination.CALENDAR.label,
                    selected = selected == RonaDockDestination.CALENDAR,
                    showLabel = showLabels,
                    onClick = { onDestinationSelected(RonaDockDestination.CALENDAR) },
                )
                DockPrimaryAction(
                    icon = Icons.Rounded.Edit,
                    label = "Catat",
                    onClick = onLogAction,
                )
            }
        }

        // ————— Companion Insight button —————
        CompanionInsightButton(
            icon = Icons.Rounded.QueryStats,
            label = RonaDockDestination.INSIGHTS.label,
            selected = selected == RonaDockDestination.INSIGHTS,
            onClick = { onDestinationSelected(RonaDockDestination.INSIGHTS) },
        )
    }
}

@Composable
private fun DockDestinationItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    showLabel: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalRonaColors.current
    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.dockSelectedContainer else Color.Transparent,
        animationSpec = RonaMotion.Selection,
        label = "dockItemContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.onDockSelectedContainer else colors.dockInactiveContent,
        animationSpec = RonaMotion.Selection,
        label = "dockItemContent",
    )

    Surface(
        onClick = onClick,
        shape = RonaPillShape,
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier
            .height(48.dp)
            .widthIn(min = 64.dp)
            .semantics {
                this.selected = selected
                this.role = Role.Tab
                contentDescription = if (selected) "$label, tab dipilih" else label
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(if (selected) 22.dp else 24.dp),
            )
            if (showLabel) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun DockPrimaryAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val colors = LocalRonaColors.current
    Surface(
        onClick = onClick,
        shape = RonaPillShape,
        color = colors.cyclePrimary,
        contentColor = colors.onCyclePrimary,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        modifier = Modifier
            .height(48.dp)
            .widthIn(min = 88.dp)
            .semantics {
                role = Role.Button
                contentDescription = "Catat keadaanmu hari ini"
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
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
        animationSpec = RonaMotion.Selection,
        label = "insightContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.onDockSelectedContainer else colors.dockContent,
        animationSpec = RonaMotion.Selection,
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
            .size(60.dp)
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
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
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

@Preview(name = "Dock — narrow width", showBackground = true, widthDp = 320)
@Composable
private fun DockNarrowPreview() {
    RonaTheme {
        RonaFloatingNavDock(
            selected = RonaDockDestination.HOME,
            onDestinationSelected = {},
            onLogAction = {},
            showLabels = false,
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
