Task T-07 — Local packaging/CI scripts for Wearless.

You are codex, build/packaging seat. Do this only after your T-05 node task is settled. Read TOOLING-PLAN.md §3 and §4.

Exclusive path: scripts/** ONLY. Do not edit app/**, node/**, qa/**, root docs, git history.

Create:
- scripts/verify-local.sh: runs bash -n on node/*.sh, then (if app/gradlew.bat exists) calls it with :app:assembleDebug and :bonding:test; prints PASS/FAIL per step with real command output; never masks a non-zero exit.
- scripts/git-commit-local.sh: stages only the paths this build owns and makes a local commit with a supplied message; no remotes, no push, no GitHub.
- scripts/README.md: how to run them on this Windows machine (Git Bash or PowerShell calling bash), what they do not do (no deploy, no VPS, no money).

Tooling: POSIX shell, local git only. Run `bash -n` on the scripts and capture output. Finish with a worker_done report containing evidence (diff summary, command output, file paths). Escalate exact errors; never stop silently.
