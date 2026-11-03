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
adb shell cmd package set-home-activity "$STOCK_HOME"
adb shell settings put secure screensaver_components "$STOCK_DREAM"
adb shell settings delete secure enabled_accessibility_services
# Afnon's remote buttons, handed to TCL's key remapping (TclKeys.java); harmless where they were never set. Only
# Afnon's entries (empty, or carrying its KEY_TARGET_MISSING mark) go: one Afnon replaced is put back, others stay.
# Keys go before the list: TCL crashes if a key changes while its code is out of the list.
keep=""
for code in $(adb shell settings get global CustomKeyList | tr -d '\r[]"' | tr ',' ' '); do
  value=$(adb shell settings get global "CustomKey_$code" | tr -d '\r')
  saved=$(adb shell settings get global "AfnonSavedKey_$code" | tr -d '\r')
  adb shell settings delete global "AfnonSavedKey_$code" >/dev/null
  case "$value" in
    ""|*tv.afnon.home.KEY_TARGET_MISSING*)
      if [ "$saved" != null ]; then
        adb shell "settings put global CustomKey_$code '$saved'" >/dev/null
        keep="$keep,\"$code\""
      else
        adb shell settings delete global "CustomKey_$code" >/dev/null
      fi ;;
    *) keep="$keep,\"$code\"" ;;
  esac
done
if [ -n "$keep" ]; then
  adb shell "settings put global CustomKeyList '[${keep#,}]'" >/dev/null
else
  adb shell settings delete global CustomKeyList >/dev/null
fi
# The radio turns the sleep timer off while it plays and saves the old value; put it back if it's still off.
if [ "$(adb shell settings get secure sleep_timeout | tr -d '\r')" = "-1" ]; then
  saved=$(adb shell "run-as tv.afnon.home cat shared_prefs/afnon.xml" 2>/dev/null | sed -n 's/.*name="sleep_saved" value="\([0-9]*\)".*/\1/p')
  adb shell settings put secure sleep_timeout "${saved:-1800000}"
fi
echo "tv.afnon.home: $(adb uninstall tv.afnon.home 2>&1 | tr -d '\r')"
adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME >/dev/null
echo "Done. Reboot the TV to finish."
