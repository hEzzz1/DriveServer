CREATE TABLE IF NOT EXISTS fleet (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  enterprise_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_fleet_enterprise_name (enterprise_id, name),
  UNIQUE KEY uk_fleet_id_enterprise (id, enterprise_id),
  KEY idx_fleet_enterprise_id (enterprise_id),
  KEY idx_fleet_status (status),
  CONSTRAINT fk_fleet_enterprise_id
    FOREIGN KEY (enterprise_id) REFERENCES enterprise (id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT chk_fleet_status
    CHECK (status IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS driver (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  enterprise_id BIGINT NOT NULL,
  fleet_id BIGINT NOT NULL,
  name VARCHAR(64) NOT NULL,
  phone VARCHAR(32) DEFAULT NULL,
  license_no VARCHAR(64) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_driver_enterprise_id (enterprise_id),
  KEY idx_driver_fleet_id (fleet_id),
  KEY idx_driver_status (status),
  KEY idx_driver_name (name),
  KEY idx_driver_license_no (license_no),
  CONSTRAINT fk_driver_enterprise_id
    FOREIGN KEY (enterprise_id) REFERENCES enterprise (id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_driver_fleet_enterprise
    FOREIGN KEY (fleet_id, enterprise_id) REFERENCES fleet (id, enterprise_id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT chk_driver_status
    CHECK (status IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
