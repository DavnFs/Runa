package id.rona.app.data.crypto

/**
 * Idempotent, thread-safe loader for the SQLCipher native library.
 *
 * The androidx SQLCipher artifact (net.zetetic:sqlcipher-android) does NOT
 * self-load its native library — `System.loadLibrary("sqlcipher")` must be
 * called before any `net.zetetic.database.sqlcipher.*` class opens a database.
 * Without it, the first encrypted DB open crashes with:
 *
 *   UnsatisfiedLinkError: No implementation found for
 *   net.zetetic.database.sqlcipher.SQLiteConnection.nativeOpen(...)
 *
 * Behavior:
 * - Idempotent: repeated calls are no-ops after the first successful load.
 * - Thread-safe: double-checked locking; concurrent callers never load twice.
 * - Failure is NOT swallowed: a genuinely missing/broken library propagates
 *   (no silent plaintext fallback — encryption is non-negotiable).
 */
class SqlCipherLibraryLoader(
    private val loadFn: () -> Unit = { System.loadLibrary(LIBRARY_NAME) },
) {

    @Volatile
    private var isLoaded = false

    fun load() {
        if (isLoaded) return

        synchronized(this) {
            if (isLoaded) return

            loadFn()
            isLoaded = true
        }
    }

    companion object {
        const val LIBRARY_NAME = "sqlcipher"
    }
}

/**
 * Process-wide facade used by production code. Kept minimal so the loader
 * itself stays JVM-testable via [SqlCipherLibraryLoader].
 */
object SqlCipherNativeLoader {
    private val delegate = SqlCipherLibraryLoader()

    fun load() = delegate.load()
}
