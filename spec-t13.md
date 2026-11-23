Task T-13 — Fix the CRITICAL and HIGH findings from code review + bug hunt (you are jcode, the implementation seat).

Use these skills (load and apply them, named with a leading slash):
- /mobile-app-debugging
- /systematic-debugging
- /development-standards
- /bug-diagnosis
- /code-refactor

Read first: qa/CLAUDE-BUGHUNT.md (claude's findings), qa/CODEX-REVIEW.md (codex's findings, if present),
SKILLS-MAP.md, and the files you are about to change.

Exclusive editable paths: app/bonding/**, app/app/**, node/**, and app/README.md.
Do NOT touch qa/**, scripts/**, SKILLS-MAP.md, TODO.md, root specs.

Fix at minimum these (verify each against the real code first — the bug hunt used byte offsets, find the real line):

NODE
1. CRITICAL node/control.sh — `check_response()` uses `grep -q ... && fail`, so a SUCCESSFUL response
   (no `"ok": false`) makes grep return 1 and `set -e` kills the script. Rewrite as a proper
   `if grep ...; then fail; fi` so success is not treated as failure. Keep `set -eu`.
2. HIGH node/wearless-node.service — a bad config causes a tight restart loop until StartLimit stops it
   silently. Add a saner `RestartSec` and make failure visible (e.g. `OnFailure=` status unit or a comment
   pointing at journalctl). Do not weaken the hardening.

ANDROID VPN SERVICE (app/app/src/main/java/com/wearless/app/vpn/TunnelService.kt)
3. CRITICAL — `Builder()...establish()` result is ignored. If it returns null the tunnel must NOT be
   reported up. Check it, set an honest Failed state, and do not start the watchdog on failure.
4. HIGH — watchdog is single-shot; a second failure leaves the tunnel dead. Make it a bounded retry loop
   with backoff that re-arms after a successful restart.
5. HIGH — battery-optimization exemption is requested from a background service (blocked on Android 10+).
   Move the request to a foreground Activity (MainActivity) or a notification action; keep a safe try/catch.
6. MEDIUM — the per-instance CoroutineScope is not cancelled in onDestroy. Store it and cancel it.
7. MEDIUM — `setBlocking(true)` on the TUN builder can block the service thread. Remove it (default false)
   or move to an IO dispatcher.

BONDING CORE (app/bonding/**)
8. HIGH — the adaptive MinRTT scheduler is dead because `BondingConfig.scheduler` is non-null, so
   `resolve()` always returns the requested WLB. Make the auto path reachable (nullable scheduler = auto,
   or an explicit AUTO), and update the tests to cover both WLB and MinRTT resolution.
9. MEDIUM — `StatsAggregator` sums rx+tx into one number, which double-counts for a "speed" display.
   Split into rx/tx aggregated fields (or document the unit clearly) and keep the honest isMeasured rule.
10. MEDIUM — `FailoverMachine` never leaves ALL_LINKS_LOST. Allow recovery when a link comes back, or
    document loudly that the engine must be restarted. Prefer real recovery with a test.

ANDROID UI / LIFECYCLE (app/app/**)
11. HIGH — `ResourceMonitorActivity` parses /proc/<pid>/stat with a naive split; comm can contain spaces
    and parens and shift the fields. Parse after the LAST ')' and extract the utime/stime fields safely.
12. MEDIUM — `MainActivity.formatBps` mixes 1000-based and 1024-based units. Pick one base (1000, label
    kb/s vs kB/s correctly) and use it consistently; also make clear whether the hero number is rx only.
13. MEDIUM — `NodeConfigActivity` saves with apply() then finishes; use commit() (or await) and wrap
    EncryptedSharedPreferences access in try/catch with an honest error message to the user.
14. LOW — `ResourceMonitorActivity` poller can leak across rapid resume/pause. Cancel before relaunching,
    ideally with lifecycleScope.

BUILD
15. MEDIUM — the ROOT settings.gradle.kts / gradlew conflict with the real layout (bonding lives at
    app/bonding, and the real build root is app/). Make a clean-machine build unambiguous: either remove
    the stale root wrapper files or make them work. Document the one true build command in app/README.md.
16. MEDIUM — app/README.md still claims bonding is consumed via reflection; the code now imports the
    contract types directly. Update the README to match reality.

Rules:
- Honest data above all: never introduce a made-up number; keep NOT MEASURED for anything unmeasured.
- Every fix must keep the build green. When you are done, run the build yourself if you can and paste the
  real output; goose will re-run `gradlew :app:assembleDebug` and `:bonding:test` independently.
- Add or update unit tests for the bonding-core fixes (scheduler auto path, stats split, failover recovery).
- Do NOT fix anything that needs the owner's real phone or VPS; list those as NOT DONE (owner) instead.

Finish with a worker_done report containing: each finding, FIXED / NOT DONE (owner) / SKIPPED (reason),
files changed, test output, and any new risk you introduced. If you hit an error you cannot fix, escalate
through orchestration with the EXACT error before stopping. Stopping silently is forbidden.