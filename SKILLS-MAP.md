# SKILLS / PLUGINS MAP — which skill or plugin to use for which kind of work

Binding for goose and for every worker (jcode, codex, claude). A task uses the tooling noted
here. If a tool is missing or broken, fixing it is step 1 of that task — prove it works, then
do the task. If this map must change, APPEND the change + reason below; never silently swap.

Status legend: APPLIED = skill used on that work. PENDING = skill mapped, not yet applied.

## 1. Work type → skill / plugin

| Work type | Skills / plugins | Used for | Owner |
|---|---|---|---|
| UI / UX design (Android) | `ui-designer`, `ui-design-guide`, `frontend-design`, `anti-slop-design`, `mobile-ui-standards`, `mobile-ux-improver`, `responsive-design` | screen hierarchy, spacing, type scale, dark mode, touch targets, honesty of what is shown | goose (review) + jcode (implement) |
| Android app development (Kotlin) | `mobile-developer`, `mobile-app-debugging`, `mobile-toolchain`, `android`-class skills, `development-standards` | VpnService, Activities, Keystore, lifecycle | jcode |
| Kotlin bonding core (pure JVM) | `Coding`, `development-standards`, `java-pro`/`modern-java` (Kotlin-aware idioms), `code-refactor` | engine, scheduler, failover, stats | jcode (impl) / claude (tricky logic) |
| Code review | `codex` agent (reviewer seat) + skills `requesting-code-review`, `deep-review`, `analyze-code`, `code-quality-analyzer`, `anti-slop`, `sonarqube` | correctness, security, slop, style | codex |
| Bug / error hunting | `claude` agent (swarm/subagent/agent-team) + skills `systematic-debugging`, `bug-diagnosis`, `test-case-generator`, `testing-strategy-builder`, `verification-and-quality-assurance` | adversarial defect hunting | claude |
| Testing (JVM + device) | `testing-strategy-builder`, `test-case-generator`, `testing-mobile-apps`, `mobile-testing-frameworks`, `mobile-app-debugging` | unit tests, emulator tests, soak | goose |
| Mobile emulator operations | Orca `emulator` verbs (`emulator attach/tap/type/gesture/button/rotate/exec`) | install, launch, drive, screenshot the app | goose |
| Node / VPS shell (systemd, bash) | `development-standards`, `Golang Backend Development` (n/a), `cloudrun-development` (n/a) → use `development-standards` + `shellbot-website` (n/a) | install.sh, control.sh, stats.sh, hardening | codex |
| Shell script safety | `development-standards`, `bug-diagnosis` | quoting, set -eu, injection | codex (impl) / claude (hunt) |
| QA / evidence / docs | `work-report-writer`, `requirements-spec`, `agentsmd-creator`, `testing-strategy-builder` | measurement templates, gate checklists, phase reports | goose / opencode-style QA |
| Security review | `frontend-security-coder` (web, limited), `sonarqube`, `security`-class skills, `ai-skill-optimizer` (security pass) | key handling, secrets, capabilities | codex + claude |
| Performance | `api-performance`, `code-optimization`, `performance`-class skills | scheduler, stats overhead, app RAM/CPU | claude (hunt) + jcode (fix) |
| Render (test-only node deploy) | `render-deploy`, `cloudrun-development` (n/a) → `render-deploy` + `docker`-free note | bring the node up on Render for a smoke test ONLY | goose |
| Git / version control | `doro-git-essentials`, `github`-class skills | local commits, branch, push | goose |

## 2. Rules

- **Every worker brief MUST name the skills/plugins to use, written with a leading `/`**
  (for example: `/mobile-ui-standards`, `/code-refactor`, `/systematic-debugging`), so the
  worker loads and applies them. If a task needs no skill, say so explicitly.
- A task that touches UI MUST run the UI/UX skills above before goose calls it done.
- A task that produces code MUST be code-reviewed by codex and bug-hunted by claude before merge.
- No fake data ever: any speed/RAM/battery number not measured is written NOT MEASURED.
- Render is TEST-ONLY. The project is NOT built for Render; the node stays native/systemd.
- No secret in code, docs, or chat. Render key and any token live only in the deploy environment.

## 3. Applied to already-completed work (backfill)

| Task | Skill(s) applied now | Evidence file |
|---|---|---|
| T-02 Phase 0 plan | `requirements-spec`, `testing-strategy-builder` | PHASE-0-TEST-PLAN.md |
| T-03 Android shell | `mobile-ui-standards`, `mobile-ux-improver`, `anti-slop-design`, `mobile-app-debugging` | qa/UIUX-REVIEW.md |
| T-04 bonding core | `Coding`, `development-standards`, `code-refactor` | qa/CODEX-REVIEW.md (codex) |
| T-05 node wrapper | `development-standards`, `bug-diagnosis` | qa/CODEX-REVIEW.md (codex) |
| T-06 QA pack | `testing-strategy-builder`, `test-case-generator` | qa/*.md |
| T-07 scripts | `development-standards`, `bug-diagnosis` | scripts/*, qa/CODEX-REVIEW.md |
| whole repo | codex review + claude bug hunt | qa/CODEX-REVIEW.md, qa/CLAUDE-BUGHUNT.md |

## 4. Changelog (append-only)

- 2026-11-23 — created by goose as the skills/plugins map the owner asked for; T-10 (codex review)
  and T-11 (claude bug hunt) dispatched against this map.