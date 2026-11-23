Task T-06 — QA pack for Wearless gates.

You are opencode, QA seat. Read BUILD-ROADMAP.md gates, WEARLESS-BOOK.md §8, and the HTML report #phase0. Exclusive path: qa/** ONLY. Do not edit other files.

Create qa/ with:
- MEASUREMENT-TEMPLATE.md: tables for A WiFi-only, B mobile-only, C bonded (single large-file download + multi-connection speed test), 3 runs, medians, WLB vs MinRTT, failover, 30-min battery; every value cell starts "NOT MEASURED".
- GATE-CHECKLIST.md: Phase 0–3 gates as checkboxes with the exact evidence required per gate (real numbers, APK build output, node bash -n output, soak/battery numbers). No assumed pass.
- BUILD-VERIFY.md: exact commands goose must run to verify app and node work (gradlew assembleDebug, bash -n node/*.sh), with a place to paste real output.
- README.md: what QA may and may not do — read-mostly, no code fixes without an explicit task, no invented numbers, no owner VPS/phone actions.

Tooling: your writer only; local files. Finish with a worker_done report containing evidence (file paths, diff summary). Escalate exact errors; never stop silently.
