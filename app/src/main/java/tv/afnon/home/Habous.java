package tv.afnon.home;

import com.batoulapps.adhan.Coordinates;
import com.batoulapps.adhan.Madhab;
import com.batoulapps.adhan.data.DateComponents;
import com.batoulapps.adhan.internal.SolarTime;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * Morocco's Ministry of Habous and Islamic Affairs, offline. The ministry publishes its timetable, not its method,
 * so these rules were recovered from the tables themselves: October 2026 for 44 cities
 * (habous.gov.ma/data/horaires-2026-10.json) and the whole of 2019 for Tetouan (the ministry's old monthly pages,
 * via the Internet Archive), on adhan-java's own solar positions:
 *
 * <ul>
 * <li>Fajr: the sun 19° below the horizon, seconds dropped.</li>
 * <li>Sunrise and Maghrib: at the city's own horizon ({@link #horizon}), rounded.</li>
 * <li>Dhuhr: transit + 5 minutes, rounded.</li>
 * <li>Asr: shadow = noon shadow + the object (twice for Hanafi), with the sun's declination at Asr itself, rounded.</li>
 * <li>Isha: 17°, rounded.</li>
 * </ul>
 *
 * Against the ministry's October 2026 table this gives the published minute for 98.8% of the 44 cities' 8184 times
 * (every Dhuhr), and the rest are a minute off. Only October is published now, so the seasons were checked on
 * Tetouan's 2019 year, which none of this was fitted to: 90.8% on the minute, all within one. adhan-java can't do
 * this with parameters: it drops every time's seconds (its rounding divides integers), adjusts in whole minutes,
 * has one horizon for all places and takes Asr's declination at 0h UTC, which moved Asr up to a minute either way
 * with the seasons.
 */
final class Habous {
    private Habous() {}

    /**
     * Arc minutes below the horizon at sunrise and at Maghrib, per city. The ministry gives each city its own:
     * lower in the Atlas (Ifrane, Midelt, Tinghir) than on the coast. Fitted to its October 2026 table at the
     * points in {@link Prefs#CITIES}, so the two go together; anywhere else uses adhan-java's standard 50'.
     */
    private static final Map<String, int[]> HORIZON = new HashMap<>();
    static {
        Map<String, int[]> h = HORIZON;
        h.put("rabat", new int[] {86, 97});
        h.put("casablanca", new int[] {92, 104});
        h.put("fes", new int[] {105, 115});
        h.put("tangier", new int[] {98, 109});
        h.put("marrakech", new int[] {112, 122});
        h.put("meknes", new int[] {111, 121});
        h.put("oujda", new int[] {117, 124});
        h.put("kenitra", new int[] {93, 104});
        h.put("agadir", new int[] {99, 110});
        h.put("tetouan", new int[] {109, 118});
        h.put("safi", new int[] {88, 100});
        h.put("mohammedia", new int[] {84, 96});
        h.put("khouribga", new int[] {121, 129});
        h.put("eljadida", new int[] {84, 96});
        h.put("benimellal", new int[] {131, 138});
        h.put("nador", new int[] {92, 103});
        h.put("taza", new int[] {122, 129});
        h.put("settat", new int[] {104, 114});
        h.put("berrechid", new int[] {95, 106});
        h.put("khemisset", new int[] {106, 116});
        h.put("guelmim", new int[] {115, 125});
        h.put("laayoune", new int[] {85, 99});
        h.put("berkane", new int[] {92, 103});
        h.put("taroudant", new int[] {99, 109});
        h.put("ksarelkebir", new int[] {88, 100});
        h.put("larache", new int[] {84, 95});
        h.put("sidislimane", new int[] {85, 95});
        h.put("khenifra", new int[] {126, 132});
        h.put("errachidia", new int[] {126, 132});
        h.put("sidikacem", new int[] {105, 115});
        h.put("tiznit", new int[] {99, 110});
        h.put("ouarzazate", new int[] {133, 137});
        h.put("dakhla", new int[] {89, 104});
        h.put("essaouira", new int[] {86, 99});
        h.put("alhoceima", new int[] {92, 102});
        h.put("ouazzane", new int[] {116, 124});
        h.put("tantan", new int[] {99, 111});
        h.put("sefrou", new int[] {126, 132});
        h.put("tinghir", new int[] {140, 143});
        h.put("midelt", new int[] {143, 145});
        h.put("azrou", new int[] {137, 141});
        h.put("chefchaouen", new int[] {130, 135});
        h.put("ifrane", new int[] {139, 143});
        h.put("zagora", new int[] {120, 127});
    }

