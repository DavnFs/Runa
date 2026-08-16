package id.rona.app.domain.nlp.engine

import com.google.common.truth.Truth.assertThat
import id.rona.app.domain.model.Energy
import id.rona.app.domain.model.FlowLevel
import id.rona.app.domain.model.Mood
import id.rona.app.domain.nlp.ConfidenceLevel
import id.rona.app.domain.nlp.P1DischargeDescriptor
import id.rona.app.domain.nlp.PregnancyContextKind
import org.junit.Test

class MoodEnergyFlowExtractorTest {

    private fun norm(text: String) = TextNormalizer.normalize(text)

    @Test
    fun extractsMoodFromDictionary() {
        val moods = MoodEnergyFlowExtractor.extractMoods(norm("mood jelek hari ini"))
        assertThat(moods.map { it.mood }).contains(Mood.VERY_LOW)
    }

    @Test
    fun extractsSensitiveAsLowMoodMediumConfidence() {
        val moods = MoodEnergyFlowExtractor.extractMoods(norm("aku sensitif banget hari ini"))
        val sensitive = moods.first { it.mood == Mood.LOW }
        assertThat(sensitive.confidence).isEqualTo(ConfidenceLevel.MEDIUM)
    }

    @Test
    fun extractsAnxietyAsVeryLowMood() {
        val moods = MoodEnergyFlowExtractor.extractMoods(norm("gelisah sejak pagi"))
        assertThat(moods.map { it.mood }).contains(Mood.VERY_LOW)
    }

    @Test
    fun negatedMoodNotExtracted() {
        val moods = MoodEnergyFlowExtractor.extractMoods(norm("tidak sedih"))
        assertThat(moods.map { it.mood }).doesNotContain(Mood.LOW)
    }

    @Test
    fun alreadySelectedMoodSkipped() {
        val moods = MoodEnergyFlowExtractor.extractMoods(
            norm("mood jelek"),
            alreadySelected = Mood.VERY_LOW,
        )
        assertThat(moods.map { it.mood }).doesNotContain(Mood.VERY_LOW)
    }

    @Test
    fun extractsEnergy() {
        val energy = MoodEnergyFlowExtractor.extractEnergy(norm("energi habis"))
        assertThat(energy!!.energy).isEqualTo(Energy.VERY_LOW)
    }

    @Test
    fun negatedEnergyNotExtracted() {
        val energy = MoodEnergyFlowExtractor.extractEnergy(norm("tidak lelah"))
        assertThat(energy).isNull()
    }

    @Test
    fun extractsHeavyFlow() {
        val flow = MoodEnergyFlowExtractor.extractFlow(norm("darahnya deras banget"))
        assertThat(flow!!.flow).isEqualTo(FlowLevel.HEAVY)
    }

    @Test
    fun extractsSpotting() {
        val flow = MoodEnergyFlowExtractor.extractFlow(norm("muncul flek sedikit"))
        assertThat(flow!!.flow).isEqualTo(FlowLevel.SPOTTING)
    }
}

class DischargeExtractorTest {

    @Test
    fun noAnchorNoDischarge() {
        val result = DischargeExtractor.extract(TextNormalizer.normalize("hari ini biasa saja"))
        assertThat(result).isNull()
    }

    @Test
    fun anchorWithClearStretchy() {
        val result = DischargeExtractor.extract(
            TextNormalizer.normalize("keputihan bening licin seperti putih telur")
        )
        assertThat(result).isNotNull()
        assertThat(result!!.descriptors).contains(P1DischargeDescriptor.CLEAR)
        assertThat(result.descriptors).contains(P1DischargeDescriptor.STRETCHY)
        assertThat(result.itchingOrBurning).isFalse()
        assertThat(result.odorConcern).isFalse()
    }

    @Test
    fun fishyOdorWithItching() {
        val result = DischargeExtractor.extract(
            TextNormalizer.normalize("keputihan bau amis dan gatal")
        )
        assertThat(result).isNotNull()
        assertThat(result!!.odorConcern).isTrue()
        assertThat(result.itchingOrBurning).isTrue()
    }

    @Test
    fun negatedOdorNotExtracted() {
        val result = DischargeExtractor.extract(
            TextNormalizer.normalize("nggak ada keputihan berbau")
        )
        assertThat(result!!.descriptors).doesNotContain(P1DischargeDescriptor.ODOR_FISHY)
    }

    @Test
    fun abnormalColorLowersConfidence() {
        val result = DischargeExtractor.extract(
            TextNormalizer.normalize("keputihan kuning kehijauan")
        )
        assertThat(result!!.confidence).isEqualTo(ConfidenceLevel.MEDIUM)
        assertThat(result.descriptors).contains(P1DischargeDescriptor.YELLOW)
    }
}

class PregnancyContextExtractorTest {

    @Test
    fun positiveTestpackHighConfidence() {
        val ctx = PregnancyContextExtractor.extract(TextNormalizer.normalize("testpack positif pagi ini"))
        assertThat(ctx).isNotNull()
        assertThat(ctx!!.kind).isEqualTo(PregnancyContextKind.TEST_POSITIVE)
    }

    @Test
    fun suspectedPregnancyMediumConfidence() {
        val ctx = PregnancyContextExtractor.extract(TextNormalizer.normalize("telat haid 8 hari"))
        assertThat(ctx!!.kind).isEqualTo(PregnancyContextKind.SUSPECTED)
    }

    @Test
    fun bukanHamilNotActivated() {
        val ctx = PregnancyContextExtractor.extract(TextNormalizer.normalize("bukan hamil"))
        assertThat(ctx).isNull()
    }

    @Test
    fun redFlagsExtracted() {
        val flags = PregnancyContextExtractor.extractRedFlags(
            TextNormalizer.normalize("hamil dan pandangan kabur")
        )
        assertThat(flags.keys).contains(id.rona.app.domain.nlp.dict.PregnancyDictionary.PregnancyRedFlag.VISUAL_CHANGE)
    }
}
