package id.rona.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.rona.app.ui.MainActivity
import id.rona.app.ui.home.HomeScreen
import id.rona.app.ui.onboarding.OnboardingScreen

@Composable
fun RonaNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Onboarding) {
        composable<Onboarding> {
            OnboardingScreen(onFinished = {
                navController.navigate(Main) {
                    popUpTo(Onboarding) { inclusive = true }
                }
            })
        }
        composable<Main> {
            HomeScreen()
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
}
