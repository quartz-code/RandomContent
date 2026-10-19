// Version J — «Подождать». The chat.log template (same 37.8 s track) with a new story: Lilith
// and Amelia wait for a paladin who has not come back. His portrait sits below in characters;
// the drop is about paladinhood — oaths, shields, a sword like a cross, his bear, holding the
// line — and about waiting. Built on version I's ASCII machinery.
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
  const portrait = (t, o = {}) => srcImage(I('portrait'), COLS / 2, ROWS - 28, 64, 64, { fy: 0.3, contrast: 1.9, bright: 1.2 + 0.06 * Math.sin(t * 1.3) + (o.flash || 0), alpha: o.alpha ?? 1, dx: o.dx || 0 });

  // ================= chat.log (0..17.17) =================
  const LOG = [
    { who: 'лилит', s: 'Как ты думаешь, он жив?', t: 0.4 },
    { who: 'амелия', s: 'Я надеюсь...', t: 3.0, wait: 2.0 },
    { who: 'лилит', s: 'Он точно жив, он всегда цеплялся за неё.', t: 5.2 },
    { who: 'амелия', s: 'Да. Спасибо...', t: 8.6, wait: 7.6 },
    { who: 'лилит', s: 'Не грусти... Он выберется, нужно только подождать немного...', t: 10.4 },
    { who: 'амелия', s: 'Да... Подождать...', t: 15.4, wait: 14.2, red: true },
  ];
  const LINE_TIMES = LOG.map(m => m.t);
  const chatLog = (ctx, t, frame) => {
    // Kostya in characters below; the glitch on the red line shakes him
    const red = t > 15.4, gl = red ? Math.exp(-(t - 15.4) * 4) : 0;
    begin(); portrait(t, { flash: 0.25 * Math.exp(-since(LINE_TIMES, t) * 5), dx: gl * (K.hash(frame, 3) - 0.5) * 8 });
    ascii(ctx, frame, { flicker: 0.04, scramble: gl * 0.35 + (t < 0.6 ? (0.6 - t) : 0) });
    // the log nudges forward on each new line, tilts away when the red line lands
    const nudge = Math.exp(-since(LINE_TIMES, t) * 6), tilt = e(t, 16.5, 17.1);
    ctx.save(); ctx.translate(W / 2, 520); ctx.scale(1 + 0.012 * nudge, 1 + 0.012 * nudge);
    ctx.transform(1, 0.05 * tilt, -0.18 * tilt, 1 - 0.12 * tilt, 0, 0); ctx.translate(-W / 2, -520);
    ctx.globalAlpha = 1 - 0.5 * tilt;
    term(ctx, '— chat.log', 44, 96, { size: 26, color: GREY }); term(ctx, '03:12', W - 44, 96, { size: 26, color: GREY, align: 'right' });
    ctx.fillStyle = 'rgba(255,255,255,0.16)'; ctx.fillRect(44, 114, W - 88, 1);
    let y = 200;
    for (const m of LOG) {
      const t0 = m.wait ?? m.t;
      if (t < t0) break;
      const lab = m.who + '>', col = m.who === 'амелия' ? RED : WHITE, lw = 44 + (m.who.length + 1) * 26.4 + 18;
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
    if (red && t - 15.4 < 0.12) { ctx.fillStyle = 'rgba(255,40,60,0.18)'; ctx.fillRect(0, 0, W, H); }
  };

  // ================= drop scenes =================
  const CROSSH = (x, y, r, col, rot = 0) => {
    sx.save(); sx.translate(x, y); sx.rotate(rot); sx.strokeStyle = col; sx.lineWidth = 1.3; sx.beginPath(); sx.arc(0, 0, r, 0, 7);
    sx.moveTo(-r - 3, 0); sx.lineTo(-r + 2, 0); sx.moveTo(r - 2, 0); sx.lineTo(r + 3, 0); sx.moveTo(0, -r - 3); sx.lineTo(0, -r + 2); sx.moveTo(0, r - 2); sx.lineTo(0, r + 3); sx.stroke(); sx.restore();
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

  // heraldic shield (flat top, pointed bottom), centred at x,y, half-width w
  const shieldPath = (x, y, w, h) => { sx.beginPath(); sx.moveTo(x - w, y - h * 0.5); sx.lineTo(x + w, y - h * 0.5); sx.lineTo(x + w, y); sx.quadraticCurveTo(x + w, y + h * 0.35, x, y + h * 0.5); sx.quadraticCurveTo(x - w, y + h * 0.35, x - w, y); sx.closePath(); };
  // a knight in profile: helm, cape, kite shield on the arm, sword; feet at (x,y), ~30 units tall
  const knight = (x, y, s, col = '#d8d8d8', dir = 1, swing = 0, o = {}) => {
    sx.save(); sx.translate(x, y); sx.scale(dir * s, s); sx.lineCap = 'round'; sx.lineJoin = 'round';
    sx.fillStyle = '#6a6a6a'; sx.beginPath(); sx.moveTo(-1, -24); sx.quadraticCurveTo(-8, -12, -7, 0); sx.lineTo(-2, 0); sx.closePath(); sx.fill(); // cape
    sx.strokeStyle = col; sx.fillStyle = col; sx.lineWidth = 3;
    sx.beginPath(); sx.moveTo(-1, -13); sx.lineTo(-4, 0); sx.moveTo(1.5, -13); sx.lineTo(4, -6.5); sx.lineTo(4.5, 0); sx.stroke();     // legs
    sx.beginPath(); sx.moveTo(-3.4, -12.5); sx.lineTo(3.4, -12.5); sx.lineTo(3.8, -24); sx.lineTo(-2.6, -24.5); sx.closePath(); sx.fill(); // torso
    sx.beginPath(); sx.roundRect(-2.2, -31.5, 6.2, 7.4, 1.6); sx.fill();                                                                   // great helm
    sx.fillStyle = '#000'; sx.fillRect(1.6, -29, 2.6, 0.9); sx.fillStyle = col;                                                           // visor slit
    if (!o.noSword) { sx.save(); sx.translate(3, -21); sx.rotate(-1.2 + swing); sx.lineWidth = 1.5; sx.beginPath(); sx.moveTo(0, 0); sx.lineTo(16, 0); sx.stroke(); sx.lineWidth = 2.2; sx.beginPath(); sx.moveTo(1.5, -2.4); sx.lineTo(1.5, 2.4); sx.stroke(); sx.restore(); } // sword
    if (!o.noShield) { sx.save(); sx.translate(4.2, -16); const sw = 3.4, sh = 9; sx.beginPath(); sx.moveTo(-sw, -sh * 0.5); sx.lineTo(sw, -sh * 0.5); sx.lineTo(sw, 0); sx.quadraticCurveTo(sw, sh * 0.35, 0, sh * 0.5); sx.quadraticCurveTo(-sw, sh * 0.35, -sw, 0); sx.closePath(); sx.fill(); sx.fillStyle = '#ff3344'; sx.fillRect(-0.6, -3.6, 1.2, 6.5); sx.fillRect(-2.4, -1.6, 4.8, 1.2); sx.restore(); } // shield + cross
    sx.restore();
  };
  // a bear in profile, walking: low heavy body, shoulder hump, head carried low, round ear, snout; feet at (x,y)
  const bear = (x, y, s, col = '#bdbdbd', dir = 1, t = 0, roar = 0) => {
    sx.save(); sx.translate(x, y); sx.scale(dir * s, s); sx.fillStyle = col; sx.strokeStyle = col; sx.lineCap = 'round';
    const st = Math.sin(t * 5) * 2, bob = Math.abs(Math.sin(t * 5)) * 0.6;
    sx.lineWidth = 6; sx.beginPath(); sx.moveTo(-13, -10 - bob); sx.lineTo(-13 + st, -1); sx.moveTo(-6, -10 - bob); sx.lineTo(-6 - st, -1); sx.moveTo(8, -10 - bob); sx.lineTo(8 - st, -1); sx.moveTo(14, -10 - bob); sx.lineTo(14 + st, -1); sx.stroke(); // thick legs
    sx.beginPath(); sx.ellipse(0, -16 - bob, 18, 9, 0, 0, 7); sx.fill();                        // body
    sx.beginPath(); sx.ellipse(-12, -17 - bob, 8.5, 8.5, 0, 0, 7); sx.fill();                   // rump
    sx.beginPath(); sx.ellipse(9, -21 - bob, 9, 7, -0.2, 0, 7); sx.fill();                      // shoulder hump
    sx.save(); sx.translate(22, -17 - bob); sx.rotate(0.12 - roar * 0.45);
    sx.beginPath(); sx.ellipse(0, 0, 6.5, 5.6, 0, 0, 7); sx.fill();                             // head
    sx.beginPath(); sx.ellipse(6.5, 1.4, 4.6, 2.9, 0.1, 0, 7); sx.fill();                       // snout
    sx.beginPath(); sx.arc(-2.5, -5.2, 2.6, 0, 7); sx.fill();                                   // ear
    sx.fillStyle = '#000';
    sx.beginPath(); sx.arc(-2.5, -5.2, 1, 0, 7); sx.fill();                                     // ear hollow
    sx.beginPath(); sx.arc(2.4, -1.4, 1.1, 0, 7); sx.fill();                                    // eye
    if (roar > 0.1) { sx.beginPath(); sx.moveTo(3, 2.5); sx.lineTo(11.5, 2.6 + 4.5 * roar); sx.lineTo(11.5, 3.4); sx.closePath(); sx.fill(); }
    sx.fillStyle = '#ff3344'; sx.fillRect(10.4, 0, 1.6, 1.4);                                   // nose
    sx.restore();
    sx.restore();
  };
  // a bear paw from above: furry forearm with a ragged edge, four toe bumps and bright claws
  const paw = (x, y, s, rot, col) => {
    sx.save(); sx.translate(x, y); sx.rotate(rot); sx.scale(s, s); sx.fillStyle = col;
    sx.beginPath(); sx.moveTo(-7.5, 0);
    for (let k = 0; k <= 10; k++) sx.lineTo(-7.5 - (k % 2) * 0.9, k * 4);
    for (let k = 10; k >= 0; k--) sx.lineTo(7.5 + (k % 2) * 0.9, k * 4);
    sx.closePath(); sx.fill();
    sx.beginPath(); sx.ellipse(0, -1, 8.5, 6.5, 0, 0, 7); sx.fill();
    const toes = [[-6, -6.5], [-2.1, -8.6], [2.1, -8.6], [6, -6.5]];
    sx.strokeStyle = '#000'; sx.lineWidth = 0.9;
    toes.forEach(([tx, ty]) => { sx.beginPath(); sx.arc(tx, ty, 2.6, 0, 7); sx.fill(); sx.stroke(); });
    sx.fillStyle = '#ffffff';
    toes.forEach(([tx, ty]) => { sx.beginPath(); sx.moveTo(tx - 0.9, ty - 2); sx.lineTo(tx + tx * 0.1, ty - 7); sx.lineTo(tx + 0.9, ty - 2); sx.closePath(); sx.fill(); });
    sx.restore();
  };
  // an armoured gauntlet: like the hand, with plates across the back
  const gauntlet = (x, y, s, rot, col, rim) => {
    hand(x, y, s, rot, col, rim, 0.9);
    sx.save(); sx.translate(x, y); sx.rotate(rot); sx.scale(s, s); sx.fillStyle = '#000';
    for (let k = 0; k < 4; k++) sx.fillRect(-5, 4 + k * 5, 10, 0.8);
    sx.restore();
  };

  const SCENES = [
    { a: 17.17, b: 17.63, draw(ctx, t, frame) { // "жив?" hesitates on the screen
      greyVignette(ctx);
      term(ctx, 'жив' + (t > 17.4 ? '?' : '') + cursor(t), W / 2 - 90, H / 2 + 18, { size: 70, color: '#f4f4f4' });
    } },
    { a: 17.63, b: 18.63, draw(ctx, t, frame) { // a wall of shields; the one in the middle bears the cross
      begin();
      let k = 0;
      for (let gy = 0; gy < 5; gy++) for (let gx = 0; gx < 3; gx++) {
        const x = 13 + gx * 23, y = 14 + gy * 25; if (gy === 2 && gx === 1) continue;
        const pop = 1 + 0.12 * (Math.floor((t - 17.63) * 8) % 14 === k ? 1 : 0);
        shieldPath(x, y, 7 * pop, 17 * pop); sx.strokeStyle = '#d0d0d0'; sx.lineWidth = 1.3; sx.stroke(); k++;
      }
      const q = K.ease.outBack(K.clamp((t - 17.7) / 0.35)), cx = COLS / 2, cy = 64;
      shieldPath(cx, cy, 11 * q, 26 * q); sx.fillStyle = '#e8e8e8'; sx.fill();
      sx.fillStyle = '#ff3344'; sx.fillRect(cx - 1.6 * q, cy - 9 * q, 3.2 * q, 17 * q); sx.fillRect(cx - 7 * q, cy - 3 * q, 14 * q, 3.2 * q);
      ascii(ctx, frame, { scramble: Math.max(0, 0.6 - (t - 17.63) * 4) });
      decode(ctx, 'у каждого ( свой обет )', W / 2, H - 150, t, 18.0, { size: 40, color: SOFT, align: 'center' });
    } },
    { a: 18.63, b: 19.27, draw(ctx, t, frame) { // two knights clash; sparks where the blades meet
      begin();
      const hits = [18.82, 18.98, 19.12], f = since(hits, t), swing = Math.exp(-f * 14) * 0.5, gy = 92, s = 1.25;
      sx.fillStyle = '#3a3a3a'; sx.fillRect(0, gy + 0.5, COLS, 1);
      knight(16, gy, s, '#dcdcdc', 1, 0.55 - swing); knight(COLS - 16, gy, s, '#dcdcdc', -1, 0.55 - swing);
      if (f < 0.1) { const r = K.rng(Math.round(t * 30)); sx.fillStyle = '#ffe2a0'; for (let i = 0; i < 14; i++) sx.fillRect(COLS / 2 + (r() - 0.5) * 10, gy - 42 + (r() - 0.5) * 8, 1, 1); }
      ascii(ctx, frame, { scramble: f < 0.06 ? 0.2 : 0 });
      decode(ctx, 'сталь против стали', W / 2, 1640, t, 18.7, { size: 40, color: SOFT, align: 'center' });
    } },
    { a: 19.27, b: 20.13, draw(ctx, t, frame) { // a sword point-down: a cross that beats
      begin();
      const q = K.ease.outCubic(K.prog(t, 19.27, 19.55)), s = 1 + 0.08 * Math.exp(-since([19.74, 19.99], t) * 7), cx = COLS / 2, cy = 58;
      sx.save(); sx.translate(cx, cy + (1 - q) * -60); sx.scale(s, s);
      sx.fillStyle = '#f0f0f0'; sx.beginPath(); sx.moveTo(-3, -10); sx.lineTo(3, -10); sx.lineTo(3, 38); sx.lineTo(0, 46); sx.lineTo(-3, 38); sx.closePath(); sx.fill(); // blade
      sx.fillStyle = '#ff3344'; sx.fillRect(-15, -14, 30, 4.5);                                  // crossguard
      sx.fillStyle = '#bdbdbd'; sx.fillRect(-2, -28, 4, 14); sx.beginPath(); sx.arc(0, -30, 3.5, 0, 7); sx.fill(); // grip, pommel
      sx.restore();
      ascii(ctx, frame, { flicker: 0.02 });
      decode(ctx, 'паладин', W / 2, H - 220, t, 19.75, { size: 50, color: SOFT, align: 'center', bold: true });
    } },
    { a: 20.13, b: 20.67, draw(ctx, t, frame) { // the oath fills the screen, a red wave through it
      ctx.save(); ctx.font = `bold 30px ${MONO}`;
      const wave = (t - 20.13) * 60;
      for (let y = 0; y < 33; y++) for (let x = 0; x < 6; x++) {
        const on = Math.abs((x * 3 + y) - wave) < 4 || K.hash(x * 7 + y, Math.floor(t * 24)) > 0.85;
        ctx.fillStyle = on ? RED : '#4a4a4a'; ctx.fillText('клянусь', 20 + x * 176 + (on ? (K.hash(x, y + Math.floor(t * 30)) - 0.5) * 8 : 0), 54 + y * 57);
      }
      ctx.restore();
    } },
    { a: 20.67, b: 21.5, draw(ctx, t, frame) { // the shield takes the blow — and cracks
      begin();
      const cx = COLS / 2, cy = 62, cr = count(KICKS, t, 20.8, 21.5), sh = kick(t) * 1.5;
      shieldPath(cx + (K.hash(frame, 4) - 0.5) * sh, cy, 22, 56); sx.fillStyle = '#dedede'; sx.fill();
      sx.fillStyle = '#ff3344'; sx.fillRect(cx - 2.5, cy - 20, 5, 36); sx.fillRect(cx - 12, cy - 7, 24, 5);
      sx.strokeStyle = '#000'; sx.lineWidth = 1.6;
      if (cr >= 1) { sx.beginPath(); sx.moveTo(cx + 6, cy - 28); sx.lineTo(cx + 2, cy - 16); sx.lineTo(cx + 8, cy - 6); sx.lineTo(cx + 3, cy + 6); sx.stroke(); }
      if (cr >= 2) { sx.beginPath(); sx.moveTo(cx - 18, cy - 10); sx.lineTo(cx - 9, cy - 4); sx.lineTo(cx - 12, cy + 8); sx.lineTo(cx - 4, cy + 16); sx.stroke(); }
      ascii(ctx, frame, { scramble: kick(t) * 0.3 });
      decode(ctx, 'щит принимает удар', W / 2, H - 380, t, 20.75, { size: 42, color: SOFT, align: 'center' });
    } },
    { a: 21.5, b: 22.63, draw(ctx, t, frame) { // protect --all: what he held
      term(ctx, '> ' + typed('защитить --всех', t, 21.55, 34) + (t < 21.95 ? cursor(t) : ''), 70, 640, { size: 46 });
      const log = ['деревня ........... цела', 'мост .............. удержан', 'раненые ........... вынесены', 'дети .............. в укрытии', 'ворота ............ закрыты', 'сам ............... не вернулся'];
      const n = [21.62, 21.79, 21.98, 22.12, 22.26, 22.42].filter(b2 => t >= b2).length;
      log.slice(0, Math.min(n, log.length)).forEach((l, i) => term(ctx, l, 70, 720 + i * 54, { size: 36, color: i === 5 ? RED : i === n - 1 ? WHITE : GREY }));
    } },
    { a: 22.63, b: 24.43, draw(ctx, t, frame) { // he had a bear
      begin();
      const walk = e(t, 22.63, 24.43) * 6, gy = 96, roar = Math.exp(-since([23.37, 23.82], t) * 5);
      sx.fillStyle = '#3a3a3a'; sx.fillRect(0, gy + 0.5, COLS, 1);
      bear(10 + walk, gy, 1.0, '#cfcfcf', 1, t, roar);
      knight(50 + walk, gy, 0.95, '#f0f0f0', 1, 0.3, {});
      ascii(ctx, frame, { scramble: Math.max(0, 0.45 - (t - 22.63) * 3) });
      decode(ctx, 'у него был медведь.', W / 2, 760, t, 22.85, { size: 50, color: WHITE, align: 'center', bold: true });
      if (t > 23.4) decode(ctx, 'шёл за ним везде.', W / 2, 840, t, 23.4, { size: 36, color: SOFT, align: 'center' });
    } },
    { a: 24.43, b: 25.13, draw(ctx, t, frame) { // fear = NULL
      greyVignette(ctx, 0.6); ctx.fillStyle = 'rgba(0,0,0,0.5)'; ctx.fillRect(0, 0, W, H);
      const j = Math.floor(t * 30) % 4 === 0 ? 10 : 0, s = 1 + 0.06 * Math.sin((t - 24.43) * 20) * Math.exp(-(t - 24.43) * 4);
      decode(ctx, 'страх =', W / 2, H / 2 - 120, t, 24.45, { size: 40, color: GREY, align: 'center' });
      ctx.save(); ctx.translate(W / 2, H / 2 + 40); ctx.rotate(-0.08); ctx.scale(s, s);
      for (const [dx, col, a] of [[-j, '#ff3344', 0.6], [j, '#33d6ff', 0.5], [0, '#d0d0d0', 1]]) term(ctx, 'NULL', dx, 0, { size: 170, bold: true, color: col, align: 'center', alpha: a });
      ctx.restore();
    } },
    { a: 25.13, b: 26.9, draw(ctx, t, frame) { // arrows rain; he holds the shield over the bear and the small ones
      begin();
      const gy = 104, cx = COLS / 2;
      bear(cx - 14, gy, 0.6, '#9a9a9a', 1, t * 0.2, 0);
      [cx + 12, cx + 17].forEach((x, i) => { sx.fillStyle = '#9a9a9a'; sx.beginPath(); sx.arc(x, gy - 9, 1.8, 0, 7); sx.fill(); sx.fillRect(x - 1.5, gy - 7, 3, 7); });
      knight(cx + 3, gy, 0.95, '#f2f2f2', 1, -0.9, { noShield: true });
      // the raised shield above them all
      const hit = Math.exp(-since([25.39, 26.16, 26.31, 26.61], t) * 10);
      sx.save(); sx.translate(cx, gy - 38 + hit * 1.5); sx.fillStyle = '#e8e8e8'; sx.beginPath(); sx.moveTo(-26, 3); sx.quadraticCurveTo(0, -11, 26, 3); sx.lineTo(24, 6.5); sx.quadraticCurveTo(0, -5, -24, 6.5); sx.closePath(); sx.fill(); sx.fillStyle = '#ff3344'; sx.fillRect(-1.5, -5, 3, 4); sx.restore();
      for (const h of [25.39, 26.16, 26.31, 26.61]) { const q = K.clamp((t - h) / 0.3); if (q > 0 && q < 1) { const hx = cx + ((h * 41) % 30) - 15; sx.fillStyle = '#ffd38a'; for (let k = 0; k < 6; k++) sx.fillRect(hx + (k - 3) * 2.2 * q, gy - 42 - q * 6 * Math.abs(k - 3) / 3, 1, 1); } }
      ascii(ctx, frame);
      ctx.save(); ctx.font = `bold 30px ${MONO}`;
      for (let i = 0; i < 46; i++) { const r = K.rng(i), sp = 700 + r() * 900, y = ((t * sp + r() * H) % (H + 200)) - 100, x = (r() * W + y * 0.25) % W; if (y > 1000 && Math.abs(x - W / 2) < 400) continue; ctx.fillStyle = r() > 0.85 ? RED : '#6a6a6a'; ctx.fillText('/', x, y); }
      ctx.restore();
      decode(ctx, 'стрелы — в щит. не в них.', W / 2, 520, t, 25.3, { size: 40, color: SOFT, align: 'center' });
    } },
    { a: 26.9, b: 29.0, draw(ctx, t, frame) { // a gauntlet and a paw reach for each other — and meet
      begin();
      const cx = COLS / 2, cy = 62;
      const reach = K.ease.inOutCubic(K.clamp(count(HATS, t, 27.1, 28.1) / 4 + K.prog(t, 26.9, 28.05) * 0.25)), meet = 28.05;
      const gap = t < meet ? K.lerp(32, 15, reach) : 13.5;
      paw(cx + gap * 0.45, cy + gap + 12, 1.8, 0.35, '#8c8c8c');
      gauntlet(cx - gap * 0.45, cy - gap - 10, 2.0, Math.PI + 0.35, '#f4f4f4', true);
      if (t > meet) { const f = K.clamp((t - meet) / 0.6); sx.strokeStyle = `rgba(255,51,68,${1 - f})`; sx.lineWidth = 1.6; sx.beginPath(); sx.arc(cx, cy + 2, 6 + 30 * f, 0, 7); sx.stroke(); }
      ascii(ctx, frame, { flicker: 0.05, scramble: since([meet], t) < 0.1 ? 0.3 : 0 });
      if (t > meet) decode(ctx, 'вместе.', W / 2, H - 260, t, meet + 0.1, { size: 56, color: WHITE, align: 'center', bold: true });
    } },
    { a: 29.0, b: 29.8, draw(ctx, t, frame) { // a pulse, faint
      decode(ctx, 'поиск сигнала жизни...', 70, 840, t, 29.02, { size: 42 });
      const p = Math.min(0.99, 0.5 + K.prog(t, 29.0, 29.45) * 0.49), n = Math.round(p * 16);
      term(ctx, '[' + '#'.repeat(n) + '.'.repeat(16 - n) + ']  ' + Math.round(p * 100) + '%', 70, 910, { size: 42, color: SOFT });
      ctx.save(); ctx.strokeStyle = RED; ctx.lineWidth = 4; ctx.beginPath();
      for (let x = 70; x < W - 70; x += 6) { const ph = ((x - 70) / 300 + (t - 29) * 1.5) % 1, y = 1060 + (ph > 0.45 && ph < 0.5 ? -60 * Math.sin((ph - 0.45) / 0.05 * Math.PI) : 0); x === 70 ? ctx.moveTo(x, y) : ctx.lineTo(x, y); }
      ctx.stroke(); ctx.restore();
      if (t > 29.27) { const s = 1 + 0.2 * kick(t); ctx.save(); ctx.translate(70, 1250); ctx.scale(s, s); term(ctx, 'ПУЛЬС: ЕСТЬ', 0, 0, { size: 64, color: RED, bold: true }); ctx.restore(); }
    } },
    { a: 29.8, b: 30.13, draw(ctx, t, frame) { begin(); sx.fillStyle = '#ff3344'; sx.beginPath(); sx.arc(COLS / 2 - 4, 62, 5, 0, 7); sx.arc(COLS / 2 + 4, 62, 5, 0, 7); sx.fill(); sx.beginPath(); sx.moveTo(COLS / 2 - 9, 64); sx.lineTo(COLS / 2, 74); sx.lineTo(COLS / 2 + 9, 64); sx.fill(); ascii(ctx, frame, { scramble: 0.35, ox: (K.hash(frame, 1) - 0.5) * 40 }); } },
    { a: 30.13, b: 31.5, draw(ctx, t, frame) { // ping: no answer… no answer… an answer
      term(ctx, '> ' + typed('ping паладин', t, 30.15, 34), 70, 760, { size: 46 });
      [[30.38, 'нет ответа', '#666'], [30.82, 'нет ответа', '#666'], [31.32, 'ответ получен.', RED]].forEach(([h, sTxt, col], i) => { if (t > h) decode(ctx, sTxt, 70, 840 + i * 60, t, h, { size: i === 2 ? 52 : 38, color: col, bold: i === 2 }); });
    } },
    { a: 31.5, b: 32.63, draw(ctx, t, frame) { // a heart made of the word "жив" gathers and beats
      begin();
      const q = K.ease.outCubic(K.prog(t, 31.5, 32.1)), s = 1 + 0.1 * kick(t), cx = COLS / 2, cy = 60, r = K.rng(4);
      sx.fillStyle = '#ff3344';
      for (let i = 0; i < 700; i++) {
        const a2 = r() * 6.283, rr = Math.sqrt(r()), hx = 16 * Math.pow(Math.sin(a2), 3) * rr, hy = -(13 * Math.cos(a2) - 5 * Math.cos(2 * a2) - 2 * Math.cos(3 * a2) - Math.cos(4 * a2)) * rr;
        sx.fillRect(K.lerp(r() * COLS, cx + hx * 1.6 * s, q), K.lerp(r() * ROWS, cy + hy * 1.6 * s, q), 1, 1);
      }
      ascii(ctx, frame, { chars: 'жив', flicker: 0 });
    } },
    { a: 32.63, b: 34.1, draw(ctx, t, frame) { // what they would tell him, around his face
      begin(); portrait(t, { flash: 0.3 * kick(t) }); ascii(ctx, frame);
      const W2 = [['держись.', 90, 300, 32.65], ['мы ждём', 620, 420, 33.0], ['вернись', 150, 600, 33.19], ['ты обещал', 640, 740, 33.38], ['медведь тоже ждёт', 120, 900, 33.61], ['я надеюсь', 600, 1040, 33.82]];
      W2.forEach(([sTxt, x, y, t0]) => decode(ctx, sTxt, x, y, t, t0, { size: 40, color: SOFT }));
    } },
    { a: 34.1, b: 36.29, draw(ctx, t, frame) { // waiting: the sand runs, the days count
      begin();
      const cx = COLS / 2, top = 30, mid = 58, bot = 86, fill = K.prog(t, 34.1, 36.29);
      sx.strokeStyle = '#d8d8d8'; sx.lineWidth = 1.6; sx.beginPath(); sx.moveTo(cx - 16, top); sx.lineTo(cx + 16, top); sx.lineTo(cx + 2, mid); sx.lineTo(cx + 16, bot); sx.lineTo(cx - 16, bot); sx.lineTo(cx - 2, mid); sx.closePath(); sx.stroke();
      sx.fillStyle = '#cfcfcf'; const h1 = (1 - fill) * 22; sx.beginPath(); sx.moveTo(cx - 16 * h1 / 28, mid - 2 - h1 + 2); sx.lineTo(cx + 16 * h1 / 28, mid - 2 - h1 + 2); sx.lineTo(cx, mid - 1); sx.closePath(); sx.fill();
      const h2 = fill * 20; sx.beginPath(); sx.moveTo(cx - 16, bot - 0.5); sx.lineTo(cx + 16, bot - 0.5); sx.lineTo(cx + 16 - h2 * 0.6, bot - h2); sx.lineTo(cx - 16 + h2 * 0.6, bot - h2); sx.closePath(); sx.fill();
      for (let k = 0; k < 6; k++) { const yy = mid + ((t * 30 + k * 4) % (bot - mid - h2)); sx.fillRect(cx - 0.5, yy, 1, 1); }
      ascii(ctx, frame, { flicker: 0.03 });
      const days = [1, 3, 7, 12, 21, 30, 40, 52, 64], n = Math.min(days.length - 1, [34.42, 34.67, 34.92, 35.05, 35.26, 35.38, 35.7, 36.0].filter(b2 => t >= b2).length);
      const s = 1 + 0.15 * kick(t); ctx.save(); ctx.translate(W / 2, 1520); ctx.scale(s, s); term(ctx, 'день ' + days[n], 0, 0, { size: 60, color: WHITE, align: 'center', bold: true }); ctx.restore();
      decode(ctx, 'подождать...', W / 2, 340, t, 34.2, { size: 44, color: SOFT, align: 'center' });
    } },
    { a: 36.29, b: 37.85, draw(ctx, t, frame) { // he will come back — big ASCII letters
      begin();
      const g = Math.floor(t * 18) % 5 === 0 ? 2 : 0, fall = K.prog(t, 37.2, 37.75);
      const q1 = K.ease.outBack(K.clamp((t - 36.29) / 0.25)), q2 = K.ease.outBack(K.clamp((t - 36.45) / 0.25));
      sx.save(); sx.translate(COLS / 2, 50); sx.scale(q1, q1); srcText('(ОН', g, 0, 15, '#ececec'); sx.restore();
      sx.save(); sx.translate(COLS / 2, 72); sx.scale(q2, q2); srcText('ВЕРНЁТСЯ)', -g, 0, 11, '#ff3344'); sx.restore();
      if (fall > 0) { const img = sx.getImageData(0, 0, COLS, ROWS); begin(); sx.putImageData(img, 0, Math.round(fall * fall * 60)); }
      ascii(ctx, frame, { flicker: 0.08, scramble: since([36.29, 36.45, 36.93], t) < 0.08 ? 0.4 : fall * 0.6 });
    } },
  ];

  window.VERSION = {
    duration: 37.83,
    images: { portrait: '/assets/images/paladin.png' },
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
