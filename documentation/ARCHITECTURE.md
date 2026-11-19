# X-RAY LINK Production Architecture & External Infrastructure Requirements

## 1. Central Control Plane
- Authoritative REST (`/api/v1/*`) & WebSocket (`/ws`) backend.
- Four-tier RBAC: `OWNER` -> `ADMIN` (granular permissions) -> `RESELLER` (strict ownership-scoped) -> `USER` (PIN + device fingerprint).
- Immutable `credit_transactions` ledger using ACID database transactions (`BEGIN IMMEDIATE` / PostgreSQL `SERIALIZABLE` or row-level locks) and unique `idempotency_key`.

## 2. External Requirements for Full Production Deployment
Because this AI Studio container runs Node.js without Docker, PostgreSQL, Redis, Java/Android SDK, or root network interfaces (`CAP_NET_ADMIN`), the following external components must be provisioned on target infrastructure:
1. **PostgreSQL 16 + Redis 7**: Provision managed PostgreSQL & Redis or launch via `docker/docker-compose.yml`, then run `database/migrations/001_init_schema.sql` or `npx prisma migrate deploy`.
2. **VPN Edge Nodes (Ubuntu 24.04 VPS)**: Run `vpn-infrastructure/wireguard/wg-setup.sh` to generate node keys and enable `wg0`, then register the node's **public key** and endpoint in Super Admin -> Servers. Never place node private keys in client apps or backend client responses.
3. **TLS Certificates & Domain**: Point `api.xraylink.example.com` to Nginx (`deployment/nginx-ssl.conf`) and issue Let's Encrypt certificates via Certbot.
4. **Android APK Builds**: Build `android-client/` and `android-admin/` using Android Studio / Gradle 8+ with JDK 17 and your release keystore (`signingConfigs`).
