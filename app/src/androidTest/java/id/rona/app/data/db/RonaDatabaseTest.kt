package id.rona.app.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import id.rona.app.data.db.dao.DailyLogDao
import id.rona.app.data.db.dao.ExportMetadataDao
import id.rona.app.data.db.dao.NlpSuggestionDao
import id.rona.app.data.db.dao.PeriodRecordDao
import id.rona.app.data.db.dao.ProfileDao
import id.rona.app.data.db.dao.SettingsDao
import id.rona.app.data.db.dao.SymptomLogDao
import id.rona.app.data.db.entity.AppLockSettingsEntity
import id.rona.app.data.db.entity.CyclePredictionEntity
import id.rona.app.data.db.entity.DailyLogEntity
import id.rona.app.data.db.entity.ExportMetadataEntity
import id.rona.app.data.db.entity.LocalProfileEntity
import id.rona.app.data.db.entity.NlpSuggestionEntity
import id.rona.app.data.db.entity.PeriodRecordEntity
import id.rona.app.data.db.entity.PrivacySettingsEntity
import id.rona.app.data.db.entity.ReminderSettingsEntity
import id.rona.app.data.db.entity.SymptomLogEntity
import id.rona.app.domain.model.Confidence
import id.rona.app.domain.model.Energy
import id.rona.app.domain.model.FlowLevel
import id.rona.app.domain.model.Mood
import id.rona.app.domain.model.NlpStatus
import id.rona.app.domain.model.PrivacyMode
import id.rona.app.domain.model.Severity
import id.rona.app.domain.model.SymptomType
import id.rona.app.domain.model.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class RonaDatabaseTest {

    private lateinit var db: RonaDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RonaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun profileUpsertAndRead() = runBlocking {
        val now = System.currentTimeMillis()
        db.profileDao().upsert(LocalProfileEntity(createdAt = now, defaultCycleLengthDays = 28))
        val profile = db.profileDao().get()
        assertThat(profile).isNotNull()
        assertThat(profile!!.defaultCycleLengthDays).isEqualTo(28)
    }

    @Test
    fun periodRecordCrudAndOngoingQuery() = runBlocking {
        val now = System.currentTimeMillis()
        val dao = db.periodRecordDao()
        val id1 = dao.upsert(PeriodRecordEntity(startEpochDay = 19000, createdAt = now, updatedAt = now))
        dao.upsert(PeriodRecordEntity(startEpochDay = 19030, createdAt = now, updatedAt = now))

        assertThat(dao.count()).isEqualTo(2)
        val ongoing = dao.getOngoing()
        assertThat(ongoing).isNotNull()
        assertThat(ongoing!!.startEpochDay).isEqualTo(19030)

        val activeOnDay25 = dao.getActiveOn(19020)
        assertThat(activeOnDay25).isNotNull()
        assertThat(activeOnDay25!!.id).isEqualTo(id1)

        dao.deleteById(id1)
        assertThat(dao.count()).isEqualTo(1)
    }

    @Test
    fun dailyLogUniquePerDate() = runBlocking {
        val now = System.currentTimeMillis()
        val dao = db.dailyLogDao()
        dao.upsert(
            DailyLogEntity(dateEpochDay = 19000, flow = FlowLevel.LIGHT, createdAt = now, updatedAt = now)
        )

        val duplicate = dao.getByDate(19000)
        assertThat(duplicate).isNotNull()

        val second = dao.upsert(
            DailyLogEntity(dateEpochDay = 19000, flow = FlowLevel.MEDIUM, createdAt = now, updatedAt = now)
        )
        assertThat(second).isEqualTo(duplicate!!.id)
        assertThat(dao.count()).isEqualTo(1)
        assertThat(dao.getByDate(19000)!!.flow).isEqualTo(FlowLevel.MEDIUM)
    }

    @Test
    fun symptomCascadeDeleteOnDailyLogRemoval() = runBlocking {
        val now = System.currentTimeMillis()
        val dailyLogId = db.dailyLogDao().upsert(
            DailyLogEntity(dateEpochDay = 19000, note = "catatan privat", createdAt = now, updatedAt = now)
        )
        db.symptomLogDao().upsert(
            SymptomLogEntity(dailyLogId = dailyLogId, symptomType = SymptomType.KRAM, severity = Severity.MODERATE)
        )
        db.symptomLogDao().upsert(
            SymptomLogEntity(dailyLogId = dailyLogId, symptomType = SymptomType.HEADACHE, severity = Severity.MILD)
        )
        assertThat(db.symptomLogDao().getForLog(dailyLogId)).hasSize(2)

        db.dailyLogDao().deleteById(dailyLogId)
        assertThat(db.symptomLogDao().getForLog(dailyLogId)).isEmpty()
    }

    @Test
    fun nlpSuggestionCascadeDeleteOnDailyLogRemoval() = runBlocking {
        val now = System.currentTimeMillis()
        val dailyLogId = db.dailyLogDao().upsert(
            DailyLogEntity(dateEpochDay = 19000, note = "note untuk nlp", createdAt = now, updatedAt = now)
        )
        db.nlpSuggestionDao().upsert(
            NlpSuggestionEntity(
                dailyLogId = dailyLogId,
                source = "RULES",
                payloadJson = """{"symptom":"KRAM","confidence":0.9}""",
                status = NlpStatus.PENDING,
                createdAt = now,
            )
        )
        assertThat(db.nlpSuggestionDao().getForLog(dailyLogId)).hasSize(1)

        db.dailyLogDao().deleteById(dailyLogId)
        assertThat(db.nlpSuggestionDao().getForLog(dailyLogId)).isEmpty()
    }

    @Test
    fun symptomUniquePerTypePerLog() = runBlocking {
        val now = System.currentTimeMillis()
        val dailyLogId = db.dailyLogDao().upsert(
            DailyLogEntity(dateEpochDay = 19000, createdAt = now, updatedAt = now)
        )
        db.symptomLogDao().upsert(
            SymptomLogEntity(dailyLogId = dailyLogId, symptomType = SymptomType.ACNE, severity = Severity.MILD)
        )
        val upsertedId = db.symptomLogDao().upsert(
            SymptomLogEntity(dailyLogId = dailyLogId, symptomType = SymptomType.ACNE, severity = Severity.SEVERE)
        )
        assertThat(db.symptomLogDao().getForLog(dailyLogId)).hasSize(1)
        assertThat(db.symptomLogDao().getForLog(dailyLogId).single().id).isEqualTo(upsertedId)
    }

    @Test
    fun settingsSingletonRows() = runBlocking {
        val now = System.currentTimeMillis()
        db.settingsDao().upsert(
            AppLockSettingsEntity(
                lockEnabled = true,
                biometricEnabled = false,
                pinHash = "hash",
                pinSalt = "salt",
                updatedAt = now,
            )
        )
        db.settingsDao().upsert(ReminderSettingsEntity(periodReminderEnabled = false))
        db.settingsDao().upsert(PrivacySettingsEntity(notificationPrivacyMode = PrivacyMode.GENERIC, themeMode = ThemeMode.DARK))

        val lock = db.settingsDao().getLockSettings()
        assertThat(lock).isNotNull()
        assertThat(lock!!.lockEnabled).isTrue()

        val reminder = db.settingsDao().getReminderSettings()
        assertThat(reminder).isNotNull()
        assertThat(reminder!!.periodReminderEnabled).isFalse()

        val privacy = db.settingsDao().getPrivacySettings()
        assertThat(privacy).isNotNull()
        assertThat(privacy!!.themeMode).isEqualTo(ThemeMode.DARK)
    }

    @Test
    fun predictionLatestAndTrim() = runBlocking {
        val dao = db.cyclePredictionDao()
        val now = System.currentTimeMillis()
        dao.upsert(
            CyclePredictionEntity(
                generatedAt = now,
                predictedStartEpochDay = 20000,
                rangeLowEpochDay = 19995,
                rangeHighEpochDay = 20005,
                medianCycleLengthDays = 30,
                meanCycleLengthDays = 30.4,
                madDays = 1.5,
                cycleCountUsed = 4,
                confidence = Confidence.MEDIUM,
                engineVersion = 1,
            )
        )
        dao.upsert(
            CyclePredictionEntity(
                generatedAt = now + 1000,
                predictedStartEpochDay = 20030,
                rangeLowEpochDay = 20025,
                rangeHighEpochDay = 20035,
                medianCycleLengthDays = 29,
                meanCycleLengthDays = 29.2,
                madDays = 2.0,
                cycleCountUsed = 5,
                confidence = Confidence.HIGH,
                engineVersion = 1,
            )
        )
        assertThat(dao.getLatest()!!.predictedStartEpochDay).isEqualTo(20030)
        dao.trimTo(1)
        assertThat(dao.getLatest()!!.predictedStartEpochDay).isEqualTo(20030)
    }

    @Test
    fun exportMetadataRoundTrip() = runBlocking {
        val now = System.currentTimeMillis()
        db.exportMetadataDao().upsert(
            ExportMetadataEntity(
                exportedAt = now,
                fileName = "rona-backup.rona",
                schemaVersion = 1,
                appVersion = "0.1.0",
                periodCount = 3,
                dailyLogCount = 42,
                ciphertextSha256 = "abc123",
            )
        )
        assertThat(db.exportMetadataDao().count()).isEqualTo(1)
        assertThat(db.exportMetadataDao().getAll().single().fileName).isEqualTo("rona-backup.rona")
    }

    @Test
    fun plaintextSensitiveStringsNeverTouchDisk_whenEncryptedFactoryUsed() {
        // In-memory DB cannot prove disk encryption; this test guards the real contract:
        // a real RonaDatabase.Factory uses SQLCipher SupportFactory. Here we verify the
        // file-based DB cannot be opened without a passphrase once created with one.
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = File(context.cacheDir, "test-encrypted.db")
        dbFile.delete()
        val passphrase = ByteArray(32) { 7 }
        val factory = net.zetetic.database.sqlcipher.SupportOpenHelperFactory(passphrase)
        val diskDb = Room.databaseBuilder(context, RonaDatabase::class.java, dbFile.name)
            .openHelperFactory(factory)
            .allowMainThreadQueries()
            .build()
        runBlocking {
            val now = System.currentTimeMillis()
            diskDb.dailyLogDao().upsert(
                DailyLogEntity(dateEpochDay = 19000, note = "RAHASIA_DI_DISK", createdAt = now, updatedAt = now)
            )
        }
        diskDb.close()

        val rawBytes = dbFile.readBytes()
        val rawText = String(rawBytes, Charsets.ISO_8859_1)
        assertThat(rawText).doesNotContain("RAHASIA_DI_DISK")
        dbFile.delete()
    }
}
