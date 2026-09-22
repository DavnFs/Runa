package id.rona.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.rona.app.domain.model.ThemeMode
import id.rona.app.ui.components.RunaSelectableChip
import id.rona.app.ui.theme.LocalRonaColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalRonaColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Tampilan", style = MaterialTheme.typography.headlineSmall)

        Text(
            "Tema",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                ThemeMode.SYSTEM to "Ikuti sistem",
                ThemeMode.LIGHT to "Terang",
                ThemeMode.DARK to "Gelap",
            ).forEach { (mode, label) ->
                RunaSelectableChip(
                    label = label,
                    selected = uiState.themeMode == mode,
                    onClick = { viewModel.setThemeMode(mode) },
                )
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Izinkan screenshot", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Default: diblokir demi privasi (tampilan kabur di daftar aplikasi terbaru).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(0.dp))
            Switch(
                checked = uiState.allowScreenshots,
                onCheckedChange = viewModel::setAllowScreenshots,
            )
        }
    }
}
