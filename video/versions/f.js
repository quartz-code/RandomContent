// Version F — «Кроме меня». Same music and editing grid as the two template videos the user
// sent (37.8 s, ~95.7 BPM): a late-night chat builds to "все стреляют", the punchline
// "(кроме меня)", then on the drop a run of pixel-game "proofs" cut on every stop of the track.
(function () {
  const { W, H } = K;
  const e = (t, a, b) => K.ease.inOutSine(K.prog(t, a, b));
  const BEAT = 0.627, B0 = 0.14;
  const RED = '#ff2d3c', WHITE = '#f2f2f2', GREY = '#7a7a7a', DARK = '#262626';

  // ================= pixel layer =================
  const PX = 4, LW = W / PX, LH = H / PX; // 270 x 480 logical pixels
  let pc, p, tc, tx;
  const initPx = () => {
    if (pc) return;
    pc = K.makeCanvas(LW, LH); p = pc.getContext('2d'); p.imageSmoothingEnabled = false;
    tc = K.makeCanvas(LW, LH); tx = tc.getContext('2d', { willReadFrequently: true });
  };
  // pixel text goes to a separate layer that is thresholded, so glyphs stay crisp
  const pt = (s, x, y, o = {}) => {
    tx.font = `${o.size || 8}px "Press Start 2P"`; tx.fillStyle = o.color || WHITE;
    tx.textAlign = o.align || 'center'; tx.textBaseline = 'top';
    tx.fillText(s, Math.round(x), Math.round(y));
  };
  const crispText = () => {
    const d = tx.getImageData(0, 0, LW, LH), a = d.data;
    for (let i = 3; i < a.length; i += 4) a[i] = a[i] > 100 ? 255 : 0;
    tx.putImageData(d, 0, 0);
    p.drawImage(tc, 0, 0);
  };
  const box = (x, y, w, h, col = WHITE) => { p.fillStyle = col; p.fillRect(x, y, w, 1); p.fillRect(x, y + h - 1, w, 1); p.fillRect(x, y, 1, h); p.fillRect(x + w - 1, y, 1, h); };
  const rect = (x, y, w, h, col) => { p.fillStyle = col; p.fillRect(Math.round(x), Math.round(y), w, h); };
  // sprites: strings, one char per pixel
  const PAL = { w: WHITE, g: GREY, d: DARK, r: RED, k: '#111', y: '#ffd34d', o: '#ff8a2b', b: '#7fb2ff', s: '#c9c9c9' };
  const spr = (rows, x, y, o = {}) => {
    const s = o.scale || 1;
    rows.forEach((row, j) => { for (let i = 0; i < row.length; i++) {
      const c = row[o.flip ? row.length - 1 - i : i]; if (c === '.') continue;
      p.fillStyle = (o.tint && c !== 'r' && c !== 'k') ? o.tint : PAL[c]; p.fillRect(Math.round(x + i * s), Math.round(y + j * s), s, s);
    } });
  };
  const KOSTYA = [
    '..www..', '.wwwww.', '.kwkwk.', '.wwwww.', '..www..', '.wwwww.', 'wwrwwww', 'w.wrw.w',
    'w.www.w', '..www..', '..w.w..', '..w.w..', '.ww.ww.',
  ];
  const SOLDIER = [
    '..ggg...', '.ggggg..', '..sss...', '..sss...', '..ggg..k', '.gggggk.', 'g.ggkk..', 'g.gkg...',
    '..ggg...', '..g.g...', '..g.g...', '.gg.gg..',
  ];
  const LYING = ['......ggg', '..gg.gsss', 'gggggggss', '.g..ggg..'];
  const CARRY = [ // Kostya with a wounded soldier over his shoulders
    'ggggggg..', 'gsgggggg.', '..wwwgg..', '.wwwww...', '.kwkwk...', '.wwwww...', '..www....',
    '.wwwww...', 'wwrwwww..', 'w.wrw.w..', '..www....', '..w.w....', '.ww.ww...',
  ];
  const HEART = ['.rr.rr.', 'rrrrrrr', 'rrrrrrr', '.rrrrr.', '..rrr..', '...r...'];
  const HEART_E = ['.gg.gg.', 'g..g..g', 'g.....g', '.g...g.', '..g.g..', '...g...'];
  const CROSS = ['.rr.', 'rrrr', 'rrrr', '.rr.'];
  const CROSS5 = ['.rrr.', 'rrrrr', 'rrrrr', 'rrrrr', '.rrr.'].map((r, i) => i === 0 || i === 4 ? '.rrr.' : 'rrrrr');
  const BULLET = ['.y.', 'yyy', 'yyy', 'ooo', 'ooo', 'ooo', 'ooo'];
  const LOCK = ['.ggg.', 'g...g', 'g...g', 'ggggg', 'gg.gg', 'gg.gg', 'ggggg'];
  const TROPHY = ['ggggggg', 'gwggggg', '.ggggg.', '..ggg..', '...g...', '..ggg..', '.ggggg.'];
  const RIFLE = ['............', 'kgggggggggg.', 'kkkkgg......', '...gg.......'];
  const PISTOL = ['ggggggg', 'gggggg.', '.gg....', '.gg....'];
  const GRENADE = ['..gg.', '.gggg', 'ggggg', 'ggggg', '.ggg.'];
  const KNIFE = ['........gs', 'ssssssssg.', 'ssssssss..'];
  const BANDAGE = ['.wwwww.', 'wwwwwww', 'wwrwwww', 'wrrrwww', 'wwrwwww', 'wwwwwww', '.wwwww.'];
  const SYRINGE = ['.s.......', 'sbbbbbsss', '.s.......'];
  const FLARE = ['..y..', '.yyy.', 'yyyyy', '.yyy.', '..y..'];

  // ================= chat (hi-res, 0..17.2) =================
  // camera glides from bubble to bubble; typing dots before each reply; punchline in a red pill
  const CHAT = [
    { t: 0.0, s: 'не спишь?', x: 520, y: 760, me: false },
    { t: 2.8, s: 'смена. а что', x: 600, y: 880, me: true, typing: 1.6 },
    { t: 5.2, s: 'странный вопрос', x: 470, y: 1030, me: false },
    { t: 7.6, s: 'давай', x: 640, y: 1150, me: true, typing: 6.8 },
    { t: 10.4, s: 'там же все стреляют, да?', x: 470, y: 1310, me: false },
    { t: 12.8, s: 'ага. все.', x: 610, y: 1450, me: true, typing: 11.6 },
    { t: 16.4, s: '(кроме меня)', x: 560, y: 1600, me: true, typing: 15.2, pill: true },
  ];
  const bubbleCache = new Map();
  const bubble = (s, pill) => {
    const key = s + pill; if (bubbleCache.has(key)) return bubbleCache.get(key);
    const m = K.makeCanvas(10, 10).getContext('2d'); m.font = `600 46px Manrope`;
    const w = Math.ceil(m.measureText(s).width + 76), h = 96, G = 50;
    const c = K.makeCanvas(w + G * 2, h + G * 2), x = c.getContext('2d');
    x.translate(G, G);
    if (pill) {
      x.shadowColor = 'rgba(255,45,60,0.9)'; x.shadowBlur = 30;
      x.strokeStyle = '#ff4a57'; x.lineWidth = 4; x.beginPath(); x.roundRect(2, 2, w - 4, h - 4, 34); x.stroke();
      x.shadowBlur = 0; x.fillStyle = 'rgba(40,6,10,0.85)'; x.beginPath(); x.roundRect(4, 4, w - 8, h - 8, 32); x.fill();
    } else {
      const g = x.createLinearGradient(0, 0, 0, h); g.addColorStop(0, '#3a3a3d'); g.addColorStop(1, '#2a2a2c');
      x.fillStyle = g; x.beginPath(); x.roundRect(0, 0, w, h, 30); x.fill();
      x.strokeStyle = 'rgba(255,255,255,0.08)'; x.lineWidth = 2; x.stroke();
    }
    x.font = `600 46px Manrope`; x.fillStyle = '#eaeaea'; x.textBaseline = 'middle'; x.textAlign = 'center';
    x.fillText(s, w / 2, h / 2 + 2);
    const b = { c, w, h, G }; bubbleCache.set(key, b); return b;
  };
  const chatCam = t => { // follow the newest bubble (or typing dots), springy
    let cx = CHAT[0].x, cy = CHAT[0].y;
    for (const m of CHAT) {
      const t0 = m.typing ?? m.t;
      if (t >= t0) { const pp = K.ease.inOutCubic(K.clamp((t - t0) / 0.55)); cx = K.lerp(cx, m.x, pp); cy = K.lerp(cy, m.y, pp); }
    }
    return { cx, cy, z: 1.5 - 0.15 * K.prog(t, 0, 17), rot: (-4 + 3 * Math.sin(t * 0.35)) * Math.PI / 180 };
  };
  const chat = (ctx, t, frame) => {
    ctx.fillStyle = '#000'; ctx.fillRect(0, 0, W, H);
    const g = ctx.createRadialGradient(W / 2, H / 2, 100, W / 2, H / 2, 1100);
    g.addColorStop(0, '#1c1c1c'); g.addColorStop(1, '#000'); ctx.fillStyle = g; ctx.fillRect(0, 0, W, H);
    const c = chatCam(t);
    ctx.save(); ctx.translate(W / 2, H / 2); ctx.rotate(c.rot); ctx.scale(c.z, c.z); ctx.translate(-c.cx, -c.cy);
    // header
    const ha = K.prog(t, 4.4, 5.0);
    if (ha > 0) { ctx.font = '500 26px Manrope'; ctx.textAlign = 'center'; ctx.fillStyle = `rgba(160,160,160,${ha * 0.8})`; ctx.fillText('Сегодня 3:12', 540, 640); }
    const last = CHAT.filter(m => t >= m.t).pop();
    for (const m of CHAT) {
      if (m.typing !== undefined && t >= m.typing && t < m.t) { // typing dots
        const a = K.prog(t, m.typing, m.typing + 0.2);
        ctx.save(); ctx.globalAlpha = a; ctx.fillStyle = '#2f2f31';
        ctx.beginPath(); ctx.roundRect(m.x - 60, m.y - 40, 120, 80, 40); ctx.fill();
        for (let k = 0; k < 3; k++) { const j = Math.max(0, Math.sin(t * 8 - k * 0.9)); ctx.fillStyle = `rgba(230,230,230,${0.4 + 0.6 * j})`; ctx.beginPath(); ctx.arc(m.x - 28 + k * 28, m.y - j * 7, 8, 0, 7); ctx.fill(); }
        ctx.restore();
      }
      if (t < m.t) continue;
      const lt = t - m.t, b = bubble(m.s, m.pill);
      const slide = m === CHAT[0] ? (1 - K.ease.outCubic(K.clamp(lt / 0.4))) * 500 : 0;
      const pop = m === CHAT[0] ? 1 : 0.8 + 0.2 * (1 - Math.exp(-9 * lt) * Math.cos(10 * lt));
      const focus = m === last ? 1 : 0.42;
      ctx.save(); ctx.globalAlpha = K.clamp(lt / 0.15) * focus;
      ctx.translate(m.x + slide, m.y); ctx.scale(pop, pop);
      // the punchline disintegrates into pixels at 16.8
      if (m.pill && t > 16.8) {
        const q = K.prog(t, 16.8, 17.2), r = K.rng(5);
        for (let k = 0; k < 140; k++) {
          const sx = (r() - 0.5) * b.w, sy = (r() - 0.5) * b.h, dx = (r() - 0.5) * 260 * q, dy = (r() - 0.6) * 200 * q;
          ctx.fillStyle = r() > 0.7 ? RED : '#ddd'; ctx.globalAlpha = (1 - q) * focus; ctx.fillRect(sx + dx, sy + dy, 14, 14);
        }
      } else ctx.drawImage(b.c, -b.w / 2 - b.G, -b.h / 2 - b.G);
      // shimmer as it lands
      const sp = K.prog(t, m.t + 0.05, m.t + 0.6);
      if (sp > 0 && sp < 1 && !m.pill) {
        ctx.beginPath(); ctx.roundRect(-b.w / 2, -b.h / 2, b.w, b.h, 30); ctx.clip();
        const sx = K.lerp(-b.w, b.w, sp), sg = ctx.createLinearGradient(sx - 60, 0, sx + 60, 0);
        sg.addColorStop(0, 'rgba(255,255,255,0)'); sg.addColorStop(0.5, 'rgba(255,255,255,0.45)'); sg.addColorStop(1, 'rgba(255,255,255,0)');
        ctx.fillStyle = sg; ctx.fillRect(-b.w / 2, -b.h / 2, b.w, b.h);
      }
      ctx.restore();
    }
    ctx.restore();
  };

  // ================= pixel scenes (17.2..37.8) =================
  const starsBG = (t, n = 40) => { const r = K.rng(3); for (let i = 0; i < n; i++) { const x = r() * LW, y = r() * LH, ph = r() * 6; if (Math.sin(t * 3 + ph) > -0.3) rect(x, y, 1, 1, r() > 0.8 ? GREY : '#444'); } };
  const floaters = (t, rows, n = 14) => { const r = K.rng(11); for (let i = 0; i < n; i++) { const x = r() * (LW - 10), y = (r() * LH + t * (8 + r() * 10)) % LH; spr(rows, x, LH - y); } };
  const dlg = (s, y = 330, t, t0) => { box(20, y, LW - 40, 34); const n = Math.floor(K.clamp((t - t0) * 30, 0, s.length)); pt('* ' + s.slice(0, n), 30, y + 12, { align: 'left' }); };
  const beatPulse = t => { const ph = ((t - B0) % BEAT) / BEAT; return Math.exp(-ph * 6); };

  const SCENES = [
    { a: 17.2, b: 17.75, draw(t) { // "patrons x 0"
      const g = p.createRadialGradient(LW / 2, LH / 2, 10, LW / 2, LH / 2, 260);
      g.addColorStop(0, '#555'); g.addColorStop(1, '#000'); p.fillStyle = g; p.fillRect(0, 0, LW, LH);
      pt('ПАТРОНЫ', LW / 2, 190, { color: '#bbb' });
      spr(BULLET, 84, 206, { scale: 5 }); pt('X 0', 168, 222, { size: 24 });
    } },
    { a: 17.75, b: 19.28, draw(t) { // title
      starsBG(t); floaters(t, CROSS, 16);
      const bob = Math.round(Math.sin(t * 6) * 1);
      pt('НА ВОЙНЕ', LW / 2, 120 + bob, { size: 16 });
      pt('ВСЕ', LW / 2, 146 + bob, { size: 16 });
      const s = 1 + 0.08 * beatPulse(t);
      tx.save(); tx.translate(LW / 2, 196); tx.scale(s, s); pt('СТРЕЛЯЮТ', 0, -12, { size: 24, color: RED }); tx.restore();
      if (t > 18.3) { pt('1 ИГРОК', LW / 2 + 6, 270); spr(CROSS, 78, 269); }
      if (t > 18.6) { pt('АТАКА', LW / 2 - 8, 290, { color: GREY }); spr(LOCK, 172, 286, { scale: 2 }); }
    } },
    { a: 19.28, b: 20.13, draw(t) { // everyone shoots
      starsBG(t, 20);
      rect(0, 300, LW, 1, '#333');
      for (let i = 0; i < 2; i++) {
        const x = 22 + i * 130;
        spr(SOLDIER, x, 300 - 36, { scale: 3 }); spr(SOLDIER, x + 66, 300 - 36, { scale: 3, flip: true });
        const fire = Math.floor((t - 19.28) / 0.21 + i) % 2 === 0;
        if (fire) { rect(x + 24, 278, 6, 2, PAL.y); rect(x + 60, 278, 6, 2, PAL.y); pt('БАХ', x + 46, 236, { color: PAL.y, size: 16 }); }
      }
    } },
    { a: 20.13, b: 21.0, draw(t) { // a wounded one is waiting
      starsBG(t, 20); rect(0, 250, LW, 1, '#333');
      spr(KOSTYA, 70, 250 - 39, { scale: 3 });
      const r = 32, cx = 192, cy = 222;
      for (let k = 0; k < 24; k++) { const a = k / 24 * 6.283 + t * 1.5; if (k % 2) rect(cx + Math.cos(a) * r, cy + Math.sin(a) * r, 2, 2, WHITE); }
      spr(LYING, cx - 18, cy + 2, { tint: '#555', scale: 2 }); pt('?', cx, cy - 18, { color: GREY, size: 16 });
      pt('РАНЕНЫЙ', cx, cy - 48, { color: GREY });
      dlg(t < 20.5 ? 'ЖДИ.' : 'ИДУ ЗА ТОБОЙ.', 290, t, t < 20.5 ? 20.2 : 20.5);
    } },
    { a: 21.0, b: 22.63, draw(t) { // high scores
      starsBG(t, 15);
      spr(TROPHY, LW / 2 - 7, 92, { scale: 2 });
      pt('ТАБЛИЦА РЕКОРДОВ', LW / 2, 116);
      pt('УБИТО', 220, 136, { color: GREY });
      const rows = [['1', 'ГАНС', '99'], ['2', 'ТОММИ', '87'], ['3', 'ФРИЦ', '76'], ['4', 'ДЖОН', '65'], ['5', 'КАРЛ', '54']];
      rows.forEach((r, i) => { if (t < 21.1 + i * 0.12) return; const y = 152 + i * 16; pt(r[0], 32, y, { color: GREY, align: 'left' }); pt(r[1], 52, y, { align: 'left' }); pt(r[2], 238, y, { align: 'right' }); });
      if (t > 21.9) {
        const bl = Math.floor(t * 8) % 2 ? RED : '#ff6b75';
        rect(24, 238, LW - 48, 1, '#444');
        pt('-', 32, 248, { color: RED, align: 'left' }); pt('КОСТЯ', 52, 248, { align: 'left', color: bl }); pt('0', 238, 248, { align: 'right', color: bl });
      }
      if (t > 22.15) pt('СПАСЕНО: 312', LW / 2, 280, { color: RED });
    } },
    { a: 22.63, b: 24.47, draw(t) { // RPG encounter: heal the enemy
      pt('ВРАГ', 100, 60, { color: GREY }); pt('LV 1', 170, 60, { color: GREY });
      const shake = t < 22.9 ? Math.round(Math.sin(t * 60) * 2) : 0;
      spr(LYING, 90 + shake, 100, { scale: 6 });
      // enemy HP bar fills when healed
      const hp = t < 23.87 ? 0.12 : 0.12 + 0.88 * K.ease.outCubic(K.prog(t, 23.87, 24.3));
      pt('HP', 64, 160, { color: GREY, align: 'left' }); box(84, 159, 120, 8, GREY); rect(86, 161, Math.round(116 * hp), 4, hp > 0.5 ? '#5dff8a' : RED);
      const msg = t < 23.2 ? 'РАНЕНЫЙ БРИТАНЕЦ!' : t < 23.87 ? 'ТЫ ЛЕЧИШЬ.' : 'ВРАГ ИСЦЕЛЁН!';
      dlg(msg, 196, t, t < 23.2 ? 22.7 : t < 23.87 ? 23.2 : 23.87);
      const opts = ['АТАКА', 'ЛЕЧИТЬ', 'МОРФИЙ', 'ВРАТЬ'];
      const sel = t < 22.95 ? 0 : 1;
      opts.forEach((o, i) => {
        const x = 20 + i * 58, on = i === sel && t > 22.95;
        if (on) rect(x, 244, 54, 16, RED); box(x, 244, 54, 16, on ? RED : GREY);
        pt(o, x + 27, 248, { color: i === 0 ? '#555' : WHITE });
      });
      if (sel === 0 && t > 22.75) { spr(LOCK, 44, 230); pt('ЗАПРЕТ', 46, 266, { color: RED }); }
      pt('ТЫ', 20, 278, { color: GREY, align: 'left' }); pt('СКОРОСТЬ 9', 60, 278, { color: GREY, align: 'left' });
    } },
    { a: 24.47, b: 26.9, draw(t) { // platformer: carry him up under fire
      pt('УРОВЕНЬ 1: ДОНЕСТИ', LW / 2, 50, { color: GREY });
      for (let i = 0; i < 9; i++) { const h = 40 + i * 22; rect(i * 30, LH - h, 30, h, '#1e1e1e'); box(i * 30, LH - h, 30, h, '#333'); }
      const step = Math.min(8, Math.floor((t - 24.47) / 0.27));
      const x = 8 + step * 30, y = LH - (40 + step * 22) - 13 * 2;
      const hit = t > 26.15 && t < 26.6;
      spr(CARRY, x, y + (hit ? 3 : 0), { scale: 2 });
      spr(CROSS, 262, LH - (40 + 8 * 22) - 14);
      // sniper shots from the right
      const shots = [24.9, 25.13, 25.6, 26.15];
      for (const s of shots) { const q = K.prog(t, s - 0.18, s); if (q > 0 && q < 1) rect(LW - q * (LW - x - 6), y + 10, 6, 1, PAL.y); }
      if (t > 25.13 && t < 25.6) pt('MISS', x + 30, y - 18, { color: RED, size: 16 });
      if (hit) { pt('-1', x + 26, y - 12, { color: RED }); }
      if (t > 26.3) pt('СКОРОСТЬ 9 > 7', LW / 2, 80, { color: RED });
    } },
    { a: 26.9, b: 29.4, draw(t) { // healing: the wounded man's hearts fill up
      pt('РАНЕНЫЙ', LW / 2, 140, { color: RED }); pt('HP', LW / 2, 152, { color: GREY });
      const n = Math.floor(K.clamp((t - 27.0) / 0.3, 0, 5.99));
      for (let i = 0; i < 5; i++) spr(i <= n ? HEART : HEART_E, 52 + i * 34, 180, { scale: 3 });
      if (t > 27.0) { const r = K.rng(Math.floor(t * 10)); for (let k = 0; k < 10; k++) rect(52 + n * 34 + r() * 24, 180 + r() * 20 - (t * 30 % 20), 1, 1, '#5dff8a'); pt('+1', 52 + n * 34 + 10, 166, { color: '#5dff8a' }); }
      spr(KOSTYA, 70, 290, { scale: 3 }); spr(LYING, 120, 314, { scale: 4 });
      if (Math.floor(t * 4) % 2) spr(SYRINGE, 96, 300, { scale: 2 });
    } },
    { a: 29.4, b: 30.13, draw(t) { // grenade → achievement
      starsBG(t, 20);
      const q = K.prog(t, 29.4, 29.75);
      if (q < 1) { spr(GRENADE, LW / 2 - 8, 100 + q * 150, { scale: 3 }); spr(KOSTYA, LW / 2 + 30, 240, { scale: 3 }); }
      else {
        const r = K.rng(7), ex = K.prog(t, 29.75, 30.0);
        for (let k = 0; k < 40; k++) { const a = r() * 6.28, d = r() * 70 * ex; rect(LW / 2 + Math.cos(a) * d, 260 + Math.sin(a) * d, 3, 3, r() > 0.5 ? PAL.o : PAL.y); }
        if (t > 29.85) { box(30, 150, LW - 60, 40, PAL.y); pt('ДОСТИЖЕНИЕ', LW / 2, 158, { color: PAL.y }); pt('ШРАМ НА ЛБУ', LW / 2, 174); }
      }
    } },
    { a: 30.13, b: 31.5, draw(t) { // choose a weapon: everything is crossed out but the bandage
      pt('ВЫБЕРИ', LW / 2, 70, { size: 16 }); pt('ОРУЖИЕ', LW / 2, 92, { size: 16 });
      const W_ = [[RIFLE, 'НЕТ'], [PISTOL, 'ПУСТОЙ'], [GRENADE, 'НЕТ'], [KNIFE, 'НЕТ'], [SYRINGE, 'МОРФИЙ'], [BANDAGE, 'ВЗЯТЬ']];
      W_.forEach(([s, lab], i) => {
        const x = 24 + (i % 3) * 78, y = 140 + Math.floor(i / 3) * 86, last = i === 5;
        if (t < 30.13 + i * 0.1) return;
        box(x, y, 66, 66, last ? RED : GREY); spr(s, x + 33 - s[0].length * 1.5, y + 18, { scale: 3 });
        if (!last && i !== 4) { p.strokeStyle = RED; p.lineWidth = 2; p.beginPath(); p.moveTo(x + 6, y + 6); p.lineTo(x + 60, y + 60); p.stroke(); rect(x + 4, y + 50, 58, 12, RED); pt(lab, x + 33, y + 52); }
        else pt(lab, x + 33, y + 52, { color: last ? RED : GREY });
      });
      if (t > 31.0 && Math.floor(t * 8) % 2) box(178, 224, 70, 70, RED);
    } },
    { a: 31.5, b: 32.63, draw(t) { // searching for the wounded
      pt('ПОИСК РАНЕНЫХ...', LW / 2, 80);
      const cx = LW / 2, cy = 210;
      for (const r of [30, 60, 90]) { p.strokeStyle = '#444'; p.lineWidth = 1; p.beginPath(); p.arc(cx, cy, r, 0, 7); p.stroke(); }
      const a = t * 4; p.strokeStyle = WHITE; p.beginPath(); p.moveTo(cx, cy); p.lineTo(cx + Math.cos(a) * 90, cy + Math.sin(a) * 90); p.stroke();
      [[40, -30], [-55, 20], [20, 60], [-20, -70], [70, 40]].forEach(([dx, dy], i) => { if (t > 31.6 + i * 0.15) spr(CROSS, cx + dx - 2, cy + dy - 2); });
      const pr = Math.min(99, Math.floor(K.prog(t, 31.5, 32.6) * 140));
      box(40, 330, LW - 80, 12); rect(42, 332, Math.round((LW - 84) * pr / 100), 8, WHITE); pt(pr + '%', LW - 40, 316, { align: 'right' });
    } },
    { a: 32.63, b: 35.0, draw(t) { // the battlefield under a hanging flare, he runs between them
      const fl = 0.7 + 0.3 * beatPulse(t);
      const g = p.createRadialGradient(LW / 2, 70, 2, LW / 2, 70, 200); g.addColorStop(0, `rgba(255,220,140,${0.35 * fl})`); g.addColorStop(1, 'rgba(0,0,0,0)');
      p.fillStyle = g; p.fillRect(0, 0, LW, LH);
      spr(FLARE, LW / 2 - 5, 62 + (t - 32.63) * 6, { scale: 2 });
      for (let i = 0; i < 6; i++) { const a = -Math.PI / 2 + (i - 2.5) * 0.4 + Math.sin(t * 2) * 0.05; p.strokeStyle = `rgba(255,220,140,${0.15 * fl})`; p.lineWidth = 2; p.beginPath(); p.moveTo(LW / 2, 70); p.lineTo(LW / 2 + Math.cos(a + Math.PI) * -300, 70 + 400); p.stroke(); }
      // checkerboard field
      for (let j = 0; j < 5; j++) for (let i = 0; i < 9; i++) {
        const lit = (i + j + Math.floor((t - B0) / BEAT)) % 3 === 0;
        rect(i * 30, 300 + j * 18, 30, 18, (i + j) % 2 ? (lit ? '#5a1218' : '#222') : (lit ? '#ddd' : '#555'));
      }
      for (let i = 0; i < 6; i++) { const x = 20 + i * 42, fire = Math.floor((t - B0) / (BEAT / 2) + i) % 3 === 0; spr(SOLDIER, x, 282 + (i % 2) * 22, { flip: i % 2 === 1 }); if (fire) rect(x + (i % 2 ? -2 : 9), 287 + (i % 2) * 22, 2, 1, PAL.y); }
      const kx = 20 + ((t - 32.63) * 90) % (LW - 40);
      spr(KOSTYA, kx, 310);
      const k = Math.floor((t - 32.63) / (BEAT / 2));
      for (let i = 0; i < Math.min(k, 6); i++) pt('+1', 30 + i * 42, 262 + (i % 2) * 22, { color: '#5dff8a' });
    } },
    { a: 35.0, b: 36.3, draw(t) { // "все"
      for (let i = 0; i < 9; i++) spr(SOLDIER, 4 + i * 30, 300 - (i % 3) * 5, { tint: '#3a3a3a', scale: 2 });
      spr(KOSTYA, LW / 2 - 7, 302, { scale: 2 });
      const n = Math.floor(K.clamp((t - 35.1) * 12, 0, 3));
      pt('все'.slice(0, n), 40, 240, { color: GREY, align: 'left', size: 16 });
    } },
    { a: 36.3, b: 37.85, draw(t) { // punchline + dissolve
      const q = K.prog(t, 37.0, 37.7);
      const gl = Math.floor(t * 20) % 5 === 0 ? 2 : 0;
      if (q === 0) {
        pt('(КРОМЕ', LW / 2 + gl, 190, { size: 24 });
        pt('НЕГО)', LW / 2 - gl, 222, { size: 24, color: RED });
        spr(CROSS5, LW / 2 + 66, 224, { scale: 4 });
      } else {
        const r = K.rng(21);
        for (let k = 0; k < 260; k++) { const x = 40 + r() * 190, y = 190 + r() * 60, dy = r() * 160 * q; rect(x, y + dy, 3, 3, r() > 0.6 ? RED : '#ccc'); }
      }
    } },
  ];

  const pixelFrame = (ctx, t, frame) => {
    initPx();
    p.fillStyle = '#000'; p.fillRect(0, 0, LW, LH);
    tx.clearRect(0, 0, LW, LH);
    const s = SCENES.find(x => t >= x.a && t < x.b);
    if (s) s.draw(t);
    crispText();
    ctx.save(); ctx.imageSmoothingEnabled = false;
    // beat bump on the drop
    const bump = 1 + 0.025 * beatPulse(t);
    ctx.translate(W / 2, H / 2); ctx.scale(bump, bump); ctx.translate(-W / 2, -H / 2);
    ctx.drawImage(pc, 0, 0, W, H);
    // soft glow: smooth upscale of the pixel layer
    ctx.globalCompositeOperation = 'lighter'; ctx.globalAlpha = 0.28; ctx.imageSmoothingEnabled = true;
    ctx.drawImage(pc, -8, -8, W + 16, H + 16);
    ctx.restore();
    // flash on each cut
    if (s && t - s.a < 0.06) { ctx.fillStyle = 'rgba(255,255,255,0.35)'; ctx.fillRect(0, 0, W, H); }
  };

  window.VERSION = {
    duration: 37.8,
    images: {},
    render(ctx, t, frame) {
      if (t < 17.2) chat(ctx, t, frame); else pixelFrame(ctx, t, frame);
      K.grain(ctx, frame, 0.05);
      K.vignette(ctx, 0.55);
      if (t > 37.6) { ctx.fillStyle = `rgba(0,0,0,${K.prog(t, 37.6, 37.8)})`; ctx.fillRect(0, 0, W, H); }
    },
  };
})();
