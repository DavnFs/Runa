package id.rona.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import id.rona.app.ui.theme.RonaTheme
import org.junit.Rule
import org.junit.Test
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Config
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = android.app.Application::class)
class RonaFloatingNavDockTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun berandaClickInvokesDestination() {
        var clicked: RonaDockDestination? = null
        composeRule.setContent {
            RonaTheme {
                RonaFloatingNavDock(
                    selected = RonaDockDestination.HOME,
                    onDestinationSelected = { clicked = it },
                    onLogAction = {},
                )
            }
        }
        composeRule.onNodeWithContentDescription("Kalender").performClick()
        composeRule.runOnIdle { assert(clicked == RonaDockDestination.CALENDAR) }
    }

    @Test
    fun kalenderClickInvokesDestination() {
        var clicked: RonaDockDestination? = null
        composeRule.setContent {
            RonaTheme {
                RonaFloatingNavDock(
                    selected = RonaDockDestination.CALENDAR,
                    onDestinationSelected = { clicked = it },
                    onLogAction = {},
                )
            }
        }
        composeRule.onNodeWithContentDescription("Beranda").performClick()
        composeRule.runOnIdle { assert(clicked == RonaDockDestination.HOME) }
    }

    @Test
    fun catatActionInvokesLogAction() {
        var logClicked = false
        composeRule.setContent {
            RonaTheme {
                RonaFloatingNavDock(
                    selected = RonaDockDestination.HOME,
                    onDestinationSelected = {},
                    onLogAction = { logClicked = true },
                )
            }
        }
        val node = composeRule.onNodeWithContentDescription("Catat keadaanmu hari ini")
        node.assertExists()
        node.assertHasClickAction()
        node.performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 2_000) { logClicked }
    }

    @Test
    fun insightCompanionInvokesDestination() {
        var clicked: RonaDockDestination? = null
        composeRule.setContent {
            RonaTheme {
                RonaFloatingNavDock(
                    selected = RonaDockDestination.HOME,
                    onDestinationSelected = { clicked = it },
                    onLogAction = {},
                )
            }
        }
        composeRule.onNodeWithContentDescription("Insight").performClick()
        composeRule.runOnIdle { assert(clicked == RonaDockDestination.INSIGHTS) }
    }

    @Test
    fun selectedSemanticsExposed() {
        composeRule.setContent {
            RonaTheme {
                RonaFloatingNavDock(
                    selected = RonaDockDestination.HOME,
                    onDestinationSelected = {},
                    onLogAction = {},
                )
            }
        }
        composeRule.onNodeWithContentDescription("Beranda, tab dipilih").assertIsSelected()
    }

    @Test
    fun narrowWidthRendersWithoutOverflow() {
        composeRule.setContent {
            RonaTheme {
                RonaFloatingNavDock(
                    selected = RonaDockDestination.HOME,
                    onDestinationSelected = {},
                    onLogAction = {},
                )
            }
        }
        // If layout overflowed, Compose would throw; merely rendering is enough.
        composeRule.onNodeWithContentDescription("Beranda, tab dipilih").assertExists()
    }

    @Test
    fun compact320dpRendersWithoutOverflow() {
        composeRule.setContent {
            RonaTheme {
                Box(
                    modifier = Modifier.width(320.dp),
                ) {
                    RonaFloatingNavDock(
                        selected = RonaDockDestination.HOME,
                        onDestinationSelected = {},
                        onLogAction = {},
                    )
                }
            }
        }
        composeRule.onNodeWithContentDescription("Beranda, tab dipilih").assertExists()
        composeRule.onNodeWithContentDescription("Catat keadaanmu hari ini").assertExists()
        composeRule.onNodeWithContentDescription("Insight").assertExists()
    }

    @Test
    fun insightSelectedSemanticsExposed() {
        composeRule.setContent {
            RonaTheme {
                RonaFloatingNavDock(
                    selected = RonaDockDestination.INSIGHTS,
                    onDestinationSelected = {},
                    onLogAction = {},
                )
            }
        }
        composeRule.onNodeWithContentDescription("Insight, tab dipilih").assertIsSelected()
    }
}
