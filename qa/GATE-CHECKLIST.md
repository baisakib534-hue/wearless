# qa/GATE-CHECKLIST.md — Phase 0–3 Gates

One checkbox per gate. A gate is checked **only** when the exact evidence named under
it exists — real measured numbers pasted in, real command output pasted in. No assumed
pass, no "should work", no lab figures carried over (WEARLESS-BOOK.md §8 rules 1–2).
Every gate below leaves its evidence slot as **NOT MEASURED / NOT CAPTURED** until a
real run fills it.

---

## Phase 0 — Research completion

**Gate 0: a written test result: three speed numbers (WiFi / mobile / bonded) from the
same place, same hour.**

- [ ] G0.1 — Three speed numbers (A Wi-Fi, B mobile, C bonded) measured on the owner's
  phone + real VPS, same place, same hour, back-to-back.
  - Evidence required: filled `qa/MEASUREMENT-TEMPLATE.md` (or equivalent owner record)
    with real Mbps per run and medians. Paste the three medians here:
    - A median: **NOT MEASURED**
    - B median: **NOT MEASURED**
    - C median (best of WLB / MinRTT): **NOT MEASURED**
- [ ] G0.2 — mqvpn hands-on on real hardware: WiFi-only, mobile-only, bonded measured
  (BUILD-ROADMAP.md Phase 0 item).
  - Evidence: same tables as G0.1; note the client/server versions actually used.
    - Client version used: **NOT MEASURED**
    - Server version used: **NOT MEASURED**

---

## Phase 1 — Bonding core proof

**Gate 1: single-file download over bonded links reaches ≥80% of the sum of the two
single-link speeds, three runs recorded. If it cannot, stop and report why — do not
fake it.**

- [ ] G1.1 — Bonding server (node) running on a real VPS (Oracle Always Free first;
  Singapore-region latency check from Dhaka recorded). Native systemd service, NO Docker.
  - Evidence required: real latency numbers (ms) Dhaka→VPS, and proof of native run:
    - Dhaka→VPS ping/RTT: **NOT MEASURED**
    - `systemctl status` output showing the node service active (paste): **NOT CAPTURED**
    - Docker absent confirmation (`docker ps` fails / not installed — paste): **NOT CAPTURED**
    - Node RSS under load (idle 36 MB already measured in lab; under-load owed): **NOT MEASURED**
- [ ] G1.2 — Android client bonds WiFi + mobile data in Speed mode with failover.
  - Evidence: bonded single-file medians from 3 runs, WLB and MinRTT:
    - Bonded WLB median (3 runs): **NOT MEASURED**
    - Bonded MinRTT median (3 runs): **NOT MEASURED**
- [ ] G1.3 — Per-link live speed + data counters present in the app (rough UI allowed).
  - Evidence: screenshot or app-screen reading showing per-link Mbps + bytes while
    bonded (paste path/reading): **NOT CAPTURED**
- [ ] G1.4 — Resource Monitor screen exists: app's own live RAM, CPU, battery drain,
  temperature, per-link data.
  - Evidence: screenshot/reading of all five values live (paste): **NOT CAPTURED**
    - RAM: **NOT MEASURED** / CPU: **NOT MEASURED** / Battery drain: **NOT MEASURED** /
      Temperature: **NOT MEASURED** / Per-link data: **NOT MEASURED**
- [ ] G1.5 — Node setup without website: server address + key entered by QR scan or
  paste; revoke = key change on node.
  - Evidence: description of the actual QR/paste flow used in a real connect (paste): **NOT CAPTURED**
- [ ] **GATE 1 decision: bonded single-file ≥ 80% of (A+B), three runs recorded.**
  - 80% threshold value: **NOT MEASURED**
  - Achieved bonded single-file median: **NOT MEASURED**
  - **GATE: NOT EVALUATED** (evaluate only when all inputs real; if fail, stop and report why)

---

## Phase 2 — Host mode (sharing)

**Gate 2: a second phone joins with password (+QR-assisted setup where needed) and
browses through the host's bonded connection; usage numbers match on both sides.**

- [ ] G2.1 — Start button: tunnel up + hotspot on + sharing engine on, one tap.
  - Evidence: step-by-step record of a real Start press on the owner's phone (paste): **NOT CAPTURED**
- [ ] G2.2 — QR join code; connected-clients list; per-client usage; block client.
  - Evidence: screenshot of clients list with per-client bytes (paste): **NOT CAPTURED**
- [ ] G2.3 — Plain-language mode indicator (what proxy-mode means for games/UDP apps).
  - Evidence: screenshot of the indicator text (paste): **NOT CAPTURED**
- [ ] **GATE 2 decision: second phone joins by password and browses through the host's
  bonded connection; usage numbers match on both sides.**
  - Second phone model: **NOT MEASURED**
  - Browsing worked through host (yes/no + what was loaded): **NOT MEASURED**
  - Host-side usage bytes for that client: **NOT MEASURED**
  - Client-side usage bytes: **NOT MEASURED**
  - Numbers match (yes/no): **NOT MEASURED**
  - **GATE: NOT EVALUATED**

---

## Phase 3 — Hardening & speed tuning

**Gate 3: 8-hour run without the tunnel dying; bonded speed re-measured after tuning;
battery drain per hour recorded.**

- [ ] G3.1 — MTU/MSS tuning + transport choice tests (MP-QUIC vs MPTCP vs WireGuard)
  and scheduler tests with unequal links (fast WiFi + slow mobile).
  - Evidence: table of tested configuration → measured bonded Mbps (paste): **NOT MEASURED**
- [ ] G3.2 — Battery/Doze survival on Samsung: foreground service, exemption flow,
  8-hour soak test (numbers read from the Resource Monitor).
  - Evidence: soak log/timeline from Resource Monitor:
    - Soak start (time, battery %): **NOT MEASURED**
    - Soak end (time, battery %): **NOT MEASURED**
    - Tunnel alive at end (yes/no): **NOT MEASURED**
    - Restart/watchdog events during soak: **NOT MEASURED**
- [ ] G3.3 — Data-cap auto-stop, WiFi-only bonding toggle, Redundant mode for calls.
  - Evidence: real test record of each feature firing once (paste): **NOT CAPTURED**
- [ ] **GATE 3 decision.**
  - 8-hour run, tunnel never died (yes/no): **NOT MEASURED**
  - Bonded speed after tuning (median of 3): **NOT MEASURED**
  - Battery drain per hour (%/h): **NOT MEASURED**
  - **GATE: NOT EVALUATED**

---

## Rules for this checklist

- A gate with any **NOT MEASURED / NOT CAPTURED** input is NOT passed.
- The evaluator (QA) records PASS/FAIL only from pasted evidence; never from memory,
  expectation, or the research report's lab figures.
- On FAIL: stop, report why, do not proceed to the next phase (BUILD-ROADMAP.md).
