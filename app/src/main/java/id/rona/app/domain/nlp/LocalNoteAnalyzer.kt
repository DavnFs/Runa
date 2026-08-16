package id.rona.app.domain.nlp

/**
 * Future-proof analyzer contract.
 *
 * P1: RuleBasedNoteAnalyzer (pure Kotlin, deterministic, offline).
 * P2: an ONNX-backed implementation may replace the rule matcher while
 *     returning the same [NoteAnalysisResult] shape — no schema or UI change.
 */
interface LocalNoteAnalyzer {
    fun analyze(input: NoteAnalysisInput): NoteAnalysisResult
}
