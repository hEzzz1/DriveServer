CREATE TABLE IF NOT EXISTS system_error_trace (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  trace_id VARCHAR(64) NOT NULL,
  occurred_at DATETIME(3) NOT NULL,
  method VARCHAR(16) DEFAULT NULL,
  request_path VARCHAR(512) DEFAULT NULL,
  query_string VARCHAR(1024) DEFAULT NULL,
  http_status INT NOT NULL,
  code INT NOT NULL,
  message VARCHAR(255) NOT NULL,
  exception_class VARCHAR(255) DEFAULT NULL,
  summary VARCHAR(1000) DEFAULT NULL,
  operator_id BIGINT DEFAULT NULL,
  ip VARCHAR(64) DEFAULT NULL,
  user_agent VARCHAR(255) DEFAULT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_system_error_trace_trace_id (trace_id),
  KEY idx_system_error_trace_occurred_at (occurred_at),
  KEY idx_system_error_trace_status (http_status),
  CONSTRAINT fk_system_error_trace_operator_id
    FOREIGN KEY (operator_id) REFERENCES user_account (id)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @driver_fk_exists = (
  SELECT COUNT(*)
  FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'driver'
    AND CONSTRAINT_NAME = 'fk_driver_fleet_enterprise'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

SET @drop_driver_fk_sql = IF(
  @driver_fk_exists > 0,
  'ALTER TABLE driver DROP FOREIGN KEY fk_driver_fleet_enterprise',
  'SELECT 1'
);

PREPARE drop_driver_fk_stmt FROM @drop_driver_fk_sql;
EXECUTE drop_driver_fk_stmt;
DEALLOCATE PREPARE drop_driver_fk_stmt;

ALTER TABLE driver MODIFY COLUMN fleet_id BIGINT NULL;

SET @driver_fk_exists = (
  SELECT COUNT(*)
  FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'driver'
    AND CONSTRAINT_NAME = 'fk_driver_fleet_enterprise'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

SET @add_driver_fk_sql = IF(
  @driver_fk_exists = 0,
  'ALTER TABLE driver ADD CONSTRAINT fk_driver_fleet_enterprise FOREIGN KEY (fleet_id, enterprise_id) REFERENCES fleet (id, enterprise_id) ON UPDATE CASCADE ON DELETE RESTRICT',
  'SELECT 1'
);

PREPARE add_driver_fk_stmt FROM @add_driver_fk_sql;
EXECUTE add_driver_fk_stmt;
DEALLOCATE PREPARE add_driver_fk_stmt;

UPDATE permission
SET name = '查看驾驶员', description = '驾驶员列表与详情'
WHERE code = 'driver.read';

UPDATE permission
SET name = '管理驾驶员', description = '创建、编辑、调整车队与重置签到码'
WHERE code = 'driver.manage';
