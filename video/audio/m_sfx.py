"""Sound for version M («Глазок»): the song plus the door — knocks, the chain, the key, the handle, the creak.
usage: python3 m_sfx.py <song> <out.wav> <length_s>"""
import sys, subprocess, numpy as np
from scipy.signal import butter, sosfilt

song, out, length = sys.argv[1], sys.argv[2], float(sys.argv[3])
SR = 44100; N = int(length * SR); rng = np.random.default_rng(5)
raw = subprocess.run(['ffmpeg', '-v', 'error', '-i', song, '-t', str(length), '-f', 'f32le', '-ac', '2', '-ar', str(SR), '-'], capture_output=True, check=True).stdout
x = np.frombuffer(raw, np.float32).reshape(-1, 2).copy(); x = np.vstack([x, np.zeros((max(0, N - len(x)), 2), np.float32)])[:N]
fx = np.zeros(N, np.float32)
def put(sig, t, g=1.0):
    i = int(t * SR); n = min(len(sig), N - i)
    if n > 0: fx[i:i + n] += sig[:n] * g
def lp(s, f): return sosfilt(butter(2, f, btype='low', fs=SR, output='sos'), s)
def bp(s, a, b): return sosfilt(butter(2, [a, b], btype='band', fs=SR, output='sos'), s)
tt = lambda d: np.arange(int(d * SR)) / SR
def knock(t, g=1.0):
    u = tt(0.25); s = np.sin(2 * np.pi * 95 * u) * np.exp(-u * 28) + lp(rng.standard_normal(len(u)), 1800) * np.exp(-u * 70) * 0.5
    put(s.astype(np.float32), t, g)
BEAT = 0.6896; BAR = BEAT * 4; bar = lambda n: 0.937 + BAR * n
for t in [1.0, 1.3, 1.6, 6.47, 6.77, 7.07]: knock(t, 0.9)
u = tt(1.2); put((bp(rng.standard_normal(len(u)), 800, 5000) * np.sin(np.pi * u / 1.2) ** 2 * 0.08).astype(np.float32), 17.5)   # the scarf
for k in range(14):                                                                                                               # the chain
    t0 = bar(29) + 0.4 + k * 0.1 + rng.random() * 0.04; u = tt(0.15); f = 2500 + rng.random() * 2500
    put((np.sin(2 * np.pi * f * u) * np.exp(-u * 45) * 0.12 + bp(rng.standard_normal(len(u)), 3000, 9000) * np.exp(-u * 80) * 0.1).astype(np.float32), t0)
for dt in (0.55, 1.15): u = tt(0.08); put((bp(rng.standard_normal(len(u)), 1500, 6000) * np.exp(-u * 90) * 0.45).astype(np.float32), bar(30) + dt)  # the key
u = tt(0.1); put((bp(rng.standard_normal(len(u)), 900, 4000) * np.exp(-u * 60) * 0.4).astype(np.float32), bar(31) + 0.7)                   # the handle
u = tt(1.5); f = 160 + 50 * np.sin(2 * np.pi * 3.1 * u) * np.exp(-u) - 30 * u; p = 2 * np.pi * np.cumsum(f) / SR                          # the creak
put((bp(((p / np.pi) % 2 - 1).astype(np.float32), 300, 2200) * np.sin(np.pi * u / 1.5) * 0.12).astype(np.float32), bar(32) + 0.05)
mix = x + np.stack([fx, fx], 1)
subprocess.run(['ffmpeg', '-v', 'error', '-y', '-f', 'f32le', '-ac', '2', '-ar', str(SR), '-i', '-', '-af', 'alimiter=limit=0.95:level=disabled', out],
               input=np.clip(mix, -2, 2).astype(np.float32).tobytes(), check=True)
print(out)
