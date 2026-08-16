# rona

Pelacak siklus menstruasi **private-first** untuk Android. Semua data tinggal
di perangkat — tidak ada akun, tidak ada cloud, tidak ada pelacak, dan
**tidak ada izin internet sama sekali**.

## Fitur inti (MVP)

- Catat periode (mulai/selesai), flow, 10 gejala + severity, mood, energi,
  dan catatan harian privat.
- Prediksi periode sebagai **rentang** berbasis median/MAD — bukan tanggal
  pasti, bukan alat kontrasepsi, bukan diagnosis.
- Kalender bulan dengan marker actual vs prediksi yang dibedakan visual.
- Insight pribadi yang dapat dijelaskan (rata-rata, variasi, pola fase).
- App lock: biometrik + PIN (PBKDF2), grace period, FLAG_SECURE.
- Reminder lokal discreet dengan privacy mode (tanpa kata sensitif).
- Backup manual terenkripsi AES-256-GCM + passphrase pengguna (format `.rona`).
- Database SQLCipher penuh dengan kunci di Android Keystore.

## P1 — Analisis catatan lokal (rule-based)

- Tombol **"Analisis catatan"** di LogEditor menjalankan rule engine lokal
  untuk menyarankan gejala, severity, durasi, mood, energi, flow, deskriptor
  keputihan, dan konteks kehamilan.
- Semua saran **harus dikonfirmasi pengguna** sebelum tersimpan.
- Negasi Bahasa Indonesia ("tidak pusing", "bukan hamil", "sudah hilang")
  dihormati.
- Safety triage berjalan paling awal (kehamilan + perdarahan, perdarahan
  berat, self-harm wording, dll.) dengan kartu peringatan berbahasa
  Indonesia yang tenang dan non-diagnostik.
- Knowledge base lokal terstruktur (13 kategori, metadata review tanggal &
  sumber) — bukan chatbot, bukan generative text.

**Tidak ada cloud AI dan tidak ada transmisi data.** Analisis berjalan
sepenuhnya on-device tanpa model remote, tanpa API key, tanpa LLM.

## Dokumentasi

- `docs/PLAN.md` — technical product plan (16 seksi, milestone M0–M12 + P1–P3).
- `docs/NLP_RULE_ENGINE.md` — arsitektur & privacy guarantees P1.
- `docs/MEDICAL_CONTENT_BOUNDARIES.md` — batas konten medis.
- `docs/TESTING_P1.md` — perintah test & checklist verifikasi.
- `docs/TOOLCHAIN.md` — setup toolchain lokal (JDK 17, SDK, Gradle).

## Build

```bash
./gradlew assembleDebug      # APK debug
./gradlew testDebugUnitTest  # unit test
./gradlew assembleRelease    # R8 minified release
```

Lihat `docs/TOOLCHAIN.md` untuk environment variable yang diperlukan.
