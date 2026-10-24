package tv.afnon.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateFormat;

import com.batoulapps.adhan.Coordinates;
import com.batoulapps.adhan.Madhab;

/**
 * Afnon's own settings (SharedPreferences "afnon"), read by every screen and the screensaver. Language and digits
 * live in {@link Screen#loadText}; the room direction in {@link Screen#facing()}.
 */
final class Prefs {
    private Prefs() {}

    static SharedPreferences of(Context c) {
        return c.getSharedPreferences("afnon", Context.MODE_PRIVATE);
    }

    // ---------------------------------------------------------------- location

    static final class City {
        final String id, ar, en, countryAr, countryEn;
        final double lat, lng;
        /** The method that city's mosques follow; picked for it when the city changes. */
        final Method method;
        /** The Asr school most of its mosques follow, also picked when the city changes. */
        final Madhab madhab;

        City(String id, String ar, String en, String countryAr, String countryEn, double lat, double lng, Method method) {
            this(id, ar, en, countryAr, countryEn, lat, lng, method, Madhab.SHAFI);
        }

        City(String id, String ar, String en, String countryAr, String countryEn, double lat, double lng, Method method, Madhab madhab) {
            this.id = id;
            this.ar = ar;
            this.en = en;
            this.countryAr = countryAr;
            this.countryEn = countryEn;
            this.lat = lat;
            this.lng = lng;
            this.method = method;
            this.madhab = madhab;
        }
    }

