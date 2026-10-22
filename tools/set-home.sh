#!/usr/bin/env bash
# Makes Afnon the home screen, safely. Run after Afnon is installed (tools/install.sh calls it).
#
# Most TVs take the usual way (the HOME role and set-home-activity) and nothing else is touched. TCL Google TV
# ignores both: on every Home press and boot the firmware starts the stock launcher by name, so Afnon only stays
# Home with the stock home (launcherx) AND Google's setup app (setupwraith) disabled. Disabling them while Afnon
# is broken leaves the TV with no home at all (black screen, reboot loop, Wi-Fi gone), so that path:
#   1. arms tools/deadman.sh on the TV: unless Afnon is confirmed within 5 minutes it puts the stock home back
#   2. disables the setup app, then the stock home (this order avoided the black screen)
#   3. presses Home and checks Afnon is in front (3 tries), and only then disarms the switch
# Disabled packages go to state/disabled.log, so tools/restore.sh brings them back.
# Adapted from UrTV's optimize/install.sh launcher (github.com/tshi-dev/UrTV, MIT).
. "$(dirname "$0")/env.sh"
ME=tv.afnon.home/.MainActivity
OK=/data/local/tmp/afnon_home_ok

sh_() { adb shell "$@" | tr -d '\r'; }
enabled() { sh_ pm list packages -e "$1" | grep -qx "package:$1"; }
front() { sh_ "dumpsys activity activities 2>/dev/null | grep -m1 -E 'topResumedActivity|mResumedActivity' 2>/dev/null"; }
log_disabled() {
  mkdir -p "$ROOT/state"
  grep -qxF "$1" "$ROOT/state/disabled.log" 2>/dev/null || echo "$1" >> "$ROOT/state/disabled.log"
}
afnon_in_front() {   # presses Home up to 3 times, a few seconds apart (a slow TV)
  local _
  for _ in 1 2 3; do
    sh_ input keyevent KEYCODE_HOME >/dev/null
    sleep 4
    front | grep -q tv.afnon.home && return 0
  done
  return 1
}

sh_ cmd role add-role-holder --user 0 android.app.role.HOME tv.afnon.home >/dev/null 2>&1 || true
sh_ cmd package set-home-activity --user 0 "$ME" >/dev/null
if afnon_in_front; then echo "Afnon is Home."; exit 0; fi

# Home still opens something else: the firmware pins its own launcher. Which one does this TV have?
if enabled com.google.android.apps.tv.launcherx; then
  STOCK=com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity
elif enabled com.google.android.tvlauncher; then
  STOCK=com.google.android.tvlauncher/.MainActivity
else
  echo "!! Home opened: $(front). Not a stock Google home this script knows: nothing was disabled."
  exit 1
fi
echo "The firmware keeps its own home (${STOCK%%/*}): switching with the safety switch armed."

sh_ "rm -f $OK"
tr -d '\r' < "$ROOT/tools/deadman.sh" | adb shell "cat > /data/local/tmp/afnon_deadman.sh"
sh_ "nohup sh /data/local/tmp/afnon_deadman.sh $STOCK 300 > /dev/null 2>&1 &"
echo "Safety switch armed: the stock home comes back by itself in 5 minutes unless Afnon works."

for p in com.google.android.tungsten.setupwraith "${STOCK%%/*}"; do
  enabled "$p" || continue
  sh_ pm disable-user --user 0 "$p" | grep -q disabled-user && log_disabled "$p"
done
sh_ cmd package set-home-activity --user 0 "$ME" >/dev/null
sh_ am force-stop com.android.tv.settings

if ! afnon_in_front; then
  echo "!! Home did not open Afnon. Leave the TV alone: the stock home returns within 5 minutes."
  exit 1
fi
sh_ "touch $OK"
# the switch may have fired already if adb was very slow: then the stock home is back
if ! sh_ cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN \
     | grep -q tv.afnon.home; then
  echo "!! The safety switch put the stock home back (adb was too slow). Nothing is broken: run this again."
  exit 1
fi

# Only now: the stock home's helpers are dead weight (restore.sh brings them back).
grep -vE '^\s*(#|$)' "$ROOT/tools/lists/home-replaced.txt" | awk '{print $1}' | while read -r p; do
  enabled "$p" || continue
  sh_ pm disable-user --user 0 "$p" | grep -q disabled-user && log_disabled "$p"
done
echo "Afnon is Home. Press Home from inside an app to double-check. Undo: tools/restore.sh"
