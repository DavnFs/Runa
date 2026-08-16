# RONA — Technical Product Plan

> **Status:** Disetujui — implementasi dimulai. Revisi pengguna: keputusan seksi 16 dikunci.
> **Aplikasi:** Rona — private-first menstrual cycle tracker, Android-only, offline-only.
> **Bahasa dokumen:** Indonesia. **Bahasa kode/identifier:** Inggris. **Bahasa UI:** Indonesia (hangat, singkat, non-judgmental).
> **Keputusan terkunci:** minSdk 26 · FLAG_SECURE default aktif + toggle opt-in · app lock biometrik + PIN aplikasi dengan grace 30 detik.

**Non-goals yang mengikat semua keputusan di bawah:** bukan alat diagnosis medis, bukan alat kontrasepsi/fertility yang menjanjikan hasil, tidak ada akun, tidak ada cloud, tidak ada analytics/tracking SDK, tidak ada iklan, tidak ada chatbot/LLM online, tidak ada social/community, tidak ada pembagian data otomatis ke siapa pun.

---

## 1. Product Scope: MVP vs Post-MVP

### MVP (inti yang wajib)
- Onboarding: privacy promise, setup biometric + PIN, input periode terakhir, panjang siklus opsional, pilihan notifikasi discreet.
- App lock: BiometricPrompt (BIOMETRIC_STRONG) + PIN fallback, timeout/grace period, lock saat background.
- Beranda: hari siklus saat ini, prediksi periode berikutnya sebagai **rentang tanggal**, quick actions ("Mulai periode", "Selesaikan periode", "Log hari ini"), insight pendek yang dapat dijelaskan.
- Kalender: riwayat menstruasi (actual), prediksi (visual berbeda), detail per tanggal, tambah/edit/hapus rentang periode.
- Catat: flow (spotting/ringan/sedang/banyak), 10 gejala multi-select + severity, mood, energi, catatan bebas privat, hapus log.
- Insight: rata-rata panjang siklus, variasi siklus, durasi rata-rata, gejala tersering, pola per fase siklus, riwayat log, ringkasan tanpa diagnosis.
- Reminder lokal discreet (WorkManager), tanpa SCHEDULE_EXACT_ALARM.
- Settings: keamanan (PIN/biometrik/timeout), notifikasi + privacy mode, tema (dynamic color + palet fallback, dark mode), kebijakan privasi dalam aplikasi.
- Encrypted backup manual (export via SAF, passphrase pengguna) + restore + verifikasi.
- Hapus data per kategori + hapus semua (dengan konfirmasi kuat).
- **Tidak ada izin INTERNET di manifest sama sekali** — isolasi jaringan paling kuat yang bisa ditegakkan OS.

### Post-MVP (direncanakan, tidak dibangun sekarang)
- **P1 — NLP rule-based lokal** (synonym map Bahasa Indonesia, deterministik, tanpa ML).
- **P2 — NLP ONNX lokal** (IndoBERT Lite fine-tuned multi-label, INT8, ONNX Runtime Mobile, model dibundel di APK).
- **P3 — Partner/shared mode** (hanya desain dokumen; harus opt-in, granular per jenis data, dapat dicabut kapan saja; memerlukan desain E2E-encryption sendiri — **tidak** dibangun di MVP).
- Baseline Profiles / startup tuning, animasi halus tambahan, widget layar utama **hanya bila** berformat "terkunci/blank" (default tidak dibuat; widget bocor data via launcher/screenshot).
- Export ringkasan PDF (tetap lokal, tanpa auto-share).

**Eksplisit tidak pernah:** fertil window/ovulation prediction, diagnosa, cloud sync, telemedicine, multi-device.

---

## 2. User Flow Lengkap

### 2.1 Onboarding (sekali)
1. Buka aplikasi → **Splash + privacy statement singkat** ("Semua data tinggal di ponselmu").
2. **Welcome & privacy promise** — satu layar, bahasa manusia, tanpa jargon legal panjang (detail lengkap di Settings).
3. **Penjelasan penyimpanan lokal** — 3 poin: tidak ada akun, tidak ada cloud, backup adalah pilihanmu.
4. **Setup lock** — tawarkan biometrik bila tersedia; wajib set PIN 4–6 digit sebagai fallback. Bisa "Lewati" (dengan peringatan, bisa diaktifkan lagi di Settings) → keputusan #4.
5. **Hari pertama menstruasi terakhir** (date picker, wajib).
6. **Tanggal selesai** (opsional).
7. **Rata-rata panjang siklus** (opsional; default dipakai hanya sampai ada ≥3 siklus tercatat).
8. **Notifikasi discreet** — izin POST_NOTIFICATIONS (bisa dilewati), pilihan: pengingat pra-periode (default 2 hari sebelum, 20:00) + pengingat log harian (default 21:00, nonaktif).
9. Masuk ke **Beranda**.

### 2.2 Penggunaan harian
- Buka → lock gate (biometrik/PIN bila timeout terlewati) → Beranda.
- Periode mulai → tap **"Mulai periode"** → tersimpan `startDate`; Beranda beralih ke mode "periode berlangsung".
- Hari biasa → tap **"Log hari ini"** → bottom sheet: flow (opsional), gejala multi-select + severity, mood, energi, catatan. Simpan.
- Periode selesai → tap **"Selesaikan periode"** → set `endDate` → engine menghitung ulang prediksi & insight.
- Kalender → koreksi rentang (geser tanggal mulai/selesai, hapus record).
- Insight → lihat statistik; semua angka punya penjelasan satu kalimat ("Rata-rata dari 6 siklus terakhir").
- Settings → ubah PIN, ganti tema, atur notifikasi, export backup.

### 2.3 Backup & penghapusan
- Export: Settings → Export → pilih lokasi (SAF) → masukkan passphrase (min 8 karakter, konfirmasi ulang) → file `.rona` tersimpan → metadata export tercatat.
- Restore: Settings → Restore → pilih file → masukkan passphrase → validasi → **pratinjau jumlah data** → konfirmasi replace → data lama dihapus dalam transaksi → engine menghitung ulang prediksi.
- Hapus semua: Settings → Hapus semua data → dialog peringatan → ketik kata konfirmasi ("HAPUS") → hapus tabel + passphrase kunci tidak bisa dipulihkan → kembali ke onboarding. Tawarkan export terlebih dahulu di dialog.

---

## 3. Information Architecture & Navigation Map

