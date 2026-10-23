package tv.afnon.home;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;

/**
 * Settings → Reminders → Turn the TV on for the adhan: the prayers picked there (none by default) wake the TV from
 * standby for their call. PrayerWatch's 15 s check stops while the TV sleeps, so this arms one AlarmManager alarm,
 * for the next picked prayer only, and arms the one after each time it rings, on boot ({@link WakeReceiver}), when
 * the remote buttons service starts, and whenever the picks, the adhan, a bell, the city, the method or the Asr
 * school change. With nothing picked nothing is armed and nothing runs.
 *
 * <p>When it rings with the TV in standby, the call opens through the remote buttons service as PrayerWatch opens
 * it, under the same "called" key so it is never shown twice, and the screen comes on (a wake lock, and the call
 * screen's turnScreenOn). When that call screen closes by itself, or with Back or Dismiss, the service asks Android
 * to go back to sleep ({@link #backToStandby}); left for another app or Home, the TV stays on. A TV that is on, in
 * the screensaver, or playing the radio with the screen off is left to PrayerWatch, as before.
 */
final class Wake {
    private Wake() {}

    private static final String TAG = "AfnonWake";
    static final String ACTION = "tv.afnon.home.WAKE";
    /** The prayers that can wake the TV, in the chooser's order. */
    static final int[] PRAYERS = {PrayerDay.FAJR, PrayerDay.DHUHR, PrayerDay.ASR, PrayerDay.MAGHRIB, PrayerDay.ISHA};
    /** PendingIntent request codes: the real alarm, and the test hook's, which never replaces it. */
    private static final int REAL = 0, TEST = 1;

    /** Arms the alarm for the next picked prayer, or cancels it when there is none (the default). */
    static void schedule(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return;
        long[] next = Prefs.wakeAny(c) ? next(c, new Date()) : null;
        if (next == null) {
            PendingIntent old = alarm(c, REAL, -1, 0, false, PendingIntent.FLAG_NO_CREATE);
            if (old != null) {
                am.cancel(old);
                old.cancel();
                Log.i(TAG, "nothing to wake for: alarm cancelled");
            }
            return;
        }
        int prayer = (int) next[0];
        long time = next[1];
        if (arm(c, am, time, alarm(c, REAL, prayer, time, false, PendingIntent.FLAG_UPDATE_CURRENT), true)) {
            Log.i(TAG, "armed for prayer " + prayer + " in " + (time - System.currentTimeMillis()) / 60_000 + " min");
        }
    }

    /**
     * The first picked prayer after now with its adhan on (the adhan as a whole and that prayer's bell), as
     * {prayer, time}; null if there is none.
     */
    private static long[] next(Context c, Date now) {
        if (!Prefs.adhan(c)) return null;
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.setTime(now);
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        tomorrow.set(Calendar.HOUR_OF_DAY, 12);
        tomorrow.set(Calendar.MINUTE, 0);
        // Today as it stands (after Isha its Fajr is already tomorrow's), then all of tomorrow, seen from midday.
        for (Date at : new Date[] {now, tomorrow.getTime()}) {
            PrayerDay day = new PrayerDay(c, at);
            int best = -1;
            for (int p : PRAYERS) {
                if (!day.times[p].after(now) || !Prefs.wake(c, p) || !Prefs.bell(c, p)) continue;
                if (best < 0 || day.times[p].before(day.times[best])) best = p;
            }
            if (best >= 0) return new long[] {best, day.times[best].getTime()};
        }
        return null;
    }

