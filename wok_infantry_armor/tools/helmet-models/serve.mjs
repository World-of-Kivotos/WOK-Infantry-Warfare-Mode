import http from 'node:http';
import fs from 'node:fs';
const dir = new URL('./', import.meta.url);
const TYPES = { html: 'text/html; charset=utf-8', js: 'text/javascript; charset=utf-8', webp: 'image/webp', png: 'image/png', json: 'application/json' };
http.createServer((req, res) => {
  const p = req.url.split('?')[0];
  // POST /save/<name>.png with a data: URL body -> shots/<name>.png (review screenshots)
  if (req.method === 'POST' && p.startsWith('/save/')) {
    let body = '';
    req.on('data', d => { body += d; });
    req.on('end', () => {
      const name = p.slice(6).replace(/[^\w.-]/g, '_');
      fs.mkdirSync(new URL('./shots/', dir), { recursive: true });
      fs.writeFileSync(new URL('./shots/' + name, dir), Buffer.from(body.replace(/^data:[^,]+,/, ''), 'base64'));
      res.writeHead(200); res.end('ok');
    });
    return;
  }
  const f = new URL('.' + (p === '/' ? '/helmet-preview.html' : decodeURIComponent(p)), dir);
  fs.readFile(f, (e, d) => {
    if (e) { res.writeHead(404); return res.end(); }
    res.writeHead(200, { 'content-type': TYPES[f.pathname.split('.').pop()] || 'application/octet-stream', 'cache-control': 'no-store' });
    res.end(d);
  });
}).listen(+process.argv[2] || 5182);
