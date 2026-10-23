// Version N — «Эпитез». Night, a bathroom, a lamp that flickers, a mirror. The reflection — his face with the prosthesis —
// moves a moment late and talks to him: «— опять не смотришь?» He can look at anyone's wounds, not at his own (from the
// backstory: he is afraid to see himself without the prosthesis). The mirror cracks into what happened; in the end he
// throws a towel over it. Song: «Credits» (No, I'm Not a Human OST), 87 bpm, hard stop at 91.95.
(function () {
  const { W, H } = K;
  const I = n => K.images[n];
  const pr = (t, a, b) => K.prog(t, a, b);
  const es = (t, a, b) => K.ease.inOutSine(K.prog(t, a, b));
  const since = (list, t) => { let b = 1e9; for (const k of list) if (k <= t) b = Math.min(b, t - k); return b; };
  const MONO = '"PT Mono"', OSW = 'Oswald';
  const HIM = '#ece6da', MIR = '#a8c3d6', RED = '#e0242f';

  const BEAT = 0.6896, BAR = BEAT * 4, bar = n => 0.937 + BAR * n;
  const STOP = 91.95, END = 97.0;
  const KICKS = []; for (let n = 4; n < 33; n++) if (n !== 16) KICKS.push(bar(n), bar(n) + 2 * BEAT);
  const kick = t => Math.exp(-since(KICKS, t) * 8);

  const ARTS = ['face', 'tent', 'coat', 'carry', 'head'];
  const images = {};
  for (const a of ARTS) for (const l of ['tone', 'lines0', 'lines1', 'lines2', 'color']) images[`${a}-${l}`] = `/assets/images/kostya/drawn/${a}-${l}.png`;
  const art = (ctx, key, t, r, o = {}) => {
    const im = I(`${key}-tone`), s = Math.max(r.w / im.width, r.h / im.height) * (o.z || 1);
    const dw = im.width * s, dh = im.height * s, dx = r.x + (r.w - dw) * (o.fx ?? 0.5), dy = r.y + (r.h - dh) * (o.fy ?? 0.5);
    ctx.save(); ctx.globalAlpha = (o.alpha ?? 1); ctx.drawImage(im, dx, dy, dw, dh);
    ctx.drawImage(I(`${key}-lines${(Math.floor(t * 8) % 3 + 3) % 3}`), dx, dy, dw, dh); // t can be negative: the reflection is late
    ctx.globalAlpha = (o.alpha ?? 1) * (o.color ?? 1); ctx.drawImage(I(`${key}-color`), dx, dy, dw, dh);
    ctx.restore();
  };
  const sub = (ctx, s, t, t0, t1, o = {}) => {
    if (t < t0 || t > t1) return;
    const str = s.slice(0, Math.floor((t - t0) * (o.cps || 28))), a = 1 - pr(t, t1 - 0.3, t1);
    ctx.save(); ctx.font = `${o.size || 54}px ${MONO}`; ctx.textAlign = 'center'; ctx.globalAlpha = a; ctx.lineWidth = 8; ctx.strokeStyle = 'rgba(0,0,0,0.85)'; ctx.lineJoin = 'round';
    str.split('\n').forEach((l, i) => { const y = (o.y || 1700) + i * (o.size || 54) * 1.25, jx = o.glitch ? (K.hash(i, Math.floor(t * 20)) - 0.5) * o.glitch : 0; ctx.strokeText(l, W / 2 + jx, y); ctx.fillStyle = o.color || HIM; ctx.fillText(l, W / 2 + jx, y); });
    ctx.restore();
  };

  // ================= the bathroom =================
  const lamp = t => { // the bulb: warm, unsteady; in the end it stutters
    const fast = pr(t, bar(25), STOP), r = K.hash(Math.floor(t * (10 + 30 * fast)), 5);
    if (t < 1.0) return 0;
    if (t < 3.0) return K.hash(Math.floor(t * 14), 2) > 0.5 ? 0.9 : 0.1;
    if (t >= STOP) return 0;
    return r > 0.95 - 0.3 * fast ? 0.12 : 0.85 + 0.15 * Math.sin(t * 50);
  };
  const MR = { x: 170, y: 250, w: 740, h: 960 };
  const tiles = (ctx, L, cold = false) => {
    ctx.fillStyle = cold ? '#1a2024' : '#26231e'; ctx.fillRect(0, 0, W, H);
    ctx.strokeStyle = 'rgba(0,0,0,0.45)'; ctx.lineWidth = 4; for (let y = 0; y < H; y += 110) { ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(W, y); ctx.stroke(); } for (let x = 0; x < W; x += 110) { ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, H); ctx.stroke(); }
    const g = ctx.createRadialGradient(W / 2, 120, 10, W / 2, 120, 1300); g.addColorStop(0, `rgba(255,220,170,${0.45 * L})`); g.addColorStop(1, 'rgba(0,0,0,0)');
    ctx.save(); ctx.globalCompositeOperation = 'lighter'; ctx.fillStyle = g; ctx.fillRect(0, 0, W, H); ctx.restore();
    ctx.fillStyle = `rgba(0,0,0,${0.85 - 0.75 * L})`; ctx.fillRect(0, 0, W, H);
  };
  // what is in the mirror: the room reversed and him, a little late
  const reflection = (ctx, t, lag, o = {}) => {
    ctx.save(); ctx.beginPath(); ctx.rect(MR.x, MR.y, MR.w, MR.h); ctx.clip();
    const tl = t - lag, L = lamp(tl);
    tiles(ctx, L, true);
    const sway = Math.sin(tl * 0.9) * 22 + (o.lean || 0);
    art(ctx, 'face', tl, { x: MR.x + 40 - sway, y: MR.y + 120, w: MR.w - 80, h: MR.h - 60 }, { fy: 0.3, z: 1.05 + (o.z || 0), alpha: 0.35 + 0.65 * L });
    ctx.fillStyle = `rgba(80,110,140,${0.12})`; ctx.fillRect(MR.x, MR.y, MR.w, MR.h);
    const gl = ctx.createLinearGradient(MR.x, MR.y, MR.x + MR.w, MR.y + MR.h); gl.addColorStop(0, 'rgba(255,255,255,0.08)'); gl.addColorStop(0.4, 'rgba(255,255,255,0)'); ctx.fillStyle = gl; ctx.fillRect(MR.x, MR.y, MR.w, MR.h);
    ctx.restore();
  };
  const room = (ctx, t, o = {}) => {
    const L = lamp(t);
    tiles(ctx, L);
    // the mirror frame, the shelf: a cup, the little case for the prosthesis; the sink below
    ctx.fillStyle = '#0e0d0b'; ctx.fillRect(MR.x - 26, MR.y - 26, MR.w + 52, MR.h + 52);
    if (o.reflect !== false) reflection(ctx, t, o.lag ?? 0.08, o);
    ctx.fillStyle = '#3a3630'; ctx.fillRect(MR.x - 60, MR.y + MR.h + 40, MR.w + 120, 22);
    ctx.fillStyle = '#2b2824'; ctx.fillRect(MR.x + 40, MR.y + MR.h - 20, 70, 60); ctx.fillStyle = '#1d1a17'; ctx.beginPath(); ctx.roundRect(MR.x + MR.w - 190, MR.y + MR.h + 5, 140, 36, 8); ctx.fill();
    ctx.fillStyle = '#8a8478'; ctx.fillRect(MR.x - 80, MR.y + MR.h + 70, MR.w + 160, 18); ctx.fillStyle = '#2a2723'; ctx.fillRect(MR.x - 80, MR.y + MR.h + 88, MR.w + 160, 60); // the edge of the sink counter
    ctx.fillStyle = `rgba(0,0,0,${0.8 - 0.7 * L})`; ctx.fillRect(MR.x - 60, MR.y + MR.h + 30, MR.w + 120, 600);
    // him, from behind: the back of the head and the shoulders, dark against the mirror
    const sway = Math.sin(t * 0.9) * 22 + (o.lean || 0);
    ctx.save(); ctx.translate(W / 2 + sway, 1660); ctx.fillStyle = '#060504';
    ctx.beginPath(); ctx.ellipse(0, -190, 120, 150, 0, 0, 7); ctx.fill();
    ctx.beginPath(); ctx.moveTo(-90, -70); ctx.quadraticCurveTo(-420, -40, -520, 400); ctx.lineTo(520, 400); ctx.quadraticCurveTo(420, -40, 90, -70); ctx.fill();
    ctx.strokeStyle = `rgba(255,220,170,${0.35 * L})`; ctx.lineWidth = 3; ctx.beginPath(); ctx.ellipse(0, -190, 120, 150, 0, 3.6, 5.8); ctx.stroke();
    if (o.hands) { // his hands come up towards the jaw
      const h = o.hands; ctx.fillStyle = '#0b0807';
      for (const sd of [-1, 1]) { ctx.save(); ctx.translate(sd * (240 - 90 * h), 300 - 380 * h); ctx.rotate(sd * (0.6 - 0.4 * h)); ctx.beginPath(); ctx.roundRect(-34, -60, 68, 120, 26); ctx.fill(); ctx.fillRect(-26, 40, 52, 500);
        ctx.strokeStyle = `rgba(255,214,160,${0.55 * L})`; ctx.lineWidth = 3; ctx.beginPath(); ctx.moveTo(-26 * sd, 540); ctx.lineTo(-26 * sd, 40); ctx.arcTo(-34 * sd, -60, 0, -60, 26); ctx.stroke(); ctx.restore(); }
    }
    ctx.restore();
  };
  // a shard of the cracked mirror: a triangle that shows something else
  const SH = (() => { const r = K.rng(7), cx = W / 2, cy = 760, a = []; let ang = 0; while (ang < Math.PI * 2 - 0.2) { const d = 0.4 + r() * 0.7; a.push([ang, ang + d]); ang += d; } return a.map(([a0, a1]) => ({ a0, a1, r: 900, cx, cy, dx: (r() - 0.5) * 30, dy: (r() - 0.5) * 30 })); })();
  const shards = (ctx, t, draw, open = 1) => {
    SH.forEach((s, i) => {
      ctx.save(); ctx.beginPath(); ctx.moveTo(s.cx, s.cy); ctx.lineTo(s.cx + Math.cos(s.a0) * s.r, s.cy + Math.sin(s.a0) * s.r); ctx.lineTo(s.cx + Math.cos(s.a1) * s.r, s.cy + Math.sin(s.a1) * s.r); ctx.closePath(); ctx.clip();
      ctx.translate(s.dx * open, s.dy * open); draw(ctx, i); ctx.restore();
      ctx.strokeStyle = 'rgba(230,240,255,0.55)'; ctx.lineWidth = 2; ctx.beginPath(); ctx.moveTo(s.cx, s.cy); ctx.lineTo(s.cx + Math.cos(s.a0) * s.r, s.cy + Math.sin(s.a0) * s.r); ctx.stroke();
    });
  };
  const vhs = (ctx, frame, a) => { // tracking noise and colour split on a memory
    ctx.save(); ctx.globalCompositeOperation = 'screen'; ctx.globalAlpha = 0.18 * a; ctx.drawImage(ctx.canvas, 10, 0); ctx.restore();
    for (let k = 0; k < 3; k++) { const y = (K.hash(k, Math.floor(frame / 2)) * H) | 0, h = 6 + K.hash(k, frame) * 30; ctx.drawImage(ctx.canvas, 0, y, W, h, (K.hash(k, frame + 3) - 0.5) * 60 * a, y, W, h); }
    ctx.fillStyle = 'rgba(0,0,0,0.18)'; for (let y = 0; y < H; y += 5) ctx.fillRect(0, y, W, 2);
  };
  // the memories, one per bar after the crack
  const MEM = [
    [8, 'взрыв.'], [9, 'бирка. две красные полосы.'], [10, 'лицом вниз.\nчтобы не задохнуться.'], [11, 'морфий.\nвремя укола записано.'],
    [12, 'потолок.'], [13, 'говорить не мог.'], [14, 'гипс. руки помнят.'], [15, 'эпитез.'],
  ];
  const memory = (ctx, t, i, frame) => {
    ctx.fillStyle = '#0b0a09'; ctx.fillRect(0, 0, W, H);
    const d = t - bar(MEM[i][0]);
    if (i === 0) { art(ctx, 'carry', t, { x: 0, y: 200, w: W, h: 1300 }, { fy: 0.3 }); const f = Math.exp(-d * 3); ctx.fillStyle = `rgba(255,240,220,${0.8 * f})`; ctx.fillRect(0, 0, W, H); }
    if (i === 1) { ctx.save(); ctx.translate(W / 2, 760); ctx.rotate(-0.12 + 0.03 * Math.sin(t)); ctx.fillStyle = '#d9cfb6'; ctx.fillRect(-230, -150, 460, 300); ctx.fillStyle = '#b01818'; ctx.fillRect(-230, -90, 460, 34); ctx.fillRect(-230, -30, 460, 34); ctx.fillStyle = '#3a3026'; ctx.beginPath(); ctx.arc(-190, -110, 14, 0, 7); ctx.fill(); ctx.font = `italic 44px "Caveat"`; ctx.fillText('03:41', -40, 90); ctx.restore(); }
    if (i === 2) art(ctx, 'head', t, { x: 0, y: 200, w: W, h: 1300 }, { fy: 0.2 });
    if (i === 3) { ctx.save(); ctx.translate(W / 2, 760); ctx.rotate(0.2); ctx.fillStyle = 'rgba(220,235,240,0.8)'; ctx.fillRect(-26, -150, 52, 300); ctx.fillStyle = 'rgba(200,120,60,0.7)'; ctx.fillRect(-26, 0, 52, 150); ctx.restore(); ctx.save(); ctx.translate(W / 2, 1050); ctx.rotate(-0.05); ctx.fillStyle = '#e9e1d3'; ctx.font = `64px "Caveat"`; ctx.textAlign = 'center'; ctx.fillText('морфий — 03:41', 0, 0); ctx.restore(); }
    if (i === 4) { ctx.fillStyle = '#c9c3b6'; ctx.fillRect(0, 0, W, H); ctx.strokeStyle = 'rgba(60,55,50,0.6)'; ctx.lineWidth = 3; const r = K.rng(4); for (let k = 0; k < 9; k++) { ctx.beginPath(); let x = r() * W, y = r() * 900; ctx.moveTo(x, y); for (let j = 0; j < 8; j++) { x += (r() - 0.5) * 160; y += r() * 90; ctx.lineTo(x, y); } ctx.stroke(); }
      ctx.fillStyle = 'rgba(0,0,0,0.5)'; ctx.fillRect(0, 0, W, H); const dd = Math.floor(1 + 299 * es(t, bar(12), bar(13) - 0.3)); ctx.fillStyle = '#e9e1d3'; ctx.font = `56px ${MONO}`; ctx.textAlign = 'center'; ctx.fillText(`день ${dd}`, W / 2, 1060); }
    if (i === 5) { art(ctx, 'face', t, { x: 0, y: 200, w: W, h: 1300 }, { fy: 0.3, z: 1.6 }); ctx.fillStyle = 'rgba(0,0,0,0.6)'; ctx.fillRect(0, 900, W, 300); }
    if (i === 6) { // hands shaping white plaster
      ctx.fillStyle = '#e8e2d6'; ctx.beginPath(); const sq = 1 + 0.08 * Math.sin(t * 3); ctx.ellipse(W / 2, 820, 170 * sq, 210 / sq, 0, 0, 7); ctx.fill(); ctx.fillStyle = 'rgba(0,0,0,0.12)'; ctx.beginPath(); ctx.ellipse(W / 2 + 40, 860, 60, 30, 0.3, 0, 7); ctx.fill();
      ctx.fillStyle = '#2a2019'; for (const sd of [-1, 1]) { ctx.save(); ctx.translate(W / 2 + sd * (230 - 20 * Math.sin(t * 3)), 820); ctx.rotate(sd * 0.3); ctx.beginPath(); ctx.roundRect(-40, -80, 80, 160, 30); ctx.fill(); ctx.fillRect(-30 + sd * 10, 60, 60, 700); ctx.restore(); } }
    if (i === 7) { art(ctx, 'face', t, { x: 0, y: 200, w: W, h: 1300 }, { fy: 0.3, z: 1.15 }); ctx.save(); ctx.globalCompositeOperation = 'lighter'; ctx.strokeStyle = `rgba(255,200,140,${0.5 + 0.3 * Math.sin(t * 6)})`; ctx.lineWidth = 5; ctx.beginPath(); ctx.moveTo(300, 1000); ctx.quadraticCurveTo(540, 1180, 800, 1000); ctx.stroke(); ctx.restore(); }
    vhs(ctx, frame, 1);
  };
  const OTHERS = [[18, 'coat', 'чужие раны —'], [19, 'carry', 'каждый день.'], [21, 'tent', 'смотрел.\nне отводил глаз.'], [23, 'coat', 'свою —']];

  // ================= scenes =================
  const SCENES = [
    { a: 0, b: 7.4, draw(ctx, t, frame) { room(ctx, t, { lag: 0.08 }); } },
    { a: 7.4, b: 23.0, draw(ctx, t, frame) { // the reflection talks; it is a little late
      const z = es(t, 20.3, 23.0) * 0.25;
      ctx.save(); ctx.translate(W / 2, 700); ctx.scale(1 + z * 0.6, 1 + z * 0.6); ctx.translate(-W / 2, -700);
      room(ctx, t, { lag: 0.12 + 0.2 * pr(t, 20.3, 23), z }); ctx.restore();
      sub(ctx, '— опять не смотришь?', t, 7.6, 10.0, { color: MIR });
      sub(ctx, '— нет.', t, 10.2, 11.9);
      sub(ctx, '— сними.', t, 12.1, 14.6, { color: MIR });
      sub(ctx, '— не буду.', t, 14.8, 17.3);
      sub(ctx, '— ты же врач.\nпосмотри на рану.', t, 17.5, 20.2, { color: MIR });
    } },
    { a: 23.0, b: bar(16), draw(ctx, t, frame) { // the crack: what happened, a shard at a time
      const i = Math.min(MEM.length - 1, Math.floor((t - bar(8)) / BAR)), d = t - bar(MEM[i][0]), kk = kick(t);
      ctx.save(); ctx.translate(W / 2, H / 2); ctx.scale(1 + 0.03 * kk, 1 + 0.03 * kk); ctx.translate(-W / 2, -H / 2);
      shards(ctx, t, (x, k) => { if (k % 3 === 0 && d > 0.4) room(x, t, { lag: 0.3 }); else memory(x, t, i, frame); }, 1);
      ctx.restore();
      sub(ctx, MEM[i][1], t, bar(MEM[i][0]) + 0.1, bar(MEM[i][0]) + BAR, { y: 1720, size: 58, cps: 40 });
      if (t - 23.0 < 0.12) { ctx.fillStyle = 'rgba(230,240,255,0.8)'; ctx.fillRect(0, 0, W, H); }
    } },
    { a: bar(16), b: bar(17), draw(ctx, t, frame) { // the shards close back into a mirror
      const c = es(t, bar(16), bar(16) + 1.2);
      shards(ctx, t, x => room(x, t, { lag: 0.25 }), 1 - c);
      sub(ctx, '— теперь смотри.', t, bar(16) + 0.15, bar(17), { color: MIR });
    } },
    { a: bar(17), b: bar(25), draw(ctx, t, frame) { // his hands come up — and every wound he did look at
      const o = OTHERS.find(q => t >= bar(q[0]) && t < bar(q[0] + 1)), kk = kick(t);
      if (o) { ctx.fillStyle = '#0a0908'; ctx.fillRect(0, 0, W, H); art(ctx, o[1], t, { x: 0, y: 220, w: W, h: 1400 }, { fy: 0.3, z: 1.04 + 0.05 * pr(t, bar(o[0]), bar(o[0] + 1)) }); vhs(ctx, frame, 0.6); sub(ctx, o[2], t, bar(o[0]) + 0.1, bar(o[0] + 1), { y: 1650, size: 62, cps: 36 }); }
      else { const h = es(t, bar(17), bar(25)); ctx.save(); ctx.translate(W / 2, 700); ctx.scale(1 + 0.15 * h + 0.02 * kk, 1 + 0.15 * h + 0.02 * kk); ctx.translate(-W / 2, -700); room(ctx, t, { lag: 0.25 + 0.15 * h, hands: 0.3 + 0.7 * h }); ctx.restore(); }
    } },
    { a: bar(25), b: STOP, draw(ctx, t, frame) { // the lamp stutters, the reflection falls further behind; the towel
      const p = pr(t, bar(25), STOP), kk = kick(t), towel = es(t, STOP - 0.45, STOP);
      ctx.save(); ctx.translate(W / 2, 700); const z = 1.15 + 0.25 * p + 0.03 * kk; ctx.scale(z, z); ctx.translate(-W / 2, -700);
      room(ctx, t, { lag: 0.4 + 1.0 * p, hands: 1 - 0.3 * towel, lean: Math.sin(t * 2) * 20 * p });
      if (towel > 0) { ctx.fillStyle = '#d9d2c4'; ctx.beginPath(); ctx.moveTo(MR.x - 60, MR.y - 80 + (1 - towel) * -900); ctx.lineTo(MR.x + MR.w + 60, MR.y - 60 + (1 - towel) * -900); ctx.quadraticCurveTo(MR.x + MR.w + 90, MR.y + MR.h * towel, MR.x + MR.w + 40, MR.y + MR.h * 1.05 * towel); ctx.lineTo(MR.x - 40, MR.y + MR.h * 1.02 * towel); ctx.quadraticCurveTo(MR.x - 90, MR.y + MR.h * 0.5 * towel, MR.x - 60, MR.y - 80 + (1 - towel) * -900); ctx.fill();
        ctx.strokeStyle = 'rgba(0,0,0,0.15)'; ctx.lineWidth = 4; for (let k = 1; k < 6; k++) { ctx.beginPath(); ctx.moveTo(MR.x + k * MR.w / 6, MR.y - 60); ctx.quadraticCurveTo(MR.x + k * MR.w / 6 + 20, MR.y + MR.h * 0.5 * towel, MR.x + k * MR.w / 6, MR.y + MR.h * towel); ctx.stroke(); } }
      ctx.restore();
      if (p > 0.5 && K.hash(Math.floor(t * 10), 9) > 0.75) vhs(ctx, frame, 0.5);
    } },
    { a: STOP, b: END, draw(ctx, t) { ctx.fillStyle = '#000'; ctx.fillRect(0, 0, W, H); sub(ctx, '(сегодня тоже нет)', t, STOP + 0.8, END - 0.1, { y: 960, size: 74, color: RED, cps: 20 }); } },
  ];
  const CUTS = [23.0, bar(16), bar(17), ...OTHERS.flatMap(o => [bar(o[0]), bar(o[0] + 1)]), bar(25)];

  window.VERSION = {
    duration: END,
    images,
    render(ctx, t, frame) {
      const sc = SCENES.find(s => t >= s.a && t < s.b) || SCENES[SCENES.length - 1];
      ctx.save(); ctx.setTransform(1, 0, 0, 1, 0, 0); ctx.globalAlpha = 1; ctx.globalCompositeOperation = 'source-over'; ctx.filter = 'none';
      ctx.fillStyle = '#000'; ctx.fillRect(0, 0, W, H);
      sc.draw(ctx, t, frame);
      ctx.restore();
      if (since(CUTS, t) < 0.06 && t < STOP) { ctx.fillStyle = 'rgba(255,255,255,0.15)'; ctx.fillRect(0, 0, W, H); }
      if (t < STOP + 0.05) { K.grain(ctx, frame, 0.12); K.vignette(ctx, 0.65); }
      if (t < 0.6) { ctx.fillStyle = `rgba(0,0,0,${1 - t / 0.6})`; ctx.fillRect(0, 0, W, H); }
    },
  };
})();
