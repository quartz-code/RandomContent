// Version F — «Кроме меня». Same track and edit grid as the user's two template videos
// (37.8 s, ~95.7 BPM). A night chat builds to "на войне все стреляют", the punchline
// "(кроме меня)" lands, then on the drop a run of pixel-game scenes, each one idea, cut on the
// track's re-entries and punched on its kicks.
(function () {
  const { W, H } = K;
  const RED = '#ff2d3c', WHITE = '#f4f4f4', GREY = '#8a8a8a', DIM = '#4a4a4a', GREEN = '#5dff8a', YEL = '#ffd34d';
  // measured on the track: kicks, and re-entries after each stop (the cut points)
  const KICKS = [17.54, 18.82, 19.27, 19.74, 20.40, 20.83, 21.33, 21.79, 22.42, 23.20, 23.37, 23.82, 24.11, 24.43, 26.61, 26.91,
    28.82, 29.27, 29.73, 30.82, 31.32, 31.80, 32.26, 32.43, 32.57, 33.19, 33.38, 33.61, 33.82, 34.42, 35.05, 35.26, 36.29, 37.34];
  const HATS = [17.88, 18.04, 18.18, 19.99, 20.17, 21.62, 22.26, 22.56, 22.87, 23.19, 23.42, 23.65, 25.39, 26.16, 26.31, 27.17,
    27.41, 27.57, 27.88, 28.05, 28.55, 29.91, 30.06, 30.38, 31.63, 31.78, 33.96, 34.67, 34.92, 35.38, 36.93];
  const since = (list, t) => { let best = 1e9; for (const k of list) if (k <= t) best = Math.min(best, t - k); return best; };
  const kickPulse = t => Math.exp(-since(KICKS, t) * 9);
  const countUpTo = (list, t, a, b) => list.filter(k => k >= a && k <= t && k < b).length;

  // ================= pixel layer =================
  const PX = 4, LW = W / PX, LH = H / PX; // 270 x 480 logical pixels
  let pc, p, tc, tx;
  const initPx = () => {
    if (pc) return;
    pc = K.makeCanvas(LW, LH); p = pc.getContext('2d'); p.imageSmoothingEnabled = false;
    tc = K.makeCanvas(LW, LH); tx = tc.getContext('2d', { willReadFrequently: true });
  };
  const pt = (s, x, y, o = {}) => { // crisp pixel text (thresholded later)
    tx.save();
    tx.font = `${o.size || 8}px "Press Start 2P"`; tx.fillStyle = o.color || WHITE;
    tx.textAlign = o.align || 'center'; tx.textBaseline = 'top';
    if (o.scale) { tx.translate(Math.round(x), Math.round(y)); tx.scale(o.scale, o.scale); tx.fillText(s, 0, 0); } else tx.fillText(s, Math.round(x), Math.round(y));
    tx.restore();
  };
  const typed = (s, t, t0, cps = 28) => s.slice(0, Math.floor(K.clamp((t - t0) * cps, 0, s.length)));
  const crispText = () => {
    const d = tx.getImageData(0, 0, LW, LH), a = d.data;
    for (let i = 3; i < a.length; i += 4) a[i] = a[i] > 100 ? 255 : 0;
    tx.putImageData(d, 0, 0); p.drawImage(tc, 0, 0);
  };
  const rect = (x, y, w, h, col) => { p.fillStyle = col; p.fillRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h)); };
  const frame = (x, y, w, h, col = WHITE) => { rect(x, y, w, 1, col); rect(x, y + h - 1, w, 1, col); rect(x, y, 1, h, col); rect(x + w - 1, y, 1, h, col); };
  const PAL = { w: WHITE, s: '#d8d8d8', g: GREY, d: '#5e5e5e', k: '#121212', r: RED, y: YEL, o: '#ff8a2b', e: GREEN, b: '#9ab8ff' };
  const spr = (rows, x, y, o = {}) => {
    const s = o.scale || 1, w = rows[0].length;
    rows.forEach((row, j) => { for (let i = 0; i < w; i++) {
      const c = row[o.flip ? w - 1 - i : i]; if (c === '.') continue;
      p.fillStyle = o.tint && c !== 'r' && c !== 'k' ? o.tint : PAL[c];
      p.fillRect(Math.round(x + i * s), Math.round(y + j * s), s, s);
    } });
  };
  const sprW = (rows, s = 1) => rows[0].length * s, sprH = (rows, s = 1) => rows.length * s;

  // ---- sprites (12 px wide characters, readable silhouettes) ----
  const KOSTYA_TOP = [
    '....kkkk....', '...kkkkkk...', '...kssssk...', '..kkskkskk..', '...ssssss...', '....ssss....',
    '...wwwwww...', '..wwwwwwww..', '.wwwwwwwwrw.', '.wwwwwwwrrr.', '.ww.wwww.rw.', '.ss.wwww.ss.', '....wwww....',
  ];
  const LEGS = [['....dd.dd...', '....dd.dd...', '....dd.dd...', '...kkk.kkk..'],
    ['....dd..dd..', '...dd...dd..', '...dd....dd.', '..kkk...kkk.'],
    ['.....dd.dd..', '....dd..dd..', '...dd...dd..', '..kkk..kkk..']];
  const kostya = (x, y, o = {}) => { spr(KOSTYA_TOP, x, y, o); spr(LEGS[o.walk ? 1 + (Math.floor(o.walk) % 2) : 0], x, y + 13 * (o.scale || 1), o); };
  const SOLDIER = [
    '...gggg.....', '..gggggg....', '.gggggggg...', '...skks.....', '...ssss.....', '..dddddd....',
    '.dddddddkkkk', '.ddddddkk...', '.dd.dddd....', '....dddd....', '....dd.dd...', '....dd.dd...',
    '....dd.dd...', '...kkk.kkk..',
  ];
  const LYING = ['..........ggg.', '..dddd...gssss', 'dddddddddddsss', 'kk.dddrdd.....', '..............'];
  const CARRY = [
    'ddddddddgg....', 'dddddddgsss...', '....kkkkgg....', '...kkkkkk.....', '...kssssk.....', '..kkskkskk....',
    '...ssssss.....', '...wwwwww.....', '..wwwwwwww....', '.wwwwwwwwrw...', '.ww.wwww.rw...', '.ss.wwww.ss...', '....wwww......',
  ];
  const TOMMY = [ // British soldier, front view: flat "brodie" helmet, arm in a sling
    '..............', '.dddddddddddd.', '...dddddddd...', '....ssssss....', '....skssks....', '....ssssss....',
    '.....ssss.....', '...gggggggg...', '..gggggggggg..', '.ggggwwwwgggg.', '.gg.gwrwwg.gg.', '.ss.gwwwwg.ss.',
    '....gggggg....', '....gg..gg....', '....gg..gg....', '....gg..gg....', '...kkk..kkk...',
  ];
  const HEART = ['.rr.rr.', 'rrrrrrr', 'rrrrrrr', '.rrrrr.', '..rrr..', '...r...'];
  const HEART_E = ['.dd.dd.', 'd..d..d', 'd.....d', '.d...d.', '..d.d..', '...d...'];
  const CROSS = ['..rr..', '..rr..', 'rrrrrr', 'rrrrrr', '..rr..', '..rr..'];
  const BULLET = ['.y.', 'yyy', 'yyy', 'ooo', 'ooo', 'ooo', 'ooo'];
  const LOCK = ['.ddd.', 'd...d', 'd...d', 'ddddd', 'dd.dd', 'dd.dd', 'ddddd'];
  const TROPHY = ['ggggggg', 'gwggggg', '.ggggg.', '..ggg..', '...g...', '..ggg..', '.ggggg.'];
  const RIFLE = ['..............', 'kkdddddddddddd', 'kkkkkdd.......', '....dd........'];
  const PISTOL = ['dddddddd', 'ddddddd.', '.dd.....', '.dd.....', '.dd.....'];
  const GRENADE = ['...dd..', '..dddd.', '.dddddd', '.dddddd', '.dddddd', '..dddd.'];
  const KNIFE = ['..........ds', 'ssssssssssd.', 'sssssssss...'];
  const BANDAGE = ['.wwwwwww.', 'wwwwwwwww', 'wwwwrwwww', 'wwwrrrwww', 'wwwwrwwww', 'wwwwwwwww', '.wwwwwww.'];
  const SYRINGE = ['.s.........', 'sbbbbbbssss', '.s.........'];
  const FLARE = ['..y..', '.yyy.', 'yyyyy', '.yyy.', '..y..'];

  // ================= the chat (hi-res, 0..17.17) =================
  const CHAT = [
    { t: 0.0, s: 'не спишь?', x: 470, y: 760, me: false },
    { t: 2.8, s: 'смена. раненых везут', x: 610, y: 890, me: true, typing: 1.6 },
    { t: 5.2, s: 'можно тупой вопрос?', x: 460, y: 1030, me: false },
    { t: 7.6, s: 'давай', x: 660, y: 1150, me: true, typing: 6.8 },
    { t: 10.4, s: 'ты там хоть раз стрелял?', x: 450, y: 1300, me: false },
    { t: 12.8, s: 'на войне все стреляют', x: 610, y: 1440, me: true, typing: 11.6 },
    { t: 16.4, s: '(кроме меня)', x: 590, y: 1590, me: true, typing: 15.2, pill: true },
  ];
  const bubbleCache = new Map();
  const bubble = (m) => {
    const key = m.s; if (bubbleCache.has(key)) return bubbleCache.get(key);
    const mm = K.makeCanvas(10, 10).getContext('2d'); mm.font = '600 50px Manrope';
    const w = Math.ceil(mm.measureText(m.s).width + 84), h = 104, G = 60;
    const c = K.makeCanvas(w + G * 2, h + G * 2), x = c.getContext('2d');
    x.translate(G, G);
    if (m.pill) {
      x.shadowColor = 'rgba(255,45,60,0.95)'; x.shadowBlur = 36;
      x.strokeStyle = '#ff4a57'; x.lineWidth = 5; x.beginPath(); x.roundRect(3, 3, w - 6, h - 6, 40); x.stroke();
      x.shadowBlur = 0; x.fillStyle = 'rgba(30,4,8,0.9)'; x.beginPath(); x.roundRect(6, 6, w - 12, h - 12, 36); x.fill();
    } else {
      const g = x.createLinearGradient(0, 0, 0, h);
      g.addColorStop(0, m.me ? '#4a4a50' : '#36363a'); g.addColorStop(1, m.me ? '#3a3a40' : '#28282b');
      x.fillStyle = g; x.beginPath(); x.roundRect(0, 0, w, h, 34); x.fill();
      x.strokeStyle = 'rgba(255,255,255,0.09)'; x.lineWidth = 2; x.stroke();
    }
    x.font = '600 50px Manrope'; x.fillStyle = '#efefef'; x.textBaseline = 'middle'; x.textAlign = 'center';
    x.fillText(m.s, w / 2, h / 2 + 2);
    const b = { c, w, h, G }; bubbleCache.set(key, b); return b;
  };
  // camera snaps to each new bubble (fast ease with a little overshoot and a rotation kick)
  const chatCam = t => {
    let cx = CHAT[0].x, cy = CHAT[0].y, rot = 0, kick = 0;
    for (const m of CHAT) {
      const t0 = m.typing ?? m.t;
      if (t >= t0) {
        const q = K.clamp((t - t0) / 0.42), e = 1 - Math.pow(1 - q, 4);
        cx = K.lerp(cx, m.x, e); cy = K.lerp(cy, m.y, e);
        kick = Math.sin(q * Math.PI) * (m.me ? 1 : -1);
      }
      if (t >= m.t) { const lt = t - m.t; kick += Math.exp(-lt * 7) * Math.sin(lt * 18) * 0.5; }
    }
    rot = (-5 + 2.5 * Math.sin(t * 0.4) + kick * 2.2) * Math.PI / 180;
    const z = 1.55 - 0.12 * K.prog(t, 0, 17) + 0.05 * Math.exp(-since(CHAT.map(m => m.t), t) * 6);
    return { cx, cy, z, rot };
  };
  const chat = (ctx, t) => {
    ctx.fillStyle = '#000'; ctx.fillRect(0, 0, W, H);
    const g = ctx.createRadialGradient(W / 2, H / 2, 80, W / 2, H / 2, 1100);
    g.addColorStop(0, '#1e1e1f'); g.addColorStop(1, '#000'); ctx.fillStyle = g; ctx.fillRect(0, 0, W, H);
    const c = chatCam(t);
    ctx.save(); ctx.translate(W / 2, H / 2); ctx.rotate(c.rot); ctx.scale(c.z, c.z); ctx.translate(-c.cx, -c.cy);
    const ha = K.prog(t, 4.4, 5.0);
    if (ha > 0) { ctx.font = '500 26px Manrope'; ctx.textAlign = 'center'; ctx.fillStyle = `rgba(150,150,150,${ha * 0.8})`; ctx.fillText('Сегодня 3:12', 540, 640); }
    const last = CHAT.filter(m => t >= m.t).pop();
    for (const m of CHAT) {
      if (m.typing !== undefined && t >= m.typing && t < m.t) {
        const a = K.prog(t, m.typing, m.typing + 0.15), pop = 0.7 + 0.3 * K.ease.outBack(K.clamp((t - m.typing) / 0.25));
        ctx.save(); ctx.globalAlpha = a; ctx.translate(m.x, m.y); ctx.scale(pop, pop);
        ctx.fillStyle = m.me ? '#424248' : '#323235'; ctx.beginPath(); ctx.roundRect(-64, -42, 128, 84, 42); ctx.fill();
        for (let k = 0; k < 3; k++) { const j = Math.max(0, Math.sin(t * 9 - k * 0.9)); ctx.fillStyle = `rgba(235,235,235,${0.35 + 0.65 * j})`; ctx.beginPath(); ctx.arc(-30 + k * 30, -j * 8, 9, 0, 7); ctx.fill(); }
        ctx.restore();
      }
      if (t < m.t) continue;
      const lt = t - m.t, b = bubble(m);
      const slide = m === CHAT[0] ? (1 - K.ease.outCubic(K.clamp(lt / 0.35))) * 600 : 0;
      const pop = m === CHAT[0] ? 1 : 0.75 + 0.25 * (1 - Math.exp(-10 * lt) * Math.cos(12 * lt));
      const focus = m === last ? 1 : 0.38;
      ctx.save(); ctx.globalAlpha = K.clamp(lt / 0.12) * focus;
      ctx.translate(m.x + slide, m.y); ctx.scale(pop, pop);
      if (m.pill && t > 16.8) { // disintegrates into pixels
        const q = K.prog(t, 16.8, 17.17), r = K.rng(5);
        for (let k = 0; k < 160; k++) {
          const sx = (r() - 0.5) * b.w, sy = (r() - 0.5) * b.h;
          ctx.fillStyle = r() > 0.65 ? RED : '#ddd'; ctx.globalAlpha = (1 - q);
          ctx.fillRect(sx + (r() - 0.5) * 320 * q, sy + (r() - 0.6) * 260 * q, 16, 16);
        }
      } else ctx.drawImage(b.c, -b.w / 2 - b.G, -b.h / 2 - b.G);
      const sp = K.prog(t, m.t + 0.04, m.t + 0.5);
      if (sp > 0 && sp < 1 && !m.pill) {
        ctx.beginPath(); ctx.roundRect(-b.w / 2, -b.h / 2, b.w, b.h, 34); ctx.clip();
        const sx = K.lerp(-b.w, b.w, sp), sg = ctx.createLinearGradient(sx - 70, 0, sx + 70, 0);
        sg.addColorStop(0, 'rgba(255,255,255,0)'); sg.addColorStop(0.5, 'rgba(255,255,255,0.5)'); sg.addColorStop(1, 'rgba(255,255,255,0)');
        ctx.fillStyle = sg; ctx.fillRect(-b.w / 2, -b.h / 2, b.w, b.h);
      }
      ctx.restore();
    }
    ctx.restore();
    if (t > 16.4 && t < 16.55) { ctx.fillStyle = 'rgba(255,40,60,0.18)'; ctx.fillRect(0, 0, W, H); }
  };

  // ================= pixel scenes (drop: 17.17..37.8) =================
  const stars = (t, n = 36) => { const r = K.rng(3); for (let i = 0; i < n; i++) { const x = r() * LW, y = r() * LH, ph = r() * 6; if (Math.sin(t * 3 + ph) > -0.2) rect(x, y, 1, 1, r() > 0.8 ? GREY : '#3c3c3c'); } };
  const ground = y => rect(0, y, LW, 1, '#3a3a3a');
  const dialog = (s, t, t0, y = 330) => { rect(18, y, LW - 36, 36, '#000'); frame(18, y, LW - 36, 36); frame(20, y + 2, LW - 40, 32, DIM); pt('* ' + typed(s, t, t0), 30, y + 14, { align: 'left' }); };
  const flash = (t, t0, col = 'rgba(255,255,255,') => { const a = 1 - K.clamp((t - t0) / 0.12); if (a > 0) rect(0, 0, LW, LH, col + (a * 0.85) + ')'); };

  const SCENES = [
    // patrons x 0 (the "lives x 0" beat)
    { a: 17.17, b: 17.53, cam: { z0: 1.15, z1: 1.0 }, draw(t) {
      const g = p.createRadialGradient(LW / 2, LH / 2, 10, LW / 2, LH / 2, 230);
      g.addColorStop(0, '#6a6a6a'); g.addColorStop(1, '#000'); p.fillStyle = g; p.fillRect(0, 0, LW, LH);
      pt('ПАТРОНЫ', LW / 2, 196, { color: '#cfcfcf' });
      spr(BULLET, 86, 214, { scale: 5 }); pt('X 0', 170, 226, { size: 24 });
    } },
    // title screen
    { a: 17.53, b: 18.78, cam: { z0: 1.25, z1: 1.0, shake: 1 }, draw(t) {
      stars(t);
      const r = K.rng(11); for (let i = 0; i < 12; i++) { const x = r() * (LW - 8), y = (r() * LH + t * (10 + r() * 14)) % LH; spr(CROSS, x, LH - y, { tint: i % 3 ? RED : '#6d1a20' }); }
      const slam = K.ease.outBack(K.clamp((t - 17.53) / 0.22));
      pt('НА ВОЙНЕ', LW / 2, 112, { size: 16 });
      pt('ВСЕ', LW / 2, 140, { size: 16 });
      const s = 0.4 + 0.6 * slam + 0.06 * kickPulse(t);
      tx.save(); tx.translate(LW / 2, 196); tx.scale(s, s); pt('СТРЕЛЯЮТ', 0, -12, { size: 24, color: RED }); tx.restore();
      if (t > 17.88) { pt('1 ИГРОК', LW / 2 + 8, 268); if (Math.floor(t * 6) % 2) pt('>', 84, 268); }
      if (t > 18.04) { pt('АТАКА', LW / 2 - 6, 290, { color: DIM }); spr(LOCK, 168, 288, { scale: 2 }); }
      if (t > 18.18) pt('СТРЕЛЯТЬ НЕЛЬЗЯ', LW / 2, 316, { color: RED });
    } },
    // everyone shoots
    { a: 18.78, b: 19.26, cam: { z0: 1.0, z1: 1.12, shake: 1 }, draw(t) {
      stars(t, 18); ground(330);
      spr(SOLDIER, 34, 330 - 42, { scale: 3 }); spr(SOLDIER, 200, 330 - 42, { scale: 3, flip: true });
      const f = Math.floor((t - 18.78) / 0.08) % 2 === 0;
      if (f) { spr(FLARE, 76, 300, { scale: 2 }); spr(FLARE, 182, 300, { scale: 2 }); }
      for (let k = 0; k < 4; k++) { const q = ((t - 18.78) * 3 + k / 4) % 1; rect(80 + q * 110, 305, 6, 1, YEL); rect(190 - q * 110, 308, 6, 1, YEL); }
      pt('БАХ!', LW / 2, 236, { size: 16, color: YEL, scale: 1 + 0.2 * kickPulse(t) });
    } },
    // he walks right through it
    { a: 19.26, b: 19.73, cam: { z0: 1.15, z1: 1.0, shake: 1 }, draw(t) {
      stars(t, 18); ground(330);
      spr(SOLDIER, 20, 330 - 42, { scale: 3 }); spr(SOLDIER, 214, 330 - 42, { scale: 3, flip: true });
      kostya(LW / 2 - 18, 330 - 51, { scale: 3, walk: (t - 19.26) * 10 });
      for (let k = 0; k < 3; k++) { const q = ((t - 19.26) * 4 + k / 3) % 1; rect(64 + q * 150, 300 + k * 6, 6, 1, YEL); }
      pt('MISS', LW / 2, 230, { size: 16, color: RED, scale: 1 + 0.25 * kickPulse(t) });
    } },
    // a wounded one is waiting: "player 2"
    { a: 19.73, b: 20.82, cam: { z0: 1.0, z1: 1.08 }, draw(t) {
      stars(t, 18); ground(300);
      const run = K.ease.inOutCubic(K.prog(t, 20.40, 20.8));
      kostya(40 + run * 90, 300 - 51, { scale: 3, walk: run > 0 && run < 1 ? t * 12 : 0 });
      const cx = 200, cy = 270, r = 30;
      for (let k = 0; k < 28; k++) { const a = k / 28 * 6.283 + t * 1.2; if (k % 2) rect(cx + Math.cos(a) * r, cy + Math.sin(a) * r, 2, 2, run > 0.9 ? RED : WHITE); }
      spr(LYING, cx - 14, cy + 4, { scale: 2, tint: run > 0.9 ? undefined : '#666' });
      pt('P2', cx, cy - 46, { color: GREY });
      dialog(t < 20.40 ? 'РАНЕНЫЙ ЖДЁТ...' : 'ИДУ!', t, t < 20.40 ? 19.8 : 20.40, 340);
    } },
    // high scores: rows land on the kicks
    { a: 20.82, b: 22.42, cam: { z0: 1.1, z1: 1.0 }, draw(t) {
      stars(t, 14);
      spr(TROPHY, LW / 2 - 7, 84, { scale: 2 });
      pt('ТАБЛИЦА РЕКОРДОВ', LW / 2, 110);
      pt('УБИТО', 234, 132, { color: GREY, align: 'right' });
      const rows = [['1', 'ГАНС', '99'], ['2', 'ТОММИ', '87'], ['3', 'ФРИЦ', '76'], ['4', 'ДЖОН', '65']];
      const n = 1 + countUpTo(KICKS, t, 20.9, 22.4) + countUpTo(HATS, t, 21.0, 22.2);
      rows.slice(0, n).forEach((r, i) => { const y = 150 + i * 18; pt(r[0], 30, y, { color: GREY, align: 'left' }); pt(r[1], 50, y, { align: 'left' }); pt(r[2], 234, y, { align: 'right' }); });
      if (t > 22.0) {
        const bl = Math.floor(t * 10) % 2 ? RED : '#ff8a93';
        rect(24, 228, LW - 48, 1, DIM);
        pt('КОСТЯ', 50, 240, { align: 'left', color: bl }); pt('0', 234, 240, { align: 'right', color: bl, scale: 1 + 0.4 * kickPulse(t) });
        pt('СПАСЕНО: 312', LW / 2, 272, { color: GREEN });
      }
    } },
    // RPG encounter
    { a: 22.42, b: 24.10, cam: { z0: 1.0, z1: 1.06 }, draw(t) {
      pt('РАНЕНЫЙ БРИТАНЕЦ', LW / 2, 56, { color: GREY });
      const shake = t < 22.6 ? Math.round(Math.sin(t * 70) * 2) : 0, sway = Math.round(Math.sin(t * 5) * 1);
      spr(TOMMY, LW / 2 - 35 + shake, 66 + sway, { scale: 5 });
      const hp = t < 23.82 ? 0.1 : 0.1 + 0.9 * K.ease.outCubic(K.prog(t, 23.82, 24.05));
      pt('HP', 48, 158, { color: GREY, align: 'left' }); frame(70, 156, 150, 10, GREY); rect(72, 158, 146 * hp, 6, hp > 0.5 ? GREEN : RED);
      const step = countUpTo(HATS, t, 22.5, 23.3);
      const sel = t >= 23.37 ? 1 : Math.min(step, 3) % 4 === 0 ? 0 : 0;
      const msg = t < 22.87 ? 'ДИКИЙ РАНЕНЫЙ ПОЯВИЛСЯ!' : t < 23.37 ? 'АТАКОВАТЬ НЕЛЬЗЯ.' : t < 23.82 ? 'ТЫ ЛЕЧИШЬ.' : 'ВРАГ ИСЦЕЛЁН!';
      const m0 = t < 22.87 ? 22.45 : t < 23.37 ? 22.87 : t < 23.82 ? 23.37 : 23.82;
      dialog(msg, t, m0, 182);
      const opts = ['АТАКА', 'ЛЕЧИТЬ', 'МОРФИЙ', 'БЕЖАТЬ'];
      opts.forEach((o, i) => {
        const x = 18 + i * 60, on = i === (t < 23.37 ? (t > 22.87 ? 0 : -1) : 1);
        if (on) rect(x, 228, 56, 18, i === 0 ? '#3a0d12' : RED);
        frame(x, 228, 56, 18, on ? RED : DIM); pt(o, x + 28, 233, { color: i === 0 ? DIM : WHITE });
      });
      if (t > 22.87 && t < 23.37) { const sx = Math.round(Math.sin(t * 60) * 2); spr(LOCK, 40 + sx, 252, { scale: 2 }); }
      pt('ТЫ', 18, 274, { color: GREY, align: 'left' }); frame(44, 272, 100, 10, GREY); rect(46, 274, 96, 6, GREEN); pt('СКОР 9', 152, 274, { color: GREY, align: 'left' });
    } },
    // enemy → patient
    { a: 24.10, b: 24.42, cam: { z0: 1.2, z1: 1.0, shake: 1 }, draw(t) {
      pt('ВРАГ', LW / 2, 200, { size: 24, color: GREY });
      rect(70, 210, 130, 3, RED);
      pt('ПАЦИЕНТ', LW / 2, 240, { size: 16, color: GREEN, scale: 1 });
    } },
    // platformer: carry him up under sniper fire
    { a: 24.42, b: 26.60, cam: { z0: 1.0, z1: 1.0, follow: true }, draw(t) {
      pt('УРОВЕНЬ 1: ДОНЕСТИ', LW / 2, 46, { color: GREY });
      for (let i = 0; i < 9; i++) { const h = 50 + i * 24; rect(i * 30, LH - h, 30, h, '#1c1c1c'); frame(i * 30, LH - h, 30, h, '#333'); }
      const step = Math.min(8, Math.floor((t - 24.42) / 0.24));
      const hop = Math.sin(K.clamp(((t - 24.42) % 0.24) / 0.24) * Math.PI) * 6 * (step < 8 ? 1 : 0);
      const x = 4 + step * 30, y = LH - (50 + step * 24) - 26 - hop;
      spr(CARRY, x, y, { scale: 2 });
      spr(CROSS, 258, LH - (50 + 8 * 24) - 16, { scale: 2 });
      for (const s of [25.39, 26.16, 26.31]) { const q = K.prog(t, s - 0.16, s); if (q > 0 && q < 1) rect(LW - q * (LW - x - 10), y + 12, 8, 1, YEL); }
      const miss = since(HATS.filter(h => h >= 25.3 && h < 26.2), t);
      if (miss < 0.3) pt('MISS', x + 34, y - 20, { size: 16, color: RED });
      if (t > 26.31) { pt('-1', x + 30, y - 14, { color: RED }); pt('СКОРОСТЬ 9 > 7', LW / 2, 74, { color: RED }); }
    } },
    // grenade
    { a: 26.60, b: 26.91, cam: { z0: 1.0, z1: 1.2 }, draw(t) {
      stars(t, 12);
      const q = K.prog(t, 26.60, 26.9);
      spr(GRENADE, LW / 2 - 10, 120 + q * 140, { scale: 3 });
      kostya(LW / 2 + 30, 280, { scale: 3 });
      pt('!', LW / 2 + 48, 250, { size: 16, color: RED });
    } },
    // boom → achievement → the wounded man's hearts fill on the hats
    { a: 26.91, b: 28.79, cam: { z0: 1.0, z1: 1.05, shake: 1 }, draw(t) {
      flash(t, 26.91, 'rgba(255,220,150,');
      const ex = K.prog(t, 26.91, 27.15), r = K.rng(7);
      if (ex < 1) for (let k = 0; k < 60; k++) { const a = r() * 6.28, d = r() * 90 * ex; rect(LW / 2 + Math.cos(a) * d, 240 + Math.sin(a) * d, 4, 4, r() > 0.5 ? PAL.o : YEL); }
      const toast = K.ease.outBack(K.clamp((t - 27.0) / 0.25));
      if (t > 27.0) { const y = 40 + (1 - toast) * -60; rect(30, y, LW - 60, 40, '#000'); frame(30, y, LW - 60, 40, YEL); pt('ДОСТИЖЕНИЕ', LW / 2, y + 8, { color: YEL }); pt('ШРАМ НА ЛБУ', LW / 2, y + 24); }
      pt('РАНЕНЫЙ', LW / 2, 176, { color: RED }); pt('HP', LW / 2, 188, { color: GREY });
      const n = Math.min(5, countUpTo(HATS, t, 27.1, 28.3));
      for (let i = 0; i < 5; i++) { const pop = i === n - 1 ? 1 + 0.4 * Math.exp(-since(HATS, t) * 8) : 1; spr(i < n ? HEART : HEART_E, 52 + i * 34 - (pop - 1) * 10, 206 - (pop - 1) * 8, { scale: Math.round(3 * pop) }); }
      if (n > 0) pt('+1', 52 + (n - 1) * 34 + 10, 236, { color: GREEN });
      kostya(78, 290, { scale: 3 }); spr(LYING, 126, 316, { scale: 3 });
      if (Math.floor(t * 4) % 2) spr(SYRINGE, 104, 298, { scale: 2 });
    } },
    // choose a weapon: only the bandage is left
    { a: 28.79, b: 30.81, cam: { z0: 1.08, z1: 1.0 }, draw(t) {
      pt('ВЫБЕРИ ОРУЖИЕ', LW / 2, 78, { size: 16 });
      const L = [[RIFLE, 'НЕТ'], [PISTOL, 'ПУСТОЙ'], [GRENADE, 'НЕТ'], [KNIFE, 'НЕТ'], [SYRINGE, 'МОРФИЙ'], [BANDAGE, 'ВЗЯТЬ']];
      const cur = t < 29.27 ? 0 : t < 29.73 ? 2 + (Math.floor((t - 29.27) / 0.12) % 3) : 5;
      L.forEach(([s, lab], i) => {
        const x = 21 + (i % 3) * 78, y = 120 + Math.floor(i / 3) * 90, isB = i === 5;
        frame(x, y, 72, 72, i === cur ? RED : DIM);
        const sc = s[0].length > 10 ? 4 : 5; spr(s, x + 36 - sprW(s, sc) / 2, y + 30 - sprH(s, sc) / 2, { scale: sc });
        if (!isB && i !== 4) { p.strokeStyle = RED; p.lineWidth = 2; p.beginPath(); p.moveTo(x + 6, y + 6); p.lineTo(x + 66, y + 66); p.stroke(); }
        pt(lab, x + 36, y + 58, { color: isB ? (t >= 29.73 ? GREEN : WHITE) : i === 4 ? GREY : RED });
      });
      if (t >= 29.73) { const b = Math.exp(-(t - 29.73) * 6); rect(177 - b * 6, 208 - b * 6, 76 + b * 12, 2, GREEN); pt('ВЫБРАНО', LW / 2, 316, { color: GREEN, scale: 1 + 0.3 * kickPulse(t) }); }
    } },
    // the field under a hanging flare: on each kick someone goes down, he is already there
    { a: 30.81, b: 34.10, cam: { z0: 1.0, z1: 1.1 }, draw(t) {
      const fl = 0.75 + 0.25 * kickPulse(t);
      const g = p.createRadialGradient(LW / 2, 70, 2, LW / 2, 70, 240); g.addColorStop(0, `rgba(255,220,150,${0.38 * fl})`); g.addColorStop(1, 'rgba(0,0,0,0)');
      p.fillStyle = g; p.fillRect(0, 0, LW, LH);
      spr(FLARE, LW / 2 - 5, 58 + (t - 30.81) * 5, { scale: 2 });
      for (let j = 0; j < 5; j++) for (let i = 0; i < 9; i++) { const lit = (i + j + countUpTo(KICKS, t, 30, 40)) % 3 === 0; rect(i * 30, 330 + j * 20, 30, 20, (i + j) % 2 ? (lit ? '#5a1218' : '#1d1d1d') : (lit ? '#cfcfcf' : '#4a4a4a')); }
      const spots = [[36, 0], [200, 1], [118, 0], [236, 0], [80, 1], [160, 1]];
      const n = countUpTo(KICKS, t, 30.8, 34.1);
      spots.forEach(([x, row], i) => {
        const down = i < n, y = 330 - (row ? 0 : 6);
        if (!down) { spr(SOLDIER, x - 10, y - 42, { scale: 3, flip: row === 1 }); if (Math.floor((t + i * 0.13) / 0.1) % 4 === 0) spr(FLARE, x + (row ? -16 : 24), y - 26); }
        else { spr(LYING, x - 16, y - 12, { scale: 3 }); if (i === n - 1) pt('+1', x + 4, y - 34, { color: GREEN, size: 16, scale: 1 + 0.3 * kickPulse(t) }); }
      });
      const tgt = spots[Math.max(0, Math.min(5, n - 1))];
      kostya(tgt[0] + 14, 330 - 51, { scale: 3, walk: since(KICKS, t) < 0.2 ? t * 14 : 0 });
      pt('СПАСЕНО: ' + (312 + n), LW / 2, 410, { color: GREEN });
    } },
    // "все"
    { a: 34.10, b: 36.29, cam: { z0: 1.0, z1: 1.15 }, draw(t) {
      stars(t, 20); ground(320);
      for (let i = 0; i < 7; i++) if (i !== 3) spr(SOLDIER, 6 + i * 38, 320 - 28, { scale: 2, tint: '#3c3c3c', flip: i > 3 });
      kostya(3 * 38 + 6, 320 - 34, { scale: 2 });
      pt(typed('все', t, 34.45, 4), 40, 230, { color: GREY, align: 'left', size: 16 });
    } },
    // punchline
    { a: 36.29, b: 37.85, cam: { z0: 1.3, z1: 1.0, shake: 1 }, draw(t) {
      const q = K.prog(t, 37.05, 37.6), gl = Math.floor(t * 24) % 5 === 0 ? 3 : 0;
      if (q === 0) {
        pt('(КРОМЕ', LW / 2 + gl, 186, { size: 24 });
        pt('НЕГО)', LW / 2 - gl - 12, 220, { size: 24, color: RED });
        spr(CROSS, LW / 2 + 60, 218, { scale: 4 });
      } else { const r = K.rng(21); for (let k = 0; k < 300; k++) { const x = 30 + r() * 210, y = 186 + r() * 60; rect(x + (r() - 0.5) * 40 * q, y + r() * 180 * q, 3, 3, r() > 0.6 ? RED : '#ccc'); } }
    } },
  ];

  const pixelFrame = (ctx, t, frame) => {
    initPx();
    p.fillStyle = '#000'; p.fillRect(0, 0, LW, LH); tx.clearRect(0, 0, LW, LH);
    const s = SCENES.find(x => t >= x.a && t < x.b);
    if (s) s.draw(t);
    crispText();
    // camera: per-scene push + kick punch + shake on hits
    const lp = s ? K.prog(t, s.a, s.b) : 0, cam = s ? s.cam : { z0: 1, z1: 1 };
    let z = K.lerp(cam.z0, cam.z1, K.ease.outCubic(lp)) * (1 + 0.045 * kickPulse(t));
    const sh = cam.shake ? 14 * kickPulse(t) : 0;
    const ox = (K.hash(frame, 1) - 0.5) * sh, oy = (K.hash(frame, 2) - 0.5) * sh;
    ctx.save(); ctx.fillStyle = '#000'; ctx.fillRect(0, 0, W, H);
    ctx.translate(W / 2 + ox, H / 2 + oy); ctx.scale(z, z); ctx.translate(-W / 2, -H / 2);
    ctx.imageSmoothingEnabled = false; ctx.drawImage(pc, 0, 0, W, H);
    ctx.globalCompositeOperation = 'lighter'; ctx.globalAlpha = 0.3; ctx.imageSmoothingEnabled = true; ctx.drawImage(pc, -10, -10, W + 20, H + 20);
    ctx.restore();
    // cut: white flash frame + horizontal glitch slices on the strongest re-entries
    if (s && t - s.a < 0.05) { ctx.fillStyle = 'rgba(255,255,255,0.5)'; ctx.fillRect(0, 0, W, H); }
    if (s && t - s.a < 0.1 && s.cam.shake) {
      const r = K.rng(frame); for (let i = 0; i < 8; i++) { const y = r() * H, h = 20 + r() * 80; ctx.drawImage(ctx.canvas, 0, y, W, h, (r() - 0.5) * 120, y, W, h); }
    }
  };

  window.VERSION = {
    duration: 37.8,
    images: {},
    render(ctx, t, frame) {
      if (t < 17.17) chat(ctx, t); else pixelFrame(ctx, t, frame);
      K.grain(ctx, frame, 0.05);
      K.vignette(ctx, 0.5);
      if (t > 37.6) { ctx.fillStyle = `rgba(0,0,0,${K.prog(t, 37.6, 37.8)})`; ctx.fillRect(0, 0, W, H); }
    },
  };
})();
