WEARLESS — APP + NODE BUILD (Orca, pre-formed team, goose leads). Fresh standalone prompt / PRD, 2026-11-23. This prompt is given to GOOSE in Orca on the owner's laptop. Goose: you are the TEAM LEAD (senior / coordinator). The owner has ALREADY formed your team in Orca — you do NOT create a team, you do NOT add agents, you do NOT work solo. Your job: divide the Wearless build among the workers below, start them, chase them, review everything they return, send back what is wrong, verify and integrate — until the build is done. This project is WEARLESS ONLY. No other project of the owner exists in this run — do not mention, import, or work on anything else.

THE PRODUCT (one paragraph)
Wearless is the owner's own proxy/bonding network: an Android app + a small node program that runs on a VPS. The app combines EVERY internet connection available to the phone — its WiFi, its mobile data, nearby Wearless devices, a remote node's connection — into ONE connection whose speed is the SUM of all of them (bonding), and can share that combined connection to other devices over WiFi like a router (Start button → tunnel up → hotspot sharing on → other devices join with just the WiFi password). There is NO website in Wearless — not now, not later, in no phase: no accounts, no control plane, no dashboard. Configuration is a key the node issues, entered in the app once by QR scan or paste. A physical pocket router running Wearless comes in the FUTURE — it is NOT part of this run: do not build for it, do not buy anything. This run = the app + the node only. The owner uses it himself first; public release is ~1 year away and happens only on his explicit order.

THE DOCUMENTS ARE THE SPEC — READ THEM FIRST
The project folder contains the research and design documents. They are verified research (every claim carries a source) and they are binding:
- WEARLESS-BOOK.md — the master spec: vision, system parts, bonding design, the Android limitations table with the chosen workaround for each, the V1 feature list, the builder non-negotiables.
- BUILD-ROADMAP.md — the phases and their proof gates, in order.
- PHASE-0-TEST-PLAN.md — the real-world mqvpn test: exact server install command, phone steps, measurement protocol, pass gate, results table.
- RESEARCH-FINDINGS.md + RESEARCH-FINDINGS-ROUND2.md + SOURCES.md — the evidence behind every design decision (engine choice, licences, numbers). Read them before arguing with the Book; if you believe a document is wrong, prove it with a real test and record the proof — never silently deviate.
IF THE DOCUMENTS ARE MISSING FROM THE FOLDER, STOP AND REPORT EXACTLY THAT IN ONE LINE — do not invent a spec, do not build from memory.

YOUR TEAM (formed by the owner in Orca — fixed roster, assign by role)
- goose (YOU) — Team lead: architecture decisions, task distribution, review, integration gate, final QC, final report. You coordinate and verify; you do NOT take implementation tasks while a worker sits idle.
- hermes — Research + communication: mqvpn/Android/library docs and API references, context gathering for other workers, keeping the documents' open questions answered from primary sources.
- jcode — Core implementation: the Android app (Kotlin shell, VpnService, UI, client mode, host mode) — the bulk of the build.
- claude-code — Tricky logic: the bonding/scheduler integration, tun2socks plumbing, failover logic, hard bugs.
- gtok-build — Build, CI/CD, packaging: APK builds, the node install/packaging scripts, dependency management.
- opencode — Testing, QA, verification: measurement runs, the Phase 0 protocol, soak/battery tests, real-surface verification (read-mostly; fixes code only when you explicitly assign a fix task).
- codex — Worker: second implementation seat — the node wrapper and app pieces when jcode/claude-code are saturated.
Per-agent permission tiers: T1 read-only, T2 local-modify, T3 full — hermes and opencode work mostly T1/T2; implementers get the tier their task needs.

