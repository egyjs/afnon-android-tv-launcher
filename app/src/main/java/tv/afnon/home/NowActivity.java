package tv.afnon.home;

import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Date;

/**
 * The call to prayer (design/Home.dc.html, mode "now"): over whatever was on, the arch, حي على الصلاة, the prayer
 * and its time, and the adhan, which starts by itself (Settings → Adhan). The focused button stops or plays it
 * again; Back or Dismiss closes, and so does leaving the screen. It closes by itself 1, 5 or 10 minutes after it
 * opens, or when the adhan ends (Settings → Reminders → Close the call screen; 10 minutes by default). It runs in
 * its own task, so closing returns to the app that was playing.
 */
public class NowActivity extends Screen {

    /** A muted TV (or one at 0) still calls the adhan, at this volume on the TV's own 0–100 scale. */
    private static final int MUTED_VOLUME = 13;

    private int prayer;
    private Date time;
    private FocusTile playButton, dismissButton;
    private TextView playLabel;
    private MediaPlayer player;
    private AudioFocusRequest focus;
    /** The volume to put back when the adhan ends, or -1 if the TV wasn't muted and nothing was changed. */
    private int restoreVolume = -1;
    private boolean restoreMute;
    private boolean started;

    private final Runnable autoClose = this::finish;

    @Override
    void build() {
        prayer = getIntent().getIntExtra("prayer", PrayerDay.DHUHR);
        time = new Date(getIntent().getLongExtra("time", System.currentTimeMillis()));
        readClockFormat();
        newRoot();
        root.setBackgroundColor(Tokens.SCRIM);

        ArchView frame = new ArchView(this);
        frame.setState(ArchView.APPROACHING);
        place(frame, 600, 110, 720, 900);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setClipChildren(false);

        TextView verse = text(sacred, 54, Tokens.GOLD_400);
        verse.setText("حَيَّ عَلَى الصَّلَاةِ");
        verse.setTextDirection(View.TEXT_DIRECTION_RTL);
        TextView label = caps(text(ui400, 22, Tokens.MIST_400));
        label.setText(t.nowLabel());
        String[] names = t.prayerNames();
        if (prayer == PrayerDay.DHUHR && new PrayerDay(this, time).friday) names[PrayerDay.DHUHR] = t.jumuah();
        TextView name = text(displayBold, 128, Tokens.PLASTER_100);
        name.setText(names[prayer]);
        TextView when = text(numbers, 64, Tokens.GOLD_400);
        when.setText(displayTime(time, 0.5f));
        when.setTextDirection(rtl ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR);
        for (TextView v : new TextView[] {verse, label, name, when}) {
            v.setGravity(Gravity.CENTER_HORIZONTAL);
            col.addView(v, new LinearLayout.LayoutParams(-2, -2));
        }
        lineHeight(verse, 1.4f);
        ((LinearLayout.LayoutParams) label.getLayoutParams()).topMargin = Ui.ipx(32);
        lineHeight(name, 1.1f);
        lineHeight(when, 1.2f);

        playButton = pill(Tokens.GOLD_400, 0);
        playLabel = (TextView) playButton.getChildAt(0);
        playButton.setOnClickListener(v -> {
            if (player != null) stopAdhan();
            else startAdhan();
            // "When the adhan ends": stopped by hand it ends now, so the silent close takes over; played again,
            // its end closes the screen.
            if (Prefs.callCloseMinutes(this) == 0) scheduleClose();
        });
        dismissButton = pill(0, 2);
        ((TextView) dismissButton.getChildAt(0)).setText(t.dismiss());
        dismissButton.setOnClickListener(v -> finish());
        float r = FocusTile.REACH;
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(Ui.ipx(420 + 2 * r), Ui.ipx(76 + 2 * r));
        plp.topMargin = Ui.ipx(36 - r);
        col.addView(playButton, plp);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(Ui.ipx(420 + 2 * r), Ui.ipx(76 + 2 * r));
        dlp.topMargin = Ui.ipx(18 - 2 * r);
        col.addView(dismissButton, dlp);

        TextView hint = text(ui400, 22, Tokens.MIST_400);
        hint.setText(t.nowHint(Prefs.callCloseMinutes(this)));
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(-2, -2);
        hlp.topMargin = Ui.ipx(20 - r);
        col.addView(hint, hlp);

        place(col, 640, 230, 640, ViewGroup.LayoutParams.WRAP_CONTENT);
        setContentView(root);
        updatePlayLabel();
        playButton.requestFocus();

        if (!started) {
            started = true;
            if (Prefs.adhan(this)) startAdhan();
            scheduleClose();
        }
    }

