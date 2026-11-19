# X-RAY LINK Deployment Guide

## 1. Environment Configuration
1. Copy `.env.example` to `.env` in `x-ray-link/`.
2. Generate 64-character hex secrets (`openssl rand -hex 32`) for `JWT_SECRET` and `REFRESH_TOKEN_SECRET`.
3. Set `DATABASE_URL`, `REDIS_URL`, `POSTGRES_PASSWORD`, `REDIS_PASSWORD`, and `PUBLIC_API_BASE`.
4. For the standalone Node/SQLite control runtime, set `DB_PATH="./data/xraylink.db"` and a persistent 64-char hex `JWT_SECRET`.

## 2. Docker Compose Stack
```bash
cd x-ray-link/docker
docker compose --env-file ../.env up -d --build
```

## 3. Database Migration Verification
```bash
psql "$DATABASE_URL" -f ../database/migrations/001_init_schema.sql
psql "$DATABASE_URL" -f ../database/migrations/002_seed_defaults.sql
```

## 4. VPN Edge Node Provisioning
Run `vpn-infrastructure/wireguard/wg-setup.sh` on each Ubuntu 24.04 VPS and register its **public key** and IP/hostname in the Web Admin Servers view.
