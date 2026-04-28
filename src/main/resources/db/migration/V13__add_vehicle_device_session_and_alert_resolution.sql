ALTER TABLE driver
  ADD COLUMN driver_code VARCHAR(64) NULL AFTER fleet_id,
  ADD COLUMN pin_hash VARCHAR(255) NULL AFTER driver_code;

CREATE UNIQUE INDEX uk_driver_enterprise_driver_code ON driver (enterprise_id, driver_code);

CREATE TABLE IF NOT EXISTS vehicle (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  enterprise_id BIGINT NOT NULL,
  fleet_id BIGINT NOT NULL,
  plate_number VARCHAR(32) NOT NULL,
  vin VARCHAR(64) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_vehicle_enterprise_plate (enterprise_id, plate_number),
  UNIQUE KEY uk_vehicle_vin (vin),
  KEY idx_vehicle_enterprise_id (enterprise_id),
  KEY idx_vehicle_fleet_id (fleet_id),
  KEY idx_vehicle_status (status),
  CONSTRAINT fk_vehicle_enterprise_id
    FOREIGN KEY (enterprise_id) REFERENCES enterprise (id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_vehicle_fleet_enterprise
    FOREIGN KEY (fleet_id, enterprise_id) REFERENCES fleet (id, enterprise_id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT chk_vehicle_status
    CHECK (status IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS device (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  enterprise_id BIGINT NOT NULL,
  fleet_id BIGINT NOT NULL,
  vehicle_id BIGINT NOT NULL,
  device_code VARCHAR(64) NOT NULL,
  device_name VARCHAR(128) NOT NULL,
  activation_code VARCHAR(64) DEFAULT NULL,
  device_token VARCHAR(255) DEFAULT NULL,
  last_activated_at DATETIME(3) DEFAULT NULL,
  token_rotated_at DATETIME(3) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_device_code (device_code),
  UNIQUE KEY uk_device_token (device_token),
  KEY idx_device_enterprise_id (enterprise_id),
  KEY idx_device_fleet_id (fleet_id),
  KEY idx_device_vehicle_id (vehicle_id),
  KEY idx_device_status (status),
  CONSTRAINT fk_device_enterprise_id
    FOREIGN KEY (enterprise_id) REFERENCES enterprise (id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_device_fleet_enterprise
    FOREIGN KEY (fleet_id, enterprise_id) REFERENCES fleet (id, enterprise_id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_device_vehicle_id
    FOREIGN KEY (vehicle_id) REFERENCES vehicle (id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT chk_device_status
    CHECK (status IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS driving_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_no VARCHAR(64) NOT NULL,
  enterprise_id BIGINT NOT NULL,
  fleet_id BIGINT NOT NULL,
  vehicle_id BIGINT NOT NULL,
  driver_id BIGINT NOT NULL,
  device_id BIGINT NOT NULL,
  sign_in_time DATETIME(3) NOT NULL,
  sign_out_time DATETIME(3) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  closed_reason VARCHAR(64) DEFAULT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_driving_session_no (session_no),
  KEY idx_driving_session_device_status (device_id, status),
  KEY idx_driving_session_driver_status (driver_id, status),
  KEY idx_driving_session_vehicle_status (vehicle_id, status),
  KEY idx_driving_session_enterprise_status (enterprise_id, status),
  CONSTRAINT fk_driving_session_enterprise_id
    FOREIGN KEY (enterprise_id) REFERENCES enterprise (id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_driving_session_fleet_enterprise
    FOREIGN KEY (fleet_id, enterprise_id) REFERENCES fleet (id, enterprise_id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_driving_session_vehicle_id
    FOREIGN KEY (vehicle_id) REFERENCES vehicle (id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_driving_session_driver_id
    FOREIGN KEY (driver_id) REFERENCES driver (id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_driving_session_device_id
    FOREIGN KEY (device_id) REFERENCES device (id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT chk_driving_session_status
    CHECK (status IN (1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE alert_event
  ADD COLUMN enterprise_id BIGINT NULL AFTER alert_no,
  ADD COLUMN device_id BIGINT NULL AFTER driver_id,
  ADD COLUMN session_id BIGINT NULL AFTER device_id,
  ADD COLUMN reported_enterprise_id BIGINT NULL AFTER session_id,
  ADD COLUMN reported_fleet_id BIGINT NULL AFTER reported_enterprise_id,
  ADD COLUMN reported_vehicle_id BIGINT NULL AFTER reported_fleet_id,
  ADD COLUMN reported_driver_id BIGINT NULL AFTER reported_vehicle_id,
  ADD COLUMN resolved_enterprise_id BIGINT NULL AFTER reported_driver_id,
  ADD COLUMN resolved_fleet_id BIGINT NULL AFTER resolved_enterprise_id,
  ADD COLUMN resolved_vehicle_id BIGINT NULL AFTER resolved_fleet_id,
  ADD COLUMN resolved_driver_id BIGINT NULL AFTER resolved_vehicle_id,
  ADD COLUMN resolution_status VARCHAR(32) NULL AFTER resolved_driver_id,
  ADD COLUMN config_version VARCHAR(64) NULL AFTER resolution_status;
