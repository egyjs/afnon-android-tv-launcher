// Interface strings. Plain ones come from the app's Text.java via scripts/extract.mjs (src/data/strings.json);
// the ones with logic are ported here from Text.java, method for method. Keep both in step with the app.
import S from '../data/strings.json';
import type { Station } from './data';

export type Lang = 'ar' | 'en';
type Entry = { ar: string | string[]; en: string | string[]; d?: boolean };
const STRINGS = S as Record<string, Entry>;
const EASTERN = '٠١٢٣٤٥٦٧٨٩';

export type Text = ReturnType<typeof makeText>;

/** Text.java for one language; `eastern` is the Eastern Arabic digits setting (on by default in Arabic). */
export function makeText(lang: Lang, eastern = lang === 'ar') {
  const ar = lang === 'ar';
  const d = (s: string | number) => {
    s = String(s);
    if (!eastern) return s;
    // "·" beside Eastern digits reads as a zero (٠); Arabic punctuation takes its own comma instead.
    return s.replaceAll(' · ', '، ').replace(/[0-9]/g, (c) => EASTERN[+c]);
  };
  /** A string from Text.java by method name, with {params} filled in. */
  const s = (key: string, params: Record<string, string | number> = {}): string => {
    const e = STRINGS[key];
    if (!e) throw new Error(`no string "${key}" in Text.java`);
    const v = e[lang] as string;
    const out = v.replace(/\{(\w+)\}/g, (_, n) => String(params[n] ?? ''));
    return e.d ? d(out) : out;
  };
  /** A String[] from Text.java. */
  const list = (key: string): string[] => STRINGS[key][lang] as string[];

  const sector = (deg: number) => Math.round((((deg % 360) + 360) % 360) / 45) % 8;
  const octant = (bearing: number) =>
    (ar
      ? ['شمال', 'شمال شرق', 'شرق', 'جنوب شرق', 'جنوب', 'جنوب غرب', 'غرب', 'شمال غرب']
      : ['North', 'North-east', 'East', 'South-east', 'South', 'South-west', 'West', 'North-west'])[sector(bearing)];
  const inMinutes = (minutes: number, hoursAndMinutes: string) => {
    if (minutes >= 60) return d(ar ? 'بعد ' + hoursAndMinutes : 'in ' + hoursAndMinutes);
    if (!ar) return d(`in ${minutes} min`);
    if (minutes <= 1) return 'بعد دقيقة واحدة';
    if (minutes === 2) return 'بعد دقيقتين';
    return d('بعد ' + minutes + (minutes <= 10 ? ' دقائق' : ' دقيقة'));
  };
  const station = (st: Station) => d(ar ? st.ar : st.en);
  const stationWhere = (st: Station) => d(ar ? st.whereAr : st.whereEn);

  return {
    lang, ar, eastern, d, s, list,
    dir: ar ? 'rtl' : 'ltr',
    sep: () => d(' · '),
    ampm: (am: boolean) => (ar ? (am ? 'ص' : 'م') : am ? 'AM' : 'PM'),
    all: (n: number) => d((ar ? 'كل التطبيقات' : 'All apps') + '\n' + n),
    hintOk: (what: string) => 'OK  ' + what,
    inMinutes,
    in: (duration: string) => d(ar ? 'بعد ' + duration : 'in ' + duration),
    apTitle: (prayer: string, minutes: number, hoursAndMinutes: string) =>
      !ar && minutes < 60
        ? d(`${prayer} in ${minutes} ${minutes === 1 ? 'minute' : 'minutes'}`)
        : prayer + ' ' + inMinutes(minutes, hoursAndMinutes),
    apSub: (time: string) => d((ar ? 'الأذان ' : 'Adhan at ') + time + (ar ? ' · رجوع للإغلاق' : ' · Back to dismiss')),
    bannerSub: (time: string, seconds: number) =>
      d((ar ? 'الأذان ' : 'Adhan ') + time + (ar ? ` · يختفي خلال ${seconds} ث` : ` · hides in ${seconds} s`)),
    station, stationWhere,
    radioStatus: (playing: boolean) => (playing ? (ar ? 'على الهواء' : 'On air') : ar ? 'متوقفة' : 'Off'),
    radioNote: () =>
      d(ar ? 'تستمر في الرئيسية وشاشة التوقف · تتوقف إذا شغّل تطبيق آخر صوتًا'
           : 'Keeps playing on Home and the screensaver · stops when another app plays sound'),
    screenOffNote: () =>
      d(ar ? 'الشاشة مطفأة والإذاعة تعمل · اضغط أي زر للعودة' : 'Screen off, radio playing · press any key to come back'),
    nowHint: () => d(ar ? 'رجوع للإغلاق · يُغلق تلقائيًا بعد 10 دقائق' : 'Back to dismiss · closes by itself after 10 min'),
    chip: (prayer: string) => (ar ? prayer + ' بعد' : prayer + ' in'),
    school: (hanafi: boolean) =>
      hanafi ? (ar ? 'العصر: الحنفي' : 'Asr: Hanafi') : ar ? 'العصر: الجمهور (شافعي)' : 'Asr: standard (Shafi‘i)',
    methodName: (m: string) =>
      ({
        UMM_AL_QURA: ar ? 'أم القرى، مكة المكرمة' : 'Umm al-Qura, Makkah',
        MUSLIM_WORLD_LEAGUE: ar ? 'رابطة العالم الإسلامي' : 'Muslim World League',
        KARACHI: ar ? 'جامعة العلوم الإسلامية، كراتشي' : 'University of Islamic Sciences, Karachi',
        DUBAI: ar ? 'دبي' : 'Dubai',
        KUWAIT: ar ? 'الكويت' : 'Kuwait',
        QATAR: ar ? 'قطر' : 'Qatar',
        NORTH_AMERICA: ar ? 'الجمعية الإسلامية لأمريكا الشمالية' : 'ISNA, North America',
        MOON_SIGHTING_COMMITTEE: ar ? 'لجنة رؤية الهلال' : 'Moonsighting Committee',
        SINGAPORE: ar ? 'سنغافورة' : 'Singapore',
        OMAN_AWQAF: ar ? 'وزارة الأوقاف والشؤون الدينية، سلطنة عُمان' : 'Ministry of Awqaf, Oman',
        GULF: ar ? 'منطقة الخليج (Aladhan)' : 'Gulf Region (Aladhan)',
      })[m] ?? (ar ? 'الهيئة المصرية العامة للمساحة' : 'Egyptian General Authority of Survey'),
    place: (city: string, country: string) => city + (ar ? '، ' : ', ') + country,
    days: (n: number) => {
      const sign = n > 0 ? '+' : '';
      if (!ar) return d(`${sign}${n} ${Math.abs(n) === 1 ? 'day' : 'days'}`);
      return d(`${sign}${n} ${Math.abs(n) === 2 ? 'يومان' : 'يوم'}`);
    },
    bell: (on: boolean) => (ar ? (on ? 'الأذان مفعّل' : 'الأذان متوقف') : on ? 'Adhan on' : 'Adhan off'),
    passed: (sunrise: boolean) => (ar ? (sunrise ? 'مضى' : 'مضت') : 'passed'),
    octant,
    qiblaRelative: (clockwise: number) =>
      (ar ? 'القبلة ' : 'Qibla is ') +
      (ar
        ? ['أمامك', 'أمامك إلى اليمين', 'على يمينك', 'خلفك إلى اليمين', 'خلفك', 'خلفك إلى اليسار', 'على يسارك', 'أمامك إلى اليسار']
        : ['ahead of you', 'ahead, to your right', 'to your right', 'behind, to your right', 'behind you',
           'behind, to your left', 'to your left', 'ahead, to your left'])[sector(clockwise)],
    bearingLabel: (deg: number) => d(octant(deg) + ' · ' + deg + '°'),
    roomButton: (set: boolean) =>
      ar ? (set ? 'تغيير اتجاه الغرفة' : 'حدّد اتجاه الغرفة') : set ? 'Change room direction' : 'Set room direction',
  };
}
