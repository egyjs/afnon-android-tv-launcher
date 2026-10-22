package tv.afnon.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/**
 * The screensaver (design/Screensaver.dc.html), as the TV's system screensaver so it starts from any app after
 * the idle timeout: black, a dim arch outline, the clock, the date and the next prayer. The whole group moves to
 * a new spot every 60 seconds, fading out and in, so nothing burns into the panel. Not interactive: any key wakes
 * the TV back to whatever was on.
 */
public class AfnonDream extends DreamService {

    private static final float W = 440, H = 640;
    private static final long DRIFT_MS = 60_000, FADE_MS = 800;

    /** True while the screensaver is showing; the reminders wait for it, and it steps aside for the call. */
    static boolean running;

    private final Handler tick = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private Text t;
    private FrameLayout root, group;
    private TextView clock, date, next, left;
    /** The clock's ص/م font where its digits are Marcellus; else null. */
    private Typeface marker;
    private PrayerDay day;
    private long lastDrift, lastMinute = -1;
    /** The radio line under the countdown, and the screen-off step while it plays (Settings → Radio). */
    private LinearLayout radioRow;
    private RadioBars radioBars;
    private TextView radioText;
    private long dreamStart;
    private boolean dark;

    private final Runnable everySecond = new Runnable() {
        @Override
        public void run() {
            render();
            long now = System.currentTimeMillis();
            if (now - lastDrift >= DRIFT_MS) drift(true);
            tick.postDelayed(this, 1000 - (now % 1000) + 5);
        }
    };

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(false);
        Ui.init(this);
        t = Screen.loadText(this);
        // Night mode dims the screensaver's text and arch too (tokens.night.json, screensaver.*).
        Tokens.use(Prefs.night(this));
        int ink = Tokens.SCREENSAVER_TEXT, dim = Tokens.SCREENSAVER_TEXT_DIM, gold = Tokens.SCREENSAVER_NEXT;

        Typeface ui = getResources().getFont(R.font.alexandria_regular);
        Typeface display = getResources().getFont(t.ar ? R.font.amiri_regular : R.font.marcellus_regular);
        // The clock in Marcellus also in Arabic with Western digits, its ص/م in Amiri (Screen.marcellusDigits).
        Typeface numbers = Screen.marcellusDigits(t) ? getResources().getFont(R.font.marcellus_regular) : display;
        marker = numbers != display ? display : null;

        root = new FrameLayout(this);
        root.setBackgroundColor(Tokens.SCREENSAVER_GROUND);
        root.setLayoutDirection(t.ar ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);

