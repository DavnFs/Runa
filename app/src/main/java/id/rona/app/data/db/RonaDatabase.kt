package id.rona.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import id.rona.app.data.db.dao.CyclePredictionDao
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
import id.rona.app.data.crypto.CryptoManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Inject
import javax.inject.Singleton

@Database(
    entities = [
        LocalProfileEntity::class,
        PeriodRecordEntity::class,
        DailyLogEntity::class,
        SymptomLogEntity::class,
        CyclePredictionEntity::class,
        AppLockSettingsEntity::class,
        ReminderSettingsEntity::class,
        PrivacySettingsEntity::class,
        ExportMetadataEntity::class,
        NlpSuggestionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class RonaDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao
    abstract fun periodRecordDao(): PeriodRecordDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun symptomLogDao(): SymptomLogDao
    abstract fun cyclePredictionDao(): CyclePredictionDao
    abstract fun settingsDao(): SettingsDao
    abstract fun exportMetadataDao(): ExportMetadataDao
    abstract fun nlpSuggestionDao(): NlpSuggestionDao

    @Singleton
    class Factory @Inject constructor(
        private val cryptoManager: CryptoManager,
    ) {
        fun create(context: Context): RonaDatabase {
            val passphrase = cryptoManager.getOrCreateDbPassphrase()
            val factory = SupportOpenHelperFactory(passphrase)
            return Room.databaseBuilder(context, RonaDatabase::class.java, DB_NAME)
                .openHelperFactory(factory)
                // Destructive migration is forbidden by design (see docs/PLAN.md §7).
                // .addMigrations(...) appended here when schema version bumps.
                .build()
        }

        companion object {
            const val DB_NAME = "rona.db"
        }
    }
}
