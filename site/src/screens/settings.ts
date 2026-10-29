// Settings (SettingsActivity): sections, each section's rows and the Asr preview, as HTML strings so the same
// code renders the page at build time and re-renders it in the browser after every change.
import voicesJson from '../data/adhans.json';
import type { Ctx } from '../lib/ctx';
import { CITIES, METHODS, REGIONS, STATIONS, cityOf, methodOf, stationOf, type Prefs, type Station } from '../lib/data';
import { ASR, clock, dayAdjustment, hijri, methodParams } from '../lib/prayer';
import type { Text } from '../lib/text';
import { loadFacing } from './prayer-day';

export const PRAYER = 0, REMINDERS = 1, RADIO = 2, DISPLAY = 3, NETWORK = 4, DEVICE = 5, SECTIONS = 6;
export type Kind = 'value' | 'seg' | 'step' | 'toggle';
export const VOICES = voicesJson;

/** Every saved setting is in Prefs; the room direction is the Prayer Day screen's (loadFacing). */
export type SPrefs = Prefs;

export type Row = {
  id: string; kind: Kind; label: string; help: string;
  value?: string; segs?: string[]; selected?: number; on?: boolean;
};

const esc = (s: string) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
const trim = (v: number) => String(+v.toFixed(2));

// ---------------------------------------------------------------- Text.java methods with logic, ported

export const sectionName = (t: Text, i: number) =>
  [t.s('sectionPrayer'), t.s('sectionReminders'), t.s('sectionRadio'), t.s('sectionDisplay'), t.ar ? 'الشبكة' : 'Network', t.s('sectionDevice')][i];

function locationHelp(t: Text, lat: number, lng: number) {
  const ns = lat >= 0 ? (t.ar ? 'ش' : 'N') : t.ar ? 'ج' : 'S';
  const ew = lng >= 0 ? (t.ar ? 'ق' : 'E') : t.ar ? 'غ' : 'W';
  return t.d(`${Math.abs(lat).toFixed(2)}° ${ns} · ${Math.abs(lng).toFixed(2)}° ${ew}` + (t.ar ? ' · يُختار من القائمة' : ' · chosen from the list'));
}

function methodAngles(t: Text, m: string) {
  const p = methodParams(m);
  const fajr = (t.ar ? 'زاوية الفجر ' : 'Fajr angle ') + trim(p.fajrAngle) + '°';
  const isha = p.ishaInterval > 0
    ? t.ar ? `العشاء بعد المغرب بـ ${p.ishaInterval} دقيقة` : `Isha ${p.ishaInterval} min after Maghrib`
    : (t.ar ? 'العشاء ' : 'Isha ') + trim(p.ishaAngle) + '°';
  const plus = dayAdjustment(m);
  const extra = m === 'MOROCCO_HABOUS'
    ? t.ar ? ' · الظهر +5 دقائق · أفق كل مدينة' : " · Dhuhr +5 min · each city's horizon"
    : plus === 0 ? '' : t.ar ? ` · +${plus} دقائق للظهر والعصر والمغرب` : ` · Dhuhr, Asr and Maghrib +${plus} min`;
  return t.d(fajr + ' · ' + isha + extra);
}

export const stationChoice = (t: Text, s: Station) => (s.reciter ? t.station(s) : t.station(s) + t.sep() + t.stationWhere(s));
const radioRowHelp = (t: Text, s: Station) => t.stationWhere(s) + (t.ar ? ' · تحتاج إلى الإنترنت' : ' · needs the internet');
const radioDarkHelp = (t: Text) =>
  t.d(t.ar ? 'توفير الكهرباء: تنطفئ الشاشة وتستمر الإذاعة · أي زر يعيدها' : 'Saves power: the screen goes dark, the radio keeps playing · any key brings it back');
export const voiceName = (t: Text, id: string | undefined, fallback: string) => {
  const v = VOICES.find((x) => x.id === (id ?? fallback)) ?? VOICES[0];
  return t.ar ? v.ar : v.en;
};
export const placeOf = (t: Text, c: (typeof CITIES)[number]) => (t.ar ? t.place(c.ar, c.countryAr) : t.place(c.en, c.countryEn));

// ---------------------------------------------------------------- rows

/** Settings → Reminders, in the order of Text.remindChoices (a list on the TV: six are too many for a seg). */
export const REMIND = [5, 10, 15, 20, 30, 0];
const remindIndex = (p: SPrefs) => (REMIND.indexOf(p.remind) < 0 ? 2 : REMIND.indexOf(p.remind));
const DARK = [0, 15, -1];
const CLOCKS = ['system', '12', '24'] as const;

