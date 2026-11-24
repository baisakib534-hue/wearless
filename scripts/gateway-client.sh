#!/usr/bin/env bash
set -eu

usage() {
    cat <<'EOF'
Usage:
  WEARLESS_NODE_KEY=<node-key> gateway-client.sh <service-host-or-url> [mqvpn client options...]

Examples:
  WEARLESS_NODE_KEY='...' scripts/gateway-client.sh wearless.example.com
  WEARLESS_NODE_KEY='...' scripts/gateway-client.sh wss://wearless.example.com --dns 1.1.1.1

The script starts a restarting wstunnel client that exposes the remote mqvpn
server on 127.0.0.1:4433, then runs the mqvpn client against that address.
EOF
}

if [ "${1:-}" = "-h" ] || [ "${1:-}" = "--help" ]; then
    usage
    exit 0
fi

if [ "$#" -lt 1 ]; then
    usage >&2
    exit 2
fi

service=$1
shift

: "${WEARLESS_NODE_KEY:?WEARLESS_NODE_KEY is required}"

command -v wstunnel >/dev/null 2>&1 || { printf '%s\n' "wstunnel is required" >&2; exit 1; }
command -v mqvpn >/dev/null 2>&1 || { printf '%s\n' "mqvpn is required" >&2; exit 1; }

case "$service" in
    ws://*|wss://*) service_url=$service ;;
    *) service_url="wss://${service}" ;;
esac

if [ "$(id -u)" -ne 0 ]; then
    printf '%s\n' "gateway-client.sh must run as root (mqvpn needs /dev/net/tun)" >&2
    exit 1
fi

cleanup() {
    if [ -n "${wstunnel_pid:-}" ]; then
        pkill -TERM -P "$wstunnel_pid" 2>/dev/null || true
        kill "$wstunnel_pid" 2>/dev/null || true
        wait "$wstunnel_pid" 2>/dev/null || true
    fi
}
trap cleanup EXIT INT TERM

(
    delay=1
    while true; do
        wstunnel client \
            -L "udp://127.0.0.1:4433:127.0.0.1:4433?timeout_sec=0" \
            "$service_url" || true
        sleep "$delay"
        if [ "$delay" -lt 30 ]; then
            delay=$((delay * 2))
        fi
    done
) &
wstunnel_pid=$!

sleep 1

delay=1
while true; do
    mqvpn --mode client \
        --server 127.0.0.1:4433 \
        --auth-key "$WEARLESS_NODE_KEY" \
        --insecure \
        "$@" || true
    sleep "$delay"
    if [ "$delay" -lt 30 ]; then
        delay=$((delay * 2))
    fi
done
