-- X-RAY LINK Central PostgreSQL Migration 001
-- Normalized schema for Super Admin, Admin, Reseller, VPN Users, Servers, Profiles, Sessions, Ledger & Audit

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE roles (
  name VARCHAR(32) PRIMARY KEY,
  description TEXT NOT NULL
);

CREATE TABLE permissions (
  code VARCHAR(64) PRIMARY KEY,
  description TEXT NOT NULL
);

CREATE TABLE administrators (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(120) NOT NULL,
  email VARCHAR(190) NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  role_name VARCHAR(16) NOT NULL CHECK (role_name IN ('OWNER', 'ADMIN')),
  permissions JSONB NOT NULL DEFAULT '[]'::jsonb,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED')),
  last_login_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMPTZ
);

CREATE TABLE resellers (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(120) NOT NULL,
  email VARCHAR(190) NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'EXPIRED')),
  credit_limit INTEGER NOT NULL DEFAULT 10000,
  max_users INTEGER NOT NULL DEFAULT 100,
  allow_negative_balance BOOLEAN NOT NULL DEFAULT FALSE,
  expires_at TIMESTAMPTZ,
  permissions JSONB NOT NULL DEFAULT '["create_users","generate_pins","extend_users","suspend_users"]'::jsonb,
  created_by_admin_id UUID REFERENCES administrators(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMPTZ
);

CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  username VARCHAR(96) NOT NULL UNIQUE,
  display_name VARCHAR(120) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'EXPIRED', 'REVOKED')),
  device_limit INTEGER NOT NULL DEFAULT 2 CHECK (device_limit > 0),
  expires_at TIMESTAMPTZ NOT NULL,
  notes TEXT DEFAULT '',
  created_by_role VARCHAR(16) NOT NULL CHECK (created_by_role IN ('OWNER', 'ADMIN', 'RESELLER')),
  created_by_id UUID NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMPTZ
);

CREATE TABLE reseller_user_assignments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  reseller_id UUID NOT NULL REFERENCES resellers(id) ON DELETE CASCADE,
  user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
  assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE pin_codes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  pin_code VARCHAR(32) NOT NULL UNIQUE,
  pin_hash VARCHAR(128) NOT NULL UNIQUE,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'EXPIRED', 'REVOKED', 'SUSPENDED')),
  expires_at TIMESTAMPTZ NOT NULL,
  issued_by_role VARCHAR(16) NOT NULL,
  issued_by_id UUID NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  revoked_at TIMESTAMPTZ
);

CREATE TABLE registered_devices (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  device_fingerprint VARCHAR(190) NOT NULL,
  device_name VARCHAR(120) NOT NULL,
  platform VARCHAR(32) NOT NULL DEFAULT 'android',
  app_version VARCHAR(32) NOT NULL DEFAULT '1.0.0',
  last_ip INET,
  is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
  last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (user_id, device_fingerprint)
);

CREATE TABLE vpn_servers (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(120) NOT NULL,
  country_code CHAR(2) NOT NULL,
  location VARCHAR(160) NOT NULL,
  endpoint VARCHAR(255) NOT NULL,
  port INTEGER NOT NULL CHECK (port BETWEEN 1 AND 65535),
  protocol VARCHAR(32) NOT NULL CHECK (protocol IN ('WIREGUARD', 'SSH_TLS', 'WEBSOCKET_TLS', 'XRAY_VLESS')),
  wg_public_key TEXT NOT NULL,
  priority INTEGER NOT NULL DEFAULT 10,
  capacity INTEGER NOT NULL DEFAULT 500,
  latency_ms INTEGER NOT NULL DEFAULT 25,
  health_status VARCHAR(16) NOT NULL DEFAULT 'ONLINE' CHECK (health_status IN ('ONLINE', 'DEGRADED', 'OFFLINE')),
  is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMPTZ
);

CREATE TABLE connection_profiles (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(120) NOT NULL,
  protocol VARCHAR(32) NOT NULL CHECK (protocol IN ('WIREGUARD', 'SSH_TLS', 'WEBSOCKET_TLS', 'XRAY_VLESS')),
  authorized_hostname VARCHAR(255) NOT NULL,
  sni_host VARCHAR(255) NOT NULL DEFAULT '',
  ws_path VARCHAR(255) NOT NULL DEFAULT '/xray-ws',
  ws_headers JSONB NOT NULL DEFAULT '{}'::jsonb,
  description TEXT NOT NULL DEFAULT '',
  access_tier VARCHAR(16) NOT NULL DEFAULT 'STANDARD' CHECK (access_tier IN ('STANDARD', 'PREMIUM', 'RESTRICTED')),
  is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMPTZ
);

CREATE TABLE server_profile_assignments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  server_id UUID NOT NULL REFERENCES vpn_servers(id) ON DELETE CASCADE,
  profile_id UUID NOT NULL REFERENCES connection_profiles(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (server_id, profile_id)
);

