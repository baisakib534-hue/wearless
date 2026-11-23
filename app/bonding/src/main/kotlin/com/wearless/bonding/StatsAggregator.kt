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
        // Aggregated rate only exists if at least one link is measured; and it is
        // only the SUM of measured links — a partial sum over measured links is
        // still the honest number for "what we can see", flagged by snapshot-level
        // isMeasured=false when any link is unmeasured so the UI can label it.
        val aggregatedBps = if (anyMeasured) links.filter { it.isMeasured }.sumOf { it.rxBps + it.txBps } else 0
        return BondingSnapshot(
            links = links,
            aggregatedBps = aggregatedBps,
            isMeasured = links.isNotEmpty() && links.all { it.isMeasured },
            failoverActive = failoverActive,
        )
    }
}
