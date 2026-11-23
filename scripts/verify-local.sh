#!/usr/bin/env bash

set -u

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
root=$(CDPATH= cd -- "$script_dir/.." && pwd)
overall_status=0

print_command() {
    printf 'RUN:'
    printf ' %q' "$@"
    printf '\n'
}

run_step() {
    label="$1"
    shift

    printf '\n[%s]\n' "$label"
    print_command "$@"
    "$@"
    status=$?
    if [ "$status" -eq 0 ]; then
        printf 'PASS: %s\n' "$label"
    else
        printf 'FAIL: %s (exit %s)\n' "$label" "$status"
        overall_status=1
    fi
}

for node_script in "$root"/node/*.sh; do
    if [ ! -f "$node_script" ]; then
        continue
    fi
    run_step "bash -n $(basename "$node_script")" bash -n "$node_script"
done

gradle_bat="$root/app/gradlew.bat"
if [ -f "$gradle_bat" ]; then
    gradle_dir="$root/app"
    cd "$gradle_dir" || exit 1
    run_step ":app:assembleDebug" "$gradle_bat" ":app:assembleDebug"
    run_step ":bonding:test" "$gradle_bat" ":bonding:test"
    cd "$root" || exit 1
else
    printf 'SKIP: app/gradlew.bat not found\n'
fi

if [ "$overall_status" -eq 0 ]; then
    printf '\nPASS: all local verification steps passed\n'
else
    printf '\nFAIL: one or more local verification steps failed\n'
fi

exit "$overall_status"
