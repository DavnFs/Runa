package id.rona.app.domain.nlp.dict

/**
 * Negation vocabulary. [NegationDetector] scans a window before/after a match
 * for these terms; distance and direction rules live in the detector.
 */
object NegationDictionary {

    /** Simple negations that directly cancel a following term. */
    val simpleNegations: List<String> = listOf(
        "tidak", "nggak", "nggak ada", "gak", "ga", "gk", "enggak", "tak",
        "bukan", "tanpa", "belum",
    )

    /** Phrase-level negations: "tidak ada X" / "nggak ada X". */
    val existenceNegations: List<String> = listOf(
        "tidak ada", "nggak ada", "gak ada", "ga ada", "tak ada", "belum ada", "tanpa",
    )

    /** Resolved/ended markers: "sudah hilang", "sudah tidak", "sudah mendingan". */
    val resolvedMarkers: List<String> = listOf(
        "hilang", "mendingan", "reda", "enakan", "sembuh", "berkurang",
        "membaik", "tidak lagi",
    )

    /** Complex multi-word negation phrases (checked before single terms). */
    val phraseNegations: List<String> = listOf(
        "tidak begitu", "tidak terlalu", "tidak terlalu terasa", "kurang terasa",
        "hampir tidak", "jarang", "tidak sampai",
    )
}
