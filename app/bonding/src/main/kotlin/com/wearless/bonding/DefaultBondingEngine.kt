package com.wearless.bonding

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow

/**
 * Default [BondingEngine] implementation wiring the pieces together.
 *
 * Design rule: this class launches nothing by itself. It validates, prepares
 * the engine session via an injected [MqvpnEngineAdapter] (or any adapter),
 * runs the [FailoverMachine] on observed link events, and publishes snapshots
 * from [StatsAggregator]. The Android layer (jcode's shell) owns process
 * lifecycle and the real counters, and calls [onLinkUp]/[onLinkDown]/
 * [pushCounters] with observed facts only.
 *
 * It NEVER fabricates: start() without a real engine fails with
 * ENGINE_NOT_INSTALLED; stats before real counters are NOT_MEASURED zeros.
 */
class DefaultBondingEngine(
    private val adapter: MqvpnEngineAdapter = MqvpnEngineAdapter(),
) : BondingEngine {

    private val snapshot = MutableStateFlow(
        BondingSnapshot(links = emptyList(), aggregatedBps = 0, isMeasured = false, failoverActive = false)
    )

    private var failover: FailoverMachine? = null
    private var running = false
    private var prepared: MqvpnEngineAdapter.PreparedSession? = null

    override fun start(config: BondingConfig, links: List<LinkHandle>): Result<Unit> {
        if (running) return Result.failure(BondingError.AlreadyRunning())
        val session = adapter.prepareSession(config, links)
            .onFailure { return Result.failure(it) }
            .getOrThrow()
        failover = FailoverMachine(links.map { it.id }.toSet()).also { it.start() }
        prepared = session
        running = true
        // Honest initial state: nothing measured yet.
        snapshot.value = adapter.notMeasuredSnapshot(links)
        return Result.success(Unit)
    }

    override fun stop() {
        running = false
        prepared = null
        failover?.stop()
        failover = null
        snapshot.value = BondingSnapshot(links = emptyList(), aggregatedBps = 0, isMeasured = false, failoverActive = false)
    }

    override fun stats(): Flow<BondingSnapshot> = snapshot.asStateFlow()

    /** Current snapshot (convenience for non-flow consumers; same data as [stats]). */
    val currentSnapshot: BondingSnapshot get() = snapshot.value

    /** Expose the prepared session (command line/config) to the platform layer that will launch the engine. Null when not running. */
    fun preparedSession(): MqvpnEngineAdapter.PreparedSession? = prepared

    /** Feed an OBSERVED link-up event. Returns new failover state. */
    fun onLinkUp(linkId: String): FailoverMachine.State? = failover?.onLinkUp(linkId)

    /** Feed an OBSERVED link-down event. Session continues on remaining links; never restarts. */
    fun onLinkDown(linkId: String): FailoverMachine.State? = failover?.onLinkDown(linkId)

    /** Feed real observed counters; produces the next snapshot. Unmeasured links become zeros + isMeasured=false. */
    fun pushCounters(counters: List<StatsAggregator.RawLinkCounters>) {
        snapshot.value = StatsAggregator.aggregate(
            counters = counters,
            failoverActive = failover?.failoverActive == true,
        )
    }
}
