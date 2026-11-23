# THE WEARLESS BOOK — Master Build Document

**Purpose of this book:** This is the single source of truth for building Wearless. A builder (human or AI) who reads this book should be able to build the system without inventing anything. Every technical claim in this book is either (a) backed by a source listed in `RESEARCH-FINDINGS.md`, or (b) explicitly marked **TO VERIFY**. Nothing in this book is a demo, a mock, or a guess presented as fact.

**Owner:** Sakib. **Status:** Research phase (started 2026-11-18). **Current scope: Android app + node only. There is NO website — removed entirely by owner's order 2026-11-18.** Pocket router hardware is a later phase — do NOT build or buy hardware now.

**How to verify this book (for any AI agent or builder):** every technical claim carries a source ID like [A2] or [C5]. Open `SOURCES.md` (same folder), find the ID, open the URL, and check the claim yourself before building on it. IDs marked **[index-only]** or listed under "Could not verify" in SOURCES.md are NOT verified — never build on them. If a source contradicts this book, the source wins and the book must be corrected.

---

## 1. What Wearless is

Wearless is a personal proxy/bonding network with one promise:

> **All your internet connections become one — and that one combined connection runs everywhere at full speed.**

Concretely, Wearless has three ideas fused into one product:

1. **Remote WiFi (the original idea):** use another device's internet remotely, through a Wearless node, as if it were your own hotspot. A node can be a VPS, a phone, or (later) a pocket router.
2. **One-tap sharing:** the host presses one **Start** button. The app connects the tunnel and opens its own WiFi. Anyone who joins needs **only the WiFi password** — no app, no settings, no proxy fields on the client side wherever technically possible.
3. **Bonding (added 2026-11-18, core feature):** every internet connection available to the host — its WiFi, its mobile data, other Wearless devices nearby, a remote node's connection — is **combined into a single connection whose speed is the SUM of all of them**, and that combined connection is what runs on the user's phone over WiFi.

Wearless is NOT: a free-MB/carrier-loophole tool, not a clone of 9router (that is a separate project of the owner), not a generic VPN reseller. It is the owner's own network, used by the owner first; public release is planned after ~1 year.

## 2. The user's experience (north star)

The owner's UX rule, in his spirit: *settings are entered once, then it just works — that simple.* Everything lives in the app; there is no website to visit, no account to create.

Rules that follow from this:

- **One key, entered once.** The node issues a key. The app takes it by QR scan or paste, once, and never asks again.
- **One Start button.** No config files, no server addresses, no ports for the end user.
- **Password-only joining.** Clients join the Wearless WiFi with a password and nothing else (see §6 for the honest technical limits of this on Android and the chosen workarounds).
- **Speed is visible.** The app always shows: each link's live speed, the combined speed, and total data used per link (mobile data is money — the user must see what bonding costs them).

## 3. System parts

| Part | What it does | Phase |
|---|---|---|
| **Android app — Client mode** | Connects the phone to Wearless (bonding client): combines the phone's own links (WiFi + mobile data) through the bonding server; shows speeds. | Now |
| **Android app — Host mode** | Start button: tunnel up + hotspot on + sharing engine on, so other devices use this phone's (bonded) connection by password only. | Now |
| **Node agent** | Small program run on a VPS (or later a phone/router) with its node key set by the owner; accepts tunnels, keeps local stats. Runs native, no Docker. | Now (VPS first) |
| **Bonding server** | The server end of the bonding tunnel. All bonded traffic passes through it and is reassembled here — this is a hard technical requirement of true bonding (see §5). Runs on the same VPS as a node where possible. | Now |
| **Pocket router** | Palm/fist-sized travel router running the full Wearless system. Hardware exists (see research); Wearless software on it is Phase 3. | Later |

## 4. How the traffic flows

```
[Source links]                [Wearless host]              [Bonding server / Node]        [Internet]
WiFi link  ──────┐
Mobile data ─────┼─► bonding client splits packets ─► reassembled into ONE stream ─► out to internet
Nearby device ───┘        across all links                 at the server
                                  │
                                  └─► Host mode: the combined stream is shared
                                      over the phone's own WiFi hotspot (password only)
```

