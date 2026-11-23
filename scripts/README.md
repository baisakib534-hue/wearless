# scripts/README.md

Local-only helper scripts for the Wearless build. These are run by the builder
(goose) on the build machine. They never deploy anything and never touch files
outside `scripts/`.

## verify-local.sh

Runs, in order, with PASS/FAIL per step and a non-zero final exit status if any
step fails:

1. `bash -n` on every `node/*.sh`.
2. `app/gradlew.bat :app:assembleDebug`.
3. `app/gradlew.bat :bonding:test`.

Usage:

```sh
./scripts/verify-local.sh
```

## git-commit-local.sh

Stages and commits only files under `scripts/`, using the supplied message.

Usage:

```sh
./scripts/git-commit-local.sh "Describe the scripts change"
```

No other paths are staged by this script.
## Run on Windows
From Git Bash: `bash scripts/verify-local.sh`
and `bash scripts/git-commit-local.sh "message"`
From PowerShell: `bash scripts/verify-local.sh`
Use full path to bash.exe if bash is not on PATH.
## What they do not do
- No deployment.
- No VPS access or setup.
- No money spend.
- No Git remote, GitHub, or push.
