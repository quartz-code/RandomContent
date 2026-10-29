"""Pulse: builds every app icon from the rings logo (pulse.svg's geometry).

Run from the repository root: python3 assets/logo/build_icons.py
Writes the Android adaptive icon layers (vector), the legacy raster mipmaps, the Play Store icon
and the desktop app's icons. Needs Chromium (to rasterise SVG) and Pillow.
"""
import io, os, shutil, subprocess, sys, tempfile
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
RES = os.path.join(ROOT, 'app/src/main/res')
INK, CORAL, AMBER = '#14131A', '#FF4F7B', '#FFA24C'

# The logo in a 512 box, centred at 256: rings, a disc and the beat across it.
RINGS = [(178, 14, 0.35), (128, 20, 0.65)]
DISC = 74
BEAT = [(-50, 0), (-28, 0), (-14, -30), (6, 34), (22, -16), (32, 0), (50, 0)]
BEAT_WIDTH = 13


def svg(size, background=True, rounded=True, scale=1.0, mono=False):
    """The logo as SVG; [scale] shrinks it inside the box (adaptive icons keep a safe margin)."""
    c = size / 2
    k = size / 512 * scale
    grad = '<defs><linearGradient id="g" x1="0" y1="1" x2="1" y2="0"><stop offset="0" stop-color="%s"/><stop offset="1" stop-color="%s"/></linearGradient></defs>' % (CORAL, AMBER)
    paint = '#FFFFFF' if mono else 'url(#g)'
    parts = [grad]
    if background:
        parts.append('<rect width="%d" height="%d" rx="%s" fill="%s"/>' % (size, size, size * 116 / 512 if rounded else 0, INK))
    for r, w, a in RINGS:
        parts.append('<circle cx="%s" cy="%s" r="%s" fill="none" stroke="%s" stroke-width="%s" opacity="%s"/>' % (c, c, r * k, paint, w * k, a))
    beat = ' '.join(('M' if i == 0 else 'L') + '%s %s' % (c + x * k, c + y * k) for i, (x, y) in enumerate(BEAT))
    if mono:
        # One colour only: the beat stands on its own where the disc would be.
        parts.append('<path d="%s" fill="none" stroke="#FFFFFF" stroke-width="%s" stroke-linecap="round" stroke-linejoin="round"/>' % (beat, BEAT_WIDTH * 1.6 * k))
    else:
        parts.append('<circle cx="%s" cy="%s" r="%s" fill="%s"/>' % (c, c, DISC * k, paint))
        parts.append('<path d="%s" fill="none" stroke="%s" stroke-width="%s" stroke-linecap="round" stroke-linejoin="round"/>' % (beat, INK, BEAT_WIDTH * k))
    return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %d %d" width="%d" height="%d">%s</svg>' % (size, size, size, size, ''.join(parts))


def render(svg_text, size):
    chrome = next((os.path.join(d, 'chrome-linux/chrome') for d in sorted(os.listdir('/opt/pw-browsers')) if d.startswith('chromium-')), None)
    chrome = os.path.join('/opt/pw-browsers', chrome) if chrome else shutil.which('chromium')
    with tempfile.TemporaryDirectory() as tmp:
        page = os.path.join(tmp, 'icon.html')
        with open(page, 'w') as f:
            f.write('<html><body style="margin:0;background:transparent">%s</body></html>' % svg_text)
        out = os.path.join(tmp, 'icon.png')
        subprocess.run([chrome, '--headless', '--no-sandbox', '--disable-gpu', '--hide-scrollbars', '--default-background-color=00000000',
                        '--screenshot=' + out, '--window-size=%d,%d' % (size, size), 'file://' + page], check=True, capture_output=True)
        return Image.open(out).convert('RGBA').crop((0, 0, size, size))


