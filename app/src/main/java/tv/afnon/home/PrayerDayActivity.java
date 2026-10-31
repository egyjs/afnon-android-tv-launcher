package tv.afnon.home;

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Prayer day (design/PrayerDay.dc.html), opened with OK on the arch: the six times with their status and an
 * adhan bell each, and the Qibla. Up/Down move through the rows, Up from the first reaches the Home pill, OK on
 * a row turns its adhan on or off, Back returns Home. Toward the end side, the Qibla panel's room-direction
 * button opens a picker (with a how-to tip) for the way the viewer faces, so the dial can point from the sofa.
 */
public class PrayerDayActivity extends Screen {

    private static final int ROWS = 6, HOME = ROWS, ROOM = ROWS + 1;
    private static final float ROW_TOP = 236, ROW_H = 104, ROW_GAP = 12;

    private final FocusTile[] row = new FocusTile[ROWS];
    private final TextView[] rowName = new TextView[ROWS], rowStatus = new TextView[ROWS], rowTime = new TextView[ROWS];
    private final FrameLayout[] bell = new FrameLayout[ROWS];
    private final Icon[] bellIcon = new Icon[ROWS];
    /** 0 past, 1 now, 2 upcoming; the row's resting look. */
    private final int[] state = new int[ROWS];
    private FocusTile homePill;
    private TextView dates, degrees, direction, northLabel, qiblaHint, roomLabel;
    private QiblaView dial;
    private FocusTile roomButton;
    /** The row focus returns to from the Qibla side. */
    private int lastRow;
    // Room-direction picker, shown over the screen while open.
    private FrameLayout picker;
    private QiblaView pickerDial;
    private TextView pickerValue, pickerWhere;
    private int pickerFacing;
    // OK and Back act on release; Settings opens the picker on press, so that press's release must not close it.
    private boolean pickerPressed;
    private LinearLayout hints;
    private PrayerDay day;
    private long lastMinute = -1;
    /** Opened by Settings for the room direction only (extra "room"). */
    private boolean fromSettings;

    @Override
    protected void onCreate(android.os.Bundle state) {
        fromSettings = getIntent().getBooleanExtra("room", false);
        super.onCreate(state);
    }

