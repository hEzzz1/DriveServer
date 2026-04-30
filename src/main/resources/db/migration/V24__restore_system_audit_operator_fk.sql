ALTER TABLE system_audit_log
  ADD CONSTRAINT fk_system_audit_log_operator_id
    FOREIGN KEY (operator_id) REFERENCES user_account (id)
    ON UPDATE CASCADE ON DELETE RESTRICT;
