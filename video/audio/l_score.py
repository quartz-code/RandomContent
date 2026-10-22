"""Procedural dark score for version L («Кали»): drones, choir pads, impacts, a battle drum, a cycle ostinato and a
music box that warps into madness. Writes the score and video/versions/l-audio.js (envelope + cue times for the picture).
usage: python3 l_score.py <out.wav>"""
import sys, json, numpy as np
from scipy.signal import butter, sosfilt, fftconvolve

SR, END = 44100, 170.0
N = int(END * SR)
rng = np.random.default_rng(11)
dry = np.zeros((N, 2), np.float32); send = np.zeros((N, 2), np.float32)
ts = np.arange(N) / SR

# ---------------- cues (the picture reads these from l-audio.js) ----------------
C = {
    'end': END,
    'clang': [15.0, 17.0, 19.0, 21.0],                 # «Лечи.»
    'boom': [9.0, 32.0, 64.0, 79.0, 98.0, 166.0],      # sigil, the door, Alarus, the kill, the black dragon, the title
    'battle': [42.0 + i * 0.5 for i in range(20)],     # the drum, 120 bpm eighths… quarter grid
    'braam': [42.0, 45.0, 48.0, 51.0],
    'whoosh': [86.6, 87.4, 88.2],                      # three teleports
    'silence': [[78.5, 79.0], [97.4, 98.0], [141.7, 142.3]],
    'rewind': [112.0, 113.6],
    'resets': [124.0, 127.0, 129.4, 131.4, 132.9, 134.1, 135.1, 135.9, 136.6, 137.2, 137.7],
    'nightmare': [150.0, 152.0, 154.0, 156.0],
}

def put(sig, t0, gain=1.0, pan=0.0, wet=0.25):
    i = int(t0 * SR); n = min(len(sig), N - i)
    if n <= 0 or i < 0: return
    s = sig[:n] * gain
    l, r = np.sqrt(0.5 * (1 - pan)), np.sqrt(0.5 * (1 + pan))
    if s.ndim == 1: s = np.stack([s * l * 1.414, s * r * 1.414], 1)
    dry[i:i + n] += s * (1 - wet); send[i:i + n] += s * wet

def lp(x, f, o=2): return sosfilt(butter(o, f, btype='low', fs=SR, output='sos'), x, axis=0)
def hp(x, f, o=2): return sosfilt(butter(o, f, btype='high', fs=SR, output='sos'), x, axis=0)
def bp(x, a, b, o=2): return sosfilt(butter(o, [a, b], btype='band', fs=SR, output='sos'), x, axis=0)
def saw(f, n, ph=0.0):
    p = (ph + np.cumsum(np.broadcast_to(f, (n,)) / SR)) % 1.0
    return (2 * p - 1).astype(np.float32)
def env(n, a, r, sus=1.0):
    e = np.ones(n, np.float32) * sus; ai = max(1, int(a * SR)); ri = max(1, int(r * SR))
    e[:ai] = np.linspace(0, sus, ai); e[-ri:] *= np.linspace(1, 0, ri); return e
def fade(n, a, r):
    e = np.ones(n, np.float32); ai, ri = int(a * SR), int(r * SR)
    if ai: e[:ai] = np.linspace(0, 1, ai) ** 2
    if ri: e[-ri:] *= np.linspace(1, 0, ri) ** 2
    return e

D1, F1, A1, D2, E2, F2, G2, A2, Bb1, Bb2, C2, C3, D3, E3, F3, G3, A3, Bb3, C4, D4, E4, F4, G4, A4 = \
    36.71, 43.65, 55.0, 73.42, 82.41, 87.31, 98.0, 110.0, 58.27, 116.54, 65.41, 130.81, 146.83, 164.81, 174.61, 196.0, 220.0, 233.08, 261.63, 293.66, 329.63, 349.23, 392.0, 440.0

def drone(t0, t1, freqs, gain, cut=400, a=3, r=3, wet=0.4):
    n = int((t1 - t0) * SR); x = np.zeros(n, np.float32)
    for f in freqs:
        for dt in (-0.12, 0.0, 0.13): x += saw(f * (1 + dt / 100), n, rng.random())
    x = lp(x, cut, 4) * (0.8 + 0.2 * np.sin(2 * np.pi * 0.07 * np.arange(n) / SR + rng.random() * 6))
    put(x / (3 * len(freqs)) * fade(n, a, r), t0, gain, 0, wet)

