// Home's live content, from MainActivity.renderMinute and Screen.renderCountdown.
import tokens from '../../../afnon-mihrab/tokens.json';
import type { Ctx, Model } from '../lib/ctx';
import { DHUHR, FAJR, MAGHRIB, clock, duration, gregorian, hijri, secondsUntil } from '../lib/prayer';

const MIN = { ms: 1 / 60000, s: 1 / 60, min: 1 } as const;
const parseMin = (v: string) => { const m = /^([\d.]+)(ms|s|min)$/.exec(v)!; return Number(m[1]) * MIN[m[2] as keyof typeof MIN]; };
/** timing.approach: the arch turns gold-200 inside these last minutes. */
export const APPROACH_MIN = parseMin(tokens.timing.approach.$value);

export function prayerNames(c: Ctx) {
  const names = [...c.t.list('prayerNames')];
  if (c.day.friday) names[DHUHR] = c.t.s('jumuah');
  return names;
}

/** Arch label, name, countdown and sub lines (Home), with the approaching state. */
export function archModel(c: Ctx) {
  const { t, day, now, h24 } = c;
  const names = prayerNames(c);
  const nextTime = clock(t, day.nextTime, h24);
  let label = t.s('nextPrayer'), name = names[day.nextIndex];
  let sub = t.s('adhanAt') + ' ' + nextTime + t.sep() + day.city[t.lang], sub2 = '';
  if (day.friday && day.nextIndex === DHUHR) label = t.s('jumuahLabel');
  if (day.kahf) sub2 = t.s('kahf');
  if (day.ramadan) {
    sub2 = t.d((t.ar ? 'السحور حتى ' : 'Suhoor ends ') + clock(t, day.suhoorEnd, h24));
    if (day.nextIndex === MAGHRIB) {
      label = t.s('iftarIn');
      name = t.s('iftar');
      sub = t.list('prayerNames')[MAGHRIB] + ' ' + nextTime + t.sep() + day.city[t.lang];
    }
  }
  const left = secondsUntil(day.nextTime, now);
  return {
    left,
    html: { archLabel: label, archName: name, archCount: t.d(duration(left, true)), archSub: t.d(sub), archSub2: t.d(sub2) },
    attr: { arch: { 'data-state': left <= APPROACH_MIN * 60 ? 'approaching' : 'rest' }, archCount: { 'data-long': String(left >= 3600) } },
  };
}

/** The six prayer cells: now (plaster), next (gold ring), past (dim), coming. */
export function cellsHtml(c: Ctx) {
  const { t, day, now, h24 } = c;
  const names = prayerNames(c);
  return day.times.map((time, i) => {
    let tag = i === day.nowIndex ? t.s('nowTag') : i === day.nextIndex ? t.s('nextTag') : '';
    if (day.ramadan && i === FAJR) tag = t.s('suhoorTag');
    if (day.ramadan && i === MAGHRIB) tag = t.s('iftarTag');
    const state = i === day.nowIndex ? 'now' : i === day.nextIndex ? 'next' : time <= now ? 'past' : 'up';
    return `<div class="cell" data-state="${state}"><span class="tag caps">${tag}</span><span class="name">${names[i]}</span><span class="time num">${clock(t, time, h24, 0.55)}</span></div>`;
  }).join('');
}

export function homeModel(c: Ctx): Model {
  const { t, day, now, h24 } = c;
  const arch = archModel(c);
  const minutes = Math.ceil(arch.left / 60);
  const names = prayerNames(c);
  return {
    html: {
      clock: clock(t, now, h24, 0.4),
      gdate: gregorian(t, now),
      hdate: hijri(t, day) + t.sep() + day.city[t.lang],
      ...arch.html,
      cells: cellsHtml(c),
      cardTitle: t.apTitle(names[day.nextIndex], minutes, duration(arch.left, false)),
      cardSub: t.apSub(clock(t, day.nextTime, h24)),
    },
    attr: { ...arch.attr, card: { 'data-for': String(day.nextTime.getTime()), 'data-left': String(arch.left) } },
  };
}
