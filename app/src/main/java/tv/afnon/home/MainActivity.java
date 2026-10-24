package tv.afnon.home;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.icu.util.IslamicCalendar;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.batoulapps.adhan.Coordinates;
import com.batoulapps.adhan.Madhab;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Afnon Home: the mihrab arch on the start side, the courtyard (clock, prayer strip, apps) beside it. */
public class MainActivity extends Screen {

    // Focus nodes, in the order of design/FocusMap.dc.html.
    // Section labels sit 10px above the design (478 / 722) so the focus ring and 1.06 scale never touch them.
    private static final int LABEL_ABOVE_FAV = 468, LABEL_ABOVE_MORE = 712;

    private static final int ARCH = 0, WIFI = 1, SETTINGS = 2, F0 = 3, M0 = 7, ALL = 12, RADIO = 13, NODES = 14;

    private final View[] nodes = new View[NODES];
    private final TextView[] cellTag = new TextView[6], cellName = new TextView[6], cellTime = new TextView[6];
    private final View[] cell = new View[6];
    private final String[] nodeLabel = new String[NODES];

    private TextView clock, gdate, hdate, wifiName;
    private LinearLayout hints;
    private PrayerDay day;
    private long lastMinute = -1;
    private int lastFav = F0;
    private boolean archFocused;
    /** What Home was built with, so it rebuilds when All apps changes favourites or apps come and go. */
    private String builtApps = "";
    /** The reminder card (design/Home.dc.html, mode "approach") and the prayer it is for. */
    private LinearLayout card;
    private FocusTile cardOk, cardDay;
    private Date cardFor;
    /** The radio's header pill and, while open, its card. */
    private Icon radioIcon, playIcon;
    private RadioBars pillBars, cardBars;
    private TextView radioLabel, cardStatus, playLabel;
    private LinearLayout radioCard;
    private FocusTile cardPlay, cardDark;
    private final Radio.Listener onRadio = state -> styleRadio();
    /** Test only (shiftTo): how far Home's clock is moved. */
    private long shift;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        runTest(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        boolean changed = loadSettings(intent);
        if (changed) build();
        // Home key from anywhere: back to Home with focus on the arch (FocusMap: Home key). Posted, because the
        // window puts back its previously focused view when it regains focus after onNewIntent.
        root.post(() -> nodes[ARCH].requestFocus());
        runTest(intent);
    }

    /**
     * Checking reminders on the TV without waiting for a prayer: `--es test reminder` shows Home's card now, with
     * the spoken reminder if it is on; `banner`, `call` and `announce` come from the remote buttons service 8 s
     * later, time enough to open another app.
     * `--es station <id>` (ids in Radio.STATIONS) picks a radio station as Settings does, before any test runs.
     */
    private void runTest(Intent i) {
        String station = i == null ? null : i.getStringExtra("station");
        if (station != null) {
            i.removeExtra("station");
            for (int k = 0; k < Radio.STATIONS.length; k++) if (Radio.STATIONS[k].id.equals(station)) Radio.switchTo(this, k);
        }
        String kind = i == null ? null : i.getStringExtra("test");
        if (kind == null) return;
        i.removeExtra("test");
        if ("reminder".equals(kind)) root.post(() -> {
            showReminder();
            if (Prefs.remindVoice(this)) Announce.before(this, day.nextIndex, day.friday);
        });
        else if ("friday".equals(kind) || "ramadan".equals(kind) || "now".equals(kind)) shiftTo(kind);
        else if ("radio".equals(kind)) Radio.toggle(this);
        else if (RemoteKeys.watch != null) RemoteKeys.watch.test(kind, 8000);
    }

