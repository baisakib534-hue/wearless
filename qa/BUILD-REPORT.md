# qa/BUILD-REPORT.md — Build Verification + Render Smoke (test-only)

Worker: grok (build/CI seat), task `task_54e0fa047c06`.
Date: 2026-11-24.
Scope: `scripts/**` and `qa/**` only. No changes to `app/**` or `node/**` source.

Both verification scripts were run on this Windows build box via Git Bash
(`C:\Program Files\Git\bin\bash.exe`). Gradle build runs inside `app/`.

> **STATUS: verify-local.sh now FAILS** (exit 1). `:app:assembleDebug` and
> `render-smoke.sh` still pass, but `:bonding:test` fails with **9 of 43 tests
> failing**. The failure is a regression in `app/bonding/**` (T-13 re-dispatch,
> commit `ec11fb9`), **outside this seat's scope** (`scripts/**` and `qa/**` only).
> Per the task rules I do NOT fix `app/**` or `node/**` source. The scripts are
> working correctly — they faithfully surface the real test failure. The T-13
> author must fix their implementation/tests. Escalated to the coordinator with
> exact errors. Details below; see §5.

---

## 1. `scripts/verify-local.sh`

Command:

```sh
bash scripts/verify-local.sh
```

Full captured output (verbatim, exit code 0):

```
[bash -n control.sh]
RUN: bash -n /c/Users/sakim/OneDrive/Desktop/wifi/node/control.sh
PASS: bash -n control.sh

[bash -n install.sh]
RUN: bash -n /c/Users/sakim/OneDrive/Desktop/wifi/node/install.sh
PASS: bash -n install.sh

[bash -n stats.sh]
RUN: bash -n /c/Users/sakim/OneDrive/Desktop/wifi/node/stats.sh
PASS: bash -n stats.sh

[:app:assembleDebug]
RUN: /c/Users/sakim/OneDrive/Desktop/wifi/app/gradlew.bat :app:assembleDebug
...
BUILD SUCCESSFUL in 22s
34 actionable tasks: 34 up-to-date
PASS: :app:assembleDebug

[:bonding:test]
RUN: /c/Users/sakim/OneDrive/Desktop/wifi/app/gradlew.bat :bonding:test
...
BUILD SUCCESSFUL in 13s
3 actionable tasks: 3 up-to-date
PASS: :bonding:test

PASS: all local verification steps passed
EXITCODE=0
```

(Progress-bar lines from Gradle collapsed with `...` in the middle; every PASS/FAIL
line and both `BUILD SUCCESSFUL` lines are shown verbatim.)

Result: **PASS (exit 0)** — all 5 steps green:
`bash -n` on control.sh / install.sh / stats.sh, `:app:assembleDebug`,
`:bonding:test`.

Note: a non-fatal Gradle warning appears — "Android Gradle plugin (8.1.0) was tested
up to compileSdk = 33 ... compileSdk = 34". It does not fail the build (BUILD
SUCCESSFUL, exit 0). Out of scope to change here (would touch `app/`).

---

## 2. `scripts/render-smoke.sh`

Command:

```sh
bash scripts/render-smoke.sh
```

Full captured output (verbatim, exit code 0):

```
== Wearless node render/smoke check ==
PASS bash -n control.sh
PASS bash -n install.sh
PASS bash -n stats.sh
PASS systemd unit has Restart=always
PASS systemd unit is docker-free
== render/smoke result ==
PASS: node wrapper is render/smoke clean
EXITCODE=0
```

Result: **PASS (exit 0)** — node wrapper is render/smoke clean; systemd unit has
`Restart=always` and is docker-free.

