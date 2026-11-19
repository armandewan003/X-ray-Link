# X-RAY LINK Production Deployment Checklist

- [ ] Set strong random 64-char `JWT_SECRET` and `REFRESH_TOKEN_SECRET` in environment secrets
- [ ] Configure `DATABASE_URL` (PostgreSQL 16 with SSL enabled) and `REDIS_URL`
- [ ] Run `database/migrations/001_init_schema.sql` and verify foreign keys & indexes
- [ ] Bootstrap the Platform Owner via `/api/v1/auth/setup-owner` (ensure `NODE_ENV=production` disables sandbox quick-login)
- [ ] Provision WireGuard / Xray-WS-TLS VPS nodes and register public keys only in Super Admin -> Servers
- [ ] Enable automated daily PostgreSQL backups via `deployment/backup-postgres.sh`
- [ ] Verify HTTPS & WSS termination in `deployment/nginx-ssl.conf`
- [ ] Build signed release APKs for `android-client` and `android-admin`
