// Version I — «chat.log». The third template (same 37.8 s track): a terminal chat log with an
// ASCII figure below, then a drop of scenes made of characters. Adapted for Kostya: everyone
// at war shoots — except him. Every scene is drawn into a low-res "source" canvas and turned
// into monospace characters by brightness; terminal lines are drawn crisp on top.
(function () {
  const { W, H } = K;
  const I = n => K.images[n];
  const KICKS = [17.54, 18.82, 19.27, 19.74, 20.40, 20.83, 21.33, 21.79, 22.42, 23.20, 23.37, 23.82, 24.11, 24.43, 26.61, 26.91,
    28.82, 29.27, 29.73, 30.82, 31.32, 31.80, 32.26, 32.43, 32.57, 33.19, 33.38, 33.61, 33.82, 34.42, 35.05, 35.26, 36.29, 37.34];
  const HATS = [17.88, 18.04, 18.18, 19.99, 20.17, 21.62, 22.26, 22.56, 22.87, 23.19, 23.42, 23.65, 25.39, 26.16, 26.31, 27.17,
    27.41, 27.57, 27.88, 28.05, 28.55, 29.91, 30.06, 30.38, 31.63, 31.78, 33.96, 34.67, 34.92, 35.38, 36.93];
  const since = (list, t) => { let b = 1e9; for (const k of list) if (k <= t) b = Math.min(b, t - k); return b; };
  const kick = t => Math.exp(-since(KICKS, t) * 8);
  const count = (list, t, a, b) => list.filter(k => k >= a && k <= t && k < b).length;
  const e = (t, a, b) => K.ease.inOutSine(K.prog(t, a, b));
  const MONO = '"PT Mono"';
  const RED = '#ff3344', WHITE = '#e9e9e9', GREY = '#8c8c8c';

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
  const GREYS = ['#3a3a3a', '#5a5a5a', '#7c7c7c', '#a0a0a0', '#c8c8c8', '#f0f0f0'];
  const REDS = ['#5a1018', '#8c1622', '#c01e2e', '#ff3344'];
  const begin = () => { if (!src) { src = K.makeCanvas(COLS, ROWS); sx = src.getContext('2d', { willReadFrequently: true }); } sx.setTransform(1, 0, 0, 1, 0, 0); sx.globalAlpha = 1; sx.filter = 'none'; sx.fillStyle = '#000'; sx.fillRect(0, 0, COLS, ROWS); return sx; };
  // turn the source canvas into characters; flicker swaps a few glyphs per frame
  const ascii = (ctx, frame, o = {}) => {
    const d = sx.getImageData(0, 0, COLS, ROWS).data, fl = o.flicker ?? 0.04, ox = o.ox || 0, oy = o.oy || 0;
    for (let y = 0; y < ROWS; y++) for (let x = 0; x < COLS; x++) {
      const i = (y * COLS + x) * 4, r = d[i], g = d[i + 1], b = d[i + 2], L = (r * 0.3 + g * 0.59 + b * 0.11) / 255;
      if (L < 0.06) continue;
      let k = Math.min(RAMP.length - 1, Math.floor(L * RAMP.length));
      if (fl && K.hash(x * 131 + y, Math.floor(frame / 2)) < fl) k = Math.max(1, Math.min(RAMP.length - 1, k + (K.hash(x, y + frame) > 0.5 ? 1 : -1)));
      const ch = o.chars ? o.chars[(x + y * 3) % o.chars.length] : RAMP[k];
      const red = r > 120 && r > g * 1.6;
      const col = red ? REDS[Math.min(3, Math.floor(r / 255 * 3.99))] : GREYS[Math.min(5, Math.floor(L * 6))]; // reds keyed on the red channel so they stay bright
      ctx.drawImage(glyphFor(ch, col), x * CELL + ox, y * CELL + oy);
    }
  };
  const term = (ctx, s, x, y, o = {}) => { ctx.save(); ctx.font = `${o.bold ? 'bold ' : ''}${o.size || 34}px ${MONO}`; ctx.fillStyle = o.color || WHITE; ctx.textAlign = o.align || 'left'; ctx.globalAlpha *= o.alpha ?? 1; ctx.fillText(s, x, y); ctx.restore(); };
  const typed = (s, t, t0, cps = 26) => s.slice(0, Math.floor(K.clamp((t - t0) * cps, 0, s.length)));
  const cursor = t => (Math.floor(t * 2.5) % 2 ? '_' : ' ');
  // draw text into the source canvas in cell units (for big ASCII letters)
  const srcText = (s, x, y, size, col = '#fff', font = `bold ${size}px ${MONO}`) => { sx.font = font; sx.fillStyle = col; sx.textAlign = 'center'; sx.textBaseline = 'middle'; sx.fillText(s, x, y); };
  const srcImage = (img, cx, cy, w, h, o = {}) => { // image into cells with a soft radial mask so the background falls away
    sx.save(); sx.filter = `grayscale(1) contrast(${o.contrast || 1.6}) brightness(${o.bright || 1.1})`;
    K.cover(sx, img, { x: cx - w / 2, y: cy - h / 2, w, h }, o.z || 1, o.fx ?? 0.5, o.fy ?? 0.35);
    sx.filter = 'none'; sx.globalCompositeOperation = 'destination-in';
    const g = sx.createRadialGradient(cx, cy - h * 0.05, Math.min(w, h) * 0.18, cx, cy, Math.max(w, h) * 0.55);
    g.addColorStop(0, 'rgba(0,0,0,1)'); g.addColorStop(1, 'rgba(0,0,0,0)'); sx.fillStyle = g; sx.fillRect(cx - w, cy - h, w * 2, h * 2);
    sx.restore();
    if (o.tint) { sx.save(); sx.globalCompositeOperation = 'multiply'; sx.fillStyle = o.tint; sx.fillRect(cx - w / 2, cy - h / 2, w, h); sx.restore(); }
  };

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
  const chatLog = (ctx, t, frame) => {
    ctx.fillStyle = '#000'; ctx.fillRect(0, 0, W, H);
    // Kostya in characters at the bottom, breathing slightly
    begin(); srcImage(I('portrait'), COLS / 2, ROWS - 28, 62, 62, { fy: 0.42, contrast: 1.9, bright: 1.25 + 0.05 * Math.sin(t * 1.3) });
    ascii(ctx, frame, { flicker: 0.03 });
    // the log tilts away when the red line lands
    const tilt = e(t, 16.3, 17.0);
    ctx.save(); ctx.translate(W / 2, 600); ctx.transform(1, 0.04 * tilt, -0.15 * tilt, 1 - 0.1 * tilt, 0, 0); ctx.translate(-W / 2, -600);
    ctx.globalAlpha = 1 - 0.55 * tilt;
    term(ctx, '— chat.log', 40, 92, { size: 24, color: GREY }); term(ctx, '03:12', W - 40, 92, { size: 24, color: GREY, align: 'right' });
    ctx.fillStyle = 'rgba(255,255,255,0.15)'; ctx.fillRect(40, 108, W - 80, 1);
    let y = 190;
    for (const m of LOG) {
      const t0 = m.wait ?? m.t;
      if (t < t0) break;
      const lab = m.who + '>', col = m.who === 'артём' ? RED : WHITE;
      term(ctx, lab, 40, y, { color: col, size: 38 });
      const lw = 40 + (m.who.length + 2) * 22.8;
      let body;
      if (t < m.t) body = '.'.repeat(1 + Math.floor((t - t0) * 3) % 3); // waiting dots
      else body = typed(m.s, t, m.t);
      const isLast = LOG.filter(q => t >= (q.wait ?? q.t)).pop() === m;
      ctx.save(); ctx.font = `38px ${MONO}`;
      const lines = K.wrap(ctx, body + (isLast ? cursor(t) : ''), W - lw - 50); ctx.restore();
      lines.forEach((l, i) => term(ctx, l, lw, y + i * 50, { color: m.red ? RED : WHITE, bold: m.red, size: 38 }));
      y += lines.length * 50 + 18;
    }
    ctx.restore();
  };

  // ================= drop scenes =================
  const CROSSH = (x, y, r, col) => { sx.strokeStyle = col; sx.lineWidth = 1.2; sx.beginPath(); sx.arc(x, y, r, 0, 7); sx.moveTo(x - r - 2, y); sx.lineTo(x + r + 2, y); sx.moveTo(x, y - r - 2); sx.lineTo(x, y + r + 2); sx.stroke(); };
  const soldier = (x, y, s, col = '#bbb', flip = 1) => { // a simple standing soldier with a rifle, in cell units
    sx.fillStyle = col; sx.beginPath(); sx.arc(x, y - 9 * s, 2.2 * s, 0, 7); sx.fill();
    sx.fillRect(x - 2.5 * s, y - 13 * s, 5 * s, 2 * s); // helmet brim
    sx.fillRect(x - 2.2 * s, y - 6.5 * s, 4.4 * s, 8 * s); sx.fillRect(x - 2 * s, y + 1.5 * s, 1.6 * s, 6 * s); sx.fillRect(x + 0.4 * s, y + 1.5 * s, 1.6 * s, 6 * s);
    sx.fillRect(x + flip * 1 * s - (flip < 0 ? 9 * s : 0), y - 4.5 * s, 9 * s, 1.2 * s); // rifle
  };
  const lying = (x, y, s, col = '#999') => { sx.fillStyle = col; sx.beginPath(); sx.arc(x + 8 * s, y - 1.5 * s, 2 * s, 0, 7); sx.fill(); sx.fillRect(x - 8 * s, y - 2.5 * s, 14 * s, 3.4 * s); };
  const hand = (x, y, s, rot, col) => { // an open hand + forearm with clearly separated fingers
    sx.save(); sx.translate(x, y); sx.rotate(rot); sx.scale(s, s); sx.fillStyle = col;
    sx.fillRect(-3.2, 2, 6.4, 30); sx.beginPath(); sx.ellipse(0, 0, 5.6, 6.2, 0, 0, 7); sx.fill();
    [[-4.8, -0.15], [-1.6, -0.04], [1.6, 0.04], [4.6, 0.16]].forEach(([fx, a], i) => { sx.save(); sx.translate(fx, -4); sx.rotate(a); sx.fillRect(-0.75, -[7, 9, 8.5, 6.5][i], 1.5, [7, 9, 8.5, 6.5][i]); sx.restore(); });
    sx.save(); sx.translate(5.2, 1); sx.rotate(-1.0); sx.fillRect(-0.8, -6.5, 1.6, 6.5); sx.restore();
    sx.restore();
  };
  const greyVignette = ctx => { const g = ctx.createRadialGradient(W / 2, H / 2, 50, W / 2, H / 2, 1000); g.addColorStop(0, '#5a5a5a'); g.addColorStop(1, '#0a0a0a'); ctx.fillStyle = g; ctx.fillRect(0, 0, W, H); };

  const SCENES = [
    { a: 17.17, b: 17.63, draw(ctx, t, frame) { greyVignette(ctx); term(ctx, typed('убито: 0', t, 17.2, 30) + cursor(t), W / 2 - 150, H / 2 + 14, { size: 54, color: '#f2f2f2' }); } },
    { a: 17.63, b: 18.63, draw(ctx, t, frame) { // a grid of sights: everyone has a target
      begin();
      for (let gy = 0; gy < 5; gy++) for (let gx = 0; gx < 3; gx++) { const x = 13 + gx * 23, y = 14 + gy * 25; if (gy === 2 && gx === 1) continue; CROSSH(x, y, 7, '#ddd'); sx.fillStyle = '#ff3344'; sx.fillRect(x - 1, y - 1, 2, 2); }
      const q = K.ease.outBack(K.clamp((t - 17.63) / 0.4)), cx = COLS / 2, cy = 64;
      sx.strokeStyle = '#ff3344'; sx.lineWidth = 3; sx.beginPath(); sx.arc(cx - 8 + (1 - q) * -20, cy, 14, 1.9, 4.4); sx.stroke(); sx.beginPath(); sx.arc(cx + 8 + (1 - q) * 20, cy, 14, -1.25, 1.25); sx.stroke();
      CROSSH(cx, cy, 6, '#fff');
      ascii(ctx, frame);
      if (t > 18.0) term(ctx, 'у каждого ( своя цель )', W / 2, H - 140, { size: 32, color: GREY, align: 'center' });
    } },
    { a: 18.63, b: 19.27, draw(ctx, t, frame) { // two soldiers aim at each other
      begin();
      soldier(17, 74, 2.4, '#cfcfcf', 1); soldier(55, 74, 2.4, '#cfcfcf', -1);
      const f = since([18.82, 19.05], t) < 0.12;
      if (f) { sx.fillStyle = '#ffdd88'; sx.beginPath(); sx.arc(42, 63, 4, 0, 7); sx.arc(30, 63, 4, 0, 7); sx.fill(); }
      for (let k = 0; k < 3; k++) { const q = ((t - 18.63) * 2.4 + k / 3) % 1; sx.fillStyle = '#fff'; sx.fillRect(27 + q * 18, 63, 2, 0.8); }
      ascii(ctx, frame);
    } },
    { a: 19.27, b: 20.13, draw(ctx, t, frame) { // a big cross made of characters, beating
      begin();
      const s = 1 + 0.12 * Math.exp(-since([19.27, 19.74, 19.99], t) * 7), cx = COLS / 2, cy = 62;
      sx.fillStyle = '#ff3344'; sx.fillRect(cx - 7 * s, cy - 24 * s, 14 * s, 48 * s); sx.fillRect(cx - 24 * s, cy - 7 * s, 48 * s, 14 * s);
      ascii(ctx, frame, { chars: '+' });
      term(ctx, 'медик', W / 2, H - 260, { size: 32, color: GREY, align: 'center' });
    } },
    { a: 20.13, b: 20.67, draw(ctx, t, frame) { // the whole screen: shots
      ctx.save(); ctx.font = `bold 30px ${MONO}`;
      for (let y = 0; y < 34; y++) for (let x = 0; x < 9; x++) { const on = K.hash(x * 7 + y, Math.floor(t * 20)) > 0.5; ctx.fillStyle = on ? RED : '#5a5a5a'; ctx.fillText('бах', 14 + x * 118, 48 + y * 56); }
      ctx.restore();
    } },
    { a: 20.67, b: 21.5, draw(ctx, t, frame) { begin(); srcText('+1', COLS / 2, 64, 34, '#ff3344'); ascii(ctx, frame, { chars: '+1' }); } },
    { a: 21.5, b: 22.63, draw(ctx, t, frame) { // heal --all
      term(ctx, '> ' + typed('лечить --всех', t, 21.55, 30) + (t < 22.0 ? cursor(t) : ''), 70, 860, { size: 40 });
      const p = K.clamp(0.2 + count(HATS, t, 21.6, 22.6) * 0.17 + count(KICKS, t, 21.6, 22.6) * 0.1, 0, 0.7);
      const n = Math.round(p * 14);
      if (t > 21.9) term(ctx, '[' + '#'.repeat(n) + '.'.repeat(14 - n) + ']  ' + Math.round(p * 100) + '%', 70, 930, { size: 40, color: GREY });
    } },
    { a: 22.63, b: 24.43, draw(ctx, t, frame) { // a sniper's eye; rows of soldiers; one of them carries a cross
      begin();
      const cx = COLS / 2, cy = 40, bl = Math.abs(Math.sin((t - 22.63) * 3)) < 0.12 ? 0.2 : 1, look = Math.sin(t * 1.6) * 5;
      sx.strokeStyle = '#ddd'; sx.lineWidth = 1.6; sx.beginPath(); sx.ellipse(cx, cy, 26, 12 * bl, 0, 0, 7); sx.stroke();
      if (bl > 0.5) { sx.fillStyle = '#ccc'; sx.beginPath(); sx.arc(cx + look, cy, 8, 0, 7); sx.fill(); sx.fillStyle = '#000'; sx.beginPath(); sx.arc(cx + look, cy, 3.5, 0, 7); sx.fill(); }
      for (let i = 0; i < 6; i++) soldier(8 + i * 11.5, 104, 1.2, '#9a9a9a', i < 3 ? 1 : -1);
      // Kostya among them: no rifle, a red cross
      const kx = COLS / 2, glow = 0.6 + 0.4 * kick(t);
      sx.fillStyle = `rgba(255,255,255,${glow})`; sx.beginPath(); sx.arc(kx, 93, 2.6, 0, 7); sx.fill(); sx.fillRect(kx - 2.6, 97, 5.2, 9); sx.fillStyle = '#ff3344'; sx.fillRect(kx - 0.7, 98.5, 1.4, 4.4); sx.fillRect(kx - 2.2, 100, 4.4, 1.4);
      ascii(ctx, frame);
      term(ctx, 'снайпер видит всех', W / 2, 1360, { size: 32, color: GREY, align: 'center', alpha: K.prog(t, 22.8, 23.1) });
    } },
    { a: 24.43, b: 25.13, draw(ctx, t, frame) { // ammo = NULL
      greyVignette(ctx); ctx.fillStyle = 'rgba(0,0,0,0.6)'; ctx.fillRect(0, 0, W, H);
      const j = Math.floor(t * 30) % 4 === 0 ? 8 : 0;
      ctx.save(); ctx.translate(W / 2, H / 2); ctx.rotate(-0.08); term(ctx, 'NULL', j, 40, { size: 150, bold: true, color: '#b8b8b8', align: 'center' }); ctx.restore();
      term(ctx, 'патроны =', W / 2, H / 2 - 110, { size: 34, color: GREY, align: 'center' });
    } },
    { a: 25.13, b: 26.9, draw(ctx, t, frame) { // rain of bullets; he carries someone through it
      begin();
      const walk = e(t, 25.13, 26.9) * 30;
      const kx = 20 + walk, ky = 100, bob = Math.abs(Math.sin(t * 9)) * 1.2;
      sx.fillStyle = '#eee'; sx.beginPath(); sx.arc(kx, ky - 22 - bob, 3, 0, 7); sx.fill(); sx.fillRect(kx - 3.4, ky - 18 - bob, 6.8, 13);
      sx.fillRect(kx - 3, ky - 5, 2.2, 10); sx.fillRect(kx + 0.8, ky - 5, 2.2, 10);
      sx.fillStyle = '#999'; sx.fillRect(kx - 10, ky - 22 - bob, 20, 4); sx.beginPath(); sx.arc(kx + 11, ky - 21 - bob, 2.4, 0, 7); sx.fill();
      sx.fillStyle = '#ff3344'; sx.fillRect(kx - 0.8, ky - 15 - bob, 1.6, 5); sx.fillRect(kx - 2.4, ky - 13.4 - bob, 4.8, 1.6);
      ascii(ctx, frame);
      // falling characters (bullets)
      ctx.save(); ctx.font = `bold 26px ${MONO}`;
      for (let i = 0; i < 46; i++) { const r = K.rng(i), x = r() * W, sp = 500 + r() * 900, y = ((t * sp + r() * H) % (H + 200)) - 100; ctx.fillStyle = r() > 0.85 ? RED : '#6a6a6a'; ctx.fillText(r() > 0.5 ? '|' : "'", x, y); ctx.fillText('|', x, y - 30); }
      ctx.restore();
    } },
    { a: 26.9, b: 29.0, draw(ctx, t, frame) { // a hand reaches down, a hand reaches up — they grip
      begin();
      const q = K.ease.inOutCubic(K.clamp(count(HATS, t, 27.1, 28.3) / 5 + K.prog(t, 26.9, 28.2) * 0.2));
      const gap = K.lerp(26, 0.5, q), cx = COLS / 2, cy = 64;
      hand(cx + gap * 0.5, cy - gap - 8, 2.2, Math.PI + 0.35, '#f0f0f0');
      hand(cx - gap * 0.5, cy + gap + 8, 2.2, 0.35, '#a8a8a8');
      if (q > 0.97) { const f = 1 - K.clamp((t - 28.25) / 0.7); sx.strokeStyle = '#ff3344'; sx.lineWidth = 2.2; sx.beginPath(); sx.arc(cx, cy, 8 + 26 * (1 - f), 0, 7); sx.stroke(); sx.fillStyle = '#ff3344'; sx.fillRect(cx - 1.2, cy - 4, 2.4, 8); sx.fillRect(cx - 4, cy - 1.2, 8, 2.4); }
      ascii(ctx, frame, { flicker: 0.06 });
    } },
    { a: 29.0, b: 29.8, draw(ctx, t, frame) { // searching for the wounded
      term(ctx, 'поиск раненых...', 70, 880, { size: 38 });
      const p = Math.min(0.99, 0.55 + K.prog(t, 29.0, 29.5) * 0.44), n = Math.round(p * 16);
      term(ctx, '[' + '#'.repeat(n) + '.'.repeat(16 - n) + ']  ' + Math.round(p * 100) + '%', 70, 940, { size: 38, color: GREY });
      if (t > 29.27) term(ctx, 'НАЙДЕН: 1', 70, 1020, { size: 40, color: RED, bold: true });
    } },
    { a: 29.8, b: 30.13, draw(ctx, t, frame) { begin(); sx.fillStyle = '#ff3344'; sx.fillRect(COLS / 2 - 2, 60, 4, 12); sx.fillRect(COLS / 2 - 6, 64, 12, 4); ascii(ctx, frame, { flicker: 0.3, ox: (K.hash(frame, 1) - 0.5) * 30 }); } },
    { a: 30.13, b: 31.5, draw(ctx, t, frame) { // ping: nobody is lost
      term(ctx, '> ' + typed('ping санитар', t, 30.15, 30), 70, 860, { size: 40 });
      [30.38, 30.82, 31.32].forEach((h, i) => { if (t > h) term(ctx, `ответ: ${[12, 9, 7][i]} мс`, 70, 930 + i * 54, { size: 36, color: GREY }); });
      if (t > 31.32) { const s = 1 + 0.1 * kick(t); ctx.save(); ctx.translate(70, 1220); ctx.scale(s, s); term(ctx, '0% потерь.', 0, 0, { size: 96, color: RED, bold: true }); ctx.restore(); }
    } },
    { a: 31.5, b: 32.63, draw(ctx, t, frame) { // a cross made of the word "жив" assembles
      begin();
      const q = K.ease.outCubic(K.prog(t, 31.5, 32.3)), cx = COLS / 2, cy = 62, r = K.rng(4);
      sx.fillStyle = '#ff3344';
      for (let i = 0; i < 420; i++) {
        const onV = r() < 0.5, tx = onV ? cx + (r() - 0.5) * 14 : cx + (r() - 0.5) * 48, ty = onV ? cy + (r() - 0.5) * 48 : cy + (r() - 0.5) * 14;
        const fx = r() * COLS, fy = r() * ROWS;
        sx.fillRect(K.lerp(fx, tx, q), K.lerp(fy, ty, q), 1, 1);
      }
      ascii(ctx, frame, { chars: 'жив' });
    } },
    { a: 32.63, b: 34.1, draw(ctx, t, frame) { // the things a medic says, around him
      begin(); srcImage(I('portrait'), COLS / 2, ROWS - 30, 62, 62, { fy: 0.42, contrast: 1.9, bright: 1.25 }); ascii(ctx, frame);
      const W2 = [['дыши.', 120, 300, 32.7], ['я рядом', 640, 420, 33.0], ['держись', 200, 620, 33.19], ['живой?', 700, 760, 33.38], ['смотри на меня', 260, 940, 33.61], ['всё будет хорошо', 560, 1080, 33.82]];
      W2.forEach(([s, x, y, t0]) => { if (t > t0) term(ctx, s, x, y, { size: 34, color: '#bdbdbd', alpha: K.prog(t, t0, t0 + 0.2) }); });
    } },
    { a: 34.1, b: 36.29, draw(ctx, t, frame) { // orders all around — "все"
      begin(); srcImage(I('portrait'), COLS / 2, ROWS - 30, 62, 62, { fy: 0.42, contrast: 1.9, bright: 1.25 }); ascii(ctx, frame);
      const O = [['огонь!', 90, 280, 34.42], ['пли!', 720, 360, 34.67], ['цель вижу', 140, 520, 34.92], ['стреляй!', 660, 640, 35.05], ['заряжай', 230, 760, 35.26], ['огонь!', 700, 880, 35.38]];
      O.forEach(([s, x, y, t0]) => { if (t > t0) term(ctx, s, x, y, { size: 32, color: '#7a2a31', alpha: 0.9 }); });
      if (t > 35.2) term(ctx, typed('все', t, 35.2, 8), W / 2, 1180, { size: 54, color: WHITE, align: 'center' });
    } },
    { a: 36.29, b: 37.85, draw(ctx, t, frame) { // the punchline in big ASCII letters
      begin();
      const g = Math.floor(t * 18) % 5 === 0 ? 2 : 0, fade = 1 - K.prog(t, 37.3, 37.7);
      sx.globalAlpha = fade;
      srcText('(КРОМЕ', COLS / 2 + g, 52, 15, '#e8e8e8');
      srcText('НЕГО)', COLS / 2 - g, 72, 18, '#ff3344');
      ascii(ctx, frame, { flicker: 0.08 });
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
        const z = 1 + 0.03 * kick(t), sh = 8 * kick(t);
        ctx.save(); ctx.translate(W / 2 + (K.hash(frame, 1) - 0.5) * sh, H / 2 + (K.hash(frame, 2) - 0.5) * sh); ctx.scale(z, z); ctx.translate(-W / 2, -H / 2);
        if (s) s.draw(ctx, t, frame);
        ctx.restore();
        if (s && t - s.a < 0.05) { ctx.fillStyle = 'rgba(255,255,255,0.25)'; ctx.fillRect(0, 0, W, H); }
      }
      K.grain(ctx, frame, 0.05);
      K.vignette(ctx, 0.45);
    },
  };
})();
