package id.rona.app.domain.nlp.safety

import com.google.common.truth.Truth.assertThat
import id.rona.app.domain.nlp.P1DischargeDescriptor
import id.rona.app.domain.nlp.PregnancyContext
import id.rona.app.domain.nlp.PregnancyContextKind
import id.rona.app.domain.nlp.ConfidenceLevel
import id.rona.app.domain.nlp.SafetyContext
import id.rona.app.domain.nlp.TriageLevel
import org.junit.Test

class SafetyTriageEngineTest {

    private val engine = SafetyTriageEngineImpl()

    @Test
    fun pregnancyBleedingIsConsultSoon() {
        val alerts = engine.evaluate(
            SafetyContext(
                pregnancyContext = PregnancyContext(PregnancyContextKind.BLEEDING_IN_PREGNANCY, ConfidenceLevel.HIGH, listOf("flek saat hamil")),
            )
        )
        assertThat(alerts.map { it.triageLevel }).contains(TriageLevel.CONSULT_SOON)
    }

    @Test
    fun pregnancyHeavyBleedingIsUrgent() {
        val alerts = engine.evaluate(
            SafetyContext(
                pregnancyContext = PregnancyContext(PregnancyContextKind.BLEEDING_IN_PREGNANCY, ConfidenceLevel.HIGH, emptyList()),
                heavyBleedingMentioned = true,
            )
        )
        assertThat(alerts.map { it.triageLevel }).contains(TriageLevel.URGENT)
    }

    @Test
    fun pregnancySevereAbdominalPainIsUrgent() {
        val alerts = engine.evaluate(
            SafetyContext(
                pregnancyContext = PregnancyContext(PregnancyContextKind.SUSPECTED, ConfidenceLevel.MEDIUM, emptyList()),
                severePainMentioned = true,
            )
        )
        assertThat(alerts.map { it.triageLevel }).contains(TriageLevel.URGENT)
    }

    @Test
    fun pregnancyOneSidedPainIsUrgent() {
        val alerts = engine.evaluate(
            SafetyContext(
                pregnancyContext = PregnancyContext(PregnancyContextKind.SUSPECTED, ConfidenceLevel.MEDIUM, emptyList()),
                severePainMentioned = true,
                normalizedText = "nyeri perut satu sisi",
            )
        )
        assertThat(alerts.map { it.triageLevel }).contains(TriageLevel.URGENT)
        assertThat(alerts.any { it.id == "safety_pregnancy_onesided_pain" }).isTrue()
    }

    @Test
    fun pregnancyFaintingIsEmergency() {
        val alerts = engine.evaluate(
            SafetyContext(
                pregnancyContext = PregnancyContext(PregnancyContextKind.SUSPECTED, ConfidenceLevel.MEDIUM, emptyList()),
                faintingMentioned = true,
            )
        )
        assertThat(alerts.map { it.triageLevel }).contains(TriageLevel.EMERGENCY)
    }

    @Test
    fun pregnancyHeadacheWithVisualIsUrgent() {
        val alerts = engine.evaluate(
            SafetyContext(
                pregnancyContext = PregnancyContext(PregnancyContextKind.SUSPECTED, ConfidenceLevel.MEDIUM, emptyList()),
                severePainMentioned = true,
                visualDisturbanceMentioned = true,
            )
        )
        assertThat(alerts.map { it.triageLevel }).contains(TriageLevel.URGENT)
        assertThat(alerts.any { it.id == "safety_pregnancy_headache_visual" }).isTrue()
    }

    @Test
    fun pregnancyFeverIsUrgent() {
        val alerts = engine.evaluate(
            SafetyContext(
                pregnancyContext = PregnancyContext(PregnancyContextKind.SUSPECTED, ConfidenceLevel.MEDIUM, emptyList()),
                feverMentioned = true,
            )
        )
        assertThat(alerts.map { it.triageLevel }).contains(TriageLevel.URGENT)
    }

    @Test
    fun heavyMenstrualBleedingIsUrgent() {
        val alerts = engine.evaluate(SafetyContext(heavyBleedingMentioned = true))
        assertThat(alerts.map { it.triageLevel }).contains(TriageLevel.URGENT)
    }

