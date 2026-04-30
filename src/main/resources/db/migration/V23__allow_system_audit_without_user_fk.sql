ALTER TABLE system_audit_log
  DROP FOREIGN KEY fk_system_audit_log_operator_id;

ALTER TABLE system_audit_log
  MODIFY COLUMN operator_id BIGINT NULL,
  MODIFY COLUMN action_by BIGINT NULL DEFAULT NULL;

UPDATE system_audit_log
SET operator_id = NULL
WHERE operator_id = 0;

UPDATE system_audit_log
SET action_by = NULL
WHERE action_by = 0;
