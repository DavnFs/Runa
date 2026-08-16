package id.rona.app.data.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject

/**
 * PBKDF2-HMAC-SHA256 PIN hashing with per-password random salt and constant-time comparison.
 * Hash and salt are stored Base64-encoded in the encrypted DB, never in SharedPreferences.
 */
class PinVerifier @Inject constructor() {

    fun generateSalt(): String {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    fun hashPin(pin: String, saltBase64: String, iterations: Int = MIN_ITERATIONS): String {
        require(pin.length in PIN_MIN_LENGTH..PIN_MAX_LENGTH) {
            "PIN length must be between $PIN_MIN_LENGTH and $PIN_MAX_LENGTH"
        }
        val salt = Base64.getDecoder().decode(saltBase64)
        val iterationsClamped = iterations.coerceAtLeast(MIN_ITERATIONS)
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterationsClamped, KEY_LENGTH_BITS)
        try {
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            val derived = factory.generateSecret(spec).encoded
            return Base64.getEncoder().encodeToString(derived)
        } finally {
            spec.clearPassword()
        }
    }

    /**
     * Compares a PIN attempt against the stored hash in constant time.
     */
    fun verify(pin: String, expectedHashBase64: String, saltBase64: String, iterations: Int): Boolean {
        val candidate = hashPin(pin, saltBase64, iterations)
        val candidateBytes = Base64.getDecoder().decode(candidate)
        val expectedBytes = Base64.getDecoder().decode(expectedHashBase64)
        return MessageDigest.isEqual(candidateBytes, expectedBytes)
    }

    companion object {
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val SALT_BYTES = 16
        private const val KEY_LENGTH_BITS = 256
        const val MIN_ITERATIONS = 120_000
        const val PIN_MIN_LENGTH = 4
        const val PIN_MAX_LENGTH = 6
    }
}
