package id.rona.app.data.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import id.rona.app.domain.model.PrivacyMode
import id.rona.app.R

object RonaNotifier {

    const val CHANNEL_PERIOD = "rona_reminder_period"
    const val CHANNEL_DAILY_LOG = "rona_reminder_daily_log"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PERIOD,
                "Pengingat periode",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Pengingat perkiraan periode berikutnya"
                setSound(null, null)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DAILY_LOG,
                "Pengingat catatan harian",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Pengingat mencatat hari ini"
                setSound(null, null)
            }
        )
    }

    fun showDailyLogReminder(context: Context, privacyMode: PrivacyMode) {
        val (title, text) = when (privacyMode) {
            PrivacyMode.GENERIC -> "rona" to "Satu menit untuk mencatat hari ini."
            PrivacyMode.TITLE_ONLY -> "rona" to null
            PrivacyMode.FULL -> "rona" to "Jangan lupa mencatat hari ini."
        }
        notify(context, CHANNEL_DAILY_LOG, 2, title, text)
    }

    fun showPeriodReminder(context: Context, privacyMode: PrivacyMode, daysUntil: Int) {
        val (title, text) = when (privacyMode) {
            PrivacyMode.GENERIC -> "rona" to "Ada pengingat kecil untukmu."
            PrivacyMode.TITLE_ONLY -> "rona" to null
            PrivacyMode.FULL -> "rona" to when (daysUntil) {
                0 -> "Perkiraan periode mulai hari ini."
                1 -> "Perkiraan periode mulai besok."
                else -> "Perkiraan periode mulai dalam $daysUntil hari."
            }
        }
        notify(context, CHANNEL_PERIOD, 1, title, text)
    }

    private fun notify(
        context: Context,
        channel: String,
        notificationId: Int,
        title: String,
        text: String?,
    ) {
        ensureChannels(context)
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification_mark)
            .setContentTitle(title)
            .setAutoCancel(true)
            .setShowWhen(false)
            .setOngoing(false)
        if (text != null) {
            builder.setContentText(text)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }
}
