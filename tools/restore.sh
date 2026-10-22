#!/usr/bin/env bash
# Puts the TV back to stock: re-enables every package listed in state/disabled.log, gives HOME, the screensaver
# and the accessibility slot back, puts the sleep timer back if the radio left it off, and uninstalls Afnon.
# Set STOCK_HOME / STOCK_DREAM if your TV's stock launcher or screensaver isn't Google TV's.
. "$(dirname "$0")/env.sh"
STOCK_HOME="${STOCK_HOME:-com.google.android.apps.tv.launcherx/.home.HomeActivity}"
STOCK_DREAM="${STOCK_DREAM:-com.google.android.apps.tv.dreamx/.service.Backdrop}"
LOG="$ROOT/state/disabled.log"
if [ -f "$LOG" ]; then
  while read -r p; do
    [ -z "$p" ] || [ "$p" = tv.afnon.home ] && continue
    echo "$p: $(adb shell pm enable --user 0 "$p" 2>&1 | tr -d '\r')"
  done < "$LOG"
fi
# The stock home, Google's setup app and the home's helpers, whether or not the log has them (set-home.sh, or by
# hand). A running safety switch is stopped first: this restores by hand.
adb shell "touch /data/local/tmp/afnon_home_ok"
for p in com.google.android.tungsten.setupwraith com.google.android.apps.tv.launcherx com.google.android.tvlauncher \
         "${STOCK_HOME%%/*}" $(grep -vE '^\s*(#|$)' "$ROOT/tools/lists/home-replaced.txt" | awk '{print $1}'); do
  adb shell pm enable --user 0 "$p" >/dev/null 2>&1
done
# Google TV's home can come back without its notification access (seen by UrTV); give it back.
adb shell cmd notification allow_listener \
  com.google.android.apps.tv.launcherx/.coreservices.notificationlistener.TvNotificationListenerService 2>/dev/null
adb shell cmd role add-role-holder --user 0 android.app.role.HOME "${STOCK_HOME%%/*}" 2>/dev/null
adb shell cmd package set-home-activity "$STOCK_HOME"
adb shell settings put secure screensaver_components "$STOCK_DREAM"
adb shell settings delete secure enabled_accessibility_services
# Afnon's remote buttons, handed to TCL's key remapping (TclKeys.java); harmless where they were never set.
for code in $(adb shell settings get global CustomKeyList | tr -d '[]"' | tr ',' ' '); do
  adb shell settings delete global "CustomKey_$code" >/dev/null
done
adb shell settings delete global CustomKeyList >/dev/null
# The radio turns the sleep timer off while it plays and saves the old value; put it back if it's still off.
if [ "$(adb shell settings get secure sleep_timeout | tr -d '\r')" = "-1" ]; then
  saved=$(adb shell "run-as tv.afnon.home cat shared_prefs/afnon.xml" 2>/dev/null | sed -n 's/.*name="sleep_saved" value="\([0-9]*\)".*/\1/p')
  adb shell settings put secure sleep_timeout "${saved:-1800000}"
fi
echo "tv.afnon.home: $(adb uninstall tv.afnon.home 2>&1 | tr -d '\r')"
adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME >/dev/null
echo "Done. Reboot the TV to finish."
