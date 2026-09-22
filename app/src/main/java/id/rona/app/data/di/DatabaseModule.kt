package id.rona.app.data.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.rona.app.data.db.RunaDatabase
import id.rona.app.data.db.dao.CyclePredictionDao
import id.rona.app.data.db.dao.DailyLogDao
import id.rona.app.data.db.dao.ExportMetadataDao
import id.rona.app.data.db.dao.NlpSuggestionDao
import id.rona.app.data.db.dao.PeriodRecordDao
import id.rona.app.data.db.dao.ProfileDao
import id.rona.app.data.db.dao.SettingsDao
import id.rona.app.data.db.dao.SymptomLogDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideRonaDatabase(
        @ApplicationContext context: Context,
        factory: RunaDatabase.Factory,
    ): RunaDatabase = factory.create(context)

    @Provides
    fun provideProfileDao(db: RunaDatabase): ProfileDao = db.profileDao()

    @Provides
    fun providePeriodRecordDao(db: RunaDatabase): PeriodRecordDao = db.periodRecordDao()

    @Provides
    fun provideDailyLogDao(db: RunaDatabase): DailyLogDao = db.dailyLogDao()

    @Provides
    fun provideSymptomLogDao(db: RunaDatabase): SymptomLogDao = db.symptomLogDao()

    @Provides
    fun provideCyclePredictionDao(db: RunaDatabase): CyclePredictionDao = db.cyclePredictionDao()

    @Provides
    fun provideSettingsDao(db: RunaDatabase): SettingsDao = db.settingsDao()

    @Provides
    fun provideExportMetadataDao(db: RunaDatabase): ExportMetadataDao = db.exportMetadataDao()

    @Provides
    fun provideNlpSuggestionDao(db: RunaDatabase): NlpSuggestionDao = db.nlpSuggestionDao()

    @Provides
    @Singleton
    fun provideTransactionRunner(db: RunaDatabase): id.rona.app.data.db.TransactionRunner =
        id.rona.app.data.db.RoomTransactionRunner(db)
}
