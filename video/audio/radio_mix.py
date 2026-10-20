"""Radio sound mix: the song through a valve radio.
usage: python3 radio_mix.py <song> <curves.json> <out.wav> <length_s> [fade_out_start_s]
Curves (sampled every dt seconds; missing ones default to 0, power to 1):
  tuned, tx  — clean music when either is 1, otherwise band-limited, crushed radio under hiss
  power      — valves warming up / dying (master gain)
  burst      — white-noise static burst (station switch, masks song edits)
  morse      — 0/1 key of an 800 Hz morse tone
  siren      — level of a distant air-raid siren inside the noise
  over       — overload: the music distorts and drowns in noise (explosion)
  boom       — level of a low explosion rumble
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
def cur(k, d=0.0, smooth=0.05):
    a = np.interp(ts, tt, [e.get(k, d) for e in v])
    w = max(1, int(smooth * SR))
    return np.convolve(a, np.ones(w) / w, mode='same') if w > 1 else a
tuned, power, tx = cur('tuned'), cur('power', 1.0), cur('tx')
burst, siren, over, boom = cur('burst', smooth=0.01), cur('siren', smooth=0.2), cur('over', smooth=0.03), cur('boom', smooth=0.02)
morse = cur('morse', smooth=0.005)
clean = np.clip(np.maximum(tuned, tx), 0, 1)

rng = np.random.default_rng(1)
radio = sosfilt(butter(4, [380, 2600], btype='band', fs=SR, output='sos'), x, axis=0)
radio = np.tanh(radio * 2.2) / 2.2 * 1.6
hiss = rng.standard_normal((n, 2)).astype(np.float32)
hiss = sosfilt(butter(2, [1500, 9000], btype='band', fs=SR, output='sos'), hiss, axis=0) * 0.07
crackle = (rng.random((n, 1)) < 0.0004) * rng.standard_normal((n, 1)) * 0.35
wobble = 0.75 + 0.25 * np.sin(2 * np.pi * 0.7 * ts)[:, None]  # fading in and out of the signal

g = clean[:, None]
mix = x * g + radio * (1 - g) * wobble * 0.9 + (hiss + crackle) * (1 - 0.85 * g)
# static burst: broadband noise, the music ducks under it
white = sosfilt(butter(2, [300, 11000], btype='band', fs=SR, output='sos'), rng.standard_normal((n, 2)), axis=0).astype(np.float32)
mix = mix * (1 - 0.85 * burst[:, None]) + white * 0.33 * burst[:, None]
# morse: 800 Hz with a slight valve buzz
tone = (np.sin(2 * np.pi * 800 * ts) + 0.15 * np.sin(2 * np.pi * 1600 * ts)) * 0.16
mix += (tone * morse)[:, None]
# distant siren: slow rising/falling tone, band-limited like the radio
sf = 520 + 260 * (0.5 - 0.5 * np.cos(2 * np.pi * ts / 4.0))
sir = np.sin(2 * np.pi * np.cumsum(sf) / SR) * 0.12
mix += (sir * siren)[:, None]
# overload: the music clips hard and drowns in white noise
mix = mix * (1 - over[:, None]) + (np.tanh(x * 9) * 0.35 + white * 0.5) * over[:, None]
# explosion rumble
brown = np.cumsum(rng.standard_normal(n)).astype(np.float32); brown -= np.convolve(brown, np.ones(4410) / 4410, mode='same')
rum = sosfilt(butter(2, 120, btype='low', fs=SR, output='sos'), brown) * 0.02
mix += (rum * boom)[:, None]

mix *= power[:, None]  # valves warming up or dying
fade = np.clip((length - ts) / max(0.5, length - fo), 0, 1)[:, None]
mix *= fade
mix = np.tanh(mix * 1.1) / 1.1
subprocess.run(['ffmpeg', '-v', 'error', '-y', '-f', 'f32le', '-ac', '2', '-ar', str(SR), '-i', '-', out],
               input=mix.astype(np.float32).tobytes(), check=True)
print(out)
