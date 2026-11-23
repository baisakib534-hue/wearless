package com.wearless.bonding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FailoverMachineTest {

    private val links = setOf("wifi", "mobile", "nearby")

    @Test
    fun `starts RUNNING with all links`() {
        val m = FailoverMachine(links)
        assertEquals(FailoverMachine.State.IDLE, m.state)
        assertEquals(FailoverMachine.State.RUNNING, m.start())
        assertEquals(links, m.activeLinks())
        assertFalse(m.failoverActive)
    }

    @Test
    fun `link loss mid-session continues on remaining links`() {
        val m = FailoverMachine(links)
        m.start()
        // [A2] grounding: author's path-cut test — session survives, speed drops, session alive.
        assertEquals(FailoverMachine.State.FAILOVER, m.onLinkDown("wifi"))
        assertTrue(m.failoverActive)
        assertEquals(setOf("mobile", "nearby"), m.activeLinks())
        // session still alive with one more loss
        assertEquals(FailoverMachine.State.FAILOVER, m.onLinkDown("nearby"))
        assertEquals(setOf("mobile"), m.activeLinks())
        // One failover EVENT (RUNNING→FAILOVER); further losses stay within failover.
        assertEquals(1, m.failoverCount)
    }

    @Test
    fun `last link loss kills session - ALL_LINKS_LOST not restart`() {
        val m = FailoverMachine(links)
        m.start()
        m.onLinkDown("wifi")
        m.onLinkDown("mobile")
        assertEquals(FailoverMachine.State.ALL_LINKS_LOST, m.onLinkDown("nearby"))
        assertTrue(m.activeLinks().isEmpty())
    }

    @Test
    fun `recovered link returns to RUNNING`() {
        val m = FailoverMachine(links)
        m.start()
        m.onLinkDown("mobile")
        assertEquals(FailoverMachine.State.FAILOVER, m.state)
        assertEquals(FailoverMachine.State.RUNNING, m.onLinkUp("mobile"))
        assertFalse(m.failoverActive)
    }

    @Test
    fun `unknown link ids ignored`() {
        val m = FailoverMachine(links)
        m.start()
        assertEquals(FailoverMachine.State.RUNNING, m.onLinkDown("bluetooth"))
        assertEquals(FailoverMachine.State.RUNNING, m.onLinkUp("bluetooth"))
    }

    @Test
    fun `down events in IDLE stay IDLE`() {
        val m = FailoverMachine(links)
        assertEquals(FailoverMachine.State.IDLE, m.onLinkDown("wifi"))
    }

    @Test
    fun `failover count increments only on RUNNING to FAILOVER`() {
        val m = FailoverMachine(links)
        m.start()
        m.onLinkDown("wifi")
        assertEquals(1, m.failoverCount)
        m.onLinkDown("mobile") // FAILOVER -> FAILOVER, not a new failover event
        assertEquals(1, m.failoverCount)
        m.onLinkUp("wifi")
        m.onLinkUp("mobile")
        assertEquals(FailoverMachine.State.RUNNING, m.state)
        m.onLinkDown("nearby")
        assertEquals(2, m.failoverCount)
    }

    @Test
    fun `cannot start twice`() {
        val m = FailoverMachine(links)
        m.start()
        assertFailsWith<IllegalStateException> { m.start() }
    }

    @Test
    fun `stop resets to IDLE`() {
        val m = FailoverMachine(links)
        m.start()
        m.onLinkDown("wifi")
        assertEquals(FailoverMachine.State.IDLE, m.stop())
        assertTrue(m.activeLinks().isEmpty())
    }

    @Test
    fun `empty link set starts ALL_LINKS_LOST`() {
        val m = FailoverMachine(emptySet())
        assertEquals(FailoverMachine.State.ALL_LINKS_LOST, m.start())
    }
}
