# qa/MEASUREMENT-TEMPLATE.md — Phase 0 Real-Device Bonding Measurements

**Status: TEMPLATE ONLY. No measurement has been performed.**
Every value cell below starts **NOT MEASURED**. No lab figure, no estimate, no carried-over
number may replace it. The ≈93% figure belongs to mqvpn's author's emulated test [A2] and is
**not** a Wearless number; it appears nowhere in the results tables.

Protocol source: `PHASE-0-TEST-PLAN.md` + HTML report `#phase0` section. Pass gate:
**bonded single-file download ≥ 80% of (A + B) medians AND failover works.**

---

## Fixed setup (identical for every run — record once per session)

| Item | Value |
|---|---|
| Test date / time | NOT MEASURED |
| Phone model + Android version | NOT MEASURED |
| mqvpn client version (F-Droid v0.19.2 expected) | NOT MEASURED |
| VPS provider + region | NOT MEASURED |
| VPS uplink (must exceed A + B sum) | NOT MEASURED |
| Test target (same URL/server for all legs) | NOT MEASURED |
| Battery-optimization exemption granted? | NOT MEASURED |
| Mode (hybrid + reorder buffer) confirmed? | NOT MEASURED |

Fairness rules: all legs back-to-back, same phone position, same server, same test target.

---

## A — Wi-Fi only (mobile data OFF)

3 runs each; record sustained Mbps per run, then the median.

### A1. Single large-file download (one flow)

| Run | Result (Mbps) |
|---|---|
| Run 1 | NOT MEASURED |
| Run 2 | NOT MEASURED |
| Run 3 | NOT MEASURED |
| **Median** | **NOT MEASURED** |

### A2. Multi-connection speed test (parallel connections)

| Run | Result (Mbps) |
|---|---|
| Run 1 | NOT MEASURED |
| Run 2 | NOT MEASURED |
| Run 3 | NOT MEASURED |
| **Median** | **NOT MEASURED** |

---

## B — Mobile data only (Wi-Fi OFF)

### B1. Single large-file download (one flow)

| Run | Result (Mbps) |
|---|---|
| Run 1 | NOT MEASURED |
| Run 2 | NOT MEASURED |
| Run 3 | NOT MEASURED |
| **Median** | **NOT MEASURED** |

### B2. Multi-connection speed test

| Run | Result (Mbps) |
|---|---|
| Run 1 | NOT MEASURED |
| Run 2 | NOT MEASURED |
| Run 3 | NOT MEASURED |
| **Median** | **NOT MEASURED** |

---

## C — Bonded (both radios ON, hybrid mode + reorder buffer)

The single large-file download is the number the pass gate uses. Per-connection
spread does not count — one connection must get faster [A13].

### C-WLB-1. Bonded, scheduler WLB (default), single large-file download

| Run | Result (Mbps) |
|---|---|
| Run 1 | NOT MEASURED |
| Run 2 | NOT MEASURED |
| Run 3 | NOT MEASURED |
| **Median** | **NOT MEASURED** |
| **As % of (A1 + B1) medians** | **NOT MEASURED** |

### C-WLB-2. Bonded, WLB, multi-connection speed test

| Run | Result (Mbps) |
|---|---|
| Run 1 | NOT MEASURED |
| Run 2 | NOT MEASURED |
| Run 3 | NOT MEASURED |
| **Median** | **NOT MEASURED** |

### C-MinRTT-1. Bonded, scheduler MinRTT, single large-file download
(Same conditions as the WLB run [A3]; on asymmetric links the scheduler choice can
be worth more than the bonding itself — author's emulated ~77 vs ~212 Mbps [A4].)

| Run | Result (Mbps) |
|---|---|
| Run 1 | NOT MEASURED |
| Run 2 | NOT MEASURED |
| Run 3 | NOT MEASURED |
| **Median** | **NOT MEASURED** |
| **As % of (A1 + B1) medians** | **NOT MEASURED** |

### C-MinRTT-2. Bonded, MinRTT, multi-connection speed test

| Run | Result (Mbps) |
|---|---|
| Run 1 | NOT MEASURED |
| Run 2 | NOT MEASURED |
| Run 3 | NOT MEASURED |
| **Median** | **NOT MEASURED** |

---

## Failover — Wi-Fi killed mid-download (bonded, single-file download active)

| Item | Value |
|---|---|
| Session survived on cellular? (yes/no) | NOT MEASURED |
| Throughput after the drop (Mbps) | NOT MEASURED |
| Time to recover (s) | NOT MEASURED |

Reference only, NOT a result: author's emulated path-cut kept the session alive
(276 → 73 Mbps) [A2].

---

## Battery — 30 minutes of bonded use

| Item | Value |
|---|---|
| Battery % at start | NOT MEASURED |
| Battery % at 30 min | NOT MEASURED |
| **Drain (% over 30 min)** | **NOT MEASURED** |

No published bonding (two-radio) battery-drain number exists [D6]; the +1.4%/day
single-path VPN figure must NOT be extrapolated.

---

## PASS GATE evaluation

> **PASS = bonded single-file download ≥ 80% of (A1 + B1) medians AND failover works.**

| Item | Value |
|---|---|
| 80% threshold = 0.8 × (A1 + B1) medians | NOT MEASURED |
| Better of C-WLB-1 / C-MinRTT-1 median ≥ threshold? | NOT MEASURED |
| Failover session survived? | NOT MEASURED |
| **GATE: PASS or FAIL** | **NOT MEASURED** |

Fail honestly if not. If the bonded number is at or below the better single-link
number, bonding has failed on real hardware — a real outcome, not a formality [A4].
Gate is not evaluated until every input cell holds a real measured number.
