"""Turns an art into 'drawn' layers for the canvas: ink lines (3 slightly different takes for a hand-drawn boil),
a charcoal tone and a colour-accent layer (only strongly coloured pixels: eyes, ribbons, gold, magic).
usage: python3 drawn.py <image> <out_prefix> [max_side]"""
import sys, numpy as np
from PIL import Image
from scipy.ndimage import gaussian_filter, map_coordinates

src, pre = sys.argv[1], sys.argv[2]
side = int(sys.argv[3]) if len(sys.argv) > 3 else 1100
im = Image.open(src)
im = im.convert('RGBA') if im.mode in ('RGBA', 'LA', 'P') else im.convert('RGB')
im.thumbnail((side, side), Image.LANCZOS)
a = np.asarray(im).astype(np.float32) / 255
rgb = a[..., :3]
alpha = a[..., 3] if a.shape[2] == 4 else np.ones(a.shape[:2], np.float32)
L = rgb @ np.array([0.299, 0.587, 0.114], np.float32)
h, w = L.shape
s = max(h, w) / 1000


def xdog(img, sigma, k=1.6, tau=0.985, eps=-0.004, phi=220):
    g1, g2 = gaussian_filter(img, sigma), gaussian_filter(img, sigma * k)
    d = g1 - tau * g2
    e = np.where(d >= eps, 1.0, 1 + np.tanh(phi * (d - eps)))
    return np.clip(1 - e, 0, 1)  # 1 = ink


def rgba(col, al):
    out = np.zeros((h, w, 4), np.uint8)
    out[..., :3] = col
    out[..., 3] = (np.clip(al, 0, 1) * 255).astype(np.uint8)
    return Image.fromarray(out, 'RGBA')


rng = np.random.default_rng(3)
yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
for v in range(3):
    dx = gaussian_filter(rng.standard_normal((h, w)).astype(np.float32), 18 * s) * 60 * s
    dy = gaussian_filter(rng.standard_normal((h, w)).astype(np.float32), 18 * s) * 60 * s
    Lw = map_coordinates(L, [yy + dy, xx + dx], order=1, mode='nearest')
    ink = np.clip(xdog(Lw, 1.1 * s) * 0.85 + xdog(Lw, 2.4 * s, eps=-0.008) * 0.5, 0, 1) * alpha
    rgba((236, 228, 214), ink).save(f'{pre}-lines{v}.png')
# charcoal tone: smooth, dim luminance with a little grain
tone = np.clip(gaussian_filter(L, 1.5 * s) ** 1.15 * 0.62 + (rng.random((h, w)) - 0.5) * 0.05, 0, 1)
rgba((tone[..., None] * np.array([255, 246, 236])).astype(np.uint8), alpha).save(f'{pre}-tone.png')
# colour accents: saturated and not dark
mx, mn = rgb.max(2), rgb.min(2)
sat = (mx - mn) / (mx + 1e-4)
m = gaussian_filter(np.clip((sat - 0.42) / 0.25, 0, 1) * np.clip((mx - 0.28) / 0.2, 0, 1) * alpha, 1.2 * s)
rgba((rgb * 255).astype(np.uint8), m).save(f'{pre}-color.png')
print(pre, w, h)
