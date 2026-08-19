package id.rona.app.ui.components

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
class RonaTopBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun displaysRunaWordmark() {
        composeRule.setContent {
            RonaTheme {
                RonaTopBar(
                    subtitle = "Senin, 17 Agustus",
                    onOpenSettings = {},
                )
            }
        }
        composeRule.onNodeWithText("RUNA").assertIsDisplayed()
        composeRule.onNodeWithText("Senin, 17 Agustus").assertIsDisplayed()
    }

    @Test
    fun settingsButtonTriggersCallback() {
        var settingsClicked = false
        composeRule.setContent {
            RonaTheme {
                RonaTopBar(
                    onOpenSettings = { settingsClicked = true },
                )
            }
        }
        composeRule.onNodeWithContentDescription("Pengaturan")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.runOnIdle {
            assert(settingsClicked)
        }
    }
}