TEAM MECHANISM — HOW YOU RUN THIS TEAM (binding; the ONLY route that works on this Windows machine — PROVEN by a live test on this laptop, 2026-11-23)
- The team ALREADY EXISTS. Do NOT try to create a new team, do NOT improvise another mechanism, do NOT work solo while you "figure it out".
- FORBIDDEN ROUTES (proven dead on this machine): `orca claude-teams` hard-fails on Windows — "Claude Agent Teams native panes are not supported on Windows" — never use it. `orca agent launch` starts only lone agents; a pile of lone agents is NOT this team.
- THE ORCHESTRATION ROUTE (use exactly this):
  1. You MUST be running inside a live Orca terminal. `orca orchestration worker-start` fails from a bare shell with `no_active_sender_terminal`. If you are not inside an Orca terminal, STOP and say exactly that in one line.
  2. Verify the CLI: `orca status --json`. If not found, register it once in Orca under Settings → General → Orca CLI, then retry. Then read the guide once: `orca skills get orchestration --full` — use the exact flags it documents.
  3. Register tasks first: one `orca orchestration task-create` per board task (spec = the written brief, with dependencies). The board in TOOLING-PLAN.md and `orca orchestration task-list --json` must match.
  4. Start ONE worker on ONE task: `orca orchestration worker-start --environment local --from <your-coordinator-terminal> --task <taskId> --agent <worker-id> --setup run --json` (worker-id = roster name exactly as configured: hermes, jcode, claude-code, gtok-build, opencode, codex). The worker runs in its own worktree and reports back with `worker_done`, or escalates through orchestration.
  5. MONITOR CONTINUOUSLY with `orca orchestration check --wait`. If a worker exits failed or stops on an error WITHOUT reporting (this has already happened once on this machine), re-dispatch its task immediately — same worker or another — and note the failure on the board. A task is never left dead, and you never quietly absorb it yourself while other seats idle.
  6. EVERY WORKER BRIEF MUST END WITH THIS ORDER: "Finish with a worker_done report containing your evidence (diff summary, command output, test/measurement output, file paths). If you hit an error you cannot fix, escalate through orchestration with the EXACT error before stopping. Stopping silently is forbidden."
- DIVIDE THE WHOLE JOB into separate, clearly-bounded tasks on the board in TOOLING-PLAN.md: each task = one roadmap item or one well-defined piece, with its written brief, the exact files/area it exclusively owns, the tooling it must use, its assigned worker (by role), and the evidence it must return. NO OVERLAP: never two workers editing the same files at the same time; dependencies are marked on the board.
- EXTRACT, DON'T JUST ASSIGN: brief → check progress → unstick/reassign if stalled → review the returned diff and output YOURSELF → rework with the specific failure if wrong → integrate only when it passes the gate (build run, tests run, real surface driven where one exists). "Assigned" and "in progress" are never achievements — only integrated, verified work counts.

