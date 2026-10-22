// Version L — «Кали». Dark fantasy: a healer who feels every wound she closes, kept in a cell by the Church;
// freed by the elf Bruno and his two friends; she kills the reborn creator-dragon Alarus to take his power and
// undo her past — and his father, the black dragon, ends the world. She falls back in time. Again. Again.
// Text only (her voice), the score is generated (audio/l_score.py → l-audio.js carries the cue times).
// The arts are not pasted: they are redrawn in ink and charcoal (tools/drawn.py) and painted in by brush strokes.
(function () {
  const { W, H } = K;
  const A = window.AUDIO || { fps: 30, rms: [], bass: [], cues: {} };
  const C = A.cues;
  const env = (k, t) => { const a = A[k], i = Math.floor(t * A.fps); return a && a.length ? a[Math.max(0, Math.min(a.length - 1, i))] : 0; };
  const since = (list, t) => { let b = 1e9; for (const k of list) if (k <= t) b = Math.min(b, t - k); return b; };
  const pr = (t, a, b) => K.prog(t, a, b);
  const es = (t, a, b) => K.ease.inOutSine(K.prog(t, a, b));
  const win = (t, a, b) => t >= a && t < b;
  const SERIF = '"Old Standard TT"', OSW = 'Oswald', MONO = '"PT Mono"';
  const BONE = '#e9e1d3', RED = '#c8102e', SILVER = '#dfe6f0';

  // ================= the arts, redrawn =================
  const ARTS = ['kali-cycle1', 'kali-power', 'alarus', 'black-dragon'];
  const images = {};
  for (const a of ARTS) for (const l of ['tone', 'lines0', 'lines1', 'lines2', 'color']) images[`${a}-${l}`] = `/assets/images/kali/drawn/${a}-${l}.png`;
  const I = n => K.images[n];
  let OFF = null;
  const off = () => OFF || (OFF = K.makeCanvas(W, H));
  // brush strokes that paint the art in: each stroke grows along a wavy path as p goes 0→1
  const STROKES = (() => { const r = K.rng(41), s = []; for (let i = 0; i < 26; i++) s.push({ x: r() * 1.2 - 0.1, y: (i + r()) / 26, a: -0.35 + r() * 0.7, w: 0.09 + r() * 0.07, d: r() * 0.35, ph: r() * 6 }); return s; })();
  // draw an art at rect r: charcoal tone, boiling ink lines, colour accents; p = how much is painted in
  const art = (ctx, key, t, r, o = {}) => {
    const p = o.p ?? 1, c = off(), x = c.getContext('2d');
    x.setTransform(1, 0, 0, 1, 0, 0); x.globalCompositeOperation = 'source-over'; x.globalAlpha = 1; x.clearRect(0, 0, W, H);
    const im = I(`${key}-tone`), s = Math.max(r.w / im.width, r.h / im.height) * (o.z || 1);
    const dw = im.width * s, dh = im.height * s, dx = r.x + (r.w - dw) * (o.fx ?? 0.5), dy = r.y + (r.h - dh) * (o.fy ?? 0.5);
    x.globalAlpha = o.tone ?? 1; x.drawImage(im, dx, dy, dw, dh);
    x.globalAlpha = o.lines ?? 1; x.drawImage(I(`${key}-lines${Math.floor(t * 8) % 3}`), dx, dy, dw, dh);
    x.globalAlpha = o.color ?? 1; x.drawImage(I(`${key}-color`), dx, dy, dw, dh);
    x.globalAlpha = 1;
    if (p < 1) {
      x.globalCompositeOperation = 'destination-in'; x.lineCap = 'round'; x.strokeStyle = '#fff';
      x.beginPath();
      for (const st of STROKES) {
        const q = K.clamp((p - st.d) / 0.65); if (q <= 0) continue;
        x.lineWidth = st.w * r.h * 1.4;
        const x0 = r.x + st.x * r.w - 0.2 * r.w, y0 = r.y + st.y * r.h;
        x.moveTo(x0, y0);
        for (let k = 1; k <= 20 * q; k++) { const u = k / 20; x.lineTo(x0 + u * r.w * 1.5, y0 + Math.sin(u * 5 + st.ph) * r.h * 0.04 + Math.tan(st.a) * u * r.w * 0.5); }
      }
      x.stroke();
      x.globalCompositeOperation = 'source-over';
    }
    ctx.save(); ctx.globalAlpha = o.alpha ?? 1;
    if (o.blend) ctx.globalCompositeOperation = o.blend;
    ctx.drawImage(c, 0, 0); ctx.restore();
  };

  // ================= text: her voice =================
  const say = (ctx, s, t, t0, t1, o = {}) => {
    if (t < t0 || t > t1) return;
    const size = o.size || 60, font = `${o.italic ? 'italic ' : ''}${size}px ${o.font || SERIF}`, lh = size * 1.3;
    ctx.save(); ctx.font = font; ctx.textAlign = 'center'; ctx.textBaseline = 'alphabetic';
    if (o.ls) ctx.letterSpacing = o.ls + 'px';
    const lines = s.split('\n').flatMap(l => K.wrap(ctx, l, o.maxW || 920));
    const fo = 1 - pr(t, t1 - 0.45, t1), y0 = (o.y || 1560) - (lines.length - 1) * lh / 2;
    let n = 0; const total = lines.join('').length, cps = o.cps || Math.max(22, total / 1.1);
    lines.forEach((l, li) => {
      const w = ctx.measureText(l).width; let x = W / 2 - w / 2;
      for (const ch of l) {
        const a = K.clamp((t - t0 - n / cps) / 0.25) * fo; n++;
        const cw = ctx.measureText(ch).width;
        if (a > 0) {
          ctx.globalAlpha = a; ctx.shadowColor = 'rgba(0,0,0,0.95)'; ctx.shadowBlur = 16;
          ctx.fillStyle = o.color || BONE; ctx.textAlign = 'left';
          ctx.fillText(ch, x + (1 - a) * (K.hash(n, 3) - 0.5) * 10, y0 + li * lh + (1 - a) * 8);
          if (o.glow) { ctx.shadowColor = o.glow; ctx.shadowBlur = 28; ctx.fillText(ch, x, y0 + li * lh); }
        }
        x += cw;
      }
    });
    ctx.restore();
  };
  const stamp = (ctx, s, t, t0, o = {}) => { // a hard word that hits: «ЛЕЧИ.»
    const d = t - t0; if (d < 0 || d > (o.dur || 1.2)) return;
    const sc = 1 + 0.35 * Math.exp(-d * 14), a = Math.min(1, d * 20) * (1 - pr(d, (o.dur || 1.2) - 0.3, o.dur || 1.2));
    ctx.save(); ctx.translate(W / 2 + (o.dx || 0), o.y || 960); ctx.scale(sc, sc); ctx.rotate(o.rot || 0);
    ctx.font = `bold ${o.size || 190}px ${OSW}`; ctx.textAlign = 'center'; ctx.textBaseline = 'middle'; ctx.letterSpacing = '6px';
    ctx.globalAlpha = a; ctx.shadowColor = 'rgba(200,16,46,0.8)'; ctx.shadowBlur = 40; ctx.fillStyle = o.color || RED; ctx.fillText(s, 0, 0);
    ctx.restore();
  };

  // ================= drawn pieces =================
  const fog = (ctx, t, col = '20,14,14', a = 0.5) => {
    ctx.save(); ctx.globalAlpha = a; ctx.globalCompositeOperation = 'screen';
    for (let k = 0; k < 2; k++) { const n = K.noise[(k * 2) % 4], ox = ((t * (14 + k * 9)) % 512) - 512; for (let y = -512; y < H; y += 512) for (let x = ox; x < W; x += 512) ctx.drawImage(n, x, y + k * 200); }
    ctx.restore();
    ctx.fillStyle = `rgba(${col},${a * 0.6})`; ctx.fillRect(0, 0, W, H);
  };
  const dark = (ctx, c = '#060404') => { ctx.fillStyle = c; ctx.fillRect(0, 0, W, H); };
  // glowing branching cracks (pain, the sky, time)
  const cracksOf = (seed, x, y, n, len, spread) => { const r = K.rng(seed), segs = []; const grow = (x, y, a, k, w, d) => { for (let i = 0; i < k; i++) { const L = len * (0.5 + r()), nx = x + Math.cos(a) * L, ny = y + Math.sin(a) * L; segs.push([x, y, nx, ny, w, d + i]); x = nx; y = ny; a += (r() - 0.5) * spread; if (r() < 0.28) grow(x, y, a + (r() - 0.5) * 2, Math.floor(k / 2), w * 0.6, d + i); } }; for (let i = 0; i < n; i++) grow(x, y, r() * 6.28, 7, 4, 0); return segs; };
  const drawCracks = (ctx, segs, p, col = '255,40,60', glow = 1) => {
    ctx.save(); ctx.lineCap = 'round'; ctx.globalCompositeOperation = 'lighter';
    const maxD = 12 * p;
    for (const [x0, y0, x1, y1, w, d] of segs) {
      if (d > maxD) continue;
      const f = K.clamp(maxD - d); if (f <= 0) continue;
      ctx.strokeStyle = `rgba(${col},${0.25 * glow})`; ctx.lineWidth = w * 4; ctx.beginPath(); ctx.moveTo(x0, y0); ctx.lineTo(x0 + (x1 - x0) * f, y0 + (y1 - y0) * f); ctx.stroke();
      ctx.strokeStyle = `rgba(${col},${0.9 * glow})`; ctx.lineWidth = w; ctx.stroke();
    }
    ctx.restore();
  };
  // the Church's sign — invented: rings, a downward triangle, seven rays, a closed lid
  const sigil = (ctx, x, y, R, p, t) => {
    ctx.save(); ctx.translate(x, y); ctx.rotate(t * 0.05); ctx.lineCap = 'round';
    const draw = () => {
      ctx.beginPath(); ctx.arc(0, 0, R, -Math.PI / 2, -Math.PI / 2 + Math.PI * 2 * K.clamp(p * 1.4)); ctx.stroke();
      if (p > 0.25) { ctx.beginPath(); ctx.arc(0, 0, R * 0.78, 0, Math.PI * 2 * K.clamp((p - 0.25) * 2)); ctx.stroke(); }
      if (p > 0.45) { const q = K.clamp((p - 0.45) * 2.5); ctx.beginPath(); for (let i = 0; i <= 3 * q; i++) { const a = Math.PI / 2 + i * Math.PI * 2 / 3; ctx.lineTo(Math.cos(a) * R * 0.74, Math.sin(a) * R * 0.74); } ctx.stroke(); }
      if (p > 0.6) for (let i = 0; i < 7; i++) { const a = -Math.PI / 2 + i * Math.PI * 2 / 7, q = K.clamp((p - 0.6) * 3 - i * 0.1); ctx.beginPath(); ctx.moveTo(Math.cos(a) * R * 1.05, Math.sin(a) * R * 1.05); ctx.lineTo(Math.cos(a) * R * (1.05 + 0.3 * q), Math.sin(a) * R * (1.05 + 0.3 * q)); ctx.stroke(); }
      if (p > 0.8) { ctx.beginPath(); ctx.ellipse(0, -R * 0.08, R * 0.3, R * 0.12 * K.clamp((p - 0.8) * 5), 0, 0, 7); ctx.stroke(); ctx.beginPath(); ctx.moveTo(-R * 0.3, -R * 0.08); ctx.lineTo(R * 0.3, -R * 0.08); ctx.stroke(); }
    };
    ctx.strokeStyle = 'rgba(255,90,40,0.25)'; ctx.lineWidth = 26; draw();
    ctx.strokeStyle = '#ff5a2a'; ctx.lineWidth = 7; draw();
    ctx.restore();
  };
  // a prison cell: stone, a high window of light, iron bars, a chain
  const cell = (ctx, t, o = {}) => {
    dark(ctx, '#0b0807');
    ctx.save(); ctx.globalAlpha = 0.35; for (let y = 0; y < H; y += 512) for (let x = 0; x < W; x += 512) ctx.drawImage(K.noise[1], x, y); ctx.restore();
    for (let r = 0; r < 18; r++) for (let c = 0; c < 6; c++) { const x = c * 200 + (r % 2) * 100 - 100, y = r * 110; ctx.strokeStyle = 'rgba(0,0,0,0.5)'; ctx.lineWidth = 4; ctx.strokeRect(x, y, 200, 110); }
    const g = ctx.createRadialGradient(W * 0.7, 300, 20, W * 0.7, 300, 900); g.addColorStop(0, `rgba(170,150,130,${0.22 * (o.light ?? 1)})`); g.addColorStop(1, 'rgba(0,0,0,0)');
    ctx.fillStyle = g; ctx.fillRect(0, 0, W, H);
    ctx.fillStyle = `rgba(200,190,170,${0.5 * (o.light ?? 1)})`; ctx.fillRect(W * 0.62, 180, 160, 110);
    ctx.fillStyle = '#0b0807'; for (let k = 0; k < 4; k++) ctx.fillRect(W * 0.62 + 18 + k * 40, 180, 10, 110);
    const fl = 0.8 + 0.2 * Math.sin(t * 9) * Math.sin(t * 3.7), tg = ctx.createRadialGradient(120, 900, 10, 120, 900, 700);
    tg.addColorStop(0, `rgba(255,140,60,${0.35 * fl * (o.torch ?? 1)})`); tg.addColorStop(1, 'rgba(255,140,60,0)'); ctx.fillStyle = tg; ctx.fillRect(0, 0, W, H);
  };
  const bars = (ctx, t, bend = 0) => {
    for (let i = 0; i < 7; i++) {
      const x = 80 + i * 155, b = bend * (i - 3) * 40;
      const g = ctx.createLinearGradient(x - 16, 0, x + 16, 0); g.addColorStop(0, '#060404'); g.addColorStop(0.45, '#4a3a30'); g.addColorStop(1, '#060404');
      ctx.fillStyle = g; ctx.beginPath(); ctx.moveTo(x - 16, 0); ctx.quadraticCurveTo(x - 16 + b, H / 2, x - 16, H); ctx.lineTo(x + 16, H); ctx.quadraticCurveTo(x + 16 + b, H / 2, x + 16, 0); ctx.fill();
    }
    ctx.fillStyle = '#1a1310'; ctx.fillRect(0, 300, W, 26); ctx.fillRect(0, 1560, W, 26);
  };
  // silhouettes: people who came to be healed, and the elves
  const person = (ctx, x, y, s, kind, col = '#050303') => {
    ctx.save(); ctx.translate(x, y); ctx.scale(s, s); ctx.fillStyle = col;
    ctx.beginPath(); ctx.arc(0, -330, 42, 0, 7); ctx.fill();
    if (kind === 'noble') { ctx.fillRect(-55, -390, 110, 18); ctx.fillRect(-35, -440, 70, 55); }       // tall hat
    if (kind === 'soldier') { ctx.beginPath(); ctx.arc(0, -340, 50, Math.PI, 0); ctx.fill(); ctx.fillRect(60, -480, 10, 480); } // helmet, spear
    const wide = kind === 'merchant' ? 130 : 95;
    ctx.beginPath(); ctx.moveTo(-60, -285); ctx.lineTo(60, -285); ctx.lineTo(wide, 0); ctx.lineTo(-wide, 0); ctx.closePath(); ctx.fill();
    if (kind === 'noble') { ctx.beginPath(); ctx.moveTo(-60, -285); ctx.lineTo(-140, 0); ctx.lineTo(140, 0); ctx.lineTo(60, -285); ctx.fill(); } // cloak
    ctx.restore();
  };
  const elf = (ctx, x, y, s, o = {}) => {
    ctx.save(); ctx.translate(x, y); ctx.scale(s * (o.flip ? -1 : 1), s); ctx.fillStyle = o.col || '#040303';
    ctx.beginPath(); ctx.ellipse(0, -360, 34, 42, 0, 0, 7); ctx.fill();
    ctx.beginPath(); ctx.moveTo(-26, -372); ctx.lineTo(-78, -410); ctx.lineTo(-30, -350); ctx.fill(); ctx.beginPath(); ctx.moveTo(26, -372); ctx.lineTo(78, -410); ctx.lineTo(30, -350); ctx.fill(); // the ears
    ctx.beginPath(); ctx.moveTo(-50, -320); ctx.quadraticCurveTo(-110, -150, -95, 0); ctx.lineTo(95, 0); ctx.quadraticCurveTo(110, -150, 50, -320); ctx.closePath(); ctx.fill(); // cloak
    if (o.bow) { ctx.strokeStyle = o.col || '#040303'; ctx.lineWidth = 9; ctx.beginPath(); ctx.arc(-120, -230, 130, -1.1, 1.1); ctx.stroke(); ctx.lineWidth = 2; ctx.beginPath(); ctx.moveTo(-120 + Math.cos(-1.1) * 130, -230 + Math.sin(-1.1) * 130); ctx.lineTo(-120 + Math.cos(1.1) * 130, -230 + Math.sin(1.1) * 130); ctx.stroke(); }
    if (o.sword) { ctx.save(); ctx.translate(80, -200); ctx.rotate(o.sword); ctx.fillRect(-6, -230, 12, 230); ctx.fillRect(-30, -10, 60, 12); ctx.restore(); }
    if (o.reach) { ctx.save(); ctx.translate(40, -250); ctx.rotate(-1.2 + 0.3 * (1 - o.reach)); ctx.fillRect(0, -16, 200 * o.reach + 60, 32); ctx.beginPath(); ctx.arc(200 * o.reach + 70, 0, 26, 0, 7); ctx.fill(); ctx.restore(); }
    ctx.restore();
  };
  // a wyvern / dragon silhouette with flapping wings
  const wyrm = (ctx, x, y, s, flap, col = '#030202') => {
    ctx.save(); ctx.translate(x, y); ctx.scale(s, s); ctx.fillStyle = col; ctx.strokeStyle = col; ctx.lineCap = 'round';
    const wing = (c, sc) => {
      ctx.save(); ctx.scale(sc, sc);
      const E = [-50, -140 * c], Wr = [40, -290 * c], F = [[-110, -330 * c], [-220, -250 * c], [-290, -120 * c]];
      ctx.lineWidth = 12; ctx.beginPath(); ctx.moveTo(0, -10); ctx.lineTo(...E); ctx.lineTo(...Wr); ctx.stroke();
      ctx.lineWidth = 5; for (const f of F) { ctx.beginPath(); ctx.moveTo(...Wr); ctx.lineTo(...f); ctx.stroke(); }
      ctx.beginPath(); ctx.moveTo(0, -10); ctx.lineTo(...E); ctx.lineTo(...Wr); ctx.lineTo(...F[0]);
      ctx.quadraticCurveTo((F[0][0] + F[1][0]) / 2 + 10, (F[0][1] + F[1][1]) / 2 + 40 * c, ...F[1]);
      ctx.quadraticCurveTo((F[1][0] + F[2][0]) / 2 + 20, (F[1][1] + F[2][1]) / 2 + 30 * c, ...F[2]);
      ctx.quadraticCurveTo(-220, -20, -130, 0); ctx.closePath(); ctx.fill();
      ctx.restore();
    };
    ctx.globalAlpha = 0.75; wing(Math.cos(flap + 0.6), 0.85); ctx.globalAlpha = 1;
    ctx.beginPath(); ctx.ellipse(0, 0, 140, 38, -0.05, 0, 7); ctx.fill();                                                   // body
    ctx.beginPath(); ctx.moveTo(120, -14); ctx.quadraticCurveTo(200, -70, 250, -62); ctx.lineTo(300, -50); ctx.lineTo(258, -36); ctx.quadraticCurveTo(210, -36, 130, 22); ctx.fill(); // neck, head
    ctx.lineWidth = 6; ctx.beginPath(); ctx.moveTo(250, -64); ctx.lineTo(225, -100); ctx.moveTo(240, -64); ctx.lineTo(205, -92); ctx.stroke(); // horns
    ctx.beginPath(); ctx.moveTo(-130, 0); ctx.quadraticCurveTo(-300, 40, -440, -10); ctx.lineTo(-470, -24); ctx.lineTo(-450, 4); ctx.quadraticCurveTo(-300, 56, -120, 24); ctx.fill(); // tail with a barb
    ctx.lineWidth = 9; ctx.beginPath(); ctx.moveTo(40, 25); ctx.lineTo(60, 80); ctx.lineTo(80, 86); ctx.moveTo(-60, 28); ctx.lineTo(-50, 80); ctx.lineTo(-30, 86); ctx.stroke(); // legs
    wing(Math.cos(flap), 1);
    ctx.restore();
  };
  const fire = (ctx, x, y, ang, t, len = 700, seed = 3) => {
    ctx.save(); ctx.globalCompositeOperation = 'lighter';
    const r = K.rng(seed);
    for (let i = 0; i < 160; i++) {
      const life = (t * 1.7 + r()) % 1, sp = (r() - 0.5) * 0.4, d = life * len;
      const px = x + Math.cos(ang + sp) * d, py = y + Math.sin(ang + sp) * d, rad = 10 + life * 60;
      const g = ctx.createRadialGradient(px, py, 0, px, py, rad); g.addColorStop(0, `rgba(255,${180 - life * 120 | 0},60,${0.5 * (1 - life)})`); g.addColorStop(1, 'rgba(255,60,20,0)');
      ctx.fillStyle = g; ctx.fillRect(px - rad, py - rad, rad * 2, rad * 2);
    }
    ctx.restore();
  };
  const sparks = (ctx, t, x, y, n, col = '255,220,160', R = 400, seed = 5) => {
    ctx.save(); ctx.globalCompositeOperation = 'lighter'; const r = K.rng(seed);
    for (let i = 0; i < n; i++) { const a = r() * 6.28, sp = 0.3 + r(), life = (t * sp * 0.5 + r()) % 1, d = life * R; ctx.fillStyle = `rgba(${col},${(1 - life) * 0.8})`; ctx.beginPath(); ctx.arc(x + Math.cos(a) * d, y + Math.sin(a) * d - life * 60, 1.5 + r() * 2.5, 0, 7); ctx.fill(); }
    ctx.restore();
  };
  const magicCircle = (ctx, x, y, R, p, t, col = '200,220,255') => {
    ctx.save(); ctx.translate(x, y); ctx.scale(1, 0.38); ctx.lineCap = 'round';
    for (const [rr, sp, w] of [[1, 0.2, 6], [0.82, -0.35, 3], [0.6, 0.5, 4]]) {
      ctx.save(); ctx.rotate(t * sp);
      ctx.strokeStyle = `rgba(${col},0.25)`; ctx.lineWidth = w * 5; ctx.beginPath(); ctx.arc(0, 0, R * rr, 0, Math.PI * 2 * K.clamp(p * 1.3)); ctx.stroke();
      ctx.strokeStyle = `rgba(${col},0.95)`; ctx.lineWidth = w; ctx.stroke();
      if (p > 0.5) for (let i = 0; i < 24; i++) { const a = i / 24 * 6.28; ctx.save(); ctx.rotate(a); ctx.translate(R * rr * 0.91, 0); ctx.strokeStyle = `rgba(${col},${0.8 * K.clamp((p - 0.5) * 3)})`; ctx.lineWidth = 3; ctx.beginPath(); const k = Math.floor(K.hash(i, rr * 10) * 4); if (k === 0) { ctx.moveTo(-10, -12); ctx.lineTo(10, 12); ctx.moveTo(10, -12); ctx.lineTo(-10, 12); } else if (k === 1) { ctx.arc(0, 0, 10, 0, 5); } else if (k === 2) { ctx.moveTo(0, -14); ctx.lineTo(0, 14); ctx.moveTo(-8, -4); ctx.lineTo(8, -4); } else { ctx.moveTo(-10, 10); ctx.lineTo(0, -12); ctx.lineTo(10, 10); } ctx.stroke(); ctx.restore(); }
      ctx.restore();
    }
    ctx.restore();
  };
  const lightColumn = (ctx, x, a, col = '220,232,255') => {
    if (a <= 0) return;
    const g = ctx.createLinearGradient(x - 260, 0, x + 260, 0); g.addColorStop(0, `rgba(${col},0)`); g.addColorStop(0.5, `rgba(${col},${0.85 * a})`); g.addColorStop(1, `rgba(${col},0)`);
    ctx.save(); ctx.globalCompositeOperation = 'lighter'; ctx.fillStyle = g; ctx.fillRect(x - 260, 0, 520, H); ctx.restore();
  };
  // shatter: the image breaks into triangles that fly apart
  const TRIS = (() => { const r = K.rng(9), t = [], N = 9, M = 14; for (let j = 0; j < M; j++) for (let i = 0; i < N; i++) { const x0 = i / N, y0 = j / M, x1 = (i + 1) / N, y1 = (j + 1) / M; t.push([[x0, y0], [x1, y0], [x0, y1]], [[x1, y0], [x1, y1], [x0, y1]]); } return t.map(p => ({ p, vx: r() - 0.5, vy: r() - 0.6, rot: (r() - 0.5) * 4 })); })();
  const shatter = (ctx, src, d) => {
    for (const tr of TRIS) {
      const cx = (tr.p[0][0] + tr.p[1][0] + tr.p[2][0]) / 3 * W, cy = (tr.p[0][1] + tr.p[1][1] + tr.p[2][1]) / 3 * H, dist = Math.hypot(cx - W / 2, cy - 900) / 900;
      const k = Math.max(0, d - dist * 0.15);
      ctx.save(); ctx.translate(cx + tr.vx * k * 1400, cy + tr.vy * k * 1400 + k * k * 900); ctx.rotate(tr.rot * k); ctx.translate(-cx, -cy);
      ctx.beginPath(); tr.p.forEach(([x, y], i) => (i ? ctx.lineTo(x * W, y * H) : ctx.moveTo(x * W, y * H))); ctx.closePath(); ctx.clip();
      const bx = Math.min(...tr.p.map(q => q[0])) * W, by = Math.min(...tr.p.map(q => q[1])) * H, bw = W / 9 + 2, bh = H / 14 + 2;
      ctx.globalAlpha = Math.max(0, 1 - k * 0.9); ctx.drawImage(src, bx, by, bw, bh, bx, by, bw, bh); ctx.restore();
    }
  };
  let SNAP = null;
  const snap = () => SNAP || (SNAP = K.makeCanvas(W, H));
  // the branches of time: a glowing tree that grows
  const BRANCH = (() => { const r = K.rng(17), segs = []; const grow = (x, y, a, L, d) => { if (d > 8) return; const nx = x + Math.cos(a) * L, ny = y + Math.sin(a) * L; segs.push([x, y, nx, ny, d]); grow(nx, ny, a - 0.25 - r() * 0.35, L * (0.72 + r() * 0.1), d + 1); grow(nx, ny, a + 0.25 + r() * 0.35, L * (0.72 + r() * 0.1), d + 1); }; grow(W / 2, 1750, -Math.PI / 2, 330, 0); return segs; })();
  const timeTree = (ctx, t, p, col = '255,214,150') => {
    ctx.save(); ctx.globalCompositeOperation = 'lighter'; ctx.lineCap = 'round';
    for (const [x0, y0, x1, y1, d] of BRANCH) {
      const f = K.clamp(p * 9 - d); if (f <= 0) continue;
      const sh = 0.6 + 0.4 * Math.sin(t * 3 + d + x0 * 0.01);
      ctx.strokeStyle = `rgba(${col},${0.18 * sh})`; ctx.lineWidth = (10 - d) * 3; ctx.beginPath(); ctx.moveTo(x0, y0); ctx.lineTo(x0 + (x1 - x0) * f, y0 + (y1 - y0) * f); ctx.stroke();
      ctx.strokeStyle = `rgba(${col},${0.85 * sh})`; ctx.lineWidth = Math.max(1, 10 - d); ctx.stroke();
    }
    ctx.restore();
  };
  // darkness pouring down: black blobs with a red rim
  const darkness = (ctx, t, p, seed = 1) => {
    const r = K.rng(seed), front = p * (H + 400) - 200;
    ctx.save();
    for (let i = 0; i < 70; i++) {
      const x = r() * W, sp = 0.6 + r() * 0.8, y = front * sp - r() * 300, R = 120 + r() * 220 + Math.sin(t * 2 + i) * 30;
      if (y < -R) continue;
      ctx.fillStyle = 'rgba(255,30,40,0.18)'; ctx.beginPath(); ctx.arc(x, y, R + 14, 0, 7); ctx.fill();
    }
    const r2 = K.rng(seed);
    for (let i = 0; i < 70; i++) {
      const x = r2() * W, sp = 0.6 + r2() * 0.8, y = front * sp - r2() * 300, R = 120 + r2() * 220 + Math.sin(t * 2 + i) * 30;
      if (y < -R) continue;
      ctx.fillStyle = '#020101'; ctx.beginPath(); ctx.arc(x, y, R, 0, 7); ctx.fillRect(x - R, y - 2000, R * 2, 2000); ctx.fill();
    }
    ctx.restore();
  };
  const glitch = (ctx, frame, amt) => {
    if (amt <= 0) return;
    for (let k = 0; k < 8; k++) { const y = Math.floor(K.hash(k, frame) * H), h = 10 + Math.floor(K.hash(k, frame + 1) * 120), dx = (K.hash(k, frame + 2) - 0.5) * 200 * amt; ctx.drawImage(ctx.canvas, 0, y, W, h, dx, y, W, h); }
    ctx.save(); ctx.globalCompositeOperation = 'screen'; ctx.globalAlpha = 0.25 * amt; ctx.drawImage(ctx.canvas, 8 * amt, 0); ctx.restore();
  };
  const kaliFigure = (ctx, t, x, y, h, o = {}) => art(ctx, 'kali-cycle1', t, { x: x - h * 0.19, y: y - h, w: h * 0.38, h }, { z: 1, ...o });

  // ================= scenes =================
  const PRISON_FOLK = ['noble', 'soldier', 'merchant', 'noble'];
  const SCENES = [
    // I. the gift
    { a: 0, b: 9.0, draw(ctx, t, frame) {
      dark(ctx); fog(ctx, t, '30,12,12', 0.35 * pr(t, 0, 3));
      // two hands, a light between them; a wound closes, the pain crawls up her arms
      const p = pr(t, 4.4, 8.8), lv = pr(t, 1, 4);
      const g = ctx.createRadialGradient(480, 1060, 10, 480, 1060, 520); g.addColorStop(0, `rgba(255,236,200,${0.55 * lv})`); g.addColorStop(1, 'rgba(255,236,200,0)');
      ctx.save(); ctx.globalCompositeOperation = 'lighter'; ctx.fillStyle = g; ctx.fillRect(0, 0, W, H); ctx.restore();
      // someone's forearm across the frame with an open wound; her hand above it, light between them
      ctx.save(); ctx.fillStyle = '#0c0707';
      ctx.beginPath(); ctx.moveTo(-40, 1080); ctx.quadraticCurveTo(400, 1050, 700, 1095); ctx.quadraticCurveTo(760, 1110, 750, 1160); ctx.quadraticCurveTo(420, 1220, -40, 1230); ctx.closePath(); ctx.fill();
      ctx.strokeStyle = `rgba(255,220,180,${0.45 * lv})`; ctx.lineWidth = 3; ctx.beginPath(); ctx.moveTo(-40, 1080); ctx.quadraticCurveTo(400, 1050, 700, 1095); ctx.stroke();
      const cl = 1 - pr(t, 4.6, 6.5); // the wound closes
      ctx.strokeStyle = `rgba(220,30,40,${0.3 + 0.7 * cl})`; ctx.lineWidth = 4 + 10 * cl; ctx.beginPath(); ctx.moveTo(430 - 110 * cl, 1112); ctx.quadraticCurveTo(470, 1102 - 6 * cl, 510 + 110 * cl, 1108); ctx.stroke();
      ctx.fillStyle = '#0a0606'; ctx.strokeStyle = '#0a0606'; ctx.lineCap = 'round';
      ctx.beginPath(); ctx.moveTo(W + 60, 420); ctx.lineTo(W + 60, 640); ctx.quadraticCurveTo(760, 860, 640, 930); ctx.lineTo(560, 820); ctx.quadraticCurveTo(820, 640, W + 60, 420); ctx.fill(); // her forearm
      ctx.beginPath(); ctx.ellipse(560, 900, 95, 70, -0.5, 0, 7); ctx.fill();                                                                     // palm
      ctx.lineWidth = 30; for (const [x1, y1, x2, y2] of [[505, 905, 420, 990], [530, 930, 470, 1030], [565, 945, 530, 1045], [605, 945, 600, 1030]]) { ctx.beginPath(); ctx.moveTo(x1, y1); ctx.lineTo(x2, y2); ctx.stroke(); } // fingers
      ctx.lineWidth = 32; ctx.beginPath(); ctx.moveTo(495, 870); ctx.lineTo(405, 880); ctx.stroke(); // thumb
      ctx.strokeStyle = `rgba(255,225,190,${0.7 * lv})`; ctx.lineWidth = 3; ctx.beginPath(); ctx.moveTo(W + 60, 640); ctx.quadraticCurveTo(760, 860, 640, 930); ctx.stroke(); ctx.beginPath(); ctx.arc(560, 900, 98, 1.6, 3.4); ctx.stroke();
      ctx.restore();
      drawCracks(ctx, CR_ARMS, pr(t, 6.6, 8.9));
      say(ctx, 'Мне говорили, что это дар.', t, 1.2, 4.4, { y: 520 });
      say(ctx, 'Я закрывала любую рану.', t, 4.6, 6.8, { y: 520 });
      say(ctx, 'А её боль забирала себе.\nВ десять раз сильнее.', t, 6.8, 9.0, { y: 520 });
    } },
    { a: 9.0, b: 12.6, draw(ctx, t, frame) { // the Church learned first
      dark(ctx, '#080404'); fog(ctx, t, '40,10,10', 0.3);
      const d = t - 9; ctx.save(); ctx.translate(W / 2, 900); ctx.scale(1 + 0.04 * Math.exp(-d * 6), 1 + 0.04 * Math.exp(-d * 6)); ctx.translate(-W / 2, -900);
      sigil(ctx, W / 2, 880, 330, pr(t, 9.0, 11.2), t); ctx.restore();
      if (d < 0.15) { ctx.fillStyle = `rgba(255,120,60,${0.5 * (1 - d / 0.15)})`; ctx.fillRect(0, 0, W, H); }
      K.embers(ctx, t, { n: 50, alpha: 0.6, seed: 4 });
      say(ctx, 'Церковь узнала первой.', t, 9.3, 12.6, { y: 1500 });
    } },
    { a: 12.6, b: 22.0, draw(ctx, t, frame) { // the cell; «Лечи.»
      cell(ctx, t);
      const k = C.clang.filter(c => t >= c).length, last = C.clang[Math.max(0, k - 1)] ?? 99;
      const cr = Math.min(1, 0.15 + k * 0.22);
      kaliFigure(ctx, t, W / 2 + 40, 1640, 1080, { p: pr(t, 12.6, 14.4), lines: 0.9 });
      drawCracks(ctx, CR_BODY, cr, '255,40,60', 0.8 + 0.4 * Math.exp(-(t - last) * 3));
      if (k > 0 && t - last < 1.6) person(ctx, 190 + (k % 2) * 30, 1700, 1.05, PRISON_FOLK[(k - 1) % 4], '#030202'); // who was brought in
      bars(ctx, t);
      say(ctx, 'Меня заперли.', t, 12.9, 14.9, { y: 1760 });
      for (const c of C.clang) stamp(ctx, 'ЛЕЧИ.', t, c, { y: 820 + (C.clang.indexOf(c) % 2) * 120, rot: (K.hash(c * 10, 1) - 0.5) * 0.1 });
      say(ctx, 'Тех, кто был им выгоден.', t, 16.0, 22.0, { y: 1760, color: '#cdb9a6' });
    } },
    { a: 22.0, b: 32.0, draw(ctx, t, frame) { // years
      cell(ctx, t, { light: 0.4 + 0.3 * Math.sin(t * 2.2), torch: 0.5 });
      kaliFigure(ctx, t, W / 2 + 40, 1640, 1000, { tone: 0.7, lines: 0.6 + 0.2 * Math.sin(t * 1.3) });
      drawCracks(ctx, CR_BODY, 1, '255,40,60', 0.5 + 0.5 * env('rms', t));
      bars(ctx, t);
      ctx.fillStyle = 'rgba(0,0,0,0.35)'; ctx.fillRect(0, 0, W, H);
      const day = Math.floor(1 + 3650 * K.ease.inOutSine(pr(t, 22.6, 27.0)) ** 2);
      say(ctx, 'Годы.', t, 22.2, 27.2, { y: 700, size: 80 });
      if (t < 27.3) say(ctx, `день ${day.toLocaleString('ru-RU')}`, t, 22.6, 27.3, { y: 820, size: 46, font: MONO, color: '#a8968a', cps: 400 });
      say(ctx, 'Боль не уходила.\nОна копилась.', t, 27.4, 32.0, { y: 760 });
    } },
    // II. Bruno
    { a: 32.0, b: 42.0, draw(ctx, t, frame) {
      const d = t - 32;
      cell(ctx, t, { light: 0.4 });
      const door = es(t, 32, 33.2);
      const g = ctx.createRadialGradient(W / 2, 900, 40, W / 2, 900, 1100); g.addColorStop(0, `rgba(255,200,130,${0.85 * door})`); g.addColorStop(1, 'rgba(255,150,60,0)');
      ctx.save(); ctx.globalCompositeOperation = 'lighter'; ctx.fillStyle = g; ctx.fillRect(0, 0, W, H); ctx.restore();
      const sh = Math.exp(-d * 4) * 30; ctx.save(); ctx.translate((K.hash(frame, 1) - 0.5) * sh, (K.hash(frame, 2) - 0.5) * sh);
      if (t > 35.0) { const q = es(t, 35.0, 36.4); elf(ctx, W / 2 - 300, 1700, 1.25 * q, { bow: true }); elf(ctx, W / 2 + 300, 1700, 1.25 * q, { sword: 0.4, flip: true }); }
      if (t > 35.6) elf(ctx, W / 2, 1760, 1.55 * es(t, 35.6, 36.8), { reach: es(t, 38.4, 40.2) });
      bars(ctx, t, door * 1.2);
      ctx.restore();
      if (d < 0.2) { ctx.fillStyle = `rgba(255,230,200,${1 - d / 0.2})`; ctx.fillRect(0, 0, W, H); }
      K.embers(ctx, t, { n: 40, alpha: 0.5, seed: 6 });
      say(ctx, 'Однажды дверь открыли не они.', t, 32.5, 35.4, { y: 360 });
      say(ctx, 'Эльф по имени Бруно.\nИ двое его друзей.', t, 35.5, 38.4, { y: 360 });
      say(ctx, '— Пойдёшь с нами?', t, 38.6, 42.0, { y: 360, italic: true, size: 66 });
    } },
    { a: 42.0, b: 52.5, draw(ctx, t, frame) { // the hunt: wyverns, dragons, nameless things
      const k = since(C.battle, t), beat = Math.exp(-k * 8);
      const sky = ctx.createLinearGradient(0, 0, 0, H); sky.addColorStop(0, '#1a0806'); sky.addColorStop(1, '#050202'); ctx.fillStyle = sky; ctx.fillRect(0, 0, W, H);
      fog(ctx, t, '60,20,10', 0.3);
      const part = t < 45 ? 0 : t < 48 ? 1 : 2;
      ctx.save(); ctx.translate(W / 2, H / 2); ctx.scale(1 + 0.03 * beat, 1 + 0.03 * beat); ctx.translate(-W / 2, -H / 2);
      if (part === 0) { const u = pr(t, 42, 45); wyrm(ctx, -200 + u * 1500, 500 + Math.sin(u * 3) * 120, 1.1, Math.sin(t * 7) * 0.9); if (t > 43.2) fire(ctx, 300 + u * 900, 560, 2.1, t, 600, 2); }
      if (part === 1) { wyrm(ctx, 760, 520, 1.6, Math.sin(t * 4) * 0.6, '#020101'); fire(ctx, 1150, 470, 2.35, t, 1100, 4); }
      if (part === 2) { const r = K.rng(8); for (let i = 0; i < 9; i++) { const x = r() * W, y = 300 + r() * 900, bl = Math.sin(t * 3 + i) > -0.85; if (bl) for (const dx of [-14, 14]) { ctx.fillStyle = 'rgba(255,60,40,0.9)'; ctx.beginPath(); ctx.ellipse(x + dx, y, 7, 4, 0, 0, 7); ctx.fill(); } } }
      elf(ctx, 220, 1780, 1.0, { bow: true }); elf(ctx, 860, 1800, 1.05, { sword: -0.6 + Math.sin(t * 5) * 0.3, flip: true });
      elf(ctx, 520, 1840, 1.15, {});
      if (t > 48.5) { kaliFigure(ctx, t, 700, 1880, 760, { tone: 0.8 }); drawCracks(ctx, CR_SMALL, pr(t, 49, 51.5), '255,40,60', 1); }
      ctx.restore();
      if (beat > 0.6) { ctx.fillStyle = `rgba(255,140,60,${0.12 * beat})`; ctx.fillRect(0, 0, W, H); }
      say(ctx, 'Драконы. Виверны.', t, 42.3, 44.9, { y: 1350 });
      say(ctx, 'Твари без имён.', t, 45.2, 47.9, { y: 1350 });
      say(ctx, 'Они знали про мой дар.\nИ всё равно берегли меня.', t, 48.2, 52.5, { y: 1350 });
    } },
    { a: 52.5, b: 58.0, draw(ctx, t, frame) { // the fire
      dark(ctx, '#050306');
      const r = K.rng(31); for (let i = 0; i < 160; i++) { const tw = 0.5 + 0.5 * Math.sin(t * 2 + i); ctx.fillStyle = `rgba(230,225,255,${0.6 * tw * r()})`; ctx.fillRect(r() * W, r() * 900, 2, 2); }
      const fl = 0.85 + 0.15 * Math.sin(t * 11) * Math.sin(t * 4.3);
      const g = ctx.createRadialGradient(W / 2, 1450, 10, W / 2, 1450, 700); g.addColorStop(0, `rgba(255,150,60,${0.55 * fl})`); g.addColorStop(1, 'rgba(255,120,40,0)'); ctx.fillStyle = g; ctx.fillRect(0, 0, W, H);
      fire(ctx, W / 2, 1480, -Math.PI / 2, t * 0.6, 220, 9);
      [[-330, 0], [-140, 30], [160, 30], [340, 0]].forEach(([dx, dy], i) => { if (i === 3) kaliFigure(ctx, t, W / 2 + dx, 1560 + dy, 520, { tone: 0.6, lines: 0.7 }); else elf(ctx, W / 2 + dx, 1560 + dy, 0.75, { flip: dx > 0 }); });
      say(ctx, 'Почти как жизнь.', t, 53.0, 58.0, { y: 760, size: 66 });
    } },
    // III. Alarus
    { a: 58.0, b: 64.0, draw(ctx, t, frame) {
      dark(ctx, '#040406'); fog(ctx, t, '20,24,40', 0.35);
      magicCircle(ctx, W / 2, 1450, 470, pr(t, 58.2, 63.5), t);
      [[-420, 1500], [420, 1500], [-250, 1700], [250, 1700]].forEach(([dx, y], i) => { if (i === 3) kaliFigure(ctx, t, W / 2 + dx, y, 520, { tone: 0.6 }); else elf(ctx, W / 2 + dx, y, 0.75, { flip: dx > 0 }); });
      lightColumn(ctx, W / 2, pr(t, 61.5, 64) * 0.6);
      sparks(ctx, t, W / 2, 1450, 60, '210,225,255', 500, 3);
      say(ctx, 'Мы решили вернуть того,\nкто создал всё.', t, 58.5, 63.6, { y: 480 });
    } },
    { a: 64.0, b: 78.5, draw(ctx, t, frame) { // the reveal; her look; the thought
      dark(ctx, '#05060a');
      const d = t - 64, z = 1.0 + 0.08 * pr(t, 64, 78.5);
      art(ctx, 'alarus', t, { x: -80, y: 120, w: W + 160, h: 1300 }, { p: pr(t, 64.0, 66.8), z, fy: 0.3 });
      lightColumn(ctx, W / 2, Math.exp(-d * 1.2) * 1.2);
      // mana: silver rings pulsing out of him
      ctx.save(); ctx.globalCompositeOperation = 'lighter';
      for (let i = 0; i < 6; i++) { const q = ((t * 0.5 + i / 6) % 1); ctx.strokeStyle = `rgba(210,225,255,${0.35 * (1 - q) * pr(t, 66, 68)})`; ctx.lineWidth = 3; ctx.beginPath(); ctx.arc(W / 2, 760, 120 + q * 900, 0, 7); ctx.stroke(); }
      ctx.restore();
      sparks(ctx, t, W / 2, 760, 90, '220,232,255', 800, 7);
      const g = ctx.createLinearGradient(0, 1250, 0, H); g.addColorStop(0, 'rgba(0,0,0,0)'); g.addColorStop(0.3, 'rgba(0,0,0,0.85)'); g.addColorStop(1, '#000'); ctx.fillStyle = g; ctx.fillRect(0, 1250, W, H);
      if (d < 0.25) { ctx.fillStyle = `rgba(230,240,255,${1 - d / 0.25})`; ctx.fillRect(0, 0, W, H); }
      say(ctx, 'АЛАРУС', t, 64.6, 68.3, { y: 1500, size: 120, ls: 18, color: SILVER, glow: 'rgba(180,200,255,0.8)', cps: 30 });
      say(ctx, 'Я смотрела на его силу.', t, 68.6, 71.8, { y: 1560 });
      say(ctx, 'И поняла: всё, что со мной сделали,\nможно стереть.', t, 72.0, 75.6, { y: 1560 });
      say(ctx, 'Как будто этого не было.', t, 75.7, 78.5, { y: 1560, color: '#ffb0b0' });
    } },
    { a: 78.5, b: 79.0, draw(ctx) { dark(ctx, '#000'); } },
    { a: 79.0, b: 82.6, draw(ctx, t, frame) { // the kill
      const d = t - 79, s = snap(), x = s.getContext('2d');
      x.setTransform(1, 0, 0, 1, 0, 0); x.globalAlpha = 1; x.globalCompositeOperation = 'source-over';
      x.fillStyle = '#05060a'; x.fillRect(0, 0, W, H); art(x, 'alarus', 78.4, { x: -80, y: 120, w: W + 160, h: 1300 }, { z: 1.08, fy: 0.3 });
      dark(ctx, '#050102');
      shatter(ctx, s, d * 0.8);
      ctx.save(); ctx.globalCompositeOperation = 'lighter'; ctx.strokeStyle = `rgba(255,40,50,${1 - pr(t, 79, 80.2)})`; ctx.lineWidth = 18; ctx.beginPath(); ctx.moveTo(-100, 300); ctx.lineTo(W + 100, 1300); ctx.stroke(); ctx.restore(); // the slash
      if (d < 0.12) { ctx.fillStyle = '#fff'; ctx.fillRect(0, 0, W, H); }
      say(ctx, 'Я убила его\nв миг возрождения.', t, 79.4, 82.6, { y: 1560, color: '#ff6b6b', size: 66 });
    } },
    { a: 82.6, b: 86.3, draw(ctx, t, frame) { // the power goes into her
      dark(ctx, '#040203');
      ctx.save(); ctx.globalCompositeOperation = 'lighter'; const r = K.rng(12);
      for (let i = 0; i < 260; i++) { const a = r() * 6.28, sp = 0.4 + r(), q = 1 - ((t * sp * 0.6 + r()) % 1), d = q * 1100; ctx.fillStyle = `rgba(215,228,255,${0.7 * (1 - q)})`; ctx.fillRect(W / 2 + Math.cos(a) * d, 760 + Math.sin(a) * d, 3, 3); }
      ctx.restore();
      art(ctx, 'kali-power', t, { x: 0, y: 120, w: W, h: 1300 }, { p: pr(t, 82.6, 85.0), fy: 0.25, z: 1 + 0.05 * pr(t, 82.6, 86.3) });
      drawCracks(ctx, CR_FACE, pr(t, 83.5, 86.0), '210,225,255', 0.55);
      const g = ctx.createLinearGradient(0, 1250, 0, H); g.addColorStop(0, 'rgba(0,0,0,0)'); g.addColorStop(0.3, 'rgba(0,0,0,0.85)'); g.addColorStop(1, '#000'); ctx.fillStyle = g; ctx.fillRect(0, 1250, W, H);
      say(ctx, 'Его сила вошла в меня.\nТело едва выдержало.', t, 82.9, 86.3, { y: 1560 });
    } },
    { a: 86.3, b: 90.3, draw(ctx, t, frame) { // they turn on her; she sends them away
      dark(ctx, '#050304'); fog(ctx, t, '40,40,60', 0.25);
      art(ctx, 'kali-power', t, { x: 0, y: 120, w: W, h: 1300 }, { fy: 0.25, z: 1.05, alpha: 0.55 });
      [[150, 1750, { bow: true }], [930, 1760, { sword: 0.6, flip: true }], [540, 1840, { sword: -0.4 }]].forEach(([x, y, o], i) => {
        const tp = C.whoosh[i], left = t - tp;
        if (left < 0) elf(ctx, x + Math.sin(t * 20 + i) * 6, y, 1.1, { ...o, col: '#020101' });
        else if (left < 0.4) { ctx.save(); ctx.globalCompositeOperation = 'lighter'; ctx.strokeStyle = `rgba(220,230,255,${1 - left / 0.4})`; ctx.lineWidth = 6; ctx.beginPath(); ctx.ellipse(x, y - 250, 140 + left * 600, 300 + left * 800, 0, 0, 7); ctx.stroke(); ctx.restore(); }
      });
      if (t < C.whoosh[0]) for (let i = 0; i < 6; i++) { ctx.strokeStyle = 'rgba(255,230,200,0.8)'; ctx.lineWidth = 3; const q = ((t * 2 + i / 6) % 1); ctx.beginPath(); ctx.moveTo(150 + q * 300, 1500 - q * 500); ctx.lineTo(190 + q * 300, 1460 - q * 500); ctx.stroke(); }
      say(ctx, 'Друзья бросились на меня.', t, 86.4, 88.5, { y: 1560 });
      say(ctx, 'Я не убила их.\nПросто убрала подальше.', t, 88.5, 90.3, { y: 1560 });
    } },
    // IV. the son
    { a: 90.3, b: 97.4, draw(ctx, t, frame) {
      dark(ctx, '#030304');
      const sh = t > 95 ? (t - 95) * 8 : 0;
      ctx.save(); ctx.translate((K.hash(frame, 1) - 0.5) * sh, (K.hash(frame, 2) - 0.5) * sh);
      timeTree(ctx, t, es(t, 90.3, 94.5));
      if (t > 95.0) drawCracks(ctx, CR_SKY, pr(t, 95.0, 97.3), '255,30,40', 1);
      kaliFigure(ctx, t, W / 2, 1900, 640, { tone: 0.7 });
      ctx.restore();
      say(ctx, 'Оставалось переписать время.', t, 90.6, 94.9, { y: 420 });
      say(ctx, 'Я не успела сказать ни слова.', t, 95.2, 97.4, { y: 420, color: '#ffb0b0' });
    } },
    { a: 97.4, b: 98.0, draw(ctx) { dark(ctx, '#000'); } },
    { a: 98.0, b: 104.0, draw(ctx, t, frame) { // «Вот и нашёл я тебя, сын.»
      const d = t - 98, sh = Math.exp(-d * 1.5) * 34;
      dark(ctx, '#0a0102');
      ctx.save(); ctx.translate((K.hash(frame, 1) - 0.5) * sh, (K.hash(frame, 2) - 0.5) * sh);
      art(ctx, 'black-dragon', t, { x: -300, y: 0, w: W + 600, h: 1500 }, { p: pr(t, 98.0, 99.6), z: 1.05 + 0.05 * pr(t, 98, 104), fx: 0.7 });
      drawCracks(ctx, CR_SKY, 1, '255,30,40', 0.6);
      ctx.restore();
      const g = ctx.createLinearGradient(0, 1150, 0, H); g.addColorStop(0, 'rgba(0,0,0,0)'); g.addColorStop(0.35, 'rgba(0,0,0,0.9)'); g.addColorStop(1, '#000'); ctx.fillStyle = g; ctx.fillRect(0, 1150, W, H);
      if (d < 0.18) { ctx.fillStyle = `rgba(255,40,50,${0.7 * (1 - d / 0.18)})`; ctx.fillRect(0, 0, W, H); }
      say(ctx, '«Вот и нашёл я тебя, сын.»', t, 99.0, 104.0, { y: 1520, size: 78, italic: true, color: '#ff3b47', glow: 'rgba(255,20,30,0.7)', cps: 14 });
    } },
    { a: 104.0, b: 112.0, draw(ctx, t, frame) { // darkness eats the world
      const sky = ctx.createLinearGradient(0, 0, 0, H); sky.addColorStop(0, '#2a0508'); sky.addColorStop(1, '#0c0204'); ctx.fillStyle = sky; ctx.fillRect(0, 0, W, H);
      // the world below: hills and towers
      ctx.fillStyle = '#120607'; ctx.beginPath(); ctx.moveTo(0, H); for (let x = 0; x <= W; x += 60) ctx.lineTo(x, 1500 - Math.sin(x * 0.006) * 120 - (x % 240 === 0 ? 160 : 0)); ctx.lineTo(W, H); ctx.fill();
      // her light, trying
      if (t < 110) { const q = (t * 1.3) % 1; ctx.save(); ctx.globalCompositeOperation = 'lighter'; const g = ctx.createRadialGradient(W / 2, 1300, 10, W / 2, 1300, 200 + q * 500); g.addColorStop(0, `rgba(230,235,255,${0.6 * (1 - q)})`); g.addColorStop(1, 'rgba(230,235,255,0)'); ctx.fillStyle = g; ctx.fillRect(0, 0, W, H); ctx.restore(); }
      darkness(ctx, t, es(t, 104, 113.5) * 0.92);
      kaliFigure(ctx, t, W / 2, 1880, 560, { tone: 0.6, alpha: 1 - pr(t, 110.5, 112) });
      say(ctx, 'Тьма пожирала мир.', t, 104.5, 107.2, { y: 1720 });
      say(ctx, 'Я не смогла её остановить.', t, 107.3, 110.0, { y: 1720 });
      say(ctx, 'Перед концом я коснулась ветви.', t, 110.1, 112.0, { y: 1720 });
      if (t > 110.1) timeTree(ctx, t, 1, '255,240,220');
    } },
    { a: 112.0, b: 113.6, draw(ctx, t, frame) { // rewind: the story runs backwards
      const q = pr(t, 112, 113.6), M = [110.5, 101, 96.5, 93, 87.5, 84, 79.5, 74, 66, 61, 55, 46, 39.5, 34, 30, 17.2, 11, 8.5, 3.5];
      const i = Math.min(M.length - 1, Math.floor(q * q * M.length * 1.6)), back = M[i] - (t - 112) * 0.5;
      const sc = SCENES.find(s => back >= s.a && back < s.b && s !== this);
      if (sc) sc.draw(ctx, back, Math.round(back * 30));
      glitch(ctx, frame, 1);
      ctx.fillStyle = `rgba(255,255,255,${0.15 + 0.6 * q * q})`; ctx.fillRect(0, 0, W, H);
    } },
    { a: 113.6, b: 122.0, draw(ctx, t, frame) {
      dark(ctx, '#050404'); fog(ctx, t, '30,30,30', 0.3 * pr(t, 113.6, 116));
      kaliFigure(ctx, t, W / 2, 1700, 1100, { p: pr(t, 113.8, 116.0), tone: 0.8 });
      say(ctx, 'Хотела умереть.\nИли вырвать из себя эту силу.', t, 113.8, 116.9, { y: 380 });
      say(ctx, 'А провалилась в прошлое.', t, 117.0, 119.4, { y: 380 });
      say(ctx, 'Сначала я не поняла.', t, 119.5, 122.0, { y: 380 });
    } },
    // V. the cycle
    { a: 122.0, b: 138.0, draw(ctx, t, frame) {
      const R = C.resets, k = R.filter(r => t >= r).length, a0 = k ? R[k - 1] : 122.0, b0 = R[k] ?? 138.0, q = pr(t, a0, b0), hit = Math.exp(-(t - a0) * 7);
      dark(ctx, '#040305');
      // one turn of the cycle in miniature: the circle lights, silver, the crack, the dark
      magicCircle(ctx, W / 2, 1300, 380, K.clamp(q * 3), t);
      lightColumn(ctx, W / 2, K.clamp(q * 3 - 1) * (1 - K.clamp(q * 3 - 2)) * 0.8);
      if (q > 0.55) drawCracks(ctx, CR_SKY, K.clamp((q - 0.55) * 4), '255,30,40', 1);
      if (q > 0.7) darkness(ctx, t, (q - 0.7) / 0.3 * 0.8, k + 2);
      if (hit > 0.05) { ctx.fillStyle = `rgba(255,255,255,${0.7 * hit})`; ctx.fillRect(0, 0, W, H); glitch(ctx, frame, hit); }
      const N = ['1', '2', '3', '5', '9', '17', '40', '112', '389', '1 024', '4 096', '∞'];
      say(ctx, `цикл ${N[Math.min(k, N.length - 1)]}`, t, a0, b0 + 0.3, { y: 300, size: 54, font: MONO, color: '#b9aa9a', cps: 200 });
      say(ctx, 'Рано или поздно\nкто-то возрождает Аларуса.', t, 122.3, 126.0, { y: 640 });
      say(ctx, 'Тогда приходит его отец.', t, 126.2, 129.3, { y: 640 });
      say(ctx, 'И все умирают.', t, 129.5, 132.3, { y: 640, color: '#ff8a8a' });
      say(ctx, 'Его не победить.', t, 132.5, 135.6, { y: 640 });
    } },
    { a: 138.0, b: 141.7, draw(ctx, t, frame) {
      dark(ctx, '#040303');
      kaliFigure(ctx, t, W / 2, 1750, 950, { tone: 0.5, lines: 0.5 + 0.3 * Math.sin(t * 2) });
      say(ctx, 'Я пыталась. Много раз.', t, 138.2, 140.0, { y: 420 });
      say(ctx, 'Потом сдалась.', t, 140.0, 141.7, { y: 520, size: 70 });
    } },
    { a: 141.7, b: 142.3, draw(ctx) { dark(ctx, '#000'); } },
    // VI. games, nightmares, the smile
    { a: 142.3, b: 150.0, draw(ctx, t, frame) {
      dark(ctx, '#060404');
      art(ctx, 'kali-power', t, { x: 0, y: 0, w: W, h: 1080 }, { p: pr(t, 142.3, 144.3), fy: 0.22, z: 1.02 });
      sparks(ctx, t, W / 2, 800, 70, '255,210,120', 700, 11);
      const g = ctx.createLinearGradient(0, 880, 0, 1100); g.addColorStop(0, 'rgba(0,0,0,0)'); g.addColorStop(1, 'rgba(6,4,4,1)'); ctx.fillStyle = g; ctx.fillRect(0, 880, W, 220);
      // a board of little figures; one by one they are flicked off
      ctx.save(); ctx.translate(W / 2, 1450); ctx.scale(1, 0.4);
      for (let i = -4; i < 4; i++) for (let j = -4; j < 4; j++) { ctx.fillStyle = (i + j) % 2 ? '#1e1512' : '#3a2b24'; ctx.fillRect(i * 130, j * 130, 130, 130); }
      ctx.restore();
      for (let i = 0; i < 8; i++) {
        const x = W / 2 - 455 + i * 130, y = 1440 + (i % 2) * 50, out = 144.5 + i * 0.6, d = t - out;
        ctx.save(); if (d > 0) { ctx.translate(x + d * 600 * (i % 2 ? 1 : -1), y - d * 400 + d * d * 1600); ctx.rotate(d * 8); ctx.translate(-x, -y); }
        person(ctx, x, y, 0.36, i % 3 ? 'merchant' : 'soldier', '#d8c9b4'); ctx.restore();
      }
      say(ctx, 'Теперь я просто развлекаюсь.', t, 142.8, 145.9, { y: 1720 });
      say(ctx, 'Люди? Фигуры на доске.\nВсё равно всё начнётся заново.', t, 146.0, 150.0, { y: 1720 });
    } },
    { a: 150.0, b: 160.0, draw(ctx, t, frame) { // the nightmare does not reset
      const k = since(C.nightmare, t), fl = Math.exp(-k * 5);
      if (fl > 0.25) { cell(ctx, t, { light: 0.6 }); kaliFigure(ctx, t, W / 2 + 40, 1640, 1000, { tone: 0.8 }); drawCracks(ctx, CR_BODY, 1, '255,40,60', 1); bars(ctx, t); glitch(ctx, frame, fl); }
      else { dark(ctx, '#030202'); art(ctx, 'kali-power', t, { x: 0, y: 120, w: W, h: 1300 }, { fy: 0.25, z: 1.1, alpha: 0.35, color: 0.6 }); }
      for (const c of C.nightmare) stamp(ctx, 'ЛЕЧИ.', t, c, { y: 900, dur: 0.6, size: 230 });
      say(ctx, 'Только сны не начинаются заново.', t, 150.5, 153.6, { y: 1640 });
      say(ctx, 'Каждую ночь — та же камера.', t, 154.0, 157.4, { y: 1640 });
      say(ctx, 'Тот же голос.', t, 157.5, 160.0, { y: 1640, color: '#ff8a8a' });
    } },
    { a: 160.0, b: 166.0, draw(ctx, t, frame) { // the smile
      dark(ctx, '#050303');
      const w = pr(t, 160, 166);
      art(ctx, 'kali-power', t, { x: 0, y: 80, w: W, h: 1400 }, { fy: 0.22, z: 1.12 + 0.12 * w, color: 1.4 });
      // laughter fills the dark around her
      ctx.save(); ctx.font = `italic 60px ${SERIF}`; ctx.textAlign = 'center';
      const n = Math.floor(w * 60);
      for (let i = 0; i < n; i++) { ctx.globalAlpha = 0.45 + 0.5 * K.hash(i, 2); ctx.fillStyle = i % 5 ? '#cdb9a6' : '#ff5a5a'; ctx.fillText('ха', K.hash(i, 3) * W, 100 + K.hash(i, 4) * 1700 + Math.sin(t * 3 + i) * 6); }
      ctx.restore();
      glitch(ctx, frame, 0.25 + 0.6 * w * (K.hash(Math.floor(t * 8), 1) > 0.6 ? 1 : 0.2));
      const g = ctx.createLinearGradient(0, 1350, 0, H); g.addColorStop(0, 'rgba(0,0,0,0)'); g.addColorStop(0.35, 'rgba(0,0,0,0.88)'); g.addColorStop(1, '#000'); ctx.fillStyle = g; ctx.fillRect(0, 1350, W, H);
      say(ctx, 'Ну что?', t, 160.6, 163.0, { y: 1600, size: 66 });
      say(ctx, 'Кто сегодня возродит Аларуса?', t, 163.0, 166.0, { y: 1640, italic: true, color: '#ffb0b0' });
    } },
    { a: 166.0, b: 170.0, draw(ctx, t, frame) {
      dark(ctx, '#000');
      const d = t - 166;
      say(ctx, 'КАЛИ', t, 166.0, 170.0, { y: 960, size: 170, ls: 30, color: RED, glow: 'rgba(200,16,46,0.8)', cps: 12 });
      say(ctx, 'цикл ∞', t, 166.8, 170.0, { y: 1100, size: 44, font: MONO, color: '#8a7c70' });
      if (d < 0.15) { ctx.fillStyle = `rgba(200,16,46,${0.6 * (1 - d / 0.15)})`; ctx.fillRect(0, 0, W, H); }
    } },
  ];
  // crack sets
  const CR_ARMS = [...cracksOf(5, 640, 880, 3, 70, 0.8), ...cracksOf(6, 820, 700, 3, 70, 0.8)];
  const CR_BODY = cracksOf(8, W / 2 + 30, 1050, 6, 60, 1.0);
  const CR_SMALL = cracksOf(10, 700, 1500, 4, 40, 1.0);
  const CR_FACE = cracksOf(12, W / 2, 1180, 6, 55, 0.9);
  const CR_SKY = [...cracksOf(14, 300, 80, 3, 120, 0.7), ...cracksOf(15, 800, 140, 3, 120, 0.7)];

  window.VERSION = {
    duration: C.end || 170,
    images,
    render(ctx, t, frame) {
      const sc = SCENES.find(s => t >= s.a && t < s.b) || SCENES[SCENES.length - 1];
      ctx.save(); ctx.setTransform(1, 0, 0, 1, 0, 0); ctx.globalAlpha = 1; ctx.globalCompositeOperation = 'source-over'; ctx.filter = 'none';
      ctx.fillStyle = '#000'; ctx.fillRect(0, 0, W, H);
      sc.draw.call(sc, ctx, t, frame);
      ctx.restore(); ctx.filter = 'none';
      K.grain(ctx, frame, 0.08); K.vignette(ctx, 0.7);
      if (t < 0.8) { ctx.fillStyle = `rgba(0,0,0,${1 - t / 0.8})`; ctx.fillRect(0, 0, W, H); }
    },
  };
})();
