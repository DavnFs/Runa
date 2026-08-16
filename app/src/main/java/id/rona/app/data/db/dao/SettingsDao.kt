package id.rona.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import id.rona.app.data.db.entity.AppLockSettingsEntity
import id.rona.app.data.db.entity.PrivacySettingsEntity
import id.rona.app.data.db.entity.ReminderSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {

    @Query("SELECT * FROM app_lock_settings WHERE id = :id")
    fun observeLockSettings(id: Int = AppLockSettingsEntity.SINGLETON_ID): Flow<AppLockSettingsEntity?>

    @Query("SELECT * FROM app_lock_settings WHERE id = :id")
    suspend fun getLockSettings(id: Int = AppLockSettingsEntity.SINGLETON_ID): AppLockSettingsEntity?

    @Upsert
    suspend fun upsert(settings: AppLockSettingsEntity)

    @Query("SELECT * FROM reminder_settings WHERE id = :id")
    fun observeReminderSettings(id: Int = ReminderSettingsEntity.SINGLETON_ID): Flow<ReminderSettingsEntity?>

    @Query("SELECT * FROM reminder_settings WHERE id = :id")
    suspend fun getReminderSettings(id: Int = ReminderSettingsEntity.SINGLETON_ID): ReminderSettingsEntity?

    @Upsert
    suspend fun upsert(settings: ReminderSettingsEntity)

    @Query("SELECT * FROM privacy_settings WHERE id = :id")
    fun observePrivacySettings(id: Int = PrivacySettingsEntity.SINGLETON_ID): Flow<PrivacySettingsEntity?>

    @Query("SELECT * FROM privacy_settings WHERE id = :id")
    suspend fun getPrivacySettings(id: Int = PrivacySettingsEntity.SINGLETON_ID): PrivacySettingsEntity?

    @Upsert
    suspend fun upsert(settings: PrivacySettingsEntity)
}
