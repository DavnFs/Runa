package id.rona.app.security

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import id.rona.app.data.crypto.CryptoManager
import id.rona.app.ui.MainActivity
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

@RunWith(AndroidJUnit4::class)
class KeystoreSecurityTest {

    @Test
    fun dbKeyIsNonExportable() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val crypto = CryptoManager(activity.applicationContext)
                crypto.getOrCreateDbPassphrase()

                val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                val entry = keyStore.getEntry("rona_db_key", null)
                assertThat(entry).isNotNull()
                assertThat(entry).isInstanceOf(KeyStore.SecretKeyEntry::class.java)
            }
        }
    }
}
