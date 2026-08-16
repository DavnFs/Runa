package id.rona.app.security

import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import id.rona.app.ui.MainActivity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecureFlagTest {

    @Test
    fun secureFlagEnabledByDefault() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val hasSecureFlag = activity.window.attributes.flags and
                    WindowManager.LayoutParams.FLAG_SECURE != 0
                assertThat(hasSecureFlag).isTrue()
            }
        }
    }
}
