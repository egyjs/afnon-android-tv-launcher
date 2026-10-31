// PrayerDay.java and Screen.java's clock helpers, on adhan-js (the JavaScript port of the app's adhan-java).
import { CalculationMethod, Coordinates, Madhab, PrayerTimes, Qibla } from 'adhan';
import type { Text } from './text';
import { cityOf, methodOf, type Prefs } from './data';

export const FAJR = 0, SUNRISE = 1, DHUHR = 2, ASR = 3, MAGHRIB = 4, ISHA = 5;
const RAMADAN = 8; // 0-based, as IslamicCalendar.RAMADAN

const METHOD: Record<string, () => ReturnType<typeof CalculationMethod.Egyptian>> = {
  EGYPTIAN: CalculationMethod.Egyptian,
  UMM_AL_QURA: CalculationMethod.UmmAlQura,
  MUSLIM_WORLD_LEAGUE: CalculationMethod.MuslimWorldLeague,
  KARACHI: CalculationMethod.Karachi,
  DUBAI: CalculationMethod.Dubai,
  KUWAIT: CalculationMethod.Kuwait,
  QATAR: CalculationMethod.Qatar,
  NORTH_AMERICA: CalculationMethod.NorthAmerica,
  MOON_SIGHTING_COMMITTEE: CalculationMethod.MoonsightingCommittee,
  SINGAPORE: CalculationMethod.Singapore,
  // Method.java: the ministry's own timetable fitted at 18° with +5 min on Dhuhr, Asr and Maghrib.
  OMAN_AWQAF: () => {
    const p = CalculationMethod.Other();
    p.fajrAngle = 18;
    p.ishaAngle = 18;
    p.methodAdjustments = { ...p.methodAdjustments, dhuhr: 5, asr: 5, maghrib: 5 };
    return p;
  },
  // Method.java/Habous.java: 19°/17° and Dhuhr +5. The app also rounds, uses each city's horizon and Asr's own
  // declination; the preview leaves those out, so its Moroccan times can be a minute or a few off the app's.
  MOROCCO_HABOUS: () => {
    const p = CalculationMethod.Other();
    p.fajrAngle = 19;
    p.ishaAngle = 17;
    p.methodAdjustments = { ...p.methodAdjustments, dhuhr: 5 };
    return p;
  },
};
/** Minutes a method adds to Dhuhr, Asr and Maghrib (Method.dayAdjustment). */
export const methodParams = (m: string) => (METHOD[m] ?? METHOD.EGYPTIAN)();
export const dayAdjustment = (m: string) => methodParams(m).methodAdjustments.dhuhr;

const minute = (d: Date) => new Date(Math.floor(d.getTime() / 60000) * 60000);
const addDays = (d: Date, n: number) => { const c = new Date(d); c.setDate(c.getDate() + n); return c; };

/** Umm al-Qura Hijri date: 0-based month, as the app's IslamicCalendar. */
export function hijriOf(date: Date, offsetDays = 0) {
  const parts = new Intl.DateTimeFormat('en-u-ca-islamic-umalqura-nu-latn', { day: 'numeric', month: 'numeric', year: 'numeric' })
    .formatToParts(addDays(date, offsetDays));
  const get = (t: string) => parseInt(parts.find((p) => p.type === t)?.value ?? '0', 10);
  return { day: get('day'), month: get('month') - 1, year: get('year') };
}

export type PrayerDay = ReturnType<typeof prayerDay>;

