# qa/RENDER-SMOKE.md — Render TEST-ONLY Smoke Deploy

Purpose: run the Wearless node wrapper smoke checks on a Render **test** service so
the same `scripts/render-smoke.sh` code that runs locally is exercised on a real
Linux container. This is a TEST-ONLY path. The production node runs **native under
systemd on the owner's VPS** (see `node/`); Render never runs the production node.

This document records the steps only. No key value is stored in this file or anywhere
in the repo. The Render API key is supplied as an **environment variable / local
gitignored file**, never committed.

---

## What the test service actually does

- Builds `render-test/Dockerfile` (ubuntu:22.04 + bash + ca-certificates + socat).
- Copies `node/`, `scripts/render-smoke.sh`, `render-test/health-server.sh` into the image.
- On boot runs `render-smoke.sh`, then serves `/health` via `health-server.sh`, which
  returns the **real** smoke output and `smoke_exit=<code>`.
- If the smoke checks fail, the container exits non-zero and Render marks the deploy
  unhealthy.

Descriptor: `render.yaml` (`services[0]` = `wearless-node-test`, `runtime: docker`,
`plan: free`, `healthCheckPath: /health`).

The test service holds **no secret**. It does not run `mqvpn`, does not touch the
owner's VPS, and does not pretend to be the production node.

---

## Key handling (TEST-ONLY, never committed)

The Render API key is used **only** to drive the Render API/CLI from the build machine.
It is provided one of these ways, never written into any tracked file:

1. **Environment variable** in the current shell (preferred):

   ```sh
   # PowerShell (build machine)
   $env:RENDER_API_KEY = "<owner's test key>"
   ```

   ```sh
   # bash
   export RENDER_API_KEY="<owner's test key>"
   ```

2. **Local gitignored file** `.render-key` at the repo root (already ignored; never
   committed). `render.yaml` references this convention in its header comment.

Rules:
- Do not paste the key into any file under `qa/`, `scripts/`, `render-test/`, or the repo.
- Do not `echo` the key into a report or log that gets committed.
- Revoke/rotate the key after the smoke test if it was shared in plaintext anywhere.

---

## Steps (OWNER / build machine — NOT run by QA)

> QA does not run deploys. These are the exact steps for whoever holds the test key.

1. **Set the key as an env var** (see "Key handling" above). Confirm it is set without
   printing it:

   ```sh
   # returns the length only, never the value
   printf 'RENDER_API_KEY length=%s\n' "${#RENDER_API_KEY}"
   ```

2. **Local pre-check first** (free, no deploy) — this must pass before spending any
   Render build minutes:

   ```sh
   bash scripts/render-smoke.sh
   # expect: "PASS: node wrapper is render/smoke clean" and exit 0
   ```

3. **Deploy the test service from the repo** using the Render API. Example with curl
   (key read from env, never inlined):

   ```sh
   curl -sS -X POST "https://api.render.com/v1/services" \
     -H "Authorization: Bearer ${RENDER_API_KEY}" \
     -H "Content-Type: application/json" \
     -d '{
           "type": "web",
           "name": "wearless-node-test",
           "runtime": "docker",
           "plan": "free",
           "repo": "<owner-repo-url>",
           "branch": "main",
           "autoDeploy": "no",
           "serviceDetails": {
             "env": "docker",
             "dockerfilePath": "render-test/Dockerfile",
             "dockerContext": ".",
             "healthCheckPath": "/health"
           }
         }'
   ```

   `autoDeploy: "no"` keeps this a manual, test-only deploy.

4. **Watch the deploy / read health** once the service URL is up:

   ```sh
   curl -sS "https://<service>.onrender.com/health"
   # expect body to include: smoke_exit=0  and the PASS lines from render-smoke.sh
   ```

5. **Tear down** the test service after verifying (free plan, but keep it clean):

   ```sh
   curl -sS -X DELETE "https://api.render.com/v1/services/<service-id>" \
     -H "Authorization: Bearer ${RENDER_API_KEY}"
   ```

6. **Unset the key** in the shell after the run.

---

## What to paste back as evidence

- The exact `render-smoke.sh` local output (already captured in `qa/BUILD-REPORT.md`).
- The `/health` body showing `smoke_exit=0` (only after a real deploy).
- The deploy URL and timestamp.

Until a real deploy is run with the owner's key, the Render-side health result is
**NOT CAPTURED**. The local smoke run (the same script the container runs on boot)
**is** captured and passing — see `qa/BUILD-REPORT.md`.