// Version M — «Глазок». Night, a knock, the view through a door peephole. A man with a medical bag and a scarf over his
// face: «— кто там?» «— врач.» He fails every sign of a human; he is sent away — and the drop shows every door he himself
// opened. In the end the door opens: nobody there, only bandages on the mat (from the backstory: when his help was
// refused, he left the bandages and went). Song: «Credits» (No, I'm Not a Human OST), 87 bpm, hard stop at 91.95.
(function () {
  const { W, H } = K;
  const I = n => K.images[n];
  const pr = (t, a, b) => K.prog(t, a, b);
  const es = (t, a, b) => K.ease.inOutSine(K.prog(t, a, b));
  const win = (t, a, b) => t >= a && t < b;
  const since = (list, t) => { let b = 1e9; for (const k of list) if (k <= t) b = Math.min(b, t - k); return b; };
  const MONO = '"PT Mono"', OSW = 'Oswald';
  const IN = '#ece6da', HIM = '#a9c6cc', RED = '#e0242f', GREEN = '#7fd18b';

  // ---- the song's grid: 87 bpm, bars from 0.937; kicks on beats 1 and 3
  const BEAT = 0.6896, BAR = BEAT * 4, bar = n => 0.937 + BAR * n;
  const STOP = 91.95, END = 97.5;
  const KICKS = []; for (let n = 4; n < 33; n++) if (n !== 16) KICKS.push(bar(n), bar(n) + 2 * BEAT);
  const kick = t => Math.exp(-since(KICKS, t) * 8);
  const KNOCKS = [1.0, 1.3, 1.6, 6.47, 6.77, 7.07];

  // ================= the arts, redrawn in ink (tools/drawn.py photo) =================
  const ARTS = ['door', 'face', 'tent', 'coat', 'carry', 'head', 'walk'];
  const images = {};
  for (const a of ARTS) for (const l of ['tone', 'lines0', 'lines1', 'lines2', 'color']) images[`${a}-${l}`] = `/assets/images/kostya/drawn/${a}-${l}.png`;
  let OFF = null; const off = () => OFF || (OFF = K.makeCanvas(W, H));
  const art = (ctx, key, t, r, o = {}) => {
    const im = I(`${key}-tone`), s = Math.max(r.w / im.width, r.h / im.height) * (o.z || 1);
    const dw = im.width * s, dh = im.height * s, dx = r.x + (r.w - dw) * (o.fx ?? 0.5), dy = r.y + (r.h - dh) * (o.fy ?? 0.5);
    ctx.save(); ctx.globalAlpha = (o.alpha ?? 1) * (o.tone ?? 1); ctx.drawImage(im, dx, dy, dw, dh);
    ctx.globalAlpha = (o.alpha ?? 1) * (o.lines ?? 1); ctx.drawImage(I(`${key}-lines${Math.floor(t * 8) % 3}`), dx, dy, dw, dh);
    ctx.globalAlpha = (o.alpha ?? 1) * (o.color ?? 1); ctx.drawImage(I(`${key}-color`), dx, dy, dw, dh);
    ctx.restore();
    return { dx, dy, dw, dh };
  };

  // ================= text =================
  const sub = (ctx, s, t, t0, t1, o = {}) => { // a subtitle that types in and fades
    if (t < t0 || t > t1) return;
    const n = Math.floor((t - t0) * (o.cps || 28)), str = s.slice(0, n), a = 1 - pr(t, t1 - 0.3, t1);
    ctx.save(); ctx.font = `${o.bold ? 'bold ' : ''}${o.size || 54}px ${o.font || MONO}`; ctx.textAlign = o.align || 'center'; ctx.globalAlpha = a;
    ctx.lineWidth = 8; ctx.strokeStyle = 'rgba(0,0,0,0.85)'; ctx.lineJoin = 'round';
    const lines = str.split('\n');
    lines.forEach((l, i) => { const y = (o.y || 1700) + i * (o.size || 54) * 1.25; ctx.strokeText(l, o.x ?? W / 2, y); ctx.fillStyle = o.color || IN; ctx.fillText(l, o.x ?? W / 2, y); });
    ctx.restore();
  };
  const stamp = (ctx, s, t, t0, o = {}) => {
    const d = t - t0; if (d < 0 || d > (o.dur ?? 99)) return;
    const sc = 1 + 0.5 * Math.exp(-d * 16), a = Math.min(1, d * 18) * (1 - pr(d, (o.dur ?? 99) - 0.25, o.dur ?? 99));
    ctx.save(); ctx.translate(o.x ?? W / 2, o.y ?? 960); ctx.rotate(o.rot ?? -0.08); ctx.scale(sc, sc); ctx.globalAlpha = a;
    ctx.font = `bold ${o.size || 150}px ${OSW}`; ctx.textAlign = 'center'; ctx.textBaseline = 'middle'; ctx.letterSpacing = '4px';
    const w = ctx.measureText(s).width;
    ctx.strokeStyle = o.color || RED; ctx.lineWidth = 10; ctx.strokeRect(-w / 2 - 30, -(o.size || 150) * 0.62, w + 60, (o.size || 150) * 1.24);
    ctx.fillStyle = o.color || RED; ctx.fillText(s, 0, 4);
    ctx.restore();
  };

  // ================= places =================
  // the stairwell outside, as the peephole sees it: green-grey walls, the neighbour's door, a bulb that flickers
  const bulb = t => { const f = K.hash(Math.floor(t * 12), 7); return f > 0.93 ? 0.15 : f > 0.85 ? 0.55 : 0.95 + 0.05 * Math.sin(t * 40); };
  const stairwell = (ctx, t, o = {}) => {
    const b = (o.light ?? 1) * bulb(t);
    const g = ctx.createLinearGradient(0, 0, 0, H); g.addColorStop(0, '#2b3029'); g.addColorStop(0.55, '#1d211c'); g.addColorStop(1, '#0d0f0c'); ctx.fillStyle = g; ctx.fillRect(0, 0, W, H);
    const pl = ctx.createLinearGradient(0, 900, 0, 1100); pl.addColorStop(0, 'rgba(70,80,64,0)'); pl.addColorStop(0.5, 'rgba(70,80,64,0.35)'); pl.addColorStop(1, 'rgba(70,80,64,0)'); ctx.fillStyle = pl; ctx.fillRect(0, 900, W, 200); // old paint, soft
    ctx.save(); ctx.globalAlpha = 0.35; for (let y = 0; y < H; y += 512) for (let x = 0; x < W; x += 512) ctx.drawImage(K.noise[2], x, y); ctx.restore();
    ctx.fillStyle = '#141712'; ctx.fillRect(80, 360, 230, 1100); ctx.fillStyle = '#0a0b09'; ctx.fillRect(250, 900, 24, 60); // a neighbour's door
    ctx.strokeStyle = '#0b0c0a'; ctx.lineWidth = 14; ctx.beginPath(); ctx.moveTo(760, 1900); ctx.lineTo(1080, 1250); ctx.stroke(); // the rail going down
    ctx.fillStyle = '#0b0c0a'; ctx.fillRect(W / 2 - 3, 0, 6, 190); ctx.beginPath(); ctx.arc(W / 2, 205, 22, 0, 7); ctx.fill();
    const l = ctx.createRadialGradient(W / 2, 215, 4, W / 2, 215, 900); l.addColorStop(0, `rgba(255,214,150,${0.9 * b})`); l.addColorStop(0.08, `rgba(255,200,130,${0.45 * b})`); l.addColorStop(1, 'rgba(255,190,120,0)');
    ctx.save(); ctx.globalCompositeOperation = 'lighter'; ctx.fillStyle = l; ctx.fillRect(0, 0, W, H); ctx.restore();
    if (b < 0.3) { ctx.fillStyle = `rgba(0,0,0,${0.6 - b})`; ctx.fillRect(0, 0, W, H); }
  };
  // the man at the door: drawn, with a dark knitted scarf over the lower face that he pulls down
  const man = (ctx, t, o = {}) => {
    const r = { x: 150, y: 330, w: 780, h: 1350 };
    const box = art(ctx, 'door', t, r, { fy: 0, z: o.z || 1, alpha: o.alpha ?? 1 });
    const sc = o.scarf ?? 1; // 1 = up over the mouth, 0 = down on the chest
    if (sc > 0.01) {
      const fx = box.dx + box.dw * 0.33, fy = box.dy + box.dh * (0.25 + (1 - sc) * 0.13), sw = box.dw * 0.2, sh = box.dh * 0.06;
      ctx.save();
      const g = ctx.createLinearGradient(fx - sw, 0, fx + sw, 0); g.addColorStop(0, '#2a211b'); g.addColorStop(0.45, '#4a3b30'); g.addColorStop(1, '#231c17');
      ctx.fillStyle = g;
      ctx.beginPath(); ctx.moveTo(fx - sw, fy + sh * 0.2); ctx.bezierCurveTo(fx - sw * 0.6, fy - sh * 0.5 * sc, fx + sw * 0.6, fy - sh * 0.5 * sc, fx + sw, fy + sh * 0.2);
      ctx.bezierCurveTo(fx + sw * 1.05, fy + sh * 1.3, fx + sw * 0.5, fy + sh * 1.9, fx, fy + sh * 1.9); ctx.bezierCurveTo(fx - sw * 0.5, fy + sh * 1.9, fx - sw * 1.05, fy + sh * 1.3, fx - sw, fy + sh * 0.2); ctx.fill();
      ctx.beginPath(); ctx.moveTo(fx + sw * 0.55, fy + sh * 1.5); ctx.quadraticCurveTo(fx + sw * 0.75, fy + sh * 3.5, fx + sw * 0.62, fy + sh * 5.2); ctx.lineTo(fx + sw * 0.32, fy + sh * 5.0); ctx.quadraticCurveTo(fx + sw * 0.4, fy + sh * 3.2, fx + sw * 0.25, fy + sh * 1.7); ctx.fill(); // the hanging end
      ctx.strokeStyle = 'rgba(0,0,0,0.35)'; ctx.lineWidth = 3;
      for (let k = -5; k <= 5; k++) { const x0 = fx + k * sw * 0.18; ctx.beginPath(); ctx.moveTo(x0, fy + sh * 0.05); ctx.quadraticCurveTo(x0 + 4, fy + sh, x0, fy + sh * 1.8); ctx.stroke(); } // ribs
      ctx.strokeStyle = 'rgba(255,214,150,0.25)'; ctx.lineWidth = 3; ctx.beginPath(); ctx.moveTo(fx - sw * 0.8, fy - sh * 0.1 * sc); ctx.bezierCurveTo(fx - sw * 0.4, fy - sh * 0.45 * sc, fx + sw * 0.4, fy - sh * 0.45 * sc, fx + sw * 0.8, fy - sh * 0.1 * sc); ctx.stroke(); // the bulb on the top fold
      ctx.restore();
    }
    // the bag, held low
    ctx.save(); ctx.fillStyle = '#1b130d'; ctx.translate(box.dx + box.dw * 0.66, box.dy + box.dh * 0.86); ctx.rotate(0.05);
    ctx.beginPath(); ctx.roundRect(-110, -70, 220, 150, 18); ctx.fill(); ctx.strokeStyle = '#3a2a1c'; ctx.lineWidth = 8; ctx.beginPath(); ctx.arc(0, -70, 50, Math.PI, 0); ctx.stroke();
    ctx.fillStyle = 'rgba(190,40,40,0.8)'; ctx.fillRect(-10, -30, 20, 70); ctx.fillRect(-35, -5, 70, 20); ctx.restore();
  };
  // through the peephole: the content is bulged like a door lens (rings at different magnification), in a brass ring
  let LENS = null; const lens = () => LENS || (LENS = K.makeCanvas(W, H));
  const peephole = (ctx, t, draw, o = {}) => {
    const c = lens(), x = c.getContext('2d'); x.setTransform(1, 0, 0, 1, 0, 0); x.globalAlpha = 1; x.globalCompositeOperation = 'source-over';
    x.fillStyle = '#000'; x.fillRect(0, 0, W, H); draw(x);
    const cx = W / 2, cy = o.cy ?? 860, R = o.R ?? 520, N = 44;
    ctx.fillStyle = '#000'; ctx.fillRect(0, 0, W, H);
    for (let i = N; i >= 1; i--) {
      const ro = R * i / N, ri = R * (i - 1) / N, src = R * Math.pow(ro / R, 1.5), k = ro / Math.max(1, src) * (o.zoom || 0.62);
      ctx.save(); ctx.beginPath(); ctx.arc(cx, cy, ro + 1, 0, Math.PI * 2); if (ri > 1) { ctx.moveTo(cx + ri - 1, cy); ctx.arc(cx, cy, ri - 1, Math.PI * 2, 0, true); } ctx.clip();
      ctx.translate(cx, cy); ctx.scale(k, k); ctx.translate(-W / 2 - (o.ox || 0), -(o.oy ?? 960)); ctx.drawImage(c, 0, 0); ctx.restore();
    }
    const v = ctx.createRadialGradient(cx, cy, R * 0.55, cx, cy, R); v.addColorStop(0, 'rgba(0,0,0,0)'); v.addColorStop(1, 'rgba(0,0,0,0.9)'); ctx.fillStyle = v; ctx.beginPath(); ctx.arc(cx, cy, R, 0, 7); ctx.fill();
    ctx.strokeStyle = '#4a3a22'; ctx.lineWidth = 34; ctx.beginPath(); ctx.arc(cx, cy, R + 17, 0, 7); ctx.stroke();
    ctx.strokeStyle = 'rgba(255,220,160,0.25)'; ctx.lineWidth = 4; ctx.beginPath(); ctx.arc(cx, cy, R + 4, 3.6, 5.2); ctx.stroke();
    ctx.save(); ctx.globalCompositeOperation = 'lighter'; ctx.fillStyle = 'rgba(255,255,255,0.05)'; ctx.beginPath(); ctx.ellipse(cx - R * 0.35, cy - R * 0.45, R * 0.28, R * 0.12, -0.6, 0, 7); ctx.fill(); ctx.restore();
  };
  const handAt = (ctx, x, y, rot, s, reach = 1) => {
    ctx.save(); ctx.translate(x, y); ctx.rotate(rot); ctx.scale(s, s); ctx.fillStyle = '#2a2019'; ctx.strokeStyle = '#2a2019'; ctx.lineCap = 'round';
    ctx.beginPath(); ctx.moveTo(-260, 40); ctx.lineTo(-60, 20); ctx.lineTo(-60, -40); ctx.lineTo(-260, -30); ctx.fill();       // the forearm, from off-frame
    ctx.beginPath(); ctx.roundRect(-70, -45, 90, 85, 30); ctx.fill();                                                          // the palm
    ctx.lineWidth = 19; for (let f = 0; f < 4; f++) { const yy = -32 + f * 21; ctx.beginPath(); ctx.moveTo(15, yy); ctx.lineTo(15 + (52 - f * 6) * reach, yy + 4 * (1 - reach) * f); ctx.stroke(); } // fingers
    ctx.lineWidth = 21; ctx.beginPath(); ctx.moveTo(-20, -40); ctx.lineTo(25, -66); ctx.stroke();                              // thumb
    ctx.strokeStyle = 'rgba(150,175,210,0.45)'; ctx.lineWidth = 3; ctx.beginPath(); ctx.moveTo(-260, -30); ctx.lineTo(-60, -42); ctx.lineTo(18, -48); ctx.stroke(); // TV light on the edge
    ctx.restore();
  };
  // inside the flat: the door with chain, lock and handle; a line of light under it
  const doorInside = (ctx, t, o = {}) => {
    ctx.fillStyle = '#0c0b0a'; ctx.fillRect(0, 0, W, H);
    const tv = 0.5 + 0.5 * K.hash(Math.floor(t * 9), 3), g = ctx.createRadialGradient(-100, 900, 50, -100, 900, 1100); g.addColorStop(0, `rgba(120,150,190,${0.28 * tv * (o.tv ?? 1)})`); g.addColorStop(1, 'rgba(0,0,0,0)'); ctx.fillStyle = g; ctx.fillRect(0, 0, W, H);
    const open = o.open || 0;
    ctx.fillStyle = '#060504'; ctx.fillRect(190, 160, 700, 1640); // the doorway
    if (open > 0) { // the stairwell behind the opening door
      ctx.save(); ctx.beginPath(); ctx.rect(190, 160, 700, 1640); ctx.clip(); ctx.translate(190, 160); ctx.scale(700 / W, 1640 / H); stairwell(ctx, t); ctx.restore();
    }
    ctx.save(); ctx.translate(190, 0); ctx.scale(1 - open * 0.93, 1);
    const dg = ctx.createLinearGradient(0, 0, 700, 0); dg.addColorStop(0, '#2a241d'); dg.addColorStop(1, '#1b1712'); ctx.fillStyle = dg; ctx.fillRect(0, 160, 700, 1640);
    ctx.strokeStyle = 'rgba(0,0,0,0.6)'; ctx.lineWidth = 6; ctx.strokeRect(60, 260, 580, 640); ctx.strokeRect(60, 980, 580, 720);
    ctx.fillStyle = '#5a4a30'; ctx.beginPath(); ctx.arc(350, 560, 20, 0, 7); ctx.fill(); ctx.fillStyle = `rgba(255,214,150,${0.5 * bulb(t)})`; ctx.beginPath(); ctx.arc(350, 560, 9, 0, 7); ctx.fill(); // peephole
    ctx.fillStyle = '#6a5a3e'; ctx.fillRect(600, 900, 34, 170); ctx.fillRect(470, 922, 140, 22); ctx.fillStyle = '#1a140e'; ctx.fillRect(611, 1020, 12, 26); // the plate, the lever handle, the keyhole
    ctx.restore();
    if (open < 0.05) { const lg = ctx.createLinearGradient(0, 1790, 0, 1830); lg.addColorStop(0, `rgba(255,210,150,${0.6 * bulb(t)})`); lg.addColorStop(1, 'rgba(255,210,150,0)'); ctx.fillStyle = lg; ctx.fillRect(190, 1790, 700, 40); }
  };

  // ================= the check list =================
  const CHECK = [ // [bar, text, ok, art, zoom rect focus fx/fy, zoom]
    [8, 'очки — разбиты', 0, 'face', 0.55, 0.36, 2.0],
    [9, 'на лбу — шрам', 0, 'face', 0.5, 0.18, 2.0],
    [10, 'челюсть — не своя', 0, 'face', 0.5, 0.62, 1.9],
    [11, 'хромает', 0, 'walk', 0.5, 0.85, 1.6],
    [12, 'руки — в чужой крови', 0, 'coat', 0.45, 0.62, 1.8],
    [13, 'форма — не наша', 0, 'tent', 0.5, 0.55, 1.5],
    [14, 'глаза — живые', 1, 'face', 0.45, 0.36, 2.2],
  ];
  // ================= the doors he opened =================
  const DOORS = [
    [17, 'tent', 'немец', 'ОТКРЫЛ'], [18, 'carry', 'советский', 'ОТКРЫЛ'], [19, 'coat', 'британец', 'ОТКРЫЛ'], [20, 'typhus', 'пленный', 'СПРЯТАЛ'],
    [21, 'head', 'тот, кто хотел\nего убить', 'ПЕРЕВЯЗАЛ'],
    [22, 'tent', 'свой', 'ОТКРЫЛ'], [22.5, 'coat', 'чужой', 'ОТКРЫЛ'], [23, 'carry', 'пленный', 'ОТКРЫЛ'], [23.5, 'head', 'враг', 'ОТКРЫЛ'], [24, 'walk', 'любой', 'ОТКРЫЛ'],
  ];
  const typhusDoor = (ctx, t) => {
    ctx.fillStyle = '#1a1712'; ctx.fillRect(0, 0, W, H);
    ctx.fillStyle = '#2e281f'; ctx.fillRect(170, 200, 740, 1500); ctx.strokeStyle = '#0d0b08'; ctx.lineWidth = 10; ctx.strokeRect(170, 200, 740, 1500);
    ctx.save(); ctx.translate(W / 2, 760); ctx.rotate(-0.04); ctx.fillStyle = '#d9cfb6'; ctx.fillRect(-250, -150, 500, 300);
    ctx.strokeStyle = '#a01818'; ctx.lineWidth = 8; ctx.strokeRect(-235, -135, 470, 270);
    ctx.fillStyle = '#a01818'; ctx.font = `bold 130px ${OSW}`; ctx.textAlign = 'center'; ctx.textBaseline = 'middle'; ctx.fillText('ТИФ', 0, -20);
    ctx.font = `34px ${MONO}`; ctx.fillText('не входить', 0, 90); ctx.restore();
  };

  // ================= scenes =================
  const SCENES = [
    { a: 0, b: 7.2, draw(ctx, t, frame) { // inside, the door; knocking
      const k = Math.exp(-since(KNOCKS, t) * 20), z = 1 + 0.18 * es(t, 0.5, 7.2);
      ctx.save(); ctx.translate(W / 2, 560); ctx.scale(z, z); ctx.translate(-W / 2 + (K.hash(frame, 1) - 0.5) * 10 * k, -560);
      doorInside(ctx, t); ctx.restore();
      if (k > 0.5) { ctx.fillStyle = `rgba(255,240,220,${0.05 * k})`; ctx.fillRect(0, 0, W, H); }
    } },
    { a: 7.2, b: 23.0, draw(ctx, t, frame) { // the peephole; the dialogue
      const sc = 1 - es(t, 17.5, 18.7), close = t > 18.7 && t < 20.25;
      peephole(ctx, t, x => { stairwell(x, t); if (close) art(x, 'face', t, { x: 140, y: 380, w: 800, h: 1100 }, { fy: 0.35, z: 1.05 }); else man(x, t, { scarf: sc }); }, { zoom: close ? 0.75 : 0.62 + 0.04 * es(t, 7.2, 23) });
      sub(ctx, '— кто там?', t, 7.5, 9.5);
      sub(ctx, '— врач.', t, 9.6, 11.9, { color: HIM });
      sub(ctx, '— у вас раненый.\nя слышал.', t, 12.0, 14.7, { color: HIM });
      sub(ctx, '— сними шарф.', t, 14.8, 17.4);
      sub(ctx, '— ты вообще человек?', t, 20.3, 23.0);
    } },
    { a: 23.0, b: bar(16), draw(ctx, t, frame) { // the check: every sign fails but one
      const i = Math.min(CHECK.length - 1, Math.max(0, Math.floor((t - bar(8)) / BAR))), c = CHECK[i], kk = kick(t);
      if (t < bar(15)) {
        const zp = es(t, bar(c[0]), bar(c[0]) + 0.5);
        peephole(ctx, t, x => { stairwell(x, t, { light: 0.8 }); art(x, c[3], t, { x: 60, y: 300, w: 960, h: 1320 }, { fx: c[4], fy: c[5], z: K.lerp(1, c[6], zp) });
          x.strokeStyle = c[2] ? GREEN : RED; x.lineWidth = 6; x.strokeRect(W / 2 - 260, 700, 520, 400); }, { zoom: 0.62 * (1 + 0.04 * kk) });
      } else { peephole(ctx, t, x => { stairwell(x, t, { light: 0.7 }); man(x, t, { scarf: 0 }); }, { zoom: 0.6 }); ctx.fillStyle = 'rgba(80,0,0,0.35)'; ctx.fillRect(0, 0, W, H); }
      // the list, growing
      ctx.save(); ctx.font = `44px ${MONO}`; ctx.textAlign = 'left';
      CHECK.forEach((c, j) => {
        const t0 = bar(c[0]); if (t < t0) return;
        const y = 1430 + j * 58, s = c[1].slice(0, Math.floor((t - t0) * 30));
        ctx.globalAlpha = j === i && t < bar(15) ? 1 : 0.7; ctx.lineWidth = 7; ctx.strokeStyle = '#000'; ctx.strokeText(s, 160, y); ctx.fillStyle = IN; ctx.fillText(s, 160, y);
        if (t > t0 + 2 * BEAT) { ctx.fillStyle = c[2] ? GREEN : RED; ctx.font = `bold 52px ${MONO}`; ctx.fillText(c[2] ? '✓' : '✗', 90, y + 2); ctx.font = `44px ${MONO}`; }
      });
      ctx.restore();
      stamp(ctx, 'НЕ ОТКРЫВАТЬ', t, bar(15), { y: 860, size: 120 });
    } },
    { a: bar(16), b: bar(17), draw(ctx, t, frame) { // the break: go away. — alright.
      const back = t > bar(16) + 1.6;
      peephole(ctx, t, x => { stairwell(x, t, { light: 0.75 }); if (!back) man(x, t, { scarf: 0 }); else art(x, 'walk', t, { x: 240, y: 520, w: 600, h: 900 }, { fy: 0.4, z: 1 - 0.25 * pr(t, bar(16) + 1.6, bar(17)), alpha: 0.95 }); }, { zoom: 0.6 });
      sub(ctx, '— уходи.', t, bar(16) + 0.1, bar(16) + 1.5);
      sub(ctx, '— хорошо.', t, bar(16) + 1.5, bar(17), { color: HIM });
    } },
    { a: bar(17), b: bar(25), draw(ctx, t, frame) { // every door he opened
      let j = 0; for (let k = 0; k < DOORS.length; k++) if (t >= bar(DOORS[k][0])) j = k;
      const [b0, key, who, word] = DOORS[j], t0 = bar(b0), len = (DOORS[j + 1] ? bar(DOORS[j + 1][0]) : bar(25)) - t0, kk = kick(t);
      ctx.save(); ctx.translate(W / 2, H / 2); ctx.scale(1 + 0.03 * kk, 1 + 0.03 * kk); ctx.translate(-W / 2, -H / 2);
      if (key === 'typhus') typhusDoor(ctx, t); else { ctx.fillStyle = '#0a0908'; ctx.fillRect(0, 0, W, H); art(ctx, key, t, { x: 0, y: 220, w: W, h: 1420 }, { fy: 0.3, z: 1.04 + 0.05 * pr(t, t0, t0 + len) }); }
      ctx.restore();
      // the door leaf swings open over the picture
      const op = es(t, t0, t0 + Math.min(0.45, len * 0.3));
      if (op < 1) { ctx.save(); ctx.fillStyle = '#15110d'; ctx.fillRect(0, 0, W * (1 - op), H); ctx.fillStyle = `rgba(255,210,150,${0.6 * (1 - op)})`; ctx.fillRect(W * (1 - op) - 8, 0, 8, H); ctx.restore(); }
      const g = ctx.createLinearGradient(0, 0, 0, 420); g.addColorStop(0, 'rgba(0,0,0,0.9)'); g.addColorStop(1, 'rgba(0,0,0,0)'); ctx.fillStyle = g; ctx.fillRect(0, 0, W, 420);
      sub(ctx, who, t, t0 + 0.15, t0 + len, { y: 200, size: 62, cps: 40 });
      stamp(ctx, word, t, t0 + Math.min(2 * BEAT, len * 0.5), { y: 1640, size: word.length > 7 ? 104 : 130, rot: -0.06 + 0.04 * (j % 2) });
    } },
    { a: bar(25), b: bar(29), draw(ctx, t, frame) { // inside: he is gone; someone is bleeding; the TV says don't
      const part = Math.floor((t - bar(25)) / (BAR / 2)) % 4, kk = kick(t);
      if (part === 0) { peephole(ctx, t, x => stairwell(x, t, { light: 0.8 }), { zoom: 0.62 }); }
      else if (part === 1) { // the wounded on the floor, in the TV light
        ctx.fillStyle = '#0b0908'; ctx.fillRect(0, 0, W, H);
        const tv = ctx.createRadialGradient(-100, 900, 50, -100, 900, 1300); tv.addColorStop(0, 'rgba(120,150,190,0.35)'); tv.addColorStop(1, 'rgba(0,0,0,0)'); ctx.fillStyle = tv; ctx.fillRect(0, 0, W, H);
        ctx.fillStyle = '#300808'; ctx.beginPath(); ctx.ellipse(560, 1260, 360, 110, -0.05, 0, 7); ctx.fill();
        const br = 1 + 0.02 * Math.sin(t * 2.4);
        ctx.save(); ctx.translate(560, 1180); ctx.scale(1, br); ctx.fillStyle = '#18130f';
        ctx.beginPath(); ctx.ellipse(-330, -10, 70, 58, 0.2, 0, 7); ctx.fill();                                               // head
        ctx.beginPath(); ctx.moveTo(-270, -50); ctx.quadraticCurveTo(0, -110, 330, -40); ctx.lineTo(360, 40); ctx.quadraticCurveTo(0, 60, -270, 40); ctx.fill(); // body
        ctx.restore();
        ctx.save(); ctx.translate(380, 1120); ctx.rotate(-0.5 + 0.04 * Math.sin(t * 5)); ctx.fillStyle = '#18130f'; ctx.fillRect(-20, -20, 300, 40); ctx.restore(); // an arm reaching up
        handAt(ctx, 640, 960 + 6 * Math.sin(t * 5), -0.6, 0.7, 0.5);
        ctx.strokeStyle = 'rgba(150,175,210,0.35)'; ctx.lineWidth = 3; ctx.beginPath(); ctx.moveTo(290, 1130); ctx.quadraticCurveTo(560, 1060, 890, 1140); ctx.stroke();
      } else if (part === 2) { // the TV
        ctx.fillStyle = '#050505'; ctx.fillRect(0, 0, W, H);
        ctx.save(); ctx.beginPath(); ctx.roundRect(120, 560, 840, 640, 60); ctx.clip(); ctx.fillStyle = '#1d2630'; ctx.fillRect(120, 560, 840, 640);
        ctx.globalAlpha = 0.6; ctx.drawImage(K.noise[frame % 4], 120, 560, 840, 640); ctx.globalAlpha = 1;
        ctx.fillStyle = 'rgba(0,0,0,0.25)'; for (let y = 560; y < 1200; y += 8) ctx.fillRect(120, y, 840, 3);
        ctx.fillStyle = '#e8e0d0'; ctx.font = `bold 66px ${MONO}`; ctx.textAlign = 'center'; ctx.fillText('НЕ ОТКРЫВАЙТЕ', W / 2, 860); ctx.fillText('ДВЕРЬ', W / 2, 950);
        ctx.restore(); ctx.strokeStyle = '#2a2724'; ctx.lineWidth = 40; ctx.beginPath(); ctx.roundRect(120, 560, 840, 640, 60); ctx.stroke();
      } else { doorInside(ctx, t); ctx.fillStyle = 'rgba(0,0,0,0.3)'; ctx.fillRect(0, 0, W, H); }
      if (kk > 0.7) { ctx.fillStyle = `rgba(255,255,255,${0.06 * kk})`; ctx.fillRect(0, 0, W, H); }
      sub(ctx, '— ушёл.', t, bar(25) + 0.2, bar(26) - 0.1);
      sub(ctx, '— пить…', t, bar(26) + 0.2, bar(27) - 0.1, { color: '#d9a0a0' });
      sub(ctx, '— чёрт.', t, bar(28) + 0.2, bar(29) - 0.1);
    } },
    { a: bar(29), b: STOP, draw(ctx, t, frame) { // chain, lock, handle, the door
      const step = Math.floor((t - bar(29)) / BAR), d = t - bar(29 + step), kk = kick(t);
      if (step === 0 || step === 1 || step === 2) { // close-ups of the hand at the chain / lock / handle
        doorInside(ctx, t);
        const z = 2.6, cx = step === 0 ? 700 : step === 1 ? 640 : 560, cy = step === 0 ? 520 : step === 1 ? 1000 : 940;
        const c = lens(), x = c.getContext('2d'); x.setTransform(1, 0, 0, 1, 0, 0); x.drawImage(ctx.canvas, 0, 0);
        ctx.save(); ctx.translate(W / 2, H / 2); ctx.scale(z, z); ctx.translate(-cx, -cy); ctx.drawImage(c, 0, 0);
        if (step === 0) { // the chain: links hanging from the frame to the track, then slid out
          const sl = es(d, 0.4, 1.8), ax = 790, ay = 505, bx = 610 + sl * 150, by = 515 + sl * 70;
          ctx.fillStyle = '#6a5a3e'; ctx.fillRect(560, 495, 240, 16); ctx.fillRect(790, 490, 30, 34);
          for (let k = 0; k <= 9; k++) { const u = k / 9, px = K.lerp(ax, bx, u), py = K.lerp(ay, by, u) + Math.sin(u * Math.PI) * (40 + 30 * sl); ctx.strokeStyle = '#b9a67c'; ctx.lineWidth = 4; ctx.beginPath(); ctx.ellipse(px, py, 9, 5, k % 2 ? 1.2 : 0.2, 0, 7); ctx.stroke(); }
          handAt(ctx, bx + 6, by + 6, 0.15, 0.55, 0.6);
        }
        if (step === 1) { // the key in the lock turns
          ctx.fillStyle = '#4a3c28'; ctx.beginPath(); ctx.arc(617, 1033, 26, 0, 7); ctx.fill();
          ctx.save(); ctx.translate(617, 1033); ctx.rotate(-es(d, 0.5, 1.2) * Math.PI / 2); ctx.fillStyle = '#c9b382'; ctx.fillRect(-7, -60, 14, 60); ctx.beginPath(); ctx.arc(0, -72, 22, 0, 7); ctx.fill(); ctx.restore();
          handAt(ctx, 600, 990 - es(d, 0.5, 1.2) * 10, 0.6, 0.55, 0.55);
        }
        if (step === 2) handAt(ctx, 520, 930 + es(d, 0.6, 1.3) * 22, 0.05 + es(d, 0.6, 1.3) * 0.25, 0.6, 0.8); // the handle goes down
        ctx.restore();
        if (step === 2) { ctx.fillStyle = `rgba(0,0,0,${0.3})`; ctx.fillRect(0, 0, W, H); }
      } else { // the door opens: nobody — on the mat, bandages and an ampoule
        const op = es(d, 0, 1.4), zz = 1 + 1.3 * es(d, 1.2, 2.6);
        ctx.save(); ctx.translate(540, 1700); ctx.scale(zz, zz); ctx.translate(-540, -1700 - 260 * (zz - 1) / 1.3);
        doorInside(ctx, t, { open: op, tv: 0.5 });
        if (op > 0.5) {
          ctx.save(); ctx.globalAlpha = pr(op, 0.5, 0.9);
          ctx.fillStyle = '#3a2f22'; ctx.fillRect(330, 1690, 420, 90);                                       // the mat
          ctx.fillStyle = '#e9e2d2'; ctx.beginPath(); ctx.ellipse(470, 1700, 60, 34, 0, 0, 7); ctx.fill(); ctx.strokeStyle = '#b9b1a0'; ctx.lineWidth = 3; ctx.beginPath(); ctx.ellipse(470, 1700, 30, 16, 0, 0, 7); ctx.stroke(); // a roll of bandage
          ctx.fillStyle = '#d6cfc0'; ctx.fillRect(520, 1688, 120, 40);                                       // a folded one
          ctx.fillStyle = 'rgba(220,235,240,0.7)'; ctx.fillRect(660, 1676, 14, 52); ctx.fillStyle = 'rgba(200,120,60,0.6)'; ctx.fillRect(660, 1700, 14, 22); // the ampoule
          ctx.restore();
        }
        ctx.restore();
      }
      if (kk > 0.6) { ctx.fillStyle = `rgba(255,240,220,${0.05 * kk})`; ctx.fillRect(0, 0, W, H); }
    } },
    { a: STOP, b: END, draw(ctx, t, frame) { // the song stops. silence.
      ctx.fillStyle = '#000'; ctx.fillRect(0, 0, W, H);
      sub(ctx, '(бинты он\nвсё-таки оставил)', t, STOP + 0.7, END - 0.1, { y: 900, size: 76, color: RED, cps: 22 });
    } },
  ];
  const CUTS = [7.2, 23.0, bar(16), bar(17), ...DOORS.map(d => bar(d[0])), bar(25), bar(29), bar(30), bar(31), bar(32), STOP];

  window.VERSION = {
    duration: END,
    images,
    render(ctx, t, frame) {
      const sc = SCENES.find(s => t >= s.a && t < s.b) || SCENES[SCENES.length - 1];
      ctx.save(); ctx.setTransform(1, 0, 0, 1, 0, 0); ctx.globalAlpha = 1; ctx.globalCompositeOperation = 'source-over'; ctx.filter = 'none';
      ctx.fillStyle = '#000'; ctx.fillRect(0, 0, W, H);
      sc.draw(ctx, t, frame);
      ctx.restore();
      const c = since(CUTS, t);
      if (c < 0.06 && t < STOP) { ctx.fillStyle = 'rgba(255,255,255,0.18)'; ctx.fillRect(0, 0, W, H); }
      if (t < STOP + 0.05) { K.grain(ctx, frame, 0.12); K.vignette(ctx, 0.6); ctx.fillStyle = 'rgba(0,0,0,0.12)'; for (let y = 0; y < H; y += 6) ctx.fillRect(0, y, W, 2); }
      if (t < 0.6) { ctx.fillStyle = `rgba(0,0,0,${1 - t / 0.6})`; ctx.fillRect(0, 0, W, H); }
    },
  };
})();
