import {test} from 'node:test';
import assert from 'node:assert/strict';

test('API uses CSRF and never preserves an authenticated view after a 401', async () => {
    const calls = [];
    globalThis.location = {
        pathname: '/auth/login.html', replace() {
        }
    };
    let expired = false;
    globalThis.fetch = async (url, options = {}) => {
        calls.push([url, options]);
        if (url.endsWith('/csrf')) return Response.json({token: 'csrf-test', headerName: 'X-CSRF-TOKEN'});
        if (expired) return Response.json({message: 'Сессия истекла'}, {status: 401});
        if (url.endsWith('/session') || url.endsWith('/login')) return Response.json({id: 'u', role: 'ROLE_USER'});
        if (url.endsWith('/snapshot')) return Response.json({user: {id: 'u'}, slots: [], events: []});
        return new Response(null, {status: 204});
    };
    const api = await import('../src/assets/api.js');
    await api.init();
    assert.equal(api.currentUser().id, 'u');
    await api.updateProfile({name: 'Клиент', phone: '', height: ''});
    const patch = calls.find(([u, o]) => u === '/api/me' && o.method === 'PATCH')[1];
    assert.equal(patch.headers['X-CSRF-TOKEN'], 'csrf-test');
    assert.equal(patch.credentials, 'same-origin');
    assert.equal(JSON.parse(patch.body).height, null);
    expired = true;
    await assert.rejects(api.refresh(), /Сессия истекла/);
    assert.equal(api.currentUser(), null);
    assert.throws(() => api.snapshot(), /не загружены/);
});

test('registration sends only registration fields, never a chosen role', async () => {
    let sent;
    globalThis.fetch = async (url, options) => {
        if (url.endsWith('/csrf')) return Response.json({token: 'token', headerName: 'X-CSRF-TOKEN'});
        sent = JSON.parse(options.body);
        return Response.json({id: 'u', role: 'ROLE_USER'});
    };
    const api = await import('../src/assets/api.js');
    await api.register({name: 'Клиент', email: 'client@test.ru', password: 'Testpass123!', role: 'ROLE_ADMIN'});
    assert.deepEqual(Object.keys(sent).sort(), ['email', 'name', 'password']);
});