FORM = {'a': [(700, 1.0), (1200, 0.5), (2600, 0.25)], 'o': [(450, 1.0), (800, 0.6), (2830, 0.15)], 'u': [(325, 1.0), (700, 0.4), (2530, 0.1)]}
def choir(t0, t1, freqs, gain, vowel='a', a=2.5, r=2.5, wet=0.6, pan=0.0):
    n = int((t1 - t0) * SR); src = np.zeros(n, np.float32); tt = np.arange(n) / SR
    for f in freqs:
        for k in range(3):
            vib = 1 + 0.004 * np.sin(2 * np.pi * (4.8 + k * 0.4) * tt + rng.random() * 6)
            src += saw(f * vib * (1 + (k - 1) * 0.0025), n, rng.random())
    x = sum(bp(src, fr * 0.85, fr * 1.15) * g for fr, g in FORM[vowel])
    put(x / (3 * len(freqs)) * fade(n, a, r) * 2.2, t0, gain, pan, wet)

def bell(t, f, gain, dur=3.0, pan=0.0, wet=0.5):
    n = int(dur * SR); tt = np.arange(n) / SR
    x = sum(np.sin(2 * np.pi * f * m * tt) * g * np.exp(-tt * d) for m, g, d in [(1, 1, 1.2), (2.0, 0.4, 2.0), (2.76, 0.35, 2.6), (5.4, 0.2, 4.0)])
    put(x.astype(np.float32) * 0.3, t, gain, pan, wet)

def boom(t, gain=1.0):
    n = int(3.5 * SR); tt = np.arange(n) / SR
    f = 30 + 40 * np.exp(-tt * 6); x = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tt * 1.3)
    nz = lp(rng.standard_normal(n), 220, 3) * np.exp(-tt * 2.2) * 0.9
    put((x + nz).astype(np.float32) * 0.9, t, gain, 0, 0.3)

def braam(t, gain=1.0, dur=3.2, base=(D1, D2, A2, F2)):
    n = int(dur * SR); x = np.zeros(n, np.float32)
    for f in base:
        for dt in (-0.3, 0, 0.35): x += saw(f * (1 + dt / 100), n, rng.random())
    x = np.tanh(x * 0.9); e = env(n, 0.06, dur * 0.8) * np.exp(-np.arange(n) / SR * 0.7)
    x = lp(x, 900, 4) * e
    put(x / 6 * 1.6, t, gain, 0, 0.35)

def clang(t, gain=1.0, base=180):
    n = int(2.6 * SR); tt = np.arange(n) / SR
    x = sum(np.sin(2 * np.pi * base * m * tt) * g * np.exp(-tt * d) for m, g, d in [(1, 1, 1.5), (2.76, 0.6, 2.2), (5.40, 0.5, 3.5), (8.93, 0.35, 5), (13.3, 0.2, 7)])
    tr = hp(rng.standard_normal(n), 2000) * np.exp(-tt * 40) * 0.6
    put((x * 0.35 + tr).astype(np.float32), t, gain, 0, 0.45)

def taiko(t, gain=1.0, f0=120, f1=52):
    n = int(1.0 * SR); tt = np.arange(n) / SR
    f = f1 + (f0 - f1) * np.exp(-tt * 18); x = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tt * 5)
    nz = lp(rng.standard_normal(n), 900) * np.exp(-tt * 30) * 0.5
    put((x + nz).astype(np.float32) * 0.8, t, gain, (rng.random() - 0.5) * 0.3, 0.2)

def heart(t0, t1, bpm, gain):
    t = t0
    while t < t1:
        for dt, g in ((0, 1.0), (0.26, 0.6)):
            n = int(0.35 * SR); tt = np.arange(n) / SR
            x = np.sin(2 * np.pi * (48 + 20 * np.exp(-tt * 30)) * tt) * np.exp(-tt * 14)
            put(lp(x, 160).astype(np.float32), t + dt, gain * g, 0, 0.1)
        t += 60 / bpm

