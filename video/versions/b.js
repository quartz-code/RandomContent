// Version B — «Легенда» (Кино). Concept: a film in chapters, widescreen band, cuts on the bar,
// a montage that accelerates into the explosion on the song's peak.
// Music: song 0..100.5 cross-faded into 206.5..end (see audio/build_b.sh), so after 98.5 s
// video time = song time - 108.
(function () {
  const { W, H } = K;
  const I = n => K.images[n];
  const up = n => `/video/assets/up/${n}.jpg`;
  const img = n => `/assets/images/${n}`;
  const GRADE = 'sepia(0.25) saturate(0.75) contrast(1.12) brightness(0.92)';
  const BY = 470, BH = 980; // the film band
  const BAR = 2.88, B0 = 0.81, beat = n => B0 + n * BAR / 4; // beat grid of the song

  // blurred backdrop is rendered once per image at low resolution (a full-frame blur per frame is slow)
  const bgCache = {};
  const backdrop = key => {
    if (!bgCache[key]) {
      const c = K.makeCanvas(270, 480), x = c.getContext('2d');
      x.filter = 'blur(7px) brightness(0.32) saturate(0.6)';
      K.cover(x, I(key), { x: -20, y: -20, w: 310, h: 520 }, 1.1);
      bgCache[key] = c;
    }
    return bgCache[key];
  };
  const band = (ctx, key, p, from, to, o = {}) => {
    const im = I(key);
    ctx.drawImage(backdrop(key), 0, 0, W, H);
    ctx.filter = o.grade || GRADE;
    ctx.save(); ctx.beginPath(); ctx.rect(0, BY, W, BH); ctx.clip();
    K.kenBurns(ctx, im, p, from, to, { x: 0, y: BY, w: W, h: BH }, o.src);
    ctx.filter = 'none';
    if (o.extra) o.extra(ctx);
    ctx.restore();
    ctx.filter = 'none'; // restore() brings back the grade set before save()
    ctx.fillStyle = 'rgba(236,224,200,0.55)';
    ctx.fillRect(60, BY - 18, W - 120, 2); ctx.fillRect(60, BY + BH + 16, W - 120, 2);
  };
  const chapter = (ctx, t, t0, roman, title) => {
    const a = K.window(t, t0, t0 + 4.5, 0.8, 1.0);
    if (a <= 0) return;
    K.text(ctx, roman, W / 2, 250, { font: '44px "Cormorant Garamond"', alpha: a * 0.8, letter: '8px' });
    K.text(ctx, title, W / 2, 335, { font: '64px "Cormorant Garamond"', alpha: a, letter: '22px' });
  };
  // one shot: band image + chapter label + words under the band
  const shot = (key, a, b, from, to, lines = [], o = {}) => ({
    a, b, fi: o.fi ?? 0.35, fo: o.fo ?? 0.35,
    draw(ctx, t, s) {
      band(ctx, key, s.p, from, to, { ...o, extra: o.extra && (c => o.extra(c, t)) });
      if (o.chapter) chapter(ctx, t, a, ...o.chapter);
      for (const L of lines) K.words(ctx, L.s, t, L.t, { y: L.y ?? 1590, ...L });
      if (o.leak) K.leak(ctx, t, a - 0.4, 1.6);
    },
  });

  // accelerating montage into the explosion
  const MONTAGE = ['medic', 'carry', 'scar', 'headdown', 'whitecoat', 'carry', 'scar', 'headdown', 'medic', 'carry', 'scar', 'headdown'];
  const EX = 91.75;
  // cuts every beat, then every half beat until the explosion
  const CUTS = [];
  for (let n = 116; beat(n) < 88.6; n++) CUTS.push(beat(n));
  for (let x = beat(122); x < EX - 0.1; x += BAR / 8) CUTS.push(x);
  const montage = {
    a: 84.33, b: EX, fi: 0.05, fo: 0, draw(ctx, t) {
      let k = 0;
      while (k + 1 < CUTS.length && CUTS[k + 1] <= t) k++;
      const cutT = CUTS[k], lp = K.clamp((t - cutT) / (BAR / 4));
      const key = MONTAGE[k % MONTAGE.length];
      const z = K.lerp(1.12, 1.0, K.ease.outCubic(lp));
      band(ctx, key, 0, { z, x: 0.5, y: 0.38 }, { z, x: 0.5, y: 0.38 }, { grade: `${GRADE} contrast(1.25)` });
      const f = 1 - K.clamp((t - cutT) / 0.12);
      ctx.fillStyle = `rgba(255,245,230,${f * 0.35})`; ctx.fillRect(0, 0, W, H);
      K.words(ctx, 'в него стреляли снайперы.', t, 84.6, { y: 1590 });
      K.words(ctx, 'он возвращался.', t, 87.5, { y: 1670, color: '#cdbfa8' });
    },
  };
  const explosion = {
    a: EX, b: 97.4, fi: 0, fo: 0.8, draw(ctx, t) {
      const L = K.layers[2], lx = L.getContext('2d'), sh = K.shake(t, EX, 1.8, 70);
      lx.setTransform(1, 0, 0, 1, sh.x, sh.y); lx.fillStyle = '#000'; lx.fillRect(-100, -100, W + 200, H + 200);
      lx.filter = `${GRADE} blur(${(10 * (1 - K.prog(t, EX, EX + 1.6))).toFixed(1)}px)`;
      K.cover(lx, I('headdown'), { x: -80, y: BY - 200, w: W + 160, h: BH + 400 }, 1.15, 0.5, 0.35);
      lx.filter = 'none'; lx.setTransform(1, 0, 0, 1, 0, 0);
      K.chroma(ctx, L, 40 * (1 - K.prog(t, EX, EX + 2.0)));
      const fl = t < EX + 0.1 ? K.prog(t, EX, EX + 0.1) : 1 - K.ease.outQuad(K.prog(t, EX + 0.1, EX + 1.5));
      ctx.fillStyle = `rgba(255,248,235,${fl})`; ctx.fillRect(0, 0, W, H);
      ctx.fillStyle = `rgba(0,0,0,${K.prog(t, EX + 1.3, EX + 2.0)})`; ctx.fillRect(0, 0, W, H);
      K.words(ctx, 'он донёс солдата.', t, EX + 2.4, { y: 900 });
      K.words(ctx, 'и только потом упал.', t, EX + 3.6, { y: 990, color: '#cdbfa8' });
    },
  };

  const scenes = [
    // prologue
    { a: 0, b: 6.6, fi: 1.5, fo: 0.5, draw(ctx, t, s) {
      ctx.filter = 'sepia(0.4) brightness(0.28) blur(1px)';
      K.kenBurns(ctx, I('sketch'), s.p, { z: 1.25, x: 0.72, y: 0.12 }, { z: 1.4, x: 0.72, y: 0.1 });
      ctx.filter = 'none';
      K.words(ctx, '«не причиняй людям вреда»', t, 1.4, { y: 900, font: 'italic 68px "Cormorant Garamond"', wps: 0.35 });
      K.words(ctx, '— мама', t, 3.7, { y: 1000, color: '#bfb2a0' });
    } },
    // I · childhood
    shot('boy', 6.57, 15.25, { z: 1.0, x: 0.5, y: 0.3 }, { z: 1.12, x: 0.5, y: 0.25 }, [
      { s: 'его отца забрали в двадцатые.', t: 8.0 },
      { s: 'костя почти не помнил его.', t: 10.9, y: 1670, color: '#cdbfa8' },
    ], { chapter: ['I', 'ДЕТСТВО'], leak: true }),
    shot('memories', 15.21, 21.0, { z: 1.05, x: 0.5, y: 0.3 }, { z: 1.15, x: 0.5, y: 0.22 }, [
      { s: 'но у него были друзья. и мама.', t: 16.0 },
    ]),
    // II · escape
    shot('student', 20.97, 26.75, { z: 1.0, x: 0.5, y: 0.3 }, { z: 1.12, x: 0.48, y: 0.26 }, [
      { s: 'медицинский. последний курс.', t: 22.2 },
    ], { chapter: ['II', 'ПОБЕГ'], leak: true }),
    shot('mother', 26.73, 38.3, { z: 1.0, x: 0.6, y: 0.45 }, { z: 1.15, x: 0.64, y: 0.4 }, [
      { s: '— чекисты, сынок. чекисты.', t: 27.6 },
      { s: '— как тогда. как твоего отца.', t: 30.4, y: 1670 },
      { s: 'он пел ей колыбельную, пока она не уснула.', t: 33.6, y: 1780, color: '#cdbfa8', font: 'italic 50px "Cormorant Garamond"' },
    ], { grade: 'sepia(0.4) saturate(0.5) brightness(0.75) contrast(1.1)' }),
    shot('train', 38.25, 44.05, { z: 1.0, x: 0.35, y: 0.5 }, { z: 1.08, x: 0.62, y: 0.45 }, [
      { s: 'в ту же ночь он бежал.', t: 39.0 },
    ], { extra: (c, t) => K.snow(c, t, { n: 70, alpha: 0.5, size: 3 }) }),
    shot('letter', 44.01, 46.95, { z: 1.0, x: 0.5, y: 0.5 }, { z: 1.08, x: 0.5, y: 0.5 }, [
      { s: 'одно письмо.', t: 44.4 },
    ], { grade: 'sepia(0.5) contrast(1.15)' }),
    shot('booth', 46.89, 52.7, { z: 1.0, x: 0.3, y: 0.55 }, { z: 1.12, x: 0.3, y: 0.5 }, [
      { s: 'больше он о маме ничего не слышал.', t: 47.4 },
    ], { extra: (c, t) => K.rain(c, t, { n: 120 }) }),
    // III · war
    shot('medic', 52.65, 58.45, { z: 1.0, x: 0.5, y: 0.35 }, { z: 1.12, x: 0.5, y: 0.28 }, [
      { s: 'война. полевой хирург.', t: 54.8 },
    ], { chapter: ['III', 'ВОЙНА'], leak: true }),
    shot('whitecoat', 58.41, 64.2, { z: 1.0, x: 0.5, y: 0.4 }, { z: 1.12, x: 0.5, y: 0.35 }, [
      { s: 'он лечил всех. своих и чужих.', t: 59.2 },
    ]),
    shot('carry', 64.17, 72.85, { z: 1.0, x: 0.5, y: 0.35 }, { z: 1.14, x: 0.5, y: 0.3 }, [
      { s: 'выносил раненых из-под огня.', t: 66.8 },
      { s: 'даже врагов.', t: 69.4, y: 1670, color: '#cdbfa8' },
    ], { extra: (c, t) => K.embers(c, t, { n: 50 }) }),
    shot('scar', 72.81, 78.6, { z: 1.0, x: 0.5, y: 0.35 }, { z: 1.12, x: 0.5, y: 0.28 }, [
      { s: 'его пистолет всегда был разряжен.', t: 73.6 },
    ]),
    shot('headdown', 78.57, 84.4, { z: 1.0, x: 0.5, y: 0.35 }, { z: 1.12, x: 0.5, y: 0.3 }, [
      { s: 'он не убил ни одного человека.', t: 79.4 },
    ], { extra: (c, t) => K.embers(c, t, { n: 40, alpha: 0.4 }) }),
    montage,
    explosion,
    // IV · silence
    shot('bath', 97.0, 105.9, { z: 1.0, x: 0.62, y: 0.4 }, { z: 1.14, x: 0.62, y: 0.32 }, [
      { s: 'потом были месяцы тишины.', t: 98.0 },
      { s: 'он ни с кем не говорил.', t: 101.2, y: 1670, color: '#cdbfa8' },
    ], { chapter: ['IV', 'ТИШИНА'], fi: 1.2, extra: (c, t) => K.ecg(c, t, 97.4, 8, { y: BY + BH - 140, alpha: 0.5, amp: 60 }) }),
    shot('mask', 105.5, 112.2, { z: 1.0, x: 0.5, y: 0.38 }, { z: 1.14, x: 0.5, y: 0.34 }, [
      { s: 'он до сих пор боится увидеть себя без маски.', t: 106.4 },
    ], { fi: 1.0 }),
    // V · after
    shot('rebel', 111.8, 117.6, { z: 1.0, x: 0.5, y: 0.4 }, { z: 1.12, x: 0.5, y: 0.35 }, [
      { s: '«мы за свободу, а не за смерть»', t: 112.8 },
    ], { chapter: ['V', 'ПОСЛЕ'], fi: 1.0 }),
    shot('walk', 117.2, 126.0, { z: 1.0, x: 0.5, y: 0.55 }, { z: 1.18, x: 0.5, y: 0.5 }, [
      { s: 'где-то есть такой же ребёнок, каким был он —', t: 118.0, font: 'italic 54px "Cormorant Garamond"' },
      { s: 'который ждёт домой отца.', t: 121.6, y: 1740, color: '#cdbfa8' },
    ], { fi: 1.0, fo: 1.4 }),
    { a: 125.2, b: 133, fi: 1.6, fo: 2.0, draw(ctx, t, s) {
      ctx.filter = 'brightness(0.55) saturate(0.6)';
      K.kenBurns(ctx, I('portrait'), s.p, { z: 1.0, x: 0.5, y: 0.4 }, { z: 1.08, x: 0.5, y: 0.38 },
        { x: 0, y: 380, w: W, h: W });
      ctx.filter = 'none';
      const a = K.prog(t, 126.4, 128.4);
      K.text(ctx, 'КОНСТАНТИН', W / 2, 1640, { font: '112px "Cormorant Garamond"', letter: '20px', alpha: a });
      K.text(ctx, 'легенда', W / 2, 1740, { font: 'italic 50px "Cormorant Garamond"', alpha: K.prog(t, 128, 130) * 0.8, color: '#cdbfa8' });
    } },
  ];

  window.VERSION = {
    duration: 133,
    images: {
      sketch: up('c_sketch_mama'), boy: img('boy-school.png'), memories: up('c_memories'), student: img('student.png'),
      mother: up('p_mother_room'), letter: up('p_letter'), train: img('train.png'), booth: img('phone-booth.png'),
      medic: img('medic-tent.png'), whitecoat: up('c_white_coat'), carry: up('c_carry'), scar: img('medic-scar.png'),
      headdown: up('c_head_down'), bath: img('bath.jpg'), mask: up('c_portrait_mask'), rebel: up('c_rebel_table'),
      walk: up('c_walk_away'), portrait: img('portrait-drawn.png'),
    },
    render(ctx, t, frame) {
      K.runScenes(ctx, scenes, t);
      K.grain(ctx, frame, 0.09);
      K.scratches(ctx, frame, 0.4);
      K.vignette(ctx, 0.5);
    },
  };
})();
