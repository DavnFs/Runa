package id.rona.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.rona.app.ui.components.RonaLoadingSkeleton
import id.rona.app.ui.theme.LocalRonaColors

@Composable
fun SettingsScreen(
    onSecurity: () -> Unit,
    onNotifications: () -> Unit,
    onAppearance: () -> Unit,
    onBackup: () -> Unit,
    onDelete: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        RonaLoadingSkeleton(modifier = modifier.fillMaxSize(), message = "Memuat pengaturan…")
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Pengaturan", style = MaterialTheme.typography.headlineSmall)

        PrivacyReassuranceBanner()

        SettingsGroup(title = "Privasi & keamanan") {
            SettingsRow(Icons.Rounded.Lock, "Keamanan", "PIN, biometrik, kunci otomatis", onSecurity)
            SettingsRow(Icons.Rounded.Notifications, "Notifikasi", "Pengingat & privasi notifikasi", onNotifications)
            SettingsRow(Icons.Rounded.Storage, "Backup", "Export & restore terenkripsi", onBackup)
            SettingsRow(Icons.Rounded.Shield, "Data & privasi", "Hapus data lokal", onDelete)
        }

        SettingsGroup(title = "Aplikasi") {
            SettingsRow(Icons.Rounded.Palette, "Tampilan", "Tema & warna", onAppearance)
            SettingsRow(Icons.Rounded.Badge, "Kebijakan privasi", "Tanpa akun, tanpa cloud, tanpa pelacak", onPrivacyPolicy)
            SettingsRow(Icons.Rounded.Info, "Tentang rona", "Versi & informasi", onAbout)
        }
    }
}

@Composable
private fun PrivacyReassuranceBanner() {
    val colors = LocalRonaColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = colors.privacySurface,
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Shield,
                contentDescription = null,
                tint = colors.sageAccent,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "Data disimpan dan dienkripsi di perangkat ini.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showDivider: Boolean = true,
) {
    val colors = LocalRonaColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = colors.cyclePrimary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = colors.dividerSubtle,
        )
    }
}
