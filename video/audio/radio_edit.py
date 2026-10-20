"""Cuts the song into a new order for a radio version: each segment is a station's piece of the song.
usage: python3 radio_edit.py <song> <edit.json> <out.wav>
edit.json: {"segments": [[src_start, src_end], ...]} — played back to back with 25 ms crossfades
(the static burst drawn over each switch hides the joins).
"""
import sys, json, subprocess, numpy as np

song, edit, out = sys.argv[1], sys.argv[2], sys.argv[3]
SR = 44100
segs = json.load(open(edit))['segments']
raw = subprocess.run(['ffmpeg', '-v', 'error', '-i', song, '-f', 'f32le', '-ac', '2', '-ar', str(SR), '-'],
                     capture_output=True, check=True).stdout
x = np.frombuffer(raw, np.float32).reshape(-1, 2)
xf = int(0.025 * SR)
parts = []
for a, b in segs:
    p = x[int(a * SR):int(b * SR)].copy()
    ramp = np.linspace(0, 1, xf, dtype=np.float32)[:, None]
    p[:xf] *= ramp; p[-xf:] *= ramp[::-1]
    parts.append(p)
y = np.concatenate(parts)
subprocess.run(['ffmpeg', '-v', 'error', '-y', '-f', 'f32le', '-ac', '2', '-ar', str(SR), '-i', '-', out],
               input=y.astype(np.float32).tobytes(), check=True)
print(out, round(len(y) / SR, 3), 's')