    private static final Method EG = Method.EGYPTIAN;
    private static final Method MWL = Method.MUSLIM_WORLD_LEAGUE;
    private static final Method IN = Method.KARACHI;
    private static final Method SG = Method.SINGAPORE;
    private static final Method NA = Method.NORTH_AMERICA;
    /** Oman's Ministry of Awqaf: 18° Fajr and Isha, +5 min on Dhuhr, Asr and Maghrib (see {@link Method#OMAN_AWQAF}). */
    private static final Method OM = Method.OMAN_AWQAF;
    /** Hanafi Asr (about an hour later), the usual school in Pakistan, Bangladesh and most of North India. */
    private static final Madhab HANAFI = Madhab.HANAFI;
    static final City[] CITIES = {
        new City("cairo", "القاهرة", "Cairo", "مصر", "Egypt", 30.0444, 31.2357, EG),
        new City("giza", "الجيزة", "Giza", "مصر", "Egypt", 30.0131, 31.2089, EG),
        new City("alexandria", "الإسكندرية", "Alexandria", "مصر", "Egypt", 31.2001, 29.9187, EG),
        new City("mansoura", "المنصورة", "Mansoura", "مصر", "Egypt", 31.0409, 31.3785, EG),
        new City("tanta", "طنطا", "Tanta", "مصر", "Egypt", 30.7865, 31.0004, EG),
        new City("portsaid", "بورسعيد", "Port Said", "مصر", "Egypt", 31.2653, 32.3019, EG),
        new City("suez", "السويس", "Suez", "مصر", "Egypt", 29.9668, 32.5498, EG),
        new City("asyut", "أسيوط", "Asyut", "مصر", "Egypt", 27.1783, 31.1859, EG),
        new City("luxor", "الأقصر", "Luxor", "مصر", "Egypt", 25.6872, 32.6396, EG),
        new City("aswan", "أسوان", "Aswan", "مصر", "Egypt", 24.0889, 32.8998, EG),
        new City("makkah", "مكة المكرمة", "Makkah", "السعودية", "Saudi Arabia", 21.3891, 39.8579, Method.UMM_AL_QURA),
        new City("madinah", "المدينة المنورة", "Madinah", "السعودية", "Saudi Arabia", 24.5247, 39.5692, Method.UMM_AL_QURA),
        new City("riyadh", "الرياض", "Riyadh", "السعودية", "Saudi Arabia", 24.7136, 46.6753, Method.UMM_AL_QURA),
        new City("jeddah", "جدة", "Jeddah", "السعودية", "Saudi Arabia", 21.5292, 39.1611, Method.UMM_AL_QURA),
        new City("dammam", "الدمام", "Dammam", "السعودية", "Saudi Arabia", 26.4333, 50.1000, Method.UMM_AL_QURA),
        new City("taif", "الطائف", "Taif", "السعودية", "Saudi Arabia", 21.2667, 40.4167, Method.UMM_AL_QURA),
        new City("tabuk", "تبوك", "Tabuk", "السعودية", "Saudi Arabia", 28.3972, 36.5789, Method.UMM_AL_QURA),
        new City("abha", "أبها", "Abha", "السعودية", "Saudi Arabia", 18.2167, 42.5000, Method.UMM_AL_QURA),
        new City("dubai", "دبي", "Dubai", "الإمارات", "UAE", 25.2048, 55.2708, Method.DUBAI),
        new City("abudhabi", "أبو ظبي", "Abu Dhabi", "الإمارات", "UAE", 24.4511, 54.3969, Method.DUBAI),
        new City("sharjah", "الشارقة", "Sharjah", "الإمارات", "UAE", 25.3375, 55.4119, Method.DUBAI),
        new City("alain", "العين", "Al Ain", "الإمارات", "UAE", 24.2075, 55.7447, Method.DUBAI),
        new City("kuwait", "الكويت", "Kuwait City", "الكويت", "Kuwait", 29.3759, 47.9774, Method.KUWAIT),
        new City("doha", "الدوحة", "Doha", "قطر", "Qatar", 25.2854, 51.5310, Method.QATAR),
        new City("amman", "عمّان", "Amman", "الأردن", "Jordan", 31.9539, 35.9106, MWL),
        new City("irbid", "إربد", "Irbid", "الأردن", "Jordan", 32.5556, 35.8500, MWL),
        new City("zarqa", "الزرقاء", "Zarqa", "الأردن", "Jordan", 32.0833, 36.1000, MWL),
        new City("aqaba", "العقبة", "Aqaba", "الأردن", "Jordan", 29.5322, 35.0061, MWL),
        new City("jerusalem", "القدس", "Jerusalem", "فلسطين", "Palestine", 31.7789, 35.2256, MWL),
        new City("ramallah", "رام الله", "Ramallah", "فلسطين", "Palestine", 31.8969, 35.2017, MWL),
        new City("nablus", "نابلس", "Nablus", "فلسطين", "Palestine", 32.2211, 35.2608, MWL),
        new City("hebron", "الخليل", "Hebron", "فلسطين", "Palestine", 31.5286, 35.0969, MWL),
        new City("bethlehem", "بيت لحم", "Bethlehem", "فلسطين", "Palestine", 31.7044, 35.2064, MWL),
        new City("jenin", "جنين", "Jenin", "فلسطين", "Palestine", 32.4611, 35.3000, MWL),
        new City("tulkarm", "طولكرم", "Tulkarm", "فلسطين", "Palestine", 32.3111, 35.0308, MWL),
        new City("qalqilya", "قلقيلية", "Qalqilya", "فلسطين", "Palestine", 32.1904, 34.9685, MWL),
        new City("salfit", "سلفيت", "Salfit", "فلسطين", "Palestine", 32.0819, 35.1822, MWL),
        new City("tubas", "طوباس", "Tubas", "فلسطين", "Palestine", 32.3222, 35.3686, MWL),
        new City("jericho", "أريحا", "Jericho", "فلسطين", "Palestine", 31.8561, 35.4600, MWL),
        new City("gaza", "غزة", "Gaza", "فلسطين", "Palestine", 31.5075, 34.4597, MWL),
        new City("jabalia", "جباليا", "Jabalia", "فلسطين", "Palestine", 31.5281, 34.4831, MWL),
        new City("deiralbalah", "دير البلح", "Deir al-Balah", "فلسطين", "Palestine", 31.4167, 34.3500, MWL),
        new City("khanyunis", "خان يونس", "Khan Yunis", "فلسطين", "Palestine", 31.3444, 34.3031, MWL),
        new City("rafah", "رفح", "Rafah", "فلسطين", "Palestine", 31.2725, 34.2586, MWL),
        new City("beirut", "بيروت", "Beirut", "لبنان", "Lebanon", 33.8869, 35.5131, MWL),
        new City("tripoli", "طرابلس", "Tripoli", "لبنان", "Lebanon", 34.4367, 35.8344, MWL),
        new City("sidon", "صيدا", "Sidon", "لبنان", "Lebanon", 33.5606, 35.3758, MWL),
        new City("tyre", "صور", "Tyre", "لبنان", "Lebanon", 33.2667, 35.2000, MWL),
        new City("baalbek", "بعلبك", "Baalbek", "لبنان", "Lebanon", 34.0061, 36.2086, MWL),
        new City("damascus", "دمشق", "Damascus", "سوريا", "Syria", 33.5131, 36.2919, MWL),
        new City("aleppo", "حلب", "Aleppo", "سوريا", "Syria", 36.2000, 37.1600, MWL),
        new City("homs", "حمص", "Homs", "سوريا", "Syria", 34.7333, 36.7167, MWL),
        new City("hama", "حماة", "Hama", "سوريا", "Syria", 35.1350, 36.7500, MWL),
        new City("latakia", "اللاذقية", "Latakia", "سوريا", "Syria", 35.5200, 35.7781, MWL),
        new City("deirezzor", "دير الزور", "Deir ez-Zor", "سوريا", "Syria", 35.3333, 40.1500, MWL),
        new City("baghdad", "بغداد", "Baghdad", "العراق", "Iraq", 33.3153, 44.3661, MWL),
        new City("basra", "البصرة", "Basra", "العراق", "Iraq", 30.5150, 47.8100, MWL),
        new City("mosul", "الموصل", "Mosul", "العراق", "Iraq", 36.3417, 43.1292, MWL),
        new City("erbil", "أربيل", "Erbil", "العراق", "Iraq", 36.1911, 44.0094, MWL),
        new City("najaf", "النجف", "Najaf", "العراق", "Iraq", 32.0290, 44.3396, MWL),
        new City("karbala", "كربلاء", "Karbala", "العراق", "Iraq", 32.6167, 44.0333, MWL),
        new City("kirkuk", "كركوك", "Kirkuk", "العراق", "Iraq", 35.4667, 44.4000, MWL),
        new City("sulaymaniyah", "السليمانية", "Sulaymaniyah", "العراق", "Iraq", 35.5500, 45.4333, MWL),
        new City("manama", "المنامة", "Manama", "البحرين", "Bahrain", 26.2167, 50.5833, MWL),
        new City("muharraq", "المحرق", "Muharraq", "البحرين", "Bahrain", 26.2533, 50.6083, MWL),
        // The ministry's Muscat timetable matches a point near Seeb (58.28°E), about 1.3 min west of the old centre.
        new City("muscat", "مسقط", "Muscat", "عُمان", "Oman", 23.6139, 58.2800, OM),
        new City("salalah", "صلالة", "Salalah", "عُمان", "Oman", 17.0197, 54.0897, OM),
        new City("sohar", "صحار", "Sohar", "عُمان", "Oman", 24.3420, 56.7299, OM),
        new City("nizwa", "نزوى", "Nizwa", "عُمان", "Oman", 22.9333, 57.5333, OM),
        new City("barka", "بركاء", "Barka", "عُمان", "Oman", 23.6807, 57.8860, OM),
        new City("rustaq", "الرستاق", "Rustaq", "عُمان", "Oman", 23.3908, 57.4244, OM),
        new City("sur", "صور", "Sur", "عُمان", "Oman", 22.5667, 59.5289, OM),
        new City("ibri", "عبري", "Ibri", "عُمان", "Oman", 23.2257, 56.5157, OM),
        new City("buraimi", "البريمي", "Al Buraimi", "عُمان", "Oman", 24.2500, 55.7933, OM),
        new City("khasab", "خصب", "Khasab", "عُمان", "Oman", 26.1800, 56.2400, OM),
        new City("duqm", "الدقم", "Duqm", "عُمان", "Oman", 19.6667, 57.7050, OM),
        new City("bahla", "بهلاء", "Bahla", "عُمان", "Oman", 22.9667, 57.3000, OM),
        new City("ibra", "إبراء", "Ibra", "عُمان", "Oman", 22.6900, 58.5300, OM),
        new City("samail", "سمائل", "Samail", "عُمان", "Oman", 23.3033, 57.9833, OM),
        new City("adam", "أدم", "Adam", "عُمان", "Oman", 22.3799, 57.5260, OM),
        new City("sanaa", "صنعاء", "Sanaa", "اليمن", "Yemen", 15.3500, 44.2000, MWL),
        new City("aden", "عدن", "Aden", "اليمن", "Yemen", 12.8000, 45.0333, MWL),
        new City("taiz", "تعز", "Taiz", "اليمن", "Yemen", 13.5789, 44.0219, MWL),
        new City("hodeidah", "الحديدة", "Hodeidah", "اليمن", "Yemen", 14.8022, 42.9511, MWL),
        new City("mukalla", "المكلا", "Mukalla", "اليمن", "Yemen", 14.5333, 49.1333, MWL),
        // United States & Canada
        new City("newyork", "نيويورك", "New York", "الولايات المتحدة", "United States", 40.7128, -74.0060, NA),
        new City("chicago", "شيكاغو", "Chicago", "الولايات المتحدة", "United States", 41.8781, -87.6298, NA),
        new City("dallas", "دالاس", "Dallas", "الولايات المتحدة", "United States", 32.7767, -96.7970, NA),
        new City("houston", "هيوستن", "Houston", "الولايات المتحدة", "United States", 29.7604, -95.3698, NA),
        new City("losangeles", "لوس أنجلوس", "Los Angeles", "الولايات المتحدة", "United States", 34.0522, -118.2437, NA),
        new City("toronto", "تورونتو", "Toronto", "كندا", "Canada", 43.6532, -79.3832, NA),

        // India
        new City("bangalore", "بنغالور", "Bengaluru", "الهند", "India", 12.9716, 77.5946, IN, HANAFI),
        new City("kozhikode", "كوزيكود", "Kozhikode", "الهند", "India", 11.2588, 75.7804, IN),
        new City("mumbai", "مومباي", "Mumbai", "الهند", "India", 19.0760, 72.8777, IN, HANAFI),
        new City("delhi", "دلهي", "Delhi", "الهند", "India", 28.6139, 77.2090, IN, HANAFI),
        new City("hyderabad", "حيدر أباد", "Hyderabad", "الهند", "India", 17.3850, 78.4867, IN, HANAFI),
        new City("chennai", "تشيناي", "Chennai", "الهند", "India", 13.0827, 80.2707, IN),
        new City("kolkata", "كولكاتا", "Kolkata", "الهند", "India", 22.5726, 88.3639, IN, HANAFI),
        new City("ahmedabad", "أحمد أباد", "Ahmedabad", "الهند", "India", 23.0225, 72.5714, IN, HANAFI),
        new City("pune", "بونه", "Pune", "الهند", "India", 18.5204, 73.8567, IN, HANAFI),
        new City("lucknow", "لكهنؤ", "Lucknow", "الهند", "India", 26.8467, 80.9462, IN, HANAFI),
        new City("srinagar", "سرينغار", "Srinagar", "الهند", "India", 34.0837, 74.7973, IN, HANAFI),
        new City("kochi", "كوتشي", "Kochi", "الهند", "India", 9.9312, 76.2673, IN),
        new City("malappuram", "ملابورام", "Malappuram", "الهند", "India", 11.0510, 76.0711, IN),
        new City("bhopal", "بوبال", "Bhopal", "الهند", "India", 23.2599, 77.4126, IN, HANAFI),
        new City("patna", "باتنا", "Patna", "الهند", "India", 25.5941, 85.1376, IN, HANAFI),

        // Pakistan
        new City("karachi", "كراتشي", "Karachi", "باكستان", "Pakistan", 24.8607, 67.0011, IN, HANAFI),
        new City("lahore", "لاهور", "Lahore", "باكستان", "Pakistan", 31.5204, 74.3587, IN, HANAFI),
        new City("islamabad", "إسلام أباد", "Islamabad", "باكستان", "Pakistan", 33.6844, 73.0479, IN, HANAFI),
        new City("rawalpindi", "راولبندي", "Rawalpindi", "باكستان", "Pakistan", 33.5651, 73.0169, IN, HANAFI),
        new City("peshawar", "بيشاور", "Peshawar", "باكستان", "Pakistan", 34.0151, 71.5249, IN, HANAFI),

        // Bangladesh
        new City("dhaka", "دكا", "Dhaka", "بنغلاديش", "Bangladesh", 23.8103, 90.4125, IN, HANAFI),
        new City("chittagong", "شيتاغونغ", "Chittagong", "بنغلاديش", "Bangladesh", 22.3569, 91.7832, IN, HANAFI),

        // Sri Lanka & Maldives (Shafi'i, as are Kerala and Tamil Nadu above)
        new City("colombo", "كولمبو", "Colombo", "سريلانكا", "Sri Lanka", 6.9271, 79.8612, IN),
        new City("male", "ماليه", "Malé", "المالديف", "Maldives", 4.1755, 73.5093, MWL),

        // Southeast Asia. Indonesia (Kemenag), Singapore (MUIS) and Brunei use 20°/18°. Malaysia (JAKIM) moved Subuh to
        // 18° in 2020; adhan-java has no JAKIM method, and Moon Sighting Committee came within 2 minutes of every
        // e-solat time for Kuala Lumpur (2026-10-22), where Singapore's 20° put Fajr 10 minutes early.
        new City("jakarta", "جاكرتا", "Jakarta", "إندونيسيا", "Indonesia", -6.2088, 106.8456, SG),
        new City("surabaya", "سورابايا", "Surabaya", "إندونيسيا", "Indonesia", -7.2575, 112.7521, SG),
        new City("bandung", "باندونغ", "Bandung", "إندونيسيا", "Indonesia", -6.9175, 107.6191, SG),
        new City("medan", "ميدان", "Medan", "إندونيسيا", "Indonesia", 3.5952, 98.6722, SG),
        new City("kualalumpur", "كوالالمبور", "Kuala Lumpur", "ماليزيا", "Malaysia", 3.1390, 101.6869, Method.MOON_SIGHTING_COMMITTEE),
        new City("penang", "بينانغ", "Penang", "ماليزيا", "Malaysia", 5.4164, 100.3327, Method.MOON_SIGHTING_COMMITTEE),
        new City("singapore", "سنغافورة", "Singapore", "سنغافورة", "Singapore", 1.3521, 103.8198, SG),
        new City("brunei", "بندر سري بكاوان", "Bandar Seri Begawan", "بروناي", "Brunei", 4.9031, 114.9398, SG),
        new City("bangkok", "بانكوك", "Bangkok", "تايلاند", "Thailand", 13.7563, 100.5018, SG),

        // East Asia
        new City("tokyo", "طوكيو", "Tokyo", "اليابان", "Japan", 35.6762, 139.6503, MWL),
        new City("seoul", "سيول", "Seoul", "كوريا الجنوبية", "South Korea", 37.5665, 126.9780, MWL),

        // Turkey: Diyanet uses MWL's angles (18°/17°) and the first-shadow Asr (asr-ı evvel), plus safety minutes
        // (temkin) adhan-java can't add. Against Diyanet's Istanbul table (2026-10-22) Fajr and Isha match within a
        // minute; Dhuhr, Asr and Maghrib come 4 to 8 minutes before the mosques'.
        new City("istanbul", "إسطنبول", "Istanbul", "تركيا", "Turkey", 41.0082, 28.9784, MWL),
        new City("ankara", "أنقرة", "Ankara", "تركيا", "Turkey", 39.9334, 32.8597, MWL),

        // Europe
        new City("london", "لندن", "London", "المملكة المتحدة", "United Kingdom", 51.5074, -0.1278, MWL),
        new City("birmingham", "برمنغهام", "Birmingham", "المملكة المتحدة", "United Kingdom", 52.4862, -1.8904, MWL),
        new City("manchester", "مانشستر", "Manchester", "المملكة المتحدة", "United Kingdom", 53.4808, -2.2426, MWL),
        new City("paris", "باريس", "Paris", "فرنسا", "France", 48.8566, 2.3522, MWL),
        new City("berlin", "برلين", "Berlin", "ألمانيا", "Germany", 52.5200, 13.4050, MWL),
        new City("frankfurt", "فرانكفورت", "Frankfurt", "ألمانيا", "Germany", 50.1109, 8.6821, MWL),
        new City("amsterdam", "أمستردام", "Amsterdam", "هولندا", "Netherlands", 52.3676, 4.9041, MWL),
        new City("brussels", "بروكسل", "Brussels", "بلجيكا", "Belgium", 50.8503, 4.3517, MWL),
        new City("vienna", "فيينا", "Vienna", "النمسا", "Austria", 48.2082, 16.3738, MWL),
        new City("zurich", "زيورخ", "Zurich", "سويسرا", "Switzerland", 47.3769, 8.5417, MWL),
        new City("rome", "روما", "Rome", "إيطاليا", "Italy", 41.9028, 12.4964, MWL),
        new City("madrid", "مدريد", "Madrid", "إسبانيا", "Spain", 40.4168, -3.7038, MWL),
        new City("sarajevo", "سراييفو", "Sarajevo", "البوسنة والهرسك", "Bosnia and Herzegovina", 43.8563, 18.4131, MWL),

        // Africa
        new City("nairobi", "نيروبي", "Nairobi", "كينيا", "Kenya", -1.2921, 36.8219, MWL),
        new City("lagos", "لاغوس", "Lagos", "نيجيريا", "Nigeria", 6.5244, 3.3792, MWL),
        new City("johannesburg", "جوهانسبرغ", "Johannesburg", "جنوب أفريقيا", "South Africa", -26.2041, 28.0473, MWL),

        // Oceania
        new City("sydney", "سيدني", "Sydney", "أستراليا", "Australia", -33.8688, 151.2093, MWL),
        new City("melbourne", "ملبورن", "Melbourne", "أستراليا", "Australia", -37.8136, 144.9631, MWL),
        new City("auckland", "أوكلاند", "Auckland", "نيوزيلندا", "New Zealand", -36.8485, 174.7633, MWL),
    };

