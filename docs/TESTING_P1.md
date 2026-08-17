# Testing P1

## P2 FINAL — icon-only dock physical validation checklist

```bash
adb uninstall id.rona.app
adb install app/build/outputs/apk/debug/app-debug.apk
adb logcat -c
adb shell monkey -p id.rona.app -c android.intent.category.LAUNCHER 1
adb logcat -b crash -d -v threadtime
```

Urutan manual (Infinix GT 30 Pro / Android 16):
1. Fresh install atau upgrade tanpa kehilangan data.
2. Onboarding → Home Empty.
3. Home menampilkan CTA utama "Catat keadaanmu hari ini" (Empty & Success).
4. Beranda ↔ Kalender ↔ Insight berulang:
   - satu active capsule bergerak mulus antar 3 icon (200ms),
   - dock tidak berubah ukuran,
   - navigasi langsung jalan.
5. Long-press tiap icon → tooltip "Beranda"/"Kalender"/"Insight" muncul.
6. TalkBack: tiap icon punya label + "tab dipilih" + Role.Tab.
7. CTA Home membuka LogEditorSheet; dock tidak menutupi CTA.
8. Dock di font 1.0×/1.3×/1.5×: tidak ada perubahan (icon-only).
9. Dock di 320dp/360dp: target tetap >=48dp, tidak ada clipping.
10. Gesture nav & 3-button: spacing benar, tidak menutupi konten.
11. Dark mode: kontras valid.
12. Force-stop → relaunch; PIN/app lock tetap.
13. `adb logcat -b crash -d` kosong.
14. Merged manifest debug & release tanpa INTERNET.

Arsitektur final dock:
- 3 destination icon-only: Beranda (Home), Kalender (CalendarMonth),
  Insight (AutoGraph).
- Satu indicator capsule bergerak (posisi + lebar) — bukan per-item.
- Tidak ada label permanen, tombol Catat, companion button, atau FAB.
- Label via tooltip long-press + contentDescription + selected semantics.

## P2 Interaction & responsive — physical validation checklist

```bash
adb uninstall id.rona.app
adb install app/build/outputs/apk/debug/app-debug.apk
adb logcat -c
adb shell monkey -p id.rona.app -c android.intent.category.LAUNCHER 1
adb logcat -b crash -d -v threadtime
```

Urutan manual (Infinix GT 30 Pro / Android 16):
1. Fresh install atau upgrade tanpa kehilangan data.
2. Onboarding → Home Empty.
3. Catat periode → Home Success.
4. Beranda ↔ Kalender berulang kali:
   - indicator aktif bergerak mulus (200ms),
   - tidak ada layout jump,
   - label tetap terbaca,
   - navigasi langsung jalan (tidak menunggu animasi).
5. Tap Catat: feedback tekan halus, LogEditorSheet terbuka, dock tidak
   mengganggu.
6. Buka Log Editor di font normal, 1.3×, 1.5×, dan saat keyboard terbuka:
   - CTA "Simpan catatan" tetap terjangkau (imePadding),
   - chips gejala wrap, tidak terpotong.
7. Settings: tidak ada overlap/trailing terpotong; dark mode; dynamic
   color tetap OFF secara default.
8. Insight: selected state companion jelas (rose ring/fill).
9. Gesture navigation & 3-button: dock spacing benar, tidak menutupi
   konten.
10. Force-stop → relaunch; PIN/app lock tetap bekerja.
11. `adb logcat -b crash -d` kosong.
12. Merged manifest debug & release tanpa INTERNET.

Catatan perilaku dock (final):
- Beranda/Kalender text-first (tanpa icon) di mode normal; icon muncul
  hanya di compact (<360dp).
- Satu active capsule bergerak (posisi + lebar) — bukan per-item.
- Label tidak pernah hilang; unselected alpha 0.72.
- Catat selalu ikon pensil + label; Insight companion + label kecil.

## P1 Visual Design System — physical validation checklist