export function rowsFor(c: Ctx, section: number, radioOn = false): Row[] {
  const { t } = c;
  const p = c.prefs as SPrefs;
  const facing = loadFacing();
  const city = cityOf(p);
  const st = stationOf(p);
  switch (section) {
    case PRAYER: return [
      { id: 'city', kind: 'value', label: t.s('location'), help: locationHelp(t, city.lat, city.lng), value: placeOf(t, city) },
      { id: 'method', kind: 'value', label: t.s('method'), help: methodAngles(t, methodOf(p)), value: t.methodName(methodOf(p)) },
      { id: 'school', kind: 'seg', label: t.s('asrSchool'), help: t.s('asrSchoolHelp'), segs: t.list('asrSchools'), selected: p.hanafi ? 1 : 0 },
      { id: 'hijri', kind: 'step', label: t.s('hijriAdjust'), help: (t.ar ? 'اليوم: ' : 'Today: ') + hijri(t, c.day), value: t.days(p.hijriOffset) },
      { id: 'room', kind: 'value', label: t.s('roomTitle'), help: t.s('roomHelp'), value: facing < 0 ? t.s('notSet') : t.bearingLabel(facing) },
    ];
    case REMINDERS: return [
      { id: 'remind', kind: 'value', label: t.s('remind'), help: t.s('remindHelp'), value: t.d(t.list('remindChoices')[remindIndex(p)]), selected: remindIndex(p) },
      { id: 'adhan', kind: 'toggle', label: t.s('adhanRow'), help: t.s('adhanHelp'), on: p.adhan },
      { id: 'voice', kind: 'value', label: t.s('voiceRow'), help: t.s('voiceHelp'), value: voiceName(t, p.voice, 'alafasy') },
      { id: 'voiceFajr', kind: 'value', label: t.s('fajrVoiceRow'), help: t.s('fajrVoiceHelp'), value: voiceName(t, p.voiceFajr, 'chebae_fajr') },
    ];
    case RADIO: return [
      { id: 'radio', kind: 'toggle', label: t.station(st), help: radioRowHelp(t, st), on: radioOn },
      { id: 'station', kind: 'value', label: t.s('stationRow'), help: t.list('stationGroups')[st.reciter ? 1 : 0], value: stationChoice(t, st) },
      { id: 'resume', kind: 'seg', label: t.s('afterAdhanRow'), help: t.s('afterAdhanHelp'), segs: t.list('afterAdhanChoices'), selected: p.radioResume === false ? 1 : 0 },
      { id: 'dark', kind: 'seg', label: t.s('radioDarkRow'), help: radioDarkHelp(t), segs: t.list('radioDarkChoices').map(t.d), selected: Math.max(0, DARK.indexOf(p.radioDark ?? 0)) },
    ];
    case DISPLAY: return [
      { id: 'lang', kind: 'seg', label: t.s('language'), help: t.s('languageHelp'), segs: [t.ar ? 'التلفاز' : 'TV', 'العربية', 'English'], selected: t.ar ? 1 : 2 },
      { id: 'digits', kind: 'seg', label: t.s('digits'), help: '', segs: ['0123', '٠١٢٣'], selected: t.eastern ? 1 : 0 },
      { id: 'clock', kind: 'seg', label: t.s('clockRow'), help: t.s('clockHelp'), segs: t.list('clocks').map(t.d), selected: Math.max(0, CLOCKS.indexOf(p.clock)) },
    ];
    case NETWORK: return [{ id: 'network', kind: 'value', label: t.s('networkRow'), help: t.s('networkHelp'), value: '' }];
    default: return [{ id: 'device', kind: 'value', label: t.s('deviceRow'), help: t.s('deviceHelp'), value: '' }];
  }
}

/** Editing a seg or step row (OK, then ◀▶): the row and its unsaved choice or amount. */
export type Edit = { row: number; draft: number };

/** What editing starts from: the chosen segment, or the Hijri offset. */
export const savedDraft = (c: Ctx, row: Row) => (row.kind === 'seg' ? row.selected! : (c.prefs as SPrefs).hijriOffset);

/** ◀▶ while editing (d = +1 toward the end), held to the row's range. */
export const moveDraft = (row: Row, draft: number, d: number) =>
  row.kind === 'seg' ? Math.max(0, Math.min(row.segs!.length - 1, draft + d)) : Math.max(-2, Math.min(2, draft + d));

/** OK while editing: the new prefs, or null when the draft is what is already saved. 'lang' is the page's. */
export function commit(c: Ctx, row: Row, draft: number): SPrefs | null {
  if (draft === savedDraft(c, row)) return null;
  return row.kind === 'seg' ? pick(c, row.id, draft) : { ...(c.prefs as SPrefs), hijriOffset: draft };
}

