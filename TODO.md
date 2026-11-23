# WEARLESS — TODO / task board

Mirror of Orca run `run_0414b3280f3f` and `TOOLING-PLAN.md`. ✔️ = settled and goose-verified. ⬜ = open. 🔄 = running. ⛔ = blocked on owner/hardware.

## Phase 0 — proof gate
- [x] ✔️ T-01 Read all documents; write binding `TOOLING-PLAN.md`; local git init — goose
- [x] ✔️ T-02 Reconstruct `PHASE-0-TEST-PLAN.md` from HTML `#phase0` + source IDs — opencode (reassigned from hermes) · `ctx_06ef7e8d28e5` — verified by goose, committed
- [ ] ⛔ T-08 Real phone + VPS measurement (A/B/C, WLB vs MinRTT, failover, battery) — OWNER only · `task_b3d25f28fe4e`
- [x] ✔️ T-06 QA templates / evidence forms in `qa/` — opencode · `ctx_4b6aeb1a1445` — verified by goose, committed

## Phase 1 — bonding core (prep that does not need T-08)
- [ ] 🔄 T-03 Android app shell in `app/**` (excluding `app/bonding/**`) — jcode · `ctx_d885eee65a5e` (stalled on pending command approvals; resume nudge sent)
- [ ] 🔄 T-04 Bonding core module in `app/bonding/**` — claude · `ctx_06bcf206021c`
- [ ] 🔄 T-05 Node wrapper + native systemd install in `node/**` — codex · `ctx_8be75dfe4b1e`
- [ ] ⬜ T-07 Local packaging/CI scripts in `scripts/**` — codex (after T-05)

## Gate / close
- [ ] ⬜ T-09 Goose integration gate + phase report — goose

## Worker health
- [x] ✔️ claude — ready
- [x] ✔️ codex — ready
- [x] ✔️ jcode — ready
- [x] ✔️ opencode — ready (auth error cleared; worker_done received)
- [ ] ⛔ hermes — 3 × `agent_readiness: timeout`; no breaker bypass; docs tasks reassigned