    static int cityIndex(Context c) {
        String id = of(c).getString("city", "cairo");
        for (int i = 0; i < CITIES.length; i++) if (CITIES[i].id.equals(id)) return i;
        return 0;
    }

    static City city(Context c) {
        return CITIES[cityIndex(c)];
    }

    /** A new city also takes that city's usual method and Asr school; their rows can still override them after. */
    static void setCity(Context c, int index) {
        City city = CITIES[index];
        of(c).edit().putString("city", city.id).putString("method", city.method.id)
            .putString("madhab", city.madhab == Madhab.HANAFI ? "hanafi" : "shafi").apply();
    }

    /**
     * Location's first step: each region's countries (English names, as in {@link #CITIES}), in the order of
     * {@link Text#regions()}. Every country in CITIES must be in exactly one region, or its cities can't be picked.
     */
    static final String[][] REGIONS = {
        {"Egypt", "Saudi Arabia", "UAE", "Kuwait", "Qatar", "Jordan", "Palestine", "Lebanon", "Syria", "Iraq", "Bahrain",
            "Oman", "Yemen"},
        {"India", "Pakistan", "Bangladesh", "Sri Lanka", "Maldives", "Indonesia", "Malaysia", "Singapore", "Brunei",
            "Thailand", "Japan", "South Korea", "Turkey"},
        {"United Kingdom", "France", "Germany", "Netherlands", "Belgium", "Austria", "Switzerland", "Italy", "Spain",
            "Bosnia and Herzegovina"},
        {"United States", "Canada"},
        {"Kenya", "Nigeria", "South Africa"},
        {"Australia", "New Zealand"},
    };

