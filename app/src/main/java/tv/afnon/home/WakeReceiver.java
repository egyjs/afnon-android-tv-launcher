package tv.afnon.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * {@link Wake}'s alarm, and the moments its alarm must be armed again: boot, an update of Afnon, the clock or time
 * zone changing, and the exact-alarm permission coming back. Each returns at once while nothing is picked.
 */
public class WakeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        if (Wake.ACTION.equals(i.getAction())) Wake.fire(c, i);
        else Wake.schedule(c);
    }
}
