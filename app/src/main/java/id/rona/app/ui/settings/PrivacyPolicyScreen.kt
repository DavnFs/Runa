package id.rona.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Kebijakan privasi", style = MaterialTheme.typography.headlineSmall)

        Text(
            "rona dirancang dengan satu prinsip: data kesehatanmu hanya milikmu.",
            style = MaterialTheme.typography.bodyLarge,
        )

        PolicySection(
            title = "Tidak ada akun",
            body = "Tidak ada email, nomor telepon, atau login. Aplikasi tidak mengenal siapa kamu.",
        )
        PolicySection(
            title = "Tidak ada cloud",
            body = "Semua data tersimpan terenkripsi di perangkatmu. Aplikasi tidak memiliki izin internet sama sekali.",
        )
        PolicySection(
            title = "Tidak ada pelacak",
            body = "Tidak ada analytics, iklan, atau SDK pihak ketiga. Tidak ada yang tahu bagaimana kamu memakai aplikasi ini.",
        )
        PolicySection(
            title = "Backup adalah pilihanmu",
            body = "Backup dibuat manual, dienkripsi dengan passphrase milikmu, dan tidak bisa dibuka siapa pun tanpa passphrase itu.",
        )
        PolicySection(
            title = "Bukan alat medis",
            body = "Perkiraan siklus adalah statistik sederhana dari catatanmu, bukan diagnosis dan bukan alat kontrasepsi. Konsultasikan tenaga kesehatan untuk keputusan medis.",
        )
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Tentang rona", style = MaterialTheme.typography.headlineSmall)
        Text("rona", style = MaterialTheme.typography.titleLarge)
        Text("Versi ${id.rona.app.BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
        Text(
            "Pelacak siklus pribadi, offline, tanpa jejak.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
