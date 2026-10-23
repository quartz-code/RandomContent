"""Accents of a song on its beat grid: for every 8th note, how hard the low (kick) and mid (snare/chord) bands hit.
Appends `window.AUDIO.acc = [[t, strength], ...]` (strength 0..1, only hits above the threshold) to an audio .js file.
usage: python3 accents.py <song.wav> <audio.js> <grid_t0> <beat_s> [threshold]"""
import sys, json, numpy as np, librosa
from scipy.signal import butter, sosfilt

wav, js, t0, beat = sys.argv[1], sys.argv[2], float(sys.argv[3]), float(sys.argv[4])
thr = float(sys.argv[5]) if len(sys.argv) > 5 else 0.6
y, sr = librosa.load(wav, sr=22050, mono=True); hop = 128
def band(lo, hi):
    z = sosfilt(butter(4, [lo, hi], btype='band', fs=sr, output='sos'), y)
    e = librosa.onset.onset_strength(y=z, sr=sr, hop_length=hop); return e / np.percentile(e, 99)
low, mid = band(35, 150), band(150, 2000)
fr = lambda t: int(t * sr / hop)
out = []
k = 0
while True:
    t = t0 + k * beat / 2; k += 1
    if t < 0: continue
    a, b = fr(t - 0.05), fr(t + 0.05)
    if b >= len(low): break
    s = min(1.0, max(low[a:b].max(), 0.85 * mid[a:b].max()))
    if s >= thr: out.append([round(t, 3), round(float(s), 2)])
open(js, 'a').write('window.AUDIO.acc = ' + json.dumps(out, separators=(',', ':')) + ';\n')
print(len(out), 'accents')
