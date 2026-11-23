#!/usr/bin/env bash
# TEST-ONLY health server for the Render smoke deploy.
# Serves /health with the real output of the node wrapper smoke checks.
# It does not run mqvpn and does not pretend to be the production node.
set -u

PORT="${PORT:-10000}"
OUT="${TMPDIR:-/tmp}/wearless-smoke.txt"

handle() {
  local smoke_exit=0
  bash /opt/wearless/scripts/render-smoke.sh >"$OUT" 2>&1 || smoke_exit=$?
  printf 'HTTP/1.1 200 OK\r\n'
  printf 'Content-Type: text/plain; charset=utf-8\r\n'
  printf 'Connection: close\r\n'
  printf '\r\n'
  printf 'wearless-node-test\n'
  printf 'mode=TEST_ONLY (native systemd node is the real target)\n'
  printf 'smoke_exit=%s\n' "$smoke_exit"
  cat "$OUT"
}

export -f handle
export OUT

exec socat "TCP-LISTEN:${PORT},reuseaddr,fork" SYSTEM:handle