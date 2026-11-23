# WEARLESS — TODO / task board

Mirror of Orca run `run_0414b3280f3f` and `TOOLING-PLAN.md`.
✔ = settled and goose-verified. ☐ = open. ◐ = running. ⛔ = blocked on owner/hardware.

## Phase 0 — proof gate
- [✔] T-01 Read all documents; write binding `TOOLING-PLAN.md`; local git init — goose
- [✔] T-02 Reconstruct `PHASE-0-TEST-PLAN.md` from HTML `#phase0` + source IDs — opencode (reassigned from hermes) — `ctx_06ef7e8d28e5` — verified by goose, committed
- [ ] ⛔ T-08 Real phone + VPS measurement (A/B/C, WLB vs MinRTT, failover, battery) — OWNER only — `task_b3d25f28fe4e`
- [✔] T-06 QA templates / evidence forms in `qa/` — opencode — `ctx_4b6aeb1a1445` — verified by goose, committed

## Phase 1 — bonding core (prep that does not need T-08)
- [✔] T-03 Android app shell in `app/**` (excluding `app/bonding/**`) — jcode — `ctx_d885eee65a5e` — assembleDebug SUCCESS, APK 6.5 MB
- [✔] T-04 Bonding core module in `app/bonding/**` — claude — `ctx_06bcf206021c` — 40/40 tests pass, committed
- [✔] T-05 Node wrapper + native systemd install in `node/**` — codex — `ctx_8be75dfe4b1e` — hardened unit, bash -n clean, committed
- [✔] T-07 Local packaging/CI scripts in `scripts/**` — codex — `ctx_2185fc939dbd` — verify-local.sh all PASS, committed f70c2fc

## Quality gates (owner-mandated review seats)
- [◐] T-10 codex code review of the whole repo — codex — `ctx_d47ebdd866f7` — writes `qa/CODEX-REVIEW.md`
- [◐] T-11 claude swarm bug hunt of the whole repo — claude — `ctx_9a167bda4245` — writes `qa/CLAUDE-BUGHUNT.md`
- [ ] T-12 jcode: professional UI/UX + accessibility pass on the Android app — jcode
- [ ] T-13 jcode: fix all BLOCKER/MAJOR findings from T-10 + T-11 — jcode
- [ ] T-14 goose: full mobile-emulator test pass (install, launch, every screen, permissions, rotation, stop) — goose
- [ ] T-15 goose: Render test-only node deploy (key as env var, never committed) — goose

## Gate / close
- [ ] T-09 Goose integration gate + phase report — goose

## Skills / plugins map
- See `SKILLS-MAP.md`. Rule: every worker brief names the skills to use, written with a leading `/`.

## Worker health
- [✔] codex — review task T-10 dispatched
- [✔] claude — bug-hunt task T-11 dispatched
- [✔] jcode — ready
- [ ] ⛔ hermes — 3× `agent_readiness: timeout`; no breaker bypass; docs tasks reassigned
- [ ] opencode — idle (not in the owner's final three-agent roster)