# qa/BUILD-VERIFY.md — Build Verification Commands (for goose to run)

Purpose: goose (the builder) runs these exact commands and pastes the **real, complete
output** into the blocks below. QA never runs these as a substitute — QA verifies the
pasted output. No output pasted = not verified. Trim nothing; paste as-is.

Work from the repo root: `C:\Users\sakim\OneDrive\Desktop\wifi` (Windows) — the
Gradle build runs inside `app/`. Linux-side commands (node scripts) run on the
VPS or a Linux shell as noted.

---

## 1. Android app — debug APK build

Command (run from `app/`):

```
cd app
gradlew.bat assembleDebug
```

(Linux/macOS: `./gradlew assembleDebug`.)

Expected: `BUILD SUCCESSFUL` and an APK at
`app/build/outputs/apk/debug/app-debug.apk` (path printed by Gradle).

### PASTE REAL OUTPUT HERE

```
goose run 2026-11-23 19:06 (+03), from app/:
> gradlew.bat :app:assembleDebug :bonding:test --console=plain
[warning] This Android Gradle plugin (8.1.0) was tested up to compileSdk = 33
[warning] Android Gradle plugin that has been tested with compileSdk = 34.
> Task :app:compileDebugKotlin UP-TO-DATE
> Task :app:packageDebug UP-TO-DATE
> Task :app:assembleDebug UP-TO-DATE
> Task :bonding:compileKotlin UP-TO-DATE
> Task :bonding:test UP-TO-DATE
BUILD SUCCESSFUL in 5s
(exit code 0; earlier cold run also exit 0)
```

### PASTE APK PATH + SIZE HERE

```
app/app/build/outputs/apk/debug/app-debug.apk — 6,544,610 bytes (6.5 MB)
(jcode first build; APK verified present by goose)
```

Verified: **PARTIAL** — build output and APK are real and pasted. Note: APK in this
build does NOT yet package :bonding (no implementation(project(":bonding")) in
app/app/build.gradle.kts at T-03 close); jcode follow-up dispatched to wire it in
(msg_c800f6bcfc11). Flip to YES after that build is verified. (flips to YES only when real output + APK path pasted above).

---

## 2. Node shell scripts — syntax check

Every shell script under `node/` must pass `bash -n` before it is counted as working.

Command (run from repo root, on Linux or in a bash shell; if `node/` does not yet
exist, record that instead — do not invent output):

```
for f in node/*.sh; do echo "== $f"; bash -n "$f" && echo "OK: $f" || echo "SYNTAX FAIL: $f"; done
```

Expected: one `OK: <file>` line per script, no `SYNTAX FAIL` lines, and exit code 0.

### PASTE REAL OUTPUT HERE

```
NOT CAPTURED
```

### Script inventory actually found in node/ (ls -1 node/*.sh)

```
NOT CAPTURED
```

Verified: **NO**

---

## 3. Node service check (on the real VPS — run by goose with owner's access)

```
systemctl status <node-service-name>
```

### PASTE REAL OUTPUT HERE

```
NOT CAPTURED
```

Verified: **NO**

---

## 4. Optional — lint/unit checks if the project adds them

If `app/` gains `gradlew.bat lint` or `test` tasks, record the command and output
here. Do not claim they pass without pasted output.

Command:

```
NOT CAPTURED
```

### PASTE REAL OUTPUT HERE

```
NOT CAPTURED
```

Verified: **NO**

---

## Rules

- Paste output exactly as the terminal printed it — errors included. A failing build
  pasted honestly is worth more than a success claimed without output.
- If a command cannot be run (missing dir, wrong OS, no VPS access), write the exact
  reason in the paste block. "NOT CAPTURED — <reason>".
- QA reads only; goose pastes.
