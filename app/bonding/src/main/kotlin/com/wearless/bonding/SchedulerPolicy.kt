package com.wearless.bonding

/**
 * Scheduler policy — decides which packet scheduler the engine should run.
 *
 * Grounded in the mqvpn author's own documented behaviour, NOT invented:
 *
 * - WLB (Weighted Load Balancing) is mqvpn's default scheduler [A3].
 * - The author's published failure/recovery cases [A4]:
 *   - WLB collapsed to 0.9 Mbps under 20 ms jitter; with the reorder buffer
 *     enabled the same setup did 37.6 Mbps → keep the reorder buffer ON with WLB.
 *   - On strongly asymmetric links WLB delivered ~77 Mbps where MinRTT
 *     delivered ~212 Mbps → switch to MinRTT when links are markedly unequal.
 *
 * Consequence (this is a policy CHOICE documented here, not a measured claim):
 * default WLB + reorder buffer; recommend MinRTT when measured RTTs of the
 * active links differ strongly. The exact threshold below is a tuning knob of
 * this policy, not an engine property; Phase-0 real-device numbers
 * (WEARLESS-BOOK.md §9, still open) will validate or adjust it.
 */
object SchedulerPolicy {

    /** RTT ratio at/above which links are considered "strongly unequal" [A4 asymmetric case]. */
    const val ASYMMETRY_RTT_RATIO_THRESHOLD = 4.0

    /**
     * Resolve the effective scheduler.
     *
     * @param requested user/system selected scheduler (null = let policy decide, default WLB [A3]).
     * @param measuredRttMs measured RTT per link id — only real observations; links without a
     *   measurement are ignored for the asymmetry heuristic (never guessed).
     * @param activeLinkIds links currently active in the session.
     */
    fun resolve(
        requested: Scheduler?,
        measuredRttMs: Map<String, Long>,
        activeLinkIds: Set<String>,
    ): Scheduler {
        val effectiveActive = activeLinkIds.filter { measuredRttMs.containsKey(it) }
        if (requested != null) return requested
        if (effectiveActive.size < 2) return Scheduler.WLB
        val rtts = effectiveActive.map { measuredRttMs.getValue(it) }
        val min = rtts.min()
        val max = rtts.max()
        if (min <= 0) return Scheduler.WLB
        val stronglyAsymmetric = (max.toDouble() / min.toDouble()) >= ASYMMETRY_RTT_RATIO_THRESHOLD
        return if (stronglyAsymmetric) Scheduler.MIN_RTT else Scheduler.WLB
    }

    /**
     * Whether the reorder buffer should be enabled for the given scheduler.
     * WLB under jitter recovered only with the reorder buffer [A4]; it is safe with
     * MinRTT too (mqvpn ships it as a general option [A3]) so we keep it on unless
     * the caller explicitly disabled it in [BondingConfig].
     *
     * Both schedulers currently benefit; the parameter is kept so a future
     * scheduler (or a MinRTT low-latency mode) can opt out without an API change.
     */
    fun reorderBufferRecommended(scheduler: Scheduler): Boolean = when (scheduler) {
        Scheduler.WLB -> true
        Scheduler.MIN_RTT -> true
    }
}