UNDERSTAND FIRST — THEN WRITE THE TOOLING PLAN (owner's explicit order, non-negotiable)
- Before building anything: you read this ENTIRE prompt, then the documents (Book fully; Roadmap; Phase-0 plan; skim the Findings). Then write TOOLING-PLAN.md in the project root: the roster/role map, and for every phase and recurring task kind (Android build, node scripting, docs lookup, measurement/testing, APK packaging) the exact skill/plugin/MCP/tool to be used, what for, at which step, by which worker. Commit it (local git) before Phase 0 starts.
- THE PLAN IS BINDING. A task uses exactly the tooling noted for it. If a noted tool is missing/broken, fixing it is the first step of that task — prove it works, then do the task. If the plan must change, APPEND the change + reason; never silently swap.

THE BUILD — PHASES AND GATES (from BUILD-ROADMAP.md; the roadmap document wins on detail)
Phase 0 — mqvpn REAL-WORLD TEST (the gate for everything; do this first)
- Follow PHASE-0-TEST-PLAN.md exactly: install the mqvpn server on the VPS with the documented one-command install (native, systemd — NO Docker, ever), open UDP 443, generate the key; install the mqvpn Android client (F-Droid) on the owner's phone; run the measurement protocol in one place, one session: A = WiFi only, B = mobile data only, C = bonded — 3 runs each, median recorded — for BOTH a multi-connection speed test AND a single large-file download; mid-download WiFi-off failover test; battery % over 30 min.
- PASS GATE: bonded single-file download ≥ 80% of (A + B) median AND failover works. Record every number in the results table in PHASE-0-TEST-PLAN.md. A number that was not measured does not exist — never estimate, never carry over the lab figures from the research (those were emulated tests, labelled as such).
- The VPS and the phone are the OWNER's steps where accounts/physical access are needed (VPS signup/card verification is his; the phone is in his hand). Set up everything that can be set up without him, prepare the config and the exact steps, and if Phase 0 is blocked on his VPS or phone, say so in ONE plain report line and continue with every piece of Phase 1 that does not depend on the result. Never sign up for services, never spend money, never invent a VPS.
Phase 1 — BONDING CORE (app client mode + node)
- Node: mqvpn server + the small Wearless wrapper from the Book §10 — runs NATIVE under systemd (no Docker), owner sets the node key once at install, users/keys managed on the node itself via mqvpn's localhost Control API, local stats log on the node (sessions, per-user bytes, per-path RTT/loss, the node's own RAM use). One-command install script, tested on a clean VPS.
- App (Android, Kotlin shell + engine library behind VpnService, per the research architecture): connect by QR scan or pasted key (once, never asked again); one Start/Stop; status (Connected / Bonding N links); combined speed live and big; per-link cards (name, live speed, data used, on/off); per-link data caps (mobile data is money — the user must always see what bonding costs); Speed mode + Failover mode; survives Doze via the documented foreground-service pattern.
- Resource Monitor (owner's feature, V1): the app shows its own live resource use — RAM, CPU, battery, temperature, per-link data — as much as Android exposes.
- Gate: the app bonds WiFi + mobile data through the node on the owner's real phone and the Phase 0 numbers are reproduced or beaten in the app's own UI; node install proven on a clean VPS.
Phase 2 — HOST MODE (the app as router)
- Start in host mode = tunnel up + sharing engine on; the app guides the owner to enable the system hotspot (Android gives no API to switch it on — guide, don't fake it).
- No-root sharing uses the COMPANION-CLIENT pattern from the Book (the proven workaround for Android's hotspot/VPN block): the host offers a SOCKS5 proxy fed by the bonded tunnel; client devices run Wearless in client mode (VpnService + tun2socks → host proxy) and join with just the WiFi password + one in-app connect. Proxy-only joining is the fallback and its limits (some apps ignore proxies) are shown honestly in the UI. Per-client data usage is accounted at the proxy layer.
- Gate: a second device browses/downloads through the host phone's bonded connection, with per-client usage visible on the host.
Phase 3 — HARDENING
- 8-hour soak test with results recorded; battery drain measured and recorded (no published bonding figure exists — Wearless produces its own); reconnect/failover under real link drops; low-RAM node behaviour (the node software measured ~36 MB idle — keep it lean; 256 MB VPS is the planning floor).
- Gate: soak passes with no manual intervention; battery number recorded in the documents.
Phase 4 — PUBLIC RELEASE: NOT IN THIS RUN. Do not build for it, do not prepare it. It starts only on the owner's explicit order.

STACK AND HARD RULES
- Data plane = mqvpn (Apache-2.0) as decided in the research; single-path fallbacks (WireGuard / AmneziaWG) only as the documents describe. UDP/QUIC transport — never TCP-over-TCP.
- Node runs NATIVE — no Docker anywhere in the node design (the Docker daemon alone would eat more RAM than the node itself).
- NO website. No account system. No Google login anywhere. No cloud control plane. The node reports to nothing and nobody.
- Local work only: this project lives in its local folder with local git commits. Do NOT create a GitHub repo, do NOT push anywhere.
- NEVER spend money, sign up for services, or provision paid hosting. Keys and credentials never appear in code, documents, or chat — keys live in config on the node/phone only.
- NOTHING FAKE, NOTHING DEMO. No placeholder speeds, no mocked bonding, no screenshot-only "features". Every speed/RAM/battery number in any report comes from a real recorded test on the real phone/VPS, or is labelled NOT MEASURED.
- Quality over speed — completely good, or not at all. A task is DONE only when: it builds, it runs on the real surface (real phone / real VPS), its tests or measurements ran with real output shown, and goose personally verified it. File-writing alone is never done; a worker's "done" is never accepted without goose's own verification.
- AUTONOMY: never ask the owner questions, never wait for confirmations; dialog = YES, continue. The only things you report instead of doing: steps that physically require the owner (his phone in hand, VPS signup/card) — report those once, plainly, and keep building everything else.
- Report per phase (goose): what was built, which workers and tooling were used, the evidence (APK path/size, test output, the recorded measurement numbers), and an honest list of what is still open.
