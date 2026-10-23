"""Sound for version O («Гипс»): the song plus the chat (message pops, a photo being sent) and the camera shutter.
usage: python3 o_sfx.py <song> <out.wav> <length_s>"""
import sys, subprocess, numpy as np
from scipy.signal import butter, sosfilt

song, out, length = sys.argv[1], sys.argv[2], float(sys.argv[3])
SR = 44100; N = int(length * SR); rng = np.random.default_rng(13)
raw = subprocess.run(['ffmpeg', '-v', 'error', '-i', song, '-t', str(length), '-f', 'f32le', '-ac', '2', '-ar', str(SR), '-'], capture_output=True, check=True).stdout
x = np.frombuffer(raw, np.float32).reshape(-1, 2).copy(); x = np.vstack([x, np.zeros((max(0, N - len(x)), 2), np.float32)])[:N]
fx = np.zeros(N, np.float32)
def put(sig, t, g=1.0):
    i = int(t * SR); n = min(len(sig), N - i)
    if n > 0: fx[i:i + n] += sig[:n].astype(np.float32) * g
def bp(s, a, b): return sosfilt(butter(2, [a, b], btype='band', fs=SR, output='sos'), s)
tt = lambda d: np.arange(int(d * SR)) / SR
BEAT = 0.6896; BAR = BEAT * 4; bar = lambda n: 0.937 + BAR * n; STOP = 91.95
def pop(t, f=880, g=0.18): u = tt(0.12); put(np.sin(2 * np.pi * (f + 500 * np.exp(-u * 40)) * u) * np.exp(-u * 30), t, g)
msgs = [(1.2, 'd'), (3.6, 'k'), (6.47, 'd'), (8.6, 'k'), (bar(4), 'd'), (bar(5), 'k'), (bar(6), 'd'), (bar(7), 'k'), (bar(16) + 0.1, 'd'), (STOP + 0.9, 'd'), (STOP + 2.6, 'k')]
for t, w in msgs: pop(t, 740 if w == 'd' else 980)
for t in (bar(7) + 1.4, bar(31)): u = tt(0.45); put(bp(rng.standard_normal(len(u)), 1500, 8000) * np.sin(np.pi * u / 0.45) ** 2 * 0.12, t)  # sending
for n in range(25, 31):                                                                                                              # the shutter on the flashes
    for b in (0, 2):
        t = bar(n) + b * BEAT; u = tt(0.09); put(bp(rng.standard_normal(len(u)), 2000, 9000) * np.exp(-u * 70) * 0.22, t); put(bp(rng.standard_normal(len(u)), 1200, 6000) * np.exp(-u * 90) * 0.15, t + 0.06)
mix = x + np.stack([fx, fx], 1)
subprocess.run(['ffmpeg', '-v', 'error', '-y', '-f', 'f32le', '-ac', '2', '-ar', str(SR), '-i', '-', '-af', 'alimiter=limit=0.95:level=disabled', out],
               input=np.clip(mix, -2, 2).astype(np.float32).tobytes(), check=True)
print(out)