```
MainActivity (single activity, edge-to-edge, FLAG_SECURE dikonfigurasi)
└── NavHost (type-safe routes, kotlinx-serialization)
    ├── LockGate            (selalu jadi gerbang saat proses hidup, termasuk kembali dari background)
    ├── Onboarding          (sub-step dalam satu route, satu ViewModel per step / satu flow VM)
    │     ├── WelcomePrivacy
    │     ├── LockSetup     (biometric + PIN)
    │     ├── LastPeriod    (start wajib, end opsional)
    │     ├── CycleLength   (opsional)
    │     └── Notifications (opsional, discreet)
    ├── Main                (4 tab)
    │     ├── Home          — Beranda
    │     ├── Calendar      — Kalender
    │     ├── (Log action)  — center action → LogEditorSheet
    │     └── Insights      — Insight
    └── Settings (stack)
          ├── SettingsHome
          ├── Security      (PIN, biometrik, timeout, izinkan screenshot)
          ├── Notifications (channel, jam, privacy mode)
          ├── Appearance    (theme, dynamic color)
          ├── BackupExport
          ├── BackupRestore
          ├── DataDeletion
          ├── PrivacyPolicy (lokal, in-app)
          └── About
```

Modal/overlay: `LogEditorSheet`, `CalendarDayDetailSheet`, `PeriodEditSheet`, `NlpSuggestionPanel` (P1+). State navigation dijaga sederhana: tab switch = `saveState/restoreState`, satu backstack untuk Settings.

---

## 4. Inventaris Screen

Setiap screen: tujuan, komponen utama, state (loading/empty/error), dan event utama.

| Screen | Tujuan | Komponen UI | States | Events utama |
|---|---|---|---|---|
| **LockGate** | Gerbang privasi saat launch & kembali dari background setelah grace period | Logo rona, indikator biometrik, `BiometricPrompt`, PIN pad (4–6 digit), link "lupa PIN → hapus data" | locked / authenticating / success / error(lockout sementara) | authenticate(bio), submitPin, cancel, forgotPin |
| **WelcomePrivacy** | Janji privasi di kesan pertama | Ilustrasi abstrak hangat, 3 bullet privasi, CTA "Lanjut" | — | continue |
| **LockSetup** | Wajibkan proteksi perangkat | `BiometricPrompt` enroll, PIN setup (input 2×), opsi lewati | biometricAvailable / pinEntry / pinConfirm / skipped | enableBiometric, setPin, skip |
| **LastPeriod** | Titik awal data siklus | `DatePicker` (Material 3), opsional end date, validasi (tidak di masa depan, rentang ≤ 15 hari) | default / invalid | save |
| **CycleLength** | Prior bila pengguna tahu | Slider/stepper 21–45, "tidak tahu" | default / skipped | save |
| **Notifications** | Izin notifikasi discreet | Toggle period reminder + hari-sebelum + jam, toggle log harian, privacy mode preview | permissionDenied / granted | requestPermission, save |
| **Home (Beranda)** | Jawab "hari ini aku di fase apa, dan apa yang perlu kutahu" | Kartu hari siklus (angka besar), chip "Periode berlangsung" bila aktif, rentang prediksi ("sekitar 12–16 Sep") + confidence lembut ("berdasarkan 6 siklus"), quick actions (Mulai/Selesaikan/Log), insight card 1–2 kalimat | hasData / notEnoughData (2 siklus) / periodActive / predictionLowConfidence | startPeriod, endPeriod, openLog, openCalendar |
| **Calendar (Kalender)** | Riwayat + prediksi dalam satu garis waktu | Month grid (Compose custom, bukan lib eksternal), marker actual (fill solid, terracotta) vs prediksi (outline/dot, alpha rendah), legend, tombol bulan | empty / hasData / monthNavigated | selectDay, editPeriod, addPeriod, deletePeriod |
| **CalendarDayDetailSheet** | Detail & koreksi satu tanggal | Info hari siklus, log hari itu (ringkas), edit rentang periode, hapus | dayNoData / dayWithPeriod / dayWithLog | editRange, deleteLog, openLog |
| **LogEditorSheet (Catat)** | Satu tempat mencatat harian | Segmen flow (5 pilihan), gejala chips multi-select (10) + severity per gejala (ringan/sedang/berat), mood (5), energi (5), catatan `TextField` multi-baris, panel suggestion NLP (P1+), tombol hapus | create / edit / nlpSuggesting (P1+) | selectFlow, toggleSymptom, setSeverity, setMood, setEnergy, typeNote, acceptNlp, save, delete |
| **Insight** | Statistik pribadi yang dapat dijelaskan | Kartu: rata-rata siklus + median, variasi (MAD), durasi rata-rata, gejala tersering (bar sederhana), pola per fase ("awal/tengah/akhir siklus"), disclaimer kecil, tombol riwayat log & ringkasan | notEnoughData / hasData | openHistory, openSummary |
| **LogHistory** | Riwayat log harian | LazyColumn per bulan, filter gejala/mood, tap → LogEditor | empty / list | filter, openLog |
| **SummaryScreen** | Ringkasan ekspor-layar tanpa diagnosis | Teks statistik ringkas, disclaimer, tanpa tombol share (MVP) | — | (view only) |
| **SettingsHome** | Pusat kontrol privasi & preferensi | Section list: Keamanan, Notifikasi, Tampilan, Backup, Data & Privasi, Kebijakan, Tentang | — | navigate |
| **SecuritySettings** | Kelola PIN/biometrik/timeout | Toggle biometrik, ubah PIN, grace period (segera/30 dtk/1 mnt/5 mnt), toggle "izinkan screenshot" | pinSetup / saving | changePin, toggleBio, setGrace, toggleScreenshot |
| **NotificationSettings** | Kontrol reminder & privacy mode | Toggle + jam per reminder, privacy mode radio (Umum/Judul saja/Lengkap), preview visual | permissionDenied | toggle, setTime, setPrivacy |
| **AppearanceSettings** | Tema | Mode (system/light/dark), toggle dynamic color | — | setTheme |
| **BackupExport** | Export manual terenkripsi | Penjelasan passphrase (tidak bisa dipulihkan), input passphrase ×2, SAF save dialog | exporting / success / error | export |
| **BackupRestore** | Pulihkan dari file | SAF open, input passphrase, pratinjau jumlah data, konfirmasi replace | validating / preview / restoring / error | restore |
| **DataDeletion** | Hapus data per kategori / semua | Daftar kategori (log harian, riwayat periode, pengaturan), dialog ketik "HAPUS" untuk hapus semua | confirm / deleting | deleteCategory, deleteAll |
| **PrivacyPolicy** | Kebijakan lokal yang jujur | "Tidak ada akun. Tidak ada cloud. Tidak ada pelacak. Data hanya di ponselmu.", FAQ, kontrol tautan ke Settings | — | (view only) |

**Konvensi state UI:** `UiState` per screen = data class dengan `isLoading / isEmpty / isError + message`. Error lokal (simpan gagal) ditampilkan Snackbar; tidak ada error "server" karena tidak ada server. Semua aksi destruktif dua langkah (dialog → konfirmasi).

---

## 5. Rekomendasi Arsitektur Android

**Pola:** MVVM pragmatis + Repository, **satu modul Gradle** (`:app`) dengan batas paket yang jelas. Tidak ada multi-module (`:core`, `:feature-*`) di MVP — biaya build & boilerplate tidak sebanding untuk satu orang; batas paket menjaga opsi split di masa depan.

