package id.rona.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_lock_settings")
data class AppLockSettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val lockEnabled: Boolean,
    val biometricEnabled: Boolean,
    val pinHash: String? = null, // SENSITIVE — PBKDF2-HMAC-SHA256
    val pinSalt: String? = null, // SENSITIVE
    val pinIterations: Int = DEFAULT_PIN_ITERATIONS,
    val gracePeriodSeconds: Int = DEFAULT_GRACE_SECONDS,
    val updatedAt: Long,
) {
    companion object {
        const val SINGLETON_ID = 1
        const val DEFAULT_PIN_ITERATIONS = 120_000
        const val DEFAULT_GRACE_SECONDS = 30
    }
}
