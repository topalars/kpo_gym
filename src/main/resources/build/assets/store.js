// Local demonstration repository. Replace this module with server requests when the API exists.
// Role checks here support UI demonstrations; production authorization belongs on the server.
const KEY = 'fitlife.demo.v1';
const SESSION = 'fitlife.session.v1';
const day = offset => {
  const date = new Date(); date.setDate(date.getDate() + offset);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
};
export const today = () => day(0);
const id = () => crypto.randomUUID();
async function hash(password, salt) {
  const key = await crypto.subtle.importKey('raw', new TextEncoder().encode(password), 'PBKDF2', false, ['deriveBits']);
  const bits = await crypto.subtle.deriveBits({ name: 'PBKDF2', salt: new TextEncoder().encode(salt), iterations: 100000, hash: 'SHA-256' }, key, 256);
  return Array.from(new Uint8Array(bits), b => b.toString(16).padStart(2, '0')).join('');
}
async function account(data, password = 'Fitlife123!') {
  const salt = id(); return { ...data, salt, passwordHash: await hash(password, salt) };
}
function save(data) { localStorage.setItem(KEY, JSON.stringify(data)); }
function read() {
  const raw = localStorage.getItem(KEY);
  if (!raw) throw new Error('Демонстрационные данные не загружены. Обновите страницу.');
  return JSON.parse(raw);
}
const publicUser = ({ passwordHash, salt, ...user }) => user;
export async function init() {
  if (localStorage.getItem(KEY)) return;
  const users = await Promise.all([
    account({ id: 'client', name: 'Александр Морозов', email: 'client@fitlife.ru', role: 'ROLE_USER', phone: '+7 999 123-45-67', height: 180, plan: 'Безлимит', planUntil: day(60), active: true }),
    account({ id: 'coach', name: 'Михаил Волков', email: 'coach@fitlife.ru', role: 'ROLE_COACH', phone: '', specialty: 'Силовые тренировки', active: true }),
    account({ id: 'coach2', name: 'Анна Соколова', email: 'anna@fitlife.ru', role: 'ROLE_COACH', phone: '', specialty: 'Йога и мобильность', active: true }),
    account({ id: 'admin', name: 'Дарья Смирнова', email: 'admin@fitlife.ru', role: 'ROLE_ADMIN', phone: '', active: true }),
    account({ id: 'client2', name: 'Мария Кузнецова', email: 'maria@fitlife.ru', role: 'ROLE_USER', phone: '+7 999 456-78-90', plan: 'Дневной', planUntil: day(24), active: true })
  ]);
  save({ users, slots: [
    { id: 's1', title: 'Силовая тренировка', coachId: 'coach', date: day(1), time: '10:00', duration: 60, capacity: 6, room: 'Силовой зал', members: ['client'] },
    { id: 's2', title: 'Функциональный тренинг', coachId: 'coach', date: day(1), time: '18:00', duration: 60, capacity: 8, room: 'Зал № 1', members: ['client2'] },
    { id: 's3', title: 'Йога и растяжка', coachId: 'coach2', date: day(2), time: '09:00', duration: 50, capacity: 10, room: 'Студия йоги', members: [] },
    { id: 's4', title: 'Персональная тренировка', coachId: 'coach', date: day(3), time: '17:00', duration: 60, capacity: 1, room: 'Силовой зал', members: [] },
    { id: 's5', title: 'Мобильность тела', coachId: 'coach2', date: day(4), time: '19:00', duration: 45, capacity: 8, room: 'Студия йоги', members: [] }
  ], progress: [
    { id: 'p1', userId: 'client', date: day(-28), weight: 84, strength: 60 },
    { id: 'p2', userId: 'client', date: day(-21), weight: 83.2, strength: 62.5 },
    { id: 'p3', userId: 'client', date: day(-14), weight: 82.5, strength: 65 },
    { id: 'p4', userId: 'client', date: day(-7), weight: 82, strength: 67.5 }
  ], events: [
    { id: 'e1', title: 'Стань сильнее: открытая тренировка', description: 'Разберём технику базовых упражнений вместе с тренером. Подходит для любого уровня подготовки.', date: day(7), time: '12:00', type: 'Мастер-класс', capacity: 20, members: [], published: true },
    { id: 'e2', title: 'Суббота в движении', description: 'Функциональная тренировка и растяжка. Приходите познакомиться с командой клуба.', date: day(9), time: '11:00', type: 'Событие клуба', capacity: 15, members: ['client2'], published: true }
  ] });
}
export function currentUser() {
  const userId = sessionStorage.getItem(SESSION);
  const user = read().users.find(u => u.id === userId && u.active);
  return user ? publicUser(user) : null;
}
function requireUser(roles) {
  const user = currentUser();
  if (!user || (roles && !roles.includes(user.role))) throw new Error('Недостаточно прав для этого действия.');
  return user;
}
export function logout() { sessionStorage.removeItem(SESSION); }
export async function login(email, password) {
  const user = read().users.find(u => u.email.toLowerCase() === email.trim().toLowerCase());
  if (!user || !user.active || await hash(password, user.salt) !== user.passwordHash) throw new Error('Неверная почта или пароль.');
  sessionStorage.setItem(SESSION, user.id); return publicUser(user);
}
function validateIdentity(name, email) {
  if (name.trim().length < 2 || name.length > 100) throw new Error('Введите имя длиной от 2 до 100 символов.');
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) || email.length > 150) throw new Error('Введите корректную электронную почту.');
}
export async function register({ name, email, password }) {
  validateIdentity(name, email);
  if (password.length < 8) throw new Error('Пароль должен содержать не менее 8 символов.');
  const data = read();
  if (data.users.some(u => u.email.toLowerCase() === email.trim().toLowerCase())) throw new Error('Эта почта уже зарегистрирована.');
  const user = await account({ id: id(), name: name.trim(), email: email.trim().toLowerCase(), role: 'ROLE_USER', phone: '', height: '', plan: 'Без абонемента', planUntil: '', active: true }, password);
  data.users.push(user); save(data); sessionStorage.setItem(SESSION, user.id); return publicUser(user);
}
export function snapshot() {
  const user = requireUser(); const data = read();
  const admin = ['ROLE_ADMIN', 'ROLE_SUPERADMIN'].includes(user.role);
  const slots = user.role === 'ROLE_COACH' ? data.slots.filter(s => s.coachId === user.id) : data.slots;
  const clientIds = new Set(slots.flatMap(s => s.members));
  return { user, users: data.users.filter(u => admin || u.id === user.id || u.role === 'ROLE_COACH' || (user.role === 'ROLE_COACH' && clientIds.has(u.id))).map(publicUser), slots,
    progress: data.progress.filter(p => p.userId === user.id), events: data.events.filter(e => admin || e.published) };
}
export function updateProfile(values) {
  const user = requireUser(); validateIdentity(values.name, user.email);
  const height = values.height === '' ? '' : Number(values.height);
  if (height !== '' && (!Number.isFinite(height) || height < 80 || height > 250)) throw new Error('Рост должен быть от 80 до 250 см.');
  const data = read(); Object.assign(data.users.find(u => u.id === user.id), { name: values.name.trim(), phone: values.phone.trim(), height }); save(data);
}
function future(date, time) { return new Date(`${date}T${time}`).getTime() > Date.now(); }
function overlap(a, b) {
  const minutes = t => Number(t.slice(0, 2)) * 60 + Number(t.slice(3));
  return a.date === b.date && minutes(a.time) < minutes(b.time) + b.duration && minutes(b.time) < minutes(a.time) + a.duration;
}
export function toggleBooking(slotId) {
  const user = requireUser(['ROLE_USER']); const data = read(); const slot = data.slots.find(s => s.id === slotId);
  if (!slot || !future(slot.date, slot.time)) throw new Error('Запись на эту тренировку закрыта.');
  if (slot.members.includes(user.id)) slot.members = slot.members.filter(x => x !== user.id);
  else {
    if (slot.members.length >= slot.capacity) throw new Error('Свободных мест больше нет.');
    if (data.slots.some(s => s.members.includes(user.id) && overlap(s, slot))) throw new Error('У вас уже есть тренировка в это время.');
    slot.members.push(user.id);
  }
  save(data);
}
export function addProgress(values) {
  const user = requireUser(['ROLE_USER']); const weight = Number(values.weight), strength = Number(values.strength);
  if (!values.date || values.date > today() || !Number.isFinite(weight) || weight < 25 || weight > 350 || !Number.isFinite(strength) || strength < 0 || strength > 500) throw new Error('Проверьте дату, вес (25–350 кг) и жим лёжа (0–500 кг).');
  const data = read(); const existing = data.progress.find(p => p.userId === user.id && p.date === values.date);
  if (existing) Object.assign(existing, { weight, strength });
  else data.progress.push({ id: id(), userId: user.id, date: values.date, weight, strength });
  save(data);
}
export function saveSlot(values, slotId) {
  const user = requireUser(['ROLE_COACH', 'ROLE_ADMIN', 'ROLE_SUPERADMIN']); const data = read();
  const old = slotId ? data.slots.find(s => s.id === slotId) : null;
  if (slotId && !old) throw new Error('Тренировка не найдена.');
  if (old && user.role === 'ROLE_COACH' && old.coachId !== user.id) throw new Error('Можно менять только свои тренировки.');
  const coachId = user.role === 'ROLE_COACH' ? user.id : values.coachId;
  const slot = { id: old?.id || id(), title: values.title.trim(), coachId, date: values.date, time: values.time, duration: Number(values.duration), capacity: Number(values.capacity), room: values.room.trim(), members: old?.members || [] };
  if (!slot.title || !slot.room || !future(slot.date, slot.time) || !Number.isInteger(slot.duration) || slot.duration < 15 || slot.duration > 180 || !Number.isInteger(slot.capacity) || slot.capacity < 1 || slot.capacity > 50 || slot.capacity < slot.members.length || !data.users.some(u => u.id === coachId && u.role === 'ROLE_COACH' && u.active)) throw new Error('Проверьте поля: будущая дата, длительность 15–180 мин, мест 1–50 и не меньше числа записанных.');
  if (data.slots.some(s => s.id !== slotId && s.coachId === coachId && overlap(s, slot))) throw new Error('У тренера уже есть занятие в это время.');
  if (old) Object.assign(old, slot); else data.slots.push(slot); save(data);
}
export function deleteSlot(slotId) {
  const user = requireUser(['ROLE_COACH', 'ROLE_ADMIN', 'ROLE_SUPERADMIN']); const data = read();
  const slot = data.slots.find(s => s.id === slotId);
  if (!slot || (user.role === 'ROLE_COACH' && slot.coachId !== user.id)) throw new Error('Тренировка недоступна.');
  data.slots = data.slots.filter(s => s.id !== slotId); save(data);
}
export function saveMember(values, userId) {
  requireUser(['ROLE_ADMIN', 'ROLE_SUPERADMIN']); const data = read();
  const member = data.users.find(u => u.id === userId && u.role === 'ROLE_USER');
  if (!member) throw new Error('Клиент не найден.');
  validateIdentity(values.name, member.email);
  if (!['Без абонемента', 'Дневной', 'Безлимит', 'Персональный'].includes(values.plan) || (values.plan !== 'Без абонемента' && !values.planUntil)) throw new Error('Укажите абонемент и срок его действия.');
  Object.assign(member, { name: values.name.trim(), phone: values.phone.trim(), plan: values.plan, planUntil: values.planUntil, active: values.active === 'on' }); save(data);
}
export function saveEvent(values, eventId) {
  requireUser(['ROLE_ADMIN', 'ROLE_SUPERADMIN']); const data = read(); const old = data.events.find(e => e.id === eventId);
  const event = { id: old?.id || id(), title: values.title.trim(), description: values.description.trim(), date: values.date, time: values.time, type: values.type, capacity: Number(values.capacity), published: values.published === 'on', members: old?.members || [] };
  if (!event.title || !event.description || !future(event.date, event.time) || !Number.isInteger(event.capacity) || event.capacity < 1 || event.capacity > 200 || event.capacity < event.members.length) throw new Error('Проверьте название, описание, будущую дату и число мест (1–200).');
  if (old) Object.assign(old, event); else data.events.push(event); save(data);
}
export function deleteEvent(eventId) {
  requireUser(['ROLE_ADMIN', 'ROLE_SUPERADMIN']); const data = read(); data.events = data.events.filter(e => e.id !== eventId); save(data);
}
export function toggleEvent(eventId) {
  const user = requireUser(['ROLE_USER']); const data = read(); const event = data.events.find(e => e.id === eventId && e.published);
  if (!event || !future(event.date, event.time)) throw new Error('Регистрация закрыта.');
  if (event.members.includes(user.id)) event.members = event.members.filter(x => x !== user.id);
  else { if (event.members.length >= event.capacity) throw new Error('Свободных мест больше нет.'); event.members.push(user.id); }
  save(data);
}
