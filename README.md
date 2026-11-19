# X-RAY LINK Ecosystem

Complete multi-application VPN platform monorepo.

## Modules

- `backend/` — NestJS + TypeScript + Prisma (PostgreSQL) + Redis + WebSocket central control plane
- `database/` — Normalized PostgreSQL DDL migrations (`001_init_schema.sql`)
- `web-admin/` — Next.js Super Admin & Delegated Admin web console (17 pages)
- `reseller-panel/` — Next.js Reseller dashboard with strict server-side ownership isolation & credit ledger
- `android-client/` — Kotlin + Jetpack Compose + Material 3 + Hilt + `VpnService` (`WireGuardTunnelAdapter` + modular SSH/TLS/WS transport adapters)
- `android-admin/` — Kotlin + Jetpack Compose Admin console synced with central backend
- `vpn-infrastructure/` — WireGuard setup, Xray WS-TLS config, Stunnel SSH-TLS config, Node Sync Agent
- `docker/` — Production Dockerfiles & `docker-compose.yml`
- `deployment/` — Nginx TLS reverse proxy, systemd unit, PostgreSQL backup script, production checklist
- `documentation/` — Architecture, API reference, Android APK build guide, testing guide, external infra requirements
- `tests/` — Automated security, RBAC, IDOR, credit atomicity, and session lifecycle tests
