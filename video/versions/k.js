// Version K — «Приёмник» (full). Night, a valve radio on a desk; the song plays through it.
// Stations are pieces of the song; pictures arrive through the static.
// Music: Кино — «Спокойная ночь», re-cut by station (audio/radio_edit.py) and mixed through the radio (audio/radio_mix.py).
(function () {
  const { W, H } = K;
  const I = n => K.images[n];
  const A = window.AUDIO || { fps: 30, rms: [], bass: [], hi: [], onset: [] };
  const env = (k, t) => { const a = A[k], i = Math.floor(t * A.fps); return a && a.length ? a[Math.max(0, Math.min(a.length - 1, i))] : 0; };
  const envS = (k, t, w = 0.25) => { let s = 0, n = 0; for (let d = -w; d <= w; d += 1 / A.fps) { s += env(k, t + d); n++; } return s / n; };
  const e = (t, a, b) => K.ease.inOutSine(K.prog(t, a, b));
  const mix = (a, b, p) => a.map((v, i) => Math.round(v + (b[i] - v) * p));
  const rgb = (c, a = 1) => `rgba(${c[0]},${c[1]},${c[2]},${a})`;
  const rr = (ctx, x, y, w, h, r) => { ctx.beginPath(); ctx.roundRect(x, y, w, h, r); };

  // ---------------- geometry (radio space = 1080x1920) ----------------
  const CAB = { x: 50, y: 300, w: 980, h: 1500 };
  const DIAL = { x: 110, y: 390, w: 860, h: 520 };
  const EYE = { x: 540, y: 1068, r: 74 };
  const KNOB_L = { x: 220, y: 1068 }, KNOB_R = { x: 860, y: 1068 };
  const GRILLE = { x: 130, y: 1200, w: 820, h: 530 };
  const BANDS = ['ДВ', 'СВ', 'КВ-1', 'КВ-2'];
  const bandY = i => DIAL.y + 120 + i * 96;
  const NX0 = DIAL.x + 120, NX1 = DIAL.x + DIAL.w - 40;
  const STATIONS = [
    ['МОСКВА', 0, 0.12], ['КИЕВ', 0, 0.48], ['МИНСК', 0, 0.80],
    ['ОДЕССА', 1, 0.20], ['БУХАРЕСТ', 1, 0.55], ['СОФИЯ', 1, 0.86],
    ['ВАРШАВА', 2, 0.10], ['БЕРЛИН', 2, 0.42], ['ВЕНА', 2, 0.74],
    ['ПРАГА', 3, 0.16], ['ЛОНДОН', 3, 0.50], ['ПАРИЖ', 3, 0.82],
  ].map(([name, band, u]) => ({ name, band, x: NX0 + u * (NX1 - NX0) }));
  const ST = n => STATIONS.find(s => s.name === n);

  /*TIMELINE*/
  const TUNES = [{ t0: 2.0, t1: 7.0, to: 'МОСКВА' }];
  const TX = [];
  const END = 12;
  const needleX = t => {
    let x = NX0 + 0.62 * (NX1 - NX0), prev = x;
    for (const k of TUNES) {
      const target = ST(k.to).x;
      if (t < k.t0) return prev;
      const p = K.clamp((t - k.t0) / (k.t1 - k.t0));
      return K.lerp(prev, target, K.ease.inOutCubic(p));
    }
    return prev;
  };
  const tuned = t => { // 0..1 how well a station is locked
    const x = needleX(t); let best = 1e9;
    for (const s of STATIONS) best = Math.min(best, Math.abs(s.x - x));
    return K.clamp(1 - best / 26);
  };
  const power = t => e(t, 1.0, 6.5); // valves warming up

  // camera in radio space
  const camera = t => {
    // start close on the dial, pull back to the whole radio, then drift
    const p = e(t, 2.0, 13.5);
    let z = K.lerp(2.15, 1.0, p), cx = K.lerp(DIAL.x + DIAL.w * 0.35, W / 2, p), cy = K.lerp(DIAL.y + DIAL.h * 0.45, H / 2 - 60, p);
    // lean in to the dial as the station locks
    const lean = e(t, 13.5, 16.4);
    z = K.lerp(z, 1.75, lean); cx = K.lerp(cx, ST('МОСКВА').x, lean * 0.7); cy = K.lerp(cy, DIAL.y + DIAL.h * 0.5, lean);
    z *= 1 + 0.01 * Math.sin(t * 0.4); cx += Math.sin(t * 0.23) * 6; cy += Math.cos(t * 0.31) * 6;
    return { z, cx, cy };
  };

  // ---------------- static textures ----------------
  // built lazily on the first frame: needs K.init's noise and loaded fonts
  let TEX = null;
  const build = () => {
    // walnut veneer
    const wood = K.makeCanvas(CAB.w, CAB.h), w = wood.getContext('2d'), r = K.rng(5);
    const g = w.createLinearGradient(0, 0, CAB.w, 0);
    g.addColorStop(0, '#2a170b'); g.addColorStop(0.5, '#3d2312'); g.addColorStop(1, '#26150a');
    w.fillStyle = g; w.fillRect(0, 0, CAB.w, CAB.h);
    for (let i = 0; i < 260; i++) {
      const x0 = r() * CAB.w, amp = 6 + r() * 30, fr = 0.002 + r() * 0.006, ph = r() * 6;
      w.strokeStyle = `rgba(${r() > 0.5 ? '20,10,4' : '90,55,30'},${0.08 + r() * 0.18})`; w.lineWidth = 0.6 + r() * 2.2;
      w.beginPath();
      for (let y = 0; y <= CAB.h; y += 12) w.lineTo(x0 + Math.sin(y * fr + ph) * amp + Math.sin(y * fr * 3.1) * amp * 0.3, y);
      w.stroke();
    }
    // speaker cloth: fine weave
    const cloth = K.makeCanvas(GRILLE.w, GRILLE.h), c = cloth.getContext('2d');
    c.fillStyle = '#5a4630'; c.fillRect(0, 0, GRILLE.w, GRILLE.h);
    for (let y = 0; y < GRILLE.h; y += 4) { c.fillStyle = `rgba(30,20,10,${y % 8 ? 0.35 : 0.15})`; c.fillRect(0, y, GRILLE.w, 2); }
    for (let x = 0; x < GRILLE.w; x += 4) { c.fillStyle = `rgba(200,170,120,${x % 8 ? 0.08 : 0.04})`; c.fillRect(x, 0, 2, GRILLE.h); }
    c.globalAlpha = 0.25; c.globalCompositeOperation = 'multiply';
    for (let y = 0; y < GRILLE.h; y += 512) for (let x = 0; x < GRILLE.w; x += 512) c.drawImage(K.noise[2], x, y);
    // dial printing (scales + names), drawn once
    const print = K.makeCanvas(DIAL.w, DIAL.h), p = print.getContext('2d');
    p.strokeStyle = 'rgba(45,22,6,0.85)'; p.fillStyle = 'rgba(45,22,6,0.9)';
    p.font = '30px "PT Mono"'; p.textBaseline = 'middle';
    BANDS.forEach((b, i) => {
      const y = bandY(i) - DIAL.y;
      p.textAlign = 'left'; p.fillText(b, 22, y);
      p.lineWidth = 2; p.beginPath(); p.moveTo(NX0 - DIAL.x - 10, y + 22); p.lineTo(NX1 - DIAL.x + 10, y + 22); p.stroke();
      for (let k = 0; k <= 40; k++) {
        const x = NX0 - DIAL.x + k * (NX1 - NX0) / 40, L = k % 5 ? 8 : 16;
        p.lineWidth = k % 5 ? 1.2 : 2; p.beginPath(); p.moveTo(x, y + 22); p.lineTo(x, y + 22 - L); p.stroke();
      }
    });
    p.font = '26px "Old Standard TT"'; p.textAlign = 'center';
    for (const s of STATIONS) p.fillText(s.name, s.x - DIAL.x, bandY(s.band) - DIAL.y - 14);
    p.font = '22px "PT Mono"'; p.textAlign = 'center';
    for (let k = 0; k <= 8; k++) p.fillText(String(150 + k * 50), NX0 - DIAL.x + k * (NX1 - NX0) / 8, 40);
    return { wood, cloth, print };
  };
  const tex = () => TEX || (TEX = build());

  // ---------------- drawing ----------------
  // dust drifting through the warm light, in front of the radio
  const MOTES = (() => { const r = K.rng(12), a = []; for (let i = 0; i < 90; i++) a.push({ x: r() * W, y: r() * H, z: 0.3 + r(), ph: r() * 6, sp: 4 + r() * 12 }); return a; })();
  const motes = (ctx, t) => {
    const pw = power(t);
    ctx.save(); ctx.globalCompositeOperation = 'lighter';
    for (const m of MOTES) {
      const x = (m.x + Math.sin(t * 0.21 + m.ph) * 60 + t * m.sp * 0.6) % W, y = (m.y + Math.cos(t * 0.17 + m.ph) * 50 - t * m.sp + H * 10) % H;
      const d = Math.hypot(x - W / 2, y - (DIAL.y + DIAL.h / 2)) / 900, a = K.clamp(1 - d) * 0.55 * pw * (0.5 + 0.5 * Math.sin(t * 1.3 + m.ph));
      if (a < 0.02) continue;
      ctx.fillStyle = `rgba(255,200,140,${a})`; ctx.beginPath(); ctx.arc(x, y, 1 + m.z * 2.2, 0, 7); ctx.fill();
    }
    ctx.restore();
  };
  const knob = (ctx, k, ang) => {
    ctx.save(); ctx.translate(k.x, k.y);
    ctx.shadowColor = 'rgba(0,0,0,0.7)'; ctx.shadowBlur = 0; ctx.fillStyle = 'rgba(0,0,0,0.5)';
    ctx.beginPath(); ctx.arc(6, 10, 80, 0, 7); ctx.fill();
    const g = ctx.createRadialGradient(-25, -30, 5, 0, 0, 82);
    g.addColorStop(0, '#6b4a33'); g.addColorStop(0.6, '#2b1a10'); g.addColorStop(1, '#120a05');
    ctx.fillStyle = g; ctx.beginPath(); ctx.arc(0, 0, 76, 0, 7); ctx.fill();
    ctx.rotate(ang);
    ctx.strokeStyle = 'rgba(0,0,0,0.5)'; ctx.lineWidth = 3;
    for (let i = 0; i < 36; i++) { const a = i / 36 * 6.283; ctx.beginPath(); ctx.moveTo(Math.cos(a) * 66, Math.sin(a) * 66); ctx.lineTo(Math.cos(a) * 76, Math.sin(a) * 76); ctx.stroke(); }
    ctx.fillStyle = '#d9c8a6'; ctx.fillRect(-4, -60, 8, 26);
    ctx.restore();
  };

  const eye = (ctx, t) => { // the magic eye tuning indicator
    const pw = power(t), tu = tuned(t), lvl = envS('rms', t, 0.08);
    const open = K.lerp(1.9, 0.18, tu) * (1 - lvl * 0.25 * tu) + (1 - tu) * 0.25 * Math.sin(t * 13) * Math.sin(t * 5.3);
    ctx.save(); ctx.translate(EYE.x, EYE.y);
    const bz = ctx.createRadialGradient(-20, -24, 4, 0, 0, EYE.r + 18);
    bz.addColorStop(0, '#e8e2d4'); bz.addColorStop(0.5, '#7a7468'); bz.addColorStop(1, '#2a2622');
    ctx.fillStyle = bz; ctx.beginPath(); ctx.arc(0, 0, EYE.r + 16, 0, 7); ctx.fill();
    ctx.fillStyle = '#020403'; ctx.beginPath(); ctx.arc(0, 0, EYE.r, 0, 7); ctx.fill();
    if (pw > 0) {
      ctx.globalCompositeOperation = 'lighter';
      const gl = ctx.createRadialGradient(0, 0, 10, 0, 0, EYE.r);
      gl.addColorStop(0, `rgba(60,255,140,${0.15 * pw})`); gl.addColorStop(0.55, `rgba(70,255,150,${0.85 * pw})`); gl.addColorStop(1, `rgba(20,120,60,${0.4 * pw})`);
      ctx.fillStyle = gl;
      const half = Math.max(0.05, open) / 2, top = -Math.PI / 2;
      ctx.beginPath(); ctx.moveTo(0, 0); ctx.arc(0, 0, EYE.r - 4, top + half, top - half + Math.PI * 2); ctx.closePath(); ctx.fill();
      ctx.fillStyle = `rgba(150,255,190,${0.25 * pw})`; ctx.beginPath(); ctx.arc(0, 0, 16, 0, 7); ctx.fill();
      ctx.globalCompositeOperation = 'source-over';
      ctx.fillStyle = '#050505'; ctx.beginPath(); ctx.arc(0, 0, 13, 0, 7); ctx.fill();
    }
    ctx.restore();
  };

  const dial = (ctx, t, frame) => {
    const pw = power(t), lvl = envS('rms', t, 0.15), tu = tuned(t);
    ctx.save(); rr(ctx, DIAL.x, DIAL.y, DIAL.w, DIAL.h, 26); ctx.clip();
    const glow = pw * (0.75 + 0.18 * lvl + 0.07 * Math.sin(t * 31) * (1 - tu));
    const g = ctx.createRadialGradient(DIAL.x + DIAL.w / 2, DIAL.y + DIAL.h * 0.55, 20, DIAL.x + DIAL.w / 2, DIAL.y + DIAL.h * 0.55, DIAL.w * 0.75);
    g.addColorStop(0, rgb(mix([40, 20, 6], [255, 196, 110], glow)));
    g.addColorStop(0.6, rgb(mix([25, 12, 4], [214, 128, 40], glow)));
    g.addColorStop(1, rgb(mix([12, 6, 2], [90, 40, 10], glow)));
    ctx.fillStyle = g; ctx.fillRect(DIAL.x, DIAL.y, DIAL.w, DIAL.h);
    ctx.globalAlpha = 0.25 + 0.75 * pw; ctx.drawImage(tex().print, DIAL.x, DIAL.y); ctx.globalAlpha = 1;
    // static shimmer on the glass while untuned
    ctx.save(); ctx.globalAlpha = 0.10 * pw * (1 - tu); ctx.globalCompositeOperation = 'overlay';
    ctx.drawImage(K.noise[frame % 4], DIAL.x, DIAL.y, DIAL.w, DIAL.h); ctx.restore();
    // needle
    const nx = needleX(t);
    ctx.fillStyle = `rgba(255,90,40,${0.25 * pw})`; ctx.fillRect(nx - 9, DIAL.y, 18, DIAL.h);
    ctx.fillStyle = '#b3160f'; ctx.fillRect(nx - 2.5, DIAL.y + 30, 5, DIAL.h - 60);
    // glass reflection
    const rf = ctx.createLinearGradient(DIAL.x, DIAL.y, DIAL.x + DIAL.w * 0.6, DIAL.y + DIAL.h);
    rf.addColorStop(0, 'rgba(255,255,255,0.10)'); rf.addColorStop(0.35, 'rgba(255,255,255,0.02)'); rf.addColorStop(0.36, 'rgba(255,255,255,0)');
    ctx.fillStyle = rf; ctx.fillRect(DIAL.x, DIAL.y, DIAL.w, DIAL.h);
    ctx.restore();
    // bezel
    ctx.strokeStyle = '#b39a6a'; ctx.lineWidth = 7; rr(ctx, DIAL.x, DIAL.y, DIAL.w, DIAL.h, 26); ctx.stroke();
    ctx.strokeStyle = 'rgba(0,0,0,0.6)'; ctx.lineWidth = 2; rr(ctx, DIAL.x + 6, DIAL.y + 6, DIAL.w - 12, DIAL.h - 12, 22); ctx.stroke();
  };

  const radio = (ctx, t, frame) => {
    const pw = power(t), bass = envS('bass', t, 0.06);
    // cabinet
    ctx.save();
    ctx.fillStyle = 'rgba(0,0,0,0.6)'; rr(ctx, CAB.x + 14, CAB.y + 24, CAB.w, CAB.h, 60); ctx.fill();
    ctx.save(); rr(ctx, CAB.x, CAB.y, CAB.w, CAB.h, 60); ctx.clip();
    ctx.drawImage(tex().wood, CAB.x, CAB.y);
    const sh = ctx.createLinearGradient(0, CAB.y, 0, CAB.y + CAB.h);
    sh.addColorStop(0, 'rgba(255,200,140,0.10)'); sh.addColorStop(0.08, 'rgba(0,0,0,0)'); sh.addColorStop(0.9, 'rgba(0,0,0,0.25)'); sh.addColorStop(1, 'rgba(0,0,0,0.5)');
    ctx.fillStyle = sh; ctx.fillRect(CAB.x, CAB.y, CAB.w, CAB.h);
    // warm spill of the dial onto the wood
    const sp = ctx.createRadialGradient(W / 2, DIAL.y + DIAL.h / 2, 100, W / 2, DIAL.y + DIAL.h / 2, 700);
    sp.addColorStop(0, `rgba(255,150,60,${0.22 * pw})`); sp.addColorStop(1, 'rgba(255,150,60,0)');
    ctx.fillStyle = sp; ctx.fillRect(CAB.x, CAB.y, CAB.w, CAB.h);
    ctx.restore();
    ctx.strokeStyle = 'rgba(255,210,160,0.12)'; ctx.lineWidth = 3; rr(ctx, CAB.x + 2, CAB.y + 2, CAB.w - 4, CAB.h - 4, 58); ctx.stroke();

    dial(ctx, t, frame);
    // brand plate
    ctx.fillStyle = '#9c8558'; rr(ctx, W / 2 - 110, 932, 220, 44, 8); ctx.fill();
    ctx.fillStyle = '#2a1a0c'; ctx.font = '28px "Old Standard TT"'; ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
    ctx.letterSpacing = '8px'; ctx.fillText('СЕВЕР', W / 2 + 4, 955); ctx.letterSpacing = '0px';
    eye(ctx, t);
    knob(ctx, KNOB_L, (needleX(t) - NX0) / (NX1 - NX0) * 5.5);
    knob(ctx, KNOB_R, -2.2 + 2.6 * e(t, 0.4, 1.4));
    // speaker: cloth breathes with the bass, wooden slats in front
    ctx.save(); rr(ctx, GRILLE.x, GRILLE.y, GRILLE.w, GRILLE.h, 30); ctx.clip();
    const s = 1 + 0.006 * bass * pw;
    ctx.translate(GRILLE.x + GRILLE.w / 2, GRILLE.y + GRILLE.h / 2); ctx.scale(s, s); ctx.translate(-GRILLE.x - GRILLE.w / 2, -GRILLE.y - GRILLE.h / 2);
    ctx.drawImage(tex().cloth, GRILLE.x, GRILLE.y);
    ctx.fillStyle = `rgba(255,170,90,${0.05 + 0.08 * bass * pw})`; ctx.fillRect(GRILLE.x, GRILLE.y, GRILLE.w, GRILLE.h);
    ctx.restore();
    for (let i = 0; i < 7; i++) {
      const x = GRILLE.x + 40 + i * (GRILLE.w - 80) / 6;
      const sg = ctx.createLinearGradient(x - 14, 0, x + 14, 0);
      sg.addColorStop(0, '#1b0f07'); sg.addColorStop(0.5, '#4a2c16'); sg.addColorStop(1, '#1b0f07');
      ctx.fillStyle = sg; rr(ctx, x - 13, GRILLE.y - 6, 26, GRILLE.h + 12, 12); ctx.fill();
    }
    ctx.strokeStyle = '#b39a6a'; ctx.lineWidth = 5; rr(ctx, GRILLE.x, GRILLE.y, GRILLE.w, GRILLE.h, 30); ctx.stroke();
    ctx.restore();
    // bloom: the lit glass bleeds light into the dark room
    const lvl = envS('rms', t, 0.2), bl = ctx.createRadialGradient(W / 2, DIAL.y + DIAL.h / 2, DIAL.w * 0.3, W / 2, DIAL.y + DIAL.h / 2, DIAL.w * 0.95);
    bl.addColorStop(0, `rgba(255,170,80,${(0.16 + 0.06 * lvl) * pw})`); bl.addColorStop(1, 'rgba(255,170,80,0)');
    ctx.save(); ctx.globalCompositeOperation = 'lighter'; ctx.fillStyle = bl; ctx.fillRect(-500, -500, W + 1000, H + 1000); ctx.restore();
    // falloff: the lower cabinet sinks into the dark
    const fo = ctx.createLinearGradient(0, DIAL.y + DIAL.h, 0, CAB.y + CAB.h + 100);
    fo.addColorStop(0, 'rgba(0,0,0,0)'); fo.addColorStop(1, 'rgba(0,0,0,0.62)');
    ctx.fillStyle = fo; ctx.fillRect(-500, DIAL.y + DIAL.h, W + 1000, 1500);
  };

  // ---------------- the room: wall, window, desk and what lies on it (world space = 1080x1920) ----------------
  // the radio is drawn in its own space (E's geometry) and placed on the desk at RS scale
  const RS = 0.72, RX = W / 2 - W / 2 * RS, RY = 264;
  const toW = (x, y) => ({ x: RX + x * RS, y: RY + y * RS });
  const DESK_Y = 1500;
  const WN = { x: 150, y: 40, w: 780, h: 500 };
  const DIAL_W = toW(DIAL.x + DIAL.w / 2, DIAL.y + DIAL.h / 2), EYE_W = toW(EYE.x, EYE.y);
  const SNOW = (() => { const r = K.rng(8), a = []; for (let i = 0; i < 120; i++) a.push({ x: r(), y: r(), z: 0.3 + r() * 0.7, ph: r() * 6 }); return a; })();
  let RTEX = null;
  const roomTex = () => {
    if (RTEX) return RTEX;
    // wallpaper: faded vertical stripes and a small repeating figure
    const wall = K.makeCanvas(W + 400, DESK_Y + 200), w = wall.getContext('2d');
    w.fillStyle = '#16110d'; w.fillRect(0, 0, wall.width, wall.height);
    for (let x = 0; x < wall.width; x += 90) { w.fillStyle = 'rgba(70,52,36,0.35)'; w.fillRect(x, 0, 38, wall.height); w.fillStyle = 'rgba(0,0,0,0.25)'; w.fillRect(x + 40, 0, 3, wall.height); }
    w.fillStyle = 'rgba(90,70,48,0.22)';
    for (let y = 30; y < wall.height; y += 120) for (let x = 19; x < wall.width; x += 90) { w.beginPath(); w.ellipse(x, y + (x / 90 % 2) * 60, 6, 10, 0, 0, 7); w.fill(); }
    w.globalAlpha = 0.3; w.globalCompositeOperation = 'multiply';
    for (let y = 0; y < wall.height; y += 512) for (let x = 0; x < wall.width; x += 512) w.drawImage(K.noise[3], x, y);
    // desk top: dark varnished boards running across, seen slightly from above
    const desk = K.makeCanvas(W + 400, 700), d = desk.getContext('2d'), r = K.rng(21);
    const dg = d.createLinearGradient(0, 0, 0, 700); dg.addColorStop(0, '#2a180c'); dg.addColorStop(1, '#120a05');
    d.fillStyle = dg; d.fillRect(0, 0, desk.width, 700);
    for (let i = 0; i < 140; i++) {
      const y0 = r() * 700, amp = 2 + r() * 6;
      d.strokeStyle = `rgba(${r() > 0.5 ? '10,5,2' : '80,48,24'},${0.12 + r() * 0.2})`; d.lineWidth = 0.6 + r() * 1.6; d.beginPath();
      for (let x = 0; x <= desk.width; x += 20) d.lineTo(x, y0 + Math.sin(x * 0.004 + i) * amp * (1 + y0 / 700));
      d.stroke();
    }
    for (const y of [120, 300, 520]) { d.fillStyle = 'rgba(0,0,0,0.5)'; d.fillRect(0, y, desk.width, 3); }
    return (RTEX = { wall, desk });
  };
  const wallAndWindow = (ctx, t) => {
    ctx.fillStyle = '#050403'; ctx.fillRect(-1500, -1500, W + 3000, H + 3000);
    ctx.drawImage(roomTex().wall, -200, -200);
    ctx.save(); rr(ctx, WN.x, WN.y, WN.w, WN.h, 6); ctx.clip();
    const sky = ctx.createLinearGradient(0, WN.y, 0, WN.y + WN.h);
    sky.addColorStop(0, '#0a1729'); sky.addColorStop(1, '#1c334d');
    ctx.fillStyle = sky; ctx.fillRect(WN.x, WN.y, WN.w, WN.h);
    // rooftops across the street, one lit window that goes out
    ctx.fillStyle = '#070b12';
    ctx.beginPath(); ctx.moveTo(WN.x, WN.y + WN.h);
    [[0, 330], [120, 330], [120, 280], [260, 280], [300, 250], [340, 280], [470, 280], [470, 310], [610, 310], [610, 260], [780, 260], [780, 500]].forEach(([x, y]) => ctx.lineTo(WN.x + x, WN.y + y));
    ctx.closePath(); ctx.fill();
    if ((t % 23) < 15) { ctx.fillStyle = 'rgba(255,190,110,0.55)'; ctx.fillRect(WN.x + 520, WN.y + 350, 26, 34); }
    const mg = ctx.createRadialGradient(WN.x + WN.w * 0.74, WN.y + 120, 0, WN.x + WN.w * 0.74, WN.y + 120, 380);
    mg.addColorStop(0, 'rgba(190,215,255,0.42)'); mg.addColorStop(1, 'rgba(190,215,255,0)');
    ctx.fillStyle = mg; ctx.fillRect(WN.x, WN.y, WN.w, WN.h);
    ctx.fillStyle = 'rgba(230,240,255,0.85)'; ctx.beginPath(); ctx.arc(WN.x + WN.w * 0.74, WN.y + 120, 34, 0, 7); ctx.fill();
    for (const f of SNOW) {
      const y = WN.y + ((f.y * WN.h + t * 55 * f.z) % WN.h), x = WN.x + ((f.x * WN.w + Math.sin(t * 0.7 + f.ph) * 20 + WN.w) % WN.w);
      ctx.fillStyle = `rgba(225,235,255,${0.3 + 0.5 * f.z})`; ctx.beginPath(); ctx.arc(x, y, 1 + f.z * 2.6, 0, 7); ctx.fill();
    }
    const fr = ctx.createRadialGradient(WN.x, WN.y + WN.h, 0, WN.x, WN.y + WN.h, 380);
    fr.addColorStop(0, 'rgba(220,235,255,0.35)'); fr.addColorStop(1, 'rgba(220,235,255,0)');
    ctx.fillStyle = fr; ctx.fillRect(WN.x, WN.y, WN.w, WN.h);
    ctx.restore();
    ctx.fillStyle = '#0c0806';
    ctx.fillRect(WN.x - 22, WN.y - 22, 22, WN.h + 44); ctx.fillRect(WN.x + WN.w, WN.y - 22, 22, WN.h + 44); ctx.fillRect(WN.x - 22, WN.y - 22, WN.w + 44, 22);
    ctx.fillRect(WN.x + WN.w / 2 - 8, WN.y, 16, WN.h); ctx.fillRect(WN.x, WN.y + WN.h * 0.42, WN.w, 12);
    ctx.fillStyle = '#1a120c'; ctx.fillRect(WN.x - 40, WN.y + WN.h, WN.w + 80, 26); // sill
    // moonlight spilling down the wall, warm dial light around the radio
    const cs = ctx.createRadialGradient(W / 2, WN.y + WN.h, 40, W / 2, WN.y + WN.h, 900);
    cs.addColorStop(0, 'rgba(90,130,190,0.16)'); cs.addColorStop(1, 'rgba(90,130,190,0)');
    ctx.fillStyle = cs; ctx.fillRect(-1000, -1000, W + 2000, H + 2000);
    const pw = power(t), lv = envS('rms', t, 0.3);
    const g = ctx.createRadialGradient(DIAL_W.x, DIAL_W.y, 40, DIAL_W.x, DIAL_W.y, 1100);
    g.addColorStop(0, `rgba(150,85,28,${(0.34 + 0.06 * lv) * pw})`); g.addColorStop(1, 'rgba(0,0,0,0)');
    ctx.fillStyle = g; ctx.fillRect(-1000, -1000, W + 2000, H + 2000);
  };
  const deskTop = (ctx, t) => {
    ctx.drawImage(roomTex().desk, -200, DESK_Y);
    ctx.fillStyle = 'rgba(255,190,120,0.10)'; ctx.fillRect(-200, DESK_Y, W + 400, 3); // the back edge catches the light
    const pw = power(t), g = ctx.createRadialGradient(DIAL_W.x, DESK_Y + 120, 30, DIAL_W.x, DESK_Y + 120, 760);
    g.addColorStop(0, `rgba(255,150,60,${0.28 * pw})`); g.addColorStop(1, 'rgba(255,150,60,0)');
    ctx.fillStyle = g; ctx.fillRect(-200, DESK_Y, W + 400, 700);
    // the radio's shadow on the desk
    const sh = ctx.createLinearGradient(0, DESK_Y + 40, 0, DESK_Y + 160);
    sh.addColorStop(0, 'rgba(0,0,0,0.75)'); sh.addColorStop(1, 'rgba(0,0,0,0)');
    ctx.fillStyle = sh; ctx.beginPath(); ctx.ellipse(W / 2, DESK_Y + 70, 420, 90, 0, 0, 7); ctx.fill();
  };
  // enamel mug with a blue rim and a chip; steam drifts up across the radio
  const CUP = { x: 175, y: 1800 };
  const cup = (ctx, t) => {
    const { x, y } = CUP, pw = power(t);
    ctx.save(); ctx.translate(x, y);
    ctx.fillStyle = 'rgba(0,0,0,0.55)'; ctx.beginPath(); ctx.ellipse(10, 4, 96, 22, 0, 0, 7); ctx.fill();
    ctx.strokeStyle = '#8f8a80'; ctx.lineWidth = 16; ctx.beginPath(); ctx.ellipse(-84, -78, 30, 42, 0, Math.PI * 0.5, Math.PI * 1.5); ctx.stroke(); // handle, in shadow
    const bg = ctx.createLinearGradient(-78, 0, 78, 0);
    bg.addColorStop(0, '#4c4840'); bg.addColorStop(0.55, '#b9b2a2'); bg.addColorStop(0.85, `rgb(${mix([200, 194, 180], [240, 214, 170], pw)})`); bg.addColorStop(1, '#8a8274');
    ctx.fillStyle = bg; ctx.beginPath(); ctx.moveTo(-78, -150); ctx.lineTo(78, -150); ctx.lineTo(72, -10); ctx.quadraticCurveTo(70, 6, 50, 8); ctx.lineTo(-50, 8); ctx.quadraticCurveTo(-70, 6, -72, -10); ctx.closePath(); ctx.fill();
    ctx.fillStyle = 'rgba(40,30,20,0.6)'; ctx.beginPath(); ctx.arc(34, -40, 7, 0, 7); ctx.arc(-40, -110, 4, 0, 7); ctx.fill(); // chips in the enamel
    ctx.fillStyle = '#26364f'; ctx.beginPath(); ctx.ellipse(0, -150, 78, 18, 0, 0, 7); ctx.fill(); // blue rim
    ctx.fillStyle = '#1a0f08'; ctx.beginPath(); ctx.ellipse(0, -148, 70, 14, 0, 0, 7); ctx.fill(); // tea
    ctx.fillStyle = `rgba(255,170,90,${0.25 * pw})`; ctx.beginPath(); ctx.ellipse(22, -150, 26, 4, 0, 0, 7); ctx.fill(); // the dial reflected
    ctx.restore();
    // steam: soft ribbons, warm where they cross the dial light
    ctx.save(); ctx.globalCompositeOperation = 'screen'; ctx.lineCap = 'round';
    for (let i = 0; i < 3; i++) {
      for (const [lw, a] of [[34, 0.035], [18, 0.05], [7, 0.07]]) {
        ctx.strokeStyle = `rgba(255,232,205,${a})`; ctx.lineWidth = lw; ctx.beginPath();
        for (let k = 0; k <= 30; k++) {
          const h = k / 30, yy = y - 160 - h * 520, xx = x - 30 + i * 30 + Math.sin(h * 5 + t * 1.1 + i * 2) * 28 * h + Math.sin(h * 11 - t * 0.7) * 10 * h + h * 60;
          k ? ctx.lineTo(xx, yy) : ctx.moveTo(xx, yy);
        }
        ctx.globalAlpha = 0.8 + 0.2 * Math.sin(t * 0.9 + i); ctx.stroke();
      }
    }
    ctx.restore();
  };
  // a letter, folded twice, with his glasses lying on it (one lens taped)
  const LETTER = { x: 610, y: 1765, r: -0.1 };
  const letter = (ctx, t) => {
    const pw = power(t);
    ctx.save(); ctx.translate(LETTER.x, LETTER.y); ctx.rotate(LETTER.r);
    ctx.fillStyle = 'rgba(0,0,0,0.5)'; ctx.fillRect(-176, -96, 360, 210);
    const pg = ctx.createLinearGradient(-180, -110, 180, 110);
    pg.addColorStop(0, '#6f6450'); pg.addColorStop(0.6, `rgb(${mix([150, 138, 112], [205, 180, 140], pw)})`); pg.addColorStop(1, '#8a7c62');
    ctx.fillStyle = pg; ctx.fillRect(-180, -105, 360, 210);
    ctx.fillStyle = 'rgba(0,0,0,0.18)'; ctx.fillRect(-180, -36, 360, 3); ctx.fillRect(-180, 36, 360, 3);
    ctx.strokeStyle = 'rgba(40,30,60,0.55)'; ctx.lineWidth = 2;
    for (let l = 0; l < 7; l++) { // handwriting: wavy lines, unreadable at this size
      const y = -84 + l * 26, len = l === 6 ? 120 : 280 - (l % 3) * 30; ctx.beginPath();
      for (let x = 0; x <= len; x += 4) ctx.lineTo(-150 + x, y + Math.sin(x * 0.7 + l) * 3 + Math.sin(x * 0.13) * 2);
      ctx.stroke();
    }
    ctx.restore();
    // glasses on top
    ctx.save(); ctx.translate(LETTER.x + 40, LETTER.y - 10); ctx.rotate(0.18); ctx.scale(1, 0.62);
    ctx.fillStyle = 'rgba(0,0,0,0.35)'; for (const dx of [-62, 62]) { ctx.beginPath(); ctx.arc(dx + 10, 14, 50, 0, 7); ctx.fill(); }
    ctx.strokeStyle = '#b89c62'; ctx.lineWidth = 6;
    for (const dx of [-62, 62]) { ctx.beginPath(); ctx.arc(dx, 0, 50, 0, 7); ctx.stroke(); }
    ctx.beginPath(); ctx.moveTo(-14, -6); ctx.quadraticCurveTo(0, -22, 14, -6); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(-112, -4); ctx.lineTo(-190, 40); ctx.moveTo(112, -4); ctx.lineTo(196, 30); ctx.stroke();
    ctx.fillStyle = `rgba(255,220,160,${0.06 + 0.1 * pw})`; for (const dx of [-62, 62]) { ctx.beginPath(); ctx.arc(dx, 0, 47, 0, 7); ctx.fill(); }
    ctx.save(); ctx.translate(62, 0); ctx.rotate(0.6); ctx.fillStyle = 'rgba(225,215,190,0.85)'; ctx.fillRect(-46, -10, 92, 20); ctx.rotate(-1.2); ctx.fillRect(-46, -10, 92, 20); ctx.restore();
    ctx.restore();
  };
  // a morphine ampoule standing by the radio and a glass syringe lying in front
  const AMP = { x: 930, y: 1690 };
  const ampoule = (ctx, t) => {
    const pw = power(t);
    ctx.save(); ctx.translate(AMP.x, AMP.y);
    ctx.fillStyle = 'rgba(0,0,0,0.5)'; ctx.beginPath(); ctx.ellipse(8, 2, 34, 9, 0, 0, 7); ctx.fill();
    const body = () => { ctx.beginPath(); ctx.moveTo(-20, 0); ctx.lineTo(-20, -86); ctx.quadraticCurveTo(-20, -100, -8, -106); ctx.lineTo(-6, -122); ctx.quadraticCurveTo(-12, -134, -6, -150); ctx.quadraticCurveTo(0, -160, 6, -150); ctx.quadraticCurveTo(12, -134, 6, -122); ctx.lineTo(8, -106); ctx.quadraticCurveTo(20, -100, 20, -86); ctx.lineTo(20, 0); ctx.closePath(); };
    ctx.fillStyle = 'rgba(200,220,230,0.10)'; body(); ctx.fill();
    ctx.fillStyle = `rgba(255,210,150,${0.12 + 0.08 * pw})`; ctx.fillRect(-18, -60, 36, 58); // liquid catching the dial light
    ctx.strokeStyle = `rgba(255,225,190,${0.45 + 0.25 * pw})`; ctx.lineWidth = 2.5; body(); ctx.stroke();
    ctx.fillStyle = 'rgba(255,245,230,0.6)'; ctx.fillRect(10, -84, 4, 70); // highlight on the lit side
    ctx.fillStyle = 'rgba(190,40,30,0.8)'; ctx.font = 'bold 11px "PT Mono"'; ctx.textAlign = 'center'; ctx.fillText('MORPH', 0, -40); ctx.fillRect(-18, -126, 36, 3);
    ctx.restore();
    ctx.save(); ctx.translate(880, 1858); ctx.rotate(-0.06); // syringe
    ctx.fillStyle = 'rgba(0,0,0,0.45)'; ctx.fillRect(-120, 8, 250, 16);
    ctx.fillStyle = 'rgba(210,225,235,0.14)'; rr(ctx, -110, -14, 170, 28, 6); ctx.fill();
    ctx.strokeStyle = `rgba(255,225,190,${0.4 + 0.25 * pw})`; ctx.lineWidth = 2; rr(ctx, -110, -14, 170, 28, 6); ctx.stroke();
    for (let k = 0; k < 9; k++) { ctx.fillStyle = 'rgba(255,230,200,0.5)'; ctx.fillRect(-96 + k * 17, -14, 2, k % 2 ? 7 : 12); }
    ctx.fillStyle = '#9a9488'; ctx.fillRect(60, -5, 46, 10); ctx.fillRect(104, -16, 8, 32);   // plunger
    ctx.fillStyle = '#c9c4b8'; ctx.fillRect(-150, -2, 40, 4);                                // needle
    ctx.restore();
  };
  const deskObjects = (ctx, t) => { letter(ctx, t); ampoule(ctx, t); cup(ctx, t); };
  // the whole world: wall, window, desk, radio, objects
  const world = (ctx, t, frame) => {
    wallAndWindow(ctx, t);
    deskTop(ctx, t);
    ctx.save(); ctx.translate(RX, RY); ctx.scale(RS, RS); radio(ctx, t, frame); ctx.restore();
    deskObjects(ctx, t);
  };

  // a picture arriving line by line through the static
  const transmission = (ctx, t, tx, frame) => {
    const IMG = { x: 0, y: 240, w: W, h: 1440 };
    const p = K.ease.linear(K.prog(t, tx.a + 0.6, tx.a + 6.0)), scanY = IMG.y + p * IMG.h;
    const fade = K.prog(t, tx.b - 1.6, tx.b); // the signal breaks up at the end
    ctx.fillStyle = '#030303'; ctx.fillRect(0, 0, W, H);
    const im = I(tx.img), r = K.rng(frame * 7 + 3);
    // revealed rows, in strips with slow horizontal jitter
    const STR = 60, sh = IMG.h / STR;
    for (let i = 0; i < STR; i++) {
      const y0 = IMG.y + i * sh;
      if (y0 > scanY) break;
      const h = Math.min(sh, scanY - y0) + 0.5;
      const jit = (K.hash(i, Math.floor(t * 6)) - 0.5) * 10 + (K.hash(i, frame) < 0.04 + fade * 0.4 ? (K.hash(i, frame + 1) - 0.5) * 140 * (0.3 + fade) : 0);
      ctx.save(); ctx.beginPath(); ctx.rect(0, y0, W, h); ctx.clip();
      ctx.filter = 'sepia(0.35) saturate(0.7) contrast(1.1)';
      K.cover(ctx, im, { x: IMG.x + jit, y: IMG.y, w: IMG.w, h: IMG.h }, 1.04 + 0.03 * K.prog(t, tx.a, tx.b), tx.fx, tx.fy);
      ctx.filter = 'none'; ctx.restore();
    }
    // colour fringe
    ctx.save(); ctx.globalCompositeOperation = 'screen'; ctx.globalAlpha = 0.10;
    ctx.fillStyle = '#ff2a2a'; ctx.fillRect(0, IMG.y, W, Math.min(IMG.h, scanY - IMG.y)); ctx.restore();
    // scanlines
    ctx.fillStyle = 'rgba(0,0,0,0.28)'; for (let y = IMG.y; y < IMG.y + IMG.h; y += 6) ctx.fillRect(0, y, W, 2);
    // static below the scan line and over the breaking picture: fine dark grain with bright specks
    const fine = (y0, y1, a) => {
      if (y1 <= y0) return;
      ctx.save(); ctx.beginPath(); ctx.rect(0, y0, W, y1 - y0); ctx.clip();
      ctx.globalAlpha = a; ctx.globalCompositeOperation = 'screen';
      const n = K.noise[frame % 4], ox = -(r() * 512) | 0, oy = -(r() * 512) | 0;
      for (let y = y0 + oy; y < y1; y += 512) for (let x = ox; x < W; x += 512) ctx.drawImage(n, x, y);
      ctx.restore();
      ctx.save(); ctx.globalAlpha = a * 0.6; // rolling interference bands
      for (let k = 0; k < 5; k++) {
        const by = y0 + ((K.hash(k, 3) * (y1 - y0) + t * 220 * (0.5 + K.hash(k, 4))) % Math.max(1, y1 - y0));
        ctx.fillStyle = `rgba(255,240,220,${0.10 + 0.15 * K.hash(k, frame)})`; ctx.fillRect(0, by, W, 2 + K.hash(k, 5) * 10);
      }
      ctx.restore();
    };
    fine(Math.max(IMG.y, scanY), IMG.y + IMG.h, 0.22);
    fine(IMG.y, Math.min(scanY, IMG.y + IMG.h), 0.05 + fade * 0.45);
    // the bright scanning line
    if (p < 1) {
      const g = ctx.createLinearGradient(0, scanY - 30, 0, scanY + 6);
      g.addColorStop(0, 'rgba(255,230,190,0)'); g.addColorStop(1, 'rgba(255,240,210,0.85)');
      ctx.fillStyle = g; ctx.fillRect(0, scanY - 30, W, 36);
    }
    // station label
    const la = K.window(t, tx.a + 0.3, tx.b, 0.6, 0.6);
    ctx.save(); ctx.globalAlpha = la; ctx.fillStyle = '#ffb45c'; ctx.font = '34px "PT Mono"'; ctx.textAlign = 'left'; ctx.letterSpacing = '6px';
    ctx.fillText(tx.st, 70, 180); ctx.textAlign = 'right'; ctx.fillStyle = 'rgba(255,180,92,0.6)'; ctx.fillText('приём', W - 70, 180);
    ctx.letterSpacing = '0px'; ctx.restore();
    // signal meter under the picture: dances with the music
    const lv = envS('rms', t, 0.05);
    for (let i = 0; i < 24; i++) {
      const on = i / 24 < lv * (1 - fade * 0.7);
      ctx.fillStyle = on ? (i > 18 ? '#ff6a3d' : '#ffb45c') : 'rgba(255,180,92,0.12)';
      ctx.fillRect(70 + i * 40, 1745, 30, 22);
    }
  };

  /*SCENES*/
  const SHOTS = [[0, 1, 540, 960], [3, 2.7, DIAL_W.x, DIAL_W.y], [5, 4, EYE_W.x, EYE_W.y], [7, 2.2, CUP.x + 120, CUP.y - 250], [9, 2.4, LETTER.x + 40, LETTER.y], [11, 2.6, AMP.x - 40, AMP.y - 60]];
  window.VERSION = {
    duration: END,
    images: {},
    render(ctx, t, frame) {
      const s = SHOTS.filter(q => t >= q[0]).pop();
      ctx.save(); ctx.translate(W / 2, H / 2); ctx.scale(s[1], s[1]); ctx.translate(-s[2], -s[3]);
      world(ctx, t, frame);
      ctx.restore();
      motes(ctx, t);
      K.grain(ctx, frame, 0.06); K.vignette(ctx, 0.55);
    },
  };
})();
