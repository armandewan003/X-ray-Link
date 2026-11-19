# X-RAY LINK Testing Guide

## 1. Automated Backend Security & Integration Tests
Run from the root workspace:
```bash
npm test
npx tsx --test tests/security_extended.test.ts
```

### Verified Test Coverage
- Owner bootstrap & Admin granular permission enforcement (`403` when missing permission)
- Reseller credit adjustments, duplicate `idempotency_key` rejection (`409`), and negative balance protection (`400`)
- Reseller atomic user creation charge & strict cross-reseller IDOR isolation (`403`)
- PIN authentication, expiration rejection, revocation rejection, brute-force lockout (`429`), and device limit enforcement
- VPN session lifecycle (`CONNECTING` -> `CONNECTED` -> `DISCONNECTED` / `REVOKED`)
- Maintenance mode & Owner emergency kill-switch enforcement (`503`)
- Refresh token rotation and replay detection (`401`)
- Authenticated WebSocket remote configuration synchronization (`config_version_updated`)
- Immutable audit logs verification

## 2. Frontend Compilation & Type Checks
```bash
npm run lint
npm run build
```
