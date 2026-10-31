# Contributing to Afnon · المساهمة في أفنون

Thank you for helping. Afnon is a home screen people look at five times a day, at prayer time, so
the bar is calm, correct and reliable. A wrong Fajr time matters more than a missing feature.

<p dir="rtl">شكرًا لمساهمتك. أفنون شاشة ينظر إليها الناس خمس مرات في اليوم عند كل صلاة، لذلك نريدها
هادئة ودقيقة ويُعتمد عليها؛ خطأ في وقت الفجر أهم من أي ميزة ناقصة. يمكنك فتح المشكلات وطلبات الدمج
بالعربية أو الإنجليزية.</p>

Issues and pull requests are welcome in Arabic or English.

## 1. Add your city

Cities live in `Prefs.java` (`CITIES`), each with an Arabic and English name, coordinates and the
calculation method used locally.

1. Add one `new City(...)` line next to its country's other cities.
2. Use the method your local religious authority publishes, for example Egyptian General Authority
   of Survey for Egypt, or Umm al-Qura for Saudi Arabia.
3. In the PR, compare Afnon's six times for one day with an official source (a ministry or awqaf
   site, or a local masjid's timetable) and link it. Within a minute or two is expected; say if
   anything is further off.

## 2. Add your TV

Afnon is built and tested on a TCL 55P7K (Google TV, Android 12), and has also run on a Super General (CVTE,
Android 9) and a Xiaomi MiTV-MOOQ0 (Android TV 10); the README has notes for each. If you run it on another TV,
tell us how it went, even if everything worked:

- Model, Android version and RAM.
- Whether Home, the remote buttons service, the screensaver and the call screen survive a reboot.
- What the remote's special buttons send. `adb shell getevent -l` shows the codes; add them to the
  table in `RemoteKeys.java`.
- If your brand has its own way to turn the panel off while sound plays (like TCL's Audio Only),
  add it to `ScreenOffActivity.audioOnly()` behind a check that fails quietly elsewhere.

## 3. Improve the launcher

The code is deliberately plain: Java, Android Views, one dependency (adhan-java), no AndroidX, no
Kotlin, no Compose. It has to stay smooth on a TV with under 2 GB of RAM, so:

- **No blur, video backgrounds or shadows.** Animate with transform and opacity only.
- **No new timers or background services.** Prayer checks share one 15-second timer in the
  accessibility service.
- **No new network access.** The radio stream is the only connection. No analytics, no ads, no
  feeds, no "recommended" rows.
- **Design tokens.** Colours, sizes and timings live in `afnon-mihrab/tokens.json`. `Tokens.java`,
  `colors.xml` and the preview site's CSS are generated from it: edit the JSON, then run `npm run tokens`
  in `site/` (Node 24). Never edit the generated files; CI checks they match. Text comes from `Text.java`,
  in both languages.
- **Arabic and English together.** Every string has both. Check the layout in RTL and in LTR.
  Arabic gets no letter-spacing. Quranic text uses Amiri.
- **D-pad focus is explicit.** Each screen has a focus map, with Left and Right swapped in RTL. Test
  Up, Down, Left and Right from every edge, and Back from every screen.
- **Verify on a real TV.** Use the test hooks in the README (`--es test friday|ramadan|call|…`) and
  `tools/shot.sh`. Put screenshots in the PR, one in Arabic and one in English if layout changed.

**The web preview** (`site/`) is an Astro site that copies each screen for the browser. It reads
`Text.java`, `Prefs.CITIES`, `Radio.STATIONS` and the fonts at build time (`site/scripts/extract.mjs`), so a
new string or city shows up there by itself. If you change a screen's layout, change its component in
`site/src/screens/` too: `cd site && npm install && npm run dev`.

Good fits: more cities and methods, an Inputs screen, accessibility of the call screen, more
reciters with clear licences, other brands' screen-off modes.

Not a fit: content feeds, ads, telemetry, account sign-ins, anything that phones home.

## 4. Religious content

Quranic verses, adhan wording and prayer names must be exact. For a verse, give the surah and ayah
number, and copy it from a standard mushaf text (for example from tanzil.net). If you are unsure
about a ruling (Asr schools, high-latitude rules, Hijri sighting), open an issue first rather than
choosing for everyone. Afnon gives users the choice wherever scholars differ.

## 5. Audio

Only add recordings you have the right to share, and say where each one comes from and under what
licence. Recordings go in `app/src/main/assets/adhan/` and their list in `Adhans.java`. If they
can't be committed, add them to `tools/fetch-adhans.sh`.

## Pull requests

- One topic per PR. Small is fine.
- Say which TV you tested on, and attach screenshots.
- `./gradlew :app:assembleDebug` must build with no new warnings.
- **No personal data.** Before you push, check screenshots and logs for your Wi-Fi name, IP
  addresses, serial numbers, your home city or room direction if you'd rather keep them private,
  and other apps' content playing behind Afnon. Never commit `state/` or `local.properties`.
- Be kind in reviews.

By contributing you agree that your work is released under the repo's MIT licence.
