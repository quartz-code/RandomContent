#!/usr/bin/env bash
# Renders a version and adds its soundtrack.
#   video/make.sh <a|b|c> <song file> [--skip-render]
# Songs are not stored in the repo: pass your own copy of the track.
#   a — Кино «Спокойная ночь»,  b — Кино «Легенда»,  c — Молчат Дома «Судно (Борис Рыжий)»
set -euo pipefail
v=$1; song=$2
here=$(cd "$(dirname "$0")" && pwd); out=$here/out; mkdir -p "$out"

[[ "${3:-}" == "--skip-render" ]] || node "$here/render.mjs" --v "$v" --out "$out/$v-video.mp4"

case $v in
  a) "$here/audio/build.sh" "$song" "$out/a-video.mp4" "$out/konstantin-a-spokoynaya-noch.mp4" 129 121 87.2 ;;
  b) # first 100.5 s of the song, cross-faded into its quiet ending (206.5 s → end)
     ffmpeg -v error -y -i "$song" -filter_complex \
       "[0:a]atrim=0:100.5,asetpts=PTS-STARTPTS[x];[0:a]atrim=206.5,asetpts=PTS-STARTPTS[y];[x][y]acrossfade=d=2:c1=tri:c2=tri[a]" \
       -map "[a]" -c:a pcm_s16le "$out/b-bed.wav"
     "$here/audio/build.sh" "$out/b-bed.wav" "$out/b-video.mp4" "$out/konstantin-b-legenda.mp4" 133 130 91.75 ;;
  c) "$here/audio/build.sh" "$song" "$out/c-video.mp4" "$out/konstantin-c-sudno.mp4" 141.4 139 80.47 ;;
  *) echo "unknown version $v" >&2; exit 1 ;;
esac
