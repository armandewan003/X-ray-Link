import { describe, it, before, after } from 'node:test';
import assert from 'node:assert/strict';
import type { AddressInfo } from 'node:net';
import WebSocket from 'ws';
import { createServer } from '../server.ts';

let baseUrl = '';
let wsUrl = '';
let closeServer: () => Promise<void>;

let ownerToken = '';
let limitedAdminToken = '';
let resellerAToken = '';
let resellerAId = '';
let resellerBToken = '';
let resellerBId = '';
let resellerAUserId = '';
let resellerAUserPin = '';
let resellerBUserId = '';
let userToken = '';
let activeSessionId = '';

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

describe('X-RAY LINK Central Backend Security & Integration Suite', () => {
  before(async () => {
    const srv = await createServer({ dbPath: ':memory:', withVite: false });
    await new Promise<void>((resolve) => {
      srv.httpServer.listen(0, '127.0.0.1', () => {
        const addr = srv.httpServer.address() as AddressInfo;
        baseUrl = `http://127.0.0.1:${addr.port}`;
        wsUrl = `ws://127.0.0.1:${addr.port}/ws`;
        resolve();
      });
    });
    closeServer = () =>
      new Promise((resolve) => {
        srv.wss.close();
        srv.httpServer.close(() => resolve());
      });
  });

  after(async () => {
    await closeServer();
  });

  it('1. Owner bootstrap & Admin granular permission enforcement', async () => {
    const boot = await api('/api/v1/auth/bootstrap-status');
    assert.equal(boot.status, 200);
    assert.equal(boot.body.needsOwnerSetup, true);

    const setup = await api('/api/v1/auth/setup-owner', {
      method: 'POST',
      body: JSON.stringify({ name: 'Master Owner', email: 'owner@xraylink.test', password: 'OwnerStrongPassword!1' }),
    });
    assert.equal(setup.status, 201);
    ownerToken = String(setup.body.accessToken);
    assert.ok(ownerToken.length > 20);

    // Owner creates an Admin with ONLY view_traffic (no manage_servers)
    const createAdm = await api('/api/v1/admins', {
      method: 'POST',
      token: ownerToken,
      body: JSON.stringify({
        name: 'Traffic Viewer',
        email: 'viewer@xraylink.test',
        password: 'AdminPassword!1',
        permissions: ['view_traffic'],
      }),
    });
    assert.equal(createAdm.status, 201);

    const admLogin = await api('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email: 'viewer@xraylink.test', password: 'AdminPassword!1' }),
    });
    assert.equal(admLogin.status, 200);
    limitedAdminToken = String(admLogin.body.accessToken);

    // Limited admin can read traffic summary
    const trafficRes = await api('/api/v1/traffic/summary', { token: limitedAdminToken });
    assert.equal(trafficRes.status, 200);

    // Limited admin CANNOT create servers or admins
    const forbiddenSrv = await api('/api/v1/servers', {
      method: 'POST',
      token: limitedAdminToken,
      body: JSON.stringify({ name: 'Unauthorized', endpoint: '1.2.3.4', port: 51820, protocol: 'WIREGUARD' }),
    });
    assert.equal(forbiddenSrv.status, 403);

    const forbiddenAdm = await api('/api/v1/admins', {
      method: 'POST',
      token: limitedAdminToken,
      body: JSON.stringify({ name: 'Hack', email: 'h@x.com', password: 'Password!1' }),
    });
    assert.equal(forbiddenAdm.status, 403);
  });

  it('2. Reseller credit deductions, idempotency, and negative balance guard', async () => {
    const resA = await api('/api/v1/resellers', {
      method: 'POST',
      token: ownerToken,
      body: JSON.stringify({
        name: 'Reseller Alpha',
        email: 'alpha@xraylink.test',
        password: 'ResellerPassword!1',
        initialCredits: 25,
        creditLimit: 500,
        maxUsers: 10,
      }),
    });
    assert.equal(resA.status, 201);
    resellerAId = String(resA.body.id);

    const resB = await api('/api/v1/resellers', {
      method: 'POST',
      token: ownerToken,
      body: JSON.stringify({
        name: 'Reseller Beta',
        email: 'beta@xraylink.test',
        password: 'ResellerPassword!1',
        initialCredits: 100,
      }),
    });
    assert.equal(resB.status, 201);
    resellerBId = String(resB.body.id);

    // Duplicate idempotency key must fail
    const dupKey = 'idem-test-001';
    const adj1 = await api('/api/v1/credits/adjust', {
      method: 'POST',
      token: ownerToken,
      body: JSON.stringify({ resellerId: resellerAId, type: 'CREDIT_ADD', amount: 15, idempotencyKey: dupKey }),
    });
    assert.equal(adj1.status, 200);
    assert.equal(adj1.body.balanceCredits, 40);

    const adjDup = await api('/api/v1/credits/adjust', {
      method: 'POST',
      token: ownerToken,
      body: JSON.stringify({ resellerId: resellerAId, type: 'CREDIT_ADD', amount: 15, idempotencyKey: dupKey }),
    });
    assert.equal(adjDup.status, 409);

    // Negative balance must be blocked
    const negRes = await api('/api/v1/credits/adjust', {
      method: 'POST',
      token: ownerToken,
      body: JSON.stringify({ resellerId: resellerAId, type: 'CREDIT_DEDUCT', amount: 9999 }),
    });
    assert.equal(negRes.status, 400);

    const loginA = await api('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email: 'alpha@xraylink.test', password: 'ResellerPassword!1' }),
    });
    resellerAToken = String(loginA.body.accessToken);

    const loginB = await api('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email: 'beta@xraylink.test', password: 'ResellerPassword!1' }),
    });
    resellerBToken = String(loginB.body.accessToken);
  });

  it('3. Reseller user creation charges credits atomically & enforces strict cross-reseller IDOR isolation', async () => {
    // Reseller A creates user (costs 10 credits -> balance drops 40 -> 30)
    const uA = await api('/api/v1/users', {
      method: 'POST',
      token: resellerAToken,
      body: JSON.stringify({ username: 'alpha_client_1', displayName: 'Alpha 1', validityDays: 30, deviceLimit: 1 }),
    });
    assert.equal(uA.status, 201);
    resellerAUserId = String((uA.body.user as { id: string }).id);
    resellerAUserPin = String((uA.body.pin as { pinCode: string }).pinCode);

    const dashA = await api('/api/v1/dashboard/reseller', { token: resellerAToken });
    assert.equal((dashA.body.stats as { availableCredits: number }).availableCredits, 30);

    // Reseller B creates their own user
    const uB = await api('/api/v1/users', {
      method: 'POST',
      token: resellerBToken,
      body: JSON.stringify({ username: 'beta_client_1', displayName: 'Beta 1', validityDays: 30, deviceLimit: 2 }),
    });
    assert.equal(uB.status, 201);
    resellerBUserId = String((uB.body.user as { id: string }).id);

    // Cross-reseller access prevention: Reseller A CANNOT view, suspend, or delete Reseller B's user
    const idorGet = await api(`/api/v1/users/${resellerBUserId}`, { token: resellerAToken });
    assert.equal(idorGet.status, 403);

    const idorPatch = await api(`/api/v1/users/${resellerBUserId}`, {
      method: 'PATCH',
      token: resellerAToken,
      body: JSON.stringify({ status: 'SUSPENDED' }),
    });
    assert.equal(idorPatch.status, 403);

    const idorDelete = await api(`/api/v1/users/${resellerBUserId}`, {
      method: 'DELETE',
      token: resellerAToken,
    });
    assert.equal(idorDelete.status, 403);

    // Reseller cannot modify global servers or app config
    const resSrv = await api('/api/v1/servers', {
      method: 'POST',
      token: resellerAToken,
      body: JSON.stringify({ name: 'Sneaky', endpoint: '1.1.1.1', port: 443, protocol: 'WIREGUARD' }),
    });
    assert.equal(resSrv.status, 403);
  });

  it('4. PIN authentication, device limit enforcement, VPN session lifecycle & WebSocket remote config sync', async () => {
    // Device 1 succeeds
    const login1 = await api('/api/v1/auth/pin-login', {
      method: 'POST',
      body: JSON.stringify({
        pin: resellerAUserPin,
        deviceFingerprint: 'android-dev-001',
        deviceName: 'Pixel 9',
      }),
    });
    assert.equal(login1.status, 200);
    userToken = String(login1.body.accessToken);

    // Device 2 exceeds deviceLimit=1 -> rejected
    const login2 = await api('/api/v1/auth/pin-login', {
      method: 'POST',
      body: JSON.stringify({
        pin: resellerAUserPin,
        deviceFingerprint: 'android-dev-002',
        deviceName: 'Galaxy S25',
      }),
    });
    assert.equal(login2.status, 403);

    // WebSocket connects with Owner token and receives live config_version_updated when server changes
    const ws = new WebSocket(`${wsUrl}?token=${ownerToken}`);
    const wsEventPromise = new Promise<Record<string, unknown>>((resolve, reject) => {
      const timer = setTimeout(() => reject(new Error('Timed out waiting for WS event')), 4000);
      ws.on('message', (raw) => {
        const msg = JSON.parse(raw.toString()) as Record<string, unknown>;
        if (msg.type === 'config_version_updated') {
          clearTimeout(timer);
          resolve(msg);
        }
      });
    });

    await new Promise<void>((resolve) => ws.on('open', () => resolve()));

    const srvUpdate = await api('/api/v1/servers/srv-sg-01', {
      method: 'PATCH',
      token: ownerToken,
      body: JSON.stringify({ latencyMs: 14 }),
    });
    assert.equal(srvUpdate.status, 200);

    const wsEvent = await wsEventPromise;
    assert.equal(wsEvent.type, 'config_version_updated');
    ws.close();

    // Initiate VPN Session -> Confirm -> Heartbeat -> Disconnect
    const connectRes = await api('/api/v1/sessions/connect', {
      method: 'POST',
      token: userToken,
      body: JSON.stringify({ serverId: 'srv-sg-01', profileId: 'prof-wg-direct', clientPublicKey: 'clientPubKeyBase64==' }),
    });
    assert.equal(connectRes.status, 201);
    assert.equal(connectRes.body.status, 'CONNECTING');
    activeSessionId = String(connectRes.body.sessionId);

    const confirmRes = await api(`/api/v1/sessions/${activeSessionId}/confirm`, {
      method: 'POST',
      token: userToken,
      body: JSON.stringify({ tunnelEstablished: true }),
    });
    assert.equal(confirmRes.status, 200);
    assert.equal(confirmRes.body.status, 'CONNECTED');

    const hbRes = await api(`/api/v1/sessions/${activeSessionId}/heartbeat`, {
      method: 'POST',
      token: userToken,
      body: JSON.stringify({ bytesUpDelta: 2048, bytesDownDelta: 8192, uploadSpeedBps: 512, downloadSpeedBps: 2048 }),
    });
    assert.equal(hbRes.status, 200);
    assert.equal(hbRes.body.action, 'KEEP_ALIVE');

    const discRes = await api(`/api/v1/sessions/${activeSessionId}/disconnect`, {
      method: 'POST',
      token: userToken,
      body: JSON.stringify({ reason: 'Test complete' }),
    });
    assert.equal(discRes.status, 200);
  });

  it('5. Revoked PIN & Audit logs verification', async () => {
    const pinsRes = await api('/api/v1/pins', { token: resellerAToken });
    const pinId = String((pinsRes.body.pins as Array<{ id: string }>)[0].id);

    const revRes = await api(`/api/v1/pins/${pinId}/revoke`, {
      method: 'POST',
      token: resellerAToken,
    });
    assert.equal(revRes.status, 200);

    const loginRevoked = await api('/api/v1/auth/pin-login', {
      method: 'POST',
      body: JSON.stringify({
        pin: resellerAUserPin,
        deviceFingerprint: 'android-dev-001',
        deviceName: 'Pixel 9',
      }),
    });
    assert.equal(loginRevoked.status, 403);

    const audits = await api('/api/v1/audit-logs', { token: ownerToken });
    assert.equal(audits.status, 200);
    assert.ok((audits.body.logs as unknown[]).length >= 5);
  });
});