    @Override
    void build() {
        newRoot();
        buildArch(500, 24, 16, Tokens.MIST_400, 0);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(displayBold, 64, Tokens.PLASTER_100);
        title.setText(t.dayTitle());
        dates = text(ui400, 26, Tokens.MIST_400);
        head.addView(title);
        head.addView(dates);
        lineHeight(title, 1.1f);
        ((LinearLayout.LayoutParams) dates.getLayoutParams()).topMargin += Ui.ipx(8);
        head.setClipChildren(false);
        place(head, Tokens.COURT_X, 56, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        homePill = homePill();

        for (int i = 0; i < ROWS; i++) row[i] = prayerRow(i);

        buildQibla();

        hints = new LinearLayout(this);
        hints.setOrientation(LinearLayout.HORIZONTAL);
        place(hints, Tokens.COURT_X, 976, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        setContentView(root);
        lastMinute = -1;
        render(true);
        row[day.nextIndex].requestFocus();
        // From Settings' room direction row: straight into the picker, and back to Settings when it closes.
        if (fromSettings) openPicker();
    }

    private FocusTile homePill() {
        FocusTile b = new FocusTile(this, 36);
        b.setRest(0, Tokens.LINE_SOFT, 2);
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(Ui.ipx(28), 0, Ui.ipx(28), 0);
        Icon back = new Icon(this, Icon.BACK, 2f);
        TextView label = text(ui400, 24, Tokens.PLASTER_100);
        label.setText(t.home());
        r.addView(back, new LinearLayout.LayoutParams(Ui.ipx(28), Ui.ipx(28)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMarginStart(Ui.ipx(12));
        r.addView(label, lp);
        b.addView(r, new FrameLayout.LayoutParams(-2, -1));
        b.setLook(f -> {
            int c = f ? Tokens.FOCUS_TEXT : Tokens.PLASTER_100;
            label.setTextColor(c);
            back.setColor(c);
        });
        b.setOnClickListener(v -> finish());
        b.setOnFocusChangeListener((v, has) -> { if (has) setHints(hints, java.util.Collections.singletonList(t.hintOk(t.home()))); });
        placeEnd(b, 1920 - (Tokens.COURT_X + Tokens.COURT_W), 56, ViewGroup.LayoutParams.WRAP_CONTENT, 72);
        return b;
    }

    private FocusTile prayerRow(int i) {
        FocusTile b = new FocusTile(this, 16);
        b.setFocusScale(1.04f);
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPaddingRelative(Ui.ipx(28), 0, Ui.ipx(20), 0);
        rowName[i] = text(ui400, 32, Tokens.PLASTER_100);
        rowStatus[i] = text(ui400, 22, Tokens.PLASTER_100);
        rowStatus[i].setAlpha(0.85f);
        rowTime[i] = text(display, 40, Tokens.PLASTER_100);
        rowTime[i].setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        rowTime[i].setTextDirection(rtl ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR);
        rowTime[i].setMinWidth(Ui.ipx(120));
        rowTime[i].setSingleLine(true);
        bell[i] = new FrameLayout(this);
        bellIcon[i] = new Icon(this, Icon.BELL, 2f);
        bell[i].addView(bellIcon[i], new FrameLayout.LayoutParams(Ui.ipx(26), Ui.ipx(26), Gravity.CENTER));

        r.addView(rowName[i], new LinearLayout.LayoutParams(0, -2, 1));
        r.addView(rowStatus[i], gap(new LinearLayout.LayoutParams(-2, -2)));
        r.addView(rowTime[i], gap(new LinearLayout.LayoutParams(-2, -2)));
        r.addView(bell[i], gap(new LinearLayout.LayoutParams(Ui.ipx(56), Ui.ipx(56))));
        b.addView(r, new FrameLayout.LayoutParams(-1, -1));
        b.setLook(f -> paintRow(i));
        b.setOnClickListener(v -> {
            String key = "bell_" + i;
            prefs().edit().putBoolean(key, !bellOn(i)).apply();
            paintRow(i);
        });
        b.setOnFocusChangeListener((v, has) -> {
            if (has) {
                lastRow = i;
                List<String> h = new ArrayList<>();
                h.add(t.hintOk(t.bellToggle()));
                h.add(t.hintQibla());
                h.add(t.hintBackHome());
                setHints(hints, h);
            }
        });
        placeTile(b, Tokens.COURT_X, ROW_TOP + i * (ROW_H + ROW_GAP), 620, ROW_H);
        return b;
    }

    private LinearLayout.LayoutParams gap(LinearLayout.LayoutParams lp) {
        lp.setMarginStart(Ui.ipx(20));
        return lp;
    }

    private void buildQibla() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setPadding(Ui.ipx(32), Ui.ipx(28), Ui.ipx(32), Ui.ipx(28));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Tokens.LAPIS_800);
        bg.setCornerRadius(Ui.px(24));
        panel.setBackground(bg);

        TextView label = caps(text(ui400, 22, Tokens.MIST_400));
        label.setText(t.qibla());
        // 260 rather than the design's 320, to make room for the room-direction button below.
        dial = new QiblaView(this);
        northLabel = text(ui400, 20, Tokens.MIST_400);
        degrees = text(display, 64, Tokens.GOLD_400);
        degrees.setTextDirection(View.TEXT_DIRECTION_LTR);
        direction = text(ui400, 24, Tokens.PLASTER_100);
        direction.setGravity(Gravity.CENTER_HORIZONTAL);
        qiblaHint = text(ui400, 20, Tokens.MIST_400);
        qiblaHint.setGravity(Gravity.CENTER_HORIZONTAL);

        panel.addView(label, new LinearLayout.LayoutParams(-1, -2));
        for (View v : new View[] {dial, northLabel, degrees, direction, qiblaHint}) {
            LinearLayout.LayoutParams lp = v == dial
                ? new LinearLayout.LayoutParams(Ui.ipx(260), Ui.ipx(260))
                : new LinearLayout.LayoutParams(-2, -2);
            lp.topMargin = Ui.ipx(v == northLabel ? 8 : 14);
            panel.addView(v, lp);
        }
        // CSS line boxes for the single lines; the hint may wrap to two, so its spacing is set directly.
        lineHeight(label, 1.3f);
        lineHeight(northLabel, 1.3f);
        lineHeight(degrees, 1f);
        lineHeight(direction, 1.3f);
        qiblaHint.setIncludeFontPadding(false);
        qiblaHint.setLineHeight(Ui.ipx(20 * 1.4f));
        panel.setClipChildren(false);
        place(panel, 1424, 236, 400, 698);

        roomButton = new FocusTile(this, 32);
        roomButton.setRest(Tokens.LAPIS_700, 0, 0);
        roomLabel = text(ui400, 22, Tokens.PLASTER_100);
        roomLabel.setGravity(Gravity.CENTER);
        roomButton.addView(roomLabel, new FrameLayout.LayoutParams(-1, -1));
        roomButton.setLook(f -> roomLabel.setTextColor(f ? Tokens.FOCUS_TEXT : Tokens.PLASTER_100));
        roomButton.setOnClickListener(v -> openPicker());
        roomButton.setOnFocusChangeListener((v, has) -> {
            if (has) {
                List<String> h = new ArrayList<>();
                h.add(t.hintOk(roomLabel.getText().toString()));
                h.add(t.hintTimes());
                setHints(hints, h);
            }
        });
        placeTile(roomButton, 1424 + 32, 236 + 698 - 28 - 64, 400 - 64, 64);
    }

    /** The picker: a dimmed screen with the tip, the value and a preview dial. Keys are handled in dispatchKeyEvent. */
    private void openPicker() {
        int f = facing();
        pickerFacing = f < 0 ? 0 : f;
        pickerPressed = false;
        picker = new FrameLayout(this);
        picker.setBackgroundColor(Tokens.SCRIM_MENU);
        picker.setClickable(true);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.ipx(56), Ui.ipx(48), Ui.ipx(56), Ui.ipx(48));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Tokens.LAPIS_800);
        bg.setCornerRadius(Ui.px(24));
        bg.setStroke(Ui.ipx(2), Tokens.GOLD_400_60);
        card.setBackground(bg);

        TextView title = text(displayBold, 52, Tokens.GOLD_400);
        title.setText(t.roomTitle());
        TextView tip = text(ui400, 26, Tokens.PLASTER_100);
        tip.setText(t.roomTip());
        tip.setLineHeight(Ui.ipx(26 * 1.6f));

        LinearLayout body = new LinearLayout(this);
        body.setGravity(Gravity.CENTER_VERTICAL);
        pickerDial = new QiblaView(this);
        LinearLayout values = new LinearLayout(this);
        values.setOrientation(LinearLayout.VERTICAL);
        pickerValue = text(display, 64, Tokens.PLASTER_100);
        pickerWhere = text(ui400, 28, Tokens.GOLD_200);
        values.addView(pickerValue);
        values.addView(pickerWhere);
        body.addView(pickerDial, new LinearLayout.LayoutParams(Ui.ipx(240), Ui.ipx(240)));
        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(0, -2, 1);
        vlp.setMarginStart(Ui.ipx(48));
        body.addView(values, vlp);

        TextView keys = text(ui400, 22, Tokens.MIST_400);
        keys.setText(t.roomKeys());
        keys.setTextDirection(rtl ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR);

        card.addView(title);
        card.addView(tip, gapTop(20));
        card.addView(body, gapTop(32));
        card.addView(keys, gapTop(32));
        lineHeight(title, 1.2f);
        lineHeight(pickerValue, 1.1f);

        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(Ui.ipx(1100), -2, Gravity.CENTER);
        picker.addView(card, clp);
        root.addView(picker, new FrameLayout.LayoutParams(-1, -1));
        renderPicker();
    }

