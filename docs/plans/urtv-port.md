# Plan: what Afnon takes from UrTV

Source: [UrTV](https://github.com/tshi-dev/UrTV) (MIT, copying approved by the maintainer). Reviewed 2026-10-22.
Everything is adapted to Afnon's rules: no network beyond the radio, no new timers, transform/opacity animation
only, no blur or shadows, Arabic and English strings in `Text.java`, explicit D-pad focus. Every screen change is
checked on the TV and mirrored in `site/`.

**Mocks approved 2026-10-22.** Run `npm run dev` in `site/` and open `/mocks/`. They are dev-only and never in the
production build. Each item below names its mock: the mock is the spec, and the app must match it. Once an item ships,
port it into the real screen component and delete its mock.

## Phase 0: TV setup tools (done, not committed)

| Item | Files |
|---|---|
| Safe home switch: tries Android's home setting first. Only where the firmware pins its own home does it disable `launcherx` and `setupwraith`, behind a 5-minute safety switch on the TV | `tools/set-home.sh`, `tools/deadman.sh`, `install.sh` |
| Lists: safe to turn off, ask first, never off. `disable.sh` takes a list and refuses the keep list | `tools/lists/*`, `disable.sh`, `keep.sh` |
| Speed tweaks: animations at 0.5x, Play Store restricted in the background, cached-app freezer | `tools/tweaks.sh` |
| `restore.sh` always brings back Google TV's home, its setup app and helpers | `restore.sh`, README, CHANGELOG, CLAUDE.md |

## Rule: Quranic text is never typed by hand (done)

`site/scripts/quran.mjs` fetches verses from Tanzil (`quran-simple-enhanced`) into `afnon-mihrab/quran.json`. The
app's `Quran.java`, the site, the README and the design files are generated from it, and CI runs
`npm run quran:check`. The crown verse (4:103) was the only typed verse, and its copy had marks in a different order
from Tanzil's.

## Phase 1: fixes and small wins

1. **Isha in Ramadan (bug).** Umm al-Qura puts Isha 120 min after Maghrib in Ramadan; we keep 90. Work out the
   Hijri month before the times and set `params.ishaInterval = 120`. Files: `PrayerDay.java` (UrTV
   `template.html:454`).
2. **Time left in words**: «بعد ساعتين و ١٢ دقيقة» instead of h:mm. Put a space after و before a digit and none
   before a word. Use «ساعة واحدة», not UrTV's «١ ساعة». Shown on Home and the screensaver. Files: `Text.java`,
   `site/src/lib/text.ts` (UrTV `template.html:477-500`). Mocks: `words`, `words-screensaver`.
3. **Network pill states**: Wi-Fi, wired, or offline. Offline shows a struck-through icon and a dimmed «غير متصل»
   / "Offline" (UrTV launcher `MainActivity.java:754-773`). Ours, not UrTV's: a gold shield reading "VPN" inside the
   pill when the connection goes through a VPN (`NetworkCapabilities.TRANSPORT_VPN`), since Tailscale's DNS has
   broken the radio before. Files: `Screen.java`, `MainActivity`; needs `ACCESS_NETWORK_STATE`. Mocks:
   `wifi-offline`, `wifi-wired`, `wifi-vpn`.

## Phase 2: prayer features

4. **Iqama countdown**: a state of its own, so it can't be mistaken for the countdown to the adhan.
   - The arch label becomes a light badge, «أُذّن المغرب ٦:١٧ م».
   - The big word is «الإقامة», with the count in gold-200.
   - The sub-line reads «بعدها العشاء …».
   - The arch gets a 4px gold outer stroke and a dotted gold inner line (`1 12`).
   - The prayer's cell tag reads «الإقامة».
   - Shown on the Home arch and the screensaver, using the existing renders.
   - **No Settings row.** The minutes follow the city's country, from a table in `Prefs`. Countries without an
     official source use Saudi Arabia's (Ministry of Islamic Affairs): Fajr 25, Dhuhr 20, Asr 20, Maghrib 10,
     Isha 20. A country is added only with a cited official source. Egypt's Awqaf publishes none that we found, so
     Egypt uses Saudi's for now. There is no iqama for Friday Dhuhr, and it never shows under one minute.
   - Source: UrTV `template.html:344-345, 517-531`. Mocks: `iqama-home`, `iqama-screensaver`.
5. **Days until Ramadan**, shown in Sha'ban only: «رمضان ١٤٤٨ · ١٢ يوم · يبدأ …», in gold-200, on its own line
   below the date with a clear gap (184px from the top on Home). Shown on Today's prayers and the screensaver.
   Source: UrTV `template.html:545-610`. Mocks: `ramadan-soon`, `ramadan-soon-screensaver`.
6. **Night dimming** of the screensaver:
   - Opacity of the group's contents ramps down from 21:00 to 23:00, holds at 0.46 dim until 05:00, and is back up
     by 07:00.
   - Opacity only, because TCL ignores `screen_brightness`. The hours go in `tokens.json`. On/off in Settings →
     Display.
   - Source: UrTV `template.html:1118-1129`. Mock: `night-dim`, which has a time slider.
7. **"Start screensaver after"** row in Settings → Display: 1/2/5/10/15/30 min. Uses the `WRITE_SETTINGS` appop and
   falls back to the TV's own screensaver settings. Source: UrTV `Prefs.java:64-74`,
   `SettingsActivity.java:410-449`. Mock: `settings`, which also shows the new rows from items 6 and 13.

## Phase 3: UI polish from UrTV's home screen

8. **Focus tiles**: bounce to 1.1x (overshoot) with a light sheen, and the app name fades in under the tile. No shadow
   lift (rule). **Decided: A**, our gold focus (the focus colour on every other screen). B, UrTV's white ring, is dropped.
   Source: UrTV launcher `MainActivity.java:432-520`. Mocks: `tiles-a`, `tiles-b`.
9. **Colour glow** behind the favourites, sampled from the focused app's icon. Two glow layers crossfade by opacity
   (no colour animation). **Decided: keep.** Needs a GPU check on the TV. Source: `sampleColor`, `MainActivity.java:945-973`. Mock:
   `glow`.
10. **Entrance**: the favourites slide in one after another on first show. Source: `playEntrance`, `:977-991`. Seen
    in the tiles mocks.
11. **Hold OK on a Home tile** opens a menu: move earlier, move later, replace with another app (opens All apps),
    All apps. Source: `showAppMenu`, `:591-653`. Mock: `hold-menu` (hold Enter or right-click).
12. **Inputs**: a pill after the network pill opens a list of HDMI inputs via `TvInputManager` and opens one
    directly. Source: `showInputs`, `:655-727`. Mock: `inputs`.

## Phase 3b: radio card (ours)

20. **Screensaver instead of Screen off** on the Quran Radio card: «شاشة التوقف» with an arch icon. It starts
    Afnon's screensaver with the radio still playing (`com.android.systemui/.Somnambulator`, as UrTV's
    `start-saver.sh` does). Mock: `radio-saver`. **Decided:** the Settings → Radio row becomes "Screensaver while
    listening", replacing "Screen off while listening".

