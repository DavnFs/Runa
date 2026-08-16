package id.rona.app

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HiltTestRunnerDemo {
    @Test
    fun contextLoads() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assert(context.packageName == "id.rona.app")
    }
}
