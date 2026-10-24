// Every page of the preview: one per TV screen or mode, in Arabic (default, at the root) and English (/en/).
import type { Lang } from './text';

export const REPO = 'https://github.com/egyjs/afnon-android-tv-launcher';
export const APK = `${REPO}/releases/latest/download/afnon.apk`;

type L = Record<Lang, string>;
export type Page = {
  slug: string; // '' is Home
  screen: 'home' | 'prayer-day' | 'apps' | 'settings' | 'call' | 'screensaver';
  mode?: 'friday' | 'ramadan' | 'reminder' | 'call';
  nav: L;
  title: L;
  description: L;
  body: Record<Lang, string[]>;
};

export const PAGES: Page[] = [
  {
    slug: '', screen: 'home',
    nav: { ar: 'الرئيسية', en: 'Home' },
    title: { ar: 'أفنون · شاشة رئيسية لتلفاز أندرويد حول يوم الصلاة', en: 'Afnon · An Android TV home screen built around the prayer day' },
    description: {
      ar: 'أفنون شاشة رئيسية مجانية ومفتوحة المصدر لتلفاز أندرويد: مواقيت الصلاة دون إنترنت، عدّ تنازلي للصلاة القادمة داخل محراب، القبلة، الأذان في وقته، وإذاعة القرآن الكريم.',
      en: 'Afnon is a free, open-source Android TV launcher: offline prayer times, a countdown to the next prayer inside a mihrab arch, Qibla, the adhan on time and Quran radio.',
    },
    body: {
      ar: [
        'أفنون يستبدل الشاشة الرئيسية لتلفاز أندرويد بشاشة واحدة هادئة. الصلاة القادمة وعدّها التنازلي داخل محراب، ومواقيت اليوم الستة في الأعلى، وتطبيقاتك المفضلة تحتها، والأذان يأتي إلى التلفاز وحده في وقته.',
        'المواقيت تُحسب على التلفاز نفسه دون إنترنت، لـ 147 مدينة في 43 دولة، مع اختيار طريقة الحساب ومذهب العصر وتعديل التاريخ الهجري.',
        'هذه معاينة تفاعلية للشاشة: استخدم الأسهم ومفتاح Enter كأنها أزرار جهاز التحكم، أو اضغط بالفأرة. المواقيت محسوبة لمدينتك المختارة الآن.',
      ],
      en: [
        'Afnon replaces the stock launcher on an Android TV with one quiet screen. The next prayer and a live countdown sit inside a mihrab arch, the day’s six times run across the top, your favourite apps sit below, and the call to prayer comes to the TV by itself.',
        'Times are calculated on the TV, fully offline, for 147 cities in 43 countries, with your choice of calculation method, Asr school and Hijri date offset.',
        'This is a live preview of the screen: use the arrow keys and Enter like the remote, or click. The times are calculated for your chosen city right now.',
      ],
    },
  },
  {
    slug: 'prayer-day', screen: 'prayer-day',
    nav: { ar: 'مواقيت اليوم والقبلة', en: 'Today & Qibla' },
    title: { ar: 'مواقيت الصلاة اليوم والقبلة على التلفاز · أفنون', en: 'Today’s prayer times and Qibla on your TV · Afnon' },
    description: {
      ar: 'كل مواقيت اليوم مع الوقت المتبقي لكل صلاة، وجرس أذان لكل صلاة، واتجاه القبلة من مدينتك ومن مكان جلوسك أمام التلفاز.',
      en: 'Every prayer time today with its countdown, an adhan bell per prayer, and the Qibla from your city and from where you sit facing the TV.',
    },
    body: {
      ar: [
        'شاشة «مواقيت اليوم» تعرض الصلوات الست مع الوقت المتبقي لكل منها، وجرسًا يشغّل الأذان أو يوقفه لكل صلاة على حدة.',
        'البوصلة تعرض اتجاه القبلة من مدينتك. وبعد ضبط اتجاه الغرفة مرة واحدة، تقول لك الشاشة أين القبلة منك: أمامك أو خلفك أو عن يمينك.',
      ],
      en: [
        'Today’s prayers shows all six times with the time left to each, and a bell that turns the adhan on or off for each prayer.',
        'The compass shows the Qibla bearing from your city. Set which way the TV faces once, and the screen tells you where the Qibla is from your seat: ahead, behind or to one side.',
      ],
    },
  },
  {
    slug: 'reminder', screen: 'home', mode: 'reminder',
    nav: { ar: 'تذكير قبل الصلاة', en: 'Reminder' },
    title: { ar: 'تذكير قبل الصلاة على التلفاز · أفنون', en: 'A reminder before each prayer on your TV · Afnon' },
    description: {
      ar: 'قبل كل صلاة بدقائق تظهر بطاقة على الشاشة الرئيسية، أو شريط فوق أي تطبيق مفتوح، فلا تفوتك الصلاة وأنت تشاهد.',
      en: 'Minutes before each prayer a card appears on Home, or a banner over whatever app is open, so you never miss a prayer while watching.',
    },
    body: {
      ar: ['تختار في الإعدادات متى يظهر التذكير: قبل الصلاة بعشر دقائق أو بخمس عشرة، أو إيقافه. ويتحوّل إطار المحراب إلى الذهبي الفاتح في آخر ربع ساعة.'],
      en: ['Settings picks when the reminder comes: 10 or 15 minutes before the prayer, or off. The arch outline turns light gold in the last quarter hour.'],
    },
  },
  {
    slug: 'call', screen: 'call', mode: 'call',
    nav: { ar: 'الأذان', en: 'The call' },
    title: { ar: 'الأذان على التلفاز في وقته · أفنون', en: 'The adhan on your TV, on time · Afnon' },
    description: {
      ar: 'في وقت الصلاة تظهر شاشة الأذان فوق أي شيء يُعرض ويُرفع الأذان وحده، بـ 14 صوتًا وأذان خاص للفجر.',
      en: 'At prayer time the call comes up over whatever is playing and the adhan plays by itself, with 14 voices and a separate Fajr adhan.',
    },
    body: {
      ar: ['إذا كان التلفاز صامتًا أو صوته صفرًا يرتفع الصوت للأذان ثم يعود كما كان. وتُغلق الشاشة بزر الرجوع أو وحدها بعد عشر دقائق. (المعاينة على الويب لا تشغّل صوت الأذان.)'],
      en: ['If the TV is muted or at zero, the volume comes up for the adhan and goes back afterwards. Back dismisses it, or it closes by itself after ten minutes. (The web preview does not play the adhan audio.)'],
    },
  },
  {
    slug: 'friday', screen: 'home', mode: 'friday',
    nav: { ar: 'الجمعة', en: 'Friday' },
    title: { ar: 'وضع الجمعة: صلاة الجمعة وسورة الكهف · أفنون', en: 'Friday mode: Jumu’ah and Surat Al-Kahf · Afnon' },
    description: {
      ar: 'يوم الجمعة تحلّ صلاة الجمعة محل الظهر في المحراب وفي المواقيت، مع تذكير بقراءة سورة الكهف.',
      en: 'On Fridays Jumu’ah replaces Dhuhr in the arch and the times, with a reminder to read Surat Al-Kahf.',
    },
    body: {
      ar: ['يعمل وضع الجمعة وحده من التاريخ، دون أي إعداد.'],
      en: ['Friday mode switches itself on from the date, with nothing to set.'],
    },
  },
  {
    slug: 'ramadan', screen: 'home', mode: 'ramadan',
    nav: { ar: 'رمضان', en: 'Ramadan' },
    title: { ar: 'وضع رمضان: العد التنازلي للإفطار والسحور · أفنون', en: 'Ramadan mode: countdown to iftar and suhoor · Afnon' },
    description: {
      ar: 'في رمضان يعدّ المحراب تنازليًا إلى الإفطار، ويعرض موعد انتهاء السحور.',
      en: 'In Ramadan the arch counts down to iftar and shows when suhoor ends.',
    },
    body: {
      ar: ['يبدأ وضع رمضان وحده من التاريخ الهجري (أم القرى)، ويمكن تعديل التاريخ يومًا أو يومين في الإعدادات حسب رؤية الهلال في بلدك.'],
      en: ['Ramadan mode starts by itself from the Hijri date (Umm al-Qura), which Settings can shift a day or two to match the moon sighting where you live.'],
    },
  },
  {
    slug: 'apps', screen: 'apps',
    nav: { ar: 'كل التطبيقات', en: 'All apps' },
    title: { ar: 'كل التطبيقات · أفنون', en: 'All apps · Afnon' },
    description: {
      ar: 'كل التطبيقات المثبتة على التلفاز في شبكة واحدة، مع الصلاة القادمة في الأعلى. اضغط OK مطولًا لوضع تطبيق في المفضلة.',
      en: 'Every app on the TV in one grid, with the next prayer at the top. Hold OK to put an app in favourites.',
    },
    body: {
      ar: ['المعاينة تعرض أسماء تطبيقات للتوضيح؛ على التلفاز تظهر تطبيقاتك أنت.'],
      en: ['The preview shows example apps; on the TV you see your own.'],
    },
  },
  {
    slug: 'settings', screen: 'settings',
    nav: { ar: 'الإعدادات', en: 'Settings' },
    title: { ar: 'إعدادات أفنون: المدينة وطريقة الحساب والأذان والإذاعة', en: 'Afnon settings: city, method, adhan and radio' },
    description: {
      ar: 'اختر مدينتك وطريقة حساب المواقيت ومذهب العصر والتذكير وصوت الأذان ومحطة إذاعة القرآن الكريم واللغة والأرقام.',
      en: 'Choose your city, calculation method, Asr school, reminders, adhan voice, Quran radio station, language and digits.',
    },
    body: {
      ar: ['الإعدادات في المعاينة تعمل فعلًا: غيّر المدينة أو طريقة الحساب وارجع إلى الرئيسية لترى مواقيتك. تُحفظ في هذا المتصفح فقط.'],
      en: ['The settings in this preview work: change the city or method and go back Home to see your own times. They are saved in this browser only.'],
    },
  },
  {
    slug: 'screensaver', screen: 'screensaver',
    nav: { ar: 'شاشة التوقف', en: 'Screensaver' },
    title: { ar: 'شاشة التوقف: محراب خافت والصلاة القادمة · أفنون', en: 'Screensaver: a dim arch and the next prayer · Afnon' },
    description: {
      ar: 'شاشة توقف خافتة: محراب ينتقل ببطء مع الساعة والصلاة القادمة، والإذاعة إن كانت تعمل.',
      en: 'A dim screensaver: a slowly drifting arch with the clock and the next prayer, plus the radio when it plays.',
    },
    body: {
      ar: ['تتحرك كل دقيقة لتحمي الشاشة، وتبقى خافتة لتناسب الغرفة ليلًا.'],
      en: ['It moves every minute to protect the panel and stays dim for a dark room.'],
    },
  },
];

export const BASE = import.meta.env.BASE_URL.replace(/\/?$/, '/');
/** Site-relative URL of a page in a language: Arabic at the root, English under /en/. */
export const href = (slug: string, lang: Lang) => BASE + (lang === 'en' ? 'en/' : '') + (slug ? slug + '/' : '');
export const pageOf = (slug: string) => PAGES.find((p) => p.slug === slug)!;
