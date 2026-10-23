// Version P — «Глазок» (second take). Night. A knock. One man inside, an old radio that tells him how to tell people from
// the others, a child coughing in the next room. Behind the door: Kostya, silent (he has no jaw to speak with). The radio
// says not to open; the man doesn't. In the morning: bandages, a bottle and a note on the mat. Why he came is only clear
// then. Arts shown as they are, through the door lens. Song: «Credits» (No, I'm Not a Human OST), 87 bpm, stop at 91.95.
(function () {
  const { W, H } = K;
  const I = n => K.images[n];
  const pr = (t, a, b) => K.prog(t, a, b);
  const es = (t, a, b) => K.ease.inOutSine(K.prog(t, a, b));
  const win = (t, a, b) => t >= a && t < b;
  const since = (list, t) => { let b = 1e9; for (const k of list) if (k <= t) b = Math.min(b, t - k); return b; };
  const MONO = '"PT Mono"', HAND = 'Caveat';
  const IN = '#ece6da', RADIO = '#f0b75c', RED = '#e0242f';

  const BEAT = 0.6896, BAR = BEAT * 4, bar = n => 0.937 + BAR * n;
  const STOP = 91.95, END = 98.0;
  const KICKS = []; for (let n = 8; n < 33; n++) if (n !== 16) KICKS.push(bar(n), bar(n) + 2 * BEAT);
  const kick = t => Math.exp(-since(KICKS, t) * 8);
  const KNOCKS = [1.0, 1.3, 1.6, 6.47, 6.77, 7.07];
  const COUGHS = [3.2, 15.6, 26.4, 33.9, 46.6, 52.0, 58.5, 63.0, 67.1, 71.4, 74.2, 77.0, 79.6, 82.3, 84.8, 87.0, 89.1];

  // ================= text =================
  const sub = (ctx, s, t, t0, t1, o = {}) => {
    if (t < t0 || t > t1) return;
    const str = s.slice(0, Math.floor((t - t0) * (o.cps || 26))), a = 1 - pr(t, t1 - 0.3, t1);
    ctx.save(); ctx.font = `${o.size || 56}px ${o.font || MONO}`; ctx.textAlign = 'center'; ctx.globalAlpha = a; ctx.lineWidth = 8; ctx.strokeStyle = 'rgba(0,0,0,0.85)'; ctx.lineJoin = 'round';
    str.split('\n').forEach((l, i) => { const y = (o.y || 1720) + i * (o.size || 56) * 1.25, jx = o.jitter ? (K.hash(i, Math.floor(t * 18)) - 0.5) * o.jitter : 0; ctx.strokeText(l, W / 2 + jx, y); ctx.fillStyle = o.color || IN; ctx.fillText(l, W / 2 + jx, y); });
    ctx.restore();
  };
  const radioSays = (ctx, s, t, t0, t1) => sub(ctx, s, t, t0, t1, { color: RADIO, jitter: 3, cps: 22 });

  // ================= the stairwell and the lens (as in M) =================
  const bulb = t => { const f = K.hash(Math.floor(t * 12), 7); return f > 0.93 ? 0.15 : f > 0.85 ? 0.55 : 0.95 + 0.05 * Math.sin(t * 40); };
  const stairwell = (ctx, t, o = {}) => {
    const b = (o.light ?? 1) * bulb(t);
    const g = ctx.createLinearGradient(0, 0, 0, H); g.addColorStop(0, '#2b3029'); g.addColorStop(0.55, '#1d211c'); g.addColorStop(1, '#0d0f0c'); ctx.fillStyle = g; ctx.fillRect(0, 0, W, H);
    ctx.save(); ctx.globalAlpha = 0.35; for (let y = 0; y < H; y += 512) for (let x = 0; x < W; x += 512) ctx.drawImage(K.noise[2], x, y); ctx.restore();
    ctx.fillStyle = '#141712'; ctx.fillRect(80, 360, 230, 1100); ctx.fillStyle = '#0a0b09'; ctx.fillRect(250, 900, 24, 60);
    ctx.strokeStyle = '#0b0c0a'; ctx.lineWidth = 14; ctx.beginPath(); ctx.moveTo(760, 1900); ctx.lineTo(1080, 1250); ctx.stroke();
    ctx.fillStyle = '#0b0c0a'; ctx.fillRect(W / 2 - 3, 0, 6, 190); ctx.beginPath(); ctx.arc(W / 2, 205, 22, 0, 7); ctx.fill();
    const l = ctx.createRadialGradient(W / 2, 215, 4, W / 2, 215, 900); l.addColorStop(0, `rgba(255,214,150,${0.9 * b})`); l.addColorStop(0.08, `rgba(255,200,130,${0.45 * b})`); l.addColorStop(1, 'rgba(255,190,120,0)');
    ctx.save(); ctx.globalCompositeOperation = 'lighter'; ctx.fillStyle = l; ctx.fillRect(0, 0, W, H); ctx.restore();
    if (b < 0.3) { ctx.fillStyle = `rgba(0,0,0,${0.6 - b})`; ctx.fillRect(0, 0, W, H); }
  };
  let LENS = null; const lens = () => LENS || (LENS = K.makeCanvas(W, H));
  const peephole = (ctx, t, draw, o = {}) => {
    const c = lens(), x = c.getContext('2d'); x.setTransform(1, 0, 0, 1, 0, 0); x.globalAlpha = 1; x.globalCompositeOperation = 'source-over'; x.filter = 'none';
    x.fillStyle = '#000'; x.fillRect(0, 0, W, H); draw(x);
    const cx = W / 2, cy = o.cy ?? 860, R = o.R ?? 520, N = 44;
    ctx.fillStyle = '#000'; ctx.fillRect(0, 0, W, H);
    for (let i = N; i >= 1; i--) {
      const ro = R * i / N, ri = R * (i - 1) / N, src = R * Math.pow(ro / R, 1.5), k = ro / Math.max(1, src) * (o.zoom || 0.62);
      ctx.save(); ctx.beginPath(); ctx.arc(cx, cy, ro + 1, 0, Math.PI * 2); if (ri > 1) { ctx.moveTo(cx + ri - 1, cy); ctx.arc(cx, cy, ri - 1, Math.PI * 2, 0, true); } ctx.clip();
      ctx.translate(cx + (o.dx || 0), cy + (o.dy || 0)); ctx.scale(k, k); ctx.translate(-W / 2 - (o.ox || 0), -(o.oy ?? 960)); ctx.drawImage(c, 0, 0); ctx.restore();
    }
    if (o.tint) { ctx.save(); ctx.globalCompositeOperation = o.tint === 'cold' ? 'multiply' : 'soft-light'; ctx.fillStyle = o.tint === 'cold' ? 'rgba(150,175,205,1)' : 'rgba(255,190,120,0.7)'; ctx.beginPath(); ctx.arc(cx, cy, R, 0, 7); ctx.fill(); ctx.restore(); }
    const v = ctx.createRadialGradient(cx, cy, R * 0.55, cx, cy, R); v.addColorStop(0, 'rgba(0,0,0,0)'); v.addColorStop(1, 'rgba(0,0,0,0.9)'); ctx.fillStyle = v; ctx.beginPath(); ctx.arc(cx, cy, R, 0, 7); ctx.fill();
    ctx.strokeStyle = '#4a3a22'; ctx.lineWidth = 34; ctx.beginPath(); ctx.arc(cx, cy, R + 17, 0, 7); ctx.stroke();
    ctx.strokeStyle = 'rgba(255,220,160,0.25)'; ctx.lineWidth = 4; ctx.beginPath(); ctx.arc(cx, cy, R + 4, 3.6, 5.2); ctx.stroke();
    ctx.save(); ctx.globalCompositeOperation = 'lighter'; ctx.fillStyle = 'rgba(255,255,255,0.05)'; ctx.beginPath(); ctx.ellipse(cx - R * 0.35, cy - R * 0.45, R * 0.28, R * 0.12, -0.6, 0, 7); ctx.fill(); ctx.restore();
  };
  // what the lens shows: him on the landing (the photo as it is), his face, or a picture of who he is
  const atDoor = (x, t, o = {}) => { stairwell(x, t, o); const im = I('door'), s = 1350 / im.height * (o.z || 1); const w = im.width * s, h = im.height * s; x.drawImage(im, W / 2 - w * 0.42, 330 + (o.lift || 0), w, h); };
  const lensPhoto = (x, key, o = {}) => { x.fillStyle = '#000'; x.fillRect(0, 0, W, H); K.cover(x, I(key), { x: 0, y: 160, w: W, h: 1600 }, o.z || 1, o.fx ?? 0.5, o.fy ?? 0.3); };

  // ================= inside: the radio, the clock, the door, the chain, the doorway, the mat =================
  const wall = ctx => {
    ctx.fillStyle = '#14110e'; ctx.fillRect(0, 0, W, H);
    for (let x = 0; x < W; x += 120) { ctx.fillStyle = 'rgba(60,46,34,0.35)'; ctx.fillRect(x, 0, 50, H); }
    ctx.save(); ctx.globalAlpha = 0.3; ctx.globalCompositeOperation = 'multiply'; for (let y = 0; y < H; y += 512) for (let x = 0; x < W; x += 512) ctx.drawImage(K.noise[3], x, y); ctx.restore();
  };
  // the radio, close: a wooden set with a lit dial and a green tuning eye; it brightens while it speaks
  const radio = (ctx, t, speak = 0, o = {}) => {
    wall(ctx);
    const glow = 0.7 + 0.3 * speak * (0.6 + 0.4 * Math.sin(t * 23) * Math.sin(t * 7)), x0 = 90, y0 = 380, w = 900, h = 1150;
    const lg = ctx.createRadialGradient(W / 2, 700, 50, W / 2, 700, 1200); lg.addColorStop(0, `rgba(255,170,80,${0.25 * glow})`); lg.addColorStop(1, 'rgba(0,0,0,0)'); ctx.fillStyle = lg; ctx.fillRect(0, 0, W, H);
    ctx.fillStyle = 'rgba(0,0,0,0.6)'; ctx.beginPath(); ctx.roundRect(x0 + 16, y0 + 26, w, h, 50); ctx.fill();
    const wg = ctx.createLinearGradient(x0, 0, x0 + w, 0); wg.addColorStop(0, '#2c180b'); wg.addColorStop(0.5, '#4a2a14'); wg.addColorStop(1, '#25140a'); ctx.fillStyle = wg; ctx.beginPath(); ctx.roundRect(x0, y0, w, h, 50); ctx.fill();
    ctx.save(); ctx.beginPath(); ctx.roundRect(x0, y0, w, h, 50); ctx.clip(); const r = K.rng(5);
    for (let i = 0; i < 120; i++) { const xx = x0 + r() * w, amp = 6 + r() * 24; ctx.strokeStyle = `rgba(${r() > 0.5 ? '20,10,4' : '90,55,30'},${0.08 + r() * 0.16})`; ctx.lineWidth = 0.8 + r() * 2; ctx.beginPath(); for (let yy = y0; yy <= y0 + h; yy += 14) ctx.lineTo(xx + Math.sin(yy * 0.004 + i) * amp, yy); ctx.stroke(); }
    ctx.restore();
    // the dial
    const D = { x: x0 + 70, y: y0 + 80, w: w - 140, h: 360 };
    ctx.save(); ctx.beginPath(); ctx.roundRect(D.x, D.y, D.w, D.h, 22); ctx.clip();
    const dg = ctx.createRadialGradient(D.x + D.w / 2, D.y + D.h * 0.55, 20, D.x + D.w / 2, D.y + D.h * 0.55, D.w * 0.7);
    dg.addColorStop(0, `rgb(${Math.round(255 * glow)},${Math.round(196 * glow)},${Math.round(110 * glow)})`); dg.addColorStop(1, `rgb(${Math.round(110 * glow)},${Math.round(52 * glow)},${Math.round(14 * glow)})`); ctx.fillStyle = dg; ctx.fillRect(D.x, D.y, D.w, D.h);
    ctx.strokeStyle = 'rgba(45,22,6,0.85)'; ctx.fillStyle = 'rgba(45,22,6,0.9)'; ctx.font = `24px ${MONO}`; ctx.textAlign = 'center';
    for (let row = 0; row < 3; row++) { const yy = D.y + 110 + row * 95; ctx.lineWidth = 2; ctx.beginPath(); ctx.moveTo(D.x + 40, yy); ctx.lineTo(D.x + D.w - 40, yy); ctx.stroke(); for (let k = 0; k <= 30; k++) { const xx = D.x + 40 + k * (D.w - 80) / 30, L = k % 5 ? 8 : 18; ctx.lineWidth = k % 5 ? 1.2 : 2; ctx.beginPath(); ctx.moveTo(xx, yy); ctx.lineTo(xx, yy - L); ctx.stroke(); if (!(k % 5) && row === 0) ctx.fillText(String(150 + k * 10), xx, D.y + 48); } }
    const nx = D.x + D.w * (0.56 + (o.needle || 0) + (speak ? 0.004 * Math.sin(t * 30) : 0));
    ctx.fillStyle = `rgba(255,90,40,${0.3 * glow})`; ctx.fillRect(nx - 8, D.y, 16, D.h); ctx.fillStyle = '#b3160f'; ctx.fillRect(nx - 2.5, D.y + 24, 5, D.h - 48);
    if (o.static) { ctx.globalAlpha = o.static; ctx.globalCompositeOperation = 'overlay'; ctx.drawImage(K.noise[Math.floor(t * 30) % 4], D.x, D.y, D.w, D.h); }
    ctx.restore();
    ctx.strokeStyle = '#b39a6a'; ctx.lineWidth = 7; ctx.beginPath(); ctx.roundRect(D.x, D.y, D.w, D.h, 22); ctx.stroke();
    // the eye
    const ex = W / 2, ey = y0 + 560, er = 62, open = speak ? 0.3 + 0.25 * Math.abs(Math.sin(t * 11)) : 1.4;
    const bz = ctx.createRadialGradient(ex - 16, ey - 20, 4, ex, ey, er + 14); bz.addColorStop(0, '#e8e2d4'); bz.addColorStop(0.5, '#7a7468'); bz.addColorStop(1, '#2a2622'); ctx.fillStyle = bz; ctx.beginPath(); ctx.arc(ex, ey, er + 14, 0, 7); ctx.fill();
    ctx.fillStyle = '#020403'; ctx.beginPath(); ctx.arc(ex, ey, er, 0, 7); ctx.fill();
    ctx.save(); ctx.globalCompositeOperation = 'lighter'; const gl = ctx.createRadialGradient(ex, ey, 8, ex, ey, er); gl.addColorStop(0, 'rgba(60,255,140,0.15)'); gl.addColorStop(0.55, 'rgba(70,255,150,0.85)'); gl.addColorStop(1, 'rgba(20,120,60,0.4)'); ctx.fillStyle = gl;
    const half = open / 2, top = -Math.PI / 2; ctx.beginPath(); ctx.moveTo(ex, ey); ctx.arc(ex, ey, er - 4, top + half, top - half + Math.PI * 2); ctx.closePath(); ctx.fill(); ctx.restore();
    ctx.fillStyle = '#050505'; ctx.beginPath(); ctx.arc(ex, ey, 11, 0, 7); ctx.fill();
    // knobs and the cloth
    for (const kx of [x0 + 170, x0 + w - 170]) { const g = ctx.createRadialGradient(kx - 20, ey - 24, 4, kx, ey, 66); g.addColorStop(0, '#6b4a33'); g.addColorStop(0.6, '#2b1a10'); g.addColorStop(1, '#120a05'); ctx.fillStyle = g; ctx.beginPath(); ctx.arc(kx, ey, 62, 0, 7); ctx.fill(); }
    const G = { x: x0 + 80, y: y0 + 680, w: w - 160, h: 400 }, bass = 0.006 * speak * Math.abs(Math.sin(t * 9));
    ctx.save(); ctx.beginPath(); ctx.roundRect(G.x, G.y, G.w, G.h, 26); ctx.clip(); ctx.fillStyle = '#4e3c29'; ctx.fillRect(G.x, G.y, G.w, G.h);
    for (let yy = G.y; yy < G.y + G.h; yy += 4) { ctx.fillStyle = `rgba(30,20,10,${yy % 8 ? 0.35 : 0.15})`; ctx.fillRect(G.x, yy, G.w, 2); }
    ctx.fillStyle = `rgba(255,170,90,${0.06 + 2 * bass})`; ctx.fillRect(G.x, G.y, G.w, G.h); ctx.restore();
    for (let i = 0; i < 6; i++) { const xx = G.x + 40 + i * (G.w - 80) / 5; const sg = ctx.createLinearGradient(xx - 12, 0, xx + 12, 0); sg.addColorStop(0, '#1b0f07'); sg.addColorStop(0.5, '#4a2c16'); sg.addColorStop(1, '#1b0f07'); ctx.fillStyle = sg; ctx.beginPath(); ctx.roundRect(xx - 12, G.y - 6, 24, G.h + 12, 10); ctx.fill(); }
    const fo = ctx.createLinearGradient(0, y0 + 600, 0, y0 + h + 100); fo.addColorStop(0, 'rgba(0,0,0,0)'); fo.addColorStop(1, 'rgba(0,0,0,0.6)'); ctx.fillStyle = fo; ctx.fillRect(0, y0 + 600, W, h);
  };
  // the wall clock: time going by
  const clock = (ctx, t, minutes, o = {}) => {
    wall(ctx);
    const cx = W / 2, cy = 860, R = 360;
    const lg = ctx.createRadialGradient(cx - 300, 300, 20, cx - 300, 300, 1400); lg.addColorStop(0, 'rgba(170,190,220,0.18)'); lg.addColorStop(1, 'rgba(0,0,0,0)'); ctx.fillStyle = lg; ctx.fillRect(0, 0, W, H);
    ctx.fillStyle = 'rgba(0,0,0,0.5)'; ctx.beginPath(); ctx.arc(cx + 14, cy + 20, R + 30, 0, 7); ctx.fill();
    ctx.fillStyle = '#3a2a1c'; ctx.beginPath(); ctx.arc(cx, cy, R + 30, 0, 7); ctx.fill();
    const fg = ctx.createRadialGradient(cx - 80, cy - 100, 20, cx, cy, R); fg.addColorStop(0, '#e6dcc6'); fg.addColorStop(1, '#a89b82'); ctx.fillStyle = fg; ctx.beginPath(); ctx.arc(cx, cy, R, 0, 7); ctx.fill();
    ctx.strokeStyle = '#2a2018'; for (let k = 0; k < 60; k++) { const a = k / 60 * 6.283, L = k % 5 ? 14 : 34; ctx.lineWidth = k % 5 ? 3 : 7; ctx.beginPath(); ctx.moveTo(cx + Math.cos(a) * (R - 20), cy + Math.sin(a) * (R - 20)); ctx.lineTo(cx + Math.cos(a) * (R - 20 - L), cy + Math.sin(a) * (R - 20 - L)); ctx.stroke(); }
    ctx.fillStyle = '#2a2018'; ctx.font = `bold 62px ${MONO}`; ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
    for (let k = 1; k <= 12; k++) { const a = k / 12 * 6.283 - Math.PI / 2; ctx.fillText(String(k), cx + Math.cos(a) * (R - 95), cy + Math.sin(a) * (R - 95)); }
    const hand = (a, L, wdt) => { ctx.save(); ctx.translate(cx, cy); ctx.rotate(a); ctx.fillStyle = '#16100b'; ctx.beginPath(); ctx.moveTo(-wdt, 30); ctx.lineTo(0, -L); ctx.lineTo(wdt, 30); ctx.closePath(); ctx.fill(); ctx.restore(); };
    hand(minutes / 720 * 6.283, R * 0.55, 16); hand((minutes % 60) / 60 * 6.283, R * 0.82, 10);
    const sec = (t * 6.283 / 60 * (o.fast || 1)); ctx.strokeStyle = '#a01818'; ctx.lineWidth = 3; ctx.beginPath(); ctx.moveTo(cx, cy); ctx.lineTo(cx + Math.sin(sec) * R * 0.85, cy - Math.cos(sec) * R * 0.85); ctx.stroke();
    ctx.fillStyle = '#16100b'; ctx.beginPath(); ctx.arc(cx, cy, 18, 0, 7); ctx.fill();
    ctx.save(); ctx.globalCompositeOperation = 'lighter'; ctx.fillStyle = 'rgba(255,255,255,0.06)'; ctx.beginPath(); ctx.ellipse(cx - 120, cy - 170, 170, 60, -0.5, 0, 7); ctx.fill(); ctx.restore();
  };
  // the door from inside, with a line of light under it; knocks shake it; a shadow of feet can stand in the light
  const door = (ctx, t, o = {}) => {
    wall(ctx);
    const k = Math.exp(-since(o.knocks || KNOCKS, t) * 18), sx = (K.hash(Math.floor(t * 60), 1) - 0.5) * 10 * k;
    ctx.save(); ctx.translate(sx, 0);
    ctx.fillStyle = '#070605'; ctx.fillRect(170, 120, 740, 1700);
    const dg = ctx.createLinearGradient(190, 0, 890, 0); dg.addColorStop(0, '#2c251d'); dg.addColorStop(1, '#1a1611'); ctx.fillStyle = dg; ctx.fillRect(190, 140, 700, 1660);
    ctx.strokeStyle = 'rgba(0,0,0,0.55)'; ctx.lineWidth = 6; ctx.strokeRect(250, 240, 580, 640); ctx.strokeRect(250, 960, 580, 760);
    ctx.fillStyle = '#5a4a30'; ctx.beginPath(); ctx.arc(540, 560, 20, 0, 7); ctx.fill(); ctx.fillStyle = `rgba(255,214,150,${0.55 * bulb(t)})`; ctx.beginPath(); ctx.arc(540, 560, 9, 0, 7); ctx.fill();
    ctx.fillStyle = '#6a5a3e'; ctx.fillRect(790, 900, 34, 170); ctx.fillRect(660, 922, 140, 22); ctx.fillStyle = '#1a140e'; ctx.fillRect(801, 1020, 12, 26);
    ctx.fillStyle = '#6a5a3e'; ctx.fillRect(700, 690, 170, 14); ctx.fillRect(860, 680, 26, 34); // the chain track
    for (let i = 0; i <= 8; i++) { const u = i / 8, px = K.lerp(873, 712, u), py = 697 + Math.sin(u * Math.PI) * 50; ctx.strokeStyle = '#b9a67c'; ctx.lineWidth = 3; ctx.beginPath(); ctx.ellipse(px, py, 8, 5, i % 2 ? 1.2 : 0.2, 0, 7); ctx.stroke(); }
    ctx.restore();
    const lt = (o.morning || 0), lg = ctx.createLinearGradient(0, 1795, 0, 1835); lg.addColorStop(0, `rgba(${lt ? '230,240,255' : '255,210,150'},${0.6 * (lt ? 1 : bulb(t))})`); lg.addColorStop(1, 'rgba(255,210,150,0)'); ctx.fillStyle = lg; ctx.fillRect(190, 1795, 700, 40);
    if (o.shadow) { ctx.fillStyle = 'rgba(0,0,0,0.9)'; ctx.fillRect(420 + Math.sin(t * 0.7) * 20, 1795, 90, 30); ctx.fillRect(560 + Math.sin(t * 0.7) * 20, 1795, 90, 30); }
  };
  // the chain, close: it slides a little and stops
  const chain = (ctx, t, slide = 0) => {
    ctx.fillStyle = '#1d1812'; ctx.fillRect(0, 0, W, H); const dg = ctx.createLinearGradient(0, 0, W, 0); dg.addColorStop(0, '#2c251d'); dg.addColorStop(1, '#1a1611'); ctx.fillStyle = dg; ctx.fillRect(0, 0, 820, H);
    ctx.fillStyle = '#0a0806'; ctx.fillRect(820, 0, 260, H);
    const tv = ctx.createRadialGradient(200, 600, 50, 200, 600, 1200); tv.addColorStop(0, 'rgba(255,190,120,0.18)'); tv.addColorStop(1, 'rgba(0,0,0,0)'); ctx.fillStyle = tv; ctx.fillRect(0, 0, W, H);
    ctx.fillStyle = '#7a6a48'; ctx.fillRect(120, 860, 620, 40); ctx.fillStyle = '#4a3c28'; ctx.fillRect(120, 896, 620, 8); // the track
    ctx.fillStyle = '#7a6a48'; ctx.fillRect(860, 820, 80, 120); ctx.fillStyle = '#3a2f20'; for (const yy of [845, 915]) { ctx.beginPath(); ctx.arc(900, yy, 9, 0, 7); ctx.fill(); }
    const bx = 700 - slide * 480, shake = (K.hash(Math.floor(t * 40), 2) - 0.5) * 6 * kick(t);
    for (let i = 0; i <= 10; i++) { const u = i / 10, px = K.lerp(900, bx, u), py = K.lerp(880, 880, u) + Math.sin(u * Math.PI) * (160 - slide * 60) + shake * Math.sin(u * Math.PI);
      ctx.strokeStyle = '#2a2216'; ctx.lineWidth = 12; ctx.beginPath(); ctx.ellipse(px + 2, py + 3, 24, 14, i % 2 ? 1.25 : 0.2, 0, 7); ctx.stroke();
      ctx.strokeStyle = '#c9b68a'; ctx.lineWidth = 8; ctx.beginPath(); ctx.ellipse(px, py, 24, 14, i % 2 ? 1.25 : 0.2, 0, 7); ctx.stroke(); }
    ctx.fillStyle = '#c9b68a'; ctx.beginPath(); ctx.arc(bx, 880, 20, 0, 7); ctx.fill();
  };
  // the doorway to the other room: a night light, the bars of a cot
  const doorway = (ctx, t, flick = 0) => {
    wall(ctx);
    ctx.fillStyle = '#050403'; ctx.fillRect(250, 280, 580, 1500);
    const nl = 0.75 + 0.25 * Math.sin(t * 1.7) - flick * 0.4, g = ctx.createRadialGradient(700, 1350, 10, 700, 1350, 700); g.addColorStop(0, `rgba(255,180,100,${0.5 * nl})`); g.addColorStop(1, 'rgba(255,150,60,0)');
    ctx.save(); ctx.beginPath(); ctx.rect(250, 280, 580, 1500); ctx.clip(); ctx.fillStyle = g; ctx.fillRect(250, 280, 580, 1500);
    ctx.fillStyle = `rgba(255,210,150,${0.9 * nl})`; ctx.beginPath(); ctx.arc(700, 1350, 14, 0, 7); ctx.fill();
    ctx.fillStyle = '#0b0805'; ctx.fillRect(300, 1180, 380, 16); for (let k = 0; k < 9; k++) ctx.fillRect(310 + k * 44, 1180, 9, 320); ctx.fillRect(300, 1490, 380, 16); // the cot
    ctx.restore();
    ctx.strokeStyle = '#1a140f'; ctx.lineWidth = 24; ctx.strokeRect(238, 268, 604, 1524);
  };
  // the mat in the morning, from above: a bandage roll, a bottle, a note
  const mat = (ctx, t, o = {}) => {
    ctx.fillStyle = '#2a2621'; ctx.fillRect(0, 0, W, H);
    const lg = ctx.createLinearGradient(0, 0, 0, H); lg.addColorStop(0, 'rgba(220,230,245,0.25)'); lg.addColorStop(1, 'rgba(0,0,0,0.3)'); ctx.fillStyle = lg; ctx.fillRect(0, 0, W, H);
    ctx.save(); ctx.translate(W / 2, 980); ctx.rotate(-0.04);
    ctx.fillStyle = 'rgba(0,0,0,0.45)'; ctx.fillRect(-440, -300, 900, 640); ctx.fillStyle = '#5a3d2a'; ctx.fillRect(-450, -320, 900, 640);
    ctx.strokeStyle = 'rgba(30,18,10,0.5)'; ctx.lineWidth = 3; for (let y = -300; y < 320; y += 14) { ctx.beginPath(); ctx.moveTo(-440, y); ctx.lineTo(440, y); ctx.stroke(); }
    // the note
    ctx.save(); ctx.translate(-120, 40); ctx.rotate(0.05); ctx.fillStyle = 'rgba(0,0,0,0.35)'; ctx.fillRect(-200, -110, 420, 250); ctx.fillStyle = '#e4dccb'; ctx.fillRect(-210, -125, 420, 250); ctx.fillStyle = 'rgba(0,0,0,0.08)'; ctx.fillRect(-210, -5, 420, 3);
    const nr = pr(t, o.write ?? 0, (o.write ?? 0) + 1.6); ctx.fillStyle = '#2a2a48'; ctx.font = `58px ${HAND}`; ctx.textAlign = 'left';
    const L1 = 'ребёнку. от жара.', L2 = 'три раза в день.'; ctx.fillText(L1.slice(0, Math.floor(L1.length * K.clamp(nr * 2))), -180, -45); ctx.fillText(L2.slice(0, Math.floor(L2.length * K.clamp(nr * 2 - 1))), -180, 50); ctx.restore();
    // the bandage roll
    ctx.save(); ctx.translate(250, -150); ctx.fillStyle = 'rgba(0,0,0,0.35)'; ctx.beginPath(); ctx.ellipse(10, 14, 120, 120, 0, 0, 7); ctx.fill();
    const bg = ctx.createRadialGradient(-30, -30, 10, 0, 0, 120); bg.addColorStop(0, '#fbf8f0'); bg.addColorStop(1, '#cfc8b8'); ctx.fillStyle = bg; ctx.beginPath(); ctx.arc(0, 0, 115, 0, 7); ctx.fill();
    ctx.strokeStyle = 'rgba(150,140,125,0.6)'; ctx.lineWidth = 2; for (let r = 30; r < 115; r += 9) { ctx.beginPath(); ctx.arc(0, 0, r, 0, 7); ctx.stroke(); } ctx.fillStyle = '#6a5a48'; ctx.beginPath(); ctx.arc(0, 0, 26, 0, 7); ctx.fill();
    ctx.fillStyle = '#efe9dc'; ctx.beginPath(); ctx.moveTo(100, 50); ctx.quadraticCurveTo(170, 120, 150, 230); ctx.lineTo(95, 225); ctx.quadraticCurveTo(110, 120, 60, 90); ctx.fill(); ctx.restore(); // the loose end
    // the bottle, lying
    ctx.save(); ctx.translate(250, 170); ctx.rotate(-0.5); ctx.fillStyle = 'rgba(0,0,0,0.35)'; ctx.fillRect(-150, 30, 300, 40);
    ctx.fillStyle = 'rgba(120,70,30,0.85)'; ctx.beginPath(); ctx.roundRect(-140, -50, 230, 100, 30); ctx.fill(); ctx.fillRect(90, -22, 50, 44); ctx.fillStyle = '#2a2a2a'; ctx.fillRect(138, -26, 26, 52);
    ctx.fillStyle = '#e9e1cf'; ctx.fillRect(-100, -38, 140, 76); ctx.fillStyle = '#2a2a48'; ctx.font = `30px ${HAND}`; ctx.textAlign = 'center'; ctx.fillText('от жара', -30, 10);
    ctx.fillStyle = 'rgba(255,255,255,0.35)'; ctx.fillRect(-125, -40, 200, 10); ctx.restore();
    ctx.restore();
  };

  // ================= scenes =================
  const TRUTH = ['tent', 'carry', 'coat', 'head', 'tent', 'coat', 'carry', 'head'];
  const HORROR = ['face', 'door', 'radio', 'under', 'face', 'door', 'radio', 'face'];
  const SCENES = [
    { a: 0, b: 4.0, draw(ctx, t, frame) { door(ctx, t); sub(ctx, '— кто там?', t, 1.8, 4.0); } },
    { a: 4.0, b: 7.3, draw(ctx, t, frame) { const k = Math.exp(-since(KNOCKS, t) * 18); peephole(ctx, t, x => atDoor(x, t), { dx: (K.hash(frame, 1) - 0.5) * 12 * k }); } },
    { a: 7.3, b: 10.0, draw(ctx, t) { radio(ctx, t, 1); radioSays(ctx, 'ночью дверь не открывать.', t, 7.5, 10.0); } },
    { a: 10.0, b: bar(4), draw(ctx, t) { peephole(ctx, t, x => atDoor(x, t)); sub(ctx, '— молчит.', t, 10.1, bar(4)); } },
    { a: bar(4), b: bar(5), draw(ctx, t) { radio(ctx, t, 1); radioSays(ctx, 'они выглядят как люди.', t, bar(4) + 0.1, bar(5)); } },
    { a: bar(5), b: bar(6), draw(ctx, t) { peephole(ctx, t, x => atDoor(x, t), { zoom: 0.62 + 0.05 * pr(t, bar(5), bar(6)) }); sub(ctx, '— похож.', t, bar(5) + 0.3, bar(6)); } },
    { a: bar(6), b: bar(7), draw(ctx, t) { radio(ctx, t, 1); radioSays(ctx, 'смотрите на лицо.', t, bar(6) + 0.1, bar(7)); } },
    { a: bar(7), b: bar(8), draw(ctx, t) { // the lens goes to his face
      const z = es(t, bar(7), bar(7) + 1.2);
      if (z < 0.6) peephole(ctx, t, x => atDoor(x, t, { z: 1 + z * 1.4, lift: z * 300 }), { zoom: 0.67 + 0.2 * z });
      else peephole(ctx, t, x => lensPhoto(x, 'face', { z: 1.05, fy: 0.32 }), { zoom: 0.8, tint: 'cold' });
      sub(ctx, '— у него…\nчто-то с лицом.', t, bar(7) + 0.4, bar(8));
    } },
    { a: bar(8), b: bar(16), draw(ctx, t, frame) { // fear and truth, half a bar each
      const n = Math.floor((t - bar(8)) / BAR), half = (t - bar(8)) % BAR >= BAR / 2, kk = kick(t);
      if (!half) {
        const h = HORROR[n % HORROR.length];
        if (h === 'face') peephole(ctx, t, x => lensPhoto(x, 'face', { z: 1.25 + 0.15 * Math.sin(t * 3), fy: 0.36 }), { zoom: 0.85 + 0.08 * kk, tint: 'cold', dx: (K.hash(frame, 3) - 0.5) * 16 * kk });
        else if (h === 'door') door(ctx, t, { knocks: KICKS });
        else if (h === 'radio') { radio(ctx, t, 1, { static: 0.5 }); radioSays(ctx, 'не открывайте.', t, bar(8 + n), bar(8 + n) + BAR / 2); }
        else { door(ctx, t, { shadow: 1 }); }
      } else peephole(ctx, t, x => lensPhoto(x, TRUTH[n % TRUTH.length], { z: 1.08 + 0.04 * pr(t, bar(8 + n) + BAR / 2, bar(9 + n)) }), { zoom: 0.78, tint: 'warm' });
      if (kk > 0.6) { ctx.fillStyle = `rgba(255,255,255,${0.05 * kk})`; ctx.fillRect(0, 0, W, H); }
    } },
    { a: bar(16), b: bar(17), draw(ctx, t) { radio(ctx, t, 1); radioSays(ctx, 'если сомневаетесь —\nне открывайте.', t, bar(16) + 0.1, bar(17)); } },
    { a: bar(17), b: bar(25), draw(ctx, t, frame) { // the knocking stops; the night goes by
      const n = Math.floor((t - bar(17)) / BAR), mins = 190 + 160 * pr(t, bar(17), bar(25));
      const shot = ['doorway', 'clock', 'lens', 'clock', 'doorway', 'radio', 'lens', 'clock'][n];
      if (shot === 'doorway') doorway(ctx, t, Math.exp(-since(COUGHS, t) * 4));
      else if (shot === 'clock') clock(ctx, t, mins);
      else if (shot === 'lens') peephole(ctx, t, x => stairwell(x, t), {});
      else radio(ctx, t, 1, { needle: 0.02 });
      sub(ctx, '— спи. спи.', t, bar(17) + 0.4, bar(18));
      sub(ctx, '— ушёл?', t, bar(19) + 0.4, bar(20));
      if (shot === 'radio') radioSays(ctx, 'не открывайте до утра.', t, bar(22) + 0.1, bar(23));
    } },
    { a: bar(25), b: STOP, draw(ctx, t, frame) { // the chain, the clock, the cough, the radio — then morning
      const n = Math.floor((t - bar(25)) / (BAR / 2)), shot = ['chain', 'clock', 'doorway', 'chain', 'radio', 'clock', 'doorway', 'chain'][n % 8];
      const mins = 350 + 40 * pr(t, bar(25), bar(31)), morning = pr(t, bar(31), bar(32));
      if (t >= bar(32)) { // the chain comes off, the door opens on daylight
        const d = t - bar(32);
        if (d < 1.0) chain(ctx, t, es(d, 0, 0.8)); else { door(ctx, t, { morning: 1 }); const o = es(d, 1.0, 2.2); ctx.fillStyle = `rgba(235,242,255,${o})`; ctx.fillRect(190 + 700 * (1 - o) * 0, 140, 700 * o, 1660); }
      } else if (t >= bar(31)) { radio(ctx, t, 1); radioSays(ctx, '…утро.\nдвери можно открывать.', t, bar(31) + 0.1, bar(32)); }
      else if (shot === 'chain') chain(ctx, t, 0.12 * Math.max(0, Math.sin((t - bar(25)) * 2.2)));
      else if (shot === 'clock') clock(ctx, t, mins, { fast: 30 });
      else if (shot === 'doorway') doorway(ctx, t, Math.exp(-since(COUGHS, t) * 4));
      else radio(ctx, t, 0.4, { static: 0.35 });
      if (morning > 0 && t < bar(32)) { ctx.fillStyle = `rgba(200,215,240,${0.12 * morning})`; ctx.fillRect(0, 0, W, H); }
    } },
    { a: STOP, b: END, draw(ctx, t) { // silence: what was left on the mat
      mat(ctx, t, { write: STOP + 0.3 });
      sub(ctx, '(из комнаты больше не кашляли)', t, STOP + 3.0, END - 0.1, { y: 1720, size: 58, color: RED, cps: 22 });
      if (t < STOP + 0.25) { ctx.fillStyle = `rgba(255,255,255,${1 - (t - STOP) / 0.25})`; ctx.fillRect(0, 0, W, H); }
    } },
  ];
  const CUTS = [4.0, 7.3, 10.0, bar(4), bar(5), bar(6), bar(7), ...Array.from({ length: 16 }, (_, i) => bar(8) + i * BAR / 2), bar(16), bar(17), bar(25), bar(31), bar(32)];

  window.VERSION = {
    duration: END,
    images: { door: '/assets/images/kostya/door.png', face: '/video/assets/up/c_portrait_mask.jpg', tent: '/assets/images/medic-tent.png', carry: '/video/assets/up/c_carry.jpg', coat: '/video/assets/up/c_white_coat.jpg', head: '/video/assets/up/c_head_down.jpg' },
    render(ctx, t, frame) {
      const sc = SCENES.find(s => t >= s.a && t < s.b) || SCENES[SCENES.length - 1];
      ctx.save(); ctx.setTransform(1, 0, 0, 1, 0, 0); ctx.globalAlpha = 1; ctx.globalCompositeOperation = 'source-over'; ctx.filter = 'none';
      ctx.fillStyle = '#000'; ctx.fillRect(0, 0, W, H);
      sc.draw(ctx, t, frame);
      ctx.restore();
      if (since(CUTS, t) < 0.05 && t < STOP) { ctx.fillStyle = 'rgba(255,255,255,0.1)'; ctx.fillRect(0, 0, W, H); }
      K.grain(ctx, frame, t < STOP ? 0.09 : 0.05); K.vignette(ctx, 0.55);
      if (t < 0.6) { ctx.fillStyle = `rgba(0,0,0,${1 - t / 0.6})`; ctx.fillRect(0, 0, W, H); }
    },
  };
})();
