ALTER TABLE alert_event
  ADD COLUMN edge_risk_level VARCHAR(32) NULL AFTER distraction_score,
  ADD COLUMN edge_dominant_risk_type VARCHAR(32) NULL AFTER edge_risk_level,
  ADD COLUMN edge_trigger_reasons VARCHAR(255) NULL AFTER edge_dominant_risk_type,
  ADD COLUMN edge_window_start_ms BIGINT NULL AFTER edge_trigger_reasons,
  ADD COLUMN edge_window_end_ms BIGINT NULL AFTER edge_window_start_ms,
  ADD COLUMN edge_created_at_ms BIGINT NULL AFTER edge_window_end_ms;
