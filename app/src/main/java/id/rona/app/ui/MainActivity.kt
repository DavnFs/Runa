package id.rona.app.ui

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import id.rona.app.data.AppLockManager
import id.rona.app.domain.model.ThemeMode
import id.rona.app.ui.lock.LockGateScreen
import id.rona.app.ui.navigation.RunaNavHost
import id.rona.app.ui.settings.SettingsViewModel
import id.rona.app.ui.theme.RunaTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appLockManager: AppLockManager

    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applySecureFlag(false)

        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> appLockManager.onAppStopped()
                Lifecycle.Event.ON_START -> appLockManager.onAppStarted()
                else -> Unit
            }
        })

        setContent {
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            RunaTheme(themeMode = settingsState.themeMode) {
                // Bar glyphs follow the theme the app is actually painting,
                // not the system dark-mode setting (BitChord's SystemBarIcons).
                val darkTheme = when (settingsState.themeMode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }
                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as? Activity)?.window ?: return@SideEffect
                        WindowCompat.getInsetsController(window, view).apply {
                            isAppearanceLightStatusBars = !darkTheme
                            isAppearanceLightNavigationBars = !darkTheme
                        }
                    }
                }

                val isUnlocked by appLockManager.unlocked.collectAsStateWithLifecycle()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    if (isUnlocked) {
                        RunaNavHost()
                    } else {
                        LockGateScreen(onUnlocked = appLockManager::onUnlocked)
                    }
                }
            }
        }
    }

    private fun applySecureFlag(allowScreenshots: Boolean) {
        if (!allowScreenshots) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
