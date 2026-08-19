package id.rona.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import id.rona.app.ui.theme.RonaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class RonaButtonsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun primaryButtonDisplaysTextAndInvokesClick() {
        var clicked = false
        composeRule.setContent {
            RonaTheme {
                RonaPrimaryButton(
                    text = "Catat keadaanmu hari ini",
                    icon = Icons.Rounded.Edit,
                    onClick = { clicked = true },
                )
            }
        }
        composeRule.onNodeWithText("Catat keadaanmu hari ini")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performSemanticsAction(SemanticsActions.OnClick)

        composeRule.runOnIdle {
            assert(clicked)
        }
    }

    @Test
    fun secondaryButtonDisplaysTextAndInvokesClick() {
        var clicked = false
        composeRule.setContent {
            RonaTheme {
                RonaSecondaryButton(
                    text = "Batal",
                    onClick = { clicked = true },
                )
            }
        }
        composeRule.onNodeWithText("Batal")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performSemanticsAction(SemanticsActions.OnClick)

        composeRule.runOnIdle {
            assert(clicked)
        }
    }
}