CREATE TABLE reseller_server_permissions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  reseller_id UUID NOT NULL REFERENCES resellers(id) ON DELETE CASCADE,
  server_id UUID NOT NULL REFERENCES vpn_servers(id) ON DELETE CASCADE,
  granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (reseller_id, server_id)
);

CREATE TABLE reseller_profile_permissions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  reseller_id UUID NOT NULL REFERENCES resellers(id) ON DELETE CASCADE,
  profile_id UUID NOT NULL REFERENCES connection_profiles(id) ON DELETE CASCADE,
  granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (reseller_id, profile_id)
);

CREATE TABLE vpn_sessions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  pin_id UUID NOT NULL REFERENCES pin_codes(id),
  device_id UUID NOT NULL REFERENCES registered_devices(id),
  server_id UUID NOT NULL REFERENCES vpn_servers(id),
  profile_id UUID NOT NULL REFERENCES connection_profiles(id),
  status VARCHAR(24) NOT NULL CHECK (status IN ('CONNECTING', 'CONNECTED', 'RECONNECTING', 'DISCONNECTED', 'FAILED', 'EXPIRED', 'REVOKED')),
  client_tunnel_ip VARCHAR(64) NOT NULL,
  client_public_key TEXT NOT NULL DEFAULT '',
  bytes_up BIGINT NOT NULL DEFAULT 0,
  bytes_down BIGINT NOT NULL DEFAULT 0,
  upload_speed_bps BIGINT NOT NULL DEFAULT 0,
  download_speed_bps BIGINT NOT NULL DEFAULT 0,
  latency_ms INTEGER NOT NULL DEFAULT 0,
  connected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_heartbeat_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  disconnected_at TIMESTAMPTZ,
  disconnect_reason TEXT
);

CREATE TABLE traffic_records (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  session_id UUID NOT NULL REFERENCES vpn_sessions(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  server_id UUID NOT NULL REFERENCES vpn_servers(id),
  bytes_up_delta BIGINT NOT NULL CHECK (bytes_up_delta >= 0),
  bytes_down_delta BIGINT NOT NULL CHECK (bytes_down_delta >= 0),
  recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE reseller_wallets (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  reseller_id UUID NOT NULL UNIQUE REFERENCES resellers(id) ON DELETE CASCADE,
  balance_credits INTEGER NOT NULL DEFAULT 0,
  total_credited INTEGER NOT NULL DEFAULT 0,
  total_spent INTEGER NOT NULL DEFAULT 0,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE credit_transactions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  reseller_id UUID NOT NULL REFERENCES resellers(id) ON DELETE CASCADE,
  tx_type VARCHAR(32) NOT NULL CHECK (tx_type IN ('CREDIT_ADD', 'CREDIT_DEDUCT', 'USER_CREATE_CHARGE', 'USER_EXTEND_CHARGE', 'REFUND')),
  amount INTEGER NOT NULL CHECK (amount > 0),
  balance_before INTEGER NOT NULL,
  balance_after INTEGER NOT NULL,
  reference_user_id UUID REFERENCES users(id),
  idempotency_key VARCHAR(128) NOT NULL UNIQUE,
  description TEXT NOT NULL,
  performed_by_role VARCHAR(16) NOT NULL,
  performed_by_id UUID NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE announcements (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  title VARCHAR(180) NOT NULL,
  body TEXT NOT NULL,
  severity VARCHAR(16) NOT NULL DEFAULT 'INFO' CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
  target_role VARCHAR(16) NOT NULL DEFAULT 'ALL',
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_by_id UUID NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE application_settings (
  key VARCHAR(64) PRIMARY KEY,
  value JSONB NOT NULL,
  updated_by_id VARCHAR(64) NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE configuration_versions (
  version SERIAL PRIMARY KEY,
  change_summary TEXT NOT NULL,
  changed_by_role VARCHAR(16) NOT NULL,
  changed_by_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE notifications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  recipient_role VARCHAR(16) NOT NULL,
  recipient_id VARCHAR(64) NOT NULL,
  title VARCHAR(180) NOT NULL,
  message TEXT NOT NULL,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE audit_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  actor_role VARCHAR(16) NOT NULL,
  actor_id VARCHAR(64) NOT NULL,
  actor_email VARCHAR(190) NOT NULL,
  action VARCHAR(64) NOT NULL,
  resource_type VARCHAR(64) NOT NULL,
  resource_id VARCHAR(64) NOT NULL,
  details JSONB NOT NULL DEFAULT '{}'::jsonb,
  ip_address VARCHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_pin_codes_user ON pin_codes(user_id);
CREATE INDEX idx_reseller_assign_reseller ON reseller_user_assignments(reseller_id);
CREATE INDEX idx_vpn_sessions_status ON vpn_sessions(status);
CREATE INDEX idx_traffic_user_time ON traffic_records(user_id, recorded_at DESC);
CREATE INDEX idx_credit_tx_reseller ON credit_transactions(reseller_id, created_at DESC);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at DESC);
