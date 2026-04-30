ALTER TABLE edge_device_bind_request
  ADD COLUMN approve_remark VARCHAR(255) DEFAULT NULL AFTER apply_remark,
  ADD COLUMN reject_reason VARCHAR(255) DEFAULT NULL AFTER approve_remark;

UPDATE edge_device_bind_request
SET approve_remark = CASE WHEN status = 'APPROVED' THEN review_remark ELSE approve_remark END,
    reject_reason = CASE WHEN status = 'REJECTED' THEN review_remark ELSE reject_reason END;