    /**
     * The auto-close from Settings: so many minutes after the screen opened, or as the adhan ends (the player's
     * completion closes it); with no adhan playing then, after {@link Tokens#MOTION_CALL_SILENT_DISMISS_MS}.
     */
    private void scheduleClose() {
        tick.removeCallbacks(autoClose);
        int minutes = Prefs.callCloseMinutes(this);
        if (minutes > 0) tick.postDelayed(autoClose, minutes * 60_000L);
        else if (player == null) tick.postDelayed(autoClose, Tokens.MOTION_CALL_SILENT_DISMISS_MS);
    }

    /** A 420×76 pill: gold when focused; at rest the given fill, or a soft outline. */
    private FocusTile pill(int restFill, float border) {
        FocusTile b = new FocusTile(this, 38);
        b.setRest(restFill == 0 ? 0 : Tokens.LAPIS_800, Tokens.LINE_SOFT, border);
        TextView v = text(ui500, 28, Tokens.PLASTER_100);
        v.setGravity(Gravity.CENTER);
        b.addView(v, new FrameLayout.LayoutParams(-1, -1));
        b.setLook(f -> v.setTextColor(f ? Tokens.FOCUS_TEXT : Tokens.PLASTER_100));
        return b;
    }

    /** See-through over what was playing, by day or night: its dimmed ground is the scrim token. */
    @Override
    int nightTheme() {
        return 0;
    }

    @Override
    void onTick() {
        // Nothing counts down here; the auto-close is its own timer.
    }

    // ---------------------------------------------------------------- adhan

    private void startAdhan() {
        stopAdhan();
        AudioManager am = getSystemService(AudioManager.class);
        // Takes the sound from the video underneath, which pauses or ducks until the call is closed.
        focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
            .build();
        if (am != null) {
            am.requestAudioFocus(focus);
            unmuteForAdhan(am);
        }
        player = Adhans.play(this, Prefs.voice(this, prayer == PrayerDay.FAJR));
        if (player != null) player.setOnCompletionListener(mp -> {
            stopAdhan();
            if (Prefs.callCloseMinutes(this) == 0) finish();
        });
        updatePlayLabel();
    }

    private void stopAdhan() {
        Adhans.stop(player);
        player = null;
        AudioManager am = getSystemService(AudioManager.class);
        if (am != null && focus != null) am.abandonAudioFocusRequest(focus);
        focus = null;
        if (am != null) restoreAfterAdhan(am);
        updatePlayLabel();
    }

    /** Muted or at 0: unmute and set {@link #MUTED_VOLUME}, remembering how it was. Otherwise leave the volume alone. */
    private void unmuteForAdhan(AudioManager am) {
        int stream = AudioManager.STREAM_MUSIC;
        boolean muted = am.isStreamMute(stream);
        int volume = am.getStreamVolume(stream);
        if (!muted && volume > 0) return;
        restoreVolume = volume;
        restoreMute = muted;
        if (muted) am.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, 0);
        am.setStreamVolume(stream, Math.min(MUTED_VOLUME, am.getStreamMaxVolume(stream)), 0);
    }

    /** The adhan ended, was stopped or the screen closed: back to the volume and mute it had. */
    private void restoreAfterAdhan(AudioManager am) {
        if (restoreVolume < 0) return;
        int stream = AudioManager.STREAM_MUSIC;
        am.setStreamVolume(stream, restoreVolume, 0);
        if (restoreMute) am.adjustStreamVolume(stream, AudioManager.ADJUST_MUTE, 0);
        restoreVolume = -1;
    }

    private void updatePlayLabel() {
        if (playLabel != null) playLabel.setText(player != null ? t.stop() : t.play());
    }

    // ---------------------------------------------------------------- keys and lifetime

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.KEYCODE_BACK) {
            if (e.getAction() == KeyEvent.ACTION_UP) finish();
            return true;
        }
        if (k == KeyEvent.KEYCODE_DPAD_UP || k == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (e.getAction() == KeyEvent.ACTION_DOWN) (k == KeyEvent.KEYCODE_DPAD_UP ? playButton : dismissButton).requestFocus();
            return true;
        }
        if (k == KeyEvent.KEYCODE_DPAD_LEFT || k == KeyEvent.KEYCODE_DPAD_RIGHT) return true;
        return super.dispatchKeyEvent(e);
    }

    /** Leaving the screen (Home, another app) ends the call: no adhan playing behind something else. */
    @Override
    protected void onStop() {
        super.onStop();
        if (!isChangingConfigurations()) finish();
    }

    @Override
    protected void onDestroy() {
        tick.removeCallbacks(autoClose);
        stopAdhan();
        super.onDestroy();
    }
}
