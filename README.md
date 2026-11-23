# Wearless

Battery-friendly multi-link bonding + VPN for Android (and a companion node).

- **App** (`app/`) — Android app, Jetpack Compose, `com.wearless.app`.
  Multi-link bonding engine (LTE + Wi-Fi), MDM/QR provisioning, battery guard,
  per-app VPN wiring, Resource Monitor, troubleshooting screen.
- **Bonding module** (`app/bonding/`) — pure Kotlin `com.wearless.bonding`
  library: scheduler policies, link failover state machine, stats aggregator,
  bandwidth/GPS simulation engine.
- **Node** (`node/`) — Linux mqvpn server unit + CLI wrapper (`control.sh`).

Wearless is a personal VPN bonding engine that combines multiple network links
(WiFi, mobile, cable, satellite) into a single high-availability tunnel using
weighted least-loaded scheduling (WLB) and automatic failover. Built for a
personal setup: a VPS as the aggregator node and a handheld Android device as
the client.

## What it does

- **Multi-path bonding**: aggregates multiple network interfaces into one tunnel
- **Adaptive scheduling**: weighted least-loaded (WLB) or minimum-RTT, resolved
  automatically based on link conditions
- **Grounding [A4]**: when all links are lost, the engine falls back to the last
  working link (no session death without blackout)
- **Honest data only**: unmeasured metrics render as `NOT MEASURED`, never
  fabricated values

See `docs/README.md` for the docs index and `SKILLS-MAP.md` for which
specialist seats touched which area.

## Build the app

The build root is `app/` (the `gradlew` wrapper lives there, not at the repo
root):

```sh
cd app
./gradlew :app:assembleDebug        # app/ is the Gradle root
./gradlew :bonding:test             # bonding unit tests
```

Windows: `cd app` then `gradlew.bat :app:assembleDebug`.

Requires JDK 21+ (AGP 9.1 + Kotlin 2.3). The project targets Android 8.0+
(API 26) through 14 (API 34).

**Install on device**:

```sh
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Troubleshooting**: if the build fails with "SDK location not found", set
`ANDROID_HOME` in your environment or create `local.properties` with
`sdk.dir=/path/to/android-sdk`.

## Tests

```sh
cd app
./gradlew :bonding:test             # 44+ tests: scheduler, failover, stats, adapter
```

Node control wrapper tests live under `node/`.

## Scheduler modes

- **WLB (default)**: weighted least-loaded — sends packets to the link with the
  most available capacity
- **Min RTT**: minimum round-trip time — sends packets to the lowest-latency link
- **Auto mode**: resolves to WLB when no RTT data exists; Min RTT on strongly
  asymmetric links (ratio > 2)

Configured in `BondingConfig.scheduler` (null = auto).

## Explicit non-goals (V1)

- No multi-user support (single-client only)
- No GUI for node configuration (edit config on the VPS directly)
- No dynamic link addition/removal at runtime (links fixed at tunnel start)
- No hardware offload (GSO/GRO not implemented)

## Layout

```
app/            Android app (app/) + bonding library (bonding/)
node/           mqvpn node unit (wearless-node.service) + control.sh
docs/           T-06..T-13 fix reports, TODO, deep-research, build prompt
qa/             emulator smoke scripts + evidence
render-test/    Render webhook smoke (internal network, test-only)
*.md            roadmap, build prompt, book, skills map, per-task specs
```

## Status

See `docs/TODO.md` for the current task board and remaining follow-ups.