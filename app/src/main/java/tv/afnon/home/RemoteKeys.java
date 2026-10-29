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

    /**
     * Key name (TCL names resolve on this firmware only) or keycode -> the app it opens. The default; the phone
     * installer replaces it through {@link SetupProvider} (pref "keys": "KEY=package,KEY=package").
     */
    static final String[][] APP_KEYS = {
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
    /** The running service, so a new button table applies at once; null while the service is off. */
    private static RemoteKeys running;

    /**
     * Pref: false once the service was turned off in Settings; {@link #ensureOn} keeps it on otherwise. Unset counts
     * as on: setup grants WRITE_SECURE_SETTINGS and turns the service on together, and TVs set up before this pref
     * existed had lost the service too.
     */
    private static final String PREF_ON = "remote_on";

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Prefs.of(this).edit().putBoolean(PREF_ON, true).apply();
        running = this;
        applyKeys();
        // The radio may have left the TV's sleep timer off (a crash, a reboot mid-listen): put it back.
        Radio.restoreSleep(this);
        if (watch != null) watch.stop();
        watch = new PrayerWatch(this);
        watch.start();
    }

    /** The button table: the saved one, else {@link #APP_KEYS}. */
    static String[][] keys(android.content.Context c) {
        String saved = Prefs.of(c).getString("keys", null);
        if (saved == null) return APP_KEYS;
        java.util.List<String[]> out = new java.util.ArrayList<>();
        for (String pair : saved.split(",")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && eq < pair.length() - 1) out.add(new String[] {pair.substring(0, eq).trim(), pair.substring(eq + 1).trim()});
        }
        return out.toArray(new String[0][]);
    }

    /** A key's code from its name ("KEYCODE_TCL_NETFLIX" or "TCL_NETFLIX") or its number ("4159"); 0 if unknown. */
    static int code(String name) {
        if (name.matches("\\d+")) return Integer.parseInt(name);
        int code = KeyEvent.keyCodeFromString(name.startsWith("KEYCODE_") ? name : "KEYCODE_" + name);
        return Math.max(code, 0);
    }

    /** Re-reads the button table and hands it to the firmware again. Main thread. */
    static void reload() {
        if (running != null) running.applyKeys();
    }

    private void applyKeys() {
        appKeys.clear();
        for (String[] k : keys(this)) {
            int code = code(k[0]);
            if (code > 0) appKeys.put(code, k[1]);
        }
        boolean tcl = TclKeys.install(this, targets());
        filterKeys(!tcl && FirmwareKeys.shouldFilter(this));
    }

    /** Turned off in Settings (no longer listed there): stop {@link #ensureOn} from turning it back on. */
    @Override
    public boolean onUnbind(Intent intent) {
        if (!listed(this)) Prefs.of(this).edit().putBoolean(PREF_ON, false).apply();
        return super.onUnbind(intent);
    }

    /**
     * Android drops an accessibility service from the enabled list when its app is force-stopped, which TV
     * "cleaners" do (Xiaomi's TV Manager boost, for one), and the call to prayer then stays off until someone
     * turns it on in Settings. Found off on a Xiaomi MiTV-MOOQ0 and on the TCL 55P7K. Home calls this; it lists the
     * service again unless it was turned off in Settings. Needs the WRITE_SECURE_SETTINGS grant from setup.
     */
    static void ensureOn(android.content.Context c) {
        ensureOn(c.getApplicationContext(), true);
    }

    /** again: after listing a missing service, look once more a little later (it may be marked crashed meanwhile). */
    private static void ensureOn(android.content.Context c, boolean again) {
        if (!Prefs.of(c).getBoolean(PREF_ON, true)) return;
        android.content.ContentResolver cr = c.getContentResolver();
        boolean listed = listed(c);
        // Listed, yet Android runs no accessibility service: it was force-stopped while bound, and Android marked it
        // crashed. A crashed service isn't bound again until it leaves the list (seen on the TCL 55P7K), so it
        // leaves and comes back.
        boolean stuck = listed && Settings.Secure.getInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 0;
        if (listed && !stuck) return;
        if (c.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
        ComponentName me = new ComponentName(c, RemoteKeys.class);
        String others = withoutMe(Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES), me);
        String all = others.isEmpty() ? me.flattenToShortString() : others + ":" + me.flattenToShortString();
        try {
            if (!stuck) {
                // Listed again right after a force-stop, Android may still mark it crashed a moment later.
                list(cr, all);
                if (again) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> ensureOn(c, false), 3000);
                }
                return;
            }
            Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, others);
            // Android reads the list when it hears of a change; written back at once, it would only see the last.
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                try {
                    list(cr, all);
                } catch (SecurityException ignored) {
                    // The grant was revoked meanwhile.
                }
            }, 1000);
        } catch (SecurityException ignored) {
            // The grant was revoked between the check and the write.
        }
    }

    private static void list(android.content.ContentResolver cr, String services) {
        Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, services);
        Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 1);
    }

    /** The enabled-services list without this service ("" if nothing else is on). */
    private static String withoutMe(String services, ComponentName me) {
        StringBuilder out = new StringBuilder();
        if (services != null) {
            for (String entry : services.split(":")) {
                if (entry.isEmpty() || me.equals(ComponentName.unflattenFromString(entry))) continue;
                if (out.length() > 0) out.append(':');
                out.append(entry);
            }
        }
        return out.toString();
    }

    /** Whether the TV's enabled accessibility services include this one. */
    private static boolean listed(android.content.Context c) {
        String s = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (s == null) return false;
        ComponentName me = new ComponentName(c, RemoteKeys.class);
        for (String entry : s.split(":")) if (me.equals(ComponentName.unflattenFromString(entry))) return true;
        return false;
    }

    @Override
    public void onDestroy() {
        if (running == this) running = null;
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
