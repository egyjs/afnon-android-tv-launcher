// Dev-only UI mocks of the features planned from UrTV (docs/plans/urtv-port.md), shown at /mocks/ by `npm run dev`.
// Each mock is a real screen of the preview with flags on the stage (data-mock): the flags change the screen model
// here (patchModel), add Settings rows (mockRows), jump the clock (mockShift), and add elements (overlay.ts).
// Nothing here ships: every hook into the real site is behind import.meta.env.DEV.
import type { Ctx, Model } from '../lib/ctx';
import type { Prefs } from '../lib/data';
import { DHUHR, SUNRISE, clock, duration, hijriOf, prayerDay, secondsUntil } from '../lib/prayer';
import type { Text } from '../lib/text';
import quran from '../../../afnon-mihrab/quran.json';

// Never typed by hand: sample verses fetched into afnon-mihrab/quran.json by scripts/quran.mjs.
const VERSES = [quran.verses.sampleS, quran.verses.sampleM, quran.verses.sampleL];

type L = { ar: string; en: string };
export type Mock = {
  slug: string;
  screen: 'home' | 'prayer-day' | 'apps' | 'settings' | 'call' | 'screensaver';
  mode?: string;
  flags: string;
  plan: string; // item number(s) in docs/plans/urtv-port.md
  title: L;
  what: L;
  try: L;
  from: string; // where in UrTV
  rules: L; // how it keeps Afnon's rules
};

