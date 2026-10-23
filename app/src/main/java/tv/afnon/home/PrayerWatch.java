package tv.afnon.home;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Date;

/**
 * Watches the clock from the remote buttons service, which the system keeps running whatever app is in front:
 * the reminder before each prayer (Home's own card, or a banner over other apps) and the call screen when the
 * time begins. The banner is an accessibility overlay, so it needs no overlay permission; it never takes focus
 * or touches and hides after 12 s (design/ApproachVideo.dc.html).
 */
final class PrayerWatch {
    private static final long CHECK_MS = 15_000;
    private static final int BANNER_SECONDS = 12;

    private final AccessibilityService service;
    private final Handler handler = new Handler(Looper.getMainLooper());
    /**
     * "prayer index:time" of the last reminder, call and spoken announcement, so each happens once. Saved, because
     * the system restarts this service (low memory, TCL's background manager) and a fresh one would repeat them.
     */
    private String reminded, called, announced;
    private View banner;

    private final Runnable check = new Runnable() {
        @Override
        public void run() {
            check();
            handler.postDelayed(this, CHECK_MS);
        }
    };

    PrayerWatch(AccessibilityService service) {
        this.service = service;
        reminded = Prefs.of(service).getString("reminded", "");
        called = Prefs.of(service).getString("called", "");
        announced = Prefs.of(service).getString("announced", "");
    }

    void start() {
        handler.removeCallbacks(check);
        handler.post(check);
    }

    void stop() {
        handler.removeCallbacksAndMessages(null);
        hideBanner();
    }

    private void check() {
        PowerManager pm = service.getSystemService(PowerManager.class);
        Date now = new Date();
        boolean asleep = pm != null && !pm.isInteractive();
        // Asleep: nothing to show, unless the radio plays with the screen off (TCL Audio Only); then the call
        // still comes, waking the screen.
        if (asleep && !Radio.on()) return;
        PrayerDay day = new PrayerDay(service, now);

        int call = Reminders.dueCall(service, day, now);
        if (call >= 0) {
            // While Afnon's screensaver runs it wakes itself for the call; open the screen once it has gone.
            if (!AfnonDream.running) callOnce(call, day.times[call], false);
            return;
        }
        // No call for a prayer whose adhan is off: just say that its time has begun (also with the screen off).
        int quiet = Reminders.dueAnnounce(service, day, now);
        if (quiet >= 0) {
            String key = quiet + ":" + day.times[quiet].getTime();
            if (!key.equals(announced)) {
                announced = key;
                Prefs.of(service).edit().putString("announced", key).apply();
                Announce.now(service, quiet, day.friday);
            }
        }
        if (asleep) return;
        if (Reminders.dueReminder(service, day, now)) {
            String key = day.nextIndex + ":" + day.nextTime.getTime();
            if (key.equals(reminded) || AfnonDream.running) return;
            reminded = key;
            Prefs.of(service).edit().putString("reminded", key).apply();
            if (Reminders.home != null) Reminders.home.showReminder();
            else showBanner(day, now);
            if (Prefs.remindVoice(service)) Announce.before(service, day.nextIndex, day.friday);
        }
    }

    /**
     * The call for this prayer time, unless it has been shown already: from this check, or from {@link Wake}'s
     * alarm with the TV in standby (woke), which shares the "called" key so the two never both open it.
     */
    void callOnce(int prayer, Date time, boolean woke) {
        String key = prayer + ":" + time.getTime();
        if (key.equals(called)) return;
        called = key;
        Prefs.of(service).edit().putString("called", key).apply();
        openCall(prayer, time, woke);
    }

