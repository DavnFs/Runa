package id.rona.app.data.repository

import id.rona.app.data.crypto.PinVerifier
import id.rona.app.data.db.dao.SettingsDao
import id.rona.app.data.db.entity.AppLockSettingsEntity
import id.rona.app.data.db.entity.PrivacySettingsEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLockRepository @Inject constructor(
    private val settingsDao: SettingsDao,
    private val pinVerifier: PinVerifier,
) {

    fun observeLockSettings(): Flow<AppLockSettingsEntity?> = settingsDao.observeLockSettings()

    fun observePrivacySettings(): Flow<PrivacySettingsEntity?> = settingsDao.observePrivacySettings()

    suspend fun getLockSettings(): AppLockSettingsEntity? = settingsDao.getLockSettings()

    /**
     * Sets a new PIN (4-6 digits). Stores PBKDF2 hash + random salt in the
     * encrypted DB only. Enables the lock.
     */
    suspend fun setPin(pin: String, nowMs: Long = System.currentTimeMillis()) {
        val salt = pinVerifier.generateSalt()
        val hash = pinVerifier.hashPin(pin, salt)
        val current = settingsDao.getLockSettings() ?: AppLockSettingsEntity(
            lockEnabled = true,
            biometricEnabled = false,
            updatedAt = nowMs,
        )
        settingsDao.upsert(
            current.copy(
                lockEnabled = true,
                pinHash = hash,
                pinSalt = salt,
                pinIterations = PinVerifier.MIN_ITERATIONS,
                pinLength = pin.length,
                updatedAt = nowMs,
            )
        )
    }

    suspend fun verifyPin(pin: String): Boolean {
        val settings = settingsDao.getLockSettings() ?: return false
        val hash = settings.pinHash ?: return false
        val salt = settings.pinSalt ?: return false
        return pinVerifier.verify(pin, hash, salt, settings.pinIterations)
    }

    suspend fun setBiometricEnabled(enabled: Boolean, nowMs: Long = System.currentTimeMillis()) {
        val current = settingsDao.getLockSettings() ?: return
        settingsDao.upsert(current.copy(biometricEnabled = enabled, updatedAt = nowMs))
    }

    suspend fun setGracePeriodSeconds(seconds: Int, nowMs: Long = System.currentTimeMillis()) {
        val current = settingsDao.getLockSettings() ?: return
        settingsDao.upsert(current.copy(gracePeriodSeconds = seconds, updatedAt = nowMs))
    }
}
