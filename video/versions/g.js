// Version G — «Кроме тебя». Space twin of version F on the same template track (37.8 s).
// A glassy night chat in space builds to "на войне стреляют все", the punchline "(кроме тебя)"
// lands, then an observatory log of a universe where everything collides and explodes —
// cut on the track's re-entries and punched on its kicks.
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
    ctx.save(); ctx.font = `800 ${o.size || 120}px Manrope`; ctx.fillStyle = o.color || '#f2f0ff';
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
    ctx.textAlign = 'left'; ctx.fillText('ОБСЕРВАТОРИЯ / 03:12', 60, 110);
    ctx.textAlign = 'right'; ctx.fillText(info.sig || 'СИГНАЛ 3%', W - 60, 110);
    ctx.textAlign = 'left'; ctx.fillText(info.ra || 'RA 20.36h  DEC -26.4°', 60, H - 90);
    ctx.textAlign = 'right'; ctx.fillText(info.cnt || 'ВЫСТРЕЛОВ В ЗОНЕ: ∞', W - 60, H - 90);
    ctx.fillStyle = 'rgba(210,205,255,0.2)'; ctx.fillRect(60, 130, W - 120, 1); ctx.fillRect(60, H - 130, W - 120, 1);
    ctx.restore();
  };
  const glowDot = (ctx, x, y, r, col, a = 1) => {
    const g = ctx.createRadialGradient(x, y, 0, x, y, r); g.addColorStop(0, rgb([255, 255, 255], a)); g.addColorStop(0.25, rgb(col, a * 0.9)); g.addColorStop(1, rgb(col, 0));
    ctx.fillStyle = g; ctx.beginPath(); ctx.arc(x, y, r, 0, 7); ctx.fill();
  };

  // ---------------- chat (0..17.17) ----------------
  const CHAT = [
    { t: 0.6, s: 'не спится', me: true },
    { t: 2.8, s: 'та же фигня. что такое?', me: false, typing: 2.0 },
    { t: 5.4, s: 'можно пройти войну и никого не убить?', me: true },
    { t: 8.0, s: 'хм. к чему это?', me: false, typing: 7.0 },
    { t: 10.6, s: 'ну правда. неужели никто не смог?', me: true },
    { t: 12.8, s: 'конечно нет. на войне стреляют все', me: false, typing: 11.8 },
    { t: 16.4, s: '(кроме тебя)', me: false, typing: 15.2, pill: true },
  ];
  const BMAX = 680, cache = new Map();
  const bubble = m => {
    if (cache.has(m.s)) return cache.get(m.s);
    const mm = K.makeCanvas(10, 10).getContext('2d'); mm.font = '600 46px Manrope';
    const lines = K.wrap(mm, m.s, BMAX - 80), tw = Math.max(...lines.map(l => mm.measureText(l).width));
    const w = Math.ceil(tw + 80), h = Math.ceil(lines.length * 58 + 48), G = 60;
    const c = K.makeCanvas(w + G * 2, h + G * 2), x = c.getContext('2d'); x.translate(G, G);
    if (m.pill) {
      x.shadowColor = 'rgba(255,90,150,0.95)'; x.shadowBlur = 40; x.strokeStyle = '#ff6aa3'; x.lineWidth = 5;
      x.beginPath(); x.roundRect(3, 3, w - 6, h - 6, 44); x.stroke(); x.shadowBlur = 0;
      x.fillStyle = 'rgba(40,8,30,0.75)'; x.beginPath(); x.roundRect(6, 6, w - 12, h - 12, 40); x.fill();
    } else if (m.me) {
      const g = x.createLinearGradient(0, 0, w, h); g.addColorStop(0, '#5160ff'); g.addColorStop(0.55, '#7d5cf6'); g.addColorStop(1, '#3fb3ff');
      x.shadowColor = 'rgba(120,110,255,0.6)'; x.shadowBlur = 40; x.fillStyle = g; x.beginPath(); x.roundRect(0, 0, w, h, 40); x.fill(); x.shadowBlur = 0;
      const hl = x.createLinearGradient(0, 0, 0, h); hl.addColorStop(0, 'rgba(255,255,255,0.25)'); hl.addColorStop(0.5, 'rgba(255,255,255,0)');
      x.fillStyle = hl; x.beginPath(); x.roundRect(0, 0, w, h, 40); x.fill();
    } else {
      x.fillStyle = 'rgba(255,255,255,0.13)'; x.beginPath(); x.roundRect(0, 0, w, h, 40); x.fill();
      x.strokeStyle = 'rgba(255,255,255,0.16)'; x.lineWidth = 2; x.stroke();
    }
    x.font = m.pill ? '500 50px Manrope' : '600 46px Manrope'; x.fillStyle = '#fff'; x.textBaseline = 'middle'; x.textAlign = 'left';
    lines.forEach((l, i) => x.fillText(l, 40, 24 + 29 + i * 58));
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
    const ag = ctx.createLinearGradient(W / 2 - 50, 230, W / 2 + 50, 330); ag.addColorStop(0, '#5aa8ff'); ag.addColorStop(1, '#7b5cf5');
    ctx.fillStyle = ag; ctx.beginPath(); ctx.arc(W / 2, 280, 52, 0, 7); ctx.fill();
    ctx.font = '700 50px Manrope'; ctx.fillStyle = '#fff'; ctx.textAlign = 'center'; ctx.fillText('А', W / 2, 298);
    ctx.font = '600 34px Manrope'; ctx.fillText('Артём', W / 2, 382); ctx.font = '500 24px Manrope'; ctx.fillStyle = 'rgba(200,200,230,0.7)'; ctx.fillText('в сети', W / 2, 416);
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
        for (let k = 0; k < 120; k++) { glowDot(ctx, x0 + r() * b.w + (r() - 0.5) * 400 * q, yb - b.h + r() * b.h + (r() - 0.5) * 300 * q, 10, PINK, 1 - q); }
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
      const a = K.prog(t, typingNow.typing, typingNow.typing + 0.15), x0 = typingNow.pill ? W / 2 - 70 : 120;
      ctx.save(); ctx.globalAlpha = a; ctx.fillStyle = 'rgba(255,255,255,0.13)'; ctx.beginPath(); ctx.roundRect(x0, 1400, 150, 90, 45); ctx.fill();
      for (let k = 0; k < 3; k++) { const j = Math.max(0, Math.sin(t * 8 - k * 0.9)); ctx.fillStyle = `rgba(235,235,255,${0.4 + 0.6 * j})`; ctx.beginPath(); ctx.arc(x0 + 44 + k * 31, 1445 - j * 8, 10, 0, 7); ctx.fill(); }
      ctx.restore();
    }
    ctx.restore();
  };

  // ---------------- the observatory log (drop) ----------------
  const CONST_ARCHER = { // a soldier aiming — "Стрелец"
    pts: [[0, 0], [30, 60], [10, 150], [-30, 250], [60, 250], [-60, 90], [110, 70], [200, 60], [270, 55]],
    edges: [[0, 1], [1, 2], [2, 3], [2, 4], [1, 5], [1, 6], [6, 7], [7, 8]],
  };
  const CONST_MEDIC = { pts: [[0, -60], [0, 60], [-60, 0], [60, 0], [0, 0]], edges: [[0, 4], [4, 1], [2, 4], [4, 3]] };
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

  const SCENES = [
    { a: 17.17, b: 17.53, draw(ctx, t) { space(ctx, t, { warp: 0.4 * (1 - K.prog(t, 17.17, 17.53)) }); bubbleHold(ctx, t); } },
    { a: 17.53, b: 18.78, hud: {}, draw(ctx, t) { // the count rolls up to 2 trillion, title flies in
      space(ctx, t); farGalaxies(ctx, t, 6);
      glowDot(ctx, W / 2, 930, 360 + 40 * kick(t), [130, 120, 255], 0.28);
      const n = Math.floor(2e12 * K.ease.outExpo(K.prog(t, 17.53, 18.1)));
      K.text(ctx, n.toLocaleString('ru-RU'), W / 2, 700, { font: '500 44px "PT Mono"', color: 'rgba(220,220,255,0.75)', shadow: false });
      title(ctx, '2 ТРИЛЛИОНА', W / 2, 880, t, 17.55, { size: 124 });
      title(ctx, 'ГАЛАКТИК', W / 2, 1020, t, 17.75, { size: 124, color: '#c7ecff', glow: 'rgba(90,200,255,0.95)' });
      sub(ctx, 'и почти все с кем-то воюют.', t, 17.95, 1170);
    } },
    { a: 18.78, b: 19.73, hud: { cnt: 'СТОЛКНОВЕНИЙ: 1' }, draw(ctx, t) { // comets crash, debris, rings
      space(ctx, t);
      const q = K.prog(t, 18.78, 19.27), cx = W / 2, cy = 900;
      if (q < 1) for (const [sx, sy, col, sd] of [[80, 330, CYAN, 1], [W - 80, 1470, PINK, 2]]) {
        const x = K.lerp(sx, cx, q), y = K.lerp(sy, cy, q), dx = sx - cx, dy = sy - cy, d = Math.hypot(dx, dy);
        const r = K.rng(sd); for (let i = 0; i < 26; i++) { const k = i / 26; glowDot(ctx, x + dx / d * 420 * k + (r() - 0.5) * 30 * k, y + dy / d * 420 * k + (r() - 0.5) * 30 * k, 22 * (1 - k), col, 0.8 * (1 - k)); }
        glowDot(ctx, x, y, 60, col);
      }
      if (t >= 19.27) { glowDot(ctx, cx, cy, 160 + 800 * K.prog(t, 19.27, 19.73), [255, 220, 180], 1 - K.prog(t, 19.27, 19.73)); ring(ctx, cx, cy, t, 19.27); ring(ctx, cx, cy, t, 19.36, { r1: 500, col: [255, 140, 190] }); debris(ctx, cx, cy, t, 19.27, { life: 0.9 }); }
      sub(ctx, 'даже кометы не могут разминуться.', t, 18.85, 1440);
    } },
    { a: 19.73, b: 20.82, hud: { sig: 'СИГНАЛ 9%' }, draw(ctx, t) { // the star swells on the hats and blows on the kick
      space(ctx, t);
      const cx = W / 2, cy = 880;
      if (t < 20.40) {
        const sw = 1 + 0.25 * Math.exp(-since([19.99, 20.17], t) * 7) + 0.6 * K.prog(t, 19.73, 20.4);
        glowDot(ctx, cx, cy, 150 * sw, [255, 190, 110], 1); glowDot(ctx, cx, cy, 60 * sw, [255, 255, 230], 1);
        for (let i = 0; i < 40; i++) { const a = i / 40 * 6.283 + t * 0.6, rr = 260 + 40 * Math.sin(t * 9 + i); glowDot(ctx, cx + Math.cos(a) * rr * (1 - K.prog(t, 19.73, 20.4) * 0.5), cy + Math.sin(a) * rr * (1 - K.prog(t, 19.73, 20.4) * 0.5), 10, [255, 200, 140], 0.6); }
      } else {
        const k = K.prog(t, 20.40, 20.82);
        glowDot(ctx, cx, cy, 250 + 1000 * k, [255, 180, 100], 1 - k * 0.7);
        for (let i = 0; i < 28; i++) { const a = i / 28 * 6.283; ctx.strokeStyle = `rgba(255,225,180,${(1 - k) * 0.85})`; ctx.lineWidth = 4; ctx.beginPath(); ctx.moveTo(cx + Math.cos(a) * 160, cy + Math.sin(a) * 160); ctx.lineTo(cx + Math.cos(a) * (220 + 1000 * k), cy + Math.sin(a) * (220 + 1000 * k)); ctx.stroke(); }
        ring(ctx, cx, cy, t, 20.40, { r1: 900 }); debris(ctx, cx, cy, t, 20.40, { n: 90, speed: 900 });
      }
      sub(ctx, 'звёзды горят миллиарды лет…', t, 19.8, 1400);
      sub(ctx, '…а потом взрываются.', t, 20.42, 1490, { color: '#ffd9b0' });
    } },
    { a: 20.82, b: 22.42, hud: { ra: 'RA 19.04h  DEC -25.3°' }, draw(ctx, t) { // pan across the sky: the Archer, then a tiny red Medic
      const pan = e(t, 20.82, 22.42) * 260;
      space(ctx, t); ctx.save(); ctx.translate(0, -pan);
      constellation(ctx, CONST_ARCHER, 300, 640, 1.7, t, 20.9, LAV);
      if (t > 21.33) title(ctx, 'СТРЕЛЕЦ', 560, 1170, t, 21.33, { size: 64, blur: 24 });
      if (t > 21.79) {
        const pul = 1 + 0.15 * kick(t); constellation(ctx, CONST_MEDIC, 760, 1520, 1.3 * pul, t, 21.79, [255, 90, 110]);
        ring(ctx, 760, 1520, t, 21.79, { col: [255, 90, 110], r1: 260, w: 5 });
        title(ctx, 'МЕДИК', 760, 1700, t, 21.85, { size: 56, color: '#ffd0d6', glow: 'rgba(255,70,100,0.95)', blur: 24 });
      }
      ctx.restore();
      sub(ctx, 'даже на небе есть стрелок.', t, 20.95, 1720);
    } },
    { a: 22.42, b: 24.10, hud: { cnt: 'СЛИЯНИЕ: 98%' }, draw(ctx, t) { // galaxies spiral into each other
      ctx.save(); ctx.translate(W / 2, 950); ctx.rotate((t - 22.42) * 0.12); ctx.translate(-W / 2, -950);
      space(ctx, t);
      const q = e(t, 22.42, 23.82), ang = q * 2.4;
      const d = K.lerp(380, 30, q);
      galaxy(ctx, W / 2 + Math.cos(ang) * d, 950 + Math.sin(ang) * d * 0.8, 280, t * 1.4, [255, 110, 170]);
      galaxy(ctx, W / 2 - Math.cos(ang) * d, 950 - Math.sin(ang) * d * 0.8, 300, -t * 1.2, [110, 200, 255]);
      for (const h of [22.56, 22.87, 23.19, 23.42, 23.65]) if (t > h && t < h + 0.25) glowDot(ctx, W / 2 + Math.cos(ang) * d * 0.4, 950, 120, [255, 255, 255], 1 - (t - h) / 0.25);
      if (t > 23.82) { const k = K.prog(t, 23.82, 24.1); glowDot(ctx, W / 2, 950, 220 + 700 * k, [255, 240, 255], 1 - k); ring(ctx, W / 2, 950, t, 23.82); }
      ctx.restore();
      sub(ctx, 'галактики тоже сталкиваются.', t, 22.5, 1460);
    } },
    { a: 24.10, b: 26.60, hud: { sig: 'СИГНАЛ 41%' }, draw(ctx, t) { // a call goes out on the beat, an answer comes back
      space(ctx, t);
      ctx.save(); ctx.translate(780, 520); ctx.rotate(t * 0.15); ctx.translate(-780, -520); moon(ctx, 780, 520, 170); ctx.restore();
      for (const h of [24.11, 24.43, 25.39, 26.16, 26.31]) ring(ctx, 300, 1230, t, h, { col: CYAN, r1: 620, life: 1.1, w: 5 });
      glowDot(ctx, 300, 1230, 34 + 10 * kick(t), CYAN);
      ctx.fillStyle = 'rgba(4,3,14,0.55)'; ctx.beginPath(); ctx.roundRect(80, 760, 920, 250, 24); ctx.fill();
      term(ctx, '> передача: «есть раненые?»', t, 24.2, 120, 830);
      term(ctx, '> ответ: да. много.', t, 25.39, 120, 900);
      if (t > 26.16) { const sl = K.ease.outBack(K.clamp((t - 26.16) / 0.25)); ctx.save(); ctx.translate(120, 975); ctx.scale(sl, sl); K.text(ctx, 'ИДУ.', 0, 0, { font: '700 64px "PT Mono"', color: '#6dffa0', align: 'left', shadow: false }); ctx.restore(); }
    } },
    { a: 26.60, b: 28.79, hud: { cnt: 'ВЫСТРЕЛОВ В ЗОНЕ: ∞' }, draw(ctx, t) { // pull out from one trench; the sky erupts on every hat
      space(ctx, t);
      const z = 1 + 7 * Math.pow(1 - e(t, 26.6, 28.4), 1.6), cx = W / 2, cy = 960; // world → screen scale
      const S = (wx, wy) => [cx + wx * z, cy + wy * z];
      // muzzle flashes and bursts: each hat throws a bigger wave, spread over a wider world
      const waves = [26.61, 26.91, ...HATS.filter(h => h >= 27.1 && h < 28.6)];
      waves.forEach((h, wi) => {
        if (t < h) return;
        const age = t - h, r = K.rng(wi * 31 + 7), n = 6 + wi * 9, spread = 40 + wi * 70;
        for (let i = 0; i < n; i++) {
          const ang = r() * 6.283, d = spread * Math.sqrt(r()), [x, y] = S(Math.cos(ang) * d, Math.sin(ang) * d * 1.7);
          const life = 0.35 + r() * 0.5, q = age / life;
          if (q > 1 || x < -50 || x > W + 50 || y < -50 || y > H + 50) continue;
          const col = r() > 0.3 ? [255, 170, 80] : [255, 240, 210];
          glowDot(ctx, x, y, (10 + r() * 22) * (1 - q * 0.5), col, 1 - q);
          if (r() > 0.6) { ctx.strokeStyle = `rgba(255,200,130,${(1 - q) * 0.7})`; ctx.lineWidth = 2; const ta = r() * 6.283, L = 40 + r() * 80; ctx.beginPath(); ctx.moveTo(x, y); ctx.lineTo(x + Math.cos(ta) * L * q * 2, y + Math.sin(ta) * L * q * 2); ctx.stroke(); }
        }
      });
      // the trench at the centre and the labels with leader lines
      const [tx0, ty0] = S(0, 0);
      glowDot(ctx, tx0, ty0, 22 + 12 * kick(t), [255, 80, 100]);
      const tag = (lab, wx, wy, t0, t1) => {
        const a2 = K.window(t, t0, t1, 0.15, 0.25); if (a2 <= 0) return;
        const [x, y] = S(wx, wy), lx = x + 120, ly = y - 140;
        ctx.save(); ctx.globalAlpha = a2; ctx.strokeStyle = '#ecebff'; ctx.lineWidth = 3;
        ctx.beginPath(); ctx.moveTo(x, y); ctx.lineTo(lx, ly); ctx.lineTo(lx + 40, ly); ctx.stroke();
        ctx.strokeRect(x - 26, y - 26, 52, 52);
        K.text(ctx, lab, lx + 52, ly + 12, { font: '600 40px "PT Mono"', align: 'left', color: '#ffffff', shadow: false });
        ctx.restore();
      };
      tag('ТВОЙ ОКОП', 0, 0, 26.62, 27.2);
      tag('ФРОНТ', 0, 0, 27.2, 27.85);
      if (t > 27.88) { title(ctx, 'ВСЯ ВСЕЛЕННАЯ', W / 2, 1500, t, 27.88, { size: 100, stagger: 0.025 }); sub(ctx, 'стреляет.', t, 28.3, 1610); }
    } },
    { a: 28.79, b: 29.73, hud: { ra: 'RA 02.11h  DEC +08.7°' }, draw(ctx, t) { // meteors hit the moon on the beat
      space(ctx, t);
      const s = 1 + 0.05 * kick(t) + 0.04 * K.prog(t, 28.79, 29.73), cx = W / 2, cy = 1000;
      moon(ctx, cx, cy, 320 * s);
      [[28.82, -0.9], [29.27, 0.6], [29.5, -0.2]].forEach(([h, ang], i) => {
        const q = K.prog(t, h - 0.18, h), hx = cx + Math.cos(ang) * 200, hy = cy + Math.sin(ang) * 180 - 60;
        if (q > 0 && q < 1) { const sx = hx + 500, sy = hy - 700; glowDot(ctx, K.lerp(sx, hx, q), K.lerp(sy, hy, q), 22, [255, 200, 150]); }
        if (t >= h) { debris(ctx, hx, hy, t, h, { n: 30, speed: 260, life: 0.6, size: 9 }); ctx.fillStyle = 'rgba(50,50,75,0.55)'; ctx.beginPath(); ctx.arc(hx, hy, 34, 0, 7); ctx.fill(); }
      });
      title(ctx, 'ДАЖЕ ЛУНА', W / 2, 520, t, 28.82, { size: 112 });
      sub(ctx, 'вся в шрамах от ударов.', t, 29.27, 1440);
    } },
    { a: 29.73, b: 30.81, hud: { cnt: 'ОБЪЕКТОВ БЕЗ ОРУЖИЯ: 1' }, draw(ctx, t) { // a reticle hunts across the orbits and locks on him
      space(ctx, t);
      const cx = W / 2, cy = 960;
      glowDot(ctx, cx, cy, 150, [255, 210, 120], 1);
      let tgt = null;
      [[210, 1.6, [160, 170, 200]], [330, 1.0, [255, 120, 100]], [460, 0.6, [255, 90, 110]], [610, 0.4, [130, 190, 255]]].forEach(([r, sp, col], i) => {
        ctx.strokeStyle = 'rgba(210,205,255,0.2)'; ctx.lineWidth = 2; ctx.beginPath(); ctx.ellipse(cx, cy, r, r * 0.42, 0, 0, 7); ctx.stroke();
        const a = t * sp + i * 1.7, x = cx + Math.cos(a) * r, y = cy + Math.sin(a) * r * 0.42;
        glowDot(ctx, x, y, i === 2 ? 36 : 24, col); if (i === 2) tgt = [x, y];
      });
      const lock = K.ease.inOutCubic(K.prog(t, 29.75, 30.3)), rx = K.lerp(150, tgt[0], lock), ry = K.lerp(1500, tgt[1], lock), rs = 90 - 30 * lock + 10 * kick(t);
      ctx.save(); ctx.strokeStyle = lock >= 1 ? '#ff5a6e' : '#e0dcff'; ctx.lineWidth = 4; ctx.translate(rx, ry); ctx.rotate(t * (1 - lock) * 4);
      for (let k = 0; k < 4; k++) { ctx.rotate(Math.PI / 2); ctx.beginPath(); ctx.moveTo(rs, -16); ctx.lineTo(rs, 16); ctx.moveTo(rs - 22, 0); ctx.lineTo(rs + 14, 0); ctx.stroke(); }
      ctx.beginPath(); ctx.arc(0, 0, rs * 0.7, 0, 7); ctx.stroke(); ctx.restore();
      if (lock >= 1) { K.text(ctx, '✚', tgt[0], tgt[1] + 13, { font: '700 36px Manrope', color: '#fff', shadow: false }); ctx.fillStyle = 'rgba(4,3,14,0.55)'; ctx.beginPath(); ctx.roundRect(80, 1440, 920, 100, 20); ctx.fill(); term(ctx, '> 1 объект без оружия', t, 30.3, 120, 1505); }
    } },
    { a: 30.81, b: 32.57, hud: { sig: 'СИГНАЛ 77%' }, draw(ctx, t) { // the count of the saved rolls up with every kick
      space(ctx, t);
      for (let i = 0; i < 9; i++) { const k = ((t - 30.81) * 1.3 + i / 9) % 1, x = 80 + i * 115; ctx.strokeStyle = `rgba(255,200,120,${0.6 * (1 - k)})`; ctx.lineWidth = 4; ctx.beginPath(); ctx.moveTo(x, 1800 - k * 1700); ctx.lineTo(x - 26, 1870 - k * 1700); ctx.stroke(); }
      const n = count(KICKS, t, 30.8, 32.6), v = 312 + Math.floor((n + K.clamp(1 - Math.exp(-since(KICKS, t) * 10))) * 7);
      glowDot(ctx, W / 2, 900, 380, [80, 255, 150], 0.18 + 0.12 * kick(t));
      big(ctx, String(v), W / 2, 960, { size: 230, color: '#e4ffee', glow: 'rgba(80,255,150,0.9)', scale: 1 + 0.1 * kick(t) });
      for (let i = 0; i < 18; i++) { const r = K.rng(i + 3), k = ((t * 0.6 + r()) % 1); glowDot(ctx, 200 + r() * 680, 1300 - k * 600, 10, [100, 255, 160], 0.7 * (1 - k)); }
      sub(ctx, 'спасённых. и ни одного выстрела.', t, 30.9, 1130);
    } },
    { a: 32.57, b: 34.10, hud: {}, draw(ctx, t) { // make a wish
      space(ctx, t);
      const q = K.ease.inOutSine(K.prog(t, 32.57, 33.5)), x = K.lerp(W + 120, 140, q), y = K.lerp(260, 760, q);
      if (q < 1) { const r = K.rng(4); for (let i = 0; i < 30; i++) { const k = i / 30; glowDot(ctx, x + 520 * k + (r() - 0.5) * 20, y - 200 * k + (r() - 0.5) * 20, 18 * (1 - k), [255, 235, 200], 0.9 * (1 - k)); } glowDot(ctx, x, y, 46, [255, 235, 200]); }
      for (const h of [33.19, 33.38, 33.61, 33.82]) if (t > h) { const r = K.rng(Math.round(h * 100)); glowDot(ctx, 180 + r() * 720, 400 + r() * 500, 26 * (1 - K.clamp((t - h) / 0.5)), [255, 240, 220], 1 - K.clamp((t - h) / 0.5)); }
      sub(ctx, 'загадай желание.', t, 32.65, 980, { size: 76 });
      ctx.fillStyle = 'rgba(4,3,14,0.5)'; if (t > 33.3) { ctx.beginPath(); ctx.roundRect(W / 2 - 230, 1040, 460, 90, 20); ctx.fill(); }
      term(ctx, '> выспаться', t, 33.38, W / 2 - 170, 1100, { cps: 12, size: 48 });
    } },
    { a: 34.10, b: 36.29, hud: { cnt: 'ЦЕЛЕЙ: ВСЕ' }, draw(ctx, t) { // waves of stars turn red and grow sights; one in the middle stays white
      const z = 1 + 0.35 * e(t, 34.1, 36.29);
      ctx.save(); ctx.translate(W / 2, 960); ctx.scale(z, z); ctx.translate(-W / 2, -960);
      space(ctx, t);
      const beats = [34.10, 34.42, 34.67, 34.92, 35.05, 35.26, 35.38, 35.7, 36.0];
      const waves = beats.filter(b2 => t >= b2).length;
      const r = K.rng(77);
      for (let i = 0; i < 90; i++) {
        const x = 60 + r() * (W - 120), y = 260 + r() * 1400, wave = Math.floor(r() * 9);
        if (Math.hypot(x - W / 2, y - 960) < 160) continue;
        const on = wave < waves, age = on ? t - beats[wave] : 0, pop = on ? 1 + 0.6 * Math.exp(-age * 9) : 1;
        if (!on) { glowDot(ctx, x, y, 12, [230, 230, 255], 0.7); continue; }
        glowDot(ctx, x, y, 16 * pop, [255, 60, 80], 0.95);
        ctx.strokeStyle = `rgba(255,80,100,${0.8})`; ctx.lineWidth = 2.5; const R = 22 * pop;
        ctx.beginPath(); ctx.arc(x, y, R, 0, 7); ctx.moveTo(x - R - 10, y); ctx.lineTo(x - R + 6, y); ctx.moveTo(x + R - 6, y); ctx.lineTo(x + R + 10, y);
        ctx.moveTo(x, y - R - 10); ctx.lineTo(x, y - R + 6); ctx.moveTo(x, y + R - 6); ctx.lineTo(x, y + R + 10); ctx.stroke();
      }
      // the one without a sight
      const pul = 1 + 0.12 * Math.sin(t * 5) + 0.2 * kick(t);
      glowDot(ctx, W / 2, 960, 70 * pul, [235, 240, 255], 1);
      K.text(ctx, '✚', W / 2, 975, { font: '800 40px Manrope', color: '#ff4a5a', shadow: false });
      ctx.restore();
      K.words(ctx, 'все', t, 34.45, { y: 560, font: 'italic 700 120px "Cormorant Garamond"', color: '#ffffff', wps: 0.2, shadow: true });
    } },
    { a: 36.29, b: 37.85, draw(ctx, t) { // punchline with RGB split
      space(ctx, t, { warp: 0.25 });
      const q = K.prog(t, 37.1, 37.75), j = kick(t) * 14 + (Math.floor(t * 20) % 6 === 0 ? 10 : 0);
      ctx.save(); ctx.translate(W / 2, 960); ctx.rotate(q * 1.4); ctx.scale(1 - q * 0.6, 1 - q * 0.6); ctx.translate(-W / 2, -960);
      for (const [dx, col, glow, a] of [[-j, '#ff4a8a', 'rgba(255,60,120,0)', 0.6], [j, '#5ad8ff', 'rgba(60,200,255,0)', 0.6], [0, '#ffe6f0', 'rgba(255,80,150,0.95)', 1]]) {
        ctx.save(); ctx.translate(dx, 0); ctx.globalCompositeOperation = a < 1 ? 'lighter' : 'source-over';
        title(ctx, '(КРОМЕ', W / 2, 920, t, 36.3, { size: 146, color: col, glow, alpha: a * (1 - q), float: 0, stagger: 0.03 });
        title(ctx, 'ТЕБЯ)', W / 2, 1090, t, 36.42, { size: 146, color: col, glow, alpha: a * (1 - q), float: 0, stagger: 0.03 });
        ctx.restore();
      }
      ctx.restore();
    } },
  ];
  // the pill stays a beat after the drop, glowing
  const bubbleHold = (ctx, t) => {
    const b = bubble(CHAT[6]), s = 1.4 + 0.1 * kick(t);
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
