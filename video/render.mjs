// Renders a version frame by frame in headless Chromium and pipes JPEG frames into ffmpeg.
//   node video/render.mjs --v a --out out/a-video.mp4            full video (no audio)
//   node video/render.mjs --v a --stills 5,20.5,88 --outdir out/  single frames as PNG
//   options: --from S --to S --fps 30 --scale 0.5 (preview resolution)
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { spawn } from 'node:child_process';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

const require = createRequire(import.meta.url);
const { chromium } = require('/opt/node22/lib/node_modules/playwright');

const args = Object.fromEntries(process.argv.slice(2).reduce((acc, a, i, all) => {
  if (a.startsWith('--')) acc.push([a.slice(2), all[i + 1] && !all[i + 1].startsWith('--') ? all[i + 1] : true]);
  return acc;
}, []));
const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const fps = Number(args.fps || 30), scale = Number(args.scale || 1);

const TYPES = { '.html': 'text/html', '.js': 'text/javascript', '.png': 'image/png', '.jpg': 'image/jpeg', '.ttf': 'font/ttf' };
const server = http.createServer((req, res) => {
  const p = path.join(ROOT, decodeURIComponent(new URL(req.url, 'http://x').pathname));
  if (!p.startsWith(ROOT) || !fs.existsSync(p) || fs.statSync(p).isDirectory()) { res.writeHead(404); return res.end(); }
  res.writeHead(200, { 'Content-Type': TYPES[path.extname(p)] || 'application/octet-stream' });
  fs.createReadStream(p).pipe(res);
});
await new Promise(r => server.listen(0, '127.0.0.1', r));
const port = server.address().port;

const browser = await chromium.launch({ args: ['--disable-web-security'] });
const page = await browser.newPage({ viewport: { width: 1080, height: 1920 } });
page.on('pageerror', e => console.error('page error:', e.message));
page.on('console', m => { if (m.type() === 'error') console.error('console:', m.text()); });
await page.goto(`http://127.0.0.1:${port}/video/index.html?v=${args.v || 'a'}`);
const { duration } = await page.evaluate(() => window.ready);

const grab = (t, frame, type) => page.evaluate(([t, frame, type, scale]) => {
  renderAt(t, frame);
  let c = K.canvas;
  if (scale !== 1) {
    const s = document.createElement('canvas'); s.width = Math.round(1080 * scale); s.height = Math.round(1920 * scale);
    s.getContext('2d').drawImage(c, 0, 0, s.width, s.height); c = s;
  }
  return c.toDataURL(type, 0.92).split(',')[1];
}, [t, frame, type, scale]);

if (args.stills) {
  const outdir = path.resolve(args.outdir || 'out'); fs.mkdirSync(outdir, { recursive: true });
  for (const s of String(args.stills).split(',')) {
    const t = Number(s), b64 = await grab(t, Math.round(t * fps), 'image/jpeg');
    const f = path.join(outdir, `${args.v || 'a'}-${t.toFixed(1)}.jpg`);
    fs.writeFileSync(f, Buffer.from(b64, 'base64')); console.log(f);
  }
} else {
  const from = Number(args.from || 0), to = Number(args.to || duration);
  const out = path.resolve(args.out || `out/${args.v || 'a'}-video.mp4`); fs.mkdirSync(path.dirname(out), { recursive: true });
  const ff = spawn('ffmpeg', ['-v', 'error', '-y', '-f', 'image2pipe', '-c:v', 'mjpeg', '-framerate', String(fps), '-i', '-',
    '-c:v', 'libx264', '-preset', 'veryfast', '-crf', '17', '-pix_fmt', 'yuv420p', out], { stdio: ['pipe', 'inherit', 'inherit'] });
  const n = Math.round((to - from) * fps), t0 = Date.now();
  for (let i = 0; i < n; i++) {
    const t = from + i / fps, b64 = await grab(t, Math.round(t * fps), 'image/jpeg');
    if (!ff.stdin.write(Buffer.from(b64, 'base64'))) await new Promise(r => ff.stdin.once('drain', r));
    if (i % (fps * 5) === 0) process.stdout.write(`\r${(t).toFixed(1)}s / ${to}s  (${((Date.now() - t0) / 1000).toFixed(0)}s elapsed)`);
  }
  ff.stdin.end(); await new Promise(r => ff.on('close', r));
  console.log('\n' + out);
}
await browser.close(); server.close();
