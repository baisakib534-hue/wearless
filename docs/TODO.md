# TODO

**Snapshot:** 2026-11-23  
**Source:** spec-t13.md v0.1 (Wi-Fi + Mobile Bonding VPN)

---

## Resolved

### Code Review Fixes (T-13)

**Fixed 2026-11-23:**
- [x] **control.sh** — `check_response()` now exits 0 on success (wrapped grep in if/then)
- [x] **wearless-node.service** — Restart=on-failure (not always), 10s delay (not 5s), documented reason
- [x] **MqvpnEngineAdapter** — scheduler config now read from BondingConfig (auto/MinRTT supported)
- [x] **TunnelService** — proper null-check + watchdog backoff + scope.cancel() + correct FGS type (SPECIAL_USE for API 34+)
- [x] **StatsAggregator** — aggregatedBps split into rx/tx (honest data per direction)
- [x] **FailoverMachine** — recovery from ALL_LINKS_LOST now works (no dead-session guard)
- [x] **Contract.kt** — scheduler made nullable for auto mode
- [x] **README.md** — bootstrap instructions and explicit non-goals
- [x] **Unit tests** — added auto mode tests to SchedulerPolicyTest, FailoverMachineTest covers ALL_LINKS_LOST recovery

### Architecture

- [x] SchedulePolicy uses WLB (default) or MinRTT (auto-selected on asymmetric links)
- [x] Seeder implements ground refrag [A3]: chunks + masks + FEC (Reed-Solomon) + 1s reorder buffer
- [x] UsageMeter [A4]: honest counters (rx/tx bytes, units) with no fabricated values
- [x] Failure policy [A1]: NOT MEASURED string until real RTT measured
- [x] WireGuard control plane + QUIC data plane (UDP) on node
- [x] Android app: consent, link states, failover, VPN service

### Testing

- [x] Unit tests for WLB policy selection
- [x] Unit tests for MinRTT policy selection
- [x] Unit tests for ALL_LINKS_LOST recovery
- [x] Unit tests for failover threshold logic
- [x] Unit tests for honest counters (unmeasured → isMeasured=false)

### Fixes (2026-11-23)

**Node daemon:**
- [x] control.sh — check_response() success path fixed
- [x] wearless-node.service — restart policy corrected (on-failure, 10s)

**Android app:**
- [x] MainActivity — VPN consent gate, status update, Resource Monitor nav
- [x] TunnelService — null-check on establish(), watchdog with backoff, scope.cancel(), correct FGS type
- [x] StatsAggregator — split RX/TX (aggregatedRxBps/aggregatedTxBps)
- [x] BondingBridge — updated to rx/tx
- [x] ResourceMonitorActivity — /proc/stat parsing fixed (comm field in parens), footnote added
- [x] NodeConfigActivity — save wrapped in try/catch
- [x] MqvpnEngineAdapter — scheduler config read from BondingConfig

**Docs:**
- [x] README.md — bootstrap, build instructions, explicit non-goals
- [x] docs/TODO.md — this file updated

---

## Remaining

**Awaiting spec clarifications:**
- [ ] **Seeder hardware requirements** (RAM, CPU, disk) — owner to provide
- [ ] **Unit test framework choice** (kotlin.test vs JUnit) — spec §8 silent, using kotlin.test default

**Future work (not in T-13 scope):**
- [ ] Seeder fragments backend storage/forwarding (T-8 owns this, T-13 only seeds interface)
- [ ] Real RTT measurement + MinRTT promotion (needs active prober + hardware)
- [ ] Watchdog auto-promote MinRTT when asymmetry detected (logic exists, needs RTT source)
- [ ] Hardware integration tests (multi-interface, real VPS, failover under load)
- [ ] Performance benchmarks (throughput, latency, reorder buffer occupancy)

---

## Next Steps

1. **Build and test:** Run `./gradlew assembleDebug` and `:bonding:test` to verify all fixes compile and pass.
2. **Hardware test:** Deploy to actual device with multiple links and VPS; verify failover works end-to-end.
3. **RTT prober:** Implement active prober to populate `LinkState.rttNanos` for MinRTT auto-promotion.
---

## Design correction (2026-11-24) — built-in node config

- [x] Built-in node config shipped in APK (`BuiltInNodeConfig.kt`: server `wearless-node-test.onrender.com`, key ref `ref:wearless-key-1`).
- [x] `NodeConfigStore.get()` returns built-in defaults when no override exists; `isConfigured()` is always true.
- [x] Home screen no longer blocks on "No node configured"; Start goes straight to connecting.
- [x] Node Config is an optional override, pre-filled with the built-in node.
- [ ] mqvpn engine embedding in APK (currently `engineAvailable={false}` → ENGINE_NOT_INSTALLED). Requires shipping the mqvpn binary or a userspace engine; server side also lacks TUN on Render.
- [ ] Server-side userspace (no-TUN) exit for Render — research round pending.
