ALTER TABLE edge_device
  ADD COLUMN upload_queue_size INT NULL AFTER token_rotated_at,
  ADD COLUMN upload_last_success_at DATETIME(3) NULL AFTER upload_queue_size,
  ADD COLUMN upload_last_failed_at DATETIME(3) NULL AFTER upload_last_success_at,
  ADD COLUMN upload_last_failure_class VARCHAR(64) NULL AFTER upload_last_failed_at,
  ADD COLUMN upload_last_error_message VARCHAR(255) NULL AFTER upload_last_failure_class,
  ADD COLUMN upload_last_report_at DATETIME(3) NULL AFTER upload_last_error_message;