    /** woke: Afnon turned the TV on for it, so the call screen puts it back to standby when it closes. */
    void openCall(int prayer, Date time, boolean woke) {
        hideBanner();
        wakeScreen();
        Intent i = new Intent(service, NowActivity.class)
            .putExtra("prayer", prayer)
            .putExtra("time", time.getTime())
            .putExtra("woke", woke)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            service.startActivity(i);
        } catch (RuntimeException ignored) {
            // Blocked on this firmware; the reminder and Home still show the time.
        }
    }

    /** Turns the screen back on (from TCL Audio Only or sleep) so the call can be seen; a no-op when it is on. */
    private void wakeScreen() {
        PowerManager pm = service.getSystemService(PowerManager.class);
        if (pm == null || pm.isInteractive()) return;
        try {
            @SuppressWarnings("deprecation")
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "afnon:call");
            wl.acquire(5000);
        } catch (RuntimeException ignored) {
            // No wake lock; the call opens when the screen next comes on.
        }
    }

    /**
     * Back to standby after a call Afnon woke the TV for: Android's lock-screen action, which also turns the screen
     * off (Android 9+). False where there is none; the TV then stays on.
     */
    boolean sleep() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
            && service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN);
    }

    // ---------------------------------------------------------------- banner

    void showBanner(PrayerDay day, Date now) {
        hideBanner();
        Ui.init(service);
        Text t = Screen.loadText(service);
        // Over other apps too, the banner takes the palette of Settings → Display → Night mode.
        Tokens.use(Prefs.night(service));
        boolean h24 = Prefs.clock24(service);
        Typeface ui = service.getResources().getFont(R.font.alexandria_regular);
        Typeface display = service.getResources().getFont(t.ar ? R.font.amiri_bold : R.font.marcellus_regular);

        LinearLayout card = new LinearLayout(service);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(t.ar ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        card.setPadding(Ui.ipx(28), Ui.ipx(24), Ui.ipx(28), 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Tokens.LAPIS_900);
        bg.setCornerRadius(Ui.px(24));
        bg.setStroke(Ui.ipx(2), Tokens.GOLD_400);
        card.setBackground(bg);
        card.setClipToOutline(true);
        card.setClipToPadding(false);

        LinearLayout row = new LinearLayout(service);
        row.setGravity(Gravity.CENTER_VERTICAL);
        Icon mihrab = new Icon(service, Icon.MIHRAB, 1.2f);
        mihrab.setColor(Tokens.GOLD_400);
        row.addView(mihrab, new LinearLayout.LayoutParams(Ui.ipx(56), Ui.ipx(72)));
        LinearLayout words = new LinearLayout(service);
        words.setOrientation(LinearLayout.VERTICAL);
        String[] names = t.prayerNames();
        if (day.friday) names[PrayerDay.DHUHR] = t.jumuah();
        long minutes = Reminders.minutesLeft(day, now);
        TextView label = text(ui, 20, Tokens.MIST_400, t.apLabel());
        if (!t.ar) label.setLetterSpacing(0.2f);
        TextView title = text(display, 44, Tokens.PLASTER_100,
            t.apTitle(names[day.nextIndex], minutes, Screen.duration(Screen.secondsUntil(day.nextTime, now), false)));
        TextView sub = text(ui, 22, Tokens.MIST_400, t.bannerSub(Screen.clock(t, day.nextTime, h24, 1f), BANNER_SECONDS));
        words.addView(label);
        words.addView(title);
        words.addView(sub);
        Screen.lineHeight(title, 1.1f);
        ((LinearLayout.LayoutParams) sub.getLayoutParams()).topMargin = Ui.ipx(4);
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(0, -2, 1);
        wlp.setMarginStart(Ui.ipx(20));
        row.addView(words, wlp);
        card.addView(row);

        // The time left before it hides, emptying toward the start side.
        FrameLayout track = new FrameLayout(service);
        track.setBackgroundColor(Tokens.BANNER_TRACK);
        View fill = new View(service);
        fill.setBackgroundColor(Tokens.GOLD_400);
        track.addView(fill, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-1, Ui.ipx(6));
        tlp.topMargin = Ui.ipx(18);
        tlp.setMarginStart(-Ui.ipx(28));
        tlp.setMarginEnd(-Ui.ipx(28));
        card.addView(track, tlp);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
            Ui.ipx(640), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        // Top end: the left in Arabic, the right in English.
        lp.gravity = Gravity.TOP | (t.ar ? Gravity.LEFT : Gravity.RIGHT);
        lp.x = Ui.ipx(96);
        lp.y = Ui.ipx(56);
        WindowManager wm = service.getSystemService(WindowManager.class);
        try {
            wm.addView(card, lp);
        } catch (RuntimeException e) {
            return;
        }
        banner = card;
        fill.post(() -> {
            fill.setPivotX(t.ar ? fill.getWidth() : 0);
            fill.animate().scaleX(0f).setDuration(BANNER_SECONDS * 1000L).setInterpolator(null).start();
        });
        handler.postDelayed(this::hideBanner, BANNER_SECONDS * 1000L);
    }

    void hideBanner() {
        if (banner == null) return;
        try {
            service.getSystemService(WindowManager.class).removeView(banner);
        } catch (RuntimeException ignored) {
            // Already gone with the service.
        }
        banner = null;
    }

    private TextView text(Typeface tf, float size, int color, CharSequence s) {
        TextView v = new TextView(service);
        v.setTypeface(tf);
        v.setTextSize(TypedValue.COMPLEX_UNIT_PX, Ui.px(size));
        v.setTextColor(color);
        v.setText(s);
        return v;
    }

    // ---------------------------------------------------------------- testing

    /**
     * For checking on the TV without waiting for a prayer: the banner (with the spoken reminder, if on), the call
     * screen, or the spoken «حان الآن موعد صلاة …» for the next prayer, after a delay to switch to another app
     * first; or standby as after a call that woke the TV. From Home:
     * `am start -n tv.afnon.home/.MainActivity --es test banner|call|announce|standby`.
     */
    void test(String kind, long delayMs) {
        handler.postDelayed(() -> {
            Date now = new Date();
            PrayerDay day = new PrayerDay(service, now);
            if ("call".equals(kind)) {
                openCall(day.nextIndex, day.nextTime, false);
            } else if ("standby".equals(kind)) {
                Wake.backToStandby();
            } else if ("announce".equals(kind)) {
                Announce.now(service, day.nextIndex, day.friday);
            } else {
                showBanner(day, now);
                if (Prefs.remindVoice(service)) Announce.before(service, day.nextIndex, day.friday);
            }
        }, delayMs);
    }
}
