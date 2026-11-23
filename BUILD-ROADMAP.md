# WEARLESS — Build Roadmap (for the builder AI / developer)

Read `WEARLESS-BOOK.md` first (what to build and why), then `RESEARCH-FINDINGS.md` (what is proven), and keep `SOURCES.md` open — every claim in the other documents carries a source ID (e.g. [A2], [C5]) that SOURCES.md maps to a URL you can open and check yourself. Build in this order. **Do not skip the proof gates** — a phase is done only when its gate passes on real hardware (owner's Samsung phone + a real VPS), with numbers recorded.

**Owner's decision (2026-11-18): NO website — removed entirely.** Wearless is app + node only, in every phase. The app is configured by QR code / pasted key. Do not design or build any website, account system, or control plane.

## Phase 0 — Research completion (current)
- [x] Deep research rounds 1 + 2 integrated into RESEARCH-FINDINGS.md / RESEARCH-FINDINGS-ROUND2.md; unverifiable items kept marked.
- [x] mqvpn server RAM measured in the lab (2026-11-18): idle RSS ≈ 36 MB, healthy under a 48 MB hard cap — a Full node fits a 100 MB budget (record: ~/workspace/wearless-lab/MEASUREMENT-2026-11-18.md). Under-load RAM still to measure on a real VPS.
- [ ] mqvpn hands-on: build/run its Android client + self-hosted server; measure WiFi-only, mobile-only, bonded speeds on the owner's phone.
- **Gate:** a written test result: three speed numbers (WiFi / mobile / bonded) from the same place, same hour.

## Phase 1 — Bonding core proof (no UI polish yet)
- [ ] Bonding server (node) on a real VPS (Oracle Always Free first; Singapore-region latency check from Dhaka). Runs as a native systemd service — **no Docker** (daemon overhead alone ≈ 100–150 MB would swamp a small host; the node itself is ~36 MB).
- [ ] Android client (mqvpn-based or from scratch if mqvpn fails the gate) that bonds WiFi + mobile data in Speed mode with failover.
- [ ] Per-link live speed + data counters (even if the UI is rough).
- [ ] **Resource Monitor (owner's order, 2026-11-18):** a screen in the app showing the app's own live resource use — RAM, CPU, battery drain, device temperature, per-link data — as much as Android exposes to an app about itself. This is also the tool that produces the Phase 3 battery numbers.
- [ ] Node setup without any website: server address + key entered in the app by QR scan or paste; revoke = change the key on the node.
- **Gate:** single-file download over bonded links reaches ≥80% of the sum of the two single-link speeds, three runs recorded. If it cannot, stop and report why — do not fake it.

## Phase 2 — Host mode (sharing)
- [ ] Start button: tunnel up + hotspot on + sharing engine on (proxy-pattern engine, ShieldShare-style, with per-client accounting).
- [ ] QR join code; connected-clients list; per-client usage; block client.
- [ ] Plain-language mode indicator: what proxy-mode means for games/UDP apps.
- **Gate:** a second phone joins with password (+QR-assisted setup where needed) and browses through the host's bonded connection; usage numbers match on both sides.

## Phase 3 — Hardening & speed tuning
- [ ] MTU/MSS tuning, transport choice tests (MP-QUIC vs MPTCP vs WireGuard), scheduler tests with unequal links (fast WiFi + slow mobile).
- [ ] Battery/Doze survival on Samsung: foreground service, exemption flow, 8-hour soak test (numbers read from the Resource Monitor).
- [ ] Data-cap auto-stop, WiFi-only bonding toggle, Redundant mode for calls.
- **Gate:** 8-hour run without the tunnel dying; bonded speed re-measured after tuning; battery drain per hour recorded.

## Phase 4 — Public-ready (only on owner's order; ~1 year private use first)
- [ ] Bangla UI, onboarding, help pages; nearby Wearless-to-Wearless link sharing.
- [ ] Pocket router phase opens here (separate decision + budget by owner).

## Rules for every phase
- Real tests on real devices; record numbers in this file under the phase.
- No demo data, no mock success screens, no placeholder counted as done.
- End of each phase: report (1) what works — proven, (2) what doesn't, (3) what's next.
