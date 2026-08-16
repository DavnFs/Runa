package id.rona.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

enum class RonaTab(val label: String) {
    HOME("Beranda"),
    CALENDAR("Kalender"),
    INSIGHTS("Insight"),
}

@Composable
fun FloatingPillNavBar(
    selectedTab: RonaTab,
    onTabSelected: (RonaTab) -> Unit,
    onLogAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f),
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
    ) {
        Box(
            modifier = Modifier.widthIn(max = 360.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PillTab(
                    icon = Icons.Rounded.Home,
                    label = RonaTab.HOME.label,
                    selected = selectedTab == RonaTab.HOME,
                    onClick = { onTabSelected(RonaTab.HOME) },
                )
                PillTab(
                    icon = Icons.Rounded.CalendarMonth,
                    label = RonaTab.CALENDAR.label,
                    selected = selectedTab == RonaTab.CALENDAR,
                    onClick = { onTabSelected(RonaTab.CALENDAR) },
                )

                CenterLogAction(onLogAction = onLogAction)

                PillTab(
                    icon = Icons.Rounded.QueryStats,
                    label = RonaTab.INSIGHTS.label,
                    selected = selectedTab == RonaTab.INSIGHTS,
                    onClick = { onTabSelected(RonaTab.INSIGHTS) },
                )
            }
        }
    }
}

@Composable
private fun PillTab(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0f)
        },
        label = "pillTabColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "pillTabContentColor",
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier
            .size(width = 56.dp, height = 48.dp)
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(if (selected) 22.dp else 24.dp),
            )
            if (selected) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun CenterLogAction(onLogAction: () -> Unit) {
    val offsetY by animateDpAsState(
        targetValue = (-12).dp,
        animationSpec = spring(),
        label = "logActionOffset",
    )

    Surface(
        onClick = onLogAction,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 8.dp,
        modifier = Modifier
            .offset(y = offsetY)
            .size(56.dp)
            .semantics {
                role = Role.Button
                contentDescription = "Catat hari ini"
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(28.dp))
        }
    }
}
