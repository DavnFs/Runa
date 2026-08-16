package id.rona.app.domain.lock

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LockPolicyTest {

    @Test
    fun staysUnlockedWithinGrace() {
        assertThat(
            LockPolicy.shouldRelockOnForeground(nowMs = 100_000, unlockedAtMs = 80_000, graceSeconds = 30)
        ).isFalse()
    }

    @Test
    fun relocksAfterGrace() {
        assertThat(
            LockPolicy.shouldRelockOnForeground(nowMs = 111_000, unlockedAtMs = 80_000, graceSeconds = 30)
        ).isTrue()
    }

    @Test
    fun boundaryGraceIsRelock() {
        assertThat(
            LockPolicy.shouldRelockOnForeground(nowMs = 110_000, unlockedAtMs = 80_000, graceSeconds = 30)
        ).isTrue()
    }

    @Test
    fun zeroGraceAlwaysRelocks() {
        assertThat(
            LockPolicy.shouldRelockOnForeground(nowMs = 80_001, unlockedAtMs = 80_000, graceSeconds = 0)
        ).isTrue()
    }

    @Test
    fun neverUnlockedAlwaysRelocks() {
        assertThat(
            LockPolicy.shouldRelockOnForeground(nowMs = 100_000, unlockedAtMs = 0, graceSeconds = 30)
        ).isTrue()
    }

    @Test
    fun lockoutStartsAtThreshold() {
        assertThat(LockPolicy.lockoutSeconds(consecutiveFailures = 4)).isEqualTo(0)
        assertThat(LockPolicy.lockoutSeconds(consecutiveFailures = 5)).isEqualTo(30)
        assertThat(LockPolicy.lockoutSeconds(consecutiveFailures = 6)).isEqualTo(30)
    }
}