export const MOCKS: Mock[] = [
  {
    slug: 'iqama-home', screen: 'home', mode: 'iqama', flags: 'iqama', plan: '4',
    title: { ar: 'العدّ للإقامة · الرئيسية', en: 'Iqama countdown · Home' },
    what: {
      ar: 'بعد الأذان يعدّ المحراب إلى الإقامة، بشكل يختلف عن العدّ العادي: شارة فاتحة «أُذّن المغرب ٦:١٧ م» بلون خانة الصلاة الحالية، وكلمة «الإقامة» مكان اسم الصلاة، والعدّ بالذهبي الفاتح، وخط منقّط داخل المحراب. وخانة الصلاة تقول «الإقامة». لا إقامة لصلاة الجمعة.',
      en: 'After the adhan the arch counts down to the iqama, and looks different from the usual countdown: a light badge “Maghrib 6:17 PM adhan” in the colour of the current prayer’s cell, “Iqama” as the big word, the count in light gold, and a dotted line inside the arch. The prayer’s cell says “Iqama”. No iqama countdown for Jumu‘ah.',
    },
    try: { ar: 'الساعة هنا بعد أذان الصلاة القادمة بسبع دقائق. الدقائق لكل صلاة من الإعدادات (المحاكاة: الفجر ٢٥، المغرب ١٠، والباقي ٢٠، كما في UrTV).', en: 'The clock here is seven minutes after the next adhan. Minutes per prayer come from Settings (mock: Fajr 25, Maghrib 10, the rest 20, as UrTV).' },
    from: 'template.html:344-345, 517-531',
    rules: { ar: 'يستعمل عدّ الثواني الموجود في الرئيسية، بلا مؤقت جديد.', en: 'Runs on Home’s existing per-second render; no new timer.' },
  },
  {
    slug: 'iqama-screensaver', screen: 'screensaver', mode: 'iqama', flags: 'iqama', plan: '4',
    title: { ar: 'العدّ للإقامة · شاشة التوقف', en: 'Iqama countdown · Screensaver' },
    what: { ar: 'شاشة التوقف تقول «العصر · الإقامة» والوقت الباقي، حتى تُقام الصلاة.', en: 'The screensaver says “Asr · Iqama” and the time left, until the iqama.' },
    try: { ar: 'أي زر يوقظ الشاشة.', en: 'Any key wakes it.' },
    from: 'template.html:517-531',
    rules: { ar: 'نفس تحديث الدقيقة في AfnonDream.', en: 'Same per-minute update in AfnonDream.' },
  },
  {
    slug: 'words', screen: 'prayer-day', mode: 'long', flags: 'words', plan: '2',
    title: { ar: 'الوقت الباقي بالكلمات', en: 'Time left in words' },
    what: {
      ar: '«بعد ٤ ساعات و ١٢ دقيقة» بدل «بعد ٤:١٢» في مواقيت اليوم وشاشة التوقف والتذكير. الساعة والساعتان والدقيقة والدقيقتان بأسمائها.',
      en: '“in 4 h 12 min” instead of “in 4:12” on Today’s prayers, the screensaver and the reminder.',
    },
    try: { ar: 'انظر عمود الحالة في الصفوف القادمة. الساعة هنا بعد الشروق بعشرين دقيقة.', en: 'Look at the status column of the coming rows. The clock is 20 minutes after sunrise.' },
    from: 'template.html:477-500 (humanRemaining)',
    rules: { ar: 'نص فقط، في Text.java بالعربية والإنجليزية.', en: 'Text only, in Text.java in Arabic and English.' },
  },
  {
    slug: 'words-screensaver', screen: 'screensaver', mode: 'long', flags: 'words', plan: '2',
    title: { ar: 'الوقت الباقي بالكلمات · شاشة التوقف', en: 'Time left in words · Screensaver' },
    what: { ar: 'السطر الأخير بالكلمات.', en: 'The last line in words.' },
    try: { ar: '', en: '' },
    from: 'template.html:477-500',
    rules: { ar: 'نص فقط.', en: 'Text only.' },
  },
  {
    slug: 'wifi-offline', screen: 'home', flags: 'wifi-off', plan: '3',
    title: { ar: 'حالة الشبكة · غير متصل', en: 'Network pill · offline' },
    what: { ar: 'زر الشبكة يقول «غير متصل» بأيقونة مشطوبة بدل «Wi-Fi» دائمًا.', en: 'The network pill says “Offline” with a struck icon instead of always “Wi-Fi”.' },
    try: { ar: 'الحالة الأخرى: «سلكي» في المحاكاة التالية.', en: 'The other state, “Wired”, is the next mock.' },
    from: 'launcher MainActivity.java:754-773',
    rules: { ar: 'يقرأ حالة الاتصال فقط، بلا شبكة.', en: 'Only reads the connection state; no network use.' },
  },
  {
    slug: 'wifi-wired', screen: 'home', flags: 'wifi-wired', plan: '3',
    title: { ar: 'حالة الشبكة · سلكي', en: 'Network pill · wired' },
    what: { ar: 'على كابل الشبكة يقول «سلكي» بأيقونة الكابل.', en: 'On Ethernet it says “Wired” with a cable icon.' },
    try: { ar: '', en: '' },
    from: 'launcher MainActivity.java:754-773',
    rules: { ar: 'كما سبق.', en: 'As above.' },
  },
  {
    slug: 'wifi-vpn', screen: 'home', flags: 'wifi-vpn', plan: '3',
    title: { ar: 'حالة الشبكة · عبر VPN', en: 'Network pill · through a VPN' },
    what: {
      ar: 'إذا كان الاتصال يمر عبر VPN (مثل Tailscale) يظهر في زر الشبكة درع صغير و«VPN». يفيد حين تتعطل الإذاعة بسبب DNS الخاص بالـ VPN.',
      en: 'When the connection goes through a VPN (Tailscale, say) the network pill shows a small shield and “VPN”. Handy when the radio fails because of the VPN’s DNS.',
    },
    try: { ar: '', en: '' },
    from: 'Afnon (new); ConnectivityManager, TRANSPORT_VPN',
    rules: { ar: 'يقرأ حالة الاتصال فقط.', en: 'Only reads the connection state.' },
  },
  {
    slug: 'radio-saver', screen: 'home', flags: 'radio-saver', plan: '20',
    title: { ar: 'الإذاعة · شاشة التوقف بدل إطفاء الشاشة', en: 'Radio · Screensaver instead of Screen off' },
    what: {
      ar: 'في بطاقة الإذاعة يصبح زر «إطفاء الشاشة» زر «شاشة التوقف»: يبدأ شاشة أفنون فورًا والإذاعة مستمرة، فترى الساعة والصلاة القادمة وأنت تستمع.',
      en: 'On the Quran Radio card, “Screen off” becomes “Screensaver”: it starts Afnon’s screensaver at once with the radio still playing, so the clock and the next prayer stay in view while you listen.',
    },
    try: {
      ar: 'البطاقة مفتوحة. OK على «شاشة التوقف» ينقلك إليها. سؤال: هل يبقى صف «إطفاء الشاشة أثناء الاستماع» في إعدادات الإذاعة، أم يصير «شاشة التوقف أثناء الاستماع»؟',
      en: 'The card is open. OK on Screensaver goes there. Question: does Settings → Radio keep “Screen off while listening”, or become “Screensaver while listening”?',
    },
    from: 'Afnon (new); DreamService via com.android.systemui/.Somnambulator (UrTV start-saver.sh)',
    rules: { ar: 'نفس شاشة التوقف الحالية؛ بلا مؤقت جديد.', en: 'The existing screensaver; no new timer.' },
  },
  {
    slug: 'ramadan-soon', screen: 'prayer-day', mode: 'shaban', flags: 'shaban', plan: '5',
    title: { ar: 'العدّ إلى رمضان · مواقيت اليوم', en: 'Days to Ramadan · Today' },
    what: { ar: 'في شعبان: «رمضان ١٤٤٨ · بعد ١٢ يومًا · يبدأ …» تحت التاريخ، وفي آخر يوم «رمضان مبارك · غدًا».', en: 'In Sha‘ban: “Ramadan 1448 · in 12 days · starts …” under the date; on the last day “Ramadan Mubarak · tomorrow”.' },
    try: { ar: 'الساعة هنا ١٨ شعبان.', en: 'The clock is on 18 Sha‘ban.' },
    from: 'template.html:545-610',
    rules: { ar: 'حساب هجري على التلفاز، مرة في اليوم.', en: 'Hijri maths on the TV, once a day.' },
  },
  {
    slug: 'ramadan-soon-screensaver', screen: 'screensaver', mode: 'shaban', flags: 'shaban', plan: '5',
    title: { ar: 'العدّ إلى رمضان · شاشة التوقف', en: 'Days to Ramadan · Screensaver' },
    what: { ar: 'سطر ذهبي تحت التاريخ الهجري.', en: 'A gold line under the Hijri date.' },
    try: { ar: '', en: '' },
    from: 'template.html:545-610',
    rules: { ar: 'كما سبق.', en: 'As above.' },
  },
  {
    slug: 'night-dim', screen: 'screensaver', mode: 'night', flags: 'night', plan: '6',
    title: { ar: 'تخفيت شاشة التوقف ليلًا', en: 'Night dimming' },
    what: {
      ar: 'من ٩ مساءً تخفت شاشة التوقف تدريجيًا حتى ١١ (٤٦٪ أخفت)، وتبقى كذلك حتى ٥ فجرًا، ثم تعود حتى ٧. بالشفافية وحدها لأن TCL يتجاهل سطوع أندرويد.',
      en: 'From 9 pm the screensaver fades down until 11 (46% dimmer), stays there until 5 am, then comes back by 7. Opacity only: TCL ignores Android’s brightness setting.',
    },
    try: { ar: 'حرّك شريط الساعة تحت الشاشة.', en: 'Move the hour slider under the screen.' },
    from: 'template.html:1118-1129',
    rules: { ar: 'شفافية فقط، مع تحديث الدقيقة الموجود. الساعات في tokens.json.', en: 'Opacity only, on the existing minute update. Hours go in tokens.json.' },
  },
  {
    slug: 'settings', screen: 'settings', flags: 'settings', plan: '6, 7, 13',
    title: { ar: 'صفوف الإعدادات الجديدة', en: 'New Settings rows' },
    what: {
      ar: 'في «العرض»: شاشة التوقف بعد (٥/١٠/١٥/٣٠ د)، التخفيت الليلي، وآية اليوم على شاشة التوقف. لا إعداد للإقامة: دقائقها حسب الدولة.',
      en: 'In Display: Screensaver after (5/10/15/30 min), night dimming, and the verse of the day on the screensaver. No iqama setting: its minutes follow the country.',
    },
    try: { ar: 'افتح «العرض» من القائمة الجانبية.', en: 'Open Display from the sidebar.' },
    from: 'Prefs.java:64-74, SettingsActivity.java:410-449',
    rules: { ar: 'نفس صفوف الإعدادات الحالية.', en: 'The same row kinds Settings already has.' },
  },
  {
    slug: 'tiles-a', screen: 'home', flags: 'tiles-a entrance', plan: '8, 10',
    title: { ar: 'المربعات · أ: تركيزنا مع ارتداد ولمعة', en: 'Tiles · A: our focus, with bounce and sheen' },
    what: {
      ar: 'نبقي تعبئة التركيز الذهبية، ونضيف ارتدادًا خفيفًا عند التكبير ولمعة مائلة. والمفضلة تدخل واحدة بعد الأخرى عند فتح الرئيسية.',
      en: 'Keeps our gold focus fill, adds a small overshoot on the zoom and a diagonal sheen. Favourites slide in one after another when Home opens.',
    },
    try: { ar: 'تنقّل بين المفضلة. أعد تحميل الصفحة لترى الدخول.', en: 'Move across the favourites. Reload to see the entrance.' },
    from: 'launcher MainActivity.java:432-520, 977-991',
    rules: { ar: 'تحريك بالتكبير والشفافية فقط؛ بلا ظل (UrTV يرفع المربع بظل).', en: 'Scale and opacity only; no shadow (UrTV lifts the tile with one).' },
  },
  {
    slug: 'tiles-b', screen: 'home', flags: 'tiles-b entrance', plan: '8, 10',
    title: { ar: 'المربعات · ب: أسلوب UrTV', en: 'Tiles · B: UrTV’s style' },
    what: {
      ar: 'المربع يبقى بلونه، وحوله حلقة بيضاء رفيعة ولمعة، ويكبر ١٫١ مع ارتداد. أهدأ من التعبئة الذهبية، لكنه يغيّر لغة التركيز في التطبيق.',
      en: 'The tile keeps its colour, with a thin white ring and a sheen, and grows to 1.1× with an overshoot. Calmer than the gold fill, but it changes the app’s focus language.',
    },
    try: { ar: 'قارن بـ «أ».', en: 'Compare with A.' },
    from: 'launcher MainActivity.java:464-500',
    rules: { ar: 'كما سبق.', en: 'As above.' },
  },
  {
    slug: 'glow', screen: 'home', flags: 'tiles-a glow', plan: '9',
    title: { ar: 'توهّج بلون التطبيق', en: 'Colour glow' },
    what: {
      ar: 'خلف المفضلة توهّج بلون أيقونة التطبيق المركّز عليه، يتبدّل بتلاشٍ بين طبقتين. للتجربة على معالج التلفاز الضعيف قبل اعتماده.',
      en: 'Behind the favourites, a glow in the focused app’s icon colour, swapped by crossfading two layers. To be tried on the TV’s weak GPU before we keep it.',
    },
    try: { ar: 'تنقّل بين المفضلة.', en: 'Move across the favourites.' },
    from: 'launcher MainActivity.java:775-797, 945-973 (sampleColor)',
    rules: { ar: 'تدرّج دائري بلا ضبابية، وتبديل بالشفافية لا بتحريك اللون.', en: 'A radial gradient, no blur; swapped by opacity, not a colour animation.' },
  },
  {
    slug: 'hold-menu', screen: 'home', flags: 'tiles-a hold-menu', plan: '11',
    title: { ar: 'الضغط المطوّل على مفضلة', en: 'Hold OK on a favourite' },
    what: {
      ar: 'الضغط المطوّل على OK فوق مفضلة في الرئيسية يفتح قائمة صغيرة: تحريك للأمام أو للخلف، استبدال بتطبيق آخر، كل التطبيقات. اليوم هذا في «كل التطبيقات» فقط.',
      en: 'Holding OK on a Home favourite opens a small menu: move earlier or later, replace with another app, All apps. Today this lives only in All apps.',
    },
    try: { ar: 'القائمة مفتوحة على المفضلة الثانية. رجوع يغلقها؛ امسك Enter على أي مفضلة لفتحها.', en: 'The menu is open on the second favourite. Back closes it; hold Enter on any favourite to open it.' },
    from: 'launcher MainActivity.java:591-653',
    rules: { ar: 'تركيز صريح داخل القائمة، والأسهم معكوسة في العربية.', en: 'Explicit focus inside the menu; arrows mirrored in RTL.' },
  },
  {
    slug: 'inputs', screen: 'home', flags: 'inputs', plan: '12',
    title: { ar: 'زر المصادر', en: 'Inputs pill' },
    what: {
      ar: 'زر «المصادر» بجانب الإذاعة والشبكة يفتح قائمة بمداخل HDMI بأسمائها في التلفاز والبث التلفزيوني، ويفتح المدخل مباشرة.',
      en: 'An Inputs pill beside Radio and Network lists the HDMI inputs by their TV names, plus Live TV, and opens one directly.',
    },
    try: { ar: 'القائمة مفتوحة. رجوع يغلقها، وOK على «المصادر» يفتحها.', en: 'The list is open. Back closes it; OK on Inputs opens it.' },
    from: 'launcher MainActivity.java:655-727',
    rules: { ar: 'من TvInputManager على التلفاز، بلا شبكة.', en: 'From TvInputManager on the TV; no network.' },
  },
  {
    slug: 'verse-screensaver', screen: 'screensaver', flags: 'verse', plan: '13',
    title: { ar: 'آية اليوم · شاشة التوقف', en: 'Verse of the day · Screensaver' },
    what: {
      ar: 'آية قصيرة بخط أميري تحت المحراب، تتغير كل يوم وهي نفسها على كل تلفاز. النص من تنزيل عبر سكربت في المستودع، لا يُكتب باليد، ويُضمّن في التطبيق.',
      en: 'A short verse in Amiri under the arch, a new one each day and the same on every TV. The text comes from Tanzil through a script in the repo, never typed by hand, and is bundled in the app.',
    },
    try: { ar: 'آية اليوم من ٢٣٩ آية قصيرة جمعها UrTV.', en: 'Today’s verse, from the 239 short ones UrTV collected.' },
    from: 'fetch_verses.py, build.py:32-51, template.html:1058-1073',
    rules: { ar: 'بلا شبكة على التلفاز؛ علامات الوقف باقية (أميري يرسمها).', en: 'No network on the TV; pause marks kept (Amiri draws them).' },
  },
];

