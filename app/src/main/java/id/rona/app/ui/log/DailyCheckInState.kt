package id.rona.app.ui.log

/**
 * Sealed step state machine representation for the Guided Adaptive Question-Card flow.
 * Steps transition logically:
 * Initial -> {QuickEnergy | PeriodStartConfirm | Flow | Symptoms | Energy} ->
 *   (Flow -> Symptoms -> Severity(Conditional)) -> Energy -> Note -> Summary.
 *
 * Notes on integrity:
 * - There is never a fabricated medical/wellness value. "Baik-baik saja" only
 *   optionally records energy; mood/symptoms/flow/note are only persisted when
 *   the user explicitly confirms them.
 * - Period flow ("Aku sedang menstruasi") routes to [PeriodStartConfirm] when no
 *   ongoing period record covers the day. The PeriodRecord is only created after
 *   an explicit "Ya, mulai periode hari ini" confirmation.
 */
sealed interface DailyCheckInStep {
    /** Step 1: High-level quick entry selection (e.g. "Baik-baik saja" 1-tap fast path). */
    data object Initial : DailyCheckInStep

    /**
     * Step 1b: Optional energy question on the short "Baik-baik saja" path.
     * User may pick an energy level or skip; skipping saves no fabricated value.
     */
    data object QuickEnergy : DailyCheckInStep

    /**
     * Step 1c: Confirmation before creating a new PeriodRecord when the user
     * indicates they are on their period but no ongoing period covers the day.
     */
    data object PeriodStartConfirm : DailyCheckInStep

    /** Step 2: Menstrual / spotting flow level selection. */
    data object Flow : DailyCheckInStep

    /** Step 3: Multi-symptom selection. */
    data object Symptoms : DailyCheckInStep

    /** Step 4: Symptom severity configuration (conditionally rendered only if symptoms are selected). */
    data object Severity : DailyCheckInStep

    /** Step 5: Energy and mood wellness state. */
    data object Energy : DailyCheckInStep

    /** Step 6: Private notes and optional on-device rule-based NLP triage. */
    data object Note : DailyCheckInStep

    /** Step 7: Review summary and final save CTA. */
    data object Summary : DailyCheckInStep
}

/**
 * Options available on the Initial choice card.
 *
 * The spec's canonical first card lists three options
 * ("Baik-baik saja" / "Ada gejala" / "Aku sedang menstruasi"). An additional
 * "Catat suasana hati & energi" option is retained for direct entry; it
 * branches into the same Energy step as the quick path.
 */
enum class InitialChoiceOption {
    FEELING_GOOD,
    PERIOD_FLOW,
    SYMPTOMS,
    ENERGY_MOOD,
}

/**
 * Immutable, one-shot persistence lifecycle for the editor.
 * Replaces the old `saved: Boolean` / `isSaving` / `error` boolean cluster
 * so the UI can express Idle -> Saving -> Saved | Error without a permanently
 * true "saved" flag that would auto-close a reopened session.
 */
sealed interface SaveState {
    /** Nothing in flight; initial state, and returned to after acknowledgeSaved. */
    data object Idle : SaveState

    /** A save/delete is currently in flight. */
    data object Saving : SaveState

    /** Last write succeeded. Transient: acknowledge + reset to [Idle]. */
    data object Saved : SaveState

    /** Write failed. [message] is safe to surface (no raw note text). */
    data class Error(val message: String) : SaveState
}
