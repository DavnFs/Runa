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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.rona.app.domain.model.PrivacyMode

@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Notifikasi", style = MaterialTheme.typography.headlineSmall)

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Pengingat perkiraan periode", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = uiState.periodReminderEnabled,
                onCheckedChange = viewModel::setPeriodReminder,
            )
        }

        Text("Berapa hari sebelumnya", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = uiState.periodReminderDaysBefore.toFloat(),
            onValueChange = { viewModel.setPeriodReminderDays(it.toInt()) },
            valueRange = 0f..7f,
            steps = 6,
        )
        Text("${uiState.periodReminderDaysBefore} hari", style = MaterialTheme.typography.bodySmall)

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Pengingat catatan harian", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = uiState.dailyLogReminderEnabled,
                onCheckedChange = viewModel::setDailyLogReminder,
            )
        }

        Spacer(Modifier.height(8.dp))
        Text("Mode privasi notifikasi", style = MaterialTheme.typography.titleMedium)
        Text(
            "Kontrol apa yang terlihat di layar kunci bila notifikasi muncul.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        listOf(
            PrivacyMode.GENERIC to "Umum — tanpa kata sensitif (default)",
            PrivacyMode.TITLE_ONLY to "Judul saja",
            PrivacyMode.FULL to "Lengkap — menyebut tanggal & konteks",
        ).forEach { (mode, label) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setNotificationPrivacyMode(mode) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                if (uiState.notificationPrivacyMode == mode) {
                    Text("✓", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
