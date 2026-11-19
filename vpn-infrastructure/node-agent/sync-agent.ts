import { execFile } from 'node:child_process';
import { promisify } from 'node:util';

const execFileAsync = promisify(execFile);

interface PeerSyncItem {
  clientPublicKey: string;
  clientTunnelIp: string;
  status: string;
}

const API_BASE = process.env.XRAYLINK_API_BASE || 'http://127.0.0.1:4000/api/v1';
const NODE_TOKEN = process.env.XRAYLINK_NODE_TOKEN || '';
const WG_IFACE = process.env.WG_IFACE || 'wg0';

async function syncActivePeers(): Promise<void> {
  if (!NODE_TOKEN) {
    throw new Error('XRAYLINK_NODE_TOKEN is required to authenticate node sync agent');
  }
  const resp = await fetch(`${API_BASE}/sessions`, {
    headers: { Authorization: `Bearer ${NODE_TOKEN}` },
  });
  if (!resp.ok) {
    throw new Error(`Failed to fetch sessions: ${resp.status}`);
  }
  const data = (await resp.json()) as { sessions: PeerSyncItem[] };
  for (const s of data.sessions) {
    if (!s.clientPublicKey || !/^[A-Za-z0-9+/=]{40,48}$/.test(s.clientPublicKey)) continue;
    if (s.status === 'CONNECTED' || s.status === 'CONNECTING') {
      await execFileAsync('wg', ['set', WG_IFACE, 'peer', s.clientPublicKey, 'allowed-ips', s.clientTunnelIp]);
    } else if (s.status === 'REVOKED' || s.status === 'DISCONNECTED' || s.status === 'EXPIRED') {
      await execFileAsync('wg', ['set', WG_IFACE, 'peer', s.clientPublicKey, 'remove']);
    }
  }
}

setInterval(() => {
  syncActivePeers().catch((err) => console.error('[NodeAgent Sync Error]', err.message));
}, 10_000);
