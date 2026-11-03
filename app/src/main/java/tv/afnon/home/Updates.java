package tv.afnon.home;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Settings → Device → Afnon updates. Afnon talks to GitHub only when someone presses OK on that row: once to read
 * the latest release's tag (where releases/latest redirects, as the phone installer does), and again to download
 * that release's afnon.apk straight into an Android install session. On Android 12+ the update installs with no
 * dialog (UPDATE_PACKAGES_WITHOUT_USER_ACTION, as Afnon updates itself); older TVs show Android's confirm. Both need
 * the install appop (a setup step). Android itself refuses an APK with another signature or a lower versionCode.
 *
 * <p>One check or download at a time, on a plain thread; the state is kept here so Settings shows it again when it
 * comes back. Installing replaces the app, which ends this process; the TV then starts the new Home.
 */
final class Updates {
    private Updates() {}

    static final int IDLE = 0, CHECKING = 1, LATEST = 2, AVAILABLE = 3, DOWNLOADING = 4, INSTALLING = 5,
        FAILED = 6, NOT_ALLOWED = 7;
    /** Why the last step failed: GitHub unreachable, the download broke, or Android refused the install. */
    static final int OFFLINE = 0, DOWNLOAD = 1, REFUSED = 2;

    private static final String REPO = "egyjs/afnon-android-tv-launcher";
    private static final String LATEST_URL = "https://github.com/" + REPO + "/releases/latest";

    private static final Handler main = new Handler(Looper.getMainLooper());
    private static int state = IDLE, failure, percent;
    /** The latest release's version ("0.9"), once a check found it. */
    private static String latest;
    private static Runnable listener;

    static int state() { return state; }
    static int failure() { return failure; }
    static int percent() { return percent; }
    static String latest() { return latest; }

    /** Settings' redraw while it is in front; null when it isn't. Called on the main thread. */
    static void listen(Runnable r) { listener = r; }

    private static void set(int s) {
        main.post(() -> {
            state = s;
            if (listener != null) listener.run();
        });
    }

    /** This build's version ("0.8.1"). */
    static String current(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    /** OK on the row: what it does depends on where the last step left off. */
    static void press(Context c) {
        Context app = c.getApplicationContext();
        if (state == CHECKING || state == DOWNLOADING || state == INSTALLING) return;
        if (state == AVAILABLE || (state == FAILED && failure != OFFLINE && latest != null)) install(app);
        else check(app);
    }

    private static void check(Context c) {
        state = CHECKING;
        new Thread(() -> {
            String tag = latestTag();
            if (tag == null) {
                failure = OFFLINE;
                set(FAILED);
                return;
            }
            latest = tag.startsWith("v") ? tag.substring(1) : tag;
            set(newer(latest, current(c)) ? AVAILABLE : LATEST);
        }, "afnon-update-check").start();
    }

    /** Whether dotted version a ("0.10") is above b ("0.9.1"); unreadable parts count as 0. */
    static boolean newer(String a, String b) {
        String[] x = a.split("\\."), y = b.split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length ? number(x[i]) : 0, q = i < y.length ? number(y[i]) : 0;
            if (p != q) return p > q;
        }
        return false;
    }

    private static int number(String s) {
        try {
            return Integer.parseInt(s.replaceAll("\\D.*", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** The latest release's tag ("v0.9"), from where releases/latest redirects; null if GitHub can't be reached. */
    private static String latestTag() {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(LATEST_URL).openConnection();
            c.setInstanceFollowRedirects(false);
            c.setRequestMethod("HEAD");
            c.setConnectTimeout(15_000);
            c.setReadTimeout(15_000);
            String to = c.getHeaderField("Location");
            c.disconnect();
            if (to == null || !to.contains("/tag/")) return null;
            return to.substring(to.lastIndexOf('/') + 1);
        } catch (IOException e) {
            return null;
        }
    }

    private static void install(Context c) {
        // Without the install appop, Android would open a "not allowed" screen most TVs can't get past.
        if (!c.getPackageManager().canRequestPackageInstalls()) {
            state = NOT_ALLOWED;
            if (listener != null) listener.run();
            return;
        }
        state = DOWNLOADING;
        percent = 0;
        if (listener != null) listener.run();
        String tag = "v" + latest;
        new Thread(() -> {
            PackageInstaller pi = c.getPackageManager().getPackageInstaller();
            int id = -1;
            try {
                PackageInstaller.SessionParams p = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                p.setAppPackageName(c.getPackageName());
                p.setInstallReason(PackageManager.INSTALL_REASON_USER);
                if (Build.VERSION.SDK_INT >= 31) p.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
                id = pi.createSession(p);
                try (PackageInstaller.Session s = pi.openSession(id)) {
                    download(s, "https://github.com/" + REPO + "/releases/download/" + tag + "/afnon.apk");
                    set(INSTALLING);
                    int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
                    PendingIntent done = PendingIntent.getBroadcast(c, id, new Intent(c, Result.class), flags);
                    s.commit(done.getIntentSender());
                }
            } catch (IOException | RuntimeException e) {
                if (id >= 0) {
                    try {
                        pi.abandonSession(id);
                    } catch (RuntimeException ignored) {
                        // Already gone.
                    }
                }
                failure = DOWNLOAD;
                set(FAILED);
            }
        }, "afnon-update-download").start();
    }

    /** Streams the APK into the session, with the row's percentage. */
    private static void download(PackageInstaller.Session s, String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(20_000);
        c.setReadTimeout(30_000);
        try {
            if (c.getResponseCode() != 200) throw new IOException("GitHub answered " + c.getResponseCode());
            long total = c.getContentLengthLong(), done = 0;
            int shown = -1;
            try (InputStream in = c.getInputStream(); OutputStream out = s.openWrite("afnon.apk", 0, total)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    done += n;
                    int pct = total > 0 ? (int) (done * 100 / total) : 0;
                    if (pct != shown) {
                        shown = pct;
                        main.post(() -> {
                            percent = pct;
                            if (listener != null) listener.run();
                        });
                    }
                }
                s.fsync(out);
            }
            if (total > 0 && done != total) throw new IOException("Download cut short");
        } finally {
            c.disconnect();
        }
    }

    /** Android's answer to the install. On success the process is already being replaced. */
    public static final class Result extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent i) {
            int status = i.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
            switch (status) {
                case PackageInstaller.STATUS_PENDING_USER_ACTION: {
                    // Android 10/11, or a TV that wants confirming anyway: its install dialog.
                    Intent confirm = i.getParcelableExtra(Intent.EXTRA_INTENT);
                    if (confirm == null) break;
                    confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        c.startActivity(confirm);
                        return;
                    } catch (RuntimeException ignored) {
                        break;
                    }
                }
                case PackageInstaller.STATUS_SUCCESS:
                    return;
                case PackageInstaller.STATUS_FAILURE_ABORTED:
                    // Cancelled in Android's dialog: offer the same update again.
                    set(AVAILABLE);
                    return;
                default:
                    break;
            }
            failure = REFUSED;
            set(FAILED);
        }
    }
}
