// Version D — «Доставлено». An atmospheric piece: one night-long chat in which Kostya writes to
// his mother for years. Every message is delivered; none is ever read. The world behind the
// floating glass chat carries the story: snow and train lights, snow freezing into stars,
// ash and distant flashes of war, the glass cracking, white hospital silence, snow again.
// Music: Кино — «Спокойная ночь», 0..126 s.
(function () {
  const { W, H } = K;
  const I = n => K.images[n];
  const FONT = 'Manrope';

  // ---------------- timeline ----------------
  const EX = 78.0; // explosion
  const items = [ // chat log: chips (dates) and messages from Kostya
    { t: 3.6, chip: 'ноябрь 1937' },
    { t: 4.6, msg: 'мам, я доехал.' },
    { t: 8.2, msg: 'тут холодно, но люди хорошие.' },
    { t: 11.8, msg: 'дядя передаст тебе письмо.' },
    { t: 19.2, chip: 'март 1938' },
    { t: 20.2, msg: 'я снова учусь.' },
    { t: 23.4, msg: 'ты получила письмо?' },
    { t: 26.8, msg: 'мама?' },
    { t: 42.2, chip: 'июнь 1941' },
    { t: 43.4, msg: 'у меня всё хорошо.' },
    { t: 47.6, msg: 'сегодня вытащил двоих.' },
    { t: 51.2, msg: 'один был англичанин.' },
    { t: 56.4, chip: '1943' },
    { t: 57.4, msg: 'не волнуйся.' },
    { t: 66.0, msg: 'скоро напишу ещё.' },
    { t: 86.0, chip: '1944' },
    { t: 97.4, msg: 'ты бы меня не узнала.' },
    { t: 104.2, chip: '1946' },
    { t: 105.4, msg: 'я сдержал обещание.' },
    { t: 109.6, msg: 'спокойной ночи, мам.' },
  ];
  const TYPING_MAMA = [15.0, 17.9]; // she seems to be typing… and then nothing
  // drafts typed and erased in the input field (hospital)
  const DRAFTS = [
    { s: 'мам, я', a: 87.0, hold: 0.9 },
    { s: 'мама, со мной', a: 89.6, hold: 1.0 },
    { s: 'я', a: 92.9, hold: 1.2 },
    { s: 'ты бы меня не узнала.', a: 95.0, hold: 0.6, send: true },
  ];
  const END = 126;

  // phase weights
  const W_ = (t, a, b, fi, fo) => K.window(t, a, b, fi, fo);
  const night = t => 1 - hosp(t);
  const hosp = t => K.clamp(K.prog(t, 82.5, 85.5) - K.prog(t, 99.5, 103));
  const war = t => W_(t, 40, EX + 0.2, 4, 0.1);
  const starsW = t => W_(t, 31, 43, 3, 3);

  // snow speed factor integrated over time so flakes can slow down and freeze into stars
  const e = (t, a, b) => K.ease.inOutSine(K.prog(t, a, b));
  const speed = t => 1 - e(t, 29, 35) + 0.35 * e(t, 41, 46) + 0.65 * e(t, 101, 106);
  const TAU = [0];
  for (let i = 1; i <= END * 10 + 10; i++) TAU.push(TAU[i - 1] + speed((i - 0.5) / 10) / 10);
  const tau = t => { const i = Math.max(0, Math.min(TAU.length - 2, Math.floor(t * 10))), f = t * 10 - i; return TAU[i] + (TAU[i + 1] - TAU[i]) * f; };

  // ---------------- helpers ----------------
  const mix = (a, b, p) => a.map((v, i) => Math.round(v + (b[i] - v) * p));
  const rgb = (c, a = 1) => `rgba(${c[0]},${c[1]},${c[2]},${a})`;
  const spring = x => { // 0 → 1 with a soft overshoot, x in seconds since start
    if (x <= 0) return 0;
    return 1 - Math.exp(-7 * x) * Math.cos(9 * x);
  };
  const rr = (ctx, x, y, w, h, r) => { ctx.beginPath(); ctx.roundRect(x, y, w, h, r); };

  // ---------------- cached assets ----------------
  const BUB_MAX = 700, PADX = 38, PADY = 28, FS = 46, LH = 60, R = 42;
  const cache = new Map();
  const bubble = (text, light) => {
    const key = text + '|' + light;
    if (cache.has(key)) return cache.get(key);
    const m = K.makeCanvas(10, 10).getContext('2d'); m.font = `600 ${FS}px ${FONT}`;
    const lines = K.wrap(m, text, BUB_MAX - PADX * 2);
    const tw = Math.max(...lines.map(l => m.measureText(l).width));
    const w = Math.ceil(tw + PADX * 2), h = Math.ceil(lines.length * LH + PADY * 2 - (LH - FS) + 6), G = 60;
    const c = K.makeCanvas(w + G * 2, h + G * 2), x = c.getContext('2d');
    const g = x.createLinearGradient(G, G, G + w, G + h);
    g.addColorStop(0, '#4f5dff'); g.addColorStop(0.55, '#7b5cf5'); g.addColorStop(1, '#3fb0ff');
    x.shadowColor = light ? 'rgba(80,90,200,0.35)' : 'rgba(110,100,255,0.55)'; x.shadowBlur = 44;
    x.fillStyle = g; rr(x, G, G, w, h, R); x.fill();
    x.shadowColor = 'transparent';
    const hl = x.createLinearGradient(0, G, 0, G + h); // glassy top highlight
    hl.addColorStop(0, 'rgba(255,255,255,0.22)'); hl.addColorStop(0.5, 'rgba(255,255,255,0)');
    x.fillStyle = hl; rr(x, G, G, w, h, R); x.fill();
    x.font = `600 ${FS}px ${FONT}`; x.fillStyle = '#fff'; x.textBaseline = 'alphabetic';
    lines.forEach((l, i) => x.fillText(l, G + PADX, G + PADY + FS - 6 + i * LH));
    const b = { c, w, h, G };
    cache.set(key, b); return b;
  };
  const blobs = (() => {
    const mk = (col, r) => {
      const c = K.makeCanvas(r * 2, r * 2), x = c.getContext('2d'), g = x.createRadialGradient(r, r, 0, r, r, r);
      g.addColorStop(0, rgb(col, 0.55)); g.addColorStop(1, rgb(col, 0)); x.fillStyle = g; x.fillRect(0, 0, r * 2, r * 2); return c;
    };
    return { violet: mk([88, 52, 190], 700), navy: mk([30, 60, 160], 800), teal: mk([20, 120, 140], 650), ember: mk([255, 110, 40], 900), white: mk([255, 255, 255], 900) };
  })();
  // flakes / stars share positions
  const P = (() => { const r = K.rng(4242), a = []; for (let i = 0; i < 320; i++) a.push({ x: r() * W, y: r() * H, z: 0.25 + r() * 0.75, ph: r() * 6.28, tw: 0.5 + r() * 2 }); return a; })();
  // glass cracks from the impact point
  const CRACKS = (() => {
    const r = K.rng(77), segs = [], ox = 640, oy = 980;
    const grow = (x, y, ang, len, depth) => {
      let px = x, py = y;
      const n = 6 + Math.floor(r() * 6);
      for (let i = 0; i < n; i++) {
        ang += (r() - 0.5) * 0.7; const l = len * (0.5 + r());
        const nx = px + Math.cos(ang) * l, ny = py + Math.sin(ang) * l;
        segs.push({ x1: px, y1: py, x2: nx, y2: ny, d: Math.hypot(px - ox, py - oy) });
        px = nx; py = ny;
        if (depth < 2 && r() < 0.28) grow(px, py, ang + (r() - 0.5) * 1.6, len * 0.7, depth + 1);
      }
    };
    for (let k = 0; k < 13; k++) grow(ox, oy, (k / 13) * Math.PI * 2 + r() * 0.3, 70, 0);
    for (let k = 0; k < 26; k++) { // ring fractures
      const a0 = r() * 6.28, rad = 40 + r() * 90;
      segs.push({ x1: ox + Math.cos(a0) * rad, y1: oy + Math.sin(a0) * rad, x2: ox + Math.cos(a0 + 0.5) * rad, y2: oy + Math.sin(a0 + 0.5) * rad, d: rad });
    }
    return segs;
  })();
  const FLASHES = (() => { const r = K.rng(9), a = []; let x = 47; while (x < EX) { a.push({ t: x, x: 150 + r() * 780, s: 0.4 + r() * 0.6 }); x += Math.max(0.35, (EX - x) / 7) * (0.5 + r()); } return a; })();

  // ---------------- background ----------------
  const background = (ctx, t, frame) => {
    const L = hosp(t), wr = war(t);
    const base = mix(mix([6, 8, 16], [24, 12, 10], wr * 0.8), [214, 220, 226], L);
    ctx.fillStyle = rgb(base); ctx.fillRect(0, 0, W, H);
    // drifting colour blobs
    ctx.save(); ctx.globalCompositeOperation = L > 0.5 ? 'source-over' : 'screen';
    const bl = (img, x, y, a) => { ctx.globalAlpha = a; ctx.drawImage(img, x - img.width / 2, y - img.height / 2); };
    const nA = (1 - L) * (1 - wr * 0.7);
    bl(blobs.violet, 300 + Math.sin(t * 0.11) * 200, 600 + Math.cos(t * 0.07) * 250, 0.9 * nA);
    bl(blobs.navy, 820 + Math.cos(t * 0.09) * 180, 1300 + Math.sin(t * 0.06) * 260, 0.9 * nA);
    bl(blobs.teal, 540 + Math.sin(t * 0.05 + 2) * 300, 1700 + Math.cos(t * 0.08) * 150, 0.7 * nA);
    // war: low burning horizon + flashes
    if (wr > 0) {
      const pulse = 0.75 + 0.25 * Math.sin(t * 1.7) * Math.sin(t * 0.9);
      bl(blobs.ember, 540, H + 250, 0.85 * wr * pulse);
      for (const f of FLASHES) {
        const a = K.clamp(1 - (t - f.t) / 0.9) * (t >= f.t ? 1 : 0);
        if (a > 0) bl(blobs.ember, f.x, H - 80, a * f.s * wr);
      }
    }
    if (L > 0) { ctx.globalCompositeOperation = 'source-over'; bl(blobs.white, 540, 700 + Math.sin(t * 0.2) * 80, 0.6 * L); }
    ctx.restore();

    // train window lights streaking past (first chapter)
    const tr = W_(t, 5, 30, 3, 4) * night(t);
    if (tr > 0) {
      ctx.save(); ctx.globalCompositeOperation = 'screen';
      const r = K.rng(31);
      for (let i = 0; i < 14; i++) {
        const sp = 900 + r() * 700, y = 300 + r() * 1400, sz = 30 + r() * 90, off = r() * 4000;
        const x = W + 300 - ((t * sp + off) % (W + 1200));
        const g = ctx.createRadialGradient(x, y, 0, x, y, sz * 2.2);
        g.addColorStop(0, `rgba(255,190,120,${0.32 * tr})`); g.addColorStop(1, 'rgba(255,190,120,0)');
        ctx.fillStyle = g; ctx.save(); ctx.translate(x, y); ctx.scale(2.6, 1); ctx.translate(-x, -y);
        ctx.fillRect(x - sz * 2.2, y - sz * 2.2, sz * 4.4, sz * 4.4); ctx.restore();
      }
      ctx.restore();
    }

    // snow ↔ stars ↔ ash
    const T = tau(t), st = starsW(t);
    ctx.save();
    for (const p of P) {
      const y = ((p.y + T * 140 * p.z) % (H + 40)) - 20;
      const x = (p.x + Math.sin(T * 0.6 + p.ph) * 30 * p.z + W) % W;
      const tw = 0.55 + 0.45 * Math.sin(t * p.tw * 2 + p.ph);
      const size = K.lerp(1.2 + p.z * 3.4, 0.8 + p.z * 1.6, st);
      const a = K.lerp(0.35 + 0.5 * p.z, (0.4 + 0.6 * p.z) * tw, st);
      let col = mix([235, 240, 255], [190, 205, 255], st);
      col = mix(col, [150, 140, 135], wr * 0.8); col = mix(col, [120, 130, 145], L);
      ctx.fillStyle = rgb(col, a * (1 - L * 0.6));
      ctx.beginPath(); ctx.arc(x, y, size, 0, 7); ctx.fill();
    }
    ctx.restore();
    if (wr > 0) K.embers(ctx, t, { n: 60, alpha: 0.65 * wr });

    // the big year counter while time passes (stars section)
    const yc = W_(t, 33.5, 41.5, 1.2, 1.0);
    if (yc > 0) {
      const p = K.ease.inOutSine(K.prog(t, 34, 40.5)), yr = Math.round(K.lerp(1938, 1941, p));
      ctx.save(); ctx.globalAlpha = yc;
      ctx.font = `800 250px ${FONT}`; ctx.textAlign = 'center'; ctx.textBaseline = 'alphabetic';
      ctx.fillStyle = rgb(mix([235, 238, 255], [255, 170, 120], K.prog(t, 39.5, 41.5)));
      const g = K.glyphs((ctx.shadowColor = 'rgba(140,150,255,0.9)', ctx.shadowBlur = 50, ctx), String(yr));
      ctx.shadowBlur = 0; ctx.drawImage(g.c, W / 2 - (g.c.width - g.pad * 2) / 2 - g.pad, 960 - g.asc);
      ctx.restore();
    }
  };

  // ---------------- chat ----------------
  const CHAT_BOTTOM = 1520; // y of the newest item's bottom, in chat-plane coordinates
  const chipH = 120;
  const layout = t => { // items visible at t with their appear progress
    const vis = [];
    for (const it of items) if (t >= it.t) vis.push({ ...it, p: spring(t - it.t), h: it.chip ? chipH : bubble(it.msg, false).h + 26 });
    const tm = t >= TYPING_MAMA[0] && t < TYPING_MAMA[1] + 0.5;
    if (tm) vis.push({ typing: true, t: TYPING_MAMA[0], h: 130, p: spring(t - TYPING_MAMA[0]) * (1 - K.prog(t, TYPING_MAMA[1], TYPING_MAMA[1] + 0.4)) });
    vis.sort((a, b) => a.t - b.t);
    let y = CHAT_BOTTOM;
    for (let i = vis.length - 1; i >= 0; i--) { vis[i].y = y; y -= vis[i].h * Math.min(1, vis[i].p); }
    return vis;
  };

  const drawChat = (ctx, t, frame) => {
    const L = hosp(t);
    const ink = rgb(mix([235, 238, 250], [30, 34, 44], L));
    const dim = rgb(mix([150, 156, 175], [110, 116, 128], L));
    // the chat plane floats in pseudo-3D
    const away = K.ease.inOutCubic(K.prog(t, 29.5, 33.5)) - K.ease.inOutCubic(K.prog(t, 40.6, 43.0));
    const off = K.ease.inOutSine(K.prog(t, 112, 118));
    const sh = K.shake(t, EX, 1.4, 50), rumble = war(t) * (FLASHES.some(f => t >= f.t && t - f.t < 0.25) ? 6 : 0);
    const rot = (-3 + 1.6 * Math.sin(t * 0.13)) * Math.PI / 180;
    const sk = 0.035 * Math.sin(t * 0.09), sc = (1 + 0.015 * Math.sin(t * 0.07)) * (1 - away * 0.35);
    const alpha = (1 - away * 0.92) * (1 - off) * (t < EX + 0.15 || t > 82 ? 1 : 0);
    if (alpha <= 0.01) return;
    ctx.save();
    ctx.globalAlpha = alpha;
    ctx.translate(W / 2 + sh.x + (K.hash(frame, 9) - 0.5) * rumble, H / 2 + sh.y - away * 260 + Math.sin(t * 0.5) * 10);
    ctx.transform(1, sk * 0.4, sk, 1, 0, 0); ctx.rotate(rot); ctx.scale(sc, sc);
    ctx.translate(-W / 2, -H / 2);

    // status bar + header
    const hd = K.ease.outCubic(K.prog(t, 1.6, 2.8));
    ctx.save(); ctx.globalAlpha *= hd; ctx.translate(0, (1 - hd) * -40);
    ctx.font = `600 34px ${FONT}`; ctx.fillStyle = ink; ctx.textAlign = 'left'; ctx.fillText('3:12', 90, 150);
    const noSig = t > EX && t < 86;
    for (let i = 0; i < 4; i++) { ctx.fillStyle = noSig || i > 1 ? dim : ink; if (!noSig || i === 0) ctx.fillRect(820 + i * 16, 150 - 10 - i * 7, 10, 10 + i * 7); }
    ctx.strokeStyle = ink; ctx.lineWidth = 3; rr(ctx, 905, 122, 64, 32, 8); ctx.stroke();
    ctx.fillStyle = '#ff4d4d'; ctx.fillRect(911, 128, 12, 20);
    if (noSig) { ctx.font = `600 26px ${FONT}`; ctx.fillStyle = '#ff6b6b'; ctx.textAlign = 'right'; ctx.fillText('нет сети', 800, 148); }
    // avatar + name
    const av = I('memories');
    ctx.save(); ctx.beginPath(); ctx.arc(W / 2, 270, 62, 0, 7); ctx.clip();
    ctx.filter = 'grayscale(0.6) brightness(0.95)';
    ctx.drawImage(av, av.width * 0.55, av.height * 0.03, av.width * 0.3, av.width * 0.3, W / 2 - 62, 208, 124, 124);
    ctx.filter = 'none'; ctx.restore();
    ctx.textAlign = 'center'; ctx.font = `700 42px ${FONT}`; ctx.fillStyle = ink; ctx.fillText('мама', W / 2, 395);
    ctx.font = `500 28px ${FONT}`; ctx.fillStyle = dim; ctx.fillText('была в сети давно', W / 2, 438);
    ctx.fillStyle = rgb(mix([255, 255, 255], [0, 0, 0], L), 0.08); ctx.fillRect(60, 480, W - 120, 2);
    ctx.restore();

    // messages
    const vis = layout(t);
    let lastMsg = null;
    for (const it of vis) if (it.msg) lastMsg = it;
    ctx.save(); ctx.beginPath(); ctx.rect(0, 495, W, 1180); ctx.clip();
    for (const it of vis) {
      const p = it.p, a = K.clamp(p * 1.4) * K.clamp((it.y - 560) / 160);
      if (a <= 0) continue;
      const s = 0.82 + 0.18 * Math.min(p, 1.08);
      ctx.save(); ctx.globalAlpha *= a;
      if (it.chip) {
        ctx.font = `600 28px ${FONT}`; ctx.textAlign = 'center';
        const tw = ctx.measureText(it.chip).width + 48;
        ctx.fillStyle = rgb(mix([255, 255, 255], [0, 0, 0], L), 0.08); rr(ctx, W / 2 - tw / 2, it.y - 92, tw, 50, 25); ctx.fill();
        ctx.fillStyle = dim; ctx.fillText(it.chip, W / 2, it.y - 57);
      } else if (it.typing) {
        const bx = 80, by = it.y - 120, bw = 170, bh = 96;
        ctx.translate(bx, by + bh); ctx.scale(s, s); ctx.translate(-bx, -(by + bh));
        ctx.fillStyle = rgb(mix([255, 255, 255], [0, 0, 0], L), 0.12); rr(ctx, bx, by, bw, bh, 48); ctx.fill();
        for (let k = 0; k < 3; k++) {
          const j = Math.max(0, Math.sin(t * 7 - k * 0.9));
          ctx.fillStyle = rgb([220, 224, 240], 0.45 + 0.5 * j);
          ctx.beginPath(); ctx.arc(bx + 50 + k * 35, by + bh / 2 - j * 10, 11, 0, 7); ctx.fill();
        }
      } else {
        const b = bubble(it.msg, L > 0.5), right = W - 70, x0 = right - b.w, y0 = it.y - 26 - b.h;
        ctx.translate(right, y0 + b.h); ctx.scale(s, s); ctx.translate(-right, -(y0 + b.h));
        ctx.drawImage(b.c, x0 - b.G, y0 - b.G);
        // shimmer sweep right after it lands
        const sp = K.prog(t, it.t + 0.15, it.t + 0.85);
        if (sp > 0 && sp < 1) {
          ctx.save(); rr(ctx, x0, y0, b.w, b.h, R); ctx.clip();
          const sx = K.lerp(x0 - 200, x0 + b.w + 200, sp), g = ctx.createLinearGradient(sx - 120, 0, sx + 120, 0);
          g.addColorStop(0, 'rgba(255,255,255,0)'); g.addColorStop(0.5, 'rgba(255,255,255,0.35)'); g.addColorStop(1, 'rgba(255,255,255,0)');
          ctx.fillStyle = g; ctx.fillRect(x0, y0, b.w, b.h); ctx.restore();
        }
        if (it === lastMsg) {
          const da = K.prog(t, it.t + 1.0, it.t + 1.5);
          ctx.globalAlpha *= da; ctx.font = `500 26px ${FONT}`; ctx.fillStyle = dim; ctx.textAlign = 'right';
          ctx.fillText('Доставлено', right - 10, it.y + 6);
        }
      }
      ctx.restore();
    }
    ctx.restore();

    // input field
    ctx.save();
    ctx.fillStyle = rgb(mix([255, 255, 255], [0, 0, 0], L), 0.07); rr(ctx, 70, 1640, W - 140, 96, 48); ctx.fill();
    ctx.strokeStyle = rgb(mix([255, 255, 255], [0, 0, 0], L), 0.12); ctx.lineWidth = 2; ctx.stroke();
    let draft = '', caret = false;
    for (const d of DRAFTS) {
      const typeEnd = d.a + d.s.length / 9, delEnd = typeEnd + d.hold + d.s.length / 22;
      if (t >= d.a && t < (d.send ? typeEnd + d.hold : delEnd)) {
        caret = true;
        if (t < typeEnd) draft = d.s.slice(0, Math.floor((t - d.a) * 9));
        else if (t < typeEnd + d.hold) draft = d.s;
        else draft = d.s.slice(0, Math.max(0, d.s.length - Math.floor((t - typeEnd - d.hold) * 22)));
      }
      if (t >= d.a - 0.6 && t < d.a) caret = true;
    }
    ctx.font = `500 40px ${FONT}`; ctx.textAlign = 'left';
    if (draft) { ctx.fillStyle = ink; ctx.fillText(draft, 120, 1702); } else { ctx.fillStyle = dim; ctx.fillText('Сообщение', 120, 1702); }
    if (caret && Math.floor(t * 2.4) % 2 === 0) { ctx.fillStyle = '#6c7bff'; ctx.fillRect(120 + (draft ? ctx.measureText(draft).width + 4 : 0), 1666, 4, 48); }
    ctx.restore();

    // cracks in the glass: fast, then a faint scar for the rest of the film
    if (t >= EX) {
      const cp = K.clamp((t - EX) / 0.18), keep = t < 82 ? 1 : 0.16;
      ctx.save(); ctx.lineCap = 'round';
      for (const s of CRACKS) {
        if (s.d > 900 * cp) continue;
        ctx.strokeStyle = `rgba(255,255,255,${0.75 * keep})`; ctx.lineWidth = 2.2;
        ctx.beginPath(); ctx.moveTo(s.x1, s.y1); ctx.lineTo(s.x2, s.y2); ctx.stroke();
      }
      ctx.restore();
    }
    ctx.restore();
  };

  // ---------------- explosion / silence / ending ----------------
  const overlay = (ctx, t, frame) => {
    // flash and black silence after the explosion
    if (t >= EX && t < 86) {
      const fl = t < EX + 0.06 ? 1 : 1 - K.ease.outQuad(K.prog(t, EX + 0.06, EX + 1.1));
      ctx.fillStyle = `rgba(255,250,240,${fl})`; ctx.fillRect(0, 0, W, H);
      const bk = K.prog(t, EX + 0.9, EX + 1.6) * (1 - K.prog(t, 82.5, 85));
      ctx.fillStyle = `rgba(0,0,0,${bk})`; ctx.fillRect(0, 0, W, H);
      // a lone heartbeat in the dark
      if (t > 80) K.ecg(ctx, t, 80.0, 6, { y: 960, amp: 80, beat: 420, alpha: K.window(t, 80, 86, 0.5, 1.5) * 0.8, color: 'rgba(130,255,170,0.9)' });
    }
    if (t >= 86 && t < 100) K.ecg(ctx, t, 86, 14, { y: 1820, amp: 40, beat: 380, alpha: 0.35 * hosp(t), color: 'rgba(60,170,110,0.9)' });
    // the screen goes dark; his reflection in the black glass
    if (t > 112) {
      const off = K.ease.inOutSine(K.prog(t, 113, 118.5));
      ctx.fillStyle = `rgba(0,0,0,${off})`; ctx.fillRect(0, 0, W, H);
      const ra = K.window(t, 117.5, END, 3, 3.5) * 0.17;
      if (ra > 0) {
        ctx.save(); ctx.globalAlpha = ra; ctx.filter = 'grayscale(1) blur(2px) contrast(1.2)';
        K.cover(ctx, I('mask'), { x: 60, y: 300, w: W - 120, h: 1300 }, 1.1 + 0.04 * K.prog(t, 117, END), 0.5, 0.36);
        ctx.filter = 'none';
        const g = ctx.createLinearGradient(0, 0, W, H); // glass glare
        g.addColorStop(0.3, 'rgba(255,255,255,0)'); g.addColorStop(0.5, 'rgba(255,255,255,0.25)'); g.addColorStop(0.7, 'rgba(255,255,255,0)');
        ctx.fillStyle = g; ctx.fillRect(0, 0, W, H);
        ctx.restore();
      }
      // the last "Доставлено" lingers
      const la = K.window(t, 113, 121.5, 0.1, 2.5) * (1 - off * 0.6);
      if (la > 0) { ctx.font = `500 28px ${FONT}`; ctx.textAlign = 'center'; ctx.fillStyle = `rgba(160,166,185,${la})`; ctx.fillText('Доставлено', W / 2, 1000); }
    }
  };

  window.VERSION = {
    duration: END,
    images: { memories: '/video/assets/up/c_memories.jpg', mask: '/video/assets/up/c_portrait_mask.jpg' },
    render(ctx, t, frame) {
      background(ctx, t, frame);
      drawChat(ctx, t, frame);
      overlay(ctx, t, frame);
      K.grain(ctx, frame, 0.05);
      K.vignette(ctx, 0.45);
      if (t < 2.5) { ctx.fillStyle = `rgba(0,0,0,${1 - K.ease.inOutSine(t / 2.5)})`; ctx.fillRect(0, 0, W, H); }
    },
  };
})();
