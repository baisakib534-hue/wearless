Task T-03 — Android app shell (client mode) for Wearless.

You are jcode, core implementation seat. Read WEARLESS-BOOK.md sections 2, 3, 6, 7, 10 and BUILD-ROADMAP.md Phase 1. Product: app + node only, NO website, no accounts, no Docker, no demo data, no fake speeds.

Exclusive paths you own: app/** EXCEPT app/bonding/** (claude owns that module). Do not edit app/bonding/**, node/**, qa/**, scripts/**, root docs, git history. app/settings.gradle.kts IS yours and MUST include both `:app` and `:bonding` modules.

Build a Kotlin Android shell:
- Gradle wrapper (`gradlew.bat`) + settings.gradle.kts + app module; target SDK 34, min SDK 26, JDK 17 at %JAVA_HOME% (Temurin), Android SDK at C:\Users\sakim\AppData\Local\Android\Sdk with platform android-34 and build-tools 34.0.0.
- VpnService foreground service with persistent notification, consent flow, battery-optimization exemption request, watchdog/restart on tunnel loss; declare foregroundServiceType correctly (Android 14+). Never claim the tunnel works if the engine is not wired.
- Home UI: one Start/Stop, status (Connected / Bonding N links / Sharing soon — do not fake host mode), big combined-speed readout, per-link cards (name, live speed, data used, on/off). When a value is not measured display the literal string "NOT MEASURED"; never invent a number.
- Node config: paste a node key once, store in Android Keystore-backed encrypted prefs, never in source/log. QR scan if a dependency is available offline; otherwise leave a clean hook and report the exact blocker — paste must work.
- Resource Monitor screen (owner's V1 order): show app RAM (ActivityManager/Debug), CPU if Android exposes it, battery level/charging, device temperature if exposed (API 34 ThermalManager), per-link data; show "unavailable" where the OS does not expose a value.
- Consume the bonding module through this EXACT contract owned by claude (do not create/duplicate it): package com.wearless.bonding — interface BondingEngine { fun start(config: BondingConfig, links: List<LinkHandle>): Result<Unit>; fun stop(); fun stats(): kotlinx.coroutines.flow.Flow<BondingSnapshot> }, data class BondingConfig(val serverAddress: String, val keyRef: String, val scheduler: Scheduler = Scheduler.WLB, val reorderBuffer: Boolean = true), enum class Scheduler { WLB, MIN_RTT }, interface LinkHandle { val id: String; val kind: LinkKind }, enum class LinkKind { WIFI, MOBILE, NEARBY, NODE }, data class LinkStats(val linkId: String, val rxBps: Long, val txBps: Long, val rxBytes: Long, val txBytes: Long, val rttMs: Long?, val lossPct: Double?, val active: Boolean, val isMeasured: Boolean), data class BondingSnapshot(val links: List<LinkStats>, val aggregatedBps: Long, val isMeasured: Boolean, val failoverActive: Boolean). Implement AndroidLinkHandle in your app module using ConnectivityManager/Network.bindSocket per [B2]. If that contract cannot compile against your code, report the exact compiler error and coordinate via goose — do not redefine the types.

Tooling: JDK 17, Android SDK above, Gradle wrapper, local build only. Attempt `gradlew.bat :app:assembleDebug` (and `:bonding:compileDebugKotlin` if available). If dependency download is blocked, capture the EXACT error; a written file is never "done".

Hard rules: no mock success screens, no placeholder speeds, no credentials in code/chat, no website, no host mode in this task.

Finish with a worker_done report containing your evidence (diff summary, command output, test/measurement output, file paths). If you hit an error you cannot fix, escalate through orchestration with the EXACT error before stopping. Stopping silently is forbidden.
