ALTER TABLE user_account
  ADD CONSTRAINT chk_user_account_status
    CHECK (status IN (0, 1));

ALTER TABLE rule_config
  ADD CONSTRAINT chk_rule_config_risk_threshold
    CHECK (risk_threshold >= 0.0000 AND risk_threshold <= 1.0000),
  ADD CONSTRAINT chk_rule_config_duration_seconds
    CHECK (duration_seconds > 0),
  ADD CONSTRAINT chk_rule_config_cooldown_seconds
    CHECK (cooldown_seconds >= 0),
  ADD CONSTRAINT chk_rule_config_enabled
    CHECK (enabled IN (0, 1)),
  ADD CONSTRAINT chk_rule_config_version
    CHECK (version > 0);

ALTER TABLE user_role
  ADD CONSTRAINT fk_user_role_user_id
    FOREIGN KEY (user_id) REFERENCES user_account (id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  ADD CONSTRAINT fk_user_role_role_id
    FOREIGN KEY (role_id) REFERENCES role (id)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE alert_event
  ADD CONSTRAINT chk_alert_event_risk_level
    CHECK (risk_level IN (1, 2, 3)),
  ADD CONSTRAINT chk_alert_event_status
    CHECK (status IN (0, 1, 2, 3)),
  ADD CONSTRAINT chk_alert_event_risk_score
    CHECK (risk_score >= 0.0000 AND risk_score <= 1.0000),
  ADD CONSTRAINT chk_alert_event_fatigue_score
    CHECK (fatigue_score >= 0.0000 AND fatigue_score <= 1.0000),
  ADD CONSTRAINT chk_alert_event_distraction_score
    CHECK (distraction_score >= 0.0000 AND distraction_score <= 1.0000),
  ADD CONSTRAINT fk_alert_event_rule_id
    FOREIGN KEY (rule_id) REFERENCES rule_config (id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  ADD CONSTRAINT fk_alert_event_latest_action_by
    FOREIGN KEY (latest_action_by) REFERENCES user_account (id)
    ON UPDATE CASCADE ON DELETE SET NULL;

ALTER TABLE alert_action_log
  ADD CONSTRAINT chk_alert_action_log_action_type
    CHECK (action_type IN ('CREATE', 'CONFIRM', 'FALSE_POSITIVE', 'CLOSE')),
  ADD CONSTRAINT fk_alert_action_log_alert_id
    FOREIGN KEY (alert_id) REFERENCES alert_event (id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  ADD CONSTRAINT fk_alert_action_log_action_by
    FOREIGN KEY (action_by) REFERENCES user_account (id)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE system_audit_log
  ADD CONSTRAINT fk_system_audit_log_operator_id
    FOREIGN KEY (operator_id) REFERENCES user_account (id)
    ON UPDATE CASCADE ON DELETE RESTRICT;
