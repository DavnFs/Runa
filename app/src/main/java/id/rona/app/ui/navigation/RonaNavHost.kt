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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RonaNavHost() {
    val navController = rememberNavController()
    var showLogSheet by rememberSaveable { mutableStateOf(false) }

    NavHost(navController = navController, startDestination = Onboarding) {
        composable<Onboarding> {
            OnboardingScreen(onFinished = {
                navController.navigate(Main) {
                    popUpTo(Onboarding) { inclusive = true }
                }
            })
        }
        composable<Main> {
            MainScreen(onLogToday = { showLogSheet = true })
        }
        composable<Settings> { /* M10 */ }
        composable<SecuritySettings> { /* M10 */ }
        composable<NotificationSettings> { /* M10 */ }
        composable<AppearanceSettings> { /* M10 */ }
        composable<BackupExport> { /* M10 */ }
        composable<BackupRestore> { /* M10 */ }
        composable<DataDeletion> { /* M10 */ }
        composable<PrivacyPolicy> { /* M10 */ }
        composable<About> { /* M10 */ }
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
