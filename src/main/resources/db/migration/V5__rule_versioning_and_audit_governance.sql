ALTER TABLE rule_config
  ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' AFTER enabled,
  ADD COLUMN published_at DATETIME(3) DEFAULT NULL AFTER status,
  ADD COLUMN published_by BIGINT DEFAULT NULL AFTER published_at,
  ADD COLUMN archived_at DATETIME(3) DEFAULT NULL AFTER published_by,
  ADD COLUMN archived_by BIGINT DEFAULT NULL AFTER archived_at;

UPDATE rule_config
SET status = CASE WHEN enabled = 1 THEN 'ENABLED' ELSE 'DISABLED' END,
    published_at = CASE WHEN enabled = 1 THEN created_at ELSE NULL END,
    published_by = CASE WHEN enabled = 1 THEN created_by ELSE NULL END;

ALTER TABLE system_audit_log
  ADD COLUMN action_type VARCHAR(64) NOT NULL DEFAULT '' AFTER action,
  ADD COLUMN action_by BIGINT NOT NULL DEFAULT 0 AFTER action_type,
  ADD COLUMN action_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) AFTER action_by,
  ADD COLUMN action_target_type VARCHAR(64) NOT NULL DEFAULT '' AFTER action_time,
  ADD COLUMN action_target_id VARCHAR(64) DEFAULT NULL AFTER action_target_type,
  ADD COLUMN action_result VARCHAR(32) NOT NULL DEFAULT 'SUCCESS' AFTER action_target_id,
  ADD COLUMN action_remark VARCHAR(255) DEFAULT NULL AFTER action_result,
  ADD COLUMN trace_id VARCHAR(64) DEFAULT NULL AFTER ip,
  ADD COLUMN user_agent VARCHAR(255) DEFAULT NULL AFTER trace_id;

CREATE TABLE IF NOT EXISTS rule_config_version (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rule_config_id BIGINT NOT NULL,
  version_no INT NOT NULL,
  rule_code VARCHAR(64) NOT NULL,
  rule_name VARCHAR(128) NOT NULL,
  risk_threshold DECIMAL(5,4) NOT NULL,
  duration_seconds INT NOT NULL,
  cooldown_seconds INT NOT NULL,
  enabled TINYINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  change_source VARCHAR(32) NOT NULL,
  change_summary VARCHAR(255) DEFAULT NULL,
  snapshot_json JSON NOT NULL,
  created_by BIGINT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_rule_config_version (rule_config_id, version_no),
  KEY idx_rule_config_id_created_at (rule_config_id, created_at),
  CONSTRAINT fk_rule_config_version_rule_config_id
    FOREIGN KEY (rule_config_id) REFERENCES rule_config (id)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
