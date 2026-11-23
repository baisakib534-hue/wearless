# WEARLESS — TODO / task board

Mirror of Orca run `run_0414b3280f3f` and `TOOLING-PLAN.md`.
✔ = settled and goose-verified. ☐ = open. ◐ = running. ⛔ = blocked on owner/hardware.

## Phase 0 — proof gate
- [✔] T-01 Read all documents; write binding `TOOLING-PLAN.md`; local git init — goose
- [✔] T-02 Reconstruct `PHASE-0-TEST-PLAN.md` from HTML `#phase0` + source IDs — opencode — verified, committed
- [ ] ⛔ T-08 Real phone + VPS measurement (A/B/C, WLB vs MinRTT, failover, battery) — OWNER only — `task_b3d25f28fe4e`
- [✔] T-06 QA templates / evidence forms in `qa/` — opencode — verified, committed

## Phase 1 — bonding core (prep that does not need T-08)
- [✔] T-03 Android app shell in `app/**` — jcode — assembleDebug SUCCESS
- [✔] T-04 Bonding core module in `app/bonding/**` — claude — 40/40 tests pass
- [✔] T-05 Node wrapper + native systemd install in `node/**` — codex — bash -n clean
- [✔] T-07 Local packaging/CI scripts in `scripts/**` — codex — verify-local.sh all PASS (f70c2fc)

## Quality gates (owner-mandated review seats)
- [✔] T-11 claude swarm bug hunt — claude — `ctx_9a167bda4245` — `qa/CLAUDE-BUGHUNT.md` (2 CRITICAL, 6 HIGH, 9 MEDIUM, 6 LOW)
- [◐] T-10 codex code review — codex — `ctx_d47ebdd866f7` — writes `qa/CODEX-REVIEW.md` (was idle 55m, nudged)
- [✔] T-12 jcode professional UI/UX + accessibility pass — jcode — `ctx_fbee7936038b` — Material 3, dark mode, content descriptions, 48dp targets
- [ ] T-13 jcode: fix all CRITICAL/HIGH findings from T-10 + T-11 + UI/UX review — jcode — `task_6a156708282d`
- [✔] T-14 goose: mobile-emulator test pass — Android 14 AOSP x86_64, install/launch/permissions/node-config/save/start/VPN-consent/engine-fail/Resource-Monitor/rotation/dark-mode — see `qa/UIUX-REVIEW.md`
- [ ] T-15 goose: Render test-only node deploy (key as env var, never committed)

## Skills / plugins
- [✔] `SKILLS-MAP.md` — work type → skill/plugin map, and the rule that every worker brief names
  the skills to use with a leading `/`.

## Gate / close
- [ ] T-09 Goose integration gate + phase report — goose

## Worker health
- [✔] claude — bug hunt delivered
- [✔] jcode — UI/UX pass delivered
- [◐] codex — review seat running (nudged after 55m idle)
- [ ] ⛔ hermes — 3× `agent_readiness: timeout`; not in the owner's final three-agent roster
- [ ] opencode — idle (not in the owner's final three-agent roster)

## Environment setup done by goose
- Android SDK: `emulator` 37.3.10, `system-images;android-34;google_apis;x86_64`,
  `system-images;android-34;default;x86_64` (AOSP, light) installed.
- AVDs: `wearless_pixel` (google_apis) and `wearless_aosp` (AOSP, used for testing:
  480x800, 1024 MB, 1 core, software GPU).
- Orca mobile emulator attached to this worktree; app installed and driven for real.