#!/system/bin/sh
# Safety switch, runs ON the TV. tools/set-home.sh starts it before it disables the stock home.
# If /data/local/tmp/afnon_home_ok does not appear within LIMIT seconds (Afnon did not come up as Home),
# it puts the stock home back on its own: re-enable it and the setup app, make it home, press Home.
# Without this, a TCL set left with no home shows a black screen and can reboot-loop with Wi-Fi down.
# Usage (on the TV): nohup sh /data/local/tmp/afnon_deadman.sh <stock home component> [seconds] &
# From UrTV (github.com/tshi-dev/UrTV, MIT).

STOCK="${1:-com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity}"
LIMIT="${2:-300}"
i=0
while [ $i -lt "$LIMIT" ]; do
  [ -f /data/local/tmp/afnon_home_ok ] && exit 0
  sleep 1
  i=$((i + 1))
done

pm enable "${STOCK%%/*}"
pm enable com.google.android.tungsten.setupwraith
cmd role add-role-holder --user 0 android.app.role.HOME "${STOCK%%/*}"
cmd package set-home-activity --user 0 "$STOCK"
am start -n "$STOCK"
input keyevent KEYCODE_HOME