```bash
adb uninstall id.rona.app
adb install app/build/outputs/apk/debug/app-debug.apk
adb logcat -c
adb shell monkey -p id.rona.app -c android.intent.category.LAUNCHER 1
adb logcat -b crash -d -v threadtime
```

Urutan manual (Infinix GT 30 Pro / Android 16):
1. Fresh install (atau upgrade tanpa kehilangan data).
2. Onboarding.
3. Home harus Empty (tanpa data) — hero ring "Belum ada data".
4. Catat periode → Home Success (ring "Hari ke-N").
5. Buka Kalender → sel actual solid rose; prediksi outline; dot log.
6. Ketuk hari → bottom sheet detail muncul.
7. Dari dock: "Catat" → LogEditorSheet terbuka; simpan catatan.
8. "Insight" (companion) → halaman Insight; "Pola akan muncul perlahan".
9. Dark mode → dock tetap kontras (plum lebih terang dari canvas).
10. Font besar (1.3×) → tidak ada overlap/tab terpotong.
11. Gesture nav & 3-button nav → dock tidak menutupi konten (bottom inset).
12. Force-stop → relaunch → data & lock tetap.
13. App lock on/off → unlock lalu Home tetap bekerja.
14. Tidak ada crash startup (SQLCipher/StrongBox) di `logcat -b crash`.

Catatan: dock sengaja gelap (ink/plum) di light theme — ini keputusan desain
(identitas), bukan bug. Dynamic color default OFF (palet Rona).

## Test commands

```bash
# Unit test (semua, termasuk P1 + StrongBox policy)
./gradlew testDebugUnitTest

# Hanya StrongBox hotfix
./gradlew testDebugUnitTest --tests "id.rona.app.data.crypto.*"

# Build debug
./gradlew assembleDebug

# Lint
./gradlew lintDebug

# R8 release
./gradlew assembleRelease

# Instrumented (butuh device/emulator — tidak bisa dijalankan di CI tanpa KVM)
./gradlew connectedDebugAndroidTest
```

## Hotfix SQLCipher native loader — physical device acceptance

```bash
adb uninstall id.rona.app
adb install app/build/outputs/apk/debug/app-debug.apk
adb logcat -c
adb shell monkey -p id.rona.app -c android.intent.category.LAUNCHER 1
adb logcat -b crash -d -v threadtime
```

Kriteria lulus (Infinix GT 30 Pro, Android 16, arm64-v8a):
1. `monkey` membuka aplikasi tanpa crash (`logcat -b crash` kosong).
2. Tidak ada `StrongBoxUnavailableException`.
3. Tidak ada `SQLiteConnection.nativeOpen` UnsatisfiedLinkError.
4. Onboarding/Home tercapai; database terinisialisasi.
5. Force-stop lalu relaunch — aplikasi tetap jalan.
6. Backup terenkripsi, PIN/app lock, dan settings tetap berfungsi.
7. Airplane mode: seluruh alur tetap berfungsi.
8. Merged manifest debug & release tetap tanpa INTERNET.

## Hotfix StrongBox — physical device acceptance

```bash
adb uninstall id.rona.app
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p id.rona.app -c android.intent.category.LAUNCHER 1
adb logcat -b crash -d -v threadtime
```

Kriteria lulus (device TANPA StrongBox — mis. Infinix GT 30 Pro):
1. `monkey` membuka aplikasi tanpa crash (logcat -b crash kosong).
2. Onboarding muncul; database terinisialisasi.
3. Kill & relaunch — aplikasi tetap jalan (key reuse, bukan regenerasi).
4. App lock / PIN flow tetap berfungsi.
5. Tidak ada file plaintext baru (hanya `db.key.enc` + `rona.db` SQLCipher).
6. Debug log memuat "StrongBox unavailable, using Android Keystore fallback"
   bila device mengiklankan feature tapi StrongBox gagal.
7. Airplane mode: seluruh alur tetap berfungsi.
8. Merged manifest debug & release tetap tanpa INTERNET.

Device DENGAN StrongBox (bila ada): aplikasi memakai StrongBox (tanpa
fallback); seluruh kriteria di atas tetap berlaku.

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
