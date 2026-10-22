#!/usr/bin/env bash
# Usage: tools/disable.sh pkg|list.txt [...]      e.g. tools/disable.sh tools/lists/tcl-safe.txt
# Disables for user 0 (reversible, nothing is uninstalled), one pm call per package, and logs each one to
# state/disabled.log so restore.sh can bring it back. A list file is one package per line (# comments).
# Refuses anything in tools/lists/keep.txt (features people use, or the TV needs them).
. "$(dirname "$0")/env.sh"
mkdir -p "$ROOT/state"
pkgs=()
for a in "$@"; do
  if [ -f "$a" ]; then pkgs+=($(grep -vE '^\s*(#|$)' "$a" | awk '{print $1}')); else pkgs+=("$a"); fi
done
blocked=$(printf '%s\n' "${pkgs[@]}" | grep -Fxf <(grep -vE '^\s*(#|$)' "$ROOT/tools/lists/keep.txt" | awk '{print $1}'))
if [ -n "$blocked" ]; then
  echo "Refusing: these are in tools/lists/keep.txt (features people use, or the TV needs them):"
  printf '  %s\n' $blocked
  exit 1
fi
for p in "${pkgs[@]}"; do
  out=$(adb shell pm disable-user --user 0 "$p" 2>&1 | tr -d '\r')
  echo "$p: $out"
  case "$out" in *"disabled-user"*) grep -qxF "$p" "$ROOT/state/disabled.log" 2>/dev/null || echo "$p" >> "$ROOT/state/disabled.log";; esac
done
