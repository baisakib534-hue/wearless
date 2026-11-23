# PHASE-0-TEST-PLAN.md — Real-Device mqvpn Bonding Test

**Status: PLAN ONLY — no measurement has been performed.** This document is the reconstructed
Phase 0 protocol. It was rebuilt from the HTML report "Wearless Deep Research Report.html"
(section `#phase0` "Phase 0 — the real-device test that is still owed" and section `#sources`),
cross-checked against `WEARLESS-BOOK.md`, `BUILD-ROADMAP.md` and `WEARLESS-BUILD-PROMPT.md`.
Every result cell below reads **NOT MEASURED** — no lab figure, no estimate, is carried over.
The ≈93% figure belongs to mqvpn's author's emulated lab test [A2]; it is **not** Wearless's
number and appears nowhere in the results table.

---

## 1. Setup — fixed for every run

Extracted from HTML `#phase0`, "Setup — fixed for every run":

- **Server:** mqvpn server on a plain Linux VPS (Ubuntu 22.04 / Debian 12, UDP 443, no custom
  kernel) [A1], placed at the lowest latency available to the phone's connections — VPS placement
  decides the ceiling. The VPS uplink must exceed the sum of the phone's links, or the VPS — not
  the bonding — becomes the bottleneck. **NO Docker, ever** (per WEARLESS-BUILD-PROMPT.md Phase 0).
- **Client:** the official mqvpn Android app (Android 8.0+, F-Droid v0.19.2, marked
  experimental) [A5] on the test phone, with Wi-Fi and mobile data both active.
- **Mode:** hybrid mode + reorder buffer enabled — the configuration behind the ≈93% lab figure
  [A2] [A3]. Battery-optimization exemption granted to the app **first**, so Doze cannot kill the
  tunnel mid-test [B3].
- **Fairness:** all measurements back-to-back, same phone position, same server, same test target
  — otherwise the numbers are not comparable.
