package com.wearless.bonding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StatsAggregatorTest {

    @Test
    fun `unmeasured counters produce zeros and isMeasured false`() {
        val snap = StatsAggregator.aggregate(
            listOf(
                StatsAggregator.RawLinkCounters(linkId = "wifi", rxBps = 12345, txBps = 999, rxBytes = 1_000_000, isMeasured = false),
            )
        )
        val link = snap.links.single()
        assertEquals("wifi", link.linkId)
        assertEquals(0L, link.rxBps)
        assertEquals(0L, link.txBps)
        assertEquals(0L, link.rxBytes)
        assertEquals(0L, link.txBytes)
        assertNull(link.rttMs)
        assertNull(link.lossPct)
        assertFalse(link.isMeasured)
        assertEquals(0L, snap.aggregatedBps)
        assertFalse(snap.isMeasured)
    }

    @Test
    fun `unmeasured counters ignore plausible-looking inputs entirely`() {
        // Even if the platform layer accidentally passes numbers with
        // isMeasured=false, the aggregator must zero them — never leak a guess.
        val snap = StatsAggregator.aggregate(
            listOf(
                StatsAggregator.RawLinkCounters(linkId = "mobile", rxBps = 50_000_000, rttMs = 42, lossPct = 0.1, isMeasured = false),
            )
        )
        val link = snap.links.single()
        assertEquals(0L, link.rxBps)
        assertNull(link.rttMs)
        assertNull(link.lossPct)
        assertFalse(link.isMeasured)
    }

    @Test
    fun `measured counters pass through unchanged`() {
        val snap = StatsAggregator.aggregate(
            listOf(
                StatsAggregator.RawLinkCounters(
                    linkId = "wifi", rxBps = 1_000, txBps = 2_000,
                    rxBytes = 10, txBytes = 20, rttMs = 25, lossPct = 0.5,
                    active = true, isMeasured = true,
                ),
            )
        )
        val link = snap.links.single()
        assertEquals(1_000L, link.rxBps)
        assertEquals(2_000L, link.txBps)
        assertEquals(10L, link.rxBytes)
        assertEquals(20L, link.txBytes)
        assertEquals(25L, link.rttMs)
        assertEquals(0.5, link.lossPct)
        assertTrue(link.active)
        assertTrue(link.isMeasured)
        assertEquals(3_000L, snap.aggregatedBps)
        assertTrue(snap.isMeasured)
    }

    @Test
    fun `mixed measured and unmeasured links flag snapshot unmeasured`() {
        val snap = StatsAggregator.aggregate(
            listOf(
                StatsAggregator.RawLinkCounters(linkId = "wifi", rxBps = 100, txBps = 100, isMeasured = true),
                StatsAggregator.RawLinkCounters(linkId = "mobile", rxBps = 200, txBps = 200, isMeasured = false),
            )
        )
        assertFalse(snap.isMeasured)
        // aggregate is the sum over MEASURED links only
        assertEquals(200L, snap.aggregatedBps)
        assertTrue(snap.links.first { it.linkId == "wifi" }.isMeasured)
        assertFalse(snap.links.first { it.linkId == "mobile" }.isMeasured)
    }

    @Test
    fun `empty input is unmeasured`() {
        val snap = StatsAggregator.aggregate(emptyList())
        assertTrue(snap.links.isEmpty())
        assertEquals(0L, snap.aggregatedBps)
        assertFalse(snap.isMeasured)
    }

    @Test
    fun `failover flag passes through`() {
        val snap = StatsAggregator.aggregate(
            listOf(StatsAggregator.RawLinkCounters(linkId = "wifi", isMeasured = false)),
            failoverActive = true,
        )
        assertTrue(snap.failoverActive)
    }
}
