package id.rona.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import id.rona.app.domain.model.PrivacyMode
import id.rona.app.domain.model.ThemeMode

@Entity(tableName = "privacy_settings")
data class PrivacySettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val notificationPrivacyMode: PrivacyMode = PrivacyMode.GENERIC,
    val allowScreenshots: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
