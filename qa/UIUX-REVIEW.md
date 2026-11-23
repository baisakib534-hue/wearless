# UI/UX + device test review — goose (senior dev / tester)

Date: 2026-11-23/24. Device: Orca mobile emulator, AOSP Android 14 (API 34), x86_64,
480x800 mdpi, 1 CPU, 1024 MB RAM, software GPU (swiftshader), headless.
App under test: `app/app/build/outputs/apk/debug/app-debug.apk` (7,561,996 bytes).
Skills applied (per SKILLS-MAP.md): `/mobile-ui-standards`, `/mobile-ux-improver`,
`/anti-slop-design`, `/mobile-app-debugging`, `/responsive-design`.

## What was exercised (real device evidence)

| # | Flow | Result |
|---|---|---|
| 1 | Cold install + launch | PASS — install "Success", MainActivity cold start ~3-7 s |
| 2 | Notification permission (POST_NOTIFICATIONS) | PASS — real system dialog, Allow/Deny both work |
| 3 | Main screen structure | PASS — Material 3 toolbar, hero card, Start button, Links section, per-link cards (WiFi, Mobile data) |
| 4 | Honesty of unmeasured data | PASS — hero speed "NOT MEASURED", per-link speed/data "NOT MEASURED", status "No node configured — open Node Config first." |
| 5 | Node Config screen | PASS — server + key fields, honest QR blocker text ("QR scan: not wired (exact blocker: ...)"); NodeConfigActivity is NOT exported (correct security) |
| 6 | Save node config | PASS — key saved (EncryptedSharedPreferences, `wearless_node_secure.xml` created), returns to MainActivity, status becomes "Stopped" |
| 7 | Start with no config | PASS — shows "No node configured — open Node Config first.", no VPN attempt |
| 8 | Start with config | PASS — VPN consent dialog shown, then service starts |
| 9 | Tunnel with no mqvpn engine | PASS (honest failure) — status "Failed: ENGINE_NOT_INSTALLED: mqvpn engine not found on this device", no fake success, speeds still NOT MEASURED |
| 10 | TunnelService start | PASS — `ServiceRecord{... com.wearless.app/.vpn.TunnelService}` with `act=com.wearless.app.vpn.START` |
| 11 | Battery-optimization dialog | PASS — real system dialog ("Let app always run in background?") with ALLOW/DENY |
| 12 | Resource Monitor | PASS — real values: App RAM (PSS) 46.1 MB, CPU 4.4% (approx), Battery 100% (charging), battery temp 25.0 °C, per-link "NOT MEASURED (bonding engine not wired)", SoC temp "unavailable" |
| 13 | Accessibility | PASS — contentDescription on toolbar, hero card, speed, toggle, each link card, both action buttons; touch targets 48dp (verified bounds 48 px at mdpi) |
| 14 | Rotation (portrait -> landscape) | PASS (delayed) — layout re-laid out to 800x480, no crash, all text present |
| 15 | Dark mode (DayNight) | PASS — no crash, layout intact |
| 16 | Crash sweep | PASS — no `FATAL EXCEPTION` / `AndroidRuntime` crash from `com.wearless.app` in any flow |

## Findings to fix (given to jcode)

| ID | Severity | Where | Problem | Fix |
|---|---|---|---|---|
| U1 | MEDIUM | `MainActivity` Start tap with no config | Status text is identical before and after the tap, so the user gets no feedback that the tap did anything. | Show a transient inline error / Snackbar ("Configure a node first") and keep the message visible. |
| U2 | LOW | `item_link_card.xml` `linkStateText` | The on/off chip is only 17 px wide at mdpi; "off" may clip at larger text scales. | Give the chip `minWidth`/`wrap_content` padding and check at 1.3x font scale. |
| U3 | LOW | `MainActivity` hero speed caption | "Combined download speed (hero metric)" is developer-speak. | Use owner-facing wording ("Combined speed"). |
| U4 | LOW | `ResourceMonitorActivity` | Reports "SoC temp: unavailable" and per-link NOT MEASURED — correct and honest, but the screen should say why in one short line. | Add a one-line footnote that these need the bonding engine / a real device. |
| U5 | INFO | Rotation | Rotation works but only after a delay on this 1-vCPU software-GPU emulator; not a device defect. | None — noted as emulator limitation. |

## Not testable on this host (honest limits)

- Real bonding throughput / failover / battery drain — needs the owner's phone + VPS (T-08, owner-only).
- Real VPN data path — the mqvpn engine binary is not installed on the emulator, so the tunnel
  correctly reports ENGINE_NOT_INSTALLED.
- Host mode / SOCKS5 companion — needs a second device.
- Rotation is verified but slow on this emulator; verified working, not a defect.