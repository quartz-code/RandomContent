import sys, json, numpy as np, librosa
from scipy.signal import butter, sosfilt
y, sr = librosa.load(sys.argv[1], sr=22050, mono=True)
fps = 30; hop = sr // fps
S = np.abs(librosa.stft(y, n_fft=2048, hop_length=hop))
f = librosa.fft_frequencies(sr=sr, n_fft=2048)
rms = librosa.feature.rms(y=y, frame_length=2048, hop_length=hop)[0]
bass = S[(f > 30) & (f < 160)].sum(0); hi = S[(f > 2000) & (f < 8000)].sum(0)
onset = librosa.onset.onset_strength(y=y, sr=sr, hop_length=hop)
def norm(a):
    a = np.asarray(a, float); lo, h = np.percentile(a, 2), np.percentile(a, 99.5)
    return np.clip((a - lo) / (h - lo + 1e-9), 0, 1)
# drum hits: onsets of the low band (kick) and the 1.5-5 kHz band (snare/hat), 512-hop for precision
def hits(lo, hi_, delta):
    z = sosfilt(butter(4, [lo, hi_], btype='band', fs=sr, output='sos'), y)
    o = librosa.onset.onset_strength(y=z, sr=sr, hop_length=256)
    pk = librosa.util.peak_pick(o, pre_max=6, post_max=6, pre_avg=20, post_avg=20, delta=delta * o.max(), wait=8)
    return [round(float(t), 3) for t in librosa.frames_to_time(pk, sr=sr, hop_length=256)], o
kicks, ok = hits(40, 140, 0.18)
snares, os_ = hits(1500, 5000, 0.22)
out = {k: [round(float(v), 3) for v in norm(a)] for k, a in [('rms', rms), ('bass', bass), ('hi', hi), ('onset', onset)]}
out['fps'] = fps; out['kicks'] = kicks; out['snares'] = snares
open(sys.argv[2], 'w').write('// audio envelope of the re-cut song (rms / bass / highs / onsets at 30 fps) and detected drum hits\nwindow.AUDIO = ' + json.dumps(out, separators=(',', ':')) + ';\n')
print('kicks', kicks); print('snares', snares)