    private static final int[] STANDARD = {50, 50};

    /**
     * Seconds the ministry's current tables put Asr and Isha after these rules: across the 44 cities its Asr sits
     * 12 s later (1333 of 1364 times on the minute with it, 1123 without) and Isha 3 s (1351, 1304 without).
     * Its 2019 tables needed neither, so likely a change in its 2021 functions.
     */
    private static final int ASR_LATER = 12, ISHA_LATER = 3;

    /** {sunrise, Maghrib} arc minutes below the horizon for a city id. */
    static int[] horizon(String cityId) {
        int[] h = HORIZON.get(cityId);
        return h != null ? h : STANDARD;
    }

    /** The six times (PrayerDay.FAJR..ISHA) on their whole minute, or null where the sun doesn't reach 19°/17°. */
    static Date[] times(Coordinates where, DateComponents day, Madhab madhab, int[] horizon) {
        SolarTime s = new SolarTime(day, where);
        double asr = asr(s, where, day, madhab == Madhab.HANAFI ? 2 : 1);
        double[] hours = {
            s.hourAngle(-19.0, false),
            s.hourAngle(-horizon[0] / 60.0, false),
            s.transit + 5 / 60.0,
            asr + ASR_LATER / 3600.0,
            s.hourAngle(-horizon[1] / 60.0, true),
            s.hourAngle(-17.0, true) + ISHA_LATER / 3600.0,
        };
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.clear();
        c.set(day.year, day.month - 1, day.day);
        long midnight = c.getTimeInMillis();
        Date[] out = new Date[6];
        for (int i = 0; i < 6; i++) {
            if (Double.isNaN(hours[i])) return null;
            long ms = midnight + Math.round(hours[i] * 3_600_000);
            out[i] = new Date((i == PrayerDay.FAJR ? ms : ms + 30_000) / 60_000 * 60_000);
        }
        return out;
    }

    /** Asr in hours UTC, its altitude from the declination at Asr (two passes are enough to settle on it). */
    private static double asr(SolarTime s, Coordinates where, DateComponents day, double shadow) {
        double jd = julianDay(day.year, day.month, day.day);
        double h = s.transit + 3;
        for (int i = 0; i < 3; i++) {
            double tangent = Math.abs(where.latitude - declination(jd + h / 24));
            h = s.hourAngle(Math.toDegrees(Math.atan(1.0 / (shadow + Math.tan(Math.toRadians(tangent))))), true);
            if (Double.isNaN(h)) return h;
        }
        return h;
    }

    // adhan-java's SolarCoordinates and CalendricalHelper (Meeus, Astronomical Algorithms), which it keeps
    // package-private: the same formulas, so Asr's declination agrees with the rest of its solar positions.

    private static double julianDay(int year, int month, int day) {
        int y = month > 2 ? year : year - 1;
        int m = month > 2 ? month : month + 12;
        int a = y / 100;
        int b = 2 - a + (a / 4);
        return (int) (365.25 * (y + 4716)) + (int) (30.6001 * (m + 1)) + day + b - 1524.5;
    }

    private static double declination(double jd) {
        double t = (jd - 2451545.0) / 36525;
        double l0 = unwind(280.4664567 + 36000.76983 * t + 0.0003032 * t * t);
        double mRad = Math.toRadians(unwind(357.52911 + 35999.05029 * t - 0.0001537 * t * t));
        double center = (1.914602 - 0.004817 * t - 0.000014 * t * t) * Math.sin(mRad)
            + (0.019993 - 0.000101 * t) * Math.sin(2 * mRad) + 0.000289 * Math.sin(3 * mRad);
        double omega = 125.04 - 1934.136 * t;
        double lambda = Math.toRadians(unwind(l0 + center - 0.00569 - 0.00478 * Math.sin(Math.toRadians(omega))));
        double e0 = 23.439291 - 0.013004167 * t - 0.0000001639 * t * t + 0.0000005036 * t * t * t;
        double e = Math.toRadians(e0 + 0.00256 * Math.cos(Math.toRadians(omega)));
        return Math.toDegrees(Math.asin(Math.sin(e) * Math.sin(lambda)));
    }

    private static double unwind(double a) {
        double v = a - 360 * Math.floor(a / 360);
        return v < 0 ? v + 360 : v;
    }
}
