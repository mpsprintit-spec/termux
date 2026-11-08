#!/data/data/com.termux/files/usr/bin/bash
# Install a NASA SDO 171 Å clip as the blue, offline background of RA Terminal.
set -euo pipefail
TARGET_DIR="$HOME/.ra-terminal"
mkdir -p "$TARGET_DIR"
OUT="$TARGET_DIR/solar.mp4"
NASA_URL="https://svs.gsfc.nasa.gov/vis/a000000/a004200/a004246/Oct2014BigSpot_171A.1Kx1K.webm"

if ! command -v ffmpeg >/dev/null 2>&1; then
  echo "ffmpeg belum dipasang. Jalankan: pkg install ffmpeg"
  exit 1
fi

INPUT="${1:-}"
TMP=""
if [[ -z "$INPUT" ]]; then
  if ! command -v curl >/dev/null 2>&1; then
    echo "curl belum dipasang. Jalankan: pkg install curl"
    exit 1
  fi
  TMP="$TARGET_DIR/nasa-original-download.webm"
  echo "Mengunduh rekaman resmi NASA SDO 171 Å..."
  curl -fL --retry 3 --connect-timeout 20 --max-time 300 "$NASA_URL" -o "$TMP"
  INPUT="$TMP"
fi
if [[ ! -f "$INPUT" ]]; then
  echo "Video tidak ditemukan: $INPUT"
  exit 1
fi
ENCODER="libx264"
if ! ffmpeg -hide_banner -encoders 2>/dev/null | grep -q ' libx264 '; then ENCODER="mpeg4"; fi
echo "Mengolah video ke biru elektrik, 720p, audio dimatikan..."
FILTER="fps=30,scale=720:720:flags=lanczos,format=gray,colorchannelmixer=rr=0.14:rg=0.14:rb=0.14:gr=0.27:gg=0.27:gb=0.27:br=0.42:bg=0.42:bb=0.42,format=yuv420p"
TEMP_OUT="$TARGET_DIR/solar-processing.mp4"
if [[ "$ENCODER" == "libx264" ]]; then
  ffmpeg -hide_banner -loglevel error -stats -y -i "$INPUT" -vf "$FILTER" -an -c:v libx264 -preset veryfast -crf 23 -pix_fmt yuv420p -movflags +faststart "$TEMP_OUT"
else
  ffmpeg -hide_banner -loglevel error -stats -y -i "$INPUT" -vf "$FILTER" -an -c:v mpeg4 -qscale:v 4 -pix_fmt yuv420p -movflags +faststart "$TEMP_OUT"
fi
test -s "$TEMP_OUT"
mv -f "$TEMP_OUT" "$OUT"
echo "Berhasil: $OUT"
echo "Buka ulang Ra Terminal. Video dijalankan di dalam aplikasi, bukan di browser."
echo "Sumber: NASA SVS 4246 (The Big Sunspot of 2014)"
if [[ -n "$TMP" ]]; then echo "Asli tetap tersimpan di $TMP"; fi
