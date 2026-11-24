#!/usr/bin/env bash
set -u

log() {
    printf '%s\n' "$*" >&2
}

fail() {
    printf 'gateway-entrypoint: %s\n' "$*" >&2
    exit 1
}

cleanup() {
    for pid in "${pids[@]:-}"; do
        kill -TERM -- -"$pid" 2>/dev/null || kill -TERM "$pid" 2>/dev/null || true
    done
    wait 2>/dev/null || true
}
trap cleanup TERM INT

: "${PORT:?PORT is required}"
: "${WEARLESS_NODE_KEY:?WEARLESS_NODE_KEY is required}"

case "$PORT" in
    ''|*[!0-9]*) fail "PORT must be a positive integer" ;;
esac
if [ "$PORT" -lt 1 ] || [ "$PORT" -gt 65535 ]; then
    fail "PORT must be between 1 and 65535"
fi

case "$WEARLESS_NODE_KEY" in
    *[[:space:]]*|*[[:cntrl:]]*) fail "WEARLESS_NODE_KEY must not contain whitespace or control characters" ;;
esac

WSTUNNEL_PORT="${WSTUNNEL_PORT:-18080}"
case "$WSTUNNEL_PORT" in
    ''|*[!0-9]*) fail "WSTUNNEL_PORT must be a positive integer" ;;
esac
if [ "$WSTUNNEL_PORT" -lt 1 ] || [ "$WSTUNNEL_PORT" -gt 65535 ]; then
    fail "WSTUNNEL_PORT must be between 1 and 65535"
fi
if [ "$WSTUNNEL_PORT" = "$PORT" ]; then
    fail "WSTUNNEL_PORT must differ from PORT"
fi

RUN_DIR=/run/wearless
mkdir -p "$RUN_DIR"
chmod 700 "$RUN_DIR"

CERT_FILE="$RUN_DIR/server.crt"
KEY_FILE="$RUN_DIR/server.key"
MQVPN_CONF="$RUN_DIR/mqvpn.conf"
NGINX_CONF=/etc/nginx/conf.d/gateway.conf

if [ ! -s "$CERT_FILE" ] || [ ! -s "$KEY_FILE" ]; then
    log "generating ephemeral self-signed mqvpn certificate"
    openssl req -x509 -newkey ec -pkeyopt ec_paramgen_curve:prime256v1 \
        -keyout "$KEY_FILE" -out "$CERT_FILE" \
        -days 365 -nodes -subj "/CN=mqvpn" >/dev/null 2>&1
    chmod 600 "$KEY_FILE"
    chmod 644 "$CERT_FILE"
fi

umask 077
cat >"$MQVPN_CONF" <<EOF
[Interface]
Listen = 127.0.0.1:4433
Subnet = 10.0.0.0/24
TunName = mqvpn0

[TLS]
Cert = $CERT_FILE
Key = $KEY_FILE

[Auth]
User = wearless-owner:$WEARLESS_NODE_KEY
EOF
chmod 600 "$MQVPN_CONF"

sed -e "s/__PORT__/${PORT}/g" -e "s/__WSTUNNEL_PORT__/${WSTUNNEL_PORT}/g" \
    /etc/nginx/templates/gateway.conf.template >"$NGINX_CONF"
chmod 644 "$NGINX_CONF"

if ! nginx -t >/tmp/nginx-test.log 2>&1; then
    cat /tmp/nginx-test.log >&2
    fail "nginx configuration test failed"
fi

if [ -x /opt/mqvpn/lib/mqvpn/mqvpn-server-nat.sh ]; then
    /opt/mqvpn/lib/mqvpn/mqvpn-server-nat.sh setup "$MQVPN_CONF" >&2 || \
        log "warning: mqvpn NAT setup failed; VPN forwarding may not work"
fi

supervise() {
    local name="$1"
    shift
    local delay=1
    while true; do
        log "starting ${name}"
        "$@" &
        local child=$!
        wait "$child"
        local rc=$?
        log "${name} exited with status ${rc}; restarting in ${delay}s"
        sleep "$delay"
        if [ "$delay" -lt 30 ]; then
            delay=$((delay * 2))
        fi
    done
}

pids=()

setsid supervise mqvpn \
    env LD_LIBRARY_PATH=/opt/mqvpn/lib \
    /opt/mqvpn/bin/mqvpn --config "$MQVPN_CONF" &
pids+=("$!")

setsid supervise wstunnel \
    /usr/local/bin/wstunnel server \
    --restrict-to 127.0.0.1:4433 \
    "ws://127.0.0.1:${WSTUNNEL_PORT}" &
pids+=("$!")

setsid supervise nginx \
    nginx -g 'daemon off;' &
pids+=("$!")

wait
