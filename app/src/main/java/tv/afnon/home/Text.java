package tv.afnon.home;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.CalculationParameters;
import com.batoulapps.adhan.Madhab;

/** Interface strings for both locales, taken from the `t` tables in design/Home.dc.html. */
final class Text {
    final boolean ar;
    /** Eastern Arabic digits (٠١٢٣). Its own setting; defaults to on in Arabic. */
    final boolean eastern;

    Text(boolean ar, boolean eastern) {
        this.ar = ar;
        this.eastern = eastern;
    }

    String d(String s) {
        if (!eastern) return s;
        // "·" beside Eastern digits reads as a zero (٠); Arabic punctuation takes its own comma instead.
        s = s.replace(" · ", "، ");
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            b.append(c >= '0' && c <= '9' ? (char) ('٠' + (c - '0')) : c);
        }
        return b.toString();
    }

    /** The " · " between parts of a line; a comma where "·" would sit beside Eastern digits. */
    String sep() { return d(" · "); }

    /** The arch verse's reference (An-Nisa 4:103), in the screen's digits like every other number. */
    String ref() { return ar ? d("النساء 103") : d("An-Nisa 4:103"); }
    String nextPrayer() { return ar ? "الصلاة القادمة" : "NEXT PRAYER"; }
    String today() { return ar ? "مواقيت اليوم والقبلة" : "Today & Qibla"; }
    String settings() { return ar ? "الإعدادات" : "Settings"; }
    /** The saved city in this language; set by {@link Screen#loadText}. */
    String cityName = "";
    String city() { return cityName; }
    String fav() { return ar ? "المفضلة" : "FAVOURITES"; }
    String more() { return ar ? "المزيد" : "MORE"; }
    /** Home's tile: the label over the count. */
    String all(int n) { return d((ar ? "كل التطبيقات" : "All apps") + "\n" + n); }
    String ampm(boolean am) { return ar ? (am ? "ص" : "م") : (am ? "AM" : "PM"); }
    String adhanAt() { return ar ? "الأذان" : "Adhan at"; }
    String nowTag() { return ar ? "الآن" : "NOW"; }
    String nextTag() { return ar ? "التالية" : "NEXT"; }
    String jumuahLabel() { return ar ? "صلاة الجمعة" : "JUMU'AH"; }
    String jumuah() { return ar ? "الجمعة" : "Jumu'ah"; }
    String kahf() { return ar ? "سورة الكهف اليوم" : "Surat Al-Kahf today"; }
    String iftarIn() { return ar ? "الإفطار بعد" : "IFTAR IN"; }
    String iftar() { return ar ? "الإفطار" : "Iftar"; }
    String iftarTag() { return ar ? "الإفطار" : "IFTAR"; }
    String suhoorTag() { return ar ? "الإمساك" : "SUHOOR"; }
    String suhoorEnds(String time) { return d(ar ? "السحور حتى " + time : "Suhoor ends " + time); }
    String comingSoon() { return ar ? "هذه الشاشة قادمة في التحديث التالي" : "This screen comes in the next update"; }

    String hintOk(String what) { return "OK  " + what; }
    String hintApps() { return ar ? "◀  التطبيقات" : "▶  Apps"; }
    String hintMore() { return ar ? "▼  المزيد" : "▼  More"; }
    String hintFavs() { return ar ? "▲  المفضلة" : "▲  Favourites"; }
    String open(String app) { return ar ? "فتح " + app : "Open " + app; }
    String network() { return ar ? "إعدادات الشبكة" : "Network settings"; }

    // Inputs panel (design/Inputs.dc.html)
    String inputs() { return ar ? "مصادر الإدخال" : "Inputs"; }
    String inputSwitch() { return ar ? "تبديل" : "Switch"; }
    String inputOn() { return ar ? "متصل" : "Connected"; }
    String inputOff() { return ar ? "غير متصل" : "Not connected"; }

    // Screensaver (design/Screensaver.dc.html)
    /**
     * "in 27 min". Arabic counts take their proper forms: دقيقة واحدة، دقيقتين، ٣–١٠ دقائق، ١١+ دقيقة. An hour
     * or more reads as h:mm, as on Prayer day.
     */
    String inMinutes(long minutes, String hoursAndMinutes) {
        if (minutes >= 60) return in(hoursAndMinutes);
        if (!ar) return d("in " + minutes + " min");
        if (minutes <= 1) return "بعد دقيقة واحدة";
        if (minutes == 2) return "بعد دقيقتين";
        return d("بعد " + minutes + (minutes <= 10 ? " دقائق" : " دقيقة"));
    }

    // Reminder and call to prayer (design/Home.dc.html approach/now, design/ApproachVideo.dc.html)
    String apLabel() { return ar ? "اقتربت الصلاة" : "PRAYER APPROACHING"; }
    /** "Asr in 13 minutes". */
    String apTitle(String prayer, long minutes, String hoursAndMinutes) {
        if (!ar && minutes < 60) return d(prayer + " in " + minutes + (minutes == 1 ? " minute" : " minutes"));
        return prayer + " " + inMinutes(minutes, hoursAndMinutes);
    }
    String apSub(CharSequence time) { return d((ar ? "الأذان " : "Adhan at ") + time + (ar ? " · رجوع للإغلاق" : " · Back to dismiss")); }
    String apOk() { return ar ? "حسنًا" : "Got it"; }
    String bannerSub(CharSequence time, int seconds) {
        return d((ar ? "الأذان " : "Adhan ") + time + (ar ? " · يختفي خلال " + seconds + " ث" : " · hides in " + seconds + " s"));
    }
    String nowLabel() { return ar ? "حان الآن وقت صلاة" : "IT'S TIME FOR"; }
    String play() { return ar ? "تشغيل الأذان" : "Play adhan"; }
    String stop() { return ar ? "إيقاف الأذان" : "Stop adhan"; }
    String dismiss() { return ar ? "إغلاق" : "Dismiss"; }
    // Radio: Home's pill and card, the screen-off view, the screensaver line and the Settings section.
    String radio() { return ar ? "الإذاعة" : "Radio"; }
    String station(Radio.Station s) { return d(ar ? s.ar : s.en); }
    String stationWhere(Radio.Station s) { return d(ar ? s.whereAr : s.whereEn); }
    /** A station in a list: its name and place, since several are called إذاعة القرآن الكريم. */
    String stationChoice(Radio.Station s) { return s.reciter ? station(s) : station(s) + sep() + stationWhere(s); }
    String stationRow() { return ar ? "المحطة" : "Station"; }
    String[] stationGroups() {
        return ar ? new String[] {"إذاعات القرآن الكريم", "القرّاء"} : new String[] {"Quran radio stations", "Reciters"};
    }
    String radioStatus(int s) {
        switch (s) {
            case Radio.PLAYING: return ar ? "على الهواء" : "On air";
            case Radio.CONNECTING: return ar ? "جارٍ الاتصال…" : "Connecting…";
            case Radio.RETRYING: return ar ? "انقطع البث · نعيد المحاولة" : "Signal lost · retrying";
            case Radio.PAUSED: return ar ? "متوقفة للأذان" : "Paused for the adhan";
            default: return ar ? "متوقفة" : "Off";
        }
    }
    String radioPlay() { return ar ? "تشغيل" : "Play"; }
    String radioStop() { return ar ? "إيقاف" : "Stop"; }
    String screenOff() { return ar ? "إطفاء الشاشة" : "Screen off"; }
    String radioNote() {
        return d(ar ? "تستمر في الرئيسية وشاشة التوقف · تتوقف إذا شغّل تطبيق آخر صوتًا"
                    : "Keeps playing on Home and the screensaver · stops when another app plays sound");
    }
    String screenOffNote() {
        return d(ar ? "الشاشة مطفأة والإذاعة تعمل · اضغط أي زر للعودة" : "Screen off, radio playing · press any key to come back");
    }
    String hintClose() { return ar ? "رجوع  إغلاق" : "Back  Close"; }
    String sectionRadio() { return ar ? "الإذاعة" : "Radio"; }
    String radioRowHelp(Radio.Station s) { return stationWhere(s) + (ar ? " · تحتاج إلى الإنترنت" : " · needs the internet"); }
    String afterAdhanRow() { return ar ? "بعد الأذان" : "After the adhan"; }
    String afterAdhanHelp() { return ar ? "تتوقف الإذاعة أثناء الأذان ثم…" : "The radio pauses for the adhan, then…"; }
    String[] afterAdhanChoices() { return ar ? new String[] {"تُستأنف", "تبقى متوقفة"} : new String[] {"Resumes", "Stays off"}; }
    String radioDarkRow() { return ar ? "إطفاء الشاشة أثناء الاستماع" : "Screen off while listening"; }
    String radioDarkHelp() {
        return d(ar ? "توفير الكهرباء: تنطفئ الشاشة وتستمر الإذاعة · أي زر يعيدها"
                    : "Saves power: the screen goes dark, the radio keeps playing · any key brings it back");
    }
    String[] radioDarkChoices() {
        return ar ? new String[] {"مع شاشة التوقف", "بعدها بـ 15 د", "أبدًا"} : new String[] {"With screensaver", "15 min later", "Never"};
    }

    /** The call screen's hint: how it closes by itself (Settings → Reminders); 0 is when the adhan ends. */
    String nowHint(int minutes) {
        if (minutes == 0) return d(ar ? "رجوع للإغلاق · يُغلق تلقائيًا بعد الأذان" : "Back to dismiss · closes by itself after the adhan");
        return d(ar ? "رجوع للإغلاق · يُغلق تلقائيًا بعد " + (minutes == 1 ? "دقيقة" : minutes + " دقائق")
                    : "Back to dismiss · closes by itself after " + minutes + " min");
    }
    String callCloseRow() { return ar ? "إغلاق شاشة الأذان" : "Close the call screen"; }
    String callCloseHelp() { return ar ? "ثم تعود إلى ما كنت تشاهده" : "Then back to what you were watching"; }
    /** In the order of the row's minutes: 0 (after the adhan), 1, 5, 10. */
    String[] callCloseChoices() {
        return ar ? new String[] {"بعد الأذان", "1 د", "5 د", "10 د"} : new String[] {"After adhan", "1 min", "5 min", "10 min"};
    }
    String voiceRow() { return ar ? "صوت الأذان" : "Adhan voice"; }
    String voiceHelp() { return ar ? "لكل الصلوات عدا الفجر · يُسمع عند التنقل في القائمة" : "Every prayer but Fajr · plays as you move through the list"; }
    String fajrVoiceRow() { return ar ? "أذان الفجر" : "Fajr adhan"; }
    String fajrVoiceHelp() { return ar ? "يُفضَّل أذان فيه «الصلاة خير من النوم»" : "Best with “prayer is better than sleep”"; }

    // All apps (design/AllApps.dc.html)
    String allTitle() { return ar ? "كل التطبيقات" : "All apps"; }
    String back() { return ar ? "رجوع" : "Back"; }
    /** "Asr in", followed by the countdown. */
    String chip(String prayer) { return ar ? prayer + " بعد" : prayer + " in"; }
    String favNote(int position) {
        String[] ord = {"الأول", "الثاني", "الثالث", "الرابع"};
        return ar ? "في المفضلة · " + ord[position - 1] : d("In favourites · position " + position);
    }
    String notFav() { return ar ? "اضغط مطولًا على OK لوضعه في المفضلة" : "Hold OK to put it in favourites"; }
    String hintOpen() { return ar ? "OK  فتح" : "OK  Open"; }
    String hintHold() { return ar ? "اضغط مطولًا  ترتيب المفضلة" : "Hold OK  Arrange favourites"; }
    String moveEarlier() { return ar ? "تقديمه في المفضلة" : "Move earlier in favourites"; }
    String moveLater() { return ar ? "تأخيره في المفضلة" : "Move later in favourites"; }
    String putInPlaceOf(String app) { return ar ? "ضعه في المفضلة مكان " + app : "Put in favourites instead of " + app; }
    String cancel() { return ar ? "إلغاء" : "Cancel"; }
    String favourite() { return ar ? "مفضل" : "Favourite"; }

    // Prayer day (design/PrayerDay.dc.html)
    String dayTitle() { return ar ? "مواقيت اليوم" : "Today's prayers"; }
    String home() { return ar ? "الرئيسية" : "Home"; }
    String school(Madhab m) {
        if (m == Madhab.HANAFI) return ar ? "العصر: الحنفي" : "Asr: Hanafi";
        return ar ? "العصر: الجمهور (شافعي)" : "Asr: standard (Shafi‘i)";
    }

    String methodName(CalculationMethod m) {
        switch (m) {
            case UMM_AL_QURA: return ar ? "أم القرى، مكة المكرمة" : "Umm al-Qura, Makkah";
            case MUSLIM_WORLD_LEAGUE: return ar ? "رابطة العالم الإسلامي" : "Muslim World League";
            case KARACHI: return ar ? "جامعة العلوم الإسلامية، كراتشي" : "University of Islamic Sciences, Karachi";
            case DUBAI: return ar ? "دبي" : "Dubai";
            case KUWAIT: return ar ? "الكويت" : "Kuwait";
            case QATAR: return ar ? "قطر" : "Qatar";
            case NORTH_AMERICA: return ar ? "الجمعية الإسلامية لأمريكا الشمالية" : "ISNA, North America";
            case MOON_SIGHTING_COMMITTEE: return ar ? "لجنة رؤية الهلال" : "Moonsighting Committee";
            case SINGAPORE: return ar ? "سنغافورة" : "Singapore";
            default: return ar ? "الهيئة المصرية العامة للمساحة" : "Egyptian General Authority of Survey";
        }
    }

    /** "Fajr angle 19.5° · Isha 17.5°", or Isha as minutes after Maghrib where the method fixes it that way. */
    String methodAngles(CalculationMethod m) {
        CalculationParameters p = m.getParameters();
        String fajr = (ar ? "زاوية الفجر " : "Fajr angle ") + trim(p.fajrAngle) + "°";
        String isha = p.ishaInterval > 0
            ? (ar ? "العشاء بعد المغرب بـ " + p.ishaInterval + " دقيقة" : "Isha " + p.ishaInterval + " min after Maghrib")
            : (ar ? "العشاء " : "Isha ") + trim(p.ishaAngle) + "°";
        return d(fajr + " · " + isha);
    }

    private static String trim(double v) {
        return v == Math.rint(v) ? String.valueOf((int) v) : String.valueOf(v);
    }

    // Settings (design/Settings.dc.html)
    String sectionPrayer() { return ar ? "مواقيت الصلاة" : "Prayer times"; }
    String sectionReminders() { return ar ? "التنبيهات والأذان" : "Reminders & adhan"; }
    String sectionDisplay() { return ar ? "العرض واللغة" : "Display & language"; }
    String sectionNetwork(String ssid) { return ssid == null ? (ar ? "الشبكة" : "Network") : (ar ? "الشبكة · " : "Network · ") + ssid; }
    String sectionDevice() { return ar ? "إعدادات الجهاز" : "Device settings"; }
    String preview() { return ar ? "العصر اليوم بهذه الإعدادات" : "Asr today with these settings"; }
    String location() { return ar ? "الموقع" : "Location"; }
    String country() { return ar ? "الدولة" : "Country"; }
    String region() { return ar ? "المنطقة" : "Region"; }
    /** Location's first step, in the order of Prefs.REGIONS. */
    String[] regions() {
        return ar ? new String[] {"الدول العربية", "آسيا", "أوروبا", "الأمريكتان", "أفريقيا", "أوقيانوسيا"}
            : new String[] {"Arab world", "Asia", "Europe", "Americas", "Africa", "Oceania"};
    }
    String locationHelp(double lat, double lng) {
        String ns = lat >= 0 ? (ar ? "ش" : "N") : (ar ? "ج" : "S");
        String ew = lng >= 0 ? (ar ? "ق" : "E") : (ar ? "غ" : "W");
        return d(String.format(java.util.Locale.US, "%.2f° %s · %.2f° %s", Math.abs(lat), ns, Math.abs(lng), ew)
            + (ar ? " · يُختار من القائمة" : " · chosen from the list"));
    }
    String place(String city, String country) { return city + (ar ? "، " : ", ") + country; }
    String method() { return ar ? "طريقة الحساب" : "Calculation method"; }
    String asrSchool() { return ar ? "مذهب العصر" : "Asr school"; }
    String asrSchoolHelp() { return ar ? "يغيّر وقت العصر فقط" : "Only changes the Asr time"; }
    String[] asrSchools() { return ar ? new String[] {"الجمهور", "الحنفي"} : new String[] {"Standard", "Hanafi"}; }
    String hijriAdjust() { return ar ? "تعديل التاريخ الهجري" : "Hijri date adjustment"; }
    String hijriToday(String date) { return (ar ? "اليوم: " : "Today: ") + date; }
    String days(int n) {
        String sign = n > 0 ? "+" : "";
        if (!ar) return d(sign + n + (Math.abs(n) == 1 ? " day" : " days"));
        return d(sign + n + (Math.abs(n) == 2 ? " يومان" : " يوم"));
    }
    String roomRow() { return roomTitle(); }
    String roomHelp() { return ar ? "لمعرفة القبلة بالنسبة لمكان جلوسك" : "So the Qibla is shown from where you sit"; }
    String notSet() { return ar ? "لم يُحدَّد" : "Not set"; }
    String remind() { return ar ? "التنبيه قبل الصلاة" : "Remind me before prayer"; }
    String remindHelp() { return ar ? "على الرئيسية وفوق الفيديو" : "On Home and over video"; }
    String[] remindChoices() {
        return ar ? new String[] {"5 د", "10 د", "15 د", "20 د", "30 د", "إيقاف"}
                  : new String[] {"5 min", "10 min", "15 min", "20 min", "30 min", "Off"};
    }
    String remindVoiceRow() { return ar ? "تذكير صوتي" : "Spoken reminder"; }
    String remindVoiceHelp() { return ar ? "«اقترب موعد صلاة …» مع التذكير" : "Says «اقترب موعد صلاة …» with the reminder"; }
    String announceRow() { return ar ? "إعلان دخول الوقت" : "Announce the time"; }
    String announceHelp() {
        return ar ? "«حان الآن موعد صلاة …» للصلاة التي أذانها متوقف" : "Says «حان الآن موعد صلاة …» when a prayer's adhan is off";
    }
    String adhanRow() { return ar ? "الأذان" : "Adhan"; }
    String adhanHelp() { return ar ? "يُشغَّل عند دخول الوقت · يمكن إيقافه لكل صلاة" : "Plays when the time begins · can be muted per prayer"; }
    String language() { return ar ? "اللغة" : "Language"; }
    String languageHelp() { return ar ? "«التلفاز» يتبع لغة التلفاز" : "“TV” follows the TV's language"; }
    String[] languages() { return new String[] {ar ? "التلفاز" : "TV", "العربية", "English"}; }
    String digits() { return ar ? "الأرقام" : "Digits"; }
    String clockRow() { return ar ? "الساعة" : "Clock"; }
    String clockHelp() { return ar ? "«التلفاز» يتبع إعداد الوقت في التلفاز" : "“TV” follows the TV's time setting"; }
    String[] clocks() { return ar ? new String[] {"التلفاز", "12 س", "24 س"} : new String[] {"TV", "12 h", "24 h"}; }
    String networkRow() { return ar ? "إعدادات الشبكة في التلفاز" : "TV network settings"; }
    String networkHelp() { return ar ? "واي فاي وإيثرنت" : "Wi-Fi and Ethernet"; }
    String deviceRow() { return ar ? "إعدادات التلفاز" : "TV settings"; }
    String deviceHelp() { return ar ? "الصورة والصوت والتطبيقات والنظام" : "Picture, sound, apps and system"; }
    String hintMove() { return ar ? "▲▼  تنقل" : "▲▼  Move"; }
    String hintChange() { return ar ? "◀▶  تغيير القيمة" : "◀▶  Change value"; }
    String hintChoose() { return ar ? "OK  اختيار" : "OK  Choose"; }
    String hintBackMenu() { return ar ? "رجوع  القائمة" : "Back  Menu"; }
    String hintEdit() { return ar ? "OK  تعديل" : "OK  Edit"; }
    String hintSwitch() { return ar ? "OK  تشغيل/إيقاف" : "OK  On/Off"; }
    String hintSave() { return ar ? "OK  حفظ" : "OK  Save"; }
    String hintBackCancel() { return ar ? "رجوع  إلغاء" : "Back  Cancel"; }
    String saved() { return ar ? "✓  تم الحفظ" : "✓  Saved"; }
    String qibla() { return ar ? "القبلة" : "QIBLA"; }
    String north() { return ar ? "الشمال ▲" : "▲ North"; }
    String qiblaHint() { return ar ? "وجّه الشمال على البوصلة نحو الشمال الحقيقي" : "Line up the compass north with true north"; }
    String bellToggle() { return ar ? "تشغيل/إيقاف الأذان لهذه الصلاة" : "Adhan on/off for this prayer"; }
    String hintBackHome() { return ar ? "رجوع  الرئيسية" : "Back  Home"; }
    String bell(boolean on) { return ar ? (on ? "الأذان مفعّل" : "الأذان متوقف") : (on ? "Adhan on" : "Adhan off"); }
    String rowNow() { return ar ? "الآن" : "now"; }
    /** Sunrise is masculine in Arabic (الشروق مضى); the prayers are feminine (الصلاة مضت). */
    String passed(boolean sunrise) { return ar ? (sunrise ? "مضى" : "مضت") : "passed"; }
    String in(String duration) { return d(ar ? "بعد " + duration : "in " + duration); }

    /** "South-east · from Cairo" for a bearing in degrees from true north. */
    String qiblaDirection(double bearing) {
        return octant(bearing) + (ar ? " · من " : " · from ") + city();
    }

    String octant(double bearing) {
        String[] names = ar ? new String[] {"شمال", "شمال شرق", "شرق", "جنوب شرق", "جنوب", "جنوب غرب", "غرب", "شمال غرب"}
                            : new String[] {"North", "North-east", "East", "South-east", "South", "South-west", "West", "North-west"};
        return names[sector(bearing)];
    }

    /**
     * Where the Qibla is for someone facing the TV, from its angle clockwise from straight ahead. Right and left
     * are the viewer's own, so they never mirror with the layout.
     */
    String qiblaRelative(double clockwise) {
        String[] where = ar
            ? new String[] {"أمامك", "أمامك إلى اليمين", "على يمينك", "خلفك إلى اليمين", "خلفك", "خلفك إلى اليسار", "على يسارك", "أمامك إلى اليسار"}
            : new String[] {"ahead of you", "ahead, to your right", "to your right", "behind, to your right", "behind you",
                            "behind, to your left", "to your left", "ahead, to your left"};
        return (ar ? "القبلة " : "Qibla is ") + where[sector(clockwise)];
    }

    /** "North-west · 320°". */
    String bearingLabel(int degrees) { return d(octant(degrees) + " · " + degrees + "°"); }

    String facingLine(int facing) { return (ar ? "تجلس مواجهًا: " : "You face: ") + bearingLabel(facing); }
    String tvMarker() { return ar ? "التلفاز ▲" : "▲ TV"; }
    String roomButton(boolean set) { return ar ? (set ? "تغيير اتجاه الغرفة" : "حدّد اتجاه الغرفة") : (set ? "Change room direction" : "Set room direction"); }
    String roomTitle() { return ar ? "اتجاه الغرفة" : "Room direction"; }
    String roomTip() {
        return ar ? "قف مواجهًا للتلفاز، وافتح تطبيق البوصلة في هاتفك وأمسكه مستويًا أمامك، ثم اضبط الرقم هنا على ما تقرؤه. يكفي ذلك مرة واحدة ما دام التلفاز في مكانه."
                  : "Stand facing the TV, open the compass app on your phone and hold it flat in front of you, then set the number here to what it reads. Once is enough while the TV stays where it is.";
    }
    String roomKeys() { return ar ? "◀ ▶  ٥°     ▲ ▼  ٤٥°     OK  حفظ     رجوع  إلغاء" : "◀ ▶  5°     ▲ ▼  45°     OK  Save     Back  Cancel"; }
    String hintQibla() { return ar ? "◀  القبلة" : "▶  Qibla"; }
    String hintTimes() { return ar ? "▶  المواقيت" : "◀  Times"; }

    private static int sector(double degrees) {
        return (int) Math.round(((degrees % 360) + 360) % 360 / 45) % 8;
    }

    /** Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha. */
    String[] prayerNames() {
        return ar ? new String[] {"الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء"}
                  : new String[] {"Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"};
    }

    String[] hijriMonths() {
        return ar ? new String[] {"محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
                                  "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"}
                  : new String[] {"Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani", "Jumada al-Ula",
                                  "Jumada al-Akhirah", "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhu al-Qadah",
                                  "Dhu al-Hijjah"};
    }

    String hijriSuffix() { return ar ? " هـ" : " AH"; }
}
