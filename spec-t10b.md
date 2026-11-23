Task T-10b — FAST, READ-ONLY code review. You are codex. Be quick; do not explore the whole repo.

Use this skill: /requesting-code-review (and /development-standards if you want).

Do exactly this, in order, with no detours:

STEP 1. Read these files only (they are the highest-risk code):
- app/bonding/src/main/kotlin/com/wearless/bonding/SchedulerPolicy.kt
- app/bonding/src/main/kotlin/com/wearless/bonding/FailoverMachine.kt
- app/bonding/src/main/kotlin/com/wearless/bonding/StatsAggregator.kt
- app/bonding/src/main/kotlin/com/wearless/bonding/MqvpnEngineAdapter.kt
- app/app/src/main/java/com/wearless/app/vpn/TunnelService.kt
- app/app/src/main/java/com/wearless/app/MainActivity.kt
- node/control.sh
- node/install.sh
- node/stats.sh
- node/wearless-node.service

STEP 2. Write the file qa/CODEX-REVIEW.md with exactly these sections: BLOCKER, MAJOR, MINOR, NIT.
Each finding line: `path:line — what is wrong — why it matters — exact fix`. If a section has no findings, write "none".
Keep it under 200 lines total. Do NOT fix anything. Do NOT run gradle.

STEP 3. Finish with a worker_done report: the file path, counts per severity, top 5 issues.

Shell notes: this shell is PowerShell 5.1. No `&&`, no heredocs. Use `;` and separate calls.
Do not stop silently. If you cannot write the file, escalate with the exact error.