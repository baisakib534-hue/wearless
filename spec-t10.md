Task T-10 — READ-ONLY code review of the whole Wearless repo (you are codex, reviewer seat).

Read TOOLING-PLAN.md, TODO.md, BUILD-ROADMAP.md, PHASE-0-TEST-PLAN.md, WEARLESS-BOOK.md first.

Scope: review ALL code and config in the repo at these paths ONLY (read, do NOT edit anything):
- app/bonding/** (pure-Kotlin bonding core: Contract, DefaultBondingEngine, MqvpnEngineAdapter, SchedulerPolicy, FailoverMachine, StatsAggregator + tests)
- app/app/** (Android shell: MainActivity, TunnelService, BondingBridge, NodeConfigStore, QrScanHook, ResourceMonitorActivity, AndroidLinkHandle, layouts, manifest, gradle)
- node/** (install.sh, control.sh, stats.sh, wearless-node.service, README)
- scripts/** (verify-local.sh, git-commit-local.sh)
- qa/** (honesty of NOT MEASURED placeholders)

What to hunt for, per the binding rules in WEARLESS-BUILD-PROMPT.md:
1. Correctness bugs, race conditions, wrong logic in scheduler/failover/stats.
2. Any FAKE or DEMO data: placeholder speeds, mocked bonding, invented numbers, TODO stubs that pretend to work.
3. Security: key handling (keyRef must stay a token, never a secret in code/log), command injection in shell scripts, unsafe quoting, root-only config, capabilities in the systemd unit.
4. Honesty violations: anything reported as measured that is not; anything silently faked.
5. Build/packaging defects: gradle wiring, missing deps, manifest/permission issues, FGS type correctness.
6. Node robustness: set -eu usage, unquoted expansions, error handling, no-Docker rule respected.

Method: read every file fully. Run `bash -n` mentally on shell. You MAY run read-only commands (git log, file reads). Do NOT modify files, do NOT run gradle builds (goose does that).

Output: write your findings to a NEW file qa/CODEX-REVIEW.md with one section per severity (BLOCKER / MAJOR / MINOR / NIT), each finding: file:line, what is wrong, why it matters, exact suggested fix. If you find nothing in a severity, say "none". Do NOT fix anything.

Finish with a worker_done report containing: path to qa/CODEX-REVIEW.md, counts per severity, and the top 5 issues. If you hit an error you cannot fix, escalate through orchestration with the EXACT error before stopping. Stopping silently is forbidden.