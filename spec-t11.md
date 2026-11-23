Task T-11 — HUNT bugs/errors/problems across the whole Wearless repo (you are claude, swarm/subagent bug-hunting seat).

Read TOOLING-PLAN.md, TODO.md, BUILD-ROADMAP.md, WEARLESS-BUILD-PROMPT.md first.

Scope: hunt for real defects in ALL code at these paths. You may use your swarm / subagent / agent-team capability to parallelize, but the FINAL report must be one file you write.

Paths: app/bonding/**, app/app/**, node/**, scripts/**, qa/**, plus root docs for internal contradictions.

Focus (different angle from codex; be adversarial):
1. Runtime crash risks: null derefs, index OOB, unhandled exceptions, lifecycle bugs in Activities/Service, coroutine misuse, threading.
2. Android specifics: VpnService lifecycle, foreground-service type + permission correctness, Keystore/EncryptedSharedPreferences usage, battery-optimization intent, watchdog restart loops, Doze survival.
3. Logic errors: scheduler thresholds, failover state transitions, stats aggregation when isMeasured=false, unit mixups (bytes vs bits, ms vs s).
4. UX/professionalism blockers: confusing flows, missing error states shown to the user, dishonest UI (showing numbers that do not exist), accessibility gaps, unhandled permissions.
5. Build integrity: anything that builds here but would break on a clean machine (missing SDK pin, unpinned deps, path assumptions).
6. Doc-vs-code drift: where the docs promise X and the code does Y.

Method: read every file. Trace the main flows (key entry -> connect -> Start -> tunnel up -> stats -> stop). Do NOT edit source files. Do NOT run gradle (goose runs builds).

Output: write findings to a NEW file qa/CLAUDE-BUGHUNT.md, grouped by area, each with: severity (CRITICAL/HIGH/MEDIUM/LOW), file:line, reproduction or reasoning, exact fix. Add a top-10 priority list at the top. Do NOT fix anything.

Finish with a worker_done report containing: path to qa/CLAUDE-BUGHUNT.md, counts by severity, and the top 10 issues. If you hit an error you cannot fix, escalate through orchestration with the EXACT error before stopping. Stopping silently is forbidden.