#!/bin/sh
set -eu

CONTROL_HOST=127.0.0.1
CONTROL_PORT=9090

usage() {
    cat <<'EOF'
Wearless local mqvpn Control API wrapper

Usage:
  control.sh list
  control.sh add NAME
  control.sh remove NAME
  control.sh stats
  control.sh status

'add' reads the user key from standard input; it is never accepted as an
argument. The API is fixed to 127.0.0.1:9090.

WARNING: the mqvpn Control API has NO authentication. It must remain bound
to localhost and must never be exposed remotely.
EOF
}

fail() {
    printf '%s\n' "$*" >&2
    exit 1
}

json_escape() {
    printf '%s' "$1" | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g'
}

validate_name() {
    name=$1
    [ -n "$name" ] || fail "User name is required"
    [ "$(printf '%s' "$name" | wc -c)" -le 63 ] || fail "User name exceeds 63 bytes"
    printf '%s' "$name" | LC_ALL=C grep -q '[[:cntrl:][:space:]]' &&
        fail "User name must not contain whitespace or control characters"
}

send_command() {
    command -v nc >/dev/null 2>&1 || fail "nc is required"
    printf '%s\n' "$1" | nc -w 5 "$CONTROL_HOST" "$CONTROL_PORT"
}

check_response() {
    response=$1
    if printf '%s' "$response" | grep -q '"ok"[[:space:]]*:[[:space:]]*false'; then
        fail "mqvpn Control API rejected the command"
    fi
}

case "${1:-}" in
    list)
        [ "$#" -eq 1 ] || fail "list takes no arguments"
        response=$(send_command '{"cmd":"list_users"}')
        check_response "$response"
        printf '%s\n' "$response"
        ;;
    add)
        [ "$#" -eq 2 ] || fail "add requires exactly one user name"
        validate_name "$2"
        printf '%s' "User key: " >&2
        IFS= read -r user_key
        [ -n "$user_key" ] || fail "User key is required"
        printf '%s' "$user_key" | LC_ALL=C grep -q '[[:cntrl:]]' &&
            fail "User key must not contain control characters"
        escaped_name=$(json_escape "$2")
        escaped_key=$(json_escape "$user_key")
        response=$(send_command "{\"cmd\":\"add_user\",\"name\":\"$escaped_name\",\"key\":\"$escaped_key\"}")
        check_response "$response"
        printf '%s\n' "$response"
        ;;
    remove)
        [ "$#" -eq 2 ] || fail "remove requires exactly one user name"
        validate_name "$2"
        escaped_name=$(json_escape "$2")
        response=$(send_command "{\"cmd\":\"remove_user\",\"name\":\"$escaped_name\"}")
        check_response "$response"
        printf '%s\n' "$response"
        ;;
    stats)
        [ "$#" -eq 1 ] || fail "stats takes no arguments"
        response=$(send_command '{"cmd":"get_stats"}')
        check_response "$response"
        printf '%s\n' "$response"
        ;;
    status)
        [ "$#" -eq 1 ] || fail "status takes no arguments"
        response=$(send_command '{"cmd":"get_status"}')
        check_response "$response"
        printf '%s\n' "$response"
        ;;
    -h|--help|help)
        usage
        ;;
    *)
        usage >&2
        exit 2
        ;;
esac
