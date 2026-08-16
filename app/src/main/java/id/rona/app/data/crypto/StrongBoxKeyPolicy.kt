package id.rona.app.data.crypto

import android.annotation.SuppressLint
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import androidx.annotation.RequiresApi
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Production key generation for the Android Keystore.
 *
 * The StrongBox decision lives in [StrongBoxKeyPolicy]; this class only
 * performs generation. [generateKey] is a pure function of the flag —
 * no builder reuse, so a StrongBox failure can never leak the StrongBox
 * flag into a retry.
 */
class AndroidKeystoreKeyFactory : KeystoreKeyFactory {

    override fun generateKey(useStrongBox: Boolean): SecretKey {
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return generateKeyApiP(builder, useStrongBox)
        }

        val keyGenerator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun generateKeyApiP(
        builder: KeyGenParameterSpec.Builder,
        useStrongBox: Boolean,
    ): SecretKey {
        builder.setUnlockedDeviceRequired(true)
        if (useStrongBox) {
            builder.setIsStrongBoxBacked(true)
        }
        val keyGenerator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    companion object {
        const val KEY_ALIAS = "rona_db_key"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    }
}

/**
 * Pure decision logic for StrongBox usage. Deterministic and JVM-testable:
 *
 * - API < 28 -> standard Android Keystore.
 * - StrongBox feature absent -> standard Android Keystore.
 * - StrongBox feature present -> try StrongBox; on
 *   [StrongBoxUnavailableException] ONLY, fall back to standard Keystore.
 * - Any other exception propagates (no silent downgrade).
 */
class StrongBoxKeyPolicy(
    private val capability: StrongBoxCapability,
    private val keyFactory: KeystoreKeyFactory,
    private val onFallback: () -> Unit = {},
    private val apiLevel: Int = Build.VERSION.SDK_INT,
) {

    /**
     * In production [apiLevel] is always [Build.VERSION.SDK_INT], so the
     * runtime guard below is exactly as strict as lint wants it to be.
     * Lint cannot see through the injected parameter (needed for JVM tests),
     * hence the targeted suppression.
     */
    @SuppressLint("NewApi")
    fun createKey(): SecretKey {
        if (!capability.isStrongBoxSupported()) {
            return keyFactory.generateKey(useStrongBox = false)
        }
        if (apiLevel >= Build.VERSION_CODES.P) {
            return createWithStrongBoxFallback()
        }
        // Defensive: capability lied (should not happen) — standard Keystore.
        return keyFactory.generateKey(useStrongBox = false)
    }

    /**
     * API-gated: StrongBox (and its dedicated exception) only exist on API 28+.
     * The capability check already guarantees P+, but this keeps lint and the
     * verifier happy on minSdk 26 devices.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private fun createWithStrongBoxFallback(): SecretKey {
        return try {
            keyFactory.generateKey(useStrongBox = true)
        } catch (e: StrongBoxUnavailableException) {
            onFallback()
            keyFactory.generateKey(useStrongBox = false)
        }
    }
}
