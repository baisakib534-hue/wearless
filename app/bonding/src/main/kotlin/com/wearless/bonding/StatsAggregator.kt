package com.wearless.bonding

/**
 * StatsAggregator — turns raw observed link counters into [BondingSnapshot]s.
 *
 * Honest-data contract (spec T-04, non-negotiable): any counter that was not
 * ACTUALLY observed produces zeros and [LinkStats.isMeasured] == false.
 * There is no interpolation, no estimation, no "plausible" default — if the
 * platform layer did not report a number, the snapshot says so.
 *
 * Input is [RawLinkCounters] supplied by the platform layer; the aggregator
 * trusts the caller only as far as the [RawLinkCounters.isMeasured] flag.
 */
object StatsAggregator {

    /**
     * Raw counters for one link, as observed by the platform layer.
     *
     * @param isMeasured the platform layer sets this true ONLY when every rate/byte
     *   counter came from a real observation this interval. If any value is missing
     *   or synthesised, pass false and zeros — never a guess.
     */
    data class RawLinkCounters(
        val linkId: String,
        val rxBps: Long = 0,
        val txBps: Long = 0,
        val rxBytes: Long = 0,
        val txBytes: Long = 0,
        val rttMs: Long? = null,
        val lossPct: Double? = null,
        val active: Boolean = false,
        val isMeasured: Boolean = false,
    )

    fun aggregate(
        counters: List<RawLinkCounters>,
        failoverActive: Boolean = false,
    ): BondingSnapshot {
        val links = counters.map { raw ->
            if (raw.isMeasured) {
                LinkStats(
                    linkId = raw.linkId,
                    rxBps = raw.rxBps,
                    txBps = raw.txBps,
                    rxBytes = raw.rxBytes,
                    txBytes = raw.txBytes,
                    rttMs = raw.rttMs,
                    lossPct = raw.lossPct,
                    active = raw.active,
                    isMeasured = true,
                )
            } else {
                // Unmeasured: zeros/nulls only. Never plausible-looking numbers.
                LinkStats(
                    linkId = raw.linkId,
                    rxBps = 0,
                    txBps = 0,
                    rxBytes = 0,
                    txBytes = 0,
                    rttMs = null,
                    lossPct = null,
                    active = raw.active,
                    isMeasured = false,
                )
            }
        }
        val anyMeasured = links.any { it.isMeasured }
        // Split rx/tx aggregation; never double-count. Zero when nothing is measured.
        val aggregatedRxBps = if (anyMeasured) links.filter { it.isMeasured }.sumOf { it.rxBps } else 0L
        val aggregatedTxBps = if (anyMeasured) links.filter { it.isMeasured }.sumOf { it.txBps } else 0L
        return BondingSnapshot(
            links = links,
            aggregatedRxBps = aggregatedRxBps,
            aggregatedTxBps = aggregatedTxBps,
            isMeasured = links.isNotEmpty() && links.all { it.isMeasured },
            failoverActive = failoverActive,
        )
    }
}