- **Scheduler knobs:** bonded runs are done once on WLB (default) and once on MinRTT [A3]; on
  asymmetric real-world links the scheduler choice can be worth more than the bonding itself
  (~77 vs ~212 Mbps in the author's emulated test [A4]).

### Install commands

- mqvpn server install command (native, systemd): **TO VERIFY — command not in provided
  sources.** The HTML report and the documents in this folder state *that* the server runs on
  plain Linux with UDP 443 and no custom kernel [A1], and that it is "packaged as a system
  service by its installer" (index-only for unit details), but no exact install command is
  present. Do not fabricate one; the owner takes it from the official mqvpn documentation at
  test time.
- mqvpn Android client: install from F-Droid, package `org.mqvpn.app`, v0.19.2, Android 8.0+,
  Apache-2.0 [A5].
- VPS firewall: open UDP 443 [A1]. Exact firewall command: **TO VERIFY — command not in
  provided sources.**
- Key/user creation: per-user PSKs via config file or the localhost Control API
  (`add_user` / `remove_user`, `--control-port 9090`, bound to 127.0.0.1 by default, no
  authentication — never expose it off-localhost) [D2]. Exact commands: **TO VERIFY — command
  not in provided sources.**

---

## 2. Measurement protocol

From HTML `#phase0` "The three measurements" + WEARLESS-BUILD-PROMPT.md Phase 0. All runs
back-to-back in one place, one session, same phone position, same server, same test target.

Each measurement is performed **twice, as two separate tests**:

1. a **multi-connection speed test** (parallel connections allowed), and
2. a **single large-file download** (one flow).

For the bonded case C, the single large-file download is the number the pass gate uses:
per-connection spread does not count — one connection must get faster for bonding to be proven
[see net-merge's counter-example, A13: "One connection always uses one link, so one big file
from one connection does not get faster"].

**3 runs each, median recorded**, for every row below.

| Leg | Radios | What to do |
|-----|--------|------------|
| **A — Wi-Fi only** | mobile data OFF | single download/stream through the tunnel over Wi-Fi only; record sustained Mbps |
| **B — mobile data only** | Wi-Fi OFF | the same test over cellular only; record sustained Mbps |
| **C — bonded** | both radios ON, hybrid mode + reorder buffer | **one single download/stream** (not several parallel connections) through the tunnel; record sustained Mbps. Run once with scheduler **WLB** (default) and once with **MinRTT** [A3] |

Additional tests:

- **Mid-download Wi-Fi-off failover:** during a bonded single-file download, kill Wi-Fi
  mid-download. The session should continue on cellular, as it did in the author's emulated
  path-cut test (276 → 73 Mbps, session alive) [A2]. Record: did the session survive, and the
  throughput after the drop.
- **Battery:** battery % over 30 minutes of bonded use. No published bonding (two-radio)
  battery-drain number exists anywhere in the research — the only verified figure is
  single-path VPN at +1.4%/day [D6], which must NOT be extrapolated to bonding. This is why
  the measurement is owed.

Record bonded ÷ (A + B) as a percentage next to the lab anchor of ≈93% [A2] — and publish
whichever number the phone actually produces, even if it is far lower.

---

## 3. PASS GATE — exact

> **PASS = bonded single-file download ≥ 80% of (A + B) median AND failover works.**

- A and B are the medians of the single large-file download runs (Wi-Fi only, mobile only).
- The bonded single-file download is the median of the C runs (report WLB and MinRTT
  separately; the better of the two is assessed against the gate, both are recorded).
- Failover works = the mid-download Wi-Fi-off test keeps the download session alive on
  cellular.
- **Fail honestly if not.** If the bonded number is at or below the better single-link number,
  bonding has failed on real hardware — the documented collapse cases (0.9 Mbps under jitter,
  ~77 Mbps on asymmetric links) show this is a real outcome, not a formality [A4]. A number that
  was not measured does not exist; never estimate, never carry over the lab figures.

---

## 4. Results — to be filled by the actual test run only

Every cell stays **NOT MEASURED** until the owner's real run writes a real number into it.

| Measurement | Result | Notes |
|-------------|--------|-------|
| A — Wi-Fi only, single large-file download (median of 3) | NOT MEASURED | sustained Mbps |
| A — Wi-Fi only, multi-connection speed test (median of 3) | NOT MEASURED | sustained Mbps |
| B — Mobile data only, single large-file download (median of 3) | NOT MEASURED | sustained Mbps |
| B — Mobile data only, multi-connection speed test (median of 3) | NOT MEASURED | sustained Mbps |
| C-WLB — Bonded, single large-file download (median of 3) | NOT MEASURED | also record as % of (A + B) |
| C-WLB — Bonded, multi-connection speed test (median of 3) | NOT MEASURED | |
| C-MinRTT — Bonded, single large-file download (median of 3) | NOT MEASURED | same conditions as the WLB run [A3] |
| C-MinRTT — Bonded, multi-connection speed test (median of 3) | NOT MEASURED | |
| Failover — Wi-Fi killed mid-download | NOT MEASURED | session survived? throughput after the drop? |
| Battery — % drain over 30 min bonded | NOT MEASURED | no published bonding figure exists [D6] |
| **PASS GATE: bonded single-file ≥ 80% of (A+B) median AND failover** | **NOT MEASURED** | gate not evaluated until all inputs are real |

Until this table is filled with real numbers, nothing in the research report may be quoted as
Wearless's speed.

---

## 5. OWNER-ONLY steps

Nothing below may be signed up, bought, or measured by any worker. These require the owner's
payment card, accounts, or physical hands:

- **VPS signup and payment** — choosing and paying for the Linux VPS (Ubuntu 22.04 / Debian 12,
  UDP 443 capable, uplink exceeding the sum of the phone's links) is the owner's step. No worker
  signs up for any service or spends money.
- **Physical phone in hand** — the owner's Android phone (Android 8.0+) with the mqvpn F-Droid
  client [A5], SIM with active mobile data, and Wi-Fi. The owner physically: grants the
  battery-optimization exemption, toggles radios between legs, kills Wi-Fi mid-download for the
  failover test, and reads the battery % over 30 minutes.
- **Key handling** — the owner creates the user/PSK on his own VPS via the localhost Control
  API or config file [D2] and scans/enters it into the app. No worker touches keys or
  credentials.
- **Running and recording the measurements** — the owner (or the owner supervising) performs
  the runs and writes the real numbers into the table in section 4.

A worker's job ends at this document: the protocol, the config preparation, and the exact
steps, prepared so the owner can execute without ambiguity.

---

## 6. Provenance

- **Primary source of this protocol:** "Wearless Deep Research Report.html", section
  `#phase0` ("Phase 0 — the real-device test that is still owed": setup, the three
  measurements, pass criteria, results table) and section `#sources` (source IDs and what each
  proves), both dated 18 Nov 2026.
- **Cross-checked against:** `WEARLESS-BOOK.md` (§8 proof gates, TO VERIFY items),
  `BUILD-ROADMAP.md` (Phase 0 gate on owner's Samsung phone + real VPS), and
  `WEARLESS-BUILD-PROMPT.md` (Phase 0: 3 runs each, medians, both test types, 30-min battery,
  the ≥80% pass gate, owner-only VPS/phone steps, NO Docker).
- **Source IDs cited:** [A1] mqvpn repo — server on plain Linux, Ubuntu 22.04 / Debian 12,
  UDP 443, no custom kernel; [A2] mqvpn benchmarks — ≈93% lab figure (emulated, author's, not
  Wearless's), path-cut failover test; [A3] mqvpn multipath guide — WLB/MinRTT schedulers,
  reorder buffer; [A4] mqvpn author blog — scheduler collapse/failure cases; [A5] mqvpn on
  F-Droid — official Android client v0.19.2, Android 8.0+; [A13] net-merge — why per-connection
  spread does not count; [B3] Doze and App Standby — battery-optimization exemption rationale;
  [D2] mqvpn Configuration / Control API — per-user PSKs, localhost Control API;
  [D6] NordVPN battery study — single-path VPN battery figure, explicitly NOT bonding;
  [D11] Wearless lab measurement record — mqvpn server idle RSS only, no phone, no bonded
  traffic.
- **Missing named documents:** `PHASE-0-TEST-PLAN.md`, `RESEARCH-FINDINGS.md`,
  `RESEARCH-FINDINGS-ROUND2.md` and `SOURCES.md` are named by `WEARLESS-BOOK.md`,
  `BUILD-ROADMAP.md` and `WEARLESS-BUILD-PROMPT.md` but are **not present in this folder**.
  They are not invented. This file reconstructs the Phase 0 protocol from the HTML report,
  which carries the same protocol and the full A1–A21 / B1–B11 / C1–C11 / D1–D11 source list.
  Until the named files appear, the HTML report is the source of record for those IDs.
- **No number in this document was measured.** All results cells read NOT MEASURED by design.
