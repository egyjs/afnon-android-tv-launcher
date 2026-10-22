package tv.afnon.home;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Listening with the screen off. On this TCL that is the TV's own Audio Only mode: TCL Settings shows a short
 * countdown, then the backlight goes off and the TV sleeps while the sound plays on; any key but volume brings
 * the picture back. Afnon can't switch the backlight itself (TCL keeps that to system apps), so it asks TCL
 * Settings to do it. Where that isn't available, this activity is the fallback: black, dimmed, any key returns.
 */
public class ScreenOffActivity extends Activity implements Radio.Listener {
    private static final long NOTE_MS = 6000;

    /** Screen off with the radio playing: TCL's Audio Only if there is one, else the black screen. */
    static void start(Context c) {
        if (!Radio.on()) Radio.start(c);
        if (audioOnly(c)) return;
        try {
            c.startActivity(new Intent(c, ScreenOffActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException ignored) {
            // Nothing to show it with; the radio still plays.
        }
    }

    /**
     * TCL Settings' panel service, Type "AudioOnly": the same countdown dialog its quick menu shows ("TV screen
     * will turn off in 10 seconds…", with Cancel), then Audio Only. False if this TV has no such service.
     * Google TV sets (55P7K) host it in com.android.tv.settings; the Android TV 11 C06_2K_GB set in
     * com.tcl.settings.
     */
    static boolean audioOnly(Context c) {
        for (String pkg : new String[] {"com.android.tv.settings", "com.tcl.settings"}) {
            try {
                ComponentName cn = c.startService(new Intent("com.tcl.settings.SHOW_WINDOW")
                    .setPackage(pkg)
                    .putExtra("Type", "AudioOnly"));
                if (cn != null) return true;
            } catch (RuntimeException ignored) {
                // Not in this package; try the next.
            }
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Ui.init(this);
        Text t = Screen.loadText(this);
        Tokens.use(Prefs.night(this));
        Typeface ui = getResources().getFont(R.font.alexandria_regular);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Tokens.SCREENSAVER_GROUND);
        root.setLayoutDirection(t.ar ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        RadioBars bars = new RadioBars(this, 5);
        bars.setColor(Tokens.SCREENSAVER_TEXT_DIM);
        bars.setLive(true);
        col.addView(bars, new LinearLayout.LayoutParams(Ui.ipx(56), Ui.ipx(48)));
        TextView note = new TextView(this);
        note.setTypeface(ui);
        note.setTextSize(TypedValue.COMPLEX_UNIT_PX, Ui.px(24));
        note.setTextColor(Tokens.SCREENSAVER_TEXT);
        note.setGravity(Gravity.CENTER);
        note.setText(t.station(Radio.station(this)) + "\n" + t.screenOffNote());
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(-2, -2);
        nlp.topMargin = Ui.ipx(20);
        col.addView(note, nlp);
        root.addView(col, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        setContentView(root);
        col.postDelayed(() -> col.animate().alpha(0f).setDuration(1500).start(), NOTE_MS);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        root.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;
        getWindow().setAttributes(lp);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Radio.listen(this);
        if (!Radio.on()) finish();
    }

    @Override
    protected void onPause() {
        Radio.unlisten(this);
        super.onPause();
    }

    @Override
    public void onRadio(int state) {
        if (state == Radio.OFF) finish();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.KEYCODE_VOLUME_UP || k == KeyEvent.KEYCODE_VOLUME_DOWN || k == KeyEvent.KEYCODE_VOLUME_MUTE) {
            return super.dispatchKeyEvent(e);
        }
        if (e.getAction() == KeyEvent.ACTION_UP) finish();
        return true;
    }

    /** Leaving (the call to prayer, Home) brings the picture back too. */
    @Override
    protected void onStop() {
        super.onStop();
        if (!isChangingConfigurations()) finish();
    }
}
