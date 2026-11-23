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
NOT CAPTURED
```

### PASTE APK PATH + SIZE HERE

```
NOT CAPTURED
```

Verified: **NO** (flips to YES only when real output + APK path pasted above).

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
