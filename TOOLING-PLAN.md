# TOOLING-PLAN.md — Wearless build plan (binding, append-only)

Owner: Sakib. Lead: goose. Date: 2026-11-23. Scope: Android app + node ONLY. No website, no accounts, no control plane, no pocket-router work, no GitHub, no paid services, no Docker in the node.

## 1. Document baseline and one substitution

Read in full: `WEARLESS-BOOK.md`, `BUILD-ROADMAP.md`, `WEARLESS-BUILD-PROMPT.md`, `Wearless Deep Research Report.html`.
Named companion docs `PHASE-0-TEST-PLAN.md`, `RESEARCH-FINDINGS.md`, `RESEARCH-FINDINGS-ROUND2.md`, `SOURCES.md` are NOT in this folder. They are not invented: the HTML report contains the same protocol (section `#phase0`) and the full source list with IDs A1–A21, B1–B11, C1–C11, D1–D11 (section `#sources`). Until the named files appear, the HTML is the source of record for those IDs. Every worker brief must cite IDs, not guesses. Claims marked index-only / could-not-verify stay unverified.

Phase 0 is physically blocked on owner's VPS + phone. It is reported once; all non-dependent Phase 1 preparation continues.

## 2. Roster / role map (actual Orca agent IDs)

| Seat in build prompt | Orca agent id used here | Role in this run |
|---|---|---|
| goose | `goose` | Lead: architecture, board, review, integration gate, final QC. No implementation while a worker idles. |
| hermes | `hermes` | Research/docs — **unavailable**: 3 × `agent_readiness: timeout`; task reassigned, see §6. |
| jcode | `jcode` | Android app shell: Kotlin/Gradle, VpnService, UI, QR/paste key, Resource Monitor screen. |
| claude-code | `claude` | Bonding core: engine adapter, scheduler/failover logic, per-link stats model. |
| gtok-build | `codex` (build seat covered here) | Node wrapper + packaging/CI scripts (no gtok-build agent configured in this Orca). |
| opencode | `opencode` | Docs extraction (hermes reassignment) + QA/verification artefacts, read-mostly. |
| codex | `codex` (second seat after node task) | Node implementation seat, as above. |

No two workers own the same path. Paths are exclusive; goose owns root docs (`TOOLING-PLAN.md`, `TODO.md`) and integration.

## 3. Tooling per task kind (measured on this machine 2026-11-23)

| Kind | Exact tool | Step | Worker |
|---|---|---|---|
| Orchestration | Orca CLI `C:\Users\sakim\AppData\Local\Programs\orca\resources\bin\orca.exe`, run `orchestration worker-start --environment local --worktree current`, `--keep-open`; monitor via wake-up messages + `worker-list`/`worker-read` | every dispatch | goose |
| Task board | `orca orchestration task-create` + `TODO.md` (same IDs, ✔️ on settle) | before dispatch | goose |
| Local versioning | `git init` in project root, local commits only; never `git remote add`, never push | after this plan, after each integrated task | goose |
| Docs / primary sources | Windows shell + HTML report sections `#phase0`, `#sources`; cite `[A2]`, `[D11]` etc | doc tasks | opencode |
| Android build | JDK 17 (`%JAVA_HOME%\bin`, Temurin 17.0.21), Android SDK `C:\Users\sakim\AppData\Local\Android\Sdk` (platform `android-34`, build-tools `34.0.0`), Gradle wrapper generated into `app/`; `gradlew.bat :app:assembleDebug` | every app change | jcode, then goose verifies |
| Kotlin/bonding logic | Same JDK/Gradle, pure-Kotlin module `:bonding`; no Android UI in that module | engine/failover work | claude |
| Node scripts | POSIX shell + systemd units; static checks with `bash -n`; no Docker, no cloud calls | node work | codex |
| Packaging / CI | Shell scripts + Gradle tasks; local-only; no GitHub Actions remote | after node/app compile | codex |
| Measurement / QA | Written protocol + result templates in `qa/`; real numbers only, otherwise `NOT MEASURED` | Phase 0 + each gate | opencode |
| Verification | `git diff`, build output, `bash -n`, file reads, real-surface check where a surface exists | goose gate | goose |

If a tool above is missing or broken, fixing/proving it is step 1 of that task; never silently substitute another tool. Plan changes get appended here with a reason.

## 4. Board (phase order; gates are hard)

| ID | Task | Owner | Exclusive path | Deps | Gate / evidence |
|---|---|---|---|---|---|
| T-01 | Write this plan + local git init | goose | root docs, `.gitignore` | — | file exists, committed |
| T-02 | Reconstruct Phase 0 test plan from HTML `#phase0` + source table | opencode (reassigned from hermes) | `PHASE-0-TEST-PLAN.md` | T-01 | exact protocol, results table with `NOT MEASURED`, owner-only steps marked |
| T-03 | Android app shell (client mode UI, key entry, VpnService FGS, per-link cards, Resource Monitor screen) | jcode | `app/**` except `app/bonding/**` | T-01 | Gradle project + `assembleDebug` output or exact blocker |
| T-04 | Bonding core module (mqvpn adapter, scheduler config WLB/MinRTT, reorder buffer, failover, per-link stats model) | claude | `app/bonding/**` | T-01 | pure-Kotlin module compiles; no fake speed values |
| T-05 | Node wrapper + native systemd install (mqvpn server, key at install, localhost Control API, local stats, own RSS) | codex | `node/**` | T-01 | `bash -n` pass, unit file, no Docker, no keys in files |
| T-06 | QA pack: measurement templates, build/run checklist, gate evidence forms | opencode | `qa/**` | T-02 | templates present; not a single invented number |
| T-07 | Package/CI local scripts + integrate verified pieces | codex (after T-05) | `scripts/**` | T-03, T-04, T-05 | local build script runs; local git commits only |
| T-08 | Phase 0 real test | OWNER only (phone + VPS) | results table in `PHASE-0-TEST-PLAN.md` | T-02 | bonded ≥80% of sum + failover; otherwise honest failure report |
| T-09 | Integration gate + phase report | goose | root docs | T-03..T-07 | build output, diffs reviewed, honest open list |

## 5. Rules

- No demo data, no mocked bonding, no screenshot-only features. Unmeasured = `NOT MEASURED`.
- Every worker brief ends with: finish with `worker_done` + evidence (diff summary, command output, file paths); on unfixable error, escalate through orchestration with the EXACT error before stopping; silent stopping forbidden.
- Stalled/dead dispatch → goose re-dispatches the same task (same worker or another) and notes it on the board.
- Keys/credentials never in code, docs, or chat. VPS purchase and phone handling are owner-only.
- End of phase report: what works (proven), what does not, what is next.

## 6. Append-only change log

- 2026-11-23: hermes failed `agent_readiness: timeout` three times (`ctx_8dc91e564ed3`, `ctx_e7e598bd7db9`, `ctx_fb43ff58e17b`). Its research/docs task T-02 reassigned to opencode; T-06 follows in the same seat. No new hermes start attempted after the third failure.
- 2026-11-23: actual Orca ids are `claude` and `codex`; build-prompt names `claude-code`, `gtok-build` are mapped above. No roster creation performed — the owner-formed team is used as-is.
