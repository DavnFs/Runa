package id.rona.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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

    // 1. Beranda icon navigates to Beranda.
    @Test
    fun berandaIconInvokesDestination() {
        var clicked: RonaDockDestination? = null
        composeRule.setContent {
            RonaTheme {
                RonaFloatingNavDock(
                    selected = RonaDockDestination.HOME,
                    onDestinationSelected = { clicked = it },
                )
            }
        }
        // When selected, the contentDescription becomes "Beranda, tab dipilih".
        composeRule.onNodeWithContentDescription("Beranda, tab dipilih")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.runOnIdle { assert(clicked == RonaDockDestination.HOME) }
    }

    // 2. Kalender icon navigates to Kalender.
    @Test
    fun kalenderIconInvokesDestination() {
        var clicked: RonaDockDestination? = null
        composeRule.setContent {
            RonaTheme {
                RonaFloatingNavDock(
                    selected = RonaDockDestination.CALENDAR,
                    onDestinationSelected = { clicked = it },
                )
            }
        }
        composeRule.onNodeWithContentDescription("Kalender, tab dipilih")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.runOnIdle { assert(clicked == RonaDockDestination.CALENDAR) }
    }

    // 3. Insight icon navigates to Insight (unselected → plain label).
    @Test
    fun insightIconInvokesDestination() {
        var clicked: RonaDockDestination? = null
        composeRule.setContent {
            RonaTheme {
                RonaFloatingNavDock(
                    selected = RonaDockDestination.HOME,
                    onDestinationSelected = { clicked = it },
                )
            }
        }
        composeRule.onNodeWithContentDescription("Insight").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.runOnIdle { assert(clicked == RonaDockDestination.INSIGHTS) }
    }

    // 4. Selected semantic state is correct.
    @Test
    fun selectedSemanticsExposed() {
        composeRule.setContent {
            RonaTheme {
                RonaFloatingNavDock(
                    selected = RonaDockDestination.HOME,
                    onDestinationSelected = {},
                )
            }
        }
        composeRule.onNodeWithContentDescription("Beranda, tab dipilih").assertIsSelected()
    }

    // 5. Insight selected semantic state.
    @Test
    fun insightSelectedSemanticsExposed() {
        composeRule.setContent {
            RonaTheme {
                RonaFloatingNavDock(
                    selected = RonaDockDestination.INSIGHTS,
                    onDestinationSelected = {},
                )
            }
        }
        composeRule.onNodeWithContentDescription("Insight, tab dipilih").assertIsSelected()
    }

    // 6. Each item exposes expected contentDescription + click action.
    @Test
    fun allItemsExposeContentDescriptionAndClick() {
        composeRule.setContent {
            RonaTheme {
                RonaFloatingNavDock(
                    selected = RonaDockDestination.HOME,
                    onDestinationSelected = {},
                )
            }
        }
        composeRule.onNodeWithContentDescription("Beranda, tab dipilih").assertHasClickAction()
        composeRule.onNodeWithContentDescription("Kalender").assertHasClickAction()
        composeRule.onNodeWithContentDescription("Insight").assertHasClickAction()
    }

    // 7. No overflow at 320dp and 360dp.
    @Test
    fun compact320dpRendersWithoutOverflow() {
        composeRule.setContent {
            RonaTheme {
                Box(modifier = Modifier.width(320.dp)) {
                    RonaFloatingNavDock(
                        selected = RonaDockDestination.HOME,
                        onDestinationSelected = {},
                    )
                }
            }
        }
        composeRule.onNodeWithContentDescription("Beranda, tab dipilih").assertExists()
        composeRule.onNodeWithContentDescription("Kalender").assertExists()
        composeRule.onNodeWithContentDescription("Insight").assertExists()
    }

    @Test
    fun width360dpRendersWithoutOverflow() {
        composeRule.setContent {
            RonaTheme {
                Box(modifier = Modifier.width(360.dp)) {
                    RonaFloatingNavDock(
                        selected = RonaDockDestination.CALENDAR,
                        onDestinationSelected = {},
                    )
                }
            }
        }
        composeRule.onNodeWithContentDescription("Kalender, tab dipilih").assertExists()
        composeRule.onNodeWithContentDescription("Insight").assertExists()
    }
}