    /**
     * setAlarmClock (exact, never deferred, and the strongest hint a firmware gets that the device must wake), or
     * setExactAndAllowWhileIdle with RTC_WAKEUP for the test hook's "via exact".
     */
    private static boolean arm(Context c, AlarmManager am, long at, PendingIntent pi, boolean clock) {
        if (!allowed(c)) {
            Log.w(TAG, "exact alarms are not allowed for Afnon: nothing armed");
            return false;
        }
        try {
            if (clock) am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, settings(c)), pi);
            else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            return true;
        } catch (SecurityException e) {
            Log.w(TAG, "exact alarm refused", e);
            return false;
        }
    }

    /**
     * Android 12 and up need the exact-alarm permission: SCHEDULE_EXACT_ALARM on 12 (granted at install, though
     * the user can take it back) and USE_EXACT_ALARM from 13 (see the manifest).
     */
    static boolean allowed(Context c) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager am = c.getSystemService(AlarmManager.class);
        return am != null && am.canScheduleExactAlarms();
    }

    private static PendingIntent alarm(Context c, int request, int prayer, long time, boolean test, int flags) {
        Intent i = new Intent(c, WakeReceiver.class).setAction(ACTION)
            .putExtra("prayer", prayer).putExtra("time", time).putExtra("test", test);
        return PendingIntent.getBroadcast(c, request, i, flags | PendingIntent.FLAG_IMMUTABLE);
    }

    /** What the system's "next alarm" opens, where a firmware shows one: this setting. */
    private static PendingIntent settings(Context c) {
        Intent i = new Intent(c, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(c, 0, i, PendingIntent.FLAG_IMMUTABLE);
    }

    // ---------------------------------------------------------------- the alarm

    /** It rang: the next prayer's alarm first, then, with the TV in standby, the call (once), waking the screen. */
    static void fire(Context c, Intent i) {
        int prayer = i.getIntExtra("prayer", -1);
        long time = i.getLongExtra("time", 0);
        boolean test = i.getBooleanExtra("test", false);
        schedule(c);
        PowerManager pm = c.getSystemService(PowerManager.class);
        boolean asleep = pm != null && !pm.isInteractive();
        Log.i(TAG, "rang" + (test ? " (test)" : "") + " for prayer " + prayer + ", screen " + (asleep ? "off" : "on"));
        // On, in the screensaver, or the radio playing with the screen off: PrayerWatch's own check calls.
        if (!asleep || Radio.on()) return;
        PrayerWatch watch = RemoteKeys.watch;
        if (watch == null) {
            Log.w(TAG, "the remote buttons service is not running: no call");
            return;
        }
        if (test) {
            // The coming prayer's call, as `--es test call` shows it, without marking it called.
            if (Prefs.adhan(c) && Prefs.bell(c, prayer)) watch.openCall(prayer, new Date(time), true);
            else Log.i(TAG, "the adhan is off for this prayer: nothing to do");
            return;
        }
        Date now = new Date();
        PrayerDay day = new PrayerDay(c, now);
        int call = Reminders.dueCall(c, day, now);
        if (call >= 0 && Prefs.wake(c, call)) watch.callOnce(call, day.times[call], true);
        else Log.i(TAG, "no call due now");
    }

    /**
     * The call Afnon turned the TV on for has closed: back to standby through the remote buttons service, the only
     * way an app may. If Android ignores it, the TV just stays on.
     */
    static void backToStandby() {
        PrayerWatch watch = RemoteKeys.watch;
        boolean asked = watch != null && watch.sleep();
        Log.i(TAG, asked ? "asked Android for standby" : "no way back to standby here: the TV stays on");
    }

    // ---------------------------------------------------------------- testing

    /**
     * `--es test wake --ei seconds N` (120 by default): turns waking on for the coming prayer, which also arms its
     * real alarm, and rings a test alarm for it N seconds from now; put the TV in standby meanwhile. `--es via
     * exact` uses setExactAndAllowWhileIdle instead of setAlarmClock.
     */
    static void test(Context c, int seconds, String via) {
        PrayerDay day = new PrayerDay(c, new Date());
        if (!Prefs.wake(c, day.nextIndex)) Prefs.setWake(c, day.nextIndex, true);
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return;
        boolean clock = !"exact".equals(via);
        long at = System.currentTimeMillis() + seconds * 1000L;
        PendingIntent pi = alarm(c, TEST, day.nextIndex, day.nextTime.getTime(), true, PendingIntent.FLAG_UPDATE_CURRENT);
        if (arm(c, am, at, pi, clock)) {
            Log.i(TAG, "test: prayer " + day.nextIndex + " rings in " + seconds + " s by "
                + (clock ? "setAlarmClock" : "setExactAndAllowWhileIdle"));
        }
    }
}
