INSERT INTO user_account (username, password_hash, nickname, status)
VALUES ('admin', '$2y$10$alTLqzEgJ1uec4ZdHGjzzO5eVGBhqh28UBZZwVLZ98ooIpVGFd9He', '系统管理员', 1)
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  status = VALUES(status);

INSERT INTO rule_config (
  rule_code,
  rule_name,
  risk_threshold,
  duration_seconds,
  cooldown_seconds,
  enabled,
  version,
  created_by,
  updated_by
)
VALUES
  ('RISK_HIGH', '高风险规则', 0.8000, 3, 60, 1, 1, 1, 1),
  ('RISK_MID', '中风险规则', 0.6500, 5, 60, 1, 1, 1, 1),
  ('RISK_LOW', '低风险规则', 0.5000, 8, 60, 1, 1, 1, 1)
ON DUPLICATE KEY UPDATE
  rule_name = VALUES(rule_name),
  risk_threshold = VALUES(risk_threshold),
  duration_seconds = VALUES(duration_seconds),
  cooldown_seconds = VALUES(cooldown_seconds),
  enabled = VALUES(enabled),
  updated_by = VALUES(updated_by),
  version = version + 1;
