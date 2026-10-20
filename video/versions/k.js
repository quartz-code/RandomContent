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

  // ================= timeline (video seconds) =================
  // The song is re-cut (audio/radio_edit.py): song 2.81–45.10 → 0–42.29 · 0.28 s gap · coda 300.78–310.24 → 42.57–52.03 ·
  // the blast (no song) → 54.40 · the ending 364.61–376.43 → 54.40–66.22 · click · silence → 70.2
  const BEAT = 0.59118, BAR = BEAT * 4;
  const bar = k => k <= 18 ? 0.45 + BAR * k - 2.81 : k < 154 ? 42.57 + BAR * (k - 127) : 54.40 + BAR * (k - 154);
  const DROP = bar(11), CODA = 42.57, FREEZE = 51.73, BLAST = 52.03, CLICK = 66.22, END = 70.2;
  // what the picture punches on: kick+snare on beat 2 of the sparse groove, every beat of the coda, the ending's accents
  const HITS = [...[11, 12, 13, 14, 15, 16, 17, 18].map(k => bar(k) + BEAT), ...Array.from({ length: 16 }, (_, i) => CODA + i * BEAT), 58.52, 60.89, 61.48];
  const since = (list, t) => { let b = 1e9; for (const k of list) if (k <= t) b = Math.min(b, t - k); return b; };
  const hit = t => Math.exp(-since(HITS, t) * 9);
  const win = (t, a, b) => t >= a && t < b;

  // ---- the needle: [time, x, move duration, overshoot] ----
  const U = u => NX0 + u * (NX1 - NX0), SX = n => ST(n).x;
  const NK = [[0, U(0.62), 0, 0], [2.5, U(0.12), 0.45, 0], [3.05, U(0.55), 0.45, 0], [3.6, U(0.86), 0.4, 0], [4.15, U(0.30), 0.5, 1]];
  NK.push([DROP + 2.2, SX('БЕРЛИН'), 0.16, 1], [bar(12) + 2.07, SX('ЛОНДОН'), 0.25, 1], [bar(13) + 2.07, SX('МИНСК'), 0.25, 1]);
  NK.push([bar(15), SX('БЕРЛИН'), 0.12, 0.6], [bar(15) + BEAT, SX('ЛОНДОН'), 0.12, 0.6], [bar(15) + 2 * BEAT, SX('МИНСК'), 0.12, 0.6], [bar(15) + 3 * BEAT, U(0.63), 0.2, 1]);
  const CROWD = ['ВЕНА', 'КИЕВ', 'ПАРИЖ', 'ОДЕССА', 'ВАРШАВА', 'СОФИЯ', 'ПРАГА', 'БЕРЛИН'];
  CROWD.forEach((n, i) => NK.push([bar(17) + i * BEAT / 2, SX(n), 0.1, 0.5]));
  NK.push([bar(18), U(0.5), 0.3, 0]);
  NK.push([CODA - 0.17, SX('ВАРШАВА'), 0.17, 1]);
  const PANIC = ['ВАРШАВА', 'БЕРЛИН', 'ЛОНДОН', 'МИНСК', 'ВЕНА', 'ПРАГА', 'КИЕВ', 'ВАРШАВА'];
  PANIC.forEach((n, i) => NK.push([bar(128) + i * BEAT / 2, SX(n), 0.09, 0.4]));
  NK.push([BLAST, U(0.98), 0.08, 0], [BLAST + 0.12, U(0.02), 0.1, 0], [BLAST + 0.26, U(0.95), 0.1, 0], [BLAST + 0.42, U(0.47), 0.6, 1]);
  NK.push([bar(154) - 0.2, SX('БЕРЛИН'), 0.3, 0.3], [bar(154) + 2 * BEAT - 0.2, SX('ЛОНДОН'), 0.3, 0.3], [bar(155) - 0.2, SX('МИНСК'), 0.3, 0.3], [bar(155) + 2 * BEAT, U(0.33), 0.5, 0]);
  NK.push([bar(156), SX('МОСКВА'), 1.1, 0.4], [bar(157) + 0.15, U(-0.11), 1.1, 0]);
  const needleBase = t => {
    let x = NK[0][1];
    for (let i = 1; i < NK.length; i++) {
      const [t0, to, d, os] = NK[i];
      if (t < t0) break;
      if (i + 1 < NK.length && t >= NK[i + 1][0]) { x = to; continue; }
      const p = d > 0 ? K.clamp((t - t0) / d) : 1;
      if (p < 1) return K.lerp(x, to, K.ease.inOutCubic(p));
      const dt = t - t0 - d;
      return to + os * 18 * Math.sin(dt * 18) * Math.exp(-dt * 7);
    }
    return x;
  };
  const needleJitter = t => {
    let j = 0;
    if (t < DROP) j += Math.sin(t * 1.7) * 2 + Math.sin(t * 4.3) * 1;                                   // fine-tuning by hand
    if (win(t, bar(18), CODA)) j += Math.sin(t * 47) * 36 * K.prog(t, bar(18), CODA) + Math.sin(t * 13) * 14;
    if (win(t, BLAST + 1.1, bar(156))) j += Math.sin(t * 9) * 3 * K.hash(Math.floor(t * 6), 3);         // trembling, weak
    return j;
  };
  const needleX = t => needleBase(t) + needleJitter(t);
  const tuned = t => { // 0..1 how close the needle is to any station
    const x = needleX(t); let best = 1e9;
    for (const s of STATIONS) best = Math.min(best, Math.abs(s.x - x));
    return K.clamp(1 - best / 26);
  };

  // ---- the night log ----
  const LOG = [
    { me: 1, s: 'есть кто живой? приём', t: bar(3), a: bar(3) - 0.8 },
    { st: 1, s: 'нет ответа', t: bar(3) + 2 * BEAT },
    { me: 1, s: 'jemand?', t: bar(4), a: bar(4) - 0.5 },
    { me: 1, s: 'anyone?', t: bar(4) + 2 * BEAT, a: bar(4) + 2 * BEAT - 0.5 },
    { s: 'тут я. ты чего на трёх языках?', t: bar(5), a: bar(5) - BEAT },
    { me: 1, s: 'мало ли кто не спит.', t: bar(6), a: bar(6) - 0.75 },
    { s: 'а зачем тебе три?', t: bar(7), a: bar(7) - 0.55 },
    { me: 1, s: 'раненые бывают разные.', t: bar(8), a: bar(8) - 0.8 },
    { s: 'и что кричат?', t: bar(9), a: bar(9) - 0.5 },
  ];
  const PUNCH = { s: '(одно и то же)', a: bar(10) + 2.5 * BEAT, t: DROP };
  const CLOCK = [[0, '03:12'], [bar(5), '03:13'], [bar(7), '03:14'], [bar(9), '03:15'], [bar(12), '03:19'], [bar(14), '03:23'], [bar(16), '03:28'], [bar(18), '03:40'], [BLAST, '03:41']];
  const clock = t => CLOCK.filter(c => t >= c[0]).pop()[1];

  // ---------------- morse: the key, the ticker, the decoded text ----------------
  const MORSE = {
    а: '.-', б: '-...', в: '.--', г: '--.', д: '-..', е: '.', ё: '.', ж: '...-', з: '--..', и: '..', й: '.---', к: '-.-', л: '.-..', м: '--', н: '-.',
    о: '---', п: '.--.', р: '.-.', с: '...', т: '-', у: '..-', ф: '..-.', х: '....', ц: '-.-.', ч: '---.', ш: '----', щ: '--.-', ъ: '--.--', ы: '-.--',
    ь: '-..-', э: '..-..', ю: '..--', я: '.-.-', a: '.-', b: '-...', c: '-.-.', d: '-..', e: '.', f: '..-.', g: '--.', h: '....', i: '..', j: '.---',
    k: '-.-', l: '.-..', m: '--', n: '-.', o: '---', p: '.--.', q: '--.-', r: '.-.', s: '...', t: '-', u: '..-', v: '...-', w: '.--', x: '-..-',
    y: '-.--', z: '--..', ä: '.-.-', '?': '..--..', '!': '--..--', '.': '......', ',': '.-.-.-', '-': '-....-',
  };
  // a keyed message: elements [{a, b, dash}] and the time each character is complete
  const keyed = (s, t0, unit) => {
    const els = [], chars = []; let t = t0;
    for (const ch of s.toLowerCase()) {
      const code = MORSE[ch];
      if (!code) { t += unit * 4; chars.push(t); continue; } // space or unknown: a word gap
      for (const d of code) { const L = d === '-' ? 3 : 1; els.push({ a: t, b: t + L * unit, dash: d === '-' }); t += (L + 1) * unit; }
      t += unit * 2; chars.push(t);
    }
    return { s, els, chars, end: t };
  };
  const keyDown = (msgs, t) => msgs.some(m => m.els.some(q => t >= q.a && t < q.b));
  const decodedUpTo = (m, t) => m.s.slice(0, m.chars.filter(c => c <= t).length);
  // the brass straight key on its wooden base; the lever dips while keyed
  const KEY = { x: 890, y: 1830 };
  const telegraphKey = (ctx, t, down) => {
    const pw = power(t);
    ctx.save(); ctx.translate(KEY.x, KEY.y);
    ctx.fillStyle = 'rgba(0,0,0,0.55)'; ctx.beginPath(); ctx.ellipse(10, 10, 170, 26, 0, 0, 7); ctx.fill();
    const wb = ctx.createLinearGradient(0, -40, 0, 10); wb.addColorStop(0, '#4a2c16'); wb.addColorStop(1, '#1c0f07');
    ctx.fillStyle = wb; rr(ctx, -160, -36, 320, 44, 8); ctx.fill();                 // base
    ctx.fillStyle = 'rgba(255,190,120,0.18)'; ctx.fillRect(-156, -36, 312, 3);
    const brass = (x, y, w, h) => { const g = ctx.createLinearGradient(0, y, 0, y + h); g.addColorStop(0, `rgb(${mix([150, 120, 60], [250, 214, 140], pw)})`); g.addColorStop(1, '#5a4320'); ctx.fillStyle = g; ctx.fillRect(x, y, w, h); };
    brass(70, -76, 34, 42); brass(60, -40, 54, 8);                                   // pivot post (back)
    brass(-120, -48, 20, 14);                                                         // contact
    ctx.save(); ctx.translate(86, -66); ctx.rotate(down ? 0.06 : 0);                  // lever
    brass(-210, -8, 230, 14);
    ctx.strokeStyle = '#8a6a34'; ctx.lineWidth = 3; ctx.beginPath(); for (let k = 0; k < 6; k++) { ctx.moveTo(-60, 8 + k * 4); ctx.lineTo(-44, 10 + k * 4); } ctx.stroke(); // spring
    ctx.fillStyle = '#0d0b0a'; ctx.beginPath(); ctx.ellipse(-206, -12, 30, 16, 0, 0, 7); ctx.fill(); // knob
    ctx.fillStyle = '#2b2622'; ctx.beginPath(); ctx.ellipse(-206, -18, 26, 9, 0, 0, 7); ctx.fill();
    ctx.restore();
    if (down) { ctx.fillStyle = 'rgba(255,220,160,0.9)'; ctx.beginPath(); ctx.arc(-110, -48, 4, 0, 7); ctx.fill(); } // contact spark
    ctx.restore();
  };
  // a ticker strip: dots and dashes scrolling left, drawn as shapes (not glyphs)
  const ticker = (ctx, msgs, t, x, y, w, o = {}) => {
    const pxs = o.pxs || 520, h = o.h || 16;
    ctx.save(); ctx.beginPath(); ctx.rect(x, y - 30, w, 60); ctx.clip();
    ctx.fillStyle = 'rgba(255,255,255,0.06)'; ctx.fillRect(x, y - 1, w, 2);
    for (const m of msgs) for (const q of m.els) {
      if (q.a > t) continue;
      const x1 = x + w - (t - q.a) * pxs, x2 = x + w - (t - Math.min(t, q.b)) * pxs;
      if (x2 < x) continue;
      ctx.fillStyle = m.color || '#ffb45c';
      if (q.dash) rr(ctx, x1, y - h * 0.3, Math.max(4, x2 - x1), h * 0.6, h * 0.3), ctx.fill();
      else { ctx.beginPath(); ctx.arc((x1 + x2) / 2, y, h * 0.36, 0, 7); ctx.fill(); }
    }
    ctx.restore();
  };

  // ---- morse traffic: his key (700 Hz) and the other end (590 Hz); text reveals over each burst ----
  const burst = (s, a, b, unit) => { const m = keyed(s, a, unit); m.els = m.els.filter(q => q.b <= b + 0.001); m.a = a; m.b = b; return m; };
  const MY = LOG.filter(m => m.me).map(m => burst(m.s, m.a, m.t, 0.06));
  MY.push(burst('одно и то же', PUNCH.a, PUNCH.t, 0.1));
  const MAMA = 'мама'.split('').map((ch, i) => keyed(ch, bar(156) + i * BEAT, 0.075));
  MY.push(...MAMA);
  const BROKEN = { s: 'есть кто жи', a: bar(155) + 2 * BEAT + 0.05, t: bar(155) + 3.3 * BEAT };
  MY.push(burst(BROKEN.s, BROKEN.a, BROKEN.t, 0.06));
  const THEIR = LOG.filter(m => !m.me && !m.st).map(m => burst(m.s, m.a, m.t, 0.055));
  const SOS = keyed('sos sos', CODA, 0.062); THEIR.push(SOS);
  THEIR.push(burst('sanitäter medic помогите hilfe help санитара', bar(17), CODA - 0.3, 0.045));
  THEIR.push(burst('medic medic sanitäter', bar(18) + 0.1, CODA - 0.3, 0.05));

  // ---- the radio's state ----
  const power = t => { // the valves: warm up, die in steps after the blast, go out at the click
    if (t < 0.15) return 0;
    if (t < BLAST) return K.ease.outCubic(K.prog(t, 0.15, 1.8));
    if (t >= CLICK) return 0;
    const steps = [[BLAST + 0.59, 0.72], [BLAST + 1.18, 0.5], [BLAST + 1.77, 0.38]];
    let p = 1; for (const [s, v] of steps) if (t >= s) p = v;
    if (t > bar(158)) p = K.lerp(0.38, 0.1, K.prog(t, bar(158), CLICK));
    const pop = [...steps.map(s => s[0]), bar(158), bar(158) + 2 * BEAT].some(s => t >= s && t < s + 0.07) ? 0.3 : 1;
    return p * pop * (0.92 + 0.08 * K.hash(Math.floor(t * 18), 7));
  };
  const overdrive = t => win(t, bar(18), CODA) ? K.prog(t, bar(18), CODA) * 0.85 : win(t, CODA, BLAST) ? 0.25 + 0.35 * K.prog(t, bar(129), FREEZE) : 0;
  const eyeTune = t => {
    let v = tuned(t);
    for (const m of LOG) if (!m.me && !m.st && win(t, m.a, m.t + 1.4)) v = Math.max(v, 0.62 + 0.1 * Math.sin(t * 9));
    if (win(t, DROP, DROP + 1.8)) v = 1;
    if (win(t, bar(15) + 3 * BEAT, bar(15) + 3 * BEAT + 0.35)) v = 1;
    if (win(t, bar(17), CODA)) v = 0.5 + 0.5 * Math.sin(t * 38);
    if (t > BLAST) v = Math.min(v, 0.15);
    if (win(t, bar(156) + 0.9, bar(157))) v = 0.5 + 0.06 * Math.sin(t * 7);
    return v;
  };
  const dead = t => t < BLAST + 0.59 ? 0 : t < BLAST + 1.18 ? 0.35 : 0.55;
  const STATION_ON = [ // which band row is lit on the dial
    [0, -1], [DROP + 2.2, 2], [bar(12) + 2.07, 3], [bar(13) + 2.07, 0], [bar(15) + 3 * BEAT, -1], [CODA - 0.17, 2], [bar(130), 2], [BLAST, -1], [bar(156), 0], [bar(157), -1],
  ];
  const bandOn = t => STATION_ON.filter(s => t >= s[0]).pop()[1];

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
    const pw = power(t), tu = eyeTune(t), lvl = envS('rms', t, 0.08);
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

  // the crack across the glass after the blast: a few branching lines from one point
  const CRACK = (() => { const r = K.rng(77), segs = [], grow = (x, y, a, n, w) => { for (let i = 0; i < n; i++) { const L = 30 + r() * 60, nx = x + Math.cos(a) * L, ny = y + Math.sin(a) * L; segs.push([x, y, nx, ny, w]); x = nx; y = ny; a += (r() - 0.5) * 0.9; if (r() < 0.3) grow(x, y, a + (r() - 0.5) * 2, Math.floor(n / 2), w * 0.6); } }; for (let k = 0; k < 6; k++) grow(DIAL.x + DIAL.w * 0.64, DIAL.y + DIAL.h * 0.34, k / 6 * 6.283 + r(), 6, 3); return segs; })();
  const crack = ctx => {
    ctx.lineCap = 'round';
    for (const [x0, y0, x1, y1, w] of CRACK) { ctx.strokeStyle = 'rgba(0,0,0,0.55)'; ctx.lineWidth = w + 2.5; ctx.beginPath(); ctx.moveTo(x0, y0); ctx.lineTo(x1, y1); ctx.stroke(); }
    for (const [x0, y0, x1, y1, w] of CRACK) { ctx.strokeStyle = 'rgba(235,240,255,0.6)'; ctx.lineWidth = w * 0.6; ctx.beginPath(); ctx.moveTo(x0, y0); ctx.lineTo(x1, y1); ctx.stroke(); }
  };
  const dial = (ctx, t, frame) => {
    const pw = power(t), lvl = envS('rms', t, 0.15), tu = tuned(t);
    ctx.save(); rr(ctx, DIAL.x, DIAL.y, DIAL.w, DIAL.h, 26); ctx.clip();
    const glow = Math.min(1, pw * (0.75 + 0.18 * lvl + 0.07 * Math.sin(t * 31) * (1 - tu)));
    const g = ctx.createRadialGradient(DIAL.x + DIAL.w / 2, DIAL.y + DIAL.h * 0.55, 20, DIAL.x + DIAL.w / 2, DIAL.y + DIAL.h * 0.55, DIAL.w * 0.75);
    g.addColorStop(0, rgb(mix([40, 20, 6], [255, 196, 110], glow)));
    g.addColorStop(0.6, rgb(mix([25, 12, 4], [214, 128, 40], glow)));
    g.addColorStop(1, rgb(mix([12, 6, 2], [90, 40, 10], glow)));
    ctx.fillStyle = g; ctx.fillRect(DIAL.x, DIAL.y, DIAL.w, DIAL.h);
    const bo = bandOn(t);
    if (bo >= 0) { ctx.fillStyle = `rgba(255,232,170,${0.22 * pw})`; ctx.fillRect(DIAL.x + 12, bandY(bo) - 44, DIAL.w - 24, 76); }
    ctx.globalAlpha = 0.25 + 0.75 * pw; ctx.drawImage(tex().print, DIAL.x, DIAL.y); ctx.globalAlpha = 1;
    const od = overdrive(t);
    if (od > 0) { ctx.fillStyle = `rgba(255,246,228,${od * 0.55})`; ctx.fillRect(DIAL.x, DIAL.y, DIAL.w, DIAL.h); }
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
    const dd = dead(t);
    if (dd > 0) { const dg = ctx.createLinearGradient(DIAL.x + DIAL.w * 0.4, 0, DIAL.x + DIAL.w * 0.7, 0); dg.addColorStop(0, 'rgba(0,0,0,0)'); dg.addColorStop(1, `rgba(0,0,0,${dd * 1.5})`); ctx.fillStyle = dg; ctx.fillRect(DIAL.x, DIAL.y, DIAL.w, DIAL.h); }
    if (t > BLAST + 0.12) crack(ctx);
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
  const LETTER = { x: 545, y: 1765, r: -0.1 };
  const letter = (ctx, t) => {
    const pw = power(t);
    ctx.save(); ctx.translate(LETTER.x, LETTER.y); ctx.rotate(LETTER.r);
    ctx.fillStyle = 'rgba(0,0,0,0.5)'; ctx.fillRect(-176, -96, 360, 210);
    const pg = ctx.createLinearGradient(-180, -110, 180, 110);
    pg.addColorStop(0, '#6f6450'); pg.addColorStop(0.6, `rgb(${mix([150, 138, 112], [205, 180, 140], pw)})`); pg.addColorStop(1, '#8a7c62');
    ctx.fillStyle = pg; ctx.fillRect(-180, -105, 360, 210);
    ctx.fillStyle = 'rgba(0,0,0,0.18)'; ctx.fillRect(-180, -36, 360, 3); ctx.fillRect(-180, 36, 360, 3);
    ctx.strokeStyle = 'rgba(40,30,60,0.5)'; ctx.lineWidth = 1.6;
    for (let l = 0; l < 7; l++) { // handwriting: wavy lines, unreadable at this size
      const y = -84 + l * 26, len = l === 6 ? 120 : 280 - (l % 3) * 30; ctx.beginPath();
      for (let x = 0; x <= len; x += 3) ctx.lineTo(-150 + x, y + Math.sin(x * 0.55 + l) * 2.2 * (0.6 + 0.4 * Math.sin(x * 0.05 + l)) + Math.sin(x * 0.11) * 1.2);
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
  const AMP = { x: 985, y: 1655 };
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
    ctx.save(); ctx.translate(380, 1880); ctx.rotate(0.05); // syringe
    ctx.fillStyle = 'rgba(0,0,0,0.45)'; ctx.fillRect(-120, 8, 250, 16);
    ctx.fillStyle = 'rgba(210,225,235,0.14)'; rr(ctx, -110, -14, 170, 28, 6); ctx.fill();
    ctx.strokeStyle = `rgba(255,225,190,${0.4 + 0.25 * pw})`; ctx.lineWidth = 2; rr(ctx, -110, -14, 170, 28, 6); ctx.stroke();
    for (let k = 0; k < 9; k++) { ctx.fillStyle = 'rgba(255,230,200,0.5)'; ctx.fillRect(-96 + k * 17, -14, 2, k % 2 ? 7 : 12); }
    ctx.fillStyle = '#9a9488'; ctx.fillRect(60, -5, 46, 10); ctx.fillRect(104, -16, 8, 32);   // plunger
    ctx.fillStyle = '#c9c4b8'; ctx.fillRect(-150, -2, 40, 4);                                // needle
    ctx.restore();
  };
  const deskObjects = (ctx, t) => { letter(ctx, t); ampoule(ctx, t); telegraphKey(ctx, t, keyDown(MY, t)); cup(ctx, t); };
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

  // ================= text, pictures, overlays =================
  const MONO = '"PT Mono"', SERIF = '"Old Standard TT"';
  const AMBER = '#ffb45c', WARM = '#f3e7d2', GREYT = '#8d8478', RED = '#ff3b30';
  const JUNK = '#%&@*+=<>/\\|ЖЩШЯ¤§01';
  const txt = (ctx, s, x, y, o = {}) => {
    ctx.save(); ctx.globalAlpha = o.alpha ?? 1;
    ctx.font = `${o.size || 48}px ${o.font || MONO}`; ctx.textAlign = o.align || 'left'; ctx.textBaseline = 'alphabetic';
    if (o.ls) ctx.letterSpacing = o.ls + 'px';
    if (o.glow) { ctx.shadowColor = o.glow; ctx.shadowBlur = o.blur || 24; }
    ctx.fillStyle = o.color || WARM; ctx.fillText(s, x, y);
    if (o.bold) { ctx.shadowBlur = 0; ctx.lineWidth = (o.size || 48) * 0.045; ctx.strokeStyle = o.color || WARM; ctx.strokeText(s, x, y); }
    ctx.restore();
  };
  // letters settle out of static glyphs, left to right
  const decodeS = (s, t, t0, dur = 0.3) => {
    if (t < t0) return '';
    let out = '';
    for (let i = 0; i < s.length; i++) {
      const ts = t0 + dur * (i / Math.max(1, s.length - 1));
      if (t >= ts || s[i] === ' ') out += s[i];
      else if (t >= t0 + dur * (i - 3) / Math.max(1, s.length - 1)) out += JUNK[Math.floor(K.hash(i, Math.floor(t * 30)) * JUNK.length)];
    }
    return out;
  };
  const typedS = (s, t, a, b) => s.slice(0, Math.floor(s.length * K.clamp((t - a) / Math.max(0.01, b - a)) + 1e-6));
  const slam = (ctx, s, x, y, t, t0, o = {}) => {
    if (t < t0) return;
    const p = K.clamp((t - t0) / 0.13), sc = K.lerp(o.from || 1.4, 1, K.ease.outCubic(p)), sp = (1 - p) * 14;
    ctx.save(); ctx.translate(x, y); ctx.scale(sc, sc);
    if (sp > 0.5) { txt(ctx, s, -sp, 0, { ...o, color: '#ff2a2a', alpha: 0.6 * (1 - p) }); txt(ctx, s, sp, 0, { ...o, color: '#2ad4ff', alpha: 0.5 * (1 - p) }); }
    txt(ctx, s, 0, 0, { ...o, alpha: (o.alpha ?? 1) * K.clamp(p * 3) });
    ctx.restore();
  };
  const header = (ctx, t, o = {}) => {
    const a = (o.alpha ?? 1) * K.prog(t, 0.9, 1.4);
    let c = clock(t);
    if (win(t, BLAST, BLAST + 1.4) && K.hash(Math.floor(t * 20), 2) > 0.5) c = '0' + Math.floor(K.hash(Math.floor(t * 20), 5) * 9) + ':' + Math.floor(K.hash(Math.floor(t * 20), 6) * 60);
    txt(ctx, '— журнал приёма', 60, 84, { size: 32, color: GREYT, alpha: a });
    txt(ctx, (t < BLAST ? 'КВ · 41 м   ' : '') + c, W - 60, 84, { size: 32, color: t > BLAST ? '#7d6a5a' : GREYT, alpha: a, align: 'right' });
    ctx.save(); ctx.globalAlpha = a * 0.5; ctx.fillStyle = GREYT; ctx.fillRect(60, 104, W - 120, 1); ctx.restore();
  };
  const cursor = t => (Math.floor(t / (BEAT / 2)) % 2 ? '▌' : ' ');

  // the radio world through a camera; o.cold = post-blast moonlight, o.white = overexposure
  const shot = (ctx, t, frame, z, cx, cy, o = {}) => {
    ctx.save(); ctx.translate(W / 2 + (o.dx || 0), H / 2 + (o.dy || 0)); ctx.scale(z, z); ctx.translate(-cx, -cy);
    world(ctx, t, frame);
    ctx.restore();
    if (power(t) > 0.05) motes(ctx, t);
    if (o.cold) { ctx.save(); ctx.globalCompositeOperation = 'multiply'; ctx.fillStyle = `rgba(110,140,190,${o.cold})`; ctx.fillRect(0, 0, W, H); ctx.restore(); }
    if (o.white) { ctx.fillStyle = `rgba(255,248,235,${o.white})`; ctx.fillRect(0, 0, W, H); }
  };
  const ACT = t => { const p = K.ease.inOutSine(K.prog(t, bar(3), bar(10))); return [1.39 + 0.1 * p, 540, 1142 - 26 * p]; };
  const DIALCAM = [1.55, 540, 1016];

  // ---- the log over the lower radio ----
  const ROWS_MAX = 6, LH = 64, LY = 1190, LX = 118;
  const logView = (ctx, t, frame, o = {}) => {
    const g = ctx.createLinearGradient(0, 980, 0, 1240); g.addColorStop(0, 'rgba(0,0,0,0)'); g.addColorStop(1, 'rgba(0,0,0,0.82)');
    ctx.fillStyle = g; ctx.fillRect(0, 980, W, H - 980);
    const rows = [];
    ctx.save(); ctx.font = `48px ${MONO}`;
    for (const m of LOG) {
      const a = m.a ?? m.t; if (t < a) continue;
      const lines = K.wrap(ctx, m.s, W - LX - 60);
      let shown = m.me ? typedS(m.s, t, a, m.t) : m.st ? decodeS(m.s, t, m.t, 0.15) : decodeS(m.s, t, a + 0.15, m.t - a - 0.15);
      let used = 0;
      lines.forEach((l, i) => { const part = shown.slice(used, used + l.length + 1); used += l.length + 1; rows.push({ m, s: part, first: i === 0, a }); });
    }
    ctx.restore();
    const lastA = Math.max(...LOG.filter(m => t >= (m.a ?? m.t)).map(m => m.a ?? m.t), -9);
    const nudge = Math.exp(-(t - lastA) * 7);
    const off = Math.max(0, rows.length - ROWS_MAX);
    rows.slice(off).forEach((r, i) => {
      const y = LY + i * LH + nudge * 6, age = rows.length - off - 1 - i, dim = age > 2 ? 0.45 : age > 0 ? 0.75 : 1;
      const col = r.m.st ? GREYT : r.m.me ? AMBER : WARM, al = dim * (o.alpha ?? 1);
      if (r.first && !r.m.st) txt(ctx, r.m.me ? '>' : '<', 62, y, { size: 48, color: col, alpha: al });
      txt(ctx, r.s, r.m.st ? 62 : LX, y, { size: r.m.st ? 40 : 48, color: col, alpha: al });
    });
    return LY + Math.min(rows.length, ROWS_MAX) * LH;
  };

  // ---- radio photos (SSTV) ----
  const BAKE = {};
  const bake = (key, mode) => {
    const id = key + mode; if (BAKE[id]) return BAKE[id];
    const c = K.makeCanvas(432, 464), x = c.getContext('2d'), P = PICS[key];
    x.filter = mode === 'cold' ? 'grayscale(0.7) sepia(0.25) hue-rotate(115deg) saturate(0.7) contrast(1.35) brightness(0.75)' : 'sepia(0.38) saturate(0.75) contrast(1.15) brightness(0.98)';
    K.cover(x, I(key), { x: 0, y: 0, w: 432, h: 464 }, P.z || 1, P.fx ?? 0.5, P.fy ?? 0.4);
    x.filter = 'none';
    return (BAKE[id] = c);
  };
  const PICS = { tent: { fx: 0.5, fy: 0.25, z: 1.0 }, coat: { fx: 0.45, fy: 0.35, z: 1.0 }, walk: { fx: 0.5, fy: 0.5, z: 1.0 }, head: { fx: 0.5, fy: 0.3, z: 1.0 }, carry: { fx: 0.5, fy: 0.35, z: 1.0 } };
  const PR = { x: 0, y: 300, w: W, h: 1160 };
  const noiseFill = (ctx, frame, y0, y1, a) => {
    if (y1 <= y0 || a <= 0) return;
    ctx.save(); ctx.beginPath(); ctx.rect(0, y0, W, y1 - y0); ctx.clip(); ctx.globalAlpha = a; ctx.globalCompositeOperation = 'screen';
    const n = K.noise[frame % 4], ox = -((K.hash(frame, 1) * 512) | 0), oy = -((K.hash(frame, 2) * 512) | 0);
    for (let y = y0 + oy; y < y1; y += 512) for (let x = ox; x < W; x += 512) ctx.drawImage(n, x, y);
    ctx.restore();
  };
  const sstv = (ctx, t, frame, o) => {
    const b = bake(o.img, o.cold ? 'cold' : 'warm'), N = 48, sh = PR.h / N, bh = b.height / N;
    const step = Math.min(1, Math.floor(Math.max(0, t - o.a) / (BEAT / 4) + 1) / 5);
    const reveal = o.instant ? 1 : step, scanY = PR.y + reveal * PR.h;
    const shk = (o.shake ? hit(t) * 14 : 0) * (K.hash(frame, 9) - 0.5) * 2;
    ctx.fillStyle = '#040404'; ctx.fillRect(0, 0, W, H);
    for (let i = 0; i < N; i++) {
      const y0 = PR.y + i * sh; if (y0 >= scanY) break;
      if (o.broken && (i % 3 === 1 || K.hash(i, Math.floor(t * 5)) < 0.12)) continue;
      if (o.torn && K.hash(i, Math.floor(t * 4) + 7) < 0.14) continue;
      let jit = (K.hash(i, Math.floor(t * 6)) - 0.5) * 6 + shk;
      if (o.torn && K.hash(i, Math.floor(t * 10)) < 0.22) jit += (K.hash(i, Math.floor(t * 10) + 1) - 0.5) * 260;
      if (o.broken && K.hash(i, Math.floor(t * 8)) < 0.2) jit += (K.hash(i, Math.floor(t * 8) + 3) - 0.5) * 160;
      ctx.drawImage(b, 0, i * bh, b.width, bh, PR.x + jit, y0, PR.w, sh + 0.6);
    }
    ctx.save(); ctx.globalCompositeOperation = 'screen'; ctx.globalAlpha = 0.08; ctx.fillStyle = o.cold ? '#2affc0' : '#ff3a2a';
    ctx.fillRect(0, PR.y, W, Math.min(PR.h, scanY - PR.y)); ctx.restore();
    ctx.fillStyle = 'rgba(0,0,0,0.3)'; for (let y = PR.y; y < PR.y + PR.h; y += 6) ctx.fillRect(0, y, W, 2);
    noiseFill(ctx, frame, scanY, PR.y + PR.h, 0.24);
    noiseFill(ctx, frame, PR.y, scanY, o.broken ? 0.3 : o.torn ? 0.16 : 0.05);
    if (reveal < 1) { const g = ctx.createLinearGradient(0, scanY - 34, 0, scanY + 4); g.addColorStop(0, 'rgba(255,230,190,0)'); g.addColorStop(1, 'rgba(255,240,210,0.85)'); ctx.fillStyle = g; ctx.fillRect(0, scanY - 34, W, 38); }
    // top and bottom bands for the plate and the captions
    const tg = ctx.createLinearGradient(0, 0, 0, PR.y + 60); tg.addColorStop(0, 'rgba(0,0,0,1)'); tg.addColorStop(0.7, 'rgba(0,0,0,0.85)'); tg.addColorStop(1, 'rgba(0,0,0,0)');
    ctx.fillStyle = tg; ctx.fillRect(0, 0, W, PR.y + 60);
    const bg = ctx.createLinearGradient(0, 1300, 0, 1560); bg.addColorStop(0, 'rgba(0,0,0,0)'); bg.addColorStop(1, 'rgba(0,0,0,0.92)');
    ctx.fillStyle = bg; ctx.fillRect(0, 1300, W, H - 1300);
  };
  // the station plate: the name decodes out of static, a small scale with the needle settling under it
  const plate = (ctx, name, t, t0, o = {}) => {
    const s = decodeS(name, t, t0, 0.2), u = (ST(name).x - NX0) / (NX1 - NX0), dim = o.dim ?? 1;
    txt(ctx, s, W / 2, 200, { size: 96, font: SERIF, color: AMBER, align: 'center', ls: 12, glow: 'rgba(255,150,40,0.7)', blur: 30, alpha: dim * (o.flick ? 0.6 + 0.4 * K.hash(Math.floor(t * 24), 4) : 1) });
    ctx.save(); ctx.globalAlpha = 0.7 * dim; ctx.fillStyle = AMBER; ctx.fillRect(140, 262, 800, 2);
    for (let k = 0; k <= 40; k++) ctx.fillRect(140 + k * 20, 262 - (k % 5 ? 8 : 16), 2, k % 5 ? 8 : 16);
    const dt = t - t0, nx = 140 + u * 800 + (dt < 0.6 ? Math.sin(dt * 22) * 40 * Math.exp(-dt * 8) : 0);
    ctx.globalAlpha = dim; ctx.fillStyle = '#ff4a2a'; ctx.fillRect(nx - 2.5, 236, 5, 44);
    ctx.restore();
  };
  const staticBurst = (ctx, frame, a) => {
    if (a <= 0) return;
    ctx.save(); ctx.globalAlpha = Math.min(1, a); ctx.fillStyle = '#9a948a'; ctx.fillRect(0, 0, W, H); ctx.restore();
    noiseFill(ctx, frame, 0, H, Math.min(1, a * 1.4));
    for (let k = 0; k < 8; k++) { ctx.fillStyle = `rgba(255,250,240,${0.3 * a})`; ctx.fillRect(0, K.hash(k, frame) * H, W, 2 + K.hash(k, frame + 1) * 18); }
  };
  const tear = (ctx, frame, amt) => {
    if (amt <= 0) return;
    for (let k = 0; k < 6; k++) {
      const y = Math.floor(K.hash(k, frame) * H), h = 20 + Math.floor(K.hash(k, frame + 3) * 90), dx = (K.hash(k, frame + 5) - 0.5) * 120 * amt;
      ctx.drawImage(ctx.canvas, 0, y, W, h, dx, y, W, h);
    }
  };
  const caption = (ctx, s, t, t0, o = {}) => slam(ctx, s, W / 2, o.y || 1640, t, t0, { size: o.size || 104, color: o.color || '#fbf3e6', align: 'center', bold: true, ...o });
  const reply = (ctx, s, t, t0, o = {}) => slam(ctx, '> ' + s, W / 2, o.y || 1770, t, t0, { size: o.size || 76, color: AMBER, align: 'center', from: 1.2, ...o });

  // his hand on the key: the arm comes from the camera side, two fingers rest on the knob, rim-lit by the dial
  const handOnKey = (ctx, t, down) => {
    const pw = power(t), kx = KEY.x - 120, ky = KEY.y - 84 + (down ? 9 : 0);
    ctx.save(); ctx.translate(kx, ky); ctx.lineCap = 'round'; ctx.lineJoin = 'round';
    ctx.fillStyle = '#0d0907';
    ctx.beginPath(); ctx.moveTo(18, 22); ctx.quadraticCurveTo(60, 2, 92, 14); ctx.lineTo(300, 300); ctx.lineTo(120, 360); ctx.closePath(); ctx.fill(); // sleeve
    ctx.fillStyle = '#2a1a11';
    ctx.beginPath(); ctx.ellipse(40, 14, 44, 26, 0.45, 0, 7); ctx.fill();                                                                   // back of the hand
    ctx.strokeStyle = '#2a1a11'; ctx.lineWidth = 15;
    ctx.beginPath(); ctx.moveTo(18, -2); ctx.quadraticCurveTo(2, -12, -12, -6); ctx.stroke();                                                 // index finger on the knob
    ctx.beginPath(); ctx.moveTo(24, 10); ctx.quadraticCurveTo(6, 6, -6, 10); ctx.stroke();                                                     // middle finger
    ctx.lineWidth = 13; ctx.beginPath(); ctx.moveTo(34, 26); ctx.quadraticCurveTo(14, 30, 4, 28); ctx.stroke();                               // thumb under
    ctx.strokeStyle = `rgba(255,175,95,${0.65 * pw})`; ctx.lineWidth = 2.5;
    ctx.beginPath(); ctx.moveTo(-18, -10); ctx.quadraticCurveTo(2, -20, 22, -10); ctx.quadraticCurveTo(58, -18, 84, 2); ctx.lineTo(300, 290); ctx.stroke(); // rim
    ctx.restore();
  };

  // ================= scenes =================
  const keyIsDown = t => keyDown(MY, t);
  const SCENES = [
    // eye lights up, the dial warms
    { a: 0, b: bar(2), draw(ctx, t, frame) { const p = K.ease.inOutSine(K.prog(t, 0, bar(2))); shot(ctx, t, frame, K.lerp(3.1, 2.4, p), 540, K.lerp(1020, 960, p)); header(ctx, t); } },
    // pull back: the needle hunts across the stations, the room appears
    { a: bar(2), b: bar(3), draw(ctx, t, frame) { const p = K.ease.inOutCubic(K.prog(t, bar(2), bar(3) - 0.25)); shot(ctx, t, frame, K.lerp(2.4, 1.0, p), 540, K.lerp(960, 960, p)); header(ctx, t); } },
    // the night log
    { a: bar(3), b: bar(10), draw(ctx, t, frame) {
      const [z, cx, cy] = ACT(t); shot(ctx, t, frame, z, cx, cy);
      header(ctx, t); logView(ctx, t, frame);
      ticker(ctx, [...MY.map(m => ({ ...m, color: AMBER })), ...THEIR.map(m => ({ ...m, color: WARM }))], t, 60, 1110, W - 120);
    } },
    // he hesitates over the key; then slowly taps the answer
    { a: bar(10), b: DROP, draw(ctx, t, frame) {
      const p = K.prog(t, bar(10), DROP), down = keyIsDown(t);
      const z = 2.3 + 0.25 * p, cx = KEY.x - 70, cy = KEY.y - 210;
      shot(ctx, t, frame, z, cx, cy);
      ctx.save(); ctx.translate(W / 2, H / 2); ctx.scale(z, z); ctx.translate(-cx, -cy); handOnKey(ctx, t, down); ctx.restore();
      const g = ctx.createLinearGradient(0, 0, 0, 760); g.addColorStop(0, 'rgba(0,0,0,0.88)'); g.addColorStop(1, 'rgba(0,0,0,0)'); ctx.fillStyle = g; ctx.fillRect(0, 0, W, 760);
      header(ctx, t);
      txt(ctx, '< и что кричат?', 62, 300, { size: 48, color: WARM, alpha: 0.6 });
      const s = t < PUNCH.a ? '' : typedS(PUNCH.s, t, PUNCH.a, PUNCH.t);
      txt(ctx, '> ' + s + (t < PUNCH.a ? (Math.floor(t * 4) % 2 ? '·' : '··') : cursor(t)), 62, 400, { size: 64, color: t < PUNCH.a ? '#57ff9a' : RED, bold: t >= PUNCH.a });
    } },
    // DROP: the answer fills the screen, the eye slams shut
    { a: DROP, b: DROP + 2.2, draw(ctx, t, frame) {
      const k = hit(t), z = 1.42 * (1 + 0.05 * k);
      shot(ctx, t, frame, z, 540, 1110, { dx: (K.hash(frame, 1) - 0.5) * 16 * k });
      header(ctx, t); logView(ctx, t, frame, { alpha: 0.1 });
      const sh = t > bar(11) + 3 * BEAT ? K.prog(t, bar(11) + 3 * BEAT, DROP + 2.2) : 0;
      ctx.save(); ctx.fillStyle = 'rgba(0,0,0,0.45)'; ctx.fillRect(0, 1180, W, 300); ctx.restore();
      if (sh <= 0) slam(ctx, PUNCH.s, W / 2, 1370, t, DROP, { size: 120, color: RED, align: 'center', bold: true, glow: 'rgba(255,40,30,0.6)', blur: 30, from: 1.25 });
      else { // shatters into flying letters
        const s = PUNCH.s; ctx.save(); ctx.font = `bold 120px ${MONO}`; const w = ctx.measureText(s).width; ctx.restore();
        for (let i = 0; i < s.length; i++) {
          const x = W / 2 - w / 2 + (i + 0.5) * w / s.length + (K.hash(i, 1) - 0.5) * 900 * sh * sh, y = 1370 + (K.hash(i, 2) - 0.3) * 1400 * sh * sh;
          txt(ctx, sh > 0.4 && K.hash(i, Math.floor(t * 30)) > 0.6 ? JUNK[i % JUNK.length] : s[i], x, y, { size: 120, color: RED, align: 'center', bold: true, alpha: 1 - sh });
        }
      }
      if (k > 0.6 && t > DROP + 0.5) tear(ctx, frame, k);
    } },
    // whip across the dial
    { a: DROP + 2.2, b: DROP + 2.37, draw(ctx, t, frame) { shot(ctx, t, frame, 2.2, 540, 732); staticBurst(ctx, frame, 0.6); } },
    // three stations: the same call, the same answer
    ...[['БЕРЛИН', 'tent', 'Sanitäter!', 'komme!'], ['ЛОНДОН', 'coat', 'Medic!', 'coming!'], ['МИНСК', 'walk', 'Санитара!', 'иду!']].map(([st, img, call, ans], i) => ({
      a: bar(12 + i), b: bar(13 + i), draw(ctx, t, frame) {
        const a = bar(12 + i), z = 1 + 0.03 * hit(t);
        ctx.save(); ctx.translate(W / 2, H / 2); ctx.scale(z, z); ctx.translate(-W / 2, -H / 2);
        sstv(ctx, t, frame, { img, a, shake: true });
        ctx.restore();
        plate(ctx, st, t, a);
        caption(ctx, call, t, a + BEAT); reply(ctx, ans, t, a + 2 * BEAT);
        const bt = a + BAR - BEAT / 2; if (t > bt) staticBurst(ctx, frame, K.prog(t, bt, a + BAR) * 0.9);
      },
    })),
    // the same three, side by side, as the needle hops
    { a: bar(15), b: bar(16), draw(ctx, t, frame) {
      shot(ctx, t, frame, DIALCAM[0] * (1 + 0.03 * hit(t)), DIALCAM[1], DIALCAM[2]); header(ctx, t);
      const g = ctx.createLinearGradient(0, 1000, 0, 1200); g.addColorStop(0, 'rgba(0,0,0,0)'); g.addColorStop(1, 'rgba(0,0,0,0.85)'); ctx.fillStyle = g; ctx.fillRect(0, 1000, W, 920);
      const rows = [['Sanitäter!', 'komme!'], ['Medic!', 'coming!'], ['Санитара!', 'иду!']], f = win(t, bar(15) + 3 * BEAT, bar(15) + 3 * BEAT + 0.3);
      rows.forEach(([c, r], i) => {
        const t0 = bar(15) + i * BEAT; if (t < t0) return;
        slam(ctx, c, 70, 1260 + i * 130, t, t0, { size: 72, color: f ? '#fff' : WARM, bold: true, from: 1.2 });
        slam(ctx, '> ' + r, 610, 1260 + i * 130, t, t0 + 0.12, { size: 72, color: f ? '#ffd9a0' : AMBER, from: 1.2 });
      });
    } },
    // a night of it, line after line
    { a: bar(16), b: bar(17), draw(ctx, t, frame) {
      shot(ctx, t, frame, DIALCAM[0] * (1 + 0.03 * hit(t)), DIALCAM[1], DIALCAM[2]); header(ctx, t);
      const g = ctx.createLinearGradient(0, 1000, 0, 1200); g.addColorStop(0, 'rgba(0,0,0,0)'); g.addColorStop(1, 'rgba(0,0,0,0.85)'); ctx.fillStyle = g; ctx.fillRect(0, 1000, W, 920);
      const L = [['03:28', 'осколок, бедро', bar(16)], ['03:29', 'жгут. морфий', bar(16) + BEAT], ['03:31', 'следующий', bar(16) + 2 * BEAT]];
      L.forEach(([c, s, t0], i) => { if (t < t0) return; txt(ctx, c, 70, 1260 + i * 110, { size: 52, color: GREYT, alpha: K.prog(t, t0, t0 + 0.1) }); txt(ctx, decodeS(s, t, t0, 0.18), 270, 1260 + i * 110, { size: 60, color: i === 2 ? AMBER : WARM, bold: i === 2 }); });
    } },
    // the ether floods: calls pile up faster than he can answer
    { a: bar(17), b: bar(18), draw(ctx, t, frame) {
      shot(ctx, t, frame, DIALCAM[0] * (1 + 0.03 * hit(t)), DIALCAM[1], DIALCAM[2]); header(ctx, t);
      ctx.fillStyle = 'rgba(0,0,0,0.55)'; ctx.fillRect(0, 0, W, H);
      const C = ['Hilfe!', 'Help!', 'Помогите!', '> иду', 'Sanitäter!', 'Medic!', 'Санитара!', 'Mama!', '> иду'];
      C.forEach((s, i) => {
        const t0 = bar(17) + i * BEAT / 2; if (t < t0) return;
        const me = s[0] === '>', age = (t - t0) / BAR;
        slam(ctx, s, 120 + K.hash(i, 11) * 520, 380 + K.hash(i, 12) * 1250, t, t0, { size: me ? 64 : 84 + K.hash(i, 13) * 40, color: me ? AMBER : WARM, bold: !me, alpha: Math.max(0.25, 1 - age * 0.8) });
      });
    } },
    // overload: the glass goes white, the needle can't hold
    { a: bar(18), b: CODA, draw(ctx, t, frame) {
      const p = K.prog(t, bar(18), CODA);
      shot(ctx, t, frame, K.lerp(1.6, 2.6, K.ease.inQuad(p)), 540, K.lerp(1000, 780, p), { dx: (K.hash(frame, 3) - 0.5) * 20 * p });
      header(ctx, t);
      ticker(ctx, THEIR.map(m => ({ ...m, color: WARM })), t, 0, 1500, W, { pxs: 900, h: 22 });
      ticker(ctx, THEIR.map(m => ({ ...m, color: AMBER })), t - 0.11, 0, 1580, W, { pxs: 700, h: 18 });
      if (t > CODA - 0.55) staticBurst(ctx, frame, K.prog(t, CODA - 0.55, CODA));
    } },
    // ВАРШАВА: a torn picture, SOS
    { a: CODA, b: bar(128), draw(ctx, t, frame) {
      const z = 1 + 0.035 * hit(t); ctx.save(); ctx.translate(W / 2, H / 2); ctx.scale(z, z); ctx.translate(-W / 2, -H / 2);
      sstv(ctx, t, frame, { img: 'head', a: CODA, torn: true, shake: true }); ctx.restore();
      plate(ctx, 'ВАРШАВА', t, CODA, { flick: true });
      let x = 70; // the dots and dashes build up big, then read
      for (const q of SOS.els) { if (q.a > t) break; ctx.fillStyle = '#fbf3e6'; if (q.dash) { rr(ctx, x, 1600, 70, 26, 13); ctx.fill(); x += 92; } else { ctx.beginPath(); ctx.arc(x + 14, 1613, 15, 0, 7); ctx.fill(); x += 42; } }
      if (t > CODA + 3 * BEAT) slam(ctx, 'SOS', W / 2, 1800, t, CODA + 3 * BEAT, { size: 120, color: '#fbf3e6', align: 'center', bold: true });
    } },
    // panic: the needle can't choose; old pictures flash on the beats
    { a: bar(128), b: bar(129), draw(ctx, t, frame) {
      const k = hit(t); shot(ctx, t, frame, 2.0 * (1 + 0.05 * k), 540, 760, { dx: (K.hash(frame, 4) - 0.5) * 22 * k, dy: (K.hash(frame, 5) - 0.5) * 22 * k });
      const i = Math.min(7, Math.floor((t - bar(128)) / (BEAT / 2))); plate(ctx, PANIC[i], t, bar(128) + i * BEAT / 2);
      const fl = [['tent', 0], ['coat', 1], ['walk', 2], ['carry', 3]].find(([, j]) => win(t, bar(128) + j * BEAT, bar(128) + j * BEAT + 0.07));
      if (fl) { sstv(ctx, t, frame, { img: fl[0], a: 0, instant: true }); }
    } },
    // the bridge: he carries a man
    { a: bar(129), b: FREEZE, draw(ctx, t, frame) {
      const z = 1 + 0.03 * hit(t); ctx.save(); ctx.translate(W / 2, H / 2); ctx.scale(z, z); ctx.translate(-W / 2, -H / 2);
      sstv(ctx, t, frame, { img: 'carry', a: bar(129), shake: true }); ctx.restore();
      plate(ctx, 'ВАРШАВА', t, bar(129) - 1);
      const b2 = bar(130);
      if (t < b2) { caption(ctx, 'раненый. у моста', t, bar(129), { size: 70, y: 1620 }); reply(ctx, 'иду.', t, bar(129) + BEAT, { y: 1730 }); if (t > bar(129) + 3 * BEAT) reply(ctx, 'несу.', t, bar(129) + 3 * BEAT, { y: 1840 }); }
      else {
        reply(ctx, 'почти дошли.', t, b2, { y: 1700, size: 84 });
        const fb = Math.exp(-since([b2, b2 + BEAT, b2 + 2 * BEAT, b2 + 3 * BEAT], t) * 6) * (0.25 + 0.2 * Math.floor((t - b2) / BEAT));
        const g = ctx.createLinearGradient(0, H, 0, H * 0.3); g.addColorStop(0, `rgba(255,236,200,${fb})`); g.addColorStop(1, 'rgba(255,236,200,0)'); ctx.fillStyle = g; ctx.fillRect(0, 0, W, H);
      }
    } },
    // held breath: the frame stops
    { a: FREEZE, b: BLAST, freeze: FREEZE - 1 / 30 },
    // the blast
    { a: BLAST, b: bar(154), draw(ctx, t, frame) {
      const d = t - BLAST, sh = Math.exp(-d * 3) * 40;
      if (d < 0.1) { ctx.fillStyle = '#fffaf0'; ctx.fillRect(0, 0, W, H); return; }
      shot(ctx, t, frame, 1.39, 540, 1080, { dx: (K.hash(frame, 1) - 0.5) * sh, dy: (K.hash(frame, 2) - 0.5) * sh, white: Math.max(0, 0.9 - d * 1.8), cold: K.prog(t, BLAST + 0.4, BLAST + 1.6) * 0.55 });
      header(ctx, t, { alpha: 0.8 });
      const g = ctx.createLinearGradient(0, 980, 0, 1240); g.addColorStop(0, 'rgba(0,0,0,0)'); g.addColorStop(1, 'rgba(0,0,0,0.8)'); ctx.fillStyle = g; ctx.fillRect(0, 980, W, H - 980);
      // the log bursts into junk glyphs that fall away
      if (d < 1.6) for (let i = 0; i < 70; i++) { const x = 60 + K.hash(i, 1) * 960, y = 1190 + (i % 6) * 64 + d * d * 900 * (0.4 + K.hash(i, 2)); txt(ctx, JUNK[Math.floor(K.hash(i, Math.floor(t * 20)) * JUNK.length)], x, y, { size: 48, color: i % 3 ? WARM : AMBER, alpha: 1 - d / 1.6 }); }
      if (d > 1.4) txt(ctx, '> ' + cursor(t), 62, 1254, { size: 48, color: AMBER, alpha: 0.8 });
      if (d < 0.6) tear(ctx, frame, 1 - d / 0.6);
    } },
    // what is left of the stations
    ...[['БЕРЛИН', 'tent', 'Mama…'], ['ЛОНДОН', 'coat', 'Mum…'], ['МИНСК', 'walk', 'мама…']].map(([st, img, s], i) => {
      const a = bar(154) + i * 2 * BEAT;
      return { a, b: a + 2 * BEAT, draw(ctx, t, frame) {
        sstv(ctx, t, frame, { img, a, broken: true, cold: true });
        plate(ctx, st, t, a, { dim: 0.55, flick: true });
        caption(ctx, s, t, a + BEAT * 0.6, { size: 110, y: 1690, color: '#e9efe8' });
        if (t > a + 2 * BEAT - 0.08) staticBurst(ctx, frame, 0.7);
      } };
    }),
    // he tries to call out; the words break
    { a: bar(155) + 2 * BEAT, b: bar(156), draw(ctx, t, frame) {
      shot(ctx, t, frame, 1.39, 540, 1100, { cold: 0.55 }); header(ctx, t, { alpha: 0.8 });
      const g = ctx.createLinearGradient(0, 980, 0, 1240); g.addColorStop(0, 'rgba(0,0,0,0)'); g.addColorStop(1, 'rgba(0,0,0,0.8)'); ctx.fillStyle = g; ctx.fillRect(0, 980, W, H - 980);
      const s = typedS(BROKEN.s, t, BROKEN.a, BROKEN.t), cr = K.prog(t, BROKEN.t + 0.1, bar(156) - 0.05);
      ctx.save(); ctx.font = `48px ${MONO}`; const cw = ctx.measureText('ж').width; ctx.restore();
      txt(ctx, '>', 62, 1254, { size: 48, color: AMBER });
      for (let i = 0; i < s.length; i++) { const fall = cr * cr * 700 * (0.5 + K.hash(i, 3)) * (K.hash(i, 4) < 0.2 + cr ? 1 : 0); txt(ctx, cr > 0.15 && K.hash(i, Math.floor(t * 24)) < cr ? '·' : s[i], LX + i * cw, 1254 + fall, { size: 48, color: AMBER, alpha: 1 - cr * 0.6 }); }
      if (t > BROKEN.t && cr < 0.2) txt(ctx, '—', LX + s.length * cw, 1254, { size: 48, color: AMBER });
      ticker(ctx, MY.map(m => ({ ...m, color: AMBER })), t, 60, 1110, W - 120);
    } },
    // МОСКВА: the eye half closes and never locks
    { a: bar(156), b: bar(158), draw(ctx, t, frame) {
      const p = K.prog(t, bar(156), bar(158));
      shot(ctx, t, frame, K.lerp(1.6, 1.5, p), 540, 1000, { cold: 0.5 }); header(ctx, t, { alpha: 0.8 });
      const g = ctx.createLinearGradient(0, 1000, 0, 1200); g.addColorStop(0, 'rgba(0,0,0,0)'); g.addColorStop(1, 'rgba(0,0,0,0.82)'); ctx.fillStyle = g; ctx.fillRect(0, 1000, W, 920);
      if (t > bar(156) + 0.7 && t < bar(157) + 0.4) plate(ctx, 'МОСКВА', t, bar(156) + 0.7, { dim: 0.7 });
      ticker(ctx, MAMA.map(m => ({ ...m, color: AMBER })), t, 60, 1250, W - 120, { pxs: 420, h: 20 });
      if (t > bar(157)) txt(ctx, decodeS('нет ответа', t, bar(157), 0.2), 62, 1400, { size: 56, color: GREYT });
    } },
    // the valves die; the room in moonlight
    { a: bar(158), b: CLICK, draw(ctx, t, frame) {
      const p = K.ease.inOutSine(K.prog(t, bar(158), CLICK));
      shot(ctx, t, frame, K.lerp(1.5, 1.0, p), 540, K.lerp(1000, 960, p), { cold: 0.55 });
      header(ctx, t, { alpha: 0.6 * (1 - p) });
      txt(ctx, '> ' + (Math.floor(t / BEAT) % 2 ? '▌' : ' '), 62, 1500, { size: 48, color: AMBER, alpha: 0.7 * (1 - p) });
    } },
    // click. silence. the answer again
    { a: CLICK, b: END, draw(ctx, t, frame) {
      ctx.fillStyle = '#000'; ctx.fillRect(0, 0, W, H);
      const ag = Math.max(0, 1 - (t - CLICK) / 0.8);
      if (ag > 0) { // the dial window and the eye fade like a filament
        const d0 = toW(DIAL.x, DIAL.y), w = DIAL.w * RS, h = DIAL.h * RS;
        ctx.save(); ctx.translate(W / 2, H / 2); ctx.translate(-540, -960);
        ctx.strokeStyle = `rgba(255,120,40,${0.5 * ag})`; ctx.lineWidth = 3; rr(ctx, d0.x, d0.y, w, h, 18); ctx.stroke();
        ctx.fillStyle = `rgba(80,255,150,${0.6 * ag})`; ctx.beginPath(); ctx.arc(EYE_W.x, EYE_W.y, 3 + 30 * ag * ag, 0, 7); ctx.fill();
        ctx.restore();
      }
      const fo = 1 - K.prog(t, END - 0.6, END);
      if (t > CLICK + 0.8) slam(ctx, decodeS('(ОДНО', t, CLICK + 0.8, 0.25), W / 2, 900, t, CLICK + 0.8, { size: 140, color: RED, align: 'center', bold: true, glow: 'rgba(255,40,30,0.55)', blur: 34, from: 1.1, alpha: fo });
      if (t > CLICK + 1.1) slam(ctx, decodeS('И ТО ЖЕ)', t, CLICK + 1.1, 0.3), W / 2, 1070, t, CLICK + 1.1, { size: 140, color: RED, align: 'center', bold: true, glow: 'rgba(255,40,30,0.55)', blur: 34, from: 1.1, alpha: fo });
    } },
  ];
  const CUTS = [bar(3), DROP, bar(12), bar(13), bar(14), bar(15), bar(16), bar(17), bar(128), bar(129), bar(154), bar(154) + 2 * BEAT, bar(155), bar(155) + 2 * BEAT, bar(156)];

  // ================= sound automation for audio/radio_mix.py =================
  const BURSTS = [[DROP + 2.2, DROP + 2.37, 0.9], ...[12, 13, 14].map(k => [bar(k) + BAR - BEAT / 2, bar(k + 1), 0.75]), [CODA - 0.55, CODA, 1],
    ...PANIC.map((_, i) => [bar(128) + i * BEAT / 2 - 0.05, bar(128) + i * BEAT / 2, 0.5]), ...CROWD.map((_, i) => [bar(17) + i * BEAT / 2 - 0.04, bar(17) + i * BEAT / 2, 0.4]),
    [bar(154) - 0.2, bar(154), 0.6], [bar(154) + 2 * BEAT - 0.08, bar(154) + 2 * BEAT, 0.6], [bar(155) - 0.08, bar(155), 0.6], [bar(155) + 2 * BEAT - 0.08, bar(155) + 2 * BEAT, 0.5], [BLAST, BLAST + 0.6, 1]];
  const curves = t => {
    const cl = t < DROP ? Math.max(tuned(t) * 0.8, ...LOG.filter(m => !m.me && !m.st).map(m => win(t, m.a, m.t + 1.2) ? 0.3 : 0))
      : t < CODA ? 1 : t < BLAST ? 0.72 : t < bar(154) ? 0 : t < bar(156) ? 0.12 : t < bar(157) ? 0.15 + 0.5 * K.prog(t, bar(156), bar(156) + 1.2) : 0;
    let pw = t < 0.15 ? 0 : K.ease.outCubic(K.prog(t, 0.15, 1.8));
    if (win(t, FREEZE, BLAST)) pw = 0;
    if (t >= bar(154)) pw = 0.8;
    if (t >= bar(158)) pw = K.lerp(0.8, 0.2, K.prog(t, bar(158), CLICK));
    if (t >= CLICK) pw = 0;
    const bu = BURSTS.reduce((m, [a, b, v]) => win(t, a, b) ? Math.max(m, v * (b - a > 0.3 ? K.prog(t, a, b) : 1)) : m, 0);
    const d = t - BLAST, imp = list => list.some(s => win(t, s, s + 0.02)) ? 1 : 0;
    return {
      tuned: +cl.toFixed(3), power: +pw.toFixed(3), burst: +bu.toFixed(3),
      morse: keyDown(MY, t) ? 1 : 0, morse2: keyDown(THEIR, t) ? 1 : 0,
      siren: +(win(t, bar(18), CODA) ? 0.8 * K.prog(t, bar(18), CODA) : win(t, CODA, bar(129)) ? 0.55 : 0).toFixed(3),
      over: +(d >= 0 && d < 2.4 ? Math.max(0, 1 - d / 2.4) : 0).toFixed(3), boom: +(d >= 0 && d < 3 ? Math.exp(-d * 1.4) : 0).toFixed(3),
      ring: +(d > 0.25 && d < 5 ? Math.exp(-(d - 0.25) * 0.7) : 0).toFixed(3),
      whistle: win(t, bar(130), FREEZE) ? 1 : 0, whf: +(2500 - 1900 * K.prog(t, bar(130), FREEZE)).toFixed(0),
      narrow: +(t >= bar(157) && t < CLICK ? 1 : t >= bar(154) && t < bar(156) ? 0.6 : 0).toFixed(3),
      click: imp([CLICK, 0.15]), pop: imp([BLAST + 0.59, BLAST + 1.18, BLAST + 1.77, bar(158), bar(158) + 2 * BEAT, bar(157) + 1.25]),
    };
  };

  window.VERSION = {
    duration: END,
    curvesDt: 0.01,
    curves,
    images: { tent: '/assets/images/medic-tent.png', coat: '/video/assets/up/c_white_coat.jpg', walk: '/video/assets/up/c_walk_away.jpg', head: '/video/assets/up/c_head_down.jpg', carry: '/video/assets/up/c_carry.jpg' },
    render(ctx, t, frame) {
      let sc = SCENES.find(s => t >= s.a && t < s.b) || SCENES[SCENES.length - 1];
      if (sc.freeze !== undefined) { t = sc.freeze; frame = Math.round(t * 30); sc = SCENES.find(s => t >= s.a && t < s.b); }
      ctx.fillStyle = '#000'; ctx.fillRect(0, 0, W, H);
      ctx.save(); sc.draw(ctx, t, frame); ctx.restore(); ctx.filter = 'none';
      const c = since(CUTS, t);
      if (c < 0.07) { ctx.fillStyle = 'rgba(255,214,150,0.35)'; ctx.fillRect(0, 0, W, H); tear(ctx, frame, 0.8); }
      if (t < CLICK) { K.grain(ctx, frame, 0.07); K.vignette(ctx, 0.55); }
    },
  };
})();
