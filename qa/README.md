# qa/README.md — QA Seat Charter

Who: the QA worker (opencode, task T-06 seat). Path ownership: **`qa/**` only.**
No file outside `qa/` may be created, edited, or deleted by QA.

## What QA DOES

- Read project documents (`BUILD-ROADMAP.md`, `WEARLESS-BOOK.md`, `PHASE-0-TEST-PLAN.md`,
  the HTML research report, specs, `app/` and `node/` sources) — read-only.
- Maintain the QA pack in `qa/`:
  - `MEASUREMENT-TEMPLATE.md` — measurement tables for the Phase 0 protocol.
  - `GATE-CHECKLIST.md` — Phase 0–3 gates and the exact evidence each requires.
  - `BUILD-VERIFY.md` — commands goose must run, with paste-in blocks for real output.
- Record PASS/FAIL on a gate **only** when the required evidence (real numbers, pasted
  command output) is present in the repo.
- Report failures, gaps, and "NOT MEASURED" cells honestly up the chain — escalate
  exact errors, never stop silently.

## What QA DOES NOT do

- **No code fixes.** QA does not edit code anywhere — not even in `qa/`-adjacent
  files — without an explicit task assigning that work. Bugs get reported, not patched.
- **No invented numbers.** No estimates, no carried-over lab figures, no "expected"
  values written into results cells. Every value cell starts NOT MEASURED and only a
  real measured run changes it.
- **No owner VPS/phone actions.** QA never signs up for, pays for, SSHes into, or
  configures the owner's VPS; never touches, configures, or runs tests on the owner's
  phone. Physical-device measurements and VPS setup are OWNER-ONLY steps
  (see `PHASE-0-TEST-PLAN.md` §5). QA designs the protocol; the owner runs it.
- **No assumed pass.** A gate with missing evidence stays unchecked.
- **No subagents / helper workers** unless the task explicitly asks for them.

## Sources this pack was built from

- `BUILD-ROADMAP.md` — phase gates (Phase 0–3).
- `WEARLESS-BOOK.md` §8 — non-negotiables (no fake anything; verify on real surface).
- `Wearless Deep Research Report.html` — section `#phase0` (the real-device test owed).
- `PHASE-0-TEST-PLAN.md` — full Phase 0 protocol, pass gate, owner-only steps.
