ALTER TABLE rule_config
  ADD COLUMN risk_level TINYINT NULL AFTER rule_name;

UPDATE rule_config
SET risk_level = CASE
  WHEN rule_code = 'RISK_HIGH' THEN 3
  WHEN rule_code = 'RISK_MID' THEN 2
  WHEN rule_code = 'RISK_LOW' THEN 1
  ELSE NULL
END;

ALTER TABLE rule_config_version
  ADD COLUMN risk_level TINYINT NULL AFTER rule_name;

UPDATE rule_config_version
SET risk_level = CASE
  WHEN rule_code = 'RISK_HIGH' THEN 3
  WHEN rule_code = 'RISK_MID' THEN 2
  WHEN rule_code = 'RISK_LOW' THEN 1
  ELSE NULL
END;

DELIMITER $$

CREATE PROCEDURE validate_rule_risk_level_backfill()
BEGIN
  IF EXISTS (SELECT 1 FROM rule_config WHERE risk_level IS NULL) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'V6 migration requires manual risk_level backfill for existing rule_config rows';
  END IF;

  IF EXISTS (SELECT 1 FROM rule_config_version WHERE risk_level IS NULL) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'V6 migration requires manual risk_level backfill for existing rule_config_version rows';
  END IF;
END$$

CALL validate_rule_risk_level_backfill()$$
DROP PROCEDURE validate_rule_risk_level_backfill$$

DELIMITER ;

ALTER TABLE rule_config
  MODIFY COLUMN risk_level TINYINT NOT NULL;

ALTER TABLE rule_config_version
  MODIFY COLUMN risk_level TINYINT NOT NULL;
