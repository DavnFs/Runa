package id.rona.app.data.backup

import id.rona.app.data.db.entity.DailyLogEntity
import id.rona.app.data.db.entity.PeriodRecordEntity
import id.rona.app.data.db.entity.ReminderSettingsEntity
import id.rona.app.data.db.entity.PrivacySettingsEntity
import id.rona.app.data.db.entity.LocalProfileEntity
import id.rona.app.domain.model.Energy
import id.rona.app.domain.model.FlowLevel
import id.rona.app.domain.model.Mood
import id.rona.app.domain.model.PrivacyMode
import id.rona.app.domain.model.Severity
import id.rona.app.domain.model.SymptomType
import id.rona.app.domain.model.ThemeMode
import kotlinx.serialization.Serializable

/**
 * Serializable backup payload. Versioned: bump [SCHEMA_VERSION] whenever the
 * shape changes and add a migration in [BackupCodec].
 */
@Serializable
data class BackupPayload(
    val schemaVersion: Int = SCHEMA_VERSION,
    val appVersion: String = "",
    val exportedAt: Long = 0L,
    val profile: BackupProfile? = null,
    val periods: List<BackupPeriod> = emptyList(),
    val dailyLogs: List<BackupDailyLog> = emptyList(),
    val reminder: BackupReminder? = null,
    val privacy: BackupPrivacy? = null,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

@Serializable
data class BackupProfile(
    val defaultCycleLengthDays: Int? = null,
    val onboardingCompletedAt: Long? = null,
)

@Serializable
data class BackupPeriod(
    val startEpochDay: Long,
    val endEpochDay: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupDailyLog(
    val dateEpochDay: Long,
    val flow: String? = null,
    val mood: String? = null,
    val energy: String? = null,
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val symptoms: List<BackupSymptom> = emptyList(),
)

@Serializable
data class BackupSymptom(
    val symptomType: String,
    val severity: String,
)

@Serializable
data class BackupReminder(
    val periodReminderEnabled: Boolean,
    val periodReminderDaysBefore: Int,
    val periodReminderMinuteOfDay: Int,
    val dailyLogReminderEnabled: Boolean,
    val dailyLogMinuteOfDay: Int,
    val vibrationEnabled: Boolean,
)

@Serializable
data class BackupPrivacy(
    val notificationPrivacyMode: String,
    val allowScreenshots: Boolean,
    val themeMode: String,
)

fun ReminderSettingsEntity.toBackup() = BackupReminder(
    periodReminderEnabled = periodReminderEnabled,
    periodReminderDaysBefore = periodReminderDaysBefore,
    periodReminderMinuteOfDay = periodReminderMinuteOfDay,
    dailyLogReminderEnabled = dailyLogReminderEnabled,
    dailyLogMinuteOfDay = dailyLogMinuteOfDay,
    vibrationEnabled = vibrationEnabled,
)

fun PrivacySettingsEntity.toBackup() = BackupPrivacy(
    notificationPrivacyMode = notificationPrivacyMode.name,
    allowScreenshots = allowScreenshots,
    themeMode = themeMode.name,
)

fun LocalProfileEntity.toBackup() = BackupProfile(
    defaultCycleLengthDays = defaultCycleLengthDays,
    onboardingCompletedAt = onboardingCompletedAt,
)

fun PeriodRecordEntity.toBackup() = BackupPeriod(
    startEpochDay = startEpochDay,
    endEpochDay = endEpochDay,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun DailyLogEntity.toBackup(symptoms: List<id.rona.app.data.db.entity.SymptomLogEntity>) =
    BackupDailyLog(
        dateEpochDay = dateEpochDay,
        flow = flow?.name,
        mood = mood?.name,
        energy = energy?.name,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
        symptoms = symptoms.map { BackupSymptom(it.symptomType.name, it.severity.name) },
    )

fun BackupDailyLog.toEntity(id: Long = 0) = DailyLogEntity(
    id = id,
    dateEpochDay = dateEpochDay,
    flow = flow?.let { FlowLevel.valueOf(it) },
    mood = mood?.let { Mood.valueOf(it) },
    energy = energy?.let { Energy.valueOf(it) },
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