    /** The region (index into {@link #REGIONS}) a city's country is in; -1 if it is in none. */
    static int region(City city) {
        for (int r = 0; r < REGIONS.length; r++) for (String n : REGIONS[r]) if (n.equals(city.countryEn)) return r;
        return -1;
    }

    static Coordinates where(Context c) {
        City city = city(c);
        return new Coordinates(city.lat, city.lng);
    }

    // ---------------------------------------------------------------- calculation

    static final Method[] METHODS = {
        Method.EGYPTIAN, Method.UMM_AL_QURA, Method.MUSLIM_WORLD_LEAGUE,
        Method.KARACHI, Method.DUBAI, Method.KUWAIT, Method.QATAR,
        Method.NORTH_AMERICA, Method.MOON_SIGHTING_COMMITTEE, Method.SINGAPORE, Method.OMAN_AWQAF, Method.GULF};

    static Method method(Context c) {
        String name = of(c).getString("method", null);
        if (name != null) for (Method m : METHODS) if (m.id.equals(name)) return m;
        return city(c).method;
    }

    static void setMethod(Context c, Method m) {
        of(c).edit().putString("method", m.id).apply();
    }

    static Madhab madhab(Context c) {
        return "hanafi".equals(of(c).getString("madhab", "shafi")) ? Madhab.HANAFI : Madhab.SHAFI;
    }

