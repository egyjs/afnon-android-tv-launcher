package tv.afnon.home;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.CalculationParameters;
import com.batoulapps.adhan.PrayerAdjustments;

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

    private static Method of(CalculationMethod m) {
        return new Method(m.name(), m);
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

    /** Aladhan's "Gulf Region" method (aladhan.com, method 8): Fajr 19.5°, Isha 90 minutes after Maghrib. */
    static final Method GULF = new Method("GULF", null);

    /** Fresh parameters on every call: callers set the Asr school on them. */
    CalculationParameters parameters() {
        if (base != null) return base.getParameters();
        if (this == OMAN_AWQAF) {
            return new CalculationParameters(18.0, 18.0, CalculationMethod.OTHER)
                .withMethodAdjustments(new PrayerAdjustments(0, 0, 5, 5, 5, 0));
        }
        return new CalculationParameters(19.5, 90, CalculationMethod.OTHER);
    }

    /** Minutes the method adds to Dhuhr, Asr and Maghrib (the same for all three), or 0. */
    int dayAdjustment() {
        return this == OMAN_AWQAF ? 5 : 0;
    }
}
