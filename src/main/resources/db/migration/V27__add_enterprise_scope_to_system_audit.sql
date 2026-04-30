ALTER TABLE system_audit_log
  ADD COLUMN operator_enterprise_id BIGINT NULL AFTER operator_id,
  ADD COLUMN target_enterprise_id BIGINT NULL AFTER action_target_id,
  ADD KEY idx_audit_operator_enterprise_time (operator_enterprise_id, action_time),
  ADD KEY idx_audit_target_enterprise_time (target_enterprise_id, action_time);

UPDATE system_audit_log sal
LEFT JOIN user_account ua ON ua.id = sal.operator_id
SET sal.operator_enterprise_id = ua.enterprise_id
WHERE sal.operator_enterprise_id IS NULL;

UPDATE system_audit_log
SET target_enterprise_id = CAST(JSON_UNQUOTE(JSON_EXTRACT(detail_json, '$.targetEnterpriseId')) AS UNSIGNED)
WHERE detail_json IS NOT NULL
  AND target_enterprise_id IS NULL
  AND JSON_EXTRACT(detail_json, '$.targetEnterpriseId') IS NOT NULL;

UPDATE system_audit_log
SET target_enterprise_id = CAST(JSON_UNQUOTE(JSON_EXTRACT(detail_json, '$.enterpriseId')) AS UNSIGNED)
WHERE detail_json IS NOT NULL
  AND target_enterprise_id IS NULL
  AND JSON_EXTRACT(detail_json, '$.enterpriseId') IS NOT NULL;

UPDATE system_audit_log
SET target_enterprise_id = CAST(action_target_id AS UNSIGNED)
WHERE target_enterprise_id IS NULL
  AND action_target_type = 'ENTERPRISE'
  AND action_target_id REGEXP '^[0-9]+$';

INSERT INTO role_permission (role_code, permission_code)
VALUES
  ('ORG_ADMIN', 'audit.read'),
  ('ORG_ADMIN', 'audit.export')
ON DUPLICATE KEY UPDATE
  permission_code = VALUES(permission_code);