    static void setMadhab(Context c, Madhab m) {
        of(c).edit().putString("madhab", m == Madhab.HANAFI ? "hanafi" : "shafi").apply();
    }

    /** Days added to the computed Hijri date, for when the local moon sighting differs: −2 to +2. */
    static int hijriOffset(Context c) {
        return of(c).getInt("hijri_offset", 0);
    }

    static void setHijriOffset(Context c, int days) {
        of(c).edit().putInt("hijri_offset", Math.max(-2, Math.min(2, days))).apply();
    }

    // ---------------------------------------------------------------- reminders

    /** Minutes before each prayer to show the reminder; 0 is off. */
    static int remindMinutes(Context c) {
        return of(c).getInt("remind", 15);
    }

    static void setRemindMinutes(Context c, int minutes) {
        of(c).edit().putInt("remind", minutes).apply();
    }

    /** Say «اقترب موعد صلاة …» with the reminder card or banner (Announce, on by default). */
    static boolean remindVoice(Context c) {
        return of(c).getBoolean("remind_voice", true);
    }

    static void setRemindVoice(Context c, boolean on) {
        of(c).edit().putBoolean("remind_voice", on).apply();
    }

    /** Say «حان الآن موعد صلاة …» when a prayer's time begins with its adhan off (Announce, on by default). */
    static boolean announce(Context c) {
        return of(c).getBoolean("announce", true);
    }

