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
  const serif = (ctx, s, x, y, o = {}) => K.text(ctx, s, x, y, { font: `italic ${o.size || 50}px "Cormorant Garamond"`, color: o.color || '#e9e4ff', alpha: o.alpha ?? 1, shadow: false });
  const term = (ctx, s, t, t0, x, y, o = {}) => { if (t < t0) return; const n = Math.floor(K.clamp((t - t0) * (o.cps || 30), 0, s.length)); K.text(ctx, s.slice(0, n), x, y, { font: `${o.size || 34}px "PT Mono"`, align: 'left', color: o.color || '#d8d4ff', shadow: false }); };
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
    { a: 17.17, b: 17.53, draw(ctx, t) { space(ctx, t); bubbleHold(ctx, t); } },
    { a: 17.53, b: 18.78, hud: {}, draw(ctx, t) {
      space(ctx, t);
      const s = 0.6 + 0.4 * K.ease.outBack(K.clamp((t - 17.53) / 0.3)) + 0.04 * kick(t);
      big(ctx, '2 ТРИЛЛИОНА', W / 2, 860, { size: 128, scale: s });
      big(ctx, 'ГАЛАКТИК', W / 2, 1000, { size: 128, scale: s, color: '#bfe8ff', glow: 'rgba(90,200,255,0.95)' });
      if (t > 17.88) serif(ctx, 'и почти все с кем-то воюют.', W / 2, 1130, { alpha: K.prog(t, 17.88, 18.2) });
      glowDot(ctx, W / 2, 940, 300, [130, 120, 255], 0.25);
    } },
    { a: 18.78, b: 19.73, hud: { cnt: 'СТОЛКНОВЕНИЙ: 1' }, draw(ctx, t) { // two comets crash
      space(ctx, t);
      const q = K.prog(t, 18.78, 19.27);
      const ax = K.lerp(-100, W / 2, q), ay = K.lerp(300, 950, q), bx = K.lerp(W + 100, W / 2, q), by = K.lerp(1500, 950, q);
      for (const [x, y, dx, dy, col] of [[ax, ay, -1, -0.9, CYAN], [bx, by, 1, 0.9, PINK]]) {
        if (q >= 1) break;
        const g = ctx.createLinearGradient(x, y, x + dx * 360, y + dy * 360); g.addColorStop(0, rgb(col, 0.9)); g.addColorStop(1, rgb(col, 0));
        ctx.strokeStyle = g; ctx.lineWidth = 8; ctx.beginPath(); ctx.moveTo(x, y); ctx.lineTo(x + dx * 360, y + dy * 360); ctx.stroke(); glowDot(ctx, x, y, 40, col);
      }
      if (t >= 19.27) { const k = K.prog(t, 19.27, 19.73); glowDot(ctx, W / 2, 950, 120 + 700 * k, [255, 210, 160], 1 - k); ctx.strokeStyle = `rgba(255,230,200,${1 - k})`; ctx.lineWidth = 6; ctx.beginPath(); ctx.arc(W / 2, 950, 60 + 600 * k, 0, 7); ctx.stroke(); }
      serif(ctx, 'даже кометы не могут разминуться.', W / 2, 1380, { alpha: K.prog(t, 18.9, 19.2) });
    } },
    { a: 19.73, b: 20.82, hud: { sig: 'СИГНАЛ 9%' }, draw(ctx, t) { // supernova on the kick
      space(ctx, t);
      const k = K.prog(t, 20.40, 20.82);
      if (t < 20.40) { glowDot(ctx, W / 2, 900, 120 + 20 * Math.sin(t * 30), [255, 200, 120], 1); }
      else {
        glowDot(ctx, W / 2, 900, 200 + 900 * k, [255, 190, 110], 1 - k * 0.8);
        for (let i = 0; i < 24; i++) { const a = i / 24 * 6.283; ctx.strokeStyle = `rgba(255,220,170,${(1 - k) * 0.8})`; ctx.lineWidth = 3; ctx.beginPath(); ctx.moveTo(W / 2 + Math.cos(a) * 150, 900 + Math.sin(a) * 150); ctx.lineTo(W / 2 + Math.cos(a) * (200 + 900 * k), 900 + Math.sin(a) * (200 + 900 * k)); ctx.stroke(); }
      }
      serif(ctx, t < 20.40 ? 'звёзды держатся миллиарды лет…' : '…а потом взрываются.', W / 2, 1380);
    } },
    { a: 20.82, b: 22.42, hud: { ra: 'RA 19.04h  DEC -25.3°' }, draw(ctx, t) { // constellations
      space(ctx, t);
      constellation(ctx, CONST_ARCHER, 330, 640, 1.6, t, 20.9, LAV);
      K.text(ctx, 'СТРЕЛЕЦ', 560, 1150, { font: '600 34px "PT Mono"', color: 'rgba(210,205,255,0.8)', letter: '8px', shadow: false });
      if (t > 21.79) { constellation(ctx, CONST_MEDIC, 760, 1420, 1.3, t, 21.79, [255, 90, 110]); K.text(ctx, 'МЕДИК', 760, 1560, { font: '600 34px "PT Mono"', color: 'rgba(255,140,150,0.9)', letter: '8px', shadow: false }); }
      serif(ctx, 'даже на небе — стрелок.', W / 2, 1720, { alpha: K.prog(t, 21.0, 21.3) });
    } },
    { a: 22.42, b: 24.10, hud: { cnt: 'СЛИЯНИЕ: 98%' }, draw(ctx, t) { // galaxies collide
      space(ctx, t);
      const q = e(t, 22.42, 23.82);
      galaxy(ctx, K.lerp(260, W / 2 - 40, q), K.lerp(700, 900, q), 280, t * 0.8, [255, 110, 170]);
      galaxy(ctx, K.lerp(820, W / 2 + 40, q), K.lerp(1150, 950, q), 300, -t * 0.7, [110, 200, 255]);
      if (t > 23.82) { const k = K.prog(t, 23.82, 24.1); glowDot(ctx, W / 2, 920, 200 + 600 * k, [255, 240, 255], 1 - k); }
      serif(ctx, 'галактики тоже сталкиваются.', W / 2, 1450, { alpha: K.prog(t, 22.6, 23.0) });
    } },
    { a: 24.10, b: 26.60, hud: { sig: 'СИГНАЛ 41%' }, draw(ctx, t) { // a call goes out, an answer comes back
      space(ctx, t);
      moon(ctx, 760, 560, 170);
      for (let i = 0; i < 4; i++) { const k = ((t - 24.1) * 0.6 + i / 4) % 1; ctx.strokeStyle = `rgba(120,220,255,${0.5 * (1 - k)})`; ctx.lineWidth = 3; ctx.beginPath(); ctx.arc(330, 1180, 40 + k * 520, 0, 7); ctx.stroke(); }
      glowDot(ctx, 330, 1180, 30, CYAN);
      term(ctx, '> передача: «есть раненые?»', t, 24.2, 120, 820);
      term(ctx, '> ответ: да. много.', t, 25.39, 120, 880);
      if (t > 26.16) K.text(ctx, 'ИДУ.', 120, 960, { font: '700 54px "PT Mono"', color: '#6dffa0', align: 'left', shadow: false });
    } },
    { a: 26.60, b: 28.79, hud: {}, draw(ctx, t) { // zoom out: trench → front → universe
      space(ctx, t);
      const z = 1 + 3.2 * (1 - e(t, 26.6, 28.4)); // start close on the trench, pull out to everything
      const rings = [['ТВОЙ ОКОП', 60], ['ФРОНТ', 200], ['ВСЯ ВСЕЛЕННАЯ', 470]];
      rings.forEach(([lab, r]) => {
        const R = r * z; if (R > 1400) return;
        ctx.strokeStyle = 'rgba(210,205,255,0.4)'; ctx.lineWidth = 3; ctx.beginPath(); ctx.arc(W / 2, 960, R, 0, 7); ctx.stroke();
        K.text(ctx, lab, W / 2, 960 - R - 18, { font: '30px "PT Mono"', color: 'rgba(225,220,255,0.85)', shadow: false });
      });
      glowDot(ctx, W / 2, 960, 26, [255, 90, 110]);
      const blk = count(HATS, t, 27.1, 28.3); // violet blocks wipe in on the hats, like the template
      for (let i = 0; i < blk; i++) { ctx.fillStyle = 'rgba(120,90,255,0.85)'; ctx.fillRect((i * 337) % (W - 300), 300 + i * 260, 300 + (i % 2) * 200, 120); }
      if (t > 27.9) { big(ctx, 'ВСЯ ВСЕЛЕННАЯ', W / 2, 1480, { size: 96 }); serif(ctx, 'стреляет.', W / 2, 1580); }
    } },
    { a: 28.79, b: 29.73, hud: { ra: 'RA 02.11h  DEC +08.7°' }, draw(ctx, t) { // even the moon: covered in scars
      space(ctx, t);
      const s = 1 + 0.06 * kick(t); moon(ctx, W / 2, 980, 330 * s);
      big(ctx, 'ДАЖЕ ЛУНА', W / 2, 520, { size: 110 });
      if (t > 29.27) serif(ctx, 'вся в шрамах от ударов.', W / 2, 1440);
    } },
    { a: 29.73, b: 30.81, hud: { cnt: 'ОБЪЕКТОВ БЕЗ ОРУЖИЯ: 1' }, draw(ctx, t) { // one object without a weapon
      space(ctx, t);
      glowDot(ctx, W / 2, 960, 140, [255, 210, 120], 1);
      [[200, 0.9, [160, 170, 200]], [320, 0.55, [255, 120, 100]], [450, 0.35, [255, 90, 110]], [600, 0.22, [130, 190, 255]]].forEach(([r, sp, col], i) => {
        ctx.strokeStyle = 'rgba(210,205,255,0.18)'; ctx.lineWidth = 2; ctx.beginPath(); ctx.ellipse(W / 2, 960, r, r * 0.4, 0, 0, 7); ctx.stroke();
        const a = t * sp + i * 1.7, x = W / 2 + Math.cos(a) * r, y = 960 + Math.sin(a) * r * 0.4;
        glowDot(ctx, x, y, i === 2 ? 34 : 22, col);
        if (i === 2) { K.text(ctx, '✚', x, y + 12, { font: '700 34px Manrope', color: '#fff', shadow: false }); ctx.strokeStyle = '#ff5a6e'; ctx.lineWidth = 3; ctx.beginPath(); ctx.arc(x, y, 54 + 8 * kick(t), 0, 7); ctx.stroke(); }
      });
      if (t > 29.91) term(ctx, '> обнаружен 1 объект без оружия', t, 29.91, 110, 1500);
    } },
    { a: 30.81, b: 32.57, hud: { sig: 'СИГНАЛ 77%' }, draw(ctx, t) { // the sky lights up with tracer "stars" — he is the one that lands softly
      space(ctx, t);
      const n = count(KICKS, t, 30.8, 32.6);
      for (let i = 0; i < 6; i++) { const k = ((t - 30.81) * 1.4 + i / 6) % 1, x = 100 + i * 170; ctx.strokeStyle = `rgba(255,200,120,${0.7 * (1 - k)})`; ctx.lineWidth = 4; ctx.beginPath(); ctx.moveTo(x, 1700 - k * 1500); ctx.lineTo(x - 30, 1760 - k * 1500); ctx.stroke(); }
      big(ctx, String(312 + n * 7), W / 2, 950, { size: 200, color: '#dfffe9', glow: 'rgba(80,255,150,0.9)', scale: 1 + 0.08 * kick(t) });
      serif(ctx, 'спасённых. и ни одного выстрела.', W / 2, 1100);
    } },
    { a: 32.57, b: 34.10, hud: {}, draw(ctx, t) { // make a wish
      space(ctx, t);
      const q = K.prog(t, 32.57, 33.4);
      if (q < 1) { const x = K.lerp(W + 100, 200, q), y = K.lerp(300, 700, q); const g = ctx.createLinearGradient(x, y, x + 400, y - 160); g.addColorStop(0, 'rgba(255,240,210,0.95)'); g.addColorStop(1, 'rgba(255,240,210,0)'); ctx.strokeStyle = g; ctx.lineWidth = 6; ctx.beginPath(); ctx.moveTo(x, y); ctx.lineTo(x + 400, y - 160); ctx.stroke(); glowDot(ctx, x, y, 34, [255, 230, 190]); }
      serif(ctx, 'загадай желание.', W / 2, 960, { size: 64 });
      term(ctx, '> выспаться', t, 33.38, W / 2 - 110, 1060, { cps: 14 });
    } },
    { a: 34.10, b: 36.29, hud: {}, draw(ctx, t) { // the moon, a tiny figure — "все"
      ctx.fillStyle = '#d9d9e4'; ctx.fillRect(0, 0, W, H);
      const g = ctx.createRadialGradient(W / 2, 700, 100, W / 2, 900, 1300); g.addColorStop(0, '#f4f4fb'); g.addColorStop(1, '#9a9aae'); ctx.fillStyle = g; ctx.fillRect(0, 0, W, H);
      ctx.save(); ctx.beginPath(); ctx.arc(W / 2 + 40, 1060, 470, 0, 7); ctx.clip(); ctx.fillStyle = '#ffffff'; ctx.fillRect(0, 0, W, H);
      ctx.fillStyle = g; ctx.beginPath(); ctx.arc(W / 2 + 250, 940, 420, 0, 7); ctx.fill(); ctx.restore(); // crescent
      ctx.fillStyle = '#5b5b6e'; ctx.beginPath(); ctx.arc(500, 1400, 13, 0, 7); ctx.fill(); ctx.fillRect(490, 1412, 20, 46); // tiny figure
      ctx.fillStyle = '#ff4a5a'; ctx.fillRect(494, 1424, 12, 4); ctx.fillRect(498, 1420, 4, 12);
      K.text(ctx, 'все'.slice(0, Math.floor(K.clamp((t - 34.45) * 5, 0, 3))), 150, 420, { font: 'italic 72px "Cormorant Garamond"', color: '#6a6a80', align: 'left', shadow: false });
    } },
    { a: 36.29, b: 37.85, draw(ctx, t) { // punchline
      space(ctx, t);
      const q = K.prog(t, 37.1, 37.75), s = 0.7 + 0.3 * K.ease.outBack(K.clamp((t - 36.29) / 0.3));
      ctx.save(); ctx.translate(W / 2, 960); ctx.rotate(q * 1.4); ctx.scale(1 - q * 0.6, 1 - q * 0.6); ctx.translate(-W / 2, -960);
      big(ctx, '(КРОМЕ', W / 2, 920, { size: 140, scale: s, color: '#ffe6f0', glow: 'rgba(255,80,150,0.95)', alpha: 1 - q });
      big(ctx, 'ТЕБЯ)', W / 2, 1080, { size: 140, scale: s, color: '#ffe6f0', glow: 'rgba(255,80,150,0.95)', alpha: 1 - q });
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
