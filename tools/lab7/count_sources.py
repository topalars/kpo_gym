from pathlib import Path
import json,hashlib
r=Path('D:/01 Work and study/01 Study/3к-1с/КПО/Project/kpo_gym')
paths=list((r/'backend/src/main/java').rglob('*.java'))+list((r/'backend/src/main/resources/db/migration').glob('*.sql'))+[p for p in (r/'frontend/src').rglob('*') if p.suffix in ['.js','.html','.css']]+[r/'frontend/server.mjs']
def counts(p):
 lines=p.read_text(encoding='utf-8-sig').splitlines();block=False;code=comment=blank=0
 for line in lines:
  if not line.strip():blank+=1;continue
  i=0;out='';quote=None
  while i<len(line):
   if block:
    j=line.find('*/',i)
    if j<0:break
    block=False;i=j+2;continue
   c=line[i]
   if quote:
    out+=c
    if c=='\\' and i+1<len(line):out+=line[i+1];i+=2;continue
    if c==quote:quote=None
    i+=1;continue
   if line[i:i+2]=='/*':block=True;i+=2;continue
   if line[i:i+2]=='//':break
   if c in ['\"',"'",'`']:quote=c
   out+=c;i+=1
  # HTML shells contain no standalone comments in the selected source.
  if out.strip():code+=1
  else:comment+=1
 return dict(total=len(lines),blank=blank,comment=comment,sloc=code)
result=[]
for p in sorted(paths):result.append({'path':str(p.relative_to(r)).replace('\\','/'),'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),**counts(p)})
Path('outputs/ЛР7_размер_исходников.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf8')
for ext in ['.java','.sql','.js','.html','.css','.mjs']:
 rows=[x for x in result if Path(x['path']).suffix==ext];print(ext,len(rows),sum(x['total'] for x in rows),sum(x['sloc'] for x in rows),sum(x['bytes'] for x in rows))
print('TOTAL',len(result),sum(x['total'] for x in result),sum(x['sloc'] for x in result))