    /**
     * Test only: Home's clock jumps to the coming Friday at 10:30 (Jumu'ah next) or to 15 Ramadan at 16:30
     * (iftar next) and keeps ticking from there; "now" puts it back. Nothing else in Afnon is shifted.
     */
    private void shiftTo(String kind) {
        Calendar c = Calendar.getInstance();
        if ("friday".equals(kind)) {
            while (c.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) c.add(Calendar.DAY_OF_MONTH, 1);
            c.set(Calendar.HOUR_OF_DAY, 10);
            c.set(Calendar.MINUTE, 30);
        } else if ("ramadan".equals(kind)) {
            IslamicCalendar h = new IslamicCalendar();
            h.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);
            if (h.get(IslamicCalendar.MONTH) > IslamicCalendar.RAMADAN) h.add(IslamicCalendar.YEAR, 1);
            h.set(IslamicCalendar.MONTH, IslamicCalendar.RAMADAN);
            h.set(IslamicCalendar.DAY_OF_MONTH, 15);
            c.setTime(h.getTime());
            c.set(Calendar.HOUR_OF_DAY, 16);
            c.set(Calendar.MINUTE, 30);
        }
        shift = "now".equals(kind) ? 0 : c.getTimeInMillis() - System.currentTimeMillis();
        lastMinute = -1;
        day = null;
    }

    private Date now() {
        return new Date(System.currentTimeMillis() + shift);
    }

    @Override
    protected void onResume() {
        if (!builtApps.equals(appsKey())) build();
        lastMinute = -1;
        updateWifi();
        super.onResume();
        Reminders.home = this::showReminder;
        Radio.listen(onRadio);
        styleRadio();
    }

    @Override
    protected void onPause() {
        Reminders.home = null;
        Radio.unlisten(onRadio);
        super.onPause();
    }

    @Override
    void onTick() {
        render(false);
        // The prayer the card was about has arrived (the call takes over) or the card outlived its prayer.
        if (card != null && day != null && !day.nextTime.equals(cardFor)) dismissReminder();
    }

    // ---------------------------------------------------------------- layout

    @Override
    void build() {
        newRoot();
        card = null;   // a reminder card went with the old root
        radioCard = null;
        buildArch(488, 26, 12, Tokens.GOLD_200, 8);

        nodes[ARCH] = archButton();

        // Courtyard: clock and dates, then Wi-Fi and Settings at the end side.
        LinearLayout when = new LinearLayout(this);
        when.setOrientation(LinearLayout.VERTICAL);
        clock = text(display, 96, Tokens.PLASTER_100);
        clock.setIncludeFontPadding(false);
        clock.setTextDirection(rtl ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR);
        clock.setFontFeatureSettings("tnum");
        // Under the clock the Hijri date (with the city) first, then the Gregorian, smaller and in mist.
        hdate = text(ui400, 26, Tokens.PLASTER_100);
        gdate = text(ui400, 24, Tokens.MIST_400);
        when.addView(clock);
        when.addView(hdate);
        when.addView(gdate);
        ((LinearLayout.LayoutParams) hdate.getLayoutParams()).topMargin = Ui.ipx(6);
        ((LinearLayout.LayoutParams) gdate.getLayoutParams()).topMargin = Ui.ipx(6);
        lineHeight(clock, 1f);
        when.setClipChildren(false);
        place(when, Tokens.COURT_X, 56, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // Radio and Wi-Fi side by side, ending 20px before the Settings button.
        nodes[RADIO] = radioPill();
        nodes[WIFI] = wifiPill();
        LinearLayout pills = new LinearLayout(this);
        pills.setClipChildren(false);
        float reach = FocusTile.REACH;
        pills.addView(nodes[RADIO], new LinearLayout.LayoutParams(-2, Ui.ipx(72 + 2 * reach)));
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(-2, Ui.ipx(72 + 2 * reach));
        wlp.setMarginStart(Ui.ipx(20 - 2 * reach));
        pills.addView(nodes[WIFI], wlp);
        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        plp.setMarginEnd(Ui.ipx(1920 - (Tokens.COURT_X + Tokens.COURT_W) + 72 + 20 - reach));
        plp.topMargin = Ui.ipx(56 - reach);
        root.addView(pills, plp);
        nodes[SETTINGS] = settingsButton();

        // The prayer strip: information only, never focusable.
        float cw = (Tokens.COURT_W - 5 * 12) / 6f;
        for (int i = 0; i < 6; i++) {
            LinearLayout v = new LinearLayout(this);
            v.setOrientation(LinearLayout.VERTICAL);
            v.setPadding(Ui.ipx(20), Ui.ipx(16), Ui.ipx(20), Ui.ipx(16));
            cellTag[i] = text(ui400, 20, Tokens.PLASTER_100);
            cellName[i] = text(ui400, 26, Tokens.PLASTER_100);
            cellTime[i] = text(ui400, 34, Tokens.PLASTER_100);
            cellTime[i].setTextDirection(rtl ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR);
            cellTime[i].setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            cellTime[i].setFontFeatureSettings("tnum");
            cellTime[i].setSingleLine(true);
            v.addView(cellTag[i], new LinearLayout.LayoutParams(-1, 0, 1));
            v.addView(cellName[i], new LinearLayout.LayoutParams(-1, -2));
            v.addView(cellTime[i], new LinearLayout.LayoutParams(-1, -2));
            lineHeight(cellName[i], 1.1f);
            lineHeight(cellTime[i], 1.1f);
            v.setClipChildren(false);
            cell[i] = v;
            place(v, Tokens.COURT_X + i * (cw + 12), 290, cw, 146);
        }

        TextView fav = caps(text(ui400, 22, Tokens.MIST_400));
        fav.setText(t.fav());
        place(fav, Tokens.COURT_X, LABEL_ABOVE_FAV, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        float fw = (Tokens.COURT_W - 3 * 24) / 4f;
        List<String> favs = Apps.slots(this, t);
        for (int i = 0; i < 4; i++) {
            nodes[F0 + i] = favouriteTile(favs.get(i), F0 + i, Tokens.COURT_X + i * (fw + 24), 520, fw);
        }

        TextView more = caps(text(ui400, 22, Tokens.MIST_400));
        more.setText(t.more());
        place(more, Tokens.COURT_X, LABEL_ABOVE_MORE, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        float mw = (Tokens.COURT_W - 5 * 20) / 6f;
        List<String> moreApps = Apps.more(this, t, 5);
        for (int i = 0; i < 5; i++) {
            nodes[M0 + i] = moreTile(i < moreApps.size() ? moreApps.get(i) : null, M0 + i, Tokens.COURT_X + i * (mw + 20), 764, mw);
        }
        nodes[ALL] = allAppsTile(Tokens.COURT_X + 5 * (mw + 20), 764, mw);

        hints = new LinearLayout(this);
        hints.setOrientation(LinearLayout.HORIZONTAL);
        place(hints, Tokens.COURT_X, 962, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        for (int i = 0; i < NODES; i++) {
            final int n = i;
            nodes[i].setOnFocusChangeListener((v, has) -> { if (has) onNodeFocused(n); else if (n == ARCH) setArchFocused(false); });
        }

        setContentView(root);
        builtApps = appsKey();
        lastMinute = -1;
        render(true);
        nodes[ARCH].requestFocus();
    }

    private View archButton() {
        FocusTile b = new FocusTile(this, 36);
        b.setRest(0, Tokens.GOLD_400_60, 2);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        Icon compass = new Icon(this, Icon.COMPASS, 1.8f);
        TextView label = text(ui400, 26, Tokens.PLASTER_100);
        label.setText(t.today());
        row.addView(compass, new LinearLayout.LayoutParams(Ui.ipx(30), Ui.ipx(30)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMarginStart(Ui.ipx(14));
        row.addView(label, lp);
        b.addView(row, new FrameLayout.LayoutParams(-1, -1));
        b.setLook(f -> {
            int c = f ? Tokens.LAPIS_900 : Tokens.PLASTER_100;
            label.setTextColor(c);
            compass.setColor(c);
        });
        b.setOnClickListener(v -> startActivity(new Intent(this, PrayerDayActivity.class)));
        nodeLabel[ARCH] = t.today();
        placeTile(b, 176, 904, 440, 72);
        return b;
    }

    private View wifiPill() {
        FocusTile b = new FocusTile(this, 36);
        b.setRest(Tokens.LAPIS_800, Tokens.LINE_SOFT, 2);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.ipx(28), 0, Ui.ipx(28), 0);
        Icon icon = new Icon(this, Icon.WIFI, 2f);
        wifiName = text(ui400, 24, Tokens.PLASTER_100);
        wifiName.setTextDirection(View.TEXT_DIRECTION_LTR);
        row.addView(icon, new LinearLayout.LayoutParams(Ui.ipx(30), Ui.ipx(30)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMarginStart(Ui.ipx(14));
        row.addView(wifiName, lp);
        b.addView(row, new FrameLayout.LayoutParams(-2, -1));
        b.setLook(f -> {
            int c = f ? Tokens.LAPIS_900 : Tokens.PLASTER_100;
            wifiName.setTextColor(c);
            icon.setColor(c);
        });
        b.setOnClickListener(v -> startSafely(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        nodeLabel[WIFI] = t.network();
        return b;
    }

    private View settingsButton() {
        FocusTile b = new FocusTile(this, 36);
        b.setRest(Tokens.LAPIS_800, Tokens.LINE_SOFT, 2);
        Icon gear = new Icon(this, Icon.GEAR, 1.8f);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Ui.ipx(34), Ui.ipx(34), Gravity.CENTER);
        b.addView(gear, lp);
        b.setContentDescription(t.settings());
        b.setLook(f -> gear.setColor(f ? Tokens.LAPIS_900 : Tokens.PLASTER_100));
        b.setOnClickListener(v -> startSafely(new Intent(this, SettingsActivity.class)));
        nodeLabel[SETTINGS] = t.settings();
        placeEnd(b, 1920 - (Tokens.COURT_X + Tokens.COURT_W), 56, 72, 72);
        return b;
    }

    private View favouriteTile(String pkg, int node, float x, float y, float w) {
        FocusTile b = new FocusTile(this, Tokens.RADIUS_TILE);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.ipx(24), Ui.ipx(22), Ui.ipx(24), Ui.ipx(22));
        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.FIT_START);
        Drawable d = Apps.icon(this, pkg);
        if (d != null) icon.setImageDrawable(d);
        String name = pkg == null ? "" : Apps.name(this, t, pkg);
        TextView label = text(ui500, Apps.isLatin(name) ? 28 : 30, Tokens.PLASTER_100);
        label.setText(name);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        col.addView(icon, new LinearLayout.LayoutParams(Ui.ipx(48), Ui.ipx(48)));
        col.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1));
        col.addView(label, new LinearLayout.LayoutParams(-1, -2));
        b.addView(col, new FrameLayout.LayoutParams(-1, -1));
        b.setLook(f -> label.setTextColor(f ? Tokens.LAPIS_900 : Tokens.PLASTER_100));
        b.setOnClickListener(v -> { if (pkg != null) Apps.launch(this, pkg); });
        nodeLabel[node] = name;
        placeTile(b, x, y, w, 150);
        return b;
    }

    private View moreTile(String pkg, int node, float x, float y, float w) {
        FocusTile b = new FocusTile(this, Tokens.RADIUS_CELL);
        String name = pkg == null ? "" : Apps.name(this, t, pkg);
        TextView label = text(ui400, 22, Tokens.PLASTER_100);
        label.setText(name);
        label.setMaxLines(2);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setPadding(Ui.ipx(18), Ui.ipx(18), Ui.ipx(18), Ui.ipx(18));
        label.setGravity(Gravity.BOTTOM | Gravity.START);
        b.addView(label, new FrameLayout.LayoutParams(-1, -1));
        b.setLook(f -> label.setTextColor(f ? Tokens.LAPIS_900 : Tokens.PLASTER_100));
        b.setOnClickListener(v -> { if (pkg != null) Apps.launch(this, pkg); });
        nodeLabel[node] = name;
        placeTile(b, x, y, w, 112);
        return b;
    }

    private View allAppsTile(float x, float y, float w) {
        FocusTile b = new FocusTile(this, Tokens.RADIUS_CELL);
        b.setRest(0, Tokens.LINE_DASH, 2).setDashed();
        TextView label = text(ui400, 22, Tokens.PLASTER_100);
        // Two lines: the dashed tile is too narrow for "All apps · N" on one.
        label.setText(t.all(Apps.installed(this).size()));
        label.setPadding(Ui.ipx(12), Ui.ipx(18), Ui.ipx(12), Ui.ipx(18));
        label.setGravity(Gravity.BOTTOM | Gravity.START);
        b.addView(label, new FrameLayout.LayoutParams(-1, -1));
        b.setLook(f -> label.setTextColor(f ? Tokens.LAPIS_900 : Tokens.PLASTER_100));
        b.setOnClickListener(v -> startActivity(new Intent(this, AllAppsActivity.class)));
        nodeLabel[ALL] = t.allTitle();
        placeTile(b, x, y, w, 112);
        return b;
    }

    // ---------------------------------------------------------------- live content

    /** Called every second. Recomputes the prayer day once a minute; the countdown changes every call. */
    private void render(boolean force) {
        Date now = now();
        long minute = now.getTime() / 60000;
        if (force || minute != lastMinute || day == null) {
            lastMinute = minute;
            day = new PrayerDay(this, now);
            renderMinute(now);
        }
        if (secondsUntil(day.nextTime, now) == 0) {
            // The prayer time has arrived: move on to the next one at once.
            day = new PrayerDay(this, new Date(now.getTime() + 1000));
            renderMinute(now);
        }
        renderCountdown(day, now, archFocused);
    }

    private void renderMinute(Date now) {
        readClockFormat();
        clock.setText(time(now, 0.4f));
        gdate.setText(gregorian(now));
        hdate.setText(hijri(day) + t.sep() + t.city());

        String[] names = t.prayerNames();
        if (day.friday) names[PrayerDay.DHUHR] = t.jumuah();
        String nextTime = time(day.nextTime, 1f).toString();
        String label = t.nextPrayer(), name = names[day.nextIndex];
        String sub = t.adhanAt() + " " + nextTime + t.sep() + t.city(), sub2 = "";
        if (day.friday && day.nextIndex == PrayerDay.DHUHR) label = t.jumuahLabel();
        if (day.friday) sub2 = t.kahf();
        if (day.ramadan) {
            sub2 = t.suhoorEnds(time(day.suhoorEnd, 1f).toString());
            if (day.nextIndex == PrayerDay.MAGHRIB) {
                label = t.iftarIn();
                name = t.iftar();
                sub = t.prayerNames()[PrayerDay.MAGHRIB] + " " + nextTime + t.sep() + t.city();
            }
        }
        archLabel.setText(label);
        archName.setText(name);
        archSub.setText(t.d(sub));
        archSub2.setText(t.d(sub2));
        archSub2.setVisibility(sub2.isEmpty() ? View.GONE : View.VISIBLE);

        for (int i = 0; i < 6; i++) {
            String tag = i == day.nowIndex ? t.nowTag() : (i == day.nextIndex ? t.nextTag() : "");
            if (day.ramadan && i == PrayerDay.FAJR) tag = t.suhoorTag();
            if (day.ramadan && i == PrayerDay.MAGHRIB) tag = t.iftarTag();
            boolean past = i != day.nextIndex && !day.times[i].after(now);
            int bg, fg, ring = 0;
            boolean bold;
            if (i == day.nowIndex) { bg = Tokens.PLASTER_100; fg = Tokens.LAPIS_900; bold = true; }
            else if (i == day.nextIndex) { bg = 0; fg = Tokens.GOLD_400; ring = Tokens.GOLD_400; bold = true; }
            else if (past) { bg = Tokens.LAPIS_800_60; fg = Tokens.MIST_400; bold = false; }
            else { bg = Tokens.LAPIS_800; fg = Tokens.PLASTER_100; bold = false; }
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(Ui.px(Tokens.RADIUS_CELL));
            g.setColor(bg);
            if (ring != 0) g.setStroke(Ui.ipx(3), ring);
            cell[i].setBackground(g);
            cellTag[i].setText(tag);
            cellName[i].setText(names[i]);
            // English 12-hour ("12:41 PM") is too wide for a cell at 34px; Arabic's ص/م is narrow enough.
            float timeSize = Ui.px(!h24 && !t.ar ? 30 : 34);
            if (cellTime[i].getTextSize() != timeSize) {
                cellTime[i].setTextSize(TypedValue.COMPLEX_UNIT_PX, timeSize);
                lineHeight(cellTime[i], 1.1f);
            }
            cellTime[i].setText(time(day.times[i], 0.55f));
            Typeface tf = bold ? ui600 : ui400;
            for (TextView v : new TextView[] {cellTag[i], cellName[i], cellTime[i]}) v.setTextColor(fg);
            cellTag[i].setTypeface(ui400);
            cellName[i].setTypeface(tf);
            cellTime[i].setTypeface(tf);
            if (!t.ar) cellTag[i].setLetterSpacing(0.2f);
        }
    }

    private void updateWifi() {
        if (wifiName == null) return;
        String ssid = ssid();
        wifiName.setText(ssid == null ? "Wi-Fi" : ssid);
    }

    // ---------------------------------------------------------------- focus

    private void onNodeFocused(int n) {
        if (n >= F0 && n < M0) lastFav = n;
        setArchFocused(n == ARCH);
        List<String> h = new ArrayList<>();
        if (n == ARCH) {
            h.add(t.hintOk(t.today()));
            h.add(t.hintApps());
        } else if (n == RADIO) {
            h.add(t.hintOk(nodeLabel[n]));
        } else if (n >= F0 && n < M0) {
            h.add(t.hintOk(t.open(nodeLabel[n])));
            h.add(t.hintMore());
        } else if (n >= M0) {
            h.add(t.hintOk(n == ALL ? nodeLabel[n] : t.open(nodeLabel[n])));
            h.add(t.hintFavs());
        } else {
            h.add(t.hintOk(nodeLabel[n]));
        }
        setHints(hints, h);
    }

    private void setArchFocused(boolean f) {
        archFocused = f;
        if (arch != null) render(false);
    }

    // ---------------------------------------------------------------- radio

    /** The header pill: "Radio" with a broadcast icon when off; level bars and "On air" (gold outline) when on. */
    private View radioPill() {
        FocusTile b = new FocusTile(this, 36);
        b.setRest(Tokens.LAPIS_800, Tokens.LINE_SOFT, 2);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.ipx(28), 0, Ui.ipx(28), 0);
        FrameLayout glyph = new FrameLayout(this);
        radioIcon = new Icon(this, Icon.RADIO, 2f);
        glyph.addView(radioIcon, new FrameLayout.LayoutParams(-1, -1));
        pillBars = new RadioBars(this, 3);
        glyph.addView(pillBars, new FrameLayout.LayoutParams(Ui.ipx(22), Ui.ipx(22), Gravity.CENTER));
        radioLabel = text(ui400, 24, Tokens.PLASTER_100);
        radioLabel.setSingleLine(true);
        row.addView(glyph, new LinearLayout.LayoutParams(Ui.ipx(30), Ui.ipx(30)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMarginStart(Ui.ipx(14));
        row.addView(radioLabel, lp);
        b.addView(row, new FrameLayout.LayoutParams(-2, -1));
        nodes[RADIO] = b;
        b.setLook(f -> styleRadio());
        b.setOnClickListener(v -> showRadioCard());
        nodeLabel[RADIO] = t.station(Radio.station(this));
        return b;
    }

    /** Puts the pill and the open card in step with the radio. */
    private void styleRadio() {
        if (radioLabel == null || nodes[RADIO] == null) return;
        nodeLabel[RADIO] = t.station(Radio.station(this));
        int state = Radio.state();
        boolean on = Radio.on(), playing = state == Radio.PLAYING;
        boolean focused = nodes[RADIO].isFocused();
        // The pill keeps to a short word; "retrying" reads as connecting there, the card says more.
        int shown = state == Radio.RETRYING ? Radio.CONNECTING : state;
        radioLabel.setText(on ? t.radioStatus(shown) : t.radio());
        radioIcon.setVisibility(on ? View.GONE : View.VISIBLE);
        pillBars.setVisibility(on ? View.VISIBLE : View.GONE);
        pillBars.setLive(playing);
        int fg = focused ? Tokens.LAPIS_900 : Tokens.PLASTER_100;
        radioLabel.setTextColor(fg);
        radioIcon.setColor(fg);
        pillBars.setColor(focused ? Tokens.LAPIS_900 : Tokens.GOLD_400);
        ((FocusTile) nodes[RADIO]).setRest(Tokens.LAPIS_800, on ? Tokens.GOLD_400_60 : Tokens.LINE_SOFT, 2);
        if (radioCard != null) {
            cardStatus.setText(t.stationWhere(Radio.station(this)) + t.sep() + t.radioStatus(state));
            cardBars.setLive(playing);
            playLabel.setText(on ? t.radioStop() : t.radioPlay());
            playIcon.setKind(on ? Icon.STOP : Icon.PLAY);
        }
    }

    /**
     * The card under the pill: the station, its state and live bars; Play/Stop (focused) and Screen off. Back
     * closes it and the radio carries on. Built like the reminder card, in the lapis of the header.
     */
    private void showRadioCard() {
        if (radioCard != null || card != null) return;
        radioCard = new LinearLayout(this);
        radioCard.setOrientation(LinearLayout.VERTICAL);
        radioCard.setPadding(Ui.ipx(36), Ui.ipx(30), Ui.ipx(36), Ui.ipx(30));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Tokens.LAPIS_700);
        bg.setCornerRadius(Ui.px(24));
        bg.setStroke(Ui.ipx(2), Tokens.GOLD_400_60);
        radioCard.setBackground(bg);
        radioCard.setClipChildren(false);
        radioCard.setClipToPadding(false);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        cardBars = new RadioBars(this, 5);
        cardBars.setColor(Tokens.GOLD_400);
        top.addView(cardBars, new LinearLayout.LayoutParams(Ui.ipx(72), Ui.ipx(64)));
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        TextView label = caps(text(ui400, 20, Tokens.MIST_400));
        label.setText(t.radio());
        TextView title = text(displayBold, 48, Tokens.PLASTER_100);
        title.setText(t.station(Radio.station(this)));
        cardStatus = text(ui400, 24, Tokens.GOLD_400);
        words.addView(label);
        words.addView(title);
        words.addView(cardStatus);
        lineHeight(title, 1.15f);
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(0, -2, 1);
        wlp.setMarginStart(Ui.ipx(28));
        top.addView(words, wlp);
        radioCard.addView(top);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setClipChildren(false);
        playIcon = new Icon(this, Icon.PLAY, 2f);
        playLabel = text(ui400, 24, Tokens.PLASTER_100);
        cardPlay = radioButton(playIcon, playLabel, t.radioPlay());
        cardPlay.setOnClickListener(v -> Radio.toggle(this));
        cardDark = radioButton(new Icon(this, Icon.MOON, 2f), text(ui400, 24, Tokens.PLASTER_100), t.screenOff());
        cardDark.setOnClickListener(v -> {
            closeRadioCard(true);
            ScreenOffActivity.start(this);
        });
        float r = FocusTile.REACH;
        buttons.addView(cardPlay, new LinearLayout.LayoutParams(-2, Ui.ipx(68 + 2 * r)));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-2, Ui.ipx(68 + 2 * r));
        dlp.setMarginStart(Ui.ipx(16 - 2 * r));
        buttons.addView(cardDark, dlp);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-2, -2);
        blp.topMargin = Ui.ipx(28 - r);
        blp.setMarginStart(-Ui.ipx(r));
        radioCard.addView(buttons, blp);

        TextView note = text(ui400, 20, Tokens.MIST_400);
        note.setText(t.radioNote());
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(-1, -2);
        nlp.topMargin = Ui.ipx(20 - r);
        radioCard.addView(note, nlp);

        // Hangs from the header, end-aligned with the Settings button, over the dates and the prayer strip.
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Ui.ipx(720), -2, Gravity.TOP | Gravity.END);
        lp.setMarginEnd(Ui.ipx(1920 - (Tokens.COURT_X + Tokens.COURT_W)));
        lp.topMargin = Ui.ipx(56 + 72 + 24);
        root.addView(radioCard, lp);
        for (View n : nodes) if (n != null) n.setFocusable(false);
        styleRadio();
        cardPlay.requestFocus();
        cardHints(cardPlay);
    }

    private void cardHints(View focused) {
        if (radioCard == null || hints == null) return;
        List<String> h = new ArrayList<>();
        h.add(t.hintOk(focused == cardDark ? t.screenOff() : (Radio.on() ? t.radioStop() : t.radioPlay())));
        h.add(t.hintClose());
        setHints(hints, h);
    }

    /** A 68px pill on the card with an icon: lapis at rest, gold when focused. */
    private FocusTile radioButton(Icon icon, TextView v, String label) {
        FocusTile b = new FocusTile(this, 34);
        b.setRest(Tokens.LAPIS_900, Tokens.LINE_SOFT, 2);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.ipx(28), 0, Ui.ipx(32), 0);
        v.setText(label);
        row.addView(icon, new LinearLayout.LayoutParams(Ui.ipx(26), Ui.ipx(26)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMarginStart(Ui.ipx(12));
        row.addView(v, lp);
        b.addView(row, new FrameLayout.LayoutParams(-2, -1));
        b.setLook(f -> {
            int c = f ? Tokens.LAPIS_900 : Tokens.PLASTER_100;
            v.setTextColor(c);
            icon.setColor(c);
            if (f) cardHints(b);
        });
        return b;
    }

    private void closeRadioCard(boolean focusPill) {
        if (radioCard == null) return;
        root.removeView(radioCard);
        radioCard = null;
        for (View n : nodes) if (n != null) n.setFocusable(true);
        if (focusPill) nodes[RADIO].requestFocus();
    }

    // ---------------------------------------------------------------- reminder card

    /** "Asr in 13 minutes" at the bottom end, over the apps; it takes focus until Got it, Today's prayers or Back. */
    private void showReminder() {
        if (card != null) dismissReminder();
        closeRadioCard(false);
        Date now = new Date();
        day = new PrayerDay(this, now);
        cardFor = day.nextTime;
        readClockFormat();

        card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.ipx(36), Ui.ipx(32), Ui.ipx(36), Ui.ipx(32));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Tokens.PLASTER_100);
        bg.setCornerRadius(Ui.px(24));
        bg.setStroke(Ui.ipx(6), Tokens.ON_PLASTER_BORDER);
        card.setBackground(bg);
        card.setClipChildren(false);
        card.setClipToPadding(false);

        String[] names = t.prayerNames();
        if (day.friday) names[PrayerDay.DHUHR] = t.jumuah();
        TextView label = caps(text(ui400, 20, Tokens.ON_PLASTER_LABEL));
        label.setText(t.apLabel());
        TextView title = text(displayBold, 52, Tokens.LAPIS_900);
        title.setText(t.apTitle(names[day.nextIndex], Reminders.minutesLeft(day, now), duration(secondsUntil(day.nextTime, now), false)));
        TextView sub = text(ui400, 24, Tokens.ON_PLASTER_SECONDARY);
        sub.setText(t.apSub(time(day.nextTime, 1f)));
        card.addView(label);
        card.addView(title);
        card.addView(sub);
        lineHeight(title, 1.15f);
        ((LinearLayout.LayoutParams) sub.getLayoutParams()).topMargin += Ui.ipx(6);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setClipChildren(false);
        cardOk = cardButton(t.apOk(), true);
        cardOk.setOnClickListener(v -> dismissReminder());
        cardDay = cardButton(t.dayTitle(), false);
        cardDay.setOnClickListener(v -> {
            dismissReminder();
            startSafely(new Intent(this, PrayerDayActivity.class));
        });
        float r = FocusTile.REACH;
        buttons.addView(cardOk, new LinearLayout.LayoutParams(-2, Ui.ipx(68 + 2 * r)));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-2, Ui.ipx(68 + 2 * r));
        dlp.setMarginStart(Ui.ipx(16 - 2 * r));
        buttons.addView(cardDay, dlp);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-2, -2);
        blp.topMargin = Ui.ipx(24 - r);
        blp.setMarginStart(-Ui.ipx(r));
        card.addView(buttons, blp);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Ui.ipx(680), -2, Gravity.BOTTOM | Gravity.END);
        lp.setMarginEnd(Ui.ipx(96));
        lp.bottomMargin = Ui.ipx(56);
        root.addView(card, lp);
        for (View n : nodes) if (n != null) n.setFocusable(false);
        cardOk.requestFocus();
        List<String> h = new ArrayList<>();
        h.add(t.hintOk(t.apOk()));
        h.add(t.hintBackHome().replace(t.home(), t.dismiss()));
        setHints(hints, h);
    }

    /** A 68px pill on the cream card: lapis (Got it) or outlined; gold when focused. */
    private FocusTile cardButton(String label, boolean filled) {
        FocusTile b = new FocusTile(this, 34);
        b.setRest(filled ? Tokens.LAPIS_900 : 0, Tokens.ON_PLASTER_BUTTON_BORDER, filled ? 0 : 2);
        TextView v = text(ui400, 24, filled ? Tokens.PLASTER_100 : Tokens.LAPIS_900);
        v.setText(label);
        v.setGravity(Gravity.CENTER);
        v.setPadding(Ui.ipx(32), 0, Ui.ipx(32), 0);
        b.addView(v, new FrameLayout.LayoutParams(-2, -1));
        b.setLook(f -> v.setTextColor(f ? Tokens.LAPIS_900 : (filled ? Tokens.PLASTER_100 : Tokens.LAPIS_900)));
        return b;
    }

    private void dismissReminder() {
        if (card == null) return;
        root.removeView(card);
        card = null;
        for (View n : nodes) if (n != null) n.setFocusable(true);
        nodes[ARCH].requestFocus();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int k = e.getKeyCode();
        // Source, where the firmware leaves it to the app in front (CVTE) and nothing else opens the source menu.
        if (k == KeyEvent.KEYCODE_TV_INPUT) {
            Intent source = FirmwareKeys.sourceMenu(this);
            if (source != null) {
                if (e.getAction() == KeyEvent.ACTION_UP && !e.isCanceled()) {
                    try {
                        startActivity(source.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    } catch (RuntimeException ignored) {
                        // Not exported or gone after all; the key just does nothing.
                    }
                }
                return true;
            }
        }
        if (radioCard != null) {
            if (k == KeyEvent.KEYCODE_BACK) {
                if (e.getAction() == KeyEvent.ACTION_UP) closeRadioCard(true);
                return true;
            }
            if (k == KeyEvent.KEYCODE_DPAD_LEFT || k == KeyEvent.KEYCODE_DPAD_RIGHT) {
                boolean toEnd = (k == KeyEvent.KEYCODE_DPAD_RIGHT) != rtl;
                if (e.getAction() == KeyEvent.ACTION_DOWN) (toEnd ? cardDark : cardPlay).requestFocus();
                return true;
            }
            if (k == KeyEvent.KEYCODE_DPAD_UP || k == KeyEvent.KEYCODE_DPAD_DOWN) return true;
            return super.dispatchKeyEvent(e);
        }
        if (card != null) {
            if (k == KeyEvent.KEYCODE_BACK) {
                if (e.getAction() == KeyEvent.ACTION_UP) dismissReminder();
                return true;
            }
            if (k == KeyEvent.KEYCODE_DPAD_LEFT || k == KeyEvent.KEYCODE_DPAD_RIGHT) {
                // Got it sits at the start; toward the end is Today's prayers (Left in Arabic).
                boolean toEnd = (k == KeyEvent.KEYCODE_DPAD_RIGHT) != rtl;
                if (e.getAction() == KeyEvent.ACTION_DOWN) (toEnd ? cardDay : cardOk).requestFocus();
                return true;
            }
            if (k == KeyEvent.KEYCODE_DPAD_UP || k == KeyEvent.KEYCODE_DPAD_DOWN) return true;
            return super.dispatchKeyEvent(e);
        }
        if (k == KeyEvent.KEYCODE_BACK) {
            // Back on Home returns focus to the arch and never leaves Home.
            if (e.getAction() == KeyEvent.ACTION_UP) nodes[ARCH].requestFocus();
            return true;
        }
        if (k == KeyEvent.KEYCODE_DPAD_LEFT || k == KeyEvent.KEYCODE_DPAD_RIGHT
                || k == KeyEvent.KEYCODE_DPAD_UP || k == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (e.getAction() == KeyEvent.ACTION_DOWN) move(k);
            return true;
        }
        return super.dispatchKeyEvent(e);
    }

    /** design/FocusMap.dc.html, written for LTR; in Arabic every Left/Right swaps. */
    private void move(int key) {
        if (rtl && key == KeyEvent.KEYCODE_DPAD_LEFT) key = KeyEvent.KEYCODE_DPAD_RIGHT;
        else if (rtl && key == KeyEvent.KEYCODE_DPAD_RIGHT) key = KeyEvent.KEYCODE_DPAD_LEFT;
        int from = currentNode();
        if (from < 0) { nodes[ARCH].requestFocus(); return; }
        boolean left = key == KeyEvent.KEYCODE_DPAD_LEFT, right = key == KeyEvent.KEYCODE_DPAD_RIGHT;
        boolean up = key == KeyEvent.KEYCODE_DPAD_UP, down = key == KeyEvent.KEYCODE_DPAD_DOWN;
        int to = -1;
        switch (from) {
            case ARCH: to = right ? F0 : up ? RADIO : -1; break;
            case RADIO: to = left ? ARCH : right ? WIFI : down ? F0 + 1 : -1; break;
            case WIFI: to = left ? RADIO : right ? SETTINGS : down ? F0 + 2 : -1; break;
            case SETTINGS: to = left ? WIFI : down ? F0 + 3 : -1; break;
            default:
                if (from >= F0 && from < M0) {
                    int i = from - F0;
                    if (left) to = i == 0 ? ARCH : from - 1;
                    else if (right) to = i == 3 ? -1 : from + 1;
                    else if (up) to = i == 3 ? SETTINGS : i == 2 ? WIFI : RADIO;
                    else to = M0 + new int[] {0, 1, 3, 4}[i];
                } else {
                    int i = from - M0;
                    if (left) to = i == 0 ? ARCH : from - 1;
                    else if (right) to = from == ALL ? -1 : from + 1;
                    else if (up) to = lastFav;
                }
        }
        if (to >= 0 && nodes[to] != null) nodes[to].requestFocus();
    }

    private int currentNode() {
        View f = getCurrentFocus();
        for (int i = 0; i < NODES; i++) if (nodes[i] == f) return i;
        return -1;
    }

    // ---------------------------------------------------------------- apps

    private String appsKey() {
        return Apps.favourites(this) + "|" + Apps.installed(this).size();
    }
}
