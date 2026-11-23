Task T-02 — Reconstruct PHASE-0-TEST-PLAN.md (owner formed team, goose leads).

You are opencode, QA/docs seat. Source of truth: WEARLESS-BOOK.md, BUILD-ROADMAP.md, WEARLESS-BUILD-PROMPT.md and the HTML report "Wearless Deep Research Report.html", especially section #phase0 and #sources. The named PHASE-0-TEST-PLAN.md / RESEARCH-FINDINGS.md / SOURCES.md are NOT in the folder; do not invent them. Extract the protocol from the HTML and cite source IDs ([A1], [A2], [A5], [D11], etc.) verbatim.

Exclusive path you own: PHASE-0-TEST-PLAN.md ONLY. Do not edit any other file. Do not touch app/**, node/**, qa/**, scripts/**, root docs, or git.

Create PHASE-0-TEST-PLAN.md containing:
1. Server/client setup from HTML #phase0 (plain Linux VPS, UDP 443, no Docker, no custom kernel; official mqvpn Android client from F-Droid, Android 8+, hybrid mode + reorder buffer; battery exemption first; same phone position/server/target for all runs). If an exact install command is not present in the provided sources, write "TO VERIFY — command not in provided sources" and do NOT fabricate one.
2. The measurement protocol: A = WiFi only, B = mobile data only, C = bonded (single flow, not parallel connections), 3 runs each, medians recorded, for BOTH a multi-connection speed test AND a single large-file download. WLB vs MinRTT bonded runs. Mid-download WiFi-off failover. Battery % over 30 minutes.
3. PASS GATE exactly: bonded single-file download >= 80% of (A + B) median AND failover works. Fail honestly if not.
4. Results table with rows for A, B, C-WLB, C-MinRTT, failover, battery — every result cell literally "NOT MEASURED" (no estimates, no lab 93% carried over).
5. Owner-only steps clearly separated (VPS signup/card, physical phone in hand). Nothing may be signed up, bought, or measured by you.
6. A provenance note naming the HTML sections and the missing named docs.

Tooling: Windows shell/type/findstr or PowerShell Get-Content to read the HTML; your writer to create the single file. No web calls, no network, no credentials.

Hard rules: no fake numbers, no website, no Docker, no keys, no GitHub. Quality over speed.

Finish with a worker_done report containing your evidence (diff summary, command output, test/measurement output, file paths). If you hit an error you cannot fix, escalate through orchestration with the EXACT error before stopping. Stopping silently is forbidden.