## Phase 4: verse of the day

13. **Verse of the day, on the screensaver only.** The crown verse (4:103) stays on every screen.
    - A fixed list of refs goes through `site/scripts/quran.mjs` like every other verse: refs only in code, text
      fetched from Tanzil, bundled in the APK, no network on the TV.
    - Shown in Amiri with surah:ayah, pause marks kept, and drifting with the screensaver.
    - Every TV shows the same verse on a given day (day number from the local date).
    - On/off in Settings → Display ("Verse of the day on the screensaver").
    - **Decided: UrTV's method, as is.** Copy UrTV's `REFS` list (`.github/tools/fetch_verses.py`, refs only)
      into `quran.mjs` as a `daily` list, fetched from Tanzil like every other verse. As UrTV's `build.py:32-51`
      does, drop the basmala that the API puts before ayah 1 (except Al-Fatiha). Unlike UrTV, keep the pause
      marks, because Amiri has them. Pick by day number into the list.
    - Source: UrTV `fetch_verses.py`, `build.py:32-51`, `template.html:1058-1073`. Mock: `verse-screensaver`.
14. **Hadith** on alternate days, later and optional. Each must be checked against the six books and Al-Albani's
    grades (UrTV `verify_hadith.py`, `build.py:53-206`).

## Phase 5: tools

15. **ADB settings broadcast** (city, method, Hijri offset, favourites), shell-only via `android.permission.DUMP`
    (UrTV `SaverReceiver.java`, `DockReceiver.java`).
16. **Back up the TV's own settings** on install (home, screensaver, timeout) so `restore.sh` restores exactly
    those (UrTV `backup.sh`, `reset.sh:30-45`).
17. **`tools/measure.sh`**: memory before and after (UrTV `measure.sh`).
18. **`shot.sh screensaver`** and a tool that starts the screensaver (UrTV `start-saver.sh`, `shot.sh`).
19. **Private-data check in CI**: IPs, keys, Wi-Fi names (UrTV `tests/check.py:179-213`).

## Not copying

- Weather, markets, football, TV episodes, hosted photos.
- The 6-hourly self-update.
- The WebView screensaver and Home card.
- The Python ADB client.
- Prayer times from an online API.

All of these need network access, timers or a WebView. We already have pixel shift, Friday mode and arranging
favourites.

## Decisions (2026-10-22)

1. Commit Phase 0 and the Quran pipeline: yes.
2. Iqama: by country, Saudi by default, no Settings row (item 4).
3. Verse of the day: screensaver only; the crown verse stays on every screen.
4. Tiles: A.
5. Glow: keep.
6. Verse list: UrTV's refs and method (item 13).
7. Radio Settings row: "Screensaver while listening".
8. The 7 remaining safe-list apps were turned off on the TV on 2026-10-22, logged in `state/disabled.log`.

## Requests from issue #26 (2026-10-31)

[#26](https://github.com/egyjs/afnon-android-tv-launcher/issues/26) asked for three things:

- **Islamic screensaver (verses and hadith, with sources):** already planned. Verses are item 13 (verse of the day,
  surah:ayah shown); hadith is item 14 (alternate days, each checked against the six books and Al-Albani's grades).
  The issue's "verses, hadith or both" choice becomes item 13's Settings row with three values once item 14 ships.
- **Weather on Home:** not taken. It needs a weather service, so it is under Not copying with UrTV's weather.
- **Thmanyah font (خط ثمانية), chosen in Settings:** open question 1 below.

## Open questions

1. **Thmanyah as a choice for the Arabic UI font.** Before any mock:
   - its licence must allow bundling in an MIT app
   - the APK size it adds
   - Arabic shaping and digits on the TV
   - how it sits next to Amiri, which stays for Quranic text whatever is chosen

   It would be a Settings → Display row, defaulting to the current font.

Next: Phase 1.
