#!/usr/bin/env bash
# Builds Afnon, installs it over the running copy and brings it straight back as the home screen. Also sets
# what Afnon needs from the system: HOME, the remote buttons service, the screensaver, TCL's auto-start
# grants, two permissions the radio uses and location for the Wi-Fi name.
set -e
. "$(dirname "$0")/env.sh"
[ -n "$(ls "$ROOT/app/src/main/assets/adhan/"*.mp3 2>/dev/null)" ] || bash "$ROOT/tools/fetch-adhans.sh"
(cd "$ROOT" && ./gradlew assembleDebug -q)
adb install -r "$ROOT/app/build/outputs/apk/debug/app-debug.apk" | tail -1
adb shell settings put secure enabled_accessibility_services tv.afnon.home/.RemoteKeys
adb shell settings put secure accessibility_enabled 1
adb shell settings put secure screensaver_components tv.afnon.home/.AfnonDream
# TCL blocks auto-start for sideloaded apps; harmless on other brands.
adb shell appops set tv.afnon.home APP_AUTO_START allow 2>/dev/null || true
adb shell appops set tv.afnon.home APP_ASSOC_START allow 2>/dev/null || true
# Radio: the level bars (RECORD_AUDIO) and the TV's sleep timer while listening (WRITE_SECURE_SETTINGS).
adb shell pm grant tv.afnon.home android.permission.RECORD_AUDIO
adb shell pm grant tv.afnon.home android.permission.WRITE_SECURE_SETTINGS
# Home's Wi-Fi name: Android 9+ hides the network name from apps without location.
adb shell pm grant tv.afnon.home android.permission.ACCESS_FINE_LOCATION
# Last, once Afnon is installed and set up: make it Home (behind a safety switch where the firmware pins its own).
bash "$ROOT/tools/set-home.sh"
echo "Afnon installed and on screen."
