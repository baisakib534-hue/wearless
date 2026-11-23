# CLAUDE-BUGHUNT.md — Adversarial bug hunt (Wearless repo)

Scope: `app/bonding/**`, `app/app/**`, `node/**`, `scripts/**`, `qa/**`, root docs.
No source files edited. Findings only.

## Top 10 priority (fix first)

1. **CRITICAL** `node/control.sh:2365` — `check_response()` exits 0-success responses under `set -eu`; every control invocation fails.
2. **CRITICAL** `app/app/.../vpn/TunnelService.kt:931` — `Builder().establish()` result ignored; tunnel can report up with no TUN.
3. **HIGH** `app/bonding/.../SchedulerPolicy.kt:1674` + `Contract.kt:1269` — adaptive MinRTT path is dead because `BondingConfig.scheduler` is non-null; explicit WLB always wins.
4. **HIGH** `app/app/.../ResourceMonitorActivity.kt:454` — naive `/proc/pid/stat` split breaks on process names with spaces/parens → wrong CPU% or crash.
5. **HIGH** `node/install.sh:2549` — mqvpn URL default is a placeholder; installer cannot run on a clean machine without manual override (documented, but still a hard blocker).
6. **HIGH** `app/app/.../vpn/TunnelService.kt:947` — watchdog runs once then exits; no sustained restart loop / no recovery after second failure.
7. **HIGH** `app/app/.../vpn/TunnelService.kt:962` — battery-exemption intent launched from a background service (Android 10+ background-start restriction) → may silently fail.
8. **MEDIUM** `settings.gradle.kts` (root) — stale module paths (`:bonding` at root) conflict with real layout (`app/bonding`); root `./gradlew` breaks on clean machine.
9. **MEDIUM** `app/app/.../MainActivity.kt:258` — `formatBps` divides by 1000 for "kbps" but by 1e6 for Mbps; inconsistent units (1024 vs 1000) and aggregatedBps mixes rx+tx.
10. **MEDIUM** `app/app/src/main/AndroidManifest.xml:53` — `BATTERY_STATS` is signature-level; declared but never granted to a normal app (harmless but misleading).

## Counts

CRITICAL: 2, HIGH: 6, MEDIUM: 9, LOW: 6 (approx).

---

## Area: Node (node/)

### CRITICAL — node/control.sh:2365 — check_response exits on success
- Repro: any successful mqvpn response lacks `"ok": false`. `grep -q` returns 1; the `&&` list returns 1; with `set -e` the function/script exits before printing.
- Fix: replace `grep ... && fail ...` with `if grep ...; then fail ...; fi`.

### HIGH — node/install.sh:2549 — mqvpn URL placeholder
- Repro: default `mqvpn_url="TO VERIFY — official release URL"`; installer refuses to run unless `MQVPN_BINARY` or `--mqvpn-url` supplied. On a clean VPS with no manual input, install fails.
- Fix: either document as owner-only step (keep) or add a verified URL after sourcing; never invent one.

### HIGH — node/wearless-node.service:2661 — Restart=always with 5s backoff
- Repro: a bad config causes tight restart loop until StartLimitBurst (5/30s) stops the service; then stays down silently.
- Fix: add `RestartSec=10s` and an `OnFailure=` hook or explicit status check; keep StartLimit.

### MEDIUM — node/stats.sh:2606 — depends on broken control.sh
- Repro: stats.sh calls `wearless-control stats`; because control.sh exits 1 on success, stats_json becomes UNAVAILABLE.
- Fix: fix control.sh first; stats.sh then works.

### MEDIUM — node/control.sh:2348 — json_escape only escapes backslash/quote
- Repro: control characters/newlines in names already validated, but other JSON-special chars (e.g. control) could break JSON. validate_name blocks whitespace/control, so mostly safe.
- Fix: keep validation; optionally full JSON escape.

### LOW — node/install.sh:2519 — mktemp in /etc/wearless
- Repro: if install interrupted, trap removes temp; okay. But config written before service restart; if restart fails, old binary may run with new config.
- Fix: restart only after validating binary exists.

## Area: Android VpnService (app/app/.../vpn/TunnelService.kt)

### CRITICAL — TunnelService.kt:931 — establish() result ignored
- Repro: `Builder()...establish()` may return null on failure; code sets engineStarted=true earlier and proceeds to startWatchdog, reporting tunnel up.
- Fix: check `val iface = ...establish(); if (iface == null) { state=Failed("TUN establish failed"); return }`.

### HIGH — TunnelService.kt:947 — watchdog single-shot
- Repro: watchdog breaks after first restart attempt; a second failure leaves tunnel dead with no further recovery.
- Fix: loop with backoff and a max-attempt counter; re-arm watchdog after successful restart.

### HIGH — TunnelService.kt:962 — battery exemption from background
- Repro: `startActivity` from service on Android 10+ may be blocked; exemption never granted.
- Fix: move request to MainActivity (foreground) or use notification action; keep service-side try/catch.

