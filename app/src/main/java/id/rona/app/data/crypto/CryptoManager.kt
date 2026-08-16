package id.rona.app.data.crypto

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import id.rona.app.util.PrivacyLogger
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Protects the SQLCipher database passphrase.
 *
 * The random 32-byte DB passphrase is encrypted with a non-exportable AES-256-GCM key held in
 * the Android Keystore. The ciphertext lives in an app-private file. Nothing sensitive ever
 * touches SharedPreferences or DataStore.
 */
@Singleton
class CryptoManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val secureRandom = SecureRandom()
    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    /**
     * Returns the existing DB passphrase or creates and persists a new one.
     */
    fun getOrCreateDbPassphrase(): ByteArray {
        val file = File(context.filesDir, KEY_FILE_NAME)
        return if (file.exists()) {
            try {
                decrypt(getOrCreateKey(), file.readBytes())
            } catch (e: Exception) {
                // Key invalidated or file corrupt: the encrypted DB can no longer be opened.
                // Regenerate cleanly so the app starts fresh instead of crashing forever.
                PrivacyLogger.e(TAG) { "db.key.enc unreadable; regenerating passphrase" }
                deletePassphrase()
                createNewPassphrase(file)
            }
        } else {
            createNewPassphrase(file)
        }
    }

    fun deletePassphrase() {
        runCatching { keyStore.deleteEntry(KEY_ALIAS) }
        File(context.filesDir, KEY_FILE_NAME).delete()
    }

    private fun createNewPassphrase(file: File): ByteArray {
        val passphrase = ByteArray(DB_PASSPHRASE_BYTES).also { secureRandom.nextBytes(it) }
        file.writeBytes(encrypt(getOrCreateKey(), passphrase))
        return passphrase
    }

    private fun getOrCreateKey(): SecretKey {
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setUnlockedDeviceRequired(true)
        }

        val keyGenerator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                keyGenerator.init(builder.setIsStrongBoxBacked(true).build())
                keyGenerator.generateKey()
            } catch (e: Exception) {
                // Device without StrongBox: fall back to software/TEE-backed key.
                PrivacyLogger.d(TAG) { "StrongBox unavailable, using standard key" }
                keyGenerator.init(builder.build())
                keyGenerator.generateKey()
            }
        } else {
            keyGenerator.init(builder.build())
            keyGenerator.generateKey()
        }
    }

    private fun encrypt(key: SecretKey, plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        return iv + ciphertext
    }

    private fun decrypt(key: SecretKey, stored: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, stored, 0, GCM_IV_BYTES))
        return cipher.doFinal(stored, GCM_IV_BYTES, stored.size - GCM_IV_BYTES)
    }

    companion object {
        private const val TAG = "CryptoManager"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "rona_db_key"
        private const val KEY_FILE_NAME = "db.key.enc"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_BYTES = 12
        private const val GCM_TAG_BITS = 128
        private const val DB_PASSPHRASE_BYTES = 32
    }
}
