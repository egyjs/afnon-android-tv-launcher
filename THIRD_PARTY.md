# Third-party material

The MIT licence in `LICENSE` covers Afnon's own code. These parts belong to others.

| What | Where | Licence |
|---|---|---|
| [adhan-java](https://github.com/batoulapps/adhan-java) by Batoul Apps | Gradle dependency | MIT |
| [Amiri](https://github.com/aliftype/amiri) by Khaled Hosny | `app/src/main/res/font/amiri_*.ttf` | SIL Open Font Licence 1.1 |
| [Alexandria](https://fonts.google.com/specimen/Alexandria) by Mohamed Gaber and Julieta Ulanovsky | `app/src/main/res/font/alexandria_*.ttf` | SIL Open Font Licence 1.1 |
| [Marcellus](https://fonts.google.com/specimen/Marcellus) by Astigmatic | `app/src/main/res/font/marcellus_regular.ttf` | SIL Open Font Licence 1.1 |
| Adhan recordings from [abodehq/Athan-MP3](https://github.com/abodehq/Athan-MP3) | Not in the source. `tools/fetch-adhans.sh` downloads them; release APKs include them | None stated. Credited here; any recording is removed on request from its rights holder. |
| Adhan recordings «المسجد النبوي» and «راغب مصطفى غلوش» from [meypod/audio_files](https://github.com/meypod/audio_files), as [meypod/al-azan](https://github.com/meypod/al-azan) uses them | Not in the source. `tools/fetch-adhans.sh` downloads them; release APKs include them | None stated. Credited here; any recording is removed on request from its rights holder. |
| Spoken reminder lines (`tools/announce-lines.txt`) voiced by Microsoft's ar-SA-HamedNeural voice through [edge-tts](https://github.com/rany2/edge-tts) | Not in the source. `tools/fetch-announce.sh` records them; release APKs include them | Microsoft's speech service terms. Replaced on request. |
| Quran radio stations: إذاعة القرآن الكريم in Cairo, Nablus, Qatar (two channels) and Jordan, and Shabab FM Quran (Nablus) | Played from each station's public stream, never stored | Belongs to each station. Afnon is not affiliated with any of them. |
| The Saudi Broadcasting Authority's radios as [Saudi Radio+](https://radioplus.sba.sa) streams them: إذاعة القرآن الكريم and إذاعة نداء الإسلام | Played from the public stream, never stored | Belongs to the Saudi Broadcasting Authority. Afnon is not affiliated with it. |
| Reciter channels from [mp3quran.net](https://mp3quran.net) (`backup.qurango.net`) | Played from the public stream, never stored | Belongs to mp3quran.net and the reciters. Afnon is not affiliated with it. |

The stream addresses are in `Radio.STATIONS`. A station or reciter is removed on request from its rights holder.

The fonts can be renamed or bundled under the OFL, but not sold on their own. The full text is at
https://openfontlicense.org.
