package id.rona.app.domain.nlp.dict

import id.rona.app.domain.model.Energy
import id.rona.app.domain.model.FlowLevel
import id.rona.app.domain.model.Mood

/** Typed mood rule: maps normalized Indonesian phrases to the MVP Mood scale. */
data class MoodRule(
    val mood: Mood,
    val terms: List<String>,
)

object MoodDictionary {
    val rules: List<MoodRule> = listOf(
        MoodRule(Mood.VERY_LOW, listOf("mood jelek", "mood buruk", "sedih sekali", "sangat sedih")),
        MoodRule(Mood.LOW, listOf("sedih", "mood turun", "down", "bad mood", "murung")),
        MoodRule(Mood.NEUTRAL, listOf("mood biasa", "mood stabil", "biasa saja")),
        MoodRule(Mood.GOOD, listOf("mood baik", "senang", "mood bagus", "happy")),
        MoodRule(Mood.GREAT, listOf("sangat senang", "mood sangat baik", "bahagia")),
    )

    /** Sensitivity/irritability/anxiety — the MVP scale has no dedicated axis. */
    val emotionalDescriptors: Map<String, Mood> = mapOf(
        "sensitif" to Mood.LOW,
        "mudah marah" to Mood.LOW,
        "emosian" to Mood.LOW,
        "emosi" to Mood.LOW,
        "cemas" to Mood.VERY_LOW,
        "gelisah" to Mood.VERY_LOW,
        "stres" to Mood.VERY_LOW,
        "sedih" to Mood.LOW,
        "tidak fokus" to Mood.LOW,
        "susah fokus" to Mood.LOW,
        "murung" to Mood.LOW,
    )
}

/** Typed energy rule. */
data class EnergyRule(
    val energy: Energy,
    val terms: List<String>,
)

object EnergyDictionary {
    val rules: List<EnergyRule> = listOf(
        EnergyRule(Energy.VERY_LOW, listOf("energi habis", "tidak bertenaga", "lemas sekali", "sangat lelah")),
        EnergyRule(Energy.LOW, listOf("lelah", "capek", "cape", "lemas", "lesu", "tidak bertenaga")),
        EnergyRule(Energy.NEUTRAL, listOf("energi biasa", "energi normal")),
        EnergyRule(Energy.HIGH, listOf("berenergi", "energik", "semangat")),
        EnergyRule(Energy.VERY_HIGH, listOf("sangat berenergi", "energi melimpah")),
    )
}

/** Typed flow rule (menstrual flow level from free text). */
data class FlowRule(
    val flow: FlowLevel,
    val terms: List<String>,
)

object FlowDictionary {
    val rules: List<FlowRule> = listOf(
        FlowRule(FlowLevel.SPOTTING, listOf("flek", "spotting", "bercak", "darah sedikit", "fleks")),
        FlowRule(FlowLevel.LIGHT, listOf("darah ringan", "flow ringan", "sedikit darah", "darahnya ringan")),
        FlowRule(FlowLevel.MEDIUM, listOf("darah sedang", "flow sedang", "darahnya sedang")),
        FlowRule(
            FlowLevel.HEAVY,
            listOf(
                "darah banyak", "darah deras", "flow deras", "darah sangat banyak",
                "tembus tiap jam", "ganti pembalut tiap jam", "darah tidak berhenti",
            ),
        ),
    )
}