### MEDIUM — TunnelService.kt:853 — scope not cancelled on destroy
- Repro: `CoroutineScope(Dispatchers.Default + Job())` created per instance; onDestroy cancels watchdog but not scope job.
- Fix: store scope and cancel in onDestroy.

### MEDIUM — TunnelService.kt:932 — setBlocking(true) on TUN builder
- Repro: `setBlocking(true)` may block the service thread; remove or set false.
- Fix: remove `setBlocking(true)` (default false) or run on IO dispatcher.

### LOW — TunnelService.kt:887 — isEngineAvailable reflection-ish check
- Repro: `BondingBridge.isEngineAvailable()` checks engine instance or class presence; can report "wired" even if engine not running.
- Fix: base on `engineStarted`/state instead of class presence.

## Area: Bonding core (app/bonding/)

### HIGH — SchedulerPolicy.kt:1674 + Contract.kt:1269 — adaptive scheduler dead
- Repro: `BondingConfig.scheduler: Scheduler` is non-null (default WLB). Adapter passes `config.scheduler` (never null) to `resolve(requested=...)`, which returns requested immediately. MinRTT adaptive path never runs.
- Fix: make `BondingConfig.scheduler: Scheduler? = null` (null = auto) OR add an explicit `Scheduler.AUTO`; pass through. Update tests.

### MEDIUM — StatsAggregator.kt:1771 — aggregatedBps sums rx+tx
- Repro: `sumOf { it.rxBps + it.txBps }` doubles counting; UI may show 2x actual. Ambiguous unit.
- Fix: document or split into rx/tx aggregated fields; UI must label.

### MEDIUM — FailoverMachine.kt:1455 — ALL_LINKS_LOST never recovers
- Repro: after all links lost, `onLinkUp` keeps state ALL_LINKS_LOST; session stays dead even if links return.
- Fix: either allow recovery to FAILOVER/RUNNING on onLinkUp, or document engine must restart.

### LOW — MqvpnEngineAdapter.kt:1599 — BIND_INTERFACE uses link id as iface name
- Repro: link ids ("wifi") may not be valid interface names; platform must map.
- Fix: add iface-name mapping in adapter or platform layer.

## Area: Android UI / lifecycle (app/app/)

### HIGH — ResourceMonitorActivity.kt:454 — CPU /proc parsing
- Repro: `/proc/<pid>/stat` comm field can contain spaces/parens; naive split shifts indices 13/14 → wrong CPU% or IndexOutOfBounds.
- Fix: parse after last ')'; use proper field extraction.

### MEDIUM — MainActivity.kt:258 — formatBps unit inconsistency
- Repro: kbps uses /1000, Mbps uses /1e6; mixed bases. Bytes uses 1e6/1e9.
- Fix: use consistent 1000-based or 1024-based and label units.

### MEDIUM — MainActivity.kt:150 — POST_NOTIFICATIONS request without rationale
- Repro: permission requested on launch with no explanation; denial leaves no error state.
- Fix: add rationale dialog and handle denial.

### MEDIUM — NodeConfigActivity.kt:306 — save uses apply() then finish()
- Repro: if process dies before apply flushes, config lost. Also no error handling if EncryptedSharedPreferences fails.
- Fix: use commit() or await; wrap in try/catch and show error.

### LOW — ResourceMonitorActivity.kt:382 — poller leak on rapid resume
- Repro: onResume launches new poller without cancelling previous; onPause cancels current poller field, may leak old.
- Fix: cancel poller before launching; use lifecycleScope.

### LOW — QrScanHook.kt:741 — QR not wired (documented blocker)
- Repro: spec promises QR scan; code only paste. Documented honestly; acceptable but listed.
- Fix: add camera dependency when offline-verified.

## Area: Build integrity (root)

### MEDIUM — settings.gradle.kts (root) — wrong module paths
- Repro: root settings includes `:app` and `:bonding`, but bonding lives at `app/bonding`; root `./gradlew` fails on clean machine.
- Fix: remove root gradle wrapper/settings or point to app/; document build from app/.

### MEDIUM — app/gradle/wrapper/gradle-wrapper.properties — networkTimeout=10000
- Repro: slow networks fail wrapper download.
- Fix: raise timeout or bundle gradle.

### LOW — app/app/build.gradle.kts:42 — security-crypto alpha
- Repro: alpha dependency may break on clean resolution.
- Fix: pin stable version or document.

## Area: Doc-vs-code drift

### MEDIUM — app/README.md — claims reflection; code uses direct imports
- Repro: README says bonding consumed via reflection; BondingBridge imports contract types directly.
- Fix: update README.

### LOW — WEARLESS-BOOK/roadmap promise Doze survival; TunnelService lacks Doze handling
- Repro: no `setIgnoreBatteryOptimizations` persistence or WorkManager restart; watchdog may be killed in Doze.
- Fix: implement foreground service + battery exemption + restart via JobScheduler/WorkManager.

---

End of report. No source files modified.