    @Test
    fun heavyBleedingWithDizzinessIsEmergency() {
        val alerts = engine.evaluate(
            SafetyContext(
                heavyBleedingMentioned = true,
                normalizedText = "darah sangat banyak dan pusing",
            )
        )
        assertThat(alerts.map { it.triageLevel }).contains(TriageLevel.EMERGENCY)
    }

    @Test
    fun severePainImpactingActivityIsConsultSoon() {
        val alerts = engine.evaluate(
            SafetyContext(
                severePainMentioned = true,
                normalizedText = "sakit sangat dan tidak bisa aktivitas",
            )
        )
        assertThat(alerts.map { it.triageLevel }).contains(TriageLevel.CONSULT_SOON)
    }

    @Test
    fun dischargeOdorWithItchingIsConsultSoon() {
        val alerts = engine.evaluate(
            SafetyContext(
                dischargeDescriptors = setOf(P1DischargeDescriptor.ODOR_FISHY),
                dischargeItchingOrBurning = true,
            )
        )
        assertThat(alerts.map { it.triageLevel }).contains(TriageLevel.CONSULT_SOON)
    }

    @Test
    fun abnormalDischargeColorIsConsultSoon() {
        val alerts = engine.evaluate(
            SafetyContext(dischargeDescriptors = setOf(P1DischargeDescriptor.GREEN))
        )
        assertThat(alerts.map { it.triageLevel }).contains(TriageLevel.CONSULT_SOON)
    }

    @Test
    fun selfHarmIsEmergency() {
        val alerts = engine.evaluate(SafetyContext(selfHarmMentioned = true))
        assertThat(alerts.map { it.triageLevel }).contains(TriageLevel.EMERGENCY)
        assertThat(alerts.first().id).isEqualTo("safety_self_harm")
    }

    @Test
    fun negatedSymptomsDoNotTriggerTriage() {
        // "tidak pusing" alone — no flags set by the analyzer's guard.
        val alerts = engine.evaluate(
            SafetyContext(
                normalizedText = "tidak pusing",
                detectedSymptoms = emptySet(),
            )
        )
        assertThat(alerts).isEmpty()
    }

    @Test
    fun emptyContextNoAlerts() {
        assertThat(engine.evaluate(SafetyContext())).isEmpty()
    }

    @Test
    fun alertsSortedBySeverityDescending() {
        val alerts = engine.evaluate(
            SafetyContext(
                pregnancyContext = PregnancyContext(PregnancyContextKind.SUSPECTED, ConfidenceLevel.MEDIUM, emptyList()),
                faintingMentioned = true,
                heavyBleedingMentioned = true,
            )
        )
        val levels: List<TriageLevel> = alerts.map { it.triageLevel }
        val sorted: List<TriageLevel> = levels.sortedByDescending { it.ordinal }
        assertThat(levels).isEqualTo(sorted)
    }

    @Test
    fun noDiagnosisWordingInAlerts() {
        val alerts = engine.evaluate(
            SafetyContext(
                pregnancyContext = PregnancyContext(PregnancyContextKind.BLEEDING_IN_PREGNANCY, ConfidenceLevel.HIGH, emptyList()),
                heavyBleedingMentioned = true,
                dischargeDescriptors = setOf(P1DischargeDescriptor.ODOR_FISHY),
                dischargeItchingOrBurning = true,
                selfHarmMentioned = true,
            )
        )
        val allText = alerts.joinToString(" ") { it.title + " " + it.message }
        assertThat(allText.lowercase()).doesNotContain("pcos")
        assertThat(allText.lowercase()).doesNotContain("endometriosis")
        assertThat(allText.lowercase()).doesNotContain("kandidiasis")
        assertThat(allText.lowercase()).doesNotContain("anemia")
        assertThat(allText.lowercase()).doesNotContain("depresi")
        assertThat(allText.lowercase()).doesNotContain("hormonal imbalance")
    }

    @Test
    fun noHardcodedEmergencyPhoneNumber() {
        val alerts = engine.evaluate(SafetyContext(selfHarmMentioned = true))
        val allText = alerts.joinToString(" ") { it.title + " " + it.message }
        assertThat(allText).doesNotContain("1-1")
        assertThat(allText).doesNotContain("911")
        assertThat(allText).doesNotContain("112")
    }
}
