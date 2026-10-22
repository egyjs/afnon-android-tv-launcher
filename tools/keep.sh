#!/usr/bin/env bash
# Turns back on anything from tools/lists/keep.txt that is disabled: AirPlay, casting, mirroring, voice, the
# remote, inputs, tuner, firmware updates, Play Store, Google services. Our tools never disable these, but a TV
# set up before, by other tools, may have them off. Safe to run any time.
# The stock home and setup app are left to tools/set-home.sh. From UrTV (github.com/tshi-dev/UrTV, MIT).
. "$(dirname "$0")/env.sh"
want=$(grep -vE '^\s*(#|$)' "$ROOT/tools/lists/keep.txt" | awk '{print $1}' \
       | grep -vxE 'com.google.android.apps.tv.launcherx|com.google.android.tvlauncher|com.google.android.tungsten.setupwraith|com.tcl.keycustomfunctionservice')
off=$(adb shell pm list packages -d | tr -d '\r' | sed -n 's/^package://p' | grep -Fxf <(printf '%s\n' "$want"))
if [ -z "$off" ]; then echo "Protected features: all on."; exit 0; fi
echo "Turning back on (they must never be off):"
for p in $off; do
  echo "  $p: $(adb shell pm enable --user 0 "$p" 2>&1 | tr -d '\r')"
  [ -f "$ROOT/state/disabled.log" ] && sed -i "/^${p//./\.}\$/d" "$ROOT/state/disabled.log"
done
