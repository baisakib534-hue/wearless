package com.wearless.bonding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MqvpnEngineAdapterTest {

    private data class FakeLink(override val id: String, override val kind: LinkKind) : LinkHandle
    private val links = listOf(
        FakeLink("wifi0", LinkKind.WIFI),
        FakeLink("cell0", LinkKind.MOBILE),
    )
    private val config = BondingConfig(serverAddress = "vps.example:443", keyRef = "wearless-key-1")

    @Test
    fun `engine not installed fails with ENGINE_NOT_INSTALLED - never a fake session`() {
        val adapter = MqvpnEngineAdapter(engineAvailable = { false })
        val result = adapter.prepareSession(config, links)
        assertFalse(result.isSuccess)
        val err = result.exceptionOrNull()
        assertIs<BondingError.EngineNotInstalled>(err)
        assertTrue(err.message!!.contains("ENGINE_NOT_INSTALLED"))
    }

    @Test
    fun `blank server address rejected`() {
        val adapter = MqvpnEngineAdapter(engineAvailable = { true })
        val result = adapter.prepareSession(config.copy(serverAddress = " "), links)
        assertIs<BondingError.InvalidConfig>(result.exceptionOrNull())
    }

    @Test
    fun `blank keyRef rejected`() {
        val adapter = MqvpnEngineAdapter(engineAvailable = { true })
        val result = adapter.prepareSession(config.copy(keyRef = ""), links)
        assertIs<BondingError.InvalidConfig>(result.exceptionOrNull())
    }

    @Test
    fun `no links rejected`() {
        val adapter = MqvpnEngineAdapter(engineAvailable = { true })
        val result = adapter.prepareSession(config, emptyList())
        assertIs<BondingError.InvalidConfig>(result.exceptionOrNull())
    }

    @Test
    fun `hybrid mode and reorder buffer default in command line`() {
        // [A2][A3]: hybrid + reorder buffer is the configuration behind the
        // author's ≈93% emulated lab figure (his number, never Wearless's).
        val adapter = MqvpnEngineAdapter(engineAvailable = { true })
        val session = adapter.prepareSession(config, links).getOrThrow()
        val cmdline = session.commandLine.joinToString(" ")
        assertTrue(cmdline.contains("--tcp-relay hybrid"), cmdline)
        assertTrue(cmdline.contains("--reorder-buffer"), cmdline)
        assertTrue(cmdline.contains("--server vps.example:443"), cmdline)
    }

    @Test
    fun `scheduler selection maps to mqvpn scheduler names`() {
        val adapter = MqvpnEngineAdapter(engineAvailable = { true })
        val wlb = adapter.prepareSession(config, links).getOrThrow()
        assertTrue(wlb.commandLine.joinToString(" ").contains("--scheduler wlb"))
        val minrtt = adapter.prepareSession(config.copy(scheduler = Scheduler.MIN_RTT), links).getOrThrow()
        assertTrue(minrtt.commandLine.joinToString(" ").contains("--scheduler minrtt"))
        assertEquals(Scheduler.MIN_RTT, minrtt.scheduler)
    }

    @Test
    fun `per-link binding present for every link`() {
        val adapter = MqvpnEngineAdapter(engineAvailable = { true })
        val session = adapter.prepareSession(config, links).getOrThrow()
        val cmdline = session.commandLine
        assertTrue(cmdline.containsAll(listOf("--bind-interface", "wifi0")))
        assertTrue(cmdline.containsAll(listOf("--bind-interface", "cell0")))
        assertEquals(listOf("wifi0", "cell0"), session.boundLinkIds)
    }

    @Test
    fun `keyRef stays a reference - no key material in output beyond the ref token`() {
        val adapter = MqvpnEngineAdapter(engineAvailable = { true })
        val session = adapter.prepareSession(config, links).getOrThrow()
        val cmdline = session.commandLine.joinToString(" ")
        // The reference token is passed as a placeholder for the platform to substitute;
        // the adapter itself can never resolve it to a real key.
        assertTrue(cmdline.contains("\${keyRef:wearless-key-1}"), cmdline)
        assertTrue(session.configDocument.contains("key-ref = wearless-key-1"))
        assertFalse(session.configDocument.contains("BEGIN"), "no key material expected")
    }

    @Test
    fun `reorderBuffer false honoured`() {
        val adapter = MqvpnEngineAdapter(engineAvailable = { true })
        val session = adapter.prepareSession(config.copy(reorderBuffer = false), links).getOrThrow()
        val cmdline = session.commandLine.joinToString(" ")
        assertFalse(cmdline.contains("--reorder-buffer ") || cmdline.endsWith("--reorder-buffer"))
        assertTrue(cmdline.contains("--no-reorder-buffer"))
        assertFalse(session.reorderBuffer)
    }

    @Test
    fun `notMeasuredSnapshot is all zeros and unmeasured`() {
        val adapter = MqvpnEngineAdapter(engineAvailable = { true })
        val snap = adapter.notMeasuredSnapshot(links, failoverActive = true)
        assertTrue(snap.failoverActive)
        assertFalse(snap.isMeasured)
        assertEquals(0L, snap.aggregatedRxBps)
        assertEquals(0L, snap.aggregatedTxBps)
        snap.links.forEach { link ->
            assertEquals(0L, link.rxBps)
            assertEquals(0L, link.rxBytes)
            assertNull(link.rttMs)
            assertNull(link.lossPct)
            assertFalse(link.isMeasured)
        }
    }
}

