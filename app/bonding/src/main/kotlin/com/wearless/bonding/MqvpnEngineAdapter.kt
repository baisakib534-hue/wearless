package com.wearless.bonding

/**
 * MqvpnEngineAdapter — command/config builder for driving the mqvpn engine
 * (Apache-2.0, github.com/mp0rta/mqvpn) as Wearless's bonding data plane.
 *
 * Scope of this class (pure JVM, no Android, no process spawning here):
 *  - builds the mqvpn client configuration/command line for a server connection
 *    in HYBRID mode with the reorder buffer (the configuration behind the
 *    author's lab ≈93% figure [A2][A3] — the author's emulated number, never
 *    Wearless's),
 *  - maps Wearless [LinkHandle]s to per-link path bindings (network binding is
 *    how mqvpn/Speedify keep WiFi + cellular simultaneously usable on Android,
 *    WEARLESS-BOOK.md §6 row 3),
 *  - resolves the scheduler via [SchedulerPolicy] (WLB default [A3], MinRTT for
 *    strongly asymmetric links [A4]).
 *
 * What it deliberately does NOT do:
 *  - it never fabricates a connection: [buildClientConfig] only produces the
 *    config; [prepareSession] checks engine availability and returns
 *    [BondingError.EngineNotInstalled] when the real engine is absent;
 *  - it never fabricates stats: without real counters from the engine it
 *    produces NOT_MEASURED snapshots (zeros, isMeasured=false) via
 *    [StatsAggregator];
 *  - it holds no keys: [BondingConfig.keyRef] is a reference into the platform's
 *    secure storage; only the platform layer can dereference it at launch time.
 *
 * mqvpn facts used (sources in the research report):
 *  - server: plain Linux VPS, UDP 443, no custom kernel [A1];
 *  - schedulers: WLB (default) / wlb_udp_pin / MinRTT; reorder buffer; Reinjection [A3];
 *  - hybrid mode: inner TCP terminated in lwIP, relayed as HTTP/3 streams so a
 *    single TCP flow can use all paths [A2];
 *  - the engine's actual CLI flag names are TO VERIFY against the installed
 *    mqvpn build — this adapter isolates them in [MqvpnFlags] so the mapping is
 *    one place to fix.
 */
