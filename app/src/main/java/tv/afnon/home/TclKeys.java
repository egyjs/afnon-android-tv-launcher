package tv.afnon.home;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Hands Afnon's remote buttons to TCL's own key remapping, so RemoteKeys need not filter keys. With key filtering
 * on, Android re-injects every key and injected keys never auto-repeat: holding Volume moved it one step.
 *
 * TCL's window manager (TclCustomShortCutKeyManager in tcl-service.jar) reads Settings.Global "CustomKeyList", a JSON
 * array of keycodes, and for each one "CustomKey_<code>": {"keycode", "behavior": [{"keyaction", "type", "intent",
 * "appnotinstall"}]}. keyaction 1 acts on press, 2 on long press, 3 on release; type 1 starts an activity, 2 a service,
 * 3 sends a broadcast. It runs before any app sees the key and swallows it.
 *
 * That code catches nothing, inside system_server, so mistakes restart the TV's whole UI:
 * - An intent with no package, or one whose activity is missing, throws. Every intent names its package (TCL checks
 *   it is installed first and otherwise runs appnotinstall), and appnotinstall is a broadcast, which cannot throw.
 * - Changing "CustomKey_<code>" while <code> is not in the list throws (an observer outlives the list entry). So
 *   codes are never dropped from the list, only emptied, and keys are written a moment after the list, once TCL
 *   has read it.
 *
 * Other entries in the list (TCL's own, another app's) are left alone. One we take over is saved under
 * "AfnonSavedKey_<code>" and put back when the button is no longer ours; tools/restore.sh does the same.
 */
final class TclKeys {

    private static final String TAG = "AfnonKeys";
    private static final String FEATURE = "com.tcl.software.tinput_shortcut_config";
    private static final String LIST = "CustomKeyList";
    private static final String KEY_PREFIX = "CustomKey_";
    private static final String MISSING = "tv.afnon.home.KEY_TARGET_MISSING";
    /** Where a firmware entry we replaced waits to be put back. */
    private static final String SAVED_PREFIX = "AfnonSavedKey_";
    /** Time for TCL's observer to reread the list before the keys change. */
    private static final long LIST_SETTLE_MS = 1000;

    private TclKeys() {
    }

    /**
     * Maps each keycode to its intent. Returns false when this TV has no TCL remapping or the settings can't be
     * written (no WRITE_SECURE_SETTINGS grant); the caller then filters keys itself.
     */
    static boolean install(Context c, Map<Integer, Intent> targets) {
        if (!c.getPackageManager().hasSystemFeature(FEATURE)) return false;
        ContentResolver r = c.getContentResolver();
        try {
            Set<String> codes = codes(Settings.Global.getString(r, LIST));
            JSONObject values = new JSONObject();
            for (Map.Entry<Integer, Intent> e : targets.entrySet()) {
                Intent i = e.getValue();
                if (i.getPackage() == null && i.getComponent() != null) i.setPackage(i.getComponent().getPackageName());
                if (i.getPackage() == null) continue;
                String code = String.valueOf(e.getKey());
                values.put(code, entry(e.getKey(), i).toString());
                String now = Settings.Global.getString(r, KEY_PREFIX + code);
                if (now != null && !ours(now) && Settings.Global.getString(r, SAVED_PREFIX + code) == null) {
                    Settings.Global.putString(r, SAVED_PREFIX + code, now);
                }
            }
            // Buttons no longer ours (their app was uninstalled) get back what they had before: the entry we
            // replaced, else empty, which hands them to the firmware. Entries we never wrote stay as they are.
            Set<String> restored = new HashSet<>();
            for (String code : codes) {
                if (values.has(code) || !ours(Settings.Global.getString(r, KEY_PREFIX + code))) continue;
                String saved = Settings.Global.getString(r, SAVED_PREFIX + code);
                values.put(code, saved != null ? saved : "");
                if (saved != null) restored.add(code);
            }
            Set<String> all = new HashSet<>(codes);
            for (Iterator<String> it = values.keys(); it.hasNext(); ) all.add(it.next());
            JSONArray list = new JSONArray();
            for (String code : all) list.put(code);
            if (!all.equals(codes)) Settings.Global.putString(r, LIST, list.toString());
            new Handler(Looper.getMainLooper()).postDelayed(() -> writeKeys(r, values, restored), LIST_SETTLE_MS);
            return true;
        } catch (SecurityException | JSONException e) {
            Log.w(TAG, "TCL key remapping unavailable", e);
            return false;
        }
    }

    private static void writeKeys(ContentResolver r, JSONObject values, Set<String> restored) {
        for (Iterator<String> it = values.keys(); it.hasNext(); ) {
            String code = it.next();
            String value = values.optString(code);
            try {
                if (!value.equals(Settings.Global.getString(r, KEY_PREFIX + code))) {
                    Settings.Global.putString(r, KEY_PREFIX + code, value);
                }
                if (restored.contains(code)) Settings.Global.putString(r, SAVED_PREFIX + code, null);
            } catch (SecurityException e) {
                Log.w(TAG, "could not write " + KEY_PREFIX + code, e);
            }
        }
    }

    /**
     * Whether an entry is Afnon's: it carries our appnotinstall broadcast. Empty counts too, since only Afnon
     * empties entries (and older versions emptied other apps' as well).
     */
    private static boolean ours(String value) {
        return value != null && (value.isEmpty() || value.contains(MISSING));
    }

    private static JSONObject entry(int code, Intent target) throws JSONException {
        JSONObject missing = new JSONObject()
                .put("type", 3)
                .put("intent", new Intent(MISSING).toUri(Intent.URI_INTENT_SCHEME));
        JSONObject behavior = new JSONObject()
                .put("keyaction", 1)
                .put("type", 1)
                .put("intent", target.toUri(Intent.URI_INTENT_SCHEME))
                .put("appnotinstall", missing);
        return new JSONObject().put("keycode", code).put("behavior", new JSONArray().put(behavior));
    }

    private static Set<String> codes(String json) {
        Set<String> out = new HashSet<>();
        if (json == null) return out;
        try {
            JSONArray a = new JSONArray(json);
            for (int i = 0; i < a.length(); i++) out.add(a.getString(i));
        } catch (JSONException ignored) {
            // Not ours or garbled: overwrite it.
        }
        return out;
    }
}
