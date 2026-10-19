// Version I — «chat.log». The third template (same 37.8 s track): a terminal chat log with an
// ASCII figure below, then a drop of scenes made of characters. Adapted for Kostya: everyone
// at war shoots — except him. Every scene is drawn into a low-res "source" canvas and turned
// into monospace characters by brightness; terminal lines are drawn crisp on top. Text and
// cuts "decode" out of random glyphs.
(function () {
  const { W, H } = K;
  const I = n => K.images[n];
  const KICKS = [17.54, 18.82, 19.27, 19.74, 20.40, 20.83, 21.33, 21.79, 22.42, 23.20, 23.37, 23.82, 24.11, 24.43, 26.61, 26.91,
    28.82, 29.27, 29.73, 30.82, 31.32, 31.80, 32.26, 32.43, 32.57, 33.19, 33.38, 33.61, 33.82, 34.42, 35.05, 35.26, 36.29, 37.34];
  const HATS = [17.88, 18.04, 18.18, 19.99, 20.17, 21.62, 22.26, 22.56, 22.87, 23.19, 23.42, 23.65, 25.39, 26.16, 26.31, 27.17,
    27.41, 27.57, 27.88, 28.05, 28.55, 29.91, 30.06, 30.38, 31.63, 31.78, 33.96, 34.67, 34.92, 35.38, 36.93];
  const since = (list, t) => { let b = 1e9; for (const k of list) if (k <= t) b = Math.min(b, t - k); return b; };
  const kick = t => Math.exp(-since(KICKS, t) * 8);
  const hat = t => Math.exp(-since(HATS, t) * 10);
  const count = (list, t, a, b) => list.filter(k => k >= a && k <= t && k < b).length;
  const e = (t, a, b) => K.ease.inOutSine(K.prog(t, a, b));
  const MONO = '"PT Mono"';
  const RED = '#ff3344', WHITE = '#ececec', SOFT = '#c4c4c4', GREY = '#8c8c8c';
  const JUNK = '!@#$%&*+=-?/\\|<>01ЖФЩЯ';

  // ================= ASCII machinery =================
  const CELL = 15, COLS = W / CELL, ROWS = Math.ceil(H / CELL); // 72 x 128 cells
  let src, sx;
  const RAMP = ' .:-=+*#%@';
  const glyph = new Map();
  const glyphFor = (ch, col) => {
    const key = ch + col; let g = glyph.get(key);
    if (!g) { const c = K.makeCanvas(CELL + 2, CELL + 4), x = c.getContext('2d'); x.font = `bold ${CELL + 2}px ${MONO}`; x.fillStyle = col; x.textBaseline = 'top'; x.fillText(ch, 1, 0); g = c; glyph.set(key, g); }
    return g;
  };
  const GREYS = ['#3a3a3a', '#5a5a5a', '#7c7c7c', '#a0a0a0', '#c8c8c8', '#f4f4f4'];
  const REDS = ['#5a1018', '#8c1622', '#c01e2e', '#ff3344'];
  const begin = () => {
    if (!src) { src = K.makeCanvas(COLS, ROWS); sx = src.getContext('2d', { willReadFrequently: true }); }
    sx.setTransform(1, 0, 0, 1, 0, 0); sx.globalAlpha = 1; sx.globalCompositeOperation = 'source-over'; sx.filter = 'none';
    sx.fillStyle = '#000'; sx.fillRect(0, 0, COLS, ROWS); return sx;
  };
  // source canvas → characters. flicker nudges a few glyphs; scramble swaps cells for junk (cuts / glitches)
  const ascii = (ctx, frame, o = {}) => {
    const d = sx.getImageData(0, 0, COLS, ROWS).data, fl = o.flicker ?? 0.04, sc = o.scramble || 0, ox = o.ox || 0, oy = o.oy || 0;
    for (let y = 0; y < ROWS; y++) for (let x = 0; x < COLS; x++) {
      const i = (y * COLS + x) * 4, r = d[i], g = d[i + 1], b = d[i + 2], L = (r * 0.3 + g * 0.59 + b * 0.11) / 255;
      if (L < 0.06) continue;
      let k = Math.min(RAMP.length - 1, Math.floor(L * RAMP.length));
      if (fl && K.hash(x * 131 + y, Math.floor(frame / 2)) < fl) k = Math.max(1, Math.min(RAMP.length - 1, k + (K.hash(x, y + frame) > 0.5 ? 1 : -1)));
      let ch = o.chars ? o.chars[(x + y * 3) % o.chars.length] : RAMP[k];
      if (sc && K.hash(x * 17 + y * 3, frame) < sc) ch = JUNK[Math.floor(K.hash(x, y * 7 + frame) * JUNK.length)];
      const red = r > 120 && r > g * 1.6;
      const col = red ? REDS[Math.min(3, Math.floor(r / 255 * 3.99))] : GREYS[Math.min(5, Math.floor(L * 6))];
      ctx.drawImage(glyphFor(ch, col), x * CELL + ox, y * CELL + oy);
    }
  };
  // terminal text; decode: each character arrives through a short burst of random glyphs
  const term = (ctx, s, x, y, o = {}) => { ctx.save(); ctx.font = `${o.bold ? 'bold ' : ''}${o.size || 40}px ${MONO}`; ctx.fillStyle = o.color || WHITE; ctx.textAlign = o.align || 'left'; ctx.globalAlpha *= o.alpha ?? 1; ctx.fillText(s, x, y); ctx.restore(); };
  const decode = (ctx, s, x, y, t, t0, o = {}) => {
    if (t < t0) return;
    const size = o.size || 40, adv = size * 0.6, rate = o.rate || 0.028, hold = o.hold || 0.12;
    let x0 = o.align === 'center' ? x - s.length * adv / 2 : x;
    ctx.save(); ctx.font = `${o.bold ? 'bold ' : ''}${size}px ${MONO}`; ctx.textAlign = 'left'; ctx.globalAlpha *= o.alpha ?? 1;
    for (let i = 0; i < s.length; i++) {
      const lt = t - t0 - i * rate; if (lt < 0) break;
      let ch = s[i], col = o.color || WHITE;
      if (lt < hold && ch !== ' ') { ch = JUNK[Math.floor(K.hash(i, Math.floor(t * 30)) * JUNK.length)]; col = o.junk || '#6f6f6f'; }
      ctx.fillStyle = col; ctx.fillText(ch, x0 + i * adv, y);
    }
    ctx.restore();
  };
  const typed = (s, t, t0, cps = 26) => s.slice(0, Math.floor(K.clamp((t - t0) * cps, 0, s.length)));
  const cursor = t => (Math.floor(t * 2.5) % 2 ? '_' : ' ');
  const srcText = (s, x, y, size, col = '#fff', font = `bold ${size}px ${MONO}`) => { sx.font = font; sx.fillStyle = col; sx.textAlign = 'center'; sx.textBaseline = 'middle'; sx.fillText(s, x, y); };
  // image into cells; a soft radial mask lets the background fall away (drawn through a scratch canvas so it never erases the rest)
  let scratch;
  const srcImage = (img, cx, cy, w, h, o = {}) => {
    if (!scratch) scratch = K.makeCanvas(COLS, ROWS);
    const q = scratch.getContext('2d'); q.setTransform(1, 0, 0, 1, 0, 0); q.globalCompositeOperation = 'source-over'; q.clearRect(0, 0, COLS, ROWS);
    q.filter = `grayscale(1) contrast(${o.contrast || 1.6}) brightness(${o.bright || 1.1})`;
    K.cover(q, img, { x: cx - w / 2, y: cy - h / 2, w, h }, o.z || 1, o.fx ?? 0.5, o.fy ?? 0.35);
    q.filter = 'none'; q.globalCompositeOperation = 'destination-in';
    const g = q.createRadialGradient(cx, cy - h * 0.05, Math.min(w, h) * 0.18, cx, cy, Math.max(w, h) * 0.55);
    g.addColorStop(0, 'rgba(0,0,0,1)'); g.addColorStop(1, 'rgba(0,0,0,0)'); q.fillStyle = g; q.fillRect(0, 0, COLS, ROWS);
    sx.save(); sx.globalAlpha = o.alpha ?? 1; sx.drawImage(scratch, o.dx || 0, 0); sx.restore();
  };
  const portrait = (t, o = {}) => srcImage(I('portrait'), COLS / 2, ROWS - 28, 64, 64, { fy: 0.42, contrast: 1.9, bright: 1.2 + 0.06 * Math.sin(t * 1.3) + (o.flash || 0), alpha: o.alpha ?? 1, dx: o.dx || 0 });

  // ================= chat.log (0..17.17) =================
  const LOG = [
    { who: 'артём', s: 'не спишь?', t: 0.4 },
    { who: 'костя', s: 'смена. раненых везут', t: 3.2, wait: 2.0 },
    { who: 'артём', s: 'можно тупой вопрос?', t: 6.4 },
    { who: 'костя', s: 'давай', t: 8.0, wait: 7.2 },
    { who: 'артём', s: 'ты там хоть раз стрелял?', t: 10.4 },
    { who: 'костя', s: 'на войне все стреляют', t: 13.0, wait: 11.8 },
    { who: 'костя', s: '(кроме меня)', t: 16.3, wait: 15.2, red: true },
  ];
  const LINE_TIMES = LOG.map(m => m.t);
  const chatLog = (ctx, t, frame) => {
    // Kostya in characters below; the glitch on the red line shakes him
    const red = t > 16.3, gl = red ? Math.exp(-(t - 16.3) * 4) : 0;
    begin(); portrait(t, { flash: 0.25 * Math.exp(-since(LINE_TIMES, t) * 5), dx: gl * (K.hash(frame, 3) - 0.5) * 8 });
    ascii(ctx, frame, { flicker: 0.04, scramble: gl * 0.35 + (t < 0.6 ? (0.6 - t) : 0) });
    // the log nudges forward on each new line, tilts away when the red line lands
    const nudge = Math.exp(-since(LINE_TIMES, t) * 6), tilt = e(t, 16.3, 17.0);
    ctx.save(); ctx.translate(W / 2, 520); ctx.scale(1 + 0.012 * nudge, 1 + 0.012 * nudge);
    ctx.transform(1, 0.05 * tilt, -0.18 * tilt, 1 - 0.12 * tilt, 0, 0); ctx.translate(-W / 2, -520);
    ctx.globalAlpha = 1 - 0.5 * tilt;
    term(ctx, '— chat.log', 44, 96, { size: 26, color: GREY }); term(ctx, '03:12', W - 44, 96, { size: 26, color: GREY, align: 'right' });
    ctx.fillStyle = 'rgba(255,255,255,0.16)'; ctx.fillRect(44, 114, W - 88, 1);
    let y = 200;
    for (const m of LOG) {
      const t0 = m.wait ?? m.t;
      if (t < t0) break;
      const lab = m.who + '>', col = m.who === 'артём' ? RED : WHITE, lw = 44 + (m.who.length + 2) * 26.4;
      term(ctx, lab, 44, y, { color: col, size: 44 });
      const body = t < m.t ? '.'.repeat(1 + Math.floor((t - t0) * 3) % 3) : typed(m.s, t, m.t, m.red ? 18 : 26);
      const isLast = LOG.filter(q => t >= (q.wait ?? q.t)).pop() === m;
      ctx.save(); ctx.font = `44px ${MONO}`; const lines = K.wrap(ctx, body + (isLast ? cursor(t) : ''), W - lw - 50); ctx.restore();
      lines.forEach((l, i) => {
        const jx = m.red && t - m.t < 0.5 ? (K.hash(frame, i) - 0.5) * 14 : 0;
        term(ctx, l, lw + jx, y + i * 56, { color: m.red ? RED : WHITE, bold: m.red, size: 44 });
      });
      y += lines.length * 56 + 20;
    }
    ctx.restore();
    if (red && t - 16.3 < 0.12) { ctx.fillStyle = 'rgba(255,40,60,0.18)'; ctx.fillRect(0, 0, W, H); }
  };

  // ================= drop scenes =================
  const CROSSH = (x, y, r, col, rot = 0) => {
    sx.save(); sx.translate(x, y); sx.rotate(rot); sx.strokeStyle = col; sx.lineWidth = 1.3; sx.beginPath(); sx.arc(0, 0, r, 0, 7);
    sx.moveTo(-r - 3, 0); sx.lineTo(-r + 2, 0); sx.moveTo(r - 2, 0); sx.lineTo(r + 3, 0); sx.moveTo(0, -r - 3); sx.lineTo(0, -r + 2); sx.moveTo(0, r - 2); sx.lineTo(0, r + 3); sx.stroke(); sx.restore();
  };
  const soldier = (x, y, s, col = '#bbb', flip = 1, recoil = 0) => {
    x -= flip * recoil;
    sx.fillStyle = col; sx.beginPath(); sx.arc(x, y - 9 * s, 2.2 * s, 0, 7); sx.fill();
    sx.fillRect(x - 2.6 * s, y - 12.4 * s, 5.2 * s, 1.6 * s);
    sx.fillRect(x - 2.2 * s, y - 6.5 * s, 4.4 * s, 8 * s); sx.fillRect(x - 2 * s, y + 1.5 * s, 1.6 * s, 6 * s); sx.fillRect(x + 0.4 * s, y + 1.5 * s, 1.6 * s, 6 * s);
    sx.fillRect(flip > 0 ? x + 1 * s : x - 10 * s, y - 4.5 * s, 9 * s, 1.3 * s);
  };
  const medic = (x, y, s, glow = 1) => { // Kostya: no rifle, a red cross
    sx.fillStyle = `rgba(255,255,255,${glow})`; sx.beginPath(); sx.arc(x, y - 9 * s, 2.3 * s, 0, 7); sx.fill();
    sx.fillRect(x - 2.4 * s, y - 6.5 * s, 4.8 * s, 8 * s); sx.fillRect(x - 2 * s, y + 1.5 * s, 1.6 * s, 6 * s); sx.fillRect(x + 0.4 * s, y + 1.5 * s, 1.6 * s, 6 * s);
    sx.fillStyle = '#ff3344'; sx.fillRect(x - 0.6 * s, y - 5.5 * s, 1.2 * s, 4.4 * s); sx.fillRect(x - 2 * s, y - 4 * s, 4 * s, 1.2 * s);
  };
  // hands: drawn bright with a dark rim (top, the rescuer) and dim (bottom, the wounded) so they never merge
  const handShape = (c, curl = 1) => {
    c.fillRect(-3.2, 2, 6.4, 34); c.beginPath(); c.ellipse(0, 0, 5.6, 6.2, 0, 0, 7); c.fill();
    [[-4.8, -0.15, 7], [-1.6, -0.04, 9], [1.6, 0.04, 8.5], [4.6, 0.16, 6.5]].forEach(([fx, a, L]) => { c.save(); c.translate(fx, -4); c.rotate(a); c.fillRect(-0.8, -L * curl, 1.6, L * curl); c.restore(); });
    c.save(); c.translate(5.2, 1); c.rotate(-1.0); c.fillRect(-0.85, -6.5, 1.7, 6.5); c.restore();
  };
  const hand = (x, y, s, rot, col, rim, curl = 1) => {
    sx.save(); sx.translate(x, y); sx.rotate(rot); sx.scale(s, s);
    if (rim) { sx.save(); sx.fillStyle = '#000'; sx.scale(1.22, 1.12); handShape(sx, curl); sx.restore(); }
    sx.fillStyle = col; handShape(sx, curl); sx.restore();
  };
  const greyVignette = (ctx, a = 1) => { const g = ctx.createRadialGradient(W / 2, H / 2, 50, W / 2, H / 2, 1000); g.addColorStop(0, `rgba(96,96,96,${a})`); g.addColorStop(1, '#070707'); ctx.fillStyle = g; ctx.fillRect(0, 0, W, H); };

  const SCENES = [
    { a: 17.17, b: 17.63, draw(ctx, t, frame) { // the kill counter tumbles and lands on 0
      greyVignette(ctx);
      const rolling = t < 17.45, digit = rolling ? String(Math.floor(K.hash(Math.floor(t * 30), 3) * 99)) : '0';
      term(ctx, 'убито: ' + digit + cursor(t), W / 2 - 170, H / 2 + 18, { size: 64, color: rolling ? '#bdbdbd' : '#ffffff' });
    } },
    { a: 17.63, b: 18.63, draw(ctx, t, frame) { // every soldier has a target; the brackets close in on the kick
      begin();
      let k = 0;
      for (let gy = 0; gy < 5; gy++) for (let gx = 0; gx < 3; gx++) {
        const x = 13 + gx * 23, y = 14 + gy * 25; if (gy === 2 && gx === 1) continue;
        CROSSH(x, y, 7 + 1.2 * hat(t), '#d8d8d8', t * (k % 2 ? 0.8 : -0.8));
        if (Math.floor((t - 17.63) * 8) % 14 === k) { sx.fillStyle = '#ff3344'; sx.fillRect(x - 1.5, y - 1.5, 3, 3); } else { sx.fillStyle = '#7a1520'; sx.fillRect(x - 0.7, y - 0.7, 1.4, 1.4); }
        k++;
      }
      const q = K.ease.outBack(K.clamp((t - 17.7) / 0.35)), cx = COLS / 2, cy = 64;
      sx.strokeStyle = '#ff3344'; sx.lineWidth = 3; sx.beginPath(); sx.arc(cx - 8 - (1 - q) * 24, cy, 14, 1.9, 4.4); sx.stroke(); sx.beginPath(); sx.arc(cx + 8 + (1 - q) * 24, cy, 14, -1.25, 1.25); sx.stroke();
      CROSSH(cx, cy, 6, '#fff', t * 2);
      ascii(ctx, frame, { scramble: Math.max(0, 0.6 - (t - 17.63) * 4) });
      decode(ctx, 'у каждого ( своя цель )', W / 2, H - 150, t, 18.0, { size: 40, color: SOFT, align: 'center' });
    } },
    { a: 18.63, b: 19.27, draw(ctx, t, frame) { // two soldiers fire at each other, recoiling
      begin();
      const shots = [18.82, 18.98, 19.12], f = since(shots, t), rc = Math.exp(-f * 18) * 1.6;
      soldier(17, 76, 2.5, '#d0d0d0', 1, rc); soldier(55, 76, 2.5, '#d0d0d0', -1, rc);
      if (f < 0.08) { sx.fillStyle = '#ffe2a0'; sx.beginPath(); sx.arc(42.5, 64.5, 3.5, 0, 7); sx.arc(29.5, 64.5, 3.5, 0, 7); sx.fill(); }
      for (let i = 0; i < 4; i++) { const q = ((t - 18.63) * 3 + i / 4) % 1; sx.fillStyle = '#fff'; sx.fillRect(28 + q * 16, 64.4, 2, 0.9); sx.fillRect(44 - q * 16, 65.4, 2, 0.9); }
      ascii(ctx, frame, { scramble: f < 0.06 ? 0.25 : 0 });
    } },
    { a: 19.27, b: 20.13, draw(ctx, t, frame) { // a cross of plus signs flies together, then beats
      begin();
      const q = K.ease.outCubic(K.prog(t, 19.27, 19.6)), s = 1 + 0.14 * Math.exp(-since([19.74, 19.99], t) * 7), cx = COLS / 2, cy = 60, r = K.rng(8);
      sx.fillStyle = '#ff3344';
      for (let i = 0; i < 900; i++) {
        const v = r() < 0.5, tx = v ? cx + (r() - 0.5) * 14 * s : cx + (r() - 0.5) * 48 * s, ty = v ? cy + (r() - 0.5) * 48 * s : cy + (r() - 0.5) * 14 * s;
        const fx = cx + (r() - 0.5) * 140, fy = cy + (r() - 0.5) * 200;
        sx.fillRect(K.lerp(fx, tx, q), K.lerp(fy, ty, q), 1, 1);
      }
      ascii(ctx, frame, { chars: '+', flicker: 0 });
      decode(ctx, 'медик', W / 2, H - 300, t, 19.75, { size: 46, color: SOFT, align: 'center' });
    } },
    { a: 20.13, b: 20.67, draw(ctx, t, frame) { // the screen fills with shots, a red wave sweeping through
      ctx.save(); ctx.font = `bold 32px ${MONO}`;
      const wave = (t - 20.13) * 60;
      for (let y = 0; y < 33; y++) for (let x = 0; x < 9; x++) {
        const d = Math.abs((x * 3 + y) - wave) , on = d < 4 || K.hash(x * 7 + y, Math.floor(t * 24)) > 0.82;
        ctx.fillStyle = on ? RED : '#4a4a4a'; ctx.fillText('бах', 14 + x * 118 + (on ? (K.hash(x, y + Math.floor(t * 30)) - 0.5) * 8 : 0), 54 + y * 57);
      }
      ctx.restore();
    } },
    { a: 20.67, b: 21.5, draw(ctx, t, frame) { // +1, +2, +3 on the kicks
      begin();
      const n = 1 + count(KICKS, t, 20.8, 21.5), pop = 1 + 0.25 * kick(t);
      srcText('+' + n, COLS / 2, 64, 34 * pop, '#ff3344');
      ascii(ctx, frame, { chars: '+' + n, scramble: kick(t) * 0.3 });
      decode(ctx, 'спасено', W / 2, H - 520, t, 20.75, { size: 42, color: SOFT, align: 'center' });
    } },
    { a: 21.5, b: 22.63, draw(ctx, t, frame) { // heal --all: a fast scrolling log
      term(ctx, '> ' + typed('лечить --всех', t, 21.55, 34) + (t < 21.95 ? cursor(t) : ''), 70, 640, { size: 46 });
      const log = ['перевязка ......... ok', 'жгут ............... ok', 'морфий ............. ok', 'шина ............... ok', 'носилки ............ ok', 'эвакуация .......... ok'];
      const n = count(HATS, t, 21.6, 22.63) + count(KICKS, t, 21.6, 22.63);
      log.slice(0, Math.min(n, log.length)).forEach((l, i) => term(ctx, l, 70, 720 + i * 54, { size: 36, color: i === n - 1 ? WHITE : GREY }));
      const p = K.clamp(0.15 + n * 0.11, 0, 0.7), m = Math.round(p * 16);
      term(ctx, '[' + '#'.repeat(m) + '.'.repeat(16 - m) + ']  ' + Math.round(p * 100) + '%', 70, 1100, { size: 42, color: SOFT });
    } },
    { a: 22.63, b: 24.43, draw(ctx, t, frame) { // the sniper's eye sweeps the line, finds the one with the cross
      begin();
      const cx = COLS / 2, cy = 38, open = K.ease.outCubic(K.prog(t, 22.63, 22.9)), blink = since([23.65], t) < 0.12 ? 0.15 : 1;
      const look = t < 23.37 ? Math.sin((t - 22.63) * 5) * 7 : K.lerp(Math.sin(0.74 * 5) * 7, 0, K.clamp((t - 23.37) / 0.2));
      sx.strokeStyle = '#e0e0e0'; sx.lineWidth = 1.6; sx.beginPath(); sx.ellipse(cx, cy, 27, 13 * open * blink, 0, 0, 7); sx.stroke();
      if (open * blink > 0.5) { sx.fillStyle = '#cfcfcf'; sx.beginPath(); sx.arc(cx + look, cy, 8.5, 0, 7); sx.fill(); sx.fillStyle = '#000'; sx.beginPath(); sx.arc(cx + look, cy, 3.6, 0, 7); sx.fill(); sx.fillStyle = '#fff'; sx.fillRect(cx + look + 2, cy - 4, 2, 2); }
      for (let i = 0; i < 6; i++) { const x = 6 + i * 12 + (i >= 3 ? 12 : 0); soldier(x, 106, 1.25, '#8f8f8f', i < 3 ? 1 : -1); }
      medic(cx, 106, 1.35, 0.7 + 0.3 * kick(t));
      // the sight finds him
      if (t > 23.37) { const q = K.ease.outBack(K.clamp((t - 23.37) / 0.3)); CROSSH(cx, 96, 9 * (2 - q), '#ff3344', (1 - q) * 3); }
      ascii(ctx, frame, { scramble: Math.max(0, 0.5 - (t - 22.63) * 3) });
      decode(ctx, 'снайпер видит всех', W / 2, 1300, t, 22.85, { size: 40, color: SOFT, align: 'center' });
    } },
    { a: 24.43, b: 25.13, draw(ctx, t, frame) { // ammo = NULL
      greyVignette(ctx, 0.6); ctx.fillStyle = 'rgba(0,0,0,0.5)'; ctx.fillRect(0, 0, W, H);
      const j = Math.floor(t * 30) % 4 === 0 ? 10 : 0, s = 1 + 0.06 * Math.sin((t - 24.43) * 20) * Math.exp(-(t - 24.43) * 4);
      decode(ctx, 'патроны =', W / 2, H / 2 - 120, t, 24.45, { size: 40, color: GREY, align: 'center' });
      ctx.save(); ctx.translate(W / 2, H / 2 + 40); ctx.rotate(-0.08); ctx.scale(s, s);
      for (const [dx, col, a] of [[-j, '#ff3344', 0.6], [j, '#33d6ff', 0.5], [0, '#d0d0d0', 1]]) term(ctx, 'NULL', dx, 0, { size: 170, bold: true, color: col, align: 'center', alpha: a });
      ctx.restore();
    } },
    { a: 25.13, b: 26.9, draw(ctx, t, frame) { // a rain of bullets; he carries someone through it
      begin();
      const walk = e(t, 25.13, 26.9) * 30, kx = 30 + walk, ky = 102, step = Math.sin(t * 8) * 2.2;
      // Kostya, leaning forward, one arm back holding the wounded man by the collar
      sx.save(); sx.translate(kx, ky); sx.rotate(0.28);
      sx.fillStyle = '#f2f2f2'; sx.beginPath(); sx.arc(0, -24, 3, 0, 7); sx.fill(); sx.fillRect(-3.4, -20, 6.8, 14);
      sx.fillStyle = '#ff3344'; sx.fillRect(-0.8, -17, 1.6, 5); sx.fillRect(-2.4, -15.4, 4.8, 1.6);
      sx.restore();
      sx.fillStyle = '#f2f2f2';
      sx.save(); sx.translate(kx - 2, ky - 14); sx.rotate(0.54); sx.fillRect(-1, 0, 2, 17); sx.restore(); // the arm reaching back down to the collar
      sx.fillRect(kx - 1 + step, ky - 6, 2.2, 11); sx.fillRect(kx + 2 - step, ky - 6, 2.2, 11);
      // the wounded, on his back, dragged
      sx.fillStyle = '#9c9c9c'; sx.beginPath(); sx.arc(kx - 11, ky + 1, 2.6, 0, 7); sx.fill(); sx.fillRect(kx - 29, ky - 1, 17, 4.4); sx.fillRect(kx - 36, ky + 0.5, 8, 2.2);
      sx.fillStyle = '#5a5a5a'; for (let k = 0; k < 6; k++) sx.fillRect(kx - 40 - k * 4 - (t * 30) % 4, ky + 4.5, 2, 0.8); // drag marks
      // sparks where bullets hit the ground
      for (const h of [25.39, 26.16, 26.31, 26.61]) { const q = K.clamp((t - h) / 0.3); if (q > 0 && q < 1) { const hx = (h * 37) % COLS; for (let k = 0; k < 6; k++) { sx.fillStyle = '#ffd38a'; sx.fillRect(hx + (k - 3) * 2 * q, ky + 6 - q * 6 * Math.abs(k - 3) / 3, 1, 1); } } }
      sx.fillStyle = '#333'; sx.fillRect(0, ky + 6, COLS, 1);
      ascii(ctx, frame);
      ctx.save(); ctx.font = `bold 28px ${MONO}`;
      for (let i = 0; i < 60; i++) { const r = K.rng(i), x = r() * W, sp = 600 + r() * 1100, y = ((t * sp + r() * H) % (H + 200)) - 100; ctx.fillStyle = r() > 0.85 ? RED : '#6a6a6a'; ctx.fillText('|', x, y); ctx.fillText(r() > 0.5 ? '|' : "'", x, y - 32); }
      ctx.restore();
    } },
    { a: 26.9, b: 29.0, draw(ctx, t, frame) { // the hands: reaching, the grip, and the pull
      begin();
      const cx = COLS / 2, cy = 62;
      const reach = K.ease.inOutCubic(K.clamp(count(HATS, t, 27.1, 28.1) / 4 + K.prog(t, 26.9, 28.05) * 0.25)); // closer on every hat
      const gripT = 28.05, pull = K.ease.inOutCubic(K.prog(t, gripT + 0.15, 28.95)) * 22;
      if (t < gripT) {
        const gap = K.lerp(30, 6, reach), tremble = Math.sin(t * 40) * 0.6;
        hand(cx + gap * 0.45 + tremble, cy + gap + 10, 2.1, 0.35, '#8e8e8e', false);        // the wounded, lower, dim, shaking
        hand(cx - gap * 0.45, cy - gap - 10, 2.1, Math.PI + 0.35, '#f4f4f4', true);           // the rescuer, from above
      } else {
        // clasped: the rescuer's palm lies over the wounded hand, fingers curled round it; both rise together
        hand(cx + 2.7, cy + 16 - pull, 2.1, 0.35, '#8e8e8e', false, 0.85);
        hand(cx - 2.7, cy - 4 - pull, 2.1, Math.PI + 0.35, '#f4f4f4', true, 0.62);
        const f = K.clamp((t - gripT) / 0.5);
        if (f < 1) { sx.strokeStyle = `rgba(255,51,68,${1 - f})`; sx.lineWidth = 1.6; sx.beginPath(); sx.arc(cx, cy + 6 - pull, 6 + 30 * f, 0, 7); sx.stroke(); }
      }
      ascii(ctx, frame, { flicker: 0.05, scramble: since([gripT], t) < 0.1 ? 0.3 : 0 });
      if (t > gripT) decode(ctx, 'держу.', W / 2, H - 260, t, gripT + 0.1, { size: 54, color: WHITE, align: 'center', bold: true });
    } },
    { a: 29.0, b: 29.8, draw(ctx, t, frame) { // searching… found
      decode(ctx, 'поиск раненых...', 70, 840, t, 29.02, { size: 44 });
      const p = Math.min(0.99, 0.5 + K.prog(t, 29.0, 29.45) * 0.49), n = Math.round(p * 16);
      term(ctx, '[' + '#'.repeat(n) + '.'.repeat(16 - n) + ']  ' + Math.round(p * 100) + '%', 70, 910, { size: 42, color: SOFT });
      for (let i = 0; i < 4; i++) if (t > 29.05 + i * 0.07) term(ctx, `сектор ${7 + i}: ${i === 3 ? 'есть сигнал' : 'пусто'}`, 70, 990 + i * 50, { size: 34, color: i === 3 ? WHITE : '#666' });
      if (t > 29.27) { const s = 1 + 0.2 * kick(t); ctx.save(); ctx.translate(70, 1270); ctx.scale(s, s); term(ctx, 'НАЙДЕН: 1', 0, 0, { size: 64, color: RED, bold: true }); ctx.restore(); }
    } },
    { a: 29.8, b: 30.13, draw(ctx, t, frame) { begin(); sx.fillStyle = '#ff3344'; sx.fillRect(COLS / 2 - 2.5, 56, 5, 16); sx.fillRect(COLS / 2 - 8, 61.5, 16, 5); ascii(ctx, frame, { scramble: 0.35, ox: (K.hash(frame, 1) - 0.5) * 40 }); } },
    { a: 30.13, b: 31.5, draw(ctx, t, frame) { // ping: no losses
      term(ctx, '> ' + typed('ping санитар', t, 30.15, 34), 70, 760, { size: 46 });
      [30.38, 30.82, 31.32].forEach((h, i) => { if (t > h) decode(ctx, `ответ от санитара: ${[12, 9, 7][i]} мс`, 70, 840 + i * 60, t, h, { size: 38, color: SOFT }); });
      if (t > 31.32) { const s = 1 + 0.15 * kick(t); ctx.save(); ctx.translate(70, 1160); ctx.scale(s, s); term(ctx, '0% потерь.', 0, 0, { size: 110, color: RED, bold: true }); ctx.restore(); }
    } },
    { a: 31.5, b: 32.63, draw(ctx, t, frame) { // a cross made of the word "жив" assembles and beats
      begin();
      const q = K.ease.outCubic(K.prog(t, 31.5, 32.1)), s = 1 + 0.1 * kick(t), cx = COLS / 2, cy = 62, r = K.rng(4);
      sx.fillStyle = '#ff3344';
      for (let i = 0; i < 700; i++) {
        const v = r() < 0.5, tx = v ? cx + (r() - 0.5) * 14 * s : cx + (r() - 0.5) * 48 * s, ty = v ? cy + (r() - 0.5) * 48 * s : cy + (r() - 0.5) * 14 * s;
        sx.fillRect(K.lerp(r() * COLS, tx, q), K.lerp(r() * ROWS, ty, q), 1, 1);
      }
      ascii(ctx, frame, { chars: 'жив', flicker: 0 });
    } },
    { a: 32.63, b: 34.1, draw(ctx, t, frame) { // what a medic says, all around him
      begin(); portrait(t, { flash: 0.3 * kick(t) }); ascii(ctx, frame);
      const W2 = [['дыши.', 90, 300, 32.65], ['я рядом', 600, 420, 33.0], ['держись', 160, 600, 33.19], ['живой?', 680, 740, 33.38], ['смотри на меня', 200, 900, 33.61], ['всё будет хорошо', 470, 1040, 33.82]];
      W2.forEach(([s, x, y, t0]) => decode(ctx, s, x, y, t, t0, { size: 40, color: SOFT }));
    } },
    { a: 34.1, b: 36.29, draw(ctx, t, frame) { // orders shouted from everywhere — then "все"
      begin(); portrait(t, { alpha: 0.8 }); ascii(ctx, frame);
      const O = [['огонь!', 70, 260, 34.42], ['пли!', 740, 330, 34.67], ['цель вижу', 120, 480, 34.92], ['стреляй!', 640, 600, 35.05], ['заряжай!', 210, 720, 35.26], ['огонь!', 720, 840, 35.38], ['пли!', 360, 380, 35.7]];
      O.forEach(([s, x, y, t0]) => { if (t > t0) { const sh = Math.exp(-(t - t0) * 6) * 10; term(ctx, s, x + (K.hash(frame, x) - 0.5) * sh, y, { size: 42, color: '#c42a3a', bold: true }); } });
      if (t > 35.6) { const s = 1 + 0.1 * kick(t); ctx.save(); ctx.translate(W / 2, 1160); ctx.scale(s, s); decode(ctx, 'все', 0, 0, t, 35.6, { size: 72, color: WHITE, align: 'center', bold: true, rate: 0.08 }); ctx.restore(); }
    } },
    { a: 36.29, b: 37.85, draw(ctx, t, frame) { // the punchline in big ASCII letters, then it falls apart
      begin();
      const g = Math.floor(t * 18) % 5 === 0 ? 2 : 0, fall = K.prog(t, 37.2, 37.75);
      const q1 = K.ease.outBack(K.clamp((t - 36.29) / 0.25)), q2 = K.ease.outBack(K.clamp((t - 36.45) / 0.25));
      sx.save(); sx.translate(COLS / 2, 50); sx.scale(q1, q1); srcText('(КРОМЕ', g, 0, 15, '#ececec'); sx.restore();
      sx.save(); sx.translate(COLS / 2, 72); sx.scale(q2, q2); srcText('НЕГО)', -g, 0, 18, '#ff3344'); sx.restore();
      if (fall > 0) { const img = sx.getImageData(0, 0, COLS, ROWS); begin(); sx.putImageData(img, 0, Math.round(fall * fall * 60)); }
      ascii(ctx, frame, { flicker: 0.08, scramble: since([36.29, 36.45, 36.93], t) < 0.08 ? 0.4 : fall * 0.6 });
    } },
  ];

  window.VERSION = {
    duration: 37.83,
    images: { portrait: '/assets/images/portrait-drawn.png' },
    render(ctx, t, frame) {
      ctx.fillStyle = '#000'; ctx.fillRect(0, 0, W, H);
      if (t < 17.17) chatLog(ctx, t, frame);
      else {
        const s = SCENES.find(x => t >= x.a && t < x.b);
        const lp = s ? K.prog(t, s.a, s.b) : 0;
        const z = (1.06 - 0.06 * K.ease.outCubic(lp)) * (1 + 0.04 * kick(t)), sh = 14 * kick(t);
        ctx.save(); ctx.translate(W / 2 + (K.hash(frame, 1) - 0.5) * sh, H / 2 + (K.hash(frame, 2) - 0.5) * sh); ctx.scale(z, z); ctx.rotate((K.hash(Math.floor(t * 2), 5) - 0.5) * 0.01); ctx.translate(-W / 2, -H / 2);
        if (s) s.draw(ctx, t, frame);
        ctx.restore();
        // cut: brief flash and horizontal tear
        if (s && t - s.a < 0.05) { ctx.fillStyle = 'rgba(255,255,255,0.18)'; ctx.fillRect(0, 0, W, H); }
        if (s && t - s.a < 0.09) { const r = K.rng(frame); for (let i = 0; i < 6; i++) { const y = r() * H, h = 16 + r() * 70; ctx.drawImage(ctx.canvas, 0, y, W, h, (r() - 0.5) * 90, y, W, h); } }
      }
      // faint CRT scanline sweep
      const sy = ((t * 260) % (H + 300)) - 150; const g = ctx.createLinearGradient(0, sy - 80, 0, sy + 80); g.addColorStop(0, 'rgba(255,255,255,0)'); g.addColorStop(0.5, 'rgba(255,255,255,0.035)'); g.addColorStop(1, 'rgba(255,255,255,0)'); ctx.fillStyle = g; ctx.fillRect(0, sy - 80, W, 160);
      K.grain(ctx, frame, 0.05);
      K.vignette(ctx, 0.45);
    },
  };
})();
