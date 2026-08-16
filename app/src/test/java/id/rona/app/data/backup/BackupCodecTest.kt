package id.rona.app.data.backup

import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Test

class BackupCodecTest {

    private val codec = BackupCodec()

    private fun samplePayload() = BackupPayload(
        appVersion = "0.1.0",
        exportedAt = 123456789L,
        profile = BackupProfile(defaultCycleLengthDays = 28, onboardingCompletedAt = 111L),
        periods = listOf(
            BackupPeriod(startEpochDay = 19000, endEpochDay = 19004, createdAt = 1L, updatedAt = 2L),
            BackupPeriod(startEpochDay = 19028, endEpochDay = null, createdAt = 3L, updatedAt = 3L),
        ),
        dailyLogs = listOf(
            BackupDailyLog(
                dateEpochDay = 19001,
                flow = "MEDIUM",
                mood = "GOOD",
                energy = "NEUTRAL",
                note = "catatan pribadi",
                createdAt = 4L,
                updatedAt = 5L,
                symptoms = listOf(BackupSymptom("KRAM", "MODERATE")),
            ),
        ),
        reminder = BackupReminder(
            periodReminderEnabled = true,
            periodReminderDaysBefore = 2,
            periodReminderMinuteOfDay = 1200,
            dailyLogReminderEnabled = false,
            dailyLogMinuteOfDay = 1260,
            vibrationEnabled = true,
        ),
        privacy = BackupPrivacy(
            notificationPrivacyMode = "GENERIC",
            allowScreenshots = false,
            themeMode = "SYSTEM",
        ),
    )

    @Test
    fun roundTripPreservesPayload() {
        val out = ByteArrayOutputStream()
        codec.export(samplePayload(), "passphrase-kuat", out)
        val bytes = out.toByteArray()

        val restored = codec.restore(ByteArrayInputStream(bytes), "passphrase-kuat")
        assertThat(restored.payload.periods).hasSize(2)
        assertThat(restored.payload.dailyLogs.single().note).isEqualTo("catatan pribadi")
        assertThat(restored.payload.dailyLogs.single().symptoms.single().symptomType)
            .isEqualTo("KRAM")
        assertThat(restored.payload.profile!!.defaultCycleLengthDays).isEqualTo(28)
    }

    @Test
    fun wrongPassphraseFails() {
        val out = ByteArrayOutputStream()
        codec.export(samplePayload(), "passphrase-benar", out)

        val exception = runCatching {
            codec.restore(ByteArrayInputStream(out.toByteArray()), "passphrase-salah")
        }.exceptionOrNull()

        assertThat(exception).isNotNull()
    }

    @Test
    fun tamperedFileFailsIntegrityCheck() {
        val out = ByteArrayOutputStream()
        codec.export(samplePayload(), "passphrase-kuat", out)
        val bytes = out.toByteArray()

        // Flip one byte near the end (inside ciphertext/tag region).
        val tampered = bytes.copyOf()
        tampered[tampered.size - 1] = (tampered[tampered.size - 1].toInt() xor 0xFF).toByte()

        val exception = runCatching {
            codec.restore(ByteArrayInputStream(tampered), "passphrase-kuat")
        }.exceptionOrNull()

        assertThat(exception).isNotNull()
    }

    @Test
    fun shortPassphraseRejected() {
        val exception = runCatching {
            codec.export(samplePayload(), "pendek", ByteArrayOutputStream())
        }.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun nonRonaFileRejected() {
        val exception = runCatching {
            codec.restore(ByteArrayInputStream("BUKANBACKUP".toByteArray()), "passphrase-kuat")
        }.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun saltIsUniquePerExport() {
        val outA = ByteArrayOutputStream()
        val outB = ByteArrayOutputStream()
        codec.export(samplePayload(), "passphrase-kuat", outA)
        codec.export(samplePayload(), "passphrase-kuat", outB)

        val bytesA = outA.toByteArray()
        val bytesB = outB.toByteArray()
        // Headers contain different salts; ciphertexts differ even with same plaintext.
        assertThat(bytesA.contentEquals(bytesB)).isFalse()
    }
}
