// What every screen model is computed from: the moment, the viewer's prefs and the page's language.
// The same models render the page at build time (Astro) and re-render it every second in the browser.
import { DEFAULT_PREFS, type Prefs } from './data';
import { is24h, prayerDay, shiftFor, type PrayerDay } from './prayer';
import { makeText, type Lang, type Text } from './text';

/** `mock`: dev-only UI mocks (src/mocks), empty on the real site. */
export type Ctx = { now: Date; prefs: Prefs; t: Text; h24: boolean; day: PrayerDay; mode?: string; lang: Lang; mock: string[] };
/** A screen model: HTML per `data-k` key, and attributes per `data-a` key. */
export type Model = { html: Record<string, string>; attr?: Record<string, Record<string, string>> };

export function makeCtx(lang: Lang, prefs: Prefs, now: Date, mode?: string, mock: string[] = []): Ctx {
  const eastern = prefs.digits === 'auto' ? lang === 'ar' : prefs.digits === 'eastern';
  const t = makeText(lang, eastern);
  return { now, prefs, t, h24: is24h(prefs), day: prayerDay(now, prefs), mode, lang, mock };
}

/** Build-time context: Cairo, the app's defaults, at the test moment for this page's mode. */
export function buildCtx(lang: Lang, mode?: string) {
  const real = new Date();
  return makeCtx(lang, DEFAULT_PREFS, new Date(real.getTime() + (mode ? shiftFor(mode, DEFAULT_PREFS, real) : 0)), mode);
}
