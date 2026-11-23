package com.wearless.bonding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class SchedulerPolicyTest {

    @Test
    fun `default is WLB when nothing requested`() {
        assertEquals(Scheduler.WLB, SchedulerPolicy.resolve(null, emptyMap(), setOf("wifi", "mobile")))
    }

    @Test
    fun `explicit WLB honoured`() {
        assertEquals(
            Scheduler.WLB,
            SchedulerPolicy.resolve(Scheduler.WLB, mapOf("a" to 10L, "b" to 200L), setOf("a", "b")),
        )
    }

    @Test
    fun `explicit MinRTT honoured`() {
        assertEquals(
            Scheduler.MIN_RTT,
            SchedulerPolicy.resolve(Scheduler.MIN_RTT, emptyMap(), setOf("a", "b")),
        )
    }

    @Test
    fun `auto mode (null requested) picks WLB when nothing measured`() {
        // BondingConfig.scheduler defaults to null (auto). With no RTT data,
        // the policy must resolve to WLB.
        val config = BondingConfig(serverAddress = "vps:443", keyRef = "ref")
        assertNull(config.scheduler)
        val resolved = SchedulerPolicy.resolve(config.scheduler, emptyMap(), setOf("wifi", "mobile"))
        assertEquals(Scheduler.WLB, resolved)
    }

    @Test
    fun `auto mode (null requested) picks MinRTT on asymmetric links`() {
        // With real RTT data showing strong asymmetry, auto mode picks MinRTT.
        val rtts = mapOf("wifi" to 20L, "mobile" to 120L)
        assertEquals(Scheduler.MIN_RTT, SchedulerPolicy.resolve(null, rtts, rtts.keys))
    }

    @Test
    fun `similar RTTs stay on WLB`() {
        // Grounding [A4]: WLB is fine for links of similar latency.
        val rtts = mapOf("wifi" to 30L, "mobile" to 45L)
        assertEquals(Scheduler.WLB, SchedulerPolicy.resolve(null, rtts, rtts.keys))
    }

    @Test
    fun `strongly asymmetric RTTs switch to MinRTT`() {
        // Grounding [A4]: on asymmetric links WLB ~77 Mbps vs MinRTT ~212 Mbps
        // in the author's test — policy prefers MinRTT when links are unequal.
        val rtts = mapOf("wifi" to 20L, "mobile" to 120L) // ratio 6x >= 4x threshold
        assertEquals(Scheduler.MIN_RTT, SchedulerPolicy.resolve(null, rtts, rtts.keys))
    }

    @Test
    fun `single measured link stays WLB`() {
        assertEquals(
            Scheduler.WLB,
            SchedulerPolicy.resolve(null, mapOf("wifi" to 20L, "mobile" to 500L), setOf("wifi")),
        )
    }

    @Test
    fun `unmeasured links never trigger the switch`() {
        // Only "mobile" has a real RTT; wifi unmeasured → not enough evidence → WLB.
        assertEquals(
            Scheduler.WLB,
            SchedulerPolicy.resolve(null, mapOf("mobile" to 500L), setOf("wifi", "mobile")),
        )
    }

    @Test
    fun `reorder buffer always recommended`() {
        // [A4]: WLB under jitter recovered only with the reorder buffer (0.9 → 37.6 Mbps).
        assertTrue(SchedulerPolicy.reorderBufferRecommended(Scheduler.WLB))
        assertTrue(SchedulerPolicy.reorderBufferRecommended(Scheduler.MIN_RTT))
    }
}
