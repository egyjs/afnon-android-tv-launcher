package tv.afnon.home;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.CalculationParameters;
import com.batoulapps.adhan.Madhab;

/** Interface strings in Arabic, English and Indonesian, taken from the `t` tables in design/Home.dc.html. */
final class Text {
    final boolean ar;
    /** Indonesian; never together with {@link #ar}. */
    final boolean id;
    /** Eastern Arabic digits (٠١٢٣). Its own setting; defaults to on in Arabic. */
    final boolean eastern;

    Text(boolean ar, boolean eastern) {
        this.ar = ar;
        this.id = false;
        this.eastern = eastern;
    }

    Text(boolean ar, boolean id, boolean eastern) {
        this.ar = ar;
        this.id = id;
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
    String nextPrayer() { return ar ? "الصلاة القادمة" : id ? "SALAT BERIKUTNYA" : "NEXT PRAYER"; }
    String today() { return ar ? "مواقيت اليوم والقبلة" : id ? "Hari Ini & Kiblat" : "Today & Qibla"; }
    String settings() { return ar ? "الإعدادات" : id ? "Pengaturan" : "Settings"; }
    /** The saved city in this language; set by {@link Screen#loadText}. */
    String cityName = "";
    String city() { return cityName; }
    String fav() { return ar ? "المفضلة" : id ? "FAVORIT" : "FAVOURITES"; }
    String more() { return ar ? "المزيد" : id ? "LAINNYA" : "MORE"; }
    /** Home's tile: the label over the count. */
    String all(int n) { return d((ar ? "كل التطبيقات" : id ? "Semua aplikasi" : "All apps") + "\n" + n); }
    String ampm(boolean am) { return ar ? (am ? "ص" : "م") : (am ? "AM" : "PM"); }
    String adhanAt() { return ar ? "الأذان" : id ? "Azan pukul" : "Adhan at"; }
    String nowTag() { return ar ? "الآن" : id ? "SEKARANG" : "NOW"; }
    String nextTag() { return ar ? "التالية" : id ? "BERIKUTNYA" : "NEXT"; }
    String jumuahLabel() { return ar ? "صلاة الجمعة" : id ? "SALAT JUMAT" : "JUMU'AH"; }
    String jumuah() { return ar ? "الجمعة" : id ? "Jumat" : "Jumu'ah"; }
    String kahf() { return ar ? "سورة الكهف اليوم" : id ? "Surat Al-Kahfi hari ini" : "Surat Al-Kahf today"; }
    String iftarIn() { return ar ? "الإفطار بعد" : id ? "BUKA PUASA DALAM" : "IFTAR IN"; }
    String iftar() { return ar ? "الإفطار" : id ? "Buka puasa" : "Iftar"; }
    String iftarTag() { return ar ? "الإفطار" : id ? "BUKA PUASA" : "IFTAR"; }
    String suhoorTag() { return ar ? "الإمساك" : id ? "SAHUR" : "SUHOOR"; }
    String suhoorEnds(String time) { return d(ar ? "السحور حتى " + time : id ? "Sahur sampai " + time : "Suhoor ends " + time); }
    String comingSoon() { return ar ? "هذه الشاشة قادمة في التحديث التالي" : id ? "Layar ini hadir di pembaruan berikutnya" : "This screen comes in the next update"; }

    String hintOk(String what) { return "OK  " + what; }
    String hintApps() { return ar ? "◀  التطبيقات" : id ? "▶  Aplikasi" : "▶  Apps"; }
    String hintMore() { return ar ? "▼  المزيد" : id ? "▼  Lainnya" : "▼  More"; }
    String hintFavs() { return ar ? "▲  المفضلة" : id ? "▲  Favorit" : "▲  Favourites"; }
    String open(String app) { return ar ? "فتح " + app : id ? "Buka " + app : "Open " + app; }
    String network() { return ar ? "إعدادات الشبكة" : id ? "Pengaturan jaringan" : "Network settings"; }

    // Inputs panel (design/Inputs.dc.html)
    String inputs() { return ar ? "مصادر الإدخال" : id ? "Sumber masukan" : "Inputs"; }
    String inputSwitch() { return ar ? "تبديل" : id ? "Ganti" : "Switch"; }
    /** The panel's first row: the TV's own channels, from its built-in receiver. */
    String inputChannels() { return ar ? "قنوات التلفاز" : id ? "Saluran TV" : "TV channels"; }
    String inputOn() { return ar ? "متصل" : id ? "Terhubung" : "Connected"; }
    String inputOff() { return ar ? "غير متصل" : id ? "Tidak terhubung" : "Not connected"; }

    // Screensaver (design/Screensaver.dc.html)
    /**
     * "in 27 min". Arabic counts take their proper forms: دقيقة واحدة، دقيقتين، ٣–١٠ دقائق، ١١+ دقيقة. An hour
     * or more reads as h:mm, as on Prayer day.
     */
    String inMinutes(long minutes, String hoursAndMinutes) {
        if (minutes >= 60) return in(hoursAndMinutes);
        if (id) return d(minutes + " mnt lagi");
        if (!ar) return d("in " + minutes + " min");
        if (minutes <= 1) return "بعد دقيقة واحدة";
        if (minutes == 2) return "بعد دقيقتين";
        return d("بعد " + minutes + (minutes <= 10 ? " دقائق" : " دقيقة"));
    }

    // Reminder and call to prayer (design/Home.dc.html approach/now, design/ApproachVideo.dc.html)
    String apLabel() { return ar ? "اقتربت الصلاة" : id ? "SALAT SEGERA TIBA" : "PRAYER APPROACHING"; }
    /** "Asr in 13 minutes". */
    String apTitle(String prayer, long minutes, String hoursAndMinutes) {
        if (id && minutes < 60) return d(prayer + " " + minutes + " menit lagi");
        if (!ar && minutes < 60) return d(prayer + " in " + minutes + (minutes == 1 ? " minute" : " minutes"));
        return prayer + " " + inMinutes(minutes, hoursAndMinutes);
    }
    String apSub(CharSequence time) { return d((ar ? "الأذان " : id ? "Azan pukul " : "Adhan at ") + time + (ar ? " · رجوع للإغلاق" : id ? " · Kembali untuk menutup" : " · Back to dismiss")); }
    String apOk() { return ar ? "حسنًا" : id ? "Mengerti" : "Got it"; }
    String bannerSub(CharSequence time, int seconds) {
        return d((ar ? "الأذان " : id ? "Azan " : "Adhan ") + time + (ar ? " · يختفي خلال " + seconds + " ث" : id ? " · hilang dalam " + seconds + " dtk" : " · hides in " + seconds + " s"));
    }
    String nowLabel() { return ar ? "حان الآن وقت صلاة" : id ? "TELAH MASUK WAKTU" : "IT'S TIME FOR"; }
    String play() { return ar ? "تشغيل الأذان" : id ? "Putar azan" : "Play adhan"; }
    String stop() { return ar ? "إيقاف الأذان" : id ? "Hentikan azan" : "Stop adhan"; }
    String dismiss() { return ar ? "إغلاق" : id ? "Tutup" : "Dismiss"; }
    // Radio: Home's pill and card, the screen-off view, the screensaver line and the Settings section.
    String radio() { return ar ? "الإذاعة" : "Radio"; }
    String station(Radio.Station s) { return d(ar ? s.ar : s.en); }
    String stationWhere(Radio.Station s) { return d(ar ? s.whereAr : s.whereEn); }
    /** A station in a list: its name and place, since several are called إذاعة القرآن الكريم. */
    String stationChoice(Radio.Station s) { return s.reciter ? station(s) : station(s) + sep() + stationWhere(s); }
    String stationRow() { return ar ? "المحطة" : id ? "Stasiun" : "Station"; }
    String[] stationGroups() {
        return ar ? new String[] {"إذاعات القرآن الكريم", "القرّاء"} : id ? new String[] {"Stasiun radio Quran", "Qari"} : new String[] {"Quran radio stations", "Reciters"};
    }
    String radioStatus(int s) {
        switch (s) {
            case Radio.PLAYING: return ar ? "على الهواء" : id ? "Siaran" : "On air";
            case Radio.CONNECTING: return ar ? "جارٍ الاتصال…" : id ? "Menghubungkan…" : "Connecting…";
            case Radio.RETRYING: return ar ? "انقطع البث · نعيد المحاولة" : id ? "Sinyal hilang · mencoba lagi" : "Signal lost · retrying";
            case Radio.PAUSED: return ar ? "متوقفة للأذان" : id ? "Dijeda untuk azan" : "Paused for the adhan";
            default: return ar ? "متوقفة" : id ? "Mati" : "Off";
        }
    }
    String radioPlay() { return ar ? "تشغيل" : id ? "Putar" : "Play"; }
    String radioStop() { return ar ? "إيقاف" : id ? "Hentikan" : "Stop"; }
    String screenOff() { return ar ? "إطفاء الشاشة" : id ? "Matikan layar" : "Screen off"; }
    String radioNote() {
        return d(ar ? "تستمر في الرئيسية وشاشة التوقف · تتوقف إذا شغّل تطبيق آخر صوتًا"
                    : id ? "Tetap berjalan di Beranda dan screensaver · berhenti jika aplikasi lain memutar suara" : "Keeps playing on Home and the screensaver · stops when another app plays sound");
    }
    String screenOffNote() {
        return d(ar ? "الشاشة مطفأة والإذاعة تعمل · اضغط أي زر للعودة" : id ? "Layar mati, radio berjalan · tekan tombol apa saja untuk kembali" : "Screen off, radio playing · press any key to come back");
    }
    String hintClose() { return ar ? "رجوع  إغلاق" : id ? "Kembali  Tutup" : "Back  Close"; }
    String sectionRadio() { return ar ? "الإذاعة" : "Radio"; }
    String radioRowHelp(Radio.Station s) { return stationWhere(s) + (ar ? " · تحتاج إلى الإنترنت" : id ? " · perlu internet" : " · needs the internet"); }
    String afterAdhanRow() { return ar ? "بعد الأذان" : id ? "Setelah azan" : "After the adhan"; }
    String afterAdhanHelp() { return ar ? "تتوقف الإذاعة أثناء الأذان ثم…" : id ? "Radio dijeda saat azan, lalu…" : "The radio pauses for the adhan, then…"; }
    String[] afterAdhanChoices() { return ar ? new String[] {"تُستأنف", "تبقى متوقفة"} : id ? new String[] {"Lanjut", "Tetap mati"} : new String[] {"Resumes", "Stays off"}; }
    String radioDarkRow() { return ar ? "إطفاء الشاشة أثناء الاستماع" : id ? "Matikan layar saat mendengarkan" : "Screen off while listening"; }
    String radioDarkHelp() {
        return d(ar ? "توفير الكهرباء: تنطفئ الشاشة وتستمر الإذاعة · أي زر يعيدها"
                    : id ? "Hemat daya: layar mati dan radio tetap berjalan · tombol apa saja mengembalikannya" : "Saves power: the screen goes dark, the radio keeps playing · any key brings it back");
    }
    String[] radioDarkChoices() {
        return ar ? new String[] {"مع شاشة التوقف", "بعدها بـ 15 د", "أبدًا"} : id ? new String[] {"Dengan screensaver", "15 menit kemudian", "Tidak pernah"} : new String[] {"With screensaver", "15 min later", "Never"};
    }

    /** The call screen's hint: how it closes by itself (Settings → Reminders); 0 is when the adhan ends. */
    String nowHint(int minutes) {
        if (minutes == 0) return d(ar ? "رجوع للإغلاق · يُغلق تلقائيًا بعد الأذان" : id ? "Kembali untuk menutup · tertutup sendiri setelah azan" : "Back to dismiss · closes by itself after the adhan");
        return d(ar ? "رجوع للإغلاق · يُغلق تلقائيًا بعد " + (minutes == 1 ? "دقيقة" : minutes + " دقائق")
                    : id ? "Kembali untuk menutup · tertutup sendiri setelah " + minutes + " menit" : "Back to dismiss · closes by itself after " + minutes + " min");
    }
    String callCloseRow() { return ar ? "إغلاق شاشة الأذان" : id ? "Tutup layar azan" : "Close the call screen"; }
    String callCloseHelp() { return ar ? "ثم تعود إلى ما كنت تشاهده" : id ? "Lalu kembali ke tayangan Anda" : "Then back to what you were watching"; }
    /** In the order of the row's minutes: 0 (after the adhan), 1, 5, 10. */
    String[] callCloseChoices() {
        return ar ? new String[] {"بعد الأذان", "1 د", "5 د", "10 د"} : id ? new String[] {"Setelah azan", "1 mnt", "5 mnt", "10 mnt"} : new String[] {"After adhan", "1 min", "5 min", "10 min"};
    }
    String voiceRow() { return ar ? "صوت الأذان" : id ? "Suara azan" : "Adhan voice"; }
    String voiceHelp() { return ar ? "لكل الصلوات عدا الفجر · يُسمع عند التنقل في القائمة" : id ? "Semua salat kecuali Subuh · berbunyi saat Anda menelusuri daftar" : "Every prayer but Fajr · plays as you move through the list"; }
    String fajrVoiceRow() { return ar ? "أذان الفجر" : id ? "Azan Subuh" : "Fajr adhan"; }
    String fajrVoiceHelp() { return ar ? "يُفضَّل أذان فيه «الصلاة خير من النوم»" : id ? "Sebaiknya yang memuat “salat lebih baik daripada tidur”" : "Best with “prayer is better than sleep”"; }

    // All apps (design/AllApps.dc.html)
    String allTitle() { return ar ? "كل التطبيقات" : id ? "Semua aplikasi" : "All apps"; }
    String back() { return ar ? "رجوع" : id ? "Kembali" : "Back"; }
    /** "Asr in", followed by the countdown. */
    String chip(String prayer) { return ar ? prayer + " بعد" : id ? prayer + " dalam" : prayer + " in"; }
    String favNote(int position) {
        String[] ord = {"الأول", "الثاني", "الثالث", "الرابع", "الخامس", "السادس"};
        return ar ? "في المفضلة · " + ord[position - 1] : id ? d("Di favorit · posisi " + position) : d("In favourites · position " + position);
    }
    String notFav() { return ar ? "اضغط مطولًا على OK لوضعه في المفضلة" : id ? "Tahan OK untuk menambahkan ke favorit" : "Hold OK to put it in favourites"; }
    String hintOpen() { return ar ? "OK  فتح" : id ? "OK  Buka" : "OK  Open"; }
    String hintHold() { return ar ? "اضغط مطولًا  ترتيب المفضلة" : id ? "Tahan OK  Atur favorit" : "Hold OK  Arrange favourites"; }
    String moveEarlier() { return ar ? "تقديمه في المفضلة" : id ? "Majukan di favorit" : "Move earlier in favourites"; }
    String moveLater() { return ar ? "تأخيره في المفضلة" : id ? "Mundurkan di favorit" : "Move later in favourites"; }
    String putInPlaceOf(String app) { return ar ? "ضعه في المفضلة مكان " + app : id ? "Taruh di favorit menggantikan " + app : "Put in favourites instead of " + app; }
    String cancel() { return ar ? "إلغاء" : id ? "Batal" : "Cancel"; }
    String favourite() { return ar ? "مفضل" : id ? "Favorit" : "Favourite"; }

    // Prayer day (design/PrayerDay.dc.html)
    String dayTitle() { return ar ? "مواقيت اليوم" : id ? "Salat hari ini" : "Today's prayers"; }
    String home() { return ar ? "الرئيسية" : id ? "Beranda" : "Home"; }
    String school(Madhab m) {
        if (m == Madhab.HANAFI) return ar ? "العصر: الحنفي" : id ? "Asar: Hanafi" : "Asr: Hanafi";
        return ar ? "العصر: الجمهور (شافعي)" : id ? "Asar: standar (Syafi'i)" : "Asr: standard (Shafi‘i)";
    }

    String methodName(Method m) {
        switch (m.id) {
            case "UMM_AL_QURA": return ar ? "أم القرى، مكة المكرمة" : id ? "Ummul Qura, Makkah" : "Umm al-Qura, Makkah";
            case "MUSLIM_WORLD_LEAGUE": return ar ? "رابطة العالم الإسلامي" : id ? "Liga Muslim Dunia" : "Muslim World League";
            case "KARACHI": return ar ? "جامعة العلوم الإسلامية، كراتشي" : id ? "Universitas Ilmu Islam, Karachi" : "University of Islamic Sciences, Karachi";
            case "DUBAI": return ar ? "دبي" : "Dubai";
            case "KUWAIT": return ar ? "الكويت" : "Kuwait";
            case "QATAR": return ar ? "قطر" : "Qatar";
            case "NORTH_AMERICA": return ar ? "الجمعية الإسلامية لأمريكا الشمالية" : id ? "ISNA, Amerika Utara" : "ISNA, North America";
            case "MOON_SIGHTING_COMMITTEE": return ar ? "لجنة رؤية الهلال" : id ? "Komite Rukyatul Hilal" : "Moonsighting Committee";
            case "SINGAPORE": return ar ? "سنغافورة" : id ? "Singapura" : "Singapore";
            case "OMAN_AWQAF": return ar ? "وزارة الأوقاف والشؤون الدينية، سلطنة عُمان" : id ? "Kementerian Awqaf, Oman" : "Ministry of Awqaf, Oman";
            case "MOROCCO_HABOUS": return ar ? "وزارة الأوقاف والشؤون الإسلامية، المملكة المغربية" : id ? "Kementerian Habous, Maroko" : "Ministry of Habous, Morocco";
            default: return ar ? "الهيئة المصرية العامة للمساحة" : id ? "Otoritas Survei Umum Mesir" : "Egyptian General Authority of Survey";
        }
    }

    /**
     * "Fajr angle 19.5° · Isha 17.5°", or Isha as minutes after Maghrib where the method fixes it that way, and
     * the minutes a method adds to Dhuhr, Asr and Maghrib where it does.
     */
    String methodAngles(Method m) {
        CalculationParameters p = m.parameters();
        String fajr = (ar ? "زاوية الفجر " : id ? "Sudut Subuh " : "Fajr angle ") + trim(p.fajrAngle) + "°";
        String isha = p.ishaInterval > 0
            ? (ar ? "العشاء بعد المغرب بـ " + p.ishaInterval + " دقيقة" : id ? "Isya " + p.ishaInterval + " menit setelah Magrib" : "Isha " + p.ishaInterval + " min after Maghrib")
                // PrayerDay makes Umm al-Qura's 120 in Ramadan.
                + (m == Method.UMM_AL_QURA ? (ar ? " (120 في رمضان)" : id ? " (120 di Ramadan)" : " (120 in Ramadan)") : "")
            : (ar ? "العشاء " : id ? "Isya " : "Isha ") + trim(p.ishaAngle) + "°";
        int plus = m.dayAdjustment();
        String extra = m == Method.MOROCCO_HABOUS
            ? ar ? " · الظهر +5 دقائق · أفق كل مدينة" : id ? " · Zuhur +5 menit · horizon tiap kota" : " · Dhuhr +5 min · each city's horizon"
            : plus == 0 ? ""
            : ar ? " · +" + plus + " دقائق للظهر والعصر والمغرب" : id ? " · Zuhur, Asar, dan Magrib +" + plus + " menit" : " · Dhuhr, Asr and Maghrib +" + plus + " min";
        return d(fajr + " · " + isha + extra);
    }

    private static String trim(double v) {
        return v == Math.rint(v) ? String.valueOf((int) v) : String.valueOf(v);
    }

    // Settings (design/Settings.dc.html)
    String sectionPrayer() { return ar ? "مواقيت الصلاة" : id ? "Waktu salat" : "Prayer times"; }
    String sectionAdjust() { return ar ? "تعديل المواقيت" : id ? "Atur waktu" : "Adjust the times"; }
    /** A time's adjustment between its − and + buttons: "+2", "0", "-3" (minutes). */
    String minutesSigned(int n) {
        // Left-to-right embedding: in an Arabic line the sign would otherwise land after the number ("2+").
        return "‪" + d(n > 0 ? "+" + n : String.valueOf(n)) + "‬";
    }
    /** Under each time's name: what it is with the adjustment (Fajr is tomorrow's after Isha), and what the buttons do. */
    String adjustTimeHelp(String time, boolean tomorrow) {
        return d(ar ? (tomorrow ? "غدًا " : "اليوم ") + time + " · بالدقائق، حتى 30 قبله أو بعده"
                    : id ? (tomorrow ? "Besok " : "Hari ini ") + time + " · dalam menit, hingga 30 lebih awal atau lebih lambat" : (tomorrow ? "Tomorrow " : "Today ") + time + " · in minutes, up to 30 earlier or later");
    }
    String sectionReminders() { return ar ? "التنبيهات والأذان" : id ? "Pengingat & azan" : "Reminders & adhan"; }
    String sectionDisplay() { return ar ? "العرض واللغة" : id ? "Tampilan & bahasa" : "Display & language"; }
    String sectionNetwork(String ssid) { return ssid == null ? (ar ? "الشبكة" : id ? "Jaringan" : "Network") : (ar ? "الشبكة · " : id ? "Jaringan · " : "Network · ") + ssid; }
    String sectionDevice() { return ar ? "إعدادات الجهاز" : id ? "Pengaturan perangkat" : "Device settings"; }
    String sectionNews() { return ar ? "ما الجديد" : id ? "Yang baru" : "What's new"; }
    /** A release's heading on the What's new page: «أفنون 0.5» / "Afnon 0.5", in the chosen digits. */
    String newsVersion(String version) {
        return (ar ? "أفنون " : "Afnon ") + version(version);
    }
    /** A version number in the chosen digits, with the Arabic decimal mark beside Eastern digits. */
    String version(String v) { return eastern ? d(v).replace('.', '٫') : v; }
    /**
     * Settings → What's new, newest release first: each group is a version (its heading, newsVersion), then a few
     * short lines, the same lines in the same order in both languages. Each line goes through d(), so its numbers
     * follow the digits setting. Each release puts its group on top (gh.md, release step 3).
     */
    String[][] news() {
        if (ar) return new String[][] {
            {"0.9",
                "إعدادات الجهاز ← تحديث أفنون: يتحقّق من وجود إصدار جديد ويثبّته عند الضغط، ولا يتصل بالإنترنت وحده"},
            {"0.8.2",
                "قائمة مصادر الإدخال تبدأ بقنوات التلفاز (الريسيفر المدمج)، ولم يعد فيها مصدر AV"},
            {"0.8.1",
                "تلفاز شاومي: زر المصادر في الريموت يفتح قائمة مصادر الإدخال",
                "الأرقام الكبيرة بخط Marcellus مع الأرقام الغربية، والعدّ التنازلي ثابت لا يتحرك كل ثانية"},
            {"0.8",
                "تعديل المواقيت: كل وقت حتى 30 دقيقة قبله أو بعده، لمطابقة تقويم مسجدك",
                "التاريخ الهجري ويومه يتغيّران عند المغرب، ومعهما رمضان وتذكير سورة الكهف",
                "أم القرى: العشاء بعد المغرب بساعتين في رمضان",
                "المدن فوق خط العرض 48: الفجر والعشاء في حدود سُبع الليل من الشروق والمغرب",
                "أذانان جديدان: المسجد النبوي، وراغب مصطفى غلوش",
                "الوضع الليلي: ألوان أهدأ وأخفت للمشاهدة ليلًا، من الزر بجانب الإعدادات في الرئيسية "
                    + "أو من الإعدادات ← العرض واللغة",
                "أيقونات التطبيقات الأخرى أخفت ودافئة قليلًا في الوضع الليلي",
                "زر «المحطة» في بطاقة الإذاعة في الرئيسية",
                "من السعودية: إذاعة القرآن الكريم وإذاعة نداء الإسلام",
                "من عُمان: إذاعة القرآن الكريم",
                "المغرب: طريقة وزارة الأوقاف والشؤون الإسلامية و44 مدينة",
                "ثماني مدن أخرى في كندا منها هاليفاكس، وكوبنهاغن في الدنمارك",
                "زر «مصادر الإدخال» في الرئيسية، لأجهزة التحكم التي ليس فيها زر المصدر",
                "تلفاز TCL: زر المصدر في الريموت يُبقي شريط المصادر مفتوحًا",
                "القوائم لم تعد تقطع العنصر الأخير أو المحدد"},
            {"0.7",
                "عُمان: طريقة وزارة الأوقاف والشؤون الدينية، وإحدى عشرة مدينة أخرى",
                "إندونيسيا: 44 مدينة أخرى، وألباني في نيويورك",
                "يعود الأذان للعمل بعد أن يوقف تطبيق تنظيف أفنون",
                "لم تعد قائمة التطبيقات تعرض مداخل النظام الخاصة بالهاتف"},
            {"0.6",
                "مثبّت أفنون: ثبّت أفنون على التلفاز من هاتفك، دون حاسوب",
                "يعمل شاشةً رئيسية على Android TV وGoogle TV الأصليين",
                "لا يتوقف على تلفاز فيه أقل من أربعة تطبيقات"},
            {"0.5",
                "التنبيه قبل الصلاة بـ 5 أو 10 أو 15 أو 20 أو 30 دقيقة",
                "تذكير صوتي: «اقترب موعد صلاة …»",
                "«حان الآن موعد صلاة …» للصلاة التي أذانها متوقف",
                "تُغلق شاشة الأذان عند انتهائه، أو بعد 1 أو 5 أو 10 دقائق",
                "التاريخ الهجري أولًا، في الرئيسية ومواقيت اليوم",
                "مرجع الآية «النساء 103» بالأرقام المختارة"},
        };
        return new String[][] {
            {"0.9",
                "Device settings → Afnon updates: checks for a new version and installs it when you press OK, never online on its own"},
            {"0.8.2",
                "The Inputs list starts with TV channels (the built-in receiver), and AV is no longer in it"},
            {"0.8.1",
                "Xiaomi TVs: the remote's Input button opens the list of inputs",
                "With Western digits the big numbers are in Marcellus, and the countdown no longer shifts every second"},
            {"0.8",
                "Adjust the times: each up to 30 minutes earlier or later, to match your mosque's timetable",
                "The Hijri date and its weekday turn at Maghrib, and Ramadan and the Al-Kahf reminder with them",
                "Umm al-Qura: Isha 2 hours after Maghrib in Ramadan",
                "Cities above 48°: Fajr and Isha within a seventh of the night of sunrise and Maghrib",
                "Two new adhans: Al-Masjid an-Nabawi and Ragheb Mustafa Ghalwash",
                "Night mode: calmer, dimmer colours for watching at night, from the button beside Home's Settings or "
                    + "Settings → Display & language",
                "Other apps' icons dimmer and a little warm in night mode",
                "A Station button on Home's radio card",
                "From Saudi Arabia: Holy Quran Radio and Nida Al-Islam Radio",
                "From Oman: Holy Quran Radio",
                "Morocco: the Ministry of Habous's method and 44 cities",
                "Eight more cities in Canada, Halifax among them, and Copenhagen in Denmark",
                "An Inputs button on Home, for remotes without a Source button",
                "TCL: the remote's Source button keeps the input bar open",
                "Lists no longer cut off their last or selected item"},
            {"0.7",
                "Oman: the Ministry of Awqaf's method and eleven more Omani cities",
                "Indonesia: 44 more cities; and Albany, New York",
                "The call to prayer comes back on after a TV cleaner force-stops Afnon",
                "Phone-only system entries no longer show in the app list"},
            {"0.6",
                "Afnon Installer: set up Afnon on a TV from your phone, no computer needed",
                "Works as Home on stock Android TV and Google TV",
                "No crash on a TV with fewer than four apps"},
            {"0.5",
                "Reminders 5, 10, 15, 20 or 30 minutes before each prayer",
                "Spoken reminder: «اقترب موعد صلاة …»",
                "«حان الآن موعد صلاة …» when a prayer's adhan is off",
                "The call screen closes when the adhan ends, or after 1, 5 or 10 minutes",
                "The Hijri date first, on Home and Today's prayers",
                "The verse reference «النساء 103» in the digits you chose"},
        };
    }
    /** The same release notes in Indonesian; its own method so site/scripts/extract.mjs keeps reading news(). */
    String[][] newsId() {
        return new String[][] {
            {"0.9",
                "Setelan perangkat → Pembaruan Afnon: memeriksa versi baru dan memasangnya saat Anda menekan OK, tidak pernah online sendiri"},
            {"0.8.2",
                "Daftar Sumber dimulai dengan Saluran TV (receiver bawaan), dan AV tidak lagi ada di dalamnya"},
            {"0.8.1",
                "TV Xiaomi: tombol Sumber di remote membuka daftar sumber masukan",
                "Dengan angka Barat, angka besar memakai Marcellus, dan hitungan mundur tidak lagi bergeser tiap detik"},
            {"0.8",
                "Sesuaikan waktu: tiap waktu hingga 30 menit lebih awal atau lebih lambat, agar cocok dengan jadwal masjid Anda",
                "Tanggal Hijriah dan harinya berganti saat Magrib, begitu pula Ramadan dan pengingat Surat Al-Kahfi",
                "Ummul Qura: Isya dua jam setelah Magrib di bulan Ramadan",
                "Kota di atas 48°: Subuh dan Isya dalam batas sepertujuh malam dari terbit dan Magrib",
                "Dua azan baru: Masjid Nabawi dan Raghib Mustafa Ghalwash",
                "Mode malam: warna lebih tenang dan redup untuk menonton malam, dari tombol di samping Pengaturan di Beranda "
                    + "atau dari Pengaturan → Tampilan & bahasa",
                "Ikon aplikasi lain lebih redup dan sedikit hangat di mode malam",
                "Tombol «Stasiun» di kartu radio di Beranda",
                "Dari Arab Saudi: Radio Al-Quran dan Radio Nida Al-Islam",
                "Dari Oman: Radio Al-Quran",
                "Maroko: metode Kementerian Habous dan 44 kota",
                "Delapan kota lain di Kanada, termasuk Halifax, dan Kopenhagen di Denmark",
                "Tombol «Sumber masukan» di Beranda, untuk remote tanpa tombol Source",
                "TCL: tombol Source di remote membuat bilah masukan tetap terbuka",
                "Daftar tidak lagi memotong item terakhir atau yang dipilih"},
            {"0.7",
                "Oman: metode Kementerian Awqaf dan sebelas kota lain di Oman",
                "Indonesia: 44 kota lain; dan Albany, New York",
                "Azan berbunyi lagi setelah aplikasi pembersih TV menghentikan paksa Afnon",
                "Entri sistem khusus ponsel tidak lagi tampil di daftar aplikasi"},
            {"0.6",
                "Pemasang Afnon: pasang Afnon di TV dari ponsel, tanpa komputer",
                "Berfungsi sebagai Beranda di Android TV dan Google TV bawaan",
                "Tidak mogok di TV dengan kurang dari empat aplikasi"},
            {"0.5",
                "Pengingat 5, 10, 15, 20, atau 30 menit sebelum tiap salat",
                "Pengingat suara: «sudah dekat waktu salat …»",
                "«telah masuk waktu salat …» saat azan salat itu dimatikan",
                "Layar azan tertutup saat azan selesai, atau setelah 1, 5, atau 10 menit",
                "Tanggal Hijriah lebih dulu, di Beranda dan Salat hari ini",
                "Rujukan ayat «النساء 103» dengan angka pilihan Anda"},
        };
    }
    String preview() { return ar ? "العصر اليوم بهذه الإعدادات" : id ? "Asar hari ini dengan pengaturan ini" : "Asr today with these settings"; }
    String location() { return ar ? "الموقع" : id ? "Lokasi" : "Location"; }
    String country() { return ar ? "الدولة" : id ? "Negara" : "Country"; }
    String region() { return ar ? "المنطقة" : id ? "Wilayah" : "Region"; }
    /** Location's first step, in the order of Prefs.REGIONS. */
    String[] regions() {
        return ar ? new String[] {"الدول العربية", "آسيا", "أوروبا", "الأمريكتان", "أفريقيا", "أوقيانوسيا"}
            : id ? new String[] {"Dunia Arab", "Asia", "Eropa", "Amerika", "Afrika", "Oseania"}
            : new String[] {"Arab world", "Asia", "Europe", "Americas", "Africa", "Oceania"};
    }
    String locationHelp(double lat, double lng) {
        String ns = lat >= 0 ? (ar ? "ش" : id ? "LU" : "N") : (ar ? "ج" : id ? "LS" : "S");
        String ew = lng >= 0 ? (ar ? "ق" : id ? "BT" : "E") : (ar ? "غ" : id ? "BB" : "W");
        return d(String.format(java.util.Locale.US, "%.2f° %s · %.2f° %s", Math.abs(lat), ns, Math.abs(lng), ew)
            + (ar ? " · يُختار من القائمة" : id ? " · dipilih dari daftar" : " · chosen from the list"));
    }
    String place(String city, String country) { return city + (ar ? "، " : ", ") + country; }
    String method() { return ar ? "طريقة الحساب" : id ? "Metode perhitungan" : "Calculation method"; }
    String asrSchool() { return ar ? "مذهب العصر" : id ? "Mazhab Asar" : "Asr school"; }
    String asrSchoolHelp() { return ar ? "يغيّر وقت العصر فقط" : id ? "Hanya mengubah waktu Asar" : "Only changes the Asr time"; }
    String[] asrSchools() { return ar ? new String[] {"الجمهور", "الحنفي"} : id ? new String[] {"Standar", "Hanafi"} : new String[] {"Standard", "Hanafi"}; }
    String hijriAdjust() { return ar ? "تعديل التاريخ الهجري" : id ? "Penyesuaian tanggal Hijriah" : "Hijri date adjustment"; }
    String hijriToday(String date) { return (ar ? "اليوم: " : id ? "Hari ini: " : "Today: ") + date; }
    String days(int n) {
        String sign = n > 0 ? "+" : "";
        if (id) return d(sign + n + " hari");
        if (!ar) return d(sign + n + (Math.abs(n) == 1 ? " day" : " days"));
        return d(sign + n + (Math.abs(n) == 2 ? " يومان" : " يوم"));
    }
    String roomRow() { return roomTitle(); }
    String roomHelp() { return ar ? "لمعرفة القبلة بالنسبة لمكان جلوسك" : id ? "Agar kiblat ditampilkan dari posisi Anda duduk" : "So the Qibla is shown from where you sit"; }
    String notSet() { return ar ? "لم يُحدَّد" : id ? "Belum diatur" : "Not set"; }
    String remind() { return ar ? "التنبيه قبل الصلاة" : id ? "Ingatkan sebelum salat" : "Remind me before prayer"; }
    String remindHelp() { return ar ? "على الرئيسية وفوق الفيديو" : id ? "Di Beranda dan di atas video" : "On Home and over video"; }
    String[] remindChoices() {
        return ar ? new String[] {"5 د", "10 د", "15 د", "20 د", "30 د", "إيقاف"}
                  : id ? new String[] {"5 mnt", "10 mnt", "15 mnt", "20 mnt", "30 mnt", "Mati"} : new String[] {"5 min", "10 min", "15 min", "20 min", "30 min", "Off"};
    }
    String remindVoiceRow() { return ar ? "تذكير صوتي" : id ? "Pengingat suara" : "Spoken reminder"; }
    String remindVoiceHelp() { return ar ? "«اقترب موعد صلاة …» مع التذكير" : id ? "Mengucapkan «sudah dekat waktu salat …» bersama pengingat" : "Says «اقترب موعد صلاة …» with the reminder"; }
    String announceRow() { return ar ? "إعلان دخول الوقت" : id ? "Umumkan waktu" : "Announce the time"; }
    String announceHelp() {
        return ar ? "«حان الآن موعد صلاة …» للصلاة التي أذانها متوقف" : id ? "Mengucapkan «telah masuk waktu salat …» saat azan salat itu mati" : "Says «حان الآن موعد صلاة …» when a prayer's adhan is off";
    }
    String adhanRow() { return ar ? "الأذان" : id ? "Azan" : "Adhan"; }
    String adhanHelp() { return ar ? "يُشغَّل عند دخول الوقت · يمكن إيقافه لكل صلاة" : id ? "Berbunyi saat waktu masuk · bisa dimatikan per salat" : "Plays when the time begins · can be muted per prayer"; }
    String language() { return ar ? "اللغة" : id ? "Bahasa" : "Language"; }
    String languageHelp() { return ar ? "«التلفاز» يتبع لغة التلفاز" : id ? "“TV” mengikuti bahasa TV" : "“TV” follows the TV's language"; }
    String[] languages() { return new String[] {ar ? "التلفاز" : "TV", "العربية", "English", "Bahasa Indonesia"}; }
    String digits() { return ar ? "الأرقام" : id ? "Angka" : "Digits"; }
    String clockRow() { return ar ? "الساعة" : id ? "Jam" : "Clock"; }
    String clockHelp() { return ar ? "«التلفاز» يتبع إعداد الوقت في التلفاز" : id ? "“TV” mengikuti pengaturan waktu TV" : "“TV” follows the TV's time setting"; }
    String[] clocks() { return ar ? new String[] {"التلفاز", "12 س", "24 س"} : id ? new String[] {"TV", "12 jam", "24 jam"} : new String[] {"TV", "12 h", "24 h"}; }
    // Night mode: Settings → Display, and the moon button beside Home's Settings.
    String nightRow() { return ar ? "الوضع الليلي" : id ? "Mode malam" : "Night mode"; }
    String nightHelp() { return ar ? "ألوان أهدأ وأخفت للمشاهدة ليلًا" : id ? "Warna lebih tenang dan redup untuk menonton malam" : "Calmer, dimmer colours for watching at night"; }
    String nightOn() { return ar ? "تشغيل الوضع الليلي" : id ? "Mode malam aktif" : "Night mode on"; }
    String nightOff() { return ar ? "إيقاف الوضع الليلي" : id ? "Mode malam mati" : "Night mode off"; }
    String networkRow() { return ar ? "إعدادات الشبكة في التلفاز" : id ? "Pengaturan jaringan TV" : "TV network settings"; }
    String networkHelp() { return ar ? "واي فاي وإيثرنت" : id ? "Wi-Fi dan Ethernet" : "Wi-Fi and Ethernet"; }
    String deviceRow() { return ar ? "إعدادات التلفاز" : id ? "Pengaturan TV" : "TV settings"; }
    String deviceHelp() { return ar ? "الصورة والصوت والتطبيقات والنظام" : id ? "Gambar, suara, aplikasi, dan sistem" : "Picture, sound, apps and system"; }
    // Settings → Device → Afnon updates (Updates): Afnon contacts GitHub only when OK is pressed there.
    String updateRow() { return ar ? "تحديث أفنون" : id ? "Pembaruan Afnon" : "Afnon updates"; }
    String updateHelp(int state, int failure, String current, String latest) {
        String now = version(current);
        if (ar) {
            switch (state) {
                case Updates.LATEST: return "الإصدار " + now + " هو الأحدث";
                case Updates.AVAILABLE: return "الإصدار " + now + sep() + "الإصدار " + version(latest) + " متاح";
                case Updates.DOWNLOADING:
                case Updates.INSTALLING: return "يعود أفنون وحده بعد التثبيت";
                case Updates.NOT_ALLOWED: return "شغّل إعداد أفنون مرة أخرى للسماح بالتحديث";
                case Updates.FAILED:
                    if (failure == Updates.OFFLINE) return "تعذّر الوصول إلى GitHub، تحقّق من الإنترنت";
                    if (failure == Updates.DOWNLOAD) return "انقطع التنزيل";
                    return "رفض التلفاز تثبيت التحديث";
                default: return "الإصدار " + now + sep() + "يتصل بـ GitHub فقط عند الضغط على OK";
            }
        }
        if (id) {
            switch (state) {
                case Updates.LATEST: return "Versi " + now + " adalah yang terbaru";
                case Updates.AVAILABLE: return "Versi " + now + sep() + "Versi " + version(latest) + " tersedia";
                case Updates.DOWNLOADING:
                case Updates.INSTALLING: return "Afnon kembali sendiri setelah dipasang";
                case Updates.NOT_ALLOWED: return "Jalankan penyiapan Afnon lagi untuk mengizinkan pembaruan";
                case Updates.FAILED:
                    if (failure == Updates.OFFLINE) return "Tidak dapat menghubungi GitHub; periksa internet";
                    if (failure == Updates.DOWNLOAD) return "Unduhan terhenti";
                    return "TV menolak memasang pembaruan";
                default: return "Versi " + now + sep() + "menghubungi GitHub hanya saat Anda menekan OK";
            }
        }
        switch (state) {
            case Updates.LATEST: return "Version " + now + " is the latest";
            case Updates.AVAILABLE: return "Version " + now + sep() + version(latest) + " is out";
            case Updates.DOWNLOADING:
            case Updates.INSTALLING: return "Afnon comes back by itself once installed";
            case Updates.NOT_ALLOWED: return "Run Afnon's setup again to allow updates";
            case Updates.FAILED:
                if (failure == Updates.OFFLINE) return "Couldn't reach GitHub; check the internet";
                if (failure == Updates.DOWNLOAD) return "The download stopped";
                return "The TV refused the update";
            default: return "Version " + now + sep() + "contacts GitHub only when you press OK";
        }
    }
    String updateValue(int state, int percent, String latest) {
        if (ar) {
            switch (state) {
                case Updates.CHECKING: return "جارٍ التحقّق…";
                case Updates.LATEST: return "محدَّث";
                case Updates.AVAILABLE: return "تحديث إلى " + version(latest);
                case Updates.DOWNLOADING: return d("جارٍ التنزيل " + percent) + (eastern ? "٪" : "%");
                case Updates.INSTALLING: return "جارٍ التثبيت…";
                case Updates.FAILED:
                case Updates.NOT_ALLOWED: return "حاول مجددًا";
                default: return "تحقّق";
            }
        }
        if (id) {
            switch (state) {
                case Updates.CHECKING: return "Memeriksa…";
                case Updates.LATEST: return "Terbaru";
                case Updates.AVAILABLE: return "Perbarui ke " + version(latest);
                case Updates.DOWNLOADING: return "Mengunduh " + percent + "%";
                case Updates.INSTALLING: return "Memasang…";
                case Updates.FAILED:
                case Updates.NOT_ALLOWED: return "Coba lagi";
                default: return "Periksa";
            }
        }
        switch (state) {
            case Updates.CHECKING: return "Checking…";
            case Updates.LATEST: return "Up to date";
            case Updates.AVAILABLE: return "Update to " + version(latest);
            case Updates.DOWNLOADING: return d("Downloading " + percent) + (eastern ? "٪" : "%");
            case Updates.INSTALLING: return "Installing…";
            case Updates.FAILED:
            case Updates.NOT_ALLOWED: return "Try again";
            default: return "Check";
        }
    }
    String hintMove() { return ar ? "▲▼  تنقل" : id ? "▲▼  Pindah" : "▲▼  Move"; }
    String hintChange() { return ar ? "◀▶  تغيير القيمة" : id ? "◀▶  Ubah nilai" : "◀▶  Change value"; }
    String hintChoose() { return ar ? "OK  اختيار" : id ? "OK  Pilih" : "OK  Choose"; }
    String hintBackMenu() { return ar ? "رجوع  القائمة" : id ? "Kembali  Menu" : "Back  Menu"; }
    String hintEdit() { return ar ? "OK  تعديل" : id ? "OK  Ubah" : "OK  Edit"; }
    String hintSwitch() { return ar ? "OK  تشغيل/إيقاف" : id ? "OK  Hidup/Mati" : "OK  On/Off"; }
    String hintSave() { return ar ? "OK  حفظ" : id ? "OK  Simpan" : "OK  Save"; }
    String hintBackCancel() { return ar ? "رجوع  إلغاء" : id ? "Kembali  Batal" : "Back  Cancel"; }
    String saved() { return ar ? "✓  تم الحفظ" : id ? "✓  Tersimpan" : "✓  Saved"; }
    String qibla() { return ar ? "القبلة" : id ? "KIBLAT" : "QIBLA"; }
    String north() { return ar ? "الشمال ▲" : id ? "▲ Utara" : "▲ North"; }
    String qiblaHint() { return ar ? "وجّه الشمال على البوصلة نحو الشمال الحقيقي" : id ? "Sejajarkan utara kompas dengan utara sebenarnya" : "Line up the compass north with true north"; }
    String bellToggle() { return ar ? "تشغيل/إيقاف الأذان لهذه الصلاة" : id ? "Azan hidup/mati untuk salat ini" : "Adhan on/off for this prayer"; }
    String hintBackHome() { return ar ? "رجوع  الرئيسية" : id ? "Kembali  Beranda" : "Back  Home"; }
    String bell(boolean on) { return ar ? (on ? "الأذان مفعّل" : "الأذان متوقف") : id ? (on ? "Azan aktif" : "Azan mati") : (on ? "Adhan on" : "Adhan off"); }
    String rowNow() { return ar ? "الآن" : id ? "sekarang" : "now"; }
    /** Sunrise is masculine in Arabic (الشروق مضى); the prayers are feminine (الصلاة مضت). */
    String passed(boolean sunrise) { return ar ? (sunrise ? "مضى" : "مضت") : id ? "lewat" : "passed"; }
    String in(String duration) { return d(ar ? "بعد " + duration : id ? duration + " lagi" : "in " + duration); }

    /** "South-east · from Cairo" for a bearing in degrees from true north. */
    String qiblaDirection(double bearing) {
        return octant(bearing) + (ar ? " · من " : id ? " · dari " : " · from ") + city();
    }

    String octant(double bearing) {
        String[] names = ar ? new String[] {"شمال", "شمال شرق", "شرق", "جنوب شرق", "جنوب", "جنوب غرب", "غرب", "شمال غرب"}
                            : id ? new String[] {"Utara", "Timur Laut", "Timur", "Tenggara", "Selatan", "Barat Daya", "Barat", "Barat Laut"} : new String[] {"North", "North-east", "East", "South-east", "South", "South-west", "West", "North-west"};
        return names[sector(bearing)];
    }

    /**
     * Where the Qibla is for someone facing the TV, from its angle clockwise from straight ahead. Right and left
     * are the viewer's own, so they never mirror with the layout.
     */
    String qiblaRelative(double clockwise) {
        String[] where = ar
            ? new String[] {"أمامك", "أمامك إلى اليمين", "على يمينك", "خلفك إلى اليمين", "خلفك", "خلفك إلى اليسار", "على يسارك", "أمامك إلى اليسار"}
            : id ? new String[] {"di depan Anda", "di depan, ke kanan Anda", "di kanan Anda", "di belakang, ke kanan Anda",
                                 "di belakang Anda", "di belakang, ke kiri Anda", "di kiri Anda", "di depan, ke kiri Anda"}
            : new String[] {"ahead of you", "ahead, to your right", "to your right", "behind, to your right", "behind you",
                            "behind, to your left", "to your left", "ahead, to your left"};
        return (ar ? "القبلة " : id ? "Kiblat " : "Qibla is ") + where[sector(clockwise)];
    }

    /** "North-west · 320°". */
    String bearingLabel(int degrees) { return d(octant(degrees) + " · " + degrees + "°"); }

    String facingLine(int facing) { return (ar ? "تجلس مواجهًا: " : id ? "Anda menghadap: " : "You face: ") + bearingLabel(facing); }
    String tvMarker() { return ar ? "التلفاز ▲" : "▲ TV"; }
    String roomButton(boolean set) { return ar ? (set ? "تغيير اتجاه الغرفة" : "حدّد اتجاه الغرفة") : id ? (set ? "Ubah arah ruangan" : "Atur arah ruangan") : (set ? "Change room direction" : "Set room direction"); }
    String roomTitle() { return ar ? "اتجاه الغرفة" : id ? "Arah ruangan" : "Room direction"; }
    String roomTip() {
        return ar ? "قف مواجهًا للتلفاز، وافتح تطبيق البوصلة في هاتفك وأمسكه مستويًا أمامك، ثم اضبط الرقم هنا على ما تقرؤه. يكفي ذلك مرة واحدة ما دام التلفاز في مكانه."
                  : id ? "Berdiri menghadap TV, buka aplikasi kompas di ponsel Anda dan pegang mendatar di depan Anda, lalu atur angkanya di sini sesuai yang terbaca. Cukup sekali selama TV tetap di tempatnya."
                  : "Stand facing the TV, open the compass app on your phone and hold it flat in front of you, then set the number here to what it reads. Once is enough while the TV stays where it is.";
    }
    String roomKeys() { return ar ? "◀ ▶  ٥°     ▲ ▼  ٤٥°     OK  حفظ     رجوع  إلغاء" : id ? "◀ ▶  5°     ▲ ▼  45°     OK  Simpan     Kembali  Batal" : "◀ ▶  5°     ▲ ▼  45°     OK  Save     Back  Cancel"; }
    String hintQibla() { return ar ? "◀  القبلة" : id ? "▶  Kiblat" : "▶  Qibla"; }
    String hintTimes() { return ar ? "▶  المواقيت" : id ? "◀  Waktu" : "◀  Times"; }

    private static int sector(double degrees) {
        return (int) Math.round(((degrees % 360) + 360) % 360 / 45) % 8;
    }

    /** Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha. */
    String[] prayerNames() {
        return ar ? new String[] {"الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء"}
                  : id ? new String[] {"Subuh", "Syuruq", "Zuhur", "Asar", "Magrib", "Isya"}
                  : new String[] {"Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"};
    }

    String[] hijriMonths() {
        return ar ? new String[] {"محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
                                  "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"}
                  : id ? new String[] {"Muharam", "Safar", "Rabiulawal", "Rabiulakhir", "Jumadilawal",
                                       "Jumadilakhir", "Rajab", "Syaban", "Ramadan", "Syawal", "Zulkaidah",
                                       "Zulhijah"}
                  : new String[] {"Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani", "Jumada al-Ula",
                                  "Jumada al-Akhirah", "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhu al-Qadah",
                                  "Dhu al-Hijjah"};
    }

    String hijriSuffix() { return ar ? " هـ" : id ? " H" : " AH"; }
}