export const mockOf = (slug: string) => MOCKS.find((m) => m.slug === slug);

// ---------------------------------------------------------------- the moment each mock shows

const RAMADAN = 8, SHABAN = 7; // 0-based Hijri months

/** Test moments for the mocks' modes (prayer.ts's shiftFor calls this for modes it doesn't know). */
export function mockShift(mode: string, prefs: Prefs, real = new Date()): number {
  const c = new Date(real);
  if (mode === 'iqama') {
    const day = prayerDay(real, prefs);
    return day.nextTime.getTime() + 7 * 60000 + 20000 - real.getTime();
  }
  if (mode === 'long') {
    const day = prayerDay(real, prefs);
    const at = new Date(day.times[SUNRISE]);
    if (at < real) at.setDate(at.getDate() + 1);
    return at.getTime() + 20 * 60000 - real.getTime();
  }
  if (mode === 'shaban') {
    for (let i = 0; i < 400; i++) {
      const h = hijriOf(c);
      if (h.month === SHABAN && h.day === 18) break;
      c.setDate(c.getDate() + 1);
    }
    c.setHours(20, 15, 0, 0);
  } else if (mode === 'night') {
    c.setHours(23, 30, 0, 0);
  } else return 0;
  return c.getTime() - real.getTime();
}

// ---------------------------------------------------------------- planned logic (to port into Text.java / PrayerDay)

