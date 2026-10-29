// Screensaver (AfnonDream): clock, Hijri date, next prayer and the time left.
import tokens from '../../../afnon-mihrab/tokens.json';
import type { Ctx, Model } from '../lib/ctx';
import { clock, duration, secondsUntil, weekday } from '../lib/prayer';
import { prayerNames } from './home';

const UNIT = { ms: 1, s: 1000, min: 60000 } as const;
const ms = (v: string) => { const m = /^([\d.]+)(ms|s|min)$/.exec(v)!; return Number(m[1]) * UNIT[m[2] as keyof typeof UNIT]; };
/** motion.screensaver-drift-interval: the group moves this often. */
export const DRIFT_MS = ms(tokens.motion['screensaver-drift-interval'].$value);

export function screensaverModel(c: Ctx): Model {
  const { t, day, now, h24 } = c;
  const secs = secondsUntil(day.nextTime, now);
  return {
    html: {
      clock: clock(t, now, h24, 0.35),
      // The Hijri day's weekday, which turns at Maghrib with its date (Screen.weekday).
      date: t.d(`${weekday(t, day)}${t.sep()}${day.hijri.day} ${t.list('hijriMonths')[day.hijri.month]}`),
      next: prayerNames(c)[day.nextIndex] + ' ' + clock(t, day.nextTime, h24),
      left: t.inMinutes(Math.ceil(secs / 60), t.d(duration(secs, false))),
    },
  };
}
