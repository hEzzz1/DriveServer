CREATE TABLE IF NOT EXISTS permission (
  code VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  module VARCHAR(64) NOT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS role_template (
  code VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  domain VARCHAR(32) NOT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS role_permission (
  id BIGINT NOT NULL AUTO_INCREMENT,
  role_code VARCHAR(64) NOT NULL,
  permission_code VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_permission (role_code, permission_code),
  KEY idx_role_permission_role_code (role_code),
  KEY idx_role_permission_permission_code (permission_code),
  CONSTRAINT fk_role_permission_role_code
    FOREIGN KEY (role_code) REFERENCES role_template (code),
  CONSTRAINT fk_role_permission_permission_code
    FOREIGN KEY (permission_code) REFERENCES permission (code)
);

CREATE TABLE IF NOT EXISTS user_scope_role (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_code VARCHAR(64) NOT NULL,
  scope_type VARCHAR(16) NOT NULL,
  enterprise_id BIGINT DEFAULT NULL,
  fleet_id BIGINT DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_scope_role (user_id, role_code, scope_type, enterprise_id, fleet_id),
  KEY idx_user_scope_role_user_id (user_id),
  KEY idx_user_scope_role_scope (scope_type, enterprise_id, fleet_id),
  CONSTRAINT fk_user_scope_role_user_id
    FOREIGN KEY (user_id) REFERENCES user_account (id)
);

INSERT INTO permission (code, name, module, remark)
VALUES
  ('overview.read', '查看风险总览', 'overview', '风险总览与实时概览'),
  ('alert.read', '查看告警', 'alert', '告警列表、详情与操作日志'),
  ('alert.handle', '处置告警', 'alert', '确认、误报、关闭等告警操作'),
  ('stats.read', '查看统计分析', 'stats', '趋势分析与风险排行'),
  ('rule.read', '查看规则', 'rule', '规则查看与版本查看'),
  ('rule.manage', '管理规则', 'rule', '规则创建、发布、回滚与开关'),
  ('audit.read', '查看审计', 'audit', '审计日志查看'),
  ('audit.export', '导出审计', 'audit', '审计日志导出'),
  ('system.read', '查看系统状态', 'system', '系统健康、服务、版本与监控'),
  ('user.read', '查看用户', 'user', '用户列表与详情'),
  ('user.manage', '管理用户', 'user', '创建、编辑、分配角色、状态与重置密码'),
  ('enterprise.read', '查看企业', 'enterprise', '企业列表与详情'),
  ('enterprise.manage', '管理企业', 'enterprise', '创建企业与更新企业基础信息'),
  ('activation_code.read', '查看企业激活码', 'enterprise', '查看企业激活码与绑定日志'),
  ('activation_code.manage', '管理企业激活码', 'enterprise', '轮换与停用企业激活码'),
  ('fleet.read', '查看车队', 'fleet', '车队列表与详情'),
  ('fleet.manage', '管理车队', 'fleet', '创建、编辑与启停车队'),
  ('driver.read', '查看司机', 'driver', '司机列表与详情'),
  ('driver.manage', '管理司机', 'driver', '创建、编辑、调整车队与重置 PIN'),
  ('vehicle.read', '查看车辆', 'vehicle', '车辆列表与详情'),
  ('vehicle.manage', '管理车辆', 'vehicle', '创建、编辑与启停车辆'),
  ('device.read', '查看设备', 'device', '设备列表与详情'),
  ('device.manage', '管理设备', 'device', '创建设备、编辑、轮换 token 与车辆分配'),
  ('session.read', '查看会话', 'session', '驾驶会话列表与详情'),
  ('session.force_sign_out', '强制签退', 'session', '后台强制签退驾驶会话')
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  module = VALUES(module),
  remark = VALUES(remark),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO role_template (code, name, domain, remark, enabled)
VALUES
  ('PLATFORM_SUPER_ADMIN', '平台超级管理员', 'PLATFORM', '全局治理与兜底角色', 1),
  ('PLATFORM_SYS_ADMIN', '平台系统管理员', 'PLATFORM', '系统健康、服务、版本与审计治理', 1),
  ('PLATFORM_RISK_ADMIN', '平台风控管理员', 'PLATFORM', '规则治理与效果观察', 1),
  ('ORG_ADMIN', '组织管理员', 'ORG', '企业级业务管理角色', 1),
  ('ORG_OPERATOR', '组织运营处理人员', 'ORG', '告警处置与实时值守角色', 1),
  ('ORG_ANALYST', '组织分析查看人员', 'ORG', '趋势分析与风险复盘角色', 1),
  ('ORG_VIEWER', '组织只读观察人员', 'ORG', '基础总览与告警只读角色', 1)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  domain = VALUES(domain),
  remark = VALUES(remark),
  enabled = VALUES(enabled),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO role_permission (role_code, permission_code)
VALUES
  ('PLATFORM_SYS_ADMIN', 'audit.read'),
  ('PLATFORM_SYS_ADMIN', 'audit.export'),
  ('PLATFORM_SYS_ADMIN', 'system.read'),
  ('PLATFORM_RISK_ADMIN', 'rule.read'),
  ('PLATFORM_RISK_ADMIN', 'rule.manage'),
  ('PLATFORM_RISK_ADMIN', 'overview.read'),
  ('PLATFORM_RISK_ADMIN', 'alert.read'),
  ('PLATFORM_RISK_ADMIN', 'stats.read'),
  ('ORG_ADMIN', 'overview.read'),
  ('ORG_ADMIN', 'alert.read'),
  ('ORG_ADMIN', 'stats.read'),
  ('ORG_ADMIN', 'user.read'),
  ('ORG_ADMIN', 'user.manage'),
  ('ORG_ADMIN', 'enterprise.read'),
  ('ORG_ADMIN', 'activation_code.read'),
  ('ORG_ADMIN', 'activation_code.manage'),
  ('ORG_ADMIN', 'fleet.read'),
  ('ORG_ADMIN', 'fleet.manage'),
  ('ORG_ADMIN', 'driver.read'),
  ('ORG_ADMIN', 'driver.manage'),
  ('ORG_ADMIN', 'vehicle.read'),
  ('ORG_ADMIN', 'vehicle.manage'),
  ('ORG_ADMIN', 'device.read'),
  ('ORG_ADMIN', 'device.manage'),
  ('ORG_ADMIN', 'session.read'),
  ('ORG_ADMIN', 'session.force_sign_out'),
  ('ORG_OPERATOR', 'overview.read'),
  ('ORG_OPERATOR', 'alert.read'),
  ('ORG_OPERATOR', 'alert.handle'),
  ('ORG_OPERATOR', 'stats.read'),
  ('ORG_OPERATOR', 'fleet.read'),
  ('ORG_OPERATOR', 'driver.read'),
  ('ORG_OPERATOR', 'vehicle.read'),
  ('ORG_OPERATOR', 'device.read'),
  ('ORG_OPERATOR', 'session.read'),
  ('ORG_ANALYST', 'overview.read'),
  ('ORG_ANALYST', 'alert.read'),
  ('ORG_ANALYST', 'stats.read'),
  ('ORG_ANALYST', 'fleet.read'),
  ('ORG_ANALYST', 'driver.read'),
  ('ORG_ANALYST', 'vehicle.read'),
  ('ORG_ANALYST', 'device.read'),
  ('ORG_ANALYST', 'session.read'),
  ('ORG_VIEWER', 'overview.read'),
  ('ORG_VIEWER', 'alert.read')
ON DUPLICATE KEY UPDATE
  created_at = created_at;

INSERT INTO user_scope_role (user_id, role_code, scope_type, enterprise_id, fleet_id, status, created_at, updated_at)
SELECT
  ur.user_id,
  CASE r.role_code
    WHEN 'SUPER_ADMIN' THEN 'PLATFORM_SUPER_ADMIN'
    WHEN 'SYS_ADMIN' THEN 'PLATFORM_SYS_ADMIN'
    WHEN 'RISK_ADMIN' THEN 'PLATFORM_RISK_ADMIN'
    WHEN 'ENTERPRISE_ADMIN' THEN 'ORG_ADMIN'
    WHEN 'OPERATOR' THEN 'ORG_OPERATOR'
    WHEN 'ANALYST' THEN 'ORG_ANALYST'
    WHEN 'VIEWER' THEN 'ORG_VIEWER'
  END AS role_code,
  CASE
    WHEN r.role_code IN ('SUPER_ADMIN', 'SYS_ADMIN', 'RISK_ADMIN') THEN 'PLATFORM'
    ELSE 'ENTERPRISE'
  END AS scope_type,
  CASE
    WHEN r.role_code IN ('ENTERPRISE_ADMIN', 'OPERATOR', 'ANALYST', 'VIEWER') THEN ua.enterprise_id
    ELSE NULL
  END AS enterprise_id,
  NULL AS fleet_id,
  1 AS status,
  CURRENT_TIMESTAMP AS created_at,
  CURRENT_TIMESTAMP AS updated_at
FROM user_role ur
JOIN role r ON r.id = ur.role_id
JOIN user_account ua ON ua.id = ur.user_id
WHERE (
  r.role_code IN ('SUPER_ADMIN', 'SYS_ADMIN', 'RISK_ADMIN')
  OR (ua.enterprise_id IS NOT NULL AND r.role_code IN ('ENTERPRISE_ADMIN', 'OPERATOR', 'ANALYST', 'VIEWER'))
)
ON DUPLICATE KEY UPDATE
  status = VALUES(status),
  updated_at = CURRENT_TIMESTAMP;
