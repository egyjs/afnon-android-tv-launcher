# Releasing Afnon

How to version, release and publish Afnon on GitHub (`egyjs/afnon-android-tv-launcher`). Run everything from the
repo root in Git Bash (Windows) or a Linux shell, with `TV` set to the test TV's address. Either machine needs the
release key below.

## How it is published today

- No CI: there are no GitHub Actions workflows. The APK is built on the maintainer's machine and uploaded with `gh`.
- One release so far, **v0.1** (2026-10-21): tag `v0.1` on the first commit `89b4d02`, created by
  `gh release create` against `main`. Commits after it (README, design handoff) are unreleased.
- One asset, `afnon.apk` (19.5 MB): `assembleDebug` output, debuggable, debug-signed, with the 14 adhan MP3s inside.
  Its SHA-256 is printed in the release notes. It already has downloads, so the signing key below is binding.
- The repo's homepage link points to `releases/latest`; the README links `releases/latest/download/afnon.apk`.
  Both always follow the newest non-draft, non-prerelease release.

## Versioning

- `versionName` in `app/build.gradle` is `MAJOR.MINOR` (`0.1`, `0.2` …), with an optional patch (`0.2.1`) for a
  fix-only release. Stay on `0.x` until Afnon has run on more TVs than the TCL 55P7K.
  - Minor: new features, settings or screens.
  - Patch: bug fixes only, nothing new to learn.
- `versionCode` goes up by **1** on every release, whatever the name. Android refuses to update to a lower code.
- The git tag is `v` + `versionName` (`v0.2`). The release title is `Afnon 0.2 · أفنون`.

## Signing (read once)

Releases are **debug-signed** with `~/.android/debug.keystore` (certificate SHA-256 starts `b061307c`). This is
deliberate: `tools/restore.sh` uses `run-as`, which only works on debuggable builds. To release from another machine,
copy that file to its `~/.android/debug.keystore` (back up the one there first) and check the fingerprint with
`keytool -list -v -keystore ~/.android/debug.keystore -storepass android`. Every release must be signed
with that same key, or `adb install -r` fails on TVs that already have Afnon (signatures do not match) and users
would have to uninstall and lose their settings. Keep a backup of that keystore somewhere safe. Never rebuild it.

## Release steps

1. **Start clean.** `git status` shows nothing unexpected; `git pull` on `main`.
2. **Bump the version** in `app/build.gradle`: `versionCode` +1, `versionName` to the new version.
3. **Update the docs** for whatever changed (list below), and in `CHANGELOG.md` rename `[Unreleased]` to
   `[X.Y] - YYYY-MM-DD`, add a fresh empty `[Unreleased]`, and update the compare links at the bottom.
4. **Build with the adhans and spoken lines in.** The APK must include the recordings:
   ```bash
   bash tools/fetch-adhans.sh          # no-op if they are already there
   bash tools/fetch-announce.sh        # the 12 spoken lines (pip install edge-tts); no-op if there
   ls app/src/main/assets/adhan/*.mp3 | wc -l   # expect 14
   ls app/src/main/assets/announce/*.mp3 | wc -l   # expect 12
   ./gradlew clean :app:assembleDebug
   cp app/build/outputs/apk/debug/app-debug.apk afnon.apk
   ```
5. **Check the APK.**
   ```bash
   BT=~/AppData/Local/Android/Sdk/build-tools/35.0.0     # Linux: ~/Android/Sdk/build-tools/35.0.0, no .exe/.bat
   $BT/aapt.exe dump badging afnon.apk | head -1          # versionCode and versionName are the new ones
   $BT/apksigner.bat verify --print-certs afnon.apk | grep SHA-256   # b061307c0c67a15b…
   sha256sum afnon.apk                                     # for the release notes
   ```
6. **Test the upgrade on the TV**, installing over the previous release (that is what users do):
   ```bash
   adb -s $TV install -r afnon.apk
   ```
   Then with the real remote: Home and every screen in Arabic and English, the test hooks
   (`--es test friday|ramadan|reminder|call|banner|radio`), each remote button from inside another app, holding
   Volume, and a reboot (Home, the remote buttons service and the screensaver come back).
   `adb -s $TV logcat -b crash -d` must show no new crashes, especially none `IN SYSTEM PROCESS`.
7. **Commit and push.**
   ```bash
   git add -A && git commit -m "Afnon X.Y"
   git push origin main
   ```
8. **Publish the release.** `gh` creates the tag on the pushed `main`, as it did for v0.1 (a lightweight tag).
   The asset must be named `afnon.apk`: the README's download link is `releases/latest/download/afnon.apk`.
   Every release also carries the phone installer as `afnon-installer.apk` (the README links
   `releases/latest/download/afnon-installer.apk`, so a release without it breaks that link). It downloads
   `afnon.apk` from the latest release itself, so it only needs rebuilding when `installer/` changed, but attach it
   each time:
   ```bash
   ./gradlew :installer:assembleDebug
   cp installer/build/outputs/apk/debug/installer-debug.apk afnon-installer.apk
   gh release create vX.Y afnon.apk afnon-installer.apk --target main --title "Afnon X.Y · أفنون" --notes-file notes.md
   git fetch --tags
   ```
   `notes.md`: the version's `CHANGELOG.md` section (plus a one-line Arabic summary), the install commands as in
   the v0.1 release, "Tested on a TCL 55P7K (Google TV, Android 12)", the debug-signing note, the APK's SHA-256,
   and the adhan credit line. Copy the layout of the v0.1 notes: `gh release view v0.1`.
9. **Check it.** `gh release view vX.Y` shows the asset, the README download link fetches the new APK, and its
   SHA-256 matches the notes. Delete the local `afnon.apk`, `afnon-installer.apk` and `notes.md` (APKs are git-ignored anyway).

## What to update, by kind of change

| Changed | Update |
|---|---|
| Anything users notice | `CHANGELOG.md` under `[Unreleased]`, as you make the change |
| A feature or screen | README Features and the Arabic section; screenshots in `docs/screenshots/` taken on the TV with `tools/shot.sh` (no personal data in them) |
| A permission, grant or setting Afnon writes | README Privacy table and Install commands, `tools/install.sh`, `tools/restore.sh` (undo it), release notes install block |
| Remote buttons | `RemoteKeys.java` table; README "Remote buttons" and TCL notes; test with the real remote |
| A dependency, font, recording or other third-party file | `THIRD_PARTY.md`, README Credits |
| A city or calculation method | `Prefs.java`; link an official timetable in the PR (CONTRIBUTING.md §1) |
| Tools or setup steps | README Install / Everyday changes, `CLAUDE.md` |
| Tested on a new TV | README intro and TCL notes, CONTRIBUTING.md §2 |

## If a release goes wrong

- Bad APK, tag fine: `gh release upload vX.Y afnon.apk --clobber`, then fix the SHA-256 in the notes
  (`gh release edit vX.Y --notes-file notes.md`).
- Bad release: publish a fixed `X.Y.1` with a higher `versionCode`. Never reuse a version code or move a pushed tag.
