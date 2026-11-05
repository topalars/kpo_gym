import { cp, mkdir, rm } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const root = path.dirname(fileURLToPath(import.meta.url));
const out = path.join(root, 'build');
await rm(out, { recursive: true, force: true });
await mkdir(out, { recursive: true });
for (const item of ['index.html', 'landing.html', 'error.html', 'assets', 'auth', 'panel', 'admin']) {
  await cp(path.join(root, item), path.join(out, item), { recursive: true });
}
console.log('Static frontend built: web/build');
