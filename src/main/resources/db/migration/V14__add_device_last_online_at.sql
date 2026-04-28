ALTER TABLE device
  ADD COLUMN last_online_at DATETIME(3) NULL AFTER last_activated_at;
