import http from 'node:http';
import { readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const root = path.join(path.dirname(fileURLToPath(import.meta.url)), 'src');
const backend = new URL(process.env.API_URL || 'http://127.0.0.1:8080');
if (backend.protocol !== 'http:') throw new Error('Local proxy API_URL must use http; terminate TLS at the reverse proxy.');
const types = { '.html':'text/html; charset=utf-8', '.css':'text/css; charset=utf-8', '.js':'text/javascript; charset=utf-8', '.svg':'image/svg+xml' };
const server = http.createServer(async (req,res) => {
 if (req.url.startsWith('/api/') || req.url === '/actuator/health') {
  const upstream = http.request(new URL(req.url,backend), { method:req.method,headers:{...req.headers,host:backend.host} }, response => {
   res.writeHead(response.statusCode,response.headers);response.pipe(res);
  });
  upstream.on('error',()=>{if(!res.headersSent)res.writeHead(502,{'Content-Type':'application/json; charset=utf-8'});res.end(JSON.stringify({message:'Backend недоступен. Запустите Spring Boot и PostgreSQL.'}));});
  upstream.setTimeout(15000,()=>upstream.destroy());req.pipe(upstream);return;
 }
 try {
  let route=decodeURIComponent(new URL(req.url,'http://localhost').pathname);if(route==='/')route='/index.html';
  const file=path.resolve(root,'.'+route);
  if(!file.startsWith(root+path.sep)||!types[path.extname(file)]){res.writeHead(404);res.end('Not found');return;}
  const body=await readFile(file);res.writeHead(200,{'Content-Type':types[path.extname(file)],'Cache-Control':'no-cache','X-Content-Type-Options':'nosniff'});res.end(body);
 }catch{res.writeHead(404);res.end('Not found');}
});
server.listen(Number(process.env.PORT||3000),'127.0.0.1',()=>console.log(`FitLife: http://localhost:${server.address().port} (API: ${backend.origin})`));
