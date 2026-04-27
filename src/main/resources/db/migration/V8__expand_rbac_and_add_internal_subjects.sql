ALTER TABLE user_account
  ADD COLUMN subject_type VARCHAR(16) NOT NULL DEFAULT 'USER' AFTER nickname;

UPDATE user_account
SET subject_type = 'USER'
WHERE subject_type IS NULL OR subject_type = '';

ALTER TABLE user_account
  ADD CONSTRAINT chk_user_account_subject_type
    CHECK (subject_type IN ('USER', 'SYSTEM'));

UPDATE role
SET role_code = 'SUPER_ADMIN',
    role_name = '超级管理员'
WHERE role_code = 'ADMIN';

UPDATE role
SET role_name = '操作员'
WHERE role_code = 'OPERATOR';

UPDATE role
SET role_name = '只读用户'
WHERE role_code = 'VIEWER';

INSERT INTO role (role_code, role_name)
VALUES
  ('SYS_ADMIN', '系统管理员'),
  ('RISK_ADMIN', '风控管理员'),
  ('ANALYST', '分析员')
ON DUPLICATE KEY UPDATE
  role_name = VALUES(role_name);

INSERT INTO user_account (username, password_hash, nickname, subject_type, status)
VALUES ('system-auto-alert', '$2y$10$alTLqzEgJ1uec4ZdHGjzzO5eVGBhqh28UBZZwVLZ98ooIpVGFd9He', '系统自动告警', 'SYSTEM', 0)
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  subject_type = VALUES(subject_type),
  status = VALUES(status);
