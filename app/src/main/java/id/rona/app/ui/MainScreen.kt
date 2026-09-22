package id.rona.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.time.LocalDate
import kotlin.math.roundToInt
import id.rona.app.ui.calendar.CalendarScreen
import id.rona.app.ui.components.LocalRonaHazeState
import id.rona.app.ui.components.RunaDockDestination
import id.rona.app.ui.components.RunaFloatingNavDock
import id.rona.app.ui.components.runaPageContainer
import id.rona.app.ui.home.HomeScreen
import id.rona.app.ui.insights.InsightsScreen
import id.rona.app.ui.theme.RunaMotion

@Composable
fun MainScreen(
    onLogToday: () -> Unit,
    onLogDate: (LocalDate) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(RunaDockDestination.HOME) }
    val hazeState = remember { HazeState() }

    CompositionLocalProvider(LocalRonaHazeState provides hazeState) {
        Scaffold(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
            bottomBar = {
                RunaFloatingNavDock(
                    selected = selectedTab,
                    onDestinationSelected = { selectedTab = it },
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .hazeSource(hazeState),
                contentAlignment = Alignment.TopCenter,
            ) {
                // Apple-style fluid cross-slide tab transitions
                AnimatedContent(
                    modifier = Modifier.fillMaxSize(),
                    targetState = selectedTab,
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal
                        if (forward) {
                            (slideInHorizontally(
                                initialOffsetX = { (it * 0.18f).roundToInt() },
                                animationSpec = RunaMotion.appleSpring(),
                            ) + fadeIn(
                                animationSpec = tween(280, easing = RunaMotion.AppleEaseOut),
                            )).togetherWith(
                                slideOutHorizontally(
                                    targetOffsetX = { (-it * 0.18f).roundToInt() },
                                    animationSpec = RunaMotion.appleSpring(),
                                ) + fadeOut(
                                    animationSpec = tween(200, easing = RunaMotion.AppleEaseIn),
                                )
                            )
                        } else {
                            (slideInHorizontally(
                                initialOffsetX = { (-it * 0.18f).roundToInt() },
                                animationSpec = RunaMotion.appleSpring(),
                            ) + fadeIn(
                                animationSpec = tween(280, easing = RunaMotion.AppleEaseOut),
                            )).togetherWith(
                                slideOutHorizontally(
                                    targetOffsetX = { (it * 0.18f).roundToInt() },
                                    animationSpec = RunaMotion.appleSpring(),
                                ) + fadeOut(
                                    animationSpec = tween(200, easing = RunaMotion.AppleEaseIn),
                                )
                            )
                        }
                    },
                    label = "mainTabTransition",
                ) { targetDestination ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        when (targetDestination) {
                            RunaDockDestination.HOME -> HomeScreen(
                                onLogToday = onLogToday,
                                onOpenCalendar = { selectedTab = RunaDockDestination.CALENDAR },
                                onOpenInsights = { selectedTab = RunaDockDestination.INSIGHTS },
                                onOpenSettings = onOpenSettings,
                                modifier = Modifier.runaPageContainer(),
                            )
                            RunaDockDestination.CALENDAR -> CalendarScreen(
                                onOpenSettings = onOpenSettings,
                                onLogDate = onLogDate,
                                modifier = Modifier.runaPageContainer(),
                            )
                            RunaDockDestination.INSIGHTS -> InsightsScreen(
                                onOpenSettings = onOpenSettings,
                                onLogToday = onLogToday,
                                modifier = Modifier.runaPageContainer(),
                            )
                        }
                    }
                }
            }
        }
    }
}
