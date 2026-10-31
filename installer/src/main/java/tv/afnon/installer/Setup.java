package tv.afnon.installer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Everything the installer takes from GitHub at run time, so a new Afnon never needs a new installer: the latest
 * release's APK, the setup steps that go with it (tools/setup-steps.txt at that release's tag, the same file
 * tools/install.sh runs), and the clean catalogue (tools/clean-catalog.tsv on main, refreshed from cleanyourtv).
 */
final class Setup {
    private Setup() {}

    static final String PACKAGE = "tv.afnon.home";
    private static final String REPO = "egyjs/afnon-android-tv-launcher";
    private static final String LATEST = "https://github.com/" + REPO + "/releases/latest";
    private static final String APK = LATEST + "/download/afnon.apk";
    private static final String RAW = "https://raw.githubusercontent.com/" + REPO + "/";

    /** One setup command (a line of tools/setup-steps.txt) and its check ("?|" line after it), if any. */
    static final class Step {
        final boolean required;
        final String en, ar, command;
        String check;
        Step(boolean required, String en, String ar, String command) {
            this.required = required;
            this.en = en;
            this.ar = ar;
            this.command = command;
        }
        String label(boolean arabic) { return arabic ? ar : en; }
    }

    /** tools/setup-steps.txt as of this installer, for when GitHub can't be reached or a release predates it. */
    private static final String FALLBACK_STEPS = ""
            + "1|Afnon is the home screen|أفنون هو الشاشة الرئيسية|O=/data/local/tmp/afnon_home_ok; C=/data/local/tmp/afnon-cleaned.txt; on() { pm list packages -e \"$1\" | grep -qx \"package:$1\"; }; front() { a=$(pidof tv.afnon.home) || return 1; sleep 3; dumpsys activity activities | grep -m1 -E 'topResumedActivity|mResumedActivity' | grep -q ' tv.afnon.home/' && [ \"$(pidof tv.afnon.home)\" = \"$a\" ]; }; home() { cmd package set-home-activity --user 0 tv.afnon.home/.MainActivity >/dev/null; for i in 1 2 3; do input keyevent KEYCODE_HOME; sleep 2; front && return 0; done; return 1; }; cmd role add-role-holder --user 0 android.app.role.HOME tv.afnon.home >/dev/null 2>&1; X=com.google.android.apps.tv.launcherx; T=com.google.android.tvlauncher; if on $X; then S=$X/$X.home.HomeActivity; P=\"com.google.android.tungsten.setupwraith $X\"; elif on $T; then S=$T/.MainActivity; P=$T; else P=; fi; H=$(cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -n 1); case \"$H\" in \"${P##* }\"/*) S=$H ;; esac; if [ -z \"$P\" ]; then home; else rm -f $O; nohup sh -c \"i=0; while [ \\$i -lt 300 ]; do [ -f $O ] && exit 0; sleep 1; i=\\$((i+1)); done; for p in $P; do pm enable \\$p; done; cmd package set-home-activity --user 0 $S; input keyevent KEYCODE_HOME\" >/dev/null 2>&1 & for p in $P; do on $p && pm disable-user --user 0 $p >/dev/null && { grep -qx $p $C 2>/dev/null || echo $p >> $C; }; done; if home; then touch $O; else for p in $P; do pm enable $p >/dev/null; done; cmd package set-home-activity --user 0 $S >/dev/null; input keyevent KEYCODE_HOME; touch $O; false; fi; fi\n"
            + "?|cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -n 1 | grep -q '^tv.afnon.home/' && ! pm list packages -e com.google.android.apps.tv.launcherx | grep -q .\n"
            + "1|Remote buttons and the call to prayer|أزرار جهاز التحكم والأذان|s=$(settings get secure enabled_accessibility_services); case \"$s\" in *tv.afnon.home/.RemoteKeys*) ;; ''|null) s=tv.afnon.home/.RemoteKeys ;; *) s=\"$s:tv.afnon.home/.RemoteKeys\" ;; esac; settings put secure enabled_accessibility_services \"$s\" && settings put secure accessibility_enabled 1\n"
            + "?|settings get secure enabled_accessibility_services | grep -q 'tv.afnon.home/.RemoteKeys' && [ \"$(settings get secure accessibility_enabled)\" = 1 ]\n"
            + "0|Afnon screensaver|شاشة توقّف أفنون|settings put secure screensaver_components tv.afnon.home/.AfnonDream\n"
            + "?|settings get secure screensaver_components | grep -q 'tv.afnon.home/.AfnonDream'\n"
            + "0|Starts by itself after a restart|يعمل وحده بعد إعادة التشغيل|appops set tv.afnon.home APP_AUTO_START allow 2>/dev/null; appops set tv.afnon.home APP_ASSOC_START allow 2>/dev/null; true\n"
            + "?|o=$(appops get tv.afnon.home APP_AUTO_START 2>&1); case \"$o\" in *allow*) true ;; *) false ;; esac\n"
            + "0|Quran Radio level bars and sleep timer|مؤشّر صوت إذاعة القرآن ومؤقّت النوم|pm grant tv.afnon.home android.permission.RECORD_AUDIO && pm grant tv.afnon.home android.permission.WRITE_SECURE_SETTINGS\n"
            + "?|d=$(dumpsys package tv.afnon.home); echo \"$d\" | grep -q 'RECORD_AUDIO: granted=true' && echo \"$d\" | grep -q 'WRITE_SECURE_SETTINGS: granted=true'\n"
            + "0|Wi-Fi name in Settings|اسم الواي فاي في الإعدادات|pm grant tv.afnon.home android.permission.ACCESS_FINE_LOCATION\n"
            + "?|dumpsys package tv.afnon.home | grep -q 'ACCESS_FINE_LOCATION: granted=true'\n";

