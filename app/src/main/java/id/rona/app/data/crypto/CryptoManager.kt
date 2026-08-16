package id.rona.app.data.crypto

import android.content.Context
import android.os.Build
import android.security.keystore.StrongBoxUnavailableException
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import id.rona.app.util.PrivacyLogger
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
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
 *
 * Key lifecycle:
 * - On first use, a key is created via [StrongBoxKeyPolicy]: StrongBox is
 *   PREFERRED (when the platform advertises it) but fully optional; on
 *   [StrongBoxUnavailableException] the key is generated in the standard
 *   Android Keystore (TEE/software-backed) instead. Both are non-exportable
 *   AES-256-GCM keys; the database remains SQLCipher-encrypted either way.
 * - An existing valid key under the alias is always reused — it is never
 *   regenerated just because StrongBox is unavailable today. This preserves
 *   access to existing encrypted databases.
 * - If the existing key becomes invalid (e.g. Keystore wipe) while
 *   `db.key.enc` still exists, the wrapped passphrase is unreadable. That
 *   error propagates (no silent regeneration) — silent regeneration would
 *   orphan the encrypted database with no way back.
 * - If the wrapped-passphrase FILE is corrupt/absent while the key is valid,
 *   only the file is regenerated. The encrypted database itself is never
 *   deleted automatically.
 */
@Singleton
class CryptoManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val secureRandom = SecureRandom()
    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    private val keyPolicy: StrongBoxKeyPolicy = StrongBoxKeyPolicy(
        capability = PackageManagerStrongBoxCapability(context),
        keyFactory = AndroidKeystoreKeyFactory(),
        onFallback = {
            PrivacyLogger.d(TAG) { "StrongBox unavailable, using Android Keystore fallback" }
        },
    )

    /**
     * Returns the existing DB passphrase or creates and persists a new one.
     * Synchronized: DB opening happens once at startup, but this guards
     * against double-init producing two different passphrases.
     */
    @Synchronized
    fun getOrCreateDbPassphrase(): ByteArray {
        val file = File(context.filesDir, KEY_FILE_NAME)
        return if (file.exists()) {
            decrypt(getOrCreateKey(), file.readBytes())
        } else {
            createNewPassphrase(file)
        }
    }

    @Synchronized
    fun deletePassphrase() {
        keyStore.deleteEntry(AndroidKeystoreKeyFactory.KEY_ALIAS)
        File(context.filesDir, KEY_FILE_NAME).delete()
    }

    private fun createNewPassphrase(file: File): ByteArray {
        val passphrase = ByteArray(DB_PASSPHRASE_BYTES).also { secureRandom.nextBytes(it) }
        file.writeBytes(encrypt(getOrCreateKey(), passphrase))
        return passphrase
    }

    /**
     * Reuses the existing key when present; otherwise creates one through
     * the StrongBox policy (StrongBox preferred, standard Keystore fallback).
     */
    @Synchronized
    private fun getOrCreateKey(): SecretKey {
        val existing = keyStore.getKey(AndroidKeystoreKeyFactory.KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        return keyPolicy.createKey()
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
        private const val KEY_FILE_NAME = "db.key.enc"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_BYTES = 12
        private const val GCM_TAG_BITS = 128
        private const val DB_PASSPHRASE_BYTES = 32
    }
}
