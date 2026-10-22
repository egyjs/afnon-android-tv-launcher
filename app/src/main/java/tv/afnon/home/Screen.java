package tv.afnon.home;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.TypefaceSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;

/**
 * What every Afnon screen shares: language and digit settings, fonts, design-px placement, the clock format,
 * a once-a-second tick, and the mihrab arch with its crown, next prayer and countdown.
 */
abstract class Screen extends Activity {

    final Handler tick = new Handler(Looper.getMainLooper());

    Text t;
    boolean rtl;
    boolean h24;
    Typeface ui400, ui500, ui600, display, displayBold, sacred;
    /**
     * The display font for numbers (the clock, the countdown, times and degrees): Marcellus, as in English, and in
     * Arabic too with Western digits ({@link #marcellusDigits}); otherwise the display font.
     */
    Typeface numbers;
    /** Where numbers are in Marcellus but the words in Amiri: the font for a time's ص/م; else null. */
    Typeface marker;
    FrameLayout root;
    ArchView arch;
    TextView archLabel, archName, archCount, archSub, archSub2;
    /** {@link Prefs#key} when the screen was last built. */
    private String builtSettings;

    private final Runnable everySecond = new Runnable() {
        @Override
        public void run() {
            onTick();
            long now = System.currentTimeMillis();
            tick.postDelayed(this, 1000 - (now % 1000) + 5);
        }
    };

    /** Builds the whole screen into a fresh {@link #root}; called again when the language changes. */
    abstract void build();

    /** Once a second while the screen is in front. */
    abstract void onTick();

    /** The window theme in night mode; 0 keeps the manifest's (the call screen is see-through). */
    int nightTheme() {
        return R.style.Theme_Afnon_Night;
    }

    @Override
    protected void onCreate(Bundle state) {
        // Night mode: the night ground under the window too, before the window is made.
        Tokens.use(Prefs.night(this));
        if (Tokens.night && nightTheme() != 0) setTheme(nightTheme());
        super.onCreate(state);
        Ui.init(this);
        loadSettings(getIntent());
        build();
    }

