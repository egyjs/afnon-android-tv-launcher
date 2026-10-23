package tv.afnon.home;

import android.content.Context;
import android.icu.util.IslamicCalendar;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.CalculationParameters;
import com.batoulapps.adhan.Coordinates;
import com.batoulapps.adhan.HighLatitudeRule;
import com.batoulapps.adhan.Madhab;
import com.batoulapps.adhan.PrayerAdjustments;
import com.batoulapps.adhan.PrayerTimes;
import com.batoulapps.adhan.Qibla;
import com.batoulapps.adhan.data.DateComponents;

import java.util.Calendar;
import java.util.Date;

/** One moment's view of the prayer day: the six times, which prayer is current and which is next. */
final class PrayerDay {
    static final int FAJR = 0, SUNRISE = 1, DHUHR = 2, ASR = 3, MAGHRIB = 4, ISHA = 5;

    /** Today's times, except that after Isha the Fajr slot holds tomorrow's Fajr (it is the next prayer). */
    final Date[] times = new Date[6];
    /** Index of the current prayer, or -1 before Fajr and between Sunrise and Dhuhr. */
    final int nowIndex;
    /** Index of the next prayer (never Sunrise, which is a marker, not a prayer). */
    final int nextIndex;
    final Date nextTime;
    /** The Fajr that ends tonight's Suhoor: today's before dawn, otherwise tomorrow's. */
    final Date suhoorEnd;
    /** Jumu'ah in Dhuhr's place: Friday until its Maghrib, when the Hijri day turns to Saturday. */
    final boolean friday;
    /** Surat al-Kahf: the Hijri Friday, from Thursday's Maghrib to Friday's. */
    final boolean kahf;
    /** The Hijri day's weekday (Calendar.SUNDAY…SATURDAY), which turns at Maghrib with its date. */
    final int weekday;
    final boolean ramadan;
    final int hijriDay, hijriMonth, hijriYear;
    final double qibla;

    /** With the saved location, method, Asr school, adjustments and Hijri adjustment (Afnon Settings). */
    PrayerDay(Context c, Date now) {
        this(now, Prefs.where(c), Prefs.method(c), Prefs.madhab(c), Prefs.adjustments(c), Prefs.hijriOffset(c));
    }

    PrayerDay(Date now, Coordinates where, CalculationMethod method, Madhab madhab, int[] adjust, int hijriOffset) {
        CalculationParameters params = method.getParameters();
        params.madhab = madhab;
        // Minutes added to each time (Settings → Adjust the times), to match a mosque's timetable.
        params.adjustments = new PrayerAdjustments(adjust[FAJR], adjust[SUNRISE], adjust[DHUHR], adjust[ASR],
            adjust[MAGHRIB], adjust[ISHA]);
        // Where summer nights never get dark enough for Fajr's and Isha's angles (above 48°: London, Paris,
        // Berlin…), a seventh of the night after Maghrib and before sunrise, as meypod/al-azan does; adhan's
        // default, the middle of the night, put both near 1 a.m. Below 48° nothing changes.
        if (where.latitude > 48) params.highLatitudeRule = HighLatitudeRule.SEVENTH_OF_THE_NIGHT;
        // Umm al-Qura's Isha is 2 hours after Maghrib in Ramadan, not 1½, as Saudi Arabia's calendar and al-azan
        // have it. An evening belongs to the next Hijri day, so this runs from the first night of Ramadan (the
        // first tarawih) to the last, and not on the night of Eid.
        if (method == CalculationMethod.UMM_AL_QURA && hijriMonthOf(now, 1) == IslamicCalendar.RAMADAN) {
            params.ishaInterval = 120;
        }
        PrayerTimes today = new PrayerTimes(where, DateComponents.from(now), params);
        times[FAJR] = minute(today.fajr);
        times[SUNRISE] = minute(today.sunrise);
        times[DHUHR] = minute(today.dhuhr);
        times[ASR] = minute(today.asr);
        times[MAGHRIB] = minute(today.maghrib);
        times[ISHA] = minute(today.isha);

        int latest = -1;
        for (int i = 0; i < 6; i++) if (!times[i].after(now)) latest = i;
        nowIndex = latest == SUNRISE ? -1 : latest;

        int next = -1;
        for (int i = 0; i < 6; i++) {
            if (i != SUNRISE && times[i].after(now)) { next = i; break; }
        }
        Calendar c = Calendar.getInstance();
        c.setTime(now);
        c.add(Calendar.DAY_OF_MONTH, 1);
        Date tomorrowFajr = minute(new PrayerTimes(where, DateComponents.from(c.getTime()), params).fajr);
        if (next == -1) {
            next = FAJR;
            times[FAJR] = tomorrowFajr;
        }
        nextIndex = next;
        nextTime = times[next];
        suhoorEnd = times[FAJR].after(now) ? times[FAJR] : tomorrowFajr;

        c.setTime(now);
        int dow = c.get(Calendar.DAY_OF_WEEK);
        // The Hijri day begins at Maghrib: from then until midnight its date and weekday are the next day's
        // (Friday evening is ليلة السبت). Ramadan follows it, so its first night has a suhoor and Eid's night none.
        int eve = now.before(times[MAGHRIB]) ? 0 : 1;
        weekday = (dow - 1 + eve) % 7 + 1;
        // Jumu'ah ends with Friday at its Maghrib, and doesn't start on Thursday evening (that Dhuhr has passed).
        friday = dow == Calendar.FRIDAY && eve == 0;
        kahf = weekday == Calendar.FRIDAY;

        IslamicCalendar h = new IslamicCalendar();
        h.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);
        h.setTime(now);
        if (hijriOffset + eve != 0) h.add(IslamicCalendar.DAY_OF_MONTH, hijriOffset + eve);
        hijriDay = h.get(IslamicCalendar.DAY_OF_MONTH);
        hijriMonth = h.get(IslamicCalendar.MONTH);
        hijriYear = h.get(IslamicCalendar.YEAR);
        ramadan = hijriMonth == IslamicCalendar.RAMADAN;

        qibla = new Qibla(where).direction;
    }

    /** The Umm al-Qura month (IslamicCalendar.MUHARRAM…) of the calendar day days after now's, at noon. */
    private static int hijriMonthOf(Date now, int days) {
        Calendar c = Calendar.getInstance();
        c.setTime(now);
        c.add(Calendar.DAY_OF_MONTH, days);
        c.set(Calendar.HOUR_OF_DAY, 12);
        IslamicCalendar h = new IslamicCalendar();
        h.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);
        h.setTime(c.getTime());
        return h.get(IslamicCalendar.MONTH);
    }

    /**
     * The time on its whole minute. adhan-java keeps the milliseconds of the moment it was asked, so the same
     * prayer came out a little different on every check, and "already shown" never matched: the reminder and the
     * call kept coming back after being closed.
     */
    private static Date minute(Date d) {
        return new Date(d.getTime() / 60_000 * 60_000);
    }
}
