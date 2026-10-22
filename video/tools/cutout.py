"""Cuts a character out of a plain studio background (flat or a soft gradient, grey or white).
The background is modelled per row from the left and right borders; pixels far from it are kept.
usage: python3 cutout.py <in> <out.png> [threshold]"""
import sys, numpy as np
from PIL import Image, ImageFilter

src, out = sys.argv[1], sys.argv[2]
thr = float(sys.argv[3]) if len(sys.argv) > 3 else 14
im = Image.open(src).convert('RGB')
a = np.asarray(im).astype(float)
h, w, _ = a.shape
left, right = np.median(a[:, :24], 1), np.median(a[:, -24:], 1)
xs = np.linspace(0, 1, w)[None, :, None]
bg = left[:, None, :] * (1 - xs) + right[:, None, :] * xs
d = np.sqrt(((a - bg) ** 2).sum(2))
m = np.clip((d - thr) / 16, 0, 1)
mi = Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.MedianFilter(5)).filter(ImageFilter.GaussianBlur(1))
res = im.copy()
res.putalpha(mi)
bb = mi.point(lambda v: 255 if v > 60 else 0).getbbox()
res.crop(bb).save(out)
print(out, bb)
