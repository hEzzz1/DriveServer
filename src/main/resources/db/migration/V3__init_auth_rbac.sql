CREATE TABLE IF NOT EXISTS role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(32) NOT NULL COMMENT 'ADMIN/OPERATOR/VIEWER',
  role_name VARCHAR(64) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_user_role (user_id, role_id),
  KEY idx_user_id (user_id),
  KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO role (role_code, role_name)
VALUES
  ('ADMIN', '系统管理员'),
  ('OPERATOR', '运维操作员'),
  ('VIEWER', '只读观察员')
ON DUPLICATE KEY UPDATE
  role_name = VALUES(role_name);

INSERT INTO user_role (user_id, role_id)
SELECT ua.id, r.id
FROM user_account ua
JOIN role r ON r.role_code = 'ADMIN'
WHERE ua.username = 'admin'
ON DUPLICATE KEY UPDATE
  user_id = VALUES(user_id);
