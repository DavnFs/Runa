package id.rona.app.domain.lock

import java.time.LocalDate

object LockPolicy {

    const val FAILURE_THRESHOLD = 5
    const val BASE_LOCKOUT_SECONDS = 30

    /**
     * Whether the app must re-lock when returning to the foreground.
     *
     * With grace = 30 s: unlock, use the app, leave, return within 30 s of the
     * unlock moment -> stays unlocked. Return later -> lock again.
     * grace = 0 -> every foreground transition locks.
     */
    fun shouldRelockOnForeground(
        nowMs: Long,
        unlockedAtMs: Long,
        graceSeconds: Int,
    ): Boolean {
        if (unlockedAtMs <= 0) return true
        if (graceSeconds <= 0) return true
        return (nowMs - unlockedAtMs) / 1000 >= graceSeconds
    }

    /**
     * Consecutive wrong PIN attempts trigger a temporary lockout.
     */
    fun lockoutSeconds(
        consecutiveFailures: Int,
        failureThreshold: Int = FAILURE_THRESHOLD,
        baseSeconds: Int = BASE_LOCKOUT_SECONDS,
    ): Int =
        if (consecutiveFailures >= failureThreshold) baseSeconds else 0
}
