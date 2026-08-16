package id.rona.app.data.repository

import id.rona.app.data.db.dao.PeriodRecordDao
import id.rona.app.data.db.dao.ProfileDao
import id.rona.app.data.db.entity.LocalProfileEntity
import id.rona.app.data.db.entity.PeriodRecordEntity
import id.rona.app.data.db.entity.ReminderSettingsEntity
import id.rona.app.data.db.dao.SettingsDao
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val periodRecordDao: PeriodRecordDao,
    private val settingsDao: SettingsDao,
) {

    fun observeProfile(): Flow<LocalProfileEntity?> = profileDao.observe()

    suspend fun getProfile(): LocalProfileEntity? = profileDao.get()

    suspend fun completeOnboarding(
        lastPeriodStart: LocalDate,
        lastPeriodEnd: LocalDate?,
        defaultCycleLengthDays: Int?,
        nowMs: Long = System.currentTimeMillis(),
    ) {
        profileDao.upsert(
            LocalProfileEntity(
                id = LocalProfileEntity.SINGLETON_ID,
                defaultCycleLengthDays = defaultCycleLengthDays,
                onboardingCompletedAt = nowMs,
                createdAt = nowMs,
            )
        )
        periodRecordDao.upsert(
            PeriodRecordEntity(
                startEpochDay = lastPeriodStart.toEpochDay(),
                endEpochDay = lastPeriodEnd?.toEpochDay(),
                createdAt = nowMs,
                updatedAt = nowMs,
            )
        )
        settingsDao.upsert(ReminderSettingsEntity())
    }

    suspend fun isOnboardingCompleted(): Boolean =
        profileDao.get()?.onboardingCompletedAt != null
}