        group = new FrameLayout(this);
        group.addView(new Outline(this), new FrameLayout.LayoutParams(-1, -1));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        clock = text(numbers, 128, ink);
        clock.setTextDirection(t.ar ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR);
        date = text(ui, 24, dim);
        View rule = new View(this);
        rule.setBackgroundColor(Tokens.SCREENSAVER_ARCH_LINE);
        next = text(ui, 28, gold);
        next.setTextDirection(t.ar ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR);
        left = text(ui, 24, dim);
        col.addView(clock, wrap(0));
        col.addView(date, wrap(10));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(Ui.ipx(80), Math.max(1, Ui.ipx(1)));
        rlp.topMargin = Ui.ipx(10 + 18);
        rlp.bottomMargin = Ui.ipx(18);
        col.addView(rule, rlp);
        col.addView(next, wrap(10));
        col.addView(left, wrap(10));
        radioRow = new LinearLayout(this);
        radioRow.setGravity(Gravity.CENTER_VERTICAL);
        radioBars = new RadioBars(this, 3);
        radioBars.setColor(gold);
        radioRow.addView(radioBars, new LinearLayout.LayoutParams(Ui.ipx(22), Ui.ipx(20)));
        radioText = text(ui, 22, dim);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-2, -2);
        tlp.setMarginStart(Ui.ipx(12));
        radioRow.addView(radioText, tlp);
        col.addView(radioRow, wrap(22));
        Screen.lineHeight(clock, 1f);
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
        clp.leftMargin = Ui.ipx(20);
        clp.rightMargin = Ui.ipx(20);
        clp.topMargin = Ui.ipx(190);
        group.addView(col, clp);
        group.setClipChildren(false);
        root.setClipChildren(false);
        // Pinned to the physical top-left, so the drift's translation means the same in Arabic and English.
        root.addView(group, new FrameLayout.LayoutParams(Ui.ipx(W), Ui.ipx(H), Gravity.TOP | Gravity.LEFT));
        setContentView(root);

        // The design's own spot first; drifting starts a minute later.
        place(1140, 250);
        lastDrift = System.currentTimeMillis();
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        running = true;
        day = null;
        dreamStart = System.currentTimeMillis();
        tick.removeCallbacks(everySecond);
        everySecond.run();
    }

    @Override
    public void onDreamingStopped() {
        running = false;
        setDark(false);
        tick.removeCallbacks(everySecond);
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        running = false;
        setDark(false);
        tick.removeCallbacks(everySecond);
        super.onDetachedFromWindow();
    }

    private void render() {
        Date now = new Date();
        // The call to prayer: wake the TV out of the screensaver so the call screen can open over what was on.
        if (day != null && Reminders.dueCall(this, day, now) >= 0) {
            running = false;
            wakeUp();
            return;
        }
        long minute = now.getTime() / 60000;
        if (day == null || Screen.secondsUntil(day.nextTime, now) == 0 || minute != lastMinute) {
            lastMinute = minute;
            boolean arrived = day != null && Screen.secondsUntil(day.nextTime, now) == 0;
            day = new PrayerDay(this, arrived ? new Date(now.getTime() + 1000) : now);
            boolean h24 = Prefs.clock24(this);
            clock.setText(Screen.clock(t, now, h24, 0.35f, marker));
            Locale loc = t.ar ? Locale.forLanguageTag("ar-EG-u-nu-latn") : Locale.UK;
            String weekday = new SimpleDateFormat("EEEE", loc).format(now);
            date.setText(t.d(weekday + t.sep() + day.hijriDay + " " + t.hijriMonths()[day.hijriMonth]));
            String[] names = t.prayerNames();
            if (day.friday) names[PrayerDay.DHUHR] = t.jumuah();
            next.setText(names[day.nextIndex] + " " + Screen.clock(t, day.nextTime, h24, 1f));
        }
        long secs = Screen.secondsUntil(day.nextTime, now);
        left.setText(t.inMinutes((secs + 59) / 60, Screen.duration(secs, false)));
        renderRadio(now.getTime());
    }

    /** On air: a quiet line with the bars; after the chosen minutes the screen goes dark and the radio plays on. */
    private void renderRadio(long now) {
        boolean on = Radio.on();
        radioRow.setVisibility(on ? View.VISIBLE : View.GONE);
        if (on) {
            radioText.setText(t.station(Radio.station(this)) + t.sep() + t.radioStatus(Radio.state()));
            radioBars.setLive(Radio.state() == Radio.PLAYING);
        }
        int minutes = Prefs.radioScreenOff(this);
        boolean due = on && minutes >= 0 && now - dreamStart >= minutes * 60_000L;
        if (due != dark) setDark(due);
    }

    /**
     * Dark: TCL's Audio Only (its countdown, then the panel off and the TV asleep with the radio playing). Where
     * there is none, the screensaver blanks itself at its lowest brightness instead.
     */
    private void setDark(boolean on) {
        if (dark == on) return;
        dark = on;
        if (on && ScreenOffActivity.audioOnly(this)) return;
        if (group != null) group.setVisibility(on ? View.INVISIBLE : View.VISIBLE);
        if (getWindow() != null) {
            android.view.WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.screenBrightness = on ? android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF
                                     : android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            getWindow().setAttributes(lp);
        }
    }

    /** Fades the group out, moves it to a random spot inside the safe area (96 inline, 56 block), fades it in. */
    private void drift(boolean animate) {
        lastDrift = System.currentTimeMillis();
        float x = 96 + random.nextFloat() * (1920 - 2 * 96 - W);
        float y = 56 + random.nextFloat() * (1080 - 2 * 56 - H);
        if (!animate) {
            place(x, y);
            return;
        }
        group.animate().alpha(0f).setDuration(FADE_MS).withEndAction(() -> {
            place(x, y);
            group.animate().alpha(1f).setDuration(FADE_MS).start();
        }).start();
    }

    private void place(float x, float y) {
        group.setTranslationX(Ui.px(x));
        group.setTranslationY(Ui.px(y));
    }

    private TextView text(Typeface tf, float size, int color) {
        TextView v = new TextView(this);
        v.setTypeface(tf);
        v.setTextSize(TypedValue.COMPLEX_UNIT_PX, Ui.px(size));
        v.setTextColor(color);
        v.setGravity(Gravity.CENTER_HORIZONTAL);
        return v;
    }

    private LinearLayout.LayoutParams wrap(float top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.topMargin = Ui.ipx(top);
        return lp;
    }

    /** The arch's open outline: M2 580 L2 230 C2 120 100 52 220 2 C340 52 438 120 438 230 L438 580. */
    private static final class Outline extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        Outline(Context c) {
            super(c);
            p.setStyle(Paint.Style.STROKE);
            p.setColor(Tokens.SCREENSAVER_ARCH_LINE);
        }

        @Override
        protected void onDraw(Canvas c) {
            float k = getWidth() / W;
            p.setStrokeWidth(2 * k);
            path.reset();
            path.moveTo(2 * k, 580 * k);
            path.lineTo(2 * k, 230 * k);
            path.cubicTo(2 * k, 120 * k, 100 * k, 52 * k, 220 * k, 2 * k);
            path.cubicTo(340 * k, 52 * k, 438 * k, 120 * k, 438 * k, 230 * k);
            path.lineTo(438 * k, 580 * k);
            c.drawPath(path, p);
        }
    }
}