def noise_sweep(t, dur, f0, f1, gain, reverse=False, wet=0.3):
    n = int(dur * SR); x = rng.standard_normal(n).astype(np.float32); out = np.zeros(n, np.float32); B = 2048
    for i in range(0, n, B):
        p = i / n; fc = f0 * (f1 / f0) ** p
        out[i:i + B] = bp(x[i:i + B], fc * 0.7, min(fc * 1.4, 20000))
    e = np.sin(np.pi * np.linspace(0, 1, n)) ** 2
    if reverse: e = np.linspace(0, 1, n) ** 3
    put(out * e, t, gain, 0, wet)

def shatter(t, gain=1.0):
    n = int(2.5 * SR); tt = np.arange(n) / SR; x = hp(rng.standard_normal(n), 3000) * np.exp(-tt * 9) * 0.6
    for k in range(40):
        st = int(rng.random() * 0.9 * SR); f = 2000 + rng.random() * 5000; m = n - st
        x[st:] += np.sin(2 * np.pi * f * tt[:m]) * np.exp(-tt[:m] * (10 + rng.random() * 20)) * 0.12
    put(x.astype(np.float32), t, gain, 0, 0.5)

def pluck(t, f, gain, dur=0.5, cut=1800):
    n = int(dur * SR); tt = np.arange(n) / SR
    x = saw(f, n) + saw(f * 1.005, n) * 0.6
    x = lp(x * np.exp(-tt * 5), cut, 2)
    put(x.astype(np.float32) * 0.45, t, gain, 0, 0.25)

def musicbox(t, f, gain, detune=0.0, crush=0, pan=0.0):
    n = int(1.6 * SR); tt = np.arange(n) / SR; ff = f * 2 ** (detune / 12)
    x = (np.sin(2 * np.pi * ff * tt) + 0.25 * np.sin(2 * np.pi * ff * 3.01 * tt) * np.exp(-tt * 6)) * np.exp(-tt * 2.2)
    if crush: x = np.round(x * crush) / crush
    put(x.astype(np.float32) * 0.35, t, gain, pan, 0.45)

# ================= the score =================
# I «Дар» 0–32: low drone, a cold high tone, the heartbeat; «Лечи.» = metal; years of dark
drone(0.3, 33, [D1, A1], 0.38, cut=300, a=4, r=2)
drone(3, 31, [D3 * 1.0, 155.56], 0.10, cut=1200, a=6, r=4, wet=0.7)       # D + Eb: the wrong interval
for f, t in [(D4 * 2, 4.6), (A4 * 2, 6.4), (F4 * 2, 8.2)]: bell(t, f, 0.18, 4, pan=0.3)
heart(0.6, 30, 60, 0.9)
for t in C['clang']: clang(t, 0.9, 170 + (t % 3) * 15)
boom(9.0, 0.8)
choir(22, 32, [D3, F3, A3], 0.25, 'u', a=4, r=3)
# II «Бруно» 32–58: the door, a warm progression, the hunt, the fire
boom(32.0, 1.0); noise_sweep(31.0, 1.0, 400, 6000, 0.25, reverse=True)
prog = [(33, [D2, D3, F3, A3]), (35.5, [Bb1, D3, F3, Bb3]), (38, [F2, F3, A3, C4]), (40.5, [C2, E3, G3, C4])]
for t, ch in prog: choir(t, t + 3.2, ch, 0.95, 'o', a=1.2, r=1.4)
drone(32.5, 42.2, [D2], 0.22, cut=260, a=1.5, r=0.5)
drone(42, 52.5, [D1, A1], 0.6, cut=500, a=0.3, r=1.0)
for i, t in enumerate(C['battle']):
    if i % 4 in (0, 3) or i % 8 == 6: taiko(t, 1.0 if i % 4 == 0 else 0.6)
