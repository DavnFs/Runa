package id.rona.app.data.crypto

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StrongBoxCapabilityInstrumentedTest {

    @Test
    fun capabilityReportsConsistentWithPlatformFeature() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val capability = PackageManagerStrongBoxCapability(context)

        val platformReports = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            context.packageManager.hasSystemFeature(
                android.content.pm.PackageManager.FEATURE_STRONGBOX_KEYSTORE
            )
        } else {
            false
        }

        assertThat(capability.isStrongBoxSupported()).isEqualTo(platformReports)
    }

    @Test
    fun capabilityNeverThrows() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val capability = PackageManagerStrongBoxCapability(context)
        val result = runCatching { capability.isStrongBoxSupported() }
        assertThat(result.isSuccess).isTrue()
    }
}
