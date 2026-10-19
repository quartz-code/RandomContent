// Version C — «Судно (Борис Рыжий)» (Молчат Дома). Concept: post-punk. Monochrome ordered
// dither, huge condensed words as chapters, hard cuts on every bar, glitch on the explosion.
// The whole song is used (141 s, natural ending).
(function () {
  const { W, H } = K;
  const I = n => K.images[n];
  const up = n => `/video/assets/up/${n}.png`;
  const img = n => `/assets/images/${n}`;
  const RED = '#d4231e';
  const BAR = 2.972, B0 = 0.23, bar = k => B0 + k * BAR;

  // ---- building blocks: each scene has draw (image, gets dithered) and text (crisp overlay) ----
  const pic = (key, k0, k1, from, to, caps = [], o = {}) => ({
    a: bar(k0), b: bar(k1), fi: 0, fo: 0, cut: true,
    draw(ctx, t, s) {
      const punch = K.lerp(1.07, 1, K.ease.outCubic(K.clamp(s.lt / 0.35)));
      const f = { ...from, z: from.z * punch };
      ctx.filter = `grayscale(1) contrast(${o.contrast ?? 1.35}) brightness(${o.bright ?? 1.05})`;
      K.kenBurns(ctx, I(key), s.p, f, to, undefined, o.src);
      ctx.filter = 'none';
      if (o.extra) o.extra(ctx, t, s);
    },
    text(ctx, t) { for (const c of caps) caption(ctx, c.s, t, bar(c.k ?? k0) + (c.d ?? 0.3), c); },
  });
  const word = (w, k0, k1, sub, o = {}) => ({
    a: bar(k0), b: bar(k1), fi: 0, fo: 0, cut: true,
    draw(ctx) { ctx.fillStyle = '#000'; ctx.fillRect(0, 0, W, H); },
    text(ctx, t) {
      const lt = t - bar(k0), s = K.lerp(1.18, 1, K.ease.outExpo(K.clamp(lt / 0.4)));
      ctx.save(); ctx.translate(W / 2, H / 2 - 40); ctx.scale(s, s);
      ctx.font = 'bold 300px "Oswald"'; ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
      const mw = ctx.measureText(w).width, fit = Math.min(1, 980 / mw);
      ctx.scale(fit, 1 + (1 - fit) * 0.6);
      ctx.fillStyle = o.red ? RED : '#f2f2f2'; ctx.fillText(w, 0, 0);
      ctx.restore();
      ctx.fillStyle = o.red ? '#f2f2f2' : RED;
      ctx.fillRect(W / 2 - 240, H / 2 + 150, 480 * K.ease.outCubic(K.clamp(lt / 0.6)), 14);
      if (sub) K.type(ctx, sub, t, bar(k0) + 0.5, { x: W / 2, align: 'center', y: H / 2 + 260, font: '40px "PT Mono"', cps: 34, color: '#d8d8d8', cursor: false });
    },
  });
  // subtitle tape: white mono text on a black strip
  const caption = (ctx, s, t, t0, o = {}) => {
    if (t < t0) return;
    ctx.save(); ctx.font = '42px "PT Mono"';
    const lines = K.wrap(ctx, s, 880), n = Math.floor((t - t0) * 40), y0 = o.y ?? 1560, lh = 66;
    let left = n;
    lines.forEach((L, i) => {
      const w = ctx.measureText(L).width, y = y0 + i * lh;
      ctx.fillStyle = o.red ? RED : '#000'; ctx.fillRect(70, y - 46, Math.min(w, ctx.measureText(L.slice(0, Math.max(0, left))).width) + 40, 62);
      ctx.fillStyle = '#f2f2f2'; ctx.fillText(L.slice(0, Math.max(0, left)), 90, y);
      left -= L.length + 1;
    });
    ctx.restore();
  };

  const KB = (z0, y0, z1, y1, x = 0.5) => [{ z: z0, x, y: y0 }, { z: z1, x, y: y1 }];
  const WAR = ['medic', 'carry', 'scar', 'headdown', 'whitecoat', 'carry', 'scar'];
  const STUTTER_WORDS = ['СНАЙПЕРЫ', 'ГРАНАТА', 'ШРАМ', 'ОГОНЬ'];
  const EXK = 27, EX = bar(EXK);

  const scenes = [
    // intro (quiet part of the song)
    { a: 0, b: bar(4), fi: 1.2, fo: 0, cut: false,
      draw(ctx, t, s) {
        ctx.filter = 'grayscale(1) contrast(1.4) brightness(0.9)';
        const sk = I('sketch');
        K.kenBurns(ctx, sk, s.p, { z: 1.0, x: 0.5, y: 0.2 }, { z: 1.35, x: 0.72, y: 0.12 }, undefined, [0, 0, sk.width, sk.height * 0.62]);
        ctx.filter = 'none';
      },
      text(ctx, t) {
        caption(ctx, 'обещание маме:', t, bar(1));
        caption(ctx, 'не причинять людям вреда.', t, bar(2), { y: 1640 });
      } },
    word('КОСТЯ', 4, 5, 'понтийский грек. ссср.'),
    word('ОТЕЦ', 5, 6, 'забран в 1920-х.'),
    pic('boy', 6, 8, ...KB(1.0, 0.3, 1.12, 0.25), [{ s: 'он почти не помнил его.' }]),
    pic('memories', 8, 9, ...KB(1.05, 0.3, 1.12, 0.25), [{ s: 'друзья. мама.' }]),
    pic('student', 9, 10, ...KB(1.0, 0.3, 1.1, 0.26), [{ s: 'медицинский. последний курс.' }]),
    word('ЧЕКИСТЫ', 10, 11, 'приказ ловить понтийцев.', { red: true }),
    pic('mother', 11, 13, ...KB(1.0, 0.45, 1.14, 0.4, 0.62), [
      { s: '— как тогда. как твоего отца.' },
      { s: 'он пел ей колыбельную.', k: 12, y: 1640 },
    ], { bright: 1.25 }),
    word('ПОБЕГ', 13, 14),
    pic('train', 14, 16, { z: 1.0, x: 0.38, y: 0.5 }, { z: 1.06, x: 0.75, y: 0.45 }, [
      { s: 'из страны, которая забрала отца.' },
      { s: 'а теперь — и мать.', k: 15, y: 1640 },
    ], { bright: 1.7, extra: (c, t) => K.snow(c, t, { n: 80, alpha: 0.6, size: 3 }) }),
    pic('letter', 16, 17, ...KB(1.0, 0.5, 1.08, 0.5), [{ s: 'одно письмо.' }]),
    pic('booth', 17, 18, ...KB(1.0, 0.55, 1.1, 0.5, 0.3), [{ s: 'больше — ничего.' }],
      { bright: 1.6, extra: (c, t) => K.rain(c, t, { n: 120, color: 'rgba(255,255,255,0.4)' }) }),
    word('ВОЙНА', 18, 19, null, { red: true }),
    pic('medic', 19, 21, ...KB(1.0, 0.35, 1.12, 0.28), [{ s: 'полевой хирург.' }]),
    pic('whitecoat', 21, 22, ...KB(1.0, 0.4, 1.1, 0.35), [{ s: 'своих и чужих.' }]),
    pic('carry', 22, 24, ...KB(1.0, 0.35, 1.14, 0.3), [{ s: 'он выносил раненых из-под огня.' }]),
    pic('scar', 24, 25, ...KB(1.0, 0.35, 1.1, 0.3), [{ s: 'пистолет всегда разряжен.', red: true }]),
    // stutter montage: half-beat cuts with flashing words
    { a: bar(25), b: EX, fi: 0, fo: 0, cut: false,
      draw(ctx, t) {
        const k = Math.floor((t - bar(25)) / (BAR / 8)), lt = (t - bar(25)) % (BAR / 8);
        ctx.filter = 'grayscale(1) contrast(1.6)';
        K.cover(ctx, I(WAR[k % WAR.length]), { x: 0, y: 0, w: W, h: H }, 1.08 + (k % 3) * 0.06, 0.5, 0.35);
        ctx.filter = 'none';
        if (lt < 0.05) { ctx.fillStyle = '#fff'; ctx.fillRect(0, 0, W, H); }
      },
      text(ctx, t) {
        const k = Math.floor((t - bar(25)) / (BAR / 2));
        ctx.save(); ctx.font = 'bold 190px "Oswald"'; ctx.textAlign = 'center'; ctx.fillStyle = k % 2 ? RED : '#f2f2f2';
        ctx.fillText(STUTTER_WORDS[k % STUTTER_WORDS.length], W / 2, H / 2 + 60); ctx.restore();
      } },
    // explosion
    { a: EX, b: bar(29), fi: 0, fo: 0, cut: false, glitch: [EX, EX + 1.6],
      draw(ctx, t) {
        const sh = K.shake(t, EX, 1.5, 70);
        ctx.filter = `grayscale(1) contrast(1.8) blur(${(6 * (1 - K.prog(t, EX, EX + 1.2))).toFixed(1)}px)`;
        K.cover(ctx, I('headdown'), { x: -80 + sh.x, y: -80 + sh.y, w: W + 160, h: H + 160 }, 1.15, 0.5, 0.35);
        ctx.filter = 'none';
        const fl = t < EX + 0.08 ? 1 : 1 - K.prog(t, EX + 0.08, EX + 1.3);
        ctx.fillStyle = `rgba(255,255,255,${fl})`; ctx.fillRect(0, 0, W, H);
        ctx.fillStyle = `rgba(0,0,0,${K.prog(t, EX + 1.2, EX + 1.8)})`; ctx.fillRect(0, 0, W, H);
      },
      text(ctx, t) {
        caption(ctx, 'он донёс солдата.', t, EX + 2.0, { y: 900 });
        caption(ctx, 'и только потом упал.', t, bar(28) + 0.2, { y: 980, red: true });
      } },
    word('ТИШИНА', 29, 30),
    pic('bath', 30, 33, ...KB(1.0, 0.4, 1.14, 0.32, 0.62), [
      { s: 'он молчал неделями.' },
      { s: 'смотрел в потолок.', k: 31.5, y: 1640 },
    ], { extra: (c, t) => K.ecg(c, t, bar(30), BAR * 3, { y: 1260, color: 'rgba(255,255,255,0.9)', amp: 70 }) }),
    word('МАСКА', 33, 34),
    pic('mask', 34, 36, ...KB(1.0, 0.38, 1.14, 0.34), [{ s: 'он боится увидеть себя без неё.' }]),
    word('СВОБОДА', 36, 37, 'а не смерть.', { red: true }),
    pic('rebel', 37, 39, ...KB(1.0, 0.4, 1.12, 0.35), [{ s: 'после войны — повстанцы.' }]),
    pic('walk', 39, 42, ...KB(1.0, 0.55, 1.18, 0.5), [
      { s: 'он спасает даже врагов.' },
      { s: 'где-то есть ребёнок, каким был он,', k: 40, y: 1640 },
      { s: 'который ждёт домой отца.', k: 41, y: 1720, red: true },
    ]),
    pic('boy', 42, 48, ...KB(1.12, 0.3, 1.25, 0.26), [], { bright: 0.7 }),
  ];
  // final title over the last image
  const title = (ctx, t) => {
    const a = K.prog(t, bar(43), bar(43) + 1.2) * (1 - K.prog(t, 137.5, 140.5));
    if (a <= 0) return;
    ctx.save(); ctx.globalAlpha = a;
    ctx.fillStyle = 'rgba(0,0,0,0.55)'; ctx.fillRect(0, H / 2 - 230, W, 420);
    ctx.font = 'bold 230px "Oswald"'; ctx.textAlign = 'center'; ctx.fillStyle = '#f2f2f2';
    ctx.fillText('КОНСТАНТИН', W / 2, H / 2 + 60, 980);
    ctx.fillStyle = RED; ctx.fillRect(W / 2 - 240, H / 2 + 110, 480, 14);
    ctx.restore();
  };

  // ---- dither + glitch post ----
  const SW = 540, SH = 960, small = K.makeCanvas(SW, SH), sx = small.getContext('2d', { willReadFrequently: true });
  const BAYER = [0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5].map(v => (v + 0.5) / 16 - 0.5);
  const LEVELS = [6, 78, 160, 242];
  const dither = (ctx) => {
    sx.drawImage(ctx.canvas, 0, 0, SW, SH);
    const d = sx.getImageData(0, 0, SW, SH), p = d.data;
    for (let y = 0; y < SH; y++) for (let x = 0; x < SW; x++) {
      const i = (y * SW + x) * 4;
      const l = (p[i] * 0.299 + p[i + 1] * 0.587 + p[i + 2] * 0.114) / 255 + BAYER[(y & 3) * 4 + (x & 3)] * 0.33;
      const v = LEVELS[Math.max(0, Math.min(3, Math.round(l * 3)))];
      p[i] = p[i + 1] = p[i + 2] = v;
    }
    sx.putImageData(d, 0, 0);
    ctx.save(); ctx.imageSmoothingEnabled = false; ctx.drawImage(small, 0, 0, W, H); ctx.restore();
  };
  const glitchBuf = K.makeCanvas();
  const glitch = (ctx, frame, amt) => {
    const g = glitchBuf.getContext('2d'); g.drawImage(ctx.canvas, 0, 0);
    const r = K.rng(frame * 13 + 1);
    for (let i = 0; i < 14 * amt; i++) {
      const y = r() * H, h = 10 + r() * 120, dx = (r() - 0.5) * 260 * amt;
      ctx.drawImage(glitchBuf, 0, y, W, h, dx, y, W, h);
    }
    if (r() < 0.5 * amt) { ctx.save(); ctx.globalCompositeOperation = 'difference'; ctx.fillStyle = '#fff'; ctx.fillRect(0, 0, W, H); ctx.restore(); }
  };
  const scan = K.makeCanvas(W, 8);
  { const s = scan.getContext('2d'); s.fillStyle = 'rgba(0,0,0,0.22)'; s.fillRect(0, 0, W, 3); }

  window.VERSION = {
    duration: 141.4,
    images: {
      sketch: up('c_sketch_mama'), boy: img('boy-school.png'), memories: up('c_memories'), student: img('student.png'),
      mother: up('p_mother_room'), letter: up('p_letter'), train: img('train.png'), booth: img('phone-booth.png'),
      medic: img('medic-tent.png'), whitecoat: up('c_white_coat'), carry: up('c_carry'), scar: img('medic-scar.png'),
      headdown: up('c_head_down'), bath: img('bath.jpg'), mask: up('c_portrait_mask'), rebel: up('c_rebel_table'),
      walk: up('c_walk_away'),
    },
    render(ctx, t, frame) {
      K.runScenes(ctx, scenes, t);
      dither(ctx);
      let gl = 0;
      for (const s of scenes) {
        if (t < s.a || t >= s.b) continue;
        if (s.text) s.text(ctx, t);
        // one-frame inverted flash on every hard cut
        if (s.cut && t - s.a < 1 / 15) { ctx.save(); ctx.globalCompositeOperation = 'difference'; ctx.fillStyle = '#fff'; ctx.fillRect(0, 0, W, H); ctx.restore(); }
        if (s.glitch && t < s.glitch[1]) gl = 1 - K.prog(t, s.glitch[0], s.glitch[1]);
      }
      title(ctx, t);
      if (gl > 0) glitch(ctx, frame, gl);
      for (let y = 0; y < H; y += 8) ctx.drawImage(scan, 0, y);
      K.vignette(ctx, 0.55);
      if (t < 1.2) { ctx.fillStyle = `rgba(0,0,0,${1 - t / 1.2})`; ctx.fillRect(0, 0, W, H); }
      if (t > 139) { ctx.fillStyle = `rgba(0,0,0,${K.prog(t, 139, 141.2)})`; ctx.fillRect(0, 0, W, H); }
    },
  };
})();