```
id.rona.app
├── ui/            # Compose screens, ViewModels, theme, navigation
│   ├── theme/     # Color, Type, Shape, typography
│   ├── navigation/# Routes (@Serializable), NavHost
│   ├── lock/      # LockGate UI + AppLockManager bridge
│   ├── onboarding/
│   ├── home/  calendar/  log/  insights/  settings/
│   └── components/# FloatingPillNavBar, cards, sheets, form controls
├── domain/        # Pure Kotlin, NO Android imports (fully unit-testable)
│   ├── model/     # Enums (FlowLevel, SymptomType, Severity, Mood, Energy, Confidence)
│   ├── engine/    # CycleEngine (prediksi), StatisticsEngine (insight)
│   └── nlp/       # LocalLogAnalyzer (interface), AnalysisSuggestion (P1+)
├── data/          # Room (entities/dao/db), repositories, keystore, backup, reminders, datastore
└── di/            # Hilt modules
```

**Aturan arsitektur:**
1. **UI → ViewModel → Repository → (Room / engine murni).** ViewModel hanya tahu Repository + engine; tidak pernah tahu Room/DAO langsung.
2. **Engine adalah pure function** (`CycleEngine.predict(periods: List<Period>): Prediction?`). Ini yang paling banyak dites unit — prediksi harus deterministik dan bisa dijelaskan.
3. **State:** `StateFlow<UiState>` per screen, `data class` `@Immutable`, `collectAsStateWithLifecycle()`. Event satu arah via lambda/`Channel`.
4. **Room sebagai satu-satunya sumber kebenaran persisten.** DataStore hanya untuk preferensi UI yang sepele (opsi; default MVP: semua setting juga di DB terenkripsi agar backup menyatu — lihat §7 & §9).
5. **App lock di level Activity**, bukan per-screen: satu `LifecycleObserver` mencatat `onStop`, `AppLockManager.isLocked` jadi gerbang NavHost (`startDestination` LockGate saat perlu). Seluruh isi proses dianggap "di balik kunci".
6. **Ketiadaan network adalah fitur arsitektur:** tidak ada Retrofit/OkHttp/HTTP client sama sekali; tidak ada izin INTERNET → tidak ada jalur keluar data. (Bisa dites otomatis, lihat §13.)
7. **Inversi untuk NLP:** UI bergantung pada `LocalLogAnalyzer` (domain), bukan impl. `RuleBasedLogAnalyzer` / `OnnxLogAnalyzer` dipilih via Hilt berdasarkan ketersediaan aset model (§12).

**Lifecycle data:**
- Semua query dibaca sebagai `Flow<List<...>>` dari DAO; ViewModel `combine` + map ke UiState.
- Penulisan via `suspend` di `viewModelScope`; operasi multi-tabel dalam `withTransaction` (contoh: hapus daily log → cascade symptom + nlp suggestion; save log + update prediction dalam satu transaksi).
- Prediksi & insight = **derived cache** yang dihitung ulang oleh engine setelah setiap mutasi data periode/log (bukan dihitung di UI thread).

---

## 6. Rekomendasi Dependency

Pin semua versi di `libs.versions.toml`. Baseline stabil saat dokumen ini ditulis; ambil **versi stabil terbaru saat scaffolding**, jangan alpha kecuali disebut.

