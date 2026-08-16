# Testing P1

## Test commands

```bash
# Unit test (semua, termasuk P1)
./gradlew testDebugUnitTest

# Hanya NLP
./gradlew testDebugUnitTest --tests "id.rona.app.domain.nlp.*"

# Build debug
./gradlew assembleDebug

# Lint
./gradlew lintDebug

# R8 release
./gradlew assembleRelease

# Instrumented (butuh device/emulator — tidak bisa dijalankan di CI tanpa KVM)
./gradlew connectedDebugAndroidTest
```

## Test fixture format

Semua fixture adalah string literal di test Kotlin (tidak ada file JSON
eksternal). Normalisasi selalu lewat `TextNormalizer.normalize(...)`;
pengujian extractor menerima normalized text.

## Manual tests on physical device/emulator

1. Instal `app/build/outputs/apk/debug/app-debug.apk`.
2. Aktifkan airplane mode — seluruh alur wajib berfungsi.
3. Onboarding -> Beranda -> Catat -> tulis "kram perut banget, pusing,
   mual dari pagi, susah tidur" -> "Analisis catatan".
4. Verifikasi: saran gejala muncul dengan checkbox; tidak ada yang
   tersimpan sebelum "Terapkan pilihan" + "Simpan".
5. Coba negasi: "tidak pusing tapi kram" -> HEADACHE tidak disarankan,
   KRAM disarankan.
6. Coba safety: "aku hamil dan keluar darah" -> kartu peringatan tampil
   PALING ATAS, bukan tips umum.
7. Coba self-harm wording -> kartu EMERGENCY tampil.
8. Dark mode + TalkBack: semua kartu terbaca, warna bukan satu-satunya
   pembeda (judul + icon + level).
9. Floating pill navigation tetap berfungsi normal.

## Security verification checklist

- [ ] `grep -c INTERNET` pada merged manifest debug & release = 0
      (bukan sekadar komentar).
- [ ] Tidak ada raw note di Logcat (PrivacyLogger no-op release).
- [ ] Analisis tidak dijadwalkan di WorkManager (hanya ReminderWorker).
- [ ] Backup round-trip masih hijau (`BackupCodecTest`).
- [ ] Tidak ada tabel baru / migrasi schema untuk P1.

## Offline verification steps

1. Airplane mode aktif.
2. Tulis catatan -> Analisis -> semua saran muncul (rule engine lokal).
3. Buka Insight -> statistik tetap ter-update.
4. Export backup -> file tersimpan via SAF.
5. Restore backup -> data kembali.
