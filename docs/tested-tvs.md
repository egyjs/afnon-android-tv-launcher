# Tested TVs

Which TVs Afnon has run on, and what is known to work on each. Only what was checked is listed; a dash
means the source doesn't say. Ran it on another TV? Tell us in an issue (see
[CONTRIBUTING.md](../CONTRIBUTING.md#2-add-your-tv)), even if everything worked.

| Brand | Model | Android | Chip / firmware | Status | Source |
|---|---|---|---|---|---|
| TCL | 55P7K | Google TV, Android 12 | TCL Google TV firmware | Main target: everything is built and checked here | [README](../README.md#tcl-notes) |
| TCL | C06_2K_GB | Android TV 11 | TCL firmware (`com.tcl.settings`) | Audio Only screen-off checked; nothing else recorded | [CHANGELOG 0.2](../CHANGELOG.md#02---2026-10-21) |
| Super General | - | Android 9 | CVTE board | Tested on one set | [README](../README.md#other-brands-cvte) |
| Xiaomi | Mi TV MiTV-MOOQ0 | Android TV 10 | MediaTek, 1.8 GB RAM | Tested, some parts not yet checked | [README](../README.md#other-brands-xiaomi), [#37](https://github.com/egyjs/afnon-android-tv-launcher/pull/37), [#39](https://github.com/egyjs/afnon-android-tv-launcher/pull/39) |
| Unbranded ("Smart TV") | Toptech board | Android 12 | HiSilicon `bigfish` (Hi352S), TitanOS 2 launcher, 1280×720, 1 GB RAM | Reported running as Home; nothing else reported yet | [#35](https://github.com/egyjs/afnon-android-tv-launcher/issues/35) |
| Google (emulator) | Stock Android TV | Android 9 | `tvlauncher` | Installer and home step tested | [#24](https://github.com/egyjs/afnon-android-tv-launcher/pull/24) |
| TCL | BeyondTV | - | - | Reported by a fork, not tested by Afnon | [Discussion #36](https://github.com/egyjs/afnon-android-tv-launcher/discussions/36) |
| Google | Google TV Streamer | - | - | Reported by a fork, not tested by Afnon | [Discussion #36](https://github.com/egyjs/afnon-android-tv-launcher/discussions/36) |

Not tested yet: Android 14, and Google TV's `launcherx` path on any TV but the 55P7K.

## TCL

**55P7K** (Google TV, Android 12, 1920×1080, about 1.8 GB RAM). Every screen, the call to prayer, reminders,
the radio and the remote are checked here, over ADB and with the real remote.

- Home sticks only with `launcherx` and `setupwraith` disabled; `tools/set-home.sh` does that behind an on-TV
  safety switch.
- The remote buttons go through TCL's own remapping (`TclKeys.java`), so the accessibility service doesn't
  filter keys and holding Volume keeps changing it.
- Source and TV are left to the firmware: TCL opens its own input bar (`com.tcl.suspension`), and a second start
  closed it again.
- Audio Only (screen off while listening) uses TCL Settings' own dialog.
- `install.sh` grants `APP_AUTO_START` so the remote buttons service survives a reboot.
- Settings sends `KEYCODE_NOTIFICATION`, the browser key sends 4159.
- `screen_brightness` does nothing; TCL drives the backlight.
- A force-stop could leave the remote buttons service listed as on but not running; Home now turns it back on (0.8).

**C06_2K_GB** (Android TV 11). Audio Only works there; TCL Settings hosts it in `com.tcl.settings` instead of
`com.android.tv.settings`. Nothing else about this set is recorded.

## Super General (CVTE)

One set, CVTE board, Android 9. Afnon detects CVTE boards by their hotkey service
(`android.intent.action.HOTKEYSERVICE`, `FirmwareKeys.java`).

- The firmware answers the remote's buttons itself, so the service doesn't filter keys and holding Volume works.
- Source is left to the app in front, so Home opens CVTE's source menu (`com.cvte.tv.globalui`) itself.
- Buttons the firmware wires to an app (ESHARE opens EShare) can be pointed elsewhere with the `redirects`
  preference (`Redirects.java`). Empty by default.
- `key_filter` (`on`/`off`) forces key filtering if Afnon guesses wrong.
- `install.sh` grants location, which Android 9+ needs before Home can show the Wi-Fi name.

## Xiaomi

One Mi TV MiTV-MOOQ0 (Android TV 10, MediaTek, 1.8 GB RAM, low-RAM mode), debloated with
[cleanyourtv](https://github.com/Gathin23/cleanyourtv)'s list for that model.

- Works: Home, the app list, favourites, the screensaver, the remote buttons service; `install.sh` makes Afnon
  Home (it turns the stock `tvlauncher` off behind the safety switch). Google's setup app stays enabled: it owns
  the saved Wi-Fi network there.
- Input sends Source (`KEYCODE_TV_INPUT`, 178). The firmware opens the stock launcher's inputs panel, which setup
  disables, so Afnon opens its own Inputs panel instead (0.8.1). The panel starts with TV channels, the built-in
  satellite receiver, then HDMI; AV is left out (0.8.2).
- Xiaomi's TV Manager force-stops apps, which switches the remote buttons service off; Home turns it back on.
- A few built-in apps have only a phone-style entry (Clock, MediaTek's Netflix watcher, a disclaimer screen);
  Afnon leaves them out of its app list.
- Not checked yet: holding Volume (the service filters keys here), Audio Only, the other remote buttons.

## Toptech / HiSilicon

An unbranded "Smart TV" on a Toptech board, HiSilicon `bigfish` (Hi352S), Android 12, 1280×720, 1 GB RAM in
low-RAM mode. Its stock launcher is Zeasn's TitanOS 2 (`com.zeasn.toptech.titanos2.launcher`), with no Google
launcher. The report in [#35](https://github.com/egyjs/afnon-android-tv-launcher/issues/35) says Afnon is
installed and running as Home; it doesn't yet say how the remote, the call or the 720p layout behave.

## Stock Android TV (emulator)

An Android TV 9 emulator with `tvlauncher`, used to test the phone installer
([#24](https://github.com/egyjs/afnon-android-tv-launcher/pull/24)).

- `set-home-activity` reports Success but the stock launcher's HOME filter outranks Afnon, so the home step turns
  the stock launcher off (plus `setupwraith` on Google TV) behind the safety switch.
- Home crashed with fewer than four apps besides Afnon; fixed in 0.6.1.

## Reported by others

- **Awan TV**, a fork built from Afnon 0.5 ([Discussion #36](https://github.com/egyjs/afnon-android-tv-launcher/discussions/36)),
  reports that on a TCL BeyondTV and a Google TV Streamer the accessibility service stayed listed as on but was not
  running after a force-stop or an update. That is the fork's finding, not an Afnon test.
- **LG (webOS)** isn't Android, so Afnon doesn't run there. Afnon LG is a separate JavaScript port, maintained in
  its own repos ([#31](https://github.com/egyjs/afnon-android-tv-launcher/issues/31)); report LG bugs there.