| Area | Library | Alasan | Risiko / Catatan |
|---|---|---|---|
| Bahasa | Kotlin 2.x (K2 compiler) | Performa compile, `@Immutable` stabil | Pin KSP dengan versi Kotlin |
| Build | AGP 8.9+ / Gradle via wrapper | Stabil | — |
| UI | Compose BOM (ui, material3, tooling, preview) | Wajib sesuai permintaan; Material 3 dinamis | Ikuti BOM, jangan campur versi |
| UI | `androidx.activity:activity-compose`, `core-splashscreen` | Single activity, splash API 31+ | — |
| Navigasi | Navigation Compose 2.9+ **type-safe routes** + `kotlinx-serialization` | Route ber-tipe, tidak ada string route yang bisa salah ketik | Butuh plugin serialization |
| DI | Hilt + `hilt-navigation-compose` + KSP | Standar, `hiltViewModel()` | Jangan kapt (lambat); pakai KSP |
| DB | Room 2.7+ (runtime, ktx, compiler KSP) | Wajib sesuai permintaan | Schema export wajib ON (`room.schemaLocation`) untuk uji migrasi |
| Enkripsi DB | `net.zetetic:sqlcipher-android:4.6+` + `android-database-sqlcipher` (SupportFactory) | Enkripsi full-DB transparan di Room | Community edition berlisensi terbuka (BSD-style); WAL juga terenkripsi |
| Keystore | Android Keystore (framework, **tanpa** androidx security-crypto) | security-crypto **sudah deprecated**; wrapper tipis sendiri (AES-GCM, ~60 LOC) lebih aman dari sisi dependensi | — |
| Biometrik | `androidx.biometric:biometric:1.1.0` (stabil; jangan 1.2.0-alpha) | BiometricPrompt lintas versi Android | — |
| Preferensi | DataStore Preferences 1.1+ | Non-sensitif & sepele saja | **Tidak** untuk health data, PIN, atau key (aturan non-negotiable #8) |
| Reminder | WorkManager 2.10+ | Reminder harian tanpa `SCHEDULE_EXACT_ALARM` (izin yang tidak pantas untuk app privacy-first) | Jendela ±15 menit tidak masalah untuk reminder harian |
| Coroutines/Flow | kotlinx-coroutines 1.10+, `lifecycle-runtime-compose` | Wajib | — |
| Serialisasi | kotlinx-serialization-json 1.8+ | Navigasi + format backup | — |
| Tanggal | `java.time` (minSdk 26 → **tanpa desugaring**) | `LocalDate`/`EpochDay` untuk semua kalkulasi | Menghindari `Calendar`/`Date` lawas |
| Test unit | JUnit4, Truth, Turbine 1.2+, coroutines-test | Flow testing ergonomis | — |
| Test instrumented | androidx.test runner/rules, `compose-ui-test-junit4`, Hilt testing, Room testing | Compose UI test + DAO test | — |
| Opsional test | Robolectric 4.14+ | ViewModel test cepat di JVM | Non-esensial; boleh masuk M0 |
| Lint | ktlint via Spotless (konfigurasi ringan) | Konsistensi | — |
| R8 | default AGP, `minifyEnabled=true` release | Ukuran & hardening | Keep rules dari Room/serialization sudah disediakan consumer rules |
| **P2 — NLP** | `com.microsoft.onnxruntime:onnxruntime-mobile` | ONNX INT8 lokal | Tambah ~5–8 MB AAR; hanya masuk saat P2 |
| **Sengaja TIDAK ada** | Retrofit/OkHttp, Firebase (semua), Play Services, analytics apa pun, ads SDK, tracking SDK, security-crypto, detekt berat | — | Tidak ada HTTP stack = tidak ada telemetry, bisa ditegaskan via test manifest |

**Strategi versi:** semua di version catalog; bump minor tiap milestone; upgrade major hanya dengan alasan (mis. Room major). Tidak ada library eksperimental. `minSdk` rekomendasi **26** (Android 8.0): `java.time` native, Keystore modern, cakupan perangkat sangat luas; keputusan final di §16.

---

## 7. Data Schema Room (Draft Kotlin)

Semua tabel ada di **satu database terenkripsi SQLCipher** (lihat §9). Enum disimpan sebagai `String` via `TypeConverter` (aman terhadap reorder). `// SENSITIVE` menandai field yang memuat data kesehatan/pribadi. Timestamp = `System.currentTimeMillis()` (UTC), tanggal = `LocalDate.toEpochDay()`.

```kotlin
@Entity(tableName = "local_profile")
data class LocalProfileEntity(
    @PrimaryKey val id: Int = 1,              // singleton row
    val defaultCycleLengthDays: Int? = null,  // prior onboarding; tidak dipakai lagi setelah ≥3 siklus
    val onboardingCompletedAt: Long? = null,
    val createdAt: Long
)

@Entity(tableName = "period_records",
    indices = [Index("start_epoch_day"), Index("end_epoch_day")])
data class PeriodRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startEpochDay: Long,                  // SENSITIVE — wajib
    val endEpochDay: Long? = null,            // SENSITIVE — null = masih berlangsung
    val createdAt: Long,
    val updatedAt: Long
) // Unique index parsial "hanya satu periode aktif" diverifikasi di repository (Room tidak dukung partial unique)

@Entity(tableName = "daily_logs",
    indices = [Index(value = ["date_epoch_day"], unique = true)])  // satu log per hari
data class DailyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,                   // SENSITIVE
    val flow: FlowLevel? = null,              // SENSITIVE — SPOTTING/LIGHT/MEDIUM/HEAVY
    val mood: Mood? = null,                   // SENSITIVE
    val energy: Energy? = null,               // SENSITIVE
    val note: String? = null,                 // SENSITIVE — catatan bebas privat
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "symptom_logs",
    foreignKeys = [ForeignKey(DailyLogEntity::class, ["id"], ["daily_log_id"], onDelete = CASCADE)],
    indices = [Index("daily_log_id"), Index(value = ["daily_log_id", "symptom_type"], unique = true)])
data class SymptomLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dailyLogId: Long,                     // SENSITIVE (FK ke catatan harian)
    val symptomType: SymptomType,             // SENSITIVE — KRAM, HEADACHE, NAUSEA, BLOATING, FATIGUE,
                                              //   BREAST_TENDERNESS, ACNE, BACKACHE, SLEEP_ISSUE, APPETITE_CHANGE
    val severity: Severity                    // SENSITIVE — MILD/MODERATE/SEVERE
)

@Entity(tableName = "cycle_predictions",      // derived cache, bukan data pengguna
    indices = [Index("generated_at"), Index("predicted_start_epoch_day")])
data class CyclePredictionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val generatedAt: Long,
    val predictedStartEpochDay: Long,
    val rangeLowEpochDay: Long,
    val rangeHighEpochDay: Long,
    val medianCycleLengthDays: Int,
    val meanCycleLengthDays: Double,
    val madDays: Double,
    val cycleCountUsed: Int,
    val confidence: Confidence,               // LOW / MEDIUM / HIGH
    val engineVersion: Int
) // Regenerasi penuh tiap mutasi periode; hanya baris terbaru dipakai UI; housekeeping: simpan ≤30 baris

@Entity(tableName = "app_lock_settings")
data class AppLockSettingsEntity(
    @PrimaryKey val id: Int = 1,              // singleton
    val lockEnabled: Boolean,
    val biometricEnabled: Boolean,
    val pinHash: String? = null,              // SENSITIVE — PBKDF2-HMAC-SHA256, 120k+ iterasi, salt acak
    val pinSalt: String? = null,              // SENSITIVE
    val pinIterations: Int = 120_000,
    val gracePeriodSeconds: Int = 30,         // 0 = langsung terkunci
    val updatedAt: Long
)

@Entity(tableName = "reminder_settings")
data class ReminderSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val periodReminderEnabled: Boolean = true,
    val periodReminderDaysBefore: Int = 2,    // 0..7
    val periodReminderMinuteOfDay: Int = 1200,// 20:00
    val dailyLogReminderEnabled: Boolean = false,
    val dailyLogMinuteOfDay: Int = 1260,      // 21:00
    val vibrationEnabled: Boolean = true
)

@Entity(tableName = "privacy_settings")
data class PrivacySettingsEntity(
    @PrimaryKey val id: Int = 1,
    val notificationPrivacyMode: PrivacyMode = GENERIC, // GENERIC (default, tanpa kata sensitif) / TITLE_ONLY / FULL
    val allowScreenshots: Boolean = false,    // false → FLAG_SECURE aktif
    val themeMode: ThemeMode = SYSTEM         // SYSTEM / LIGHT / DARK
)

@Entity(tableName = "export_metadata")        // audit export; TIDAK menyimpan salt/passphrase/isi payload
data class ExportMetadataEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exportedAt: Long,
    val fileName: String? = null,
    val schemaVersion: Int,
    val appVersion: String,
    val periodCount: Int,
    val dailyLogCount: Int,
    val ciphertextSha256: String              // checksum ciphertext (integritas file), bukan plaintext
)

@Entity(tableName = "nlp_suggestions",        // P1+; TIDAK menduplikasi raw note (hanya FK + label + confidence)
    foreignKeys = [ForeignKey(DailyLogEntity::class, ["id"], ["daily_log_id"], onDelete = CASCADE)],
    indices = [Index("daily_log_id"), Index("status")])
data class NlpSuggestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dailyLogId: Long,
    val source: String,                       // RULES | ONNX
    val modelVersion: String? = null,
    val payloadJson: String,                  // label saran + confidence; TIDAK ada teks catatan
    val status: NlpStatus,                    // PENDING / CONFIRMED / DISMISSED
    val createdAt: Long
)
```

**Enum domain** (dipakai DB & UI): `FlowLevel { SPOTTING, LIGHT, MEDIUM, HEAVY }`, `SymptomType { KRAM, HEADACHE, NAUSEA, BLOATING, FATIGUE, BREAST_TENDERNESS, ACNE, BACKACHE, SLEEP_ISSUE, APPETITE_CHANGE }`, `Severity { MILD, MODERATE, SEVERE }`, `Mood` & `Energy` (skala 5), `Confidence { LOW, MEDIUM, HIGH }`.

**Keputusan storage:** setting (lock, reminder, privacy, theme) disimpan di **DB terenkripsi yang sama**, bukan DataStore — satu sumber kebenaran, ikut ter-backup, dan tidak ada data sensitif (PIN hash) di SharedPreferences. DataStore (bila dipakai nanti) hanya untuk preferensi ephemeral seperti "tab terakhir dibuka".

**Retention & deletion:** MVP **tidak menghapus data otomatis** (data milik pengguna). Cascades: hapus DailyLog → hapus SymptomLog + NlpSuggestion; hapus semua → truncate semua tabel (profile & settings direset ke default) lalu engine reset. Migrasi Room: `fallbackToDestructiveMigration` **dilarang**; tiap perubahan schema harus migrasi eksplisit + uji migrasi.

---

## 8. Privacy & Security Threat Model

| # | Ancaman | Kemungkinan | Dampak | Mitigasi |
|---|---|---|---|---|
| T1 | HP hilang/dicuri, data dibaca | Sedang | Tinggi | DB SQLCipher (key di Keystore, non-exportable, StrongBox bila ada); app lock aktif; grace period pendek; `allowBackup=false` |
| T2 | Aplikasi dibuka orang lain (pasangan, anak, orang sekitar) | Sedang | Tinggi | Lock gate biometric+PIN; lock on background + timeout; PIN hash PBKDF2 + constant-time compare; **tidak ada** fitur share/akun yang bisa disalahgunakan |
| T3 | Backup file `.rona` bocor (email, cloud drive, USB) | Rendah–sedang | Tinggi | Format terenkripsi AES-256-GCM + passphrase pengguna (PBKDF2 ≥120k iterasi); tanpa passphrase file tidak bisa dibaca; UI memperingatkan passphrase tidak dapat dipulihkan |
| T4 | Screenshot / recent apps menampilkan data | Sedang | Sedang | `FLAG_SECURE` default aktif (memblokir screenshot & blur di recents); toggle "izinkan screenshot" opt-in di Settings (keputusan #3) |
| T5 | Notification preview membocorkan konteks | Sedang | Sedang | Privacy mode default GENERIC: judul "rona", teks "Pengingat harianmu" — tanpa kata menstruasi/haid/tanggal; mode FULL hanya opt-in |
| T6 | Data terhapus tidak sengaja | Sedang | Sedang | Konfirmasi dua langkah + ketik "HAPUS"; tawaran export sebelum hapus; restore dari backup terenkripsi |
| T7 | Android Auto Backup / Google backup menyalin data | (bila salah konfigurasi) | Tinggi | `android:allowBackup="false"` + `dataExtractionRules` kosong; dites otomatis di CI |
| T8 | Log bocor via Logcat (adb, bug report) | Sedang | Tinggi | `PrivacyLogger` yang no-op di release; lint rule + test yang memastikan tidak ada `Log.*` di jalur data; catatan tidak pernah masuk string log |
| T9 | Keylogger/IME pihak ketiga menangkap PIN & catatan | Rendah | Tinggi | Edukasi in-app: saran gunakan keyboard sistem; tidak ada mitigasi teknis penuh (residual risk didokumentasikan) |
| T10 | Perangkat rooted, attacker punya akses fisik + unlock | Rendah | Tinggi | Keystore hardware-backed tetap mempersulit ekstraksi key; diterima sebagai residual risk dan ditulis di kebijakan privasi |
| T11 | Malware membaca proses/penyimpanan | Rendah | Sedang | `targetSdk` terbaru, storage internal app-private, tanpa INTERNET permission (data tidak bisa dieksfiltrasi dari app ini), R8 |
| T12 | Clipboard bocor (passphrase backup tersalin) | Rendah | Sedang | Tidak pernah menyalin passphrase/note ke clipboard otomatis; tidak ada tombol "copy" untuk data sensitif |
| T13 | Klaim prediksi disalahartikan medis | — | Reputasi | Copy UI non-diagnostik ("perkiraan", rentang, bukan tanggal pasti), disclaimer di Insight & Summary, tidak ada fitur fertile window |

**Pernyataan privasi (yang akan tertulis di in-app Privacy Policy):** tidak ada akun, tidak ada cloud, tidak ada tracker, tidak ada izin INTERNET, backup manual terenkripsi olehmu, data hanya di ponselmu.

---

## 9. Encrypted Local Storage & Backup Design

### 9.1 Kunci database (Keystore)
1. `KeyGenParameterSpec("rona_db_key", AES/GCM/NoPadding, 256-bit)` — non-exportable, `setUserAuthenticationRequired(false)`, `setUnlockedDeviceRequired(true)` bila perangkat mendukung (API 28+), `setIsStrongBoxBacked(true)` best-effort.
2. On first run: generate **passphrase DB acak 32 byte** → enkripsi dengan kunci Keystore (AES-GCM) → simpan ciphertext di `filesDir/db.key.enc` (internal, app-private, 0600).
3. Room dibuka dengan `SupportFactory(passphraseBytes)` dari SQLCipher. Passphrase hanya hidup di memori sesaat saat open; tidak pernah di SharedPreferences/DataStore/Logcat.

**Trade-off dicatat (keputusan #4):** kunci **tidak** di-`setUserAuthenticationRequired(true)`. Konsekuensi: DB bisa dibuka kapan pun proses berjalan, tetapi app lock (biometric+PIN) yang menjadi gerbang UI. Alasan: (a) auth-gated key invalid setelah reboot → reminder/prediksi butuh DB sebelum unlock pertama, (b) threat model T1–T2 dijawab oleh kombinasi SQLCipher + lock ketat. Alternatif (key auth-gated) tercatat sebagai opsi hardening post-MVP.

### 9.2 Format backup terenkripsi (file `.rona`)
```
RONA1 (magic, 5 byte)
header JSON (plaintext): formatVersion, appVersion, exportedAt,
  kdf { algo:"PBKDF2WithHmacSHA256", iterations:120000 (dikalibrasi perangkat), salt: b64(16B) },
  cipher { algo:"AES-256-GCM", nonce: b64(12B) }, payloadLen
ciphertext + 128-bit authentication tag
```
- Passphrase pengguna (min 8 karakter) → key via PBKDF2 (salt acak per export). Tag GCM memastikan **integritas + otentikasi** (file yang diubah 1 bit → restore ditolak).
- Payload JSON (streaming, `Json.decodeFromStream`): `schemaVersion`, `profile`, `periods[]`, `dailyLogs[]{…, symptoms[]}`, `settings{reminder, privacy}`. **Dikecualikan:** `cycle_predictions` (derived, dihitung ulang), `nlp_suggestions` (derived), `export_metadata` (audit lokal).
- Est. ukuran: 5 tahun log harian ≈ < 2 MB.
- **Tidak ada key escrow, tidak ada recovery.** UI menyatakan ini eksplisit: "Jika passphrase lupa, backup tidak bisa dibuka."
- Export via SAF `ACTION_CREATE_DOCUMENT` (pengguna pilih lokasi; app tidak menyimpan file di storage publik). Restore via `ACTION_OPEN_DOCUMENT` → validasi → **pratinjau jumlah data** → replace dalam satu transaksi Room.

### 9.3 Kebijakan lain
- `android:allowBackup="false"`, `dataExtractionRules` exclude-all, `usesCleartextTraffic=false` (defense in depth; INTERNET memang tidak ada).
- Data pengguna tidak pernah masuk DataStore/SharedPreferences/konten provider/exported component. Semua provider/activity `exported=false`.
- Penghapusan semua data juga menghapus `db.key.enc` dan DB → kondisi seperti instalasi baru.

---

## 10. Local Notification Design

- **Mesin:** `ReminderWorker` (WorkManager, periodic 24 jam, `ExistingPeriodicWorkPolicy.UPDATE`) membaca `ReminderSettings` + prediksi terbaru dari DB → memposting notifikasi. Reschedule segera setiap kali data periode/log berubah (prediksi bergeser → jadwal ikut). Tidak ada `SCHEDULE_EXACT_ALARM`.
- **Channels:**
  - `rona_reminder_period` — IMPORTANCE_DEFAULT, **tanpa suara default**, vibrasi bisa dimatikan.
  - `rona_reminder_daily_log` — IMPORTANCE_LOW.
- **Konten discreet (default GENERIC):**
  - Pra-periode: judul **"rona"**, teks **"Ada pengingat kecil untukmu."** (tanpa kata menstruasi/haid, tanpa tanggal).
  - Log harian: judul **"rona"**, teks **"Satu menit untuk mencatat hari ini."**
  - Mode TITLE_ONLY: hanya judul. Mode FULL (opt-in): "Perkiraan periode mulai dalam 2 hari."
- **Aturan:** tidak ada data kesehatan di `extras`; tap → buka MainActivity (melewati lock gate dulu). Izin `POST_NOTIFICATIONS` diminta saat onboarding, bisa dilewati, bisa diubah di Settings.
- Tidak ada notifikasi apa pun yang dikirim tanpa aksi lokal pengguna (tidak ada push).

---

## 11. Floating Pill Navigation Bar — Spesifikasi

Komponen custom `FloatingPillNavBar` (bukan `NavigationBar` standar).

**Geometri & visual:**
- Posisi: bottom-center, `Scaffold(bottomBar = …)`, container `Box` dengan `padding(horizontal = 20.dp, bottom = 12.dp)` + `navigationBarsPadding()` (hormati gesture inset).
- Bentuk: `Surface(shape = RoundedCornerShape(32.dp), color = surfaceContainer.copy(alpha = 0.96f), tonalElevation = 3.dp, shadowElevation = 6.dp)` — pill 32dp radius, tinggi baris 64dp, lebar wrap-content (maks ~360dp), shadow sangat lembut.
- Komposisi: `[Beranda] [Kalender] [● Catat ●] [Insight]`. **Catat** = center action melingkar 56dp, `containerColor = primary`, icon `add`, offset vertikal −12dp (muncul "mengambang" di atas pill), dengan shadow sendiri.
- Tab terpilih: pill kecil `secondaryContainer` berisi **icon + label** (12sp); tab tidak terpilih: **icon saja** (warna `onSurfaceVariant`). Transisi `animateColorAsState` + `AnimatedVisibility`.
- Ikon: Material Symbols Rounded — `home`, `calendar_month`, `add`, `query_stats`.

**Accessibility:**
- Tap target ≥ 48dp (`minimumInteractiveComponentSize`), `Role.Tab`, `semantics { selected }`, `contentDescription` eksplisit ("Beranda, tab dipilih").
- State terpilih tidak hanya warna: ikon filled + label + indikator bentuk.
- Uji TalkBack + font scale 200% di M11.

**Perilaku:** 4 destinasi; Catat membuka `LogEditorSheet` (bukan tab penuh). State tab disimpan (`saveState/restoreState`). Opsional post-MVP: hide-on-scroll via `NestedScrollConnection`. Edge-to-edge diaktifkan (`enableEdgeToEdge()`).

**Palet fallback (arah desain — final di M0/M11):** warm neutral, bukan pink. Light: primary `#8C5A4B` (tanah liat), secondary `#6F7D5F` (sage), tertiary `#7C6078` (plum lembut), surface `#FDF8F3`, container `#F4EBE2`. Dark: primary `#FFB59E`, surface `#171412`, container `#211C19`. Dynamic color default ON (Android 12+), palet ini jadi fallback. Typography: font sistem (tidak bundel font custom di MVP), skala teks dihormati.

**Copywriting (contoh, final di M11):** "Mulai periode hari ini?" — "Tercatat. Semoga harimu tenang." — "Perkiraan periode berikutnya: sekitar 12–16 September." — "Berdasarkan 6 siklus terakhir." Tidak ada kata diagnosis, tidak ada "risiko", tidak ada warna merah alarm.

---

## 12. NLP Future Architecture (Local-Only)

**Abstraksi (domain):**
```kotlin
interface LocalLogAnalyzer {
    val source: String                 // "RULES" | "ONNX"
    suspend fun analyze(note: String, logDate: LocalDate): AnalysisResult
}
data class AnalysisResult(
    val symptoms: List<LabelConfidence>,   // hanya dari allowlist 10 gejala
    val mood: LabelConfidence?, val energy: LabelConfidence?, val severityHint: LabelConfidence?,
    val diagnosticsBypassed: Boolean       // guard: input memuat istilah diagnosis → hasil dikosongkan
)
data class LabelConfidence(val label: String, val confidence: Float)
```
**Pipa threshold (milik UI, sama untuk semua engine):** ≥0.85 → suggestion **preselected** (tetap bisa di-uncheck); 0.60–0.84 → suggestion **tanpa auto-save**; <0.60 → tidak ditampilkan. **Selalu** butuh konfirmasi sebelum tersimpan. `NlpSuggestionEntity` mencatat audit (PENDING/CONFIRMED/DISMISSED) tanpa menduplikasi catatan.

**P1 — Rule-based (tanpa ML):** `RuleBasedLogAnalyzer` — synonym map Bahasa Indonesia (contoh: "kram", "perut sakit", "mules" → KRAM; "pusing" → HEADACHE; "begah" → BLOATING; "capek/lelah/ngantuk" → FATIGUE; "susah tidur/begadang" → SLEEP_ISSUE; "ngidam/nafsu makan berubah" → APPETITE_CHANGE), deteksi negasi ("tidak/nggak/ga kram"), penguat severity ("parah", "banget", "sedikit"), skor deterministik dipetakan ke confidence band di atas. Diuji terhadap korpus kalimat Indonesia.

**P2 — ONNX lokal:** `indobert-lite-base-p2` → fine-tune multi-label (10 gejala + mood/energi/severity, output sigmoid) → ekspor ONNX → **INT8 dynamic quantization** → bundle `model.int8.onnx` + `vocab.txt` sebagai **asset APK** (tidak ada download model — jaringan memang tidak ada). Tokenizer WordPiece diimplementasikan manual (~150 LOC, tanpa dependensi transformers). Ukuran APK bertambah ~8–12 MB (konsekuensi diterima; tercantum di §16).

**Guard diagnosis (berlapis):**
1. **Struktural:** output layer hanya bisa menghasilkan label dari allowlist; tidak ada head free-text/generation → model tidak mungkin mengeluarkan "PCOS" dsb.
2. **Rule blocklist:** regex istilah diagnosis (PCOS, endometriosis, anemia, depresi, gangguan hormonal, dll.) → bila terdeteksi, hasil suggestion dikosongkan (diagnosticsBypassed=true), catatan tetap tersimpan normal.
3. **Copy UI:** tidak pernah mengaitkan gejala dengan kondisi medis.

**Privasi:** teks tidak pernah keluar proses; **tidak ada training dalam aplikasi** (model statis). Bila personalisasi pernah dipertimbangkan, wajib consent eksplisit + ekspor — di luar roadmap saat ini.

---

## 13. Test Plan

### Unit (JVM, murni)
- `CycleEngineTest`: panjang siklus dari selisih start; filter validitas (15–90 hari, configurable); median vs weighted mean; MAD & range prediksi (`±max(1, 1.5×MAD)` dengan cap); confidence band (≥5 siklus & MAD≤2 → HIGH; 3–4 siklus → MEDIUM; <3 → LOW); edge cases: <2 record, periode berlangsung, MAD tinggi, tahun kabisat, entri masa depan, duplikat start, end < start.
- `StatisticsEngineTest`: rata-rata durasi, gejala tersering, bucket per fase ("awal/tengah/akhir" dinormalisasi terhadap panjang siklus).
- `RuleBasedLogAnalyzerTest` (P1): korpus ~100 kalimat Indonesia; negasi; severity; threshold bands; blocklist diagnosis.
- `BackupCodecTest`: round-trip; passphrase salah → gagal; 1 byte diubah → tag GCM gagal; salt unik antar export.
- `PinVerifierTest`: PBKDF2 deterministik, perbandingan constant-time, iterasi minimum.

### Database (instrumented, perangkat/emulator)
- `PeriodDaoTest`, `DailyLogDaoTest`, `SymptomDaoTest`: CRUD, unique constraint (satu log/hari), cascade delete (log → gejala + nlp), indeks terpakai.
- `EncryptionInstrumentedTest`: buka file DB mentah di disk → assert **tidak ada plaintext** catatan/gejala.
- `MigrationTest`: schema export + migrasi antar versi.

### Security (CI + instrumented)
- `ManifestSecurityTest`: assert tidak ada `<uses-permission INTERNET>`, `allowBackup=false`, `dataExtractionRules` exclude-all, semua komponen `exported=false`, `usesCleartextTraffic=false`.
- `SecureFlagTest`: activity menampilkan data → assert `FLAG_SECURE` aktif saat setting default.
- `LogcatGuardTest`: `PrivacyLogger` no-op di release; grep hasil build untuk `Log.(d|i|v)` di paket data.
- `KeystoreTest`: key non-exportable, regenerate gagal tanpa akses file internal.

### UI (Compose, instrumented)
- `OnboardingFlowTest`: happy path penuh sampai Beranda; lewati notifikasi; input tanggal invalid.
- `LockGateTest`: biometrik (mock) sukses/gagal → PIN fallback; grace period; kembali dari background → terkunci.
- `LogEditorTest`: pilih flow, gejala + severity, simpan → muncul di Kalender/Insight; edit; hapus.
- `CalendarTest`: marker actual vs prediksi berbeda secara visual/semantik; edit rentang.
- `NavBarAccessibilityTest`: contentDescription, state selected, ukuran tap.
- `DataDeletionTest`: konfirmasi "HAPUS" → onboarding; kategori terhapus tanpa merusak yang lain.
- `BackupRestoreFlowTest`: export → hapus → restore → data sama.

### Manual Acceptance (rilis)
Pesawat baru instalasi; **airplane mode penuh semua fitur berfungsi**; notifikasi muncul dengan teks generic; screenshot diblokir (recents blur); kill & relaunch → lock; salah PIN 5× → cooldown; export → pindah file → restore di device lain; TalkBack + font besar + dark mode; tidak ada frame jank di kalender.

---

## 14. Roadmap Implementasi (Milestone Kecil)

Setiap milestone selesai = build hijau + bisa dijalankan manual.

| # | Milestone | Isi | Keluaran |
|---|---|---|---|
| M0 | Fondasi | Gradle + version catalog + package `id.rona.app` + CI (lint, unit, assemble) + tema M3 dasar + R8 | APK kosong ber-tema |
| M1 | Data & keamanan inti | Keystore `CryptoManager`, Room + SQLCipher, seluruh schema + DAO, manifest security, `PrivacyLogger`, PIN verifier | DB terenkripsi teruji |
| M2 | Engine siklus | `CycleEngine`, `StatisticsEngine`, confidence & range | 100% unit-tested engine |
| M3 | App lock | BiometricPrompt + PIN, lock gate + grace period, screen lock settings | Aplikasi terkunci |
| M4 | Onboarding | Seluruh alur onboarding + persistensi | First-run lengkap |
| M5 | Beranda + navigasi | Floating pill nav (spec §11), Home screen, quick actions | App mulai terasa jadi produk |
| M6 | Kalender | Grid bulan, marker actual/prediksi, detail hari, edit rentang | Riwayat & prediksi visual |
| M7 | Catat | Log editor lengkap (flow/gejala/severity/mood/energi/note), edit/hapus | Pencatatan harian |
| M8 | Insight | Statistik, gejala tersering, pola fase, riwayat log, summary | Insight tanpa diagnosis |
| M9 | Reminder | WorkManager, channels, privacy mode, reschedule | Notifikasi discreet |
| M10 | Settings & backup | Settings lengkap, export/restore terenkripsi, delete kategori/semua, privacy policy | Siklus data tertutup |
| M11 | Polish | Motion, a11y audit, copywriting final, empty/error states audit | Kualitas rilis |
| M12 | Hardening & rilis | Security test suite penuh, R8 + ukuran APK, threat model final, Data Safety form | Siap rilis |
| P1 | NLP rule-based | `LocalLogAnalyzer`, extractor + synonym map, UI suggestion, audit row | Saran lokal deterministik |
| P2 | NLP ONNX | Pipeline fine-tune (di luar repo), ONNX INT8, tokenizer, bench | Model lokal |
| P3 | Partner mode | **Dokumen desain saja** (opt-in, granular, revocable, E2E) | Keputusan tanpa kode |

Estimasi urutan disengaja: **keamanan sebelum fitur**, engine sebelum UI (agar UI dibangun di atas logika yang sudah benar).

---

## 15. Usulan GitHub Issues per Milestone

**M0**
1. Scaffold project Gradle, version catalog, package `id.rona.app`, wrapper. AC: build hijau.
2. CI GitHub Actions: `lint` + `test` + `assembleDebug` per PR.
3. Tema M3: dynamic color + palet fallback light/dark (§11), typography.
4. R8 release + keep rules + `minifyEnabled`.

**M1**
5. `CryptoManager` Keystore (AES-GCM, non-exportable, StrongBox best-effort) + `db.key.enc`.
6. Room + SQLCipher `SupportFactory`, schema export, framework migrasi.
7. Seluruh entitas + DAO + TypeConverter enum (§7).
8. DAO instrumented tests + test file DB terenkripsi di disk.
9. Manifest security: no INTERNET, allowBackup=false, dataExtractionRules, exported=false.
10. `PinVerifier` (PBKDF2 + salt + constant-time) + unit tests.
11. `PrivacyLogger` no-op release + aturan lint no-log.

**M2**
12. `CycleEngine` inti (validasi siklus 15–90, median, MAD).
13. Range prediksi + confidence band + `CyclePredictionEntity`.
14. `CycleEngineTest` edge cases lengkap.
15. `StatisticsEngine` (durasi, gejala tersering, pola fase).

**M3**
16. BiometricPrompt enroll + fallback PIN.
17. Lock gate + grace period + lock-on-background.
18. Security settings screen (ubah PIN, toggle bio, timeout).

**M4**
19. Welcome + privacy promise screen.
20. Lock setup step dalam onboarding.
21. Last period + end date + cycle length optional.
22. Notifikasi onboarding (izin + preferensi discreet).
23. Persistensi onboarding + guard re-entry.

**M5**
24. `FloatingPillNavBar` sesuai spec §11 + test aksesibilitas.
25. Home screen: hari siklus, rentang prediksi, chip periode aktif.
26. Quick actions Mulai/Selesaikan/Log + insight card + empty state.

**M6**
27. Kalender bulan custom + marker actual vs prediksi + legend.
28. Day detail sheet + edit rentang periode.
29. Tambah/hapus record periode dari kalender.

**M7**
30. Log editor sheet lengkap (flow, 10 gejala + severity, mood, energi, note).
31. Edit/hapus log harian + cascade bersih.
32. Slot UI suggestion NLP (disabled, siap P1).

**M8**
33. Insight screen (kartu statistik + grafik bar sederhana).
34. Riwayat log + filter.
35. Summary screen + disclaimer non-diagnosis.

**M9**
36. `ReminderWorker` + WorkManager + reschedule saat data berubah.
37. Channels + privacy mode (GENERIC/TITLE_ONLY/FULL) + preview.

**M10**
38. Settings home lengkap.
39. Backup codec + export via SAF + `ExportMetadata`.
40. Restore (validasi → pratinjau → replace transaksional).
41. Delete by category + delete all (ketik "HAPUS").
42. Privacy policy in-app.

**M11**
43. Motion & polish (transisi tab, sheet, animasi pill).
44. Audit aksesibilitas (TalkBack, font scale, kontras, tap target).
45. Copywriting Indonesia final + audit string non-diagnostik.
46. Audit state loading/empty/error semua screen.

**M12**
47. Security test suite (manifest, FLAG_SECURE, keystore, logcat).
48. R8 + ukuran APK + cold start.
49. Threat model final + Data Safety form + checklist rilis.

**P1**
50. `LocalLogAnalyzer` + `RuleBasedLogAnalyzer` + synonym map.
51. UI suggestion + threshold bands + konfirmasi + `NlpSuggestionEntity`.
52. Korpus test + guard blocklist diagnosis.

**P2**
53. Repo terpisah: fine-tune IndoBERT Lite multi-label + evaluasi + ekspor ONNX INT8.
54. `OnnxLogAnalyzer` + tokenizer WordPiece + bundling asset.
55. Evaluasi threshold on-device + bench CPU/memori.

**P3**
56. Dokumen desain partner mode (opt-in, granular, revocable, E2E) — tanpa kode.

---

## 16. Risiko Teknis, Trade-off, dan Keputusan yang Perlu Kamu Pilih

### Risiko & trade-off utama
- **SQLCipher + Room:** harus pakai `SupportFactory`; schema export & migrasi jadi lebih ketat (tidak boleh destructive). Mitigasi: uji migrasi sejak M1.
- **Keystore & reinstall OS:** kunci DB hilang bila OS di-reset → data tak terbaca (by design). Jalur pemulihan satu-satunya = backup `.rona` manual. Harus dikomunikasikan di Settings & onboarding.
- **FLAG_SECURE vs kenyamanan:** blokir screenshot melindungi privasi tapi pengguna tidak bisa screenshot kalender untuk keperluan sendiri. Default: blokir + toggle opt-in (keputusan #3).
- **WorkManager inexact (±15 mnt):** reminder bisa telat beberapa menit. Trade-off yang tepat vs `SCHEDULE_EXACT_ALARM` yang butuh izin sensitif.
- **ONNX APK +8–12 MB (P2):** model dibundel (tidak boleh download). Diterima demi local-only absolut.
- **Median vs weighted mean:** median dipilih (robust outlier), mean ditampilkan sebagai informasi tambahan. Prediksi selalu berupa rentang + confidence — tidak pernah tanggal pasti.
- **Satu modul vs multi-module:** satu modul sekarang; batas paket memungkinkan split nanti bila repo tumbuh.
- **Residual risk perangkat rooted / IME pihak ketiga:** didokumentasikan jujur di kebijakan privasi, tidak bisa dihilangkan teknis.
- **Copywriting Indonesia:** kualitas copy menentukan "rasa tenang" produk; dialokasikan milestone khusus (M11) + review oleh penutur asli.

### Keputusan yang harus kamu pilih sebelum coding
1. **minSdk** — rekomendasi **26** (Android 8.0). Alternatif: 24 (butuh desugaring java.time), 29 (lebih modern, cakupan lebih kecil).
2. **Aplikasi dirilis ke mana** — sideload APK (rekomendasi awal) vs Play Store (tambah Data Safety form, signing key publik — tidak mengubah arsitektur).
3. **Screenshot** — default **blokir (FLAG_SECURE)** dengan toggle opt-in di Settings, atau izinkan default?
4. **Lock default** — **biometrik + PIN aplikasi, grace 30 detik** (rekomendasi). Alternatif: langsung terkunci tiap background, atau fallback device credential (lebih nyaman, tapi bergantung pada PIN perangkat yang mungkin dibagikan).

### Default yang saya pakai bila tidak ada keberatan (bukan blocker)
- `applicationId = id.rona.app`; nama tampilan "rona" (huruf kecil).
- Kode & identifier bahasa Inggris; string UI bahasa Indonesia.
- Tidak ada font custom (font sistem, aksesibilitas lebih baik).
- Jam reminder default 20:00 (pra-periode, 2 hari sebelum) & 21:00 (log harian, off).
- Rule-based NLP masuk **P1** (bukan MVP inti) — sesuai permintaanmu.
