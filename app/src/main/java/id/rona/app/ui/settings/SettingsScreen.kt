package id.rona.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.rona.app.ui.components.RonaLoadingSkeleton
import id.rona.app.ui.theme.LocalRonaColors
import id.rona.app.ui.theme.RonaTheme
import id.rona.app.ui.theme.RonaWordmarkStyle

@Composable
fun SettingsScreen(
    onSecurity: () -> Unit,
    onNotifications: () -> Unit,
    onAppearance: () -> Unit,
    onBackup: () -> Unit,
    onDelete: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onAbout: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalRonaColors.current

    if (uiState.isLoading) {
        RonaLoadingSkeleton(modifier = modifier.fillMaxSize(), message = "Memuat pengaturan…")
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Top App Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            color = Color.Transparent,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.CenterStart),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Kembali",
                            tint = colors.inkSecondary,
                        )
                    }
                }

                Text(
                    text = "RUNA",
                    style = RonaWordmarkStyle,
                    color = colors.cyclePrimary,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Heading: "Pengaturan"
            Text(
                text = "Pengaturan",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary,
                ),
            )

            // Privacy Card ("Privasi Utama")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = colors.privacySurface,
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFBF3F4),
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = colors.cyclePrimary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Privasi Utama",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                color = colors.inkPrimary,
                            ),
                        )
                        Text(
                            text = "Data Anda disimpan secara lokal dan dienkripsi. Kami tidak menjual atau membagikan informasi kesehatan pribadi Anda.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = colors.inkSecondary,
                            ),
                        )
                    }
                }
            }

            // Group 1: AKUN & PRIVASI
            FigmaSettingsGroup(
                headerText = "AKUN & PRIVASI",
            ) {
                FigmaSettingsItem(
                    icon = Icons.Rounded.Person,
                    title = "Profil Saya",
                    onClick = onSecurity,
                )
                FigmaSettingsItem(
                    icon = Icons.Rounded.Lock,
                    title = "Keamanan PIN",
                    onClick = onSecurity,
                )
                FigmaSettingsItem(
                    icon = Icons.Rounded.Sync,
                    title = "Cadangkan Data",
                    onClick = onBackup,
                )
            }

            // Group 2: TAMPILAN & PENGALAMAN
            FigmaSettingsGroup(
                headerText = "TAMPILAN & PENGALAMAN",
            ) {
                FigmaSettingsItem(
                    icon = Icons.Rounded.DarkMode,
                    title = "Tema Gelap",
                    trailingValue = "Otomatis",
                    onClick = onAppearance,
                )
                FigmaSettingsItem(
                    icon = Icons.Rounded.Notifications,
                    title = "Pengingat",
                    onClick = onNotifications,
                )
            }

            // Group 3: INFORMASI
            FigmaSettingsGroup(
                headerText = "INFORMASI",
            ) {
                FigmaSettingsItem(
                    icon = Icons.Rounded.Info,
                    title = "Tentang Aplikasi",
                    onClick = onAbout,
                )
                FigmaSettingsItem(
                    icon = Icons.AutoMirrored.Rounded.HelpOutline,
                    title = "Pusat Bantuan",
                    onClick = onPrivacyPolicy,
                )
            }

            // Destructive Action: Hapus Semua Data
            Surface(
                onClick = onDelete,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Hapus Semua Data",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.error,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FigmaSettingsGroup(
    headerText: String,
    content: @Composable () -> Unit,
) {
    val colors = LocalRonaColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header bar
            Surface(
                color = colors.surfaceSoft,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = colors.inkTertiary,
                    ),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun FigmaSettingsItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    trailingValue: String? = null,
) {
    val colors = LocalRonaColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.inkSecondary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                color = colors.inkPrimary,
            ),
            modifier = Modifier.weight(1f),
        )
        if (trailingValue != null) {
            Text(
                text = trailingValue,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    color = colors.inkSecondary,
                ),
            )
            Spacer(Modifier.width(8.dp))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFD7C1C4),
            modifier = Modifier.size(20.dp),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Settings Screen — Light", showBackground = true)
@Composable
private fun SettingsScreenLightPreview() {
    RonaTheme {
        SettingsScreen(
            onSecurity = {},
            onNotifications = {},
            onAppearance = {},
            onBackup = {},
            onDelete = {},
            onPrivacyPolicy = {},
            onAbout = {},
            onBack = {},
        )
    }
}
