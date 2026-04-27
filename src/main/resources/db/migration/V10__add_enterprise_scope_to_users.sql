ALTER TABLE user_account
  ADD COLUMN enterprise_id BIGINT DEFAULT NULL AFTER subject_type;

CREATE INDEX idx_user_account_enterprise_id ON user_account (enterprise_id);

INSERT INTO role (role_code, role_name)
VALUES ('ENTERPRISE_ADMIN', '企业管理员')
ON DUPLICATE KEY UPDATE
  role_name = VALUES(role_name);
