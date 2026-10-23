// Version Q — «Письмо». One room, nights. Kostya writes to his mother: every true line is crossed out and the calm one is
// written over it (the letter is the user's: «Со мною всё хорошо… Я здесь не один…»). He sends it. Then the nights:
// knocks, the radio, the slot in the door stays empty. In the silence after the song: «(ответа не было)».
// Only objects, paper, light and text. Song: «Credits» (No, I'm Not a Human OST), 87 bpm, stop at 91.95.
(function () {
  const { W, H } = K;
  const pr = (t, a, b) => K.prog(t, a, b);
  const es = (t, a, b) => K.ease.inOutSine(K.prog(t, a, b));
  const win = (t, a, b) => t >= a && t < b;
  const since = (list, t) => { let b = 1e9; for (const k of list) if (k <= t) b = Math.min(b, t - k); return b; };
  const MONO = '"PT Mono"', HAND = 'Caveat';
  const RADIO = '#f0b75c', RED = '#e0242f', INK = '#1f2340';

  const BEAT = 0.6896, BAR = BEAT * 4, bar = n => 0.937 + BAR * n;
  const STOP = 91.95, END = 97.5;
  const KICKS = []; for (let n = 8; n < 33; n++) if (n !== 16) KICKS.push(bar(n), bar(n) + 2 * BEAT);
  const kick = t => Math.exp(-since(KICKS, t) * 8);
  const KNOCKS = [12.0, 12.3, 12.6, ...Array.from({ length: 8 }, (_, i) => bar(17 + i) + 0.3), ...KICKS.filter(k => k > bar(25))];

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

  // ================= the room, as one fixed frame =================
  const night = t => { // which night it is
    if (t < bar(17)) return 1;
    if (t < bar(25)) return [2, 5, 9, 16, 30, 47, 70, 99][Math.min(7, Math.floor((t - bar(17)) / BAR))];
    if (t < STOP) return Math.floor(100 + 630 * Math.pow(pr(t, bar(25), STOP), 1.6));
    return 731;
  };
  const counter = (ctx, t, a = 1) => { ctx.save(); ctx.globalAlpha = a; ctx.font = `44px ${MONO}`; ctx.textAlign = 'left'; ctx.fillStyle = 'rgba(0,0,0,0.6)'; ctx.fillText(`ночь ${night(t)}`, 63, 103); ctx.fillStyle = '#d9d1c2'; ctx.fillText(`ночь ${night(t)}`, 60, 100); ctx.restore(); };
  const lampOn = t => { if (t < 1.0) return 0; if (t < 1.4) return K.hash(Math.floor(t * 20), 1) > 0.4 ? 1 : 0.2; const f = pr(t, bar(25), STOP); return K.hash(Math.floor(t * (8 + 24 * f)), 3) > 0.97 - 0.25 * f ? 0.35 : 1; };
  const weather = (ctx, t, x, y, w, h) => { // rain through the first nights, snow later
    const snow = night(t) > 40, r = K.rng(4);
    for (let i = 0; i < 90; i++) { const px = x + (r() * w + (snow ? Math.sin(t + i) * 20 : -t * 60)) % w, py = y + ((r() * h + t * (snow ? 50 : 900) * (0.6 + r() * 0.6)) % h);
      ctx.strokeStyle = snow ? 'rgba(230,236,250,0.8)' : 'rgba(170,190,220,0.35)'; ctx.fillStyle = 'rgba(230,236,250,0.8)';
      if (snow) { ctx.beginPath(); ctx.arc(px < x ? px + w : px, py, 2.5, 0, 7); ctx.fill(); } else { ctx.lineWidth = 2; ctx.beginPath(); ctx.moveTo(px, py); ctx.lineTo(px - 4, py + 26); ctx.stroke(); } }
  };
  const room = (ctx, t, o = {}) => {
    const L = lampOn(t), z = 1 + 0.06 * (o.push || 0);
    ctx.save(); ctx.translate(W / 2, 1150); ctx.scale(z, z); ctx.translate(-W / 2, -1150);
    wall(ctx);
    // the window: night over roofs, rain or snow
    const WN = { x: 230, y: 200, w: 620, h: 560 };
    ctx.save(); ctx.beginPath(); ctx.rect(WN.x, WN.y, WN.w, WN.h); ctx.clip();
    const sky = ctx.createLinearGradient(0, WN.y, 0, WN.y + WN.h); sky.addColorStop(0, '#0a1220'); sky.addColorStop(1, '#16202e'); ctx.fillStyle = sky; ctx.fillRect(WN.x, WN.y, WN.w, WN.h);
    ctx.fillStyle = '#06090e'; ctx.beginPath(); ctx.moveTo(WN.x, WN.y + WN.h); [[0, 380], [90, 380], [90, 330], [200, 330], [240, 290], [280, 330], [380, 330], [380, 360], [500, 360], [500, 310], [620, 310], [620, 560]].forEach(([a, b]) => ctx.lineTo(WN.x + a, WN.y + b)); ctx.closePath(); ctx.fill();
    for (const [a, b] of [[130, 400], [430, 410], [540, 380]]) if (K.hash(a, Math.floor(t / 7)) > 0.4) { ctx.fillStyle = 'rgba(255,200,120,0.5)'; ctx.fillRect(WN.x + a, WN.y + b, 18, 24); }
    weather(ctx, t, WN.x, WN.y, WN.w, WN.h); ctx.restore();
    ctx.fillStyle = '#0c0907'; ctx.fillRect(WN.x - 20, WN.y - 20, 20, WN.h + 40); ctx.fillRect(WN.x + WN.w, WN.y - 20, 20, WN.h + 40); ctx.fillRect(WN.x - 20, WN.y - 20, WN.w + 40, 20); ctx.fillRect(WN.x + WN.w / 2 - 7, WN.y, 14, WN.h); ctx.fillRect(WN.x - 40, WN.y + WN.h, WN.w + 80, 24);
    // the door on the right, a brass slot in it
    ctx.fillStyle = '#1e1813'; ctx.fillRect(880, 520, 260, 1300); ctx.fillStyle = 'rgba(0,0,0,0.5)'; ctx.fillRect(880, 520, 12, 1300);
    ctx.fillStyle = '#7a6440'; ctx.fillRect(930, 1180, 130, 34); ctx.fillStyle = '#1a140e'; ctx.fillRect(940, 1190, 110, 12);
    const k = Math.exp(-since(KNOCKS, t) * 14); if (k > 0.05) { ctx.save(); ctx.globalAlpha = k; ctx.fillStyle = '#1e1813'; ctx.fillRect(880 + (K.hash(Math.floor(t * 50), 2) - 0.5) * 8, 520, 260, 1300); ctx.fillStyle = '#7a6440'; ctx.fillRect(930, 1180, 130, 34); ctx.restore(); }
    // the desk, the lamp, the paper, the radio
    ctx.fillStyle = '#24170e'; ctx.fillRect(0, 1360, W, 560); ctx.fillStyle = 'rgba(255,200,140,0.08)'; ctx.fillRect(0, 1360, W, 4);
    const lg = ctx.createRadialGradient(250, 1300, 20, 250, 1300, 1000); lg.addColorStop(0, `rgba(255,190,120,${0.45 * L})`); lg.addColorStop(1, 'rgba(0,0,0,0)'); ctx.fillStyle = lg; ctx.fillRect(0, 0, W, H);
    ctx.fillStyle = '#15100b'; ctx.fillRect(235, 1130, 22, 240); ctx.beginPath(); ctx.ellipse(246, 1372, 70, 16, 0, 0, 7); ctx.fill();
    ctx.fillStyle = '#2a2018'; ctx.beginPath(); ctx.moveTo(150, 1140); ctx.lineTo(340, 1140); ctx.lineTo(290, 1060); ctx.lineTo(200, 1060); ctx.closePath(); ctx.fill();
    ctx.fillStyle = `rgba(255,220,160,${0.9 * L})`; ctx.fillRect(170, 1138, 150, 6);
    const pile = o.pile || 0; // drafts piling up on the desk
    for (let i = 0; i < pile; i++) { ctx.save(); ctx.translate(520 + K.hash(i, 1) * 120 - 60, 1500 - i * 3); ctx.rotate((K.hash(i, 2) - 0.5) * 0.5); ctx.fillStyle = `rgb(${200 + (i % 3) * 8},${192 + (i % 3) * 8},${176 + (i % 3) * 8})`; ctx.fillRect(-170, -110, 340, 220); ctx.restore(); }
    ctx.save(); ctx.translate(520, 1500); ctx.rotate(-0.05); ctx.fillStyle = '#e8e0cf'; ctx.fillRect(-170, -110, 340, 220); ctx.strokeStyle = 'rgba(31,35,64,0.5)'; ctx.lineWidth = 2; for (let l = 0; l < 6; l++) { ctx.beginPath(); for (let x = 0; x < 260; x += 4) ctx.lineTo(-140 + x, -80 + l * 30 + Math.sin(x * 0.5 + l) * 2); ctx.stroke(); } ctx.restore();
    ctx.fillStyle = '#3a2412'; ctx.beginPath(); ctx.roundRect(760, 1200, 150, 170, 14); ctx.fill(); ctx.fillStyle = `rgba(255,180,90,${0.55 + 0.2 * Math.sin(t * 3)})`; ctx.fillRect(775, 1215, 120, 50); ctx.fillStyle = 'rgba(80,255,150,0.7)'; ctx.beginPath(); ctx.arc(835, 1300, 14, 0, 7); ctx.fill();
    ctx.restore();
    ctx.fillStyle = `rgba(0,0,0,${0.75 - 0.6 * L})`; ctx.fillRect(0, 0, W, H);
    counter(ctx, t);
  };
  // the sheet, close: lines written in his hand; a line can be crossed out
  const sheet = (ctx, t, lines, o = {}) => {
    ctx.fillStyle = '#1a120c'; ctx.fillRect(0, 0, W, H);
    const L = lampOn(t), sc = o.scroll || 0, z = 1 + 0.04 * (o.push || 0);
    ctx.save(); ctx.translate(W / 2, H / 2 - sc); ctx.scale(z, z); ctx.rotate(-0.02 + 0.01 * (o.push || 0));
    ctx.fillStyle = 'rgba(0,0,0,0.5)'; ctx.fillRect(-470, -880, 960, 2400);
    const pg = ctx.createLinearGradient(-480, -900, 480, 900); pg.addColorStop(0, '#eee6d4'); pg.addColorStop(1, '#d6ccb6'); ctx.fillStyle = pg; ctx.fillRect(-480, -900, 960, 2400);
    ctx.strokeStyle = 'rgba(60,90,140,0.15)'; ctx.lineWidth = 2; for (let y = -780; y < 1500; y += 92) { ctx.beginPath(); ctx.moveTo(-430, y + 22); ctx.lineTo(430, y + 22); ctx.stroke(); }
    ctx.font = `66px ${HAND}`; ctx.textAlign = 'left'; ctx.textBaseline = 'alphabetic';
    for (const ln of lines) {
      if (t < ln.t) continue;
      const n = Math.floor((t - ln.t) * (ln.cps || 20)), s = ln.s.slice(0, n), y = ln.y;
      ctx.fillStyle = INK; ctx.globalAlpha = ln.dim ?? 1; ctx.fillText(s, -420 + (ln.x || 0), y); ctx.globalAlpha = 1;
      if (ln.x2 !== undefined && t > ln.x2) { // crossed out, twice, by a hurried pen
        const w = ctx.measureText(ln.s).width, q = es(t, ln.x2, ln.x2 + 0.3);
        ctx.strokeStyle = INK; ctx.lineCap = 'round'; ctx.globalAlpha = ln.dim ?? 1;
        for (const [dy, lw, tilt] of [[-20, 4, -0.012], [-13, 3, 0.01]]) { ctx.lineWidth = lw; ctx.beginPath(); for (let x = -10; x <= (w + 20) * q; x += 12) ctx.lineTo(-420 + (ln.x || 0) + x, y + dy + x * tilt + Math.sin(x * 0.05 + dy) * 1.5); ctx.stroke(); }
        ctx.globalAlpha = 1;
      }
    }
    ctx.restore();
    const lg = ctx.createRadialGradient(150, 300, 50, 150, 300, 1600); lg.addColorStop(0, 'rgba(255,200,130,0.18)'); lg.addColorStop(1, 'rgba(0,0,0,0.45)'); ctx.fillStyle = lg; ctx.fillRect(0, 0, W, H);
    ctx.fillStyle = `rgba(0,0,0,${0.7 - 0.7 * L})`; ctx.fillRect(0, 0, W, H);
    counter(ctx, t, 0.8);
  };
  // the slot in the door, close: light from the landing through it; it rattles at a knock and stays empty
  const slot = (ctx, t) => {
    ctx.fillStyle = '#1e1813'; ctx.fillRect(0, 0, W, H);
    const dg = ctx.createLinearGradient(0, 0, W, 0); dg.addColorStop(0, '#2a221a'); dg.addColorStop(1, '#17120d'); ctx.fillStyle = dg; ctx.fillRect(0, 0, W, H);
    ctx.strokeStyle = 'rgba(0,0,0,0.4)'; ctx.lineWidth = 6; ctx.strokeRect(120, 240, 840, 1400);
    const k = Math.exp(-since(KNOCKS, t) * 12), sh = (K.hash(Math.floor(t * 60), 4) - 0.5) * 12 * k;
    ctx.save(); ctx.translate(W / 2 + sh, 960);
    ctx.fillStyle = '#5a4528'; ctx.beginPath(); ctx.roundRect(-330, -110, 660, 220, 20); ctx.fill();
    ctx.fillStyle = '#08060a'; ctx.fillRect(-260, -36, 520, 72);
    const lt = 0.55 + 0.45 * Math.sin(t * 0.8); ctx.fillStyle = `rgba(255,214,150,${0.35 * lt})`; ctx.fillRect(-258, -10, 516, 8);
    const g = ctx.createLinearGradient(0, -110, 0, 30); g.addColorStop(0, '#c8a868'); g.addColorStop(1, '#7a6036'); ctx.fillStyle = g; ctx.save(); ctx.rotate(0); ctx.translate(0, -36); ctx.scale(1, 1 - 0.25 * k); ctx.beginPath(); ctx.roundRect(-280, 0, 560, 80, 10); ctx.fill(); ctx.restore();
    ctx.fillStyle = '#3a2c18'; ctx.font = `bold 46px ${MONO}`; ctx.textAlign = 'center'; ctx.fillText('ПИСЬМА', 0, 22);
    ctx.restore();
    const lg = ctx.createRadialGradient(W / 2, 960, 50, W / 2, 960, 900); lg.addColorStop(0, 'rgba(255,200,130,0.12)'); lg.addColorStop(1, 'rgba(0,0,0,0.55)'); ctx.fillStyle = lg; ctx.fillRect(0, 0, W, H);
    ctx.fillStyle = '#0d0a08'; ctx.fillRect(0, 1800, W, 120); // the floor under the slot: nothing on it
    counter(ctx, t);
  };
  // the envelope: folded letter in, «маме» on it, sealed
  const envelope = (ctx, t, t0) => {
    ctx.fillStyle = '#1a120c'; ctx.fillRect(0, 0, W, H);
    const d = t - t0, fold = es(d, 0, 0.8), close = es(d, 0.9, 1.5);
    ctx.save(); ctx.translate(W / 2, 960); ctx.rotate(-0.04);
    ctx.fillStyle = 'rgba(0,0,0,0.5)'; ctx.fillRect(-400, -230, 820, 500);
    ctx.fillStyle = '#d9c9a4'; ctx.fillRect(-410, -250, 820, 500);
    if (fold < 1) { ctx.fillStyle = '#eee6d4'; ctx.fillRect(-300, -250 - 400 * (1 - fold), 600, 400 * (1 - fold) + 60); }
    ctx.fillStyle = '#cbb990'; ctx.beginPath(); ctx.moveTo(-410, -250); ctx.lineTo(0, -250 + 260 * close); ctx.lineTo(410, -250); ctx.closePath(); ctx.fill();
    ctx.strokeStyle = 'rgba(80,60,30,0.4)'; ctx.lineWidth = 3; ctx.beginPath(); ctx.moveTo(-410, 250); ctx.lineTo(0, 20); ctx.lineTo(410, 250); ctx.stroke();
    if (d > 1.5) { ctx.fillStyle = INK; ctx.font = `92px ${HAND}`; ctx.textAlign = 'center'; ctx.fillText('маме'.slice(0, Math.floor((d - 1.5) * 8)), 0, 160); }
    ctx.restore();
    const lg = ctx.createRadialGradient(150, 300, 50, 150, 300, 1600); lg.addColorStop(0, 'rgba(255,200,130,0.18)'); lg.addColorStop(1, 'rgba(0,0,0,0.45)'); ctx.fillStyle = lg; ctx.fillRect(0, 0, W, H);
    counter(ctx, t, 0.8);
  };
  const windowClose = (ctx, t) => {
    ctx.fillStyle = '#0c0907'; ctx.fillRect(0, 0, W, H);
    const WN = { x: 80, y: 260, w: 920, h: 1300 };
    ctx.save(); ctx.beginPath(); ctx.rect(WN.x, WN.y, WN.w, WN.h); ctx.clip();
    const sky = ctx.createLinearGradient(0, WN.y, 0, WN.y + WN.h); sky.addColorStop(0, '#0a1220'); sky.addColorStop(1, '#1b2636'); ctx.fillStyle = sky; ctx.fillRect(WN.x, WN.y, WN.w, WN.h);
    ctx.fillStyle = '#06090e'; ctx.beginPath(); ctx.moveTo(WN.x, WN.y + WN.h); [[0, 900], [200, 900], [200, 800], [420, 800], [480, 720], [540, 800], [700, 800], [700, 860], [920, 860], [920, 1300]].forEach(([a, b]) => ctx.lineTo(WN.x + a, WN.y + b)); ctx.closePath(); ctx.fill();
    weather(ctx, t, WN.x, WN.y, WN.w, WN.h); ctx.restore();
    ctx.fillStyle = '#0c0907'; ctx.fillRect(WN.x + WN.w / 2 - 12, WN.y, 24, WN.h); ctx.fillRect(WN.x, WN.y + WN.h * 0.45, WN.w, 18);
    ctx.save(); ctx.globalCompositeOperation = 'lighter'; const r = ctx.createRadialGradient(300, 1700, 30, 300, 1700, 900); r.addColorStop(0, 'rgba(255,190,120,0.12)'); r.addColorStop(1, 'rgba(0,0,0,0)'); ctx.fillStyle = r; ctx.fillRect(0, 0, W, H); ctx.restore();
    counter(ctx, t);
  };
  const drafts = (ctx, t, n) => { // the crossed-out sheets, filling the frame
    ctx.fillStyle = '#1a120c'; ctx.fillRect(0, 0, W, H);
    const D = ['меня ранило.', 'я не могу говорить.', 'я совсем один.', 'мама, я не похож на себя.', 'мне страшно.', 'я не знаю, где ты.'];
    for (let i = 0; i < n; i++) {
      ctx.save(); ctx.translate(K.hash(i, 1) * W, 200 + K.hash(i, 2) * 1500); ctx.rotate((K.hash(i, 3) - 0.5) * 0.8);
      ctx.fillStyle = 'rgba(0,0,0,0.4)'; ctx.fillRect(-300, -180, 620, 380); ctx.fillStyle = `rgb(${226 - (i % 4) * 6},${218 - (i % 4) * 6},${200 - (i % 4) * 6})`; ctx.fillRect(-310, -190, 620, 380);
      const s = D[i % D.length]; ctx.font = `56px ${HAND}`; ctx.fillStyle = INK; ctx.textAlign = 'left'; ctx.fillText(s, -270, -60); const w = ctx.measureText(s).width;
      ctx.strokeStyle = INK; ctx.lineCap = 'round'; ctx.lineWidth = 4; ctx.beginPath(); ctx.moveTo(-280, -80); ctx.lineTo(-260 + w, -86); ctx.stroke(); ctx.lineWidth = 3; ctx.beginPath(); ctx.moveTo(-280, -70); ctx.lineTo(-260 + w, -66); ctx.stroke();
      ctx.restore();
    }
    const lg = ctx.createRadialGradient(W / 2, 900, 100, W / 2, 900, 1300); lg.addColorStop(0, 'rgba(0,0,0,0)'); lg.addColorStop(1, 'rgba(0,0,0,0.6)'); ctx.fillStyle = lg; ctx.fillRect(0, 0, W, H);
    counter(ctx, t);
  };
  const sub = (ctx, s, t, t0, t1, o = {}) => {
    if (t < t0 || t > t1) return;
    const str = s.slice(0, Math.floor((t - t0) * (o.cps || 24))), a = 1 - pr(t, t1 - 0.3, t1);
    ctx.save(); ctx.font = `${o.size || 56}px ${MONO}`; ctx.textAlign = 'center'; ctx.globalAlpha = a; ctx.lineWidth = 8; ctx.strokeStyle = 'rgba(0,0,0,0.85)'; ctx.lineJoin = 'round';
    str.split('\n').forEach((l, i) => { const y = (o.y || 1720) + i * (o.size || 56) * 1.25, jx = o.jitter ? (K.hash(i, Math.floor(t * 18)) - 0.5) * o.jitter : 0; ctx.strokeText(l, W / 2 + jx, y); ctx.fillStyle = o.color || '#ece6da'; ctx.fillText(l, W / 2 + jx, y); });
    ctx.restore();
  };
  const radioSays = (ctx, s, t, t0, t1) => sub(ctx, s, t, t0, t1, { color: RADIO, jitter: 3, cps: 22 });

  // ================= the letter =================
  const Y = i => -560 + i * 92;
  const DRAFT = [ // the first night: the truth crossed out, the calm line written over
    { s: 'Здравствуй, милая моя мама.', t: 4.6, y: Y(0) },
    { s: 'меня ранило.', t: 8.0, y: Y(1), x2: 9.4 }, { s: 'Со мною всё хорошо.', t: 9.9, y: Y(2) },
    { s: 'я не могу говорить.', t: bar(5) + 0.1, y: Y(3), x2: bar(5) + 1.5 }, { s: 'Не волнуйся за меня.', t: bar(5) + 2.0, y: Y(4) },
    { s: 'я совсем один.', t: bar(6) + 1.4, y: Y(5), x2: bar(6) + 2.6 }, { s: 'Я здесь не один.', t: bar(7) + 0.6, y: Y(6) },
  ];
  const CLEAN = [
    'Войну пережил, надеюсь,', 'что ты жива и здорова.', 'Сейчас живу в Берлине,', 'работаю в больнице.', 'Слово, которое я дал тебе', 'перед уходом, я сохранил.', 'Рядом со мной хорошие,', 'добрые люди.',
    'Как там Артём и Андрей?', 'Очень скучаю по нашим посиделкам', 'за кружкой чёрного горячего чая.', 'Мы обязательно увидимся', 'и сядем рядом, как прежде.', 'Целую тебя крепко-крепко.', 'Твой сын Костя.',
  ].map((s, i) => ({ s, t: bar(8) + i * BAR * 8 / 16 + 0.05, y: Y(7 + i), cps: 26 }));

  // ================= scenes =================
  const SCENES = [
    { a: 0, b: 4.4, draw(ctx, t) { room(ctx, t, { push: es(t, 0, 4.4) }); } },
    { a: 4.4, b: 12.0, draw(ctx, t) { sheet(ctx, t, DRAFT, { push: pr(t, 4.4, 12.0) }); } },
    { a: 12.0, b: 13.0, draw(ctx, t) { room(ctx, t, { push: 1 + 0.5 * pr(t, 12, 13) }); } },
    { a: 13.0, b: bar(5), draw(ctx, t) { radio(ctx, t, 1); radioSays(ctx, 'не открывайте.', t, 13.1, bar(5)); } },
    { a: bar(5), b: bar(8), draw(ctx, t) { sheet(ctx, t, DRAFT, { push: 1 + pr(t, bar(5), bar(8)) }); } },
    { a: bar(8), b: bar(16), draw(ctx, t) { // the letter, clean, on the beat; the paper scrolls with it
      const f = K.clamp((t - bar(8)) / (BAR / 2), 0, 14.5), kk = kick(t);
      const scroll = Math.max(0, (7 + f) * 92 - 760);
      ctx.save(); ctx.translate(W / 2, H / 2); ctx.scale(1 + 0.012 * kk, 1 + 0.012 * kk); ctx.translate(-W / 2, -H / 2);
      sheet(ctx, t, [...DRAFT.map(l => ({ ...l, dim: 0.5 })), ...CLEAN], { scroll, push: 2 });
      ctx.restore();
    } },
    { a: bar(16), b: bar(17), draw(ctx, t) { envelope(ctx, t, bar(16)); } },
    { a: bar(17), b: bar(25), draw(ctx, t) { // the nights: the room, the slot, the radio, the window
      const n = Math.floor((t - bar(17)) / BAR), shot = ['room', 'slot', 'window', 'room', 'radio', 'slot', 'window', 'room'][n];
      if (shot === 'room') room(ctx, t, { pile: n, push: es(t, bar(17 + n), bar(18 + n)) });
      else if (shot === 'slot') slot(ctx, t);
      else if (shot === 'window') windowClose(ctx, t);
      else { radio(ctx, t, 1, { static: 0.3 }); radioSays(ctx, 'ночью дверь не открывать.', t, bar(17 + n) + 0.1, bar(18 + n)); }
    } },
    { a: bar(25), b: STOP, draw(ctx, t, frame) { // faster and faster
      const p = pr(t, bar(25), STOP), step = p < 0.5 ? BAR / 2 : BEAT, n = Math.floor((t - bar(25)) / step), shot = ['slot', 'drafts', 'room', 'radio', 'window', 'drafts', 'slot', 'room'][n % 8], kk = kick(t);
      ctx.save(); ctx.translate(W / 2, H / 2); ctx.scale(1 + 0.02 * kk, 1 + 0.02 * kk); ctx.translate(-W / 2, -H / 2);
      if (shot === 'slot') slot(ctx, t);
      else if (shot === 'drafts') drafts(ctx, t, 4 + Math.floor(p * 30));
      else if (shot === 'room') room(ctx, t, { pile: 8 + Math.floor(p * 20) });
      else if (shot === 'radio') radio(ctx, t, 1, { static: 0.6 });
      else windowClose(ctx, t);
      ctx.restore();
    } },
    { a: STOP, b: END, draw(ctx, t) { // silence
      room(ctx, t, { pile: 30, push: 0.6 * pr(t, STOP, END) });
      ctx.fillStyle = 'rgba(0,0,0,0.35)'; ctx.fillRect(0, 0, W, H);
      sub(ctx, '(ответа не было)', t, STOP + 0.9, END - 0.1, { y: 960, size: 74, color: RED, cps: 18 });
    } },
  ];
  const CUTS = [4.4, 12.0, 13.0, bar(5), bar(8), bar(16), ...Array.from({ length: 8 }, (_, i) => bar(17 + i))];

  window.VERSION = {
    duration: END,
    images: {},
    render(ctx, t, frame) {
      const sc = SCENES.find(s => t >= s.a && t < s.b) || SCENES[SCENES.length - 1];
      ctx.save(); ctx.setTransform(1, 0, 0, 1, 0, 0); ctx.globalAlpha = 1; ctx.globalCompositeOperation = 'source-over'; ctx.filter = 'none';
      ctx.fillStyle = '#000'; ctx.fillRect(0, 0, W, H);
      sc.draw(ctx, t, frame);
      ctx.restore();
      if (since(CUTS, t) < 0.05 && t < STOP) { ctx.fillStyle = 'rgba(255,255,255,0.08)'; ctx.fillRect(0, 0, W, H); }
      K.grain(ctx, frame, 0.09); K.vignette(ctx, 0.55);
      if (t < 0.6) { ctx.fillStyle = `rgba(0,0,0,${1 - t / 0.6})`; ctx.fillRect(0, 0, W, H); }
    },
  };
})();
