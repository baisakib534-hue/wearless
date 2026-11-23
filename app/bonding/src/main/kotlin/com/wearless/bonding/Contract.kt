package com.wearless.bonding

import kotlinx.coroutines.flow.Flow

/**
 * Public bonding contract for Wearless.
 *
 * EXACT contract fixed by spec T-04; jcode's Android shell codes against this.
 * Do not rename any public symbol here without a spec change.
 *
 * Honest-data rule (WEARLESS-BOOK.md §8, non-negotiable 1):
 * nothing in this package ever fabricates a connection, a speed, or a counter.
 * Everything that was not actually observed reports [BondingSnapshot.isMeasured] == false
 * and zero values — never plausible-looking numbers.
 */
interface BondingEngine {
    /**
     * Start bonding across [links] using [config].
     *
     * Fails (Result.failure) with [BondingError.EngineNotInstalled] when no real
     * bonding engine is available — it never starts a fake session.
     */
    fun start(config: BondingConfig, links: List<LinkHandle>): Result<Unit>

    /** Stop the session and release per-link bindings. Safe to call when not running. */
    fun stop()

    /** Live stats. Emits [BondingSnapshot] updates; unobserved counters are zeros with isMeasured=false. */
    fun stats(): Flow<BondingSnapshot>
}

/** Configuration for a bonding session. [keyRef] is a reference to a key in secure storage — never the key itself. */
data class BondingConfig(
    val serverAddress: String,
    val keyRef: String,
    val scheduler: Scheduler = Scheduler.WLB,
    val reorderBuffer: Boolean = true,
)

/** Packet scheduler for the multipath engine. Mirrors mqvpn's scheduler names [A3]. */
enum class Scheduler { WLB, MIN_RTT }

/** One usable network link handed to the engine by the platform layer. */
interface LinkHandle {
    val id: String
    val kind: LinkKind
}

enum class LinkKind { WIFI, MOBILE, NEARBY, NODE }

/**
 * Observed counters for one link. All numbers must come from real measurements;
 * a [LinkStats] with [isMeasured] == false must carry zeros/nulls only.
 */
data class LinkStats(
    val linkId: String,
    val rxBps: Long,
    val txBps: Long,
    val rxBytes: Long,
    val txBytes: Long,
    val rttMs: Long?,
    val lossPct: Double?,
    val active: Boolean,
    val isMeasured: Boolean,
)

/** Aggregated view across all links at one point in time. */
data class BondingSnapshot(
    val links: List<LinkStats>,
    val aggregatedBps: Long,
    val isMeasured: Boolean,
    val failoverActive: Boolean,
)

/** Typed failures for [BondingEngine.start]. */
sealed class BondingError(message: String) : Exception(message) {
    /** No real bonding engine (e.g. mqvpn binary/library) is present on this device. */
    class EngineNotInstalled(detail: String) : BondingError("ENGINE_NOT_INSTALLED: $detail")

    /** The session is already running. */
    class AlreadyRunning : BondingError("ALREADY_RUNNING")

    /** Invalid configuration (blank address, no links, ...). */
    class InvalidConfig(detail: String) : BondingError("INVALID_CONFIG: $detail")
}
