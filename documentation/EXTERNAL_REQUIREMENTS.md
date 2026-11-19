# X-RAY LINK External Infrastructure & Environment Requirements

The following external runtime values and infrastructure components are required for full production deployment and cannot be executed inside the Node.js sandbox:

## 1. Required Environment Variables
- `JWT_SECRET`: Persistent 64-character hex string (32 random bytes, e.g., `openssl rand -hex 32`). Must NOT be `"CHANGE_ME_IN_PRODUCTION_32_BYTE_HEX"`. If omitted, the local runtime generates an ephemeral per-process fallback secret which invalidates tokens on restart.
- `DB_PATH`: Local SQLite database file path for the central Express runtime. Required value for the existing workspace state: `./data/xraylink.db` (or `/app/applet/data/xraylink.db`).
- `DATABASE_URL` / `REDIS_URL`: Required when running the Docker Compose PostgreSQL 16 + Redis 7 NestJS stack in `x-ray-link/docker/docker-compose.yml`.
- `GEMINI_API_KEY`: Required server-side for the Brand Icon Studio (`gemini-nano-banana-2.1`).

## 2. External Toolchains & Infrastructure
- **Android APK Compilation**: Requires JDK 17, Android SDK 35, Gradle 8.10+, and a release signing keystore. APKs have NOT been compiled in this Node.js-only environment.
- **VPN Edge Nodes**: Requires Ubuntu 24.04 VPS hosts with `CAP_NET_ADMIN` / WireGuard kernel module, Xray-core, Stunnel, and Let's Encrypt TLS certificates. VPN edge nodes have NOT been deployed from this environment.
