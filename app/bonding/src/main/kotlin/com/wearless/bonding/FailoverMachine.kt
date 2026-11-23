package com.wearless.bonding

/**
 * Failover state machine.
 *
 * Requirement (spec T-04 / WEARLESS-BOOK.md §5.4 failover mode): if a link dies
 * mid-session, the session CONTINUES on the remaining links — it must never be
 * restarted as a new (fake) session.
 *
 * This is a pure logic component: the platform layer feeds it observed link
 * up/down events; it decides the session-level outcome. It owns no network.
 */
class FailoverMachine(private val linkIds: Set<String>) {

    enum class State {
        /** Session not started. */
        IDLE,

        /** All configured links up (or at least never observed down). */
        RUNNING,

        /** One or more links lost; session continues on the remainder. */
        FAILOVER,

        /**
         * Every link lost. The session is dead — [BondingEngine] must surface this
         * honestly; it must NOT silently restart and report a "new" session as if
         * it were the same one.
         */
        ALL_LINKS_LOST,
    }

    var state: State = State.IDLE
        private set

    /** Links currently considered up (never guessed — set only from observed events). */
    private val upLinks = linkedSetOf<String>()

    /** Monotonic count of failover transitions (for stats/UI). */
    var failoverCount: Int = 0
        private set

    fun start(): State {
        check(state == State.IDLE) { "FailoverMachine can only start from IDLE (was $state)" }
        upLinks.clear()
        upLinks.addAll(linkIds)
        state = if (linkIds.isEmpty()) State.ALL_LINKS_LOST else State.RUNNING
        return state
    }

    /**
     * Report an observed link state change. Returns the resulting [State].
     * Passing a link id not in the configured set is ignored (defensive).
     */
    fun onLinkUp(linkId: String): State {
        if (linkId !in linkIds) return state
        upLinks.add(linkId)
        state = when {
            state == State.IDLE -> State.IDLE
            upLinks.size == linkIds.size -> State.RUNNING
            upLinks.isNotEmpty() -> State.FAILOVER
            else -> State.ALL_LINKS_LOST
        }
        return state
    }

    fun onLinkDown(linkId: String): State {
        if (linkId !in linkIds) return state
        val wasRunning = state == State.RUNNING
        upLinks.remove(linkId)
        state = when {
            state == State.IDLE -> State.IDLE
            upLinks.isEmpty() -> State.ALL_LINKS_LOST
            state == State.RUNNING || state == State.FAILOVER -> State.FAILOVER
            else -> state
        }
        if (wasRunning && state == State.FAILOVER) failoverCount++
        return state
    }

    fun stop(): State {
        state = State.IDLE
        upLinks.clear()
        return state
    }

    /** True while the session continues on fewer than all configured links. */
    val failoverActive: Boolean get() = state == State.FAILOVER

    /** Snapshot of currently-up link ids (read-only). */
    fun activeLinks(): Set<String> = upLinks.toSet()
}
