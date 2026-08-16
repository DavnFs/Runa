package id.rona.app.data.repository

import id.rona.app.data.crypto.CryptoManager
import id.rona.app.data.db.RonaDatabase
import id.rona.app.data.db.dao.DailyLogDao
import id.rona.app.data.db.dao.ExportMetadataDao
import id.rona.app.data.db.dao.NlpSuggestionDao
import id.rona.app.data.db.dao.PeriodRecordDao
import id.rona.app.data.db.dao.ProfileDao
import id.rona.app.data.db.dao.SymptomLogDao
import id.rona.app.data.db.entity.AppLockSettingsEntity
import id.rona.app.data.db.entity.PrivacySettingsEntity
import id.rona.app.data.db.entity.ReminderSettingsEntity
import androidx.room.withTransaction
import id.rona.app.data.db.dao.CyclePredictionDao
import id.rona.app.data.db.dao.SettingsDao
import javax.inject.Inject
import javax.inject.Singleton

enum class DataCategory {
    DAILY_LOGS,
    PERIOD_HISTORY,
    SETTINGS,
}

@Singleton
class DataManager @Inject constructor(
    private val db: RonaDatabase,
    private val profileDao: ProfileDao,
    private val periodRecordDao: PeriodRecordDao,
    private val dailyLogDao: DailyLogDao,
    private val symptomLogDao: SymptomLogDao,
    private val cyclePredictionDao: CyclePredictionDao,
    private val nlpSuggestionDao: NlpSuggestionDao,
    private val exportMetadataDao: ExportMetadataDao,
    private val settingsDao: SettingsDao,
    private val cryptoManager: CryptoManager,
) {

    suspend fun deleteCategory(category: DataCategory) {
        db.withTransaction {
            when (category) {
                DataCategory.DAILY_LOGS -> {
                    dailyLogDao.deleteAll()
                    symptomLogDao.deleteAll()
                    nlpSuggestionDao.deleteAll()
                }
                DataCategory.PERIOD_HISTORY -> {
                    periodRecordDao.deleteAll()
                    cyclePredictionDao.deleteAll()
                }
                DataCategory.SETTINGS -> {
                    settingsDao.upsert(AppLockSettingsEntity(
                        lockEnabled = false,
                        biometricEnabled = false,
                        pinHash = null,
                        pinSalt = null,
                        updatedAt = System.currentTimeMillis(),
                    ))
                    settingsDao.upsert(ReminderSettingsEntity())
                    settingsDao.upsert(PrivacySettingsEntity())
                    exportMetadataDao.deleteAll()
                }
            }
        }
    }

    /**
     * Wipes everything: health data, profile, settings, predictions, export
     * audit. App returns to the onboarding flow; DB passphrase is regenerated
     * so nothing old can be recovered.
     */
    suspend fun deleteAllData() {
        db.withTransaction {
            profileDao.upsert(
                id.rona.app.data.db.entity.LocalProfileEntity(
                    id = id.rona.app.data.db.entity.LocalProfileEntity.SINGLETON_ID,
                    defaultCycleLengthDays = null,
                    onboardingCompletedAt = null,
                    createdAt = System.currentTimeMillis(),
                )
            )
            periodRecordDao.deleteAll()
            dailyLogDao.deleteAll()
            symptomLogDao.deleteAll()
            cyclePredictionDao.deleteAll()
            nlpSuggestionDao.deleteAll()
            exportMetadataDao.deleteAll()
            settingsDao.upsert(AppLockSettingsEntity(
                lockEnabled = false,
                biometricEnabled = false,
                pinHash = null,
                pinSalt = null,
                updatedAt = System.currentTimeMillis(),
            ))
            settingsDao.upsert(ReminderSettingsEntity())
            settingsDao.upsert(PrivacySettingsEntity())
        }
        cryptoManager.deletePassphrase()
        db.close()
    }
}
