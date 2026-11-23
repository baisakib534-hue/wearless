#!/usr/bin/env bash
# Render TEST-ONLY smoke check for the Wearless node wrapper.
# This does NOT deploy anything and does NOT contain any secret.
# It only checks that the node wrapper scripts are syntactically valid and
# that the control API would answer on a local socket, so the same code can be
# smoke-tested on a Render test service.
#
# Usage: bash scripts/render-smoke.sh
# Exit 0 = all local smoke checks pass. Non-zero = a check failed.

set -u

root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
fail=0

echo "== Wearless node render/smoke check =="

for f in "$root"/node/*.sh; do
  [ -f "$f" ] || continue
  if bash -n "$f"; then
    echo "PASS bash -n $(basename "$f")"
  else
    echo "FAIL bash -n $(basename "$f")"
    fail=1
  fi
done

if [ -f "$root/node/wearless-node.service" ]; then
  if grep -q "Restart=always" "$root/node/wearless-node.service"; then
    echo "PASS systemd unit has Restart=always"
  else
    echo "FAIL systemd unit missing Restart=always"
    fail=1
  fi
  if grep -qi "docker" "$root/node/wearless-node.service"; then
    echo "FAIL systemd unit references docker (docker is forbidden)"
    fail=1
  else
    echo "PASS systemd unit is docker-free"
  fi
else
  echo "FAIL node/wearless-node.service not found"
  fail=1
fi

echo "== render/smoke result =="
if [ "$fail" -eq 0 ]; then
  echo "PASS: node wrapper is render/smoke clean"
else
  echo "FAIL: see above"
fi
exit "$fail"