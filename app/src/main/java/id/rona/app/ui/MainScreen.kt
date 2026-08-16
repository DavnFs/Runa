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
import id.rona.app.ui.components.FloatingPillNavBar
import id.rona.app.ui.components.RonaTab
import id.rona.app.ui.home.HomeScreen
import id.rona.app.ui.insights.InsightsScreen

@Composable
fun MainScreen(
    onLogToday: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(RonaTab.HOME) }

    Scaffold(
        bottomBar = {
            FloatingPillNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onLogAction = onLogToday,
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (selectedTab) {
            RonaTab.HOME -> HomeScreen(
                onLogToday = onLogToday,
                onOpenCalendar = { selectedTab = RonaTab.CALENDAR },
                onOpenSettings = onOpenSettings,
                modifier = contentModifier,
            )
            RonaTab.CALENDAR -> CalendarScreen(modifier = contentModifier)
            RonaTab.INSIGHTS -> InsightsScreen(modifier = contentModifier)
        }
    }
}
