package id.rona.app.data.crypto

import android.security.keystore.StrongBoxUnavailableException
import com.google.common.truth.Truth.assertThat
import java.security.GeneralSecurityException
import java.security.KeyStoreException
import javax.crypto.SecretKey
import org.junit.Test

class StrongBoxKeyPolicyTest {

    // ————— Fakes —————

    private class FakeKey(val marker: String) : SecretKey {
        override fun getAlgorithm(): String = "AES"
        override fun getFormat(): String? = null
        override fun getEncoded(): ByteArray? = null
    }

    private class FakeCapability(private val supported: Boolean) : StrongBoxCapability {
        override fun isStrongBoxSupported(): Boolean = supported
    }

    private class RecordingKeyFactory : KeystoreKeyFactory {
        val calls = mutableListOf<Boolean>()
        var failStrongBox: Boolean = false
        var unexpectedFailure: Exception? = null

        override fun generateKey(useStrongBox: Boolean): SecretKey {
            calls += useStrongBox
            unexpectedFailure?.let { throw it }
            if (useStrongBox && failStrongBox) {
                throw StrongBoxUnavailableException()
            }
            return FakeKey(if (useStrongBox) "strongbox" else "standard")
        }
    }

    private fun policy(
        capability: StrongBoxCapability,
        factory: KeystoreKeyFactory,
        onFallback: () -> Unit = {},
        apiLevel: Int = 30,
    ) = StrongBoxKeyPolicy(capability, factory, onFallback, apiLevel)

    // ————— Tests —————

    @Test
    fun apiBelow28UsesStandardKeystore() {
        // API 26: capability false (PackageManager already gates on version
        // in production) AND the policy's own apiLevel guard is 26.
        val factory = RecordingKeyFactory()
        val key = policy(FakeCapability(supported = false), factory, apiLevel = 26).createKey()
        assertThat((key as FakeKey).marker).isEqualTo("standard")
        assertThat(factory.calls).containsExactly(false)
    }

    @Test
    fun apiBelow28EvenIfCapabilityMisreportsUsesStandardKeystore() {
        val factory = RecordingKeyFactory()
        val key = policy(FakeCapability(supported = true), factory, apiLevel = 26).createKey()
        assertThat((key as FakeKey).marker).isEqualTo("standard")
        assertThat(factory.calls).containsExactly(false)
    }

    @Test
    fun featureAbsentUsesStandardKeystore() {
        val factory = RecordingKeyFactory()
        val key = policy(FakeCapability(supported = false), factory).createKey()
        assertThat((key as FakeKey).marker).isEqualTo("standard")
        assertThat(factory.calls).containsExactly(false)
    }

    @Test
    fun featurePresentAndSucceedsUsesStrongBox() {
        val factory = RecordingKeyFactory()
        val key = policy(FakeCapability(supported = true), factory).createKey()
        assertThat((key as FakeKey).marker).isEqualTo("strongbox")
        assertThat(factory.calls).containsExactly(true)
    }

    @Test
    fun strongBoxUnavailableFallsBackToStandard() {
        val factory = RecordingKeyFactory().apply { failStrongBox = true }
        var fallbackLogged = false
        val key = policy(FakeCapability(supported = true), factory, onFallback = { fallbackLogged = true })
            .createKey()
        assertThat((key as FakeKey).marker).isEqualTo("standard")
        assertThat(factory.calls).containsExactly(true, false).inOrder()
        assertThat(fallbackLogged).isTrue()
    }

    @Test
    fun unexpectedKeystoreExceptionDoesNotSilentlyDowngrade() {
        val factory = RecordingKeyFactory().apply { unexpectedFailure = KeyStoreException("boom") }
        val thrown = runCatching {
            policy(FakeCapability(supported = true), factory).createKey()
        }.exceptionOrNull()
        assertThat(thrown).isInstanceOf(KeyStoreException::class.java)
        assertThat(factory.calls).containsExactly(true)
    }

    @Test
    fun unexpectedExceptionOnStandardPathAlsoPropagates() {
        val factory = RecordingKeyFactory().apply { unexpectedFailure = GeneralSecurityException("boom") }
        val thrown = runCatching {
            policy(FakeCapability(supported = false), factory).createKey()
        }.exceptionOrNull()
        assertThat(thrown).isInstanceOf(GeneralSecurityException::class.java)
    }

    @Test
    fun fallbackFailurePropagatesInsteadOfLooping() {
        val factory = object : KeystoreKeyFactory {
            var count = 0
            override fun generateKey(useStrongBox: Boolean): SecretKey {
                count++
                throw StrongBoxUnavailableException()
            }
        }
        val thrown = runCatching {
            policy(FakeCapability(supported = true), factory).createKey()
        }.exceptionOrNull()
        assertThat(thrown).isInstanceOf(StrongBoxUnavailableException::class.java)
        assertThat(factory.count).isEqualTo(2) // exactly one retry, then propagate
    }

    @Test
    fun existingKeyReuseIsPolicyIndependent() {
        // The reuse contract lives in CryptoManager.getOrCreateKey: existing
        // key is loaded before any generation. The policy itself only handles
        // creation; this test documents that a successful policy call returns
        // a key without a second generation attempt.
        val factory = RecordingKeyFactory()
        policy(FakeCapability(supported = true), factory).createKey()
        assertThat(factory.calls).hasSize(1)
    }
}
