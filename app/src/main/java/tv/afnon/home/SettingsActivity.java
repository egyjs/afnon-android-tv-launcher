package tv.afnon.home;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.Madhab;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Afnon Settings (design/Settings.dc.html): sections down the start side, the section's rows beside them, and a
 * preview of today's Asr under the current choices. Every change is saved at once; the other screens rebuild
 * when they come back to the front.
 *
 * <p>Focus: Up/Down through the sections, toward the end side (or OK) into the rows. On a row Up/Down move,
 * ◀▶ change a choice, a stepper or a switch, OK opens a list or flips the choice; Back returns to the sections,
 * Back again goes Home. Network and Device settings open the TV's own screens.
 */
public class SettingsActivity extends Screen {

    private static final int PRAYER = 0, REMINDERS = 1, RADIO = 2, DISPLAY = 3, NETWORK = 4, DEVICE = 5, SECTIONS = 6;
    private static final int VALUE = 0, SEG = 1, STEP = 2, TOGGLE = 3;
    private static final int SIDEBAR = Tokens.LAPIS_950;
    private static final float ROWS_TOP = 112, ROW_H = 88, ROW_GAP = 10;
    private static final float NAV_TOP = 152, NAV_H = 76, NAV_GAP = 10;

    /** One row: what it shows and what ◀▶ and OK do. */
    private abstract class Row {
        final int kind;
        FocusTile tile;
        TextView label, help;
        LinearLayout control;

        Row(int kind) {
            this.kind = kind;
        }

        abstract String label();

        String help() { return ""; }

        /** VALUE and STEP rows. */
        String value() { return ""; }

        /** SEG rows: the choices (already in the screen's digits) and the chosen one. */
        String[] segs() { return new String[0]; }

        int selected() { return 0; }

        void select(int i) {}

        void step(int delta) {}

        boolean on() { return false; }

        void set(boolean on) {}

        /** VALUE rows: OK. */
        void open() {}
    }

    private int section = PRAYER;
    private boolean onNav = true;
    private int rowIndex;
    private final FocusTile[] nav = new FocusTile[SECTIONS];
    private final TextView[] navLabel = new TextView[SECTIONS];
    private final List<Row> rows = new ArrayList<>();
    private TextView sectionLabel, previewValue;
    private LinearLayout hints;
    private FrameLayout chooser;
    /** What Back in the open chooser goes back to (the city list returns to the countries); null closes it. */
    private Runnable chooserBack;
    /** The voice being previewed in the adhan list. */
    private android.media.MediaPlayer previewPlayer;
    private PrayerDay day;