/** Choosing item i of a seg row. 'lang' is handled by the page (it moves to the other language's page). */
export function pick(c: Ctx, id: string, i: number): SPrefs | null {
  const p = { ...(c.prefs as SPrefs) };
  switch (id) {
    case 'school': return { ...p, hanafi: i === 1 };
    case 'remind': return { ...p, remind: REMIND[i] };
    case 'resume': return { ...p, radioResume: i === 0 };
    case 'dark': return { ...p, radioDark: DARK[i] };
    case 'digits': return { ...p, digits: i === 1 ? 'eastern' : 'western' };
    case 'clock': return { ...p, clock: CLOCKS[i] };
    default: return null;
  }
}

// ---------------------------------------------------------------- HTML

const chevron = '<svg class="chev" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 5l7 7-7 7"/></svg>';

export function rowHtml(t: Text, r: Row, i: number, editing = false) {
  const hint = JSON.stringify(editing
    ? [t.s('hintChange'), t.s('hintSave'), t.s('hintBackCancel')]
    : [t.s('hintMove'), r.kind === 'value' ? t.s('hintChoose') : r.kind === 'toggle' ? t.s('hintSwitch') : t.s('hintEdit'), t.s('hintBackMenu')]);
  let control = '';
  if (r.kind === 'value') control = `<span class="val">${esc(r.value ?? '')}</span>${chevron}`;
  else if (r.kind === 'seg')
    control = `<span class="track">${r.segs!.map((s, k) => `<span class="seg${k === r.selected ? ' on' : ''}" data-seg="${k}">${esc(s)}</span>`).join('')}</span>`;
  else if (r.kind === 'step')
    control = `<span class="stepb" data-step="-1">−</span><span class="stepv">${esc(r.value ?? '')}</span><span class="stepb" data-step="1">+</span>`;
  else control = `<span class="switch${r.on ? ' on' : ''}"><i></i></span>`;
  return `<button class="ft row srow${editing ? ' editing' : ''}" data-row="${i}" data-id="${r.id}" data-kind="${r.kind}" style="top:${112 + i * 98}px" data-hints="${esc(hint)}">`
    + `<span class="words"><span class="rl">${esc(r.label)}</span>${r.help ? `<span class="rh">${esc(r.help)}</span>` : ''}</span>`
    + `<span class="ctl">${control}</span></button>`;
}

export function navHtml(t: Text, section: number) {
  const hint = esc(JSON.stringify([t.s('hintMove'), t.s('hintOpen'), t.s('hintBackHome')]));
  return Array.from({ length: SECTIONS }, (_, i) =>
    `<button class="ft row nav${i === section ? ' open' : ''}" data-nav="${i}"${i === section ? ' data-focus-start' : ''} style="top:${152 + i * 86}px" data-hints="${hint}">${esc(sectionName(t, i))}</button>`).join('');
}

/** The rows; the one being edited shows its draft. */
export const rowsHtml = (c: Ctx, section: number, radioOn = false, edit: Edit | null = null) =>
  rowsFor(c, section, radioOn).map((r, i) => {
    if (edit?.row !== i) return rowHtml(c.t, r, i);
    const shown = r.kind === 'seg' ? { ...r, selected: edit.draft } : { ...r, value: c.t.days(edit.draft) };
    return rowHtml(c.t, shown, i, true);
  }).join('');
export const previewHtml = (c: Ctx) => clock(c.t, c.day.times[ASR], c.h24);

// ---------------------------------------------------------------- chooser lists

export type Item = { label: string; chosen: boolean };

/** The region a city's country is in (index into REGIONS). */
export const regionOf = (c: (typeof CITIES)[number]) => REGIONS.findIndex((r) => r.includes(c.countryEn));
/** One region's countries, A to Z in the page's language ("ال" not counted). */
export const countries = (t: Text, region: number) =>
  REGIONS[region]
    .map((en) => CITIES.find((c) => c.countryEn === en)!)
    .map((c) => (t.ar ? c.countryAr : c.countryEn))
    .sort((a, b) => new Intl.Collator(t.ar ? 'ar' : 'en').compare(a.replace(/^ال/, ''), b.replace(/^ال/, '')));
export const citiesOf = (t: Text, country: string) => CITIES.filter((c) => (t.ar ? c.countryAr : c.countryEn) === country);
export const methodItems = (t: Text, p: Prefs): Item[] => METHODS.map((m) => ({ label: t.methodName(m), chosen: m === methodOf(p) }));
export const stationsOf = (reciters: boolean) => STATIONS.filter((s) => !!s.reciter === reciters);

export function chooserHtml(t: Text, title: string, items: Item[]) {
  const hint = esc(JSON.stringify([t.s('hintMove'), t.s('hintChoose'), t.s('hintBackCancel')]));
  return `<div class="ch-card"><div class="ch-title display-bold">${esc(title)}</div><div class="ch-list">`
    + items.map((it, i) => `<button class="ft ch-item${it.chosen ? ' chosen' : ''}" data-item="${i}" data-hints="${hint}">${esc(it.label)}</button>`).join('')
    + '</div></div>';
}
