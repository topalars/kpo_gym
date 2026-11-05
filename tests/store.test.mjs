import { test } from 'node:test';
import assert from 'node:assert/strict';
import { webcrypto } from 'node:crypto';
globalThis.crypto ??= webcrypto;
class MemoryStorage { data = new Map(); getItem(k) { return this.data.get(k) ?? null; } setItem(k, v) { this.data.set(k, String(v)); } removeItem(k) { this.data.delete(k); } }
globalThis.localStorage = new MemoryStorage();
globalThis.sessionStorage = new MemoryStorage();
const store = await import('../web/assets/store.js');
await store.init();
const login = role => store.login(`${role}@fitlife.ru`, 'Fitlife123!');
test('authentication rejects invalid credentials and exposes no password hashes', async () => {
  await assert.rejects(store.login('client@fitlife.ru', 'wrong'), /Неверная/);
  const user = await login('client');
  assert.equal(user.role, 'ROLE_USER'); assert.equal(user.passwordHash, undefined);
  assert.ok(store.snapshot().users.every(u => !('salt' in u) && !('passwordHash' in u)));
});
test('public registration always creates client and rejects duplicate email', async () => {
  const values = { name: 'Тестовый клиент', email: 'test@example.ru', password: 'Test12345!', role: 'ROLE_ADMIN' };
  const user = await store.register(values); assert.equal(user.role, 'ROLE_USER');
  await assert.rejects(store.register(values), /уже зарегистрирована/);
});
test('booking, cancellation and capacity remain consistent', async () => {
  await login('client'); store.toggleBooking('s4');
  assert.ok(store.snapshot().slots.find(s => s.id === 's4').members.includes('client'));
  await store.login('test@example.ru', 'Test12345!');
  assert.throws(() => store.toggleBooking('s4'), /Свободных мест/);
  await login('client'); store.toggleBooking('s4');
  assert.ok(!store.snapshot().slots.find(s => s.id === 's4').members.includes('client'));
});
test('progress validates values, updates same date, and stays private', async () => {
  await login('client'); const date = store.today();
  assert.throws(() => store.addProgress({ date, weight: -10, strength: 60 }), /Проверьте/);
  store.addProgress({ date, weight: 81, strength: 70 });
  store.addProgress({ date, weight: 80.5, strength: 72.5 });
  const records = store.snapshot().progress.filter(p => p.date === date);
  assert.equal(records.length, 1); assert.equal(records[0].weight, 80.5);
  await store.login('test@example.ru', 'Test12345!'); assert.equal(store.snapshot().progress.length, 0);
});
test('coach cannot edit other coaches and conflicting time slots are rejected', async () => {
  await login('coach'); const s = store.snapshot().slots[0];
  assert.ok(store.snapshot().slots.every(s => s.coachId === 'coach'));
  assert.throws(() => store.saveSlot(s, 's3'), /только свои/);
  assert.throws(() => store.saveSlot({ ...s, title: 'Другое занятие' }), /уже есть занятие/);
  assert.throws(() => store.saveMember({ name: 'Клиент' }, 'client'), /Недостаточно прав/);
});
test('client schedule overlaps are rejected, including partial overlaps', async () => {
  await login('admin'); const s = store.snapshot().slots.find(s => s.id === 's1');
  store.saveSlot({ ...s, title: 'Пересечение', coachId: 'coach2', time: '10:30', capacity: 5 });
  const extra = store.snapshot().slots.find(s => s.title === 'Пересечение');
  await login('client'); assert.throws(() => store.toggleBooking(extra.id), /уже есть тренировка/);
});
test('admin edits plans and drafts are hidden until publication', async () => {
  await login('admin'); const date = store.snapshot().slots.find(s => s.id === 's5').date;
  store.saveMember({ name: 'Александр Морозов', phone: '', plan: 'Персональный', planUntil: date, active: 'on' }, 'client');
  store.saveEvent({ title: 'Тестовое событие', description: 'Описание', date, time: '13:00', capacity: 3, type: 'Мастер-класс' });
  const event = store.snapshot().events.find(e => e.title === 'Тестовое событие');
  await login('client'); assert.equal(store.currentUser().plan, 'Персональный');
  assert.ok(!store.snapshot().events.some(e => e.id === event.id));
  assert.throws(() => store.saveEvent({}), /Недостаточно прав/);
  await login('admin'); store.saveEvent({ ...event, published: 'on' }, event.id);
  await login('client'); store.toggleEvent(event.id); assert.ok(store.snapshot().events.find(e => e.id === event.id).members.includes('client'));
  store.toggleEvent(event.id); assert.ok(!store.snapshot().events.find(e => e.id === event.id).members.includes('client'));
});
test('deactivated accounts cannot sign in and lose current demo session access', async () => {
  const user = await store.register({ name: 'Ещё клиент', email: 'blocked@example.ru', password: 'Test12345!' });
  await login('admin'); store.saveMember({ name: 'Тестовый клиент', phone: '', plan: 'Без абонемента', planUntil: '' }, user.id);
  await assert.rejects(store.login('blocked@example.ru', 'Test12345!'), /Неверная/);
  store.logout(); assert.equal(store.currentUser(), null);
  assert.throws(() => store.snapshot(), /Недостаточно прав/);
});
