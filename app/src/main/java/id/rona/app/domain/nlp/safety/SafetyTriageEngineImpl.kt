package id.rona.app.domain.nlp.safety

import id.rona.app.domain.nlp.SafetyAlert
import id.rona.app.domain.nlp.SafetyContext
import id.rona.app.domain.nlp.SafetyTriageEngine
import id.rona.app.domain.nlp.TriageLevel
import id.rona.app.domain.nlp.PregnancyContextKind
import id.rona.app.domain.nlp.P1DischargeDescriptor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deterministic safety triage. Evaluated BEFORE any suggestion or knowledge
 * output. Copy is calm, non-judgmental, Bahasa Indonesia, and free of
 * diagnosis wording (see docs/MEDICAL_CONTENT_BOUNDARIES.md).
 *
 * No emergency phone numbers are hardcoded: locale-specific services are not
 * configured for P1, so wording refers to nearest health facility / trusted person.
 */
@Singleton
class SafetyTriageEngineImpl @Inject constructor() : SafetyTriageEngine {

    override fun evaluate(context: SafetyContext): List<SafetyAlert> {
        val alerts = mutableListOf<SafetyAlert>()

        // ————— D. Mental health immediate safety (always first) —————
        if (context.selfHarmMentioned) {
            alerts += SafetyAlert(
                id = "safety_self_harm",
                title = "Keselamatanmu yang utama",
                message = "Jika kamu merasa tidak aman atau berpikir untuk melukai diri, " +
                    "segera hubungi orang tepercaya di sekitarmu atau cari pertolongan " +
                    "darurat di fasilitas kesehatan terdekat. Kamu tidak sendirian.",
                triageLevel = TriageLevel.EMERGENCY,
                matchedTerms = emptyList(),
            )
        }

        val pregnancyActive = context.pregnancyContext != null
        val pregnancyBleeding = context.pregnancyContext?.kind == PregnancyContextKind.BLEEDING_IN_PREGNANCY
        val heavyBleeding = context.heavyBleedingMentioned
        val severePain = context.severePainMentioned
        val fainting = context.faintingMentioned
        val visual = context.visualDisturbanceMentioned
        val fever = context.feverMentioned
        val fluidLeak = context.normalizedText.contains("cairan keluar banyak")

        // ————— A. Pregnancy context —————
        if (pregnancyActive) {
            if (pregnancyBleeding && heavyBleeding) {
                alerts += SafetyAlert(
                    id = "safety_pregnancy_heavy_bleeding",
                    title = "Perdarahan dalam kehamilan",
                    message = "Hubungi dokter/bidan/layanan kesehatan sesegera mungkin. " +
                        "Perdarahan dalam kehamilan perlu dibicarakan dengan tenaga kesehatan.",
                    triageLevel = TriageLevel.URGENT,
                    matchedTerms = emptyList(),
                )
            } else if (pregnancyBleeding) {
                alerts += SafetyAlert(
                    id = "safety_pregnancy_bleeding",
                    title = "Perdarahan dalam kehamilan",
                    message = "Perdarahan atau flek dalam kehamilan perlu dibicarakan " +
                        "dengan tenaga kesehatan. Pertimbangkan untuk segera menghubungi " +
                        "dokter atau bidan.",
                    triageLevel = TriageLevel.CONSULT_SOON,
                    matchedTerms = emptyList(),
                )
            }

            if (severePain && context.normalizedText.contains("satu sisi")) {
                alerts += SafetyAlert(
                    id = "safety_pregnancy_onesided_pain",
                    title = "Nyeri tajam satu sisi",
                    message = "Hubungi dokter/bidan/layanan kesehatan sesegera mungkin.",
                    triageLevel = TriageLevel.URGENT,
                    matchedTerms = emptyList(),
                )
            } else if (severePain) {
                alerts += SafetyAlert(
                    id = "safety_pregnancy_severe_pain",
                    title = "Nyeri perut berat",
                    message = "Hubungi dokter/bidan/layanan kesehatan sesegera mungkin.",
                    triageLevel = TriageLevel.URGENT,
                    matchedTerms = emptyList(),
                )
            }

            if (fainting) {
                alerts += SafetyAlert(
                    id = "safety_pregnancy_fainting",
                    title = "Pingsan atau hampir pingsan",
                    message = "Jika kamu merasa tidak aman atau gejalanya berat, segera " +
                        "cari pertolongan darurat di fasilitas kesehatan terdekat.",
                    triageLevel = TriageLevel.EMERGENCY,
                    matchedTerms = emptyList(),
                )
            }

            if (severePain && visual) {
                alerts += SafetyAlert(
                    id = "safety_pregnancy_headache_visual",
                    title = "Sakit kepala berat dan gangguan penglihatan",
                    message = "Hubungi dokter/bidan/layanan kesehatan sesegera mungkin.",
                    triageLevel = TriageLevel.URGENT,
                    matchedTerms = emptyList(),
                )
            }

            if (fever || fluidLeak) {
                alerts += SafetyAlert(
                    id = "safety_pregnancy_fever_fluid",
                    title = "Demam atau cairan keluar banyak",
                    message = "Hubungi dokter/bidan/layanan kesehatan sesegera mungkin.",
                    triageLevel = TriageLevel.URGENT,
                    matchedTerms = emptyList(),
                )
            }
        }

        // ————— B. Menstruation / heavy bleeding —————
        if (heavyBleeding && (fainting || context.normalizedText.contains("lemas") || context.normalizedText.contains("pusing"))) {
            alerts += SafetyAlert(
                id = "safety_heavy_bleeding_dizzy",
                title = "Perdarahan sangat banyak",
                message = "Jika kamu merasa tidak aman atau gejalanya berat, segera cari " +
                    "pertolongan darurat di fasilitas kesehatan terdekat.",
                triageLevel = TriageLevel.EMERGENCY,
                matchedTerms = emptyList(),
            )
        } else if (heavyBleeding) {
            alerts += SafetyAlert(
                id = "safety_heavy_bleeding",
                title = "Perdarahan sangat banyak",
                message = "Hubungi dokter/bidan/layanan kesehatan sesegera mungkin.",
                triageLevel = TriageLevel.URGENT,
                matchedTerms = emptyList(),
            )
        }

        // Severe pelvic/abdominal pain interfering with activity.
        if (severePain && context.normalizedText.contains("tidak bisa aktivitas")) {
            alerts += SafetyAlert(
                id = "safety_severe_pain_activity",
                title = "Nyeri yang mengganggu aktivitas",
                message = "Pertimbangkan berbicara dengan tenaga kesehatan, terutama " +
                    "jika nyeri terasa berbeda dari biasanya atau terus berlanjut.",
                triageLevel = TriageLevel.CONSULT_SOON,
                matchedTerms = emptyList(),
            )
        }

        // ————— C. Discharge —————
        val discharge = context.dischargeDescriptors
        val odorFishy = P1DischargeDescriptor.ODOR_FISHY in discharge
        val itching = P1DischargeDescriptor.ITCHING in discharge || context.dischargeItchingOrBurning
        val burning = P1DischargeDescriptor.BURNING in discharge
        val abnormalColor = discharge.any {
            it in setOf(P1DischargeDescriptor.YELLOW, P1DischargeDescriptor.GREEN, P1DischargeDescriptor.GREY)
        }

        if (odorFishy && (itching || burning)) {
            alerts += SafetyAlert(
                id = "safety_discharge_odor_itching",
                title = "Keputihan dengan bau dan rasa tidak nyaman",
                message = "Pertimbangkan berbicara dengan tenaga kesehatan.",
                triageLevel = TriageLevel.CONSULT_SOON,
                matchedTerms = emptyList(),
            )
        }

        if (abnormalColor && (severePain || fever || heavyBleeding)) {
            alerts += SafetyAlert(
                id = "safety_discharge_abnormal_combo",
                title = "Keputihan tidak biasa disertai gejala lain",
                message = "Pertimbangkan berbicara dengan tenaga kesehatan.",
                triageLevel = TriageLevel.CONSULT_SOON,
                matchedTerms = emptyList(),
            )
        } else if (abnormalColor) {
            alerts += SafetyAlert(
                id = "safety_discharge_abnormal",
                title = "Keputihan dengan warna tidak biasa",
                message = "Bila disertai bau menyengat, gatal, perih, nyeri, atau demam, " +
                    "pertimbangkan bicara dengan tenaga kesehatan.",
                triageLevel = TriageLevel.CONSULT_SOON,
                matchedTerms = emptyList(),
            )
        }

        // Dedupe by id, sort by severity (EMERGENCY first).
        return alerts.distinctBy { it.id }
            .sortedByDescending { it.triageLevel.ordinal }
    }
}