## 5. Bonding — the heart of the product (verified facts)

These are established by the research in `RESEARCH-FINDINGS.md`:

1. **Load balancing ≠ bonding.** Load balancing sends each session over one link: many small things speed up, but a *single* download/stream does NOT get faster [B4]. True **channel bonding** splits traffic at packet level across all links and reassembles it at a server, so even one download gets the combined speed [B1]. Wearless must do true bonding, not load balancing. (net-merge's own README admits the load-balancing limit [A13].)
2. **True bonding REQUIRES a bonding server.** The packets are split on the device and rejoined at the server; the server's connection is the ceiling for the combined speed [A9]. The server must have more bandwidth than the sum of the links (rule of thumb from OpenMPTCProuter community: ~120% of target aggregate).
3. **Realistic efficiency:** the only sourced efficiency figure is mqvpn's ≈93% of the sum in its author's *emulated* tests (no real-phone measurements exist yet). A "95%" figure often quoted for Speedify could NOT be found on Speedify's own pages — do not use it. Wearless targets 85–93% under good conditions, must *measure and display* the real number, and must auto-detect when bonding is slower than the best single link and fall back (see §5.7).
4. **Bonding modes** (copy the capability pattern, not the brand):
   - **Speed mode:** spread packets over all links for maximum total speed.
   - **Redundant mode:** duplicate packets over 2+ links, first arrival wins — for calls/livestreams that must never freeze; costs extra data.
   - **Failover:** if a link dies mid-download, the session continues on the remaining links without restarting.
5. **Candidate open-source engines** (full comparison in the research doc):
   - **mqvpn** — open-source multipath VPN using MASQUE CONNECT-IP (RFC 9484) over Multipath QUIC; has an Android client; bonds WiFi + cellular with aggregation + failover; needs a self-hosted server. *Currently the most promising base for the Android app.* Status: experimental on Android — TO VERIFY in depth.
   - **OpenMPTCProuter** — open-source, OpenWrt-based, MPTCP bonding of up to 8 links, terminates at a VPS; the reference for the later router phase and for the bonding-server design. Not an Android solution.
   - **MLVPN / Glorytun** — alternative aggregation transports used by OpenMPTCProuter; relevant for the server/router side.
   - **net-merge** — open-source Android app that uses WiFi + mobile data simultaneously, but it is load balancing (single downloads do not speed up). Useful as a reference for Android interface handling only; NOT sufficient for Wearless.
6. **Nearby-device sharing (idea worth building):** Speedify's "Pair & Share" lets nearby phones share their cellular links with each other. Wearless version: two phones running Wearless near each other can lend each other a link, so the combined pool grows. Phase 2 feature — research first.

## 6. Android limitations — stated honestly, with the chosen workaround for each

| # | Limitation (verified) | Workaround Wearless will use |
|---|---|---|
| 1 | Android hotspot traffic bypasses the phone's VPN/tunnel by design; sharing the tunnel over hotspot needs root (VPNHotspot approach). | Host mode V1 uses a **built-in local proxy engine + companion client mode**: host runs a SOCKS5 proxy fed by the tunnel; client devices run Wearless in client mode (VpnService + tun2socks → host proxy) so sharing is transparent without root (pattern proven by Hotspot-Bypass-VPN, MIT, and Every Proxy's Tun2Socks mode; ShieldShare is a design reference for per-client accounting/QR — its code has NO license, never copy it). Proxy-only joining (no client app) stays available with its limits stated in-app (games/some apps ignore proxy settings). Host mode V2 (rooted phones / pocket router) does true NAT sharing with zero client setup. |
| 2 | `LocalOnlyHotspot` gives a local network with NO internet by itself. | Never rely on it alone; sharing always goes through the sharing engine in row 1. |
| 3 | Android normally keeps only one data path "active" (WiFi OR mobile data). | The bonding client uses Android's network APIs to keep WiFi + cellular up together and bind tunnel paths per interface (the mqvpn/Speedify approach). TO VERIFY per-device behaviour on the owner's Samsung phone. |
| 4 | Android kills background apps (Doze, OEM battery savers — Samsung is aggressive). | Foreground service with persistent notification, battery-optimization exemption request, auto-restart of the tunnel, watchdog. Must be tested on the owner's actual phone, not an emulator. |
| 5 | Only ONE VPN (VpnService) can be active at a time on Android. | Wearless IS the VPN while running; the app must warn if another VPN is active and take over cleanly. |
| 6 | Bonding uses the user's mobile data — bonding can burn a data pack fast. | Per-link data counters, per-link ON/OFF switches, "WiFi-only bonding" toggle, monthly cap with auto-stop. Visible on the home screen. |
| 7 | A node behind CGNAT / home NAT cannot accept incoming connections directly. | Node agent always dials OUT and holds a persistent outbound tunnel (WireGuard-style; frp-style NAT traversal patterns). No port forwarding ever asked of the user. |
| 8 | Free hosting (Render etc.) cannot run a node: HTTP/WS inbound only, sleeps when idle, no custom ports/UDP. | Nodes and the bonding server run on a REAL VPS (Oracle Always Free is the zero-cost candidate; KVM VPS required if MPTCP kernel is needed). |

