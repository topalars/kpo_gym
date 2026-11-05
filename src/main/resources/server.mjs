import http from 'node:http';
import { readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const root = path.dirname(fileURLToPath(import.meta.url));
const types = { '.html': 'text/html; charset=utf-8', '.css': 'text/css; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.svg': 'image/svg+xml' };
const server = http.createServer(async (req, res) => {
  try {
    let route = decodeURIComponent(new URL(req.url, 'http://localhost').pathname);
    if (route === '/') route = '/index.html';
    if (route.endsWith('/')) route += 'index.html';
    const file = path.resolve(root, '.' + route);
    if (!file.startsWith(root + path.sep) || !['.html', '.css', '.js', '.svg'].includes(path.extname(file))) {
      res.writeHead(404); res.end('Not found'); return;
    }
    const body = await readFile(file);
    res.writeHead(200, { 'Content-Type': types[path.extname(file)], 'Cache-Control': 'no-cache', 'X-Content-Type-Options': 'nosniff' });
    res.end(body);
  } catch { res.writeHead(404); res.end('Not found'); }
});
server.listen(Number(process.env.PORT || 3000), '127.0.0.1', () => console.log(`FitLife: http://localhost:${server.address().port}`));