/** UrTV's defaults (Saudi Ministry of Islamic Affairs), minutes from adhan to iqama; Sunrise has none. */
export const IQAMA = [25, 0, 20, 20, 10, 20];

/** Between an adhan and its iqama: which prayer and how long is left. None for Jumu'ah. */
export function iqamaOf(c: Ctx) {
  const { day, now } = c;
  const i = day.nowIndex;
  if (i < 0 || i === SUNRISE || (day.friday && i === DHUHR)) return null;
  const end = new Date(day.times[i].getTime() + IQAMA[i] * 60000);
  return end > now ? { index: i, end, left: secondsUntil(end, now) } : null;
}

const count = (t: Text, n: number, one: string, two: string, few: string, many: string) =>
  n === 1 ? one : n === 2 ? two : t.d(n + ' ' + (n <= 10 ? few : many));

/** «بعد ساعتين و ١٢ دقيقة»: hours and minutes in words (UrTV's humanRemaining, with «ساعة واحدة»). */
export function inWords(t: Text, seconds: number) {
  const total = Math.max(1, Math.ceil(seconds / 60));
  const h = Math.floor(total / 60), m = total % 60;
  if (!t.ar) return t.d('in ' + [h ? h + ' h' : '', m ? m + ' min' : ''].filter(Boolean).join(' '));
  const hs = h ? count(t, h, 'ساعة واحدة', 'ساعتين', 'ساعات', 'ساعة') : '';
  const ms = m ? count(t, m, 'دقيقة واحدة', 'دقيقتين', 'دقائق', 'دقيقة') : '';
  // «و ١٢ دقيقة» before a number, «ودقيقتين» before a word.
  return 'بعد ' + (hs && ms ? hs + (/^[0-9٠-٩]/.test(ms) ? ' و ' : ' و') + ms : hs || ms);
}

