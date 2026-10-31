package tv.afnon.home;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Home's Inputs button, for remotes without a Source button. It opens the TV's own inputs screen, as Source does
 * ({@link #screen}). Where the firmware has none, Home shows its own panel (design/Inputs.dc.html) of the sources
 * the TV Input Framework lists ({@link #list}), which works with or without the stock launcher; switching opens the
 * input's passthrough channel in the TV's own player (com.tcl.tv on TCL).
 */
final class Inputs {
    private Inputs() {}

    private static final String ACTION_VIEW_INPUTS = "com.android.tv.action.VIEW_INPUTS";
    private static final Pattern NUMBER = Pattern.compile("(\\d+)");

    static final class Source {
        final String id, name, badge;
        /** Whether something is plugged in; null where the TV doesn't say (it never reports a port as empty). */
        Boolean connected;
        final Intent open;

        Source(String id, String name, String badge, Boolean connected, Intent open) {
            this.id = id;
            this.name = name;
            this.badge = badge;
            this.connected = connected;
            this.open = open;
        }
    }

    /** The passthrough inputs a player can open, HDMI first, each group in label order; empty if there are none. */
    static List<Source> list(Context c) {
        List<Source> out = new ArrayList<>();
        TvInputManager tim = (TvInputManager) c.getSystemService(Context.TV_INPUT_SERVICE);
        if (tim == null) return out;
        List<TvInputInfo> infos;
        try {
            infos = tim.getTvInputList();
        } catch (RuntimeException e) {
            return out;
        }
        for (TvInputInfo info : infos) {
            // Tuners have channels rather than one picture to show; the TV's own screen covers them.
            if (!info.isPassthroughInput() || info.isHidden(c)) continue;
            Intent open = explicit(c, new Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(info.getId())));
            if (open == null) continue;
            CharSequence custom = info.loadCustomLabel(c), label = info.loadLabel(c);
            String name = String.valueOf(!TextUtils.isEmpty(custom) ? custom : label);
            boolean connected = tim.getInputState(info.getId()) != TvInputManager.INPUT_STATE_DISCONNECTED;
            out.add(new Source(info.getId(), name, badge(info.getType(), String.valueOf(label)), connected, open));
        }
        // The 55P7K calls every port connected or standby, empty or not; a TV that never says "disconnected"
        // doesn't know, so the panel shows no state rather than a wrong one.
        boolean tracks = false;
        for (Source s : out) tracks |= !s.connected;
        if (!tracks) for (Source s : out) s.connected = null;
        Collections.sort(out, (a, b) -> {
            boolean ha = a.badge.startsWith("H"), hb = b.badge.startsWith("H");
            return ha != hb ? (ha ? -1 : 1) : a.name.compareToIgnoreCase(b.name);
        });
        return out;
    }

    /** The short tag in the row's badge: H1 for HDMI 1, AV, DVI… */
    private static String badge(int type, String label) {
        switch (type) {
            case TvInputInfo.TYPE_HDMI: {
                Matcher m = NUMBER.matcher(label);
                return m.find() ? "H" + m.group(1) : "HDMI";
            }
            case TvInputInfo.TYPE_COMPOSITE: return "AV";
            case TvInputInfo.TYPE_SVIDEO: return "S";
            case TvInputInfo.TYPE_SCART: return "SCART";
            case TvInputInfo.TYPE_COMPONENT: return "YPbPr";
            case TvInputInfo.TYPE_VGA: return "VGA";
            case TvInputInfo.TYPE_DVI: return "DVI";
            case TvInputInfo.TYPE_DISPLAY_PORT: return "DP";
            default: return label.length() > 3 ? label.substring(0, 3) : label;
        }
    }

    /** The Android TV inputs screen, else the firmware's own source menu; null if neither exists. */
    static Intent screen(Context c) {
        Intent i = explicit(c, new Intent(ACTION_VIEW_INPUTS));
        return i != null ? i : FirmwareKeys.sourceMenu(c);
    }

    /** The intent pinned to the activity that answers it now, or null if none does. */
    static Intent explicit(Context c, Intent i) {
        ResolveInfo r = c.getPackageManager().resolveActivity(i, 0);
        if (r == null || r.activityInfo == null) return null;
        ActivityInfo a = r.activityInfo;
        return i.setComponent(new ComponentName(a.packageName, a.name));
    }
}