    static void setAnnounce(Context c, boolean on) {
        of(c).edit().putBoolean("announce", on).apply();
    }

    /**
     * When the call screen closes by itself: 1, 5 or 10 minutes after it opens ({@link Tokens#MOTION_CALL_AUTO_DISMISS_MS}
     * by default), or 0 when the adhan ends.
     */
    static int callCloseMinutes(Context c) {
        return of(c).getInt("call_close", (int) (Tokens.MOTION_CALL_AUTO_DISMISS_MS / 60_000));
    }

    static void setCallCloseMinutes(Context c, int minutes) {
        of(c).edit().putInt("call_close", minutes).apply();
    }

    /** The adhan as a whole; each prayer's bell (Prayer day) still applies under it. */
    static boolean adhan(Context c) {
        return of(c).getBoolean("adhan", true);
    }

    static void setAdhan(Context c, boolean on) {
        of(c).edit().putBoolean("adhan", on).apply();
    }

    /** Each prayer's own adhan bell (Prayer day); on by default except Sunrise, which has no adhan. */
    static boolean bell(Context c, int prayer) {
        return of(c).getBoolean("bell_" + prayer, prayer != PrayerDay.SUNRISE);
    }

    /** The recording (Adhans id) for every prayer but Fajr, and the one for Fajr, whose adhan has its own line. */
    static String voice(Context c, boolean fajr) {
        return fajr ? of(c).getString("voice_fajr", "chebae_fajr") : of(c).getString("voice", "alafasy");
    }

