package tv.afnon.home;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.CalculationParameters;
import com.batoulapps.adhan.Coordinates;
import com.batoulapps.adhan.Madhab;
import com.batoulapps.adhan.PrayerAdjustments;
import com.batoulapps.adhan.PrayerTimes;
import com.batoulapps.adhan.data.DateComponents;

import java.util.Date;

/**
 * A prayer-time calculation method: one of adhan-java's, or one Afnon defines itself where adhan-java has none
 * (all offline, nothing is fetched). The {@link #id} is what Settings saves, so the ids of adhan-java's methods are
 * its enum names and old saved choices keep working.
 */
final class Method {
    final String id;
    private final CalculationMethod base;

    private Method(String id, CalculationMethod base) {
        this.id = id;
        this.base = base;
    }

    private static final java.util.Map<CalculationMethod, Method> BY_ENUM = new java.util.EnumMap<>(CalculationMethod.class);

    /** The one Method for an adhan-java method, so == works; made on first use for ones Afnon doesn't list. */
    static Method of(CalculationMethod m) {
        Method found = BY_ENUM.get(m);
        if (found == null) {
            found = new Method(m.name(), m);
            BY_ENUM.put(m, found);
        }
        return found;
    }

    static final Method EGYPTIAN = of(CalculationMethod.EGYPTIAN);
    static final Method UMM_AL_QURA = of(CalculationMethod.UMM_AL_QURA);
    static final Method MUSLIM_WORLD_LEAGUE = of(CalculationMethod.MUSLIM_WORLD_LEAGUE);
    static final Method KARACHI = of(CalculationMethod.KARACHI);
    static final Method DUBAI = of(CalculationMethod.DUBAI);
    static final Method KUWAIT = of(CalculationMethod.KUWAIT);
    static final Method QATAR = of(CalculationMethod.QATAR);
    static final Method NORTH_AMERICA = of(CalculationMethod.NORTH_AMERICA);
    static final Method MOON_SIGHTING_COMMITTEE = of(CalculationMethod.MOON_SIGHTING_COMMITTEE);
    static final Method SINGAPORE = of(CalculationMethod.SINGAPORE);

    /**
     * Oman's Ministry of Awqaf and Religious Affairs: Fajr and Isha at 18°, Asr by the standard shadow, and five
     * minutes added to Dhuhr, Asr and Maghrib. The ministry publishes no parameters, so these were fitted to its own
     * timetable (mara.gov.om/arabic/calendar_page1.asp) for Muscat, Sohar, Nizwa and Salalah across 2026: every
     * prayer within about two minutes of the published time.
     */
    static final Method OMAN_AWQAF = new Method("OMAN_AWQAF", null);

    /**
     * Morocco's Ministry of Habous and Islamic Affairs: Fajr 19°, Isha 17°, Dhuhr 5 minutes after transit, and
     * sunrise and Maghrib at each city's own horizon. Its rounding, horizons and Asr need {@link Habous}, so its
     * {@link #parameters()} (19°/17°) only describe it and stand in where the sun doesn't reach those angles.
     */
    static final Method MOROCCO_HABOUS = new Method("MOROCCO_HABOUS", null);

    /** Fresh parameters on every call: callers set the Asr school on them. */
    CalculationParameters parameters() {
        if (base != null) return base.getParameters();
        if (this == MOROCCO_HABOUS) return new CalculationParameters(19.0, 17.0, CalculationMethod.OTHER);
        return new CalculationParameters(18.0, 18.0, CalculationMethod.OTHER)
            .withMethodAdjustments(new PrayerAdjustments(0, 0, 5, 5, 5, 0));
    }

    /**
     * The six times (PrayerDay.FAJR..ISHA) for a day, seconds dropped. {@code horizon} is the city's
     * {sunrise, Maghrib} arc minutes below the horizon, which only Morocco's method uses.
     */
    Date[] times(Coordinates where, DateComponents day, Madhab madhab, int[] horizon) {
        if (this == MOROCCO_HABOUS) {
            Date[] t = Habous.times(where, day, madhab, horizon);
            if (t != null) return t;
        }
        CalculationParameters params = parameters();
        params.madhab = madhab;
        PrayerTimes p = new PrayerTimes(where, day, params);
        return new Date[] {p.fajr, p.sunrise, p.dhuhr, p.asr, p.maghrib, p.isha};
    }

    /** Minutes the method adds to Dhuhr, Asr and Maghrib (the same for all three), or 0. */
    int dayAdjustment() {
        return parameters().methodAdjustments.dhuhr;
    }
}
