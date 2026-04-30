ALTER TABLE edge_device
  DROP CHECK chk_device_status;

UPDATE edge_device
SET status = CASE
  WHEN status IN ('DISABLED', '0') THEN 'DISABLED'
  WHEN enterprise_id IS NULL THEN 'NEW'
  ELSE 'BOUND'
END;

ALTER TABLE edge_device
  ADD CONSTRAINT chk_device_status
    CHECK (status IN ('NEW', 'BOUND', 'DISABLED'));