    /** The TV's language changed (the manifest keeps the activity alive for it); rebuild if Afnon follows it. */
    @Override
    public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        boolean wasAr = t.ar;
        loadSettings(null);
        if (t.ar != wasAr) build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Back from Afnon Settings with something changed: rebuild in the new language, place or method.
        if (builtSettings != null && !builtSettings.equals(Prefs.key(this))) {
            loadSettings(null);
            build();
        }
        tick.removeCallbacks(everySecond);
        everySecond.run();
    }

    @Override
    protected void onPause() {
        tick.removeCallbacks(everySecond);
        super.onPause();
    }

    // ---------------------------------------------------------------- settings

    /**
     * Language and digits. "locale" is "system" (default: Arabic only when the TV's language is Arabic), "ar" or
     * "en"; "digits" is empty (Eastern in Arabic, Western in English), "eastern" or "western". Set in Afnon
     * Settings, or for testing: `adb shell am start -n tv.afnon.home/.MainActivity --es locale ar|en|system`.
     *
     * @return whether a setting changed since the screen was built: by the intent, or while it was away (Afnon
     *     Settings, or the phone installer through SetupProvider) and Home brought it back through onNewIntent
     */
    boolean loadSettings(Intent i) {
        SharedPreferences p = prefs();
        String built = builtSettings;
        boolean changed = false;
        if (i != null && i.hasExtra("locale")) {
            p.edit().putString("locale", i.getStringExtra("locale")).apply();
            changed = true;
        }
        if (i != null && i.hasExtra("digits")) {
            p.edit().putString("digits", i.getStringExtra("digits")).apply();
            changed = true;
        }
        t = loadText(this);
        rtl = t.ar;
        Tokens.use(Prefs.night(this));
        builtSettings = Prefs.key(this);
        return changed || (built != null && !built.equals(builtSettings));
    }

    /**
     * Arabic with Western digits: the big numbers take English's Marcellus instead of Amiri (the user's choice),
     * and the Arabic in them (ص/م) stays Amiri through a TypefaceSpan, which needs Android 9. Eastern digits keep
     * Amiri: Marcellus has none.
     */
    static boolean marcellusDigits(Text t) {
        return t.ar && !t.eastern && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
    }

    /** The saved language and digits, for screens and for the screensaver, which is not an Activity. */
    static Text loadText(Context c) {
        SharedPreferences p = c.getSharedPreferences("afnon", MODE_PRIVATE);
        String locale = p.getString("locale", "system");
        boolean ar = "system".equals(locale)
            ? "ar".equals(c.getResources().getConfiguration().getLocales().get(0).getLanguage())
            : "ar".equals(locale);
        String digits = p.getString("digits", "");
        boolean eastern = digits.isEmpty() ? ar : "eastern".equals(digits);
        Text t = new Text(ar, eastern);
        Prefs.City city = Prefs.city(c);
        t.cityName = ar ? city.ar : city.en;
        return t;
    }

    /**
     * The direction the viewer faces when watching, in degrees from true north (read once from a phone compass
     * while facing the TV); -1 until it is set.
     */
    int facing() {
        return prefs().getInt("facing", -1);
    }

    void setFacing(int degrees) {
        prefs().edit().putInt("facing", degrees).apply();
    }

    SharedPreferences prefs() {
        return getSharedPreferences("afnon", MODE_PRIVATE);
    }

    // ---------------------------------------------------------------- shared layout

    /** Fonts for the current language, and an empty root laid out in its direction over the star lattice. */
    void newRoot() {
        ui400 = getResources().getFont(R.font.alexandria_regular);
        ui500 = getResources().getFont(R.font.alexandria_medium);
        ui600 = getResources().getFont(R.font.alexandria_semibold);
        display = getResources().getFont(t.ar ? R.font.amiri_regular : R.font.marcellus_regular);
        displayBold = t.ar ? getResources().getFont(R.font.amiri_bold) : display;
        sacred = getResources().getFont(R.font.amiri_regular);
        numbers = marcellusDigits(t) ? getResources().getFont(R.font.marcellus_regular) : display;
        marker = numbers != display ? display : null;

        // A chooser open over the old root goes with it; left set, it would swallow every key while nothing shows.
        chooser = null;
        chooserBack = null;
        root = new FrameLayout(this);
        root.setLayoutDirection(rtl ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        root.setBackground(Ui.starLattice(this));
        root.setClipChildren(false);
    }

    /**
     * The arch on the start side: crown verse (never focusable, never covered), reference, rule, then the info
     * column at infoTop: label, prayer name, countdown and two sub lines.
     */
    void buildArch(float infoTop, float subSize, float subGap, int sub2Color, float sub2Gap) {
        arch = new ArchView(this);
        place(arch, Tokens.ARCH_X, Tokens.ARCH_Y, Tokens.ARCH_W, Tokens.ARCH_H);

        TextView verse = text(sacred, 32, Tokens.GOLD_400);
        verse.setText("إِنَّ الصَّلَاةَ كَانَتْ\nعَلَى الْمُؤْمِنِينَ\nكِتَابًا مَّوْقُوتًا");
        verse.setGravity(Gravity.CENTER_HORIZONTAL);
        verse.setTextDirection(View.TEXT_DIRECTION_RTL);
        verse.setLineHeight(Ui.ipx(32 * 1.55f));
        place(verse, 156, 238, 480, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView ref = text(ui400, 20, Tokens.MIST_400);
        ref.setText(t.ref());
        ref.setGravity(Gravity.CENTER_HORIZONTAL);
        place(ref, 156, 410, 480, ViewGroup.LayoutParams.WRAP_CONTENT);

        View rule = new View(this);
        rule.setBackgroundColor(Tokens.GOLD_400);
        rule.setAlpha(0.6f);
        place(rule, 336, 460, 120, 2);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setGravity(Gravity.CENTER_HORIZONTAL);
        archLabel = caps(text(ui400, 22, Tokens.MIST_400));
        archName = text(displayBold, 88, Tokens.GOLD_400);
        archCount = text(numbers, 150, Tokens.PLASTER_100);
        archCount.setTextDirection(View.TEXT_DIRECTION_LTR);
        archCount.setFontFeatureSettings("tnum");
        archSub = text(ui400, subSize, Tokens.MIST_400);
        archSub2 = text(ui400, 24, sub2Color);
        for (TextView v : new TextView[] {archLabel, archName, archCount, archSub, archSub2}) {
            v.setGravity(Gravity.CENTER_HORIZONTAL);
            info.addView(v, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        // The countdown spans the column from a start renderCountdown sets, so it never re-centres each second.
        archCount.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
        archCount.setGravity(Gravity.LEFT);
        countHead = null;
        ((LinearLayout.LayoutParams) archSub.getLayoutParams()).topMargin = Ui.ipx(subGap);
        ((LinearLayout.LayoutParams) archSub2.getLayoutParams()).topMargin = Ui.ipx(sub2Gap);
        lineHeight(archName, 1.15f);
        lineHeight(archCount, 1f);
        info.setClipChildren(false);
        place(info, 136, infoTop, ARCH_INFO_W, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    /** The arch's info column, and the countdown's text before its last two digits when its start was set. */
    private static final float ARCH_INFO_W = 520;
    private String countHead;

    /**
     * The arch countdown to day's next prayer: mm:ss under an hour at 150px, h:mm:ss above at 120px, and the
     * arch outline turns gold-200 inside the last {@link Tokens#APPROACH_MIN} minutes.
     *
     * @return seconds left
     */
    long renderCountdown(PrayerDay day, Date now, boolean archFocused) {
        long left = secondsUntil(day.nextTime, now);
        long h = left / 3600;
        String count = t.d(duration(left, true));
        archCount.setText(count);
        float countSize = Ui.px(h > 0 ? Tokens.FONT_SIZE_DISPLAY_XL_HOURS : Tokens.FONT_SIZE_DISPLAY_XL);
        if (archCount.getTextSize() != countSize) {
            archCount.setTextSize(TypedValue.COMPLEX_UNIT_PX, countSize);
            lineHeight(archCount, 1f);
            countHead = null;
        }
        // Marcellus has no tabular digits (its 0 is nearly three 1s wide), so a centred countdown would sway every
        // second. It starts where the text is centred with average-width seconds, set again only when something
        // before the seconds changes: a digit that isn't changing never moves. Amiri's equal digits centre exactly.
        String head = count.substring(0, count.length() - 2);
        if (!head.equals(countHead)) {
            countHead = head;
            android.graphics.Paint p = archCount.getPaint();
            float w = p.measureText(head) + p.measureText(t.d("0123456789")) / 5;
            archCount.setPadding(Math.max(0, Math.round((Ui.px(ARCH_INFO_W) - w) / 2)), 0, 0, 0);
        }
        boolean approaching = left <= Tokens.APPROACH_MIN * 60L;
        arch.setState(approaching ? ArchView.APPROACHING : (archFocused ? ArchView.FOCUSED : ArchView.REST));
        return left;
    }

    static long secondsUntil(Date when, Date now) {
        return Math.max(0, (when.getTime() - now.getTime() + 999) / 1000);
    }

    /** mm:ss under an hour; above it h:mm:ss, or h:mm without seconds. Western digits; pass through t.d(). */
    static String duration(long seconds, boolean withSeconds) {
        long h = seconds / 3600, m = (seconds % 3600) / 60, s = seconds % 60;
        if (h == 0) return String.format(Locale.US, "%02d:%02d", m, s);
        return withSeconds ? String.format(Locale.US, "%d:%02d:%02d", h, m, s) : String.format(Locale.US, "%d:%02d", h, m);
    }

    /** "21 October 2026" in the screen's language and digits; the weekday goes with the Hijri date. */
    String gregorian(Date now) {
        return t.d(new SimpleDateFormat("d MMMM yyyy", dateLocale(t)).format(now));
    }

    /** "Saturday 10 Jumada al-Ula 1448 AH": the weekday turns at Maghrib with the Hijri date. */
    String hijri(PrayerDay day) {
        return t.d(weekday(t, day) + " " + day.hijriDay + " " + t.hijriMonths()[day.hijriMonth] + " " + day.hijriYear
            + t.hijriSuffix());
    }

    /** The Hijri day's weekday name ("السبت" from Friday's Maghrib). */
    static String weekday(Text t, PrayerDay day) {
        return new java.text.DateFormatSymbols(dateLocale(t)).getWeekdays()[day.weekday];
    }

    private static Locale dateLocale(Text t) {
        return t.ar ? Locale.forLanguageTag("ar-EG-u-nu-latn") : Locale.UK;
    }

    /**
     * A clock time in the 12/24-hour format from Afnon Settings, by default the TV's own (Settings → Date & time);
     * call {@link #readClockFormat()} first. In 12-hour the marker (ص/م, AM/PM) is drawn at markerScale of the digits; 1 for running text.
     */
    CharSequence time(Date d, float markerScale) {
        return clock(t, d, h24, markerScale);
    }

    /** {@link #time} for a view in {@link #numbers}: its ص/م in the Arabic font. */
    CharSequence displayTime(Date d, float markerScale) {
        return clock(t, d, h24, markerScale, marker);
    }

    static CharSequence clock(Text t, Date d, boolean h24, float markerScale) {
        return clock(t, d, h24, markerScale, null);
    }

    /** marker, if given, is the typeface for the ص/م after digits drawn in Marcellus (Android 9+). */
    static CharSequence clock(Text t, Date d, boolean h24, float markerScale, Typeface marker) {
        if (h24) return t.d(new SimpleDateFormat("HH:mm", Locale.US).format(d));
        String hm = t.d(new SimpleDateFormat("h:mm", Locale.US).format(d));
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        SpannableString s = new SpannableString(hm + " " + t.ampm(c.get(Calendar.AM_PM) == Calendar.AM));
        if (markerScale != 1f) s.setSpan(new RelativeSizeSpan(markerScale), hm.length(), s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (marker != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            s.setSpan(new TypefaceSpan(marker), hm.length() + 1, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return s;
    }

    void readClockFormat() {
        h24 = Prefs.clock24(this);
    }

    /** The key hints along the bottom, 40px apart. */
    void setHints(LinearLayout hints, List<String> h) {
        hints.removeAllViews();
        for (int i = 0; i < h.size(); i++) {
            TextView v = text(ui400, 22, Tokens.MIST_400);
            v.setText(h.get(i));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            if (i > 0) lp.setMarginStart(Ui.ipx(40));
            hints.addView(v, lp);
        }
    }

    /** The connected Wi-Fi network's name, or null when there is none or it can't be read. */
    String ssid() {
        String ssid = null;
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wm == null ? null : wm.getConnectionInfo();
            if (info != null) ssid = info.getSSID();
        } catch (SecurityException ignored) {
            // Location not granted: no name.
        }
        if (ssid == null || ssid.isEmpty() || "<unknown ssid>".equals(ssid)) return null;
        if (ssid.length() > 1 && ssid.startsWith("\"") && ssid.endsWith("\"")) ssid = ssid.substring(1, ssid.length() - 1);
        return ssid;
    }

    void startSafely(Intent i) {
        try {
            startActivity(i);
        } catch (ActivityNotFoundException | SecurityException ignored) {
            // Nothing to open on this firmware; stay where we are.
        }
    }

    // ---------------------------------------------------------------- helpers

    TextView text(Typeface tf, float sizeDesignPx, int color) {
        TextView v = new TextView(this);
        v.setTypeface(tf);
        v.setTextSize(TypedValue.COMPLEX_UNIT_PX, Ui.px(sizeDesignPx));
        v.setTextColor(color);
        return v;
    }

    /**
     * CSS line-height for a single line already in a LinearLayout: the glyph box is centred in a line box of
     * lh × size. Android keeps the font's full ascent and descent instead, which for Amiri is far taller than the
     * design, so negative margins take back the difference. Safe to call again after the text size changes.
     */
    static void lineHeight(TextView v, float lh) {
        v.setIncludeFontPadding(false);
        android.graphics.Paint.FontMetricsInt fm = v.getPaint().getFontMetricsInt();
        int half = Math.round((v.getTextSize() * lh - (fm.descent - fm.ascent)) / 2);
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
        Object base = v.getTag();
        int[] b = base instanceof int[] ? (int[]) base : new int[] {lp.topMargin, lp.bottomMargin};
        v.setTag(b);
        lp.topMargin = b[0] + half;
        lp.bottomMargin = b[1] + half;
        v.setLayoutParams(lp);
    }

    /** English caps labels get 0.2em tracking; Arabic is never letter-spaced. */
    TextView caps(TextView v) {
        if (!t.ar) v.setLetterSpacing(0.2f);
        return v;
    }

    /** Places a view at a design rect, measured from the start edge, so Arabic mirrors it. */
    void place(View v, float x, float y, float w, float h) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size(w), size(h), Gravity.TOP | Gravity.START);
        lp.setMarginStart(Ui.ipx(x));
        lp.topMargin = Ui.ipx(y);
        root.addView(v, lp);
    }

    /** Places a view measured from the end edge; a focus tile grows by the ring's reach. */
    void placeEnd(View v, float endInset, float y, float w, float h) {
        float r = v instanceof FocusTile ? FocusTile.REACH : 0;
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            w < 0 ? (int) w : size(w + 2 * r), size(h + 2 * r), Gravity.TOP | Gravity.END);
        lp.setMarginEnd(Ui.ipx(endInset - r));
        lp.topMargin = Ui.ipx(y - r);
        root.addView(v, lp);
    }

    /** Places a focus tile by its design rect; the view grows by the ring's reach on each side. */
    void placeTile(FocusTile v, float x, float y, float w, float h) {
        float r = FocusTile.REACH;
        place(v, x - r, y - r, w + 2 * r, h + 2 * r);
    }

    static int size(float designPx) {
        return designPx < 0 ? (int) designPx : Ui.ipx(designPx);
    }

    // ---------------------------------------------------------------- list chooser

    /** The list open over the screen (Settings' lists, the station from Home's radio card), or null. */
    FrameLayout chooser;
    /** What Back in the open chooser goes back to (the city list returns to the countries); null closes it. */
    Runnable chooserBack;

    /** The screen's key hints, which an open chooser takes over; null for none. */
    LinearLayout hintLine() {
        return null;
    }

    /** A chooser opened (the screen's own buttons step aside) or closed (they come back, and focus with them). */
    void onChooser(boolean open) {}

    /** A list over the screen (cities, methods, stations): OK picks, Back cancels. Up/Down are the system's own. */
    void openChooser(String title, String[] items, int selected, IntConsumer pick) {
        openChooser(title, items, selected, pick, null);
    }

    /** onFocus, if given, hears each item as focus reaches it (the adhan list plays that voice). */
    void openChooser(String title, String[] items, int selected, IntConsumer pick, IntConsumer onFocus) {
        chooser = new FrameLayout(this);
        chooser.setBackgroundColor(Tokens.SCRIM_MENU);
        chooser.setClickable(true);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.ipx(48), Ui.ipx(40), Ui.ipx(48), Ui.ipx(32));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Tokens.LAPIS_800);
        bg.setCornerRadius(Ui.px(24));
        bg.setStroke(Ui.ipx(2), Tokens.GOLD_400_60);
        card.setBackground(bg);
        card.setClipChildren(true);

        TextView head = text(displayBold, Tokens.FONT_SIZE_DISPLAY_S, Tokens.GOLD_400);
        head.setText(title);
        card.addView(head);
        lineHeight(head, 1.2f);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        // No fading edges: focus then scrolls by whole items, so none is ever cut at the top or bottom.
        scroll.setVerticalFadingEdgeEnabled(false);
        int reach = Ui.ipx(FocusTile.REACH);
        // Padding at the sides only: ScrollView's focus scrolling ignores its own padding, so a focused tile could
        // stop under a bottom padding and be clipped. Each tile already holds its focus ring (its reach) inside.
        scroll.setPadding(reach, 0, reach, 0);
        scroll.setClipToPadding(true);
        scroll.setClipChildren(true);
        int itemH = Ui.ipx(68 + 2 * FocusTile.REACH), gap = Ui.ipx(6 - 2 * FocusTile.REACH);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setClipChildren(false);
        FocusTile first = null;
        for (int i = 0; i < items.length; i++) {
            final int index = i;
            boolean chosen = i == selected;
            FocusTile item = new FocusTile(this, 14);
            item.setFocusScale(1f);   // the list scrolls, so a grown tile would be clipped at its edges
            item.setRest(chosen ? Tokens.LAPIS_700 : 0, 0, 0);
            TextView v = text(chosen ? ui500 : ui400, 26, Tokens.PLASTER_100);
            v.setText(items[i]);
            v.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            v.setPadding(Ui.ipx(24), 0, Ui.ipx(24), 0);
            item.addView(v, new FrameLayout.LayoutParams(-1, -1));
            item.setLook(f -> {
                v.setTextColor(f ? Tokens.FOCUS_TEXT : (chosen ? Tokens.GOLD_400 : Tokens.PLASTER_100));
                if (f && onFocus != null) onFocus.accept(index);
            });
            item.setOnClickListener(x -> {
                pick.accept(index);
                closeChooser();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, itemH);
            if (i > 0) lp.topMargin = gap;
            list.addView(item, lp);
            if (chosen || first == null) first = item;
        }
        scroll.addView(list);
        // Up to eight whole tiles, rings included, in the same screen px as the tiles (no rounding gap).
        int shown = Math.max(1, Math.min(items.length, 8));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, shown * itemH + (shown - 1) * gap);
        slp.topMargin = Ui.ipx(20);
        slp.setMarginStart(-reach);
        slp.setMarginEnd(-reach);
        card.addView(scroll, slp);

        chooser.addView(card, new FrameLayout.LayoutParams(Ui.ipx(900), -2, Gravity.CENTER));
        onChooser(true);
        root.addView(chooser, new FrameLayout.LayoutParams(-1, -1));
        FocusTile start = first;
        if (start != null) root.post(start::requestFocus);
        LinearLayout hints = hintLine();
        if (hints == null) return;
        List<String> h = new java.util.ArrayList<>();
        h.add(t.hintMove());
        h.add(t.hintChoose());
        h.add(t.hintBackCancel());
        setHints(hints, h);
    }

    void closeChooser() {
        root.removeView(chooser);
        chooser = null;
        chooserBack = null;
        onChooser(false);
    }

    /** Keys while a chooser is open: Back goes back a step (or closes it), ◀▶ do nothing, the rest are the system's. */
    boolean chooserKey(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.KEYCODE_BACK) {
            if (e.getAction() != KeyEvent.ACTION_DOWN) {
                Runnable back = chooserBack;
                closeChooser();
                if (back != null) back.run();
            }
            return true;
        }
        if (k == KeyEvent.KEYCODE_DPAD_LEFT || k == KeyEvent.KEYCODE_DPAD_RIGHT) return true;
        return super.dispatchKeyEvent(e);
    }

    /**
     * Station (Settings → Radio, and Home's radio card): the broadcast stations or the reciters, then one of them.
     * Playing radio moves to the pick at once (Radio.switchTo); picked runs after it.
     */
    void chooseStation(Runnable picked) {
        int sel = Radio.station(this).reciter ? 1 : 0;
        // The station list opens once this chooser has closed (closeChooser runs after the pick).
        openChooser(t.stationRow(), t.stationGroups(), sel, i -> root.post(() -> openStations(i == 1, picked)));
    }

    /** Station's second step: one group's stations. Back returns to the groups. */
    private void openStations(boolean reciters, Runnable picked) {
        List<Integer> found = new java.util.ArrayList<>();
        for (int i = 0; i < Radio.STATIONS.length; i++) if (Radio.STATIONS[i].reciter == reciters) found.add(i);
        String[] names = new String[found.size()];
        int current = Radio.stationIndex(this);
        // -1 when the station is in the other group: nothing is marked, focus starts on the first one.
        int sel = -1;
        for (int k = 0; k < names.length; k++) {
            names[k] = t.stationChoice(Radio.STATIONS[found.get(k)]);
            if (found.get(k) == current) sel = k;
        }
        openChooser(t.stationGroups()[reciters ? 1 : 0], names, sel, k -> {
            Radio.switchTo(this, found.get(k));
            picked.run();
        });
        chooserBack = () -> chooseStation(picked);
    }
}
