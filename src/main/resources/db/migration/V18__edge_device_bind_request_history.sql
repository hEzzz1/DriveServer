CREATE TABLE IF NOT EXISTS edge_device_bind_request_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  bind_request_id BIGINT NOT NULL,
  action VARCHAR(32) NOT NULL,
  operator_id BIGINT DEFAULT NULL,
  operator_name VARCHAR(128) DEFAULT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_edge_device_bind_request_history_request_time (bind_request_id, created_at, id),
  CONSTRAINT fk_edge_device_bind_request_history_request
    FOREIGN KEY (bind_request_id) REFERENCES edge_device_bind_request (id)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