    @Override
    void build() {
        // A rebuild (back with settings changed elsewhere, or a language change) drops an open chooser with the
        // old root; left set, it would swallow every key while nothing shows.
        stopPreview();
        chooser = null;
        chooserBack = null;
        newRoot();
        root.setBackgroundColor(Tokens.LAPIS_900);
        readClockFormat();

        View side = new View(this);
        side.setBackgroundColor(SIDEBAR);
        place(side, 0, 0, Tokens.LAYOUT_SETTINGS_SIDEBAR_WIDTH, 1080);

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        Icon mihrab = new Icon(this, Icon.MIHRAB, 1.3f);
        mihrab.setColor(Tokens.GOLD_400);
        head.addView(mihrab, new LinearLayout.LayoutParams(Ui.ipx(36), Ui.ipx(46)));
        TextView title = text(displayBold, 52, Tokens.PLASTER_100);
        title.setText(t.settings());
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-2, -2);
        tlp.setMarginStart(Ui.ipx(16));
        head.addView(title, tlp);
        lineHeight(title, 1.1f);
        head.setClipChildren(false);
        place(head, 96, 56, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        for (int i = 0; i < SECTIONS; i++) nav[i] = navItem(i);

        LinearLayout preview = new LinearLayout(this);
        preview.setOrientation(LinearLayout.VERTICAL);
        preview.setPadding(Ui.ipx(24), Ui.ipx(20), Ui.ipx(24), Ui.ipx(20));
        preview.setBackground(round(Tokens.LAPIS_800, 16));
        TextView pl = text(ui400, 20, Tokens.MIST_400);
        pl.setText(t.preview());
        previewValue = text(display, Tokens.FONT_SIZE_DISPLAY_XS, Tokens.GOLD_400);
        preview.addView(pl);
        preview.addView(previewValue);
        lineHeight(previewValue, 1.2f);
        ((LinearLayout.LayoutParams) previewValue.getLayoutParams()).topMargin += Ui.ipx(6);
        place(preview, 96, 1080 - 56 - 124, 424, 124);

        sectionLabel = caps(text(ui400, 22, Tokens.MIST_400));
        sectionLabel.setAllCaps(!t.ar);
        place(sectionLabel, 640, 56, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        hints = new LinearLayout(this);
        hints.setOrientation(LinearLayout.HORIZONTAL);
        place(hints, 640, 976, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        setContentView(root);
        showSection();
        if (onNav || rows.isEmpty()) focusNav();
        else focusRow(Math.min(rowIndex, rows.size() - 1));
    }

    @Override
    void onTick() {
        // Nothing moves on this screen; the preview changes only with the settings.
    }

    // ---------------------------------------------------------------- sections

    private String sectionName(int i) {
        switch (i) {
            case PRAYER: return t.sectionPrayer();
            case REMINDERS: return t.sectionReminders();
            case RADIO: return t.sectionRadio();
            case DISPLAY: return t.sectionDisplay();
            case NETWORK: return t.sectionNetwork(ssid());
            default: return t.sectionDevice();
        }
    }

    private FocusTile navItem(int i) {
        FocusTile b = new FocusTile(this, 14);
        b.setFocusScale(1.04f);
        TextView v = text(ui400, 26, Tokens.MIST_400);
        v.setText(sectionName(i));
        v.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        v.setPadding(Ui.ipx(24), 0, Ui.ipx(24), 0);
        v.setSingleLine(true);
        b.addView(v, new FrameLayout.LayoutParams(-1, -1));
        navLabel[i] = v;
        b.setLook(f -> styleNav(i));
        placeTile(b, 96, NAV_TOP + i * (NAV_H + NAV_GAP), 424, NAV_H);
        return b;
    }

    /** The open section keeps a gold inset ring when focus is in its rows; focused, it is the gold tile. */
    private void styleNav(int i) {
        boolean focused = nav[i] != null && nav[i].isFocused();
        boolean open = i == section;
        if (nav[i] != null) nav[i].setRest(open ? Tokens.LAPIS_800 : 0, Tokens.GOLD_400, open ? 2 : 0);
        navLabel[i].setTextColor(focused ? Tokens.LAPIS_900 : (open ? Tokens.PLASTER_100 : Tokens.MIST_400));
    }

    private void showSection() {
        for (Row r : rows) root.removeView(r.tile);
        rows.clear();
        sectionLabel.setText(sectionName(section));
        if (section == NETWORK) sectionLabel.setText(t.sectionNetwork(null));
        for (int i = 0; i < SECTIONS; i++) styleNav(i);
        rows.addAll(rowsFor(section));
        for (int i = 0; i < rows.size(); i++) addRow(rows.get(i), i);
        refresh();
    }

    private List<Row> rowsFor(int s) {
        Context c = this;
        switch (s) {
            case PRAYER: return Arrays.asList(
                new Row(VALUE) {
                    String label() { return t.location(); }
                    String help() { Prefs.City city = Prefs.city(c); return t.locationHelp(city.lat, city.lng); }
                    String value() { Prefs.City city = Prefs.city(c); return t.ar ? t.place(city.ar, city.countryAr) : t.place(city.en, city.countryEn); }
                    void open() {
                        List<String> countries = new ArrayList<>();
                        for (Prefs.City city : Prefs.CITIES) {
                            String name = t.ar ? city.countryAr : city.countryEn;
                            if (!countries.contains(name)) countries.add(name);
                        }
                        Prefs.City now = Prefs.city(c);
                        int sel = countries.indexOf(t.ar ? now.countryAr : now.countryEn);
                        // The city list opens once this chooser has closed (closeChooser runs after the pick).
                        openChooser(t.country(), countries.toArray(new String[0]), sel,
                            i -> root.post(() -> openCities(countries.get(i), this::open)));
                    }
                },
                new Row(VALUE) {
                    String label() { return t.method(); }
                    String help() { return t.methodAngles(Prefs.method(c)); }
                    String value() { return t.methodName(Prefs.method(c)); }
                    void open() {
                        String[] names = new String[Prefs.METHODS.length];
                        int sel = 0;
                        for (int i = 0; i < names.length; i++) {
                            names[i] = t.methodName(Prefs.METHODS[i]);
                            if (Prefs.METHODS[i] == Prefs.method(c)) sel = i;
                        }
                        openChooser(t.method(), names, sel, i -> Prefs.setMethod(c, Prefs.METHODS[i]));
                    }
                },
                new Row(SEG) {
                    String label() { return t.asrSchool(); }
                    String help() { return t.asrSchoolHelp(); }
                    String[] segs() { return t.asrSchools(); }
                    int selected() { return Prefs.madhab(c) == Madhab.HANAFI ? 1 : 0; }
                    void select(int i) { Prefs.setMadhab(c, i == 1 ? Madhab.HANAFI : Madhab.SHAFI); }
                },
                new Row(STEP) {
                    String label() { return t.hijriAdjust(); }
                    String help() { return t.hijriToday(hijri(day)); }
                    String value() { return t.days(Prefs.hijriOffset(c)); }
                    void step(int d) { Prefs.setHijriOffset(c, Prefs.hijriOffset(c) + d); }
                },
                new Row(VALUE) {
                    String label() { return t.roomRow(); }
                    String help() { return t.roomHelp(); }
                    String value() { int f = facing(); return f < 0 ? t.notSet() : t.bearingLabel(f); }
                    void open() { startSafely(new Intent(c, PrayerDayActivity.class).putExtra("room", true)); }
                });
            case REMINDERS: return Arrays.asList(
                new Row(SEG) {
                    final int[] minutes = {10, 15, 0};
                    String label() { return t.remind(); }
                    String help() { return t.remindHelp(); }
                    String[] segs() { return digits(t.remindChoices()); }
                    int selected() {
                        int m = Prefs.remindMinutes(c);
                        for (int i = 0; i < minutes.length; i++) if (minutes[i] == m) return i;
                        return 1;
                    }
                    void select(int i) { Prefs.setRemindMinutes(c, minutes[i]); }
                },
                new Row(TOGGLE) {
                    String label() { return t.adhanRow(); }
                    String help() { return t.adhanHelp(); }
                    boolean on() { return Prefs.adhan(c); }
                    void set(boolean on) { Prefs.setAdhan(c, on); }
                },
                voiceRow(false),
                voiceRow(true));
            case RADIO: return Arrays.asList(
                new Row(TOGGLE) {
                    String label() { return t.station(Radio.station(c)); }
                    String help() { return t.radioRowHelp(Radio.station(c)); }
                    boolean on() { return Radio.on(); }
                    void set(boolean on) { if (on) Radio.start(c); else Radio.stop(); }
                },
                new Row(VALUE) {
                    String label() { return t.stationRow(); }
                    String help() { return t.stationGroups()[Radio.station(c).reciter ? 1 : 0]; }
                    String value() { return t.stationChoice(Radio.station(c)); }
                    void open() {
                        int sel = Radio.station(c).reciter ? 1 : 0;
                        // The station list opens once this chooser has closed (closeChooser runs after the pick).
                        openChooser(t.stationRow(), t.stationGroups(), sel, i -> root.post(() -> openStations(i == 1, this::open)));
                    }
                },
                new Row(SEG) {
                    String label() { return t.afterAdhanRow(); }
                    String help() { return t.afterAdhanHelp(); }
                    String[] segs() { return t.afterAdhanChoices(); }
                    int selected() { return Prefs.radioResume(c) ? 0 : 1; }
                    void select(int i) { Prefs.setRadioResume(c, i == 0); }
                },
                new Row(SEG) {
                    final int[] minutes = {0, 15, -1};
                    String label() { return t.radioDarkRow(); }
                    String help() { return t.radioDarkHelp(); }
                    String[] segs() { return digits(t.radioDarkChoices()); }
                    int selected() {
                        int m = Prefs.radioScreenOff(c);
                        for (int i = 0; i < minutes.length; i++) if (minutes[i] == m) return i;
                        return 0;
                    }
                    void select(int i) { Prefs.setRadioScreenOff(c, minutes[i]); }
                });
            case DISPLAY: return Arrays.asList(
                new Row(SEG) {
                    final String[] values = {"system", "ar", "en"};
                    String label() { return t.language(); }
                    String help() { return t.languageHelp(); }
                    String[] segs() { return t.languages(); }
                    int selected() { return Math.max(0, Arrays.asList(values).indexOf(prefs().getString("locale", "system"))); }
                    void select(int i) { prefs().edit().putString("locale", values[i]).apply(); relaunch(); }
                },
                new Row(SEG) {
                    String label() { return t.digits(); }
                    String[] segs() { return new String[] {"0123", "٠١٢٣"}; }
                    int selected() { return t.eastern ? 1 : 0; }
                    void select(int i) { prefs().edit().putString("digits", i == 1 ? "eastern" : "western").apply(); relaunch(); }
                },
                new Row(SEG) {
                    final String[] values = {"system", "12", "24"};
                    String label() { return t.clockRow(); }
                    String help() { return t.clockHelp(); }
                    String[] segs() { return digits(t.clocks()); }
                    int selected() { return Math.max(0, Arrays.asList(values).indexOf(Prefs.clock(c))); }
                    void select(int i) { prefs().edit().putString("clock", values[i]).apply(); readClockFormat(); }
                });
            case NETWORK: return Arrays.asList(
                new Row(VALUE) {
                    String label() { return t.networkRow(); }
                    String help() { return t.networkHelp(); }
                    String value() { String s = ssid(); return s == null ? "" : s; }
                    void open() { startSafely(new Intent(Settings.ACTION_WIFI_SETTINGS)); }
                });
            default: return Arrays.asList(
                new Row(VALUE) {
                    String label() { return t.deviceRow(); }
                    String help() { return t.deviceHelp(); }
                    void open() { startSafely(new Intent(Settings.ACTION_SETTINGS)); }
                });
        }
    }

    /** The adhan recording for every prayer but Fajr, or for Fajr: a list that plays each voice as focus reaches it. */
    private Row voiceRow(boolean fajr) {
        return new Row(VALUE) {
            String label() { return fajr ? t.fajrVoiceRow() : t.voiceRow(); }
            String help() { return fajr ? t.fajrVoiceHelp() : t.voiceHelp(); }
            String value() { return Adhans.name(t, Prefs.voice(SettingsActivity.this, fajr)); }
            void open() {
                String[] names = new String[Adhans.VOICES.length];
                for (int i = 0; i < names.length; i++) names[i] = Adhans.name(t, Adhans.VOICES[i][0]);
                openChooser(label(), names, Adhans.index(Prefs.voice(SettingsActivity.this, fajr)),
                    i -> Prefs.setVoice(SettingsActivity.this, fajr, Adhans.VOICES[i][0]),
                    i -> preview(Adhans.VOICES[i][0]));
            }
        };
    }

    private void preview(String id) {
        Adhans.stop(previewPlayer);
        previewPlayer = Adhans.play(this, id);
    }

    private void stopPreview() {
        Adhans.stop(previewPlayer);
        previewPlayer = null;
    }

    @Override
    protected void onPause() {
        stopPreview();
        super.onPause();
    }

    private String[] digits(String[] s) {
        String[] out = new String[s.length];
        for (int i = 0; i < s.length; i++) out[i] = t.d(s[i]);
        return out;
    }

    /** Language or digits changed: every string on the screen changes, so rebuild it where it was. */
    private void relaunch() {
        loadSettings(null);
        build();
    }

    // ---------------------------------------------------------------- rows

    private void addRow(Row r, int i) {
        r.tile = new FocusTile(this, 16);
        r.tile.setFocusScale(Tokens.FOCUS_SCALE_SETTINGS_ROW);
        r.tile.setRest(Tokens.LAPIS_800, 0, 0);
        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setPadding(Ui.ipx(28), 0, Ui.ipx(28), 0);
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        r.label = text(ui500, 26, Tokens.PLASTER_100);
        r.help = text(ui400, 20, Tokens.PLASTER_100);
        r.help.setSingleLine(true);
        words.addView(r.label);
        words.addView(r.help);
        lineHeight(r.label, 1.25f);
        lineHeight(r.help, 1.35f);
        line.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
        r.control = new LinearLayout(this);
        r.control.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-2, -2);
        clp.setMarginStart(Ui.ipx(24));
        line.addView(r.control, clp);
        r.tile.addView(line, new FrameLayout.LayoutParams(-1, -1));
        r.tile.setLook(f -> render(r, f));
        placeTile(r.tile, 640, ROWS_TOP + i * (ROW_H + ROW_GAP), Tokens.LAYOUT_SETTINGS_ROW_WIDTH, ROW_H);
    }

    /** Every row and the preview, from the saved settings. */
    private void refresh() {
        day = new PrayerDay(this, new Date());
        for (Row r : rows) render(r, r.tile.isFocused());
        previewValue.setText(time(day.times[PrayerDay.ASR], 1f));
    }

    private void render(Row r, boolean focused) {
        if (day == null) return;
        int ink = focused ? Tokens.LAPIS_900 : Tokens.PLASTER_100;
        r.label.setText(r.label());
        r.label.setTextColor(ink);
        String help = r.help();
        r.help.setText(help);
        r.help.setVisibility(help.isEmpty() ? View.GONE : View.VISIBLE);
        r.help.setTextColor(ink);
        r.help.setAlpha(0.75f);
        r.control.removeAllViews();
        switch (r.kind) {
            case VALUE: {
                TextView v = text(ui400, 24, ink);
                v.setText(r.value());
                v.setSingleLine(true);
                r.control.addView(v);
                Chevron ch = new Chevron(this, ink);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.ipx(22), Ui.ipx(22));
                lp.setMarginStart(Ui.ipx(12));
                r.control.addView(ch, lp);
                break;
            }
            case SEG: {
                LinearLayout track = new LinearLayout(this);
                int pad = Ui.ipx(6);
                track.setPadding(pad, pad, pad, pad);
                track.setBackground(round(focused ? Tokens.SEG_TRACK_FOCUSED : Tokens.LAPIS_900, 30));
                String[] segs = r.segs();
                for (int i = 0; i < segs.length; i++) {
                    boolean chosen = i == r.selected();
                    TextView s = text(ui400, 22, chosen ? (focused ? Tokens.PLASTER_100 : Tokens.LAPIS_900) : (focused ? Tokens.LAPIS_900 : Tokens.MIST_400));
                    s.setText(segs[i]);
                    s.setGravity(Gravity.CENTER);
                    s.setPadding(Ui.ipx(24), 0, Ui.ipx(24), 0);
                    if (chosen) {
                        GradientDrawable bg = round(focused ? Tokens.LAPIS_900 : Tokens.PLASTER_100, 26);
                        if (focused) bg.setStroke(Ui.ipx(3), Tokens.PLASTER_100);
                        s.setBackground(bg);
                    }
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, Ui.ipx(52));
                    if (i > 0) lp.setMarginStart(Ui.ipx(6));
                    track.addView(s, lp);
                }
                r.control.addView(track);
                break;
            }
            case STEP: {
                r.control.addView(stepButton("−"), new LinearLayout.LayoutParams(Ui.ipx(48), Ui.ipx(48)));
                TextView v = text(ui400, 26, ink);
                v.setText(r.value());
                v.setGravity(Gravity.CENTER);
                v.setMinWidth(Ui.ipx(150));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
                lp.setMarginStart(Ui.ipx(18));
                lp.setMarginEnd(Ui.ipx(18));
                r.control.addView(v, lp);
                r.control.addView(stepButton("+"), new LinearLayout.LayoutParams(Ui.ipx(48), Ui.ipx(48)));
                break;
            }
            default:
                r.control.addView(new Switch(this, r.on(), focused), new LinearLayout.LayoutParams(Ui.ipx(88), Ui.ipx(48)));
        }
    }

    private TextView stepButton(String sign) {
        TextView b = text(ui400, 26, Tokens.PLASTER_100);
        b.setText(sign);
        b.setGravity(Gravity.CENTER);
        b.setIncludeFontPadding(false);
        b.setBackground(round(Tokens.LAPIS_700, 24));
        return b;
    }

    private static GradientDrawable round(int color, float radiusDesignPx) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(Ui.px(radiusDesignPx));
        return d;
    }

    private boolean savedFeedback;
    private final Runnable clearSaved = () -> {
        savedFeedback = false;
        if (!onNav && rowIndex >= 0 && rowIndex < rows.size()) updateHints();
    };

    private void showSaved() {
        savedFeedback = true;
        updateHints();
        root.removeCallbacks(clearSaved);
        root.postDelayed(clearSaved, 1500);
    }

    private void updateHints() {
        if (onNav || rowIndex < 0 || rowIndex >= rows.size()) return;
        Row r = rows.get(rowIndex);
        List<String> h = new ArrayList<>();
        if (savedFeedback) h.add(t.hintSaved());
        h.add(t.hintMove());
        h.add(r.kind == VALUE ? t.hintChoose() : t.hintChange());
        h.add(t.hintBackMenu());
        setHints(hints, h);
    }

    private void focusNav() {
        onNav = true;
        savedFeedback = false;
        root.removeCallbacks(clearSaved);
        nav[section].requestFocus();
        List<String> h = new ArrayList<>();
        h.add(t.hintMove());
        h.add(t.hintOpen());
        h.add(t.hintBackHome());
        setHints(hints, h);
    }

    private void focusRow(int i) {
        onNav = false;
        rowIndex = i;
        Row r = rows.get(i);
        r.tile.requestFocus();
        updateHints();
        styleNav(section);
    }

    private void navTo(int i) {
        if (i < 0 || i >= SECTIONS || i == section) return;
        section = i;
        showSection();
        focusNav();
    }

    @Override
    protected void onDestroy() {
        root.removeCallbacks(clearSaved);
        super.onDestroy();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int k = e.getKeyCode();
        boolean down = e.getAction() == KeyEvent.ACTION_DOWN;
        if (chooser != null) {
            if (k == KeyEvent.KEYCODE_BACK) {
                if (!down) {
                    Runnable back = chooserBack;
                    closeChooser();
                    if (back != null) back.run();
                }
                return true;
            }
            if (k == KeyEvent.KEYCODE_DPAD_LEFT || k == KeyEvent.KEYCODE_DPAD_RIGHT) return true;
            return super.dispatchKeyEvent(e);
        }
        if (k == KeyEvent.KEYCODE_BACK) {
            if (!down) {
                if (onNav) finish();
                else focusNav();
            }
            return true;
        }
        boolean ok = k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER || k == KeyEvent.KEYCODE_NUMPAD_ENTER;
        boolean horizontal = k == KeyEvent.KEYCODE_DPAD_LEFT || k == KeyEvent.KEYCODE_DPAD_RIGHT;
        boolean vertical = k == KeyEvent.KEYCODE_DPAD_UP || k == KeyEvent.KEYCODE_DPAD_DOWN;
        if (!ok && !horizontal && !vertical) return super.dispatchKeyEvent(e);
        if (!down || (ok && e.getRepeatCount() > 0)) return true;
        // "Toward the end" is Right in English and Left in Arabic; choices and steppers run the same way.
        boolean toEnd = (k == KeyEvent.KEYCODE_DPAD_RIGHT) != rtl;
        int up = k == KeyEvent.KEYCODE_DPAD_UP ? -1 : 1;

        if (onNav) {
            if (vertical) navTo(section + up);
            else if (ok && section == NETWORK) startSafely(new Intent(Settings.ACTION_WIFI_SETTINGS));
            else if (ok && section == DEVICE) startSafely(new Intent(Settings.ACTION_SETTINGS));
            else if ((ok || toEnd) && !rows.isEmpty()) focusRow(0);
            return true;
        }

        Row r = rows.get(rowIndex);
        if (vertical) {
            int to = rowIndex + up;
            if (to >= 0 && to < rows.size()) focusRow(to);
            return true;
        }
        int d = toEnd ? 1 : -1;
        switch (r.kind) {
            case SEG: {
                int n = r.segs().length;
                if (ok) {
                    showSaved();
                    return true;
                }
                int to = Math.max(0, Math.min(n - 1, r.selected() + d));
                if (to == r.selected()) {
                    if (!toEnd) focusNav();
                    return true;
                }
                r.select(to);
                showSaved();
                break;
            }
            case STEP:
                if (ok) {
                    showSaved();
                    return true;
                }
                r.step(d);
                showSaved();
                break;
            case TOGGLE:
                r.set(!r.on());
                showSaved();
                break;
            default:
                if (ok) r.open();
                else if (!toEnd) focusNav();
                return true;
        }
        // A language or digits change rebuilt the screen and its rows already.
        if (chooser == null && rows.contains(r)) refresh();
        return true;
    }

    // ---------------------------------------------------------------- list chooser

    /** Location's second step: one country's cities. A country with a single city takes it at once. Back runs back. */
    private void openCities(String country, Runnable back) {
        List<Integer> found = new ArrayList<>();
        for (int i = 0; i < Prefs.CITIES.length; i++) {
            Prefs.City city = Prefs.CITIES[i];
            if ((t.ar ? city.countryAr : city.countryEn).equals(country)) found.add(i);
        }
        if (found.size() == 1) {
            Prefs.setCity(this, found.get(0));
            refresh();
            focusRow(rowIndex);
            return;
        }
        String[] names = new String[found.size()];
        int current = Prefs.cityIndex(this);
        // -1 when the saved city is in another country: nothing is marked, focus starts on the first city.
        int sel = -1;
        for (int k = 0; k < names.length; k++) {
            Prefs.City city = Prefs.CITIES[found.get(k)];
            names[k] = t.ar ? city.ar : city.en;
            if (found.get(k) == current) sel = k;
        }
        openChooser(country, names, sel, k -> Prefs.setCity(this, found.get(k)));
        chooserBack = back;
    }

    /** Station's second step: the broadcast stations or the reciters. Playing radio moves to the pick. Back runs back. */
    private void openStations(boolean reciters, Runnable back) {
        List<Integer> found = new ArrayList<>();
        for (int i = 0; i < Radio.STATIONS.length; i++) if (Radio.STATIONS[i].reciter == reciters) found.add(i);
        String[] names = new String[found.size()];
        int current = Radio.stationIndex(this);
        // -1 when the station is in the other group: nothing is marked, focus starts on the first one.
        int sel = -1;
        for (int k = 0; k < names.length; k++) {
            names[k] = t.stationChoice(Radio.STATIONS[found.get(k)]);
            if (found.get(k) == current) sel = k;
        }
        openChooser(t.stationGroups()[reciters ? 1 : 0], names, sel, k -> Radio.switchTo(this, found.get(k)));
        chooserBack = back;
    }

    /** A list over the screen (cities, methods): OK picks, Back cancels. Up/Down are the system's own. */
    private void openChooser(String title, String[] items, int selected, IntConsumer pick) {
        openChooser(title, items, selected, pick, null);
    }

    /** onFocus, if given, hears each item as focus reaches it (the adhan list plays that voice). */
    private void openChooser(String title, String[] items, int selected, IntConsumer pick, IntConsumer onFocus) {
        chooser = new FrameLayout(this);
        chooser.setBackgroundColor(Tokens.SCRIM_MENU);
        chooser.setClickable(true);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.ipx(48), Ui.ipx(40), Ui.ipx(48), Ui.ipx(32));
        GradientDrawable bg = round(Tokens.LAPIS_800, 24);
        bg.setStroke(Ui.ipx(2), Tokens.GOLD_400_60);
        card.setBackground(bg);
        card.setClipChildren(false);

        TextView head = text(displayBold, Tokens.FONT_SIZE_DISPLAY_S, Tokens.GOLD_400);
        head.setText(title);
        card.addView(head);
        lineHeight(head, 1.2f);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        int reach = Ui.ipx(FocusTile.REACH);
        scroll.setPadding(reach, reach, reach, reach);
        scroll.setClipToPadding(false);   // the focus ring may use the padding...
        scroll.setClipChildren(true);     // ...but scrolled items must not draw over the title above
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
                v.setTextColor(f ? Tokens.LAPIS_900 : (chosen ? Tokens.GOLD_400 : Tokens.PLASTER_100));
                if (f && onFocus != null) onFocus.accept(index);
            });
            item.setOnClickListener(x -> {
                pick.accept(index);
                closeChooser();
                showSaved();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, Ui.ipx(68 + 2 * FocusTile.REACH));
            if (i > 0) lp.topMargin = Ui.ipx(6 - 2 * FocusTile.REACH);
            list.addView(item, lp);
            if (chosen || first == null) first = item;
        }
        scroll.addView(list);
        float listH = Math.min(items.length, 8) * 74 + 2 * FocusTile.REACH;
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, Ui.ipx(listH));
        slp.topMargin = Ui.ipx(20 - FocusTile.REACH);
        slp.setMarginStart(-reach);
        slp.setMarginEnd(-reach);
        card.addView(scroll, slp);

        chooser.addView(card, new FrameLayout.LayoutParams(Ui.ipx(900), -2, Gravity.CENTER));
        setRowsFocusable(false);
        root.addView(chooser, new FrameLayout.LayoutParams(-1, -1));
        FocusTile start = first;
        if (start != null) root.post(start::requestFocus);
        List<String> h = new ArrayList<>();
        h.add(t.hintMove());
        h.add(t.hintChoose());
        h.add(t.hintBackMenu().replace(t.ar ? "القائمة" : "Menu", t.cancel()));
        setHints(hints, h);
    }

    private void closeChooser() {
        stopPreview();
        root.removeView(chooser);
        chooser = null;
        chooserBack = null;
        setRowsFocusable(true);
        refresh();
        focusRow(rowIndex);
    }

    private void setRowsFocusable(boolean on) {
        for (FocusTile n : nav) n.setFocusable(on);
        for (Row r : rows) r.tile.setFocusable(on);
    }

    // ---------------------------------------------------------------- small views

    /** The value row's chevron toward the end side (design: M9 5l7 7-7 7); mirrored in Arabic. */
    private static final class Chevron extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        Chevron(Context c, int color) {
            super(c);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            p.setColor(color);
        }

        @Override
        protected void onDraw(Canvas c) {
            float k = getWidth() / 24f;
            p.setStrokeWidth(2.2f * k);
            if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) c.scale(-1, 1, getWidth() / 2f, 0);
            path.reset();
            path.moveTo(9 * k, 5 * k);
            path.lineTo(16 * k, 12 * k);
            path.lineTo(9 * k, 19 * k);
            c.drawPath(path, p);
        }
    }

    /** On: gold track, lapis knob at the end. Off: lapis-700 track, mist knob at the start. On a gold row the on track turns lapis so it still shows. */
    private static final class Switch extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final boolean on, focused;

        Switch(Context c, boolean on, boolean focused) {
            super(c);
            this.on = on;
            this.focused = focused;
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), r = h / 2;
            p.setColor(on ? (focused ? Tokens.LAPIS_900 : Tokens.GOLD_400) : Tokens.LAPIS_700);
            c.drawRoundRect(0, 0, w, h, r, r, p);
            float knob = h * 18 / 48, inset = h * 6 / 48;
            boolean atEnd = on;
            boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
            float cx = (atEnd != rtl) ? w - inset - knob : inset + knob;
            p.setColor(on ? (focused ? Tokens.GOLD_400 : Tokens.LAPIS_900) : Tokens.MIST_400);
            c.drawCircle(cx, r, knob, p);
        }
    }
}
