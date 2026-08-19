package id.rona.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.CircularProgressIndicator()
        }
        return
    }

    when (uiState.step) {
        OnboardingStep.WELCOME -> WelcomeStep(onNext = viewModel::nextStep)
        OnboardingStep.LOCK_SETUP -> LockSetupStep(
            state = uiState,
            onPinChange = viewModel::setPin,
            onConfirmPin = viewModel::confirmPin,
            onNext = viewModel::nextStep,
        )
        OnboardingStep.LAST_PERIOD -> LastPeriodStep(
            state = uiState,
            onStartChange = viewModel::setLastPeriodStart,
            onEndChange = viewModel::setLastPeriodEnd,
            onNext = viewModel::nextStep,
        )
        OnboardingStep.CYCLE_LENGTH -> CycleLengthStep(
            state = uiState,
            onCycleLengthChange = viewModel::setCycleLength,
            onNext = viewModel::nextStep,
        )
        OnboardingStep.NOTIFICATIONS -> NotificationsStep(
            state = uiState,
            onToggle = viewModel::setNotificationsEnabled,
            onNext = {
                viewModel.completeOnboarding()
            },
        )
        OnboardingStep.DONE -> Unit
    }

    // Already onboarded from a previous launch: skip directly.
    androidx.compose.runtime.LaunchedEffect(uiState.isOnboardingCompleted) {
        if (uiState.isOnboardingCompleted) onFinished()
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.Shield, contentDescription = null, modifier = Modifier.height(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Text("Selamat datang di Runa", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            "Semua data tinggal di ponselmu.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("• Tanpa akun", style = MaterialTheme.typography.bodyMedium)
            Text("• Tanpa cloud", style = MaterialTheme.typography.bodyMedium)
            Text("• Tanpa pelacak", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(48.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Mulai") }
    }
}

@Composable
private fun LockSetupStep(
    state: OnboardingUiState,
    onPinChange: (String) -> Unit,
    onConfirmPin: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.height(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Text("Kunci Runa", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            "Lindungi catatan pribadimu dengan PIN 4–6 digit. Kamu bisa mengaktifkan " +
                "sidik jari/wajah setelahnya.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        PinInputField(pin = state.pin, onPinChange = onPinChange)
        if (state.pinError != null) {
            Text(state.pinError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onConfirmPin, enabled = state.pin.length in 4..6) {
            Text(if (state.pinConfirmed) "PIN tersimpan ✓" else "Konfirmasi PIN")
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onNext) { Text("Lewati untuk sekarang") }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onNext) { Text("Lanjut") }
    }
}

@Composable
private fun PinInputField(pin: String, onPinChange: (String) -> Unit) {
    androidx.compose.material3.OutlinedTextField(
        value = pin,
        onValueChange = { onPinChange(it.filter { c -> c.isDigit() }.take(6)) },
        label = { Text("PIN") },
        singleLine = true,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LastPeriodStep(
    state: OnboardingUiState,
    onStartChange: (LocalDate) -> Unit,
    onEndChange: (LocalDate?) -> Unit,
    onNext: () -> Unit,
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.DateRange, contentDescription = null, modifier = Modifier.height(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Text("Kapan hari pertama menstruasi terakhirmu?", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            "Ini menjadi titik awal perhitungan siklus.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = { showStartPicker = true }) {
            Text(state.lastPeriodStart?.toString() ?: "Pilih tanggal mulai")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { showEndPicker = true }) {
            Text("Tanggal selesai (opsional): ${state.lastPeriodEnd ?: "belum diisi"}")
        }
        if (state.error != null) {
            Spacer(Modifier.height(8.dp))
            Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth(), enabled = state.lastPeriodStart != null) {
            Text("Lanjut")
        }

        if (showStartPicker) {
            val startPickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showStartPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        startPickerState.selectedDateMillis?.let { millis ->
                            onStartChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        showStartPicker = false
                    }) { Text("Pilih") }
                },
                dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("Batal") } },
            ) {
                DatePicker(state = startPickerState)
            }
        }
        if (showEndPicker) {
            val endPickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showEndPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        endPickerState.selectedDateMillis?.let { millis ->
                            onEndChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        showEndPicker = false
                    }) { Text("Pilih") }
                },
                dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("Batal") } },
            ) {
                DatePicker(state = endPickerState)
            }
        }
    }
}

@Composable
private fun CycleLengthStep(
    state: OnboardingUiState,
    onCycleLengthChange: (Int?) -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Berapa rata-rata panjang siklusmu?", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            "Tidak tahu? Tidak masalah. Runa akan menghitungnya setelah beberapa siklus tercatat.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(32.dp))
        val value = state.defaultCycleLengthDays ?: 28
        Slider(
            value = value.toFloat(),
            onValueChange = { onCycleLengthChange(it.toInt()) },
            valueRange = 21f..45f,
            steps = 23,
        )
        Text("${value} hari", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = { onCycleLengthChange(null) }) { Text("Saya tidak tahu") }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Lanjut") }
    }
}

@Composable
private fun NotificationsStep(
    state: OnboardingUiState,
    onToggle: (Boolean) -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.Notifications, contentDescription = null, modifier = Modifier.height(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Text("Pengingat yang tenang", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            "Kami bisa mengingatkan perkiraan periode mendatang — dengan cara yang tidak " +
                "menyebut apa pun secara terbuka.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Aktifkan pengingat", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = state.notificationsEnabled, onCheckedChange = onToggle)
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Selesai") }
    }
}
