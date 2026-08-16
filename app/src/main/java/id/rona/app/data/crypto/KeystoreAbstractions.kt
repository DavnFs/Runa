package id.rona.app.data.crypto

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.KeyStore
import javax.crypto.SecretKey

/**
 * Capability lookup for StrongBox. Abstracted so the decision can be unit
 * tested on the JVM without real StrongBox hardware.
 */
interface StrongBoxCapability {
    /** True only when StrongBox is genuinely advertised by the platform. */
    fun isStrongBoxSupported(): Boolean
}

class PackageManagerStrongBoxCapability(private val context: Context) : StrongBoxCapability {
    override fun isStrongBoxSupported(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
    }
}

/**
 * Generates AES-256-GCM keys inside the Android Keystore. Abstracted for
 * JVM testability; the production implementation never materializes key
 * bytes outside the Keystore (non-exportable by construction).
 */
interface KeystoreKeyFactory {
    fun generateKey(useStrongBox: Boolean): SecretKey
}
