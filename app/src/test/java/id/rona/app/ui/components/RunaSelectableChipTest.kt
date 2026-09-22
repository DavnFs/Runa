package id.rona.app.ui.components

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import id.rona.app.ui.theme.RunaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class RunaSelectableChipTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun chipDisplaysLabelAndHandlesSelection() {
        var clicked = false
        composeRule.setContent {
            RunaTheme {
                RunaSelectableChip(
                    label = "Kram perut",
                    selected = true,
                    onClick = { clicked = true },
                )
            }
        }
        composeRule.onNodeWithText("Kram perut")
            .assertIsDisplayed()
            .assertIsSelected()
            .assertHasClickAction()
            .performSemanticsAction(SemanticsActions.OnClick)

        composeRule.runOnIdle {
            assert(clicked)
        }
    }
}
