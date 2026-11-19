import { describe, it, before, after } from 'node:test';
import assert from 'node:assert/strict';
import type { AddressInfo } from 'node:net';
import { createServer } from '../server.ts';

let baseUrl = '';
let closeServer: () => Promise<void>;
let ownerToken = '';
let resellerToken = '';
let resellerId = '';
let userPin = '';
let userToken = '';
let userRefreshToken = '';

async function api(path: string, opts?: RequestInit & { token?: string }) {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(opts?.token ? { Authorization: `Bearer ${opts.token}` } : {}),
  };
  const res = await fetch(`${baseUrl}${path}`, {
    ...opts,
    headers: { ...headers, ...(opts?.headers as Record<string, string> | undefined) },
  });
  const body = (await res.json()) as Record<string, unknown>;
  return { status: res.status, body };
}

describe('X-RAY LINK Extended Security, Scoping & Resilience Suite', () => {
  before(async () => {
    const srv = await createServer({ dbPath: ':memory:', withVite: false });
    await new Promise<void>((resolve) => {
      srv.httpServer.listen(0, '127.0.0.1', () => {
        const addr = srv.httpServer.address() as AddressInfo;
        baseUrl = `http://127.0.0.1:${addr.port}`;
        resolve();
      });
    });
    closeServer = () =>
      new Promise((resolve) => {
        srv.wss.close();
        srv.httpServer.close(() => resolve());
      });

    const setup = await api('/api/v1/auth/setup-owner', {
      method: 'POST',
      body: JSON.stringify({ name: 'Owner', email: 'owner@ext.test', password: 'StrongOwnerPassword!9' }),
    });
    ownerToken = String(setup.body.accessToken);
  });

  after(async () => {
    await closeServer();
  });

  it('1. Enforces reseller server/profile scoping & never exposes server private keys', async () => {
    const resCreate = await api('/api/v1/resellers', {
      method: 'POST',
      token: ownerToken,
      body: JSON.stringify({
        name: 'Scoped Partner',
        email: 'scoped@ext.test',
        password: 'ScopedPassword!9',
        initialCredits: 100,
        allowedServerIds: ['srv-mv-01'],
        allowedProfileIds: ['prof-ws-tls'],
      }),
    });
    assert.equal(resCreate.status, 201);
    resellerId = String(resCreate.body.id);

    const resLogin = await api('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email: 'scoped@ext.test', password: 'ScopedPassword!9' }),
    });
    resellerToken = String(resLogin.body.accessToken);

    const userCreate = await api('/api/v1/users', {
      method: 'POST',
      token: resellerToken,
      body: JSON.stringify({ username: 'scoped_u1', validityDays: 30, deviceLimit: 2 }),
    });
    assert.equal(userCreate.status, 201);
    userPin = String((userCreate.body.pin as { pinCode: string }).pinCode);

    const pinLogin = await api('/api/v1/auth/pin-login', {
      method: 'POST',
      body: JSON.stringify({ pin: userPin, deviceFingerprint: 'dev-ext-1', deviceName: 'Pixel' }),
    });
    assert.equal(pinLogin.status, 200);
    userToken = String(pinLogin.body.accessToken);
    userRefreshToken = String(pinLogin.body.refreshToken);

    // User under Scoped Partner should only see srv-mv-01 & prof-ws-tls
    const srvs = await api('/api/v1/servers', { token: userToken });
    assert.equal(srvs.status, 200);
    const serverList = srvs.body.servers as Array<Record<string, unknown>>;
    assert.equal(serverList.length, 1);
    assert.equal(serverList[0].id, 'srv-mv-01');
    assert.equal(serverList[0].privateKey, undefined);

    // Attempting to connect to unpermitted srv-sg-01 must fail with 403
    const forbiddenConnect = await api('/api/v1/sessions/connect', {
      method: 'POST',
      token: userToken,
      body: JSON.stringify({ serverId: 'srv-sg-01', profileId: 'prof-ws-tls' }),
    });
    assert.equal(forbiddenConnect.status, 403);
    assert.ok(resellerId);
  });

  it('2. Rotates refresh tokens and rejects replayed refresh tokens', async () => {
    const rotated = await api('/api/v1/auth/refresh', {
      method: 'POST',
      body: JSON.stringify({ refreshToken: userRefreshToken }),
    });
    assert.equal(rotated.status, 200);
    assert.ok(rotated.body.accessToken);
    assert.notEqual(rotated.body.refreshToken, userRefreshToken);

    // Reusing old refresh token must fail with 401
    const replay = await api('/api/v1/auth/refresh', {
      method: 'POST',
      body: JSON.stringify({ refreshToken: userRefreshToken }),
    });
    assert.equal(replay.status, 401);
  });

  it('3. Enforces maintenance mode & emergency kill switch on tunnels', async () => {
    // Connect valid session first
    const conn = await api('/api/v1/sessions/connect', {
      method: 'POST',
      token: userToken,
      body: JSON.stringify({ serverId: 'srv-mv-01', profileId: 'prof-ws-tls' }),
    });
    assert.equal(conn.status, 201);
    const sessionId = String(conn.body.sessionId);
    await api(`/api/v1/sessions/${sessionId}/confirm`, {
      method: 'POST',
      token: userToken,
      body: JSON.stringify({ tunnelEstablished: true }),
    });

    // Engage emergency kill switch as Owner
    const ks = await api('/api/v1/config/emergency_kill_switch', {
      method: 'PATCH',
      token: ownerToken,
      body: JSON.stringify({ enabled: true, reason: 'Emergency test' }),
    });
    assert.equal(ks.status, 200);

    // Heartbeat must instruct client to DISCONNECT
    const hb = await api(`/api/v1/sessions/${sessionId}/heartbeat`, {
      method: 'POST',
      token: userToken,
      body: JSON.stringify({ bytesUpDelta: 100, bytesDownDelta: 100 }),
    });
    assert.equal(hb.status, 200);
    assert.equal(hb.body.action, 'DISCONNECT');

    // New PIN login blocked with 503
    const blockedPin = await api('/api/v1/auth/pin-login', {
      method: 'POST',
      body: JSON.stringify({ pin: userPin, deviceFingerprint: 'dev-ext-1', deviceName: 'Pixel' }),
    });
    assert.equal(blockedPin.status, 503);

    // Restore kill switch
    await api('/api/v1/config/emergency_kill_switch', {
      method: 'PATCH',
      token: ownerToken,
      body: JSON.stringify({ enabled: false, reason: '' }),
    });
  });

  it('4. Locks out PIN brute-force attempts with HTTP 429', async () => {
    for (let i = 0; i < 5; i++) {
      const bad = await api('/api/v1/auth/pin-login', {
        method: 'POST',
        body: JSON.stringify({ pin: `XL-FAKE-000${i}`, deviceFingerprint: 'bf-dev', deviceName: 'BF' }),
      });
      assert.equal(bad.status, 401);
    }
    const locked = await api('/api/v1/auth/pin-login', {
      method: 'POST',
      body: JSON.stringify({ pin: userPin, deviceFingerprint: 'bf-dev', deviceName: 'BF' }),
    });
    assert.equal(locked.status, 429);
  });
});
