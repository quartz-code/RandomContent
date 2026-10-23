"""Sound for version N («Эпитез»): the song plus the bathroom — the switch, drips, a heartbeat, the mirror cracking,
the towel and the light going off.  usage: python3 n_sfx.py <song> <out.wav> <length_s>"""
import sys, subprocess, numpy as np
from scipy.signal import butter, sosfilt

song, out, length = sys.argv[1], sys.argv[2], float(sys.argv[3])
SR = 44100; N = int(length * SR); rng = np.random.default_rng(9)
raw = subprocess.run(['ffmpeg', '-v', 'error', '-i', song, '-t', str(length), '-f', 'f32le', '-ac', '2', '-ar', str(SR), '-'], capture_output=True, check=True).stdout
x = np.frombuffer(raw, np.float32).reshape(-1, 2).copy(); x = np.vstack([x, np.zeros((max(0, N - len(x)), 2), np.float32)])[:N]
fx = np.zeros(N, np.float32)
def put(sig, t, g=1.0):
    i = int(t * SR); n = min(len(sig), N - i)
    if n > 0: fx[i:i + n] += sig[:n].astype(np.float32) * g
def bp(s, a, b): return sosfilt(butter(2, [a, b], btype='band', fs=SR, output='sos'), s)
def lp(s, f): return sosfilt(butter(2, f, btype='low', fs=SR, output='sos'), s)
tt = lambda d: np.arange(int(d * SR)) / SR
BEAT = 0.6896; BAR = BEAT * 4; bar = lambda n: 0.937 + BAR * n; STOP = 91.95
def click(t, g=0.5): u = tt(0.06); put(bp(rng.standard_normal(len(u)), 1200, 7000) * np.exp(-u * 120), t, g)
def drip(t, g=0.25): u = tt(0.3); f = 900 + 1400 * np.exp(-u * 30); put(np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-u * 18), t, g)
click(1.0)
for t in (6.47, 9.9, 13.4, 16.8): drip(t)
for k in range(5):                                                                                       # heartbeat before he looks
    for dt, g in ((0, 0.9), (0.26, 0.55)): u = tt(0.3); put(lp(np.sin(2 * np.pi * (48 + 20 * np.exp(-u * 30)) * u) * np.exp(-u * 14), 160), 20.3 + k * 0.62 + dt, g)
u = tt(1.2); put(bp(rng.standard_normal(len(u)), 2500, 11000) * np.exp(-u * 6) * 0.5, 23.0)                     # the mirror cracks
for k in range(18): u = tt(0.4); put(np.sin(2 * np.pi * (2500 + rng.random() * 4000) * u) * np.exp(-u * 25) * 0.08, 23.02 + rng.random() * 0.8)
u = tt(1.0); put(bp(rng.standard_normal(len(u)), 1500, 9000) * np.linspace(1, 0, len(u)) ** 2 * 0.2, bar(16) - 0.05)  # shards back (reversed tinkle)
u = tt(0.5); put(bp(rng.standard_normal(len(u)), 300, 3000) * np.sin(np.pi * u / 0.5) ** 2 * 0.35, STOP - 0.48)       # the towel
click(STOP + 0.05, 0.6)
mix = x + np.stack([fx, fx], 1)
subprocess.run(['ffmpeg', '-v', 'error', '-y', '-f', 'f32le', '-ac', '2', '-ar', str(SR), '-i', '-', '-af', 'alimiter=limit=0.95:level=disabled', out],
               input=np.clip(mix, -2, 2).astype(np.float32).tobytes(), check=True)
print(out)
