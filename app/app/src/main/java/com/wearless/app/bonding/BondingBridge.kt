package com.wearless.app.bonding

import com.wearless.app.model.AppLink
import com.wearless.bonding.BondingConfig
import com.wearless.bonding.BondingEngine
import com.wearless.bonding.BondingSnapshot
import com.wearless.bonding.DefaultBondingEngine
import com.wearless.bonding.LinkHandle
import com.wearless.bonding.MqvpnEngineAdapter
import com.wearless.bonding.LinkKind
import com.wearless.bonding.Scheduler
import com.wearless.app.link.AndroidLinkHandle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Bridge to claude-owned module com.wearless.bonding (now packaged in the APK
 * via implementation(project(":bonding"))).
 *
 * The contract types are consumed directly here — never duplicated. The app
 * side supplies AndroidLinkHandle implementations of the bonding LinkHandle
 * interface (ConnectivityManager/Network.bindSocket per [B2]) and forwards
 * start/stop/stats to a real DefaultBondingEngine.
 *
 * Honest-data rule kept: the engine itself refuses to start without a real
 * data plane (ENGINE_NOT_INSTALLED) and every unmeasured value stays
 * isMeasured=false / NOT MEASURED in the UI. This bridge never fakes success.
 */
object BondingBridge {

    data class BridgeSnapshot(
        val links: List<AppLink>,
        val aggregatedRxBps: Long?,
        val aggregatedTxBps: Long?,
        val isMeasured: Boolean,
        val failoverActive: Boolean
    ) {
        companion object {
            val NOT_MEASURED = BridgeSnapshot(emptyList(), null, null, false, false)
        }
    }

    sealed class State {
        object EngineNotWired : State()
        object Running : State()
        object Stopped : State()
        data class Failed(val message: String) : State()
    }

    val state = MutableStateFlow<State>(State.Stopped)
    private var engine: BondingEngine? = null

    /** True when the bonding classes are packaged and an engine instance exists. */
    fun isEngineAvailable(): Boolean = engine != null ||
        try {
            Class.forName("com.wearless.bonding.DefaultBondingEngine")
            true
        } catch (_: ClassNotFoundException) {
            false
        }

    /**
     * Start the bonding engine. The engine (DefaultBondingEngine) itself
     * returns Result.failure(BondingError.EngineNotInstalled) when no real
     * data plane is present — that failure propagates honestly.
     *
     * scheduler=null means "auto": SchedulerPolicy.resolve decides WLB vs
     * MIN_RTT from real RTT observations, never a guess.
     */
    fun start(
        serverAddress: String,
        keyRef: String,
        scheduler: Scheduler?,
        reorderBuffer: Boolean,
        links: List<AndroidLinkHandle>
    ): Result<Unit> {
        val e = engine ?: DefaultBondingEngine().also { engine = it }
        val bondingLinks: List<LinkHandle> = links.map { h ->
            BondingLinkHandle(h.id, LinkKind.values()[h.kindValue])
        }
        val config = BondingConfig(
            serverAddress = serverAddress,
            keyRef = keyRef,
            scheduler = scheduler,
            reorderBuffer = reorderBuffer
        )
        val result = e.start(config, bondingLinks)
        state.value = if (result.isSuccess) State.Running
        else State.Failed(result.exceptionOrNull()?.message ?: "engine start failed")
        return result
    }

    fun stop() {
        engine?.stop()
        state.value = State.Stopped
    }

    /**
     * Convert the engine's latest snapshot to app-side values. Without a
     * running engine this returns NOT_MEASURED — no invented numbers.
     */
    fun snapshot(uiLinks: List<AppLink>): BridgeSnapshot {
        val e = engine as? DefaultBondingEngine ?: return BridgeSnapshot.NOT_MEASURED
        val snap: BondingSnapshot = try {
            e.currentSnapshot
        } catch (_: Throwable) {
            return BridgeSnapshot.NOT_MEASURED
        }
        val byId = uiLinks.associateBy { it.id }
        val mapped = snap.links.map { ls ->
            val ui = byId[ls.linkId] ?: AppLink(ls.linkId, ls.linkId, 0)
            // Numbers only when actually measured; nulls mean NOT MEASURED in UI.
            if (ls.isMeasured) ui.copy(
                rxBps = ls.rxBps,
                txBps = ls.txBps,
                rxBytes = ls.rxBytes,
                txBytes = ls.txBytes,
                rttMs = ls.rttMs,
                lossPct = ls.lossPct,
                active = ls.active,
                isMeasured = true
            ) else ui.copy(
                rxBps = null,
                txBps = null,
                rxBytes = null,
                txBytes = null,
                rttMs = null,
                lossPct = null,
                active = ls.active,
                isMeasured = false
            )
        }
        return BridgeSnapshot(
            links = mapped,
            aggregatedRxBps = if (snap.isMeasured) snap.aggregatedRxBps else null,
            aggregatedTxBps = if (snap.isMeasured) snap.aggregatedTxBps else null,
            isMeasured = snap.isMeasured,
            failoverActive = snap.failoverActive
        )
    }

    fun stateFlow(): StateFlow<State> = state

    /** Compile-time implementation of the bonding LinkHandle for one Android link. */
    private data class BondingLinkHandle(
        override val id: String,
        override val kind: LinkKind
    ) : LinkHandle
}
