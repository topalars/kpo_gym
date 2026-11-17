// The only client-side data cache is transient; PostgreSQL and the server session are authoritative.
let user = null;
let cache = null;
let csrf = null;
const mutating = method => !['GET', 'HEAD'].includes(method);
async function token() {
 const response = await fetch('/api/auth/csrf', { credentials: 'same-origin', cache: 'no-store' });
 if (!response.ok) throw new Error('Backend недоступен. Проверьте запуск сервера и базы данных.');
 csrf = await response.json();
}
export async function request(path, method = 'GET', data) {
 if (mutating(method) && !csrf) await token();
 let response;
 try {
  response = await fetch('/api' + path, { method, credentials: 'same-origin', cache: 'no-store', headers: {
   ...(data === undefined ? {} : { 'Content-Type': 'application/json' }),
   ...(mutating(method) ? { [csrf.headerName]: csrf.token } : {})
  }, ...(data === undefined ? {} : { body: JSON.stringify(data) }) });
 } catch { throw new Error('Нет соединения с сервером. Проверьте подключение и повторите действие.'); }
 if (!response.ok) {
  const error = await response.json().catch(() => ({}));
  if (response.status === 401) {
   user = null; cache = null; csrf = null;
   if (!path.startsWith('/auth/') && location.pathname.includes('/panel')) location.replace('/auth/login.html');
  }
  if (response.status === 403) csrf = null;
  const exception = new Error(error.message || `Ошибка сервера (${response.status}).`); exception.status = response.status; throw exception;
 }
 if (response.status === 204 || response.headers.get('content-length') === '0') return;
 const body = await response.text(); return body ? JSON.parse(body) : undefined;
}
export async function init() {
 await token();
 try { user = await request('/auth/session'); }
 catch (error) { if (error.status !== 401) throw error; }
 if (user) await refresh();
}
export async function refresh() { cache = await request('/snapshot'); user = cache.user; return cache; }
export function currentUser() { return user; }
export function snapshot() { if (!cache) throw new Error('Данные ещё не загружены. Обновите страницу.'); return cache; }
export const today = () => { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`; };
export async function login(email, password) { user = await request('/auth/login', 'POST', { email, password }); csrf = null; return user; }
export async function register({name,email,password}) { user = await request('/auth/register', 'POST', {name,email,password}); csrf = null; return user; }
export async function logout() { await request('/auth/logout','POST'); user = null; cache = null; csrf = null; }
async function change(path, method, data) { await request(path, method, data); await refresh(); }
export const updateProfile = v => change('/me','PATCH',{name:v.name,phone:v.phone,height:v.height === '' ? null : Number(v.height)});
export const addProgress = v => change('/progress','PUT',{date:v.date,weight:Number(v.weight),strength:Number(v.strength)});
export const saveSlot = (v,id) => change('/trainings'+(id?'/'+id:''),id?'PATCH':'POST',{title:v.title,coachId:v.coachId || null,date:v.date,time:v.time,duration:Number(v.duration),capacity:Number(v.capacity),room:v.room});
export const deleteSlot = id => change('/trainings/'+id,'DELETE');
export const saveMember = (v,id) => change('/clients/'+id,'PATCH',{name:v.name,phone:v.phone,active:v.active==='on',plan:v.plan,planUntil:v.planUntil || null});
export const saveTrainer = (v,id) => change('/trainers'+(id?'/'+id:''),id?'PATCH':'POST',{name:v.name,email:v.email,password:v.password || null,phone:v.phone,specialty:v.specialty,active:v.active==='on'});
export const saveEvent = (v,id) => change('/events'+(id?'/'+id:''),id?'PATCH':'POST',{title:v.title,description:v.description,date:v.date,time:v.time,type:v.type,capacity:Number(v.capacity),published:v.published==='on'});
export const deleteEvent = id => change('/events/'+id,'DELETE');
export function toggleBooking(id) {
 const booked = snapshot().slots.find(s=>s.id===id)?.members.includes(user.id);
 return booked ? change('/bookings/'+id,'DELETE') : change('/bookings','POST',{trainingId:id});
}
export function toggleEvent(id) {
 const joined = snapshot().events.find(e=>e.id===id)?.members.includes(user.id);
 return change('/events/'+id+'/registration',joined?'DELETE':'POST');
}
export const readNotification = id => change('/notifications/'+id+'/read','PATCH');