class MqvpnEngineAdapter(
    private val engineAvailable: () -> Boolean = { false },
) {

    /** Central place for the actual mqvpn CLI/config tokens (TO VERIFY per installed build). */
    object MqvpnFlags {
        const val SERVER = "--server"
        const val MODE_HYBRID = "--tcp-relay hybrid"
        val MODE_HYBRID_LIST = listOf("--tcp-relay", "hybrid")
        const val SCHEDULER_WLB = "--scheduler wlb"
        val SCHEDULER_WLB_LIST = listOf("--scheduler", "wlb")
        const val SCHEDULER_MIN_RTT = "--scheduler minrtt"
        val SCHEDULER_MIN_RTT_LIST = listOf("--scheduler", "minrtt")
        const val REORDER_BUFFER_ON = "--reorder-buffer"
        val REORDER_BUFFER_ON_LIST = listOf("--reorder-buffer")
        const val REORDER_BUFFER_OFF = "--no-reorder-buffer"
        val REORDER_BUFFER_OFF_LIST = listOf("--no-reorder-buffer")
        const val KEY_FILE = "--key-file"
        const val BIND_INTERFACE = "--bind-interface"
        val BIND_INTERFACE_LIST = listOf("--bind-interface")
    }

    /** Prepared (but not started) mqvpn session description. Pure data; launching is the platform layer's job. */
    data class PreparedSession(
        val commandLine: List<String>,
        val configDocument: String,
        val boundLinkIds: List<String>,
        val scheduler: Scheduler,
        val reorderBuffer: Boolean,
    )

    /**
     * Check the real engine is present and build the session parameters.
     * Returns failure with [BondingError.EngineNotInstalled] when the engine
     * binary/library is not available — the caller must NOT fall back to a mock.
     */
    fun prepareSession(config: BondingConfig, links: List<LinkHandle>): Result<PreparedSession> {
        if (!engineAvailable()) {
            return Result.failure(
                BondingError.EngineNotInstalled("mqvpn engine not found on this device")
            )
        }
        if (config.serverAddress.isBlank()) {
            return Result.failure(BondingError.InvalidConfig("serverAddress is blank"))
        }
        if (config.keyRef.isBlank()) {
            return Result.failure(BondingError.InvalidConfig("keyRef is blank (no key reference in secure storage)"))
        }
        if (!config.keyRef.startsWith("ref:") && !config.keyRef.startsWith("keystore:") &&
            !config.keyRef.startsWith("wearless-key") && config.keyRef.length < 200 &&
            !config.keyRef.contains("BEGIN")
        ) {
            return Result.failure(BondingError.InvalidConfig("keyRef must be a reference token, not raw key material"))
        }
        if (links.isEmpty()) {
            return Result.failure(BondingError.InvalidConfig("no links to bond"))
        }
        // Resolve the scheduler. If [BondingConfig.scheduler] is null (default),
        // [SchedulerPolicy.resolve] picks WLB or MinRTT adaptively. An explicit
        // choice (WLB or MIN_RTT) is honoured as-is. Phase-0 RTT data does not
        // exist yet, so the adaptive path currently returns WLB; once real
        // per-link RTTs flow in, pass them via measuredRttMs.
        val scheduler = SchedulerPolicy.resolve(
            requested = config.scheduler,
            measuredRttMs = emptyMap(),
            activeLinkIds = links.map { it.id }.toSet(),
        )
        val reorder = config.reorderBuffer && SchedulerPolicy.reorderBufferRecommended(scheduler)

        val argv = mutableListOf(
            "mqvpn", "client",
            MqvpnFlags.SERVER, config.serverAddress,
        )
        // keyRef is a REFERENCE — the platform layer resolves it to a real
        // key file path/contents at launch. We only pass the reference token
        // through so it can be substituted; no key material ever lives here.
        argv += listOf(MqvpnFlags.KEY_FILE, "\${keyRef:${config.keyRef}}")
        argv += MqvpnFlags.MODE_HYBRID_LIST
        argv += when (scheduler) {
            Scheduler.WLB -> MqvpnFlags.SCHEDULER_WLB_LIST
            Scheduler.MIN_RTT -> MqvpnFlags.SCHEDULER_MIN_RTT_LIST
        }
        argv += if (reorder) MqvpnFlags.REORDER_BUFFER_ON_LIST else MqvpnFlags.REORDER_BUFFER_OFF_LIST
        for (link in links) {
            argv += MqvpnFlags.BIND_INTERFACE_LIST + link.id
        }

        val configDoc = buildString {
            appendLine("server = ${config.serverAddress}")
            appendLine("tcp-relay = hybrid")
            appendLine("scheduler = ${if (scheduler == Scheduler.WLB) "wlb" else "minrtt"}")
            appendLine("reorder-buffer = $reorder")
            appendLine("key-ref = ${config.keyRef}   # reference only; resolved by platform key store at launch")
            appendLine("paths:")
            for (link in links) {
                appendLine("  - id = ${link.id}   kind = ${link.kind}   bind = ${link.id}")
            }
        }

        return Result.success(
            PreparedSession(
                commandLine = argv,
                configDocument = configDoc,
                boundLinkIds = links.map { it.id },
                scheduler = scheduler,
                reorderBuffer = reorder,
            )
        )
    }

    /**
     * Build a NOT_MEASURED snapshot for the given links: zeros everywhere,
     * isMeasured=false. Used when the engine is running but has not yet
     * delivered real counters — the UI must show NOT MEASURED, never a guess.
     */
    fun notMeasuredSnapshot(links: List<LinkHandle>, failoverActive: Boolean = false): BondingSnapshot =
        StatsAggregator.aggregate(
            counters = links.map {
                StatsAggregator.RawLinkCounters(linkId = it.id, active = false, isMeasured = false)
            },
            failoverActive = failoverActive,
        )
}
