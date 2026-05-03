ALTER TABLE alert_event
  ADD COLUMN evidence_type VARCHAR(32) NULL AFTER edge_created_at_ms,
  ADD COLUMN evidence_url LONGTEXT NULL AFTER evidence_type,
  ADD COLUMN evidence_mime_type VARCHAR(64) NULL AFTER evidence_url,
  ADD COLUMN evidence_captured_at_ms BIGINT NULL AFTER evidence_mime_type,
  ADD COLUMN evidence_retention_until DATETIME(3) NULL AFTER evidence_captured_at_ms;
