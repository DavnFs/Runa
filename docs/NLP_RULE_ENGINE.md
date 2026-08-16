# NLP Rule Engine — P1

## Status
P1 (rule-based, fully local) diterapkan. P2 (ONNX) belum diimplementasikan.

## Arsitektur

```
domain/nlp/
  NoteAnalysisInput.kt      — input contract (pure Kotlin)
  LocalNoteAnalyzer.kt      — interface analyzer (P1: rule-based; P2: ONNX)
  NlpModels.kt              — semua tipe output: suggestion, safety, knowledge
  SafetyTriageEngine.kt     — interface + SafetyContext
  dict/                     — typed dictionaries (gejala, mood/energi/flow,
                              keputihan, kehamilan, negasi, intent)
  engine/                   — TextNormalizer, NegationDetector, SeverityExtractor,
                              DurationExtractor, SymptomExtractor, MoodEnergyFlowExtractor,
                              DischargeExtractor, PregnancyContextExtractor,
                              IntentRouter, RuleBasedNoteAnalyzer
  safety/                   — SafetyTriageEngineImpl
  knowledge/                — KnowledgeCardSeed, LocalKnowledgeCardRepository
```

UI (`ui/nlp/NoteAnalysisResultSheet`, `ui/log/LogEditorViewModel`) hanya memanggil
`LocalNoteAnalyzer.analyze(...)` — tidak pernah menyentuh regex/rule secara langsung.

## Privacy guarantees

1. Tidak ada izin INTERNET (debug & release). Tidak ada network client.
2. Analisis hanya berjalan saat pengguna menekan "Analisis catatan"
   (`analysisTriggeredByUser` guard di `RuleBasedNoteAnalyzer`).
3. Raw note tidak pernah ditulis ke Logcat (`PrivacyLogger` no-op release;
   analyzer hanya log jumlah saran, bukan teks).
4. Normalized text adalah salinan ephemeral — raw note pengguna tidak diubah.
5. Tidak ada pemrosesan otomatis di background worker.
6. Tabel `nlp_suggestions` hanya menyimpan audit terstruktur saat pengguna
   menyimpan log: daftar acceptedSymptomTypes + ruleVersion + timestamp.
   Tidak ada raw note, tidak ada matched text, tidak ada health narrative.
7. Tidak ada profiling lintas siklus. Tidak ada shared mode / cloud / sync.

## Text normalizer

Urutan (deterministik):
1. Unicode NFKD + hapus combining marks (emoji tetap utuh).
2. Lowercase `Locale.ROOT`.
3. Phrase slang map (kunci terpanjang dulu).
4. Word-boundary slang map — "ga" tidak memakan "agak"/"gatal".
5. Possessive suffix "-nya" dihapus ("kramnya" -> "kram").
6. Elongation stripping: hanya kata dengan run 3+ huruf identik yang
   di-collapse; kata valid ("menggumpal", "mood") tidak tersentuh.
7. Punctuation & emoji difold jadi spasi; whitespace dinormalisasi.

## Rule dictionary convention

- Setiap rule typed (`SymptomRule`, `MoodRule`, ...) dengan daftar term
  normalized.
- Match memakai whole-word boundary dan phrase terpanjang lebih dulu.
- Menambah/mengubah rule WAJIB disertai unit test di
  `app/src/test/java/id/rona/app/domain/nlp/` dan lolos test safety
  (tidak boleh memunculkan wording diagnosis).

## Negation behavior

- Simple negation ("tidak", "nggak", dst.) membatalkan term dalam window
  3 kata sebelum, dengan batas klausa ("tapi", "tetapi", "namun", "dan").
- Existence negation ("tidak ada X") membatalkan descriptor sampai 4 kata
  setelah marker (bigram-aware).
- Resolved marker ("hilang", "reda", "sembuh", dst.) membatalkan term aktif
  dalam window 4 kata sebelum ATAU sesudah.
- Negasi pada klausa lain tidak bocor ("tidak pusing tapi mual").

## Confidence mapping

| Kondisi | Confidence |
|---|---|
| Term dictionary tepat, tanpa modifier | HIGH |
| Ada severity modifier yang cocok | MEDIUM–HIGH sesuai modifier |
| Rule fuzzy (mis. APPETITE_CHANGE) | MEDIUM |
| Ambiguitas/descriptor warna abnormal | MEDIUM |
| Tanpa anchor/descriptor kosong | LOW |

## Safety triage priority

Dievaluasi PERTAMA sebelum saran/gejala/knowledge card. Urutan tampil:
EMERGENCY > URGENT > CONSULT_SOON > SELF_CARE > INFO.

Grup aturan:
- Pregnancy: bleeding (CONSULT_SOON), heavy bleeding (URGENT), nyeri berat
  (URGENT), nyeri satu sisi (URGENT), pingsan (EMERGENCY), sakit kepala
  berat + gangguan penglihatan (URGENT), demam/cairan banyak (URGENT).
- Perdarahan menstruasi: sangat banyak (URGENT), + pusing/lemas (EMERGENCY).
- Keputihan: bau amis + gatal/perih (CONSULT_SOON), warna abnormal
  (CONSULT_SOON).
- Mental health immediate safety: self-harm wording (EMERGENCY).

Tidak ada nomor darurat hardcoded; tidak ada wording diagnosis.

## Knowledge card schema

`KnowledgeCard` (lihat `NlpModels.kt`): id, category, title, shortSummary,
whatMayHelp, whatToAvoidClaiming, whenToSeekHelp, triageLevel,
applicablePhases, applicableSymptoms, pregnancyModeOnly, medicalDisclaimer,
sourceCategory, reviewedAt, contentVersion.

Maksimal 3 card per analisis; urgent card lebih dulu; pregnancy card hanya
muncul bila pregnancy mode aktif atau konteks kehamilan terdeteksi.

## Explicit non-goals

- Bukan chatbot, bukan generative text.
- Bukan diagnosis medis; tidak menyebut nama penyakit.
- Bukan alat kontrasepsi/fertility yang menjanjikan hasil.
- Tidak memberi dosis obat/suplemen/antibiotik/hormonal therapy.
- Tidak ada "safe days".
- Tidak memproses note tanpa aksi pengguna.
- Tidak ada model remote/API key/LLM.

## Future P2 ONNX migration path

`LocalNoteAnalyzer` adalah titik ganti: implementasi `OnnxNoteAnalyzer`
cukup mengembalikan `NoteAnalysisResult` yang sama. Rule-based tetap
dipakai sebagai fallback/guard diagnosis. Tidak ada perubahan schema
atau public domain contract yang diperlukan.

## How to add or modify a safe rule

1. Edit dictionary yang relevan di `domain/nlp/dict/`.
2. Tambah unit test untuk term baru (positive + negation + boundary).
3. Jalankan seluruh test NLP: `./gradlew testDebugUnitTest`.
4. Pastikan test `noDiagnosisClaimsInSeedContent` & `SafetyTriageEngineTest`
   tetap hijau.
5. Commit dengan format `feat(nlp): ...`.

## How to update content with clinical review

1. Ubah `KnowledgeCardSeed.kt` hanya setelah review klinis.
2. Perbarui `reviewedAt` dan `contentVersion`.
3. Tambah test bila ada klaim baru.
4. Dokumentasikan perubahan di commit message.
