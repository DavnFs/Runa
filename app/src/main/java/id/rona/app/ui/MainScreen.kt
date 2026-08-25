package id.rona.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.time.LocalDate
import kotlin.math.roundToInt
import id.rona.app.ui.calendar.CalendarScreen
import id.rona.app.ui.components.RonaDockDestination
import id.rona.app.ui.components.RonaFloatingNavDock
import id.rona.app.ui.home.HomeScreen
import id.rona.app.ui.insights.InsightsScreen
import id.rona.app.ui.theme.RonaMotion

@Composable
fun MainScreen(
    onLogToday: () -> Unit,
    onLogDate: (LocalDate) -> Unit,
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

        // Apple-style fluid cross-slide tab transitions
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                if (forward) {
                    (slideInHorizontally(
                        initialOffsetX = { (it * 0.18f).roundToInt() },
                        animationSpec = RonaMotion.appleSpring(),
                    ) + fadeIn(
                        animationSpec = tween(280, easing = RonaMotion.AppleEaseOut),
                    )).togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { (-it * 0.18f).roundToInt() },
                            animationSpec = RonaMotion.appleSpring(),
                        ) + fadeOut(
                            animationSpec = tween(200, easing = RonaMotion.AppleEaseIn),
                        )
                    )
                } else {
                    (slideInHorizontally(
                        initialOffsetX = { (-it * 0.18f).roundToInt() },
                        animationSpec = RonaMotion.appleSpring(),
                    ) + fadeIn(
                        animationSpec = tween(280, easing = RonaMotion.AppleEaseOut),
                    )).togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { (it * 0.18f).roundToInt() },
                            animationSpec = RonaMotion.appleSpring(),
                        ) + fadeOut(
                            animationSpec = tween(200, easing = RonaMotion.AppleEaseIn),
                        )
                    )
                }
            },
            label = "mainTabTransition",
        ) { targetDestination ->
            when (targetDestination) {
                RonaDockDestination.HOME -> HomeScreen(
                    onLogToday = onLogToday,
                    onOpenCalendar = { selectedTab = RonaDockDestination.CALENDAR },
                    onOpenInsights = { selectedTab = RonaDockDestination.INSIGHTS },
                    onOpenSettings = onOpenSettings,
                    modifier = contentModifier,
                )
                RonaDockDestination.CALENDAR -> CalendarScreen(
                    onOpenSettings = onOpenSettings,
                    onLogDate = onLogDate,
                    modifier = contentModifier,
                )
                RonaDockDestination.INSIGHTS -> InsightsScreen(
                    onOpenSettings = onOpenSettings,
                    modifier = contentModifier,
                )
            }
        }
    }
}
