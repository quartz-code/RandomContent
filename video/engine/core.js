// Shared animation engine: every frame is a pure function of time t (seconds),
// so the renderer can step through frames deterministically.
(function () {
  const W = 1080, H = 1920;
  const K = { W, H, images: {}, fps: 30 };

  // ---------- math ----------
  K.clamp = (v, a = 0, b = 1) => Math.min(b, Math.max(a, v));
  K.lerp = (a, b, p) => a + (b - a) * p;
  K.prog = (t, a, b) => K.clamp((t - a) / (b - a));
  K.ease = {
    linear: p => p,
    inQuad: p => p * p,
    outQuad: p => 1 - (1 - p) * (1 - p),
    outCubic: p => 1 - Math.pow(1 - p, 3),
    inOutCubic: p => (p < 0.5 ? 4 * p * p * p : 1 - Math.pow(-2 * p + 2, 3) / 2),
    inOutSine: p => -(Math.cos(Math.PI * p) - 1) / 2,
    outExpo: p => (p === 1 ? 1 : 1 - Math.pow(2, -10 * p)),
    outBack: p => { const c1 = 1.70158, c3 = c1 + 1; return 1 + c3 * Math.pow(p - 1, 3) + c1 * Math.pow(p - 1, 2); },
  };
  // fade in over [a, a+fi], hold, fade out over [b-fo, b]
  K.window = (t, a, b, fi = 0.6, fo = 0.6) => {
    if (t < a || t > b) return 0;
    return Math.min(fi > 0 ? K.clamp((t - a) / fi) : 1, fo > 0 ? K.clamp((b - t) / fo) : 1);
  };
  K.rng = seed => () => {
    seed |= 0; seed = (seed + 0x6d2b79f5) | 0;
    let r = Math.imul(seed ^ (seed >>> 15), 1 | seed);
    r = (r + Math.imul(r ^ (r >>> 7), 61 | r)) ^ r;
    return ((r ^ (r >>> 14)) >>> 0) / 4294967296;
  };
  K.hash = (a, b = 0) => K.rng(a * 7919 + b * 104729 + 13)();

  // ---------- canvases ----------
  // Offscreen canvases are forced onto the CPU raster path: in headless Chromium the default
  // (software-emulated GPU) path makes text and small draws on them ~100x slower.
  K.makeCanvas = (w = W, h = H) => {
    const c = document.createElement('canvas'); c.width = w; c.height = h;
    c.getContext('2d', { willReadFrequently: true }); return c;
  };
  K.init = canvas => {
    K.canvas = canvas; K.ctx = canvas.getContext('2d');
    K.layers = [K.makeCanvas(), K.makeCanvas(), K.makeCanvas()];
    K.noise = [0, 1, 2, 3].map(i => {
      const c = K.makeCanvas(512, 512), x = c.getContext('2d'), d = x.createImageData(512, 512), r = K.rng(99 + i);
      for (let p = 0; p < d.data.length; p += 4) {
        const v = (r() * 255) | 0; d.data[p] = d.data[p + 1] = d.data[p + 2] = v; d.data[p + 3] = 255;
      }
      x.putImageData(d, 0, 0); return c;
    });
  };

  K.loadImages = map => Promise.all(Object.entries(map).map(([k, url]) => new Promise((res, rej) => {
    const im = new Image();
    im.onload = () => { K.images[k] = im; res(); };
    // upscaled crops may not exist yet: fall back to the original crop
    im.onerror = () => {
      if (url.includes('/up/')) { url = url.replace('/up/', '/').replace(/\.jpg$/, '.png'); im.src = url; } else rej(new Error('image failed: ' + url));
    };
    im.src = url;
  })));

  // ---------- image drawing ----------
  // Draw img to cover rect (x,y,w,h) at zoom z (>=1) with focus point fx,fy in 0..1 of the image.
  K.cover = (ctx, img, rect, z = 1, fx = 0.5, fy = 0.5, src) => {
    const [sx0, sy0, sw0, sh0] = src || [0, 0, img.width, img.height];
    const s = Math.max(rect.w / sw0, rect.h / sh0) * z;
    const vw = rect.w / s, vh = rect.h / s;
    const sx = sx0 + K.clamp(fx * sw0 - vw / 2, 0, sw0 - vw);
    const sy = sy0 + K.clamp(fy * sh0 - vh / 2, 0, sh0 - vh);
    ctx.drawImage(img, sx, sy, vw, vh, rect.x, rect.y, rect.w, rect.h);
  };
  // Ken Burns between two {z, x, y} states over progress p.
  K.kenBurns = (ctx, img, p, from, to, rect = { x: 0, y: 0, w: W, h: H }, src) => {
    const e = K.ease.inOutSine(K.clamp(p));
    K.cover(ctx, img, rect, K.lerp(from.z, to.z, e), K.lerp(from.x, to.x, e), K.lerp(from.y, to.y, e), src);
  };
  // Photo print: white border, soft shadow, rotation (deg), centred at cx,cy.
  K.print = (ctx, img, cx, cy, w, h, rot = 0, o = {}) => {
    const b = o.border ?? 22;
    ctx.save();
    ctx.translate(cx, cy); ctx.rotate(rot * Math.PI / 180);
    ctx.shadowColor = 'rgba(0,0,0,0.55)'; ctx.shadowBlur = o.shadow ?? 40; ctx.shadowOffsetY = 14;
    ctx.fillStyle = o.paper || '#e9e1cf';
    ctx.fillRect(-w / 2 - b, -h / 2 - b, w + 2 * b, h + 2 * b + (o.bottom || 0));
    ctx.shadowColor = 'transparent';
    ctx.filter = o.filter || 'sepia(0.35) contrast(1.05) saturate(0.7)';
    K.cover(ctx, img, { x: -w / 2, y: -h / 2, w, h }, o.z || 1, o.fx ?? 0.5, o.fy ?? 0.5, o.src);
    ctx.filter = 'none';
    // age the print a little
    const g = ctx.createRadialGradient(0, 0, Math.min(w, h) * 0.3, 0, 0, Math.max(w, h) * 0.75);
    g.addColorStop(0, 'rgba(60,40,20,0)'); g.addColorStop(1, 'rgba(60,40,20,0.35)');
    ctx.fillStyle = g; ctx.fillRect(-w / 2, -h / 2, w, h);
    if (o.clip) K.paperclip(ctx, -w / 2 + w * 0.18, -h / 2 - b - 18);
    if (o.caption) {
      ctx.fillStyle = 'rgba(40,30,25,0.85)'; ctx.font = `44px Caveat`; ctx.textAlign = 'center';
      ctx.fillText(o.caption, 0, h / 2 + b + (o.bottom || 0) * 0.6);
    }
    ctx.restore();
  };
  K.paperclip = (ctx, x, y) => {
    ctx.save(); ctx.translate(x, y); ctx.rotate(-0.08);
    ctx.strokeStyle = '#9a9a9a'; ctx.lineWidth = 6; ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.moveTo(10, 120); ctx.lineTo(10, 18); ctx.arc(28, 18, 18, Math.PI, 0); ctx.lineTo(46, 100);
    ctx.arc(32, 100, 14, 0, Math.PI); ctx.lineTo(18, 34);
    ctx.stroke();
    ctx.strokeStyle = 'rgba(255,255,255,0.5)'; ctx.lineWidth = 2; ctx.stroke();
    ctx.restore();
  };

  // ---------- paper ----------
  K.paper = (() => {
    let cache = null;
    return () => {
      if (cache) return cache;
      const c = K.makeCanvas(), x = c.getContext('2d'), r = K.rng(7);
      x.fillStyle = '#d9cdb1'; x.fillRect(0, 0, W, H);
      for (let i = 0; i < 70; i++) { // stains
        const gx = r() * W, gy = r() * H, rad = 60 + r() * 380;
        const g = x.createRadialGradient(gx, gy, 0, gx, gy, rad);
        const a = 0.03 + r() * 0.07;
        g.addColorStop(0, `rgba(120,90,50,${a})`); g.addColorStop(1, 'rgba(120,90,50,0)');
        x.fillStyle = g; x.fillRect(gx - rad, gy - rad, rad * 2, rad * 2);
      }
      x.globalAlpha = 0.18; x.globalCompositeOperation = 'multiply';
      for (let yy = 0; yy < H; yy += 512) for (let xx = 0; xx < W; xx += 512) x.drawImage(K.noise[0], xx, yy);
      x.globalCompositeOperation = 'source-over'; x.globalAlpha = 1;
      const v = x.createRadialGradient(W / 2, H / 2, H * 0.3, W / 2, H / 2, H * 0.75);
      v.addColorStop(0, 'rgba(70,45,20,0)'); v.addColorStop(1, 'rgba(70,45,20,0.45)');
      x.fillStyle = v; x.fillRect(0, 0, W, H);
      x.strokeStyle = 'rgba(90,70,50,0.25)'; x.lineWidth = 2; // fold line
      x.beginPath(); x.moveTo(0, H * 0.52); x.lineTo(W, H * 0.515); x.stroke();
      cache = c; return c;
    };
  })();

  // ---------- text ----------
  K.wrap = (ctx, text, maxW) => {
    const out = [];
    for (const para of text.split('\n')) {
      let line = '';
      for (const word of para.split(' ')) {
        const test = line ? line + ' ' + word : word;
        if (ctx.measureText(test).width > maxW && line) { out.push(line); line = word; } else line = test;
      }
      out.push(line);
    }
    return out;
  };
  // Typewriter reveal. Wrapping is computed on the full text so lines never jump.
  K.type = (ctx, text, t, t0, o = {}) => {
    const cps = o.cps || 22;
    const n = Math.floor(K.clamp((t - t0) * cps, 0, text.length));
    if (t < t0) return false;
    ctx.save();
    ctx.font = o.font || '46px "PT Mono"';
    ctx.fillStyle = o.color || '#ece4d4';
    ctx.textAlign = o.align || 'left'; ctx.textBaseline = 'alphabetic';
    if (o.shadow !== false) { ctx.shadowColor = o.shadowColor || 'rgba(0,0,0,0.8)'; ctx.shadowBlur = o.shadowBlur ?? 12; }
    ctx.globalAlpha *= o.alpha ?? 1;
    const lines = K.wrap(ctx, text, o.maxW || 900), lh = o.lh || 64;
    let left = n, x = o.x ?? 90, y = o.y ?? 1500;
    let cx = x, cy = y;
    for (let i = 0; i < lines.length; i++) {
      const L = lines[i], part = L.slice(0, Math.max(0, left));
      if (left > 0) {
        if (o.align === 'center') {
          // keep characters in their final positions while revealing
          const full = ctx.measureText(L).width, sx = x - full / 2;
          ctx.textAlign = 'left'; ctx.fillText(part, sx, y + i * lh); ctx.textAlign = 'center';
          cx = sx + ctx.measureText(part).width;
        } else { ctx.fillText(part, x, y + i * lh); cx = x + ctx.measureText(part).width; }
        cy = y + i * lh;
      }
      left -= L.length + 1;
    }
    if (o.cursor !== false && n < text.length + 1 && Math.floor(t * 2.2) % 2 === 0) {
      ctx.fillRect(cx + 4, cy + 6, 24, 4);
    }
    ctx.restore();
    return n >= text.length;
  };
  K.text = (ctx, text, x, y, o = {}) => {
    ctx.save();
    ctx.font = o.font || '46px "PT Mono"'; ctx.fillStyle = o.color || '#ece4d4';
    ctx.textAlign = o.align || 'center'; ctx.globalAlpha *= o.alpha ?? 1;
    if (o.letter) ctx.letterSpacing = o.letter;
    if (o.shadow !== false) { ctx.shadowColor = o.shadowColor || 'rgba(0,0,0,0.85)'; ctx.shadowBlur = o.shadowBlur ?? 14; }
    const lines = o.maxW ? K.wrap(ctx, text, o.maxW) : text.split('\n');
    lines.forEach((l, i) => ctx.fillText(l, x, y + i * (o.lh || 64)));
    ctx.restore();
  };
  // A word rendered once per font/colour/shadow and reused: text rasterisation is the slow part.
  K.glyphCache = new Map();
  K.glyphs = (ctx, w) => {
    const key = `${ctx.font}|${ctx.fillStyle}|${ctx.shadowBlur}|${w}`;
    let g = K.glyphCache.get(key);
    if (!g) {
      const size = Number((ctx.font.match(/(\d+)px/) || [0, 50])[1]), pad = 24 + ctx.shadowBlur, asc = size * 1.1 + pad;
      const c = K.makeCanvas(Math.ceil(ctx.measureText(w).width + pad * 2), Math.ceil(size * 1.5 + pad * 2)), x = c.getContext('2d');
      x.font = ctx.font; x.fillStyle = ctx.fillStyle; x.shadowColor = ctx.shadowColor; x.shadowBlur = ctx.shadowBlur;
      x.fillText(w, pad, asc);
      g = { c, pad, asc }; K.glyphCache.set(key, g);
    }
    return g;
  };
  // Word-by-word reveal: each word fades in and rises slightly.
  K.words = (ctx, text, t, t0, o = {}) => {
    if (t < t0) return;
    ctx.save();
    ctx.font = o.font || 'italic 58px "Cormorant Garamond"';
    ctx.fillStyle = o.color || '#efe6d6'; ctx.textAlign = 'left';
    // per-word shadowBlur is very slow in the software canvas, so it is opt-in
    if (o.shadow) { ctx.shadowColor = 'rgba(0,0,0,0.9)'; ctx.shadowBlur = 16; }
    const lines = K.wrap(ctx, text, o.maxW || 900), lh = o.lh || 72, wps = o.wps || 0.16, base = ctx.globalAlpha * (o.alpha ?? 1);
    let k = 0;
    lines.forEach((L, i) => {
      const lw = ctx.measureText(L).width;
      let x = o.align === 'left' ? (o.x ?? 90) : (o.x ?? W / 2) - lw / 2;
      for (const w of L.split(' ')) {
        const p = K.ease.outCubic(K.clamp((t - t0 - k * wps) / 0.5));
        if (p > 0) {
          const g = K.glyphs(ctx, w);
          ctx.globalAlpha = base * p; ctx.drawImage(g.c, x - g.pad, (o.y ?? 1500) + i * lh + (1 - p) * 14 - g.asc);
        }
        x += ctx.measureText(w + ' ').width; k++;
      }
    });
    ctx.restore();
  };
  // Red/cyan channel split of a full-frame layer (exact image when dx = 0).
  K.chroma = (() => {
    let r, c;
    return (ctx, layer, dx) => {
      if (!r) { r = K.makeCanvas(); c = K.makeCanvas(); }
      for (const [cv, col] of [[r, '#f00'], [c, '#0ff']]) {
        const x = cv.getContext('2d');
        x.globalCompositeOperation = 'source-over'; x.drawImage(layer, 0, 0);
        x.globalCompositeOperation = 'multiply'; x.fillStyle = col; x.fillRect(0, 0, W, H);
      }
      ctx.save(); ctx.fillStyle = '#000'; ctx.fillRect(0, 0, W, H);
      ctx.globalCompositeOperation = 'lighter';
      ctx.drawImage(r, -dx, 0); ctx.drawImage(c, dx, 0);
      ctx.restore();
    };
  })();
  // Warm light leak sweeping across the frame.
  K.leak = (ctx, t, t0, dur, o = {}) => {
    const p = K.prog(t, t0, t0 + dur);
    if (p <= 0 || p >= 1) return;
    const a = Math.sin(p * Math.PI) * (o.alpha ?? 0.55);
    const x = K.lerp(-300, W + 300, p), y = o.y ?? H * 0.45;
    ctx.save(); ctx.globalCompositeOperation = 'screen';
    for (const [dx, rad, col] of [[0, 900, '255,140,60'], [-200, 500, '255,220,170'], [260, 700, '220,60,30']]) {
      const g = ctx.createRadialGradient(x + dx, y, 0, x + dx, y, rad);
      g.addColorStop(0, `rgba(${col},${a})`); g.addColorStop(1, `rgba(${col},0)`);
      ctx.fillStyle = g; ctx.fillRect(0, 0, W, H);
    }
    ctx.restore();
  };
  K.rain = (ctx, t, o = {}) => {
    const n = o.n || 160, r = K.rng(o.seed || 5);
    ctx.save(); ctx.strokeStyle = o.color || 'rgba(200,210,230,0.35)'; ctx.lineWidth = 2;
    for (let i = 0; i < n; i++) {
      const sp = 1400 + r() * 900, len = 30 + r() * 50, x0 = r() * W, y0 = r() * H;
      const y = (y0 + t * sp) % (H + 100) - 50, x = x0 - (y / H) * 60;
      ctx.beginPath(); ctx.moveTo(x, y); ctx.lineTo(x - 6, y + len); ctx.stroke();
    }
    ctx.restore();
  };

  // Rubber stamp that slams down at t0.
  K.stamp = (ctx, text, t, t0, o = {}) => {
    if (t < t0) return;
    const p = K.clamp((t - t0) / 0.22), e = K.ease.outCubic(p);
    const s = K.lerp(2.4, 1, e), a = K.clamp(p * 1.6) * (o.alpha ?? 0.82);
    ctx.save();
    ctx.translate(o.x ?? W / 2, o.y ?? H / 2); ctx.rotate((o.rot ?? -12) * Math.PI / 180); ctx.scale(s, s);
    ctx.globalAlpha = a; ctx.strokeStyle = ctx.fillStyle = o.color || '#9b1c1c';
    ctx.font = `bold ${o.size || 92}px "PT Mono"`; ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
    if (o.letter) ctx.letterSpacing = o.letter;
    const w = ctx.measureText(text).width + 70, h = (o.size || 92) * 1.45;
    ctx.lineWidth = 9; ctx.strokeRect(-w / 2, -h / 2, w, h);
    ctx.lineWidth = 3; ctx.strokeRect(-w / 2 + 14, -h / 2 + 14, w - 28, h - 28);
    ctx.fillText(text, 0, 4);
    // worn ink: punch holes through the stamp with noise
    ctx.globalCompositeOperation = 'destination-out'; ctx.globalAlpha = 0.5;
    ctx.drawImage(K.noise[1], -w / 2, -h / 2, w, h);
    ctx.restore();
  };

  // ---------- effects ----------
  K.grain = (ctx, frame, amount = 0.12) => {
    const n = K.noise[frame % 4], r = K.rng(frame * 31 + 5);
    ctx.save(); ctx.globalAlpha = amount; ctx.globalCompositeOperation = 'overlay';
    const ox = -(r() * 512) | 0, oy = -(r() * 512) | 0;
    for (let y = oy; y < H; y += 512) for (let x = ox; x < W; x += 512) ctx.drawImage(n, x, y);
    ctx.restore();
  };
  K.vignette = (ctx, strength = 0.65) => {
    const g = ctx.createRadialGradient(W / 2, H / 2, H * 0.25, W / 2, H / 2, H * 0.72);
    g.addColorStop(0, 'rgba(0,0,0,0)'); g.addColorStop(1, `rgba(0,0,0,${strength})`);
    ctx.fillStyle = g; ctx.fillRect(0, 0, W, H);
  };
  K.scratches = (ctx, frame, amount = 1) => {
    const r = K.rng(frame * 17 + 3);
    ctx.save();
    const n = Math.floor(r() * 3 * amount);
    for (let i = 0; i < n; i++) {
      ctx.strokeStyle = `rgba(255,250,235,${0.08 + r() * 0.18})`; ctx.lineWidth = 1 + r() * 2;
      const x = r() * W; ctx.beginPath(); ctx.moveTo(x, r() * H * 0.3); ctx.lineTo(x + (r() - 0.5) * 20, H * (0.6 + r() * 0.4)); ctx.stroke();
    }
    const d = Math.floor(r() * 7 * amount); // dust specks
    for (let i = 0; i < d; i++) {
      ctx.fillStyle = r() > 0.5 ? 'rgba(0,0,0,0.55)' : 'rgba(255,250,235,0.35)';
      ctx.beginPath(); ctx.ellipse(r() * W, r() * H, 1 + r() * 4, 1 + r() * 2.5, r() * 3, 0, 7); ctx.fill();
    }
    ctx.restore();
  };
  K.flicker = frame => 1 - K.hash(frame, 1) * 0.06;
  K.shake = (t, t0, dur, amp) => {
    if (t < t0 || t > t0 + dur) return { x: 0, y: 0 };
    const k = 1 - (t - t0) / dur, f = Math.floor(t * 60);
    return { x: (K.hash(f, 2) - 0.5) * 2 * amp * k, y: (K.hash(f, 3) - 0.5) * 2 * amp * k };
  };
  K.snow = (ctx, t, o = {}) => {
    const n = o.n || 140, r = K.rng(o.seed || 11);
    ctx.save(); ctx.fillStyle = o.color || 'rgba(240,240,255,0.8)';
    for (let i = 0; i < n; i++) {
      const sp = 40 + r() * 120, sz = 1 + r() * (o.size || 4), x0 = r() * W, y0 = r() * H, ph = r() * 6;
      const y = (y0 + t * sp) % (H + 20) - 10, x = (x0 + Math.sin(t * 0.8 + ph) * 30 + t * (o.wind || -30) + W * 10) % W;
      ctx.globalAlpha = (o.alpha || 0.8) * (0.3 + 0.7 * (sz / ((o.size || 4) + 1)));
      ctx.beginPath(); ctx.arc(x, y, sz, 0, 7); ctx.fill();
    }
    ctx.restore();
  };
  K.embers = (ctx, t, o = {}) => {
    const n = o.n || 70, r = K.rng(o.seed || 23);
    ctx.save(); ctx.globalCompositeOperation = 'lighter';
    for (let i = 0; i < n; i++) {
      const sp = 30 + r() * 90, sz = 1 + r() * 3.5, x0 = r() * W, y0 = r() * H, ph = r() * 6;
      const y = H - ((H - y0 + t * sp) % (H + 40)), x = x0 + Math.sin(t * 1.3 + ph) * 40;
      const fl = 0.5 + 0.5 * Math.sin(t * 7 + ph * 3);
      ctx.fillStyle = `rgba(255,${120 + (r() * 80) | 0},40,${(o.alpha || 0.7) * fl})`;
      ctx.beginPath(); ctx.arc(x, y, sz, 0, 7); ctx.fill();
    }
    ctx.restore();
  };
  // ECG trace drawn left to right over [t0, t0+dur]; flat=true draws a flatline.
  K.ecg = (ctx, t, t0, dur, o = {}) => {
    if (t < t0) return;
    const p = K.clamp((t - t0) / dur), y0 = o.y ?? 1650, amp = o.amp ?? 90, x1 = W * p;
    ctx.save();
    ctx.strokeStyle = o.color || 'rgba(140,255,170,0.9)'; ctx.lineWidth = 4;
    ctx.shadowColor = o.color || 'rgba(140,255,170,0.9)'; ctx.shadowBlur = 18; ctx.globalAlpha *= o.alpha ?? 1;
    ctx.beginPath(); ctx.moveTo(0, y0);
    const beat = o.beat || 360;
    for (let x = 0; x <= x1; x += 3) {
      const ph = (x % beat) / beat;
      let y = 0;
      if (!o.flat) {
        if (ph > 0.40 && ph < 0.44) y = -0.15 * Math.sin((ph - 0.40) / 0.04 * Math.PI);
        else if (ph > 0.48 && ph < 0.50) y = 0.25 * (ph - 0.48) / 0.02;
        else if (ph >= 0.50 && ph < 0.53) y = 0.25 - 1.25 * (ph - 0.50) / 0.03;
        else if (ph >= 0.53 && ph < 0.56) y = -1 + 1.4 * (ph - 0.53) / 0.03;
        else if (ph >= 0.56 && ph < 0.58) y = 0.4 - 0.4 * (ph - 0.56) / 0.02;
        else if (ph > 0.66 && ph < 0.76) y = -0.22 * Math.sin((ph - 0.66) / 0.10 * Math.PI);
      }
      ctx.lineTo(x, y0 + y * amp);
    }
    ctx.stroke();
    // bright head
    ctx.fillStyle = '#eaffef'; ctx.beginPath(); ctx.arc(x1, y0, 7, 0, 7); ctx.fill();
    ctx.restore();
  };

  // ---------- scene runner ----------
  // scenes: [{a, b, fi, fo, draw(ctx, t, s)}]; overlapping scenes cross-fade.
  K.runScenes = (ctx, scenes, t) => {
    ctx.fillStyle = '#000'; ctx.fillRect(0, 0, W, H);
    let li = 0;
    for (const s of scenes) {
      const a = K.window(t, s.a, s.b, s.fi ?? 0.8, s.fo ?? 0.8);
      if (a <= 0) continue;
      const L = K.layers[li++ % K.layers.length], lx = L.getContext('2d');
      lx.setTransform(1, 0, 0, 1, 0, 0); lx.globalAlpha = 1; lx.globalCompositeOperation = 'source-over'; lx.filter = 'none';
      lx.fillStyle = '#000'; lx.fillRect(0, 0, W, H);
      s.draw(lx, t, { p: K.prog(t, s.a, s.b), lt: t - s.a, dur: s.b - s.a });
      ctx.globalAlpha = a; ctx.drawImage(L, 0, 0); ctx.globalAlpha = 1;
    }
  };

  window.K = K;
})();
