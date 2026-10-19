"""Radio sound mix for version E: the song through a valve radio.
usage: python3 radio_mix.py <song> <curves.json> <out.wav> <length_s> [fade_out_start_s]
Untuned: band-limited, slightly crushed music under hiss. Tuned / during a transmission: clean.
"""
import sys, json, subprocess, numpy as np
from scipy.signal import butter, sosfilt

song, curves, out, length = sys.argv[1], sys.argv[2], sys.argv[3], float(sys.argv[4])
fo = float(sys.argv[5]) if len(sys.argv) > 5 else length - 3
SR = 44100
raw = subprocess.run(['ffmpeg', '-v', 'error', '-i', song, '-t', str(length), '-f', 'f32le', '-ac', '2', '-ar', str(SR), '-'],
                     capture_output=True, check=True).stdout
x = np.frombuffer(raw, np.float32).reshape(-1, 2).copy()
n = int(length * SR)
if len(x) < n: x = np.vstack([x, np.zeros((n - len(x), 2), np.float32)])
x = x[:n]

c = json.load(open(curves)); dt = c['dt']; v = c['v']
tt = np.arange(len(v)) * dt; ts = np.arange(n) / SR
cur = lambda k: np.interp(ts, tt, [e[k] for e in v])
tuned, power, tx = cur('tuned'), cur('power'), cur('tx')
clean = np.clip(np.maximum(tuned, tx), 0, 1)
clean = np.convolve(clean, np.ones(2205) / 2205, mode='same')  # 50 ms smoothing

radio = sosfilt(butter(4, [380, 2600], btype='band', fs=SR, output='sos'), x, axis=0)
radio = np.tanh(radio * 2.2) / 2.2 * 1.6
rng = np.random.default_rng(1)
hiss = rng.standard_normal((n, 2)).astype(np.float32)
hiss = sosfilt(butter(2, [1500, 9000], btype='band', fs=SR, output='sos'), hiss, axis=0) * 0.07
crackle = (rng.random((n, 1)) < 0.0004) * rng.standard_normal((n, 1)) * 0.35
wobble = 0.75 + 0.25 * np.sin(2 * np.pi * 0.7 * ts)[:, None]  # fading in and out of the signal

g_clean = clean[:, None]
mix = x * g_clean + radio * (1 - g_clean) * wobble * 0.9 + (hiss + crackle) * (1 - 0.85 * g_clean)
mix *= power[:, None]  # valves warming up
fade = np.clip((length - ts) / max(0.5, length - fo), 0, 1)[:, None]
mix *= fade
mix = np.tanh(mix * 1.1) / 1.1
subprocess.run(['ffmpeg', '-v', 'error', '-y', '-f', 'f32le', '-ac', '2', '-ar', str(SR), '-i', '-', out],
               input=mix.astype(np.float32).tobytes(), check=True)
print(out)
