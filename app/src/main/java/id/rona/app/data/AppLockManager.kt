package id.rona.app.data

import id.rona.app.data.db.dao.SettingsDao
import id.rona.app.domain.lock.LockPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide lock state. Tracks the unlocked moment and evaluates re-lock on
 * foreground transitions using [LockPolicy]. Lock settings (enabled, grace)
 * are cached from the encrypted DB.
 */
@Singleton
class AppLockManager @Inject constructor(
    settingsDao: SettingsDao,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var lockEnabled = false
    private var graceSeconds = 30
    private var hasBackgrounded = false
    private var unlockedAtMs = 0L

    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    init {
        scope.launch {
            settingsDao.observeLockSettings().collect { settings ->
                if (settings != null) {
                    lockEnabled = settings.lockEnabled
                    graceSeconds = settings.gracePeriodSeconds
                }
            }
        }
    }

    fun onUnlocked() {
        unlockedAtMs = System.currentTimeMillis()
        _unlocked.value = true
    }

    fun lock() {
        _unlocked.value = false
    }

    fun onAppStopped() {
        hasBackgrounded = true
    }

    /**
     * Evaluated when the app returns to the foreground. Returns true when the
     * user may continue, false when the app must show the lock gate.
     */
    fun onAppStarted(): Boolean {
        if (!hasBackgrounded) return true
        val shouldLock = lockEnabled &&
            LockPolicy.shouldRelockOnForeground(System.currentTimeMillis(), unlockedAtMs, graceSeconds)
        if (shouldLock) _unlocked.value = false
        return !shouldLock
    }
}
