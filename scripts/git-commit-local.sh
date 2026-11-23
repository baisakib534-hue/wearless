#!/usr/bin/env bash

set -eu

if [ "$#" -ne 1 ]; then
    printf 'Usage: bash scripts/git-commit-local.sh "<commit message>"\n' >&2
    exit 2
fi

commit_message=$1
script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
root=$(CDPATH= cd -- "$script_dir/.." && pwd)

outside_scripts=$(git -C "$root" diff --cached --name-only | sed '/^scripts\//d')
if [ -n "$outside_scripts" ]; then
    printf 'FAIL: staged paths outside scripts/ exist:\n%s\n' "$outside_scripts" >&2
    exit 1
fi

git -C "$root" add -- scripts
if git -C "$root" diff --cached --quiet -- scripts; then
    printf 'FAIL: no staged changes under scripts/\n' >&2
    exit 1
fi

git -C "$root" commit --only -m "$commit_message" -- scripts
