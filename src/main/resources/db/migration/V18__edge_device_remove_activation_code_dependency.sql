UPDATE edge_device
SET status = CASE
  WHEN status = 'DISABLED' THEN 'DISABLED'
  WHEN enterprise_id IS NULL THEN 'NEW'
  ELSE 'BOUND'
END;