## 7. Feature list

**V1 (app + node):**
- App home: one Start/Stop button; status (Connected / Bonding N links / Sharing on); combined speed (live, big); per-link cards (name, live speed, data used, on/off); hotspot name + password + QR join code; connected clients list with per-client data usage.
- Bonding: Speed mode + Failover; per-link data caps.
- Node agent (VPS): one-command install with a node key set by the owner; bonding server co-located; local stats only.

- **Resource Monitor (owner's order, 2026-11-18):** the app shows its own live resource use — RAM, CPU, battery, temperature, per-link data — as much as Android exposes. Part of V1.

**V2:**
- Redundant mode for calls/streaming; nearby Wearless-to-Wearless link sharing; per-client quotas/blocking in host mode; scheduled on/off; Bangla UI (owner's language) alongside English.

**V3 (later phase):**
- Pocket router: full Wearless on a palm-size travel router (Cudy TR1200 class hardware noted in research; purchase DEFERRED by owner — phone first, no money spent until the app proves itself).

## 8. Non-negotiables for any builder

1. **No fake anything.** No demo data presented as real, no placeholder screens counted as features, no mock server responses. If a part is not built, the book/roadmap must say so.
2. **Verify on the real surface.** Speed claims only from real speed tests on the owner's phone + real VPS, recorded with numbers.
3. **Money decisions are the owner's.** VPS purchases, domains, hardware — propose, never buy.
4. **Credentials stay out of code and chat.** API keys and passwords live in the app's secure storage / server env, never in this book or the repo.
5. **One story = one result.** Every build round ends with: what works now (proven), what does not, what is next — in that order.

## 9. Open questions — status after deep research (2026-11-18)

- ✅ **mqvpn Android maturity:** official client exists (Android 8.0+, F-Droid v0.19.2) but is marked **experimental**; single-flow summation works via hybrid mode in the author's *emulated* tests (~93% of sum). Server = plain Linux VPS, UDP 443, no custom kernel; its Control API manages the node's users/keys locally. **Still open: real-phone performance — nobody has published it; Wearless's Phase-0 gate will produce it.**
- ✅ **Android has no native MPTCP** → OpenMPTCProuter is router-phase only; the phone uses mqvpn (MP-QUIC). Scheduler answer: no single winner — adaptive policy required (§5.7 in findings: WLB for similar links, reorder buffer under jitter, MinRTT for strongly unequal links).
- ✅ **ShieldShare license:** NONE — design reference only, code must not be copied. The reusable open implementations of the same pattern are Hotspot-Bypass-VPN (MIT) and VPNHotspot (Apache-2.0, root).
- ⏳ Best bonding-server location for Dhaka (Singapore latency) — TO VERIFY with real latency checks when the VPS is set up.
- ⏳ Samsung-specific: does the owner's phone keep WiFi + mobile data simultaneously usable by a VpnService app? — TO VERIFY on the device in Phase 0.
- ❌ **Could not be verified, do not use:** Speedify "95%" efficiency (not on Speedify's own pages); Wifi Scaler internals (no repo, self-described proof-of-concept); RLNC/network-coding gains (vendor claims only).

## 10. Round 2 decisions (2026-11-18) — app, router, node

Full detail: RESEARCH-FINDINGS-ROUND2.md in this folder.

- **App:** home screen follows the Speedify shape — one big toggle, live per-connection graphs, tappable connection list, Statistics (today/week/month). Plus a **Resource Monitor** screen (owner's order 2026-11-18): the app's own live RAM, CPU, battery, temperature and per-link data use, as much as Android exposes — the app must prove its own footprint, the same standard the node was held to (36 MB idle, measured). Architecture: Kotlin shell + the engine as a native Go/C library behind VpnService (the WireGuard/sing-box pattern); Flutter only if desktop clients come. Year 1 distribution: APK/F-Droid; Play later (its VPN policy is verified and strict).
- **Website: REMOVED entirely — owner's order 2026-11-18.** Wearless has no website in any phase: no accounts, no control plane, no dashboard. The system is the app + the node, configured by QR/pasted key. Do not design or build one. (mqvpn's own Control API is unauthenticated and binds localhost on the node — it stays on the node, for the owner's local admin only.)
- **Router:** lead candidate is now **GL.iNet Beryl AX (GL-MT3000)**. **Cudy TR1200 is downgraded** (MT7628 CPU, 100 Mbps ports, OpenWrt install needs disassembly + TFTP) — cheap in BD but a poor bonding node. Mudi V2 noted as the only battery + OpenWrt + SIM candidate. Purchase remains DEFERRED (owner's order).
- **Node — two tiers.** *Node Lite* (proxy only: kernel WireGuard + microsocks/Tinyproxy) fits ~100 MB RAM comfortably on published numbers. *Node Full* (mqvpn bonding): **measured by us on 2026-11-18 — server idle RSS ≈ 36 MB, and the server runs healthy under a hard 48 MB memory cap** (own lab test, `~/workspace/wearless-lab/MEASUREMENT-2026-11-18.md`; idle only — no client session was possible in that sandbox, and under-load memory is still to be measured on a real VPS). Practical floor: 128 MB hosts are realistic for a Full node; 256 MB stays the comfortable recommendation until the under-load number exists.
- **Node agent:** mqvpn server + a small Wearless wrapper (mqvpn has no self-update and no remote management): the owner sets the node key once at install; users/keys are managed on the node itself via mqvpn's localhost Control API (owner connects by SSH — the node reports to nothing and nobody); systemd Restart=always watchdog; local stats log on the node (sessions, per-user bytes, per-path RTT/loss, **own RSS**). **Runs NATIVE — no Docker** (owner's concern, confirmed 2026-11-18): mqvpn's own installer is a bare binary + systemd service, and the Docker daemon stack alone (dockerd + containerd ≈ 100–150 MB idle) would eat more RAM than the node itself (~36 MB) — on a 128 MB host that leaves nothing. Docker is not used anywhere in the node design.
- **Hosting reality:** NAT VPS at 128–256 MB exist for ~$2.8–8/year (UDP forwarding terms per provider unverified — check before buying). Koyeb exposes no UDP → cannot host a mqvpn node at all. Fly.io UDP needs a dedicated IPv4 ($2/mo). **Oracle Always Free is now 2 OCPU / 12 GB total** per official docs (cut from 4/24) with an idle-reclamation rule — still the best free Full node if capacity is available.

*This book is a living document. As the deep research lands, §5, §6 and §9 get updated with verified answers and sources; unverified items stay marked.*
