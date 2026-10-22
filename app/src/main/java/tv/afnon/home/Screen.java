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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

    @Override
    protected void onCreate(Bundle state) {
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
     * @return whether the intent changed a setting
     */
    boolean loadSettings(Intent i) {
        SharedPreferences p = prefs();
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
        builtSettings = Prefs.key(this);
        return changed;
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
        archCount = text(display, 150, Tokens.PLASTER_100);
        archCount.setTextDirection(View.TEXT_DIRECTION_LTR);
        archCount.setFontFeatureSettings("tnum");
        archSub = text(ui400, subSize, Tokens.MIST_400);
        archSub2 = text(ui400, 24, sub2Color);
        for (TextView v : new TextView[] {archLabel, archName, archCount, archSub, archSub2}) {
            v.setGravity(Gravity.CENTER_HORIZONTAL);
            info.addView(v, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        ((LinearLayout.LayoutParams) archSub.getLayoutParams()).topMargin = Ui.ipx(subGap);
        ((LinearLayout.LayoutParams) archSub2.getLayoutParams()).topMargin = Ui.ipx(sub2Gap);
        lineHeight(archName, 1.15f);
        lineHeight(archCount, 1f);
        info.setClipChildren(false);
        place(info, 136, infoTop, 520, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    /**
     * The arch countdown to day's next prayer: mm:ss under an hour at 150px, h:mm:ss above at 120px, and the
     * arch outline turns gold-200 inside the last {@link Tokens#APPROACH_MIN} minutes.
     *
     * @return seconds left
     */
    long renderCountdown(PrayerDay day, Date now, boolean archFocused) {
        long left = secondsUntil(day.nextTime, now);
        long h = left / 3600;
        archCount.setText(t.d(duration(left, true)));
        float countSize = Ui.px(h > 0 ? Tokens.FONT_SIZE_DISPLAY_XL_HOURS : Tokens.FONT_SIZE_DISPLAY_XL);
        if (archCount.getTextSize() != countSize) {
            archCount.setTextSize(TypedValue.COMPLEX_UNIT_PX, countSize);
            lineHeight(archCount, 1f);
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

    /** "Wednesday 21 October 2026" in the screen's language and digits. */
    String gregorian(Date now) {
        Locale dateLocale = t.ar ? Locale.forLanguageTag("ar-EG-u-nu-latn") : Locale.UK;
        return t.d(new SimpleDateFormat("EEEE d MMMM yyyy", dateLocale).format(now));
    }

    /** "10 Jumada al-Ula 1448 AH". */
    String hijri(PrayerDay day) {
        return t.d(day.hijriDay + " " + t.hijriMonths()[day.hijriMonth] + " " + day.hijriYear + t.hijriSuffix());
    }

    /**
     * A clock time in the 12/24-hour format from Afnon Settings, by default the TV's own (Settings → Date & time);
     * call {@link #readClockFormat()} first. In 12-hour the marker (ص/م, AM/PM) is drawn at markerScale of the digits; 1 for running text.
     */
    CharSequence time(Date d, float markerScale) {
        return clock(t, d, h24, markerScale);
    }

    static CharSequence clock(Text t, Date d, boolean h24, float markerScale) {
        if (h24) return t.d(new SimpleDateFormat("HH:mm", Locale.US).format(d));
        String hm = t.d(new SimpleDateFormat("h:mm", Locale.US).format(d));
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        SpannableString s = new SpannableString(hm + " " + t.ampm(c.get(Calendar.AM_PM) == Calendar.AM));
        if (markerScale != 1f) s.setSpan(new RelativeSizeSpan(markerScale), hm.length(), s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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

    /**
     * Afnon's own screens open with no transition: the one in front stays until the new one has drawn. (Its
     * replacement, overrideActivityTransition, is Android 14+; these TVs run 11 and 12.)
     */
    @SuppressWarnings("deprecation")
    @Override
    public void startActivity(Intent i, Bundle options) {
        super.startActivity(i, options);
        if (i.getComponent() != null && getPackageName().equals(i.getComponent().getPackageName())) {
            overridePendingTransition(0, 0);
        }
    }

    /** …and close with none. */
    @SuppressWarnings("deprecation")
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
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
}
