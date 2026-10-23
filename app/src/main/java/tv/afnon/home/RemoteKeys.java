package tv.afnon.home;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Remote buttons that only the stock launcher answered, from any app: the app buttons, Settings, Input and All
 * apps. Every other key passes through untouched. Taken over from the cleanyourtv launcher's InputKeyService and
 * HomeKeyService so that package can be disabled. Keys injected over ADB never reach this; test with the remote.
 * Being always on, it also hosts {@link PrayerWatch}: the reminders and the call to prayer.
 *
 * On TCL firmwares the buttons are handed to TCL's own key remapping ({@link TclKeys}) and this service stops
 * filtering keys: filtered keys lose their auto-repeat, so holding Volume moved it only one step. Firmwares that
 * answer the buttons themselves (CVTE's hotkey service, see {@link FirmwareKeys}) are left alone too. Elsewhere it
 * filters keys and opens the targets itself, as below; a button whose target is missing passes through.
 */
public class RemoteKeys extends AccessibilityService {

    /** Key name (TCL names resolve on this firmware only) or keycode -> the app it opens. */
    private static final String[][] APP_KEYS = {
        {"KEYCODE_BUTTON_3", "com.google.android.youtube.tv"},
        {"KEYCODE_TCL_YOUTUBE", "com.google.android.youtube.tv"},
        {"KEYCODE_TCL_NETFLIX", "net.mbc.shahidTV"},
        {"KEYCODE_INFO", "dev.ctv.tv"},
        // The Web button: TCL's own keycode with no Android name, sent over IR (scancode 651).
        {"4159", "com.tcl.browser"},
    };
    /** TCL remotes send Settings as 83 (KEYCODE_NOTIFICATION), which the stock home turned into its panel. */
    private static final int SETTINGS_KEY = KeyEvent.KEYCODE_NOTIFICATION;
    private static final String ACTION_VIEW_INPUTS = "com.android.tv.action.VIEW_INPUTS";

    private final Map<Integer, String> appKeys = new HashMap<>();
    /** The running watch, for Home's test hook; null while the service is off. */
    static PrayerWatch watch;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        appKeys.clear();
        for (String[] k : APP_KEYS) {
            int code = KeyEvent.keyCodeFromString(k[0]);
            if (code > 0) appKeys.put(code, k[1]);
        }
        boolean tcl = TclKeys.install(this, targets());
        filterKeys(!tcl && FirmwareKeys.shouldFilter(this));
        // The radio may have left the TV's sleep timer off (a crash, a reboot mid-listen): put it back.
        Radio.restoreSleep(this);
        if (watch != null) watch.stop();
        watch = new PrayerWatch(this);
        watch.start();
        // The alarm that turns the TV on for the adhan, if any prayer is picked: also armed here, in case the
        // firmware holds back BOOT_COMPLETED.
        Wake.schedule(this);
    }

    @Override
    public void onDestroy() {
        if (watch != null) watch.stop();
        watch = null;
        super.onDestroy();
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        String app = appKeys.get(code);
        Intent open;
        if (app != null) open = null;
        // Afnon Settings, which lists the TV's own under Device settings.
        else if (code == SETTINGS_KEY || code == KeyEvent.KEYCODE_SETTINGS) open = new Intent(this, SettingsActivity.class);
        else if (code == KeyEvent.KEYCODE_TV || code == KeyEvent.KEYCODE_TV_INPUT) open = inputs();
        else if (code == KeyEvent.KEYCODE_ALL_APPS) open = new Intent(this, AllAppsActivity.class);
        else return false;
        // Nothing here answers it (the app is gone, or the firmware has no Android TV inputs screen): let the key
        // through so the firmware's own handling (a CVTE source menu, say) still gets it.
        if (app != null ? Apps.launchIntent(this, app) == null : open == null || explicit(new Intent(open)) == null) return false;
        // Act on release only, and swallow the press too so the app underneath never sees half a key.
        if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
            if (app != null) {
                Apps.launch(this, app);
            } else {
                try {
                    startActivity(open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } catch (ActivityNotFoundException ignored) {
                    // No input switcher or settings on this build; the key just does nothing.
                }
            }
        }
        return true;
    }

    /** Every button this service answers, with what it opens; buttons whose target is missing are left out. */
    private Map<Integer, Intent> targets() {
        Map<Integer, Intent> out = new HashMap<>();
        for (Map.Entry<Integer, String> e : appKeys.entrySet()) {
            Intent i = Apps.launchIntent(this, e.getValue());
            if (i != null) out.put(e.getKey(), i);
        }
        Intent settings = new Intent(this, SettingsActivity.class);
        out.put(SETTINGS_KEY, settings);
        out.put(KeyEvent.KEYCODE_SETTINGS, settings);
        Intent inputs = inputs();
        if (inputs != null) {
            out.put(KeyEvent.KEYCODE_TV, inputs);
            out.put(KeyEvent.KEYCODE_TV_INPUT, inputs);
        }
        out.put(KeyEvent.KEYCODE_ALL_APPS, new Intent(this, AllAppsActivity.class));
        for (Intent i : out.values()) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        return out;
    }

    /** The Android TV inputs screen, else the firmware's own source menu; null if neither exists. */
    private Intent inputs() {
        Intent i = explicit(new Intent(ACTION_VIEW_INPUTS));
        return i != null ? i : FirmwareKeys.sourceMenu(this);
    }

    /** The intent pinned to the activity that answers it now, or null if none does. */
    private Intent explicit(Intent i) {
        ResolveInfo r = getPackageManager().resolveActivity(i, 0);
        if (r == null || r.activityInfo == null) return null;
        ActivityInfo a = r.activityInfo;
        return i.setComponent(new ComponentName(a.packageName, a.name));
    }

    private void filterKeys(boolean on) {
        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) return;
        int flag = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        info.flags = on ? info.flags | flag : info.flags & ~flag;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Redirects.onWindowChanged(this, event);
    }

    @Override
    public void onInterrupt() {
    }
}
