# Wearless App Emulator Flow Test Report

Device: wearless_aosp (emulator-5554, Android 14, x86_64)
Package: com.wearless.app/.MainActivity
APK: app/app/build/outputs/apk/debug/app-debug.apk (also apk/wearless-debug.apk, 6,595,424 bytes)
Date: 2026-11-24

## Flow Test Summary

| Flow | Status | Notes |
|---|---|---|
| 01 App launch | Driven | Captured 01-launch.png |
| 02 Home screen | Driven | Stopped status, NOT MEASURED speeds (02-home.png) |
| 03 Node config | Driven | 03-node-config.png |
| 04 Input node key | Driven | TESTNODEKEY0001 entered (04-06 screenshots) |
| 05 Save node config | Driven | Saved to Keystore (08-saved-key.png) |
| 06 Tap Connect/Start | Driven | Hero card: "Failed: Bonding engine start failed: ENGINE_NOT_INSTALLED: mqvpn engine not found on this device" (09-10 screenshots) |
| 07 Per-link cards | Driven | WiFi + Mobile show off / NOT MEASURED (11-link-tap.png) |
| 08 Scroll home | Driven | 12-13 screenshots |
| 09 Resource Monitor | Driven | PSS 50.5MB, CPU 1.9%, battery 100%, temp 25C, per-link NOT MEASURED (14-resource-monitor.png) |
| 10 Rotate | Driven | Landscape reflow (15-landscape.png) |
| 11 Dark mode | Driven | Material3 dark palette (16-dark-mode.png) |

## Engine availability
BondingBridge.isEngineAvailable() returns false because MqvpnEngineAdapter.engineAvailable defaults to { false } and no mqvpn binary is shipped in the APK. MqvpnEngineAdapter.prepareSession therefore returns BondingError.EngineNotInstalled. This is the honest failure path; no fake session is created.

## Real connection proof — BLOCKED
Attempted end-to-end tunnel against https://wearless-node-test.onrender.com. Server side fails:
  05:52:09.715 [ERR] TUN create failed
  05:52:09.715 [ERR] open /dev/net/tun: No such file or directory
Render free web services do not expose /dev/net/tun (no CAP_NET_ADMIN), so mqvpn cannot bring up mqvpn0 and no session/traffic can be proven.

Emulator side: /dev/tun exists but is crw-rw---- system:vpn; the app (non-root) cannot open it. adb root can open it, but that is not the app's runtime identity, so it does not constitute proof of the app connecting.

## Provider per PROVIDERS.md
Only Fly.io documents real TUN/UDP support, and it requires a paid dedicated IPv4 (unavailable on the free plan). No Fly token or flyctl present in this environment. No other provider in PROVIDERS.md grants TUN.

## Required to finish
1. A host with /dev/net/tun + CAP_NET_ADMIN (or Fly.io paid dedicated IPv4 + token + flyctl).
2. Deploy the gateway container there, run scripts/gateway-client.sh against the new URL as root, capture ping + real download through the tunnel.
3. Embed or push the mqvpn engine binary (x86_64 Android or push to emulator) and re-run the app flow; screenshot connected state with moving counters.

## Logcat (com.wearless.app)
PID 2763
11-24 08:05:04.885 2763 2763 D com.wearless.app: MainActivity created and rendered
11-24 08:13:30.489 2763 2770 I com.wearless.app: NodeConfigStore saved server=127.0.0.1:443
11-24 08:31:22.011 2763 2763 I com.wearless.app: TunnelService foreground start requested
11-24 08:35:10.123 2763 2763 E com.wearless.app: Bonding engine start failed: ENGINE_NOT_INSTALLED: mqvpn engine not found on this device
11-24 08:41:05.412 2763 2763 I com.wearless.app: ResourceMonitorActivity opened: PSS=50.5MB, CPU=1.9%

Screenshots: qa/emulator-flows/01-launch.png .. 16-dark-mode.png (16 files).