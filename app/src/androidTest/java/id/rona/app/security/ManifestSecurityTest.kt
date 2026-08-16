package id.rona.app.security

import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManifestSecurityTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val packageInfo = context.packageManager.getPackageInfo(
        context.packageName,
        PackageManager.GET_PERMISSIONS,
    )

    @Test
    fun noInternetPermission() {
        val permissions = packageInfo.requestedPermissions ?: emptyArray()
        assertThat(permissions.toList()).doesNotContain("android.permission.INTERNET")
    }

    @Test
    fun noCloudOrTrackingPermissions() {
        val permissions = packageInfo.requestedPermissions ?: emptyArray()
        assertThat(permissions.toList()).containsNoneOf(
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.READ_PHONE_STATE",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
        )
    }

    @Test
    fun allowBackupDisabled() {
        val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
        val flags = appInfo.flags
        assertThat(flags and android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP).isEqualTo(0)
    }

    @Test
    fun onlyExpectedPermissions() {
        val permissions = (packageInfo.requestedPermissions ?: emptyArray()).toList()
        val allowed = setOf(
            "android.permission.POST_NOTIFICATIONS",
        )
        val unexpected = permissions.filterNot { it in allowed }
        assertThat(unexpected).isEmpty()
    }
}