Render test-only deploy steps (key as env var, never committed) are documented in
`qa/RENDER-SMOKE.md`. The Render-side `/health` result is **NOT CAPTURED** (no deploy
was run — that requires the owner's test key and is an owner/build-machine action).

---

## 3. Secret scan (tracked files)

Method: enumerated `git ls-files` (108 tracked files on the re-run after commit
`ec11fb9`), scanned every file for common
secret/token/key patterns (AWS `AKIA`/`ASIA`, PEM/OPENSSH/RSA private keys, Slack
`xox*`, GitHub `ghp_`/`gho_`/`github_pat_`, Google `AIza`, Stripe `sk_live_`/`rk_live_`,
generic `sk-`, `password=`/`api_key=`/`secret=`/`token=` assignments, `Bearer <tok>`,
`ClientSecret`). Also scanned for sensitive-named files (`.env`, `secret`,
`credential`, `*.pem`, `*.key`, `*.p12`, `id_rsa`, `*.pfx`, `keystore`, `*.jks`).

Result:

```
NO_SECRET_PATTERNS_MATCHED
TOTAL_TRACKED_FILES_SCANNED: 108
sensitive-named tracked files: NONE
Bearer/PEM/ClientSecret hits: NONE
```

Result: **CLEAN** — no secrets, keys, tokens, or credential files found in tracked
files. (Untracked `.orca-spec-*.txt` task files are not committed and were not part of
the tracked scan.)

---

## 4. APK path / size

Built by `:app:assembleDebug` during this run.

```
APK:        app/app/build/outputs/apk/debug/app-debug.apk
SIZE_BYTES: 6,730,724
SIZE_MB:    6.42 MB
MODIFIED:   2026-11-24 00:31
```

Verified present on disk after the build (rebuilt fresh on the re-run).

---

## 5. CURRENT verify-local result — `:bonding:test` FAILS (app/** regression, out of scope)

Re-run on dispatch `task_7230e46924f6` after commit `ec11fb9`
("T-13 re-dispatch: fix remaining MqvpnEngineAdapter/FailoverMachine/SchedulerPolicy/MainActivity findings")
landed. That commit changed `app/bonding/**` implementation. `:app:assembleDebug`
still BUILD SUCCESSFUL (44s), but `:bonding:test` now fails.

Exact output tail:

```
43 tests completed, 9 failed
> Task :bonding:test FAILED
FAIL: :bonding:test (exit 1)
FAIL: one or more local verification steps failed
```

The 9 failing tests and their assertions:

```
DefaultBondingEngineTest > link down mid-session continues - session alive, failover flag set() FAILED
    org.opentest4j.AssertionFailedError at MqvpnEngineAdapterTest.kt:152
DefaultBondingEngineTest > stop resets everything() FAILED
    org.opentest4j.AssertionFailedError at MqvpnEngineAdapterTest.kt:182
DefaultBondingEngineTest > stats before counters are NOT_MEASURED zeros() FAILED
    org.opentest4j.AssertionFailedError at MqvpnEngineAdapterTest.kt:141
DefaultBondingEngineTest > second start fails with ALREADY_RUNNING() FAILED
    org.opentest4j.AssertionFailedError at MqvpnEngineAdapterTest.kt:171
MqvpnEngineAdapterTest > hybrid mode and reorder buffer default in command line() FAILED
    com.wearless.bonding.BondingError$InvalidConfig at MqvpnEngineAdapterTest.kt:55
MqvpnEngineAdapterTest > scheduler selection maps to mqvpn scheduler names() FAILED
    com.wearless.bonding.BondingError$InvalidConfig at MqvpnEngineAdapterTest.kt:65
MqvpnEngineAdapterTest > per-link binding present for every link() FAILED
    com.wearless.bonding.BondingError$InvalidConfig at MqvpnEngineAdapterTest.kt:75
MqvpnEngineAdapterTest > keyRef stays a reference - no key material in output beyond the ref token() FAILED
    com.wearless.bonding.BondingError$InvalidConfig at MqvpnEngineAdapterTest.kt:85
MqvpnEngineAdapterTest > reorderBuffer false honoured() FAILED
    com.wearless.bonding.BondingError$InvalidConfig at MqvpnEngineAdapterTest.kt:97
```

### Root cause (read-only diagnosis; NOT fixed here — app/** out of scope)

`app/bonding/src/main/.../MqvpnEngineAdapter.kt` line 86 now rejects any `keyRef`
that is not a `ref:...` or `keystore:...` token:

```
return Result.failure(BondingError.InvalidConfig(
    "keyRef must be a reference token (ref:... or keystore:...), not raw key material"))
```

The tests still construct `BondingConfig("vps:443", "ref")` / `"wearless-key-1"`
(old-style plain refs). Every test that calls `prepareSession` / `engine.start`
now trips this validation, so:
- `MqvpnEngineAdapterTest` cases fail with `BondingError$InvalidConfig` (5 tests).
- `DefaultBondingEngineTest` cases fail with `AssertionFailedError` because
  `engine.start(...)` returns failure and leaves `preparedSession()`/snapshot in the
  wrong state (4 tests).

This is an implementation/test contract mismatch introduced by `ec11fb9`. The fix
belongs to the T-13 seat and touches only `app/bonding/**` (implementation or test
keyRef format). It is explicitly outside this seat's `scripts/**` + `qa/**` scope.

### What is NOT broken

- `scripts/verify-local.sh` — behaves correctly; reports the real failure (exit 1).
- `scripts/render-smoke.sh` — PASS (exit 0), independent of Gradle.
- `:app:assembleDebug` — BUILD SUCCESSFUL; APK present.
- Secret scan — CLEAN.

---

## Summary

| Check                         | Result                              |
|-------------------------------|-------------------------------------|
| `scripts/verify-local.sh`     | **FAIL (exit 1) — :bonding:test 9/43 failed (app/** regression)** |
| `:app:assembleDebug`          | PASS (BUILD SUCCESSFUL, APK present) |
| `scripts/render-smoke.sh`     | PASS (exit 0)                       |
| Secret scan (103 tracked)     | CLEAN, no matches                   |
| Debug APK                     | 6,730,724 bytes (6.42 MB)           |
| Render `/health` (live)       | NOT CAPTURED (owner action)         |

Scripts needed no fixes. The verify-local failure is a real regression in
`app/bonding/**` from commit `ec11fb9`, outside this seat's allowed scope; it is
escalated to the coordinator with exact errors rather than patched here. Only
`qa/RENDER-SMOKE.md` and `qa/BUILD-REPORT.md` were created/updated under `qa/**`.