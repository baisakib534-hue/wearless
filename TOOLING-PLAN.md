# Wearless TOOLING-PLAN (2026-11-24 autonomous run)

## Tasks and tools

- JCODE (coder): embed mqvpn engine in APK. Tools: Gradle/Android SDK, adb, Kotlin, Git Bash. Skills: /development-standards, /mobile-app-debugging.
- CODEX (reviewer): review VpnService fd handoff + engineAvailable. Tools: Read/Grep/Edit. Skills: /code-review.
- GOOSE (researcher): userspace no-TUN exit + provider survey + Fly.io/Oracle docs. Tools: WebSearch/WebFetch (exa), curl, git. Skills: /technical-research, /requirements-spec.
- GROK-BUILD (bugfixer): reproduce TUN error, prototype userspace exit on Render. Tools: Docker, Render API, curl, bash. Skills: /bug-diagnosis, /development-standards.

## Environment
- Windows 11, Git Bash. No docker locally. Render token available. GitHub token available.
- Emulator: wearless_aosp (emulator-5554), /dev/tun present only under adb root.

## Evidence targets
- qa/emulator-flows/ (screenshots), qa/gateway-proof/ (traffic outputs), PROVIDERS.md (experiments).