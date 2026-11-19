-- X-RAY LINK Initial Roles, Permissions & Default Application Settings

INSERT INTO roles (name, description) VALUES
  ('OWNER', 'Super Administrator with unrestricted platform governance'),
  ('ADMIN', 'Delegated Administrator with granular configurable permissions'),
  ('RESELLER', 'Authorized partner managing scoped VPN users and credit balances'),
  ('USER', 'VPN Client authenticated via PIN and registered device')
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions (code, description) VALUES
  ('manage_users', 'Create, edit, suspend, extend, and revoke VPN user accounts'),
  ('manage_resellers', 'Create, suspend, configure limits and credits for resellers'),
  ('generate_pins', 'Generate and revoke subscriber PIN codes'),
  ('manage_servers', 'Add, edit, enable, disable, and delete VPN edge nodes'),
  ('manage_profiles', 'Manage transport payloads, SNI hosts, and WebSocket paths'),
  ('view_traffic', 'Inspect aggregate and per-server bandwidth statistics'),
  ('manage_announcements', 'Publish operator announcements and notifications'),
  ('monitor_sessions', 'Monitor and revoke active VPN tunnel sessions'),
  ('manage_settings', 'Configure application version policy and maintenance mode')
ON CONFLICT (code) DO NOTHING;

INSERT INTO application_settings (key, value, updated_by_id) VALUES
  ('maintenance_mode', '{"enabled":false,"message":"Scheduled network upgrade in progress."}'::jsonb, 'system'),
  ('emergency_kill_switch', '{"enabled":false,"reason":""}'::jsonb, 'system'),
  ('min_app_version', '{"androidClient":"1.0.0","androidAdmin":"1.0.0","forceUpdate":false}'::jsonb, 'system'),
  ('credit_pricing', '{"costPerUser30Days":10,"costPerExtend30Days":10,"costPerDay":1}'::jsonb, 'system'),
  ('global_limits', '{"maxGlobalUsers":100000,"defaultDeviceLimit":2,"maxDeviceLimit":10}'::jsonb, 'system')
ON CONFLICT (key) DO NOTHING;

INSERT INTO configuration_versions (change_summary, changed_by_role, changed_by_id)
SELECT 'Initial platform bootstrap', 'SYSTEM', 'system'
WHERE NOT EXISTS (SELECT 1 FROM configuration_versions);
