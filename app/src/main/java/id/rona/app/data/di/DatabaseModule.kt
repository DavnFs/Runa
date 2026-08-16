package id.rona.app.data.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.rona.app.data.db.RonaDatabase
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
        factory: RonaDatabase.Factory,
    ): RonaDatabase = factory.create(context)

    @Provides
    fun provideProfileDao(db: RonaDatabase): ProfileDao = db.profileDao()

    @Provides
    fun providePeriodRecordDao(db: RonaDatabase): PeriodRecordDao = db.periodRecordDao()

    @Provides
    fun provideDailyLogDao(db: RonaDatabase): DailyLogDao = db.dailyLogDao()

    @Provides
    fun provideSymptomLogDao(db: RonaDatabase): SymptomLogDao = db.symptomLogDao()

    @Provides
    fun provideCyclePredictionDao(db: RonaDatabase): CyclePredictionDao = db.cyclePredictionDao()

    @Provides
    fun provideSettingsDao(db: RonaDatabase): SettingsDao = db.settingsDao()

    @Provides
    fun provideExportMetadataDao(db: RonaDatabase): ExportMetadataDao = db.exportMetadataDao()

    @Provides
    fun provideNlpSuggestionDao(db: RonaDatabase): NlpSuggestionDao = db.nlpSuggestionDao()
}
