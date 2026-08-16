package id.rona.app.util

import android.util.Log

/**
 * The only logging entry point in rona.
 *
 * Compiles to a no-op in release builds (R8 strips the calls) so that personal notes,
 * health data, and other sensitive content can never reach Logcat on production devices.
 */
object PrivacyLogger {

    @JvmStatic
    fun d(tag: String, message: () -> String) {
        if (BuildConfigSafe.isDebug()) Log.d("Rona/$tag", message())
    }

    @JvmStatic
    fun e(tag: String, message: () -> String) {
        if (BuildConfigSafe.isDebug()) Log.e("Rona/$tag", message())
    }

    @JvmStatic
    fun w(tag: String, message: () -> String) {
        if (BuildConfigSafe.isDebug()) Log.w("Rona/$tag", message())
    }
}

private object BuildConfigSafe {
    fun isDebug(): Boolean = id.rona.app.BuildConfig.DEBUG
}