    private LinearLayout.LayoutParams gapTop(float px) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = Ui.ipx(px);
        return lp;
    }

    private void renderPicker() {
        double relative = ((day.qibla - pickerFacing) % 360 + 360) % 360;
        pickerDial.setNorth((360 - pickerFacing) % 360);
        pickerDial.setBearing(relative);
        pickerValue.setText(t.bearingLabel(pickerFacing));
        pickerWhere.setText(t.qiblaRelative(relative));
    }

    private void closePicker(boolean save) {
        if (save) setFacing(pickerFacing);
        if (fromSettings) {
            finish();
            return;
        }
        root.removeView(picker);
        picker = null;
        renderMinute(new Date());
        roomButton.requestFocus();
        // Still focused from before the picker, so the focus listener won't run: refresh the hint's wording here.
        List<String> h = new ArrayList<>();
        h.add(t.hintOk(roomLabel.getText().toString()));
        h.add(t.hintTimes());
        setHints(hints, h);
    }

    @Override
    void onTick() {
        render(false);
    }

    /** Every second: countdown and row statuses. Once a minute, or when a prayer time arrives: everything else. */
    private void render(boolean force) {
        Date now = new Date();
        long minute = now.getTime() / 60000;
        boolean arrived = day != null && secondsUntil(day.nextTime, now) == 0;
        if (force || arrived || minute != lastMinute || day == null) {
            lastMinute = minute;
            // At the moment a prayer arrives, look a second ahead so the next one takes over at once.
            day = new PrayerDay(this, arrived ? new Date(now.getTime() + 1000) : now);
            renderMinute(now);
        }
        renderCountdown(day, now, false);
        for (int i = 0; i < ROWS; i++) {
            String status;
            if (state[i] == 1) status = t.rowNow();
            else if (state[i] == 0) status = t.passed(i == PrayerDay.SUNRISE);
            else status = t.in(duration(secondsUntil(day.times[i], now), false));
            rowStatus[i].setText(status);
        }
    }

    private void renderMinute(Date now) {
        readClockFormat();
        // The Hijri date first, as on Home.
        dates.setText(hijri(day) + t.sep() + gregorian(now) + t.sep() + t.city());

        String[] names = t.prayerNames();
        if (day.friday) names[PrayerDay.DHUHR] = t.jumuah();
        archLabel.setText(day.friday && day.nextIndex == PrayerDay.DHUHR ? t.jumuahLabel() : t.nextPrayer());
        archName.setText(names[day.nextIndex]);
        archSub.setText(t.methodName(Prefs.method(this)));
        archSub2.setText(t.school(Prefs.madhab(this)));

        for (int i = 0; i < ROWS; i++) {
            state[i] = i == day.nowIndex ? 1 : (i != day.nextIndex && !day.times[i].after(now) ? 0 : 2);
            rowName[i].setText(names[i]);
            rowTime[i].setText(time(day.times[i], 0.55f));
            paintRow(i);
        }

        degrees.setText(t.d(Math.round(day.qibla) + "°"));
        int facing = facing();
        roomLabel.setText(t.roomButton(facing >= 0));
        if (facing < 0) {
            // Room direction unknown: north-up, as in the design.
            dial.setNorth(Double.NaN);
            dial.setBearing(day.qibla);
            northLabel.setText(t.north());
            direction.setText(t.qiblaDirection(day.qibla));
            qiblaHint.setText(t.qiblaHint());
        } else {
            // The top of the dial is the TV, so the needle points the way to turn from the sofa.
            double relative = ((day.qibla - facing) % 360 + 360) % 360;
            dial.setNorth((360 - facing) % 360);
            dial.setBearing(relative);
            northLabel.setText(t.tvMarker());
            direction.setText(t.qiblaRelative(relative));
            qiblaHint.setText(t.facingLine(facing));
        }
    }

    private boolean bellOn(int i) {
        return prefs().getBoolean("bell_" + i, i != PrayerDay.SUNRISE);
    }

    /**
     * design: now = plaster (current.fill), focused = gold (focus.fill), past = lapis-800 at 60% with mist text,
     * upcoming = lapis-800.
     */
    private void paintRow(int i) {
        if (rowName[i] == null || bell[i] == null) return;
        boolean focused = row[i] != null && row[i].isFocused();
        int s = state[i];
        int fill = s == 1 ? Tokens.CURRENT_FILL : (s == 0 ? Tokens.LAPIS_800_60 : Tokens.LAPIS_800);
        if (row[i] != null) row[i].setRest(fill, 0, 0);
        boolean dark = focused || s == 1;
        int ink = focused ? Tokens.FOCUS_TEXT : Tokens.CURRENT_TEXT;
        int fg = dark ? ink : (s == 0 ? Tokens.MIST_400 : Tokens.PLASTER_100);
        for (TextView v : new TextView[] {rowName[i], rowStatus[i], rowTime[i]}) v.setTextColor(fg);
        rowName[i].setTypeface(dark ? ui600 : ui400);

        boolean on = bellOn(i);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        if (on) {
            g.setColor(dark ? Tokens.LAPIS_900 : Tokens.LAPIS_700);
            bellIcon[i].setColor(Tokens.GOLD_400);
            bell[i].setAlpha(1f);
        } else {
            g.setColor(0);
            g.setStroke(Ui.ipx(2), Tokens.BELL_OFF_BORDER);
            bellIcon[i].setColor(dark ? ink : Tokens.MIST_400);
            bell[i].setAlpha(0.7f);
        }
        bell[i].setBackground(g);
        bell[i].setContentDescription(t.bell(on));
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int k = e.getKeyCode();
        if (picker != null) return pickerKey(e);
        if (k == KeyEvent.KEYCODE_BACK) {
            if (e.getAction() == KeyEvent.ACTION_UP) finish();
            return true;
        }
        boolean nav = k == KeyEvent.KEYCODE_DPAD_UP || k == KeyEvent.KEYCODE_DPAD_DOWN
            || k == KeyEvent.KEYCODE_DPAD_LEFT || k == KeyEvent.KEYCODE_DPAD_RIGHT;
        if (!nav) return super.dispatchKeyEvent(e);
        if (e.getAction() != KeyEvent.ACTION_DOWN) return true;
        // Written for LTR, where the Qibla side is to the right; in Arabic Left/Right swap.
        if (rtl && k == KeyEvent.KEYCODE_DPAD_LEFT) k = KeyEvent.KEYCODE_DPAD_RIGHT;
        else if (rtl && k == KeyEvent.KEYCODE_DPAD_RIGHT) k = KeyEvent.KEYCODE_DPAD_LEFT;
        int from = current();
        int to;
        if (from < 0) to = day.nextIndex;
        else if (from == ROOM) to = k == KeyEvent.KEYCODE_DPAD_LEFT ? lastRow : (k == KeyEvent.KEYCODE_DPAD_UP ? HOME : -1);
        else if (k == KeyEvent.KEYCODE_DPAD_RIGHT) to = from == HOME ? -1 : ROOM;
        else if (k == KeyEvent.KEYCODE_DPAD_LEFT) to = -1;
        else if (from == HOME) to = k == KeyEvent.KEYCODE_DPAD_DOWN ? 0 : -1;
        else if (k == KeyEvent.KEYCODE_DPAD_UP) to = from == 0 ? HOME : from - 1;
        else to = Math.min(ROWS - 1, from + 1);
        if (to == HOME) homePill.requestFocus();
        else if (to == ROOM) roomButton.requestFocus();
        else if (to >= 0) row[to].requestFocus();
        return true;
    }

    /**
     * Picker keys: Right/Left turn by 5° (clockwise is always Right, as on a compass, in either language),
     * Up/Down by 45° to the next whole direction, OK saves, Back cancels.
     */
    private boolean pickerKey(KeyEvent e) {
        int k = e.getKeyCode();
        boolean up = e.getAction() == KeyEvent.ACTION_UP;
        boolean ok = k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER || k == KeyEvent.KEYCODE_NUMPAD_ENTER;
        if (k == KeyEvent.KEYCODE_BACK || ok) {
            if (!up) pickerPressed = true;
            else if (pickerPressed) closePicker(ok);
            return true;
        }
        if (e.getAction() == KeyEvent.ACTION_DOWN) {
            int v = pickerFacing;
            if (k == KeyEvent.KEYCODE_DPAD_RIGHT) v += 5;
            else if (k == KeyEvent.KEYCODE_DPAD_LEFT) v -= 5;
            else if (k == KeyEvent.KEYCODE_DPAD_UP) v = (v / 45 + 1) * 45;
            else if (k == KeyEvent.KEYCODE_DPAD_DOWN) v = ((v + 44) / 45 - 1) * 45;
            pickerFacing = ((v % 360) + 360) % 360;
            renderPicker();
        }
        return true;
    }

    private int current() {
        View f = getCurrentFocus();
        if (f == homePill) return HOME;
        if (f == roomButton) return ROOM;
        for (int i = 0; i < ROWS; i++) if (row[i] == f) return i;
        return -1;
    }
}
