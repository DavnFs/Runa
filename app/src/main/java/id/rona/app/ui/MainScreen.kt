package id.rona.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import id.rona.app.ui.calendar.CalendarScreen
import id.rona.app.ui.components.RonaDockDestination
import id.rona.app.ui.components.RonaFloatingNavDock
import id.rona.app.ui.home.HomeScreen
import id.rona.app.ui.insights.InsightsScreen

@Composable
fun MainScreen(
    onLogToday: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(RonaDockDestination.HOME) }

    Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        bottomBar = {
            RonaFloatingNavDock(
                selected = selectedTab,
                onDestinationSelected = { selectedTab = it },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (selectedTab) {
            RonaDockDestination.HOME -> HomeScreen(
                onLogToday = onLogToday,
                onOpenCalendar = { selectedTab = RonaDockDestination.CALENDAR },
                onOpenSettings = onOpenSettings,
                modifier = contentModifier,
            )
            RonaDockDestination.CALENDAR -> CalendarScreen(modifier = contentModifier)
            RonaDockDestination.INSIGHTS -> InsightsScreen(modifier = contentModifier)
        }
    }
}
