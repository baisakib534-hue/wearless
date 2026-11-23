#!/bin/sh
set -eu

LOG_PATH=${WEARLESS_STATS_LOG:-/var/log/wearless/node-stats.log}
CONTROL=${WEARLESS_CONTROL:-/usr/local/sbin/wearless-control}

fail() {
    printf '%s\n' "$*" >&2
    exit 1
}

[ "$(id -u)" -eq 0 ] || fail "Run as root so the local log stays root-only"
[ -x "$CONTROL" ] || fail "Control wrapper is missing: $CONTROL"

timestamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)

stats_json=$( "$CONTROL" stats 2>/dev/null || printf '%s' 'UNAVAILABLE')
status_json=$( "$CONTROL" status 2>/dev/null || printf '%s' 'UNAVAILABLE')
stats_json=$(printf '%s' "$stats_json" | tr '\n' ' ')
status_json=$(printf '%s' "$status_json" | tr '\n' ' ')

sessions=NOT_MEASURED
per_user_bytes=NOT_MEASURED
per_path_rtt_loss=NOT_MEASURED
if command -v jq >/dev/null 2>&1; then
    sessions=$(printf '%s' "$status_json" | jq -r '
        .sessions //
        .session_count //
        .client_count //
        ((.clients // []) | length) //
        "NOT_MEASURED"
    ' 2>/dev/null || printf '%s' NOT_MEASURED)
    extracted=$(printf '%s' "$stats_json" | jq -c '
        .users // .per_user // .stats // empty
    ' 2>/dev/null || true)
    [ -n "$extracted" ] && per_user_bytes=$extracted
    extracted=$(printf '%s' "$status_json" | jq -c '
        .clients // .paths // .status // empty
    ' 2>/dev/null || true)
    [ -n "$extracted" ] && per_path_rtt_loss=$extracted
fi

main_pid=$(systemctl show wearless-node.service --property=MainPID --value 2>/dev/null || printf '%s' 0)
node_rss_kb=NOT_MEASURED
if [ "$main_pid" != "0" ] && [ -r "/proc/$main_pid/status" ]; then
    rss=$(awk '/^VmRSS:/ { print $2; exit }' "/proc/$main_pid/status")
    [ -n "$rss" ] && node_rss_kb=$rss
fi

install -d -m 0700 -o root -g root "$(dirname -- "$LOG_PATH")"
touch "$LOG_PATH"
chmod 0600 "$LOG_PATH"
printf '%s\n' \
    "timestamp=$timestamp sessions=$sessions per_user_bytes=$per_user_bytes per_path_rtt_loss=$per_path_rtt_loss node_rss_kb=$node_rss_kb stats_json=$stats_json status_json=$status_json" \
    >>"$LOG_PATH"

