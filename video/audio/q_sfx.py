"""Sound for version Q («Письмо»): the song plus the room — the lamp switch, the pen, the strikes, the envelope,
knocks on the door, the brass slot rattling, radio static, and the rain that stays after the song stops.
usage: python3 q_sfx.py <song> <out.wav> <length_s>"""
import sys, subprocess, numpy as np
from scipy.signal import butter, sosfilt

song, out, length = sys.argv[1], sys.argv[2], float(sys.argv[3])
SR = 44100; N = int(length * SR); rng = np.random.default_rng(17)
raw = subprocess.run(['ffmpeg', '-v', 'error', '-i', song, '-t', str(length), '-f', 'f32le', '-ac', '2', '-ar', str(SR), '-'], capture_output=True, check=True).stdout
x = np.frombuffer(raw, np.float32).reshape(-1, 2).copy(); x = np.vstack([x, np.zeros((max(0, N - len(x)), 2), np.float32)])[:N]
fx = np.zeros(N, np.float32)
def put(sig, t, g=1.0):
    i = int(t * SR); n = min(len(sig), N - i)
    if n > 0: fx[i:i + n] += sig[:n].astype(np.float32) * g
def bp(s, a, b): return sosfilt(butter(2, [a, b], btype='band', fs=SR, output='sos'), s)
def lp(s, f): return sosfilt(butter(2, f, btype='low', fs=SR, output='sos'), s)
noise = lambda d: rng.standard_normal(int(d * SR))
tt = lambda d: np.arange(int(d * SR)) / SR
BEAT = 0.6896; BAR = BEAT * 4; bar = lambda n: 0.937 + BAR * n; STOP = 91.95
pr = lambda t, a, b: min(1, max(0, (t - a) / (b - a)))

def click(t, g=0.3): u = tt(0.05); put(bp(noise(0.05), 1500, 7000) * np.exp(-u * 160), t, g); put(np.sin(2 * np.pi * 180 * u) * np.exp(-u * 90), t, g * 0.6)
def pen(t, d, g=0.05):  # a nib on paper: filtered noise in little strokes
    u = tt(d); env = (0.5 + 0.5 * np.abs(np.sin(2 * np.pi * 7 * u + rng.random() * 6))) ** 2 * np.minimum(1, np.minimum(u, d - u) * 30)
    put(bp(noise(d), 2500, 7000) * env, t, g)
def strike(t, g=0.13): u = tt(0.32); put(bp(noise(0.32), 1200, 5000) * np.sin(np.pi * u / 0.32) * (0.6 + 0.4 * np.sin(2 * np.pi * 14 * u)), t, g)
def knock(t, g=0.5):
    u = tt(0.25); put(lp(noise(0.25), 500) * np.exp(-u * 40) * 2.2 + np.sin(2 * np.pi * (95 + 40 * np.exp(-u * 30)) * u) * np.exp(-u * 22), t, g)
def rattle(t, g=0.12):  # the brass flap of the slot
    for j, dt in enumerate((0, 0.045, 0.1)):
        u = tt(0.3); put(sum(np.sin(2 * np.pi * f * u) for f in (2140, 3410, 5230)) * np.exp(-u * (28 + 10 * j)) / 3, t + dt, g * (1 - 0.3 * j))
def static(t, d, g=0.06):  # an old set between stations: hiss and crackle
    u = tt(d); s = bp(noise(d), 600, 4500) * (0.7 + 0.3 * np.sin(2 * np.pi * 0.7 * u)); c = (rng.random(len(u)) > 0.9993) * rng.standard_normal(len(u)) * 6
    env = np.minimum(1, np.minimum(u, d - u) * 12); put((s + bp(c, 800, 6000)) * env, t, g)
def rustle(t, d, g=0.12): u = tt(d); put(bp(noise(d), 700, 6000) * np.abs(np.sin(2 * np.pi * 3 * u)) * np.sin(np.pi * u / d), t, g)