/** In Sha‘ban: «رمضان ١٤٤٨ · بعد ١٢ يومًا · يبدأ الأربعاء ١٨ فبراير». */
export function ramadanSoon(c: Ctx) {
  const { t, now, prefs, day } = c;
  if (day.hijri.month !== SHABAN) return '';
  const d = new Date(now);
  d.setHours(12, 0, 0, 0);
  for (let n = 1; n <= 31; n++) {
    d.setDate(d.getDate() + 1);
    const h = hijriOf(d, prefs.hijriOffset);
    if (h.month !== RAMADAN) continue;
    const starts = new Intl.DateTimeFormat(t.ar ? 'ar-EG-u-nu-latn' : 'en-GB', { weekday: 'long', day: 'numeric', month: 'long' }).format(d);
    const when = n === 1
      ? (t.ar ? 'رمضان مبارك · غدًا' : 'Ramadan Mubarak · tomorrow')
      : t.ar ? 'بعد ' + count(t, n, 'يوم', 'يومين', 'أيام', 'يومًا') : `in ${n} days`;
    return t.d((t.ar ? 'رمضان ' : 'Ramadan ') + h.year + ' · ' + when + ' · ' + (t.ar ? 'يبدأ ' : 'starts ') + starts);
  }
  return '';
}

/** The same verse on every TV each day: the local date's day number into the list. */
export function verseOf(c: Ctx) {
  const n = Math.floor(Date.UTC(c.now.getFullYear(), c.now.getMonth(), c.now.getDate()) / 86400000);
  const v = VERSES[n % VERSES.length];
  return { text: v.lines.join(' '), ref: c.t.ar ? c.t.d(`${v.surah.replace(/^سُورَةُ /, '').replace(/[ً-ٰٟۥۦ۪-ۭ]/g, '')} ${v.ayah}`) : `Quran ${v.ref}` };
}

