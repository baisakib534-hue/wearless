# Wearless Android app shell (client mode)

Kotlin Android shell for Wearless client mode (Phase 1, task T-03).

## Layout

- `settings.gradle.kts` — includes `:app` and `:bonding` (bonding owned by claude, `app/bonding/` is NOT created by this task).
- `app/` — the application module (this task's exclusive path).
- `gradlew.bat` — Gradle wrapper, pinned to the locally cached `gradle-8.3-all` distribution.

## Build

```
set JAVA_HOME=C:\Users\sakim\tools\jdk17
set ANDROID_HOME=C:\Users\sakim\AppData\Local\Android\Sdk
gradlew.bat :app:assembleDebug
```

Requirements: JDK 17 (Temurin), Android SDK platform 34, build-tools 34.0.0.

## Honest-state rules

- The bonding engine (`com.wearless.bonding`, claude-owned) is consumed through `BondingBridge.kt` via reflection; its contract types are NEVER duplicated in this module. If the engine is absent, the app shows "Engine not wired — tunnel NOT up" and every speed/data value reads the literal string "NOT MEASURED".
- `AndroidLinkHandle` (app module) binds sockets to specific networks via `ConnectivityManager`/`Network.bindSocket` per [B2].
- Node key is pasted once into Node Config and stored in Android Keystore-backed `EncryptedSharedPreferences`; never logged, never echoed back.
- QR scan is an unwired hook (`QrScanHook`) — exact blocker documented in code. Paste works.
- Resource Monitor shows app RAM (PSS), approximate process CPU (/proc stat deltas), battery level/charging, battery temperature; anything Android does not expose to an app reads "unavailable".
