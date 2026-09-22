package id.rona.app.ui.components

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
class RunaTopBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun displaysRunaLogoMark() {
        composeRule.setContent {
            RunaTheme {
                RunaTopBar(
                    subtitle = "Senin, 17 Agustus",
                    onOpenSettings = {},
                )
            }
        }
        // The bar carries the brand mark, not a text wordmark.
        composeRule.onNodeWithContentDescription("Runa").assertIsDisplayed()
        composeRule.onNodeWithText("Senin, 17 Agustus").assertIsDisplayed()
    }

    @Test
    fun settingsButtonTriggersCallback() {
        var settingsClicked = false
        composeRule.setContent {
            RunaTheme {
                RunaTopBar(
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
