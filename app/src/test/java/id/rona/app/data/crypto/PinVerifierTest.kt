package id.rona.app.data.crypto

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PinVerifierTest {

    private val verifier = PinVerifier()

    @Test
    fun hashAndVerifyRoundTrip() {
        val salt = verifier.generateSalt()
        val hash = verifier.hashPin("1234", salt)
        assertThat(hash).isNotEqualTo("1234")
        assertThat(verifier.verify("1234", hash, salt, PinVerifier.MIN_ITERATIONS)).isTrue()
    }

    @Test
    fun wrongPinFails() {
        val salt = verifier.generateSalt()
        val hash = verifier.hashPin("1234", salt)
        assertThat(verifier.verify("0000", hash, salt, PinVerifier.MIN_ITERATIONS)).isFalse()
        assertThat(verifier.verify("12345", hash, salt, PinVerifier.MIN_ITERATIONS)).isFalse()
    }

    @Test
    fun saltIsUniquePerCall() {
        val saltA = verifier.generateSalt()
        val saltB = verifier.generateSalt()
        assertThat(saltA).isNotEqualTo(saltB)
    }

    @Test
    fun samePinDifferentSaltProducesDifferentHash() {
        val hashA = verifier.hashPin("567890", verifier.generateSalt())
        val hashB = verifier.hashPin("567890", verifier.generateSalt())
        assertThat(hashA).isNotEqualTo(hashB)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPinShorterThanMinimum() {
        verifier.hashPin("12", verifier.generateSalt())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPinLongerThanMaximum() {
        verifier.hashPin("1234567", verifier.generateSalt())
    }

    @Test
    fun iterationsAreClampedUpward() {
        val salt = verifier.generateSalt()
        val hash = verifier.hashPin("1234", salt, iterations = 1)
        assertThat(verifier.verify("1234", hash, salt, iterations = PinVerifier.MIN_ITERATIONS)).isTrue()
    }
}
