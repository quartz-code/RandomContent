// Version H — «Цена». Same template track and cut grid (37.8 s), space style, new context:
// Aizen writes at night and asks who a hero is; the red line "(герой тот, кто готов
// пожертвовать собой)" lands on the drop, then a star chronicle of legends — sword, shield,
// dragon, runes, phoenix, dying stars, scales — each one a different kind of sacrifice.
(function () {
  const { W, H } = K;
  const KICKS = [17.54, 18.82, 19.27, 19.74, 20.40, 20.83, 21.33, 21.79, 22.42, 23.20, 23.37, 23.82, 24.11, 24.43, 26.61, 26.91,
    28.82, 29.27, 29.73, 30.82, 31.32, 31.80, 32.26, 32.43, 32.57, 33.19, 33.38, 33.61, 33.82, 34.42, 35.05, 35.26, 36.29, 37.34];
  const HATS = [17.88, 18.04, 18.18, 19.99, 20.17, 21.62, 22.26, 22.56, 22.87, 23.19, 23.42, 23.65, 25.39, 26.16, 26.31, 27.17,
    27.41, 27.57, 27.88, 28.05, 28.55, 29.91, 30.06, 30.38, 31.63, 31.78, 33.96, 34.67, 34.92, 35.38, 36.93];
  const since = (list, t) => { let b = 1e9; for (const k of list) if (k <= t) b = Math.min(b, t - k); return b; };
  const kick = t => Math.exp(-since(KICKS, t) * 8);
  const count = (list, t, a, b) => list.filter(k => k >= a && k <= t && k < b).length;
  const e = (t, a, b) => K.ease.inOutSine(K.prog(t, a, b));
  const rgb = (c, a = 1) => `rgba(${c[0]},${c[1]},${c[2]},${a})`;
  const PINK = [255, 90, 150], LAV = [200, 190, 255], CYAN = [120, 220, 255];

  // ---------------- space backdrop ----------------
  const blob = (col, r) => {
    const c = K.makeCanvas(r * 2, r * 2), x = c.getContext('2d'), g = x.createRadialGradient(r, r, 0, r, r, r);
    g.addColorStop(0, rgb(col, 0.6)); g.addColorStop(1, rgb(col, 0)); x.fillStyle = g; x.fillRect(0, 0, r * 2, r * 2); return c;
  };
  let B = null;
  const blobs = () => B || (B = { v: blob([90, 50, 200], 760), n: blob([30, 70, 170], 820), t: blob([20, 130, 150], 640), p: blob([200, 50, 130], 600) });
  const STARS = (() => { const r = K.rng(42), a = []; for (let i = 0; i < 420; i++) a.push({ x: r() * W, y: r() * H, z: 0.2 + r() * 0.8, ph: r() * 6, tw: 0.5 + r() * 2.5 }); return a; })();
  const space = (ctx, t, o = {}) => {
    ctx.fillStyle = '#04030b'; ctx.fillRect(0, 0, W, H);
    const b = blobs();
    ctx.save(); ctx.globalCompositeOperation = 'screen';
    const dr = (img, x, y, a) => { ctx.globalAlpha = a; ctx.drawImage(img, x - img.width / 2, y - img.height / 2); };
    dr(b.v, 320 + Math.sin(t * 0.13) * 160, 620 + Math.cos(t * 0.09) * 200, 0.85);
    dr(b.n, 800 + Math.cos(t * 0.1) * 150, 1250 + Math.sin(t * 0.07) * 220, 0.8);
    dr(b.t, 520 + Math.sin(t * 0.06 + 2) * 260, 1700, 0.55);
    dr(b.p, 200 + Math.cos(t * 0.08) * 120, 1500, 0.35);
    ctx.restore();
    const warp = o.warp || 0;
    ctx.save();
    for (const s of STARS) {
      const tw = 0.5 + 0.5 * Math.sin(t * s.tw + s.ph);
      if (warp > 0.02) { // hyperspace streaks radiating from the centre
        const dx = s.x - W / 2, dy = s.y - H / 2, d = Math.hypot(dx, dy) + 1, k = ((t * (0.6 + s.z) * 0.9 + s.ph) % 1);
        const px = W / 2 + dx * (0.2 + k * 1.6), py = H / 2 + dy * (0.2 + k * 1.6), L = 40 + warp * 260 * s.z * k;
        ctx.strokeStyle = `rgba(220,225,255,${(0.25 + 0.6 * s.z) * Math.min(1, warp * 1.6) * k})`; ctx.lineWidth = 1 + s.z * 1.6;
        ctx.beginPath(); ctx.moveTo(px, py); ctx.lineTo(px - dx / d * L, py - dy / d * L); ctx.stroke();
      }
      ctx.fillStyle = `rgba(230,232,255,${(0.25 + 0.65 * s.z) * tw * (1 - warp * 0.6)})`;
      ctx.beginPath(); ctx.arc(s.x, s.y, 0.6 + s.z * 1.7, 0, 7); ctx.fill();
    }
    ctx.restore();
  };

  // ---------------- text helpers ----------------
  const big = (ctx, s, x, y, o = {}) => { // heavy glowing title, cached per string
    ctx.save(); ctx.font = `800 ${o.size || 120}px Manrope`; ctx.fillStyle = o.color || '#f2f0ff';
    ctx.shadowColor = o.glow || 'rgba(150,140,255,0.95)'; ctx.shadowBlur = o.blur ?? 40;
    const g = K.glyphs(ctx, s); ctx.shadowBlur = 0;
    const sc = o.scale || 1, w = g.c.width - g.pad * 2;
    ctx.globalAlpha *= o.alpha ?? 1;
    ctx.translate(x, y); ctx.scale(sc, sc);
    ctx.drawImage(g.c, -w / 2 - g.pad, -g.asc);
    ctx.restore();
  };
  // legible subtitle: words rise in one by one, heavier italic, dark halo baked into cached glyphs
  const sub = (ctx, s, t, t0, y, o = {}) => {
    if (t < t0) return;
    ctx.save(); ctx.shadowColor = 'rgba(0,0,10,0.95)';
    K.words(ctx, s, t, t0, { y, font: `italic 600 ${o.size || 62}px "Cormorant Garamond"`, color: o.color || '#f3f0ff', wps: o.wps || 0.07, shadow: true, maxW: 940, lh: 74, alpha: o.alpha ?? 1 });
    ctx.restore();
  };
  // terminal line with a blinking block cursor
  const term = (ctx, s, t, t0, x, y, o = {}) => {
    if (t < t0) return;
    const n = Math.floor(K.clamp((t - t0) * (o.cps || 32), 0, s.length)), str = s.slice(0, n);
    ctx.save(); ctx.font = `${o.size || 42}px "PT Mono"`; ctx.textAlign = 'left'; ctx.fillStyle = o.color || '#ecebff';
    ctx.shadowColor = 'rgba(0,0,10,0.9)'; ctx.shadowBlur = 0; ctx.fillText(str, x, y);
    if (Math.floor(t * 3) % 2 === 0 || n < s.length) ctx.fillRect(x + ctx.measureText(str).width + 6, y - 34, 22, 40);
    ctx.restore();
  };
  // title that flies in letter by letter (each glyph cached), with a small overshoot
  const title = (ctx, s, x, y, t, t0, o = {}) => {
    if (t < t0) return;
    ctx.save(); ctx.font = o.font ? o.font.replace('$', o.size || 120) : `800 ${o.size || 120}px Manrope`; ctx.fillStyle = o.color || '#f2f0ff';
    ctx.shadowColor = o.glow || 'rgba(150,140,255,0.95)'; ctx.shadowBlur = o.blur ?? 36;
    const chars = [...s], widths = chars.map(c => ctx.measureText(c).width), total = widths.reduce((a, b) => a + b, 0);
    let cx = x - total / 2;
    chars.forEach((c, i) => {
      const lt = t - t0 - i * (o.stagger ?? 0.035), q = K.clamp(lt / 0.32);
      if (q > 0 && c !== ' ') {
        const g = K.glyphs(ctx, c), e2 = K.ease.outBack(q), fl = Math.sin(t * 2 + i * 0.6) * (o.float ?? 4);
        ctx.save(); ctx.globalAlpha = Math.min(1, q * 1.5) * (o.alpha ?? 1); ctx.shadowBlur = 0;
        ctx.translate(cx + widths[i] / 2, y + (1 - e2) * 60 + fl); ctx.scale(0.6 + 0.4 * e2, 0.6 + 0.4 * e2); ctx.rotate((1 - e2) * 0.25);
        ctx.drawImage(g.c, -widths[i] / 2 - g.pad, -g.asc); ctx.restore();
      }
      cx += widths[i];
    });
    ctx.restore();
  };
  // particles thrown out from a point at time t0
  const debris = (ctx, x, y, t, t0, o = {}) => {
    const q = (t - t0) / (o.life || 1.2); if (q < 0 || q > 1) return;
    const r = K.rng(o.seed || 7);
    for (let i = 0; i < (o.n || 70); i++) {
      const a = r() * 6.283, sp = (o.speed || 700) * (0.3 + r()), d = sp * q * (1 - q * 0.35);
      glowDot(ctx, x + Math.cos(a) * d, y + Math.sin(a) * d, (o.size || 14) * (1 - q * 0.6), o.col || [255, 200, 140], (1 - q) * 0.9);
    }
  };
  const ring = (ctx, x, y, t, t0, o = {}) => {
    const q = (t - t0) / (o.life || 0.8); if (q < 0 || q > 1) return;
    ctx.strokeStyle = rgb(o.col || [255, 230, 210], (1 - q) * 0.9); ctx.lineWidth = (o.w || 8) * (1 - q) + 1;
    ctx.beginPath(); ctx.arc(x, y, (o.r0 || 40) + (o.r1 || 700) * K.ease.outCubic(q), 0, 7); ctx.stroke();
  };
  // small spinning background galaxies for depth
  const farGalaxies = (ctx, t, n = 5) => { const r = K.rng(17); for (let i = 0; i < n; i++) { const x = r() * W, y = 300 + r() * 1300, R = 50 + r() * 70; ctx.save(); ctx.globalAlpha = 0.45; galaxy(ctx, x, y, R, t * (0.3 + r()) * (i % 2 ? 1 : -1), i % 2 ? [255, 140, 200] : [140, 200, 255], 0.4 + r() * 0.3); ctx.restore(); } };
  const hud = (ctx, t, info) => {
    ctx.save(); ctx.font = '24px "PT Mono"'; ctx.fillStyle = 'rgba(210,205,255,0.55)';
    ctx.textAlign = 'left'; ctx.fillText('ХРОНИКА ЛЕГЕНД / 03:12', 60, 110);
    ctx.textAlign = 'right'; ctx.fillText(info.sig || 'МАНА 3%', W - 60, 110);
    ctx.textAlign = 'left'; ctx.fillText(info.ra || 'RA 20.36h  DEC -26.4°', 60, H - 90);
    ctx.textAlign = 'right'; ctx.fillText(info.cnt || 'ГЕРОЕВ В ЗОНЕ: 0', W - 60, H - 90);
    ctx.fillStyle = 'rgba(210,205,255,0.2)'; ctx.fillRect(60, 130, W - 120, 1); ctx.fillRect(60, H - 130, W - 120, 1);
    ctx.restore();
  };
  const glowDot = (ctx, x, y, r, col, a = 1) => {
    const g = ctx.createRadialGradient(x, y, 0, x, y, r); g.addColorStop(0, rgb([255, 255, 255], a)); g.addColorStop(0.25, rgb(col, a * 0.9)); g.addColorStop(1, rgb(col, 0));
    ctx.fillStyle = g; ctx.beginPath(); ctx.arc(x, y, r, 0, 7); ctx.fill();
  };

  // ---------------- chat (0..17.17) ----------------
  const CHAT = [ // Aizen writes (left); our replies on the right
    { t: 0.6, s: 'не спится', me: false },
    { t: 2.8, s: 'мне тоже. что такое?', me: true, typing: 2.0 },
    { t: 5.4, s: 'а кто такой герой?', me: false },
    { t: 8.0, s: 'к чему вопрос?', me: true, typing: 7.0 },
    { t: 10.6, s: 'просто ответь.', me: false },
    { t: 12.8, s: 'ну… тот, кто сильнее всех?', me: true, typing: 11.8 },
    { t: 16.4, s: '(герой тот, кто готов пожертвовать собой)', me: false, typing: 15.2, pill: true },
  ];
  const BMAX = 680, cache = new Map();
  const bubble = m => {
    if (cache.has(m.s)) return cache.get(m.s);
    const mm = K.makeCanvas(10, 10).getContext('2d'); mm.font = '600 46px Manrope';
    const lines = K.wrap(mm, m.s, BMAX - 80), tw = Math.max(...lines.map(l => mm.measureText(l).width));
    const w = Math.ceil(tw + 80), h = Math.ceil(lines.length * 58 + 48), G = 60;
    const c = K.makeCanvas(w + G * 2, h + G * 2), x = c.getContext('2d'); x.translate(G, G);
    if (m.pill) {
      x.shadowColor = 'rgba(255,40,60,0.95)'; x.shadowBlur = 44; x.strokeStyle = '#ff3b4e'; x.lineWidth = 5;
      x.beginPath(); x.roundRect(3, 3, w - 6, h - 6, 44); x.stroke(); x.shadowBlur = 0;
      x.fillStyle = 'rgba(40,4,10,0.8)'; x.beginPath(); x.roundRect(6, 6, w - 12, h - 12, 40); x.fill();
    } else if (m.me) {
      const g = x.createLinearGradient(0, 0, w, h); g.addColorStop(0, '#5160ff'); g.addColorStop(0.55, '#7d5cf6'); g.addColorStop(1, '#3fb3ff');
      x.shadowColor = 'rgba(120,110,255,0.6)'; x.shadowBlur = 40; x.fillStyle = g; x.beginPath(); x.roundRect(0, 0, w, h, 40); x.fill(); x.shadowBlur = 0;
      const hl = x.createLinearGradient(0, 0, 0, h); hl.addColorStop(0, 'rgba(255,255,255,0.25)'); hl.addColorStop(0.5, 'rgba(255,255,255,0)');
      x.fillStyle = hl; x.beginPath(); x.roundRect(0, 0, w, h, 40); x.fill();
    } else {
      x.fillStyle = 'rgba(255,255,255,0.13)'; x.beginPath(); x.roundRect(0, 0, w, h, 40); x.fill();
      x.strokeStyle = 'rgba(255,255,255,0.16)'; x.lineWidth = 2; x.stroke();
    }
    x.font = m.pill ? '500 46px Manrope' : '600 46px Manrope'; x.fillStyle = m.pill ? '#ffe3e6' : '#fff'; x.textBaseline = 'middle'; x.textAlign = m.pill ? 'center' : 'left';
    lines.forEach((l, i) => x.fillText(l, m.pill ? w / 2 : 40, 24 + 29 + i * 58));
    const b = { c, w, h, G }; cache.set(m.s, b); return b;
  };
  const spring = x => x <= 0 ? 0 : 1 - Math.exp(-8 * x) * Math.cos(10 * x);
  const chat = (ctx, t, frame) => {
    const warp = e(t, 10.0, 15.8) * (1 - e(t, 16.3, 16.9));
    space(ctx, t, { warp });
    // the whole chat floats and tilts in pseudo-3D
    const rot = (-4 + 2.4 * Math.sin(t * 0.35) + 3 * warp * Math.sin(t * 1.3)) * Math.PI / 180;
    const sk = 0.05 * Math.sin(t * 0.27), sc = 1.04 + 0.04 * Math.sin(t * 0.2) + 0.06 * warp;
    ctx.save(); ctx.translate(W / 2, H / 2 + Math.sin(t * 0.6) * 12); ctx.transform(1, sk * 0.3, sk, 1, 0, 0); ctx.rotate(rot); ctx.scale(sc, sc); ctx.translate(-W / 2, -H / 2);
    // header
    const ha = K.prog(t, 0, 0.5);
    ctx.save(); ctx.globalAlpha = ha;
    const ag = ctx.createLinearGradient(W / 2 - 50, 230, W / 2 + 50, 330); ag.addColorStop(0, '#8a6bff'); ag.addColorStop(1, '#3b2a8f');
    ctx.fillStyle = ag; ctx.beginPath(); ctx.arc(W / 2, 280, 52, 0, 7); ctx.fill();
    ctx.font = '700 50px Manrope'; ctx.fillStyle = '#fff'; ctx.textAlign = 'center'; ctx.fillText('А', W / 2, 298); // Айзен
    ctx.font = '600 34px Manrope'; ctx.fillText('Айзен', W / 2, 382); ctx.font = '500 24px Manrope'; ctx.fillStyle = 'rgba(200,200,230,0.7)'; ctx.fillText('в сети', W / 2, 416);
    ctx.fillText('Сегодня 3:12', W / 2, 480); ctx.restore();
    // messages, newest at the bottom pushing the rest up
    const vis = []; for (const m of CHAT) if (t >= m.t) vis.push({ m, p: spring(t - m.t) });
    const typingNow = CHAT.find(m => m.typing !== undefined && t >= m.typing && t < m.t);
    let y = 1480;
    if (typingNow) y -= 110 * K.clamp((t - typingNow.typing) / 0.2);
    const pos = [];
    for (let i = vis.length - 1; i >= 0; i--) { const b = bubble(vis[i].m); pos[i] = y; y -= (b.h + 30) * Math.min(1, vis[i].p); }
    vis.forEach(({ m, p }, i) => {
      const b = bubble(m), yb = pos[i], a = K.clamp(p * 1.5) * K.clamp((yb - 520) / 200);
      if (a <= 0) return;
      const right = W - 120, left = 120, x0 = m.pill ? W / 2 - b.w / 2 : m.me ? right - b.w : left;
      const s = 0.8 + 0.2 * Math.min(1.06, p), ax = m.pill ? W / 2 : m.me ? right : left;
      ctx.save(); ctx.globalAlpha = a; ctx.translate(ax, yb); ctx.scale(s, s); ctx.translate(-ax, -yb);
      if (m.pill && t > 16.85) { // breaks apart into sparks
        const q = K.prog(t, 16.85, 17.17), r = K.rng(9);
        for (let k = 0; k < 120; k++) { glowDot(ctx, x0 + r() * b.w + (r() - 0.5) * 400 * q, yb - b.h + r() * b.h + (r() - 0.5) * 300 * q, 10, [255, 50, 70], 1 - q); }
      } else ctx.drawImage(b.c, x0 - b.G, yb - b.h - b.G);
      const sp = K.prog(t, m.t + 0.05, m.t + 0.6);
      if (sp > 0 && sp < 1 && !m.pill) {
        ctx.beginPath(); ctx.roundRect(x0, yb - b.h, b.w, b.h, 40); ctx.clip();
        const sx = K.lerp(x0 - 150, x0 + b.w + 150, sp), g = ctx.createLinearGradient(sx - 90, 0, sx + 90, 0);
        g.addColorStop(0, 'rgba(255,255,255,0)'); g.addColorStop(0.5, 'rgba(255,255,255,0.45)'); g.addColorStop(1, 'rgba(255,255,255,0)');
        ctx.fillStyle = g; ctx.fillRect(x0, yb - b.h, b.w, b.h);
      }
      ctx.restore();
      if (m.me && m === [...vis].reverse().find(v => v.m.me)?.m && !m.pill) {
        ctx.save(); ctx.font = '500 24px Manrope'; ctx.textAlign = 'right'; ctx.fillStyle = 'rgba(200,200,230,0.6)';
        ctx.fillText(t - m.t > 1.2 ? 'Прочитано' : 'Доставлено', right - 8, yb + 32); ctx.restore();
      }
    });
    if (typingNow) {
      const a = K.prog(t, typingNow.typing, typingNow.typing + 0.15), x0 = typingNow.pill ? W / 2 - 75 : typingNow.me ? W - 120 - 150 : 120;
      ctx.save(); ctx.globalAlpha = a; ctx.fillStyle = 'rgba(255,255,255,0.13)'; ctx.beginPath(); ctx.roundRect(x0, 1400, 150, 90, 45); ctx.fill();
      for (let k = 0; k < 3; k++) { const j = Math.max(0, Math.sin(t * 8 - k * 0.9)); ctx.fillStyle = `rgba(235,235,255,${0.4 + 0.6 * j})`; ctx.beginPath(); ctx.arc(x0 + 44 + k * 31, 1445 - j * 8, 10, 0, 7); ctx.fill(); }
      ctx.restore();
    }
    ctx.restore();
  };

  // ---------------- the observatory log (drop) ----------------
  const constellation = (ctx, C, cx, cy, s, t, t0, col) => {
    const n = C.edges.length, q = K.clamp((t - t0) / 0.9) * n;
    ctx.save(); ctx.strokeStyle = rgb(col, 0.7); ctx.lineWidth = 3;
    C.edges.forEach(([a, b], i) => {
      const f = K.clamp(q - i); if (f <= 0) return;
      const [ax, ay] = C.pts[a], [bx, by] = C.pts[b];
      ctx.beginPath(); ctx.moveTo(cx + ax * s, cy + ay * s); ctx.lineTo(cx + (ax + (bx - ax) * f) * s, cy + (ay + (by - ay) * f) * s); ctx.stroke();
    });
    C.pts.forEach(([x, y], i) => { if (t > t0 + i * 0.05) glowDot(ctx, cx + x * s, cy + y * s, 26, col, 0.9); });
    ctx.restore();
  };
  const GAL = (() => { const r = K.rng(5), a = []; for (let i = 0; i < 700; i++) { const arm = i % 2, d = Math.pow(r(), 0.7), ang = d * 7 + arm * Math.PI + (r() - 0.5) * 0.6; a.push({ d, ang, z: r() }); } return a; })();
  const galaxy = (ctx, x, y, R, rot, col, tilt = 0.45) => {
    ctx.save(); ctx.translate(x, y); ctx.scale(1, tilt); ctx.globalCompositeOperation = 'lighter';
    glowDot(ctx, 0, 0, R * 0.55, col, 0.8);
    for (const s of GAL) { const a = s.ang + rot, px = Math.cos(a) * s.d * R, py = Math.sin(a) * s.d * R; ctx.fillStyle = rgb(s.z > 0.7 ? [255, 255, 255] : col, 0.35 + 0.4 * (1 - s.d)); ctx.fillRect(px, py, 2.5, 2.5); }
    ctx.restore();
  };
  const moon = (ctx, x, y, r, light = 0.85) => {
    const g = ctx.createRadialGradient(x - r * 0.35, y - r * 0.35, r * 0.1, x, y, r);
    g.addColorStop(0, rgb([245, 244, 255], light)); g.addColorStop(0.7, rgb([150, 150, 175], light)); g.addColorStop(1, rgb([40, 40, 60], light));
    ctx.fillStyle = g; ctx.beginPath(); ctx.arc(x, y, r, 0, 7); ctx.fill();
    const r2 = K.rng(3); ctx.fillStyle = 'rgba(60,60,85,0.35)';
    for (let i = 0; i < 14; i++) { const a = r2() * 6.28, d = r2() * r * 0.75, cr = r * (0.05 + r2() * 0.13); ctx.beginPath(); ctx.arc(x + Math.cos(a) * d, y + Math.sin(a) * d, cr, 0, 7); ctx.fill(); }
  };

  // ---------------- a star chronicle of legends (drop) ----------------
  const GOLD = [255, 205, 120], RED = [255, 50, 70];
  const SERIF = '700 $px "Cormorant Garamond"';
  const gold = (ctx, s, x, y, t, t0, o = {}) => title(ctx, s, x, y, t, t0, { font: SERIF, color: '#fff1d6', glow: 'rgba(255,190,90,0.95)', ...o });

  const sword = (ctx, x, y, s, rot = 0, glow = 1) => {
    ctx.save(); ctx.translate(x, y); ctx.rotate(rot); ctx.scale(s, s);
    if (glow > 0) { ctx.globalCompositeOperation = 'lighter'; glowDot(ctx, 0, -120, 260, [180, 200, 255], 0.25 * glow); ctx.globalCompositeOperation = 'source-over'; }
    const bl = ctx.createLinearGradient(-16, 0, 16, 0); bl.addColorStop(0, '#8d97b8'); bl.addColorStop(0.5, '#f4f7ff'); bl.addColorStop(1, '#7a83a6');
    ctx.fillStyle = bl; ctx.beginPath(); ctx.moveTo(-16, -20); ctx.lineTo(-16, -360); ctx.lineTo(0, -410); ctx.lineTo(16, -360); ctx.lineTo(16, -20); ctx.closePath(); ctx.fill();
    ctx.fillStyle = 'rgba(255,255,255,0.6)'; ctx.fillRect(-2, -380, 4, 350);
    const gd = ctx.createLinearGradient(0, -30, 0, 0); gd.addColorStop(0, '#ffe2a0'); gd.addColorStop(1, '#b8862f');
    ctx.fillStyle = gd; ctx.beginPath(); ctx.roundRect(-90, -24, 180, 22, 10); ctx.fill();
    ctx.fillStyle = '#4a2e1a'; ctx.fillRect(-10, -2, 20, 90);
    ctx.fillStyle = '#d8a548'; ctx.beginPath(); ctx.arc(0, 100, 18, 0, 7); ctx.fill();
    ctx.restore();
  };
  const shieldPath = (ctx, x, y, w, h) => {
    ctx.beginPath(); ctx.moveTo(x - w / 2, y - h / 2); ctx.lineTo(x + w / 2, y - h / 2); ctx.lineTo(x + w / 2, y);
    ctx.quadraticCurveTo(x + w / 2, y + h * 0.35, x, y + h / 2); ctx.quadraticCurveTo(x - w / 2, y + h * 0.35, x - w / 2, y); ctx.closePath();
  };
  const runeMarks = (ctx, cx, cy, r, n, rot, col, a, seed) => { // procedural rune glyphs around a circle
    const rr = K.rng(seed);
    for (let i = 0; i < n; i++) {
      const ang = rot + i / n * 6.283, x = cx + Math.cos(ang) * r, y = cy + Math.sin(ang) * r;
      ctx.save(); ctx.translate(x, y); ctx.rotate(ang + Math.PI / 2); ctx.strokeStyle = rgb(col, a); ctx.lineWidth = 3;
      ctx.beginPath(); ctx.moveTo(0, -14); ctx.lineTo(0, 14);
      const k = Math.floor(rr() * 4);
      if (k === 0) { ctx.moveTo(0, -14); ctx.lineTo(9, -5); ctx.moveTo(0, -2); ctx.lineTo(9, 7); }
      else if (k === 1) { ctx.moveTo(-9, -8); ctx.lineTo(9, 8); }
      else if (k === 2) { ctx.moveTo(0, -4); ctx.lineTo(-9, -12); ctx.moveTo(0, -4); ctx.lineTo(9, -12); }
      else { ctx.moveTo(0, -14); ctx.lineTo(10, -2); ctx.lineTo(0, 10); }
      ctx.stroke(); ctx.restore();
    }
  };
  const DRAGON = { // a long serpent across the sky
    pts: [[-330, -380], [-250, -330], [-170, -360], [-90, -300], [-110, -200], [-40, -120], [60, -150], [150, -80], [120, 20], [200, 100], [140, 200], [40, 230], [-40, 320], [20, 420], [120, 460]],
    edges: [[0, 1], [1, 2], [2, 3], [3, 4], [4, 5], [5, 6], [6, 7], [7, 8], [8, 9], [9, 10], [10, 11], [11, 12], [12, 13], [13, 14], [1, 3]],
  };
  const KNIGHT = { pts: [[0, -70], [0, 0], [-30, 70], [30, 70], [-40, -20], [45, -60], [70, -110]], edges: [[0, 1], [1, 2], [1, 3], [1, 4], [1, 5], [5, 6]] };
  // phoenix: wing curves of fire particles, flapping
  const BIRD = (() => { const r = K.rng(31), a = []; for (let i = 0; i < 260; i++) a.push({ u: r(), part: r() < 0.38 ? 0 : r() < 0.62 ? 1 : 2, jit: r(), ph: r() * 6 }); return a; })();
  const phoenix = (ctx, x, y, s, t, burn, life) => {
    const flap = Math.sin(t * 9) * 0.5;
    ctx.save(); ctx.globalCompositeOperation = 'lighter';
    for (const b of BIRD) {
      let px, py;
      if (b.part === 0) { const side = b.u < 0.5 ? -1 : 1, u = (b.u % 0.5) * 2; px = side * u * 300; py = -Math.sin(u * Math.PI) * 90 - u * u * 160 * (1 + flap) * (side ? 1 : 1) + u * 40; }
      else if (b.part === 1) { px = (b.jit - 0.5) * 40; py = -60 + b.u * 140; }
      else { px = (b.jit - 0.5) * 120 * b.u; py = 80 + b.u * 260; }
      px += Math.sin(t * 12 + b.ph) * 6; py += Math.cos(t * 10 + b.ph) * 6;
      const fall = burn * (200 + b.jit * 500), drift = burn * (b.jit - 0.5) * 200;
      const col = burn > 0.3 ? [150, 140, 150] : b.u < 0.3 ? [255, 240, 180] : [255, 120 + b.jit * 80, 40];
      glowDot(ctx, x + (px + drift) * s, y + (py * s) + fall, (6 + b.jit * 12) * s * (1 - burn * 0.5), col, life * (1 - burn * 0.6));
    }
    if (burn < 0.3) glowDot(ctx, x, y - 20 * s, 120 * s, [255, 150, 60], 0.35 * life);
    ctx.restore();
  };

  const SCENES = [
    { a: 17.17, b: 17.53, draw(ctx, t) { space(ctx, t, { warp: 0.4 * (1 - K.prog(t, 17.17, 17.53)) }); bubbleHold(ctx, t); } },
    { a: 17.53, b: 18.78, hud: { cnt: 'ЛЕГЕНД: 1000' }, draw(ctx, t) { // a thousand legends
      space(ctx, t); farGalaxies(ctx, t, 5);
      runeMarks(ctx, W / 2, 940, 430, 36, t * 0.25, GOLD, 0.35, 3);
      glowDot(ctx, W / 2, 940, 360 + 40 * kick(t), [255, 180, 90], 0.22);
      const n = Math.floor(1000 * K.ease.outExpo(K.prog(t, 17.53, 18.1)));
      K.text(ctx, String(n).padStart(4, '0'), W / 2, 720, { font: '500 44px "PT Mono"', color: 'rgba(255,230,190,0.8)', shadow: false });
      gold(ctx, 'ТЫСЯЧА ЛЕГЕНД', W / 2, 890, t, 17.55, { size: 100 });
      gold(ctx, 'О ГЕРОЯХ', W / 2, 1010, t, 17.75, { size: 100 });
      sub(ctx, 'и почти в каждой герой не вернулся.', t, 17.95, 1170);
    } },
    { a: 18.78, b: 19.73, hud: { cnt: 'ПРЕТЕНДЕНТОВ: МНОГО' }, draw(ctx, t) { // a sword falls into the stone
      space(ctx, t);
      const cx = W / 2, gy = 1180, q = K.ease.inQuad(K.prog(t, 18.78, 19.27));
      // the stone
      const sg = ctx.createRadialGradient(cx - 80, gy - 40, 20, cx, gy + 60, 320); sg.addColorStop(0, '#6a6680'); sg.addColorStop(1, '#1d1b2a');
      ctx.fillStyle = sg; ctx.beginPath(); ctx.ellipse(cx, gy + 90, 300, 150, 0, 0, 7); ctx.fill();
      runeMarks(ctx, cx, gy + 90, 230, 18, t * 0.2, GOLD, t > 19.27 ? 0.9 : 0.25, 9);
      sword(ctx, cx, K.lerp(-500, gy + 40, q), 1.1, 0, 1);
      if (t > 19.27) { ring(ctx, cx, gy + 40, t, 19.27, { col: [200, 220, 255], r1: 650 }); debris(ctx, cx, gy + 40, t, 19.27, { col: [200, 215, 255], n: 60, speed: 600 }); }
      sub(ctx, 'сильных было много.', t, 18.85, 1520);
    } },
    { a: 19.73, b: 20.82, hud: { sig: 'МАНА 9%' }, draw(ctx, t) { // a shield under a rain of fire
      space(ctx, t);
      const cx = W / 2, cy = 1000, hits = [19.99, 20.17, 20.40];
      // little lights of a village under the shield
      for (let i = 0; i < 14; i++) glowDot(ctx, 260 + i * 42, 1300 + Math.sin(i * 1.7) * 14, 12, [255, 210, 140], 0.7);
      // fire arrows falling onto the shield
      const r = K.rng(4);
      for (let i = 0; i < 18; i++) {
        const t0 = 19.73 + r() * 0.9, q = K.prog(t, t0, t0 + 0.35), sx = 150 + r() * 780;
        if (q <= 0 || q >= 1) continue;
        const x = K.lerp(sx + 200, cx + (sx - cx) * 0.4, q), y = K.lerp(250, cy - 250, q);
        ctx.strokeStyle = 'rgba(255,170,80,0.9)'; ctx.lineWidth = 4; ctx.beginPath(); ctx.moveTo(x, y); ctx.lineTo(x + 50, y - 90); ctx.stroke(); glowDot(ctx, x, y, 20, [255, 160, 70]);
      }
      const hit = Math.exp(-since(hits, t) * 8), crack = t > 20.40;
      ctx.save(); ctx.translate(cx, cy); ctx.scale(1 + 0.04 * hit, 1 - 0.03 * hit); ctx.translate(-cx, -cy);
      shieldPath(ctx, cx, cy, 520, 620); ctx.fillStyle = `rgba(120,140,255,${0.18 + 0.25 * hit})`; ctx.fill();
      ctx.strokeStyle = `rgba(255,215,140,${0.85})`; ctx.lineWidth = 8; ctx.stroke();
      ctx.strokeStyle = 'rgba(255,215,140,0.6)'; ctx.lineWidth = 6; ctx.beginPath(); ctx.moveTo(cx, cy - 230); ctx.lineTo(cx, cy + 230); ctx.moveTo(cx - 170, cy - 60); ctx.lineTo(cx + 170, cy - 60); ctx.stroke();
      if (crack) { ctx.strokeStyle = 'rgba(255,255,255,0.85)'; ctx.lineWidth = 3; ctx.beginPath(); ctx.moveTo(cx + 40, cy - 310); ctx.lineTo(cx + 10, cy - 200); ctx.lineTo(cx + 60, cy - 120); ctx.lineTo(cx + 20, cy - 40); ctx.stroke(); }
      ctx.restore();
      for (const h of hits) { debris(ctx, cx + (h === 20.40 ? 40 : -60), cy - 300, t, h, { n: 30, speed: 350, life: 0.5, size: 10, col: [255, 180, 90] }); }
      sub(ctx, 'щит держит тот, кто стоит впереди.', t, 19.8, 1500);
    } },
    { a: 20.82, b: 22.42, hud: { ra: 'СОЗВЕЗДИЕ: ДРАКОН' }, draw(ctx, t) { // the dragon and a lone knight
      space(ctx, t);
      const pan = e(t, 20.82, 22.42) * 200;
      ctx.save(); ctx.translate(0, -pan);
      constellation(ctx, DRAGON, 560, 820, 1.25, t, 20.9, [255, 120, 90]);
      if (t > 21.33) title(ctx, 'ДРАКОН', 760, 680, t, 21.33, { size: 70, font: SERIF, color: '#ffd9cc', glow: 'rgba(255,90,60,0.95)', blur: 24 });
      if (t > 21.79) {
        constellation(ctx, KNIGHT, 330, 1500, 1.4 * (1 + 0.12 * kick(t)), t, 21.79, GOLD);
        ring(ctx, 330, 1500, t, 21.79, { col: GOLD, r1: 260, w: 5 });
        title(ctx, 'РЫЦАРЬ', 330, 1680, t, 21.85, { size: 60, font: SERIF, color: '#fff1d6', glow: 'rgba(255,190,90,0.95)', blur: 24 });
      }
      ctx.restore();
      sub(ctx, 'против дракона выходят в одиночку.', t, 20.95, 1730);
    } },
    { a: 22.42, b: 24.10, hud: { sig: 'МАНА 0%' }, draw(ctx, t) { // rune circles open on the hats
      ctx.save(); ctx.translate(W / 2, 950); ctx.rotate((t - 22.42) * 0.1); ctx.translate(-W / 2, -950);
      space(ctx, t);
      const cx = W / 2, cy = 950, opened = count(HATS, t, 22.5, 23.7);
      glowDot(ctx, cx, cy, 140 + 40 * kick(t), [150, 170, 255], 0.9);
      [[170, 12, 1], [280, 20, -1], [400, 28, 1], [520, 36, -1]].forEach(([r, n, dir], i) => {
        if (i > opened) return;
        const q = K.ease.outBack(K.clamp((t - (HATS.filter(h => h >= 22.5)[i] || 22.45)) / 0.3));
        ctx.strokeStyle = rgb([170, 190, 255], 0.7); ctx.lineWidth = 3; ctx.beginPath(); ctx.arc(cx, cy, Math.max(1, r * q), 0, 7); ctx.stroke();
        ctx.beginPath(); ctx.arc(cx, cy, Math.max(1, r * q - 22), 0, 7); ctx.stroke();
        runeMarks(ctx, cx, cy, Math.max(1, r * q - 11), n, dir * t * (0.6 - i * 0.1), [210, 220, 255], 0.9, 20 + i);
      });
      // a hexagram in the middle
      ctx.strokeStyle = 'rgba(200,215,255,0.8)'; ctx.lineWidth = 3;
      for (const off of [0, Math.PI]) { ctx.beginPath(); for (let k = 0; k <= 3; k++) { const a2 = off + t * 0.5 + k * 2.094; const x = cx + Math.cos(a2) * 150, y = cy + Math.sin(a2) * 150; k ? ctx.lineTo(x, y) : ctx.moveTo(x, y); } ctx.stroke(); }
      if (t > 23.82) { const k = K.prog(t, 23.82, 24.1); glowDot(ctx, cx, cy, 250 + 800 * k, [230, 235, 255], 1 - k); ring(ctx, cx, cy, t, 23.82, { col: [200, 215, 255] }); }
      ctx.restore();
      sub(ctx, 'магия всегда чего-то стоит.', t, 22.5, 1560);
    } },
    { a: 24.10, b: 26.60, hud: { cnt: 'ПЕПЕЛ: 100%' }, draw(ctx, t) { // the phoenix burns down and rises again
      space(ctx, t);
      const fly = e(t, 24.1, 25.3), burn = K.prog(t, 25.39, 26.0) * (1 - K.prog(t, 26.16, 26.4));
      const rise = e(t, 26.16, 26.6);
      const x = K.lerp(W + 150, W / 2, fly), y = K.lerp(1300, 820, fly) - rise * 260;
      phoenix(ctx, x, y, 1.0 + 0.1 * kick(t), t, burn, 1);
      if (t > 26.16) { ring(ctx, W / 2, 820, t, 26.16, { col: [255, 170, 80], r1: 700 }); glowDot(ctx, W / 2, 820, 300 * (1 - K.prog(t, 26.16, 26.5)), [255, 200, 120], 1 - K.prog(t, 26.16, 26.5)); }
      sub(ctx, 'феникс сгорает…', t, 24.2, 1460);
      sub(ctx, '…чтобы снова взлететь.', t, 26.16, 1550, { color: '#ffd9b0' });
    } },
    { a: 26.60, b: 28.79, hud: { cnt: 'НОВЫХ МИРОВ: 7' }, draw(ctx, t) { // a star dies; worlds light up from its dust
      space(ctx, t);
      const cx = W / 2, cy = 900;
      if (t < 26.91) { const sw = 1 + 1.4 * K.prog(t, 26.6, 26.91); glowDot(ctx, cx, cy, 160 * sw, [255, 170, 90], 1); glowDot(ctx, cx, cy, 60 * sw, [255, 250, 220], 1); }
      else { ring(ctx, cx, cy, t, 26.91, { r1: 900, life: 1.2 }); debris(ctx, cx, cy, t, 26.91, { n: 110, speed: 650, life: 1.6, col: [255, 190, 130] }); }
      // worlds forming on the hats
      HATS.filter(h => h >= 27.1 && h < 28.3).forEach((h, i) => {
        if (t < h) return; const r = K.rng(i + 50), ang = r() * 6.283, d = 220 + r() * 300, x = cx + Math.cos(ang) * d, y = cy + Math.sin(ang) * d * 1.2;
        const pop = 1 + 0.6 * Math.exp(-(t - h) * 8), col = [[120, 200, 255], [255, 150, 120], [170, 255, 190], [220, 180, 255]][i % 4];
        glowDot(ctx, x, y, 40 * pop, col, 1);
        ctx.strokeStyle = rgb(col, 0.35); ctx.lineWidth = 2; ctx.beginPath(); ctx.ellipse(x, y, 70 * pop, 24 * pop, -0.3, 0, 7); ctx.stroke();
      });
      if (t > 27.9) { gold(ctx, 'ИЗ ПЕПЛА ЗВЁЗД', W / 2, 1480, t, 27.9, { size: 96, stagger: 0.025 }); sub(ctx, 'родились миры.', t, 28.3, 1590); }
    } },
    { a: 28.79, b: 29.73, hud: { ra: 'СОЗВЕЗДИЕ: ВЕСЫ' }, draw(ctx, t) { // one life against the whole world
      space(ctx, t);
      const cx = W / 2, py = 760;
      const tilt = t < 29.27 ? 0.32 : 0.32 * Math.exp(-(t - 29.27) * 5) * Math.cos((t - 29.27) * 14); // world heavy → balanced
      ctx.strokeStyle = '#ffd98a'; ctx.lineWidth = 8; ctx.beginPath(); ctx.moveTo(cx, py); ctx.lineTo(cx, 1300); ctx.stroke();
      ctx.beginPath(); ctx.moveTo(cx - 200, 1300); ctx.lineTo(cx + 200, 1300); ctx.stroke();
      const L = 340, lx = cx - Math.cos(tilt) * L, ly = py - Math.sin(-tilt) * L, rx = cx + Math.cos(tilt) * L, ry = py + Math.sin(-tilt) * L;
      ctx.beginPath(); ctx.moveTo(lx, ly + 2 * 0); ctx.lineTo(rx, ry); ctx.stroke();
      glowDot(ctx, cx, py, 22, GOLD);
      for (const [x, y, side] of [[lx, ly, 0], [rx, ry, 1]]) {
        ctx.strokeStyle = 'rgba(255,217,138,0.7)'; ctx.lineWidth = 3; ctx.beginPath(); ctx.moveTo(x, y); ctx.lineTo(x - 90, y + 220); ctx.moveTo(x, y); ctx.lineTo(x + 90, y + 220); ctx.stroke();
        ctx.fillStyle = 'rgba(255,217,138,0.25)'; ctx.beginPath(); ctx.ellipse(x, y + 225, 110, 22, 0, 0, 7); ctx.fill(); ctx.strokeStyle = '#ffd98a'; ctx.stroke();
        if (side === 0) { // the world
          const pg = ctx.createRadialGradient(x - 30, y + 150, 10, x, y + 175, 70); pg.addColorStop(0, '#9fd2ff'); pg.addColorStop(1, '#1b3d7a');
          ctx.fillStyle = pg; ctx.beginPath(); ctx.arc(x, y + 170, 62, 0, 7); ctx.fill();
        } else if (t > 29.27) { glowDot(ctx, x, y + 190, 40, GOLD); ctx.fillStyle = '#fff3d6'; ctx.beginPath(); ctx.arc(x, y + 150, 12, 0, 7); ctx.fill(); ctx.fillRect(x - 8, y + 162, 16, 42); }
      }
      gold(ctx, 'ОДНА ЖИЗНЬ', W / 2, 520, t, 28.82, { size: 104 });
      sub(ctx, 'против целого мира.', t, 29.27, 1480);
    } },
    { a: 29.73, b: 30.81, hud: { cnt: 'ВРАГОВ: 10 000' }, draw(ctx, t) { // one at the gate, a horde of red eyes coming
      space(ctx, t);
      const hz = 1300;
      ctx.fillStyle = '#0b0918'; ctx.beginPath(); ctx.moveTo(0, hz); for (let x = 0; x <= W; x += 60) ctx.lineTo(x, hz - Math.sin(x * 0.01) * 20); ctx.lineTo(W, H); ctx.lineTo(0, H); ctx.fill();
      // the gate
      ctx.fillStyle = '#16122a'; ctx.fillRect(120, hz - 360, 160, 360); ctx.fillRect(800, hz - 360, 160, 360);
      ctx.fillRect(120, hz - 420, 840, 70); for (let i = 0; i < 9; i++) ctx.fillRect(120 + i * 100, hz - 460, 50, 40);
      // the horde: red eyes advancing from the dark
      const adv = e(t, 29.73, 30.81), r = K.rng(8);
      for (let i = 0; i < 120; i++) { const x = r() * W, y = hz + 40 + r() * 500 - adv * 160, bl = Math.sin(t * 6 + i) > -0.9; if (bl) { glowDot(ctx, x - 6, y, 8, RED, 0.9); glowDot(ctx, x + 6, y, 8, RED, 0.9); } }
      // the one who stays
      const gl = 0.6 + 0.4 * Math.exp(-since(HATS, t) * 6);
      glowDot(ctx, W / 2, hz - 60, 120 * gl, GOLD, 0.5);
      ctx.fillStyle = '#fff1d6'; ctx.beginPath(); ctx.arc(W / 2, hz - 120, 18, 0, 7); ctx.fill(); ctx.fillRect(W / 2 - 14, hz - 102, 28, 80);
      sword(ctx, W / 2 + 30, hz - 40, 0.25, 0.15, 0.4);
      sub(ctx, 'кто-то должен остаться у ворот.', t, 29.8, 560);
    } },
    { a: 30.81, b: 32.57, hud: { sig: 'МАНА 77%' }, draw(ctx, t) { // names that only the stars remember
      space(ctx, t);
      const n = count(KICKS, t, 30.8, 32.6), v = 997 + n + Math.floor(K.clamp(1 - Math.exp(-since(KICKS, t) * 10)));
      const r = K.rng(12); for (let i = 0; i < 40; i++) { const x = 90 + r() * 900, y = 250 + r() * 1400; if (i < n * 5) glowDot(ctx, x, y, 14 + 8 * Math.sin(t * 4 + i), GOLD, 0.8); }
      glowDot(ctx, W / 2, 900, 360, [255, 190, 100], 0.15 + 0.1 * kick(t));
      big(ctx, String(v), W / 2, 960, { size: 220, color: '#fff3dc', glow: 'rgba(255,180,80,0.9)', scale: 1 + 0.1 * kick(t) });
      sub(ctx, 'павших героев. их имена помнят звёзды.', t, 30.9, 1130);
    } },
    { a: 32.57, b: 34.10, hud: {}, draw(ctx, t) { // make a wish
      space(ctx, t);
      const q = K.ease.inOutSine(K.prog(t, 32.57, 33.5)), x = K.lerp(W + 120, 140, q), y = K.lerp(260, 760, q);
      if (q < 1) { const r = K.rng(4); for (let i = 0; i < 30; i++) { const k = i / 30; glowDot(ctx, x + 520 * k + (r() - 0.5) * 20, y - 200 * k + (r() - 0.5) * 20, 18 * (1 - k), [255, 235, 200], 0.9 * (1 - k)); } glowDot(ctx, x, y, 46, [255, 235, 200]); }
      for (const h of [33.19, 33.38, 33.61, 33.82]) if (t > h) { const r = K.rng(Math.round(h * 100)); glowDot(ctx, 180 + r() * 720, 400 + r() * 500, 26 * (1 - K.clamp((t - h) / 0.5)), [255, 240, 220], 1 - K.clamp((t - h) / 0.5)); }
      sub(ctx, 'загадай желание.', t, 32.65, 980, { size: 76 });
      ctx.fillStyle = 'rgba(4,3,14,0.5)'; if (t > 33.3) { ctx.beginPath(); ctx.roundRect(W / 2 - 250, 1040, 500, 90, 20); ctx.fill(); }
      term(ctx, '> стать героем', t, 33.38, W / 2 - 200, 1100, { cps: 12, size: 48 });
    } },
    { a: 34.10, b: 36.29, hud: { cnt: 'ГЕРОЕВ В ЗОНЕ: ВСЕ' }, draw(ctx, t) { // every star raises a sword
      const z = 1 + 0.3 * e(t, 34.1, 36.29);
      ctx.save(); ctx.translate(W / 2, 1000); ctx.scale(z, z); ctx.translate(-W / 2, -1000);
      space(ctx, t);
      const beats = [34.10, 34.42, 34.67, 34.92, 35.05, 35.26, 35.38, 35.7, 36.0], waves = beats.filter(b2 => t >= b2).length, r = K.rng(91);
      for (let i = 0; i < 80; i++) {
        const x = 60 + r() * (W - 120), y = 300 + r() * 1400, wave = Math.floor(r() * 9);
        if (wave >= waves) { glowDot(ctx, x, y, 10, [230, 230, 255], 0.7); continue; }
        const pop = 1 + 0.8 * Math.exp(-(t - beats[wave]) * 9);
        sword(ctx, x, y + 20, 0.09 * pop, 0, 0); glowDot(ctx, x, y - 10, 18 * pop, GOLD, 0.5);
      }
      ctx.restore();
      K.words(ctx, 'все хотят быть героями.', t, 34.45, { y: 560, font: 'italic 700 92px "Cormorant Garamond"', color: '#ffffff', wps: 0.18, shadow: true, maxW: 980, lh: 100 });
    } },
    { a: 36.29, b: 37.85, draw(ctx, t) { // the price
      space(ctx, t, { warp: 0.25 });
      const q = K.prog(t, 37.1, 37.75), j = kick(t) * 14 + (Math.floor(t * 20) % 6 === 0 ? 10 : 0);
      ctx.save(); ctx.translate(W / 2, 960); ctx.rotate(q * 1.4); ctx.scale(1 - q * 0.6, 1 - q * 0.6); ctx.translate(-W / 2, -960);
      for (const [dx, col, glow, a] of [[-j, '#ff3b4e', 'rgba(255,40,60,0)', 0.6], [j, '#5ad8ff', 'rgba(60,200,255,0)', 0.6], [0, '#ffe3e6', 'rgba(255,40,60,0.95)', 1]]) {
        ctx.save(); ctx.translate(dx, 0); ctx.globalCompositeOperation = a < 1 ? 'lighter' : 'source-over';
        title(ctx, '(ПОКА НЕ', W / 2, 860, t, 36.3, { size: 130, color: col, glow, alpha: a * (1 - q), float: 0, stagger: 0.025 });
        title(ctx, 'УЗНАЮТ', W / 2, 1010, t, 36.4, { size: 130, color: col, glow, alpha: a * (1 - q), float: 0, stagger: 0.025 });
        title(ctx, 'ЦЕНУ)', W / 2, 1160, t, 36.5, { size: 130, color: col, glow, alpha: a * (1 - q), float: 0, stagger: 0.025 });
        ctx.restore();
      }
      ctx.restore();
    } },
  ];
  // the pill stays a beat after the drop, glowing
  const bubbleHold = (ctx, t) => {
    const b = bubble(CHAT[6]), s = 1.25 + 0.08 * kick(t);
    ctx.save(); ctx.translate(W / 2, 960); ctx.scale(s, s); ctx.drawImage(b.c, -b.w / 2 - b.G, -b.h / 2 - b.G); ctx.restore();
  };

  window.VERSION = {
    duration: 37.8,
    images: {},
    render(ctx, t, frame) {
      ctx.fillStyle = '#04030b'; ctx.fillRect(0, 0, W, H);
      if (t < 17.17) chat(ctx, t, frame);
      else {
        const s = SCENES.find(x => t >= x.a && t < x.b);
        // camera: push in on each scene + punch on kicks + shake on the first kick of a scene
        const lp = s ? K.prog(t, s.a, s.b) : 0, z = (1.08 - 0.08 * K.ease.outCubic(lp)) * (1 + 0.035 * kick(t));
        const sh = 12 * kick(t) * (s && t - s.a < 0.3 ? 1 : 0.3);
        ctx.save(); ctx.translate(W / 2 + (K.hash(frame, 1) - 0.5) * sh, H / 2 + (K.hash(frame, 2) - 0.5) * sh); ctx.scale(z, z); ctx.translate(-W / 2, -H / 2);
        if (s) s.draw(ctx, t); ctx.restore();
        if (s && s.hud) hud(ctx, t, s.hud);
        if (s && t - s.a < 0.06) { ctx.fillStyle = 'rgba(255,255,255,0.4)'; ctx.fillRect(0, 0, W, H); }
        if (s && t - s.a < 0.1 && s.a > 17.5) { const r = K.rng(frame); for (let i = 0; i < 6; i++) { const y = r() * H, h = 20 + r() * 70; ctx.drawImage(ctx.canvas, 0, y, W, h, (r() - 0.5) * 100, y, W, h); } }
      }
      K.grain(ctx, frame, 0.04);
      K.vignette(ctx, 0.45);
      if (t > 37.6) { ctx.fillStyle = `rgba(0,0,0,${K.prog(t, 37.6, 37.8)})`; ctx.fillRect(0, 0, W, H); }
    },
  };
})();
