package id.rona.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.rona.app.data.db.dao.SettingsDao
import id.rona.app.data.db.entity.AppLockSettingsEntity
import id.rona.app.data.db.entity.PrivacySettingsEntity
import id.rona.app.data.db.entity.ReminderSettingsEntity
import id.rona.app.data.repository.AppLockRepository
import id.rona.app.domain.model.PrivacyMode
import id.rona.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = true,
    val biometricEnabled: Boolean = false,
    val lockEnabled: Boolean = false,
    val gracePeriodSeconds: Int = 30,
    val periodReminderEnabled: Boolean = true,
    val periodReminderDaysBefore: Int = 2,
    val dailyLogReminderEnabled: Boolean = false,
    val notificationPrivacyMode: PrivacyMode = PrivacyMode.GENERIC,
    val allowScreenshots: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDao: SettingsDao,
    private val appLockRepository: AppLockRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsDao.observeLockSettings(),
        settingsDao.observeReminderSettings(),
        settingsDao.observePrivacySettings(),
    ) { lock, reminder, privacy ->
        SettingsUiState(
            isLoading = false,
            biometricEnabled = lock?.biometricEnabled ?: false,
            lockEnabled = lock?.lockEnabled ?: false,
            gracePeriodSeconds = lock?.gracePeriodSeconds ?: 30,
            periodReminderEnabled = reminder?.periodReminderEnabled ?: true,
            periodReminderDaysBefore = reminder?.periodReminderDaysBefore ?: 2,
            dailyLogReminderEnabled = reminder?.dailyLogReminderEnabled ?: false,
            notificationPrivacyMode = privacy?.notificationPrivacyMode ?: PrivacyMode.GENERIC,
            allowScreenshots = privacy?.allowScreenshots ?: false,
            themeMode = privacy?.themeMode ?: ThemeMode.SYSTEM,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { appLockRepository.setBiometricEnabled(enabled) }
    }

    fun setGracePeriod(seconds: Int) {
        viewModelScope.launch { appLockRepository.setGracePeriodSeconds(seconds) }
    }

    fun setPeriodReminder(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsDao.getReminderSettings() ?: ReminderSettingsEntity()
            settingsDao.upsert(current.copy(periodReminderEnabled = enabled))
        }
    }

    fun setPeriodReminderDays(days: Int) {
        viewModelScope.launch {
            val current = settingsDao.getReminderSettings() ?: ReminderSettingsEntity()
            settingsDao.upsert(current.copy(periodReminderDaysBefore = days))
        }
    }

    fun setDailyLogReminder(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsDao.getReminderSettings() ?: ReminderSettingsEntity()
            settingsDao.upsert(current.copy(dailyLogReminderEnabled = enabled))
        }
    }

    fun setNotificationPrivacyMode(mode: PrivacyMode) {
        viewModelScope.launch {
            val current = settingsDao.getPrivacySettings() ?: PrivacySettingsEntity()
            settingsDao.upsert(current.copy(notificationPrivacyMode = mode))
        }
    }

    fun setAllowScreenshots(allow: Boolean) {
        viewModelScope.launch {
            val current = settingsDao.getPrivacySettings() ?: PrivacySettingsEntity()
            settingsDao.upsert(current.copy(allowScreenshots = allow))
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            val current = settingsDao.getPrivacySettings() ?: PrivacySettingsEntity()
            settingsDao.upsert(current.copy(themeMode = mode))
        }
    }
}
