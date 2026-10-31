#!/usr/bin/env bash
# Downloads the adhan recordings into app/src/main/assets/adhan/. They come from
# https://github.com/abodehq/Athan-MP3, which has no licence, so they are not committed to this repo.
# Without them Afnon still works; the call screen just stays silent.
set -e
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEST="$ROOT/app/src/main/assets/adhan"
BASE="https://raw.githubusercontent.com/abodehq/Athan-MP3/master/Sounds"
mkdir -p "$DEST"
while IFS='|' read -r id name; do
  [ -f "$DEST/$id.mp3" ] && continue
  echo "  $id"
  curl -fsSL -o "$DEST/$id.mp3" "$BASE/$(printf '%s' "$name" | sed 's/ /%20/g')"
done <<'LIST'
alafasy|Athan Mishary Alafasi.mp3
almalki|Athan Hamdan Almalki.mp3
alarkani|Athan Ibrahim Al-Arkani.mp3
chebae_fajr|Athan Al-fajer - Malek chebae.mp3
alhamathani|Athan Majed Al-hamathani.mp3
alzahrani|Athan Mansoor Az-Zahrani.mp3
deghreri|Athan Hamad Deghreri.mp3
alqatami|Athan Nasser Alqatami.mp3
khatba|Athan Suhaib Khatba.mp3
menshawy|Athan Mohammad Almenshawy.mp3
refaat|Athan Mohammad Ref3at.mp3
abdulbasit|Athan Abed Albase6.mp3
nuyne|Athan Ahmad Nuyne3.mp3
makkah|Athan Makkah.mp3
LIST
# Two more that meypod/al-azan uses, from https://github.com/meypod/audio_files (no licence stated there either).
BASE2="https://raw.githubusercontent.com/meypod/audio_files/main/adhan"
for item in nabawi\|masjid_an_nabawi.mp3 ghalwash\|ragheb_mustafa_ghalwash.mp3; do
  id="${item%%|*}"
  [ -f "$DEST/$id.mp3" ] && continue
  echo "  $id"
  curl -fsSL -o "$DEST/$id.mp3" "$BASE2/${item#*|}"
done
echo "Adhan recordings are in $DEST"
