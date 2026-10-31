# Afnon (أفنون): notes for Claude

Afnon is an Android TV home screen (HOME launcher) built around the prayer day: next-prayer countdown in a mihrab
arch, offline prayer times, Qibla, the adhan at prayer time, reminders, Friday/Ramadan modes and Quran Radio Cairo.
Package `tv.afnon.home`. Public repo: github.com/egyjs/afnon-android-tv-launcher (MIT).

Read `README.md` (features, install, TCL notes) and `CONTRIBUTING.md` (the rules below in full) before larger work.
Releasing: `gh.md`. History: `CHANGELOG.md`. Design spec: `afnon-mihrab/HANDOFF.md` and `afnon-mihrab/design/`.
Machine- and TV-specific details (TV address, adb path) live in `CLAUDE.local.md`, which is not committed.

## Target device

TCL 55P7K, Google TV, Android 12, 1920×1080, ~1.8 GB RAM, weak GPU, D-pad remote only. Everything is checked on the
real TV over ADB, never just in the build.

## Hard rules

- Plain Java + Android Views. One dependency (adhan-java). No AndroidX, Kotlin or Compose.
- No blur, video or shadows; animate transform/opacity only. No new timers or background services: prayer checks
  share one 15 s timer in the accessibility service.
- No network access except the chosen radio station's stream (hosts listed in `Radio.STATIONS` and the README's
  Privacy section). No analytics, ads or feeds.
- Colours/sizes/timings from `afnon-mihrab/tokens.json` (generated into `Tokens.java`, `colors.xml` and the site's
  `tokens.css` by `cd site && npm run tokens`; never edit those by hand); every string in Arabic and English in
  `Text.java`. Arabic (RTL) is the
  default; check RTL and LTR. No letter-spacing on Arabic. Quranic text uses Amiri and must be exact (surah:ayah).
- D-pad focus is explicit per screen, with Left/Right swapped in RTL.
- Never commit `state/`, `local.properties`, APKs or the adhan MP3s (no licence; `tools/fetch-adhans.sh` downloads
  them). No personal data in screenshots or logs (Wi-Fi names, IPs, serials).

## Build, install, test

```bash
export TV=<ip>:5555                 # the tools refuse to run without it
bash tools/install.sh               # fetch adhans (first time), assembleDebug, install, set HOME, grants, a11y service
bash tools/shot.sh home             # screenshot -> state/shots/home.png (look at it)
adb -s $TV shell am start -n tv.afnon.home/.MainActivity --es test friday   # ramadan|now|reminder|call|banner|announce|radio
```

`tools/env.sh` finds adb itself (PATH, then `local.properties` sdk.dir, ANDROID_HOME, default SDK folders);
`ADB_BIN` overrides. Other tools: `disable.sh`/`enable.sh` (stock apps, logged to `state/disabled.log`),
`restore.sh` (back to stock and uninstall, then reboot).

Keys injected with `adb shell input keyevent` do not reach the accessibility service: test remote buttons with the
real remote, and ask the user to press them. `adb shell getevent -lt` and `logcat | grep handleKeyBefore` show what
a button sends and who handled it.

## Architecture

- `MainActivity` Home · `PrayerDayActivity` today's prayers + Qibla · `AllAppsActivity` · `SettingsActivity` ·
  `NowActivity` the call · `AfnonDream` screensaver · `ScreenOffActivity` radio with the panel off (TCL Audio Only).
- `PrayerDay`, `Prefs` (cities and methods), `Reminders`, `Adhans`, `Radio`/`RadioBars`, `Apps` (launching).
- `RemoteKeys` is the accessibility service. It is always on, so it also hosts `PrayerWatch` (reminders, banner,
  the call). Turning it off stops the call to prayer.
- Android removes a force-stopped app's accessibility service from `enabled_accessibility_services` (TV cleaners
  force-stop apps, and so does `am force-stop` in testing). `RemoteKeys.ensureOn`, called from Home's `onResume`,
  lists it again unless pref `remote_on` is false (set in `onUnbind` when the service is no longer listed, i.e.
  turned off in Settings or by `restore.sh`). After a force-stop in tests, open Home before checking the service.
- `Apps.installed` skips phone-only (`CATEGORY_LAUNCHER` without a TV entry) system apps that were never updated,
  unless Afnon names them or they are favourites.

### Remote buttons and the volume-hold bug (fixed 2026-10-21)

With accessibility key filtering on (`FLAG_REQUEST_FILTER_KEY_EVENTS`), Android re-injects every key and injected
keys never auto-repeat, so holding Volume moved it one step. On this firmware the volume keys never even reach the
service. So filtering is off on TCL:

- `TclKeys` hands the buttons to TCL's own remapping in system_server (`TclCustomShortCutKeyManager` in
  `/system/framework/tcl-service.jar`): Settings.Global `CustomKeyList` (JSON array of keycodes) and
  `CustomKey_<code>` (`{"keycode", "behavior":[{"keyaction":1 press|2 long|3 release, "type":1 activity|2 service|
  3 broadcast, "intent":"<intent: URI>", "appnotinstall":{...}}]}`). It runs before apps see the key and swallows it.
- `RemoteKeys.onServiceConnected` rewrites the mappings and turns key filtering on only where TCL remapping is
  unavailable (no feature `com.tcl.software.tinput_shortcut_config`, or no WRITE_SECURE_SETTINGS grant). The
  button table is pref `keys` (else `APP_KEYS`) plus `targets()` in `RemoteKeys.java`.
- **TCL's code has no error handling; mistakes restart the TV's UI.** Changing `CustomKey_<n>` while n is not in
  `CustomKeyList` crashes it (this happened once). So: write the list first, keys a second later, never drop codes
  from the list (empty them). Every intent must name its package (TCL checks it is installed and otherwise runs
  `appnotinstall`, which must be a broadcast). Removing: delete keys before the list (`restore.sh` does), then reboot.
- After any change here: `logcat -b crash -d | grep "SYSTEM PROCESS"` must stay empty, and hold-volume must work.

## Phone installer (`installer/`)

A separate phone app (plain Java, libadb-android, Conscrypt) that installs and sets up Afnon over ADB. It keeps no
copies of what changes: the APK and `tools/setup-steps.txt` come from the latest release's tag (so a new setup step
goes in that file, which `tools/install.sh` also runs, with a `?|` check line that exits 0 once it is in place on
the TV; both tools skip a step whose check passes and require it to pass after the command), the cleanup list from `tools/clean-catalog.tsv` on main, and
cities, apps, icons and buttons from the TV through `SetupProvider` (`content query/read/insert`, guarded by DUMP).
That provider is a contract: add to it, bump `SetupProvider.API`, never break what's there. Remote buttons are pref
`keys` (`KEY=package`, KEY a keycode name or number), defaulting to `RemoteKeys.APP_KEYS`.
`--es screen <name>` opens any installer screen with sample data (debug builds); CI screenshots them.

## TCL specifics

Settings button sends `KEYCODE_NOTIFICATION` (83); Web is 4159; Netflix is 4062 (mapped to Shahid); YouTube 4065
and 190. Audio Only via `com.tcl.settings.SHOW_WINDOW`. `APP_AUTO_START` appop keeps the service across reboots.
`screen_brightness` does nothing (TCL drives the backlight). Each of these fails quietly on other brands.

## Other brands (CVTE)

`FirmwareKeys` detects CVTE boards (`android.intent.action.HOTKEYSERVICE`): no key filtering there, and Home opens
CVTE's source menu for Source. `Redirects` (pref `redirects`, `from=to` packages) repoints buttons the firmware
hard-wires to an app. Tested on one Super General (Android 9) only; any change here must leave TCL untouched.

## Other brands (Xiaomi)

One Xiaomi MiTV-MOOQ0 (Android TV 10, MediaTek, 1.8 GB, low-RAM mode), debloated with cleanyourtv's
`examples/xiaomi-mitv-mooq0` list. Stock launcher `com.google.android.tvlauncher` (no launcherx, so setup leaves
Google's setup app alone; on that TV it owns the saved Wi-Fi network and must stay enabled). Input sends
`KEYCODE_MEDIA_RECORD` (130), not mapped yet. Not checked there: holding Volume (the service filters keys on
non-TCL, non-CVTE firmware), Audio Only, the remote's buttons. Live TV/HDMI is `com.mediatek.wwtv.tvcenter`.

## Web preview (`site/`)

Astro site on GitHub Pages (`.github/workflows/site.yml`), one page per screen and mode in Arabic (root) and English
(`/en/`). `scripts/extract.mjs` reads `Text.java`, `Prefs`, `Radio.STATIONS`, the fonts and `docs/screenshots` into
gitignored files; strings with logic are ported by hand in `src/lib/text.ts`. Each screen is `src/screens/<X>.astro`
plus a model (`(ctx) => {html, attr}`) that renders at build time and every second in the browser. When a screen's
layout changes in the app, update its component. `npm run dev` / `npm run build` in `site/`.

## Releases

Debug-signed on purpose (`restore.sh` uses `run-as`), with `~/.android/debug.keystore` on the maintainer's machine:
releases must keep that key or installed TVs can't upgrade. Steps in `gh.md`; record changes in `CHANGELOG.md`
under Unreleased as you go.
