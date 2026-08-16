package id.rona.app.ui.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.rona.app.domain.model.LockOutcome
import id.rona.app.ui.components.PinPad
import id.rona.app.ui.components.PinDots

@Composable
fun LockGateScreen(
    onUnlocked: () -> Unit,
    viewModel: LockGateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val outcome by viewModel.outcome.collectAsState()

    LaunchedEffect(outcome) {
        when (outcome) {
            LockOutcome.UNLOCKED -> onUnlocked()
            LockOutcome.LOCKOUT -> { /* PIN locked out; app stays locked */ }
            else -> Unit
        }
        viewModel.onOutcomeHandled()
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LaunchedEffect(uiState.isLocked) {
        if (!uiState.isLocked) {
            viewModel.onUnlockSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "rona",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Masukkan PIN untuk membuka",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))

        var enteredPin by remember { mutableStateOf("") }

        PinDots(pinLength = uiState.pinLength, filled = enteredPin.length)

        Spacer(Modifier.height(16.dp))

        Text(
            text = when (outcome) {
                LockOutcome.PIN_INCORRECT -> "PIN salah. ${uiState.attemptsLeft} percobaan tersisa."
                LockOutcome.AUTHENTICATION_FAILED -> "Autentikasi gagal. Coba lagi."
                else -> ""
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )

        Spacer(Modifier.height(8.dp))

        PinPad(
            onDigit = { enteredPin = (enteredPin + it).take(uiState.pinLength) },
            onBackspace = { enteredPin = enteredPin.dropLast(1) },
            enabled = outcome != LockOutcome.LOCKOUT,
        )

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = viewModel::onForgotPinRequested) {
            Text("Lupa PIN?")
        }

        LaunchedEffect(enteredPin) {
            if (enteredPin.length == uiState.pinLength) {
                viewModel.submitPin(enteredPin)
                enteredPin = ""
            }
        }
    }

    if (uiState.showForgotPinDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissForgotPinDialog,
            title = { Text("Lupa PIN?") },
            text = {
                Text(
                    "Demi privasimu, PIN tidak bisa di-reset. Satu-satunya jalan " +
                        "adalah menghapus seluruh data aplikasi."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissForgotPinDialog) {
                    Text("Mengerti")
                }
            },
        )
    }
}
