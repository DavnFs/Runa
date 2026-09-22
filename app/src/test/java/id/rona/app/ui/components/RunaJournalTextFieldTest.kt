package id.rona.app.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import id.rona.app.ui.theme.RunaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class RunaJournalTextFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun displaysJournalText() {
        composeRule.setContent {
            RunaTheme {
                RunaJournalTextField(
                    value = "Catatan hari ini tenang dan nyaman.",
                    onValueChange = {},
                )
            }
        }
        composeRule.onNodeWithText("Catatan hari ini tenang dan nyaman.")
            .assertIsDisplayed()
    }
}