    /** The setup steps for that release; never empty. */
    static List<Step> steps(String tag) {
        String text = tag == null ? null : fetch(RAW + tag + "/tools/setup-steps.txt");
        List<Step> steps = parseSteps(text);
        return steps.isEmpty() ? parseSteps(FALLBACK_STEPS) : steps;
    }

    static List<Step> parseSteps(String text) {
        List<Step> out = new ArrayList<>();
        if (text == null) return out;
        for (String line : text.split("\n")) {
            if (line.startsWith("#") || line.trim().isEmpty()) continue;
            if (line.startsWith("?|")) {
                if (!out.isEmpty()) out.get(out.size() - 1).check = line.substring(2);
                continue;
            }
            // The command may hold "|" itself: it is everything after the third one.
            String[] f = line.split("\\|", 4);
            if (f.length == 4 && !f[3].trim().isEmpty()) out.add(new Step("1".equals(f[0].trim()), f[1], f[2], f[3]));
        }
        return out;
    }

    /** One package the cleanup may offer (a line of tools/clean-catalog.tsv). */
    static final class Bloat {
        final String pkg, kind, note;
        final int tvs, kept;
        Bloat(String pkg, int tvs, int kept, String kind, String note) {
            this.pkg = pkg;
            this.tvs = tvs;
            this.kept = kept;
            this.kind = kind;
            this.note = note;
        }
        /** Ticked for the user: tracking, ads or store demo that several TVs turn off and none keeps. */
        boolean suggested() {
            return tvs >= 2 && kept == 0 && (kind.equals("tracking") || kind.equals("ads") || kind.equals("demo"));
        }
    }

    /** The clean catalogue; empty when GitHub can't be reached. */
    static List<Bloat> catalogue() {
        List<Bloat> out = new ArrayList<>();
        String text = fetch(RAW + "main/tools/clean-catalog.tsv");
        if (text == null) return out;
        for (String line : text.split("\n")) {
            if (line.startsWith("#") || line.trim().isEmpty()) continue;
            String[] f = line.split("\t", 5);
            if (f.length < 4) continue;
            try {
                out.add(new Bloat(f[0], Integer.parseInt(f[1]), Integer.parseInt(f[2]), f[3], f.length > 4 ? f[4] : ""));
            } catch (NumberFormatException ignored) {
                // A line from a future format; skip it.
            }
        }
        return out;
    }

    interface Progress { void on(long done, long total); }

    /** The latest release's tag ("v0.5"), from where releases/latest redirects; null if GitHub can't be reached. */
    static String latestTag() {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(LATEST).openConnection();
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

    /** Downloads the latest afnon.apk into {@code dir} and checks it is a whole APK (a zip). */
    static File download(File dir, Progress progress) throws IOException {
        File part = new File(dir, "afnon.apk.part");
        File apk = new File(dir, "afnon.apk");
        HttpURLConnection c = (HttpURLConnection) new URL(APK).openConnection();
        c.setConnectTimeout(20_000);
        c.setReadTimeout(30_000);
        try {
            if (c.getResponseCode() != 200) throw new IOException("GitHub answered " + c.getResponseCode());
            long total = c.getContentLengthLong();
            long done = 0;
            try (InputStream in = c.getInputStream(); OutputStream out = new FileOutputStream(part)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    done += n;
                    progress.on(done, total);
                }
            }
            if (total > 0 && done != total) throw new IOException("Download cut short");
        } finally {
            c.disconnect();
        }
        try (RandomAccessFile f = new RandomAccessFile(part, "r")) {
            if (f.length() < 1024 || f.readInt() != 0x504b0304) throw new IOException("Not an APK");
        }
        if (apk.exists() && !apk.delete()) throw new IOException("Can't replace the old download");
        if (!part.renameTo(apk)) throw new IOException("Can't save the download");
        return apk;
    }

    /** A small text file from GitHub, or null. */
    private static String fetch(String url) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(15_000);
            c.setReadTimeout(20_000);
            try {
                if (c.getResponseCode() != 200) return null;
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try (InputStream in = c.getInputStream()) {
                    byte[] buf = new byte[16 * 1024];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                }
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            } finally {
                c.disconnect();
            }
        } catch (IOException e) {
            return null;
        }
    }
}
