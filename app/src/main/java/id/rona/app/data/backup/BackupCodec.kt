package id.rona.app.data.backup

import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * File format (docs/PLAN.md §9.2):
 *
 *   RONA1 (magic, 5 bytes)
 *   header JSON (plaintext): formatVersion, kdf {algo, iterations, salt},
 *     cipher {algo, nonce}, payloadLen
 *   ciphertext + GCM tag (authenticated)
 *
 * Passphrase is user-supplied and never persisted. GCM tag guarantees
 * integrity: any single-byte change makes restore fail.
 */
@Singleton
class BackupCodec @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class EncodedHeader(
        val formatVersion: String = FORMAT_VERSION,
        val kdf: KdfHeader,
        val cipher: CipherHeader,
        val payloadLen: Long,
    )

    @Serializable
    data class KdfHeader(
        val algo: String = KDF_ALGO,
        val iterations: Int,
        val salt: String,
    )

    @Serializable
    data class CipherHeader(
        val algo: String = CIPHER_ALGO,
        val nonce: String,
    )

    data class ExportResult(
        val ciphertextSha256: String,
    )

    data class DecodeResult(
        val payload: BackupPayload,
    )

    fun export(payload: BackupPayload, passphrase: String, out: OutputStream): ExportResult {
        require(passphrase.length >= MIN_PASSPHRASE_LENGTH) {
            "Passphrase must be at least $MIN_PASSPHRASE_LENGTH characters"
        }
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val nonce = ByteArray(GCM_NONCE_BYTES).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(passphrase, salt, KDF_ITERATIONS)

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, nonce))

        val header = EncodedHeader(
            kdf = KdfHeader(iterations = KDF_ITERATIONS, salt = Base64.getEncoder().encodeToString(salt)),
            cipher = CipherHeader(nonce = Base64.getEncoder().encodeToString(nonce)),
            payloadLen = -1L, // placeholder, replaced below; payloadLen is informational
        )

        val payloadBytes = json.encodeToString(BackupPayload.serializer(), payload).toByteArray()
        val headerBytes = json.encodeToString(EncodedHeader.serializer(), header.copy(payloadLen = payloadBytes.size.toLong())).toByteArray()

        out.write(MAGIC)
        out.write(headerBytes.size.let { byteArrayOf(
            (it ushr 24).toByte(), (it ushr 16).toByte(), (it ushr 8).toByte(), it.toByte()
        ) })
        out.write(headerBytes)

        val cipherOut = CipherOutputStream(out, cipher)
        cipherOut.write(payloadBytes)
        cipherOut.flush()
        // CipherOutputStream.close() writes the GCM tag; do NOT close underlying stream.
        cipherOut.close()

        val ciphertext = payloadBytes.let { plain ->
            val c = Cipher.getInstance(CIPHER_TRANSFORMATION)
            c.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, nonce))
            c.doFinal(plain)
        }
        val sha = MessageDigest.getInstance("SHA-256").digest(ciphertext)
        return ExportResult(Base64.getEncoder().encodeToString(sha))
    }

    fun restore(input: InputStream, passphrase: String): DecodeResult {
        require(passphrase.length >= MIN_PASSPHRASE_LENGTH) {
            "Passphrase must be at least $MIN_PASSPHRASE_LENGTH characters"
        }
        val magic = readFully(input, MAGIC.size)
        require(magic != null && magic.contentEquals(MAGIC)) { "Not a rona backup file" }

        val headerLenBytes = readFully(input, 4)
        require(headerLenBytes != null && headerLenBytes.size == 4) { "Truncated header" }
        val headerLen = ((headerLenBytes[0].toInt() and 0xFF) shl 24) or
            ((headerLenBytes[1].toInt() and 0xFF) shl 16) or
            ((headerLenBytes[2].toInt() and 0xFF) shl 8) or
            (headerLenBytes[3].toInt() and 0xFF)

        val headerBytes = readFully(input, headerLen)
        require(headerBytes != null && headerBytes.size == headerLen) { "Truncated header" }
        val header = json.decodeFromString(EncodedHeader.serializer(), headerBytes.decodeToString())

        val salt = Base64.getDecoder().decode(header.kdf.salt)
        val nonce = Base64.getDecoder().decode(header.cipher.nonce)
        val key = deriveKey(passphrase, salt, header.kdf.iterations)

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, nonce))
        val cipherIn = CipherInputStream(input, cipher)
        val plaintext = cipherIn.readBytes()
        cipherIn.close()

        val payload = json.decodeFromString(BackupPayload.serializer(), plaintext.decodeToString())
        return DecodeResult(payload)
    }

    private fun readFully(input: InputStream, count: Int): ByteArray? {
        val buffer = ByteArray(count)
        var offset = 0
        while (offset < count) {
            val read = input.read(buffer, offset, count - offset)
            if (read < 0) return null
            offset += read
        }
        return buffer
    }

    private fun deriveKey(passphrase: String, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(
            passphrase.toCharArray(),
            salt,
            iterations.coerceAtLeast(MIN_KDF_ITERATIONS),
            KEY_BITS,
        )
        try {
            val factory = SecretKeyFactory.getInstance(KDF_ALGO)
            val derived = factory.generateSecret(spec).encoded
            return SecretKeySpec(derived, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    companion object {
        const val FORMAT_VERSION = "RONA1"
        val MAGIC = "RONA1".toByteArray(Charsets.US_ASCII)
        const val KDF_ALGO = "PBKDF2WithHmacSHA256"
        const val CIPHER_ALGO = "AES-256-GCM"
        const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        const val SALT_BYTES = 16
        const val GCM_NONCE_BYTES = 12
        const val GCM_TAG_BITS = 128
        const val KEY_BITS = 256
        const val KDF_ITERATIONS = 120_000
        const val MIN_KDF_ITERATIONS = 10_000
        const val MIN_PASSPHRASE_LENGTH = 8
    }
}
