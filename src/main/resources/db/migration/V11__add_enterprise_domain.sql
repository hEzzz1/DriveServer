CREATE TABLE IF NOT EXISTS enterprise (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  contact_name VARCHAR(64) DEFAULT NULL,
  contact_phone VARCHAR(32) DEFAULT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_enterprise_code (code),
  KEY idx_enterprise_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE enterprise
  ADD CONSTRAINT chk_enterprise_status
    CHECK (status IN (0, 1));

ALTER TABLE user_account
  ADD CONSTRAINT fk_user_account_enterprise_id
    FOREIGN KEY (enterprise_id) REFERENCES enterprise (id)
    ON UPDATE CASCADE ON DELETE RESTRICT;
