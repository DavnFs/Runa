package id.rona.app.domain.nlp.knowledge

import id.rona.app.domain.engine.CyclePhase
import id.rona.app.domain.model.SymptomType
import id.rona.app.domain.nlp.KnowledgeCard
import id.rona.app.domain.nlp.KnowledgeCategory
import id.rona.app.domain.nlp.SourceCategory
import id.rona.app.domain.nlp.TriageLevel
import java.time.LocalDate

/**
 * Typed local knowledge seed (Bahasa Indonesia). Content policy:
 * - warm, short, non-judgmental, non-diagnostic.
 * - no food taboos per phase, no "safe days", no dose/supplement advice.
 * - no diagnosis names anywhere in card text.
 *
 * Updating content requires clinical review — see docs/MEDICAL_CONTENT_BOUNDARIES.md.
 */
object KnowledgeCardSeed {

    val cards: List<KnowledgeCard> = listOf(
        // ————— 1. Cycle basics —————
        KnowledgeCard(
            id = "cycle_basics",
            category = KnowledgeCategory.CYCLE_BASICS,
            title = "Siklus berbeda tiap orang",
            shortSummary = "Panjang siklus yang umum berkisar luas, dan milikmu bisa berbeda dari 28 hari. Prediksi rona hanyalah estimasi dari riwayatmu.",
            whatMayHelp = listOf(
                "Mencatat tanggal mulai secara konsisten membantu estimasi menjadi lebih personal.",
                "Perhatikan pola tubuhmu tanpa memaksakan angka tertentu.",
            ),
            whatToAvoidClaiming = listOf(
                "Siklus 'normal' harus 28 hari.",
                "Prediksi aplikasi dapat dipakai sebagai kontrasepsi atau penentu kehamilan.",
            ),
            whenToSeekHelp = listOf(
                "Bila ada perubahan besar dari pola biasanya dan kamu merasa khawatir.",
            ),
            triageLevel = TriageLevel.INFO,
            applicablePhases = setOf(CyclePhase.EARLY, CyclePhase.MIDDLE, CyclePhase.LATE),
            applicableSymptoms = emptySet(),
            pregnancyModeOnly = false,
            medicalDisclaimer = "Ini informasi umum, bukan saran medis.",
            sourceCategory = SourceCategory.PRODUCT_SAFETY_POLICY,
            reviewedAt = LocalDate.of(2026, 8, 1),
            contentVersion = "1.0",
        ),

        // ————— 2. Period pain —————
        KnowledgeCard(
            id = "period_pain",
            category = KnowledgeCategory.PERIOD_PAIN,
            title = "Nyeri saat menstruasi",
            shortSummary = "Kram ringan hingga sedang dapat terjadi saat menstruasi. Mencatat seberapa berat dan dampaknya membantumu mengenali pola.",
            whatMayHelp = listOf(
                "Istirahat sesuai kebutuhan tubuhmu.",
                "Minum cukup air dan makan teratur.",
                "Aktivitas ringan sesuai kenyamanan dan kompres hangat bisa menjadi pilihan self-care umum.",
            ),
            whatToAvoidClaiming = listOf(
                "Makanan/minuman tertentu pasti menghilangkan nyeri.",
                "Nyeri berat berarti ada penyakit tertentu.",
            ),
            whenToSeekHelp = listOf(
                "Nyeri berat, berbeda dari biasanya, berulang, atau mengganggu aktivitas — pertimbangkan konsultasi tenaga kesehatan.",
            ),
            triageLevel = TriageLevel.SELF_CARE,
            applicablePhases = setOf(CyclePhase.EARLY),
            applicableSymptoms = setOf(SymptomType.KRAM, SymptomType.BACKACHE),
            pregnancyModeOnly = false,
            medicalDisclaimer = "Ini informasi umum, bukan saran medis.",
            sourceCategory = SourceCategory.NHS_PATIENT_GUIDANCE,
            reviewedAt = LocalDate.of(2026, 8, 1),
            contentVersion = "1.0",
        ),

        // ————— 3. PMS / mood —————
        KnowledgeCard(
            id = "pms_and_mood",
            category = KnowledgeCategory.PMS_AND_MOOD,
            title = "Mood dan energi menjelang periode",
            shortSummary = "Perubahan mood, energi, tidur, dan nafsu makan bisa dicatat sebagai pola personal. Ini bukan 'kekurangan' dirimu.",
            whatMayHelp = listOf(
                "Bersikap lembut pada diri sendiri saat mood sedang rendah.",
                "Tidur dan makan teratur sesuai kemampuanmu.",
            ),
            whatToAvoidClaiming = listOf(
                "Perubahan mood berarti gangguan mental.",
                "Perubahan mood berarti ketidakseimbangan hormon.",
            ),
            whenToSeekHelp = listOf(
                "Bila mood sangat memengaruhi keseharianmu dalam waktu lama — tidak ada salahnya bicara dengan tenaga kesehatan.",
            ),
            triageLevel = TriageLevel.SELF_CARE,
            applicablePhases = setOf(CyclePhase.LATE),
            applicableSymptoms = setOf(SymptomType.SLEEP_ISSUE, SymptomType.APPETITE_CHANGE, SymptomType.FATIGUE),
            pregnancyModeOnly = false,
            medicalDisclaimer = "Ini informasi umum, bukan saran medis.",
            sourceCategory = SourceCategory.SYSTEMATIC_REVIEW,
            reviewedAt = LocalDate.of(2026, 8, 1),
            contentVersion = "1.0",
        ),

        // ————— 4. Discharge normal context —————
        KnowledgeCard(
            id = "discharge_normal",
            category = KnowledgeCategory.DISCHARGE,
            title = "Keputihan bisa berubah sepanjang siklus",
            shortSummary = "Lendir vagina dapat lebih bening, basah, licin, atau elastis di sekitar pertengahan siklus. Ini gambaran umum, bukan aturan untuk semua orang.",
            whatMayHelp = listOf(
                "Mencatat tekstur dan warnanya membantumu mengenali pola pribadi.",
            ),
            whatToAvoidClaiming = listOf(
                "Lendir tertentu adalah penentu pasti ovulasi.",
                "Mengamati lendir bisa dipakai sebagai metode kontrasepsi yang andal.",
            ),
            whenToSeekHelp = listOf(
                "Bila ada perubahan kuat dari pola biasa disertai bau menyengat, gatal, perih, nyeri, demam, atau perdarahan tidak biasa.",
            ),
            triageLevel = TriageLevel.INFO,
            applicablePhases = setOf(CyclePhase.EARLY, CyclePhase.MIDDLE, CyclePhase.LATE),
            applicableSymptoms = emptySet(),
            pregnancyModeOnly = false,
            medicalDisclaimer = "Ini informasi umum, bukan saran medis.",
            sourceCategory = SourceCategory.NHS_PATIENT_GUIDANCE,
            reviewedAt = LocalDate.of(2026, 8, 1),
            contentVersion = "1.0",
        ),

        // ————— 5. Discharge consultation —————
        KnowledgeCard(
            id = "discharge_consultation",
            category = KnowledgeCategory.DISCHARGE,
            title = "Kapan membicarakan keputihan",
            shortSummary = "Bila keputihan berubah kuat dari pola biasanya dan disertai rasa tidak nyaman, ada baiknya dibicarakan dengan tenaga kesehatan.",
            whatMayHelp = listOf(
                "Catat warna, bau, dan rasa tidak nyaman untuk membantu diskusi.",
            ),
            whatToAvoidClaiming = listOf(
                "Nama penyakit tertentu sebagai penjelasan keputihan.",
                "Pengobatan sendiri tanpa konsultasi.",
            ),
            whenToSeekHelp = listOf(
                "Bau menyengat/amis, gatal, perih, nyeri panggul, demam, atau perdarahan tidak biasa — pertimbangkan bicara dengan tenaga kesehatan.",
            ),
            triageLevel = TriageLevel.CONSULT_SOON,
            applicablePhases = setOf(CyclePhase.EARLY, CyclePhase.MIDDLE, CyclePhase.LATE),
            applicableSymptoms = emptySet(),
            pregnancyModeOnly = false,
            medicalDisclaimer = "Ini informasi umum, bukan saran medis.",
            sourceCategory = SourceCategory.CDC_GUIDANCE,
            reviewedAt = LocalDate.of(2026, 8, 1),
            contentVersion = "1.0",
        ),

        // ————— 6. Fertility limitations —————
        KnowledgeCard(
            id = "fertility_limitations",
            category = KnowledgeCategory.FERTILITY_LIMITATIONS,
            title = "Batas informasi masa subur",
            shortSummary = "Ovulasi tidak dapat dipastikan hanya dari prediksi kalender. Catatan siklus dan lendir membantu memahami pola, bukan jaminan.",
            whatMayHelp = listOf(
                "Gunakan rona untuk memahami pola pribadimu.",
            ),
            whatToAvoidClaiming = listOf(
                "Ada 'hari aman' yang pasti dari kalender.",
                "Prediksi kalender bisa mencegah atau menjamin kehamilan.",
            ),
            whenToSeekHelp = listOf(
                "Untuk keputusan kontrasepsi atau merencanakan kehamilan, bicarakan dengan tenaga kesehatan.",
            ),
            triageLevel = TriageLevel.INFO,
            applicablePhases = setOf(CyclePhase.MIDDLE),
            applicableSymptoms = emptySet(),
            pregnancyModeOnly = false,
            medicalDisclaimer = "Bukan alat kontrasepsi dan bukan penentu pasti kesuburan.",
            sourceCategory = SourceCategory.ACOG_GUIDANCE,
            reviewedAt = LocalDate.of(2026, 8, 1),
            contentVersion = "1.0",
        ),

        // ————— 7. Pregnancy general —————
        KnowledgeCard(
            id = "pregnancy_general",
            category = KnowledgeCategory.PREGNANCY_GENERAL,
            title = "Dukungan umum selama kehamilan",
            shortSummary = "Nutrisi seimbang, hidrasi cukup, dan istirahat adalah dukungan umum. Pemeriksaan antenatal secara teratur penting.",
            whatMayHelp = listOf(
                "Makan teratur dengan variasi makanan sesuai kenyamanan.",
                "Minum cukup air dan istirahat saat lelah.",
            ),
            whatToAvoidClaiming = listOf(
                "Diet ketat tertentu untuk kehamilan.",
                "Dosis vitamin atau suplemen tertentu tanpa tenaga kesehatan.",
            ),
            whenToSeekHelp = listOf(
                "Jadwalkan dan ikuti pemeriksaan antenatal dengan tenaga kesehatan.",
            ),
            triageLevel = TriageLevel.SELF_CARE,
            applicablePhases = setOf(CyclePhase.EARLY, CyclePhase.MIDDLE, CyclePhase.LATE),
            applicableSymptoms = emptySet(),
            pregnancyModeOnly = true,
            medicalDisclaimer = "Ini informasi umum, bukan saran medis.",
            sourceCategory = SourceCategory.WHO_GUIDANCE,
            reviewedAt = LocalDate.of(2026, 8, 1),
            contentVersion = "1.0",
        ),

        // ————— 8. Pregnancy nausea —————
        KnowledgeCard(
            id = "pregnancy_nausea",
            category = KnowledgeCategory.PREGNANCY_NAUSEA,
            title = "Mual saat hamil",
            shortSummary = "Mual dapat terjadi terutama di awal kehamilan. Beberapa orang merasa terbantu dengan makan porsi kecil lebih sering.",
            whatMayHelp = listOf(
                "Makan porsi kecil lebih sering bila cocok untukmu.",
                "Minum cukup air dan hindari pemicu pribadimu.",
            ),
            whatToAvoidClaiming = listOf(
                "Saran obat atau suplemen tertentu tanpa tenaga kesehatan.",
            ),
            whenToSeekHelp = listOf(
                "Muntah terus menerus atau tidak bisa minum — bicarakan dengan tenaga kesehatan.",
            ),
            triageLevel = TriageLevel.SELF_CARE,
            applicablePhases = setOf(CyclePhase.EARLY),
            applicableSymptoms = setOf(SymptomType.NAUSEA),
            pregnancyModeOnly = true,
            medicalDisclaimer = "Ini informasi umum, bukan saran medis.",
            sourceCategory = SourceCategory.NHS_PATIENT_GUIDANCE,
            reviewedAt = LocalDate.of(2026, 8, 1),
            contentVersion = "1.0",
        ),

        // ————— 9. Food and hydration —————
        KnowledgeCard(
            id = "food_and_hydration",
            category = KnowledgeCategory.FOOD_AND_HYDRATION,
            title = "Makan dan minum yang mendukung",
            shortSummary = "Sebagian orang merasa terbantu dengan makan teratur, cukup minum, serat, protein, buah dan sayur. Catat apa yang terasa cocok untuk tubuhmu.",
            whatMayHelp = listOf(
                "Makan teratur dan minum cukup air.",
                "Catat makanan apa yang terasa cocok untuk tubuhmu.",
            ),
            whatToAvoidClaiming = listOf(
                "Makanan tertentu 'melancarkan darah'.",
                "Makanan tertentu 'menyeimbangkan hormon' atau 'menyembuhkan PMS'.",
                "Pantangan makanan universal berdasarkan fase siklus.",
            ),
            whenToSeekHelp = listOf(
                "Bila keluhan fisik mengganggu aktivitas dan tidak membaik.",
            ),
            triageLevel = TriageLevel.INFO,
            applicablePhases = setOf(CyclePhase.EARLY, CyclePhase.MIDDLE, CyclePhase.LATE),
            applicableSymptoms = emptySet(),
            pregnancyModeOnly = false,
            medicalDisclaimer = "Ini informasi umum, bukan saran medis.",
            sourceCategory = SourceCategory.WHO_GUIDANCE,
            reviewedAt = LocalDate.of(2026, 8, 1),
            contentVersion = "1.0",
        ),

        // ————— 10. Menstrual phase cards —————
        KnowledgeCard(
            id = "phase_menstruation",
            category = KnowledgeCategory.MENSTRUATION,
            title = "Fase menstruasi",
            shortSummary = "Ini fase awal siklus. Energi dan mood setiap orang bisa berbeda — tidak ada 'seharusnya' tertentu.",
            whatMayHelp = listOf(
                "Istirahat dan hidrasi sesuai kebutuhan.",
            ),
            whatToAvoidClaiming = listOf(
                "Semua orang pasti lelah/nyeri saat menstruasi.",
            ),
            whenToSeekHelp = listOf(
                "Nyeri berat atau perdarahan jauh lebih banyak dari biasanya.",
            ),
            triageLevel = TriageLevel.INFO,
            applicablePhases = setOf(CyclePhase.EARLY),
            applicableSymptoms = emptySet(),
            pregnancyModeOnly = false,
            medicalDisclaimer = "Ini informasi umum, bukan saran medis.",
            sourceCategory = SourceCategory.PRODUCT_SAFETY_POLICY,
            reviewedAt = LocalDate.of(2026, 8, 1),
            contentVersion = "1.0",
        ),
        KnowledgeCard(
            id = "phase_follicular",
            category = KnowledgeCategory.MENSTRUATION,
            title = "Fase folikular",
            shortSummary = "Setelah menstruasi, sebagian orang merasa energi meningkat. Ini gambaran umum, bukan aturan.",
            whatMayHelp = listOf(
                "Gunakan momen berenergi untuk aktivitas yang kamu sukai.",
            ),
            whatToAvoidClaiming = listOf(
                "Semua orang pasti berenergi di fase ini.",
            ),
            whenToSeekHelp = emptyList(),
            triageLevel = TriageLevel.INFO,
            applicablePhases = setOf(CyclePhase.EARLY, CyclePhase.MIDDLE),
            applicableSymptoms = emptySet(),
            pregnancyModeOnly = false,
            medicalDisclaimer = "Ini informasi umum, bukan saran medis.",
            sourceCategory = SourceCategory.PRODUCT_SAFETY_POLICY,
            reviewedAt = LocalDate.of(2026, 8, 1),
            contentVersion = "1.0",
        ),
        KnowledgeCard(
            id = "phase_ovulation_est",
            category = KnowledgeCategory.MENSTRUATION,
            title = "Perkiraan jendela ovulasi",
            shortSummary = "Aplikasi hanya bisa memperkirakan rentang, bukan hari pasti. Ini bukan alat kontrasepsi.",
            whatMayHelp = listOf(
                "Gunakan sebagai gambaran pola, bukan kepastian.",
            ),
            whatToAvoidClaiming = listOf(
                "Hari ovulasi yang pasti.",
                "Hari aman untuk hubungan tanpa kontrasepsi.",
            ),
            whenToSeekHelp = emptyList(),
            triageLevel = TriageLevel.INFO,
            applicablePhases = setOf(CyclePhase.MIDDLE),
            applicableSymptoms = emptySet(),
            pregnancyModeOnly = false,
            medicalDisclaimer = "Bukan alat kontrasepsi dan bukan penentu pasti kesuburan.",
            sourceCategory = SourceCategory.PRODUCT_SAFETY_POLICY,
            reviewedAt = LocalDate.of(2026, 8, 1),
            contentVersion = "1.0",
        ),
        KnowledgeCard(
            id = "phase_luteal",
            category = KnowledgeCategory.MENSTRUATION,
            title = "Fase luteal",
            shortSummary = "Menjelang periode, sebagian orang mencatat perubahan mood, tidur, atau nafsu makan. Pola pribadi lebih berarti dari aturan umum.",
            whatMayHelp = listOf(
                "Catat pola pribadimu dan bersikap lembut pada diri sendiri.",
            ),
            whatToAvoidClaiming = listOf(
                "Semua orang pasti mengalami PMS tertentu.",
            ),
            whenToSeekHelp = listOf(
                "Bila mood sangat mengganggu keseharian dalam waktu lama.",
            ),
            triageLevel = TriageLevel.INFO,
            applicablePhases = setOf(CyclePhase.LATE),
            applicableSymptoms = emptySet(),
            pregnancyModeOnly = false,
            medicalDisclaimer = "Ini informasi umum, bukan saran medis.",
            sourceCategory = SourceCategory.PRODUCT_SAFETY_POLICY,
            reviewedAt = LocalDate.of(2026, 8, 1),
            contentVersion = "1.0",
        ),

        // ————— 11. Sleep and activity —————
        KnowledgeCard(
            id = "sleep_and_activity",
            category = KnowledgeCategory.SLEEP_AND_ACTIVITY,
            title = "Tidur dan aktivitas ringan",
            shortSummary = "Tidur teratur dan gerak ringan sesuai kenyamanan bisa membantu banyak orang. Bukan keharusan untuk semua.",
            whatMayHelp = listOf(
                "Usahakan waktu tidur yang konsisten.",
                "Aktivitas ringan seperti jalan santai bila nyaman.",
            ),
            whatToAvoidClaiming = listOf(
                "Olahraga tertentu pasti memperbaiki semua keluhan.",
            ),
            whenToSeekHelp = listOf(
                "Gangguan tidur berkepanjangan yang mengganggu keseharian.",
            ),
            triageLevel = TriageLevel.INFO,
            applicablePhases = setOf(CyclePhase.EARLY, CyclePhase.MIDDLE, CyclePhase.LATE),
            applicableSymptoms = setOf(SymptomType.SLEEP_ISSUE, SymptomType.FATIGUE),
            pregnancyModeOnly = false,
            medicalDisclaimer = "Ini informasi umum, bukan saran medis.",
            sourceCategory = SourceCategory.WHO_GUIDANCE,
            reviewedAt = LocalDate.of(2026, 8, 1),
            contentVersion = "1.0",
        ),

        // ————— 12. Privacy and data —————
        KnowledgeCard(
            id = "privacy_and_data",
            category = KnowledgeCategory.PRIVACY_AND_DATA,
            title = "Data tetap di ponselmu",
            shortSummary = "Analisis catatan berjalan sepenuhnya di perangkat. Tidak ada cloud, tidak ada akun, dan tidak ada data yang keluar dari ponselmu.",
            whatMayHelp = listOf(
                "Buka Pengaturan → Kebijakan privasi untuk detail lengkap.",
            ),
            whatToAvoidClaiming = emptyList(),
            whenToSeekHelp = emptyList(),
            triageLevel = TriageLevel.INFO,
            applicablePhases = setOf(CyclePhase.EARLY, CyclePhase.MIDDLE, CyclePhase.LATE),
            applicableSymptoms = emptySet(),
            pregnancyModeOnly = false,
            medicalDisclaimer = null,
            sourceCategory = SourceCategory.PRODUCT_SAFETY_POLICY,
            reviewedAt = LocalDate.of(2026, 8, 1),
            contentVersion = "1.0",
        ),
    )
}
