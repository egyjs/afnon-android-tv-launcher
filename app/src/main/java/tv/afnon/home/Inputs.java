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
 * input's passthrough channel in the TV's own player (com.tcl.tv on TCL). Its first row is the TV's channels: the
 * tuner's channel list, which also takes the player off an HDMI or AV input it was left on.
 */
final class Inputs {
    private Inputs() {}

    private static final String ACTION_VIEW_INPUTS = "com.android.tv.action.VIEW_INPUTS";
    private static final Pattern NUMBER = Pattern.compile("(\\d+)");

    static final class Source {
        final String id, name, badge;
        /** Whether something is plugged in; null where the TV doesn't say (it never reports a port as empty). */
        Boolean connected;
        /** The TV's input state (TvInputManager.INPUT_STATE_*), for the order. */
        final int state;
        /** The TV's own channels (its built-in tuner), named by the panel rather than by the TV. */
        final boolean tuner;
        final Intent open;

        Source(String id, String name, String badge, Boolean connected, int state, boolean tuner, Intent open) {
            this.id = id;
            this.name = name;
            this.badge = badge;
            this.connected = connected;
            this.state = state;
            this.tuner = tuner;
            this.open = open;
        }
    }

    /**
     * The TV's channels (its first tuner, opened on its channel list, so the player leaves an HDMI or AV input it
     * was left on), then the passthrough inputs a player can open: HDMI, then the rest, the ones with something
     * plugged in first, then by label. AV is left out: nothing uses it, and an empty input shows only "No signal"
     * (the Xiaomi MiTV lists it). Empty if there is nothing to switch to.
     */
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
        Source tuner = null;
        for (TvInputInfo info : infos) {
            if (info.isHidden(c)) continue;
            if (info.getType() == TvInputInfo.TYPE_TUNER) {
                // One row for the channels, however many tuners the TV has (the MiTV's two play the same list).
                if (tuner != null && tuner.id.compareTo(info.getId()) <= 0) continue;
                Intent watch = explicit(c, new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(TvContract.buildChannelsUriForInput(info.getId()), TvContract.Channels.CONTENT_TYPE));
                if (watch != null) tuner = new Source(info.getId(), String.valueOf(info.loadLabel(c)), "TV", null,
                    TvInputManager.INPUT_STATE_CONNECTED, true, watch);
                continue;
            }
            if (!info.isPassthroughInput() || info.getType() == TvInputInfo.TYPE_COMPOSITE) continue;
            Intent open = explicit(c, new Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(info.getId())));
            if (open == null) continue;
            CharSequence custom = info.loadCustomLabel(c), label = info.loadLabel(c);
            String name = String.valueOf(!TextUtils.isEmpty(custom) ? custom : label);
            int state = tim.getInputState(info.getId());
            boolean connected = state != TvInputManager.INPUT_STATE_DISCONNECTED;
            out.add(new Source(info.getId(), name, badge(info.getType(), String.valueOf(label)), connected, state, false, open));
        }
        // The 55P7K calls every port connected or standby, empty or not; a TV that never says "disconnected"
        // doesn't know, so the panel shows no state rather than a wrong one.
        boolean tracks = false;
        for (Source s : out) tracks |= !s.connected;
        if (!tracks) for (Source s : out) s.connected = null;
        // INPUT_STATE_CONNECTED (0) before standby (1) before disconnected (2): the MiTV calls only a port with
        // a device on it connected.
        Collections.sort(out, (a, b) -> {
            boolean ha = a.badge.startsWith("H"), hb = b.badge.startsWith("H");
            if (ha != hb) return ha ? -1 : 1;
            if (a.state != b.state) return Integer.compare(a.state, b.state);
            return a.name.compareToIgnoreCase(b.name);
        });
        if (tuner != null) out.add(0, tuner);
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

    /** Home's extra that opens Afnon's Inputs panel. */
    static final String EXTRA_PANEL = "inputs_panel";

    /**
     * What Source opens: the TV's own inputs screen, else Home with Afnon's panel where the TV lists sources (the
     * Xiaomi MiTV's firmware opens the stock launcher's panel, which is gone once setup disables it); null if neither.
     */
    static Intent source(Context c) {
        Intent i = screen(c);
        if (i != null || list(c).isEmpty()) return i;
        return new Intent(c, MainActivity.class).putExtra(EXTRA_PANEL, true);
    }

    /** The intent pinned to the activity that answers it now, or null if none does. */
    static Intent explicit(Context c, Intent i) {
        ResolveInfo r = c.getPackageManager().resolveActivity(i, 0);
        if (r == null || r.activityInfo == null) return null;
        ActivityInfo a = r.activityInfo;
        return i.setComponent(new ComponentName(a.packageName, a.name));
    }
}
