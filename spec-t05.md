Task T-05 — Node wrapper + native systemd install for Wearless.

You are codex, node implementation seat. Read WEARLESS-BOOK.md §10 node section and the HTML report #node plus sources A1, D2, D3, D9, D10, D11. Product: app + node only. NO website, no accounts, no cloud control plane, NO Docker anywhere (the daemon alone eats more RAM than the ~36 MB node [D11]). Never spend money, never provision a VPS, never put a key in a file you write.

Exclusive paths you own: node/** ONLY. Do not touch app/**, qa/**, scripts/**, root docs, git.

Create:
- node/install.sh: idempotent native installer for Ubuntu 22.04 / Debian 12. Takes the node key and server address from installer arguments/environment, writes them to a root-only config outside the repo (/etc/wearless/node.conf, mode 0600) — NO key or password literal in the repo. Downloads/uses the mqvpn binary per its official release path; if the exact URL cannot be verified from the provided sources, write "TO VERIFY — official release URL" and do not invent one. Opens UDP 443. Enables the systemd service. No Docker, no custom kernel, no paid service.
- node/wearless-node.service: systemd unit, Restart=always, watchdog/restart policy, native mqvpn server exec with the config file, runs as a dedicated unprivileged user where possible.
- node/control.sh: thin local-only wrapper around mqvpn Control API on 127.0.0.1:9090 [D2] for list/add/remove users and get_stats/get_status. Warn in its help text that the API has NO authentication and must stay bound to localhost. Never expose it remotely.
- node/stats.sh: writes local-only stats lines (timestamp, sessions, per-user bytes, per-path RTT/loss if the API exposes them, node RSS) to a local log path; fields that the API does not expose are printed as "NOT MEASURED", never estimated.
- node/README.md: owner-facing install/verify/revoke steps (revoke = change the key on the node), UDP 443 note, memory notes [D11], under-load RAM still NOT MEASURED on a real VPS, no website.

Tooling: POSIX shell + systemd; run `bash -n` on every .sh you create and capture the output. Git local only. Do not attempt any network install here.

Hard rules: no fake stats, no placeholder success, no credentials in code/docs/chat, no Docker, no GitHub.

Finish with a worker_done report containing your evidence (diff summary, command output, test/measurement output, file paths). If you hit an error you cannot fix, escalate through orchestration with the EXACT error before stopping. Stopping silently is forbidden.
