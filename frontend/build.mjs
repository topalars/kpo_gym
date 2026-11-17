import { cp,mkdir,rm } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const root=path.dirname(fileURLToPath(import.meta.url));
await rm(path.join(root,'build'),{recursive:true,force:true});
await mkdir(path.join(root,'build'),{recursive:true});
await cp(path.join(root,'src'),path.join(root,'build'),{recursive:true});
console.log('FitLife frontend built.');
