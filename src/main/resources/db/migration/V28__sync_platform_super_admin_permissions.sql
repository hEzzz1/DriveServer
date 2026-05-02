INSERT INTO role_permission (role_code, permission_code)
VALUES
  ('PLATFORM_SUPER_ADMIN', 'overview.read'),
  ('PLATFORM_SUPER_ADMIN', 'alert.read'),
  ('PLATFORM_SUPER_ADMIN', 'alert.handle'),
  ('PLATFORM_SUPER_ADMIN', 'stats.read'),
  ('PLATFORM_SUPER_ADMIN', 'rule.read'),
  ('PLATFORM_SUPER_ADMIN', 'rule.manage'),
  ('PLATFORM_SUPER_ADMIN', 'audit.read'),
  ('PLATFORM_SUPER_ADMIN', 'audit.export'),
  ('PLATFORM_SUPER_ADMIN', 'system.read'),
  ('PLATFORM_SUPER_ADMIN', 'user.read'),
  ('PLATFORM_SUPER_ADMIN', 'user.manage'),
  ('PLATFORM_SUPER_ADMIN', 'enterprise.read'),
  ('PLATFORM_SUPER_ADMIN', 'enterprise.manage'),
  ('PLATFORM_SUPER_ADMIN', 'activation_code.read'),
  ('PLATFORM_SUPER_ADMIN', 'activation_code.manage'),
  ('PLATFORM_SUPER_ADMIN', 'fleet.read'),
  ('PLATFORM_SUPER_ADMIN', 'fleet.manage'),
  ('PLATFORM_SUPER_ADMIN', 'driver.read'),
  ('PLATFORM_SUPER_ADMIN', 'driver.manage'),
  ('PLATFORM_SUPER_ADMIN', 'vehicle.read'),
  ('PLATFORM_SUPER_ADMIN', 'vehicle.manage'),
  ('PLATFORM_SUPER_ADMIN', 'device.read'),
  ('PLATFORM_SUPER_ADMIN', 'device.manage'),
  ('PLATFORM_SUPER_ADMIN', 'session.read'),
  ('PLATFORM_SUPER_ADMIN', 'session.force_sign_out')
ON DUPLICATE KEY UPDATE
  created_at = created_at;
