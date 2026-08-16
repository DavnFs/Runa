package id.rona.app.domain.nlp.dict

import id.rona.app.domain.nlp.P1DischargeDescriptor

/**
 * Discharge dictionary: purely descriptive descriptors. Deliberately no
 * diagnosis labels (no BV, no candidiasis, no STI) — see
 * docs/MEDICAL_CONTENT_BOUNDARIES.md.
 */
data class DischargeRule(
    val descriptor: P1DischargeDescriptor,
    val terms: List<String>,
)

object DischargeDictionary {

    val rules: List<DischargeRule> = listOf(
        DischargeRule(P1DischargeDescriptor.CLEAR, listOf("bening", "jernih", "transparan")),
        DischargeRule(P1DischargeDescriptor.WHITE, listOf("putih", "keputihan putih", "putih susu")),
        DischargeRule(P1DischargeDescriptor.YELLOW, listOf("kuning", "kekuningan", "kuning kehijauan")),
        DischargeRule(P1DischargeDescriptor.GREEN, listOf("hijau", "kehijauan")),
        DischargeRule(P1DischargeDescriptor.GREY, listOf("abu abu", "keabuan", "abu-abu")),
        DischargeRule(P1DischargeDescriptor.BROWN, listOf("coklat", "kecoklatan", "cokelat")),
        DischargeRule(P1DischargeDescriptor.WATERY, listOf("encer", "cair", "berair")),
        DischargeRule(P1DischargeDescriptor.STRETCHY, listOf("elastis", "melar", "licin", "seperti putih telur", "lendir bening")),
        DischargeRule(P1DischargeDescriptor.THICK, listOf("kental", "putih kental")),
        DischargeRule(P1DischargeDescriptor.CLUMPY, listOf("menggumpal", "gumpalan", "seperti ampas")),
        DischargeRule(P1DischargeDescriptor.ODOR_NONE, listOf("tidak berbau", "tidak bau", "tanpa bau")),
        DischargeRule(P1DischargeDescriptor.ODOR_FISHY, listOf("bau amis", "bau ikan", "amis", "bau menyengat", "bau tidak sedap", "berbau")),
        DischargeRule(P1DischargeDescriptor.ITCHING, listOf("gatal", "gatal di area kewanitaan", "gatal pada vagina")),
        DischargeRule(P1DischargeDescriptor.BURNING, listOf("perih", "terbakar", "panas", "nyeri saat buang air kecil")),
    )

    /** Anchor terms that indicate the note is talking about discharge at all. */
    val anchors: List<String> = listOf(
        "keputihan", "lendir", "cairan vagina", "cairan kewanitaan",
        "lendir serviks", "cairan di celana", "keputihan keluar",
    )
}
