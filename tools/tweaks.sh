#!/usr/bin/env bash
# Speed tweaks that are safe and reversible. Nothing is disabled or uninstalled.
#   tools/tweaks.sh apply     animations at half length; the Play Store kept from running in the background (it
#                             stays installed, opens and updates normally; on one TCL set its idle CPU fell from
#                             31% to 6%); closed apps frozen while cached (no CPU in the background); caches trimmed
#   tools/tweaks.sh restore   back to Android's defaults
# From UrTV's optimize/tweaks.sh (github.com/tshi-dev/UrTV, MIT), measured there on TCL sets.
case "$1" in
  apply)   v=0.5; bucket=restricted; bg=deny ;;
  restore) v=1.0; bucket=active;     bg=allow ;;
  *) echo "usage: tools/tweaks.sh apply|restore"; exit 1 ;;
esac
. "$(dirname "$0")/env.sh"
for k in window_animation_scale transition_animation_scale animator_duration_scale; do
  adb shell settings put global "$k" "$v"
done
adb shell am set-standby-bucket com.android.vending "$bucket"
adb shell cmd appops set com.android.vending RUN_IN_BACKGROUND "$bg"
adb shell cmd appops set com.android.vending RUN_ANY_IN_BACKGROUND "$bg"
if [ "$1" = apply ]; then
  adb shell settings put global cached_apps_freezer enabled   # Android 11+; a harmless unused key before that
  adb shell pm trim-caches 999G
  freezer=on
else
  adb shell settings put global cached_apps_freezer device_default
  freezer=default
fi
echo "Animation scale $v, Play Store in the background: $bg, cached-app freezer: $freezer"
