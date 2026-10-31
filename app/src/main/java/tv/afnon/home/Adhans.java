package tv.afnon.home;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;

import java.io.IOException;

/**
 * The adhan recordings shipped in assets/adhan (from github.com/abodehq/Athan-MP3, no licence stated; for this TV
 * only). Which one plays is chosen in Settings, with a separate choice for Fajr.
 */
final class Adhans {
    private Adhans() {}

    /** id (assets/adhan/&lt;id&gt;.mp3), Arabic name, English name. */
    static final String[][] VOICES = {
        {"alafasy", "مشاري العفاسي", "Mishary Alafasy"},
        {"almalki", "حمدان المالكي", "Hamdan Al-Malki"},
        {"alarkani", "إبراهيم الأركاني", "Ibrahim Al-Arkani"},
        {"chebae_fajr", "مالك شيباي (أذان الفجر)", "Malek Chebae (Fajr)"},
        {"alhamathani", "ماجد الهمذاني", "Majed Al-Hamathani"},
        {"alzahrani", "منصور الزهراني", "Mansoor Az-Zahrani"},
        {"deghreri", "حمد الدغريري", "Hamad Al-Deghreri"},
        {"alqatami", "ناصر القطامي", "Nasser Al-Qatami"},
        {"khatba", "صهيب خطبا", "Suhaib Khatba"},
        {"menshawy", "محمد صديق المنشاوي", "Mohammad Al-Menshawy"},
        {"refaat", "محمد رفعت", "Mohammad Refaat"},
        {"abdulbasit", "عبد الباسط عبد الصمد", "Abdul Basit"},
        {"nuyne", "أحمد نعينع", "Ahmad Nuaina"},
        {"makkah", "الحرم المكي", "Makkah, Al-Haram"},
        // From meypod/al-azan's audio (github.com/meypod/audio_files); tools/fetch-adhans.sh downloads them too.
        {"nabawi", "المسجد النبوي", "Madinah, Al-Masjid an-Nabawi"},
        {"ghalwash", "راغب مصطفى غلوش", "Ragheb Mustafa Ghalwash"},
    };

    static int index(String id) {
        for (int i = 0; i < VOICES.length; i++) if (VOICES[i][0].equals(id)) return i;
        return 0;
    }

    static String name(Text t, String id) {
        String[] v = VOICES[index(id)];
        return t.ar ? v[1] : v[2];
    }

    /** Starts a recording at once; null if it can't be read. The caller releases it. */
    static MediaPlayer play(Context c, String id) {
        MediaPlayer mp = new MediaPlayer();
        try (AssetFileDescriptor fd = c.getAssets().openFd("adhan/" + id + ".mp3")) {
            mp.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build());
            mp.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            mp.prepare();
            mp.start();
            return mp;
        } catch (IOException | RuntimeException e) {
            mp.release();
            return null;
        }
    }

    static void stop(MediaPlayer mp) {
        if (mp == null) return;
        try {
            mp.stop();
        } catch (IllegalStateException ignored) {
            // Already finished.
        }
        mp.release();
    }
}
