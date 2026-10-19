// Version A — «Спокойная ночь» (Кино). Concept: a personal file from an archive.
(function () {
  const { W, H } = K;
  const I = n => K.images[n];
  const GRADE = 'sepia(0.32) saturate(0.6) contrast(1.08) brightness(0.9)';
  const INK = '#2b2420';

  const up = n => `/video/assets/up/${n}.png`;
  const img = n => `/assets/images/${n}`;

  const shade = (ctx, from = 1150, a = 0.85) => {
    const g = ctx.createLinearGradient(0, from, 0, H);
    g.addColorStop(0, 'rgba(0,0,0,0)'); g.addColorStop(1, `rgba(0,0,0,${a})`);
    ctx.fillStyle = g; ctx.fillRect(0, from, W, H - from);
  };
  const desk = ctx => { ctx.fillStyle = '#0d0b09'; ctx.fillRect(0, 0, W, H); };
  const paper = ctx => ctx.drawImage(K.paper(), 0, 0);

  // full-bleed photo with Ken Burns + typed lines [{t, s, ...opts}]
  const photo = (key, a, b, from, to, lines = [], o = {}) => ({
    a, b, fi: o.fi ?? 0.8, fo: o.fo ?? 0.8,
    draw(ctx, t, s) {
      ctx.filter = o.grade || GRADE;
      K.kenBurns(ctx, I(key), s.p, from, to, undefined, o.src);
      ctx.filter = 'none';
      if (o.extra) o.extra(ctx, t, s);
      shade(ctx, o.shadeFrom ?? 1150);
      for (const L of lines) K.type(ctx, L.s, t, L.t, { y: L.y ?? 1560, maxW: 900, cps: L.cps ?? 24, ...L });
    },
  });

  const scenes = [
    // 1 — the promise
    { a: 0, b: 9.6, fi: 1.6, fo: 0.8, draw(ctx, t, s) {
      desk(ctx);
      const z = K.lerp(1, 1.1, K.ease.inOutSine(s.p));
      ctx.save(); ctx.translate(W / 2, 820); ctx.scale(z, z); ctx.translate(-W / 2, -820);
      const sk = I('sketch');
      K.print(ctx, sk, W / 2, 820, 880, 880 * 0.62 * sk.height / sk.width, -2,
        { src: [0, 0, sk.width, sk.height * 0.62], filter: 'sepia(0.2) contrast(1.1)', border: 18 });
      ctx.restore();
      K.type(ctx, 'он обещал маме никогда не вредить людям.', t, 3.8, { y: 1420, align: 'center', x: W / 2, maxW: 860 });
      K.type(ctx, 'и сдержал обещание.', t, 7.0, { y: 1600, align: 'center', x: W / 2, color: '#c9bfae' });
    } },

    // 2 — the file
    { a: 9.2, b: 19.2, fi: 0.8, fo: 0.6, draw(ctx, t, s) {
      const z = K.lerp(1, 1.04, s.p);
      ctx.save(); ctx.translate(W / 2, H / 2); ctx.scale(z, z); ctx.translate(-W / 2, -H / 2);
      paper(ctx);
      K.text(ctx, 'ЛИЧНОЕ ДЕЛО', W / 2, 330, { font: '96px "Old Standard TT"', color: INK, shadow: false, letter: '10px' });
      ctx.fillStyle = INK; ctx.fillRect(170, 375, W - 340, 4);
      const rows = [
        ['Имя: Константин (Костя)', 10.2],
        ['Национальность: понтийский грек', 11.4],
        ['Образование: медицинский,\n               не окончено', 12.7],
        ['Отец: арестован в 1920-х', 14.3],
        ['Мать: —', 15.4],
      ];
      let y = 560;
      for (const [r, t0] of rows) {
        K.type(ctx, r, t, t0, { x: 130, y, color: INK, shadow: false, cps: 30, font: '44px "PT Mono"', cursor: false });
        y += r.includes('\n') ? 150 : 100;
      }
      K.stamp(ctx, 'СЕКРЕТНО', t, 16.4, { x: 640, y: 1330, rot: -14, size: 104, letter: '6px' });
      ctx.restore();
    } },

    // 3 — childhood
    { a: 18.8, b: 27.4, fi: 0.5, fo: 0.8, draw(ctx, t, s) {
      paper(ctx);
      const sl = K.ease.outCubic(K.prog(t, 18.8, 19.9));
      K.print(ctx, I('boy'), W / 2, K.lerp(1700, 760, sl), 600, 800, -3, { clip: true, fy: 0.35 });
      K.type(ctx, '1920-е. отца забрали, когда костя был совсем маленьким.', t, 20.2,
        { x: 110, y: 1370, color: INK, shadow: false, maxW: 860 });
      K.type(ctx, 'он почти не помнил его.', t, 24.2, { x: 110, y: 1570, color: INK, shadow: false });
    } },
    { a: 26.8, b: 33.4, fi: 0.8, fo: 0.8, draw(ctx, t, s) {
      paper(ctx);
      const sl = K.ease.outCubic(K.prog(t, 26.8, 27.9));
      K.print(ctx, I('memories'), K.lerp(-400, W / 2, sl), 780, 640, 800, 2.5, { fy: 0.3, z: 1.05 });
      K.type(ctx, 'зато рядом были друзья. и мама.', t, 28.4, { x: 110, y: 1400, color: INK, shadow: false });
      K.type(ctx, 'с ними он смеялся.', t, 30.6, { x: 110, y: 1500, color: INK, shadow: false });
    } },

    // 4 — student
    { a: 32.8, b: 41.2, fi: 0.8, fo: 0.9, draw(ctx, t, s) {
      paper(ctx);
      const sl = K.ease.outCubic(K.prog(t, 32.8, 33.9));
      K.print(ctx, I('student'), W / 2 + 20, K.lerp(-600, 760, sl), 600, 800, 2, { clip: true, fy: 0.3 });
      K.type(ctx, 'медицинский институт.', t, 34.4, { x: 110, y: 1400, color: INK, shadow: false });
      K.type(ctx, 'до диплома оставался последний курс.', t, 36.2, { x: 110, y: 1500, color: INK, shadow: false, maxW: 880 });
    } },

    // 5 — the night before the escape
    { a: 40.6, b: 55.4, fi: 1.2, fo: 0.9, draw(ctx, t, s) {
      const fl = 0.75 + 0.25 * Math.sin(t * 9) * Math.sin(t * 3.7);
      ctx.filter = `sepia(0.4) saturate(0.5) brightness(${0.42 * fl + 0.12}) blur(1.5px)`;
      K.kenBurns(ctx, I('mother'), s.p, { z: 1.0, x: 0.62, y: 0.5 }, { z: 1.12, x: 0.66, y: 0.45 });
      ctx.filter = 'none';
      shade(ctx, 700, 0.9);
      const dl = [['— Мам, я дома!', 41.6], ['— Костя… надо торопиться.', 44.0], ['— Чекисты, сынок. Чекисты.', 46.4], ['— Как тогда. Как твоего отца.', 48.8]];
      const out = 1 - K.prog(t, 51.0, 51.6);
      dl.forEach(([l, t0], i) => {
        const a = K.prog(t, t0, t0 + 0.7) * out;
        if (a > 0) K.text(ctx, l, W / 2, 1180 + i * 100, { font: 'italic 54px "Old Standard TT"', alpha: a, color: i % 2 ? '#e8d9c0' : '#bfb2a0' });
      });
      K.type(ctx, 'он пел ей колыбельную.', t, 51.8, { y: 1420, align: 'center', x: W / 2 });
      K.type(ctx, 'голос дрожал.', t, 53.6, { y: 1520, align: 'center', x: W / 2, color: '#c9bfae' });
    } },

    // 6 — the train
    photo('train', 54.8, 64.2, { z: 1.0, x: 0.40, y: 0.5 }, { z: 1.06, x: 0.80, y: 0.42 }, [
      { s: 'он бежал из страны, которая забрала у него отца.', t: 56.4 },
      { s: 'а теперь — и мать.', t: 60.6, y: 1720, color: '#c9bfae' },
    ], { extra: (ctx, t) => K.snow(ctx, t, { n: 90, alpha: 0.5, size: 3 }) }),

    // 6b — the only letter
    { a: 63.8, b: 68.4, fi: 0.8, fo: 0.6, draw(ctx, t, s) {
      desk(ctx);
      const z = K.lerp(1.0, 1.08, s.p);
      ctx.save(); ctx.translate(W / 2, 800); ctx.scale(z, z); ctx.translate(-W / 2, -800);
      K.print(ctx, I('letter'), W / 2, 800, 860, 860 * 180 / 275, 4, { filter: 'sepia(0.5) contrast(1.15) brightness(1.1)' });
      ctx.restore();
      K.type(ctx, 'одно письмо.', t, 64.8, { y: 1420, align: 'center', x: W / 2 });
    } },
    photo('booth', 68.0, 72.6, { z: 1.05, x: 0.25, y: 0.55 }, { z: 1.15, x: 0.3, y: 0.5 }, [
      { s: 'больше он о ней ничего не слышал.', t: 68.8 },
    ]),

    // 7 — war
    photo('medic', 72.2, 77.6, { z: 1.0, x: 0.5, y: 0.45 }, { z: 1.12, x: 0.5, y: 0.32 }, [
      { s: 'война. он стал полевым хирургом.', t: 73.0 },
    ]),
    photo('carry', 77.2, 82.6, { z: 1.0, x: 0.5, y: 0.4 }, { z: 1.1, x: 0.5, y: 0.35 }, [
      { s: 'он выносил раненых — своих и чужих.', t: 77.9 },
    ], { extra: (ctx, t) => K.embers(ctx, t, { n: 40, alpha: 0.5 }) }),
    photo('scar', 82.2, 87.25, { z: 1.0, x: 0.5, y: 0.4 }, { z: 1.14, x: 0.5, y: 0.3 }, [
      { s: 'его пистолет всегда был разряжен.', t: 83.0 },
    ], { fo: 0.05 }),

    // 8 — explosion
    { a: 87.2, b: 92.6, fi: 0, fo: 0.8, draw(ctx, t, s) {
      const sh = K.shake(t, 87.2, 1.6, 60);
      const blur = 8 * (1 - K.prog(t, 87.2, 88.6));
      ctx.save(); ctx.translate(sh.x, sh.y);
      ctx.filter = `${GRADE} blur(${blur.toFixed(1)}px)`;
      K.cover(ctx, I('headdown'), { x: -60, y: -60, w: W + 120, h: H + 120 }, 1.1, 0.5, 0.35);
      ctx.filter = 'none'; ctx.restore();
      const fl = t < 87.32 ? K.prog(t, 87.2, 87.32) : 1 - K.ease.outQuad(K.prog(t, 87.32, 88.8));
      ctx.fillStyle = `rgba(255,250,240,${fl})`; ctx.fillRect(0, 0, W, H);
      ctx.fillStyle = `rgba(0,0,0,${K.prog(t, 88.4, 89.0)})`; ctx.fillRect(0, 0, W, H);
      K.type(ctx, 'он донёс солдата.', t, 89.3, { y: 900, align: 'center', x: W / 2 });
      K.type(ctx, 'и только потом упал.', t, 90.6, { y: 1000, align: 'center', x: W / 2, color: '#c9bfae' });
    } },

    // 9 — hospital
    photo('bath', 92.2, 100.2, { z: 1.0, x: 0.62, y: 0.4 }, { z: 1.12, x: 0.62, y: 0.32 }, [
      { s: 'он молчал неделями.', t: 93.4 },
      { s: 'и смотрел в потолок.', t: 95.8, y: 1660, color: '#c9bfae' },
    ], { extra: (ctx, t) => K.ecg(ctx, t, 92.6, 7, { y: 1250, alpha: 0.55, amp: 70 }) }),
    photo('mask', 99.8, 105.2, { z: 1.0, x: 0.5, y: 0.4 }, { z: 1.12, x: 0.5, y: 0.36 }, [
      { s: 'до сих пор он боится увидеть себя без маски.', t: 100.6 },
    ]),

    // 10 — rebels
    photo('rebel', 104.8, 110.2, { z: 1.0, x: 0.5, y: 0.4 }, { z: 1.1, x: 0.5, y: 0.35 }, [
      { s: 'после войны — повстанцы.', t: 105.6 },
      { s: '«мы за свободу, а не за смерть.»', t: 107.4, y: 1660, color: '#c9bfae' },
    ]),

    // 11 — final
    photo('walk', 109.8, 121.4, { z: 1.0, x: 0.5, y: 0.55 }, { z: 1.18, x: 0.5, y: 0.5 }, [
      { s: 'он спасает даже врагов.', t: 110.8, y: 1400 },
      { s: 'ведь где-то есть такой же ребёнок, каким был он —', t: 113.2, y: 1500 },
      { s: 'который ждёт домой отца.', t: 117.4, y: 1700, color: '#c9bfae' },
    ], { fo: 1.6 }),
    { a: 120.4, b: 129, fi: 1.8, fo: 2.0, draw(ctx, t, s) {
      ctx.filter = 'grayscale(1) brightness(0.3) blur(2px)';
      K.kenBurns(ctx, I('boy'), s.p, { z: 1.15, x: 0.5, y: 0.3 }, { z: 1.25, x: 0.5, y: 0.28 });
      ctx.filter = 'none';
      shade(ctx, 900, 0.9);
      const a = K.prog(t, 121.6, 123.4);
      K.text(ctx, 'КОНСТАНТИН', W / 2, 1380, { font: '118px "Old Standard TT"', letter: '16px', alpha: a, color: '#efe6d6' });
      K.type(ctx, 'спокойной ночи, костя.', t, 124.4, { y: 1500, align: 'center', x: W / 2, color: '#bfb2a0', font: '40px "PT Mono"' });
    } },
  ];

  window.VERSION = {
    duration: 129,
    images: {
      sketch: up('c_sketch_mama'), boy: img('boy-school.png'), memories: up('c_memories'), student: img('student.png'),
      mother: up('p_mother_room'), letter: up('p_letter'), train: img('train.png'), booth: img('phone-booth.png'),
      medic: img('medic-tent.png'), carry: up('c_carry'), scar: img('medic-scar.png'), headdown: up('c_head_down'),
      bath: img('bath.jpg'), mask: up('c_portrait_mask'), rebel: up('c_rebel_table'), walk: up('c_walk_away'),
    },
    render(ctx, t, frame) {
      K.runScenes(ctx, scenes, t);
      K.grain(ctx, frame, 0.11);
      K.scratches(ctx, frame, 0.8);
      K.vignette(ctx, 0.6);
      ctx.fillStyle = `rgba(0,0,0,${1 - K.flicker(frame)})`; ctx.fillRect(0, 0, W, H);
    },
  };
})();
