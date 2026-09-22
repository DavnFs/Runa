package id.rona.app.data.reminder

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import id.rona.app.data.db.dao.CyclePredictionDao
import id.rona.app.data.db.dao.SettingsDao
import id.rona.app.domain.model.PrivacyMode
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local reminder worker. Runs once per day (inexact) and posts reminders
 * only when enabled. Notification content honors the privacy mode:
 * GENERIC/TITLE_ONLY never mention menstruation or dates.
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val settingsDao: SettingsDao,
    private val predictionDao: CyclePredictionDao,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val reminder = settingsDao.getReminderSettings()
        val privacy = settingsDao.getPrivacySettings()?.notificationPrivacyMode
            ?: PrivacyMode.GENERIC

        if (reminder?.dailyLogReminderEnabled == true) {
            RunaNotifier.showDailyLogReminder(applicationContext, privacy)
        }

        if (reminder?.periodReminderEnabled == true) {
            val prediction = predictionDao.getLatest()
            if (prediction != null) {
                val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.LocalDate.now(),
                    java.time.LocalDate.ofEpochDay(prediction.predictedStartEpochDay),
                )
                if (daysUntil >= 0 && daysUntil <= reminder.periodReminderDaysBefore) {
                    RunaNotifier.showPeriodReminder(
                        context = applicationContext,
                        privacyMode = privacy,
                        daysUntil = daysUntil.toInt(),
                    )
                }
            }
        }

        return Result.success()
    }

    @Singleton
    class Scheduler @Inject constructor() {

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        companion object {
            const val WORK_NAME = "rona_daily_reminder"
        }
    }
}