for t in C['braam']: braam(t, 0.8, 2.8)
for t, ch in [(42, [D3, A3]), (45, [Bb2, F3]), (48, [F3, C4]), (51, [A2, E3])]: choir(t, t + 3.1, ch, 0.25, 'a', a=0.3, r=1)
choir(52.5, 58.5, [D3, F3, A3, D4], 0.65, 'u', a=1.5, r=2.5)
for t, f in [(53, D4), (54.6, F4), (56.2, A3), (57.4, E4)]: bell(t, f, 0.35, 3, pan=-0.2)
for i in range(300): put(lp(rng.standard_normal(200) * 0.4, 3000).astype(np.float32), 52.5 + rng.random() * 6, 0.25, rng.random() - 0.5, 0.05)  # campfire
# III «Аларус» 58–90: the ritual rises, silver bells, the reveal; the kill; the power; the teleports
for k, t in enumerate(np.arange(58, 78.5, 2.5)):
    choir(t, t + 3.0, [D3, A3, D4, E4 if k % 2 else F4][:3 + min(1, k // 3)], 0.18 + 0.03 * k, 'a', a=0.8, r=1.2)
drone(58, 78.5, [D1, D2], 0.5, cut=350, a=4, r=0.05)
boom(64.0, 1.0); shatter(64.0, 0.25)
for i, t in enumerate(np.arange(64.5, 78.4, 0.214)): bell(t, [D4 * 2, A4 * 2, F4 * 2, E4 * 2, A4, D4 * 2][i % 6] * (1 if i % 12 < 6 else 1.335), 0.05 + 0.04 * (t - 64) / 14, 1.6, pan=np.sin(i) * 0.6)
noise_sweep(75.5, 3.0, 200, 9000, 0.3, reverse=True)
boom(79.0, 1.2); shatter(79.0, 0.9); braam(79.0, 1.0, 4.0, base=(D1, D2, Bb1 * 2, F2))
drone(79.5, 90.5, [D1, Bb1, D2], 0.55, cut=700, a=0.5, r=1.5)
choir(81, 90, [D3, Eb3 := 155.56, A3], 0.2, 'a', a=2, r=1.5)
for t in C['whoosh']: noise_sweep(t - 0.35, 0.6, 300, 8000, 0.45); bell(t, A4 * 2, 0.15, 2)
# IV «Сын» 90–118: the branches of time; the sky shakes; the line; darkness; the rewind
for t, f in [(90.5, 880), (91.6, 1318.5), (92.7, 1174.7), (93.8, 1760)]: bell(t, f, 0.4, 4.5, pan=0.4 * np.sin(t))
drone(90.3, 97.4, [D2 * 2, D2 * 2 * 1.006], 0.25, cut=3000, a=2, r=0.1, wet=0.8)
drone(90.3, 97.4, [D1], 0.3, cut=200, a=1, r=0.1)
n = int(2.4 * SR); put(lp(rng.standard_normal(n), 120, 3).astype(np.float32) * np.linspace(0, 1, n) ** 2 * 1.2, 95.0, 1.0, 0, 0.2)  # rumble
boom(98.0, 1.3); braam(98.0, 1.2, 6.0, base=(D1, D1 * 1.5, D2, Eb3 / 2))
drone(99, 112.2, [D1, D1 * 1.059, A1], 0.7, cut=500, a=1, r=0.2)
choir(100, 112, [D2 * 2, F2 * 2, Bb2], 0.3, 'u', a=3, r=0.3)
n = int(8 * SR); x = np.cumsum(rng.standard_normal(n)).astype(np.float32); x -= np.convolve(x, np.ones(2000) / 2000, 'same')
put(np.tanh(lp(x * 0.02, 400, 2) * 3) * np.linspace(0, 1, n) ** 1.5 * 0.5, 104.0, 1.0, 0, 0.3)          # darkness roar
n = int(1.6 * SR); tt = np.arange(n) / SR; f = 220 * np.exp(-tt * 2.5) + 20                                      # tape stop
put(lp(saw(f, n) * (1 - tt / 1.6), 1500).astype(np.float32) * 0.5, 112.0, 1.0, 0, 0.3); noise_sweep(112.0, 1.6, 8000, 200, 0.4)
# V «Цикл» 113.6–142: waking, the ostinato speeds up, a reset at every turn, then it falls apart
for t, f in [(114.5, D4), (116.5, A3), (118.5, F4)]: bell(t, f, 0.35, 3.5)
drone(113.6, 118.5, [D1, A1], 0.3, cut=250, a=1.5, r=0.5)
drone(118, 141.6, [D1, A1], 0.45, cut=380, a=3, r=0.2)
motif = [D3, F3, E3, A2]; t, k = 122.0, 0
while t < 141.5:
    gap = np.interp(t, [122, 137.7, 141.5], [0.42, 0.12, 0.09])
    pluck(t, motif[k % 4] * (2 if t > 132 and k % 8 > 3 else 1), 0.7, dur=min(0.5, gap * 2.2), cut=1200 + 2500 * (t - 122) / 20); k += 1; t += gap
for t in C['resets']: noise_sweep(t - 0.25, 0.3, 6000, 300, 0.3); boom(t, 0.45)
for t in np.arange(122, 141.5, 0.5): put(hp(rng.standard_normal(600), 4000).astype(np.float32) * np.exp(-np.arange(600) / 60), t, 0.25, 0.3, 0.1)  # clock
# VI «Веселье» 142–170: the music box; nightmares; it warps; the end
LULL = [(0, A4), (1, D4 * 2), (2, F4 * 2), (3, E4 * 2), (4, D4 * 2), (5, A4), (6, Bb3 * 2), (7, A4), (8, F4), (9, G4), (10, A4), (11, D4)]
for rep in range(3):
    for b, f in LULL:
        t = 142.3 + rep * 6.6 + b * 0.55
        if t > 166: break
        warp = 0 if t < 150 else (t - 150) / 16
        musicbox(t, f, 0.5, detune=warp * 0.9 * np.sin(t * 3.1), crush=0 if t < 158 else 12, pan=0.2 * np.sin(b))
heart(150, 160, 104, 1.0)
for t in C['nightmare']: clang(t, 0.8, 150); choir(t, t + 1.2, [D3, Eb3, A3], 0.3, 'a', a=0.02, r=0.8)
drone(150, 166, [D1, D1 * 1.03], 0.4, cut=600, a=1, r=0.3)
boom(166.0, 1.1); bell(166.0, D4, 0.35, 4); bell(166.02, D4 * 1.5 * 1.01, 0.2, 4)
drone(166, 170, [D2], 0.2, cut=300, a=0.05, r=3.5)

# ---------------- reverb, silences, master ----------------
ir_n = int(3.4 * SR); tt = np.arange(ir_n) / SR
ir = np.stack([rng.standard_normal(ir_n) * np.exp(-tt * 2.0), rng.standard_normal(ir_n) * np.exp(-tt * 2.0)], 1).astype(np.float32)
ir = lp(ir, 5000); ir /= np.sqrt((ir ** 2).sum(0))
wet = np.stack([fftconvolve(send[:, c], ir[:, c])[:N] for c in range(2)], 1) * 0.9
mix = dry + wet
for a, b in C['silence']:
    g = np.ones(N, np.float32); i0, i1 = int(a * SR), int(b * SR); g[i0:i1] = 0.0
    g[i0 - 2205:i0] = np.linspace(1, 0, 2205); mix *= g[:, None]
mix *= np.clip((END - ts) / 3.0, 0, 1)[:, None]
mix /= max(1e-6, np.percentile(np.abs(mix), 99.9)) / 0.7
import subprocess
subprocess.run(['ffmpeg', '-v', 'error', '-y', '-f', 'f32le', '-ac', '2', '-ar', str(SR), '-i', '-', '-af', 'alimiter=limit=0.93:attack=4:release=120:level=disabled', sys.argv[1]],
               input=np.clip(mix, -3, 3).astype(np.float32).tobytes(), check=True)
# envelope + cues for the picture
fps = 30; hop = SR // fps; m = mix.mean(1); nf = len(m) // hop
rms = np.sqrt((m[:nf * hop].reshape(nf, hop) ** 2).mean(1)); bass = np.abs(lp(m, 150)[:nf * hop]).reshape(nf, hop).mean(1)
nrm = lambda a: [round(float(v), 3) for v in np.clip(a / (np.percentile(a, 99) + 1e-9), 0, 1)]
open('video/versions/l-audio.js', 'w').write('// score envelope (30 fps) and cue times, written by audio/l_score.py\nwindow.AUDIO = ' + json.dumps({'fps': fps, 'rms': nrm(rms), 'bass': nrm(bass), 'cues': C}, separators=(',', ':')) + ';\n')
print(sys.argv[1])
