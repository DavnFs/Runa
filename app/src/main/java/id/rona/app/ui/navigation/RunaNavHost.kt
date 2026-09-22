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
import id.rona.app.ui.log.FullLogEditorSheet
import id.rona.app.ui.log.GuidedCheckInSheet
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
import id.rona.app.ui.theme.RunaBottomSheetShape
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunaNavHost() {
    val navController = rememberNavController()
    // null == sheet closed; non-null epochDay == sheet open for that calendar day.
    var guidedLogDateEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var guidedLogSessionId by rememberSaveable { mutableStateOf(0L) }
    var fullLogDateEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var fullLogSessionId by rememberSaveable { mutableStateOf(0L) }
    var showFullLogSheet by rememberSaveable { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = Onboarding,
        enterTransition = { id.rona.app.ui.theme.RunaMotion.NavPushEnter },
        exitTransition = { id.rona.app.ui.theme.RunaMotion.NavPushExit },
        popEnterTransition = { id.rona.app.ui.theme.RunaMotion.NavPopEnter },
        popExitTransition = { id.rona.app.ui.theme.RunaMotion.NavPopExit },
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
                onLogToday = {
                    guidedLogSessionId += 1
                    guidedLogDateEpochDay = LocalDate.now().toEpochDay()
                },
                onLogDate = { date ->
                    guidedLogSessionId += 1
                    guidedLogDateEpochDay = date.toEpochDay()
                },
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

    val guidedLogOpen = guidedLogDateEpochDay != null
    if (guidedLogOpen) {
        val sheetDate = guidedLogDateEpochDay?.let { LocalDate.ofEpochDay(it) }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { guidedLogDateEpochDay = null },
            sheetState = sheetState,
            shape = RunaBottomSheetShape,
        ) {
            GuidedCheckInSheet(
                date = sheetDate,
                sessionId = guidedLogSessionId,
                onDismiss = { guidedLogDateEpochDay = null },
                onOpenFullEditor = {
                    fullLogSessionId += 1
                    fullLogDateEpochDay = sheetDate?.toEpochDay()
                    guidedLogDateEpochDay = null
                    showFullLogSheet = true
                },
            )
        }
    }

    if (showFullLogSheet) {
        val fullSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showFullLogSheet = false },
            sheetState = fullSheetState,
            shape = RunaBottomSheetShape,
        ) {
            FullLogEditorSheet(
                date = fullLogDateEpochDay?.let { LocalDate.ofEpochDay(it) },
                sessionId = fullLogSessionId,
                onDismiss = { showFullLogSheet = false },
            )
        }
    }
}