// ---------------------------------------------------------------- the screen models, changed

export function patchModel(screen: string, c: Ctx, m: Model): Model {
  const { t, day, h24 } = c;
  const has = (f: string) => c.mock.includes(f);
  const names = [...t.list('prayerNames')];
  if (day.friday) names[DHUHR] = t.s('jumuah');
  const html = { ...m.html };
  const attr = { ...(m.attr ?? {}) };
  const iq = has('iqama') ? iqamaOf(c) : null;

  if (screen === 'home' && iq) {
    // A different state at a glance, not just other words: a plaster "now" badge (the colour of the current
    // prayer's cell), «الإقامة» as the big word, the count in gold-200, and a dotted inner line in the arch.
    html.archLabel = `<span class="mock-badge">${t.d((t.ar ? 'أُذّن ' : '') + names[iq.index] + ' ' + clock(t, day.times[iq.index], h24) + (t.ar ? '' : ' adhan'))}</span>`;
    html.archName = t.ar ? 'الإقامة' : 'Iqama';
    html.archCount = t.d(duration(iq.left, true));
    html.archSub = t.d((t.ar ? 'بعدها ' : 'Then ') + names[day.nextIndex] + ' ' + clock(t, day.nextTime, h24));
    html.archSub2 = '';
    html.cells = html.cells.replace(`data-state="now"><span class="tag caps">${t.s('nowTag')}<`, `data-state="now"><span class="tag caps">${t.ar ? 'الإقامة' : 'Iqama'}<`);
    attr.arch = { 'data-state': 'iqama' };
    attr.archCount = { 'data-long': 'false', 'data-iqama': 'true' };
  }
  if (screen === 'screensaver') {
    if (iq) {
      html.next = names[iq.index] + t.sep() + (t.ar ? 'الإقامة' : 'Iqama');
      html.left = inWords(t, iq.left);
    } else if (has('words')) {
      html.left = inWords(t, secondsUntil(day.nextTime, c.now));
    }
    if (has('shaban')) html.date += `<span class="mock-gold">${ramadanSoon(c)}</span>`;
  }
  if (screen === 'prayer-day' && has('words')) {
    day.times.forEach((time, i) => {
      if (time > c.now && i !== day.nowIndex) html['s' + i] = inWords(t, secondsUntil(time, c.now));
    });
  }
  if (has('shaban')) html.mockRamadan = ramadanSoon(c);
  if (has('verse')) {
    const v = verseOf(c);
    html.mockVerse = v.text;
    html.mockVerseRef = v.ref;
  }
  return { html, attr };
}

