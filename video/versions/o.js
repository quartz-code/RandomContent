// Version O — «Гипс». A night chat with the doctor who brought him back (from the backstory: he came back to life through
// a doctor and modelling in plaster). «— снял?» «— снял.» «— посмотрел?» «— нет.» «— а лепить будешь?» «— уже.»
// Then his hands in plaster, a face growing on the beat; the jaw he lost, made last. In the silence after the song:
// «— на кого похоже?» — «(на меня. до)». Song: «Credits» (No, I'm Not a Human OST), 87 bpm, hard stop at 91.95.
// The arts are shown as they are (no filters).
(function () {
  const { W, H } = K;
  const I = n => K.images[n];
  const pr = (t, a, b) => K.prog(t, a, b);
  const es = (t, a, b) => K.ease.inOutSine(K.prog(t, a, b));
  const since = (list, t) => { let b = 1e9; for (const k of list) if (k <= t) b = Math.min(b, t - k); return b; };
  const SANS = 'Manrope', RED = '#e0242f';

  const BEAT = 0.6896, BAR = BEAT * 4, bar = n => 0.937 + BAR * n;
  const STOP = 91.95, END = 97.5;
  const KICKS = []; for (let n = 8; n < 33; n++) if (n !== 16) KICKS.push(bar(n), bar(n) + 2 * BEAT);
  const kick = t => Math.exp(-since(KICKS, t) * 8);

  // ================= the chat =================
  const MSG = [ // who: d = the doctor, k = Kostya
    { w: 'd', s: 'не спишь?', t: 1.2 }, { w: 'k', s: 'нет.', t: 3.6 },
    { w: 'd', s: 'снял?', t: 6.47 }, { w: 'k', s: 'снял.', t: 8.6 },
    { w: 'd', s: 'посмотрел?', t: bar(4) }, { w: 'k', s: 'нет.', t: bar(5) },
    { w: 'd', s: 'а лепить сегодня будешь?', t: bar(6) }, { w: 'k', s: 'уже.', t: bar(7) },
    { w: 'k', photo: 1, t: bar(7) + 1.4 },
    { w: 'd', s: 'покажи.', t: bar(16) + 0.1, scene: 2 },
    { w: 'k', photo: 2, t: bar(31), scene: 3 },
    { w: 'd', s: 'на кого похоже?', t: STOP + 0.9, scene: 4 }, { w: 'k', s: '(на меня. до)', t: STOP + 2.6, red: 1, scene: 4 },
  ];
  const bubble = (ctx, x, y, w, h, mine) => { ctx.beginPath(); ctx.roundRect(x, y, w, h, [34, 34, mine ? 8 : 34, mine ? 34 : 8]); };
  const chat = (ctx, t, frame, scene) => {
    ctx.fillStyle = '#0f1013'; ctx.fillRect(0, 0, W, H);
    ctx.save(); ctx.globalAlpha = 0.25; for (let y = 0; y < H; y += 512) for (let x = 0; x < W; x += 512) ctx.drawImage(K.noise[3], x, y); ctx.restore();
    ctx.fillStyle = '#17181c'; ctx.fillRect(0, 0, W, 210); ctx.fillStyle = 'rgba(255,255,255,0.06)'; ctx.fillRect(0, 210, W, 2);
    ctx.fillStyle = '#3a3f48'; ctx.beginPath(); ctx.arc(110, 120, 46, 0, 7); ctx.fill(); ctx.fillStyle = '#d9d4ca'; ctx.font = `600 42px ${SANS}`; ctx.textAlign = 'center'; ctx.textBaseline = 'middle'; ctx.fillText('Д', 110, 122);
    ctx.textAlign = 'left'; ctx.font = `600 50px ${SANS}`; ctx.fillStyle = '#ece8e0'; ctx.fillText('доктор', 180, 100);
    const typing = MSG.some(m => m.w === 'd' && (m.scene || 1) === scene && t > m.t - 1.2 && t < m.t);
    ctx.font = `400 36px ${SANS}`; ctx.fillStyle = typing ? '#8fb3c9' : '#7d7f86'; ctx.fillText(typing ? 'печатает…' : 'в сети', 180, 150);
    ctx.textAlign = 'right'; ctx.fillStyle = '#6f7178'; ctx.fillText(scene === 4 ? '03:58' : '02:47', W - 60, 120);
    // messages of this scene, newest at the bottom
    const list = MSG.filter(m => (m.scene || 1) <= scene && t >= m.t && (scene < 4 ? (m.scene || 1) !== 4 : true));
    const shown = list.slice(-7); let y = 1720;
    for (let i = shown.length - 1; i >= 0; i--) {
      const m = shown[i], mine = m.w === 'k', age = t - m.t, pop = K.ease.outBack(K.clamp(age / 0.25));
      ctx.font = `${m.red ? 600 : 400} ${m.red ? 66 : 58}px ${SANS}`;
      const tw = m.photo ? 560 : ctx.measureText(m.s).width + 76, th = m.photo ? 460 : (m.red ? 124 : 110);
      y -= th + 22;
      const x = mine ? W - 60 - tw : 60;
      ctx.save(); ctx.translate(x + (mine ? tw : 0), y + th); ctx.scale(pop, pop); ctx.translate(-(x + (mine ? tw : 0)), -(y + th));
      ctx.fillStyle = mine ? (m.red ? '#3a1416' : '#5b4230') : '#2b2e35'; bubble(ctx, x, y, tw, th, mine); ctx.fill();
      if (m.photo) {
        ctx.save(); bubble(ctx, x + 12, y + 12, tw - 24, th - 24, mine); ctx.clip();
        ctx.fillStyle = '#1a1714'; ctx.fillRect(x, y, tw, th);
        if (m.photo === 1) plaster(ctx, x + tw / 2, y + th / 2 + 20, 0.42, 0.08, 1, t); else plaster(ctx, x + tw / 2, y + th / 2 + 20, 0.42, 1, 1, t);
        const load = pr(age, 0, 1.1);
        if (load < 1) { ctx.fillStyle = 'rgba(0,0,0,0.55)'; ctx.fillRect(x, y, tw, th); ctx.strokeStyle = '#e8e4dc'; ctx.lineWidth = 8; ctx.beginPath(); ctx.arc(x + tw / 2, y + th / 2, 50, -Math.PI / 2, -Math.PI / 2 + load * 6.283); ctx.stroke(); }
        ctx.restore();
        if (age > 1.2) { ctx.font = `400 28px ${SANS}`; ctx.fillStyle = '#b9c8cf'; ctx.textAlign = 'right'; ctx.fillText('✓✓', x + tw - 20, y + th - 26); }
      } else { ctx.fillStyle = m.red ? '#ff5a5a' : '#ece8e0'; ctx.textAlign = 'left'; ctx.textBaseline = 'middle'; ctx.fillText(m.s, x + 35, y + th / 2 + 2); }
      ctx.restore();
    }
    if (typing) { const x = 60, yy = 1720 - 80; ctx.fillStyle = '#2b2e35'; bubble(ctx, x, yy, 150, 70, false); ctx.fill(); for (let k = 0; k < 3; k++) { ctx.fillStyle = `rgba(230,228,220,${0.35 + 0.6 * (Math.floor(t * 4) % 3 === k)})`; ctx.beginPath(); ctx.arc(x + 45 + k * 30, yy + 35, 9, 0, 7); ctx.fill(); } }
  };

  // ================= the plaster face =================
  // s: 0 = a lump, 0.85 = a face without the jaw, 1 = the whole face. Drawn in a 600x800 box at scale sc.
  const MARKS = (() => { const r = K.rng(21), a = []; for (let i = 0; i < 70; i++) a.push([r() * 2 - 1, r() * 2 - 1, r() * 6.28, 6 + r() * 16]); return a; })();
  const plaster = (ctx, cx, cy, sc, s, light, t) => {
    ctx.save(); ctx.translate(cx, cy); ctx.scale(sc, sc);
    const f = K.ease.inOutSine(K.clamp(s / 0.6)), jaw = K.clamp((s - 0.85) / 0.15);
    const L2 = (a, b, u) => [K.lerp(a[0], b[0], u), K.lerp(a[1], b[1], u)];
    // the block it stands in
    ctx.fillStyle = `rgb(${Math.round(170 * light)},${Math.round(163 * light)},${Math.round(152 * light)})`;
    ctx.beginPath(); for (let i = 0; i <= 24; i++) { const a = Math.PI + i / 24 * Math.PI, r = 1 + 0.06 * Math.sin(i * 2.7); ctx.lineTo(Math.cos(a) * 340 * r, 400 + Math.sin(a) * 110 * r); } ctx.lineTo(340, 470); ctx.lineTo(-340, 470); ctx.closePath(); ctx.fill();
    // the outline: lump → face; below the cheekbones it runs down into the block until the jaw is made
    const top = L2([0, -330], [0, -365], f), tem = L2([235, -260], [185, -300], f), fh = L2([262, -120], [214, -165], f), ck = L2([268, 40], [226, -15], f);
    const ang = L2([255, 260], [186, 190], f), low = L2(L2([240, 410], [240, 410], f), [96, 300], jaw), bot = L2([0, 425], [0, 332], jaw);
    ctx.beginPath(); ctx.moveTo(top[0], top[1]);
    ctx.bezierCurveTo(tem[0] * 0.55, top[1], tem[0], tem[1] + 10, fh[0], fh[1]);
    ctx.bezierCurveTo(fh[0] + 14, fh[1] + 60, ck[0] + 6, ck[1] - 60, ck[0], ck[1]);
    ctx.bezierCurveTo(ck[0] - 6, ck[1] + 90, ang[0] + 8, ang[1] - 70, ang[0], ang[1]);
    ctx.bezierCurveTo(ang[0] - 20, ang[1] + 50, low[0] + 30, low[1] - 10, low[0], low[1]);
    ctx.bezierCurveTo(low[0] - 40, low[1] + 20 * jaw, bot[0] + 50, bot[1], bot[0], bot[1]);
    ctx.bezierCurveTo(-bot[0] - 50, bot[1], -low[0] + 40, low[1] + 20 * jaw, -low[0], low[1]);
    ctx.bezierCurveTo(-low[0] - 30, low[1] - 10, -ang[0] + 20, ang[1] + 50, -ang[0], ang[1]);
    ctx.bezierCurveTo(-ang[0] - 8, ang[1] - 70, -ck[0] + 6, ck[1] + 90, -ck[0], ck[1]);
    ctx.bezierCurveTo(-ck[0] - 6, ck[1] - 60, -fh[0] - 14, fh[1] + 60, -fh[0], fh[1]);
    ctx.bezierCurveTo(-tem[0], tem[1] + 10, -tem[0] * 0.55, top[1], top[0], top[1]);
    ctx.closePath();
    const chinY = bot[1];
    const g = ctx.createRadialGradient(-120, -200, 40, 0, 0, 520); g.addColorStop(0, `rgb(${Math.round(246 * light)},${Math.round(240 * light)},${Math.round(230 * light)})`); g.addColorStop(0.7, `rgb(${Math.round(200 * light)},${Math.round(192 * light)},${Math.round(180 * light)})`); g.addColorStop(1, `rgb(${Math.round(120 * light)},${Math.round(114 * light)},${Math.round(106 * light)})`);
    ctx.fillStyle = g; ctx.fill();
    ctx.save(); ctx.clip();
    const sh = (x, y, rx, ry, a, rot = 0) => { ctx.fillStyle = `rgba(70,60,50,${a})`; ctx.beginPath(); ctx.ellipse(x, y, rx, ry, rot, 0, 7); ctx.fill(); };
    const hl = (x, y, rx, ry, a, rot = 0) => { ctx.fillStyle = `rgba(255,252,245,${a})`; ctx.beginPath(); ctx.ellipse(x, y, rx, ry, rot, 0, 7); ctx.fill(); };
    const k = K.clamp((s - 0.15) / 0.2), e = K.clamp((s - 0.3) / 0.2), n = K.clamp((s - 0.45) / 0.2), m = K.clamp((s - 0.6) / 0.2);
    if (k > 0) { sh(0, -150, 200, 34, 0.18 * k); hl(-60, -200, 120, 40, 0.18 * k); }                                         // the brow
    if (e > 0) { sh(-88, -105, 62, 30, 0.32 * e); sh(88, -105, 62, 30, 0.32 * e); ctx.strokeStyle = `rgba(60,50,42,${0.6 * e})`; ctx.lineWidth = 5; for (const sd of [-1, 1]) { ctx.beginPath(); ctx.moveTo(sd * 130, -100); ctx.quadraticCurveTo(sd * 88, -84, sd * 46, -100); ctx.stroke(); } } // closed eyes
    if (n > 0) { ctx.fillStyle = `rgba(70,60,50,${0.25 * n})`; ctx.beginPath(); ctx.moveTo(18, -95); ctx.quadraticCurveTo(40, 0, 44, 40); ctx.lineTo(10, 46); ctx.fill(); hl(-12, -20, 16, 70, 0.3 * n); sh(-22, 46, 14, 7, 0.45 * n); sh(22, 46, 14, 7, 0.45 * n); } // the nose
    if (m > 0) { ctx.strokeStyle = `rgba(70,55,45,${0.55 * m})`; ctx.lineWidth = 5; ctx.beginPath(); ctx.moveTo(-58, 118); ctx.quadraticCurveTo(0, 128, 58, 118); ctx.stroke(); hl(0, 100, 50, 10, 0.25 * m); sh(0, 140, 40, 10, 0.25 * m); } // the mouth
    if (s > 0.5) { const c = K.clamp((s - 0.5) / 0.2); sh(-150, 20, 50, 90, 0.12 * c, 0.3); sh(150, 20, 50, 90, 0.12 * c, -0.3); }
    if (jaw < 1) { const r = K.rng(31); for (let i = 0; i < 60; i++) { const x = (r() - 0.5) * 420, y = 170 + r() * 240; sh(x, y, 10 + r() * 26, 6 + r() * 14, 0.18 * (1 - jaw), r() * 3); hl(x - 6, y - 5, 8 + r() * 16, 4 + r() * 8, 0.15 * (1 - jaw), r() * 3); } }
    if (jaw > 0) { sh(0, chinY - 40, 80, 24, 0.2 * jaw); hl(-30, chinY - 70, 60, 20, 0.2 * jaw); sh(-150, 250, 60, 30, 0.12 * jaw, 0.6); sh(150, 250, 60, 30, 0.12 * jaw, -0.6); }
    for (const [mx, my, a, L] of MARKS) { const x = mx * 210, y = my * 330 - 30; if (y > 150 + 180 * jaw) continue; ctx.strokeStyle = 'rgba(90,80,70,0.18)'; ctx.lineWidth = 2; ctx.beginPath(); ctx.moveTo(x, y); ctx.lineTo(x + Math.cos(a) * L, y + Math.sin(a) * L); ctx.stroke(); }
    ctx.restore();
    ctx.restore();
  };
  // his hands, working: rim-lit by the lamp; pos 0..1 cycles their place on the face
  const hands = (ctx, t, cx, cy, sc, pos, press, still = false) => {
    const P = [[-0.95, 0.05], [-0.85, -0.45], [-0.9, 0.45], [-0.7, 0.85]]; // places along the left edge; the right hand mirrors
    const i = Math.floor(pos) % P.length, j = (i + 1) % P.length, u = still ? 0 : K.ease.inOutSine(pos % 1);
    for (const side of [-1, 1]) {
      const px = K.lerp(P[i][0], P[j][0], u) * -side, py = K.lerp(P[i][1], P[j][1], u);
      const x = cx + px * 250 * sc - side * 0, y = cy + py * 360 * sc;
      ctx.save(); ctx.translate(x, y); ctx.scale(sc * 0.9, sc * 0.9); ctx.rotate(side * -0.35 + side * 0.25 * py);
      ctx.translate(side * (40 + press * 8), 0);
      const skin = '#8a6650';
      ctx.fillStyle = skin; ctx.beginPath(); ctx.moveTo(side * 30, 40); ctx.lineTo(side * 150, 900); ctx.lineTo(side * 330, 900); ctx.lineTo(side * 120, 30); ctx.fill(); // the forearm, out and down
      ctx.beginPath(); ctx.ellipse(side * 70, -10, 70, 95, side * 0.2, 0, 7); ctx.fill();                                   // the palm against the plaster
      ctx.strokeStyle = skin; ctx.lineCap = 'round'; ctx.lineWidth = 28; for (let k = 0; k < 4; k++) { ctx.beginPath(); ctx.moveTo(side * (20 + k * 6), -60 + k * 30); ctx.lineTo(side * (-30 - (k === 0 ? 10 : 0)), -110 + k * 40); ctx.stroke(); } // fingers curving onto the face
      ctx.fillStyle = 'rgba(238,233,224,0.6)'; for (let k = 0; k < 4; k++) { ctx.beginPath(); ctx.arc(side * -30, -110 + k * 40, 13, 0, 7); ctx.fill(); }   // plaster on the fingertips
      ctx.strokeStyle = 'rgba(255,214,170,0.55)'; ctx.lineWidth = 4; ctx.beginPath(); ctx.moveTo(side * 330, 900); ctx.lineTo(side * 120, 30); ctx.stroke();
      ctx.restore();
    }
  };
  const bench = (ctx, t, L = 1) => {
    ctx.fillStyle = '#100c09'; ctx.fillRect(0, 0, W, H);
    const g = ctx.createRadialGradient(260, 200, 30, 260, 200, 1500); g.addColorStop(0, `rgba(255,200,140,${0.3 * L})`); g.addColorStop(1, 'rgba(0,0,0,0)'); ctx.fillStyle = g; ctx.fillRect(0, 0, W, H);
    ctx.fillStyle = '#2a1d14'; ctx.fillRect(0, 1360, W, 560); ctx.fillStyle = 'rgba(255,200,140,0.08)'; ctx.fillRect(0, 1360, W, 4);
    ctx.fillStyle = 'rgba(232,226,214,0.2)'; for (let i = 0; i < 40; i++) { const x = K.hash(i, 1) * W, y = 1400 + K.hash(i, 2) * 480; ctx.beginPath(); ctx.ellipse(x, y, 6 + K.hash(i, 3) * 20, 3 + K.hash(i, 4) * 6, 0, 0, 7); ctx.fill(); } // plaster crumbs
  };
  const dust = (ctx, t, x, y, k) => { if (k < 0.05) return; ctx.save(); const r = K.rng(Math.floor(t * 1.45)); for (let i = 0; i < 40; i++) { const a = r() * 6.28, d = (1 - k) * 200 * (0.5 + r()); ctx.fillStyle = `rgba(240,236,228,${0.6 * k})`; ctx.beginPath(); ctx.arc(x + Math.cos(a) * d, y + Math.sin(a) * d - (1 - k) * 60, 2 + r() * 4, 0, 7); ctx.fill(); } ctx.restore(); };
  const photo = (ctx, key, t, o = {}) => { ctx.fillStyle = '#000'; ctx.fillRect(0, 0, W, H); K.cover(ctx, I(key), { x: 0, y: 0, w: W, h: H }, o.z || 1.05, o.fx ?? 0.5, o.fy ?? 0.3); };
  const mirrorCovered = ctx => { ctx.fillStyle = '#1a1c1d'; ctx.fillRect(0, 0, W, H); ctx.fillStyle = '#0d0d0c'; ctx.fillRect(170, 250, 740, 960); ctx.fillStyle = '#cfc8ba'; ctx.beginPath(); ctx.moveTo(150, 220); ctx.lineTo(930, 230); ctx.quadraticCurveTo(960, 700, 920, 1240); ctx.lineTo(160, 1230); ctx.quadraticCurveTo(120, 700, 150, 220); ctx.fill(); ctx.strokeStyle = 'rgba(0,0,0,0.15)'; ctx.lineWidth = 5; for (let k = 1; k < 6; k++) { ctx.beginPath(); ctx.moveTo(150 + k * 130, 230); ctx.quadraticCurveTo(170 + k * 130, 700, 150 + k * 130, 1230); ctx.stroke(); } };
  const prosthesis = ctx => { ctx.fillStyle = '#16120f'; ctx.fillRect(0, 0, W, H); ctx.fillStyle = '#3a2c22'; ctx.beginPath(); ctx.ellipse(W / 2, 1000, 480, 260, 0, 0, 7); ctx.fill();
    ctx.fillStyle = '#d6c2ae'; ctx.beginPath(); ctx.moveTo(300, 900); ctx.quadraticCurveTo(540, 1180, 780, 900); ctx.quadraticCurveTo(700, 1050, 540, 1070); ctx.quadraticCurveTo(380, 1050, 300, 900); ctx.fill();
    ctx.strokeStyle = '#5a4a3a'; ctx.lineWidth = 6; ctx.beginPath(); ctx.moveTo(300, 900); ctx.quadraticCurveTo(200, 820, 150, 860); ctx.moveTo(780, 900); ctx.quadraticCurveTo(880, 820, 930, 860); ctx.stroke(); };

  // ================= scenes =================
  const INSERTS = [[bar(10) + 2 * BEAT, 'mirror'], [bar(12) + 2 * BEAT, 'prosthesis'], [bar(14) + 2 * BEAT, 'mirror']];
  const CLIMAX = ['plaster', 'student', 'plaster', 'mask'];
  const SCENES = [
    { a: 0, b: 23.0, draw(ctx, t, frame) { chat(ctx, t, frame, 1); } },
    { a: 23.0, b: bar(16), draw(ctx, t, frame) { // the hands and the plaster, on the beat
      const ins = INSERTS.find(q => t >= q[0] && t < q[0] + 0.4);
      if (ins) { if (ins[1] === 'mirror') mirrorCovered(ctx); else prosthesis(ctx); return; }
      const s = 0.06 + 0.79 * pr(t, 23.0, bar(16)), kk = kick(t), z = 1 + 0.02 * kk;
      bench(ctx, t);
      ctx.save(); ctx.translate(W / 2, 880); ctx.scale(z, z); ctx.translate(-W / 2, -880);
      plaster(ctx, W / 2, 880, 1.15, s, 1, t);
      hands(ctx, t, W / 2, 880, 1.15, (t - 23.0) / (BEAT * 2), kk);
      ctx.restore();
      dust(ctx, t, W / 2, 900, kk);
    } },
    { a: bar(16), b: bar(17), draw(ctx, t, frame) { chat(ctx, t, frame, 2); } },
    { a: bar(17), b: bar(25), draw(ctx, t, frame) { // the jaw — he stops on it, then makes it
      const kk = kick(t), stop = t >= bar(20) && t < bar(21);
      if (t >= bar(20) + BAR / 2 && t < bar(21)) { photo(ctx, 'mask', t, { z: 1.1 + 0.03 * pr(t, bar(20), bar(21)), fy: 0.35 }); ctx.fillStyle = 'rgba(0,0,0,0.25)'; ctx.fillRect(0, 0, W, H); return; }
      const s = t < bar(20) ? 0.85 : 0.85 + 0.15 * pr(t, bar(21), bar(24.5)), z = 1.15 + 0.25 * pr(t, bar(17), bar(25));
      bench(ctx, t);
      ctx.save(); ctx.translate(W / 2, 1050); ctx.scale(z / 1.15 * (1 + 0.02 * kk), z / 1.15 * (1 + 0.02 * kk)); ctx.translate(-W / 2, -1050);
      plaster(ctx, W / 2, 880, 1.15, s, 1, t);
      hands(ctx, t, W / 2, 880, 1.15, stop ? 3 : 2 + (t - bar(17)) / (BEAT * 4), stop ? 0 : kk, stop);
      ctx.restore();
      if (!stop) dust(ctx, t, W / 2, 1150, kk);
    } },
    { a: bar(25), b: bar(31), draw(ctx, t, frame) { // the flash: the plaster, him at eighteen, him now
      const step = bar(28) > t ? BEAT * 2 : BEAT, k = Math.floor((t - bar(25)) / step) % CLIMAX.length, what = CLIMAX[k], fl = Math.exp(-since(KICKS, t) * 14);
      if (what === 'plaster') { bench(ctx, t, 0.6); plaster(ctx, W / 2, 900, 1.2, 1, 0.85 + 0.25 * fl, t); }
      else if (what === 'student') photo(ctx, 'student', t, { z: 1.25, fy: 0.25 });
      else photo(ctx, 'mask', t, { z: 1.12, fy: 0.35 });
      ctx.fillStyle = `rgba(255,255,255,${0.55 * fl})`; ctx.fillRect(0, 0, W, H);
    } },
    { a: bar(31), b: STOP, draw(ctx, t, frame) { chat(ctx, t, frame, 3); } },
    { a: STOP, b: END, draw(ctx, t, frame) { chat(ctx, t, frame, 4); } },
  ];
  const CUTS = [23.0, bar(16), bar(17), bar(25), bar(31)];

  window.VERSION = {
    duration: END,
    images: { student: '/assets/images/student.png', mask: '/video/assets/up/c_portrait_mask.jpg' },
    render(ctx, t, frame) {
      const sc = SCENES.find(s => t >= s.a && t < s.b) || SCENES[SCENES.length - 1];
      ctx.save(); ctx.setTransform(1, 0, 0, 1, 0, 0); ctx.globalAlpha = 1; ctx.globalCompositeOperation = 'source-over'; ctx.filter = 'none';
      ctx.fillStyle = '#000'; ctx.fillRect(0, 0, W, H);
      sc.draw(ctx, t, frame);
      ctx.restore();
      if (since(CUTS, t) < 0.06) { ctx.fillStyle = 'rgba(255,255,255,0.15)'; ctx.fillRect(0, 0, W, H); }
      K.grain(ctx, frame, 0.05); K.vignette(ctx, 0.45);
      if (t < 0.5) { ctx.fillStyle = `rgba(0,0,0,${1 - t / 0.5})`; ctx.fillRect(0, 0, W, H); }
    },
  };
})();
