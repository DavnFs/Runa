package id.rona.app.ui.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.rona.app.ui.MainScreen
import id.rona.app.ui.log.LogEditorSheet
import id.rona.app.ui.onboarding.OnboardingScreen
import id.rona.app.ui.settings.AboutScreen
import id.rona.app.ui.settings.AppearanceSettingsScreen
import id.rona.app.ui.settings.BackupExportScreen
import id.rona.app.ui.settings.BackupRestoreScreen
import id.rona.app.ui.settings.DataDeletionScreen
import id.rona.app.ui.settings.NotificationSettingsScreen
import id.rona.app.ui.settings.PrivacyPolicyScreen
import id.rona.app.ui.settings.SecuritySettingsScreen
import id.rona.app.ui.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RonaNavHost() {
    val navController = rememberNavController()
    var showLogSheet by rememberSaveable { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = Onboarding,
        enterTransition = { id.rona.app.ui.theme.RonaMotion.NavPushEnter },
        exitTransition = { id.rona.app.ui.theme.RonaMotion.NavPushExit },
        popEnterTransition = { id.rona.app.ui.theme.RonaMotion.NavPopEnter },
        popExitTransition = { id.rona.app.ui.theme.RonaMotion.NavPopExit },
    ) {
        composable<Onboarding> {
            OnboardingScreen(onFinished = {
                navController.navigate(Main) {
                    popUpTo(Onboarding) { inclusive = true }
                }
            })
        }
        composable<Main> {
            MainScreen(
                onLogToday = { showLogSheet = true },
                onOpenSettings = { navController.navigate(Settings) },
            )
        }
        composable<Settings> {
            SettingsScreen(
                onSecurity = { navController.navigate(SecuritySettings) },
                onNotifications = { navController.navigate(NotificationSettings) },
                onAppearance = { navController.navigate(AppearanceSettings) },
                onBackup = { navController.navigate(BackupExport) },
                onDelete = { navController.navigate(DataDeletion) },
                onPrivacyPolicy = { navController.navigate(PrivacyPolicy) },
                onAbout = { navController.navigate(About) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<SecuritySettings> { SecuritySettingsScreen(onBack = { navController.popBackStack() }) }
        composable<NotificationSettings> { NotificationSettingsScreen(onBack = { navController.popBackStack() }) }
        composable<AppearanceSettings> { AppearanceSettingsScreen(onBack = { navController.popBackStack() }) }
        composable<BackupExport> { BackupExportScreen(onBack = { navController.popBackStack() }) }
        composable<BackupRestore> { BackupRestoreScreen(onBack = { navController.popBackStack() }) }
        composable<DataDeletion> { DataDeletionScreen(onBack = { navController.popBackStack() }) }
        composable<PrivacyPolicy> { PrivacyPolicyScreen(onBack = { navController.popBackStack() }) }
        composable<About> { AboutScreen(onBack = { navController.popBackStack() }) }
    }

    if (showLogSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showLogSheet = false },
            sheetState = sheetState,
        ) {
            LogEditorSheet(onDismiss = { showLogSheet = false })
        }
    }
}
