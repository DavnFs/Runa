package id.rona.app.data.repository

import android.net.Uri
import androidx.room.withTransaction
import id.rona.app.data.backup.BackupCodec
import id.rona.app.data.backup.BackupPayload
import id.rona.app.data.backup.toBackup
import id.rona.app.data.backup.toEntity
import id.rona.app.data.db.RonaDatabase
import id.rona.app.data.db.dao.DailyLogDao
import id.rona.app.data.db.dao.ExportMetadataDao
import id.rona.app.data.db.dao.PeriodRecordDao
import id.rona.app.data.db.dao.ProfileDao
import id.rona.app.data.db.dao.SettingsDao
import id.rona.app.data.db.dao.SymptomLogDao
import id.rona.app.data.db.entity.DailyLogEntity
import id.rona.app.data.db.entity.ExportMetadataEntity
import id.rona.app.data.db.entity.LocalProfileEntity
import id.rona.app.data.db.entity.PeriodRecordEntity
import id.rona.app.data.db.entity.ReminderSettingsEntity
import id.rona.app.data.db.entity.PrivacySettingsEntity
import id.rona.app.data.db.entity.SymptomLogEntity
import id.rona.app.domain.model.PrivacyMode
import id.rona.app.domain.model.Severity
import id.rona.app.domain.model.SymptomType
import id.rona.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    private val db: RonaDatabase,
    private val profileDao: ProfileDao,
    private val periodRecordDao: PeriodRecordDao,
    private val dailyLogDao: DailyLogDao,
    private val symptomLogDao: SymptomLogDao,
    private val settingsDao: SettingsDao,
    private val exportMetadataDao: ExportMetadataDao,
    private val codec: BackupCodec,
) {

    suspend fun export(passphrase: String, out: OutputStream, fileName: String?) {
        val now = System.currentTimeMillis()
        val profile = profileDao.get()
        val periods = periodRecordDao.getAll()
        val logs = dailyLogDao.getAll()
        val symptomsByLog = logs.associate { log ->
            log.id to symptomLogDao.getForLog(log.id)
        }

        val payload = BackupPayload(
            appVersion = BuildConfigHolder.versionName,
            exportedAt = now,
            profile = profile?.toBackup(),
            periods = periods.map { it.toBackup() },
            dailyLogs = logs.map { it.toBackup(symptomsByLog[it.id].orEmpty()) },
            reminder = settingsDao.getReminderSettings()?.toBackup(),
            privacy = settingsDao.getPrivacySettings()?.toBackup(),
        )

        val result = codec.export(payload, passphrase, out)
        exportMetadataDao.upsert(
            ExportMetadataEntity(
                exportedAt = now,
                fileName = fileName,
                schemaVersion = BackupPayload.SCHEMA_VERSION,
                appVersion = BuildConfigHolder.versionName,
                periodCount = periods.size,
                dailyLogCount = logs.size,
                ciphertextSha256 = result.ciphertextSha256,
            )
        )
    }

    /**
     * Restores a backup transactionally: wipes existing data, inserts payload.
     * Cycle predictions are NOT in the payload and get recomputed by the engine.
     */
    suspend fun restore(input: InputStream, passphrase: String) {
        val decoded = codec.restore(input, passphrase)
        val payload = decoded.payload

        db.withTransaction {
            periodRecordDao.deleteAll()
            dailyLogDao.deleteAll()
            symptomLogDao.deleteAll()

            payload.periods.forEach { period ->
                periodRecordDao.upsert(
                    PeriodRecordEntity(
                        startEpochDay = period.startEpochDay,
                        endEpochDay = period.endEpochDay,
                        createdAt = period.createdAt,
                        updatedAt = period.updatedAt,
                    )
                )
            }

            payload.dailyLogs.forEach { log ->
                val id = dailyLogDao.upsert(log.toEntity())
                log.symptoms.forEach { symptom ->
                    symptomLogDao.upsert(
                        SymptomLogEntity(
                            dailyLogId = id,
                            symptomType = SymptomType.valueOf(symptom.symptomType),
                            severity = Severity.valueOf(symptom.severity),
                        )
                    )
                }
            }

            payload.profile?.let { profile ->
                profileDao.upsert(
                    LocalProfileEntity(
                        id = LocalProfileEntity.SINGLETON_ID,
                        defaultCycleLengthDays = profile.defaultCycleLengthDays,
                        onboardingCompletedAt = profile.onboardingCompletedAt,
                        createdAt = System.currentTimeMillis(),
                    )
                )
            }

            payload.reminder?.let { reminder ->
                settingsDao.upsert(
                    ReminderSettingsEntity(
                        periodReminderEnabled = reminder.periodReminderEnabled,
                        periodReminderDaysBefore = reminder.periodReminderDaysBefore,
                        periodReminderMinuteOfDay = reminder.periodReminderMinuteOfDay,
                        dailyLogReminderEnabled = reminder.dailyLogReminderEnabled,
                        dailyLogMinuteOfDay = reminder.dailyLogMinuteOfDay,
                        vibrationEnabled = reminder.vibrationEnabled,
                    )
                )
            }

            payload.privacy?.let { privacy ->
                settingsDao.upsert(
                    PrivacySettingsEntity(
                        notificationPrivacyMode = PrivacyMode.valueOf(privacy.notificationPrivacyMode),
                        allowScreenshots = privacy.allowScreenshots,
                        themeMode = ThemeMode.valueOf(privacy.themeMode),
                    )
                )
            }
        }
    }
}

private object BuildConfigHolder {
    val versionName: String get() = id.rona.app.BuildConfig.VERSION_NAME
}