    static void setVoice(Context c, boolean fajr, String id) {
        of(c).edit().putString(fajr ? "voice_fajr" : "voice", id).apply();
    }

    // ---------------------------------------------------------------- radio

    /** After the adhan paused the radio: true resumes it (the default), false leaves it off. */
    static boolean radioResume(Context c) {
        return of(c).getBoolean("radio_resume", true);
    }

    static void setRadioResume(Context c, boolean on) {
        of(c).edit().putBoolean("radio_resume", on).apply();
    }

    /** Minutes of screensaver before the screen goes dark while the radio plays: 0 at once (the default), -1 never. */
    static int radioScreenOff(Context c) {
        return of(c).getInt("radio_dark", 0);
    }

    static void setRadioScreenOff(Context c, int minutes) {
        of(c).edit().putInt("radio_dark", minutes).apply();
    }

    // ---------------------------------------------------------------- display

    /** "system" (the TV's own Date & time setting, the default), "12" or "24". */
    static String clock(Context c) {
        return of(c).getString("clock", "system");
    }

    static boolean clock24(Context c) {
        String s = clock(c);
        return "system".equals(s) ? DateFormat.is24HourFormat(c) : "24".equals(s);
    }

    /** Everything a built screen depends on; a screen whose key changed while it was away rebuilds. */
    static String key(Context c) {
        SharedPreferences p = of(c);
        return p.getString("locale", "") + "|" + p.getString("digits", "") + "|" + clock24(c) + "|" + cityIndex(c)
            + "|" + method(c) + "|" + madhab(c) + "|" + hijriOffset(c) + "|" + p.getInt("facing", -1);
    }
}
