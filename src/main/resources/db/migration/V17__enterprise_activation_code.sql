ALTER TABLE enterprise
  ADD COLUMN activation_code VARCHAR(64) DEFAULT NULL AFTER remark,
  ADD COLUMN activation_code_status VARCHAR(32) DEFAULT NULL AFTER activation_code,
  ADD COLUMN activation_code_rotated_at DATETIME(3) DEFAULT NULL AFTER activation_code_status,
  ADD COLUMN activation_code_expires_at DATETIME(3) DEFAULT NULL AFTER activation_code_rotated_at,
  ADD COLUMN activation_code_remark VARCHAR(255) DEFAULT NULL AFTER activation_code_expires_at;

UPDATE enterprise
SET activation_code = CONCAT('ENT-', LPAD(id, 4, '0'), '-', LPAD(HEX(id), 4, '0'))
WHERE activation_code IS NULL;

UPDATE enterprise
SET activation_code_status = 'ACTIVE',
    activation_code_rotated_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP(3))
WHERE activation_code_status IS NULL;

ALTER TABLE enterprise
  ADD CONSTRAINT uk_enterprise_activation_code UNIQUE (activation_code);