# the lamp: the switch, then the bulb catches
click(0.98, 0.35)
for k in range(6): put(bp(noise(0.03), 3000, 9000) * 0.4, 1.0 + k * 0.067, 0.12)

# the letter, as written in q.js
DRAFT = [('Здравствуй, милая моя мама.', 4.6, None), ('меня ранило.', 8.0, 9.4), ('Со мною всё хорошо.', 9.9, None),
         ('я не могу говорить.', bar(5) + 0.1, bar(5) + 1.5), ('Не волнуйся за меня.', bar(5) + 2.0, None),
         ('я совсем один.', bar(6) + 1.4, bar(6) + 2.6), ('Я здесь не один.', bar(7) + 0.6, None)]
for s, t0, x2 in DRAFT:
    pen(t0, len(s) / 20)
    if x2: strike(x2)
CLEAN = ['Войну пережил, надеюсь,', 'что ты жива и здорова.', 'Сейчас живу в Берлине,', 'работаю в больнице.', 'Слово, которое я дал тебе', 'перед уходом, я сохранил.', 'Рядом со мной хорошие,', 'добрые люди.',
         'Как там Артём и Андрей?', 'Очень скучаю по нашим посиделкам', 'за кружкой чёрного горячего чая.', 'Мы обязательно увидимся', 'и сядем рядом, как прежде.', 'Целую тебя крепко-крепко.', 'Твой сын Костя.']
for i, s in enumerate(CLEAN): pen(bar(8) + i * BAR / 2 + 0.05, len(s) / 26, 0.035)
# the envelope: folded, sealed
rustle(bar(16), 0.8); rustle(bar(16) + 0.9, 0.6, 0.08); pen(bar(16) + 1.5, 0.5, 0.05)

# the knocks and the radio
for t in (12.0, 12.3, 12.6): knock(t, 0.6)
static(13.0, bar(5) - 13.0, 0.07)
for n in range(8):  # the nights: one knock a night, and the shot decides what else we hear
    knock(bar(17 + n) + 0.3, 0.45)
    shot = ['room', 'slot', 'window', 'room', 'radio', 'slot', 'window', 'room'][n]
    if shot == 'slot': rattle(bar(17 + n) + 0.3)
    if shot == 'radio': static(bar(17 + n), BAR, 0.07)
t = bar(25); i = 0
while t < STOP:  # faster and faster
    p = pr(t, bar(25), STOP); step = BAR / 2 if p < 0.5 else BEAT
    shot = ['slot', 'drafts', 'room', 'radio', 'window', 'drafts', 'slot', 'room'][i % 8]
    if shot == 'slot': rattle(t, 0.1 + 0.08 * p)
    elif shot == 'radio': static(t, min(step, STOP - t), 0.06 + 0.06 * p)
    elif shot == 'drafts': rustle(t, min(step, 0.5), 0.07)
    if i % 2 == 0: knock(t + 0.02, 0.25 + 0.2 * p)
    t += step; i += 1

# rain on the window all along, under the song; alone after it stops
rain = lp(bp(noise(length), 300, 6000), 3500) * 0.018
drops = (rng.random(N) > 0.9985) * rng.standard_normal(N); rain += bp(drops, 2000, 8000) * 0.05
u = np.arange(N) / SR; rain *= np.clip(u / 2, 0, 1) * (0.6 + 0.4 * np.clip((u - STOP) / 0.8, 0, 1))
fx += rain.astype(np.float32)
# the lamp hum in the silence
put(np.sin(2 * np.pi * 50 * tt(length - STOP)) * 0.012 + np.sin(2 * np.pi * 100 * tt(length - STOP)) * 0.006, STOP)

mix = x + np.stack([fx, fx], 1)
subprocess.run(['ffmpeg', '-v', 'error', '-y', '-f', 'f32le', '-ac', '2', '-ar', str(SR), '-i', '-', '-af', 'alimiter=limit=0.95:level=disabled', out],
               input=np.clip(mix, -2, 2).astype(np.float32).tobytes(), check=True)
print(out)
