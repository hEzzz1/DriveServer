CREATE TABLE IF NOT EXISTS edge_device_bind_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  device_id BIGINT NOT NULL,
  device_code VARCHAR(64) NOT NULL,
  enterprise_id BIGINT NOT NULL,
  enterprise_name_snapshot VARCHAR(128) DEFAULT NULL,
  activation_code_masked VARCHAR(64) DEFAULT NULL,
  action VARCHAR(32) NOT NULL,
  operator_type VARCHAR(32) NOT NULL,
  operator_id BIGINT DEFAULT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_edge_device_bind_log_enterprise_time (enterprise_id, created_at, id),
  KEY idx_edge_device_bind_log_device_time (device_id, created_at, id),
  CONSTRAINT fk_edge_device_bind_log_device
    FOREIGN KEY (device_id) REFERENCES edge_device (id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_edge_device_bind_log_enterprise
    FOREIGN KEY (enterprise_id) REFERENCES enterprise (id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
