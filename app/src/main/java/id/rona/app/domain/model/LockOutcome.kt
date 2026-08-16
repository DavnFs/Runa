package id.rona.app.domain.model

enum class LockOutcome {
    NOT_REQUIRED,
    UNLOCKED,
    PIN_INCORRECT,
    PIN_VERIFIED,
    AUTHENTICATION_FAILED,
    LOCKOUT,
}