// ---------------------------------------------------------------- Settings rows

type Row = { id: string; kind: 'value' | 'seg' | 'step' | 'toggle'; label: string; help: string; value?: string; segs?: string[]; selected?: number; on?: boolean };

export function mockRows<R extends Row>(c: Ctx, section: number, rows: R[]): R[] {
  if (!c.mock.includes('settings')) return rows;
  const { t } = c;
  const ar = t.ar;
  if (section === 3) {
    return [
      ...rows,
      { id: 'saver', kind: 'seg', label: ar ? 'شاشة التوقف بعد' : 'Screensaver after',
        help: ar ? 'إعداد التلفاز نفسه، من هنا مباشرة' : 'The TV’s own setting, from here',
        segs: (ar ? ['5 د', '10 د', '15 د', '30 د'] : ['5 min', '10 min', '15 min', '30 min']).map(t.d), selected: 1 } as R,
      { id: 'nightDim', kind: 'toggle', label: ar ? 'تخفيت شاشة التوقف ليلًا' : 'Dim the screensaver at night',
        help: t.d(ar ? 'تخفت من 9 م وتعود حتى 7 ص' : 'Fades from 9 pm, back by 7 am'), on: true } as R,
      { id: 'verse', kind: 'toggle', label: ar ? 'آية اليوم على شاشة التوقف' : 'Verse of the day on the screensaver',
        help: ar ? 'آية قصيرة كل يوم، من نص المصحف كما هو' : 'A short verse each day, the exact Mushaf text', on: true } as R,
    ];
  }
  return rows;
}
