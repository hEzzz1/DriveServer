CREATE TABLE IF NOT EXISTS user_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  nickname VARCHAR(64) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1=enabled,0=disabled',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rule_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rule_code VARCHAR(64) NOT NULL,
  rule_name VARCHAR(128) NOT NULL,
  risk_threshold DECIMAL(5,4) NOT NULL,
  duration_seconds INT NOT NULL,
  cooldown_seconds INT NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  version INT NOT NULL DEFAULT 1,
  created_by BIGINT NOT NULL DEFAULT 0,
  updated_by BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_rule_code (rule_code),
  KEY idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS alert_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  alert_no VARCHAR(64) NOT NULL,
  fleet_id BIGINT NOT NULL,
  vehicle_id BIGINT NOT NULL,
  driver_id BIGINT NOT NULL,
  rule_id BIGINT NOT NULL,
  risk_level TINYINT NOT NULL COMMENT '1=low,2=mid,3=high',
  risk_score DECIMAL(5,4) NOT NULL,
  fatigue_score DECIMAL(5,4) NOT NULL,
  distraction_score DECIMAL(5,4) NOT NULL,
  trigger_time DATETIME(3) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0=new,1=confirmed,2=false_positive,3=closed',
  latest_action_by BIGINT DEFAULT NULL,
  latest_action_time DATETIME(3) DEFAULT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_alert_no (alert_no),
  KEY idx_vehicle_time (vehicle_id, trigger_time),
  KEY idx_driver_time (driver_id, trigger_time),
  KEY idx_status_time (status, trigger_time),
  KEY idx_level_status (risk_level, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS alert_action_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  alert_id BIGINT NOT NULL,
  action_type VARCHAR(32) NOT NULL COMMENT 'CREATE/CONFIRM/FALSE_POSITIVE/CLOSE',
  action_by BIGINT NOT NULL,
  action_time DATETIME(3) NOT NULL,
  action_remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_alert_id_time (alert_id, action_time),
  KEY idx_action_by_time (action_by, action_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS system_audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  operator_id BIGINT NOT NULL,
  operator_name VARCHAR(64) NOT NULL,
  module VARCHAR(64) NOT NULL,
  action VARCHAR(64) NOT NULL,
  target_id VARCHAR(64) DEFAULT NULL,
  detail_json JSON DEFAULT NULL,
  ip VARCHAR(64) DEFAULT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_operator_time (operator_id, created_at),
  KEY idx_module_time (module, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