def vector(mono=False, scale=0.72, size_dp=108):
    """The adaptive foreground (or monochrome) layer: 108dp, the logo inside the 66dp safe circle."""
    k = 108 / 512 * scale
    c = 54
    def ring(r, w, a):
        rr = r * k
        stroke = '        <aapt:attr name="android:strokeColor"><gradient android:type="linear" android:startX="0" android:startY="108" android:endX="108" android:endY="0" android:startColor="%s" android:endColor="%s"/></aapt:attr>\n' % (CORAL, AMBER)
        return ('    <path android:strokeWidth="%.2f" android:strokeAlpha="%s" android:pathData="M%.2f,%.2fa%.2f,%.2f 0 1,1 %.2f,0a%.2f,%.2f 0 1,1 %.2f,0"%s>\n%s    </path>\n'
                % (w * k, a, c - rr, c, rr, rr, 2 * rr, rr, rr, -2 * rr, ' android:strokeColor="#FFFFFFFF"' if mono else '', '' if mono else stroke))
    beat = ' '.join(('M' if i == 0 else 'L') + '%.2f,%.2f' % (c + x * k, c + y * k) for i, (x, y) in enumerate(BEAT))
    body = ''.join(ring(*r) for r in RINGS)
    if mono:
        body += '    <path android:strokeColor="#FFFFFFFF" android:strokeWidth="%.2f" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="%s" />\n' % (BEAT_WIDTH * 1.6 * k, beat)
    else:
        rr = DISC * k
        body += ('    <path android:pathData="M%.2f,%.2fa%.2f,%.2f 0 1,1 %.2f,0a%.2f,%.2f 0 1,1 %.2f,0">\n'
                 '        <aapt:attr name="android:fillColor"><gradient android:type="linear" android:startX="%.2f" android:startY="%.2f" android:endX="%.2f" android:endY="%.2f" android:startColor="%s" android:endColor="%s"/></aapt:attr>\n'
                 '    </path>\n' % (c - rr, c, rr, rr, 2 * rr, rr, rr, -2 * rr, c - rr, c + rr, c + rr, c - rr, CORAL, AMBER))
        body += '    <path android:strokeColor="#FF14131A" android:strokeWidth="%.2f" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="%s" />\n' % (BEAT_WIDTH * k, beat)
    return ('<?xml version="1.0" encoding="utf-8"?>\n<!-- Pulse logo: a beat spreading in rings. Made by assets/logo/build_icons.py; edit that, not this. -->\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android" xmlns:aapt="http://schemas.android.com/aapt"\n'
            '    android:width="%ddp" android:height="%ddp" android:viewportWidth="108" android:viewportHeight="108">\n%s</vector>\n' % (size_dp, size_dp, body))


def main():
    with open(os.path.join(RES, 'drawable/ic_launcher_foreground.xml'), 'w') as f:
        f.write(vector())
    with open(os.path.join(RES, 'drawable/ic_launcher_monochrome.xml'), 'w') as f:
        f.write(vector(mono=True))
    # The notification's small icon and the logo on the About screen: one colour, edge to edge.
    with open(os.path.join(RES, 'drawable/small_icon.xml'), 'w') as f:
        f.write(vector(mono=True, scale=1.0, size_dp=24))
    sizes = {'mdpi': 48, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}
    for density, px in sizes.items():
        folder = os.path.join(RES, 'mipmap-' + density)
        square = render(svg(px * 4), px * 4).resize((px, px), Image.LANCZOS)
        round_ = Image.new('RGBA', (px * 4, px * 4))
        full = render(svg(px * 4, rounded=False), px * 4)
        mask = Image.new('L', (px * 4, px * 4))
        from PIL import ImageDraw
        ImageDraw.Draw(mask).ellipse((0, 0, px * 4 - 1, px * 4 - 1), fill=255)
        round_.paste(full, (0, 0), mask)
        round_ = round_.resize((px, px), Image.LANCZOS)
        for name, img in (('ic_launcher', square), ('ic_launcher_round', round_)):
            img.save(os.path.join(folder, name + '.webp'), 'WEBP', lossless=True)
        square.save(os.path.join(folder, 'ic_launcher_static.png'))
    render(svg(512), 512).save(os.path.join(ROOT, 'assets/ic_launcher-playstore.png'))
    store = os.path.join(ROOT, 'fastlane/metadata/android/en-US/images/icon.png')
    if os.path.isdir(os.path.dirname(store)):
        render(svg(512), 512).save(store)
    # Desktop: a PNG for Linux, an ICO with several sizes for Windows.
    icons = os.path.join(ROOT, 'desktop/icons')
    os.makedirs(icons, exist_ok=True)
    big = render(svg(512), 512)
    big.save(os.path.join(icons, 'pulse.png'))
    big.save(os.path.join(icons, 'pulse.ico'), sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
    print('icons written')


if __name__ == '__main__':
    sys.exit(main())