class DefaultBondingEngineTest {

    private data class FakeLink(override val id: String, override val kind: LinkKind) : LinkHandle
    private val links = listOf(FakeLink("wifi0", LinkKind.WIFI), FakeLink("cell0", LinkKind.MOBILE))

    @Test
    fun `start without engine fails - no fake session`() {
        val engine = DefaultBondingEngine(MqvpnEngineAdapter(engineAvailable = { false }))
        val result = engine.start(BondingConfig("vps:443", "ref"), links)
        assertFalse(result.isSuccess)
        assertIs<BondingError.EngineNotInstalled>(result.exceptionOrNull())
        assertNull(engine.preparedSession())
    }

    @Test
    fun `stats before counters are NOT_MEASURED zeros`() {
        val engine = DefaultBondingEngine(MqvpnEngineAdapter(engineAvailable = { true }))
        engine.start(BondingConfig("vps:443", "ref"), links)
        val snap = engine.currentSnapshot
        assertEquals(2, snap.links.size)
        assertFalse(snap.isMeasured)
        assertEquals(0L, snap.aggregatedRxBps)
        assertEquals(0L, snap.aggregatedTxBps)
    }

    @Test
    fun `link down mid-session continues - session alive, failover flag set`() {
        val engine = DefaultBondingEngine(MqvpnEngineAdapter(engineAvailable = { true }))
        engine.start(BondingConfig("vps:443", "ref"), links)
        val state = engine.onLinkDown("wifi0")
        assertEquals(FailoverMachine.State.FAILOVER, state)
        // engine still running; no restart happened
        assertTrue(engine.preparedSession() != null)
        engine.pushCounters(
            listOf(StatsAggregator.RawLinkCounters(linkId = "cell0", rxBps = 100, txBps = 50, isMeasured = true))
        )
        val snap = engine.currentSnapshot
        assertTrue(snap.failoverActive)
        assertTrue(snap.isMeasured)
        // rx=100, tx=50 for the one measured link
        assertEquals(100L, snap.aggregatedRxBps)
        assertEquals(50L, snap.aggregatedTxBps)
    }

    @Test
    fun `second start fails with ALREADY_RUNNING`() {
        val engine = DefaultBondingEngine(MqvpnEngineAdapter(engineAvailable = { true }))
        engine.start(BondingConfig("vps:443", "ref"), links)
        val second = engine.start(BondingConfig("vps:443", "ref"), links)
        assertIs<BondingError.AlreadyRunning>(second.exceptionOrNull())
    }

    @Test
    fun `stop resets everything`() {
        val engine = DefaultBondingEngine(MqvpnEngineAdapter(engineAvailable = { true }))
        engine.start(BondingConfig("vps:443", "ref"), links)
        engine.stop()
        assertNull(engine.preparedSession())
        assertEquals(0, engine.currentSnapshot.links.size)
        // can start again after stop
        assertTrue(engine.start(BondingConfig("vps:443", "ref"), links).isSuccess)
    }

    @Test
    fun `stats exposes a kotlinx Flow`() {
        val engine = DefaultBondingEngine(MqvpnEngineAdapter(engineAvailable = { true }))
        val flow: kotlinx.coroutines.flow.StateFlow<BondingSnapshot> =
            engine.stats() as kotlinx.coroutines.flow.StateFlow<BondingSnapshot>
        assertEquals(engine.currentSnapshot, flow.value)
    }
}
