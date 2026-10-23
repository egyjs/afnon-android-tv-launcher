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
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.batoulapps.adhan.CalculationMethod;
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

    private static final int ARCH = 0, WIFI = 1, SETTINGS = 2, F0 = 3, M0 = 7, ALL = 12, RADIO = 13, NIGHT = 14, LIVE = 15,
        PANEL = 16, FULL = 17, NODES = 18;
    /**
     * The live panel (local only): the courtyard's whole width at 16:9, from the prayer strip's top, over the
     * strip and the favourites and More rows; the clock and dates stay above it.
     */
    private static final float PANEL_Y = 290, PANEL_W = Tokens.COURT_W, PANEL_H = Tokens.COURT_W * 9 / 16;

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
    /**
     * The apps of the last build, with their names and icons: a night-mode switch rebuilds with them instead of
     * asking the package manager again, which is slow on this TV.
     */
    private List<String> favApps, moreApps;
    private int appCount;
    private final java.util.Map<String, Drawable> icons = new java.util.HashMap<>();
    private final java.util.Map<String, String> names = new java.util.HashMap<>();
    private boolean sameApps;
    /** The reminder card (design/Home.dc.html, mode "approach") and the prayer it is for. */
    private LinearLayout card;
    private FocusTile cardOk, cardDay;
    private Date cardFor;
    /** The radio's header pill and, while open, its card. */
    private Icon radioIcon, playIcon;
    private RadioBars pillBars, cardBars;
    private TextView radioLabel, cardTitle, cardStatus, playLabel;
    private LinearLayout radioCard;
    /** The card's buttons from the start side: Play/Stop, Station, Screen off. */
    private FocusTile cardPlay, cardStation, cardDark;
    private final Radio.Listener onRadio = state -> styleRadio();
    /**
     * Live from Makkah and Madinah (local only): the player, the header pill, its card while open, and the panel
     * (nodes[PANEL]) with the place and its state beside it while one is on.
     */
    private final Live live = new Live(state -> styleLive());
    private Icon liveIcon;
    private TextView liveLabel, panelName, panelStatus;
    private LinearLayout liveCard, panelSide;
    /** The live card's buttons from the start side: Makkah, Madinah, and Stop while one is on. */
    private FocusTile[] liveButtons;
    private View favLabel, moreLabel;
    /** The panel filling the screen (nodes[FULL] toggles it), where it sits otherwise, and the fading hint. */
    private boolean fullScreen;
    private ViewGroup.LayoutParams panelPlace;
    private TextView fullHint;
    /** Full screen's quality bar while it shows: its buttons, lowest first, and their picture heights. */
    private LinearLayout qualityBar;
    private FocusTile[] qualityButtons;
    private int[] qualityHeights;
    /** Test only (--ez bar true): the quality bar once the video plays. */
    private boolean barWhenPlaying;
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
     * the spoken reminder if it is on; `banner`, `call`, `announce` and `standby` come from the remote buttons
     * service 8 s later, time enough to open another app. `wake` (with `--ei seconds N`, 120 by default) turns the
     * TV on for the coming prayer N seconds from now, after it has been put in standby (Wake.test).
     * `--es station <id>` (ids in Radio.STATIONS) picks a radio station as Settings does, before any test runs.
     * `--es live makkah|madinah|off` starts or stops the live panel as its card does; with `--ei quality 1080`
     * (360, 480…) it saves that quality first, as the quality bar does; `--ez full true` fills the screen, and
     * `--ez bar true` also opens the quality bar once the video plays.
     */
    private void runTest(Intent i) {
        String station = i == null ? null : i.getStringExtra("station");
        if (station != null) {
            i.removeExtra("station");
            for (int k = 0; k < Radio.STATIONS.length; k++) if (Radio.STATIONS[k].id.equals(station)) Radio.switchTo(this, k);
        }
        String place = i == null ? null : i.getStringExtra("live");
        if (place != null) {
            int quality = i.getIntExtra("quality", 0);
            boolean bar = i.getBooleanExtra("bar", false), full = bar || i.getBooleanExtra("full", false);
            for (String x : new String[] {"live", "quality", "bar", "full"}) i.removeExtra(x);
            // Posted: it plays once Home is resumed (an intent pauses Home for a moment, and onPause stops it).
            root.post(() -> {
                if ("off".equals(place)) {
                    live.stop();
                    return;
                }
                if (quality > 0) Prefs.setLiveQuality(this, quality);
                startLive("madinah".equals(place) ? Live.MADINAH : Live.MAKKAH);
                if (full) enterFullScreen();
                barWhenPlaying = bar;
            });
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
        else if ("wake".equals(kind)) Wake.test(this, i.getIntExtra("seconds", 120), i.getStringExtra("via"));
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
        // The live video plays only on Home and only while chosen: leaving Home (an app, the call, the
        // screensaver) stops it and releases everything.
        live.stop();
        super.onPause();
    }

    @Override
    void onTick() {
        // In case a screensaver ever starts over Home without pausing it.
        if (AfnonDream.running && live.state() != Live.OFF) live.stop();
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
        liveCard = null;
        nodes[PANEL] = null;   // and the live panel, put back at the end while it plays
        nodes[FULL] = null;
        fullScreen = false;
        fullHint = null;
        // The package manager is slow on this TV; a palette switch keeps the apps (and icons) it already has.
        if (!sameApps || favApps == null) {
            favApps = Apps.slots(this, t);
            moreApps = Apps.more(this, t, 5);
            appCount = Apps.installed(this).size();
            builtApps = appsKey();
            icons.clear();
            names.clear();
        }
        sameApps = false;
        buildArch(488, 26, 12, Tokens.GOLD_200, 8);

        nodes[ARCH] = archButton();

        // Courtyard: clock and dates, then Wi-Fi and Settings at the end side.
        LinearLayout when = new LinearLayout(this);
        when.setOrientation(LinearLayout.VERTICAL);
        clock = text(numbers, 96, Tokens.PLASTER_100);
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

        // Live, Radio and Wi-Fi side by side, ending 20px before the night button, which is 20px before Settings.
        nodes[LIVE] = livePill();
        nodes[RADIO] = radioPill();
        nodes[WIFI] = wifiPill();
        LinearLayout pills = new LinearLayout(this);
        pills.setClipChildren(false);
        float reach = FocusTile.REACH;
        pills.addView(nodes[LIVE], new LinearLayout.LayoutParams(-2, Ui.ipx(72 + 2 * reach)));
        for (View pill : new View[] {nodes[RADIO], nodes[WIFI]}) {
            LinearLayout.LayoutParams plp2 = new LinearLayout.LayoutParams(-2, Ui.ipx(72 + 2 * reach));
            plp2.setMarginStart(Ui.ipx(20 - 2 * reach));
            pills.addView(pill, plp2);
        }
        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        plp.setMarginEnd(Ui.ipx(1920 - (Tokens.COURT_X + Tokens.COURT_W) + 2 * (72 + 20) - reach));
        plp.topMargin = Ui.ipx(56 - reach);
        root.addView(pills, plp);
        nodes[NIGHT] = nightButton();
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
        favLabel = fav;
        float fw = (Tokens.COURT_W - 3 * 24) / 4f;
        for (int i = 0; i < 4; i++) {
            nodes[F0 + i] = favouriteTile(favApps.get(i), F0 + i, Tokens.COURT_X + i * (fw + 24), 520, fw);
        }

        TextView more = caps(text(ui400, 22, Tokens.MIST_400));
        more.setText(t.more());
        place(more, Tokens.COURT_X, LABEL_ABOVE_MORE, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        moreLabel = more;
        float mw = (Tokens.COURT_W - 5 * 20) / 6f;
        for (int i = 0; i < 5; i++) {
            nodes[M0 + i] = moreTile(i < moreApps.size() ? moreApps.get(i) : null, M0 + i, Tokens.COURT_X + i * (mw + 20), 764, mw);
        }
        nodes[ALL] = allAppsTile(Tokens.COURT_X + 5 * (mw + 20), 764, mw);

        hints = new LinearLayout(this);
        hints.setOrientation(LinearLayout.HORIZONTAL);
        place(hints, Tokens.COURT_X, 962, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        for (int i = 0; i < NODES; i++) {
            final int n = i;
            if (nodes[i] == null) continue;   // the live panel, made when it shows
            nodes[i].setOnFocusChangeListener((v, has) -> { if (has) onNodeFocused(n); else if (n == ARCH) setArchFocused(false); });
        }

        setContentView(root);
        lastMinute = -1;
        render(true);
        // A rebuild in front (night mode, language) gets no onResume to fill in the network's name.
        updateWifi();
        // The live video carries on through a rebuild in front (night mode): a new panel, the same player.
        styleLive();
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
            int c = f ? Tokens.FOCUS_TEXT : Tokens.PLASTER_100;
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
        // The header row also holds the Live pill now: a long network name is cut short, clear of the clock.
        wifiName.setSingleLine(true);
        wifiName.setEllipsize(TextUtils.TruncateAt.END);
        wifiName.setMaxWidth(Ui.ipx(200));
        row.addView(icon, new LinearLayout.LayoutParams(Ui.ipx(30), Ui.ipx(30)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMarginStart(Ui.ipx(14));
        row.addView(wifiName, lp);
        b.addView(row, new FrameLayout.LayoutParams(-2, -1));
        b.setLook(f -> {
            int c = f ? Tokens.FOCUS_TEXT : Tokens.PLASTER_100;
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
        b.setLook(f -> gear.setColor(f ? Tokens.FOCUS_TEXT : Tokens.PLASTER_100));
        b.setOnClickListener(v -> startSafely(new Intent(this, SettingsActivity.class)));
        nodeLabel[SETTINGS] = t.settings();
        placeEnd(b, 1920 - (Tokens.COURT_X + Tokens.COURT_W), 56, 72, 72);
        return b;
    }

    /**
     * Night mode, beside Settings: a full moon with a star while it is off, a sun while it is on. OK switches at
     * once: every colour comes from {@link Tokens}, so Home is built again in the new palette (with the apps it
     * already has) and focus stays here.
     */
    private View nightButton() {
        FocusTile b = new FocusTile(this, 36);
        b.setRest(Tokens.LAPIS_800, Tokens.LINE_SOFT, 2);
        Icon icon = new Icon(this, Tokens.night ? Icon.SUN : Icon.NIGHT, 1.8f);
        b.addView(icon, new FrameLayout.LayoutParams(Ui.ipx(34), Ui.ipx(34), Gravity.CENTER));
        String label = Tokens.night ? t.nightOff() : t.nightOn();
        b.setContentDescription(label);
        b.setLook(f -> icon.setColor(f ? Tokens.FOCUS_TEXT : Tokens.PLASTER_100));
        b.setOnClickListener(v -> {
            Prefs.setNight(this, !Tokens.night);
            loadSettings(null);
            // Back to day: the icons kept for the rebuild drop night mode's filter (each was mutated for it).
            if (!Tokens.night) for (Drawable d : icons.values()) if (d != null) d.setColorFilter(null);
            sameApps = true;
            build();
            nodes[NIGHT].requestFocus();
        });
        nodeLabel[NIGHT] = label;
        placeEnd(b, 1920 - (Tokens.COURT_X + Tokens.COURT_W) + 72 + 20, 56, 72, 72);
        return b;
    }

    private View favouriteTile(String pkg, int node, float x, float y, float w) {
        FocusTile b = new FocusTile(this, Tokens.RADIUS_TILE);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.ipx(24), Ui.ipx(22), Ui.ipx(24), Ui.ipx(22));
        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.FIT_START);
        if (!icons.containsKey(pkg)) icons.put(pkg, Apps.icon(this, pkg));
        Apps.showIcon(icon, icons.get(pkg));
        String name = appName(pkg);
        TextView label = text(ui500, Apps.isLatin(name) ? 28 : 30, Tokens.PLASTER_100);
        label.setText(name);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        col.addView(icon, new LinearLayout.LayoutParams(Ui.ipx(48), Ui.ipx(48)));
        col.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1));
        col.addView(label, new LinearLayout.LayoutParams(-1, -2));
        b.addView(col, new FrameLayout.LayoutParams(-1, -1));
        b.setLook(f -> label.setTextColor(f ? Tokens.FOCUS_TEXT : Tokens.PLASTER_100));
        b.setOnClickListener(v -> { if (pkg != null) Apps.launch(this, pkg); });
        nodeLabel[node] = name;
        placeTile(b, x, y, w, 150);
        return b;
    }

    private String appName(String pkg) {
        if (pkg == null) return "";
        if (!names.containsKey(pkg)) names.put(pkg, Apps.name(this, t, pkg));
        return names.get(pkg);
    }

    private View moreTile(String pkg, int node, float x, float y, float w) {
        FocusTile b = new FocusTile(this, Tokens.RADIUS_CELL);
        String name = appName(pkg);
        TextView label = text(ui400, 22, Tokens.PLASTER_100);
        label.setText(name);
        label.setMaxLines(2);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setPadding(Ui.ipx(18), Ui.ipx(18), Ui.ipx(18), Ui.ipx(18));
        label.setGravity(Gravity.BOTTOM | Gravity.START);
        b.addView(label, new FrameLayout.LayoutParams(-1, -1));
        b.setLook(f -> label.setTextColor(f ? Tokens.FOCUS_TEXT : Tokens.PLASTER_100));
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
        label.setText(t.all(appCount));
        label.setPadding(Ui.ipx(12), Ui.ipx(18), Ui.ipx(12), Ui.ipx(18));
        label.setGravity(Gravity.BOTTOM | Gravity.START);
        b.addView(label, new FrameLayout.LayoutParams(-1, -1));
        b.setLook(f -> label.setTextColor(f ? Tokens.FOCUS_TEXT : Tokens.PLASTER_100));
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
        clock.setText(displayTime(now, 0.4f));
        gdate.setText(gregorian(now));
        hdate.setText(hijri(day) + t.sep() + t.city());

        String[] names = t.prayerNames();
        if (day.friday) names[PrayerDay.DHUHR] = t.jumuah();
        String nextTime = time(day.nextTime, 1f).toString();
        String label = t.nextPrayer(), name = names[day.nextIndex];
        String sub = t.adhanAt() + " " + nextTime + t.sep() + t.city(), sub2 = "";
        if (day.friday && day.nextIndex == PrayerDay.DHUHR) label = t.jumuahLabel();
        if (day.kahf) sub2 = t.kahf();
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
            if (i == day.nowIndex) { bg = Tokens.CURRENT_FILL; fg = Tokens.CURRENT_TEXT; bold = true; }
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
        } else if (n == RADIO || n == NIGHT || n == LIVE) {
            h.add(t.hintOk(nodeLabel[n]));
        } else if (n == PANEL || n == FULL) {
            h.add(t.hintOk(n == FULL ? t.fullScreen() : t.liveTitle()));
            h.add(t.hintStop());
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
        int fg = focused ? Tokens.FOCUS_TEXT : Tokens.PLASTER_100;
        radioLabel.setTextColor(fg);
        radioIcon.setColor(fg);
        pillBars.setColor(focused ? Tokens.FOCUS_TEXT : Tokens.GOLD_400);
        ((FocusTile) nodes[RADIO]).setRest(Tokens.LAPIS_800, on ? Tokens.GOLD_400_60 : Tokens.LINE_SOFT, 2);
        if (radioCard != null) {
            cardTitle.setText(t.station(Radio.station(this)));
            cardStatus.setText(t.stationWhere(Radio.station(this)) + t.sep() + t.radioStatus(state));
            cardBars.setLive(playing);
            playLabel.setText(on ? t.radioStop() : t.radioPlay());
            playIcon.setKind(on ? Icon.STOP : Icon.PLAY);
        }
    }

    /**
     * The card under the pill: the station, its state and live bars; Play/Stop (focused), Station and Screen off.
     * Station opens Settings' station list over Home (groups, then stations); playing radio moves to the pick. Back
     * closes the card and the radio carries on. Built like the reminder card, in the lapis of the header.
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
        cardTitle = text(displayBold, 48, Tokens.PLASTER_100);
        cardTitle.setText(t.station(Radio.station(this)));
        cardStatus = text(ui400, 24, Tokens.GOLD_400);
        words.addView(label);
        words.addView(cardTitle);
        words.addView(cardStatus);
        lineHeight(cardTitle, 1.15f);
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(0, -2, 1);
        wlp.setMarginStart(Ui.ipx(28));
        top.addView(words, wlp);
        radioCard.addView(top);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setClipChildren(false);
        playIcon = new Icon(this, Icon.PLAY, 2f);
        playLabel = text(ui400, 24, Tokens.PLASTER_100);
        cardPlay = cardPill(playIcon, playLabel, t.radioPlay());
        cardPlay.setOnClickListener(v -> Radio.toggle(this));
        cardStation = cardPill(new Icon(this, Icon.RADIO, 2f), text(ui400, 24, Tokens.PLASTER_100), t.stationRow());
        cardStation.setOnClickListener(v -> chooseStation(this::styleRadio));
        cardDark = cardPill(new Icon(this, Icon.MOON, 2f), text(ui400, 24, Tokens.PLASTER_100), t.screenOff());
        cardDark.setOnClickListener(v -> {
            closeRadioCard(true);
            ScreenOffActivity.start(this);
        });
        float r = FocusTile.REACH;
        buttons.addView(cardPlay, new LinearLayout.LayoutParams(-2, Ui.ipx(68 + 2 * r)));
        for (FocusTile b : new FocusTile[] {cardStation, cardDark}) {
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-2, Ui.ipx(68 + 2 * r));
            blp.setMarginStart(Ui.ipx(16 - 2 * r));
            buttons.addView(b, blp);
        }
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

    /** The hints for the radio or the live card's focused button. */
    private void cardHints(View focused) {
        if (hints == null) return;
        List<String> h = new ArrayList<>();
        if (radioCard != null) {
            h.add(t.hintOk(focused == cardDark ? t.screenOff() : focused == cardStation ? t.stationRow()
                : (Radio.on() ? t.radioStop() : t.radioPlay())));
        } else if (liveCard != null && liveButtons != null) {
            int i = java.util.Arrays.asList(liveButtons).indexOf(focused);
            h.add(t.hintOk(i == 2 ? t.radioStop() : t.livePlaces()[Math.max(0, i)]));
        } else {
            return;
        }
        h.add(t.hintClose());
        setHints(hints, h);
    }

    @Override
    LinearLayout hintLine() {
        return hints;
    }

    /** The station list over the card: the card's buttons step aside, then Station takes focus back. */
    @Override
    void onChooser(boolean open) {
        if (radioCard == null) return;
        for (View b : new View[] {cardPlay, cardStation, cardDark}) b.setFocusable(!open);
        if (!open) cardStation.requestFocus();
    }

    /** A 68px pill on the radio or the live card, with an icon: lapis at rest, gold when focused. */
    private FocusTile cardPill(Icon icon, TextView v, String label) {
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
            int c = f ? Tokens.FOCUS_TEXT : Tokens.PLASTER_100;
            v.setTextColor(c);
            icon.setColor(c);
            if (f) cardHints(b);
        });
        return b;
    }

    private void closeRadioCard(boolean focusPill) {
        if (radioCard == null) return;
        if (chooser != null) closeChooser();
        root.removeView(radioCard);
        radioCard = null;
        for (View n : nodes) if (n != null) n.setFocusable(true);
        if (focusPill) nodes[RADIO].requestFocus();
    }

    // ---------------------------------------------------------------- live from Makkah and Madinah (local only)

    /** The header pill: «مباشر» with a screen icon; a gold outline while the video is on. OK opens its card. */
    private View livePill() {
        FocusTile b = new FocusTile(this, 36);
        b.setRest(Tokens.LAPIS_800, Tokens.LINE_SOFT, 2);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.ipx(28), 0, Ui.ipx(28), 0);
        liveIcon = new Icon(this, Icon.LIVE, 2f);
        liveLabel = text(ui400, 24, Tokens.PLASTER_100);
        liveLabel.setText(t.live());
        liveLabel.setSingleLine(true);
        row.addView(liveIcon, new LinearLayout.LayoutParams(Ui.ipx(30), Ui.ipx(30)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMarginStart(Ui.ipx(14));
        row.addView(liveLabel, lp);
        b.addView(row, new FrameLayout.LayoutParams(-2, -1));
        b.setLook(f -> {
            int c = f ? Tokens.FOCUS_TEXT : Tokens.PLASTER_100;
            liveLabel.setTextColor(c);
            liveIcon.setColor(c);
        });
        b.setOnClickListener(v -> showLiveCard());
        nodeLabel[LIVE] = t.live();
        return b;
    }

    /**
     * The live card, hanging from the header like the radio card: Makkah and Madinah (the one on has a gold
     * outline), and Stop while one is on. A choice plays on the panel and focus goes there; Back closes the card.
     */
    private void showLiveCard() {
        if (liveCard != null || radioCard != null || card != null) return;
        liveCard = new LinearLayout(this);
        liveCard.setOrientation(LinearLayout.VERTICAL);
        liveCard.setPadding(Ui.ipx(36), Ui.ipx(30), Ui.ipx(36), Ui.ipx(30));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Tokens.LAPIS_700);
        bg.setCornerRadius(Ui.px(24));
        bg.setStroke(Ui.ipx(2), Tokens.GOLD_400_60);
        liveCard.setBackground(bg);
        liveCard.setClipChildren(false);
        liveCard.setClipToPadding(false);

        TextView label = caps(text(ui400, 20, Tokens.MIST_400));
        label.setText(t.live());
        TextView title = text(displayBold, 48, Tokens.PLASTER_100);
        title.setText(t.liveTitle());
        liveCard.addView(label);
        liveCard.addView(title);
        lineHeight(title, 1.15f);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setClipChildren(false);
        String[] places = t.livePlaces();
        int on = live.state() == Live.OFF ? -1 : live.place();
        liveButtons = new FocusTile[on < 0 ? 2 : 3];
        float r = FocusTile.REACH;
        for (int i = 0; i < liveButtons.length; i++) {
            final int which = i;
            boolean stop = i == 2;
            FocusTile b = cardPill(new Icon(this, stop ? Icon.STOP : Icon.PLAY, 2f), text(ui400, 24, Tokens.PLASTER_100),
                stop ? t.radioStop() : places[i]);
            if (i == on) b.setRest(Tokens.LAPIS_900, Tokens.GOLD_400, 2);
            b.setOnClickListener(v -> {
                if (stop) {
                    closeLiveCard(false);
                    live.stop();
                    nodes[LIVE].requestFocus();
                } else {
                    startLive(which);
                }
            });
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-2, Ui.ipx(68 + 2 * r));
            if (i > 0) blp.setMarginStart(Ui.ipx(16 - 2 * r));
            buttons.addView(b, blp);
            liveButtons[i] = b;
        }
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-2, -2);
        blp.topMargin = Ui.ipx(28 - r);
        blp.setMarginStart(-Ui.ipx(r));
        liveCard.addView(buttons, blp);

        TextView note = text(ui400, 20, Tokens.MIST_400);
        note.setText(t.liveNote());
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(-1, -2);
        nlp.topMargin = Ui.ipx(20 - r);
        liveCard.addView(note, nlp);

        // Where the radio card hangs: end-aligned with the Settings button, over the dates and the prayer strip.
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Ui.ipx(720), -2, Gravity.TOP | Gravity.END);
        lp.setMarginEnd(Ui.ipx(1920 - (Tokens.COURT_X + Tokens.COURT_W)));
        lp.topMargin = Ui.ipx(56 + 72 + 24);
        root.addView(liveCard, lp);
        for (View n : nodes) if (n != null) n.setFocusable(false);
        liveButtons[0].requestFocus();
        cardHints(liveButtons[0]);
    }

    /** Closes the live card; refocus puts focus back where it was opened (the panel, or the pill). */
    private void closeLiveCard(boolean refocus) {
        if (liveCard == null) return;
        root.removeView(liveCard);
        liveCard = null;
        liveButtons = null;
        for (View n : nodes) if (n != null) n.setFocusable(true);
        if (refocus) nodes[nodes[PANEL] != null ? PANEL : LIVE].requestFocus();
    }

    /** Makkah or Madinah on the panel (the radio stops); focus goes to the panel. */
    private void startLive(int which) {
        closeLiveCard(false);
        if (card != null || radioCard != null) return;
        live.start(this, which);
        if (nodes[PANEL] != null) nodes[PANEL].requestFocus();
    }

    /** Puts the pill and the panel in step with the live video: OFF takes the panel away. */
    private void styleLive() {
        int s = live.state();
        if (nodes[LIVE] != null) {
            ((FocusTile) nodes[LIVE]).setRest(Tokens.LAPIS_800, s == Live.OFF ? Tokens.LINE_SOFT : Tokens.GOLD_400_60, 2);
        }
        if (s == Live.OFF) {
            closeLivePanel();
            return;
        }
        if (nodes[PANEL] == null) showLivePanel();
        panelName.setText(t.livePlaces()[live.place()]);
        panelStatus.setText(t.liveStatus(s));
        panelStatus.setTextColor(s == Live.FAILED ? Tokens.PLASTER_100 : Tokens.MIST_400);
        // Couldn't play: the message shows on the panel, and there is nothing to fill the screen with.
        if (s == Live.FAILED) {
            exitFullScreen();
            if (nodes[FULL].isFocused()) nodes[PANEL].requestFocus();
        }
        nodes[FULL].setVisibility(s == Live.FAILED || fullScreen ? View.INVISIBLE : View.VISIBLE);
        if (s == Live.PLAYING && barWhenPlaying && fullScreen) {
            barWhenPlaying = false;
            showQualityBar();
        }
    }

    /**
     * The panel: the video at 16:9 across the courtyard, where the prayer strip and the favourites and More rows
     * are (hidden while it shows), and under it «مباشر», the place and its state on one line. The arch (with the
     * next prayer and its countdown), the clock and the dates stay. It is the focus node for the rows it covers;
     * OK opens the live card, Back stops it. A SurfaceView, so the TV's video plane draws it; the view itself
     * doesn't scale on focus.
     */
    private void showLivePanel() {
        FocusTile b = new FocusTile(this, Tokens.RADIUS_BADGE);
        b.setFocusScale(1f);
        b.setRest(0, 0, 0);
        SurfaceView video = new SurfaceView(this);
        video.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder h) {
                live.setSurface(h);
            }

            @Override
            public void surfaceChanged(SurfaceHolder h, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder h) {
                live.surfaceGone(h);
            }
        });
        b.addView(video, new FrameLayout.LayoutParams(-1, -1));
        b.setOnClickListener(v -> showLiveCard());
        b.setOnFocusChangeListener((v, has) -> { if (has) onNodeFocused(PANEL); });
        b.setFocusable(liveCard == null && radioCard == null && card == null && chooser == null);
        nodes[PANEL] = b;
        placeTile(b, Tokens.COURT_X, PANEL_Y, PANEL_W, PANEL_H);

        panelSide = new LinearLayout(this);
        panelSide.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = caps(text(ui500, 22, Tokens.GOLD_400));
        label.setText(t.live());
        panelName = text(ui500, 26, Tokens.PLASTER_100);
        panelStatus = text(ui400, 22, Tokens.MIST_400);
        panelSide.addView(label);
        for (TextView v : new TextView[] {panelName, panelStatus}) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.setMarginStart(Ui.ipx(20));
            panelSide.addView(v, lp);
        }
        // Under the panel: the words at its start corner, Full screen at its end corner, centred on one line.
        float under = PANEL_Y + PANEL_H + 14;
        place(panelSide, Tokens.COURT_X, under, ViewGroup.LayoutParams.WRAP_CONTENT, 56);
        nodes[FULL] = fullScreenButton(under);

        rowsShown(false);
    }

    /** «ملء الشاشة» with the expand icon, a 56px pill like the header's: OK fills the screen with the panel. */
    private View fullScreenButton(float y) {
        FocusTile b = new FocusTile(this, 28);
        b.setRest(Tokens.LAPIS_800, Tokens.LINE_SOFT, 2);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.ipx(24), 0, Ui.ipx(28), 0);
        Icon icon = new Icon(this, Icon.EXPAND, 2f);
        TextView label = text(ui400, 22, Tokens.PLASTER_100);
        label.setText(t.fullScreen());
        row.addView(icon, new LinearLayout.LayoutParams(Ui.ipx(24), Ui.ipx(24)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMarginStart(Ui.ipx(12));
        row.addView(label, lp);
        b.addView(row, new FrameLayout.LayoutParams(-2, -1));
        b.setLook(f -> {
            int c = f ? Tokens.FOCUS_TEXT : Tokens.PLASTER_100;
            label.setTextColor(c);
            icon.setColor(c);
        });
        b.setOnClickListener(v -> enterFullScreen());
        b.setOnFocusChangeListener((v, has) -> { if (has) onNodeFocused(FULL); });
        b.setFocusable(nodes[PANEL].isFocusable());
        placeEnd(b, 1920 - (Tokens.COURT_X + Tokens.COURT_W), y, ViewGroup.LayoutParams.WRAP_CONTENT, 56);
        return b;
    }

    /**
     * The video fills the screen: the same player and surface, its panel laid out over everything and drawn last
     * (translationZ), with nothing else showing but a hint that fades after a few seconds. Back or OK returns.
     */
    private void enterFullScreen() {
        View panel = nodes[PANEL];
        if (panel == null || fullScreen || live.state() == Live.FAILED) return;
        fullScreen = true;
        panelPlace = panel.getLayoutParams();
        panel.setPadding(0, 0, 0, 0);
        panel.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        // Its video covers whatever was drawn before it; the two views added after it step aside, so only the
        // hint and the quality bar (added after these) draw over the video.
        nodes[FULL].setVisibility(View.INVISIBLE);
        panelSide.setVisibility(View.INVISIBLE);
        fullHint = text(ui400, 24, Tokens.PLASTER_100);
        fullHint.setPadding(Ui.ipx(28), Ui.ipx(12), Ui.ipx(28), Ui.ipx(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Tokens.SCRIM_PANEL);
        bg.setCornerRadius(Ui.px(36));
        fullHint.setBackground(bg);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lp.bottomMargin = Ui.ipx(Tokens.SPACE_SAFE_BLOCK);
        root.addView(fullHint, lp);
        flash(t.fullScreenHint(), 4000);
    }

    /** Full screen's hint, or the quality just picked, for a moment. */
    private void flash(String s, long ms) {
        if (fullHint == null) return;
        fullHint.setText(s);
        fullHint.animate().cancel();
        fullHint.setAlpha(1);
        fullHint.animate().alpha(0).setStartDelay(ms).setDuration(Tokens.MOTION_OVERLAY_FADE_MS);
    }

    /**
     * Full screen's quality bar at the bottom, over the video: «الجودة», then each rendition the stream's master
     * playlist offers (lowest first) with what an hour of it costs in data; the one playing is outlined in gold and
     * focused. ◀▶ move, OK picks it (saved for next time, also for the panel), Back closes the bar. It fades away
     * after a few seconds without a key.
     */
    private void showQualityBar() {
        List<Live.Rendition> list = live.renditions();
        if (qualityBar != null || list.isEmpty() || !fullScreen) return;
        qualityBar = new LinearLayout(this);
        qualityBar.setGravity(Gravity.CENTER_VERTICAL);
        qualityBar.setPadding(Ui.ipx(32), Ui.ipx(14), Ui.ipx(24), Ui.ipx(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Tokens.SCRIM_PANEL);
        bg.setCornerRadius(Ui.px(Tokens.RADIUS_PANEL));
        qualityBar.setBackground(bg);
        qualityBar.setClipChildren(false);
        qualityBar.setClipToPadding(false);
        TextView label = text(ui500, 24, Tokens.GOLD_400);
        label.setText(t.quality());
        qualityBar.addView(label);
        qualityButtons = new FocusTile[list.size()];
        qualityHeights = new int[list.size()];
        float r = FocusTile.REACH;
        FocusTile current = null;
        for (int i = 0; i < list.size(); i++) {
            Live.Rendition q = list.get(i);
            boolean on = q.height == live.height();
            FocusTile b = new FocusTile(this, 18);
            b.setRest(on ? Tokens.LAPIS_900 : Tokens.LAPIS_800, on ? Tokens.GOLD_400 : Tokens.LINE_SOFT, 2);
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER_HORIZONTAL);
            col.setPadding(Ui.ipx(24), Ui.ipx(8), Ui.ipx(24), Ui.ipx(8));
            TextView name = text(ui500, 26, Tokens.PLASTER_100);
            name.setText(t.quality(q.height));
            TextView cost = text(ui400, 20, Tokens.MIST_400);
            cost.setText(t.perHour(q.bandwidth));
            col.addView(name);
            col.addView(cost);
            b.addView(col, new FrameLayout.LayoutParams(-2, -2));
            b.setLook(f -> {
                name.setTextColor(f ? Tokens.FOCUS_TEXT : Tokens.PLASTER_100);
                cost.setTextColor(f ? Tokens.FOCUS_TEXT : Tokens.MIST_400);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.setMarginStart(Ui.ipx(i == 0 ? 24 - r : 12 - 2 * r));
            qualityBar.addView(b, lp);
            qualityButtons[i] = b;
            qualityHeights[i] = q.height;
            if (on) current = b;
        }
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lp.bottomMargin = Ui.ipx(Tokens.SPACE_SAFE_BLOCK);
        root.addView(qualityBar, lp);
        // The hint sits where the bar is: it gives way.
        if (fullHint != null) {
            fullHint.animate().cancel();
            fullHint.setAlpha(0);
        }
        (current != null ? current : qualityButtons[0]).requestFocus();
        keepQualityBar();
    }

    /** The bar fades away after a few seconds without a key; each key starts the wait again. */
    private void keepQualityBar() {
        LinearLayout bar = qualityBar;
        if (bar == null) return;
        bar.animate().cancel();
        bar.setAlpha(1);
        bar.animate().alpha(0).setStartDelay(5000).setDuration(Tokens.MOTION_OVERLAY_FADE_MS)
            .withEndAction(() -> { if (qualityBar == bar) closeQualityBar(); });
    }

    private void closeQualityBar() {
        if (qualityBar == null) return;
        qualityBar.animate().cancel();
        root.removeView(qualityBar);
        qualityBar = null;
        qualityButtons = null;
        qualityHeights = null;
        if (nodes[FULL] != null) nodes[FULL].requestFocus();
    }

    /** That rendition on the same player (a short pause), saved; its height shows for a moment. */
    private void pickQuality(int i) {
        int h = qualityHeights[i];
        closeQualityBar();
        live.setQuality(this, h);
        flash(t.quality(live.height()), 2500);
    }

    /** Back to the panel on Home, still playing, with focus on Full screen. */
    private void exitFullScreen() {
        if (!fullScreen) return;
        closeQualityBar();
        fullScreen = false;
        if (fullHint != null) {
            fullHint.animate().cancel();
            root.removeView(fullHint);
            fullHint = null;
        }
        View panel = nodes[PANEL];
        if (panel == null) return;
        int r = Ui.ipx(FocusTile.REACH);
        panel.setPadding(r, r, r, r);
        panel.setLayoutParams(panelPlace);
        panelSide.setVisibility(View.VISIBLE);
        boolean failed = live.state() == Live.FAILED;
        nodes[FULL].setVisibility(failed ? View.INVISIBLE : View.VISIBLE);
        nodes[failed ? PANEL : FULL].requestFocus();
    }

    /** The prayer strip and the favourites and More rows: hidden under the live panel, back as they were after. */
    private void rowsShown(boolean shown) {
        int v = shown ? View.VISIBLE : View.INVISIBLE;
        for (View c : cell) c.setVisibility(v);
        for (int i = F0; i <= ALL; i++) nodes[i].setVisibility(v);
        favLabel.setVisibility(v);
        moreLabel.setVisibility(v);
    }

    /** The panel goes and the strip and rows it covered come back; focus on it moves to the Live pill. */
    private void closeLivePanel() {
        View panel = nodes[PANEL];
        if (panel == null) return;
        boolean focused = panel.isFocused() || nodes[FULL].isFocused()
            || (qualityButtons != null && java.util.Arrays.asList(qualityButtons).contains(getCurrentFocus()));
        if (fullScreen) {
            fullScreen = false;
            if (fullHint != null) root.removeView(fullHint);
            fullHint = null;
            if (qualityBar != null) {
                qualityBar.animate().cancel();
                root.removeView(qualityBar);
            }
            qualityBar = null;
            qualityButtons = null;
            qualityHeights = null;
        }
        root.removeView(nodes[FULL]);
        nodes[FULL] = null;
        nodes[PANEL] = null;
        rowsShown(true);
        root.removeView(panel);
        root.removeView(panelSide);
        panelSide = null;
        if (focused) nodes[LIVE].requestFocus();
    }

    // ---------------------------------------------------------------- reminder card

    /** "Asr in 13 minutes" at the bottom end, over the apps; it takes focus until Got it, Today's prayers or Back. */
    private void showReminder() {
        if (card != null) dismissReminder();
        exitFullScreen();
        closeRadioCard(false);
        closeLiveCard(false);
        Date now = new Date();
        day = new PrayerDay(this, now);
        cardFor = day.nextTime;
        readClockFormat();

        card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.ipx(36), Ui.ipx(32), Ui.ipx(36), Ui.ipx(32));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Tokens.CARD_FILL);
        bg.setCornerRadius(Ui.px(24));
        bg.setStroke(Ui.ipx(6), Tokens.ON_PLASTER_BORDER);
        card.setBackground(bg);
        card.setClipChildren(false);
        card.setClipToPadding(false);

        String[] names = t.prayerNames();
        if (day.friday) names[PrayerDay.DHUHR] = t.jumuah();
        TextView label = caps(text(ui400, 20, Tokens.ON_PLASTER_LABEL));
        label.setText(t.apLabel());
        TextView title = text(displayBold, 52, Tokens.CARD_TEXT);
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
        TextView v = text(ui400, 24, filled ? Tokens.PLASTER_100 : Tokens.CARD_TEXT);
        v.setText(label);
        v.setGravity(Gravity.CENTER);
        v.setPadding(Ui.ipx(32), 0, Ui.ipx(32), 0);
        b.addView(v, new FrameLayout.LayoutParams(-2, -1));
        b.setLook(f -> v.setTextColor(f ? Tokens.FOCUS_TEXT : (filled ? Tokens.PLASTER_100 : Tokens.CARD_TEXT)));
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
        // The live video filling the screen: Back or OK returns to the panel, ▲▼ or Menu open the quality bar.
        // With the bar open: ◀▶ move along it (mirrored in Arabic), OK picks, Back closes it first.
        if (fullScreen) {
            boolean ok = k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER || k == KeyEvent.KEYCODE_NUMPAD_ENTER;
            boolean down = e.getAction() == KeyEvent.ACTION_DOWN, up = e.getAction() == KeyEvent.ACTION_UP;
            boolean sideways = k == KeyEvent.KEYCODE_DPAD_LEFT || k == KeyEvent.KEYCODE_DPAD_RIGHT;
            boolean upDown = k == KeyEvent.KEYCODE_DPAD_UP || k == KeyEvent.KEYCODE_DPAD_DOWN;
            if (qualityBar != null) {
                keepQualityBar();
                int i = java.util.Arrays.asList(qualityButtons).indexOf(getCurrentFocus());
                if (k == KeyEvent.KEYCODE_BACK) {
                    if (up) closeQualityBar();
                } else if (ok) {
                    if (up && i >= 0) pickQuality(i);
                } else if (sideways) {
                    boolean toEnd = (k == KeyEvent.KEYCODE_DPAD_RIGHT) != rtl;
                    if (down) qualityButtons[i < 0 ? 0 : Math.max(0, Math.min(qualityButtons.length - 1, i + (toEnd ? 1 : -1)))].requestFocus();
                } else if (!upDown) {
                    return super.dispatchKeyEvent(e);
                }
                return true;
            }
            if (k == KeyEvent.KEYCODE_BACK || ok) {
                if (up) exitFullScreen();
                return true;
            }
            if (upDown || k == KeyEvent.KEYCODE_MENU) {
                if (down) showQualityBar();
                return true;
            }
            if (sideways) return true;
            return super.dispatchKeyEvent(e);
        }
        // The station list (from the radio card): Back steps back, Up/Down move through it.
        if (chooser != null) return chooserKey(e);
        if (radioCard != null) {
            if (k == KeyEvent.KEYCODE_BACK) {
                if (e.getAction() == KeyEvent.ACTION_UP) closeRadioCard(true);
                return true;
            }
            if (k == KeyEvent.KEYCODE_DPAD_LEFT || k == KeyEvent.KEYCODE_DPAD_RIGHT) {
                // Play, Station, Screen off from the start side; toward the end is Left in Arabic.
                boolean toEnd = (k == KeyEvent.KEYCODE_DPAD_RIGHT) != rtl;
                if (e.getAction() == KeyEvent.ACTION_DOWN) {
                    View[] row = {cardPlay, cardStation, cardDark};
                    int i = java.util.Arrays.asList(row).indexOf(getCurrentFocus());
                    row[i < 0 ? 0 : Math.max(0, Math.min(row.length - 1, i + (toEnd ? 1 : -1)))].requestFocus();
                }
                return true;
            }
            if (k == KeyEvent.KEYCODE_DPAD_UP || k == KeyEvent.KEYCODE_DPAD_DOWN) return true;
            return super.dispatchKeyEvent(e);
        }
        if (liveCard != null) {
            if (k == KeyEvent.KEYCODE_BACK) {
                if (e.getAction() == KeyEvent.ACTION_UP) closeLiveCard(true);
                return true;
            }
            if (k == KeyEvent.KEYCODE_DPAD_LEFT || k == KeyEvent.KEYCODE_DPAD_RIGHT) {
                // Makkah, Madinah (and Stop) from the start side; toward the end is Left in Arabic.
                boolean toEnd = (k == KeyEvent.KEYCODE_DPAD_RIGHT) != rtl;
                if (e.getAction() == KeyEvent.ACTION_DOWN) {
                    int i = java.util.Arrays.asList(liveButtons).indexOf(getCurrentFocus());
                    liveButtons[i < 0 ? 0 : Math.max(0, Math.min(liveButtons.length - 1, i + (toEnd ? 1 : -1)))].requestFocus();
                }
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
            // Back on Home stops the live video if it is on; otherwise it returns focus to the arch. It never
            // leaves Home.
            if (e.getAction() == KeyEvent.ACTION_UP) {
                if (nodes[PANEL] != null) live.stop();
                else nodes[ARCH].requestFocus();
            }
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
        // While the live panel shows it stands for the favourites and More rows it covers.
        boolean panel = nodes[PANEL] != null;
        switch (from) {
            case ARCH: to = right ? (panel ? PANEL : F0) : up ? LIVE : -1; break;
            case LIVE: to = left ? ARCH : right ? RADIO : down ? (panel ? PANEL : F0) : -1; break;
            case RADIO: to = left ? LIVE : right ? WIFI : down ? (panel ? PANEL : F0 + 1) : -1; break;
            case WIFI: to = left ? RADIO : right ? NIGHT : down ? (panel ? PANEL : F0 + 2) : -1; break;
            case NIGHT: to = left ? WIFI : right ? SETTINGS : down ? (panel ? PANEL : F0 + 3) : -1; break;
            case SETTINGS: to = left ? NIGHT : down ? (panel ? PANEL : F0 + 3) : -1; break;
            case PANEL: to = left ? ARCH : up ? LIVE : down ? FULL : -1; break;
            // Full screen sits under the panel's end corner, in the row of the arch's foot button.
            case FULL: to = left ? ARCH : up ? PANEL : -1; break;
            default:
                if (from >= F0 && from < M0) {
                    int i = from - F0;
                    if (left) to = i == 0 ? ARCH : from - 1;
                    else if (right) to = i == 3 ? -1 : from + 1;
                    else if (up) to = i == 3 ? SETTINGS : i == 2 ? WIFI : i == 1 ? RADIO : LIVE;
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
