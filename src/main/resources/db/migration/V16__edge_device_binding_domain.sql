RENAME TABLE device TO edge_device;

ALTER TABLE edge_device
  MODIFY COLUMN enterprise_id BIGINT NULL,
  MODIFY COLUMN fleet_id BIGINT NULL,
  MODIFY COLUMN vehicle_id BIGINT NULL,
  CHANGE COLUMN last_online_at last_seen_at DATETIME(3) NULL,
  MODIFY COLUMN status VARCHAR(64) NOT NULL;

UPDATE edge_device
SET status = CASE
  WHEN status = '0' THEN 'DISABLED'
  WHEN last_activated_at IS NULL THEN 'NEW'
  WHEN enterprise_id IS NULL THEN 'ACTIVATED'
  WHEN vehicle_id IS NULL THEN 'ENTERPRISE_BOUND'
  ELSE 'VEHICLE_BOUND'
END;

CREATE TABLE IF NOT EXISTS edge_device_bind_request (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  device_id BIGINT NOT NULL,
  device_code VARCHAR(64) NOT NULL,
  requested_enterprise_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  apply_remark VARCHAR(255) DEFAULT NULL,
  review_remark VARCHAR(255) DEFAULT NULL,
  submitted_at DATETIME(3) NOT NULL,
  reviewed_at DATETIME(3) DEFAULT NULL,
  reviewed_by BIGINT DEFAULT NULL,
  expires_at DATETIME(3) DEFAULT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_edge_device_bind_request_device_status (device_id, status),
  KEY idx_edge_device_bind_request_enterprise_status (requested_enterprise_id, status),
  CONSTRAINT fk_edge_device_bind_request_device
    FOREIGN KEY (device_id) REFERENCES edge_device (id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_edge_device_bind_request_enterprise
    FOREIGN KEY (requested_enterprise_id) REFERENCES enterprise (id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
