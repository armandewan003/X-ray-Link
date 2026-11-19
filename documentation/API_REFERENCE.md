# X-RAY LINK REST & WebSocket API Reference (`/api/v1`)

## Authentication (`/api/v1/auth`)
- `GET /api/v1/auth/bootstrap-status` — Check initial Owner setup status
- `POST /api/v1/auth/setup-owner` — First-run Platform Owner registration
- `POST /api/v1/auth/login` — Email/password login for Owner, Admin, and Reseller
- `POST /api/v1/auth/pin-login` — PIN + deviceFingerprint login for Android VPN Client
- `POST /api/v1/auth/refresh` — Rotate opaque refresh token & issue fresh JWT
- `POST /api/v1/auth/logout` — Revoke refresh token
- `POST /api/v1/auth/change-password` — Update authenticated account password
- `GET /api/v1/auth/me` — Current authenticated principal, wallet, or user/device profile

## RBAC & Reseller Endpoints
- `GET/POST/PATCH/DELETE /api/v1/admins` (Owner only)
- `GET/POST/PATCH /api/v1/resellers` (Owner / permitted Admin)
- `POST /api/v1/credits/adjust` (ACID transaction with `idempotencyKey`)
- `GET /api/v1/transactions` (Immutable financial ledger; Resellers see strictly own records)
- `GET/POST/GET:id/PATCH:id/DELETE:id /api/v1/users` (Strict reseller IDOR verification)
- `GET /api/v1/pins`, `POST /api/v1/pins/:id/revoke`

## Infrastructure & Tunneling
- `GET/POST/PATCH/DELETE /api/v1/servers` (Sanitized; server private keys never exposed)
- `GET/POST/PATCH/DELETE /api/v1/profiles` (WireGuard, SSH_TLS, WEBSOCKET_TLS, XRAY_VLESS)
- `POST /api/v1/sessions/connect` -> `POST /api/v1/sessions/:id/confirm` -> `POST /api/v1/sessions/:id/heartbeat` -> `POST /api/v1/sessions/:id/disconnect`
- `POST /api/v1/sessions/:id/revoke` (Immediate session termination)
- `GET /api/v1/traffic/summary`
- `GET/PATCH /api/v1/config/:key` (Maintenance mode, kill switch, app version, credit pricing)
- `GET/POST /api/v1/notifications/announcements`
- `GET /api/v1/audit-logs`

## WebSocket Real-Time Control (`/ws`)
Authenticate via `?token=<JWT>` or `{ "type": "auth", "token": "<JWT>" }`.
Events: `server_status`, `session_update`, `traffic_update`, `config_version_updated`, `user_status`, `notification`, `maintenance_update`.
