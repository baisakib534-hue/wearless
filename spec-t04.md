Task T-04 — Bonding core module (pure Kotlin) for Wearless.

You are claude, tricky-logic seat: bonding/scheduler integration, tun2socks plumbing design, failover, hard bugs. Read WEARLESS-BOOK.md §5, §6 rows 3/4/5, §9, §10 and the HTML report sections #bonding options, #phase0 plus sources A1–A5, A13, B2, B3, D2, D3, D11. No fake speeds; lab 93% is the mqvpn author's emulated number [A2], never Wearless's.

Exclusive paths you own: app/bonding/** ONLY. Do not touch app/src/**, app/settings.gradle.kts, app/build.gradle.kts (jcode owns them), node/**, qa/**, scripts/**, root docs, git.

Create a pure-Kotlin JVM module `app/bonding/` with build.gradle.kts (no Android plugin, no heavy external deps) exposing this EXACT contract in package com.wearless.bonding (jcode codes against it):
- interface BondingEngine { fun start(config: BondingConfig, links: List<LinkHandle>): Result<Unit>; fun stop(); fun stats(): kotlinx.coroutines.flow.Flow<BondingSnapshot> }
- data class BondingConfig(val serverAddress: String, val keyRef: String, val scheduler: Scheduler = Scheduler.WLB, val reorderBuffer: Boolean = true)
- enum class Scheduler { WLB, MIN_RTT }
- interface LinkHandle { val id: String; val kind: LinkKind }
- enum class LinkKind { WIFI, MOBILE, NEARBY, NODE }
- data class LinkStats(val linkId: String, val rxBps: Long, val txBps: Long, val rxBytes: Long, val txBytes: Long, val rttMs: Long?, val lossPct: Double?, val active: Boolean, val isMeasured: Boolean)
- data class BondingSnapshot(val links: List<LinkStats>, val aggregatedBps: Long, val isMeasured: Boolean, val failoverActive: Boolean)

If kotlinx.coroutines cannot be fetched offline, use a small local Flow-like interface and report the exact dependency error to goose — do not silently change the public names.

Implement:
- MqvpnEngineAdapter: command/config builder for mqvpn server connection (hybrid mode, reorder buffer, per-link binding input), with explicit ENGINE_NOT_INSTALLED / NOT_MEASURED states. Never fabricate a connection or speed. No keys in code; keyRef is a reference only.
- SchedulerPolicy: WLB default, MinRTT selectable; adaptive decision documented from [A3]/[A4], not invented.
- FailoverMachine: link loss mid-session continues on remaining links; state transitions unit-tested; never restart a fake session.
- StatsAggregator: consumes real counters; any counter not actually observed must produce isMeasured=false and zeros, never plausible-looking numbers.
- Unit tests (JUnit if available offline) for scheduler selection, failover transitions and unmeasured-stats behaviour.

Tooling: JDK 17 (%JAVA_HOME%\bin), Gradle local only; attempt `:bonding:test` once the wrapper exists (jcode creates app/gradlew.bat). If blocked by jcode's files not existing yet, write the module and report the exact blocker; goose integrates.

Hard rules: no website, no Docker, no credentials, no mqtt/cloud control plane, no Play/account work, no invented benchmarks.

Finish with a worker_done report containing your evidence (diff summary, command output, test/measurement output, file paths). If you hit an error you cannot fix, escalate through orchestration with the EXACT error before stopping. Stopping silently is forbidden.
