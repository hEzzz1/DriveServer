ALTER TABLE driving_session
  ADD COLUMN last_heartbeat_at DATETIME(3) NULL AFTER sign_in_time;
