#!/usr/bin/env bash
# Builds the soundtrack for a version and muxes it with the rendered (silent) video.
#   video/audio/build.sh <song> <video.mp4> <out.mp4> <length_s> <fade_out_start_s> [explosion_s]
# The song is trimmed from 0, faded out at the end; at the explosion the music
# ducks under a low boom and a tinnitus tone, then comes back.
set -euo pipefail
song=$1; video=$2; out=$3; len=$4; fo=$5; ex=${6:-}

fade_len=$(python3 -c "print(max(0.5, $len - $fo))")
if [[ -z "$ex" ]]; then
  ffmpeg -v error -y -i "$video" -i "$song" -filter_complex \
    "[1:a]atrim=0:$len,asetpts=PTS-STARTPTS,afade=t=out:st=$fo:d=$fade_len,alimiter=limit=0.95[a]" \
    -map 0:v -map "[a]" -c:v libx264 -preset medium -crf 25 -pix_fmt yuv420p -movflags +faststart -c:a aac -b:a 192k -shortest "$out"
else
  d=$(python3 -c "print(int($ex * 1000))")
  duck="if(lt(t,$ex),1,if(lt(t,$ex+2.4),0.08,if(lt(t,$ex+5.3),0.08+0.92*(t-$ex-2.4)/2.9,1)))"
  ffmpeg -v error -y -i "$video" -i "$song" -filter_complex "
    [1:a]atrim=0:$len,asetpts=PTS-STARTPTS,volume='$duck':eval=frame,afade=t=out:st=$fo:d=$fade_len[m];
    anoisesrc=d=3.2:c=brown:a=1,lowpass=f=140,lowpass=f=140,afade=t=in:d=0.01,afade=t=out:st=0.05:d=3,volume=7,
      aformat=channel_layouts=stereo,adelay=$d|$d[boom];
    sine=f=4100:d=5.5,volume=0.045,afade=t=in:d=0.08,afade=t=out:st=1.4:d=4,aformat=channel_layouts=stereo,
      adelay=$((d + 120))|$((d + 120))[ring];
    [m][boom][ring]amix=inputs=3:normalize=0:duration=first,alimiter=limit=0.95[a]" \
    -map 0:v -map "[a]" -c:v libx264 -preset medium -crf 25 -pix_fmt yuv420p -movflags +faststart -c:a aac -b:a 192k -shortest "$out"
fi
echo "$out"
