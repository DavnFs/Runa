package id.rona.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminder_settings")
data class ReminderSettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val periodReminderEnabled: Boolean = true,
    val periodReminderDaysBefore: Int = 2,
    val periodReminderMinuteOfDay: Int = 20 * 60,
    val dailyLogReminderEnabled: Boolean = false,
    val dailyLogMinuteOfDay: Int = 21 * 60,
    val vibrationEnabled: Boolean = true,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