/** One moment's view of the prayer day: the six times, the current prayer and the next. */
export function prayerDay(now: Date, prefs: Prefs) {
  const city = cityOf(prefs);
  const where = new Coordinates(city.lat, city.lng);
  const params = methodParams(methodOf(prefs));
  params.madhab = prefs.hanafi ? Madhab.Hanafi : Madhab.Shafi;
  const at = (d: Date) => new PrayerTimes(where, d, params);
  const today = at(now);
  const times = [today.fajr, today.sunrise, today.dhuhr, today.asr, today.maghrib, today.isha].map(minute);

  let latest = -1;
  times.forEach((t, i) => { if (t <= now) latest = i; });
  const nowIndex = latest === SUNRISE ? -1 : latest;
  let next = times.findIndex((t, i) => i !== SUNRISE && t > now);
  const tomorrowFajr = minute(at(addDays(now, 1)).fajr);
  if (next === -1) { next = FAJR; times[FAJR] = tomorrowFajr; }
  const h = hijriOf(now, prefs.hijriOffset);
  return {
    city, times, nowIndex, nextIndex: next, nextTime: times[next],
    suhoorEnd: times[FAJR] > now ? times[FAJR] : tomorrowFajr,
    friday: now.getDay() === 5,
    ramadan: h.month === RAMADAN,
    hijri: h,
    qibla: Qibla(where) as number, // degrees clockwise from true north
  };
}

export const secondsUntil = (when: Date, now: Date) => Math.max(0, Math.floor((when.getTime() - now.getTime() + 999) / 1000));

const p2 = (n: number) => String(n).padStart(2, '0');
/** mm:ss under an hour; above it h:mm:ss, or h:mm without seconds. Western digits: pass through t.d(). */
export function duration(seconds: number, withSeconds: boolean) {
  const h = Math.floor(seconds / 3600), m = Math.floor((seconds % 3600) / 60), s = seconds % 60;
  if (h === 0) return `${p2(m)}:${p2(s)}`;
  return withSeconds ? `${h}:${p2(m)}:${p2(s)}` : `${h}:${p2(m)}`;
}

/** 12/24-hour setting; "system" follows this browser's locale, as the TV follows its own. */
export function is24h(prefs: Prefs) {
  if (prefs.clock !== 'system') return prefs.clock === '24';
  if (typeof Intl === 'undefined') return false;
  return new Intl.DateTimeFormat(undefined, { hour: 'numeric' }).resolvedOptions().hourCycle?.startsWith('h2') ?? false;
}

/** A clock time as HTML; in 12-hour the ص/م (AM/PM) marker is drawn at markerScale of the digits. */
export function clock(t: Text, d: Date, h24: boolean, markerScale = 1) {
  if (h24) return t.d(`${p2(d.getHours())}:${p2(d.getMinutes())}`);
  const hm = t.d(`${d.getHours() % 12 || 12}:${p2(d.getMinutes())}`);
  const mark = t.ampm(d.getHours() < 12);
  return markerScale === 1 ? `${hm} ${mark}` : `${hm} <small style="font-size:${markerScale}em">${mark}</small>`;
}

/** "Thursday 22 October 2026" in the page's language and digits. */
export function gregorian(t: Text, d: Date) {
  const parts = new Intl.DateTimeFormat(t.ar ? 'ar-EG-u-nu-latn' : 'en-GB', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).formatToParts(d);
  const get = (k: string) => parts.find((p) => p.type === k)?.value ?? '';
  return t.d(`${get('weekday')} ${get('day')} ${get('month')} ${get('year')}`);
}

/** "11 Jumada al-Ula 1448 AH". */
export function hijri(t: Text, day: PrayerDay) {
  return t.d(`${day.hijri.day} ${t.list('hijriMonths')[day.hijri.month]} ${day.hijri.year}${t.s('hijriSuffix')}`);
}

/** Test moments, as `adb … --es test friday|ramadan` on the TV: the clock jumps there and keeps ticking. */
export function shiftFor(mode: string, prefs: Prefs, real = new Date()): number {
  const c = new Date(real);
  if (mode === 'friday') {
    while (c.getDay() !== 5) c.setDate(c.getDate() + 1);
    c.setHours(10, 30, 0, 0);
  } else if (mode === 'ramadan') {
    for (let i = 0; i < 400; i++) {
      const h = hijriOf(c);
      if (h.month === RAMADAN && h.day === 15) break;
      c.setDate(c.getDate() + 1);
    }
    c.setHours(16, 30, 0, 0);
  } else if (mode === 'reminder' || mode === 'call') {
    const day = prayerDay(real, prefs);
    const lead = mode === 'reminder' ? prefs.remind * 60000 - 1000 : -2000;
    return day.nextTime.getTime() - lead - real.getTime();
  } else return 0;
  return c.getTime() - real.getTime();
}
